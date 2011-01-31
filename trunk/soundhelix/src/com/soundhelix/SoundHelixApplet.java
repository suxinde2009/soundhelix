package com.soundhelix;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.net.URL;
import java.util.Random;

import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import com.soundhelix.constants.BuildConstants;
import com.soundhelix.player.Player;
import com.soundhelix.remotecontrol.SwingRemoteControl;
import com.soundhelix.remotecontrol.TextRemoteControl;
import com.soundhelix.util.SongUtils;

/**
 * Implements a Swing-based applet for SoundHelix.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class SoundHelixApplet extends JApplet implements Runnable {

    /** The root logger. */
    private static Logger rootLogger = Logger.getRootLogger();
    
    /** The logger. */
    private static Logger logger = Logger.getLogger(SoundHelixApplet.class);

    /** The remote control. */
    private TextRemoteControl remoteControl;
    
    /**
     * Starts the applet.
     * 
     * @param args the arguments
     */
    
    public static void main(String[] args) {
        new SoundHelixApplet().start();
    }
    
    @Override
    public void start() {
        Component parent = this;

        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        
        if (parent instanceof Frame) {
            if (!((Frame) parent).isResizable()) {
                ((Frame) parent).setResizable(true);
            }
        }
        
        setLayout(new BorderLayout());

        JTextArea outputTextArea;
        outputTextArea = new JTextArea();
        Font font = new Font("Monospaced", Font.PLAIN, 11);
        outputTextArea.setFont(font);
        outputTextArea.setEditable(false);
        
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(" Command: "), BorderLayout.WEST);
        JTextField inputTextField = new JTextField();
        inputTextField.requestFocus();
        panel.add(inputTextField, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        super.start();

        remoteControl = new SwingRemoteControl(inputTextField, outputTextArea);

        // launch console thread with normal priority
        Thread consoleThread = new Thread(new Runnable() {
            public void run() {
                remoteControl.run();
            }
        }, "Console");
        
        consoleThread.setPriority(Thread.NORM_PRIORITY);
        consoleThread.start();

        // launch song generation and playing thread with high priority
        Thread playerThread = new Thread(this, "Player");
        playerThread.setPriority(Thread.MAX_PRIORITY);
        playerThread.start();
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        super.stop();
    }
    
    /**
     * Implements the player thread functionality.
     */
    
    public void run() {
        initializeLog4j();

        logger.info("SoundHelix " + BuildConstants.VERSION + " (r" + BuildConstants.REVISION + "), built on "
                  + BuildConstants.BUILD_DATE);

        Random random = new Random();
    
        for (;;) {
            try {
                URL url = new URL(getDocumentBase(), "examples/SoundHelix-Piano.xml");
                Player player = SongUtils.generateSong(url, random.nextLong());
                
                player.open();
                remoteControl.setPlayer(player);
                player.play();
                remoteControl.setPlayer(null);
                player.close();
            } catch (Exception e) {
                logger.debug("Exception occurred", e);
            }
            
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    /**
     * Initializes log4j.
     */
    
    private void initializeLog4j() {
        Layout layout = new PatternLayout("%d{ISO8601} %-5p [%t] %c{1}: %m%n");
        Appender consoleAppender = new TextRemoteControlAppender(layout, remoteControl);
        rootLogger.addAppender(consoleAppender);
        rootLogger.setLevel(Level.DEBUG);
    }
    
    /**
     * Implements a log4j appender that uses the TextRemoteControl for logging.
     */
    
    public static class TextRemoteControlAppender extends AppenderSkeleton {
        /** The layout. */
        private Layout layout;
        
        /** The remote control. */
        private TextRemoteControl remoteControl;
        
        public TextRemoteControlAppender(Layout layout, TextRemoteControl remoteControl) {
            super();
            this.layout = layout;
            this.remoteControl = remoteControl;
        }
        
        public boolean requiresLayout() {
            return true;
        }
        
        public void append(LoggingEvent event) {
            String message = layout.format(event);
            
            if (message.endsWith("\r\n")) {
                message = message.substring(0, message.length() - 2);
            } else if (message.endsWith("\n")) {
                message = message.substring(0, message.length() - 1);                
            }
            
            remoteControl.writeLine(message);

            if (layout.ignoresThrowable()) {
                String[] s = event.getThrowableStrRep();
                if (s != null) {
                    int len = s.length;
                    for (int i = 0; i < len; i++) {
                        remoteControl.writeLine(s[i]);
                    }
                }
            }
        }
        
        public void close() {
        }
    }
}
