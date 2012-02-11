package com.brackeen.scared.entity;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoftTexture;

public class MedKit extends Entity {
    
    private final Map map;
    private final boolean nuclear;
    
    public MedKit(Map map, SoftTexture texture, float x, float y, boolean nuclear) {
        super(0.25f, x, y);
        this.map = map;
        this.nuclear = nuclear;
        setTexture(texture);
    }
    
    @Override
    public void notifyPlayerCollision(Player player) {
        int health = player.getHealth();
        if (!nuclear && health < Player.MAX_HEALTH) {
            map.setMessage("You got a med kit");
            App.getApp().getAudio("/sound/getammo.wav", 1).play();
            player.setHealth(Math.min(health + 20, Player.MAX_HEALTH));
            delete();
        }
        else if (nuclear && player.getHealth() < Player.MAX_NUCLEAR_HEALTH) {
            map.setMessage("N*U*C*L*E*A*R");
            App.getApp().getAudio("/sound/nuclear_health.wav", 1).play();
            player.setHealth(Player.MAX_NUCLEAR_HEALTH);
            delete();
        }
    }
}
