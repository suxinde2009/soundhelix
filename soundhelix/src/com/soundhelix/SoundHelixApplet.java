package com.soundhelix;

import java.applet.Applet;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import com.soundhelix.player.Player;
import com.soundhelix.util.SongUtils;

public class SoundHelixApplet extends Applet implements Runnable {

	public static void main(String[] args) {
		new SoundHelixApplet().start();
	}
	
	@Override
	public void start() {
		super.start();
		
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
		Random random = new Random();
	
		for (;;) {
			try {
				InputStream urlInputStream = new URL("http://www.soundhelix.com/examples/SoundHelix-Piano.xml").openConnection().getInputStream();
				
				Player player = SongUtils.generateSong(urlInputStream,random.nextLong());
				player.open();
				player.play();
				player.close();
			} catch (Exception e) {}
			
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
		}
	}
}
