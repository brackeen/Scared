package com.brackeen.scared.entity;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoftTexture;
import java.awt.geom.Point2D;
import java.util.List;

public class Enemy extends Entity {
    
    public static final int NUM_IMAGES = 15;
    
    private static final int STATE_ASLEEP = 0;
    private static final int STATE_TERMINATE = 1;
    private static final int STATE_MOVE_LEFT = 2;
    private static final int STATE_MOVE_FAR_LEFT = 3;
    private static final int STATE_MOVE_RIGHT = 4;
    private static final int STATE_MOVE_FAR_RIGHT = 5;
    private static final int LAST_STATE_WITH_ANIM = STATE_MOVE_FAR_RIGHT;
    private static final int STATE_READY = 6;
    private static final int STATE_AIM = 7;
    private static final int STATE_FIRE = 8;
    private static final int STATE_HURT = 9;
    private static final int STATE_DYING = 10;
    private static final int STATE_DEAD = 11;
    
    //                                 state =   0   1   2   3   4   5   6   7   8   9  10  11
    private static final int[] STATE_TEXTURE = { 0,  0,  2,  4,  6,  8,  0, 10, 11, 12, 13, 14 };
    private static final int[] STATE_TICKS =  { 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12 };
    
    private static final float STEP_SIZE = 0.05f;

    private final SoftTexture[] textures;
    private final int type;
    private final Map map;
    private int state;
    private int health;
    private final double p; //probability of changing states
    private int ticksRemaining;
    private int ticks;
    private double aimAngle;
    
    private boolean playerVisibilityNeedsCalculation;
    private boolean isPlayerVisible;
    
    public Enemy(Map map, SoftTexture[] textures, float x, float y, int type) {
        super(0.25f, x, y);
        this.type = type;
        this.textures = textures;
        this.map = map;
        setTexture(textures[0]);
        setTextureScale(getTextureScale() / 2);
        setZ(-4f / DEFAULT_PIXELS_PER_TILE);
        setState(STATE_ASLEEP);
        
        switch (type) {
        case 1: default:
            health = 20;
            STATE_TICKS[STATE_READY] = 18;
            STATE_TICKS[STATE_AIM] = 40;
            p = .1;
            break;

        case 2:
            health = 30;
            STATE_TICKS[STATE_READY] = 12;
            STATE_TICKS[STATE_AIM] = 24;
            p = .05;
            break;

        case 3:
            health = 50;
            STATE_TICKS[STATE_READY] = 6;
            STATE_TICKS[STATE_AIM] = 18;
            p = .1;
            break;

        case 4:
            health = 80;
            STATE_TICKS[STATE_READY] = 0;
            STATE_TICKS[STATE_AIM] = 12;
            p = .03;
            break;
        }
    }

    public boolean isDead() {
        return state == STATE_DEAD;
    }
    
    private void setState(int state) {
        if (this.state != state) {
            this.state = state;
            if (state == STATE_DEAD) {
                setRadius(0); // Prevent future collisions
            }
            ticksRemaining = STATE_TICKS[state];
        }
    }
    
    public int getType() {
        return type;
    }

    public void hurt(int points) {
        if (health > 0) {
            health -= points;
            boolean gotoHurtState = false;
            
            if (health <= 0) {
                gotoHurtState = true;
            }
            else if (state == STATE_FIRE) {
                // 50% of interrupting firing
                if (Math.random() < .5) {
                    gotoHurtState = true;
                }
            }
            else if (state != STATE_HURT) {
                gotoHurtState = true;
            }
            
            if (gotoHurtState) {
                setState(STATE_HURT);
            }
        }
    }
    
    @Override
    public boolean notifyCollision(Entity movingEntity) {
        return true;
    }
    
    private boolean isPlayerVisible(float angleToPlayer) {
        if (playerVisibilityNeedsCalculation) {
            playerVisibilityNeedsCalculation = false;
            isPlayerVisible = false;
            Point2D.Float point = map.getWallCollision(getX(), getY(), (float)Math.toDegrees(angleToPlayer));
            if (point != null) {
                List<Entity> playerHit = map.getCollisions(Player.class, getX(), getY(), point.x, point.y);
                if (playerHit.size() > 0) {
                    isPlayerVisible = true;
                }
            }
        }
        return isPlayerVisible;
    }
    
    @Override
    public void tick() {
        playerVisibilityNeedsCalculation = true;
        Player player = map.getPlayer();
        
        float stepx = 0;
        float stepy = 0;
        float dx = player.getX() - getX();
        float dy = player.getY() - getY();
        float angleToPlayer = (float)Math.atan2(dy, dx);

        if ((ticksRemaining <= 0 || state == STATE_TERMINATE) && Math.abs(dx) < 2f && Math.abs(dy) < 2f && state < STATE_READY) {
            // Player is very close - move immediately or fire
            double pq = Math.random();

            if (pq < 0.25f) {
                setState(STATE_MOVE_FAR_LEFT);
            }
            else if (pq < 0.50f) {
                setState(STATE_MOVE_FAR_RIGHT);
            }
            else {
                setState(STATE_READY);
            }
        }
        else if (state > STATE_ASLEEP && state < STATE_READY && Math.random() < p) {
            // When moving, randomly change to another move state
            int s = (int)Math.round(Math.random() * 6);
            switch (s) {
                case 0: default:
                    setState(STATE_TERMINATE);
                    break;
                case 1:
                    setState(STATE_MOVE_LEFT);
                    break;
                case 2:
                    setState(STATE_MOVE_RIGHT);
                    break;
                case 3:
                    setState(STATE_MOVE_FAR_LEFT);
                    break;
                case 4:
                    setState(STATE_MOVE_FAR_RIGHT);
                    break;
                case 5:
                    if (isPlayerVisible(angleToPlayer)) {
                        setState(STATE_READY);
                    }
                    else {
                        setState(STATE_TERMINATE);
                    }
                    break;
            }
        }
        
        switch (state) {
            case STATE_ASLEEP:
                if (isPlayerVisible(angleToPlayer)) {
                    setState(STATE_TERMINATE);
                }
                break;
                
            case STATE_TERMINATE:
                stepx = (float)Math.cos(angleToPlayer) * STEP_SIZE;
                stepy = (float)Math.sin(angleToPlayer) * STEP_SIZE;
                break;

            case STATE_MOVE_LEFT:
                stepx = (float)Math.cos(angleToPlayer + Math.PI/4) * STEP_SIZE;
                stepy = (float)Math.sin(angleToPlayer + Math.PI/4) * STEP_SIZE;
                break;

            case STATE_MOVE_RIGHT:
                stepx = (float)Math.cos(angleToPlayer - Math.PI/4) * STEP_SIZE;
                stepy = (float)Math.sin(angleToPlayer - Math.PI/4) * STEP_SIZE;
                break;

            case STATE_MOVE_FAR_LEFT:
                stepx = (float)Math.cos(angleToPlayer + Math.PI/2) * STEP_SIZE;
                stepy = (float)Math.sin(angleToPlayer + Math.PI/2) * STEP_SIZE;
                break;

            case STATE_MOVE_FAR_RIGHT:
                stepx = (float)Math.cos(angleToPlayer - Math.PI/2) * STEP_SIZE;
                stepy = (float)Math.sin(angleToPlayer - Math.PI/2) * STEP_SIZE;
                break;

            case STATE_READY:
                if (ticksRemaining <= 0) {
                    setState(STATE_AIM);
                }
                break;

            case STATE_AIM:
                if (ticksRemaining <= 0) {
                    if (player.isAlive() && isPlayerVisible(angleToPlayer)) {
                        aimAngle = angleToPlayer;
                        setState(STATE_FIRE);
                    }
                    else {
                        setState(STATE_TERMINATE);
                    }
                }
                break;

            case STATE_FIRE:
                if (player.isFreezeEnemies()) {
                    setState(STATE_TERMINATE);
                }
                else if (ticksRemaining <= 0) {
                    App.getApp().getAudio("/sound/laser0.wav", 1).play();

                    // fire shot
                    if (isPlayerVisible(angleToPlayer)) {
                        
                        Point2D.Float point = map.getWallCollision(getX(), getY(), (float)Math.toDegrees(aimAngle));
                        if (point != null) {
                            List<Entity> playerHit = map.getCollisions(Player.class, getX(), getY(), point.x, point.y);
                            if (playerHit.size() > 0) {
                                // here, diffAngle is the differnce between the angle the
                                // robot aimed at and the angle the player is currently at
                                double diffAngle = Math.abs(aimAngle - angleToPlayer);
                                int hitPoints = 0;
                                if (diffAngle < .04) { // about 2.3 degrees
                                    hitPoints = 15 + (int)Math.round(Math.random() * 7);
                                }
                                else if (diffAngle < .25) { // about 15 degrees
                                    hitPoints = 3 + (int)Math.round(Math.random() * 5);
                                }
                                
                                player.hurt(hitPoints);
                            }
                        }
                    }
                    
                    setState(STATE_TERMINATE);
                }

                break;

            case STATE_HURT:
                if (ticksRemaining <= 0 || health <= 0) {

                    if (health <= 0) {
                        App.getApp().getAudio("/sound/enemy_dead.wav", 1).play();
                        setState(STATE_DYING);
                    }
                    else if (Math.random() < .666) {
                        setState(STATE_TERMINATE);
                    }
                    else {
                        setState(STATE_ASLEEP);
                        // immediate fire
                        aimAngle = angleToPlayer;
                        setState(STATE_FIRE);
                    }

                }
                break;
                
            case STATE_DYING:
                if (ticksRemaining <= 0) {
                    setState(STATE_DEAD);
                    player.setKills(player.getKills() + 1);
                }
                break;
        }
        
        if (!player.isFreezeEnemies()) {
            float newX = getX() + stepx;
            float newY = getY() + stepy;

            if (!isCollision(newX, newY)) {
                setLocation(newX, newY);
            }
            else if (!isCollision(newX, getY())) {
                setX(newX);
            }
            else if (!isCollision(getX(), newY)) {
                setY(newY);
            }
        }
        
        ticksRemaining--;
        ticks++;
        int textureIndex = STATE_TEXTURE[state];
        if (state <= LAST_STATE_WITH_ANIM && ((ticks / 12) & 1) == 0) {
            textureIndex++;
        }
        setTexture(textures[textureIndex]);
    }
    
    private boolean isCollision(float x, float y) {
        int minTileX = (int)(x - getRadius());
        int maxTileX = (int)(x + getRadius());
        int minTileY = (int)(y - getRadius());
        int maxTileY = (int)(y + getRadius());

        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                if (map.isSolidAt(tileX, tileY)) {
                    return true;
                }
            }
        }

        return false;
    }
}
