package ru.diasoft.integration.vtb.service.stub;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import ru.diasoft.utils.XMLUtil;

/**
 * @author asviridova
 *
 */
public class FileProcessor {

    protected static Logger logger = Logger.getLogger(FileProcessor.class);
    protected static XMLUtil util = new XMLUtil(true, false); 
    private static FileProcessor INSTANCE = new FileProcessor();
    
    public static FileProcessor getINSTANCE() {
		return INSTANCE;
	}


	public String getFakeResponseObjectByOperation(final String operationName) {

			String folderPath = AdtConfig.getFAKE_RESPONSE_FOLDER_PATH()  ;

            File folder = new File(folderPath);
            if (logger.isDebugEnabled()) {
            	logger.debug(String.format("Folder path [%s] exists=%b", folderPath, folder.exists()));
            }
            if (!folder.exists() || !folder.isDirectory()) {
                return null;
            }
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lowercaseName = name.toLowerCase();
                    if (lowercaseName.startsWith(operationName.toLowerCase())) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            File[] listXMLFiles = folder.listFiles(filter);
            if (listXMLFiles == null || listXMLFiles.length == 0) {
                return null;
            }

            for (File file : listXMLFiles) {
            	if (logger.isDebugEnabled()) {
                	logger.debug(String.format("File to process: %s ", file.getName()));
                }
                return process(file);
            }

        return null;
    }
        
    
    public String process(File file) {
    	
    	try {
			return FileUtils.readFileToString(file, "cp1251");
		} catch (IOException e) {
			logger.warn("Cannot read file", e);
		}
    	return null;
    }
    	
        
}
