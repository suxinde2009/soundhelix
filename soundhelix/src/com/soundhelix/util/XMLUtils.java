package com.soundhelix.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.soundhelix.misc.XMLConfigurable;

/**
 * Implements some static methods for parsing XML data.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class XMLUtils {
	private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
	
	private static Random random = new Random();

	private XMLUtils() {}
	
	/**
	 * Returns the first child of the given node that is an element node. If
	 * such node doesn't exist, null is returned.
	 * 
	 * @param the node
	 * 
	 * @return the first element child node
	 */
		
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

	/**
	 * Returns the given node's next element sibling. If such node doesn't exist,
	 * null is returned.
	 * 
	 * @param the node
	 * 
	 * @return the next sibling element node
	 */
	
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
				String s = (String)xpath.evaluate("attribute::list",n,XPathConstants.STRING);

				if(s != null && !s.equals("")) {
					String[] str = s.split("\\|");

					return Integer.parseInt(str[random.nextInt(str.length)]);
				} else {
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
						double mean;
				    	String meanstr = (String)xpath.evaluate("attribute::mean",n,XPathConstants.STRING);
						
						if(meanstr != null && !meanstr.equals("")) {
							mean = Double.parseDouble(meanstr);
						} else {
						    // use arithmetic mean
							mean = (min+max)/2.0f;
						}
						
						double variance = Double.parseDouble((String)xpath.evaluate("attribute::variance",n,XPathConstants.STRING));

						return RandomUtils.getNormalInteger(min,max,mean,variance);
					} else {
						throw(new RuntimeException("Unknown random distribution \""+type+"\""));
					}
				}
			} catch(Exception e) {throw(new RuntimeException("Error parsing random attributes",e));}
		}
		else {
			throw(new RuntimeException("Invalid element "+n.getNodeName()));
		}
	}

	public static boolean parseBoolean(String path,Node parentNode,XPath xpath) {
		try {
			Node node = (Node)xpath.evaluate(path,parentNode,XPathConstants.NODE);

			if(node == null) {
				throw(new RuntimeException("Path \""+path+"\" not found within node "+parentNode.getNodeName()));
			}

			return XMLUtils.parseBoolean(node,xpath);
		} catch(Exception e) {
			throw(new RuntimeException("Error parsing boolean",e));
		}
	}

	/**
	 * Tries to parse the text content of the given node as a boolean.
	 * If it is a boolean, the boolean. Otherwise, the node
	 * is checked for valid subelements, which are then evaluated.
	 * 
	 * @param node the node to parse
	 * @param xpath an XPath instance
	 * 
	 * @return the boolean
	 */

	public static boolean parseBoolean(Node node,XPath xpath) {
		String content = node.getTextContent();
		
		if(content != null && !content.equals("")) {
			if(content.equals("1") || content.equals("yes") || content.equals("true") || content.equals("on")) {
				return true;
			} else if(content.equals("0") || content.equals("no") || content.equals("false") || content.equals("off")) {
				return false;
			}
		}
	
		Node n = getFirstElementChild(node);
	
		if(n.getNodeName().equals("random")) {
			try {
				int prob = Integer.parseInt((String)xpath.evaluate("attribute::probability",n,XPathConstants.STRING));
				return random.nextInt(100) < prob;
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
			// prefix the class name with the package name of the superclass
			className = superclass.getName().substring(0,superclass.getName().lastIndexOf('.')+1)+className;
		}
		
		logger.trace("Instantiating class "+className);
		
		Class<?> cl = Class.forName(className);
		
		if(superclass.isAssignableFrom(cl)) {
			// TODO: any chance of getting rid of the warning here?
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

	public static int expandIncludeTags(Document doc,Node node,XPath xpath) {
		return expandIncludeTags(doc,node,xpath,0);
	}

	/**
	 * Recursively traverses the node and expands any "include" tags it finds
	 * by replacing it with the corresponding contents of the included files.
	 * An included file must contain an XML fragment (plain XML without an XML header and
	 * without a document type) and may contain more than one tag at the
	 * top level. The original include node is replaced by all top-level nodes in the order
	 * within the included file. This method throws a RuntimeException if the total
	 * number of included files exceeds a certain threshold. This is done to
	 * prevent inclusion cycles (file including itself directly or indirectly via
	 * other included files).
	 *
	 * @param doc the document
	 * @param node the node to start searching from (must belong to doc)
	 * @param xpath an XPath instance
	 * @param includedFiles the number of files already included so far
	 *
	 * @return includedFiles increased by the number of included files of this method call (including recursively included files)
	 */
	
	private static int expandIncludeTags(Document doc,Node node,XPath xpath,int includedFiles) {
		NodeList nodeList = null;
		
		try {
		    // recursively search all "include" tags, starting from node
			nodeList = (NodeList)xpath.evaluate("//include",node,XPathConstants.NODESET);
		} catch(Exception e) {}
		
		int nodes = nodeList.getLength();
	
		for(int i=0;i<nodes;i++) {
	        includedFiles++;

	        if(includedFiles > 100) {
	        	throw(new RuntimeException("More than 100 files included (inclusion cycle?)"));
	        }
	            
			Node includeNode = nodeList.item(i);
			
			String filename = XMLUtils.parseString(includeNode,xpath);
			String fileData;

			logger.debug("Including XML fragment \""+filename+"\"");			

			try {
				fileData = readTextFile(filename);
			} catch(Exception e) {
				throw(new RuntimeException("Could not read included file \""+filename+"\"",e));
			}

			DocumentBuilder builder;
			Document includeDoc;
			
			try {
				builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				
				// we wrap the file data into an artificial XML tag "fragment", so that
				// we have a valid XML document having one root element
				
				includeDoc = builder.parse(new InputSource(new StringReader("<fragment>"+fileData+"</fragment>")));
			} catch(Exception e) {
				throw(new RuntimeException("Error parsing XML document \""+filename+"\"",e));
			}
			
			// recursively expand the include tags of the included file (if any)
			includedFiles = expandIncludeTags(includeDoc,includeDoc.getDocumentElement(),xpath,includedFiles);
			
			// import the document to include into the original document tree
			// (without having any location in the tree yet, see importNode())
			// newNode will be a recursively cloned version of the "fragment" tag
			
			Node newNode = doc.importNode(includeDoc.getDocumentElement(),true);
			
			// create a DocumentFragment to hold the child nodes of the
			// "fragment" tag
			
            DocumentFragment docFragment = doc.createDocumentFragment();
    
            // move the children of the "fragment" tag into the fragment
            
            while(newNode.hasChildNodes()) {
                docFragment.appendChild(newNode.removeChild(newNode.getFirstChild()));
            }

            // replace the original "include" node by the document fragment holding the
            // included nodes
            
            includeNode.getParentNode().replaceChild(docFragment,includeNode);
		}
		
		return includedFiles;
	}
	
	/**
	 * Reads the given file and returns its contents as a string. The
	 * file is assumed have standard system encoding.
	 * 
	 * @param filename the filename of the file to read
	 *
	 * @return the file's contents
	 * 
	 * @throws IOException in case of an I/O error
	 */
	
	private static String readTextFile(String filename) throws IOException {
		StringBuilder sb = new StringBuilder(1024);
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		
		// the buffer used for reading
		char[] buf = new char[16384];
		
		int numRead;
		
		while((numRead = reader.read(buf)) != -1){
			sb.append(buf, 0, numRead);
		}
		
		reader.close();
		
		return sb.toString();
	}
}
