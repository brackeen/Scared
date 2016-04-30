package com.brackeen.scared;

import com.brackeen.app.App;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class ConsoleScene extends BaseConsoleScene {

    private static final int CURSOR_BLINK_TICKS = 20;
    private static final int MAX_COMMAND_HISTORY = 200;
    private static final String PROMPT = "] ";

    private static List<String> originalCommandHistory = new ArrayList<>();
    private static List<String> editedCommandHistory = new ArrayList<>();

    private final GameScene gameScene;
    private String newCommandLine = "";
    private int commandHistoryIndex;
    private int ticks = 0;
    private boolean cursorOn = true;

    public ConsoleScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        commandHistoryIndex = originalCommandHistory.size();

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
                    commandHistoryIndex = Math.min(originalCommandHistory.size(), commandHistoryIndex + 1);
                    setCursorOn(true);
                } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    setCursorOn(true);
                    String currentLine = getCurrentLine();
                    App.log(PROMPT + currentLine);
                    if (currentLine.length() > 0) {
                        String response = gameScene.doCommand(currentLine);
                        if (response != null) {
                            App.log(response);
                        }

                        setCurrentLine(getCurrentLine(originalCommandHistory));

                        originalCommandHistory.add(currentLine);
                        editedCommandHistory.add(currentLine);
                        if (originalCommandHistory.size() > MAX_COMMAND_HISTORY) {
                            originalCommandHistory.remove(0);
                            editedCommandHistory.remove(0);
                        }
                        commandHistoryIndex = originalCommandHistory.size();
                        newCommandLine = "";
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {

            }
        });
    }

    private void setCursorOn(boolean cursorOn) {
        this.cursorOn = cursorOn;
        ticks = 0;
    }

    private String getCurrentLine() {
        return getCurrentLine(editedCommandHistory);
    }

    private String getCurrentLine(List<String> commandHistory) {
        String currentLine;
        if (commandHistoryIndex < commandHistory.size()) {
            currentLine = commandHistory.get(commandHistoryIndex);
        } else {
            currentLine = newCommandLine;
        }
        return currentLine;
    }

    private void setCurrentLine(String currentLine) {
        if (commandHistoryIndex < editedCommandHistory.size()) {
            editedCommandHistory.set(commandHistoryIndex, currentLine);
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

        setupTextViews(PROMPT + getCurrentLine() + (cursorOn ? "_" : ""));
    }
}
