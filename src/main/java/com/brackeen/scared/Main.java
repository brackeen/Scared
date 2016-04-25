package com.brackeen.scared;

import com.brackeen.app.App;
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
        setAppName("Scared");
        setAutoPixelScale(true);
        setAutoPixelScaleBaseSize(320, 240);
    }

    @Override
    public Scene createFirstScene() {
        return new TitleScene();
    }
}
