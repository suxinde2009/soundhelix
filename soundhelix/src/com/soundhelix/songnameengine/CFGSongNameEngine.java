package com.soundhelix.songnameengine;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Random;

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
    private Map<String,String[]> variableMap;
    
    public String createSongName() {
        Random random = new Random(randomSeed);

        String[] songNames = variableMap.get("songName");

        if (songNames == null || songNames.length == 0) {
            throw new RuntimeException("Variable \"songName\" is undefined or has no values");
        }

        String songName = songNames[random.nextInt(songNames.length)];
        songName = StringUtils.replaceVariables(random, songName, variableMap);
        
        return StringUtils.capitalize(songName);
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
        Random random = new Random(randomSeed);
        
        NodeList nodeList = (NodeList)xpath.evaluate("variable",node,XPathConstants.NODESET);
        int variableCount = nodeList.getLength();

        Map <String,String[]> variableMap = new HashMap<String,String[]>(variableCount);

        for (int i = 0; i < variableCount; i++) {
            String name = XMLUtils.parseString(random,"attribute::name",nodeList.item(i),xpath);

            if (variableMap.containsKey(name)) {
                throw new RuntimeException("Variable \"" + name + "\" defined more than once");
            }
            
            String valueString = XMLUtils.parseString(random,nodeList.item(i),xpath);
            String[] values = valueString.split(",");
            
            variableMap.put(name, values);
        }
        
        setVariableMap(variableMap);
    }

    public Map<String, String[]> getVariableMap() {
        return variableMap;
    }

    public void setVariableMap(Map<String, String[]> variableMap) {
        this.variableMap = variableMap;
    }
}
