package ru.diasoft.integration.vtb.service.stub.kafka;

import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.AdtConfig;
import ru.diasoft.services.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefConfig {
    protected static final Logger logger = Logger.getLogger(RefConfig.class);

    private static Map<String, String> httpsCertificates = null;
    private static List<Map<String, Object>> kafkaConsumers = null;
    private static List<Map<String, Object>> kafkaProducers = null;

    private static void initComplexTag(Map<String, String> mapToInit, String topTagName, String listTagName, String keyTagName, String valueTagName) {
        Map<String, Object> map = Config.getConfig(AdtConfig.CONFIG_NAME).getAllParams();
        ru.diasoft.services.config.ConfigMap map1 = (ru.diasoft.services.config.ConfigMap) map.get(topTagName);
        if (map1 != null) {
            Object o = map1.get(listTagName);
            if (o instanceof List) {
                List<Object> list = map1.getList(listTagName);
                if (list != null && !list.isEmpty()) {
                    for (Object item : list) {
                        Map<String, Object> item1 = (Map<String, Object>) item;
                        String b = (String) item1.get(valueTagName);
                        String f = (String) item1.get(keyTagName);
                        mapToInit.put(f, b);
                    }
                }
            } else {
                Map<String, Object> mapInner = map1.getMap(listTagName);
                if (mapInner != null) {
                    String b = (String) mapInner.get(valueTagName);
                    String f = (String) mapInner.get(keyTagName);
                    mapToInit.put(f, b);
                }
            }
        }
    }

    private static void initKafkaTag(List<Map<String, Object>> listToInit, String topTagName, String listTagName) {
        Map<String, Object> map = Config.getConfig(AdtConfig.CONFIG_NAME).getAllParams();
        ru.diasoft.services.config.ConfigMap map1 = (ru.diasoft.services.config.ConfigMap) map.get(topTagName);
        if (map1 != null) {
            Object o = map1.get(listTagName);
            if (o instanceof List) {
                List<Object> list = map1.getList(listTagName);
                if (list != null && !list.isEmpty()) {
                    for (Object item : list) {
                        listToInit.add((Map<String, Object>) item);
                    }
                }
            } else {
                Map<String, Object> mapInner = map1.getMap(listTagName);
                if (mapInner != null) {
                    listToInit.add(mapInner);
                }
            }
        }
    }


    public static Map<String, String> getHttpsCertificates() {
        if (httpsCertificates == null) {
            initHttpsCertificates();
        }
        return httpsCertificates;
    }

    private static synchronized void initHttpsCertificates() {
        if (httpsCertificates == null) {
            httpsCertificates = new HashMap<>();
            initComplexTag(httpsCertificates, "HttpsCertificates", "Certificate", "ID", "Name");
        }
    }

    public static List<Map<String, Object>> getKafkaConsumers() {
        if (kafkaConsumers == null) {
            initKafkaConsumers();
        }
        logger.debug("----------------------------------------------------------started  getKafkaConsumers from fakeResponser");
        logger.debug(kafkaConsumers);
        return kafkaConsumers;
    }

    private static synchronized void initKafkaConsumers() {
        if (kafkaConsumers == null) {
            kafkaConsumers = new ArrayList<>();
            initKafkaTag(kafkaConsumers, "KafkaConsumers", "KafkaConsumer");
        }
    }

    public static List<Map<String, Object>> getKafkaProducers() {
        if (kafkaProducers == null) {
            initKafkaProducers();
        }
        return kafkaProducers;
    }

    private static synchronized void initKafkaProducers() {
        if (kafkaProducers == null) {
            kafkaProducers = new ArrayList<>();
            initKafkaTag(kafkaProducers, "KafkaProducers", "KafkaProducer");
        }
    }
}
