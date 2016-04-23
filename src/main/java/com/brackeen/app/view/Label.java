package com.brackeen.app.view;

import com.brackeen.app.BitmapFont;

import java.awt.Graphics2D;

@SuppressWarnings("unused")
public class Label extends View {

    private BitmapFont font;
    private String text;

    public Label() {

    }

    public Label(BitmapFont font, String text) {
        this.font = font;
        this.text = text;
        sizeToFit();
    }

    public BitmapFont getFont() {
        return font;
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void sizeToFit() {
        if (font == null) {
            setSize(0, 0);
        } else {
            setSize(font.getStringWidth(text), font.getHeight());
        }
    }

    @Override
    public void onDraw(Graphics2D g) {
        if (font != null) {
            font.drawString(g, text);
        }
    }

    public static View makeMultilineLabel(BitmapFont font, String text, float anchorX) {
        View view = new View();
        int y = 0;
        int index = 0;
        while (true) {
            String line;
            int newIndex = text.indexOf('\n', index);
            if (newIndex == -1) {
                line = text.substring(index);
            } else {
                line = text.substring(index, newIndex);
            }
            Label label = new Label(font, line);
            label.setLocation(0, y);
            label.setAnchor(anchorX, 0);
            view.addSubview(label);
            y += font.getHeight();

            if (newIndex == -1) {
                break;
            }
            index = newIndex + 1;
        }
        view.setHeight(y);
        return view;
    }
}
