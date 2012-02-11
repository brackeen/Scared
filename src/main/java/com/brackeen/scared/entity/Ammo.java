package com.brackeen.scared.entity;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoftTexture;

public class Ammo extends Entity {
    
    private final Map map;
    
    public Ammo(Map map, SoftTexture texture, float x, float y) {
        super(0.25f, x, y);
        this.map = map;
        setTexture(texture);
    }
        
    @Override
    public void notifyPlayerCollision(Player player) {
        int ammo = player.getAmmo();
        if (ammo < Player.MAX_AMMO) {
            App.getApp().getAudio("/sound/getammo.wav", 1).play();
            
            map.setMessage("You got some ammo");
            player.setAmmo(Math.min(ammo + 20, Player.MAX_AMMO));
            
            delete();
        }
    }
}
