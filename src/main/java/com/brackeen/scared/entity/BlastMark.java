package com.brackeen.scared.entity;

import com.brackeen.scared.SoftTexture;

public class BlastMark extends Entity {
    
    private SoftTexture[] textures;
    private int countdown;
    
    public BlastMark(SoftTexture[] textures, float x, float y, int countdown) {
        super(0, x, y);
        this.textures = textures;
        this.countdown = countdown;
        setTexture(textures[0]);
        setZ(0.5f - getTexture().getHeight()/2 * getTextureScale());
    }
    
    @Override
    public void tick() {
        if ((countdown % 2) == 0) {
            int index = (int)(Math.random() * textures.length);
            index = Math.min(index, textures.length - 1);
            setTexture(textures[index]);
        }
        countdown--;
        if (countdown <= 0) {
            delete();
        }
    }
    
    @Override
    public void setDistanceFromCamera(float distanceFromCamera) {
        // Bring it forward a bit so that it appears in front of walls
        float extraDist = getTexture().getWidth() * getTextureScale();
        super.setDistanceFromCamera(distanceFromCamera - extraDist);
    }
}
