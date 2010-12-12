package com.soundhelix;

import java.awt.Font;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Random;

import javax.swing.JApplet;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import com.soundhelix.player.Player;
import com.soundhelix.util.SongUtils;

public class SoundHelixApplet extends JApplet implements Runnable {

	private static Logger rootLogger = Logger.getRootLogger();
	private static Logger logger = Logger.getLogger(SoundHelixApplet.class);

	private JTextArea area;
	
	public static void main(String[] args) {
		new SoundHelixApplet().start();
	}
	
	@Override
	public void start() {
		super.start();
		
		area = new JTextArea();
		Font font = new Font("Monospaced", Font.PLAIN, 11);
        area.setFont(font);

		add(new JScrollPane(area));

		// launch song generation thread with low priority
		Thread t = new Thread(this,"Player");
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
		
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}
	
	public void run() {
		Layout layout = new PatternLayout("%d{ISO8601} %-5p [%t] %c{1}: %m%n");
		Appender consoleAppender = new JTextAreaAppender(layout, area);
		rootLogger.addAppender(consoleAppender);
		rootLogger.setLevel(Level.DEBUG);
		
		Random random = new Random();
	
		for (;;) {
			try {
				logger.debug("Reading XML file");
				InputStream urlInputStream = new URL(getDocumentBase(), "examples/SoundHelix-Piano.xml").openConnection().getInputStream();
				//InputStream urlInputStream = new URL("file:/examples/SoundHelix-Piano.xml").openConnection().getInputStream();
				
				Player player = SongUtils.generateSong(urlInputStream,random.nextLong());
				logger.debug("Opening player");
				player.open();
				logger.debug("Playing");
				player.play();
				logger.debug("Closing player");
				player.close();
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				logger.debug("Exception occurred:\n" + sw.toString());
			}
			
			try {
				Thread.sleep(3000);
			} catch (Exception e) {}
		}
	}
	
	public static class JTextAreaAppender extends AppenderSkeleton {
		private Layout layout;
		private JTextArea textArea;
		
		public JTextAreaAppender(Layout layout,JTextArea textArea) {
			super();
			this.layout = layout;
			this.textArea = textArea;
		}
		
		public boolean requiresLayout() {
			return true;
		}
		
		public void append(LoggingEvent event) {
			String message = layout.format(event);
			textArea.append(message);
			textArea.setCaretPosition(textArea.getText().length());
		}
		
		public void close() {
		}
	}
}
