package com.soundhelix.util;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.RandomSeedable;
import com.soundhelix.component.XMLConfigurable;
import com.soundhelix.misc.SongContext;

/**
 * Implements some static methods for parsing XML data.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class XMLUtils {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** The factor used for augmenting the random seed (56-bit prime). */
    private static final long RANDOM_SEED_PRIME = 0xFFFFFFFFFFFFC7L;

    /** The ThreadLocal for XPath instances. Makes sure that every thread uses its own XPath instance, as XPath instances are not thread-safe. */
    private static final ThreadLocal<XPath> XPATH = new ThreadLocal<XPath>() {
        @Override protected XPath initialValue() {
            return XPathFactory.newInstance().newXPath();
        }
    };
    
    /**
     * Private constructor.
     */

    private XMLUtils() {
    }
    
    /**
     * Returns the node found at the given path.
     *
     * @param path the XPath expression relative to the node
     * @param node the node
     *
     * @return the node list
     *
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    public static Node getNode(String path, Node node) throws XPathExpressionException {
        return (Node) XPATH.get().evaluate(path, node, XPathConstants.NODE);
    }

    /**
     * Returns the node list found at the given path.
     *
     * @param path the XPath expression relative to the node
     * @param node the node
     *
     * @return the node list
     *
     * @throws XPathExpressionException in case of an XPath expression problem
     */

    public static NodeList getNodeList(String path, Node node) throws XPathExpressionException {
        return (NodeList) XPATH.get().evaluate(path, node, XPathConstants.NODESET);
    }

    /**
     * Returns the first child of the given node that is an element node. If such node doesn't exist, null is returned.
     *
     * @param node the node
     *
     * @return the first element child node
     */

    private static Node getFirstElementChild(Node node) {
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
     * Searches for the element pointed to by path and tries to parse it as an integer.
     *
     * @param random the random generator
     * @param path the XPath expression relative to the node
     * @param node the node
     *
     * @return the integer
     */

    public static int parseInteger(Random random, String path, Node node) {
        try {
            Node node2 = XMLUtils.getNode(path, node);

            if (node2 == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + node.getNodeName());
            }

            return parseInteger(random, node2);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing integer", e);
        }
    }

    /**
     * Tries to parse the text content of the given node as an integer. If it is an integer, the integer is returned. Otherwise, the node is checked
     * for valid subelements, which are then evaluated.
     *
     * @param random the random generator
     * @param node the node to parse
     *
     * @return the integer
     */

    public static int parseInteger(Random random, Node node) {
        try {
            return Integer.parseInt(node.getTextContent());
        } catch (RuntimeException e) {
        }

        XPath xpath = XPATH.get();
        
        Node n = getFirstElementChild(node);

        if (n.getNodeName().equals("random")) {
            try {
                String s = xpath.evaluate("attribute::list", n);

                if (s != null && !s.equals("")) {
                    String[] str = s.split("\\|");

                    return Integer.parseInt(str[random.nextInt(str.length)]);
                } else {
                    int min = Integer.parseInt(xpath.evaluate("attribute::min", n));
                    int max = Integer.parseInt(xpath.evaluate("attribute::max", n));

                    String type = xpath.evaluate("attribute::type", n);

                    if (type == null || type.equals("") || type.equals("uniform")) {
                        int step = 1;

                        try {
                            step = Integer.parseInt(xpath.evaluate("attribute::step", n));
                        } catch (NumberFormatException e) {
                        }

                        return RandomUtils.getUniformInteger(random, min, max, step);
                    } else if (type.equals("normal")) {
                        double mean;
                        String meanstr = xpath.evaluate("attribute::mean", n);

                        if (meanstr != null && !meanstr.equals("")) {
                            mean = Double.parseDouble(meanstr);
                        } else {
                            // use arithmetic mean
                            mean = (min + max) / 2.0f;
                        }

                        double variance = Double.parseDouble(xpath.evaluate("attribute::variance", n));

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
     * Searches for the element pointed to by path and tries to parse it as an integer.
     *
     * @param random the random generator
     * @param path the XPath expression relative to the node
     * @param node the node
     *
     * @return the integer
     */

    public static double parseDouble(Random random, String path, Node node) {
        try {
            Node node2 = XMLUtils.getNode(path, node);

            if (node2 == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + node.getNodeName());
            }

            return XMLUtils.parseDouble(random, node2);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing double", e);
        }
    }

    /**
     * Tries to parse the text content of the given node as an integer. If it is an integer, the integer is returned. Otherwise, the node is checked
     * for valid subelements, which are then evaluated.
     *
     * @param random the random generator
     * @param node the node to parse
     *
     * @return the integer
     */

    public static double parseDouble(Random random, Node node) {
        return Double.parseDouble(node.getTextContent());
    }

    public static boolean parseBoolean(Random random, String path, Node node) {
        try {
            Node node2 = XMLUtils.getNode(path, node);

            if (node2 == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + node.getNodeName());
            }

            return XMLUtils.parseBoolean(random, node2);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing boolean", e);
        }
    }

    /**
     * Tries to parse the text content of the given node as a boolean. If it is a boolean, the boolean. Otherwise, the node is checked for valid
     * subelements, which are then evaluated.
     *
     * @param random the random generator
     * @param node the node to parse
     *
     * @return the boolean
     */

    public static boolean parseBoolean(Random random, Node node) {
        String content = node.getTextContent();

        if (content != null && !content.equals("")) {
            if (content.equals("true")) {
                return true;
            } else if (content.equals("false")) {
                return false;
            }
        }

        Node n = getFirstElementChild(node);

        if (n.getNodeName().equals("random")) {
            try {
                double prob = Double.parseDouble(XPATH.get().evaluate("attribute::probability", n));
                return RandomUtils.getBoolean(random, prob / 100.0d);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing random attributes", e);
            }
        } else {
            throw new RuntimeException("Invalid element " + n.getNodeName());
        }
    }

    /**
     * Searches for the element pointed to by path and tries to parse it as a string.
     *
     * @param random the random generator
     * @param path the XPath expression relative to the node
     * @param node the node
     *
     * @return the integer
     */

    public static String parseString(Random random, String path, Node node) {
        try {
            Node node2 = XMLUtils.getNode(path, node);
            return XMLUtils.parseString(random, node2);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing string", e);
        }
    }

    /**
     * Searches for the element pointed to by path and tries to parse it as a string list, split by the given separator character.
     *
     * @param random the random generator
     * @param path the XPath expression relative to the node
     * @param node the node
     * @param separatorChar the separator character
     *
     * @return the integer
     */

    public static String[] parseStringList(Random random, String path, Node node, char separatorChar) {
        try {
            Node node2 = XMLUtils.getNode(path, node);

            if (node2 == null) {
                throw new RuntimeException("Path \"" + path + "\" not found within node " + node.getNodeName());
            }

            return StringUtils.split(XMLUtils.parseString(random, node2), separatorChar);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing string list", e);
        }
    }

    /**
     * Tries to parse the given node as a string.
     *
     * @param random the random generator
     * @param node the node
     *
     * @return the string (or null)
     */

    public static String parseString(Random random, Node node) {
        if (node == null) {
            return null;
        }

        Node n = getFirstElementChild(node);

        if (n == null) {
            return node.getTextContent();
        }

        if (n.getNodeName().equals("random")) {
            try {
                String s = XPATH.get().evaluate("attribute::list", n);

                if (s == null) {
                    throw new RuntimeException("Attribute \"list\" is undefined");
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

    /**
     * Tries to parse the given node as a string.
     *
     * @param random the random generator
     * @param node the node
     * @param separatorChar the separator character
     *
     * @return the string (or null)
     */

    public static String[] parseStringList(Random random, Node node, char separatorChar) {
        if (node == null) {
            return null;
        }

        Node n = getFirstElementChild(node);

        if (n == null) {
            return StringUtils.split(node.getTextContent(), separatorChar);
        }

        if (n.getNodeName().equals("random")) {
            try {
                String s = XPATH.get().evaluate("attribute::list", n);

                if (s == null || s.equals("")) {
                    throw new RuntimeException("Attribute \"list\" is empty");
                }

                String[] str = StringUtils.split(s, '|');

                return StringUtils.split(str[random.nextInt(str.length)], separatorChar);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing random attributes", e);
            }
        } else {
            throw new RuntimeException("Invalid element " + n.getNodeName());
        }
    }

    public static int[] parseIntegerListString(Random random, String path, Node node) {
        String string = XMLUtils.parseString(random, path, node);

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
     * Tries to instantiate an instance from the class defined by the node's attribute "class" by calling its nullary (empty) constructor. If the
     * given class name is not fully qualified (i.e., contains no dot), the package of the given superclass plus "." is prefixed to the class name
     * (unless the given superclass is an interface, then the package plus ".impl." is prefixed to the class name).  The class must be a subclass of
     * the given class (or implement the interface) to succeed. If the class defines the interface RandomSeedable, it is random-seeded by creating a
     * random seed based on the specified random seed, the class name and the specified modifier. If the class defines the interface XMLConfigurable,
     * it is configured by calling configure() with the node as the configuration root. The given salt value can be overridden by using a "salt" or
     * "seed" attribute.
     *
     * @param songContext the song context
     * @param superclazz the superclass
     * @param node the node to use for configuration
     * @param parentRandomSeed the random seed origin to use (the random seed of the parent component)
     * @param salt the random salt (each instance created by the parent should use a different salt value)
     * @param <T> the type
     *
     * @return the instance
     *
     * @throws InstantiationException if the class cannot be instantiated
     * @throws XPathException in case of an XPath problem
     * @throws ClassNotFoundException if the class cannot be found
     * @throws IllegalAccessException if the class cannot be instantiated
     */

    public static <T> T getInstance(SongContext songContext, Class<T> superclazz, Node node, long parentRandomSeed, int salt)
            throws InstantiationException, XPathException, IllegalAccessException, ClassNotFoundException {
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        
        XPath xpath = XPATH.get();

        String className = xpath.evaluate("attribute::class", node);

        if (className == null) {
            throw new RuntimeException("Attribute \"class\" not defined");
        }

        if (className.indexOf('.') < 0) {
            // determine the superclass' package (including the trailing dot)
            String packageName = superclazz.getName().substring(0, superclazz.getName().lastIndexOf('.') + 1);

            if (superclazz.isInterface()) {
                className = packageName + "impl." + className;
            } else {
                className = packageName + className;
            }
        }

        boolean isSeedProvided = false;
        long providedSeed = 0;

        String seedString = xpath.evaluate("attribute::seed", node);
        String saltString = xpath.evaluate("attribute::salt", node);

        if (seedString != null && !seedString.equals("") && saltString != null && !saltString.equals("")) {
            throw new RuntimeException("Only one of the attributes \"seed\" and \"salt\" may be provided");
        }

        if (seedString != null && !seedString.equals("")) {
            try {
                // take the given number directly as the random seed
                providedSeed = Long.parseLong(seedString);
                isSeedProvided = true;
            } catch (NumberFormatException e) {
                throw new RuntimeException("Seed \"" + seedString + "\" is invalid", e);
            }
        } else if (saltString != null && !saltString.equals("")) {
            try {
                salt = Integer.parseInt(saltString);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Salt \"" + saltString + "\" is invalid", e);
            }
        }

        T instance;

        try {
            instance = ClassUtils.newInstance(className, superclazz);
        } catch (ClassCastException e) {
            throw new RuntimeException("Class " + className + " is not a subclass of " + superclazz, e);
        }

        // random-seed instance if it is random-seedable (it's important to seed before configuring)

        if (instance instanceof RandomSeedable) {
            long randomSeed;

            if (isSeedProvided) {
                randomSeed = providedSeed;
            } else {
                randomSeed = getDerivedRandomSeed(parentRandomSeed, className, salt);
            }

            ((RandomSeedable) instance).setRandomSeed(randomSeed);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Instantiated class " + className + " with random seed " + randomSeed);
            }
        } else {
            LOGGER.trace("Instantiated class " + className + " without a random seed");
        }

        // configure instance if it is XML-configurable

        if (instance instanceof XMLConfigurable) {
            ((XMLConfigurable) instance).configure(songContext, node);
        }

        return instance;
    }

    /**
     * Returns a derived random seed that is based on the parent random seed, the class name and the modifier.
     *
     * @param parentRandomSeed the parent random seed
     * @param className the class name
     * @param modifier the modifier
     *
     * @return the derived random seed
     */

    private static long getDerivedRandomSeed(long parentRandomSeed, String className, int modifier) {
        return parentRandomSeed + getLongHashCode(className) - RANDOM_SEED_PRIME * modifier * Math.abs(modifier);
    }

    /**
     * Returns the hash code the given string as a long. This method works like String.hashCode(), but uses a long internally and returns a long as
     * the result.
     *
     * @param str the string
     *
     * @return the hash code as a long
     */

    private static long getLongHashCode(String str) {
        if (str == null) {
            return 0;
        }

        long hash = 0;
        int len = str.length();

        for (int i = 0; i < len; i++) {
            hash = 31 * hash + str.charAt(i);
        }

        return hash;
    }
}
