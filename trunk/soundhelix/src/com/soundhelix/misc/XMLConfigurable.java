package com.soundhelix.misc;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Simple interface that adds XML-configurability to a class. XML configuration is based on DOM and XPath.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public interface XMLConfigurable {
	/**
	 * Configures the instance using the specified XML node. The method
	 * should read all supported configuration tags and should call the
	 * corresponding setter methods of the class. All settings of the
	 * class that are settable using XML should also be settable by
	 * using public methods (for example, if the beats per minute are
	 * settable using an XML tag, then the class should also provide
	 * a public method setBPM(), which does the same). XML configuration
	 * can also be a recursive process, if XML settings contain nodes
	 * that require XML configuration themselves, but this is up to the
	 * implementation of this method. The method should (but need not)
	 * use XPath to access the node's tags.
	 * 
	 * @param node the parent XML node of the configuration tags
	 * @param xpath the XPath instance to use
	 * 
	 * @throws XPathException
	 */
	
	void configure(Node node,XPath xpath) throws XPathException;
}
