package com.brackeen.scared.entity;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoftTexture;

public class Key extends Entity {
    
    public static final int NUM_KEYS = 3;
    
    private static final String[] KEY_COLORS = { "", "RED", "GREEN", "BLUE" };
    
    private final Map map;
    private final int type;
    
    public Key(Map map, SoftTexture texture, float x, float y, int type) {
        super(0.25f, x, y);
        this.map = map;
        this.type = type;
        setTexture(texture);
    }
    
    @Override
    public void notifyPlayerCollision(Player player) {
        App.getApp().getAudio("/sound/unlock.wav", 1).play();
        map.setMessage("You got the " + KEY_COLORS[type] + " key");
        player.addKey(type);
        delete();
    }
}
