package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Button;
import com.brackeen.app.view.ImageView;
import com.brackeen.app.view.Scene;
import java.awt.Color;

public class TitleScene extends Scene {
    
    @Override
    public void onLoad() {
        
        App app = App.getApp();
        
        App.log("Scared Console");
        App.log("Type \"HELP\" for commands");
        
        // Preload audio.
        // Number of loaded audio buffers must be 32 or less, due to a Java Sound limitation.
        app.getAudio("/sound/bigfan.wav", 1);
        app.getAudio("/sound/doorclose.wav", 2);
        app.getAudio("/sound/doorwoosh.wav", 2);
        app.getAudio("/sound/endlevel.wav", 1);
        app.getAudio("/sound/enemy_dead.wav", 2);
        app.getAudio("/sound/getammo.wav", 4);
        app.getAudio("/sound/laser0.wav", 4);
        app.getAudio("/sound/laser1.wav", 4);
        app.getAudio("/sound/no_ammo.wav", 4);
        app.getAudio("/sound/nuclear_health.wav", 1);
        app.getAudio("/sound/player_dead.wav", 1);
        app.getAudio("/sound/player_hurt.wav", 3);
        app.getAudio("/sound/startlevel.wav", 1);
        app.getAudio("/sound/unlock.wav", 1);
        app.getAudio("/sound/wallmove.wav", 1);
        
        setBackgroundColor(new Color(12, 12, 12));
        
        ImageView title = new ImageView(app.getImage("/ui/splash.png"));
        title.setAnchor(0.5f, 0);
        title.setLocation(getWidth() / 2, 0);
        addSubview(title);
        
        Button startButton = new Button(app.getImage("/ui/start_button_normal.png"));
        startButton.setHoverImage(app.getImage("/ui/start_button_hover.png"));
        startButton.setPressedImage(app.getImage("/ui/start_button_pressed.png"));
        startButton.setLocation(getWidth() / 2, 280);
        startButton.setAnchor(0.5f, 0.5f);
        startButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().pushScene(new GameScene());
            }
        });
        addSubview(startButton);
        
        Button helpButton = new Button(app.getImage("/ui/help_button_normal.png"));
        helpButton.setHoverImage(app.getImage("/ui/help_button_hover.png"));
        helpButton.setPressedImage(app.getImage("/ui/help_button_pressed.png"));
        helpButton.setLocation(getWidth() / 2, 350);
        helpButton.setAnchor(0.5f, 0.5f);
        helpButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().pushScene(new HelpScene());
            }
        });
        addSubview(helpButton);

    }
}
