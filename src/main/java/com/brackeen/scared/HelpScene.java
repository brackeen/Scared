package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Button;
import com.brackeen.app.view.ImageView;
import com.brackeen.app.view.Scene;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class HelpScene extends Scene {
    
    @Override
    public void onLoad() {
        
        App app = App.getApp();
        
        setBackgroundColor(new Color(12, 12, 12));
        
        ImageView help = new ImageView(app.getImage("/ui/help.png"));
        help.setLocation(0, 40);
        addSubview(help);
        
        Button backButton = new Button(app.getImage("/ui/back_button_normal.png"));
        backButton.setHoverImage(app.getImage("/ui/back_button_hover.png"));
        backButton.setPressedImage(app.getImage("/ui/back_button_pressed.png"));
        backButton.setLocation(getWidth() / 2, 350);
        backButton.setAnchor(0.5f, 0.5f);
        backButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().popScene();
            }
        });
        addSubview(backButton);
        
        setKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent ke) {

            }

            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().popScene();
                }
            }

            public void keyReleased(KeyEvent ke) {
                
            }
        });
    }
}
