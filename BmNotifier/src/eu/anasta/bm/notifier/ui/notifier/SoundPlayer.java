package eu.anasta.bm.notifier.ui.notifier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;



/**
 * Class for caching images
 * 
 * @author Emil
 */
public class SoundPlayer {



    // what path to get to the "icons" directory without actually including it
    private static final String           SOUND_ROOT_PATH = "/sound/";

    public static void playSound() {
    	playSound(null);
    }
    /**
     * 
     * @param fileName Filename of image to fetch
     * @return Image null if it could not be found
     */
    public static void playSound(String fileName) {
    	if (fileName==null){
    		fileName="Notify.wav";
    	}
        fileName = SOUND_ROOT_PATH + fileName;
        ClassLoader classLoader = SoundPlayer.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            // the old way didn't have leading slash, so if we can't find the image stream,
            // let's see if the old way works.
            is = classLoader.getResourceAsStream(fileName.substring(1));

            if (is == null) {
                is = classLoader.getResourceAsStream(fileName);
                if (is == null) {
                    is = classLoader.getResourceAsStream(fileName.substring(1));
                    if (is == null) { return ; }
                }
            }
        }
        try {
			playClip(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    private static void playClip(InputStream clipFile) throws IOException, 
    UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
    class AudioListener implements LineListener {
      private boolean done = false;
      @Override public synchronized void update(LineEvent event) {
        Type eventType = event.getType();
        if (eventType == Type.STOP || eventType == Type.CLOSE) {
          done = true;
          notifyAll();
        }
      }
      public synchronized void waitUntilDone() throws InterruptedException {
        while (!done) { wait(); }
      }
    }
    AudioListener listener = new AudioListener();
    InputStream bufferedIn = new BufferedInputStream(clipFile);
    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
    try {
      Clip clip = AudioSystem.getClip();
      clip.addLineListener(listener);
      clip.open(audioInputStream);
      try {
        clip.start();
        listener.waitUntilDone();
      } finally {
        clip.close();
      }
    } finally {
      audioInputStream.close();
      bufferedIn.close();
      clipFile.close();
    }
  }
}
