package com.soundhelix.remotecontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Implements a remote control bases on Swing.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SwingRemoteControl extends AbstractTextRemoteControl {
    /** The output text area. */
    private JTextArea outputTextArea;

    /** The queue for entered texts lines. */
    private BlockingQueue<String> textQueue = new LinkedBlockingQueue<String>();

    /**
     * Constructor.
     */

    @SuppressWarnings("unused")
    private SwingRemoteControl() {
    }

    /**
     * Constructor.
     * 
     * @param inputTextField the input text field
     * @param outputTextArea the output text field
     */

    public SwingRemoteControl(JTextField inputTextField, JTextArea outputTextArea) {
        this.outputTextArea = outputTextArea;

        inputTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField inputTextField = (JTextField) e.getSource();
                textQueue.add(inputTextField.getText());
                inputTextField.setText("");
            }
        });
    }

    @Override
    public String readLine() {
        try {
            return textQueue.take();
        } catch (InterruptedException e) {}

        return null;
    }

    @Override
    public void writeLine(String line) {
        if (line != null) {
            outputTextArea.append(line + "\n");
            outputTextArea.setCaretPosition(outputTextArea.getText().length());
        }
    }
}
