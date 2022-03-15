package ru.diasoft.integration.vtb.service.stub.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.kafka.KafkaConsumersController;
import ru.diasoft.integration.vtb.utils.Utl;
import ru.diasoft.integration.vtb.service.stub.flexadt.*;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;

@javax.jws.WebService(endpointInterface = "ru.diasoft.integration.vtb.service.stub.flexadt.WSPORTTYPE", 
	wsdlLocation = "WEB-INF/wsdl/WS.wsdl", 
	portName = "WSPORT", serviceName = "ulbsws", 
	targetNamespace = "http://support.diasoft.ru")
public class UlbsStub implements WSPORTTYPE {

	private static final Logger logger = Logger.getLogger(UlbsStub.class);
	
	@Resource
    private WebServiceContext context;
	
	@Override
	public String dscall(String commandtext, String commanddata, ContextData contextdata) throws DSCALLFAULT_Exception {
		
		String uri = (String) context.getMessageContext().get("com.sun.xml.ws.transport.http.servlet.requestURL");
		
		logger.info("call " + uri + " with command: " + commandtext + 
				", sessionId: " + contextdata.getSessionId() + 
				", processId: " + contextdata.getProcessId());
		logger.debug("Request:\n" + commanddata);
		
		if ("$$resetcache".equalsIgnoreCase(commandtext)) {
			StubConfig.clear();
			return "reset cache success";
		} else if ("$$externalload".equalsIgnoreCase(commandtext)) {
			return StubProcessor.processExternalLoad(commanddata);
		}

		try {
			
			String adapterName = extracted(uri);
			String response = StubProcessor.processSync(adapterName, commandtext, commanddata);
			
			logger.debug("Response:\n" + response);
			return response;
		} catch (Exception e) {
			logger.error("DSCALL ERROR", e);
			return e.getMessage();
		} finally {
			logger.info("call " + uri + " with command: " + commandtext + 
				", sessionId: " + contextdata.getSessionId() + 
				", processId: " + contextdata.getProcessId() + " finished");
		}	
	}


	private String extracted(String uri) {
		String[] splitUri = uri.split("\\?")[0].split("/");
		
		return splitUri[splitUri.length - 1];
	}
	
	
	@Override
	public void dscallasync(String commandtext, String commanddata, ContextData contextdata) {
		String uri = (String) context.getMessageContext().get("com.sun.xml.ws.transport.http.servlet.requestURL");
		
		logger.info("callasync " + uri + " with command: " + commandtext + 
				", sessionId: " + contextdata.getSessionId() + 
				", processId: " + contextdata.getProcessId());
		logger.debug("Request:\n" + commanddata);
		
		try {
			
			String adapterName = extracted(uri);
			StubProcessor.processAsync(adapterName, commandtext, commanddata, contextdata.getSessionId(), contextdata.getProcessId());
		} catch (Exception e) {
			logger.error("DSCALLASYNC ERROR", e);
		} finally {
			logger.info("call " + uri + " with command: " + commandtext + 
				", sessionId: " + contextdata.getSessionId() + 
				", processId: " + contextdata.getProcessId() + " finished");
		}
	}

	@Override
	public String dscallcallback(String commandtext, String commanddata, ContextData contextdata)
			throws DSCALLCALLBACKFault {		
		return null;
	}

	@Override
	public Response request(Request request) {
		logger.info("Call UlbsStub Request operation");

		Response response = new Response();
		String command = "unknown";
		String uri = "";
		try {
			uri = (String) context
					.getMessageContext()
					.get("com.sun.xml.ws.transport.http.servlet.requestURL");

			String xml = BaseStub.objectToXml(request);
			xml = Utl.cutBlockCDATA(xml);

			logger.debug("xml request: " + xml);

			if (!StringUtils.isBlank(xml) && xml.contains("RelayMessageOneWay")) {
				command = "RelayMessageOneWay";
				logger.info("call RelayMessageOneWay by url " + uri);

				String adapterName = extracted(uri);
				StubProcessor.processAsync(adapterName, command, xml, "1", 11L);
				logger.debug("xml resp: " + xml);

			} else if (!StringUtils.isBlank(xml)) {
				command = "RelayMessage";
				logger.info("call relayMessage by url " + uri);

				logger.debug("xml Request: " + xml);
				String adapterName = extracted(uri);
				String resp = StubProcessor.processSync(adapterName, command, xml);
				response.setAny(resp);
				logger.debug("xml Response:\n" + resp);
			}

		} catch (Exception e) {
			logger.error("DSCALL ERROR", e);

		} finally {
			logger.info("call " + uri + " with command: " + command + " finished");
		}
		return response;
	}

	@Override
	public DSMortgageMiniAppGetListLKKAttrByIDRes dsMortgageMiniAppGetListLKKAttrByIDReq(DSMortgageMiniAppGetListLKKAttrByIDReq request) {
		logger.info("Call UlbsStub dsMortgageMiniAppGetListLKKAttrByIDReq operation");

		DSMortgageMiniAppGetListLKKAttrByIDRes response = new DSMortgageMiniAppGetListLKKAttrByIDRes();
		String command = "unknown";
		String uri = "";
		try {
			uri = (String) context
					.getMessageContext()
					.get("com.sun.xml.ws.transport.http.servlet.requestURL");

			String xml = BaseStub.objectToXml(request);
			xml = Utl.cutBlockCDATA(xml);

			logger.debug("xml request: " + xml);

			command = "DSMortgageMiniAppGetListLKKAttrByIDReq";
			logger.info("call DSMortgageMiniAppGetListLKKAttrByIDReq by url " + uri);

			String adapterName = extracted(uri);
			String resp = StubProcessor.processSync(adapterName, command, xml);
			response.setAny(resp);
			logger.debug("xml Response:\n" + resp);

		} catch (Exception e) {
			logger.error("SYNC CALL ERROR", e);

		} finally {
			logger.info("call " + uri + " with command: " + command + " finished");
		}
		return response;
	}

	public WebServiceContext getContext() {
		return context;
	}


	public void setContext(WebServiceContext context) {
		this.context = context;
		try {
		    logger.info("setContext in fakeResponser");
			KafkaConsumersController.AllConsumersInConfigStart();

		} catch (Exception e) {
			logger.info("contextInitialized error: " + e.getMessage());
		}
	}

	@PreDestroy
	void preDestroy(){
		System.out.println("Config destroyed");
		StubConfig.clear();
		try {
			KafkaConsumersController.AllConsumersInConfigStop();

		} catch (Exception e) {
			logger.info("contextDestroyed error: " + e.getMessage());
		}
	}

}
