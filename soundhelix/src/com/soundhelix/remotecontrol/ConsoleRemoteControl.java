package com.soundhelix.remotecontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Implements a remote control that uses the console.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ConsoleRemoteControl extends AbstractTextRemoteControl {
    /** The console reader. */
    private BufferedReader consoleReader;

    /**
     * Constructor.
     */
    public ConsoleRemoteControl() {
        try {
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            throw new RuntimeException("Could not open console");
        }
    }

    @Override
    public String readLine() {
        try {
            return consoleReader.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void writeLine(String line) {
        System.out.println(line);
    }
}
