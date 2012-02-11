package com.brackeen.app.view;

import com.brackeen.app.BitmapFont;
import java.awt.Graphics2D;

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
        }
        else {
            setSize(font.getStringWidth(text), font.getHeight());
        }
    }
    
    @Override
    public void onDraw(Graphics2D g) {
        if (font != null) {
            font.drawString(g, text);
        }
    }
}
