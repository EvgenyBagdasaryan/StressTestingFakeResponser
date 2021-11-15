package ru.diasoft.integration.vtb.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DataConvertUtil {

    private static Logger logger = Logger.getLogger(DataConvertUtil.class);

    public static Map<String, Object> jsonToMap(String json) {
        logger.debug("fake DataConvertUtil.jsonToMap start with params: " + json);
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, typeRef);

        } catch (Exception e) {
            logger.error("fake DataConvertUtil.jsonToMap error: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
