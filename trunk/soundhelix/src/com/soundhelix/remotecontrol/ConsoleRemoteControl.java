package com.soundhelix.remotecontrol;

import java.io.Console;

public class ConsoleRemoteControl extends TextRemoteControl {
    /** The console. */
    private Console console;

    public ConsoleRemoteControl() {    
        console = System.console();
        
        if (console == null) {
            throw new RuntimeException("No console available");
        }  
    }
    
    public String readLine() {
        return console.readLine();
    }
    
    public void writeLine(String line) {
        System.out.println(line);
    }
}
