package com.brackeen.scared.entity;

import com.brackeen.scared.SoftTexture;
import com.brackeen.scared.Tile;

public class Entity implements Comparable<Entity> {
    
    public static final int DEFAULT_PIXELS_PER_TILE = 64;
    
    public enum CollisionType {
        NONE,
        SLIDE,
        STOP,
    }
    
    private Tile tile;
    private float radius;
    private float x;
    private float y;
    private float z;
    private float distanceFromCamera;
    private boolean deleted;
    private float direction; // degrees
    private SoftTexture texture;
    private float textureScale = 1f / DEFAULT_PIXELS_PER_TILE;
    
    public Entity(float radius, float x, float y) {
        setRadius(radius);
        setLocation(x, y);
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public SoftTexture getTexture() {
        return texture;
    }

    public void setTexture(SoftTexture texture) {
        this.texture = texture;
    }

    public float getTextureScale() {
        return textureScale;
    }

    public void setTextureScale(float textureScale) {
        this.textureScale = textureScale;
    }

    public float getDistanceFromCamera() {
        return distanceFromCamera;
    }

    public void setDistanceFromCamera(float distanceFromCamera) {
        this.distanceFromCamera = distanceFromCamera;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
    
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
    
    public float getZ() {
        return z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }
    
    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getDirection() {
        return direction;
    }

    public void setDirection(float direction) {
        this.direction = direction;
    }
    
    public void tick() {
        
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void delete() {
        deleted = true;
    }
    
    public boolean onCollisionWithEntityShouldSlide() {
        return false;
    }
    
    public boolean onCollisionWithWallShouldSlide() {
        return false;
    }

    public void notifyPlayerCollision(Player player) {
        // Do nothing
    }
    
    /*
    Notify that a moving entity collided with this entity. 
    Returns true if the moving entity should stop.
    */
    public boolean notifyCollision(Entity movingEntity) {
        if (this instanceof Player) {
            movingEntity.notifyPlayerCollision((Player)this);
        }
        else if (movingEntity instanceof Player) {
            this.notifyPlayerCollision((Player)movingEntity);
        }
        return false;
    }
    
    // Sort back-to-front
    public int compareTo(Entity t) {
        if (distanceFromCamera < t.distanceFromCamera) {
            return 1;
        }
        else if (distanceFromCamera > t.distanceFromCamera) {
            return -1;
        }
        return 0;
    }
}
