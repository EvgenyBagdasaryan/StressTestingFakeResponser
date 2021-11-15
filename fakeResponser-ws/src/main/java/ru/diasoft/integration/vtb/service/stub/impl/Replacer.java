package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ru.diasoft.integration.vtb.utils.Utl;

public class Replacer {
	
	private static Logger logger = Logger.getLogger(Replacer.class);
	
	public static class Context {

		private final String commanddata;
		
		private Document doc;
		private XPath xpath;

		public Context(String commanddata) {
			this.commanddata = commanddata;			
		}
		
		public String getCommanddata() {
			return commanddata;
		}

		public Document getDoc() throws Exception {
			if (doc == null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(new ByteArrayInputStream(commanddata.getBytes(Utl.CHARSET_UTF_8)));
			}
			return doc;
		}

		public XPath getXpath() {
			if (xpath == null) {
				XPathFactory xpathFactory = XPathFactory.newInstance();
				xpath = xpathFactory.newXPath();
			}
			return xpath;
		}

		
		
	}
	
	private static interface Calculate {
		public String calculate(String val, Context context) throws Exception;
	}
	
	private static class XPatchCalculate implements Calculate {

		@Override
		public String calculate(String val, Context context) throws Exception {
			XPathExpression expr = context.getXpath().compile(val);
			return (String) expr.evaluate(context.getDoc(), XPathConstants.STRING);
			
		}
		
	}
	
	private static class SequenceCalculate implements Calculate {

		@Override
		public String calculate(String val, Context context) throws Exception {			
			return Long.toString(Sequence.nextVal(val));
		}
		
	}
	
	
	public static enum Type {
		XPATH("xpath", new XPatchCalculate()),
		SEQUENCE("sequence", new SequenceCalculate());
		
		private final String id;
		private final Calculate calculator;
		
		private Type(String id, Calculate calculator) {
			this.id = id;
			this.calculator = calculator;
		}
		
		public static Type get(String _id) {
			for (Type t : Type.values()) {
				if (t.id.equals(_id)) {
					return t;
				}
			}
			
			throw new RuntimeException("Replacer Type " + _id + " not found");
		}
		
		public String calculate(String val, Context context) throws Exception {
			return calculator.calculate(val, context);
		}
		
	}
	
	
	private final Type type;
	private final String label;
	private final String value;
	
	public Replacer(String type, String label, String value) {
		this.label = label;
		this.value = value;
		this.type = Type.get(type);
	}
	
	
	public String replace(String input, Context context) throws Exception {
		String result = type.calculate(value, context);
		
		logger.debug("Start replace " + label + " to " + result + " " + type + ":[" + value + "]");
		return input.replaceAll("\\$\\{"+label+"\\}", result);
	}
	
}
