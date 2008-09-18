package com.soundhelix.misc;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Simple interface that adds XML-configurability to a class. XML
 * configuration is based on DOM.
 *
 * @author Thomas Schürger
 */

public interface XMLConfigurable {
	/**
	 * Configures the instance using the specified XML node. The method
	 * should read all supported configuration tags and should call the
	 * corresponding setter methods of the class. The method should
	 * (but need not) use XPath to access the node's tags.
	 * 
	 * @param node the parent XML node of the configuration tags
	 * @param xpath the XPath instance to use
	 * 
	 * @throws XPathException
	 */
	
	public abstract void configure(Node node,XPath xpath) throws XPathException;
}
