package com.brackeen.app;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class BufferedAudio {
    
    public static class Stream {
        
        private final Clip clip;
        
        private Stream(Clip clip) {
            this.clip = clip;
        }
        
        private void setControlValue(FloatControl.Type type, float value) {
            FloatControl control = null;
            try {
                control = (FloatControl)clip.getControl(type);
            }
            catch (Exception ex) { 
                // Unsupported control type?
            }
            if (control != null) {
                if (clip.isActive()) {
                    control.shift(control.getValue(), value, 50);
                }
                else {
                    control.setValue(value);
                }
            }
        }
        
        /**
        Sets the linear volume of the line from 0 to 1.
        */
        public void setVolume(float volume) {
            float gainDB = (float)(20 * Math.log10(volume));
            setControlValue(FloatControl.Type.MASTER_GAIN, gainDB);
        }

        /**
        Sets the pan from -1 (left channel) to 0 (center) to 1 (right channel).
        */
        public void setPan(float pan) {
            setControlValue(FloatControl.Type.PAN, pan);
        }
        
        public boolean isPlaying() {
            return clip.isActive();
        }
        
        public void stop() {
            clip.stop();
            clip.setFramePosition(0);
        }
        
        public void pause() {
            clip.stop();
        }
        
        public void play() {
            clip.loop(0);
        }
        
        public void loop() {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    
    public static BufferedAudio read(URL url, int maxSimultaneousCopies) throws IOException {
        if (url == null || maxSimultaneousCopies <= 0) {
            return null;
        }
        
        List<Stream> streams = new ArrayList<Stream>(maxSimultaneousCopies);
        for (int i = 0; i < maxSimultaneousCopies; i++) {
            Clip clip = null;
            try {
                clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(url));
            }
            catch (UnsupportedAudioFileException ex) {
                throw new IOException(ex);
            }            
            catch (LineUnavailableException ex) {
                throw new IOException(ex);
            }
            if (clip != null) {
                streams.add(new Stream(clip));
            }
        }
        
        return new BufferedAudio(streams);
    }
  
    private final List<Stream> streams;
    
    private BufferedAudio(List<Stream> streams) {
        this.streams = streams;
    }
    
    private Stream getUnusedStream() {
        for (Stream stream : streams) {
            if (!stream.isPlaying()) {
                return stream;
            }
        }
        return null;
    }
    
    public Stream play(float volume, float pan, boolean loop) {
        Stream stream = getUnusedStream();
        if (stream != null) {
            stream.stop();
            stream.setVolume(volume);
            stream.setPan(pan);
            if (loop) {
                stream.loop();
            }
            else {
                stream.play();
            }
        }
        return stream;
    }
    
    public Stream play() {
        return play(1, 0, false);
    }
    
    public Stream loop() {
        return play(1, 0, true);
    }
    
    public void dispose() {
        // Note: this causes some audio distortion on Mac. But it has to be done to get free 
        // audio resources back.
        // No distortion occurs on Windows.
        for (Stream stream : streams) {
            stream.clip.close();
        }
    }
}
