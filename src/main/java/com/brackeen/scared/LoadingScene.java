package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.scared.entity.Enemy;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoadingScene extends BaseConsoleScene {

    private final HashMap<String, SoftTexture> textureCache = new HashMap<>();
    private List<Runnable> itemsToLoad;
    private List<BufferedImage> loadedAssets = new ArrayList<>(); // Keep a reference until GameScene is loaded
    private int itemsLoaded = 0;
    private boolean loadNextItem = false;

    @Override
    public void onLoad() {
        super.onLoad();
        onResize();

        final App app = App.getApp();

        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "";
        }

        App.log("Scared " + version);
        App.log("Java " + System.getProperty("java.version"));

        itemsToLoad = new ArrayList<>();
        itemsToLoad.add(new Runnable() {
            @Override
            public void run() {

            }
        });

        // Audio
        itemsToLoad.add(new Runnable() {
            @Override
            public void run() {
                // Number of loaded audio buffers must be 32 or less, due to a Java Sound limitation.
                app.getAudio("/sound/bigfan.wav", 1);
                app.getAudio("/sound/doorclose.wav", 2);
                app.getAudio("/sound/doorwoosh.wav", 2);
                app.getAudio("/sound/endlevel.wav", 1);
                app.getAudio("/sound/enemy_dead.wav", 2);
                app.getAudio("/sound/getammo.wav", 4);
                app.getAudio("/sound/laser0.wav", 4);
                app.getAudio("/sound/laser1.wav", 4);
                app.getAudio("/sound/no_ammo.wav", 4);
                app.getAudio("/sound/nuclear_health.wav", 1);
                app.getAudio("/sound/player_dead.wav", 1);
                app.getAudio("/sound/player_hurt.wav", 3);
                app.getAudio("/sound/startlevel.wav", 1);
                app.getAudio("/sound/unlock.wav", 1);
                app.getAudio("/sound/wallmove.wav", 1);
            }
        });

        // HUD
        itemsToLoad.add(new Runnable() {
            @Override
            public void run() {
                loadedAssets.add(app.getImage("/ui/message_font.png"));
                loadedAssets.add(app.getImage("/ui/score_font.png"));
                loadedAssets.add(app.getImage("/hud/crosshair.png"));
                loadedAssets.add(app.getImage("/hud/gun01.png"));
                loadedAssets.add(app.getImage("/hud/gun02.png"));
            }
        });

        // Game Sprites
        // NOTE: Java has trouble with indexed PNG images with a palette of less than 16 colors.
        // PNG optimizers create these. Images created from Photoshop or other major tools are fine.
        itemsToLoad.add(new Runnable() {
            @Override
            public void run() {
                cacheTexture("/sprites/ammo.png");
                cacheTexture("/sprites/blast1.png");
                cacheTexture("/sprites/blast2.png");
                cacheTexture("/sprites/blast3.png");
                cacheTexture("/sprites/key01.png");
                cacheTexture("/sprites/key02.png");
                cacheTexture("/sprites/key03.png");
                cacheTexture("/sprites/medkit.png");
                cacheTexture("/sprites/nuclear.png");
                for (int i = 0; i < Enemy.NUM_IMAGES; i++) {
                    cacheTexture("/enemy/" + i + ".png");
                }
            }
        });

        // Wall textures
        itemsToLoad.add(new Runnable() {
            @Override
            public void run() {
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

                // Create mip-maps
                final int mipMapCount = 3;
                for (String textureName : textures) {
                    String fullname = "/textures/" + textureName;
                    SoftTexture texture = cacheTexture(fullname, textureName);

                    SoftTexture.DownscaleType downscaleType = SoftTexture.DownscaleType.WEIGHTED_EVEN;
                    // Hack: Sharpen on odd pixels on these two textures to make their highlights look better
                    if ("wall01.png".equals(textureName) || "wall06.png".equals(textureName)) {
                        downscaleType = SoftTexture.DownscaleType.WEIGHTED_ODD;
                    }

                    for (int i = 0; i < mipMapCount; i++) {
                        texture.createHalfSizeTexture(downscaleType);
                        texture = texture.getHalfSizeTexture();
                        if (texture == null) {
                            break;
                        }
                        downscaleType = SoftTexture.DownscaleType.AVERAGE;
                    }
                }
            }
        });
    }

    private SoftTexture cacheTexture(String name) {
        return cacheTexture(name, name);
    }

    private SoftTexture cacheTexture(String fileName, String cacheName) {
        App app = App.getApp();
        BufferedImage image = app.getImage(fileName);
        loadedAssets.add(image);
        SoftTexture texture = new SoftTexture(image);
        textureCache.put(cacheName, texture);
        return texture;
    }

    @Override
    public void onTick() {
        if (loadNextItem) {
            loadNextItem = false;
            if (itemsToLoad.isEmpty()) {
                App.log("");
                App.getApp().setScene(new GameScene(textureCache));
            } else {
                Runnable runnable = itemsToLoad.remove(0);
                runnable.run();
                itemsLoaded++;
            }
        }
        String s = "Loading";
        for (int i = 0; i < itemsLoaded; i++) {
            s += '.';
        }
        setupTextViews(s);
    }

    public void onDraw(Graphics2D g) {
        loadNextItem = true;
    }
}
