package ru.diasoft.integration.vtb.service.stub.kafka;

import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.AdtConfig;
import ru.diasoft.integration.vtb.service.stub.Constants;
import ru.diasoft.integration.vtb.utils.DataConvertUtil;
import ru.diasoft.integration.vtb.utils.ParamsUtil;
import ru.diasoft.utils.XMLUtil;

import java.util.*;

public class KafkaConsumersController {

    protected static final Logger logger = Logger.getLogger(KafkaConsumersController.class);
    private static List<Map<String, Object>> kafkaConsumersProps = RefConfig.getKafkaConsumers();
    public static List<MessageReceiver> messageReceiverList = new ArrayList<>();

    public static Map<String, Object> ConsumerStop(Map<String, Object> consumer) {

        Map<String, Object> result = new HashMap<>();
        result.put(XMLUtil.RETURN_CODE, Constants.OK_CODE);
        result.put(XMLUtil.SR_STATUS, Constants.STATUS_OK);
        try {
            // получаем наименование Url и Topic для консюмера, который хотим стопнуть
            String kafkaUrlJson = ParamsUtil.getString(consumer.get("Url"));
            String kafkaTopicJson = ParamsUtil.getString(consumer.get("Topic"));
            String kafkaСommentsJson = ParamsUtil.getString(consumer.get("Comments"));

            // удаляем все ненужные прерванные потоки
            RemoveIntereptedThreadsFromPull();

            Boolean consumerFound = false;

            // стопаем поток
            MessageReceiver receiverWillStop = FindConsumerInThreadsPull(kafkaUrlJson, kafkaTopicJson);
            if(receiverWillStop != null){
                logger.debug("KafkaConsumersController: try to stop " + kafkaUrlJson + " kafka consumer for topic" + kafkaTopicJson);
                receiverWillStop.stop();
                consumerFound = true;
            }

            // проверка запущен ли уже поток
            if(ParamsUtil.isNotEmpty(kafkaСommentsJson) &&
                    ParamsUtil.isNotEmpty(kafkaUrlJson) &&
                    ParamsUtil.isNotEmpty(kafkaTopicJson) &&
                    !consumerFound){
                String msg = "Консьюмер " + kafkaСommentsJson + " с Url: " + kafkaUrlJson + " и topic: " + kafkaTopicJson + " не запущен";
                logger.debug(msg);
                result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
                result.put(XMLUtil.RETURN_MSG, msg);
            }

        } catch (Exception e) {
            String msg = "KafkaConsumersController stop error: " + e.getMessage();
            logger.error(msg);
            result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
            result.put(XMLUtil.RETURN_MSG, msg);
        }

        return result;
    }

    public static Map<String, Object> ConsumerStart(Map<String, Object> consumer) {

        Map<String, Object> result = new HashMap<>();
        result.put(XMLUtil.RETURN_CODE, Constants.OK_CODE);
        result.put(XMLUtil.SR_STATUS, Constants.STATUS_OK);
        try {

            // получаем наименование Url и Topic для кафки, от которую стартуем
            String kafkaUrlJson = ParamsUtil.getString(consumer.get("Url"));
            String kafkaTopicJson = ParamsUtil.getString(consumer.get("Topic"));
            String isEarliestJson = ParamsUtil.getString(consumer.get("Earliest"));
            String seekToEndJson = ParamsUtil.getString(consumer.get("SeekToEnd"));
            Long durationOfMillisJson = ParamsUtil.getLong("DurationOfMillis", consumer);

            // если есть объект-поток, который совпадает по Url и Topic, имеет признак isInterrupted == false, т.е. запущен, прекращаем запуск дубликата
            MessageReceiver receiverNotDouble = FindConsumerInThreadsPull(kafkaUrlJson, kafkaTopicJson);
            if(receiverNotDouble != null){
                String msg = "Сonsumer с Url: " + kafkaUrlJson + " и topic: " + kafkaTopicJson + " уже запущен";
                logger.debug(msg);

                result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
                result.put(XMLUtil.RETURN_MSG, msg);
                return result;
            }

            // перебираем все настройки конфига кафки
            // если находим что конфиг совпадает по Url и Topic с заданной в Json то поднимаем ее
            Map<String, Object> findConsumerProps = FindConsumerInConvertorConfig(kafkaUrlJson, kafkaTopicJson);
            if(findConsumerProps != null){

                // если в Json заданы параметры "Earliest", "DurationOfMillis" и "SeekToEnd" то добавляем их к конфигу кафки
                if(isEarliestJson != null){
                    findConsumerProps.put("Earliest", isEarliestJson);
                }
                if(durationOfMillisJson != null){
                    findConsumerProps.put("DurationOfMillis", durationOfMillisJson);
                }
                if(seekToEndJson != null){
                    findConsumerProps.put("SeekToEnd", seekToEndJson);
                }

                MessageReceiver newMessageReceiver = new MessageReceiver(findConsumerProps);

                logger.debug("KafkaConsumersController: try to start " + kafkaUrlJson + " kafka consumer");
                messageReceiverList.add(newMessageReceiver);

                newMessageReceiver.start();
            }

            return result;

        } catch (Exception e) {
            String msg = "KafkaConsumersController start error: " + e.getMessage();
            logger.error(msg);

            result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
            result.put(XMLUtil.RETURN_MSG, msg);
        }

        return result;
    }

    public static Map<String, Object> ConsumerStartNew(Map<String, Object> consumer){

        Map<String, Object> result = new HashMap<>();
        result.put(XMLUtil.RETURN_CODE, Constants.OK_CODE);
        result.put(XMLUtil.SR_STATUS, Constants.STATUS_OK);
        try {

            // получаем параметры для кафки, от которую стартуем
            String kafkaUrlJson = ParamsUtil.getString(consumer.get("Url"));
            String kafkaTopicJson = ParamsUtil.getString(consumer.get("Topic"));
            String kafkaGroupId = ParamsUtil.getString(consumer.get("GroupId"));
            String kafkaService = ParamsUtil.getString(consumer.get("Service"));
            String kafkaMethod = ParamsUtil.getString(consumer.get("Method"));
            String kafkaMessageFilterString = ParamsUtil.getString(consumer.get("MessageFilterString"));
            String isEarliestJson = ParamsUtil.getString(consumer.get("Earliest"));
            String seekToEndJson = ParamsUtil.getString(consumer.get("SeekToEnd"));
            Long durationOfMillisJson = ParamsUtil.getLong("DurationOfMillis", consumer);
            String sslTruststoreLocation = ParamsUtil.getString(consumer.get("SslTruststoreLocation"));
            String sslTruststorePassword = ParamsUtil.getString(consumer.get("SslTruststorePassword"));
            String sslKeystoreLocation = ParamsUtil.getString(consumer.get("SslKeystoreLocation"));
            String sslKeystorePassword = ParamsUtil.getString(consumer.get("SslKeystorePassword"));
            String sslKeyPassword = ParamsUtil.getString(consumer.get("SslKeyPassword"));

            // если есть объект-поток, который совпадает по Url и Topic, имеет признак isInterrupted == false, т.е. запущен, прекращаем запуск дубликата
            MessageReceiver receiverNotDouble = FindConsumerInThreadsPull(kafkaUrlJson, kafkaTopicJson);
            if(receiverNotDouble != null){
                String msg = "Сonsumer с Url: " + kafkaUrlJson + " и topic: " + kafkaTopicJson + " уже запущен";
                logger.debug(msg);

                result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
                result.put(XMLUtil.RETURN_MSG, msg);
                return result;
            }

            // формируем набор параметров, отвечающих за запуск консьюмера
            Map<String, Object> kafkaConsumerProp = new HashMap<>();
            if(ParamsUtil.isNotEmpty(kafkaUrlJson) &&
                    ParamsUtil.isNotEmpty(kafkaTopicJson) &&
                    ParamsUtil.isNotEmpty(kafkaService) &&
                    ParamsUtil.isNotEmpty(kafkaMethod)){
                kafkaConsumerProp.put("Url", kafkaUrlJson);
                kafkaConsumerProp.put("Topic", kafkaTopicJson);
                kafkaConsumerProp.put("Service", kafkaService);
                kafkaConsumerProp.put("Method", kafkaMethod);

                if(ParamsUtil.isNotEmpty(kafkaGroupId)){
                    kafkaConsumerProp.put("GroupId", kafkaGroupId);
                }
                if(ParamsUtil.isNotEmpty(kafkaMessageFilterString)){
                    kafkaConsumerProp.put("MessageFilterString", kafkaMessageFilterString);
                }
                if(ParamsUtil.isNotEmpty(kafkaGroupId)){
                    kafkaConsumerProp.put("GroupId", kafkaGroupId);
                }
                if(ParamsUtil.isNotEmpty(isEarliestJson)){
                    kafkaConsumerProp.put("Earliest", isEarliestJson);
                }
                if(ParamsUtil.isNotEmpty(seekToEndJson)){
                    kafkaConsumerProp.put("SeekToEnd", seekToEndJson);
                }
                if(durationOfMillisJson != null){
                    kafkaConsumerProp.put("DurationOfMillis", durationOfMillisJson);
                }

                if(ParamsUtil.isNotEmpty(sslTruststoreLocation) &&
                        ParamsUtil.isNotEmpty(sslTruststorePassword) &&
                        ParamsUtil.isNotEmpty(sslKeystoreLocation) &&
                        ParamsUtil.isNotEmpty(sslKeystorePassword) &&
                        ParamsUtil.isNotEmpty(sslKeyPassword)
                ){
                    kafkaConsumerProp.put("SslTruststoreLocation", sslTruststoreLocation);
                    kafkaConsumerProp.put("SslTruststorePassword", sslTruststorePassword);
                    kafkaConsumerProp.put("SslKeystoreLocation", sslKeystoreLocation);
                    kafkaConsumerProp.put("SslKeystorePassword", sslKeystorePassword);
                    kafkaConsumerProp.put("SslKeyPassword", sslKeyPassword);
                }
            }
            else{
                String msg = "KafkaConsumersController: Отсутствуют необходимые параметры: Url, Topic, Service, Method";

                logger.debug(msg);

                result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
                result.put(XMLUtil.RETURN_MSG, msg);
                return result;
            }

            MessageReceiver newMessageReceiver = new MessageReceiver(kafkaConsumerProp);

            logger.debug("KafkaConsumersController: try to start " + kafkaUrlJson + " kafka consumer on topic " + kafkaTopicJson);
            messageReceiverList.add(newMessageReceiver);

            newMessageReceiver.start();

            String msg = DataConvertUtil.mapToJson(kafkaConsumerProp);

            logger.debug(msg);

            result.put(XMLUtil.RETURN_MSG, msg);
            return result;

        } catch (Exception e) {
            String msg = "KafkaConsumersController startNew error: " + e.getMessage();
            logger.error(msg);

            result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
            result.put(XMLUtil.RETURN_MSG, msg);
        }

        return result;
    }

    public static List<Map<String, Object>> ConsumerGetAll(){

        List<Map<String, Object>> allConsumersList = new ArrayList<>();

        try {
            if(messageReceiverList != null){
                // перебираем все объекты-потоки отвечающие за соединения с кафкой
                for (MessageReceiver tmpMessageReceiver : messageReceiverList) {

                    Map<String, Object> mapMessageReceiver = new HashMap<>();
                    mapMessageReceiver.put("Url", tmpMessageReceiver.url);
                    mapMessageReceiver.put("Topic", tmpMessageReceiver.topic);
                    mapMessageReceiver.put("Service", tmpMessageReceiver.service);
                    mapMessageReceiver.put("Method", tmpMessageReceiver.method);
                    mapMessageReceiver.put("MessageFilterString", tmpMessageReceiver.messageFilterString);
                    mapMessageReceiver.put("Earliest", tmpMessageReceiver.isEarliest);
                    mapMessageReceiver.put("SeekToEnd", tmpMessageReceiver.seekToEnd);
                    mapMessageReceiver.put("DurationOfMillis", tmpMessageReceiver.durationOfMillis);
                    mapMessageReceiver.put("Interrupted", tmpMessageReceiver.isInterrupted);

                    allConsumersList.add(mapMessageReceiver);
                }
            }

            logger.debug("kafka getAll ConsumersList: " + allConsumersList);

        } catch (Exception e) {
            String msg = "dsKafkaController getAll error: " + e.getMessage();
            logger.error(msg);
        }

        return allConsumersList;
    }

    public static Map<String, Object> ConsumerGet(Map<String, Object> consumer){

        Map<String, Object> result = new HashMap<>();
        result.put(XMLUtil.RETURN_CODE, Constants.OK_CODE);
        result.put(XMLUtil.SR_STATUS, Constants.STATUS_OK);
        // получаем из запроса наименование Url и Topic для консьюмера кафки, которого ищем
        String urlKafkaConsumer = ParamsUtil.getString(consumer.get("Url"));
        String topicKafkaConsumer = ParamsUtil.getString(consumer.get("Topic"));
        String commentsKafkaConsumer = ParamsUtil.getString(consumer.get("Comments"));

        try {

            MessageReceiver receiverGet = FindConsumerInThreadsPull(urlKafkaConsumer, topicKafkaConsumer);
            if(receiverGet != null){
                result.put("Comments", commentsKafkaConsumer);
                result.put("Url", receiverGet.url);
                result.put("Topic", receiverGet.topic);
                result.put("Service", receiverGet.service);
                result.put("Method", receiverGet.method);
                result.put("MessageFilterString", receiverGet.messageFilterString);
                result.put("Earliest", receiverGet.isEarliest);
                result.put("SeekToEnd", receiverGet.seekToEnd);
                result.put("DurationOfMillis", receiverGet.durationOfMillis);
                result.put("Interrupted", receiverGet.isInterrupted.get()?true:false);
                result.put("connectionState", receiverGet.getConnectionState());

            }

        } catch (Exception e) {
            String msg = "KafkaConsumersController get error: " + e.getMessage();
            logger.error(msg);

            result.put(XMLUtil.RETURN_CODE, Constants.ERR_CODE);
            result.put(XMLUtil.RETURN_MSG, msg);
        }

        String msg = "KafkaConsumersController get, для consumer: " + commentsKafkaConsumer + " ничего не найдено";
        result.put(XMLUtil.RETURN_MSG, msg);

        return result;
    }

    public static MessageSender ProducerCheckConnection(Map<String, Object> consumer, String id) throws Exception {

        // получаем из запроса наименование Url и Topic для консьюмера кафки, которого ищем
        String urlKafkaConsumer = ParamsUtil.getString(consumer.get("Url"));
        String topicKafkaConsumer = ParamsUtil.getString(consumer.get("Topic"));

        Map<String, Object> findConsumerProps = FindConsumerInConvertorConfig(urlKafkaConsumer, topicKafkaConsumer);
        if(findConsumerProps != null){
            MessageSender sender  = new MessageSender(findConsumerProps);
            sender.sendWithDefaultKey(id,"test request id:"+ id);
            return sender;
        }
        return null;
    }

    public static Map<String, Object> FindConsumerInConvertorConfig(String url, String topic) throws Exception {

        // перебираем все настройки конфига кафки
        if(kafkaConsumersProps != null){
            for (Map<String, Object> kafkaConsumerProp : kafkaConsumersProps) {

                // получаем из конфига конвертора кафки наименование Url и Topic для кафки, которую стартуем
                String urlKafkaConsumerProp = ParamsUtil.getString(kafkaConsumerProp.get("Url"));
                String topicKafkaConsumerProp = ParamsUtil.getString(kafkaConsumerProp.get("Topic"));

                // если находим что конфиг совпадает по Url и Topic с заданной в Json то поднимаем ее
                if(urlKafkaConsumerProp != null &&
                        topicKafkaConsumerProp != null &&
                        ParamsUtil.isNotEmpty(url) &&
                        ParamsUtil.isNotEmpty(topic) &&
                        urlKafkaConsumerProp.equals(url)  &&
                        topicKafkaConsumerProp.equals(topic)){

                    return kafkaConsumerProp;
                }
            }
        }
        return null;
    }

    public static Map<String, Object> FindConsumerInConvertorConfigByComments(String comments) throws Exception {

        // перебираем все настройки конфига кафки
        if(kafkaConsumersProps != null) {
            for (Map<String, Object> kafkaConsumerProp : kafkaConsumersProps) {
                // получаем из конфига конвертора кафки наименование Comments, Url, Topic для кафки, которую стартуем
                String commentsKafkaConsumerProp = ParamsUtil.getString(kafkaConsumerProp.get("Comments"));

                // находим наш консьюмер через "Comments" в UpperCase
                if (commentsKafkaConsumerProp != null && (commentsKafkaConsumerProp.toUpperCase()).contains(comments.toUpperCase())) {
                    return kafkaConsumerProp;
                }
            }
        }

        return null;
    }

    public static MessageReceiver FindConsumerInThreadsPull(String url, String topic) throws Exception {

        if(messageReceiverList != null){

            // перебираем все объекты-потоки отвечающие за соединения с кафкой
            for (MessageReceiver tmpMessageReceiver : messageReceiverList) {

                // если есть объект-поток, который совпадает по Url и Topic, имеет признак isInterrupted == false, т.е. запущен, выдаем по нему результат
                if(ParamsUtil.isNotEmpty(url) &&
                        ParamsUtil.isNotEmpty(topic) &&
                        tmpMessageReceiver.url.equals(url) &&
                        tmpMessageReceiver.topic.equals(topic) &&
                        !tmpMessageReceiver.isInterrupted.get()){

                    return tmpMessageReceiver;
                }
            }
        }
        return null;
    }

    public static void RemoveIntereptedThreadsFromPull(){

        // перебираем все объекты-потоки отвечающие за соединения с кафкой и удаляем объекты с признаком isInterrupted (те что были уже остановлены)
        Iterator<MessageReceiver> messageReceiverIterator = messageReceiverList.iterator();//создаем итератор
        while(messageReceiverIterator.hasNext()) { //до тех пор, пока в списке есть элементы

            MessageReceiver nextMessageReceiver = messageReceiverIterator.next();//получаем следующий элемент
            if (nextMessageReceiver.isInterrupted.get()) {
                messageReceiverIterator.remove(); //удаляем
            }
        }
    }



    public static void AllConsumersInConfigStart() throws Exception {
        logger.debug("----------------------------------------------------------started  AllConsumersInConfigStart from fakeResponser");
        List<Map<String, Object>> kafkaConsumersProps = RefConfig.getKafkaConsumers();
        if(kafkaConsumersProps != null){
            // перебираем все консюмеры в конфиге конвертора
            for (Map<String, Object> kafkaConsumerProp : kafkaConsumersProps) {

                Boolean start = true;

                String commentsKafkaConsumerProp = ParamsUtil.getString(kafkaConsumerProp.get("Comments"));
                String urlKafkaConsumerProp = ParamsUtil.getString(kafkaConsumerProp.get("Url"));
                // стартуем консюмер
                if(start){
                    MessageReceiver tmpMessageReceiver = new MessageReceiver(kafkaConsumerProp);
                    KafkaConsumersController.messageReceiverList.add(tmpMessageReceiver);
                    tmpMessageReceiver.start();
                }
            }
            logger.debug("----------------------------------------------------------started from fakeResponser" + KafkaConsumersController.messageReceiverList.size() + " kafka consumer");
        }
    }
    public static void AllConsumersInConfigStop() throws Exception, InterruptedException  {

        for (MessageReceiver messageReceiver : KafkaConsumersController.messageReceiverList) {
            if(!messageReceiver.isInterrupted.get()){
                messageReceiver.stop();
            }
        }
        logger.debug("----------------------------------------------------------stoped " + KafkaConsumersController.messageReceiverList.size() + " kafka consumer");
    }
}
