package ru.diasoft.integration.vtb.service.stub.rest;

import com.sun.jersey.spi.container.ResourceFilters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.Constants;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;
import ru.diasoft.integration.vtb.service.stub.kafka.MessageSender;
import ru.diasoft.integration.vtb.service.stub.rest.model.Device;
import ru.diasoft.integration.vtb.service.stub.rest.response.AuthorizationResponse;
import ru.diasoft.integration.vtb.service.stub.rest.response.BaseResponse;
import ru.diasoft.integration.vtb.service.stub.rest.response.DeviceCollectionResponse;
import ru.diasoft.integration.vtb.utils.*;
import ru.diasoft.integration.vtb.service.stub.impl.StubProcessor;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.net.URLEncoder;
import java.util.*;

@Path("/fake")
public class RestServer {

    private static Logger logger = Logger.getLogger(RestServer.class);

    private static final String TYPE_DOC = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String TYPE_JSON = "application/json;charset=UTF-8";

    private static final String USERNAME = "client_id";     //username
    private static final String PASSWORD = "client_secret"; //password

    @Context
    UriInfo uri;

    /* BR-21227 Тестовый API для оформления автокредита в ВТБ-Онлайн "Сделка"
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/CarDealAppRestUrl
     */
    @Path("/CarDealAppRestUrl")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response carDealAppRestUrl(String params) {
        return getData("CarDealAppRestUrl", params);
    }

    /* BR-20656 Тестовый API для эмуляции цепочки СФР -> Сервис печатных форм -> СФР
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/file/response
     * Если ошибок нет, тогда на выходе поток байт
     * Если возникла ошибка, тогда на выходе JSON
     */
    @Path("/DsMortgageLoanAppGetListCreditContractAttribute")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response fileResponse(String params) {
        logger.debug("fake DsMortgageLoanAppGetListCreditContractAttribute start");
        String path = null;
        try {

            String command = Utl.getCommandFromRestUrl(uri.getPath());
            String response = StubProcessor.processSyncJson(command + "ReqA", command, params);

            Map<String, Object> mapFromJson = DataConvertUtil.jsonToMap(response);
            path = ParamsUtil.getString("filePath", mapFromJson);

            String directory = MyFileUtils.getDirectoryFromPath(path);
            String fileName = MyFileUtils.getFileNameFromPath(path);
            File file = MyFileUtils.findFile(directory, fileName);

            byte[] bytes = (file != null) ? FileUtils.readFileToByteArray(file) : null;
            if (bytes == null) {
                throw new Exception("file not found by path = " + path);
            }

            fileName = (StringUtils.isNotBlank(fileName)) ? URLEncoder.encode(fileName, "UTF-8") : null;
            return Response.status(200)
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .header("Content-Type", TYPE_DOC)
                    .entity(bytes).build();

        } catch (Exception e) {
            logger.error("fake DsMortgageLoanAppGetListCreditContractAttribute error: " + e.getMessage());
            return Response.status(500)
                    .header("Content-Type", TYPE_JSON)
                    .entity("{" +
                            "\"timestamp\": " + new Date() + "," +
                            "\"status\": 500," +
                            "\"error\": \"Internal Server Error\"," +
                            "\"message\": \"" + e.getMessage() + "\"," +
                            "\"path\": \"" + path + "\"," +
                            "\"MSA_ID\": \"pdoc-admin-back\"," +
                            "\"ERROR_CODE\": \"PDOC2_QUERY_NOSYSNAME\"," +
                            "\"ERROR_ID\": \"PDOC21902299758\"," +
                            "\"USER_INFO\": \"" + e.getMessage() + "\"," +
                            "\"ADD_PARAM\": \"credContrRefCompleted\"" +
                            "}").build();
        }
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsClientBlacklistMatch
     */
    @Path("/DsClientBlacklistMatch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsClientBlacklistMatch(String params) {
        return getData("DsClientBlacklistMatch", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsArbitrationBlacklistCreate
     */
    @Path("/DsArbitrationBlacklistCreate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsArbitrationBlacklistCreate(String params) {
        return getData("DsArbitrationBlacklistCreate", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/dsInsuranceCompanyAttributes
     */
    @Path("/DsInsuranceCompanyAttributes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsInsuranceCompanyAttributes(String params) {
        return getData("DsInsuranceCompanyAttributes", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsLegalEntities
     */
    @Path("/DsLegalEntities")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsLegalEntities(String params) {
        return getData("DsLegalEntities", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsLegalEntity
     */
    @Path("/DsLegalEntity")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsLegalEntity(String params) {
        return getData("DsLegalEntity", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsLegalEntityPaymentDetails
     */
    @Path("/DsLegalEntityPaymentDetails")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsLegalEntityPaymentDetails(String params) {
        return getData("DsLegalEntityPaymentDetails", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsLegalEntitySellerDetails
     */
    @Path("/DsLegalEntitySellerDetails")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsLegalEntitySellerDetails(String params) {
        return getData("DsLegalEntitySellerDetails", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/authorization_service
     */
    @POST
    @Path(value = "/authorization_service")
    @Produces(MediaType.APPLICATION_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response authorizationService(
            @HeaderParam(USERNAME) String userName,
            @HeaderParam(PASSWORD) String password) {

        logger.debug("fake authorization_service start");
        /*try {
            if (userName.isEmpty()) {
                throw new Exception(USERNAME + " field cannot be empty");
            }
            if (password.isEmpty()) {
                throw new Exception(PASSWORD + " field cannot be empty");
            }
            String privateKey = JwTokenHelper.getInstance().generatePrivateKey(userName, password);
            return Response.status(200).entity(
                    new AuthorizationResponse(
                            BaseResponse.SUCCESS,
                            "You're authenticated successfully. Private key will be valid for 30 mins",
                            privateKey)
            ).build();

        } catch (Exception e) {
            logger.error("fake authorization_service error: " + e.getMessage());
            return Response.status(400).entity("{" +
                    "\"Status\": ERROR," +
                    "\"ReturnCode\": " + 1 + " ," +
                    "\"ReturnMsg\": \"" + e.getMessage() + "\"," +
                    "}").build();
        }*/
        String uuid = UUID.randomUUID().toString();
        return Response.status(200).entity("{" +
                "\"access_token\": \"" + uuid + "\"" +
                "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsCreateCrossRefPerson
     */
    @Path("/DsCreateCrossRefPerson")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCreateCrossRefPerson(String params) {
        return getData("DsCreateCrossRefPerson", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsGetCrossRefPerson
     */
    @Path("/DsGetCrossRefPerson")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsGetCrossRefPerson(String params) {
        return getData("DsGetCrossRefPerson", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsDeleteCrossRefPerson
     */
    @Path("/DsDeleteCrossRefPerson")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsDeleteCrossRefPerson(String params) {
        return getData("DsDeleteCrossRefPerson", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsCspcPersonMatch
     */
    @Path("/DsCspcPersonMatch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCspcPerson(String params) {
        return getData("DsCspcPersonMatch", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsCspcPersonCreate
     */
    @Path("/DsCspcPersonCreate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCspcPersonCreate(String params) {
        return getData("DsCspcPersonCreate", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsCspcPersonGet
     */
    @Path("/DsCspcPersonGet")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCspcPersonGet(String params) {
        return getData("DsCspcPersonGet", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendSMSMessage
     */
    @Path("/DsSendSMSMessage")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsSendSMSMessage(String params) {
        return Response.status(200).entity("[{" +
                "\"status\": \"OK\"" +
                "}]").build();
    }

    //<PFRApiKey>60059182d87cbc468b65bba16b940b6a6706414da9061d49ce52fece</PFRApiKey>
    //<PFRCancelWaitingResponseRestUrl>https://k4-epaa-app401lv.vtb24.ru/api/rb/smkp/v1/credit-conveyor-request</PFRCancelWaitingResponseRestUrl>
    //<PFRNotificationRestUrl>https://k4-epaa-app401lv.vtb24.ru/api/rb/smkp/v1/credit-conveyor-request/initial-payment-request</PFRNotificationRestUrl>
    //<PFROrderRestURL>https://k4-epaa-app401lv.vtb24.ru/api/rb/smkp/v1/credit-conveyor-request/early-repayment-request</PFROrderRestURL>

    //DsSendDataToPFR
    //http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFRCancelWaitingResponse
    //http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFRNotification
    //http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFROrder

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFRCancelWaitingResponse
     */
    @Path("/DsSendDataToPFRCancelWaitingResponse")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsSendDataToPFRCancelWaitingResponse(String params) {
        return Response.status(200).entity("{" +
                "\"status\": \"OK\"" +
                "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFRNotification
     */
    @Path("/DsSendDataToPFRNotification")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsSendDataToPFRNotification(String params) throws InterruptedException {

        String id = UUID.randomUUID().toString();

        new PFRNotificationThread(id).start();

        return Response.status(200).entity("{" + "\"id\": \"" + id + "\"" + "}").build();
    }

    @Path("/DsProductDossierConverter")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsProductDossierConverter(String params) throws InterruptedException {

       return Response.status(200).entity("{" + "\"errorCode\": \"" + 0 + "\"" + "}").build();
    }

    @Path("/DsProductDossierBarCode")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsProductDossierBarCode(String params) throws InterruptedException {

        String barCode = "259362045688178167605669812097352137891";

        return Response.status(200).entity(barCode).build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsPutFileToECM/files?folder=DOSE"
     */
    @Path("/DsPutFileToECM/files")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsPutFileToECM2(String params) throws InterruptedException {

        String uuid = UUID.randomUUID().toString();
        return Response.status(200).entity("{" + "\"uuid\": \"" + uuid + "\"" + "}").build();
    }
    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsPutFileToECM/permission-sharings/transfer
     */
    @Path("/DsPutFileToECM/permission-sharings/transfer")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsPermissionSharing(String params) throws InterruptedException {

        return Response.status(200).entity("[{" +
                "\"status\": \"OK\"" +
                "}]").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFRNotification
     */
    @Path("/DsPublishPersonFromMDM")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsPublishPersonFromMDM(String params) {

        Map<String, Object> kafkaConfig = StubConfig.getKafkaConfig();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.valueOf(kafkaConfig.get("Url")));

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, String.valueOf(kafkaConfig.get("SslTruststoreLocation")));
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,  String.valueOf(kafkaConfig.get("SslTruststorePassword")));

        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, String.valueOf(kafkaConfig.get("SslKeystoreLocation")));
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, String.valueOf(kafkaConfig.get("SslKeystorePassword")));
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, String.valueOf(kafkaConfig.get("SslKeyPassword")));

        //props.put(ProducerConfig.ACKS_CONFIG, "all");
        //props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        Header headerm = new RecordHeader("keyHeader", "from fakeResponser with love".getBytes());
        List<Header> headerma = Arrays.asList(headerm);

        String id = UUID.randomUUID().toString();

        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
        ProducerRecord<String, String> records = new ProducerRecord(String.valueOf(kafkaConfig.get("Topic")), 0, id,"{\"headerRequest\":{\"messageID\":\"85ff3295-ee64-4a8b-9789-d4ef01fc71ca\",\"creationDateTime\":\"2020-12-15T12:01:59\",\"systemFrom\":\"MDM_CH\",\"systemTo\":\"Kafka\",\"contactName\":\"MS Person Fl\"},\"messageRequest\":{\"person\":[{\"partyUId\":\"1542025959\",\"consentCreditBureauVerification\":false,\"consentCreditBureauUpload\":false,\"terminated\":false,\"underInvestigationFlag\":false,\"criminalRecordFlag\":false,\"taxRezident\":true,\"rezidentFlag\":true,\"type\":\"1\",\"status\":\"1\",\"genderCode\":\"1\",\"birthDateTime\":\"1990-12-10T00:00:00\",\"firstName\":\"Олег\",\"middleName\":\"Олегович\",\"lastName\":\"Воронин\",\"lastNameLat\":\"VORONIN\",\"firstNameLat\":\"OLEG\",\"pubOfficialStatus\":0,\"nationalityCountryCode\":\"643\",\"loginExternalSystem\":\"4516\",\"nameExternalSystem\":\"EID\",\"updateDate\":\"2021-11-23T14:16:25\",\"startDate\":\"2021-11-23T14:17:34\",\"employment\":[],\"riskLevelJustification\":[],\"education\":[],\"relatives\":[],\"personDocumentIdentity\":[{\"number\":\"560560\",\"trustFlag\":true,\"loginExternalSystem\":\"4516\",\"nameExternalSystem\":\"EID\",\"updateDate\":\"2021-11-23T14:16:25\",\"series\":\"56 07\",\"issueDate\":\"2018-08-08\",\"expirationDate\":null,\"startDate\":null,\"endDate\":null,\"typeCode\":\"21\",\"issueCountryCode\":null,\"issueName\":null,\"issueCode\":null,\"startOfRightToStay\":null,\"endOfRightToStay\":null,\"comment\":null}],\"address\":[],\"contactPhoneCommunication\":[],\"emailCommunication\":[],\"contactRelationship\":[],\"segment\":[{\"nameExternalSystem\":\"EID\",\"startDate\":null,\"endDate\":null,\"updateDate\":\"2021-11-23T14:16:25\",\"loginExternalSystem\":\"4516\",\"value\":\"N\",\"status\":0,\"type\":\"39\",\"criterion\":null}],\"blackList\":[],\"personCrossRef\":[{\"organizationFlag\":false,\"id\":\"29262131\",\"externalSystemId\":\"EID\"},{\"organizationFlag\":false,\"id\":\"29262131\",\"externalSystemId\":\"EID2\"},{\"organizationFlag\":true,\"id\":\"1-VSKAWMS\",\"externalSystemId\":\"SBL_FR\"},{\"organizationFlag\":true,\"id\":\"86516039\",\"externalSystemId\":\"IVR\"}]}]}}", headerma);
        producer.send(records);

        producer.flush();
        producer.close();

        return Response.status(200).entity("{" + "\"id\": \"" + id + "\"" + "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsSendDataToPFROrder
     */
    @Path("/DsSendDataToPFROrder")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsSendDataToPFROrder(String params) {
        return Response.status(200).entity("{" +
                "\"status\": \"OK\"" +
                "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsQualifyCustomerByPayroll
     */
    @Path("/DsQualifyCustomerByPayroll")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsQualifyCustomerByPayroll(String params) {
        return getData("DsQualifyCustomerByPayroll", params);
    }
	
	/*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsMarketingOffer
     */
    @Path("/DsMarketingOffer")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsMarketingOffer(String params) {
        return getData("DsMarketingOffer", params);
    }


    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsAddCustomerDigitalDocument
     */
    @Path("/DsAddCustomerDigitalDocument")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsAddCustomerDigitalDocument(String params) {
        String uuid = UUID.randomUUID().toString();
        return Response.status(200).entity("{" +
                "\"errorCode\": " + 0 + " ," +
                "\"requestId\": \"" + uuid + "\"," +
                "\"errorDescription\": \"" + "Успех" + "\"" +
                "}").build();
    }


    private Response getData(String command, String params) {
        logger.debug("fake " + command + " start");
        try {
            command = Utl.getCommandFromRestUrl(uri.getPath());
            String response = StubProcessor.processSyncJson(command + "ReqA", command, params);
            return Response.status(200).entity(response).build();

        } catch (Exception e) {
            logger.error("fake " + command + " error: " + e.getMessage());
            return Response.status(400).entity("{" +
                    "\"Status\": ERROR," +
                    "\"ReturnCode\": " + 1 + " ," +
                    "\"ReturnMsg\": \"" + e.getMessage() + "\"," +
                    "}").build();
        }
    }

    private Response getDataForKafka(String command, String params) {
        logger.debug("fake " + command + " start for kafka");
        try {
            String response = StubProcessor.processSyncJson(command + "ReqA", command, params);
            return Response.status(200).entity(response).build();

        } catch (Exception e) {
            logger.error("fake " + command + " error: " + e.getMessage());
            return Response.status(400).entity("{" +
                    "\"Status\": ERROR," +
                    "\"ReturnCode\": " + 1 + " ," +
                    "\"ReturnMsg\": \"" + e.getMessage() + "\"," +
                    "}").build();
        }
    }

    public void mortgageContractKafkaRouter (String type, Map<String, Object> dataFromKafka) throws Exception {
        List<Map<String, Object>> headers = (List<Map<String, Object>>) dataFromKafka.get("headers");
        Map<String, Object> kafkaTS73Config = StubConfig.getKafkaAZSConfig();
        MessageSender sender = new MessageSender(kafkaTS73Config);
        List<Header> headerList = new ArrayList<>();
        String command = "";
        for(Map<String, Object> head : headers) {

            for(Map.Entry<String, Object> entryHead : head.entrySet()) {
                if(entryHead.getKey().equals("__TypeId__")) {
                    String typeId = "";
                    if(entryHead.getValue().equals(Constants.AZS_CREATEUPDATE_CONTRACT_OPERATION_NAME)) {
                        typeId = Constants.AZS_KAFKA_CREATEUPDATE_CONTRACT_REPLY_MESSAGE;
                        command = "DsCreateUpdateContract";
                    }
                    if(entryHead.getValue().equals(Constants.AZS_CANCEL_CONTRACT_OPERATION_NAME)) {
                        typeId = Constants.AZS_KAFKA_CANCEL_CONTRACT_REPLY_MESSAGE;
                        command = "DsCancelUpdateContract";
                    }
                    if(entryHead.getValue().equals(Constants.AZS_CREATE_CREDITING_ORDER_OPERATION_NAME)) {
                        typeId = Constants.AZS_KAFKA_CREATE_CREDITING_ORDER_REPLY_MESSAGE;
                        command = "DsCreateCreditingOrder";
                    }
                    if(entryHead.getValue().equals(Constants.AZS_UPDATE_COLLATERAL_OPERATION_NAME)) {
                        typeId = Constants.AZS_KAFKA_UPDATE_COLLATERAL_REPLY_MESSAGE;
                        command = "DsUpdateMortgageCollateral";
                    }
                    headerList.add(new RecordHeader("__TypeId__", typeId.getBytes()));
                } else {
                    headerList.add(new RecordHeader(entryHead.getKey(), entryHead.getValue().toString().getBytes()));
                }
            }
        }

        Response resp = dsCommonTs73Router("", command);
        String json = resp.getEntity().toString();
        sender.send(json, headerList, UUID.randomUUID().toString());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCommonTs73Router(String params, String command) {
        return getDataForKafka(command, params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/tessa/GetTaskStatus
     */
    @Path("/tessa/GetTaskStatus")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsTessaGetTaskStatus(String params) throws InterruptedException {

        Map<String, Object> mapParams = DataConvertUtil.jsonToMap(params);
        String requestID = ParamsUtil.getString(mapParams.get("RequestID"));
        String cRMTaskID = ParamsUtil.getString(mapParams.get("CRMTaskID"));

        return Response.status(200).entity("{\n" +
                "   \"RequestID\": \"" + requestID + "\",\n"  +
                "   \"CRMTaskID\": \"" + cRMTaskID + "\",\n"  +
                "   \"TessaTasks\": [   {\n" +
                "      \"TessaTaskID\": \"4785c12d-76aa-459f-8bf3-7a90d943a835\",\n" +
                "      \"TessaTaskState\": \"9\",\n" +
                "      \"ErrorDescription\": \"Успех\",\n" +
                "      \"Description\": null,\n" +
                "      \"TaskErrors\": [],\n" +
                "      \"Contracts\": [      {\n" +
                "      \"ContractNumber\": \"634/0100-0006168\",\n" +
                "      \"ContractDate\": \"0001-01-01T00:00:00\"\n" +
                "      }],\n" +
                "      \"BOD\": \"ОКСФПН УКДОФЛ ДОПБ\",\n" +
                "      \"EmployeeClockNumber\": null,\n" +
                "      \"EmployeeFIO\": null,\n" +
                "      \"EmployeeComment\": null\n" +
                "   }],\n" +
                "   \"TaskType\": 31,\n" +
                "   \"Files\": [],\n" +
                "   \"InitSystem\": null\n" +
                "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/tessa/CreateOrUpdateCreditTaskAndFiles
     */
    @Path("/tessa/CreateOrUpdateCreditTaskAndFiles")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsTessaCreateOrUpdateCreditTaskAndFiles(String params) throws InterruptedException {

        Map<String, Object> mapParams = DataConvertUtil.jsonToMap(params);
        String requestID = ParamsUtil.getString(mapParams.get("RequestID"));
        String cRMTaskID = ParamsUtil.getString(mapParams.get("CRMTaskID"));

        new TessaResponseThread(requestID, cRMTaskID).start();

        return Response.status(200).entity("{\n" +
                "   \"RequestID\": \"" + requestID + "\",\n"  +
                "   \"TraceID\": null,\n" +
                "   \"CRMTaskID\": null,\n" +
                "   \"TessaTaskState\": 0,\n" +
                "   \"Message\": \"Успех.\",\n" +
                "   \"ErrList\": [   {\n" +
                "      \"Code\": \"0\",\n" +
                "      \"Description\": \"Успех.\"\n" +
                "   }]\n" +
                "}").build();
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/dsCustomerRelationshipCreateOrModify
     */
    @Path("/dsCustomerRelationshipCreateOrModify")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response dsCustomerRelationshipCreateOrModify(String params) {
        return getData("dsCustomerRelationshipCreateOrModify", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsLegalBrowseListByParam
     */
    @Path("/DsLegalBrowseListByParam")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response DsLegalBrowseListByParam(String params) {
        return getData("DsLegalBrowseListByParam", params);
    }

    /*
     * Пример: http://debwlsapp05:8004/fakeResponser/rest/fake/DsCreateGuarantorOrganization
     */
    @Path("/DsCreateGuarantorOrganization")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(TYPE_JSON)
    @ResourceFilters({RestLoggingFilter.class})
    public Response DsCreateGuarantorOrganization(String params) {
        return getData("DsCreateGuarantorOrganization", params);
    }
}

