package ru.diasoft.integration.vtb.utils;

import org.apache.commons.lang.time.DateUtils;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

public class Utl {
    public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    public static Date getCurrentDateTimeZoneUTC(Date inputDate) {
        if (inputDate == null)
            return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(inputDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        Calendar calendarUTC = Calendar.getInstance(DateUtils.UTC_TIME_ZONE);
        calendarUTC.set(year, month, dayOfMonth, 0, 0, 0);
        return calendarUTC.getTime();
    }

    public static String cutBlockCDATA(String msg) {
        String resultMsg = msg;
        int begin = msg.indexOf("<![CDATA");
        if (begin < 0)
            begin = msg.indexOf("&amp;lt;![CDATA");

        if (begin >= 0) {
            int indxAttachEnd = msg.indexOf("]]>") + 3;
            resultMsg = msg.substring(0, begin) + msg.substring(indxAttachEnd);
        }
        resultMsg = resultMsg.replaceAll("<>", "");
        resultMsg = resultMsg.replaceAll("</>", "");
        return resultMsg;
    }

    public static String changeTags(String str) {
        if (str != null) {
            str = str.replaceAll("&amp;quot;", "\"");
            str = str.replaceAll("&quot;", "\"");
            str = str.replaceAll("&amp;lt;", "<");
            str = str.replaceAll("&amp;gt;", ">");
            str = str.replaceAll("&lt;", "<");
            str = str.replaceAll("&gt;", ">");
        }
        return str;
    }

    public static String getCommandFromRestUrl(String url) {
        String[] array = url.split("/");
        return (array.length > 0) ? array[array.length - 1] : null;
    }
}
