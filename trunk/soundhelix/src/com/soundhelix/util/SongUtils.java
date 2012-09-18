package com.soundhelix.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.soundhelix.constants.BuildConstants;
import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;
import com.soundhelix.player.Player;
import com.soundhelix.songnameengine.SongNameEngine;

/**
 * Provides static convenience methods for creating a song.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class SongUtils {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** Flag indicating whether the XML files should be validated against an XSD schema. */
    private static final boolean ENABLE_SCHEMA_VALIDATION = false;

    /** The XSD schema to use for schema validation. */
    private static final String VALIDATION_SCHEMA_FILENAME = "SoundHelix.xsd";

    /**
     * Private constructor.
     */

    private SongUtils() {
    }

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures the player to use this arrangement.
     *
     * @param url the URL
     * @param randomSeed the random seed
     *
     * @return the player
     *
     * @throws Exception in case of a problem
     */

    public static Player generateSong(URL url, long randomSeed) throws Exception {
        Document doc = parseDocument(url);
        return createPlayer(doc, randomSeed);
    }

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures the player to use this arrangement.
     *
     * @param inputStream the input stream
     * @param systemId the system ID specifying the source of the input stream
     * @param randomSeed the random seed
     *
     * @return the player
     *
     * @throws Exception in case of a problem
     */

    public static Player generateSong(InputStream inputStream, String systemId, long randomSeed) throws Exception {
        Document doc = parseDocument(inputStream, systemId);
        return createPlayer(doc, randomSeed);
    }

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures the player to use this arrangement.
     *
     * @param url the URL
     * @param songName the song name
     *
     * @return the player
     *
     * @throws Exception in case of a problem
     */

    public static Player generateSong(URL url, String songName) throws Exception {
        Document doc = parseDocument(url);
        return createPlayer(doc, songName);
    }

    /**
     * Parses the XML file provided by the given input stream, creates an arrangement and a player and configures the player to use this arrangement.
     *
     * @param inputStream the input stream
     * @param systemId the system ID specifying the source of the input stream
     * @param songName the song name
     *
     * @return the player
     *
     * @throws Exception in case of a problem
     */

    public static Player generateSong(InputStream inputStream, String systemId, String songName) throws Exception {
        Document doc = parseDocument(inputStream, systemId);
        return createPlayer(doc, songName);
    }

    /**
     * Creates a SongNameEngine, generates a song and creates a Player from it.
     *
     * @param doc the parsed document
     * @param randomSeed the random seed
     *
     * @return the player
     *
     * @throws ClassNotFoundException if the class cannot be found
     * @throws IllegalAccessException if access is illegal
     * @throws InstantiationException if the class cannot be instantiated
     * @throws XPathException in case of an XPath error
     */

    private static Player createPlayer(Document doc, long randomSeed) throws InstantiationException, XPathException, IllegalAccessException,
            ClassNotFoundException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node rootNode = (Node) xpath.evaluate("/*", doc, XPathConstants.NODE);
        checkVersion(rootNode, xpath);

        Node songNameEngineNode = (Node) xpath.evaluate("songNameEngine", rootNode, XPathConstants.NODE);
        SongNameEngine songNameEngine = XMLUtils.getInstance(SongNameEngine.class, songNameEngineNode, xpath, randomSeed, -1);

        String songName = songNameEngine.createSongName();
        LOGGER.info("Song name: \"" + songName + "\"");
        Player player = generateSong(doc, getSongRandomSeed(songName));

        // store the song name
        player.getArrangement().getStructure().setSongName(songName);
        return player;
    }

    /**
     * Creates a player using the specified song name.
     *
     * @param doc the parsed document
     * @param songName the song name
     *
     * @return the player
     *
     * @throws ClassNotFoundException if the class cannot be found
     * @throws IllegalAccessException if access is illegal
     * @throws InstantiationException if the class cannot be instantiated
     * @throws XPathException in case of an XPath error
     */

    private static Player createPlayer(Document doc, String songName) throws InstantiationException, XPathException, IllegalAccessException,
            ClassNotFoundException {
        checkVersion(doc);

        LOGGER.info("Song name: \"" + songName + "\"");
        Player player = generateSong(doc, getSongRandomSeed(songName));

        // store the song name
        player.getArrangement().getStructure().setSongName(songName);
        return player;
    }

    /**
     *
     * Generates a new song based on the given document and random seed and returns the pre-configured player that can be used to play the song.
     *
     * @param doc the document
     * @param randomSeed the random seed
     *
     * @return the player
     *
     * @throws InstantiationException if a class cannot be instantiated
     * @throws XPathException in case of an XPath problem
     * @throws IllegalAccessException if an illegal class access is made
     * @throws ClassNotFoundException if a class cannot be found
     */

    private static Player generateSong(Document doc, long randomSeed) throws InstantiationException, XPathException, IllegalAccessException,
            ClassNotFoundException {

        LOGGER.debug("Rendering new song with random seed " + randomSeed);

        XPath xpath = XPathFactory.newInstance().newXPath();

        // get the root element of the file (we don't care what it's called)
        Node mainNode = (Node) xpath.evaluate("/*", doc, XPathConstants.NODE);

        Node structureNode = (Node) xpath.evaluate("structure", mainNode, XPathConstants.NODE);
        Node harmonyEngineNode = (Node) xpath.evaluate("harmonyEngine", mainNode, XPathConstants.NODE);
        Node arrangementEngineNode = (Node) xpath.evaluate("arrangementEngine", mainNode, XPathConstants.NODE);
        Node playerNode = (Node) xpath.evaluate("player", mainNode, XPathConstants.NODE);

        Random random = new Random(randomSeed);

        Structure structure = parseStructure(random.nextLong(), structureNode, xpath, null);
        structure.setRandomSeed(randomSeed);

        HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class, harmonyEngineNode, xpath, randomSeed, 0);
        structure.setHarmonyEngine(harmonyEngine);

        ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class, arrangementEngineNode, xpath, randomSeed, 1);
        arrangementEngine.setStructure(structure);
        long startTime = System.nanoTime();
        Arrangement arrangement = arrangementEngine.render();
        long time = System.nanoTime() - startTime;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rendering took " + (time / 1000000) + " ms");
        }

        Player player = XMLUtils.getInstance(Player.class, playerNode, xpath, randomSeed, 2);
        player.setArrangement(arrangement);
        return player;
    }

    /**
     * Parses the XML document provided by the given URL. The URL is used as the system ID (i.e., base URL) of the document, so relative URLs
     * referenced in the XML document (e.g., for XInclude inclusion), will use the document URL as the base.
     *
     * @param url the URL
     *
     * @return the parsed document
     *
     * @throws ParserConfigurationException in case of a parsing exception
     * @throws SAXException if a SAX exception occurs
     * @throws IOException in case of an I/O problem
     */

    private static Document parseDocument(URL url) throws ParserConfigurationException, SAXException, IOException {
        LOGGER.debug("Loading XML data from URL \"" + url + "\"");
        return parseDocument(url.openStream(), url.toExternalForm());
    }

    /**
     * Parses the XML document provided by the input stream. The system ID is used as the base URL of the document, so relative URLs referenced in the
     * XML document (e.g., for XInclude inclusion), will use the document URL as the base.
     *
     * @param inputStream the input stream
     * @param systemId the system ID of the XML content
     *
     * @return the parsed document
     *
     * @throws ParserConfigurationException in case of a parsing exception
     * @throws SAXException if a SAX exception occurs
     * @throws IOException in case of an I/O problem
     */

    private static Document parseDocument(InputStream inputStream, String systemId) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(true);
        dbf.setValidating(false);

        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc;

        doc = builder.parse(inputStream, systemId);

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
     * Parses the structure tag and creates a Structure instance. Note that no HarmonyEngine is set yet.
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
     * Determines the root node and calls checkVersion().
     *
     * @param doc the parsed document
     *
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    private static void checkVersion(Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node rootNode = (Node) xpath.evaluate("/*", doc, XPathConstants.NODE);
        checkVersion(rootNode, xpath);
    }

    /**
     * Checks if the version of the XML document is compatible with the application version. If the versions are not compatible a RuntimeException
     * will be thrown with an appropriate message. If the application version is undefined ("???"), the check is skipped.
     *
     * @param xpath the XPath instance
     * @param rootNode the root node
     *
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    private static void checkVersion(Node rootNode, XPath xpath) throws XPathExpressionException {
        if (BuildConstants.VERSION.equals("???")) {
            return;
        }

        String version = (String) xpath.evaluate("attribute::version", rootNode, XPathConstants.STRING);

        if (version != null && !version.equals("")) {
            if (version.endsWith("+")) {
                // strip off "+" character
                version = version.substring(0, version.length() - 1);

                if (VersionUtils.compareVersions(BuildConstants.VERSION, version) < 0) {
                    throw new RuntimeException("Document requires at least version " + version + ", but application version is "
                            + BuildConstants.VERSION);
                }
            } else {
                if (VersionUtils.compareVersions(BuildConstants.VERSION, version) != 0) {
                    throw new RuntimeException("Document requires exactly version " + version + ", but application version is "
                            + BuildConstants.VERSION);
                }
            }
        }
    }

    /**
     * Returns the random seed for the song title.
     *
     * @param title the song tile
     *
     * @return the random seed
     */

    public static long getSongRandomSeed(String title) {
        return StringUtils.getLongHashCode(title.trim().toLowerCase());
    }
}
