package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.BufferedAudio;
import com.brackeen.scared.entity.Entity;

public class SoundPlayer3D {
    
    // Prevent instantiation
    private SoundPlayer3D() {
    }
    
    private static final int MAX_DIST = 24;
    
    public static void play(String audioName, Entity listener, int sourceTileX, int sourceTileY) {
        BufferedAudio audio = App.getApp().getAudio(audioName, 1);
        if (audio != null) {
            audio.play(getVolume(listener, sourceTileX, sourceTileY), 
                    getPan(listener, sourceTileX, sourceTileY), 
                    false);
        }
    }
    
    public static void play(String audioName, Entity listener, Entity source) {
        BufferedAudio audio = App.getApp().getAudio(audioName, 1);
        if (audio != null) {
            audio.play(getVolume(listener, source), 
                    getPan(listener, source), 
                    false);
        }
    }
    
    public static float getVolume(Entity listener, int sourceTileX, int sourceTileY) {
        return getVolume((sourceTileX + 0.5f) - listener.getX(), 
                (sourceTileY + 0.5f) - listener.getY());
    }
    
    public static float getVolume(Entity listener, Entity source) {
        return getVolume(source.getX() - listener.getX(),
                source.getY() - listener.getY());
    }
    
    private static float getVolume(float sourceX, float sourceY) {
        float dist = (float)(Math.sqrt(sourceX * sourceX + sourceY * sourceY));
        
        if (dist < 0 || dist >= MAX_DIST) {
            return 0;
        }
        else if (dist <= 1) {
            return 1;
        }
        else {
            return 1 / dist;
        }
    }
    
    public static float getPan(Entity listener, int sourceTileX, int sourceTileY) {
        return getPan(listener.getDirection(), 
                (sourceTileX + 0.5f) - listener.getX(),
                (sourceTileY + 0.5f) - listener.getY());
    }
    
    public static float getPan(Entity listener, Entity source) {
        return getPan(listener.getDirection(), 
                source.getX() - listener.getX(),
                source.getY() - listener.getY());
    }
    
    private static float getPan(float listenerDirection, float sourceX, float sourceY) {
        // Vector a = direction of player view
        // Vector b = direction of sounce source (from player)
        // side = the side point b is on of line a (1, 0, or -1)
        // pan = side * (1 - abs((a.b)/(|a|*|b|)))
        
        // NOTE: Scared has a fucked-up coordinate system because I didn't know
        // what I was doing when I made this game in 1997-1998, which was based on code
        // I wrote in C as a teenager.
        // The angle, cos table, and sin table are all correct. Y values need to be inversed.
        
        float aX = (float)Math.cos(Math.toRadians(listenerDirection));
        float aY = (float)Math.sin(Math.toRadians(listenerDirection));
        float bX = sourceX;
        float bY = -sourceY;
        float bLength = (float)(Math.sqrt(bX * bX + bY * bY));
        if (bLength <= 0) {
            return 0;
        }
        float dotProduct = aX * bX + aY * bY;
        
        // Dot product between location b and the perpendicular to vector a
        float side = Math.signum(bX * aY - aX * bY);
        
        float cosAngle = dotProduct / bLength;

        return side * (1 - Math.abs(cosAngle));
    }
}
