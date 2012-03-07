package com.soundhelix.remotecontrol;

import java.io.Console;

/**
 * Implements a remote control that uses the console.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ConsoleRemoteControl extends AbstractTextRemoteControl {
    /** The console. */
    private Console console;

    public ConsoleRemoteControl() {    
        console = System.console();
        
        if (console == null) {
            throw new RuntimeException("No console available");
        }  
    }
    
    @Override
    public String readLine() {
        return console.readLine();
    }
    
    @Override
    public void writeLine(String line) {
        System.out.println(line);
    }
}
