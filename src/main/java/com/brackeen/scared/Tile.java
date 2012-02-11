package com.brackeen.scared;

import com.brackeen.scared.entity.Entity;
import java.util.ArrayList;
import java.util.List;

public class Tile {
    
    public static final int RENDER_STATE_MAX = (1 << 16);
    
    public static final int TYPE_NOTHING = 0;
    public static final int TYPE_WALL = 1;
    public static final int TYPE_DOOR = 2;  //(subtype: key #)
    public static final int TYPE_WINDOW = 3;  //(subtype: 1=west/east 2=north/south)
    public static final int TYPE_GENERATOR = 4;
    public static final int TYPE_MOVABLE_WALL = 5;
    public static final int TYPE_EXIT = 6;
    public static final int NUM_TYPES = 7;
    
    public int type;
    public int subtype;
    public int state;
    public int renderState;
    public int renderVisible;
    private SoftTexture texture;
    private List<Entity> entities;
    
    /* Checks if the tile is solid for collision purposes. */
    public boolean isSolid() {
        if (type == TYPE_DOOR) {
            return renderState < RENDER_STATE_MAX * 3 / 4;
        }
        else {
            return (type != TYPE_NOTHING);
        }
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public boolean hasEntities() {
        return (entities != null && entities.size() > 0);
    }

    public SoftTexture getTexture() {
        return texture;
    }

    public void setTexture(SoftTexture texture) {
        if (!texture.isPowerOfTwo()) {
            throw new IllegalArgumentException("Texture not a power of two");
        }
        this.texture = texture;
    }
    
    public void addEntity(Entity entity) {
        if (entity.getTile() != null) {
            entity.getTile().removeEntity(entity);
        }
        if (entities == null) {
            entities = new ArrayList<Entity>();
        }
        entities.add(entity);
        entity.setTile(this);
    }
    
    public void removeEntity(Entity entity) {
        if (entity.getTile() == this) {
            entity.setTile( null);
        }
        if (entities != null) {
            entities.remove(entity);
        }
    }
}
