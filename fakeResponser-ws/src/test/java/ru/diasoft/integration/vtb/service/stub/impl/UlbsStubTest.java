package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.junit.Test;
import org.w3c.dom.Element;

import ru.diasoft.integration.vtb.service.stub.flexadt.ContextData;
import ru.diasoft.integration.vtb.service.stub.flexadt.DSCALLFAULT_Exception;
import ru.diasoft.utils.XMLUtil;

public class UlbsStubTest {

	@Test
	public void execTest() throws UnsupportedEncodingException, DSCALLFAULT_Exception {

		// Config.getConfig("test_fakeResponser");
		UlbsStub ulbsStub = new UlbsStub();
		WebServiceContext context = new WebServiceContext() {

			@Override
			public boolean isUserInRole(String role) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Principal getUserPrincipal() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public MessageContext getMessageContext() {
				// TODO Auto-generated method stub
				return new MessageContext() {

					@Override
					public Collection<Object> values() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public int size() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public Object remove(Object key) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void putAll(Map<? extends String, ? extends Object> m) {
						// TODO Auto-generated method stub

					}

					@Override
					public Object put(String key, Object value) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public Set<String> keySet() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean isEmpty() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public Object get(Object key) {
						// TODO Auto-generated method stub
						return "http://integr3.diasoft.ru:7003/fakeResponser/AutoLoanContractDSFTBQTS";
					}

					@Override
					public Set<Entry<String, Object>> entrySet() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean containsValue(Object value) {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public boolean containsKey(Object key) {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void clear() {
						// TODO Auto-generated method stub

					}

					@Override
					public void setScope(String name, Scope scope) {
						// TODO Auto-generated method stub

					}

					@Override
					public Scope getScope(String name) {
						// TODO Auto-generated method stub
						return null;
					}
				};
			}

			@Override
			public <T extends EndpointReference> T getEndpointReference(Class<T> clazz,
					Element... referenceParameters) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EndpointReference getEndpointReference(Element... referenceParameters) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		ulbsStub.setContext(context);
		ContextData contextdata = new ContextData();
		contextdata.setSessionId("342523_356346");
		String commandtext = "CreateUpdateAutoLoanContract";
		XMLUtil util = new XMLUtil();
		Map<String, Object> reqMap = new HashMap<String, Object>();
		reqMap.put("Id", 1);
		String commanddata = util.createXML(reqMap);
		ulbsStub.dscallasync(commandtext, commanddata, contextdata);
		//System.out.println(res);
	}

	public static void main(String[] args) throws UnsupportedEncodingException, DSCALLFAULT_Exception {
		UlbsStubTest test = new UlbsStubTest();
		test.execTest();

	}

}
