package ru.diasoft.integration.vtb.service.stub;

import java.util.Map;


import ru.diasoft.services.config.Config;

public class AdtConfig {
	public static final String CONFIG_NAME = "fakeResponser";
	
	public static String getFAKE_RESPONSE_FOLDER_PATH() {
		return Config.getConfig(AdtConfig.CONFIG_NAME).getParam("FAKE_RESPONSE_FOLDER_PATH", ".");
	}
	
	public static Map<String, Object> getAllParams() {
		Config.getConfig(AdtConfig.CONFIG_NAME).reload();
		return Config.getConfig(AdtConfig.CONFIG_NAME).getAllParams();
	}
	
	public static int getAsyncThreadPoolSize() {
		String val = Config.getConfig(AdtConfig.CONFIG_NAME).getParam("AsyncThreadPoolSize", "");
		int res;
		try {
			res = Integer.valueOf(val);
		} catch (NumberFormatException e) {			
			res = 4;
		}
		return res;
	}

}
