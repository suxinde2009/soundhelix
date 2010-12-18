package com.soundhelix.remotecontrol;

import java.io.Console;

import org.apache.log4j.Logger;

import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;

public class ConsoleRemoteControl extends TextRemoteControl {
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
