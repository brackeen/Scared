package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.scared.action.Action;
import com.brackeen.scared.action.DoorAction;
import com.brackeen.scared.action.GeneratorAction;
import com.brackeen.scared.action.MoveableWallAction;
import com.brackeen.scared.entity.Ammo;
import com.brackeen.scared.entity.Enemy;
import com.brackeen.scared.entity.Entity;
import com.brackeen.scared.entity.Key;
import com.brackeen.scared.entity.MedKit;
import com.brackeen.scared.entity.Player;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Map {
    
    private int width;
    private int height;
    
    private Player player;
    private List<Entity> entities = new ArrayList<Entity>();
    private Tile[][] tiles;
    private boolean electricityOn = true;
    private List<Action> actions = new ArrayList<Action>();
    private SoftTexture defaultFloorTexture;
    private SoftTexture exitButtonOnTexture;
    private SoftTexture generatorOnTexture;
    private boolean exitFound = false;
    private Tile lastCollidedWall;
    private MessageQueue messageQueue;

    private int numSecrets = 0;
    private int numEnemies = 0;
    
    public Map(HashMap<String, SoftTexture> textureCache, MessageQueue messageQueue, String mapName, Player oldPlayer) throws IOException {
        
        this.messageQueue = messageQueue;
        
        SoftTexture[] enemyTextures = new SoftTexture[Enemy.NUM_IMAGES];
        for (int i = 0; i < Enemy.NUM_IMAGES; i++) {
            enemyTextures[i] = textureCache.get("/enemy/" + i + ".png");
        }

        defaultFloorTexture = textureCache.get("wall00.png");
        generatorOnTexture = textureCache.get("generator01.png");
        exitButtonOnTexture = textureCache.get("exit01.png");
        
        player = new Player(this);
        if (oldPlayer != null) {
            player.setHealth(Math.max(oldPlayer.getHealth(), player.getHealth()));
            player.setAmmo(Math.max(oldPlayer.getAmmo(), player.getAmmo()));
            player.setGodMode(oldPlayer.isGodMode());
            player.setFreezeEnemies(oldPlayer.isFreezeEnemies());
        }
        addEntity(player);
        
        InputStream stream = getClass().getResourceAsStream(mapName);
        if (stream == null) {
            throw new IOException("Not found: " + mapName);
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        try {
            // Width
            line = r.readLine();
            if (!line.startsWith("w=")) {
                throw new IOException("Illegal width line: " + line);
            }
            width = Integer.parseInt(line.substring(2));

            // Height
            line = r.readLine();
            if (!line.startsWith("h=")) {
                throw new IOException("Illegal height line: " + line);
            }
            height = Integer.parseInt(line.substring(2));

            // Direction
            line = r.readLine();
            if (!line.startsWith("dir=")) {
                throw new IOException("Illegal dir line: " + line);
            }
            player.setDirection(Integer.parseInt(line.substring(4)));

            tiles = new Tile[width][height];

            // Read tile types
            for (int y = 0; y < height; y++) {
                line = r.readLine();
                if (line.length() != width) {
                    throw new IOException("Wrong width: " + line);
                }
                for (int x = 0; x < width; x++) {
                    Tile tile = new Tile();

                    tiles[x][y] = tile;

                    switch (line.charAt(x)) {
                        case ' ':
                            tile.type = Tile.TYPE_NOTHING;
                            break;
                        case '#':
                            tile.type = Tile.TYPE_WALL;
                            break;
                        case 'A':
                            tile.type = Tile.TYPE_DOOR;
                            break;
                        case 'B':
                            tile.type = Tile.TYPE_DOOR;
                            tile.subtype = 1;
                            break;
                        case 'C':
                            tile.type = Tile.TYPE_DOOR;
                            tile.subtype = 2;
                            break;
                        case 'D':
                            tile.type = Tile.TYPE_DOOR;
                            tile.subtype = 3;
                            break;
                        case '-':
                            tile.type = Tile.TYPE_WINDOW;
                            tile.subtype = 1;
                            break;
                        case '|':
                            tile.type = Tile.TYPE_WINDOW;
                            tile.subtype = 2;
                            break;
                        case '+':
                            tile.type = Tile.TYPE_WINDOW;
                            tile.subtype = 3;
                            break;
                        case '*':
                            tile.type = Tile.TYPE_GENERATOR;
                            electricityOn = false;
                            break;
                        case '@':
                            tile.type = Tile.TYPE_MOVABLE_WALL;
                            numSecrets++;
                            break;
                        case 'X':
                            tile.type = Tile.TYPE_EXIT;
                            break;
                        case 'S':
                            tile.type = Tile.TYPE_NOTHING;
                            player.setLocation(x + 0.5f, y + 0.5f);
                            break;
                        case '^':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new Enemy(this, enemyTextures, x + 0.5f, y + 0.5f, 1));
                            numEnemies++;
                            break;
                        case 'b':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new Key(this, textureCache.get("/sprites/key01.png"), x + 0.5f, y + 0.5f, 1));
                            break;
                        case 'c':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new Key(this, textureCache.get("/sprites/key02.png"), x + 0.5f, y + 0.5f, 2));
                            break;
                        case 'd':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new Key(this, textureCache.get("/sprites/key03.png"), x + 0.5f, y + 0.5f, 3));
                            break;
                        case 'h':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new MedKit(this, textureCache.get("/sprites/medkit.png"), x + 0.5f, y + 0.5f, false));
                            break;
                        case 'H':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new MedKit(this, textureCache.get("/sprites/nuclear.png"), x + 0.5f, y + 0.5f, true));
                            break;
                        case 'm':
                            tile.type = Tile.TYPE_NOTHING;
                            addEntity(new Ammo(this, textureCache.get("/sprites/ammo.png"), x + 0.5f, y + 0.5f));
                            break;
                        default:
                            tile.type = Tile.TYPE_NOTHING;
                            App.log("Invalid char: " + line.charAt(x));
                            break;
                    }
                }
            }

            line = r.readLine();

            // Read textures
            for (int y = 0; y < height; y++) {
                line = r.readLine();
                if (line.length() != width) {
                    throw new IOException("Wrong width: " + line);
                }
                for (int x = 0; x < width; x++) {
                    Tile tile = tiles[x][y];

                    if (tile.type == Tile.TYPE_GENERATOR) {
                        tile.setTexture(textureCache.get("generator00.png"));
                    }
                    else if (tile.type == Tile.TYPE_EXIT) {
                        tile.setTexture(textureCache.get("exit00.png"));
                    }
                    else {
                        int textureIndex = Integer.parseInt(line.substring(x, x + 1), 16);
                        if (textureIndex < 10) {
                            tile.setTexture(textureCache.get("wall0" + textureIndex + ".png"));
                        }
                        else {
                            tile.setTexture(textureCache.get("wall" + textureIndex + ".png"));
                        }
                    }
                }
            }
        }
        catch (NumberFormatException ex) {
            throw new IOException(ex);
        }
    }
    
    public void setMessage(String message) {
        messageQueue.add(message);
    }

    public SoftTexture getDefaultFloorTexture() {
        return defaultFloorTexture;
    }

    public void setDefaultFloorTexture(SoftTexture defaultFloorTexture) {
        this.defaultFloorTexture = defaultFloorTexture;
    }
    
    public boolean isExitFound() {
        return exitFound;
    }
    
    public void tick() {
        // Handle regular actions
        Iterator<Action> i = actions.iterator();
        while (i.hasNext()) {
            Action action = i.next();
            action.tick();
            if (action.isFinished()) {
                action.unload();
                i.remove();
            }
        }
         
        // Move entities
        Iterator<Entity> i2 = entities.iterator();
        while (i2.hasNext()) {
            Entity entity = i2.next();
            tickEntity(entity);
            if (entity.isDeleted()) {
                i2.remove();
            }
        }
    }
    
    private boolean tickEntity(Entity entity) {
        Tile oldTile = entity.getTile();
        entity.tick();
        if (entity.isDeleted()) {
            if (oldTile != null) {
                oldTile.removeEntity(entity);
            }
            return true;
        }
        else {
            Tile newTile = getTileAt(entity);
            if (oldTile != newTile) {
                if (oldTile != null) {
                    oldTile.removeEntity(entity);
                }
                if (newTile != null) {
                    newTile.addEntity(entity);
                }
            }
            return false;
        }
    }
    
    public void unload() {
        for (Action action : actions) {
            action.unload();
        }
    }
    
    public int getNumActions() {
        return actions.size();
    }
    
    public int getNumEntities() {
        return entities.size();
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
        
        Tile tile = getTileAt(entity);
            
        if (tile != null) {
            tile.addEntity(entity);
        }
    }

    public Player getPlayer() {
        return player;
    }
    
    public boolean isElectricityOn() {
        return electricityOn;
    }

    public void setElectricityOn(boolean electricityOn) {
        this.electricityOn = electricityOn;
    }

    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    public int getNumEnemies() {
        return numEnemies;
    }

    public int getNumSecrets() {
        return numSecrets;
    }
    
    public Tile getTileAt(Entity entity) {
        return getTileAt((int)entity.getX(), (int)entity.getY());
    }

    public Tile getTileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        
        return tiles[x][y];
    }
    
    public boolean isSolidAt(int tileX, int tileY) {
        Tile tile = getTileAt(tileX, tileY);
        
        if (tile == null) {
            return true;
        }
        else {
            return tile.isSolid();
        }
    }

    public void notifyPlayerEnteredTile(int tileX, int tileY) {
        Tile tile = getTileAt(tileX, tileY);
        if (tile != null && isElectricityOn()) {
            // check if surrounding tiles are a door
            if (isUnlockedDoor(tileX, tileY - 1)) {
                activateDoor(tileX, tileY - 1);
            }
            if (isUnlockedDoor(tileX, tileY + 1)) {
                activateDoor(tileX, tileY + 1);
            }
            if (isUnlockedDoor(tileX - 1, tileY)) {
                activateDoor(tileX - 1, tileY);
            }
            if (isUnlockedDoor(tileX + 1, tileY)) {
                activateDoor(tileX + 1, tileY);
            }
        }
    }
        
    private boolean isUnlockedDoor(int tileX, int tileY) {
        Tile tile = getTileAt(tileX, tileY);
        if (tile != null) {
            if (tile.type == Tile.TYPE_DOOR && player.hasKey(tile.subtype)) {
                return true;
            }
        }
        return false;
    }
    private void activateDoor(int tileX, int tileY) {
        
        // Check to see if there already is a door action
        for (Action action : actions) {
            if (action instanceof DoorAction) {
                DoorAction doorAction = (DoorAction)action;
                
                if (doorAction.getTileX() == tileX && 
                    doorAction.getTileY() == tileY) 
                {
                    return;
                }
            }
        }
        actions.add(new DoorAction(this, tileX, tileY));
    }
    
    public void notifyPlayerTouchedNoWall() {
        lastCollidedWall = null;
    }

    public void notifyPlayerTouchedWall(Tile tile, int tileX, int tileY) {
        if (tile.type == Tile.TYPE_MOVABLE_WALL) { 
            if (tile.state == MoveableWallAction.STATE_DONE) {
                int dx = tileX - (int)player.getX();
                int dy = tileY - (int)player.getY();

                if ((dx == 0 && Math.abs(dy) == 1) || (dy == 0 && Math.abs(dx) == 1)) {
                    actions.add(new MoveableWallAction(this, tileX, tileY));
                    player.setSecrets(player.getSecrets() + 1);
                }
            }
        }
        else if (tile.type == Tile.TYPE_EXIT) { 
            if (tile.state == 0) {
                tile.state = 1;
                tile.setTexture(exitButtonOnTexture);
                App.getApp().getAudio("/sound/endlevel.wav", 1).play();
                exitFound = true;
            }
        }
        else if (tile.type == Tile.TYPE_GENERATOR) { 
            if (tile.state == 0) {
                tile.state = 1;
                tile.setTexture(generatorOnTexture);
                actions.add(new GeneratorAction(this, tileX, tileY));
                electricityOn = true;
                setMessage("The power is now on");
            }
        }
        else if (tile.type == Tile.TYPE_DOOR) {
            if (tile != lastCollidedWall) {
                lastCollidedWall = tile;
                if (!electricityOn) {
                    setMessage("The power is off");
                    App.getApp().getAudio("/sound/no_ammo.wav", 1).play();
                }
                else if (!player.hasKey(tile.subtype)) {
                    setMessage("The door is locked");
                    App.getApp().getAudio("/sound/no_ammo.wav", 1).play();
                }
            }
        }
    }
    
    public List<Entity> getCollisions(Class <? extends Entity> entityClass, 
            float x1, float y1, float x2, float y2) {
        List<Entity> hitEntities = new ArrayList<Entity>();
        
        float dx = x2 - x1;
        float dy = y2 - y1;
        float segmentLengthSq = dx * dx + dy * dy;
        
        List<? extends Entity> entitiesToSearch;
        if (entityClass == Player.class) {
            entitiesToSearch = Collections.singletonList(player);
        }
        else {
            entitiesToSearch = entities;
        }
        
        for (Entity entity : entitiesToSearch) {
            float radius = entity.getRadius();
            if (radius > 0 && entity.getClass().isAssignableFrom(entityClass)) {

                // Point-to-line distance
                float pointX = entity.getX();
                float pointY = entity.getY();
                dx = pointX - x1;
                dy = pointY - y1;

                float u = ((pointX - x1) * (x2 - x1) +
                    (pointY - y1) * (y2 - y1) ) / segmentLengthSq;

                if (u < 0 || u > 1) {
                    // Not within the segment
                    continue;
                }

                float intersectionX = x1 + u * (x2 - x1);
                float intersectionY = y1 + u * (y2 - y1);

                dx = pointX - intersectionX;
                dy = pointY - intersectionY;
                float distToRaySq = dx * dx + dy * dy;
                float radiusSq = radius * radius;
                if (distToRaySq <= radiusSq) {
                    hitEntities.add(entity);
                }
            }
        }
        return hitEntities;
    }

    public Point2D.Float getWallCollision(float x, float y, float angleInDegrees) {
        
        int tileX = (int)x;
        int tileY = (int)y;
        
        if (isSolidAt(tileX, tileY)) {
            return null;
        }
        
        float dx = (float)Math.cos(Math.toRadians(angleInDegrees));
        float dy = (float)Math.sin(Math.toRadians(angleInDegrees));
        
        Point2D.Float p1 = getCollisionPart2(x, y, dx, dy, false);
        Point2D.Float p2 = getCollisionPart2(y, x, dy, dx, true);
        
        if (p1 == null) {
            return p2;
        }
        else if (p2 == null) {
            return p1;
        }
        else {
            // Nearest collision
            float dx1 = p1.x - x;
            float dy1 = p1.y - y;
            float dx2 = p2.x - x;
            float dy2 = p2.y - y;
            
            float d1Sq = dx1 * dx1 + dy1 * dy1;
            float d2Sq = dx2 * dx2 + dy2 * dy2;
            
            if (d1Sq < d2Sq) {
                return p1;
            }
            else {
                return p2;
            }
        }
    }
    
    private Point2D.Float getCollisionPart2(float x, float y, float dx, float dy, boolean inversed) {
        
        if (dx == 0) {
            return null;
        }
        
        float fx;
        float fdx;
        
        if (dx < 0) {
            fx = (float)Math.floor(x) - 0.00001f;
            fdx = -1;
        }
        else {
            fx = (float)Math.ceil(x);
            fdx = 1;
        }
        
        float fdy = dy / Math.abs(dx);
        float fy = y + Math.abs(fx - x) * fdy;
        
        if (inversed) {
            return getCollisionPart3(fy, fx, fdy, fdx);
        }
        else {
            return getCollisionPart3(fx, fy, fdx, fdy);
        }
    }
    
    private Point2D.Float getCollisionPart3(float x, float y, float dx, float dy) {
        while (true) {
            int tileX = (int)x;
            int tileY = (int)y;
            if (isSolidAt(tileX, tileY)) {
                return new Point2D.Float(x, y);
            }
            
            x += dx;
            y += dy;
        }
    }
}
