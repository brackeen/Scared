package com.brackeen.app.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageView extends View {
    
    private BufferedImage image;
    
    public ImageView(BufferedImage image) {
        this.image = image;
        sizeToFit();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    @Override
    public void sizeToFit() {
        if (image == null) {
            setSize(0, 0);
        }
        else {
            setSize(image.getWidth(), image.getHeight());
        }
    }
    
    @Override
    public void onDraw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, null, null);
        }
    }
}
