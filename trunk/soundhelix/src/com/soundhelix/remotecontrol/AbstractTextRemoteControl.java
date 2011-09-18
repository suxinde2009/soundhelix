package com.soundhelix.remotecontrol;

import java.security.AccessControlException;

import org.apache.log4j.Logger;

import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;
import com.soundhelix.util.HarmonyEngineUtils;

/**
 * Implements an abstract simple text-based remote control.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class AbstractTextRemoteControl implements RemoteControl {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** The player. */
    private Player player;
    
    /**
     * Implements the main functionality of the remote control.
     */
    
    public void run() {
        boolean hasExitPermission = hasExitPermission(0);

        while (true) {
            try {
                String line = readLine();

                if (line != null && !line.equals("")) {
                    Player player = this.player;

                    if (line.startsWith("bpm ")) {
                        if (player != null) {
                            writeLine("Setting BPM");

                            player.setMilliBPM((int) (1000 * Double.parseDouble(line.substring(4))));
                        }
                    } else if (line.startsWith("skip ")) {
                        int tick;
                        if (line.endsWith("%")) {
                            double percentage = Double.parseDouble(line.substring(5, line.length() - 1));
                            tick = (int) (percentage * player.getArrangement().getStructure().getTicks() / 100d);
                        } else if (line.substring(5).equals("+")) {
                            tick = player.getCurrentTick();
                            if (tick >= 0) {
                                tick += player.getArrangement().getStructure().getHarmonyEngine().
                                    getChordSectionTicks(tick);
                            }
                        } else if (line.charAt(5) == '#') {
                            int chordSection = Integer.parseInt(line.substring(6));
                            tick = HarmonyEngineUtils.getChordSectionTick(player.getArrangement().getStructure(),
                                    chordSection);
                        } else {
                            tick = Integer.parseInt(line.substring(5));
                        }
                        
                        if (player != null) {
                            boolean success = player.skipToTick(tick);

                            if (success) {
                                writeLine("Skipping to tick " + tick);
                            } else {
                                writeLine("Skipping failed");
                            }
                        }
                    } else if (line.startsWith("transposition ")) {
                        if (player != null && player instanceof MidiPlayer) {
                            writeLine("Setting transposition");
                           
                            ((MidiPlayer) player).setTransposition(Integer.parseInt(line.substring(14)));
                        }
                    } else if (line.startsWith("groove ")) {
                        if (player != null && player instanceof MidiPlayer) {
                            writeLine("Setting groove");

                            ((MidiPlayer) player).setGroove(line.substring(7));
                        }
                    } else if (line.equals("next")) {
                        if (player != null) {
                            writeLine("Next Song");
                            player.abortPlay();
                        }
                    } else if (hasExitPermission && line.equals("quit")) {
                        writeLine("Quitting");
                        System.exit(0);
                    } else if (line.equals("help")) {
                        writeLine("\nAvailable commands");
                        writeLine("------------------\n");
                        writeLine("bpm <value>             Sets the BPM. Example: \"bpm 140\"");
                        writeLine("skip <value>            Skips to the specified tick. Example: \"skip 1000\"");
                        writeLine("skip <value>%           Skips to the specified tick percentage. Example: \"skip 50%\"");
                        writeLine("skip #<value>           Skips to the first tick of the specified chord section. Example: \"skip #3\"");
                        writeLine("skip +                  Skips to the first tick of the next chord section. Example: \"skip +\"");
                        writeLine("transposition <value>   Sets the transposition. Example: \"transposition 70\"");
                        writeLine("groove <value>          Sets the groove. Example: \"groove 130,70\"");
                        writeLine("next                    Aborts playing and starts the next song. Example: \"next\"");
                        if (hasExitPermission) {
                            writeLine("quit                    Quits. Example: \"quit\"");
                        }
                        writeLine("");
                    } else {
                        writeLine("Invalid command. Type \"help\" for help.");
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                LOGGER.error("Exception in console thread", e);
            }
        }
    }

    /**
     * Checks if the JVM environment supports calling System.exit(). Returns true if this
     * is the case, false otherwise.
     * 
     * @param returnCode the return code to consider
     * 
     * @return true if System.exit() is supported, false otherwise
     */

    private boolean hasExitPermission(int returnCode) {
        try {
            SecurityManager securitymanager = System.getSecurityManager();

            if (securitymanager != null) {
                securitymanager.checkExit(returnCode);
            }

            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
    
    /**
     * Returns the next line entered by the user. This method is allowed to block
     * until input is available, or it can return null if there is no input. The returned string
     * must not contain CR or CR and LF at the end.
     *
     * @return the user input string or null if none is available
     */
    public abstract String readLine();
    
    /**
     * Writes a line to the output that is to made visible to the user. The string should not contain
     *
     * @param string the string to write
     */
    
    public abstract void writeLine(String string);
}
