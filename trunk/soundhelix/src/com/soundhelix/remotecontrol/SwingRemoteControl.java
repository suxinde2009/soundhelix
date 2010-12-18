package com.soundhelix.remotecontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SwingRemoteControl extends TextRemoteControl {
    JTextField inputTextField;
    JTextArea outputTextArea;
    
    BlockingQueue<String> textQueue = new LinkedBlockingQueue<String>();
    
    private SwingRemoteControl() {
    }

    public SwingRemoteControl(JTextField inputTextField,JTextArea outputTextArea) {
        this.inputTextField = inputTextField;
        this.outputTextArea = outputTextArea;
        
        inputTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField inputTextField = (JTextField) e.getSource();
                textQueue.add(inputTextField.getText());
                inputTextField.setText("");
            }
        });
    }

    public String readLine() {
        try {
            return  textQueue.take();
        } catch(InterruptedException e) {}
        
        return null;
    }
    
    public void writeLine(String line) {
        if (line != null) {
            outputTextArea.append(line + "\n");
            outputTextArea.setCaretPosition(outputTextArea.getText().length());
        }
    }
}
