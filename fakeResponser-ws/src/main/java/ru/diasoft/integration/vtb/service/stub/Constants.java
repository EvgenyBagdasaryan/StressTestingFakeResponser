package ru.diasoft.integration.vtb.service.stub;

public class Constants {

	public static final String ENCODING = "windows-1251";

    public static final Long OK_CODE = 0L;
    public static final Long ERR_CODE = 1L;
    public static final String STATUS_OK = "OK";

	//[TS73] для запроса от адаптера
	public static final String AZS_CREATEUPDATE_CONTRACT_OPERATION_NAME = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.ExtBqCreateUpdateContractMessage";
	public static final String AZS_CANCEL_CONTRACT_OPERATION_NAME = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.ExtBqCancelMortgageContractMessage";
	public static final String AZS_CREATE_CREDITING_ORDER_OPERATION_NAME = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.ExtBqCreateCreditingOrderMessage";
	public static final String AZS_UPDATE_COLLATERAL_OPERATION_NAME = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.ExtBqUpdateMortgageCollateralMessage";

	//[TS73] для ответа в адаптер
	public static final String AZS_KAFKA_CREATEUPDATE_CONTRACT_REPLY_MESSAGE = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.reply.ExtBqCreateUpdateContractReplyMessage";
	public static final String AZS_KAFKA_CANCEL_CONTRACT_REPLY_MESSAGE = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.reply.ExtBqCancelMortgageContractReplyMessage";
	public static final String AZS_KAFKA_CREATE_CREDITING_ORDER_REPLY_MESSAGE = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.reply.ExtBqCreateCreditingOrderReplyMessage";
	public static final String AZS_KAFKA_UPDATE_COLLATERAL_REPLY_MESSAGE = "ru.vtb.maps.externalrouter.api.dto.external.bq.contractprocessing.v1.reply.ExtBqUpdateMortgageCollateralReplyMessage";


}
