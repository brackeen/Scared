package com.brackeen.app;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BitmapFont {
    
    private final BufferedImage image;
    private final int charWidth;
    private final int charHeight;
    private final char firstChar;
    private final int numChars;
    private final boolean hasLowercase;
    private int tracking = 0;
    
    public BitmapFont(BufferedImage image, int charWidth, char firstChar) {
        this.image = image;
        this.charWidth = charWidth;
        this.charHeight = image.getHeight();
        this.firstChar = firstChar;
        this.numChars = image.getWidth() / charWidth;
        this.hasLowercase = firstChar <= 'a' && firstChar + numChars >= 'z';
    }

    public int getTracking() {
        return tracking;
    }

    public void setTracking(int tracking) {
        this.tracking = tracking;
    }
    
    public int getStringWidth(String s) {
        if (s == null) {
            return 0;
        }
        else {
            return s.length() * charWidth + (s.length() - 1) * tracking;
        }
    }
    
    public int getHeight() {
        return charHeight;
    }
    
    public boolean canDisplay(char ch) {
        if (Character.isLowerCase(ch) && !hasLowercase) {
            ch = Character.toUpperCase(ch);
        }
        return (ch >= firstChar && ch < firstChar + numChars);
    }
    
    public void drawString(Graphics2D g, String s) {
        if (s != null) {
            int x = 0;
            int y = 0;
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (Character.isLowerCase(ch) && !hasLowercase) {
                    ch = Character.toUpperCase(ch);
                }
                if (ch >= firstChar && ch < firstChar + numChars) {
                    int charX = (ch - firstChar) * charWidth;
                    g.drawImage(image, 
                            x, y, x + charWidth, y + charHeight, 
                            charX, 0, charX + charWidth, charHeight, null);
                }
                x += charWidth + tracking;
            }
        }
    }
}
