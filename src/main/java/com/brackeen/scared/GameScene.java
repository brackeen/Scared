package com.brackeen.scared;

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
import com.brackeen.scared.entity.Player;

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
import java.util.prefs.Preferences;

public class GameScene extends Scene {

    private static final boolean DEBUG_ALLOW_CAMERA_Z_CHANGES = Boolean.parseBoolean("false");

    public static final int NUM_LEVELS = 7;

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

    private static final int WARNING_BORDER_SIZE = 6;
    private static final int UI_SPACING = 4;

    private static final int VOLUME_SCALE = 10;

    private final HashMap<String, SoftTexture> textureCache;

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
    private boolean showCrosshair = true;
    private Stats stats = new Stats();

    private float runVelocity = 0;
    private float strafeVelocity = 0;
    private float turnVelocity = 0;

    private int ticksUntilRefire;
    private int nextAction = ACTION_NONE;
    private int nextActionTicksRemaining;

    private final SoftTexture[] blastTextures = new SoftTexture[3];

    // HUD
    private final MessageQueue messageQueue = new MessageQueue(4);
    private final Label[] messageLabels = new Label[4];
    private BitmapFont messageFont;
    private BitmapFont scoreFont;
    private Label focusLostLabel;
    private boolean paused;
    private final ImageView[] keys = new ImageView[Key.NUM_KEYS];
    private View normalStats;
    private View specialStats;
    private Label healthLabel;
    private Label healthHeaderLabel;
    private Label ammoLabel;
    private Label ammoHeaderLabel;
    private Label enemiesLabel;
    private Label secretsLabel;
    private Label levelLabel;
    private Label fpsLabel;
    private int ticksUntilHideSpecialStats;
    private ImageView gunView;
    private ImageView gunBlastView;
    private int gunBlastCountdown;
    private int deathTicksRemaining;
    private View warningSplash;
    private View gameOverBackground;
    private View gameOverMessage;
    private ImageView crosshair;

    public GameScene(HashMap<String, SoftTexture> textureCache) {
        this.textureCache = textureCache;
    }

    @Override
    public void onLoad() {
        App app = App.getApp();

        App.log("Use Arrows or WASD to move\n" +
                "Q/E to strafe\n" +
                "Space or mouse1 to fire");

        messageFont = new BitmapFont(app.getImage("/ui/message_font.png"), 8, ' ');
        scoreFont = new BitmapFont(app.getImage("/ui/score_font.png"), 12, '0');
        scoreFont.setTracking(0);

        blastTextures[0] = textureCache.get("/sprites/blast1.png");
        blastTextures[1] = textureCache.get("/sprites/blast2.png");
        blastTextures[2] = textureCache.get("/sprites/blast3.png");

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        this.renderer = new SoftRender3D(textureCache);
        renderer.setDepthShadingEnabled(prefs.getBoolean(Main.SETTING_DEPTH_SHADING, true));
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
        warningSplash = new View();
        warningSplash.setVisible(false);
        addSubview(warningSplash);

        float hudOpacity = 1.0f;

        // Message queue
        float messageX = UI_SPACING;
        float messageY = UI_SPACING;
        for (int i = 0; i < messageQueue.getMaxSize(); i++) {
            messageLabels[i] = new Label(messageFont, "");
            messageLabels[i].setLocation(messageX, messageY);
            messageLabels[i].setOpacity(hudOpacity);
            addSubview(messageLabels[i]);
            messageY += messageLabels[i].getHeight();
        }

        // Keys
        for (int i = 0; i < Key.NUM_KEYS; i++) {
            keys[i] = new ImageView(App.getApp().getImage("/sprites/key0" + (i + 1) + ".png"));
            keys[i].setAnchor(1, 1);
            keys[i].setOpacity(hudOpacity);
            keys[i].setVisible(false);
            addSubview(keys[i]);
        }

        // Health/ammo
        normalStats = new View();
        normalStats.setOpacity(hudOpacity);
        healthLabel = new Label(scoreFont, Integer.toString(Player.DEFAULT_HEALTH));
        healthLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(healthLabel);
        healthHeaderLabel = new Label(messageFont, "HEALTH");
        healthHeaderLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(healthHeaderLabel);

        ammoLabel = new Label(scoreFont, Integer.toString(Player.DEFAULT_AMMO));
        ammoLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(ammoLabel);
        ammoHeaderLabel = new Label(messageFont, "AMMO");
        ammoHeaderLabel.setAnchor(0.5f, 1);
        normalStats.addSubview(ammoHeaderLabel);
        addSubview(normalStats);

        // Secrets/enemies
        specialStats = new View();
        specialStats.setOpacity(hudOpacity);
        specialStats.setVisible(false);
        secretsLabel = new Label(messageFont, "Secrets: 0/0");
        secretsLabel.setAnchor(0, 1);
        specialStats.addSubview(secretsLabel);
        enemiesLabel = new Label(messageFont, "Enemies: 0/0");
        enemiesLabel.setAnchor(0, 1);
        specialStats.addSubview(enemiesLabel);
        levelLabel = new Label(messageFont, "Level 1");
        levelLabel.setAnchor(0, 1);
        specialStats.addSubview(levelLabel);
        addSubview(specialStats);

        // FPS
        fpsLabel = new Label(messageFont, "0 fps");
        fpsLabel.setOpacity(hudOpacity);
        fpsLabel.setAnchor(1, 0);
        fpsLabel.setVisible(false);
        addSubview(fpsLabel);

        // Focus message
        focusLostLabel = new Label(messageFont, "Click to continue");
        focusLostLabel.setAnchor(0.5f, 0.5f);
        focusLostLabel.setVisible(false);
        addSubview(focusLostLabel);

        onResize();

        // Hide the cursor
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension size = toolkit.getBestCursorSize(32, 32);
            if (size != null && size.width > 0 && size.height > 0) {
                BufferedImage cursorImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                Cursor noCursor = toolkit.createCustomCursor(cursorImage, new Point(0, 0), "none");
                setCursor(noCursor);
            }
        } catch (Exception ex) {
            // Ignore it
        }

        setKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {
                // Do nothing
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    App.getApp().pushScene(new ConsoleScene(GameScene.this));
                } else if (ke.getKeyCode() == KeyEvent.VK_X) {
                    if (map != null && map.getPlayer().isAlive()) {
                        showCrosshair = !showCrosshair;
                    }
                } else if (ke.getKeyCode() == KeyEvent.VK_R) {
                    fpsLabel.setVisible(!fpsLabel.isVisible());
                } else if (ke.getKeyCode() == KeyEvent.VK_TAB || ke.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
                    specialStats.setVisible(true);
                    normalStats.setVisible(false);
                    ticksUntilHideSpecialStats = 60;
                } else if (DEBUG_ALLOW_CAMERA_Z_CHANGES && ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    Player player = map.getPlayer();
                    player.setZ(Math.min(1 - 1 / 8f, player.getZ() + 1 / 128f));
                } else if (DEBUG_ALLOW_CAMERA_Z_CHANGES && ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    Player player = map.getPlayer();
                    player.setZ(Math.max(1 / 8f, player.getZ() - 1 / 128f));
                } else if (DEBUG_ALLOW_CAMERA_Z_CHANGES && ke.getKeyCode() == KeyEvent.VK_HOME) {
                    Player player = map.getPlayer();
                    player.setZ(0.5f);
                } else {
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

            @Override
            public void mouseDragged(MouseEvent me) {
                mouseMoved(me);
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                Point2D.Float pos = getLocalLocation(me.getX(), me.getY());
                pos.x = Math.round(pos.x);
                pos.y = Math.round(pos.y);
                if (pos.x >= 0 && pos.x < getWidth() && pos.y >= 0 && pos.y < getHeight()) {
                    crosshair.setLocation(pos.x, pos.y);
                } else {
                    crosshair.setLocation(getWidth() / 2, getHeight() / 2);
                }
            }
        });

        setFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent fe) {
                focusLostLabel.setVisible(false);
                paused = false;
            }

            @Override
            public void focusLost(FocusEvent fe) {
                focusLostLabel.setVisible(gameOverMessage == null);
                paused = true;
            }
        });

        setLevel(0);
    }

    @Override
    public void onResize() {
        renderer.setSize(getWidth(), getHeight());

        // Warning splash
        warningSplash.removeAllSubviews();
        View view = new View(0, 0, getWidth(), WARNING_BORDER_SIZE);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(0, WARNING_BORDER_SIZE, WARNING_BORDER_SIZE, getHeight() - WARNING_BORDER_SIZE * 2);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(getWidth() - WARNING_BORDER_SIZE, WARNING_BORDER_SIZE, WARNING_BORDER_SIZE, getHeight() - WARNING_BORDER_SIZE * 2);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);
        view = new View(0, getHeight() - WARNING_BORDER_SIZE, getWidth(), WARNING_BORDER_SIZE);
        view.setBackgroundColor(Color.RED);
        warningSplash.addSubview(view);

        // Keys
        float keyX = getWidth() + UI_SPACING - 1;
        float keyY = getHeight() - UI_SPACING;
        for (int i = 0; i < Key.NUM_KEYS; i++) {
            keys[i].setLocation(keyX, keyY);
            keyX -= keys[i].getWidth() / 2 + UI_SPACING + 1;
        }

        // Health/ammo
        healthLabel.setLocation(UI_SPACING * 3 + scoreFont.getStringWidth("000") / 2, getHeight() - UI_SPACING);
        healthHeaderLabel.setLocation(healthLabel.getX(), healthLabel.getY() - healthLabel.getHeight() - UI_SPACING);
        ammoLabel.setLocation(healthLabel.getX() + UI_SPACING * 4 + scoreFont.getStringWidth("000"), getHeight() - UI_SPACING);
        ammoHeaderLabel.setLocation(ammoLabel.getX(), ammoLabel.getY() - ammoLabel.getHeight() - UI_SPACING);

        // Secrets/level
        secretsLabel.setLocation(UI_SPACING * 3 / 2, getHeight() - UI_SPACING);
        enemiesLabel.setLocation(UI_SPACING * 3 / 2, secretsLabel.getY() - UI_SPACING - secretsLabel.getHeight());
        levelLabel.setLocation(UI_SPACING * 3 / 2, enemiesLabel.getY() - UI_SPACING - enemiesLabel.getHeight());

        // FPS
        fpsLabel.setLocation(getWidth() - UI_SPACING, UI_SPACING);

        // UI Labels
        focusLostLabel.setLocation(getWidth() / 2, getHeight() / 2);
        if (gameOverMessage != null) {
            gameOverMessage.setLocation(getWidth() / 2, getHeight() / 2);
        }
        if (gameOverBackground != null) {
            gameOverBackground.setSize(getWidth(), getHeight());
        }
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
            map = new Map(textureCache, messageQueue, "/maps/level" + level + ".txt", oldPlayer, stats);
        } catch (IOException ex) {
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
        if (gameOverMessage != null) {
            gameOverMessage.removeFromSuperview();
            gameOverMessage = null;
        }
        if (gameOverBackground != null) {
            gameOverBackground.removeFromSuperview();
            gameOverBackground = null;
        }

        playSound("/sound/startlevel.wav");
    }

    private void keyDown(int keyCode, boolean down) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                keyLeft = down;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                keyRight = down;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                keyUp = down;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                keyDown = down;
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_Z:
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

        if (command == null) {
            command = "";
        } else {
            command = command.trim();
        }

        if ("HELP".equalsIgnoreCase(command)) {
            return ("stats     Show stats.\n" +
                    "volume x  Set audio volume (from 0 to " + VOLUME_SCALE + ").\n" +
                    "shading   Enable/disable depth shading.\n" +
                    "scaling   Enable/disable auto pixel scaling.\n" +
                    "level x   Skip to level x (from 1 to " + NUM_LEVELS + ").\n" +
                    "ammo      Give yourself some ammo.\n" +
                    "health    Give yourself a health kit.\n" +
                    "key x     Give yourself key x (from 1 to " + Key.NUM_KEYS + ").\n" +
                    "cheat     Give yourself invincibility.\n" +
                    "freeze    Freeze all enemies in place.\n" +
                    "debug     Show debug info.\n" +
                    "exit      Exit game.");
        } else if ("CLEAR".equalsIgnoreCase(command)) {
            App.getApp().getLog().clear();
            return null;
        } else if ("EXIT".equalsIgnoreCase(command) && App.getApp().dispose()) {
            return "Exiting...";
        } else if ("4 8 15 16 23 42".equalsIgnoreCase(command)) {
            return "Timer reset to 108 minutes.";
        } else if ("STATS".equalsIgnoreCase(command)) {
            return stats.getDescription(map, level + 1);
        } else if ("DEBUG".equalsIgnoreCase(command)) {
            float dx = (float) Math.cos(Math.toRadians(player.getDirection()));
            float dy = (float) Math.sin(Math.toRadians(player.getDirection()));
            return ("location=" + player.getX() + "," + player.getY() + "\n" +
                    "facing=" + dx + "," + dy + "\n" +
                    "angle=" + String.format("%.2f", player.getDirection()) + "\n" +
                    "actions=" + map.getNumActions() + "\n" +
                    "entities=" + map.getNumEntities());
        } else if ("SHADING".equalsIgnoreCase(command)) {
            renderer.setDepthShadingEnabled(!renderer.isDepthShadingEnabled());
            Preferences prefs = Preferences.userNodeForPackage(Main.class);
            prefs.putBoolean(Main.SETTING_DEPTH_SHADING, renderer.isDepthShadingEnabled());
            return "Depth shading is now " + (renderer.isDepthShadingEnabled() ? "on" : "off");
        } else if ("SCALING".equalsIgnoreCase(command)) {
            App.getApp().setAutoPixelScale(!App.getApp().isAutoPixelScale());
            Preferences prefs = Preferences.userNodeForPackage(Main.class);
            prefs.putBoolean(Main.SETTING_AUTO_PIXEL_SCALE, App.getApp().isAutoPixelScale());
            return "Auto pixel scaling is now " + (App.getApp().isAutoPixelScale() ? "on" : "off");
        } else if ("FREEZE".equalsIgnoreCase(command)) {
            player.setFreezeEnemies(!player.isFreezeEnemies());
            if (player.isFreezeEnemies()) {
                stats.cheated = true;
                playSound("/sound/nuclear_health.wav");
            }
            return "Freeze mode is now " + (player.isFreezeEnemies() ? "on" : "off");
        } else if ("CHEAT".equalsIgnoreCase(command)) {
            player.setGodMode(!player.isGodMode());
            if (player.isGodMode()) {
                stats.cheated = true;
                player.setAmmo(Player.MAX_AMMO);
                player.setHealth(Player.MAX_HEALTH);
                playSound("/sound/nuclear_health.wav");
            }
            return "Cheat mode is now " + (player.isGodMode() ? "on" : "off");
        } else if ("AMMO".equalsIgnoreCase(command)) {
            stats.cheated = true;
            player.setAmmo(Math.min(Player.MAX_AMMO, player.getAmmo() + 20));
            playSound("/sound/getammo.wav");
            return "You got some ammo";
        } else if ("HEALTH".equalsIgnoreCase(command)) {
            stats.cheated = true;
            player.setHealth(Math.min(Player.MAX_HEALTH, player.getHealth() + 20));
            playSound("/sound/getammo.wav");
            return "You got a med kit";
        } else if (command.length() > 5 && "LEVEL".equalsIgnoreCase(command.substring(0, 5))) {
            int newLevel;
            try {
                newLevel = Integer.parseInt(command.substring(5).trim()) - 1;
            } catch (NumberFormatException ex) {
                newLevel = -1;
            }

            if (newLevel >= 0 && newLevel < NUM_LEVELS) {
                stats.cheated = true;
                setLevel(newLevel);
                return "Jump to level " + (newLevel + 1);
            } else {
                return "Invalid level";
            }
        } else if (command.length() > 3 && "KEY".equalsIgnoreCase(command.substring(0, 3))) {
            int key;
            try {
                key = Integer.parseInt(command.substring(3).trim());
            } catch (NumberFormatException ex) {
                key = -1;
            }

            if (key > 0 && key < 4) {
                stats.cheated = true;
                playSound("/sound/unlock.wav");
                player.addKey(key);
                return "You got key " + key;
            } else {
                return "Invalid key";
            }
        } else if (command.length() >= 6 && "VOLUME".equalsIgnoreCase(command.substring(0, 6))) {
            int volume;
            try {
                volume = Integer.parseInt(command.substring(6).trim());
            } catch (NumberFormatException ex) {
                volume = -1;
            }
            if (volume < 0 || volume > VOLUME_SCALE) {
                volume =  Math.round(BufferedAudio.getMasterVolume() * VOLUME_SCALE);
            } else {
                BufferedAudio.setMasterVolume(volume / (float)VOLUME_SCALE);
                Preferences prefs = Preferences.userNodeForPackage(Main.class);
                prefs.putFloat(Main.SETTING_VOLUME, BufferedAudio.getMasterVolume());
            }
            return "Volume set to " + volume;
        } else {
            return "Unknown command";
        }
    }

    @Override
    public void onTick() {
        crosshair.setVisible(showCrosshair && map.getPlayer().isAlive());

        if (paused) {
            resetKeys();
            return;
        }

        // Handle blocking actions
        if (nextAction != ACTION_NONE) {
            nextActionTicksRemaining--;
            if (nextActionTicksRemaining <= 0) {

                if (nextAction == ACTION_NEW_LEVEL) {
                    nextLevelAction();
                } else if (nextAction == ACTION_WIN) {
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
            } else {
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
        fpsLabel.setText(String.format("%.2f fps", App.getApp().getActualFrameRate()));
        fpsLabel.sizeToFit();
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
            displayWeaponOffset = Math.max(displayWeaponOffset, 32);
            displayWeaponOffset = Math.min(displayWeaponOffset, getWidth() -  gunView.getWidth());

            // Make the gun bob
            float v = Math.abs(runVelocity) + Math.abs(strafeVelocity);
            v = Math.min(v, MAX_RUN_VELOCITY);
            double angle = (System.currentTimeMillis() / 80.0) % (Math.PI * 2);
            int bob = (int) Math.round(gunView.getHeight() * 0.75f * v * Math.sin(angle));

            float x = gunView.getWidth() * 0.3f + displayWeaponOffset;
            float y = Math.round(getHeight() + bob - gunView.getWidth() * 0.7f);

            if (gunBlastCountdown > 0) {
                gunBlastCountdown--;
                gunBlastView.setVisible(true);
                x += 3;
                y += 5;
            } else {
                gunBlastView.setVisible(false);
            }

            float stepSize = Math.round(getWidth() / 64);
            float dx = x - gunView.getX();
            if (Math.abs(dx) > stepSize) {
                x = gunView.getX() + Math.signum(dx) * stepSize;
            }

            gunBlastView.setLocation((int) x, (int) y);
            gunView.setLocation((int) x, (int) y);
        } else {
            gunBlastView.setVisible(false);
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

        stats.numShotsFired++;
        playSound("/sound/laser1.wav");

        if (!player.isGodMode()) {
            player.setAmmo(player.getAmmo() - 1);
        }
        gunBlastCountdown = GUN_BLAST_COUNTDOWN;

        int weaponAimX = (int) crosshair.getX();
        float aimAngle = renderer.getAngleAt(weaponAimX);
        aimAngle += Math.random() * 4 - 2; // +/- 2 degrees

        Point2D.Float p = map.getWallCollision(player.getX(), player.getY(), aimAngle);
        if (p == null) {
            return;
        }

        boolean hitSomething = false;
        List<Entity> hitEnemies = map.getCollisions(Enemy.class, player.getX(), player.getY(), p.x, p.y);
        if (hitEnemies.size() > 0) {
            for (Entity entity : hitEnemies) {
                if (entity instanceof Enemy) {
                    hitSomething |= ((Enemy) entity).hurt(6 + (int) (Math.random() * 3)); //6..8
                }
            }
        }
        if (hitSomething) {
            stats.numShotsFiredHit++;
        } else {
            // Miss - show the hit on the wall
            map.addEntity(new BlastMark(blastTextures, p.x, p.y, GUN_BLAST_COUNTDOWN * 3 / 2));
        }
    }

    private void copyLevelStats() {
        stats.numSecretsFound += map.getPlayer().getSecrets();
        stats.totalSecrets += map.getNumSecrets();
        stats.numKills += map.getPlayer().getKills();
        stats.totalEnemies += map.getNumEnemies();
    }

    private void nextLevelAction() {
        copyLevelStats();
        setLevel(level + 1);
    }

    private void winAction() {
        String statsDescription = stats.getDescription(map, level + 1);
        copyLevelStats();
        hasWon = true;
        mousePressed = false;
        map.getPlayer().setAlive(false);
        if (gameOverMessage != null) {
            gameOverMessage.removeFromSuperview();
        }
        if (gameOverBackground == null) {
            gameOverBackground = new View(0, 0, getWidth(), getHeight());
            gameOverBackground.setBackgroundColor(new Color(0, 0, 0, 0.25f));
            addSubview(gameOverBackground);
        }
        gameOverMessage = Label.makeMultilineLabel(messageFont,
                "YOU WIN. Click to play again.\n\n" + statsDescription, 0.5f);
        gameOverMessage.setLocation(getWidth() / 2, getHeight() / 2);
        gameOverMessage.setAnchor(0.5f, 0.5f);
        addSubview(gameOverMessage);
    }

    private void tickPlayer() {
        Player player = map.getPlayer();

        if (hasWon) {
            if (mousePressed) {
                player.setHealth(Player.DEFAULT_HEALTH);
                player.setAmmo(Player.DEFAULT_AMMO);
                stats = new Stats();
                setLevel(0);
            }
            return;
        } else if (!player.isAlive()) {
            player.setZ(Math.max(player.getZ() - 0.008f, player.getRadius()));
            deathTicksRemaining--;
            if (deathTicksRemaining <= 0) {
                if (deathTicksRemaining == 0) {
                    if (gameOverMessage != null) {
                        gameOverMessage.removeFromSuperview();
                    }
                    gameOverMessage = new Label(messageFont, "YOU DIED.");
                    gameOverMessage.setLocation(getWidth() / 2, getHeight() / 2);
                    gameOverMessage.setAnchor(0.5f, 0.5f);
                    addSubview(gameOverMessage);
                    stats.numDeaths++;
                }
                if (mousePressed) {
                    player.setHealth(Player.DEFAULT_HEALTH);
                    player.setAmmo(Player.DEFAULT_AMMO);
                    setLevel(level);
                }
            } else {
                mousePressed = false;
            }
            return;
        } else {
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
            } else if (runVelocity < 0) {
                runVelocity += RUN_DECEL;
            } else {
                runVelocity -= RUN_DECEL;
            }
        }

        if (!keyStrafe && strafeVelocity != 0) {
            if (Math.abs(strafeVelocity) <= STRAFE_DECEL) {
                strafeVelocity = 0;
            } else if (strafeVelocity < 0) {
                strafeVelocity += STRAFE_DECEL;
            } else {
                strafeVelocity -= STRAFE_DECEL;
            }
        }

        if (!keyTurn && turnVelocity != 0) {
            if (Math.abs(turnVelocity) <= TURN_DECEL) {
                turnVelocity = 0;
            } else if (turnVelocity < 0) {
                turnVelocity += TURN_DECEL;
            } else {
                turnVelocity -= TURN_DECEL;
            }
        }

        if (turnVelocity != 0) {
            player.setDirection((player.getDirection() + turnVelocity) % 360);
        }

        float strafeDir = player.getDirection() + 90;
        float cosPlayerDir = (float) Math.cos(Math.toRadians(player.getDirection()));
        float sinPlayerDir = (float) Math.sin(Math.toRadians(player.getDirection()));
        float cosPlayerStrafeDir = (float) Math.cos(Math.toRadians(strafeDir));
        float sinPlayerStrafeDir = (float) Math.sin(Math.toRadians(strafeDir));

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
