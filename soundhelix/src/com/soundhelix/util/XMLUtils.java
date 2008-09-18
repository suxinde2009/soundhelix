package com.soundhelix.util;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import com.soundhelix.misc.XMLConfigurable;

/**
 * Implements some static methods for parsing XML data.
 * 
 * @author Thomas Schürger
 */

public class XMLUtils {
	private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
	
	
	
	private static Random random = new Random();
	
	public static Node getFirstElementChild(Node node) {
		node = node.getFirstChild();
		
		while(node != null) {
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				return node;
			}			
			node = node.getNextSibling();
		}
		
		return null;	
	}

	public static Node getNextElementSibling(Node node) {
		while(node != null) {
			node = node.getNextSibling();
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				return node;
			}
		}
		
		return null;	
	}

	/**
	 * Searches for the element pointed to by path and tries to parse
	 * it as an integer.
	 * 
	 * @param path the XPath expression
	 * @param parentNode the parent node to start searching from
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */
	
	public static int parseInteger(String path,Node parentNode,XPath xpath) {
		try {
			Node node = (Node)xpath.evaluate(path,parentNode,XPathConstants.NODE);

			if(node == null) {
				throw(new RuntimeException("Path \""+path+"\" not found within node "+parentNode.getNodeName()));
			}

			return XMLUtils.parseInteger(node,xpath);
		} catch(Exception e) {
			throw(new RuntimeException("Error parsing integer",e));
		}
	}

	/**
	 * Tries to parse the text content of the given node as an integer.
	 * If it is an integer, the integer is returned. Otherwise, the node
	 * is checked for valid subelements, which are then evaluated.
	 * 
	 * @param node the node to parse
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */

	public static int parseInteger(Node node,XPath xpath) {
		try {
		  return Integer.parseInt(node.getTextContent());
		} catch(RuntimeException e) {}

		Node n = getFirstElementChild(node);
	
		if(n.getNodeName().equals("random")) {
			try {
				int min = Integer.parseInt((String)xpath.evaluate("attribute::min",n,XPathConstants.STRING));
				int max = Integer.parseInt((String)xpath.evaluate("attribute::max",n,XPathConstants.STRING));

				String type = (String)xpath.evaluate("attribute::type",n,XPathConstants.STRING);
				
				if(type == null || type.equals("") || type.equals("uniform")) {
					int step = 1;
					
					try {
						step = Integer.parseInt((String)xpath.evaluate("attribute::step",n,XPathConstants.STRING));						
					} catch(Exception e) {}
					
					return RandomUtils.getUniformInteger(min,max,step);
				} else if(type.equals("normal")) {
					double mean = Double.parseDouble((String)xpath.evaluate("attribute::mean",n,XPathConstants.STRING));
					double variance = Double.parseDouble((String)xpath.evaluate("attribute::variance",n,XPathConstants.STRING));
	
					return RandomUtils.getNormalInteger(min,max,mean,variance);
				} else {
					throw(new RuntimeException("Unknown random distribution \""+type+"\""));
				}
			} catch(Exception e) {throw(new RuntimeException("Error parsing random attributes",e));}
		}
		else {
			throw(new RuntimeException("Invalid element "+n.getNodeName()));
		}
	}

	/**
	 * Searches for the element pointed to by path and tries to parse
	 * it as a string.
	 * 
	 * @param path the XPath expression
	 * @param parentNode the parent node to start searching from
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */
	
	public static String parseString(String path,Node parentNode,XPath xpath) {
		try {
			Node node = (Node)xpath.evaluate(path,parentNode,XPathConstants.NODE);

			if(node == null) {
				return null;
			}

			return XMLUtils.parseString(node,xpath);
		} catch(Exception e) {
			throw(new RuntimeException("Error parsing string",e));
		}
	}
	
	public static String parseString(Node node,XPath xpath) {
		if(node == null) {
			return null;
		}
		
		Node n = getFirstElementChild(node);

		if(n == null) {
			return node.getTextContent();
		}

		if(n.getNodeName().equals("random")) {
			try {
				String s = (String)xpath.evaluate("attribute::list",n,XPathConstants.STRING);

				if(s == null || s.equals("")) {
					throw(new RuntimeException("Attribute \"list\" is empty"));
				}

				String[] str = s.split("\\|");

				return str[random.nextInt(str.length)];
			} catch(Exception e) {
				throw(new RuntimeException("Error parsing random attributes",e));
			}
		} else {
			throw(new RuntimeException("Invalid element "+n.getNodeName()));
		}
	}
	
	/**
	 * Tries to instantiate an object from the class defined by the
	 * node's attribute "class". If the given class name is not
	 * fully qualified (i.e., contains no dot), the package of the
	 * given superclass is prefixed to the class name. The class must
	 * be a subclass of the given superclass to succeed. If the class
	 * defines the interface XMLConfigurable, it is configured by calling
	 * configure() with the node as the configuration root.
	 * 
	 * @param superclass the superclass
	 * @param node the node to use for configuration
	 * @param xpath an XPath instance
	 * 
	 * @return the instance
	 * 
	 * @throws Exception
	 */
	
	public static <T> T getInstance(Class<T> superclass,Node node,XPath xpath) throws Exception {
		String className = (String)xpath.evaluate("attribute::class",node,XPathConstants.STRING);

		if(className.indexOf('.') < 0) {
			className = superclass.getName().substring(0,superclass.getName().lastIndexOf('.')+1)+className;
		}
		
		logger.trace("Instantiating class "+className);
		
		Class<?> cl = Class.forName(className);
		
		if(superclass.isAssignableFrom(cl)) {
			T inst = (T)cl.newInstance();

			// configure instance if it is XML-configurable
			
			if(inst instanceof XMLConfigurable) {
				((XMLConfigurable)inst).configure(node,xpath);
			}
			
			return inst;
		}
		else {
			throw(new RuntimeException("Class "+className+" is not a subclass of "+superclass));
		}
	}
}
