package com.soundhelix.remotecontrol;

/**
 * Extension to the RemoteControl which allows terminal-style communication, e.g., allowing input of lines by the user and allowing output of lines by
 * the system.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface TextRemoteControl extends RemoteControl {
    /**
     * Returns the next line entered by the user. This method is allowed to block until input is available, or it can return null if there is no
     * input. The returned string must contain the net line content only (without any line separator(s) at the end).
     * 
     * @return the user input string or null if none is available
     */

    String readLine();

    /**
     * Writes a line to the output that is to made visible to the user. The string should not contain CR or CR and LF at the end.
     * 
     * @param string the string to write
     */

    void writeLine(String string);
}
