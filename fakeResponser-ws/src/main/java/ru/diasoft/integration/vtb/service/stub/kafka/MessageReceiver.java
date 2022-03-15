package ru.diasoft.integration.vtb.service.stub.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.Constants;
import ru.diasoft.integration.vtb.service.stub.rest.RestServer;
import ru.diasoft.integration.vtb.utils.ParamsUtil;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

public class MessageReceiver {
    public static final Logger logger = LogManager.getLogger(MessageReceiver.class);
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-YY");

    private KafkaConsumer<String, Object> consumer;
    public final AtomicBoolean isInterrupted = new AtomicBoolean(false);
    private final Thread receiverThread;
    public String url;
    public String topic;
    public String service;
    public String method;
    public String groupId;
    public String messageFilterString;
    public Long durationOfMillis;
    public String isEarliest;
    public String seekToEnd;
    public String testRequestId;

    private int сalcFalseConnections = 0;
    private List<Integer> сalcFalseConnectionsList = new ArrayList<>();
    String login;
    String command;
    public String comments;
    String compressionType;
    Boolean needDecompress;

    /**
     * Конструктор класса, который инициализирует объект тип Consumer (подписчика)
     *
     * @param kafkaProps - параметры, служащие для конфигурации подписчика
     */
    public MessageReceiver(Map<String, Object> kafkaProps) {


        url = ParamsUtil.getString(kafkaProps.get("Url"));
        topic = ParamsUtil.getString(kafkaProps.get("Topic"));
        groupId = ParamsUtil.getString(kafkaProps.get("GroupId"));

        service = ParamsUtil.getString(kafkaProps.get("Service"));
        method = ParamsUtil.getString(kafkaProps.get("Method"));

        messageFilterString = ParamsUtil.getString(kafkaProps.get("MessageFilterString"));

        isEarliest = ParamsUtil.getString(kafkaProps.get("Earliest"));
        seekToEnd = ParamsUtil.getString(kafkaProps.get("SeekToEnd"));

        comments = ParamsUtil.getString(kafkaProps.get("Comments"));

        durationOfMillis = (Long) kafkaProps.get("DurationOfMillis");
        if (durationOfMillis == null) {
            durationOfMillis = 100L; // по умолчанию устанавливаем задержку в 100 миллисекунд
        }

        //security
        String sslTruststoreLocation = ParamsUtil.getString(kafkaProps.get("SslTruststoreLocation"));
        String sslTruststorePassword = ParamsUtil.getString(kafkaProps.get("SslTruststorePassword"));
        String sslKeystoreLocation = ParamsUtil.getString(kafkaProps.get("SslKeystoreLocation"));
        String sslKeystorePassword = ParamsUtil.getString(kafkaProps.get("SslKeystorePassword"));
        String sslKeyPassword = ParamsUtil.getString(kafkaProps.get("SslKeyPassword"));
        Properties props = new Properties();

        if (ParamsUtil.isNotEmpty(url)) {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        }
        if (ParamsUtil.isNotEmpty(groupId)) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        compressionType = ParamsUtil.getString(kafkaProps.get("CompressionType"));

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        if (ParamsUtil.isNotEmpty(compressionType)) {
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            needDecompress = true;
        } else {
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            needDecompress = false;
        }
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        if (isEarliest != null) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }

        if (ParamsUtil.isNotEmpty(sslTruststoreLocation) &&
                ParamsUtil.isNotEmpty(sslTruststorePassword) &&
                ParamsUtil.isNotEmpty(sslKeystoreLocation) &&
                ParamsUtil.isNotEmpty(sslKeystorePassword) &&
                ParamsUtil.isNotEmpty(sslKeyPassword)
        ) {

            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);

            //p.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.3,TLSv1.2");
            //p.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
            //p.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
            //p.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        }

        try {
            consumer = new KafkaConsumer<String, Object>(props);
            if (ParamsUtil.isNotEmpty(topic)) {

                //подписываемся на топик
                setTopics(Arrays.asList(topic));
                Set<TopicPartition> assignment = new HashSet<>();
                // Получить набор разделов, в настоящее время назначенных этому потребителю
                while (assignment.size() == 0) {
                    consumer.poll(durationOfMillis);
                    assignment = consumer.assignment();
                }
                logger.debug("for url: " + url + "and topic: " + topic + "assignment: " + assignment);
                // если параметр seekToEnd задан, то старые сообщения, полученные при отключенном консьюмере не получаем
                if (seekToEnd != null) {
                    consumer.seekToEnd(assignment);
                }

            }

        } catch (Exception e) {
            logger.error("KafkaConsumer error by " + url + " : ", e);
            isInterrupted.set(true);
        }
        receiverThread = new Thread(getReceiver());
        receiverThread.setName("KafkaThread:" + url);
    }

    /**
     * Установка тем, из которых будет производится чтение
     *
     * @param topics - список тем
     */
    public void setTopics(List<String> topics) {
        consumer.subscribe(topics);
    }

    /**
     * Установка тем и разделов, из которых будет производится чтение
     *
     * @param tp - список тем с разделами
     */
    public void setTopicPartition(List<TopicPartition> tp) {
        consumer.assign(tp);
    }

    /**
     * Установка смещения в начало, либо в конце раздела
     *
     * @param seekType - тип смещения: Beginning (начало), End (конец)
     * @param tp       - список тем с разделами
     */
    public void setOffset(String seekType, List<TopicPartition> tp) {
        if (seekType.equalsIgnoreCase("Beginning"))
            consumer.seekToBeginning(tp);
        if (seekType.equalsIgnoreCase("End"))
            consumer.seekToEnd(tp);
    }

    /**
     * Установка произвольного смещения
     *
     * @param tp     - переменная, содержащая тему с разделом
     * @param offset - смещение, относительно которого будет производится чтение
     */
    public void setOffset(TopicPartition tp, long offset) {
        consumer.seek(tp, offset);
    }

    /**
     * Запускает отдельный поток для чтения сообщений из брокера
     */
    public synchronized void start() {

        if (isInterrupted.get()) {
            throw new IllegalStateException("Method MessageSender::start used after receiver was interrupted");
        }
        logger.debug("MessageSender started successfully");
        receiverThread.start();
    }

    /**
     * Останавливает поток, таким образом завершает чтение данных
     */
    public synchronized void stop() throws InterruptedException {

        if (isInterrupted.get()) {
            throw new IllegalStateException("Method MessageReceiver::stop used after receiver was interrupted");
        }
        isInterrupted.set(true);

        receiverThread.join();
        consumer.close();

        logger.debug("MessageReceiver finished successfully");
    }

    /**
     * Производит конвертацию миллисекунд в указанную дату
     *
     * @param millis - миллисекунды
     */
    private String convertMillInDate(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE,MMMM d,yyyy h:mm,a");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Возвращает ID проверчного запроса
     */
    public String getTestRequestId() {

        logger.debug("testRequestId: " + testRequestId);

        return testRequestId;
    }

    /**
     * Возвращает флаг состояния соединения с брокером
     */
    public Boolean getConnectionState() {

        // перебираем все элементы массива connectionStates, сравнивая друг с другом на равенство
        for (int i = 0; i < сalcFalseConnectionsList.size(); i++) {
            for (int k = 0; k < сalcFalseConnectionsList.size(); k++) {
                if (сalcFalseConnectionsList.get(i) != сalcFalseConnectionsList.get(k)) {
                    return false;
                }
            }
        }

        logger.debug("connectionState: " + true);

        return true;
    }

    /**
     * Метод, производящий чтение из брокера
     */
    private Runnable getReceiver() {
        return () -> {

            while (!isInterrupted.get()) {
                try {

                    ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(durationOfMillis));

                    for (ConsumerRecord<String, Object> record : records) {

                        String message = null;
                        if (needDecompress) {
                            logger.debug("decompress with type " + compressionType);
                            byte[] messageBytes = (byte[]) record.value();
                            if (compressionType.equals("gzip")) {
                                message = decompressFromGzip(messageBytes);
                            }
                        } else {
                            message = ParamsUtil.getString(record.value());
                        }

                        if (message == null || message.equals("")) {
                            throw new Exception("Received message from kafka is empty!");
                        }

                        Map<String, Object> dataFromKafka = new HashMap<>();
                        dataFromKafka.put("key", record.key());
                        dataFromKafka.put("value", message);

                        //headers
                        List<Map<String, Object>> headersList = new ArrayList<>();
                        Map<String, Object> header;
                        for (Header hed : record.headers()) {
                            header = new HashMap<>();
                            if (needDecompress) {
                                if (compressionType.equals("gzip")) {
                                    header.put(hed.key(), decompressFromGzip(hed.value()));
                                }
                            } else {
                                header.put(hed.key(), hed.value());
                            }
                            headersList.add(header);
                        }
                        dataFromKafka.put("headers", headersList);

                        // фильтрация сообщения от брокера по messageFilterString
                        boolean filterApprove = true;
                        // если в строке фильтрации что-то есть, но в сообщении строки фильтрации не найдено, то не пропускаем
                        if (messageFilterString != null && message.indexOf(messageFilterString) == -1) {
                            filterApprove = false;
                        }


                        if (message.indexOf("test request id:") != -1) {
                            testRequestId = message.split(":")[1];
                        }

                        if (filterApprove) {
                            try {
                                for(Map<String, Object> hea : headersList) {
                                    if(hea.get("__TypeId__") != null) {
                                        String type = ParamsUtil.getString(hea.get("__TypeId__"));
                                        if(type.equals(Constants.AZS_CREATEUPDATE_CONTRACT_OPERATION_NAME) ||
                                                type.equals(Constants.AZS_CANCEL_CONTRACT_OPERATION_NAME)  ||
                                                type.equals(Constants.AZS_CREATE_CREDITING_ORDER_OPERATION_NAME) ||
                                                type.equals(Constants.AZS_UPDATE_COLLATERAL_OPERATION_NAME)
                                        ) {
                                            RestServer rest = new RestServer();
                                            rest.mortgageContractKafkaRouter(type, dataFromKafka);
                                        }
                                    }
                                }


                            } catch (Exception e) {
                                logger.error("KafkaConsumer error by " + url + " : ", e);
                            }

                            logger.debug(
                                    String.format("received record: topic = %s, partition = %d, offset = %d, date = %s, key = %s, value = %s, headers = %s",
                                            record.topic(),
                                            record.partition(),
                                            record.offset(),
                                            convertMillInDate(record.timestamp()),
                                            record.key(),
                                            message,
                                            record.headers()
                                    ));

                        }
                    }

                } catch (Exception ex) {

                    logger.debug("exception occurred: " + ex.getMessage());

                    // считаем частоту возникновения ошибок подключения
                    getCalcFalseConnectionsList();
                }
            }
        };
    }

    // считаем частоту возникновения ошибок подключения
    public void getCalcFalseConnectionsList() {

        if (сalcFalseConnectionsList.size() > 10) {
            сalcFalseConnectionsList.remove(0);

            // если после 10 раз обращения к брокеру соединение не было установлено, остановить поток
            /*if(!getConnectionState()){
                stop();
            } */
        } else {
            сalcFalseConnections++;
            сalcFalseConnectionsList.add(сalcFalseConnections);
        }

        if (сalcFalseConnections == 1000) {
            сalcFalseConnections = 0;
        }

        //logger.debug("сalcFalseConnectionsList: " + сalcFalseConnectionsList);
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static String decompressFromGzip(final byte[] compressed) throws IOException {
        final StringBuilder outStr = new StringBuilder();
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        if (isCompressed(compressed)) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }
        } else {
            outStr.append(new String(compressed));
        }
        return outStr.toString();
    }

}
