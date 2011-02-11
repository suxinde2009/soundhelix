package com.soundhelix.util;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Implements some static methods for parsing XML data.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class XMLUtils {
	/** The logger. */
	private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
	
	private XMLUtils() {}
	
	/**
	 * Returns the first child of the given node that is an element node. If
	 * such node doesn't exist, null is returned.
	 * 
	 * @param node the node
	 * 
	 * @return the first element child node
	 */
		
	public static Node getFirstElementChild(Node node) {
		node = node.getFirstChild();
		
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return node;
			}			
			node = node.getNextSibling();
		}
		
		return null;	
	}

	/**
	 * Returns the given node's next element sibling. If such node doesn't exist,
	 * null is returned.
	 * 
	 * @param node the node
	 * 
	 * @return the next sibling element node
	 */
	
	public static Node getNextElementSibling(Node node) {
		while (node != null) {
			node = node.getNextSibling();
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return node;
			}
		}
		
		return null;	
	}

	/**
	 * Searches for the element pointed to by path and tries to parse
	 * it as an integer.
	 * 
	 * @param random the random generator
	 * @param path the XPath expression
	 * @param parentNode the parent node to start searching from
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */
	
	public static int parseInteger(Random random, String path, Node parentNode, XPath xpath) {
		try {
			Node node = (Node) xpath.evaluate(path, parentNode, XPathConstants.NODE);

			if (node == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + parentNode.getNodeName());
			}

			return XMLUtils.parseInteger(random, node, xpath);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing integer", e);
		}
	}

	/**
	 * Tries to parse the text content of the given node as an integer.
	 * If it is an integer, the integer is returned. Otherwise, the node
	 * is checked for valid subelements, which are then evaluated.
	 * 
	 * @param random the random generator
	 * @param node the node to parse
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */

	public static int parseInteger(Random random, Node node, XPath xpath) {
	    try {
	        return Integer.parseInt(node.getTextContent());
	    } catch (RuntimeException e) {}

		Node n = getFirstElementChild(node);
	
		if (n.getNodeName().equals("random")) {
			try {
				String s = (String) xpath.evaluate("attribute::list", n, XPathConstants.STRING);

				if (s != null && !s.equals("")) {
					String[] str = s.split("\\|");

					return Integer.parseInt(str[random.nextInt(str.length)]);
				} else {
                    int min = Integer.parseInt((String) xpath.evaluate("attribute::min", n, XPathConstants.STRING));
                    int max = Integer.parseInt((String) xpath.evaluate("attribute::max", n, XPathConstants.STRING));

                    String type = (String) xpath.evaluate("attribute::type", n, XPathConstants.STRING);

					if (type == null || type.equals("") || type.equals("uniform")) {
						int step = 1;

						try {
                            step = Integer.parseInt((String) xpath.evaluate("attribute::step",
							                        n, XPathConstants.STRING));
						} catch (NumberFormatException e) {
						}

						return RandomUtils.getUniformInteger(random, min, max, step);
					} else if (type.equals("normal")) {
						double mean;
                        String meanstr = (String) xpath.evaluate("attribute::mean", n, XPathConstants.STRING);
						
						if (meanstr != null && !meanstr.equals("")) {
							mean = Double.parseDouble(meanstr);
						} else {
						    // use arithmetic mean
							mean = (min + max) / 2.0f;
						}
						
						double variance = Double.parseDouble(
                                          (String) xpath.evaluate("attribute::variance", n, XPathConstants.STRING));

						return RandomUtils.getNormalInteger(random, min, max, mean, variance);
					} else {
                        throw new RuntimeException("Unknown random distribution \"" + type + "\"");
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Error parsing random attributes", e);
			}
		} else {
			throw new RuntimeException("Invalid element " + n.getNodeName());
		}
	}

	/**
	 * Searches for the element pointed to by path and tries to parse
	 * it as an integer.
	 * 
	 * @param random the random generator
	 * @param path the XPath expression
	 * @param parentNode the parent node to start searching from
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */
	
	public static double parseDouble(Random random, String path, Node parentNode, XPath xpath) {
		try {
			Node node = (Node) xpath.evaluate(path, parentNode, XPathConstants.NODE);

			if (node == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + parentNode.getNodeName());
			}

			return XMLUtils.parseDouble(random, node, xpath);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing double", e);
		}
	}

	/**
	 * Tries to parse the text content of the given node as an integer.
	 * If it is an integer, the integer is returned. Otherwise, the node
	 * is checked for valid subelements, which are then evaluated.
	 * 
	 * @param random the random generator
	 * @param node the node to parse
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */

	public static double parseDouble(Random random, Node node, XPath xpath) {
		return Double.parseDouble(node.getTextContent());
	}
	
	public static boolean parseBoolean(Random random, String path, Node parentNode, XPath xpath) {
		try {
			Node node = (Node) xpath.evaluate(path, parentNode, XPathConstants.NODE);

			if (node == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + parentNode.getNodeName());
			}

			return XMLUtils.parseBoolean(random, node, xpath);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing boolean", e);
		}
	}

	/**
	 * Tries to parse the text content of the given node as a boolean.
	 * If it is a boolean, the boolean. Otherwise, the node
	 * is checked for valid subelements, which are then evaluated.
	 * 
	 * @param random the random generator
	 * @param node the node to parse
	 * @param xpath an XPath instance
	 * 
	 * @return the boolean
	 */

	public static boolean parseBoolean(Random random, Node node, XPath xpath) {
		String content = node.getTextContent();
		
		if (content != null && !content.equals("")) {
            if (content.equals("1") || content.equals("yes") || content.equals("true") || content.equals("on")) {
				return true;
			} else if (content.equals("0") || content.equals("no")
			           || content.equals("false") || content.equals("off")) {
				return false;
			}
		}
	
		Node n = getFirstElementChild(node);
	
		if (n.getNodeName().equals("random")) {
			try {
				double prob = Double.parseDouble(
                              (String) xpath.evaluate("attribute::probability", n, XPathConstants.STRING));
				return RandomUtils.getBoolean(random, prob / 100.0d);
			} catch (Exception e) {
				throw new RuntimeException("Error parsing random attributes", e);
			}
		} else {
			throw new RuntimeException("Invalid element " + n.getNodeName());
		}
	}

	/**
	 * Searches for the element pointed to by path and tries to parse
	 * it as a string.
	 * 
	 * @param random the random generator
	 * @param path the XPath expression
	 * @param parentNode the parent node to start searching from
	 * @param xpath an XPath instance
	 * 
	 * @return the integer
	 */
	
	public static String parseString(Random random, String path, Node parentNode, XPath xpath) {
		try {
			Node node = (Node) xpath.evaluate(path, parentNode, XPathConstants.NODE);

			if (node == null) {
				return null;
			}

			return XMLUtils.parseString(random, node, xpath);
		} catch (Exception e) {
			throw new RuntimeException("Error parsing string", e);
		}
	}
	
	/**
	 * Tries to parse the given node as a string.
	 * 
	 * @param random the random generator
	 * @param node the node
	 * @param xpath an XPath instance
	 * 
	 * @return the string (or null)
	 */

	public static String parseString(Random random, Node node, XPath xpath) {
		if (node == null) {
			return null;
		}
		
		Node n = getFirstElementChild(node);

		if (n == null) {
			return node.getTextContent();
		}

		if (n.getNodeName().equals("random")) {
			try {
				String s = (String) xpath.evaluate("attribute::list", n, XPathConstants.STRING);

				if (s == null || s.equals("")) {
					throw new RuntimeException("Attribute \"list\" is empty");
				}

				String[] str = StringUtils.split(s, '|');

				return str[random.nextInt(str.length)];
			} catch (Exception e) {
				throw new RuntimeException("Error parsing random attributes", e);
			}
		} else {
			throw new RuntimeException("Invalid element " + n.getNodeName());
		}
	}
	
	public static int[] parseIntegerListString(Random random, String path, Node parentNode, XPath xpath) {
		String string = XMLUtils.parseString(random, path, parentNode, xpath);
    	
    	if (string == null || string.equals("")) {
    		return null;
    	}

    	String[] stringArray = string.split(",");
    	int length = stringArray.length;
    	
    	
		int[] intArray = new int[length];
    	
    	for (int i = 0; i < length; i++) {
    		intArray[i] = Integer.parseInt(stringArray[i]);
    	}
    	
    	return intArray;
	}
	
	/**
	 * Tries to instantiate an object from the class defined by the
	 * node's attribute "class". If the given class name is not
	 * fully qualified (i.e., contains no dot), the package of the
	 * given superclass is prefixed to the class name. The class must
	 * be a subclass of the given class to succeed. If the class
	 * defines the interface XMLConfigurable, it is configured by calling
	 * configure() with the node as the configuration root. If the class
	 * defines the interface RandomSeedable, it is random-seeded by using
	 * the specified random seed and the class name.
	 * 
	 * @param clazz the class
	 * @param node the node to use for configuration
	 * @param xpath an XPath instance
	 * @param randomSeed the random seed to use
	 * @param <T> the type
	 * 
	 * @return the instance
	 * 
	 * @throws InstantiationException if the class cannot be instantiated
	 * @throws XPathException in case of an XPath problem
	 * @throws ClassNotFoundException if the class cannot be found
	 * @throws IllegalAccessException if the class cannot be instantiated
	 */
	
	public static <T> T getInstance(Class<T> clazz, Node node, XPath xpath, long randomSeed)
        throws InstantiationException, XPathException,
				   IllegalAccessException, ClassNotFoundException {
		if (node == null) {
			throw new IllegalArgumentException("Node is null");
		}
		
		String className = (String) xpath.evaluate("attribute::class", node, XPathConstants.STRING);

		if (className.indexOf('.') < 0) {
			// prefix the class name with the package name of the superclass
			className = clazz.getName().substring(0, clazz.getName().lastIndexOf('.') + 1) + className;
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace("Instantiating class " + className);
		}

		T instance;
		
		try {
			instance = ClassUtils.newInstance(className, clazz);
		} catch (ClassCastException e) {
			throw new RuntimeException("Class " + className + " is not a subclass of " + clazz, e);
		}
		
		// random-seed instance if it is random-seedable
		// (it's important to seed before configuring)

		if (instance instanceof RandomSeedable) {
			if (logger.isTraceEnabled()) {
				logger.trace("Base random seed: " + randomSeed + ", using "
                        + (randomSeed ^ className.hashCode() - 1478923845823984391L * randomSeed));
			}
			
			((RandomSeedable) instance).setRandomSeed(randomSeed ^ className.hashCode());
		}

		// configure instance if it is XML-configurable

		if (instance instanceof XMLConfigurable) {
			((XMLConfigurable) instance).configure(node, xpath);
		}

		return instance;
	}
}
