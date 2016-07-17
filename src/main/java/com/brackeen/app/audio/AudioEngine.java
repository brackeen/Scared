package com.brackeen.app.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * An alternative to using the Java Sound API's Clip, which have the following issues:
 * * Only 32 Clips can be loaded on some systems.
 * * Volume/pan changes can't happen in real-time because of Clip's large internal buffer.
 *
 * Features of this audio engine:
 * * Unlimited AudioBuffers can be loaded (limited only by memory).
 * * Real-time volume/pan changes.
 * * Up to 40 simultaneous playback streams.
 * * Low-latency.
 * * Looping.
 *
 * Notes:
 * * Audio files are loaded into memory.
 * * All audio must have the same sample rate.
 * * All 1-channel (mono) audio files are converted to stereo.
 * * No mp3/ogg support.
 *
 * Simple Example:
 *     AudioEngine.init(44100);
 *     ...
 *     AudioBuffer audioBuffer = AudioBuffer.read(fileUrl);
 *     audioBuffer.play();
 *     ...
 *     AudioEngine.destroy();
 *
 * Real-time changes example:
 *     AudioEngine.init(44100);
 *     ...
 *     AudioBuffer audioBuffer = AudioBuffer.read(fileUrl);
 *     AudioStream stream = audioBuffer.play();
 *     ...
 *     stream.setPan(-1.0f);
 *     ...
 *     stream.setVolume(0.5f);
 *     ...
 *     AudioEngine.destroy();
 */
public class AudioEngine {
    private static final int MAX_SIMULTANEOUS_SOUNDS = 40;

    private static Context context;
    private static float masterVolume = 1.0f;

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static void setMasterVolume(float masterVolume) {
        AudioEngine.masterVolume = masterVolume;
    }

    /**
     * Initializes the audio engine. The method must be called before playing audio.
     * @param frameRate The audio frame rate. Typically supported: 8000, 11025, 22050, or 44100.
     *                  All audio files will be played at this rate, regardless of the audio files'
     *                  internal frame rate.
     */
    public static void init(float frameRate) {
        if (context == null) {
            context = new Context(frameRate);
            context.start();
        }
    }

    public static void destroy() {
        if (context != null) {
            context.destroy();
            context = null;
        }
    }

    static AudioStream getAvailableStream() {
        if (context == null) {
            throw new IllegalStateException("AudioEngine.init not called.");
        }
        return context.getAvailableStream();
    }

    static void notifySoundAdded() {
        final Context currContext = context;
        if (currContext != null) {
            currContext.notifySoundAdded();
        }
    }

    private static class Context implements Runnable {
        private final Object stateLock = new Object();
        private final Object updateLock = new Object();

        private Thread renderThread;
        private final AudioFormat audioFormat;
        private final AtomicBoolean running = new AtomicBoolean(true);

        private Mixer mixer;
        private final List<AudioStream> streams = new ArrayList<>();

        private Context(float frameRate) {
            audioFormat = new AudioFormat(frameRate, 16, 2, true, false);
        }

        private void start() {
            synchronized (stateLock) {
                renderThread = new Thread(this, "AudioEngine");
                renderThread.setDaemon(true);
                renderThread.setPriority(Thread.MAX_PRIORITY);
                renderThread.start();
                try {
                    stateLock.wait(1000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        }

        private void destroy() {
            synchronized (stateLock) {
                running.set(false);
                try {
                    stateLock.wait(1000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        }

        private AudioStream getAvailableStream() {
            for (AudioStream stream : streams) {
                if (stream.isAvailable()) {
                    return stream;
                }
            }
            return null;
        }

        private void notifySoundAdded() {
            synchronized (updateLock) {
                updateLock.notify();
            }
        }

        //region Methods called on the audio thread

        @Override
        public void run() {
            // Create mixer
            try {
                mixer = AudioSystem.getMixer(null);
            } catch (IllegalArgumentException ex) {
                Mixer.Info[] mixerInfoArray = AudioSystem.getMixerInfo();
                for (Mixer.Info mixerInfo : mixerInfoArray) {
                    try {
                        mixer = AudioSystem.getMixer(mixerInfo);
                        if (mixer != null) {
                            break;
                        }
                    } catch (IllegalArgumentException ex2) {
                        // Ignore
                    }
                }
            }

            // Create streams
            if (mixer != null) {
                int maxStreams = getMaxSimultaneousSounds();
                DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
                for (int i = 0; i < maxStreams; i++) {
                    try {
                        SourceDataLine line = (SourceDataLine) mixer.getLine(lineInfo);
                        line.open(audioFormat);
                        if (line.isOpen()) {
                            streams.add(new AudioStream(audioFormat, line));
                        }
                    } catch (LineUnavailableException | IllegalArgumentException ex) {
                        // Ignore
                    }
                }
            }
            if (streams.isEmpty()) {
                destroy();
            }

            // Notify started
            synchronized (stateLock) {
                stateLock.notify();
            }

            // Run
            while (running.get()) {
                synchronized (updateLock) {
                    for (AudioStream stream : streams) {
                        stream.tick();
                    }
                    try {
                        updateLock.wait(1);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
            }

            // Cleanup
            for (AudioStream stream : streams) {
                stream.close();
            }
            streams.clear();
            if (mixer != null) {
                mixer.close();
                mixer = null;
            }

            // Notify destroyed
            synchronized (stateLock) {
                stateLock.notify();
            }
        }

        private int getMaxSimultaneousSounds() {
            try {
                DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
                int maxLines = mixer.getMaxLines(lineInfo);
                if (maxLines == AudioSystem.NOT_SPECIFIED || maxLines > MAX_SIMULTANEOUS_SOUNDS) {
                    return MAX_SIMULTANEOUS_SOUNDS;
                } else {
                    return maxLines;
                }
            } catch (Exception ex) {
                return 0;
            }
        }

        //endregion
    }
}
