package ru.diasoft.integration.vtb.service.stub.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.utils.ParamsUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageSender {


    String url;
    String topic;
    String sslTruststoreLocation;
    String sslTruststorePassword;
    String sslKeystoreLocation;
    String sslKeystorePassword;
    String sslKeyPassword;
    String compressionType;
    Boolean needCompress;

    Boolean sendSuccess = false;
    Properties properties;

    public static final Logger logger = LogManager.getLogger(MessageSender.class);

    public Boolean getSendSuccess() {
        return sendSuccess;
    }

    //на вход подаём только параметры кафки
    public MessageSender(Map<String, Object> kafkaProps) {
        url = ParamsUtil.getString(kafkaProps.get("Url"));
        topic = ParamsUtil.getString(kafkaProps.get("Topic"));
        sslTruststoreLocation = ParamsUtil.getString(kafkaProps.get("SslTruststoreLocation"));
        sslTruststorePassword = ParamsUtil.getString(kafkaProps.get("SslTruststorePassword"));
        sslKeystoreLocation = ParamsUtil.getString(kafkaProps.get("SslKeystoreLocation"));
        sslKeystorePassword = ParamsUtil.getString(kafkaProps.get("SslKeystorePassword"));
        sslKeyPassword = ParamsUtil.getString(kafkaProps.get("SslKeyPassword"));
        compressionType = ParamsUtil.getString(kafkaProps.get("CompressionType"));

        properties = new Properties();
        if (ParamsUtil.isNotEmpty(url)) {
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        }

        if (ParamsUtil.isNotEmpty(sslTruststoreLocation) &&
                ParamsUtil.isNotEmpty(sslTruststorePassword) &&
                ParamsUtil.isNotEmpty(sslKeystoreLocation) &&
                ParamsUtil.isNotEmpty(sslKeystorePassword) &&
                ParamsUtil.isNotEmpty(sslKeyPassword)
        ) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);
            properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
            properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
            properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
        }

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        if (ParamsUtil.isNotEmpty(compressionType)) {
            properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
            needCompress = true;
        } else {
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            needCompress = false;
        }

    }

    //просто отправить сообщение с хедерами (партиция определится сама) и ключом
    public void send(String sendMessage, List<Header> headersList, String key) throws Exception {

        final KafkaProducer<String, Object> producer = new KafkaProducer<String, Object>(properties);
        ProducerRecord<String, Object> record = null;

        if (needCompress) {
            logger.debug("compress with type: " + compressionType);
            byte[] messageBytes;
            if (compressionType.equals("gzip")) {
                messageBytes = compressByGzip(sendMessage);
                if (messageBytes == null) {
                    throw new Exception("Can't compress message");
                }
                record = new ProducerRecord<>(String.valueOf(topic), null, null, key, messageBytes, headersList);
            }
        } else {
            record = new ProducerRecord<>(String.valueOf(topic), null, null, key, sendMessage, headersList);
        }

        Future<RecordMetadata> future = producer.send(record);

        try {
            RecordMetadata meta = future.get();
            if (meta.hasOffset()) {
                sendSuccess = true;
                String message = String.format("sent message to topic:%s partition:%s offset:%s", meta.topic(), meta.partition(), meta.offset());
                logger.debug(message);
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.debug(e.getMessage());
        }

        producer.close();
    }

    //отправка сообщения с хедером в указанную партицию
    public void sendOld(String keyHeader, String valueHeader, String key, Integer partition, String sendMessage) {
        Header headerRequest = new RecordHeader(keyHeader, valueHeader.getBytes());
        List<Header> headerList = Arrays.asList(headerRequest);

        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        ProducerRecord<String, String> records = new ProducerRecord(String.valueOf(topic), partition, key, sendMessage, headerList);
        producer.send(records, (recordMetadata, e) -> {
            if (e != null) {
                logger.debug("Error while producing message to topic :" + recordMetadata);
                sendSuccess = false;
            } else {
                String message = String.format("sent message to topic:%s partition:%s  offset:%s", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                logger.debug(message);
                sendSuccess = true;
            }
        });

        producer.close();
    }

    public void sendWithDefaultKey(String key, String sendMessage) {
        sendOld("default key header request", "default value header request", key, 0, sendMessage);
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static byte[] compressByGzip(String message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(message.getBytes());
        gzipOutputStream.close();
        byte[] resultArray = byteArrayOutputStream.toByteArray();
        if (isCompressed(resultArray)) {
            return resultArray;
        }
        return null;
    }


}
