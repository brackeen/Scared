package com.brackeen.scared.entity;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.Tile;

public class Player extends Entity {

    public static final int MAX_AMMO = 100;
    public static final int MAX_HEALTH = 100;
    public static final int MAX_NUCLEAR_HEALTH = 200;
    public static final int DEFAULT_AMMO = 20;
    public static final int DEFAULT_HEALTH = MAX_HEALTH;

    private final Map map;
    private int health = DEFAULT_HEALTH;
    private int ammo = DEFAULT_AMMO;

    private int keys = 1;
    private int kills = 0;
    private int secrets = 0;
    private int hitWarningTicksRemaining = 0;

    private boolean godMode = false;
    private boolean freezeEnemies = false;
    private boolean isAlive = true;

    public Player(Map map) {
        super(0.25f, 0, 0);
        this.map = map;
        setZ(0.5f);
    }

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public boolean isFreezeEnemies() {
        return freezeEnemies;
    }

    public void setFreezeEnemies(boolean freezeEnemies) {
        this.freezeEnemies = freezeEnemies;
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getSecrets() {
        return secrets;
    }

    public void setSecrets(int secrets) {
        this.secrets = secrets;
    }

    public boolean wasHitRecently() {
        return hitWarningTicksRemaining > 0;
    }

    @Override
    public void setTile(Tile tile) {
        if (tile != null && tile != getTile()) {
            map.notifyPlayerEnteredTile((int) getX(), (int) getY());
        }
        super.setTile(tile);
    }

    public boolean hurt(int points) {
        if (godMode || !isAlive() || points <= 0) {
            return false;
        } else {
            hitWarningTicksRemaining = 12;
            health -= points;
            if (health <= 0) {
                health = 0;
                isAlive = false;
                App.getApp().getAudio("/sound/player_dead.wav", 1).play();
            } else if (points > 15) {
                App.getApp().getAudio("/sound/player_hurt.wav", 1).play();
            }
            return true;
        }
    }

    @Override
    public void tick() {
        if (hitWarningTicksRemaining > 0) {
            hitWarningTicksRemaining--;
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    @Override
    public boolean onCollisionWithEntityShouldSlide() {
        return true;
    }

    @Override
    public boolean onCollisionWithWallShouldSlide() {
        return true;
    }

    public boolean hasKey(int key) {
        return (keys & (1 << key)) != 0;
    }

    public void addKey(int key) {
        keys |= (1 << key);
    }
}
