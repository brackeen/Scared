package com.brackeen.scared;

import com.brackeen.scared.entity.Player;
import com.brackeen.app.App;
import com.brackeen.app.BitmapFont;
import com.brackeen.app.BufferedAudio;
import com.brackeen.app.view.ImageView;
import com.brackeen.app.view.Label;
import com.brackeen.app.view.Scene;
import com.brackeen.app.view.View;
import com.brackeen.scared.entity.BlastMark;
import com.brackeen.scared.entity.Enemy;
import com.brackeen.scared.entity.Entity;
import com.brackeen.scared.entity.Key;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class GameScene extends Scene {
    
    private static final boolean DEBUG_ALLOW_CAMERA_Z_CHANGES = false;
    
    private static final int NUM_LEVELS = 7;
    
    private static final float MIN_RUN_VELOCITY = -0.055f;
    private static final float MAX_RUN_VELOCITY = 0.078f;
    private static final float MIN_STRAFE_VELOCITY = -0.055f;
    private static final float MAX_STRAFE_VELOCITY = 0.055f;
    private static final float MAX_TURN_VELOCITY = 360 / 128f;
    private static final float MIN_TURN_VELOCITY = -MAX_TURN_VELOCITY;
    
    private static final float RUN_ACCEL = 0.0047f;
    private static final float RUN_DECEL = 0.0047f;
    private static final float STRAFE_ACCEL = 0.0039f;
    private static final float STRAFE_DECEL = 0.0039f;
    private static final float TURN_ACCEL = 360 / 680f;
    private static final float TURN_DECEL = 360 / 512f;
    
    private static final int FIRE_COUNTDOWN = 15;
    private static final int GUN_BLAST_COUNTDOWN = 6;
    
    private static final int ACTION_NONE = 0;
    private static final int ACTION_NEW_LEVEL = 1;
    private static final int ACTION_WIN = 2;
        
    private HashMap<String, SoftTexture> textureCache = new HashMap<String, SoftTexture>();
    
    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyDown = false;
    private boolean keyUp = false;
    private boolean keyStrafeLeft = false;
    private boolean keyStrafeRight = false;
    private boolean keyStrafeModifier = false;
    private boolean keyFire = false;
    private boolean mousePressed = false;
       
    private SoftRender3D renderer;
    private Map map;
    private CollisionDetection collisionDetection;
    private int level;
    private boolean hasWon;
    
    private float runVelocity = 0;
    private float strafeVelocity = 0;
    private float turnVelocity = 0;
    
    private int ticksUntilRefire;
    private int nextAction = ACTION_NONE;
    private int nextActionTicksRemaining;
    
    private SoftTexture[] blastTextures = new SoftTexture[3];
    
    // HUD
    private MessageQueue messageQueue = new MessageQueue(4);
    private Label[] messageLabels = new Label[4];
    private Label focusLostLabel;
    private boolean paused;
    private ImageView[] keys = new ImageView[Key.NUM_KEYS];
    private View normalStats;
    private View specialStats;
    private Label healthLabel;
    private Label ammoLabel;
    private Label enemiesLabel;
    private Label secretsLabel;
    private Label levelLabel;
    private int ticksUntilHideSpecialStats;
    private ImageView gunView;
    private ImageView gunBlastView;
    private int gunBlastCountdown;
    private int deathTicksRemaining;
    private View warningSplash;
    private Label gameOverWinMessage;
    private Label gameOverLoseMessage;
    private ImageView crosshair;
    
    @Override
    public void onLoad() {
        
        App app = App.getApp();
        
        BitmapFont messageFont = new BitmapFont(app.getImage("/ui/message_font.png"), 11, ' ');
        BitmapFont scoreFont1 = new BitmapFont(app.getImage("/ui/score_font1.png"), 19, '0');
        BitmapFont scoreFont2 = new BitmapFont(app.getImage("/ui/score_font2.png"), 19, '0');
        scoreFont1.setTracking(2);
        scoreFont2.setTracking(2);
        
        // Cache textures
        // NOTE: Java has trouble with indexed PNG images with a pallete of less than 16 colors.
        // PNG optimizers create these. Images created from Photoshop or other major tools are fine.
        getTexture("/sprites/key01.png", true);
        getTexture("/sprites/key02.png", true);
        getTexture("/sprites/key03.png", true);
        getTexture("/sprites/medkit.png", true);
        getTexture("/sprites/ammo.png", true);
        getTexture("/sprites/nuclear.png", true);
        blastTextures[0] = getTexture("/sprites/blast1.png", false);
        blastTextures[1] = getTexture("/sprites/blast2.png", false);
        blastTextures[2] = getTexture("/sprites/blast3.png", false);
        
        for (int i = 0; i < Enemy.NUM_IMAGES; i++) {
            getTexture("/enemy/" + i + ".png", true);
        }
        
        // All textures must be a size that is a power-of-two. 128x128, 64x64, etc.
        String[] textures = {
            "door00.png",
            "door01.png",
            "door02.png",
            "door03.png",
            "exit00.png",
            "exit01.png",
            "generator00.png",
            "generator01.png",
            "wall00.png",
            "wall01.png",
            "wall02.png",
            "wall03.png",
            "wall04.png",
            "wall05.png",
            "wall06.png",
            "wall07.png",
            "wall08.png",
            "wall09.png",
            "wall10.png",
            "wall11.png",
            "wall12.png",
            "wall13.png",
            "wall14.png",
            "wall15.png",
            "window00.png",
        };
        int[] mipMapSizes = { 64, 32, 16 };
        for (String textureName : textures) {
            SoftTexture texture = null;
            SoftTexture lastTexture = null;
            for (int i = 0; i < mipMapSizes.length; i++) {
                int size = mipMapSizes[i];
                String fullname = "/texture" + size + "/" + textureName;
                SoftTexture thisTexture = getTexture(fullname, false);
                if (lastTexture == null) {
                    texture = thisTexture;
                }
                else {
                    lastTexture.setHalfSizeTexture(thisTexture);
                }
                lastTexture = thisTexture;
            }
            textureCache.put(textureName, texture);
        }
                
        renderer = new SoftRender3D(textureCache, getWidth(), getHeight());
        //renderer.setPixelScale(4);
        addSubview(renderer);
        
        // Crosshair
        crosshair = new ImageView(app.getImage("/hud/crosshair.png"));
        crosshair.setAnchor(0.5f, 0.5f);
        addSubview(crosshair);
        
        // Gun
        gunBlastView = new ImageView(app.getImage("/hud/gun02.png"));
        gunBlastView.setVisible(false);
        gunBlastView.setLocation(getWidth() / 2, getHeight());
        addSubview(gunBlastView);
        gunView = new ImageView(app.getImage("/hud/gun01.png"));
        gunView.setLocation(getWidth() / 2, getHeight());
        addSubview(gunView);
        
        // Red warning splash
        int borderSize = 6;
        warningSplash = new View();
        warningSplash.setVisible(false);
        View view = new View(0, 0, getWidth(), borderSize);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(0, borderSize, borderSize, getHeight() - borderSize*2);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(getWidth() - borderSize, borderSize, borderSize, getHeight() - borderSize*2);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(0, getHeight() - borderSize, getWidth(), borderSize);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        addSubview(warningSplash);
        
        float hudOpacity = 0.5f;
        
        // Message queue
        final int spacing = 4;
        float x = spacing;
        float y = spacing;
        for (int i = 0; i < messageQueue.getMaxSize(); i++) {
            messageLabels[i] = new Label(messageFont, "");
            messageLabels[i].setLocation(x, y);
            messageLabels[i].setOpacity(hudOpacity);
            addSubview(messageLabels[i]);
            y += messageLabels[i].getHeight();
        }
        
        // Keys
        x = getWidth() - spacing;
        y = getHeight() - spacing;
        for (int i = 0; i < Key.NUM_KEYS; i++) {
            keys[i] = new ImageView(App.getApp().getImage("/sprites/key0" + (i+1) + ".png"));
            keys[i].setAnchor(1, 1);
            keys[i].setLocation(x, y);
            keys[i].setOpacity(hudOpacity);
            keys[i].setVisible(false);
            addSubview(keys[i]);
            x -= keys[i].getWidth() - spacing;
        }
        
        // Health/ammo
        normalStats = new View();
        normalStats.setOpacity(hudOpacity);
        healthLabel = new Label(scoreFont1, Integer.toString(Player.DEFAULT_HEALTH));
        healthLabel.setLocation(spacing*4 + scoreFont1.getStringWidth("000")/2, getHeight() - spacing*2);
        healthLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(healthLabel);
        Label label = new Label(messageFont, "HEALTH");
        label.setLocation(healthLabel.getX(), healthLabel.getY() - healthLabel.getHeight() - spacing);
        label.setAnchor(0.5f, 1);
        normalStats.addSubview(label);
        
        ammoLabel = new Label(scoreFont1, Integer.toString(Player.DEFAULT_AMMO));
        ammoLabel.setLocation(healthLabel.getX() + spacing*4 + scoreFont1.getStringWidth("000"), getHeight() - spacing*2);
        ammoLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(ammoLabel);
        label = new Label(messageFont, "AMMO");
        label.setLocation(ammoLabel.getX(), ammoLabel.getY() - ammoLabel.getHeight() - spacing);
        label.setAnchor(0.5f, 1);
        normalStats.addSubview(label);
        addSubview(normalStats);
        
        // Secrets/enemies
        specialStats = new View();
        specialStats.setOpacity(hudOpacity);
        specialStats.setVisible(false);
        secretsLabel = new Label(messageFont, "Secrets: 0/0");
        secretsLabel.setAnchor(0, 1);
        secretsLabel.setLocation(spacing * 2, getHeight() - spacing * 2);
        specialStats.addSubview(secretsLabel);
        enemiesLabel = new Label(messageFont, "Enemies: 0/0");
        enemiesLabel.setAnchor(0, 1);
        enemiesLabel.setLocation(spacing * 2, secretsLabel.getY() - spacing - secretsLabel.getHeight());
        specialStats.addSubview(enemiesLabel);
        levelLabel = new Label(messageFont, "Level 1");
        levelLabel.setAnchor(0, 1);
        levelLabel.setLocation(spacing * 2, enemiesLabel.getY() - spacing - enemiesLabel.getHeight());
        specialStats.addSubview(levelLabel);
        addSubview(specialStats);

        // Focus message
        focusLostLabel = new Label(messageFont, "Click to continue");
        focusLostLabel.setAnchor(0.5f, 0.5f);
        focusLostLabel.setLocation(getWidth() / 2, getHeight() / 2);
        focusLostLabel.setVisible(false);
        addSubview(focusLostLabel);
        
        // Win/lose messages
        gameOverWinMessage = new Label(messageFont, "YOU WIN. Click to play again.");
        gameOverWinMessage.setAnchor(0.5f, 0.5f);
        gameOverWinMessage.setLocation(getWidth() / 2, focusLostLabel.getY() + focusLostLabel.getHeight());
        gameOverWinMessage.setVisible(false);
        addSubview(gameOverWinMessage);
        gameOverLoseMessage = new Label(messageFont, "Click to try again");
        gameOverLoseMessage.setAnchor(0.5f, 0.5f);
        gameOverLoseMessage.setLocation(getWidth() / 2, focusLostLabel.getY() + focusLostLabel.getHeight());
        gameOverLoseMessage.setVisible(false);
        addSubview(gameOverLoseMessage);
        
        // Hide the cursor
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension size = toolkit.getBestCursorSize(32, 32);
            if (size != null && size.width > 0 && size.height > 0) {
                BufferedImage cursorImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                Cursor noCursor = toolkit.createCustomCursor(cursorImage, new Point(0,0), "none");
                setCursor(noCursor);
            }
        }
        catch (Exception ex) {
            // Ignore it
        }
        
        setKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent ke) {
                // Do nothing
            }

            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().pushScene(new ConsoleScene(GameScene.this));
                }
                else if (ke.getKeyCode() == KeyEvent.VK_X) {
                    crosshair.setVisible(!crosshair.isVisible());
                }
                else if (ke.getKeyCode() == KeyEvent.VK_TAB || ke.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
                    specialStats.setVisible(true);
                    normalStats.setVisible(false);
                    ticksUntilHideSpecialStats = 60;
                }
                else if (DEBUG_ALLOW_CAMERA_Z_CHANGES && ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    Player player = map.getPlayer();
                    player.setZ(Math.min(1-1/8f, player.getZ() + 1/8f));
                }
                else if (DEBUG_ALLOW_CAMERA_Z_CHANGES && ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    Player player = map.getPlayer();
                    player.setZ(Math.max(1/8f, player.getZ() - 1/8f));
                }
                else {
                    keyDown(ke.getKeyCode(), true);
                }
            }

            public void keyReleased(KeyEvent ke) {
                keyDown(ke.getKeyCode(), false);
            }
        });
        setMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (!paused) {
                    mousePressed = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent me) {
                mousePressed = false;
            }

            @Override
            public void mouseExited(MouseEvent me) {
                mousePressed = false;
                crosshair.setLocation(getWidth() / 2, getHeight() / 2);
            }
        });
        setMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent me) {
                int mouseX = me.getX();
                int mouseY = me.getY();
                if (mouseX >=0 && mouseX < getWidth() && mouseY >= 0 && mouseY < getHeight()) {
                    crosshair.setLocation(mouseX, mouseY);
                }
                else {
                    crosshair.setLocation(getWidth() / 2, getHeight() / 2);
                }
            }

            public void mouseMoved(MouseEvent me) {
                int mouseX = me.getX();
                int mouseY = me.getY();
                if (mouseX >=0 && mouseX < getWidth() && mouseY >= 0 && mouseY < getHeight()) {
                    crosshair.setLocation(mouseX, mouseY);
                }
                else {
                    crosshair.setLocation(getWidth() / 2, getHeight() / 2);
                }
            }
        });
        
        setFocusListener(new FocusListener() {

            public void focusGained(FocusEvent fe) {
                focusLostLabel.setVisible(false);
                paused = false;
            }

            public void focusLost(FocusEvent fe) {
                focusLostLabel.setVisible(true);
                paused = true;
            }
        });
        
        setLevel(0);
    }
    
    private SoftTexture getTexture(String name, boolean cache) {
        App app = App.getApp();
        BufferedImage image = app.getImage(name);
        SoftTexture texture = new SoftTexture(image);
        if (cache) {
            textureCache.put(name, texture);
        }
        return texture;
    }
    
    private void setMessage(String message) {
        messageQueue.add(message);
    }
    
    private void playSound(String soundName) {
        BufferedAudio audio = App.getApp().getAudio(soundName, 1);
        if (audio != null) {
            audio.play();
        }
    }
    
    private void setLevel(int level) {
        this.level = level;
        
        Player oldPlayer = null;
        if (map != null) {
            oldPlayer = map.getPlayer();
            map.unload();
        }
        
        try {
            map = new Map(textureCache, messageQueue, "/maps/level" + level + ".txt", oldPlayer);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            App.getApp().popScene();
            return;
        }

        collisionDetection = new CollisionDetection(map);
        renderer.setMap(map);
        
        if (level != 0) {
            setMessage("LEVEL " + (level + 1));
        }
        
        resetKeys();
        runVelocity = 0;
        strafeVelocity = 0;
        turnVelocity = 0;
        
        hasWon = false;
        gameOverWinMessage.setVisible(false);
        gameOverLoseMessage.setVisible(false);
        
        playSound("/sound/startlevel.wav");
    }
    
    private void keyDown(int keyCode, boolean down) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT: case KeyEvent.VK_A:
                keyLeft = down;
                break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D:
                keyRight = down;
                break;
            case KeyEvent.VK_UP: case KeyEvent.VK_W:
                keyUp = down;
                break;
            case KeyEvent.VK_DOWN: case KeyEvent.VK_S:
                keyDown = down;
                break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_Z:
                keyFire = down;
                break;
            case KeyEvent.VK_Q: 
                keyStrafeLeft = down;
                break;
            case KeyEvent.VK_E:
                keyStrafeRight = down;
                break;
            case KeyEvent.VK_C:
                keyStrafeModifier = down;
                break;
        }
    }
    
    public String doCommand(String command) {
        Player player = map.getPlayer();
        
        if ("HELP".equalsIgnoreCase(command)) {
            return  "restart  Restart the level.\n" +
                    "quit     Quit to main menu.\n" +
                    "ammo     Give yourself some ammo.\n" +
                    "health   Give yourself a health kit.\n" +
                    "key x    Give yourself key x (from 1 to " + Key.NUM_KEYS + ").\n" +
                    "level x  Skip to level x (from 1 to " + NUM_LEVELS + ").\n" +
                    "freeze   Freeze all enemies in place.\n" +
                    "cheat    Give yourself invincibility.\n" +
                    "debug    Show debug info.";
        }
        else if ("QUIT".equalsIgnoreCase(command)) {
            App.getApp().popScene(); // Back to game
            App.getApp().popScene(); // Back to title
            return "Quitting...";
        }
        else if ("4 8 15 16 23 42".equalsIgnoreCase(command)) {
            return "Timer reset to 108 minutes.";
        }
        else if ("DEBUG".equalsIgnoreCase(command)) {
            float dx = (float)Math.cos(Math.toRadians(player.getDirection()));
            float dy = (float)Math.sin(Math.toRadians(player.getDirection()));
            return
                    "location=" + player.getX() + "," + player.getY() + "\n" +
                    "facing=" + dx + "," + dy + "\n" + 
                    "actions=" + map.getNumActions() + "\n" +
                    "entities=" + map.getNumEntities();
        }
        else if ("RESTART".equalsIgnoreCase(command)) {
            setLevel(level);
            App.getApp().popScene(); // Back to game
            return "Restarting level " + (level + 1) + "...";
        }
        else if ("FREEZE".equalsIgnoreCase(command)) {
            player.setFreezeEnemies(!player.isFreezeEnemies());
            if (player.isFreezeEnemies()) {
                playSound("/sound/nuclear_health.wav");
            }
            return "Freeze mode is now " + (player.isFreezeEnemies() ? "on" : "off");
        }
        else if ("CHEAT".equalsIgnoreCase(command)) {
            player.setGodMode(!player.isGodMode());
            if (player.isGodMode()) {
                player.setAmmo(Player.MAX_AMMO);
                player.setHealth(Player.MAX_HEALTH);
                playSound("/sound/nuclear_health.wav");
            }
            return "Cheat mode is now " + (player.isGodMode() ? "on" : "off");
        }
        else if ("AMMO".equalsIgnoreCase(command)) {
            player.setAmmo(Math.min(Player.MAX_AMMO, player.getAmmo() + 20));
            playSound("/sound/getammo.wav");
            return "You got some ammo";
        }
        else if ("HEALTH".equalsIgnoreCase(command)) {
            player.setHealth(Math.min(Player.MAX_HEALTH, player.getHealth() + 20));
            playSound("/sound/getammo.wav");
            return "You got a med kit";
        }
        else if (command != null && command.length() > 5 && "LEVEL".equalsIgnoreCase(command.substring(0, 5))) {
            int newLevel;
            try {
                newLevel = Integer.parseInt(command.substring(5).trim()) - 1;
            }
            catch (NumberFormatException ex) {
                newLevel = -1;
            }

            if (newLevel >= 0 && newLevel < NUM_LEVELS) {
                setLevel(newLevel);
                return "Jump to level " + (newLevel + 1);
            }
            else {
                return "Invalid level";
            }
        }
        else if (command != null && command.length() > 3 && "KEY".equalsIgnoreCase(command.substring(0, 3))) {
            int key;
            try {
                key = Integer.parseInt(command.substring(3).trim());
            }
            catch (NumberFormatException ex) {
                key = -1;
            }

            if (key > 0 && key < 4) {
                playSound("/sound/unlock.wav");
                player.addKey(key);
                return "You got key " + key;
            }
            else {
                return "Invalid key";
            }
        }
        else {
            return "Unknown command";
        }
    }

    @Override
    public void onTick() {
        
        if (paused) {
            resetKeys();
            return;
        }
        
        // Handle blocking actions
        if (nextAction != ACTION_NONE) {
            nextActionTicksRemaining--;
            if (nextActionTicksRemaining <= 0) {
                
                if (nextAction == ACTION_NEW_LEVEL) {
                    newLevelAction();
                }
                else if (nextAction == ACTION_WIN) {
                    winAction();
                }
                
                nextAction = ACTION_NONE;
            }
            return;
        }
        
        // Move entities, handle actions
        map.tick();
        
        // Move player
        tickPlayer();
        Player player = map.getPlayer();
        renderer.setCamera(player.getX(), player.getY(), player.getZ(), player.getDirection());
        if (map.isExitFound() && !hasWon) {
            if (level < NUM_LEVELS - 1) {
                nextAction = ACTION_NEW_LEVEL;
            }
            else {
                nextAction = ACTION_WIN;
            }
            nextActionTicksRemaining = 90;
        }
        
        // Update the HUD
        warningSplash.setVisible(player.wasHitRecently());
        messageQueue.tick();
        for (int i = 0; i < messageQueue.getMaxSize(); i++) {
            messageLabels[i].setText(messageQueue.get(i));
        }
        for (int i = 0; i < Key.NUM_KEYS; i++) {
            keys[i].setVisible(player.hasKey(i + 1));
        }
        healthLabel.setText(Integer.toString(player.getHealth()));
        healthLabel.sizeToFit();
        ammoLabel.setText(Integer.toString(player.getAmmo()));
        ammoLabel.sizeToFit();
        levelLabel.setText("Level: " + (level + 1) + "/" + NUM_LEVELS);
        levelLabel.sizeToFit();
        secretsLabel.setText("Secrets: " + player.getSecrets() + "/" + map.getNumSecrets());
        secretsLabel.sizeToFit();
        enemiesLabel.setText("Enemies: " + player.getKills() + "/" + map.getNumEnemies());
        enemiesLabel.sizeToFit();
        if (ticksUntilHideSpecialStats > 0) {
            ticksUntilHideSpecialStats--;
            if (ticksUntilHideSpecialStats <= 0) {
                specialStats.setVisible(false);
                normalStats.setVisible(true);
            }
        }

        if (player.isAlive()) {
            
            
            float displayWeaponOffset = crosshair.getX();
            displayWeaponOffset = Math.max(displayWeaponOffset, 60);
            displayWeaponOffset = Math.min(displayWeaponOffset, getWidth() - 180);
            
            // Make the gun bob
            float v = Math.abs(runVelocity) + Math.abs(strafeVelocity);
            v = Math.min(v, MAX_RUN_VELOCITY);
            double angle = (System.currentTimeMillis() / 80.0) % (Math.PI * 2);
            int bob = (int)Math.round(100 * v * Math.sin(angle));
        
            
            float x = gunView.getWidth() / 2 + displayWeaponOffset;
            float y = getHeight() + bob - gunView.getWidth() * 3 / 4;
            
            if (gunBlastCountdown > 0) {
                gunBlastCountdown--;
                gunBlastView.setVisible(true);
                x += 3;
                y += 5;                
            }
            else {
                gunBlastView.setVisible(false);
            }
            
            float stepSize = 10;
            float dx = x - gunView.getX();
            if (Math.abs(dx) > stepSize) {
                x = gunView.getX() + Math.signum(dx) * stepSize;
            }

            gunBlastView.setLocation(x, y);
            gunView.setLocation(x, y);
        }
        else {
            gunBlastCountdown = 0;
        }
    }
    
    private void resetKeys() {
        keyLeft = false;
        keyRight = false;
        keyDown = false;
        keyUp = false;
        keyStrafeLeft = false;
        keyStrafeRight = false;
        keyStrafeModifier = false;
        keyFire = false;
        mousePressed = false;
        crosshair.setLocation(getWidth() / 2, getHeight() / 2);
    }
    
    private void fire() {
        Player player = map.getPlayer();
        if (!player.isAlive()) {
            return;
        }
        
        if (player.getAmmo() <= 0) {
            playSound("/sound/no_ammo.wav");
            return;
        }
        
        playSound("/sound/laser1.wav");

        if (!player.isGodMode()) {
            player.setAmmo(player.getAmmo() - 1);
        }
        gunBlastCountdown = GUN_BLAST_COUNTDOWN;
        
        int weaponAimX = (int)crosshair.getX();
        float aimAngle = renderer.getAngleAt(weaponAimX);
        aimAngle += Math.random() * 4 - 2; // +/- 2 degrees
        
        Point2D.Float p = map.getWallCollision(player.getX(), player.getY(), aimAngle);
        if (p == null) {
            return;
        }
        
        List<Entity> hitEnemies = map.getCollisions(Enemy.class, player.getX(), player.getY(), p.x, p.y);
        if (hitEnemies.size() > 0) {
            for (Entity entity : hitEnemies) {
                if (entity instanceof Enemy) {
                    ((Enemy)entity).hurt(6 + (int)(Math.random()*3)); //6..8
                }
            }
        }
        else {
            // Miss - show the hit on the wall
            map.addEntity(new BlastMark(blastTextures, p.x, p.y, GUN_BLAST_COUNTDOWN * 3 / 2));
        }
    }
    
    private void newLevelAction() {
        setLevel(level + 1);
    }
    
    private void winAction() {
        hasWon = true;
        mousePressed = false;
        map.getPlayer().setAlive(false);
        gameOverWinMessage.setVisible(true);
    }
    
    private void tickPlayer() {
        Player player = map.getPlayer();
        
        if (hasWon) {
            if (mousePressed) {
                player.setHealth(Player.DEFAULT_HEALTH);
                player.setAmmo(Player.DEFAULT_AMMO);
                setLevel(0);
            }
            return;
        }
        else if (!player.isAlive()) {
            player.setZ(Math.max(player.getZ() - 0.008f, player.getRadius()));
            deathTicksRemaining--;
            if (deathTicksRemaining <= 0) {
                gameOverLoseMessage.setVisible(true);
                if (mousePressed) {
                    player.setHealth(Player.DEFAULT_HEALTH);
                    player.setAmmo(Player.DEFAULT_AMMO);
                    setLevel(level);
                }
            }
            else {
                mousePressed = false;
            }
            return;
        }
        else {
            deathTicksRemaining = 60;
        }

        // Handle firing
        if (keyFire || mousePressed) {
            if (ticksUntilRefire <= 0) {
                fire();
                ticksUntilRefire = FIRE_COUNTDOWN;
            }
        }
        if (ticksUntilRefire > 0) {
            ticksUntilRefire--;
        }
        
        // Move player
        boolean keyRun = false;
        boolean keyStrafe = false;
        boolean keyTurn = false;
        if (keyUp) {
            keyRun = true;
            runVelocity += RUN_ACCEL;

            if (runVelocity > MAX_RUN_VELOCITY) {
                runVelocity = MAX_RUN_VELOCITY;
            }
        }

        if (keyDown) {
            keyRun = true;
            runVelocity -= RUN_ACCEL;

            if (runVelocity < MIN_RUN_VELOCITY) {
                runVelocity = MIN_RUN_VELOCITY;
            }
        }
        
        if (keyStrafeLeft || (keyStrafeModifier && keyLeft)) {
            keyStrafe = true;
            strafeVelocity += STRAFE_ACCEL;

            if (strafeVelocity > MAX_STRAFE_VELOCITY) {
                strafeVelocity = MAX_STRAFE_VELOCITY;
            }
        }

        if (keyLeft && !keyStrafeModifier) {
            keyTurn = true;
            turnVelocity += TURN_ACCEL;

            if (turnVelocity > MAX_TURN_VELOCITY) {
                turnVelocity = MAX_TURN_VELOCITY;
            }
        }
        
        if (keyStrafeRight || (keyStrafeModifier && keyRight)) {
            keyStrafe = true;
            strafeVelocity -= STRAFE_ACCEL;

            if (strafeVelocity < MIN_STRAFE_VELOCITY) {
                strafeVelocity = MIN_STRAFE_VELOCITY;
            }
        }

        if (keyRight && !keyStrafeModifier) {
            keyTurn = true;
            turnVelocity -= TURN_ACCEL;

            if (turnVelocity < MIN_TURN_VELOCITY) {
                turnVelocity = MIN_TURN_VELOCITY;
            }
        }

        if (!keyRun && runVelocity != 0) {
            if (Math.abs(runVelocity) <= RUN_DECEL) {
                runVelocity = 0;
            }
            else if (runVelocity < 0) {
                runVelocity += RUN_DECEL;
            }
            else {
                runVelocity -= RUN_DECEL;
            }
        }

        if (!keyStrafe && strafeVelocity != 0) {
            if (Math.abs(strafeVelocity) <= STRAFE_DECEL) {
                strafeVelocity = 0;
            }
            else if (strafeVelocity < 0) {
                strafeVelocity += STRAFE_DECEL;
            }
            else {
                strafeVelocity -= STRAFE_DECEL;
            }
        }

        if (!keyTurn && turnVelocity != 0) {
            if (Math.abs(turnVelocity) <= TURN_DECEL) {
                turnVelocity = 0;
            }
            else if (turnVelocity < 0) {
                turnVelocity += TURN_DECEL;
            }
            else {
                turnVelocity -= TURN_DECEL;
            }
        }
        
        if (turnVelocity != 0) {
            player.setDirection((player.getDirection() + turnVelocity) % 360);
        }
        
        float strafeDir = player.getDirection() + 90;
        float cosPlayerDir = (float)Math.cos(Math.toRadians(player.getDirection()));
        float sinPlayerDir = (float)Math.sin(Math.toRadians(player.getDirection()));
        float cosPlayerStrafeDir = (float)Math.cos(Math.toRadians(strafeDir));
        float sinPlayerStrafeDir = (float)Math.sin(Math.toRadians(strafeDir));
            
        float dx = cosPlayerDir * runVelocity;
        float dy = -sinPlayerDir * runVelocity;
        dx += cosPlayerStrafeDir * strafeVelocity;
        dy += -sinPlayerStrafeDir * strafeVelocity;

        if (dx == 0 && dy == 0) {
            return;
        }
        
        collisionDetection.move(player, player.getX() + dx, player.getY() + dy, true, true);
    }
}
