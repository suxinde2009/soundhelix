package com.soundhelix.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
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
import com.soundhelix.songnameengine.SongNameEngine;

public class SongUtils {
    /** The logger. */
    private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** Flag indicating whether the XML files should be validated against an XSD schema. */
    private static final boolean ENABLE_SCHEMA_VALIDATION = false;
    
    /** The XSD schema to use for schema validation. */
    private static final String VALIDATION_SCHEMA_FILENAME = "SoundHelix.xsd";

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures
     * the player to use this arrangement.
     * 
     * @param url the URL
     * @param randomSeed the random seed
     * 
     * @return the player
     */
    
    public static Player generateSong(URL url, long randomSeed) throws Exception {
        Document doc = parseDocument(url);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node mainNode = (Node) xpath.evaluate("/*", doc, XPathConstants.NODE);
        Node songNameEngineNode = (Node) xpath.evaluate("songNameEngine", mainNode, XPathConstants.NODE);
        SongNameEngine songNameEngine = XMLUtils.getInstance(SongNameEngine.class,
                songNameEngineNode, xpath, randomSeed ^ 12345);
        
        String songName = songNameEngine.createSongName();
        logger.info("Song name: \"" + songName + "\"");        
        return generateSong(doc, getSongRandomSeed(songName));
    }

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures
     * the player to use this arrangement.
     * 
     * @param url the URL
     * @param songName the song name
     * 
     * @return the player
     */
    
    public static Player generateSong(URL url, String songName) throws Exception {
        Document doc = parseDocument(url);
        logger.info("Song name: \"" + songName + "\"");        
        return generateSong(doc, getSongRandomSeed(songName));
    }

    /**
     * Generates a new song based on the given document and random seed and returns the pre-configured player that
     * can be used to play the song.
     * 
     * @param doc the document
     * @param randomSeed the random seed
     *
     * @return the player
     */
    
    private static Player generateSong(Document doc, long randomSeed)
            throws XPathExpressionException, InstantiationException,
            XPathException, IllegalAccessException, ClassNotFoundException {

        logger.debug("Rendering new song");
        
        XPath xpath = XPathFactory.newInstance().newXPath();

        // get the root element of the file (we don't care what it's called)
        Node mainNode = (Node) xpath.evaluate("/*", doc, XPathConstants.NODE);

        Node structureNode = (Node) xpath.evaluate("structure", mainNode, XPathConstants.NODE);
        Node harmonyEngineNode = (Node) xpath.evaluate("harmonyEngine", mainNode, XPathConstants.NODE);
        Node arrangementEngineNode = (Node) xpath.evaluate("arrangementEngine",  mainNode, XPathConstants.NODE);
        Node playerNode = (Node) xpath.evaluate("player", mainNode, XPathConstants.NODE);

        logger.debug("Using song random seed " + randomSeed);
        Random random = new Random(randomSeed);

        Structure structure = parseStructure(random.nextLong(), structureNode, xpath, null);
    
        HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class,
                    harmonyEngineNode, xpath, randomSeed ^ 47357892832l);
        structure.setHarmonyEngine(harmonyEngine);    

        ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class,
                    arrangementEngineNode, xpath, randomSeed ^ 123454893l);
        arrangementEngine.setStructure(structure);
        long startTime = System.nanoTime();
        Arrangement arrangement = arrangementEngine.render();
        long time = System.nanoTime() - startTime;

        if (logger.isDebugEnabled()) {
            logger.debug("Rendering took " + (time / 1000000) + " ms");
        }
        
        Player player = XMLUtils.getInstance(Player.class, playerNode, xpath, randomSeed ^ 5915925127l);
        player.setArrangement(arrangement);
        return player;
    }

    /**
     * Parses the XML document provided by the given URL. The URL is used as the system ID (i.e., base URL) of the
     * document, so relative URLs referenced in the XML document (e.g., for XInclude inclusion), will use the document
     * URL as the base.
     * 
     * @param url the URL
     * 
     * @return the parsed document
     */
    
    private static Document parseDocument(URL url) throws ParserConfigurationException, SAXException, IOException {

        logger.debug("Loading XML data from URL \"" + url + "\"");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(true);
        dbf.setValidating(false);
        
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc;
        
        doc = builder.parse(url.openStream(), url.toExternalForm());
        
        if (ENABLE_SCHEMA_VALIDATION) {
            // create a SchemaFactory capable of understanding WXS schemas
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(new File(VALIDATION_SCHEMA_FILENAME)));

            // validate the DOM tree against the schema
            
            try {
                schema.newValidator().validate(new DOMSource(doc));
            } catch (SAXException e) {
                throw e;
            }
        }
        
        return doc;
    }
    
    /**
     * Parses the structure tag and creates a Structure instance. Note that
     * no HarmonyEngine is set yet.
     * 
     * @param randomSeed the random seed
     * @param node the node of the tag
     * @param xpath an XPath instance
     * @param songName the song name
     * 
     * @return a Structure
     */
    
    private static Structure parseStructure(long randomSeed, Node node, XPath xpath, String songName) {
        Random random = new Random(randomSeed);
        
        int bars = XMLUtils.parseInteger(random, "bars", node, xpath);
        int beatsPerBar = XMLUtils.parseInteger(random, "beatsPerBar", node, xpath);
        int ticksPerBeat = XMLUtils.parseInteger(random, "ticksPerBeat", node, xpath);

        if (bars <= 0) {
            throw new RuntimeException("Number of bars must be > 0");
        }
        
        if (beatsPerBar <= 0) {
            throw new RuntimeException("Number of beats per bar must be > 0");
        }
        
        if (ticksPerBeat <= 0) {
            throw new RuntimeException("Number of ticks per beat must be > 0");
        }

        Structure structure = new Structure(bars, beatsPerBar, ticksPerBeat, songName);

        return structure;
    }
    
    /**
     * Returns the random seed for the song title.
     * 
     * @param title the song tile
     * 
     * @return the random seed
     */
    
    private static long getSongRandomSeed(String title) {
        return title.trim().toLowerCase().hashCode();
    }
}
