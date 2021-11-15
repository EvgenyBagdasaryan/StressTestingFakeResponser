/*
 * Copyright (c) Diasoft 2004-2009
 */
package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.conf.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ru.diasoft.integration.vtb.service.stub.FileProcessor;
import ru.diasoft.integration.vtb.service.stub.flexadt.*;
import ru.diasoft.utils.XMLUtil;

@javax.jws.WebService(endpointInterface = "ru.diasoft.integration.vtb.service.stub.flexadt.WSPORTTYPE", wsdlLocation = "WEB-INF/wsdl/WS.wsdl", portName = "WSPORT", serviceName = "vtbadtws", targetNamespace = "http://support.diasoft.ru")

public class FlexAdtStub implements WSPORTTYPE{

	public static final String WS_NAME = "vtb24adtws";

	private static Logger logger = Logger.getLogger(FlexAdtStub.class);

	private static Configuration config;

	 private XMLUtil util = new XMLUtil(false, true);


	@Override
	public String dscall(String commandtext, String commanddata, ContextData contextdata) throws DSCALLFAULT_Exception {
		String retMsg = FileProcessor.getINSTANCE().getFakeResponseObjectByOperation(commandtext);
		if(StringUtils.isBlank(retMsg)){
			//основная логика
			Map<String, Object> retMap = new HashMap<String, Object>();
			retMap.put("Status", "OK");
			retMap.put("ReturnCode", 1001L);
			retMap.put("ReturnMsg", "No fake response found");
			String retString = "";
			try {
				retString = util.createXML(retMap);
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
			}
			return retString;
		}
		else{
			return retMsg;
		}
	}

	@Override
	public void dscallasync(String commandtext, String commanddata, ContextData contextdata) {
		logger.info("Call dscallasync operation");
		
	}

	@Override
	public String dscallcallback(String commandtext, String commanddata, ContextData contextdata)
			throws DSCALLCALLBACKFault {
		logger.info("Call dscallcallback operation");

		return "ОК";
	}

	@Override
	public Response request(Request request) {
		logger.info("Call FlexAdtStub Request operation");

		Response response = new Response();
		String retMsg = FileProcessor
				.getINSTANCE()
				.getFakeResponseObjectByOperation("RelayMessage");

		if(StringUtils.isBlank(retMsg)){
			//основная логика
			Map<String, Object> retMap = new HashMap<>();
			retMap.put("Status", "OK");
			retMap.put("ReturnMsg", "No fake response found");
			retMap.put("ReturnCode", 1001L);
			String retString = "";
			try {
				retString = util.createXML(retMap);
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
			}
			return response;
		} else{
			response.setAny(retMsg);
			return response;
		}
	}

	@Override
	public DSMortgageMiniAppGetListLKKAttrByIDRes dsMortgageMiniAppGetListLKKAttrByIDReq(DSMortgageMiniAppGetListLKKAttrByIDReq request) {
		logger.info("Call dsMortgageMiniAppGetListLKKAttrByIDReq operation");
		return new DSMortgageMiniAppGetListLKKAttrByIDRes();
	}
}