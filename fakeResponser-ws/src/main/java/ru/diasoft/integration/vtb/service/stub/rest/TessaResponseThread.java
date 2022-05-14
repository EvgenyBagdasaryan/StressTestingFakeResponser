package ru.diasoft.integration.vtb.service.stub.rest;

import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;
import sun.misc.BASE64Encoder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

class TessaResponseThread extends Thread {

    private String reqID;
    private String crmTaskID;
    private static Logger logger = Logger.getLogger(TessaResponseThread.class);

    TessaResponseThread(String requestID, String cRMTaskID){
        reqID = requestID;
        crmTaskID = cRMTaskID;
    }

    @Override
    public void run() {
        try {

            Thread.sleep(3000);

            String url = StubConfig.getTessaAsyncCallbackUrl();//"http://k6-sfr-app01.vtb24.ru:8004/vtbadt24conv/rest/dsGetResultTaskInTessa/application";
            String name = StubConfig.getAdminLogin();
            String password = StubConfig.getAdminPassword();
            String authString = name + ":" + password;
            String authStringEnc = new BASE64Encoder().encode(authString.getBytes());
            logger.debug("Base64 encoded auth string: " + authStringEnc);
            Client restClient = Client.create();
            WebResource webResource = restClient.resource(url);
            String json = "{ \"RequestID\": \"" + reqID + "\", " +
                    "\"TraceID\":\"8F2C9524-EF18-4303-AAE0-0011E4C07D60\", " +
                    "\"CRMTaskID\": \"" + crmTaskID + "\", " +
                    "\"TessaTaskState\":12, " +
                    "\"Message\":\"\\n\", " +
                    "\"ErrList\":null }";
            logger.debug("income dsGetResultTaskInTessa json: " + json);
            ClientResponse resp = webResource
                    .type("application/json;charset=UTF-8")
                    .header("Authorization", "Basic " + authStringEnc)
                    .post(ClientResponse.class, json);
            logger.debug("output dsGetResultTaskInTessa Status: " + resp.getStatus());
            if(resp.getStatus() != 200){
                logger.debug("Unable to connect to the server");
            }
            String output = resp.getEntity(String.class);
            logger.debug("response dsGetResultTaskInTessa: " + output);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


