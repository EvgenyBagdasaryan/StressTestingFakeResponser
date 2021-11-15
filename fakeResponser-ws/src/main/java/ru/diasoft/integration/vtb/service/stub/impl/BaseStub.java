package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;


public class BaseStub {

    protected static final String ENCODING = "utf8";
    
    private static JAXBContext jaxbContext = null;
	
    protected static Marshaller getMarshaller(Class clz) throws JAXBException, PropertyException {
        Marshaller marshaller = getJAXBContext(clz).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, ENCODING);
        return marshaller;
}

    protected static JAXBContext getJAXBContext(Class clz) throws JAXBException {
        if (jaxbContext == null) {
                        jaxbContext = JAXBContext.newInstance(clz);
        }
        return jaxbContext;
}

    protected static String objectToXml(Object object) throws Exception {        
                        Marshaller marshaller = getMarshaller(object.getClass());
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        marshaller.marshal(object, out);
                        return out.toString(ENCODING);
}       
	
}
