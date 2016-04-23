package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Button;
import com.brackeen.app.view.ImageView;
import com.brackeen.app.view.Scene;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class HelpScene extends Scene {

    private ImageView helpView;
    private Button backButton;

    @Override
    public void onLoad() {
        App app = App.getApp();

        setBackgroundColor(new Color(12, 12, 12));

        helpView = new ImageView(app.getImage("/ui/help.png"));
        helpView.setAnchor(0.5f, 0.0f);
        addSubview(helpView);

        backButton = new Button(app.getImage("/ui/back_button_normal.png"));
        backButton.setHoverImage(app.getImage("/ui/back_button_hover.png"));
        backButton.setPressedImage(app.getImage("/ui/back_button_pressed.png"));
        backButton.setAnchor(0.5f, 0.5f);
        backButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().popScene();
            }
        });
        addSubview(backButton);

        onResize();

        setKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {

            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().popScene();
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {

            }
        });
    }

    @Override
    public void onResize() {
        helpView.setLocation(getWidth() / 2, 40);
        backButton.setLocation(getWidth() / 2, 350);
    }
}
