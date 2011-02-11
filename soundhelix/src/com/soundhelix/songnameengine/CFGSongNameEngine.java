package com.soundhelix.songnameengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.util.StringUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a song name engine based on a context-free grammar.
 *
 * @author Thomas Schürger
 */

public class CFGSongNameEngine extends AbstractSongNameEngine {
    /** The pattern for variable replacement. */
    private static Pattern variablePattern = Pattern.compile("\\$\\{(.*?)\\}");
    
    /** The variable map. */
    private Map<String, RandomStringArray> variableMap;
    
    /** The start variable. */
    private String startVariable = "songName";
    
    /** The separator. */
    private char stringSeparator = ',';
    
    public String createSongName() {
        Random random = new Random(randomSeed);

        RandomStringArray rsa = variableMap.get(startVariable);

        if (rsa == null || rsa.strings.length == 0) {
            throw new RuntimeException("Variable \"" + startVariable + "\" is undefined or has no values");
        }

        String songName = getRandomString(random, rsa);
        songName = replaceVariables(random, songName, variableMap);
        
        return StringUtils.capitalize(songName);
    }
    
    public void configure(Node node, XPath xpath) throws XPathException {
        Random random = new Random(randomSeed);
        
        NodeList nodeList = (NodeList) xpath.evaluate("variable", node, XPathConstants.NODESET);
        int variableCount = nodeList.getLength();

        Map<String, RandomStringArray> variableMap = new HashMap<String, RandomStringArray>(variableCount);

        for (int i = 0; i < variableCount; i++) {
            String name = XMLUtils.parseString(random, "attribute::name", nodeList.item(i), xpath);
            boolean once;
            
            try {
                once = XMLUtils.parseBoolean(random, "attribute::once", nodeList.item(i), xpath);
            } catch (Exception e) {
                once = true;
            }

            // TODO: merge the variable definitions instead
            
            if (variableMap.containsKey(name)) {
                throw new RuntimeException("Variable \"" + name + "\" defined more than once");
            }
            
            String valueString = XMLUtils.parseString(random, nodeList.item(i), xpath);
            String[] values = StringUtils.split(valueString, stringSeparator);
            
            variableMap.put(name, new RandomStringArray(values, once));
        }
        
        setVariableMap(variableMap);
    }

    /**
     * Recursively replaces all variables in the given string by a randomly chosen value from the corresponding variable
     * map values.
     * 
     * @param random the random generator to use
     * @param string the string to perform replacements in
     * @param variableMap the variable map
     * 
     * @return the string with all variables replaced
     */
     
    public static String replaceVariables(Random random, String string, Map<String, RandomStringArray> variableMap) {
        if (string.indexOf('$') < 0) {
            // string contains no variables, return it unchanged
            return string;
        }
        
        Matcher matcher = variablePattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");  
            RandomStringArray rsa = variableMap.get(matcher.group(1));
            
            if (rsa == null) {
                throw new RuntimeException("Variable \"" + matcher.group(1) + "\" is invalid");
            }
            
            sb.append(getRandomString(random, rsa));  
        }

        matcher.appendTail(sb);  
        
        return replaceVariables(random, sb.toString(), variableMap);
    }

    /**
     * Returns a random string from the given RandomStringArray.
     * 
     * @param random the random generator to use
     * @param randomList the RandomList
     *
     * @return the random string
     */
    
    private static String getRandomString(Random random, RandomStringArray randomList) {
        if (randomList.selectOnce) {
            String[] strings = randomList.strings;
            int remaining = randomList.remaining;
            
            if (remaining <= 0) {
                remaining = strings.length;
            }
            
            int offset = random.nextInt(remaining);
            remaining--;
            
            String value = strings[offset];
            String otherValue = strings[remaining];
            
            strings[remaining] = value;
            strings[offset] = otherValue;

            randomList.remaining = remaining;
            
            return value;
        } else {
            return randomList.strings[random.nextInt(randomList.strings.length)];
        }
    }
    
    public Map<String, RandomStringArray> getVariableMap() {
        return variableMap;
    }

    public void setVariableMap(Map<String, RandomStringArray> variableMap) {
        this.variableMap = variableMap;
    }
    
    private static class RandomStringArray {
        private String[] strings;
        private int remaining;
        private boolean selectOnce = true;
        
        public RandomStringArray(String[] strings, boolean selectOnce) {
            this.strings = strings;
            this.selectOnce = selectOnce;
        }
    }
}
