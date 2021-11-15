package ru.diasoft.integration.vtb.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;

public class MyFileUtils {

    private static Logger logger = Logger.getLogger(MyFileUtils.class);

    public static File findFile(String dir, String fileName) {
        logger.debug("fake MyFileUtils.findFile dir = " + dir + "; fileName = " + fileName);
        if (StringUtils.isBlank(dir) || StringUtils.isBlank(fileName)) {
            return null;
        }
        File sourceDirectory = getDirectory(dir);
        return getFile(sourceDirectory, fileName);
    }

    private static File getDirectory(String directory) {
        logger.debug("fake MyFileUtils.getDirectory: " + directory);
        if (StringUtils.isBlank(directory)) {
            return null;
        }
        File dir = null;
        try {
            dir = new File(directory);
            dir.setWritable(true);
            dir.setReadable(true);

            if (OSVersionUtil.isUnix()) {
                logger.debug("operation system is Linux or Unix. Exec: chmod 777 " + dir.getAbsolutePath());
                Runtime.getRuntime().exec("chmod 777 " + dir.getAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("fake MyFileUtils.getDirectory error: " + e.getMessage());
        }
        return dir;
    }

    public static String getFileNameFromPath(String path) {
        logger.debug("fake MyFileUtils.getDirectoryFromPath path = " + path);
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String[] array = path.split("/");
        return (array.length > 0) ? array[array.length - 1] : null;
    }

    public static String getDirectoryFromPath(String path) {
        logger.debug("fake MyFileUtils.getDirectoryFromPath path = " + path);
        if (StringUtils.isBlank(path)) {
            return null;
        }
        int lastSlash = path.lastIndexOf("/");
        return (lastSlash > 0) ? path.substring(0, lastSlash) : null;
    }

    private static File getFile(File dir, String fileName) {
        if (dir == null || StringUtils.isBlank(fileName)) {
            return null;
        }
        File file = null;
        try {
            file = new File(dir.getPath() + File.separator + fileName);
            file.setWritable(true);
            file.setReadable(true);

            if (OSVersionUtil.isUnix()) {
                logger.debug("operation system is Linux or Unix. Exec: chmod 777 " + file.getAbsolutePath());
                Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("fake MyFileUtils.getFile error: " + e.getMessage());
        }
        return file;
    }
}
