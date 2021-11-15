package ru.diasoft.integration.vtb.service.stub.impl;

import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.jms.JmsConfig;

public class CallbackAsyncJMSTask implements Runnable {

    private static final Logger logger = Logger.getLogger(CallbackAsyncTask.class);

    private final String responseData;
    private final JmsConfig jmsConfig;

    public CallbackAsyncJMSTask(String responseData, JmsConfig jmsConfig) {
        this.responseData = responseData;
        this.jmsConfig = jmsConfig;
    }

    @Override
    public void run() {
        logger.info("Start JMS callback for async request");
        logger.debug("Response:\n" + responseData);
        try {
            StubProcessor.putMessageToQueue(this.responseData, this.jmsConfig);

        } catch (Exception e) {
            logger.error("Can`t Send JMS callback", e);
        }
    }
}
