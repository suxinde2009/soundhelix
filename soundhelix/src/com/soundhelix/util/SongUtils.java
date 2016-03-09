package com.soundhelix.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.soundhelix.component.arrangementengine.ArrangementEngine;
import com.soundhelix.component.harmonyengine.HarmonyEngine;
import com.soundhelix.component.player.Player;
import com.soundhelix.component.songnameengine.SongNameEngine;
import com.soundhelix.constants.BuildConstants;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;

/**
 * Provides static convenience methods for creating songs.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class SongUtils {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** Flag indicating whether the XML files should be validated against an XSD schema. */
    private static final boolean ENABLE_SCHEMA_VALIDATION = false;

    /** The XSD schema to use for schema validation. */
    private static final String VALIDATION_SCHEMA_FILENAME = "resources/SoundHelix.xsd";

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

    public static SongContext generateSong(URL url, long randomSeed) throws Exception {
        return generateSong(parseDocument(url), randomSeed);
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

    public static SongContext generateSong(InputStream inputStream, String systemId, long randomSeed) throws Exception {
        return generateSong(parseDocument(inputStream, systemId), randomSeed);
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

    public static SongContext generateSong(URL url, String songName) throws Exception {
        return generateSong(parseDocument(url), songName);
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

    public static SongContext generateSong(InputStream inputStream, String systemId, String songName) throws Exception {
        return generateSong(parseDocument(inputStream, systemId), songName);
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

    private static SongContext generateSong(Document doc, long randomSeed) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, XPathException {
        Node rootNode = XMLUtils.getNode("/*", doc);
        checkVersion(rootNode);

        Node songNameEngineNode = XMLUtils.getNode("songNameEngine", rootNode);
        SongNameEngine songNameEngine = XMLUtils.getInstance(null, SongNameEngine.class, songNameEngineNode, randomSeed, -1);

        String songName = songNameEngine.createSongName();
        LOGGER.info("Song name: \"" + songName + "\"");
        SongContext songContext = generateSongInternal(doc, songName);

        return songContext;
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

    private static SongContext generateSong(Document doc, String songName) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, XPathException {
        checkVersion(doc);

        LOGGER.info("Song name: \"" + songName + "\"");
        SongContext songContext = generateSongInternal(doc, songName);

        return songContext;
    }

    /**
     * 
     * Generates a new song based on the given document and song name and returns the song context, including a pre-configured arrangement and player.
     * 
     * @param doc the document
     * @param songName the song name
     * 
     * @return the player
     * 
     * @throws InstantiationException if a class cannot be instantiated
     * @throws XPathException in case of an XPath problem
     * @throws IllegalAccessException if an illegal class access is made
     * @throws ClassNotFoundException if a class cannot be found
     */

    private static SongContext generateSongInternal(Document doc, String songName) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, XPathException {

        long randomSeed = getSongRandomSeed(songName);

        LOGGER.debug("Rendering new song with random seed " + randomSeed);

        // get the root element of the file (we don't care what it's called)
        Node mainNode = XMLUtils.getNode("/*", doc);

        Node structureNode = XMLUtils.getNode("structure", mainNode);
        Node harmonyEngineNode = XMLUtils.getNode("harmonyEngine", mainNode);
        Node arrangementEngineNode = XMLUtils.getNode("arrangementEngine", mainNode);
        Node playerNode = XMLUtils.getNode("player", mainNode);

        Random random = new Random(randomSeed);

        SongContext songContext = new SongContext();
        songContext.setRandomSeed(Long.valueOf(randomSeed));
        songContext.setSongName(songName);

        Structure structure = parseStructure(random.nextLong(), structureNode, null);
        songContext.setStructure(structure);

        HarmonyEngine harmonyEngine = XMLUtils.getInstance(songContext, HarmonyEngine.class, harmonyEngineNode, randomSeed, 0);
        Harmony harmony = harmonyEngine.render(songContext);
        songContext.setHarmony(harmony);
        HarmonyUtils.checkSanity(songContext);

        ArrangementEngine arrangementEngine = XMLUtils.getInstance(songContext, ArrangementEngine.class, arrangementEngineNode, randomSeed, 1);
        long startTime = System.nanoTime();
        Arrangement arrangement = arrangementEngine.render(songContext);
        songContext.setArrangement(arrangement);
        long time = System.nanoTime() - startTime;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rendering took " + time / 1000000 + " ms");
        }

        Player player = XMLUtils.getInstance(songContext, Player.class, playerNode, randomSeed, 2);
        songContext.setPlayer(player);

        return songContext;
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
        LOGGER.info("Loading XML data from URL \"" + url + "\"");
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
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            Schema schema = factory.newSchema(new StreamSource(new File(VALIDATION_SCHEMA_FILENAME)));

            // validate the DOM tree against the schema

            long time = System.currentTimeMillis();
            schema.newValidator().validate(new DOMSource(doc));
            LOGGER.debug("XML validation took " + (System.currentTimeMillis() - time) + " ms");
        }

        return doc;
    }

    /**
     * Parses the structure tag and creates a Structure instance. Note that no HarmonyEngine is set yet.
     * 
     * @param randomSeed the random seed
     * @param node the node of the tag
     * @param songName the song name
     * 
     * @return a Structure
     */

    private static Structure parseStructure(long randomSeed, Node node, String songName) {
        Random random = new Random(randomSeed);

        int bars = XMLUtils.parseInteger(random, "bars", node);
        int beatsPerBar = XMLUtils.parseInteger(random, "beatsPerBar", node);
        int ticksPerBeat = XMLUtils.parseInteger(random, "ticksPerBeat", node);

        if (bars <= 0) {
            throw new RuntimeException("Number of bars must be > 0");
        }

        if (beatsPerBar <= 0) {
            throw new RuntimeException("Number of beats per bar must be > 0");
        }

        if (ticksPerBeat <= 0) {
            throw new RuntimeException("Number of ticks per beat must be > 0");
        }

        // default value for backwards compatibility
        int maxVelocity = 32767;

        try {
            maxVelocity = XMLUtils.parseInteger(random, "maxVelocity", node);
        } catch (Exception e) {}

        return new Structure(bars, beatsPerBar, ticksPerBeat, maxVelocity);
    }

    /**
     * Determines the root node and calls checkVersion().
     * 
     * @param doc the parsed document
     * 
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    private static void checkVersion(Document doc) throws XPathExpressionException {
        Node rootNode = XMLUtils.getNode("/*", doc);
        checkVersion(rootNode);
    }

    /**
     * Checks if the version of the XML document is compatible with the application version. If the versions are not compatible a RuntimeException
     * will be thrown with an appropriate message. If the application version is undefined ("???"), the check is skipped.
     * 
     * @param rootNode the root node
     * 
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    private static void checkVersion(Node rootNode) throws XPathExpressionException {
        if (BuildConstants.VERSION.equals("???")) {
            // undefined build version is always OK
            return;
        }

        String version = XMLUtils.parseString(null, "@version", rootNode);

        if (version != null && !version.equals("")) {
            if (!VersionUtils.checkVersion(BuildConstants.VERSION, version)) {
                throw new RuntimeException("Application version " + BuildConstants.VERSION + " does not match allowed version(s) \"" + version
                        + "\"");
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
