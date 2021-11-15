package ru.diasoft.integration.vtb.service.stub.impl;

import java.util.Map;

import org.apache.log4j.Logger;

import ru.diasoft.utils.SimpleSessionProvider;
import ru.diasoft.utils.XMLUtil;
import ru.diasoft.utils.exception.XMLUtilException;

public class CallbackAsyncTask implements Runnable {

	private static final Logger logger = Logger.getLogger(CallbackAsyncTask.class);
	
	private final String responseData;
	private final String command;
	private final String url;
	private final String sessionId;
	private final long processId;
	
	public CallbackAsyncTask(String url, String command, String responseData, String sessionId, long processId) {
		this.command = command;
		this.responseData = responseData;
		this.url = url;
		this.sessionId = sessionId;
		this.processId = processId;
		
	}

	@Override
	public void run() {
		logger.info("Start callback for async request. Url:" + url + ", Command: "  + command + ", SessionId: " + sessionId + ", ProcessId:" + processId) ;
		logger.debug("Response:\n" + responseData);
		
		try {
			
			XMLUtil xmlUtil = new XMLUtil();			
			xmlUtil.setObjectSessionProvider(new SimpleSessionProvider(sessionId));
			Map<String, Object> resp = xmlUtil.doURLHTTP(url, command, responseData, "UTF-8", null, null, null, processId, false);
			System.out.println("Ответ на вызов DSCALLCALLBACK:" + resp);
		} catch (XMLUtilException e) {
			logger.error("Can`t Send callback", e);
		}
		
	}

}
