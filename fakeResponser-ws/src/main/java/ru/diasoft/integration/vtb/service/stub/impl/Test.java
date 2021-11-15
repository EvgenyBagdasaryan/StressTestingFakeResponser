package ru.diasoft.integration.vtb.service.stub.impl;

import ru.diasoft.integration.vtb.utils.MyFileUtils;
import ru.diasoft.integration.vtb.utils.Utl;

public class Test {

    public static void main(String[] args) throws Exception {
        getDirectoryAndFileNameFromPath();
    }

    private static void getDirectoryAndFileNameFromPath() {
        String path = "/u01/weblogic/domains/sfrdev_debwlsapp05/fakeResponse/DsMortgageLoanAppGetListCreditContractAttributeReqA/DsMortgageLoanAppGetListCreditContractAttribute.json";
        String directory = MyFileUtils.getDirectoryFromPath(path);
        String fileName = MyFileUtils.getFileNameFromPath(path);
        System.out.println("directory: " + directory);
        System.out.println("fileName: " + fileName);
    }

    private static void getCommandFromURL() {
        String command = "/fakeResponser/rest/fake/post/CarDealAppRestUrl";
        String[] array = command.split("/");
        System.out.println((array.length > 0) ? array[array.length - 1] : null);
    }

    private static void getRequestPutValueXML() {
        String xml = "<Request xmlns:ns0=\"http://vtb24.ru/bankgateway/types\">" +
                "<ns0:RequestPutValue xmlns:ns0=\"http://ws.q30\">" +
                "   <ns0:SysId>DIAFRONT</ns0:SysId>" +
                "   <ns0:DataType>700</ns0:DataType>" +
                "   <ns0:DataValue>&amp;lt;![CDATA[" +
                "       <ns0:RequestSave xmlns:ns0=\"http://vtbins.ru/schema/kis10\" xmlns:ns2=\"http://ws.rls10\" xmlns:ns1=\"http://vtbins.ru/schema/common10\" schemaVersion=\"1.0\">" +
                "           <ns0:ExportInfo>" +
                "               <ns1:SysId>VTBBANK</ns1:SysId>" +
                "               <ns1:ExportDate>2020-05-30T01:06:36.733+03:00</ns1:ExportDate>" +
                "               <ns1:ExportId>0</ns1:ExportId>" +
                "           </ns0:ExportInfo>" +
                "       </ns0:RequestSave>" +
                "   ]]></ns0:DataValue>" +
                "</ns0:RequestPutValue>" +
                "</Request>";

        xml = Utl.cutBlockCDATA(xml);
        System.out.println("xml: " + xml);
    }
}
