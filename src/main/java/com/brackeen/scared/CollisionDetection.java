package com.brackeen.scared;

import com.brackeen.scared.entity.Entity;
import com.brackeen.scared.entity.Player;

public class CollisionDetection {
    
    public static final int WALL_COLLISION_NONE = 0;
    public static final int WALL_COLLISION_NORTH = 1;
    public static final int WALL_COLLISION_SOUTH = 2;
    public static final int WALL_COLLISION_WEST = 4;
    public static final int WALL_COLLISION_EAST = 8;
    
    private static final float SLIDE_ERROR = 0.00002f;
    
    private final Map map;
    
    public CollisionDetection(Map map) {
        this.map = map;
    }
    
    public void move(Entity entity, float newX, float newY, boolean collideWithEntities, boolean collideWithWalls) {
        float oldX = entity.getX();
        float oldY = entity.getY();
        if (oldX == newX && oldY == newY) {
            return;
        }
        
        Tile oldTile = map.getTileAt(entity);
        entity.setLocation(newX, newY);
        
        if (entity.getRadius() > 0) {
            if (collideWithEntities) {
                detectAndHandleEntityCollisions(entity, oldX, oldY);
            }

            if (collideWithWalls) {
                detectAndHandleWallCollisions(entity, oldX, oldY);
            }
        }
        
        Tile newTile = map.getTileAt(entity);
        if (oldTile != newTile) {
            if (oldTile != null) {
                oldTile.removeEntity(entity);
            }
            if (newTile != null) {
                newTile.addEntity(entity);
            }
        }
    }
    
    //
    // Entity collisions
    //
    
    private void detectAndHandleEntityCollisions(Entity movingEntity, float oldX, float oldY) {
        
        Entity[] collidingEntities = checkEntityCollision(movingEntity, oldX, oldY);
        
        // Handle object collision 
        if (collidingEntities == null || collidingEntities.length == 0) {
            return;
        }
        else if (collidingEntities.length == 1) {
            
            Entity collidingEntity = collidingEntities[0];
            
            if (movingEntity.onCollisionWithEntityShouldSlide()) {
                float collisionDx = movingEntity.getX() - collidingEntity.getX();
                float collisionDy = movingEntity.getY() - collidingEntity.getY();
                
                float actualDistanceSq = collisionDx * collisionDx + collisionDy * collisionDy;
                float actualDistance = (float)Math.sqrt(actualDistanceSq);
                
                if (actualDistance <= 0) {
                    movingEntity.setLocation(oldX, oldY);
                }
                else {
                    float goalDistance = movingEntity.getRadius() + collidingEntity.getRadius();
                    
                    movingEntity.setX(collidingEntity.getX() + collisionDx * goalDistance / actualDistance);
                    movingEntity.setY(collidingEntity.getY() + collisionDy * goalDistance / actualDistance);
                }
            }
            else {
                // Circle-Line Intersection
                float angle = (float)Math.atan2(movingEntity.getY() - oldY, movingEntity.getX() - oldX);
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);
                
                float x = collidingEntity.getX() - oldX;
                float y = collidingEntity.getY() - oldY;
                float u = x * cos + y * sin;
                float D = y * cos - x * sin;
                
                float error = 0.005f; // I chose this value at random
                float r = movingEntity.getRadius() + collidingEntity.getRadius() + error;
                float discriminant = r * r - D * D;
                
                if (discriminant < 0) {
                    movingEntity.setLocation(oldX, oldY);
                }
                else {
                    float d = (float)Math.sqrt(discriminant);
                    movingEntity.setLocation(oldX + (u - d) * cos, oldY + (u - d) * sin);
                } 
            }
        }
        else { // collidingObjects.length == 2
            // From "Intersection of two circles" by Paul Bourke
            // http://astronomy.swin.edu.au/~pbourke/geometry/2circle/
            
            Entity P0 = collidingEntities[0];
            Entity P1 = collidingEntities[1];
            
            float dx = P1.getX() - P0.getX();
            float dy = P1.getY() - P0.getY();
            
            float r0 = P0.getRadius() + movingEntity.getRadius();
            float r1 = P1.getRadius() + movingEntity.getRadius();
            
            float r0Squared = r0 * r0;
            float r1Squared = r1 * r1;
            
            float dSquared = dx * dx + dy * dy;
            
            if (dSquared <= 0) {
                movingEntity.setLocation(oldX, oldY);
                return;
            }
            
            float d = (float)Math.sqrt(dSquared);
            
            float a = (r0Squared - r1Squared + dSquared) / (2 * d);
            float aSquared = a * a;
            
            float hSquared = r0Squared - aSquared;
            
            if (hSquared <= 0) {
                movingEntity.setLocation(oldX, oldY);
                return;
            }
            
            float h = (float)Math.sqrt(hSquared);
            
            float midX = P0.getX() + a * dx / d;
            float midY = P0.getY() + a * dy / d;
            
            float sx = h * dy / d;
            float sy = h * dx / d;
            
            // two solutions - use the solution closest to original location.
            float sx1 = midX + sx;
            float sy1 = midY - sy;
            
            float sx2 = midX - sx;
            float sy2 = midY + sy;
            
            float d1 = (sx1 - oldX) * (sx1 - oldX) + (sy1 - oldY) * (sy1 - oldY);
            float d2 = (sx2 - oldX) * (sx2 - oldX) + (sy2 - oldY) * (sy2 - oldY);
            
            if (d1 < d2) {
                movingEntity.setLocation(sx1, sy1);
            }
            else if (d1 > d2) {
                movingEntity.setLocation(sx2, sy2);
            }
            else {
                movingEntity.setLocation(oldX, oldY);
            }
        }
    }
    
    /**
    Checks the object's path for any collisions.
    */
    private Entity[] checkEntityCollision(Entity movingEntity, float oldX, float oldY) {
        
        float newX = movingEntity.getX();
        float newY = movingEntity.getY();
        
        float dx = newX - oldX;
        float dy = newY - oldY;
        
        float distanceSquared = dx * dx + dy * dy;
        
        // the shorter the maxDistance, the more accurate it'll be.
        float maxDistance = movingEntity.getRadius();
        float maxDistanceSquared = maxDistance * maxDistance;
       
        if (distanceSquared > maxDistanceSquared) {
        
            // follow the path
            int steps = (int)(Math.sqrt(distanceSquared) / maxDistance);
            
            for (int i = 1; i <= steps; i++) {
                movingEntity.setLocation(oldX + i * dx / steps, oldY + i * dy / steps);
                
                Entity[] entities = checkEntityCollisionAtPoint(movingEntity);
                
                if (entities != null) {
                    return entities;
                }
            }
            
            movingEntity.setLocation(newX, newY);
        }
        
        return checkEntityCollisionAtPoint(movingEntity);
    }
    
    /**
    Returns up to two entity collisions of the moving entity at it's current location.
    */
    private Entity[] checkEntityCollisionAtPoint(Entity movingEntity) {
        
        int tileX = (int)movingEntity.getX();
        int tileY = (int)movingEntity.getY();
        
        Entity closestEntity = null;
        Entity secondClosestEntity = null;
        float closestDist = Float.MAX_VALUE;
        float secondClosestDist = Float.MAX_VALUE;
        
        // check this tile and 8 surrounding tiles
        for (int x = tileX-1; x <= tileX+1; x++) {
            
            if (movingEntity.isDeleted()) {
                break;
            }
            
            for (int y = tileY-1; y <= tileY+1; y++) {
                
                if (movingEntity.isDeleted()) {
                    break;
                }
                
                Tile tile = map.getTileAt(x, y);
                
                if (tile == null || !tile.hasEntities()) {
                    continue;
                }
                
                for (Entity entity : tile.getEntities()) {

                    if (movingEntity.isDeleted()) {
                        break;
                    }
                    
                    if (movingEntity == entity || entity.isDeleted() || entity.getRadius() <= 0) {
                        continue;
                    }
                    
                    float dx = entity.getX() - movingEntity.getX();
                    float dy = entity.getY() - movingEntity.getY();
                    
                    // Don't collide with entities in exact same location
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    
                    float r = movingEntity.getRadius() + entity.getRadius();
                    float r2 = r * r;
                    
                    float d2 = dx*dx + dy*dy;
                    
                    if (d2 < r2) {
                        // collision found
                        boolean handleCollision = entity.notifyCollision(movingEntity);
                        
                        if (handleCollision) {
                            if (closestEntity == null) {
                                closestEntity = entity;
                                closestDist = d2;
                            }
                            else if (d2 < closestDist) {
                                secondClosestEntity = closestEntity;
                                secondClosestDist = closestDist;
                                closestEntity = entity;
                                closestDist = d2;
                            }
                            else if (secondClosestEntity == null || d2 < secondClosestDist) {
                                secondClosestEntity = entity;
                                secondClosestDist = d2;
                            }
                        }
                    }
                }
            }
        }
        
        if (closestEntity == null) {
            return null;
        }
        else if (secondClosestEntity == null) {
            return new Entity[] { closestEntity };
        }
        else {
            return new Entity[] { closestEntity, secondClosestEntity };
        }
    }
   
    //
    // Wall collisions
    //
    
    private void detectAndHandleWallCollisions(Entity entity, float oldX, float oldY) {
    
        boolean isPlayer = (entity instanceof Player);
        
        int collision = checkWallCollision(oldX, oldY, entity.getX(), entity.getY(), entity.getRadius(), isPlayer);        
        if (collision == WALL_COLLISION_NONE) {
            if (isPlayer) {
                map.notifyPlayerTouchedNoWall();
            }
        }
        else if (entity.onCollisionWithWallShouldSlide()) {
            float altX = entity.getX();
            float altY = entity.getY();
            boolean collisionX = false;
            boolean collisionY = false;
            
            if ((collision & WALL_COLLISION_WEST) != 0) {
                altX = (float)Math.floor(oldX) + entity.getRadius() + SLIDE_ERROR;
                collisionX = true;
            }
            else if ((collision & WALL_COLLISION_EAST) != 0) {
                altX = (float)Math.ceil(oldX) - entity.getRadius() - SLIDE_ERROR;
                collisionX = true;
            }
            
            if ((collision & WALL_COLLISION_NORTH) != 0) {
                altY = (float)Math.floor(oldY) + entity.getRadius() + SLIDE_ERROR;
                collisionY = true;
            }
            else if ((collision & WALL_COLLISION_SOUTH) != 0) {
                altY = (float)Math.ceil(oldY) - entity.getRadius() - SLIDE_ERROR;
                collisionY = true;
            }
            
            if (collisionX && collisionY) {
                if (checkWallCollision(oldX, oldY, altX, entity.getY(), entity.getRadius(), isPlayer) == WALL_COLLISION_NONE) {
                    altY = entity.getY();
                }
                else if (checkWallCollision(oldX, oldY, entity.getX(), altY, entity.getRadius(), isPlayer) == WALL_COLLISION_NONE) {
                    altX = entity.getX();
                }
            }
            
            entity.setLocation(altX, altY);
        }
        else {
            float newX = entity.getX();
            float newY = entity.getY();
            
            float cX1 = 0;
            float cY1 = 0;
            float cX2 = 0;
            float cY2 = 0;
            boolean collisionX = false;
            boolean collisionY = false;
            
            if ((collision & WALL_COLLISION_WEST) != 0) {
                cX1 = (float)Math.floor(oldX) + entity.getRadius() + SLIDE_ERROR;
                cY1 = oldY + (cX1 - oldX) * (newY - oldY) / (newX - oldX);
                if (!map.isSolidAt((int)cX1, (int)cY1)) {
                    collisionX = true;
                }
                
            }
            else if ((collision & WALL_COLLISION_EAST) != 0) {
                cX1 = (float)Math.ceil(oldX) - entity.getRadius() - SLIDE_ERROR;
                cY1 = oldY + (cX1 - oldX) * (newY - oldY) / (newX - oldX);
                if (!map.isSolidAt((int)cX1, (int)cY1)) {
                    collisionX = true;
                }
            }
            
            if ((collision & WALL_COLLISION_NORTH) != 0) {
                cY2 = (float)Math.floor(oldY) + entity.getRadius() + SLIDE_ERROR;
                cX2 = oldX + (cY2 - oldY) * (newX - oldX) / (newY - oldY);
                if (!map.isSolidAt((int)cX2, (int)cY2)) {
                    collisionY = true;
                }
            }
            else if ((collision & WALL_COLLISION_SOUTH) != 0) {
                cY2 = (float)Math.ceil(oldY) - entity.getRadius() - SLIDE_ERROR;
                cX2 = oldX + (cY2 - oldY) * (newX - oldX) / (newY - oldY);
                if (!map.isSolidAt((int)cX2, (int)cY2)) {
                    collisionY = true;
                }
            }
            
            if (collisionX && collisionY) {
                // if more than one collision, use the collision closest to the old position
                float dist1Sq = (cX1 - oldX) * (cX1 - oldX) + (cY1 - oldY) * (cY1 - oldY);
                float dist2Sq = (cX2 - oldX) * (cX2 - oldX) + (cY2 - oldY) * (cY2 - oldY);
                
                if (dist1Sq <= dist2Sq) {
                    newX = cX1;
                    newY = cY1;
                    collisionY = false;
                }
                else {
                    newX = cX2;
                    newY = cY2;
                    collisionX = true;
                }
            }
            else if (collisionX) {
                newX = cX1;
                newY = cY1;
            }
            else if (collisionY) {
                newX = cX2;
                newY = cY2;
            }
            else {
                // no good "stop" result - go back to old location
                entity.setLocation(oldX, oldY);
                return;
            }
            
            entity.setLocation(newX, newY);
        }
   }
   
      
   /**
       Returns integer with flags WALL_COLLISION_WEST, WALL_COLLISION_EAST,
       WALL_COLLISION_NORTH, and/or WALL_COLLISION_SOUTH; 
       or returns WALL_COLLISION_NONE if there is no wall collision.
   */
   private int checkWallCollision(float oldX, float oldY, float x, float y, float radius, boolean isPlayer) {
        
        int originTileX = (int)oldX;
        int originTileY = (int)oldY;

        int x1 = (int)(x - radius);
        int y1 = (int)(y - radius);
        int x2 = (int)(x + radius);
        int y2 = (int)(y + radius);
        
        int collision = 0;

        // check for solid walls
        for (int tileY = y1; tileY <= y2; tileY++) {
            
            for (int tileX = x1; tileX <= x2; tileX++) {
                
                Tile tile = map.getTileAt(tileX, tileY);
                
                // Treat out-of-bounds tiles as "solid"
                if (tile != null && !tile.isSolid()) {
                    continue;
                }
                    
                if (tileX < originTileX && x < oldX) {
                    collision |= WALL_COLLISION_WEST;
                }
                else if (tileX > originTileX && x > oldX) {
                    collision |= WALL_COLLISION_EAST;
                }
                
                if (tileY < originTileY && y < oldY) {
                    collision |= WALL_COLLISION_NORTH;
                }
                else if (tileY > originTileY && y > oldY) {
                    collision |= WALL_COLLISION_SOUTH;
                }
                
                if (isPlayer && tile != null && tile.renderState == 0) {
                    map.notifyPlayerTouchedWall(tile, tileX, tileY);
                }
            }
        }
        
        return collision;
    }
}
