package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.BitmapFont;
import com.brackeen.app.view.Label;
import com.brackeen.app.view.Scene;
import com.brackeen.app.view.View;

import java.awt.Color;
import java.util.List;

public class BaseConsoleScene extends Scene {

    private static final int BORDER_SIZE = 0;

    protected BitmapFont messageFont;
    protected View textView;

    @Override
    public void onLoad() {
        App app = App.getApp();

        messageFont = new BitmapFont(app.getImage("/ui/console_font.png"), 8, ' ');
        messageFont.setTracking(-2);

        setBackgroundColor(new Color(12, 12, 12));

        textView = new View();
        textView.setLocation(BORDER_SIZE, BORDER_SIZE);
        addSubview(textView);
    }

    @Override
    public void onResize() {
        textView.setSize(getWidth() - BORDER_SIZE * 2, getHeight() - BORDER_SIZE * 2);

        textView.removeAllSubviews();
        int maxLines = (int) textView.getHeight() / messageFont.getHeight();
        for (int i = 0; i < maxLines; i++) {
            Label label = new Label(messageFont, "");
            label.setLocation(0, i * messageFont.getHeight());
            textView.addSubview(label);
        }
    }

    @Override
    public void onTick() {
        setupTextViews("");
    }

    protected void setupTextViews(String lastLine) {
        List<String> log = App.getApp().getLog();
        List<View> labels = textView.getSubviews();
        int numLogLines = Math.min(log.size(), labels.size() - 1);
        for (int i = 0; i < labels.size(); i++) {
            Label label = (Label) labels.get(i);
            if (i < numLogLines) {
                label.setText(log.get(log.size() - numLogLines + i));
            } else if (i == numLogLines) {
                label.setText(lastLine);
            } else {
                label.setText("");
            }
        }
    }
}
