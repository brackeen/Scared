package com.brackeen.app.audio;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioBuffer {
    public static final AudioBuffer BLANK_AUDIO = new AudioBuffer(null, null);

    private final AudioFormat format;
    final byte[] data;

    public AudioBuffer(AudioFormat format, byte[] data) {
        this.format = format;
        this.data = data;
    }

    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Plays this AudioBuffer.
     * @param volume The volume, from 0 to 1.
     * @param pan The pan, from -1 (left channel) to 0 (center) to 1 (right channel).
     * @param loop Whether to loop the audio. If true, the looping continues indefinitely until {@link AudioStream#stop()} is called.
     * @return An AudioStream if playback started, or null if there are no AudioStreams available
     * (all streams are currently playing).
     */
    public AudioStream play(float volume, float pan, boolean loop) {
        AudioStream stream = null;
        if (data != null) {
            stream = AudioEngine.getAvailableStream();
            if (stream != null) {
                stream.play(this, volume, pan, loop);
            }
        }
        return stream;
    }

    /**
     * Plays this AudioBuffer.
     * @return An AudioStream if playback started, or null if there are no AudioStreams available
     * (all streams are currently playing).
     */
    public AudioStream play() {
        return play(1, 0, false);
    }

    /**
     * Loops this AudioBuffer. The looping continues indefinitely until {@link AudioStream#stop()} is called.
     * @return An AudioStream if playback started, or null if there are no AudioStreams available
     * (all streams are currently playing).
     */
    public AudioStream loop() {
        return play(1, 0, true);
    }

    public static AudioBuffer read(URL url) throws IOException {
        if (url == null) {
            return null;
        }

        // Read input data
        AudioFormat format;
        byte[] data;
        try (AudioInputStream is = AudioSystem.getAudioInputStream(url)) {
            format = is.getFormat();
            if (format == null) {
                return null;
            }
            long frameLength = is.getFrameLength();
            int length;
            if (frameLength > 0 && format.getFrameSize() > 0) {
                length = (int) frameLength * format.getFrameSize();
            } else {
                length = -1;
            }
            data = readFully(is, length);
        } catch (IllegalArgumentException | UnsupportedAudioFileException ex) {
            // org.classpath.icedtea.pulseaudio may throw IllegalArgumentException?
            throw new IOException(ex);
        }

        // Use 8000hz instead of 8012hz or 8016hz
        if (format.getFrameRate() > 8000 && format.getFrameRate() < 8100) {
            format = new AudioFormat(8000, format.getSampleSizeInBits(), format.getChannels(),
                    format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED, format.isBigEndian());
        }

        // Convert to stereo so that the Pan control works.
        if (format.getChannels() == 1) {
            format = new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), 2,
                    format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED, format.isBigEndian());
            byte[] dstData = new byte[data.length * 2];
            for (int srcOffset = 0, dstOffset = 0; srcOffset < data.length; srcOffset += 2, dstOffset += 4) {
                dstData[dstOffset] = data[srcOffset];
                dstData[dstOffset + 1] = data[srcOffset + 1];
                dstData[dstOffset + 2] = data[srcOffset];
                dstData[dstOffset + 3] = data[srcOffset + 1];
            }
            data = dstData;
        }

        return new AudioBuffer(format, data);
    }

    private static byte[] readFully(InputStream is, int length) throws IOException {
        if (length > 0) {
            byte[] data = new byte[length];
            DataInputStream dis = new DataInputStream(is);
            dis.readFully(data);
            return data;
        } else {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            return os.toByteArray();
        }
    }
}
