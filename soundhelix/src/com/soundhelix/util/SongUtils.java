package com.soundhelix.util;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.soundhelix.arrangementengine.ArrangementEngine;
import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;
import com.soundhelix.player.Player;

public class SongUtils {
	/** The logger. */
	private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    private static final boolean ENABLE_SCHEMA_VALIDATION = false;
	private static final String VALIDATION_SCHEMA_FILENAME = "SoundHelix.xsd";

	/**
	 * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures
	 * the player to use this arrangement.
	 * 
	 * @param inputStream the input stream
	 * @param randomSeed the random seed
	 */
	
	public static Player generateSong(InputStream inputStream,long randomSeed) throws Exception {
		
		logger.debug("Rendering new song");
		
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(true);
        dbf.setValidating(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        
        if (ENABLE_SCHEMA_VALIDATION) {
            // create a SchemaFactory capable of understanding WXS schemas
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(new File(VALIDATION_SCHEMA_FILENAME)));

            // validate the DOM tree against the schema
            
            try {
                schema.newValidator().validate(new DOMSource(doc));
            } catch (SAXException e) {
                throw(e);
                // instance document is invalid!
            }
        }
        
		XPath xpath = XPathFactory.newInstance().newXPath();

		// get the root element of the file (we don't care what it's called)
		Node mainNode = (Node)xpath.evaluate("/*",doc,XPathConstants.NODE);

		Node structureNode = (Node)xpath.evaluate("structure",mainNode,XPathConstants.NODE);
		Node harmonyEngineNode = (Node)xpath.evaluate("harmonyEngine",mainNode,XPathConstants.NODE);
		Node arrangementEngineNode = (Node)xpath.evaluate("arrangementEngine",mainNode,XPathConstants.NODE);
		Node playerNode = (Node)xpath.evaluate("player",mainNode,XPathConstants.NODE);

        logger.debug("Using song random seed " + randomSeed);
        Random random = new Random(randomSeed);

		Structure structure = parseStructure(random,structureNode,xpath);
	
		HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class,
					harmonyEngineNode,xpath,randomSeed ^ 47357892832l);
		structure.setHarmonyEngine(harmonyEngine);	

		ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class,
					arrangementEngineNode,xpath,randomSeed ^ 123454893l);
		arrangementEngine.setStructure(structure);
        long startTime = System.nanoTime();
		Arrangement arrangement = arrangementEngine.render();
		long time = System.nanoTime() - startTime;

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering took " + (time / 1000000) + " ms");
		}
		
		Player player = XMLUtils.getInstance(Player.class,playerNode,xpath,randomSeed ^ 5915925127l);
		player.setArrangement(arrangement);
		return player;
	}
	
	/**
	 * Parses the structure tag and creates a Structure instance. Note that
	 * no HarmonyEngine is set yet.
	 * 
	 * @param random the random generator
	 * @param node the node of the tag
	 * @param xpath an XPath instance
	 * 
	 * @return a Structure
	 * 
	 * @throws XPathException
	 */
	
	private static Structure parseStructure(Random random,Node node,XPath xpath) throws XPathException {
		int bars = XMLUtils.parseInteger(random,"bars",node,xpath);
		int beatsPerBar = XMLUtils.parseInteger(random,"beatsPerBar",node,xpath);
		int ticksPerBeat = XMLUtils.parseInteger(random,"ticksPerBeat",node,xpath);

		if (bars <= 0) {
			throw(new RuntimeException("Number of bars must be > 0"));
		}
		
		if (beatsPerBar <= 0) {
			throw(new RuntimeException("Number of beats per bar must be > 0"));
		}
		
		if (ticksPerBeat <= 0) {
			throw(new RuntimeException("Number of ticks per beat must be > 0"));
		}
		
		Structure structure = new Structure(bars,beatsPerBar,ticksPerBeat);

		return structure;
	}
}
