package com.soundhelix.remotecontrol;

import java.io.Console;

import org.apache.log4j.Logger;

import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;

public class ConsoleRemoteControl implements RemoteControl {
	/** The logger. */
	private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

	private Player player;
	
		public void run() {
			Console console = System.console();
			
			if (console == null) {
				logger.debug("Exiting console thread, because no console is available");
				return;
			}
			
			while (true) {
				try {
					String line = console.readLine();

					Player player = this.player;

					if (line.startsWith("bpm ")) {
						if (player != null) {
							System.out.println("Setting BPM");

							player.setMilliBPM((int)(1000 * Double.parseDouble(line.substring(4))));
						}
					} else if (line.startsWith("transposition ")) {
							if (player != null && player instanceof MidiPlayer) {
								System.out.println("Setting transposition");

								((MidiPlayer)player).setTransposition(Integer.parseInt(line.substring(14)));
							}
					} else if (line.startsWith("groove ")) {
						if (player != null && player instanceof MidiPlayer) {
							System.out.println("Setting groove");

							((MidiPlayer)player).setGroove(line.substring(7));
						}
					} else if (line.equals("next")) {
						if (player != null) {
							System.out.println("Next Song");
							player.abortPlay();
						}
					} else if (line.equals("help")) {
						System.out.println("\nAvailable commands");
						System.out.println("------------------\n");
						System.out.println("bpm <value>             Sets the BPM. Example: \"bpm 140\"");
						System.out.println("transposition <value>   Sets the transposition. Example: \"transposition 70\"");
						System.out.println("groove <value>          Sets the groove. Example: \"groove 130,70\"");
						System.out.println("next                    Aborts playing and starts the next song. Example: \"next\"");
						System.out.println();
					} else {
						System.out.println("Invalid command. Type \"help\" for help.");
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
}
