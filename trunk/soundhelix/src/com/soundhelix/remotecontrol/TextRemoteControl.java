package com.soundhelix.remotecontrol;

import java.security.AccessControlException;

import org.apache.log4j.Logger;

import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;

public abstract class TextRemoteControl implements RemoteControl {
	/** The logger. */
    private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

	private Player player;
	
		public void run() {
		    boolean canExit = checkExitPermission(0);
		    writeLine("Can exit: "+canExit);
		    
			while (true) {
				try {
					String line = readLine();

					if (line != null && !line.equals("")) {
    					Player player = this.player;
    
    					if (line.startsWith("bpm ")) {
    						if (player != null) {
    							writeLine("Setting BPM");
    
    							player.setMilliBPM((int)(1000 * Double.parseDouble(line.substring(4))));
    						}
    					} else if (line.startsWith("transposition ")) {
    							if (player != null && player instanceof MidiPlayer) {
    								writeLine("Setting transposition");
    
    								((MidiPlayer)player).setTransposition(Integer.parseInt(line.substring(14)));
    							}
    					} else if (line.startsWith("groove ")) {
    						if (player != null && player instanceof MidiPlayer) {
    							writeLine("Setting groove");
    
    							((MidiPlayer)player).setGroove(line.substring(7));
    						}
    					} else if (line.equals("next")) {
    						if (player != null) {
    							writeLine("Next Song");
    							player.abortPlay();
    						}
                        } else if (canExit && line.equals("quit")) {
                            writeLine("Quitting");
                            System.exit(0);
    					} else if (line.equals("help")) {
    						writeLine("\nAvailable commands");
    						writeLine("------------------\n");
    						writeLine("bpm <value>             Sets the BPM. Example: \"bpm 140\"");
    						writeLine("transposition <value>   Sets the transposition. Example: \"transposition 70\"");
    						writeLine("groove <value>          Sets the groove. Example: \"groove 130,70\"");
    						writeLine("next                    Aborts playing and starts the next song. Example: \"next\"");
                            if (canExit) {
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
					logger.error("Exception in console thread",e);
				}
			}
		}
	
	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	private boolean checkExitPermission(int returnCode) {
        try {
            SecurityManager securitymanager = System.getSecurityManager();

            if(securitymanager != null) {
                securitymanager.checkExit(returnCode);
            }
 
            return true;
        } catch(AccessControlException e) {
            return false;
        }
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
