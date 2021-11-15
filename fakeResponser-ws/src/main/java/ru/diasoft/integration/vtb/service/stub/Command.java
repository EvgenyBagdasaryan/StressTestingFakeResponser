package ru.diasoft.integration.vtb.service.stub;

import java.util.Map;

public interface Command {

	public Map<String, Object> invoke(String commandtext, Map<String, Object> params) throws Exception;

}
