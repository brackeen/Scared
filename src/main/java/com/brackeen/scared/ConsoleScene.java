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
import java.util.ArrayList;
import java.util.List;

public class ConsoleScene extends Scene {

    private static final int CURSOR_BLINK_TICKS = 20;
    private static final int BORDER_SIZE = 10;
    private static final int MAX_COMMAND_HISTORY = 200;
    private static final String PROMPT = "] ";

    private static List<String> commandHistory = new ArrayList<>();

    private final GameScene gameScene;
    private BitmapFont messageFont;
    private Button backButton;
    private Button helpButton;
    private View textView;
    private String newCommandLine = "";
    private int commandHistoryIndex;
    private int ticks = 0;
    private boolean cursorOn = true;

    public ConsoleScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    @Override
    public void onLoad() {
        App app = App.getApp();

        commandHistoryIndex = commandHistory.size();

        messageFont = new BitmapFont(app.getImage("/ui/message_font.png"), 11, ' ');

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

        setKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {
                char ch = ke.getKeyChar();
                if (messageFont.canDisplay(ch)) {
                    setCurrentLine(getCurrentLine() + ch);
                }
                setCursorOn(true);
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().popScene();
                } else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    String currentLine = getCurrentLine();
                    if (currentLine.length() > 0) {
                        setCurrentLine(currentLine.substring(0, currentLine.length() - 1));
                        setCursorOn(true);
                    }
                } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
                    commandHistoryIndex = Math.max(0, commandHistoryIndex - 1);
                    setCursorOn(true);
                } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                    commandHistoryIndex = Math.min(commandHistory.size(), commandHistoryIndex + 1);
                    setCursorOn(true);
                } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    setCursorOn(true);
                    String currentLine = getCurrentLine();
                    App.log(PROMPT + currentLine);
                    if (currentLine.length() > 0) {
                        String response = gameScene.doCommand(currentLine);
                        App.log(response);

                        commandHistory.add(currentLine);
                        if (commandHistory.size() > MAX_COMMAND_HISTORY) {
                            commandHistory.remove(0);
                        }
                        commandHistoryIndex = commandHistory.size();
                        newCommandLine = "";
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {

            }
        });
    }

    @Override
    public void onResize() {
        backButton.setLocation(getWidth() / 2 - BORDER_SIZE / 2, getHeight() - BORDER_SIZE);
        helpButton.setLocation(getWidth() / 2 + BORDER_SIZE / 2, getHeight() - BORDER_SIZE);
        textView.setSize(getWidth() - BORDER_SIZE * 2, getHeight() - BORDER_SIZE * 3 - helpButton.getHeight());

        textView.removeAllSubviews();
        int maxLines = (int) textView.getHeight() / messageFont.getHeight();
        for (int i = 0; i < maxLines; i++) {
            Label label = new Label(messageFont, "");
            label.setLocation(0, i * messageFont.getHeight());
            textView.addSubview(label);
        }
    }

    private void setCursorOn(boolean cursorOn) {
        this.cursorOn = cursorOn;
        ticks = 0;
    }

    private String getCurrentLine() {
        String currentLine;
        if (commandHistoryIndex < commandHistory.size()) {
            currentLine = commandHistory.get(commandHistoryIndex);
        } else {
            currentLine = newCommandLine;
        }
        return currentLine;
    }

    private void setCurrentLine(String currentLine) {
        if (commandHistoryIndex < commandHistory.size()) {
            commandHistory.set(commandHistoryIndex, currentLine);
        } else {
            newCommandLine = currentLine;
        }
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
            Label label = (Label) labels.get(i);
            if (i < numLogLines) {
                label.setText(log.get(log.size() - numLogLines + i));
            } else if (i == numLogLines) {
                label.setText(PROMPT + getCurrentLine() + (cursorOn ? "_" : ""));
            } else {
                label.setText("");
            }
        }
    }
}
