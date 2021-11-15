package ru.diasoft.integration.vtb.service.stub;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.log4j.Logger;

public class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {
	
	private static XmlFormatter xmlFormatter = new XmlFormatter();
	
	private static Logger logger = Logger.getLogger(SOAPLoggingHandler.class);
	
    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext context) {
    	
        Boolean isResponse = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        SOAPMessage message = context.getMessage();
        
        try {          
            ByteArrayOutputStream outputStr = new ByteArrayOutputStream();
			message.writeTo(outputStr);
			
			String soupStr = new String (outputStr.toByteArray());
			
			if (isResponse){
				logger.debug("Webservice response: " + xmlFormatter.format(soupStr));				
			} else {
				logger.debug("Webservice obtain request: " + xmlFormatter.format(soupStr));
			}
			
        } catch (Exception e) {
        	logger.error("Error log soap message", e);
        }
        return true;
    }

    public boolean handleFault(SOAPMessageContext smc) {
        return true;
    }

    // nothing to clean up
    public void close(MessageContext messageContext) {
    }

}