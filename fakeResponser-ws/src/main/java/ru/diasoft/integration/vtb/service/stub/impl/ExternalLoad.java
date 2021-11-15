package ru.diasoft.integration.vtb.service.stub.impl;

import org.apache.log4j.Logger;

import ru.diasoft.integration.vtb.service.stub.impl.StubConfig.Command;
import ru.diasoft.integration.vtb.service.stub.impl.StubProcessor.ProcessResult;
import ru.diasoft.utils.SimpleSessionProvider;
import ru.diasoft.utils.XMLUtil;
import ru.diasoft.utils.exception.XMLUtilException;

public class ExternalLoad implements Runnable {

	private final static Logger logger = Logger.getLogger(ExternalLoad.class);
	
	private final int idx;
	private final Command commandConf;
	private final int numRequests;
	private final long delay;

	public ExternalLoad(int idx, Command commandConf, int numRequests, long delay) {
		this.idx = idx;
		this.commandConf = commandConf;
		this.numRequests = numRequests;
		this.delay = delay;		
	}

	@Override
	public void run() {
		logger.info("Start external load for " + idx);
		
		XMLUtil xmlUtil = new XMLUtil();			
		
		int count = 0;
		while (count < numRequests) {
			
			logger.debug("Start send request #"+count);
			
			ProcessResult __process;
			try {
				__process = StubProcessor.__process(commandConf, "", false);
			} catch (Exception e) {
				logger.error("External load for " + idx + " broken");
				return;
			}
			
					
			xmlUtil.setObjectSessionProvider(new SimpleSessionProvider(Integer.toString(count)));
			try {
				xmlUtil.doURLHTTP(StubConfig.getCallbackUrl(), commandConf.getCommand(), __process.getResult(), "UTF-8", null, null, null, (long) idx, false);
			} catch (XMLUtilException e1) {
				logger.error("External load for " + idx + " call failed");				
			}
			
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				logger.info("External load for " + idx + " interupted");
				return;
			}
			count++;
		}
		
		logger.info("End external load for " + idx);
	}

}
