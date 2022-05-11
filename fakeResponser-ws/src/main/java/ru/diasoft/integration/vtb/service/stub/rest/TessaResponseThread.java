package ru.diasoft.integration.vtb.service.stub.rest;

import com.sun.jersey.spi.container.ResourceFilters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;
import ru.diasoft.integration.vtb.service.stub.impl.StubProcessor;
import ru.diasoft.integration.vtb.utils.DataConvertUtil;
import ru.diasoft.integration.vtb.utils.MyFileUtils;
import ru.diasoft.integration.vtb.utils.ParamsUtil;
import ru.diasoft.integration.vtb.utils.Utl;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URLEncoder;
import java.util.*;
//import ru.diasoft.integration.vtb.services.DSCALLSender;

class TessaResponseThread extends Thread {

    private String reqID;
    private String crmTaskID;
    //private final DSCALLSender dscallService = ServiceFactory.getInstance().getDsCallService();

    TessaResponseThread(String requestID, String cRMTaskID){
        reqID = requestID;
        crmTaskID = cRMTaskID;
    }

    @Override
    public void run() {
        try {

            //Map<String, Object> crossRes = dscallService.dscall(AdtConfig.getUrl_VTB24CONVWS(), "dsGetCrossRefPerson", crossReq);

            Thread.sleep(3000);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


