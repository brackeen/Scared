package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.audio.AudioEngine;
import com.brackeen.app.view.Scene;

import javax.swing.SwingUtilities;

public class Main extends App {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Main main = new Main();
                main.initFrame(640, 480);
            }
        });
    }

    public Main() {
        AudioEngine.setMasterVolume(Settings.getFloat(Settings.VOLUME, 1.0f));

        setAppName("Scared");
        setAudioSampleRate(8000);
        setAutoPixelScale(Settings.getBoolean(Settings.AUTO_PIXEL_SCALE, true));
        setAutoPixelScaleBaseSize(320, 240);
    }

    @Override
    public Scene createFirstScene() {
        return new LoadingScene();
    }
}
