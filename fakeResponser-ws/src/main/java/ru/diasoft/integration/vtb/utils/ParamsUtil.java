package ru.diasoft.integration.vtb.utils;

import org.apache.commons.lang.StringUtils;

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

    public static boolean isNotEmpty(String val) {
        return val != null && !"null".equals(val.trim()) && !"".equals(val.trim());
    }

    public static Long getLong(String sourceName, Map<String, Object> sourceMap) {
        Object value = sourceMap.get(sourceName);
        return getLong(value);
    }

    public static Long getLong(Object value) {
        Long result = null;
        if (value != null) {
            if (value instanceof Long) {
                result = (Long)value;
            }
            else if (value instanceof Integer) {
                result = new Long((Integer)value);
            }
            else if (value instanceof String) {
                String valStr = (String)value;
                if(!StringUtils.isBlank(valStr)){
                    result = Long.valueOf((String)value);
                }
            }
        }
        return result;
    }
}
