package com.soundhelix.remotecontrol;

import java.security.AccessControlException;

import org.apache.log4j.Logger;

import com.soundhelix.component.player.Player;
import com.soundhelix.component.player.impl.MidiPlayer;
import com.soundhelix.misc.Structure;
import com.soundhelix.util.HarmonyEngineUtils;

/**
 * Implements an abstract simple text-based remote control.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractTextRemoteControl implements TextRemoteControl {
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
                    } else if (line.startsWith("skip ") || line.equals("+")) {
                        if (player != null) {
                            int tick;
                            Structure structure = player.getArrangement().getStructure();

                            if (line.endsWith("%")) {
                                double percentage = Double.parseDouble(line.substring(5, line.length() - 1));
                                tick = (int) (percentage * structure.getTicks() / 100d);
                            } else if (line.equals("+") || line.substring(5).equals("+")) {
                                tick = player.getCurrentTick();

                                if (tick >= 0) {
                                    tick += structure.getHarmonyEngine().getChordSectionTicks(tick);
                                }
                            } else if (line.charAt(5) == '#') {
                                double chordSectionDouble = Double.parseDouble(line.substring(6));

                                int chordSection = (int) chordSectionDouble;
                                double chordSectionFraction = chordSectionDouble - Math.floor(chordSectionDouble);

                                // use the integer part to find the start of the chord section
                                tick = HarmonyEngineUtils.getChordSectionTick(structure, chordSection);

                                // add the fractional part
                                tick += (int) (structure.getHarmonyEngine().getChordSectionTicks(tick) * chordSectionFraction);
                            } else {
                                tick = Integer.parseInt(line.substring(5));
                            }

                            if (tick < 0 || tick > structure.getTicks()) {
                                writeLine("Invalid tick to skip to selected");
                            } else {
                                boolean success = player.skipToTick(tick);

                                if (success) {
                                    int chordSection = HarmonyEngineUtils.getChordSectionNumber(structure, tick);

                                    if (chordSection >= 0) {
                                        int chordSectionTick = tick - HarmonyEngineUtils.getChordSectionTick(structure, chordSection);
                                        writeLine("Skipping to tick " + tick + " (tick " + chordSectionTick + " of chord section " + chordSection + ")");
                                    } else {
                                        writeLine("Skipping to tick " + tick);
                                    }
                                } else {
                                    writeLine("Skipping failed");
                                }
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
                        writeLine("skip <value>%           Skips to the specified tick percentage. Example: \"skip 50.8%\"");
                        writeLine("skip #<value>           Skips to the specified chord section. "
                                + "Example: \"skip #3.5\" (skips to the middle of chord section 3)");
                        writeLine("skip +                  Skips to the first tick of the next chord section. "
                                + "Example: \"skip +\". Short form: \"+\"");
                        writeLine("transposition <value>   Sets the transposition. Example: \"transposition 70\"");
                        writeLine("groove <value>          Sets the groove pattern. Example: \"groove 130,70\"");
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
     * Checks if the JVM environment supports calling System.exit(). Returns true if this is the case, false otherwise.
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
}
