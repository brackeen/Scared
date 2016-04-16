package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.BitmapFont;
import com.brackeen.app.view.Button;
import com.brackeen.app.view.Label;
import com.brackeen.app.view.Scene;
import com.brackeen.app.view.View;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

public class ConsoleScene extends Scene {
    
    private static final int CURSOR_BLINK_TICKS = 20;
    private static final int BORDER_SIZE = 10;
    private static final String PROMPT = "] ";

    private final GameScene gameScene;
    private Button backButton;
    private Button helpButton;
    private View textView;
    private String currentLine = "";
    private int ticks = 0;
    private boolean cursorOn = true;
    
    public ConsoleScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }
    
    @Override
    public void onLoad() {
        App app = App.getApp();
        
        final BitmapFont messageFont = new BitmapFont(app.getImage("/ui/message_font.png"), 11, ' ');

        setBackgroundColor(new Color(12, 12, 12));
        
        backButton = new Button(app.getImage("/ui/back_button_normal.png"));
        backButton.setHoverImage(app.getImage("/ui/back_button_hover.png"));
        backButton.setPressedImage(app.getImage("/ui/back_button_pressed.png"));
        backButton.setAnchor(1, 1);
        backButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().popScene();
            }
        });
        addSubview(backButton);
        
        helpButton = new Button(app.getImage("/ui/help_button_normal.png"));
        helpButton.setHoverImage(app.getImage("/ui/help_button_hover.png"));
        helpButton.setPressedImage(app.getImage("/ui/help_button_pressed.png"));
        helpButton.setAnchor(0, 1);
        helpButton.setButtonListener(new Button.Listener() {

            public void buttonClicked(Button button) {
                App.getApp().pushScene(new HelpScene());
            }
        });
        addSubview(helpButton);
        
        textView = new View();
        textView.setLocation(BORDER_SIZE, BORDER_SIZE);
        addSubview(textView);

        onResize();
        
        int maxLines = (int)textView.getHeight() / messageFont.getHeight();
        for (int i = 0; i < maxLines; i++) {
            Label label = new Label(messageFont, "");
            label.setLocation(0, i * messageFont.getHeight());
            textView.addSubview(label);
        }
        
        setKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {
                char ch = ke.getKeyChar();
                if (messageFont.canDisplay(ch)) {
                    currentLine += ch;
                }
                setCursorOn(true);
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().popScene();
                }
                else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (currentLine.length() > 0) {
                        currentLine = currentLine.substring(0, currentLine.length() - 1);
                    }
                }
                else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    App.log(PROMPT + currentLine);
                    String response = gameScene.doCommand(currentLine);
                    App.log(response);
                    currentLine = "";
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                
            }
        });
    }
    
    @Override
    public void onResize() {
        backButton.setLocation(getWidth() / 2 - BORDER_SIZE/2, getHeight() - BORDER_SIZE);
        helpButton.setLocation(getWidth() / 2 + BORDER_SIZE/2, getHeight() - BORDER_SIZE);
        textView.setSize(getWidth() - BORDER_SIZE * 2, getHeight() - BORDER_SIZE * 3 - helpButton.getHeight());
    }
    
    private void setCursorOn(boolean cursorOn) {
        this.cursorOn = cursorOn; 
        ticks = 0;
    }
    
    @Override
    public void onTick() {
        ticks++;
        if (ticks >= CURSOR_BLINK_TICKS) {
            setCursorOn(!cursorOn);
        }
        
        List<String> log = App.getApp().getLog();
        List<View> labels = textView.getSubviews();
        int numLogLines = Math.min(log.size(), labels.size() - 1);
        for (int i = 0; i < labels.size(); i++) {
            Label label = (Label)labels.get(i);
            if (i < numLogLines) {
                label.setText(log.get(log.size() - numLogLines + i));
            }
            else if (i == numLogLines) {
                label.setText(PROMPT + currentLine + (cursorOn ? "_" : ""));
            }
            else {
                label.setText("");
            }
        }
    }
}
