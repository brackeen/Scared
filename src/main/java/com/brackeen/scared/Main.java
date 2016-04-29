package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.BufferedAudio;
import com.brackeen.app.view.Scene;

import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

public class Main extends App {

    private static final long serialVersionUID = 1L;

    public static final String SETTING_AUTO_PIXEL_SCALE = "autoPixelScale";
    public static final String SETTING_DEPTH_SHADING = "depthShading";
    public static final String SETTING_VOLUME = "volume";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Main main = new Main();
                main.initFrame(640, 480);
            }
        });
    }

    public Main() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        BufferedAudio.setMasterVolume(prefs.getFloat(Main.SETTING_VOLUME, 1.0f));

        setAppName("Scared");
        setAutoPixelScale(prefs.getBoolean(Main.SETTING_AUTO_PIXEL_SCALE, true));
        setAutoPixelScaleBaseSize(320, 240);
    }

    @Override
    public Scene createFirstScene() {
        return new TitleScene();
    }
}
