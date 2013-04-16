package com.soundhelix.misc;

import java.util.HashMap;
import java.util.Map;

import com.soundhelix.component.player.Player;

/**
 * Defines the context of a song. During song generation, the context is enriched with information.
 * Each of the fields can only be set to at most once.
 * 
 * Custom attributes can be set in the song context to pass custom data along between components.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SongContext {
    /** The song's random seed (null if a song is generated by supplying a song name). */
    private Long randomSeed;

    /** The song name. */
    private String songName;

    /** The structure. */
    private Structure structure;

    /** The harmony. */
    private Harmony harmony;

    /** The activity matrix. */
    private ActivityMatrix activityMatrix;

    /** The arrangement. */
    private Arrangement arrangement;
    
    /** The player. */
    private Player player;

    /** The attribute map (initialized lazily). */
    private Map<String, Object> attributeMap;
    
    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        if (this.randomSeed != null) {
            throw new IllegalArgumentException("randomSeed already set");
        }
        
        this.randomSeed = randomSeed;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        if (this.songName != null) {
            throw new IllegalArgumentException("songName already set");
        }
        
        this.songName = songName;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        if (this.structure != null) {
            throw new IllegalArgumentException("structure already set");
        }

        this.structure = structure;
    }

    public Harmony getHarmony() {
        return harmony;
    }

    public void setHarmony(Harmony harmony) {
        if (harmony == null) {
            throw new RuntimeException("harmony already set");
        }

        this.harmony = harmony;
    }

    public ActivityMatrix getActivityMatrix() {
        return activityMatrix;
    }

    public void setActivityMatrix(ActivityMatrix activityMatrix) {
        if (this.activityMatrix != null) {
            throw new IllegalArgumentException("activityMatrix already set");
        }

        this.activityMatrix = activityMatrix;
    }

    public Arrangement getArrangement() {
        return arrangement;
    }

    public void setArrangement(Arrangement arrangement) {
        if (this.arrangement != null) {
            throw new IllegalArgumentException("arrangement already set");
        }
        
        this.arrangement = arrangement;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        if (this.player != null) {
            throw new IllegalArgumentException("player already set");
        }

        this.player = player;
    }
    
    /**
     * Returns the attribute with the given name. If the attribute doesn't exist or it's value is null, null is returned.
     * 
     * @param name the attribute name
     *
     * @return the attribute value
     */
    
    public Object getAttribute(String name) {
        return getAttributeMap().get(name);
    }

    /**
     * Sets the attribute to the given value. If the attribute is already set, it is replaced with the new value.
     * 
     * @param name the attribute name
     * @param value the attribute value
     */
    
    public void setAttribute(String name, Object value) {
        getAttributeMap().put(name, value);
    }

    /**
     * Checks if the given attribute has been set. Can be used to distinguish null values from non-existing values.
     * 
     * @param name the attribute name
     *
     * @return true if the attribute is set, false otherwise
     */
    
    public boolean hasAttribute(String name) {
        return getAttributeMap().containsKey(name);
    }

    /**
     * Removes the given attribute. If the attribute is not set, nothing is done.
     * 
     * @param name the attribute name
     */
    
    public void removeAttribute(String name) {
        getAttributeMap().remove(name);
    }

    /**
     * Returns the attribute map. The map is created if it doesn't exist yet.
     * 
     * @return the attribute map
     */
    
    private Map<String, Object> getAttributeMap() {
        if (attributeMap == null) {
            attributeMap = new HashMap<String, Object>();
        }
        
        return attributeMap;
    }    
}
