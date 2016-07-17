package com.brackeen.app.audio;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/**
 * AudioStreams are created when playback starts, and can be used to modify volume/pan, or stop the
 * playback early.
 * @see AudioBuffer#play()
 */
public class AudioStream {
    private static byte[] blankData = new byte[4096];

    private final AudioFormat format;
    private final SourceDataLine line;

    private AudioBuffer buffer;
    private boolean loop = false;
    private float volume;
    private float pan;

    private int playbackPos;
    private float playbackVolume;
    private float playbackPan;

    AudioStream(AudioFormat format, SourceDataLine line) {
        this.format = format;
        this.line = line;
    }

    boolean isAvailable() {
        synchronized (this) {
            return buffer == null;
        }
    }

    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets the linear volume of the line from 0 to 1.
     */
    public void setVolume(float volume) {
        this.volume = volume;
    }

    /**
     * Sets the pan from -1 (left channel) to 0 (center) to 1 (right channel).
     */
    public void setPan(float pan) {
        this.pan = pan;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    void play(AudioBuffer buffer, float volume, float pan, boolean loop) {
        synchronized (this) {
            this.buffer = buffer;
            this.volume = volume;
            this.pan = pan;
            this.loop = loop;
            this.playbackPos = 0;
            this.playbackVolume = Float.MIN_VALUE;
            this.playbackPan = Float.MIN_VALUE;
        }
        AudioEngine.notifySoundAdded();
    }

    public void stop() {
        synchronized (this) {
            if (buffer != null) {
                loop = false;
                playbackPos = buffer.data.length;
            }
        }
    }

    //region Private methods called on the audio thread

    void close() {
        if (line.isOpen()) {
            // TODO: This might cause a glitch if audio is currently playing?
            line.close();
        }
    }

    void tick() {
        if (buffer == null) {
            return;
        }
        synchronized (this) {
            if (buffer == null) {
                return;
            }
            if (!line.isActive()) {
                line.start();
            }
            float currVolume = volume * AudioEngine.getMasterVolume();
            if (playbackVolume != currVolume) {
                playbackVolume = currVolume;
                float gainDB = (float) (20 * Math.log10(playbackVolume));
                setControlValue(FloatControl.Type.MASTER_GAIN, gainDB);
            }
            if (playbackPan != pan) {
                playbackPan = pan;
                setControlValue(FloatControl.Type.PAN, pan);
            }

            if (playbackPos < buffer.data.length) {
                // Keep 32ms of data in the buffer
                final int maxFrames = (int)Math.ceil(format.getFrameRate() * 0.032f);
                final int maxLength = maxFrames * format.getFrameSize();
                final int currLength = line.getBufferSize() - line.available();
                if (currLength < maxLength) {
                    final int length = Math.min(maxLength - currLength, buffer.data.length - playbackPos);
                    final int bytesWritten = line.write(buffer.data, playbackPos, length);
                    if (bytesWritten > 0) {
                        playbackPos += bytesWritten;
                        if (loop && playbackPos == buffer.data.length) {
                            playbackPos = 0;
                        }
                    }
                }
            }
            if (playbackPos >= buffer.data.length) {
                // Fill entire buffer of blank data, which ensures the audio data is drained.
                int bytesToWrite = line.available();
                if (blankData.length < bytesToWrite) {
                    blankData = new byte[bytesToWrite];
                }
                final int bytesWritten = line.write(blankData, 0, bytesToWrite);
                if (bytesWritten > 0) {
                    playbackPos += bytesWritten;
                }

                if (playbackPos >= buffer.data.length + line.getBufferSize()) {
                    buffer = null;
                    line.stop();
                    line.flush();
                }
            }
        }
    }

    private void setControlValue(FloatControl.Type type, float value) {
        FloatControl control = null;
        try {
            control = (FloatControl) line.getControl(type);
        } catch (Exception ex) {
            // Unsupported control type
        }
        if (control != null) {
            if (line.isActive()) {
                control.shift(control.getValue(), value, 50);
            } else {
                control.setValue(value);
            }
        }
    }

    // endregion
}
