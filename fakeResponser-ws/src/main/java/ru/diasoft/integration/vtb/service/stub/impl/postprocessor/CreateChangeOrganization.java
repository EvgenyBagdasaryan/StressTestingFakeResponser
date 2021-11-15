package ru.diasoft.integration.vtb.service.stub.impl.postprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.diasoft.integration.vtb.service.stub.impl.PostProcessor;
import ru.diasoft.utils.XMLUtil;

public class CreateChangeOrganization implements PostProcessor {
	
	@Override
	public String process(String commandData, String responseData) throws Exception {
		XMLUtil xmlUtl = new XMLUtil();
		
		
		Map<String, Object> request = xmlUtl.xmlParse(commandData);		
		
		Map<String, Object> response = new HashMap<String, Object>();
		
		List<Map<String, Object>> legalList = (List<Map<String, Object>>) request.get("LegalList");
		List<Map<String, Object>> legalIDList = new ArrayList<Map<String,Object>>();
		response.put("LegalIDList", legalIDList);
		
		for (Map<String, Object> item : legalList) {
			Long id = (Long) item.get("LegalID");
			
			Map<String, Object> resultItem = new HashMap<String, Object>();
			legalIDList.add(resultItem);
			
			resultItem.put("LegalID", id);
			resultItem.put("MDMID", Long.toString(id + 1000000L));
		}
		
		return xmlUtl.createXML(response);
	}

}
