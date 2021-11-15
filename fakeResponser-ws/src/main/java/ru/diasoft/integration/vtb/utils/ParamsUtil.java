package ru.diasoft.integration.vtb.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ParamsUtil {

    public static String getString(String sourceName, Map<String, Object> sourceMap) {
        if (sourceMap == null) {
            return null;
        }
        Object value = sourceMap.get(sourceName);
        return getString(value);
    }

    public static String getString(Object value) {
        String result = null;
        if (value != null) {
            if (value instanceof String) {
                result = (String) value;
            } else if (value instanceof Date) {
                Date d = (Date) value;
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                result = formatter.format(d);
            } else {
                result = String.valueOf(value);
            }
        }
        return result;
    }
}
