package ru.diasoft.integration.vtb.service.stub.rest;

import com.sun.jersey.spi.container.ResourceFilters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;
import ru.diasoft.integration.vtb.service.stub.impl.StubProcessor;
import ru.diasoft.integration.vtb.utils.DataConvertUtil;
import ru.diasoft.integration.vtb.utils.MyFileUtils;
import ru.diasoft.integration.vtb.utils.ParamsUtil;
import ru.diasoft.integration.vtb.utils.Utl;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URLEncoder;
import java.util.*;

class PFRNotificationThread extends Thread {

    private String id;

    PFRNotificationThread(String idNew){
        id = idNew;
    }

    @Override
    public void run() {
        try {

            Map<String, Object> kafkaConfig = StubConfig.getKafkaConfig();

            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.valueOf(kafkaConfig.get("Url")));

            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, String.valueOf(kafkaConfig.get("SslTruststoreLocation")));
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,  String.valueOf(kafkaConfig.get("SslTruststorePassword")));

            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, String.valueOf(kafkaConfig.get("SslKeystoreLocation")));
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, String.valueOf(kafkaConfig.get("SslKeystorePassword")));
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, String.valueOf(kafkaConfig.get("SslKeyPassword")));

            //props.put(ProducerConfig.ACKS_CONFIG, "all");
            //props.put(ProducerConfig.RETRIES_CONFIG, 0);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

            Header headerm = new RecordHeader("keyHeader", "from fakeResponser with love".getBytes());
            List<Header> headerma = Arrays.asList(headerm);

            //String id = UUID.randomUUID().toString();

            final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
            ProducerRecord<String, String> records = new ProducerRecord(String.valueOf(kafkaConfig.get("Topic")), 0, id,"{\"id\":\""+ id + "\",\"status\":\"SUCCESS\",\"description\":\"Ответ предоставлен. По вашему запросу сформировано уведомление о наличии права владельца сертификата МСК на распоряжение средствами (частью средств) материнского (семейного) капитала\",\"active\":\"false\",\"availableAmount\":1000000}", headerma);

            Thread.sleep(3000);
            producer.send(records);

            producer.flush();
            producer.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
