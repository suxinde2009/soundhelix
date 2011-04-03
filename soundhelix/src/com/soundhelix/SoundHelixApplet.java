package com.soundhelix;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
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

import com.soundhelix.player.Player;
import com.soundhelix.remotecontrol.AbstractTextRemoteControl;
import com.soundhelix.remotecontrol.SwingRemoteControl;
import com.soundhelix.util.SongUtils;
import com.soundhelix.util.VersionUtils;

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

    /** The desktop. */
    private Desktop desktop;
    
    /** The remote control. */
    private AbstractTextRemoteControl remoteControl;
    
    /** The text field for the song name. */
    private JTextField songNameTextField;

    /** The Facebook share button. */
    private JButton facebookShareButton;

    /** The Twitter share button. */
    private JButton twitterShareButton;

    /** The YouTube share button. */
    private JButton youTubeShareButton;

    /** The SoundHelix share button. */
    private JButton soundHelixShareButton;

    /** The current song name. */
    private String currentSongName;
    
    /** The song name of the next song. */
    private String nextSongName;

    /** The player. */
    private Player player;
    
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
        makeResizable();

        desktop = getDesktop();
        
        String songName = getParameter("songName");
        
        if (songName != null && !songName.equals("")) {
            nextSongName = songName;
        }
                
        setLayout(new BorderLayout());

        JPanel songNamePanel = new JPanel();
        songNamePanel.setLayout(new BorderLayout());
        songNamePanel.add(new JLabel(" Song name: "), BorderLayout.WEST);
        JTextField songNameTextField = new JTextField();
        songNameTextField.setToolTipText("Enter a song name here (this will generate a new song)");
        songNamePanel.add(songNameTextField, BorderLayout.CENTER);
        this.songNameTextField = songNameTextField;

        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                JPanel panel = new JPanel(new GridLayout(0, 4));
                JButton shareButton;
                
                shareButton = getIconButton("http://www.soundhelix.com/applet/images/facebook-share.png",
                        "Share the current song on Facebook (shows preview)");
                panel.add(shareButton);
                this.facebookShareButton = shareButton;

                shareButton = getIconButton("http://www.soundhelix.com/applet/images/twitter-share.png",
                        "Share the current song on Twitter (shows preview)");
                panel.add(shareButton);
                this.twitterShareButton = shareButton;

                shareButton = getIconButton("http://www.soundhelix.com/applet/images/youtube-share.png",
                        "Visit the SoundHelix channel on YouTube");
                panel.add(shareButton);
                this.youTubeShareButton = shareButton;

                shareButton = getIconButton("http://www.soundhelix.com/applet/images/soundhelix-share.png",
                        "Visit the SoundHelix website");
                panel.add(shareButton);
                this.soundHelixShareButton = shareButton;

                songNamePanel.add(panel, BorderLayout.EAST);
            } catch (MalformedURLException e) {}
        }
        
        add(songNamePanel, BorderLayout.NORTH);
               
        JTextArea outputTextArea = new JTextArea();
        Font font = new Font("Monospaced", Font.PLAIN, 11);
        outputTextArea.setFont(font);
        outputTextArea.setEditable(false);
        
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BorderLayout());
        commandPanel.add(new JLabel(" Command: "), BorderLayout.WEST);
        final JTextField commandTextField = new JTextField();
        commandPanel.add(commandTextField, BorderLayout.CENTER);
        commandTextField.setToolTipText("Enter command here (\"help\" for help)");
        add(commandPanel, BorderLayout.SOUTH);

        commandTextField.requestFocusInWindow();

        songNameTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField textField = (JTextField) e.getSource();
                
                String songName = textField.getText();
                
                if (!songName.equals("")) {
                    nextSongName = songName;
                    player.abortPlay();
                    commandTextField.requestFocusInWindow();
                }                
            }
        });
 
        addUrlActionListener(facebookShareButton, true);
        addUrlActionListener(twitterShareButton, true);
        addUrlActionListener(youTubeShareButton, false);
        addUrlActionListener(soundHelixShareButton, false);

        super.start();

        remoteControl = new SwingRemoteControl(commandTextField, outputTextArea);

        initializeLog4j();

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

    /**
     * Adds an action listener for the given button that opens an external website.
     * 
     * @param button the button
     * @param needsCurrentSongName flag indicating whether the current song name is needed
     */
    
    private void addUrlActionListener(final JButton button, final boolean needsCurrentSongName) {
        if (button != null) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (desktop != null && (!needsCurrentSongName || currentSongName != null)) {
                        try {
                            String url;
                            
                            if (button == facebookShareButton) {
                                url = getFacebookUrl(currentSongName);
                            } else if (button == twitterShareButton) {
                                url = getTwitterUrl(currentSongName);
                            } else if (button == youTubeShareButton) {
                                url = "http://www.youtube.com/SoundHelix/";                                
                            } else {
                                url = "http://www.soundhelix.com";
                            }
                            
                            desktop.browse(new URI(url));
                        } catch (Exception e2) {
                            logger.error("Exception", e2);
                        }
                    }
                }
            });
        }
    }

    /**
     * Creates a JButton with the given icon and the given tool tip.
     * 
     * @param iconUrl the icon URL
     * @param toolTip the tool tip
     * 
     * @return the JButton
     * 
     * @throws MalformedURLException if the URL is malformed
     */
    
    private JButton getIconButton(String iconUrl, String toolTip) throws MalformedURLException {
        JButton shareButton;
        shareButton = new JButton(null, new ImageIcon(new URL(iconUrl)));
        shareButton.setToolTipText(toolTip);
        shareButton.setMargin(new Insets(0, 0, 0, 0));
        return shareButton;
    }

    /**
     * Makes the applet frame resizable.
     */
    
    private void makeResizable() {
        Component parent = this;

        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        
        if (parent instanceof Frame) {
            if (!((Frame) parent).isResizable()) {
                ((Frame) parent).setResizable(true);
            }
        }
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        super.stop();
    }
    
    /**
     * Gets the desktop, if available.
     * 
     * @return the desktop (or null)
     */
    
    private Desktop getDesktop() {
        if (Desktop.isDesktopSupported()) {
            return Desktop.getDesktop();
        } else {
            return null;
        }
    }
    
    /**
     * Generates and returns a Facebook URL for sharing the given song.
     * 
     * @param songName the song name
     * 
     * @return the URL
     */
    
    private static String getFacebookUrl(String songName) {
        return "http://www.facebook.com/sharer.php?u="
            + encode("http://www.soundhelix.com/applet/SoundHelix-applet.jnlp?songName=" + encode(songName));
    }

    /**
     * Generates and returns a Facebook URL for sharing the given song.
     * 
     * @param songName the song name
     * 
     * @return the URL
     */
    
    private static String getTwitterUrl(String songName) {
        String url = "http://www.soundhelix.com/applet/SoundHelix-applet.jnlp?songName=" + encode(songName);
        String text = "Check out this cool song called \"" + songName + "\"! Needs audio and a browser with "
            + "Java 6 plugin.";
        
        // URL shortener creates URL like http://t.co/CmVMQgu
        
        if (text.length() + 22 > 140) {
            text = "Check out this cool song called \"" + songName + "\"!";
            
            if (text.length() + 22 > 140) {
                text = "Check out this cool song!";
            }            
        }
        
        return "http://twitter.com/share?url=" + encode(url) + "&text=" + encode(text);
    }

    private static String encode(String text) {
        try {
            return URLEncoder.encode(text, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    
    /**
     * Implements the player thread functionality.
     */
    
    public void run() {
        VersionUtils.logVersion();
        
        Random random = new Random();
    
        for (;;) {
            try {
                URL url = new URL(getDocumentBase(), "examples/SoundHelix-Piano.xml");
                Player player;
                
                if (nextSongName != null) {
                    player = SongUtils.generateSong(url, nextSongName);
                    nextSongName = null;
                } else {
                    player = SongUtils.generateSong(url, random.nextLong());
                }
                
                this.player = player;
                
                this.currentSongName = player.getArrangement().getStructure().getSongName();
                songNameTextField.setText(this.currentSongName);
                
                player.open();
                remoteControl.setPlayer(player);
                player.play();
                remoteControl.setPlayer(null);
                player.close();
            } catch (Exception e) {
                logger.debug("Exception occurred", e);
                
                try {
                    Thread.sleep(3000);
                } catch (Exception e2) {}
            }            
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
        private AbstractTextRemoteControl remoteControl;
        
        public TextRemoteControlAppender(Layout layout, AbstractTextRemoteControl remoteControl) {
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
