package ru.diasoft.integration.vtb.service.stub.jms;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;

public class JmsSender {

    private static Logger logger = Logger.getLogger(JmsSender.class);

    private JmsConfig jmsConfig;
    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueSender qsender;
    private Queue queue;
    private TextMessage msg;

    public JmsSender() {
        this.jmsConfig = StubConfig.getJmsConfig();
    }

    public void init(Context ctx, String queueName) throws NamingException, JMSException {
        logger.debug("JmsSender.init() start with params: queueName = " + queueName);

        if (ctx == null) {
            logger.debug("return from JmsSender.init() because Context is null");
            return;
        }
        if (jmsConfig == null || StringUtils.isBlank(jmsConfig.getJndiNameConnectionFactory())) {
            logger.debug("return from JmsSender.init() because jmsConfig is not initialized");
            return;
        }

        qconFactory = (QueueConnectionFactory) ctx.lookup(jmsConfig.getJndiNameConnectionFactory());
        qcon = qconFactory.createQueueConnection();
        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        queue = (Queue) ctx.lookup(queueName);
        qsender = qsession.createSender(queue);
        msg = qsession.createTextMessage();
        qcon.start();

        logger.debug("JmsSender.init() finished");
    }

    public void send(String message) throws JMSException {
        logger.debug("JmsSender.send() try to send message: " + message);
        msg.setText(message);
        qsender.send(msg);
        logger.debug("JmsSender.send() finished");
    }

    public void close() throws JMSException {
        qsender.close();
        qsession.close();
        qcon.close();
        logger.debug("JmsSender.close() finished");
    }
}
