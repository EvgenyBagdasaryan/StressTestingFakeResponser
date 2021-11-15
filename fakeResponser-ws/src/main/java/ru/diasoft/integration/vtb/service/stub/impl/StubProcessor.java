package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import org.w3c.dom.Element;
import ru.diasoft.integration.vtb.utils.Utl;
import ru.diasoft.integration.vtb.service.stub.AdtConfig;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig.Response;
import ru.diasoft.integration.vtb.service.stub.jms.JmsConfig;
import ru.diasoft.integration.vtb.service.stub.jms.JmsSender;
import ru.diasoft.utils.XMLUtil;

public class StubProcessor {

    public static class ProcessResult {
        private String result;
        private Response response;

        public ProcessResult(String result, Response response) {
            this.setResult(result);
            this.response = response;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

    }

    private static final Logger logger = Logger.getLogger(StubProcessor.class);

    private final static ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(AdtConfig.getAsyncThreadPoolSize());

    private static StubConfig.Response getResponseSeq(StubConfig.Command commandConf, String data, boolean isJson) throws Exception {


        int seq = commandConf.getCount().getAndAdd(1);

        List<Response> reponses = filterResponses(commandConf.getReponses(), data, isJson);

        if (reponses.isEmpty()) {
            throw new Exception("Responses is empty");
        }

        int idx = seq % reponses.size();
        logger.debug("Select response by sequence " + idx);

        return reponses.get(idx);
    }


    private static List<Response> filterResponses(List<Response> reponses, String data, boolean isJson) throws Exception {
        data = Utl.changeTags(data);
        logger.debug("filterResponses reponses: " + reponses + ", data = " + data);

        List<Response> result = new ArrayList<StubConfig.Response>();

        if (!isJson) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(data.getBytes(Utl.CHARSET_UTF_8)));
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            int idx = 0;
            for (Response item : reponses) {
                idx++;
                if (StringUtils.isBlank(item.getCondition())) {
                    result.add(item);
                    logger.debug("Condition for response #" + idx + " is Empty. Response added");
                    continue;
                }

                XPathExpression expr = xpath.compile(item.getCondition());
                Boolean evaluate = (Boolean) expr.evaluate(doc, XPathConstants.BOOLEAN);

                logger.debug("Condition for response #" + idx + " is " + item.getCondition() + ". Evulated to " + evaluate);

                if (Boolean.TRUE.equals(evaluate)) {
                    result.add(item);
                    logger.debug("Condition for response #" + idx + " added");
                }
            }
        } else {
            for (Response item : reponses) {
                result.add(item);
                logger.debug("Condition for response added");
            }
        }
        return result;
    }


    public static String processSync(String adapter, String command, String commanddata) throws Exception {
        logger.debug("Start call sync process adapter = " + adapter + ", command = " + command);

        StubConfig.Command commandConf = StubConfig.getCommand(adapter, command, "CALL");
        logger.debug("commandConf = " + commandConf.getCommand());

        ProcessResult response = __process(commandConf, commanddata, false);

        if (response.response.getTimeout() > 0) {
            logger.debug("Wait timeout: " + response.response.getTimeout());
            Thread.sleep(response.response.getTimeout());
        }

        processOperation(response.response, commanddata, 0);

        return response.getResult();
    }

    public static String processSyncJson(String adapter, String command, String commanddata) throws Exception {
        logger.debug("Start call sync process json adapter = " + adapter + ", command = " + command);

        StubConfig.Command commandConf = StubConfig.getCommand(adapter, command, "CALL");
        logger.debug("commandConf = " + commandConf.getCommand());

        ProcessResult response = __process(commandConf, commanddata, true);
        return response.getResult();
    }

    public static ProcessResult __process(StubConfig.Command commandConf, String commanddata, boolean isJson) throws Exception {

        Response response = getResponseSeq(commandConf, commanddata, isJson);

        String responseData = response.getResponseData();

        responseData = generateRandomData(responseData);

        if (!response.getReplaces().isEmpty()) {
            responseData = replaceLabel(commanddata, response.getReplaces(), responseData);
        }

        if (response.getPostProcessor() != null && !response.getPostProcessor().isEmpty()) {
            responseData = postprocess(response.getPostProcessor(), commanddata, responseData);
        }
        return new ProcessResult(responseData, response);
    }

    private static String generateRandomData(String xml) {
        if (StringUtils.isBlank(xml))
            return "";

        String xmlStringValue = "RND_STRING_VALUE";
        String xmlBigDecimalValue = "RND_BIG_DECIMAL_VALUE";
        String xmlLongValue = "RND_LONG_VALUE";
        String xmlIntegerValue = "RND_INTEGER_VALUE";
        String xmlDateValue = "RND_DATE_VALUE";

        // Генерация случайных значений
        Random random = new Random();
        Long longValue = random.nextLong();
        Integer integerValue = random.nextInt(1000000000);
        BigDecimal bigDecimalValue = new BigDecimal(Math.random()).setScale(2, BigDecimal.ROUND_HALF_UP);

        Integer intVal = random.nextInt(1000000000);
        String stringValue = "TEST-" + intVal;

        DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateValue = Utl.getCurrentDateTimeZoneUTC(new Date());
        String dateStringValue = formatter.format(dateValue);

        xml = xml.replaceAll(xmlStringValue, stringValue);
        xml = xml.replaceAll(xmlBigDecimalValue, bigDecimalValue.toString());
        xml = xml.replaceAll(xmlLongValue, longValue.toString());
        xml = xml.replaceAll(xmlIntegerValue, integerValue.toString());
        xml = xml.replaceAll(xmlDateValue, dateStringValue);

        return xml;
    }

    private static String postprocess(String postProcessor, String commanddata, String responseData) throws Exception {

        Class<?> clazz = Class.forName(postProcessor);
        Constructor<?> ctor = clazz.getConstructor();
        PostProcessor pProcessor = (PostProcessor) ctor.newInstance();

        return pProcessor.process(commanddata, responseData);
    }


    private static String replaceLabel(String commanddata, Map<String, Replacer> replaces, String responseData)
            throws Exception {
        logger.debug("Start replace loop");
        String respResult = responseData;

        Replacer.Context context = new Replacer.Context(commanddata);

        for (Entry<String, Replacer> e : replaces.entrySet()) {

            respResult = e.getValue().replace(respResult, context);


        }
        logger.debug("End replace loop");
        return respResult;
    }


    public static void processAsync(String adapter, String command, String commanddata, String sessionId, long processId) throws Exception {
        logger.debug("Start call async process adapter = " + adapter + ", command = " + command);

        StubConfig.Command commandConf = StubConfig.getCommand(adapter, command, "CALLASYNC");

        ProcessResult response = __process(commandConf, commanddata, false);

        JmsConfig jmsConfig = StubConfig.getJmsConfig();
        if (jmsConfig != null && jmsConfig.getUseJMS()) {
            // Кладем сообщение в очередь
            scheduler.schedule(new CallbackAsyncJMSTask(prepareResponseXML(response.response.getCommand(), response.getResult(), null,
                    null, sessionId, processId, "ru"), jmsConfig), response.response.getTimeout(), TimeUnit.MILLISECONDS);
        } else {
            scheduler.schedule(new CallbackAsyncTask(StubConfig.getCallbackUrl(), response.response.getCommand(), response.getResult()
                    , sessionId, processId), response.response.getTimeout(), TimeUnit.MILLISECONDS);
        }

        processOperation(response.response, commanddata, response.response.getTimeout());
    }

    public static String processExternalLoad(String commanddata) {
        try {
            XMLUtil xmlUtil = new XMLUtil();

            Map<String, Object> runData = xmlUtil.parse(commanddata);

            Integer numThreads = (Integer) runData.get("numThreads");
            Integer numRequests = (Integer) runData.get("numRequests");
            Long delay = (Long) runData.get("delay");
            String adapter = (String) runData.get("adapter");
            String command = (String) runData.get("command");

            logger.info("Start external load with param [" + adapter + "," + command + "," + numThreads + "," + numRequests + "," + delay + "]");

            if (numThreads == null || numThreads < 1 || numThreads > 128) {
                throw new IllegalArgumentException("numThreads: " + numThreads);
            }

            if (numRequests == null || numThreads < 1) {
                throw new IllegalArgumentException("numRequests: " + numRequests);
            }

            if (delay == null || delay < 1) {
                throw new IllegalArgumentException("delay: " + delay);
            }

            if (StringUtils.isBlank("adapter")) {
                throw new IllegalArgumentException("adapter is empty");
            }

            if (StringUtils.isBlank("command")) {
                throw new IllegalArgumentException("command is empty");
            }

            StubConfig.Command commandConf = StubConfig.getCommand(adapter, command, "EXTERNAL");

            if (commandConf == null) {
                throw new IllegalArgumentException("Config not found for [" + adapter + "," + command + "]");
            }

            for (int idx = 0; idx < numThreads; idx++) {
                ExternalLoad externalLoad = new ExternalLoad(idx, commandConf, numRequests, delay);

                Thread thread = new Thread(externalLoad);
                thread.setName("ExternalLoad-" + idx);
                thread.setDaemon(true);

                thread.start();
            }

            return "OK";
        } catch (Exception e) {
            logger.error("ProcessExternalLoad " + e.getMessage(), e);
            return e.getMessage();
        }
    }

    private static void processOperation(Response responseOper, String commanddata, long addTimeout) throws Exception {
        if (StringUtils.isBlank(responseOper.getOperation())) {
            return;
        }

        String[] splitOper = responseOper.getOperation().split("\\.");

        if (splitOper.length != 2) {
            logger.warn("Operation format is invalid: " + responseOper.getOperation());
            return;
        }

        logger.info("Start External operation " + responseOper.getOperation());
        StubConfig.Command commandConf = StubConfig.getCommand(splitOper[0], splitOper[1], "EXTERNAL");

        ProcessResult response = __process(commandConf, commanddata, false);

        JmsConfig jmsConfig = StubConfig.getJmsConfig();
        if (jmsConfig != null && jmsConfig.getUseJMS()) {
            // Кладем сообщение в очередь
            scheduler.schedule(new CallbackAsyncJMSTask(prepareResponseXML(commandConf.getCommand(), response.getResult(), null,
                    null, null, null, "ru"), jmsConfig), addTimeout + response.response.getTimeout(), TimeUnit.MILLISECONDS);

        } else {
            scheduler.schedule(new CallbackAsyncTask(StubConfig.getCallbackUrl(), commandConf.getCommand(), response.getResult()
                    , null, 0), addTimeout + response.response.getTimeout(), TimeUnit.MILLISECONDS);
        }

        processOperation(response.response, commanddata, addTimeout + response.response.getTimeout());
    }

    public static void putMessageToQueue(String message, JmsConfig jmsConfig) {
        logger.info("start putMessageToQueue: url " + jmsConfig.getJmsUrl());
        try {
            InitialContext ic = getInitialContext(jmsConfig);
            JmsSender qs = new JmsSender();
            qs.init(ic, StubConfig.getJmsConfig().getJndiNameQueue());
            qs.send(message);
            qs.close();

            logger.info("DSCALLASYNC putMessageToQueue successful");

        } catch (Exception e) {
            logger.error("DSCALLASYNC putMessageToQueue error: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static InitialContext getInitialContext(JmsConfig jmsConfig) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, jmsConfig.getInitialContextFactory());
        env.put(Context.PROVIDER_URL, jmsConfig.getJmsUrl());
        return new InitialContext(env);
    }

    public static String prepareResponseXML(
            String command,
            String content,
            String from,
            String to,
            String sesID,
            Long procID,
            String loc) {

        String result = null;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element dscallcallback = doc.createElement("DSCALLCALLBACK");
            dscallcallback.setAttribute("xmlns", "http://support.diasoft.ru");
            doc.appendChild(dscallcallback);

            Element commandtext = doc.createElement("commandtext");
            commandtext.setTextContent(command);
            dscallcallback.appendChild(commandtext);

            Element commanddata = doc.createElement("commanddata");
            commanddata.setTextContent(content);
            dscallcallback.appendChild(commanddata);

            Element contextdata = doc.createElement("contextdata");
            dscallcallback.appendChild(contextdata);

            Element fromSystem = doc.createElement("fromSystem");
            fromSystem.setTextContent(from);
            contextdata.appendChild(fromSystem);

            Element toSystem = doc.createElement("toSystem");
            toSystem.setTextContent(to);
            contextdata.appendChild(toSystem);

            Element sessionId = doc.createElement("sessionId");
            sessionId.setTextContent(sesID);
            contextdata.appendChild(sessionId);

            Element processId = doc.createElement("processId");
            processId.setTextContent(String.valueOf(procID));
            contextdata.appendChild(processId);

            Element locale = doc.createElement("locale");
            locale.setTextContent(loc);
            contextdata.appendChild(locale);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            result = writer.getBuffer().toString();

        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("prepareResponseXML error: ", e);
        }
        return result;
    }
}
