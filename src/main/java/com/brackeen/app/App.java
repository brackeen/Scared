package com.brackeen.app;

import com.brackeen.app.audio.AudioBuffer;
import com.brackeen.app.audio.AudioEngine;
import com.brackeen.app.view.Scene;
import com.brackeen.app.view.View;

import java.applet.Applet;
import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * The App class sets up the animation loop and provides methods to load images and set the
 * current scene.
 */
@SuppressWarnings("unused")
public abstract class App extends Applet implements MouseListener, MouseMotionListener,
        KeyListener, FocusListener {

    private static final InheritableThreadLocal<App> APP = new InheritableThreadLocal<>();

    public static App getApp() {
        return APP.get();
    }

    private static final int MAX_LOG_LINES = 1000;

    public static void log(String statement) {
        log(statement, false);
    }

    public static void logError(String statement) {
        log(statement, true);
    }

    private static void log(String statement, boolean toSystemOut) {
        if (toSystemOut) {
            System.out.println(statement);
        }

        List<String> log = App.getApp().getLog();

        // Split on newlines
        int index = 0;
        while (true) {
            int newIndex = statement.indexOf('\n', index);
            if (newIndex == -1) {
                log.add(statement.substring(index));
                break;
            }
            log.add(statement.substring(index, newIndex));
            index = newIndex + 1;
        }

        while (log.size() > MAX_LOG_LINES) {
            log.remove(0);
        }
    }

    private JFrame frame;
    private String appName = "App";
    private BufferedImage iconImage;
    private final float simulationRate = 60;
    private final float frameRate = 60;
    private float audioSampleRate = 44100;
    private boolean gameLoopRunning = false;
    private long lastTime = 0;
    private double remainingTime = 0;
    private long lastTickTime = 0;
    private float actualFrameRate = 0;
    private long actualFrameRateLastTime = 0;
    private long actualFrameRateTickCount = 0;
    private int pixelScale = 1;
    private boolean autoPixelScale = false;
    private int autoPixelScaleBaseWidth = 320;
    private int autoPixelScaleBaseHeight = 240;
    private BufferedImage pixelScaleBufferedImage;

    private final List<String> log = new ArrayList<>();

    private final HashMap<String, WeakReference<BufferedImage>> imageCache = new HashMap<>();
    private final HashMap<String, AudioBuffer> loadedAudio = new HashMap<>();
    private final Stack<Scene> sceneStack = new Stack<>();
    private List<View> prevViewsWithTouchInside = new ArrayList<>();
    private List<View> currViewsWithTouchInside = new ArrayList<>();
    private BufferStrategy bufferStrategy;
    private Canvas canvas;
    private int mouseX = -1;
    private int mouseY = -1;

    public App() {
        APP.set(this);
        setBackground(Color.BLACK);
    }

    public List<String> getLog() {
        return log;
    }

    // Applet callbacks

    @Override
    public synchronized void init() {
        AudioEngine.init(audioSampleRate);
    }

    @Override
    public synchronized void start() {
        lastTime = System.nanoTime();
        remainingTime = 0;
        actualFrameRate = 0;
        actualFrameRateLastTime = 0;
        actualFrameRateTickCount = 0;
        gameLoopRunning = true;
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        });
    }

    @Override
    public synchronized void stop() {
        gameLoopRunning = false;
    }

    @Override
    public synchronized void destroy() {
        while (canPopScene()) {
            popScene();
        }
        if (bufferStrategy != null) {
            bufferStrategy.dispose();
            bufferStrategy = null;
        }
        loadedAudio.clear();
        imageCache.clear();
        prevViewsWithTouchInside.clear();
        currViewsWithTouchInside.clear();
        log.clear();
        canvas = null;
        removeAll();
        AudioEngine.destroy();
    }

    @Override
    public void update(Graphics g) {
        // Do nothing
    }

    @Override
    public void paint(Graphics g) {
        // Do nothing
    }

    protected void initFrame(int width, int height) {
        // Create frame
        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImage(iconImage);
        enableOSXFullscreen(frame);

        // Add applet to contentPane
        setSize(width, height);
        final Container contentPane = frame.getContentPane();
        contentPane.setBackground(Color.BLACK);
        contentPane.setPreferredSize(new Dimension(width, height));
        contentPane.setLayout(null);
        contentPane.add(this);

        // Show frame
        frame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(Math.max(0, (dim.width - frame.getWidth()) / 2),
                Math.max(0, (dim.height - frame.getHeight()) / 2));
        frame.setVisible(true);

        // Start
        init();
        start();

        // Resize applet on frame resize
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(contentPane.getBounds());
            }
        });
    }

    private static void enableOSXFullscreen(Window window) {
        try {
            Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
            Class params[] = new Class[]{Window.class, Boolean.TYPE};
            Method method = util.getMethod("setWindowCanFullScreen", params);
            method.invoke(util, window, true);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
            // Ignore
        }
    }

    public void setAppIcon(String imageName) {
        iconImage = getImage(imageName);
        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            // Mac OS X appears to want a 128x128 image
            final int iconSize = 128;
            BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(iconImage, 0, 0, iconSize, iconSize, null);
            g.dispose();

            try {
                Class c = Class.forName("com.apple.eawt.Application");
                Method m = c.getMethod("getApplication");
                Object applicationInstance = m.invoke(null);
                m = applicationInstance.getClass().getMethod("setDockIconImage", java.awt.Image.class);
                m.invoke(applicationInstance, image);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    public synchronized boolean dispose() {
        if (frame != null) {
            final JFrame thisFrame = frame;
            frame = null;
            stop();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    thisFrame.dispatchEvent(new WindowEvent(thisFrame, WindowEvent.WINDOW_CLOSING));
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private void setPixelScale() {
        if (autoPixelScale) {
            float area = getWidth() * getHeight();
            float areaFactor = area / (autoPixelScaleBaseWidth * autoPixelScaleBaseHeight);
            float scale = (float) (Math.log(areaFactor) / Math.log(2));
            int pixelScale;
            if (scale < 2.0) {
                pixelScale = Math.max(1, (int) Math.ceil(scale));
            } else if (scale < 4.0) {
                pixelScale = Math.round(scale);
            } else {
                pixelScale = (int) Math.floor(scale);
            }
            setPixelScale(pixelScale);
        } else {
            setPixelScale(1);
        }
    }

    private synchronized void tick() {
        if (gameLoopRunning) {
            doTick();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            });
        }
    }

    private void doTick() {
        long tickTime = System.nanoTime();
        if (tickTime - lastTickTime < 1000000000 / frameRate) {
            if (tickTime - lastTickTime < 1000000000 / frameRate - 2000000) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    // Do nothing
                }
            }
            return;
        } else {
            lastTickTime = tickTime;
        }

        boolean needsResize = false;
        if (App.getApp() == null) {
            // For appletviewer
            APP.set(this);
        }

        int oldPixelScale = pixelScale;
        setPixelScale();
        if (oldPixelScale != pixelScale && bufferStrategy != null) {
            bufferStrategy.dispose();
            bufferStrategy = null;
            needsResize = true;
        }

        if (bufferStrategy != null && canvas != null &&
                (canvas.getWidth() != getWidth() || canvas.getHeight() != getHeight())) {
            bufferStrategy.dispose();
            bufferStrategy = null;
            needsResize = true;
        }
        if (bufferStrategy == null) {
            removeAll();
            canvas = new Canvas();
            canvas.setSize(getWidth(), getHeight());
            canvas.setLocation(0, 0);
            setLayout(null);
            add(canvas);
            try {
                canvas.createBufferStrategy(2);
                bufferStrategy = canvas.getBufferStrategy();
            } catch (Exception ex) {
                // Do nothing
            }
            if (bufferStrategy == null) {
                canvas = null;
            } else {
                canvas.addMouseListener(this);
                canvas.addMouseMotionListener(this);
                canvas.addKeyListener(this);
                canvas.addFocusListener(this);
                canvas.setFocusTraversalKeysEnabled(false);
                canvas.requestFocus();
                lastTime = System.nanoTime();
                remainingTime = 0;
            }
        }
        if (bufferStrategy != null) {
            // Resize
            if (needsResize) {
                for (Scene scene : sceneStack) {
                    scene.notifySuperviewDirty();
                    scene.setSize(getWidthForScene(), getHeightForScene());
                }
            }

            // Tick
            double elapsedTime = (System.nanoTime() - lastTime) / 1000000000.0 + remainingTime;
            int ticks = (int) (simulationRate * elapsedTime);
            if (ticks > 0) {
                if (ticks > 4) {
                    ticks = 4;
                    remainingTime = 0;
                } else {
                    remainingTime = Math.max(0, elapsedTime - ticks / simulationRate);
                }
                for (int i = 0; i < ticks; i++) {
                    if (sceneStack.size() == 0) {
                        pushScene(createFirstScene());
                    }
                    View scene = sceneStack.peek();
                    scene.tick();
                }
                lastTime = System.nanoTime();
            }
            View scene = null;
            if (!sceneStack.isEmpty()) {
                scene = sceneStack.peek();
            }

            // Set cursor
            Cursor cursor = Cursor.getDefaultCursor();
            if (scene != null) {
                View pick = scene.pick(mouseX, mouseY);
                while (pick != null) {
                    Cursor pickCursor = pick.getCursor();
                    if (pickCursor != null) {
                        cursor = pickCursor;
                        break;
                    }
                    pick = pick.getSuperview();
                }
            }
            if (getCursor() != cursor) {
                setCursor(cursor);
            }

            // Draw
            Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
            if (scene == null) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            } else if (pixelScale > 1) {
                if (pixelScaleBufferedImage == null ||
                        pixelScaleBufferedImage.getWidth() != getWidthForScene() ||
                        pixelScaleBufferedImage.getHeight() != getHeightForScene()) {
                    pixelScaleBufferedImage = new BufferedImage(getWidthForScene(),
                            getHeightForScene(),
                            BufferedImage.TYPE_INT_RGB);
                }
                Graphics2D g2 = pixelScaleBufferedImage.createGraphics();
                scene.draw(g2);
                g2.dispose();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.setTransform(AffineTransform.getScaleInstance(pixelScale, pixelScale));
                g.setComposite(AlphaComposite.Src);
                g.drawImage(pixelScaleBufferedImage, 0, 0, null);
            } else {
                pixelScaleBufferedImage = null;
                g.setComposite(AlphaComposite.SrcOver);
                scene.draw(g);
            }
            g.dispose();
            bufferStrategy.show();

            // Frame rate
            actualFrameRateTickCount++;
            if (lastTime - actualFrameRateLastTime >= 500000000) {
                float duration = (lastTime - actualFrameRateLastTime) / 1000000000.0f;
                if (actualFrameRateLastTime == 0) {
                    actualFrameRate = 0;
                } else {
                    actualFrameRate = actualFrameRateTickCount / duration;
                }
                actualFrameRateTickCount = 0;
                actualFrameRateLastTime = lastTime;
            }
        }
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public float getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(float audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public float getActualFrameRate() {
        return actualFrameRate;
    }

    public int getPixelScale() {
        return pixelScale;
    }

    public void setPixelScale(int pixelScale) {
        this.pixelScale = pixelScale;
    }

    public boolean isAutoPixelScale() {
        return autoPixelScale;
    }

    public void setAutoPixelScale(boolean autoPixelScale) {
        this.autoPixelScale = autoPixelScale;
    }

    public void setAutoPixelScaleBaseSize(int w, int h) {
        this.autoPixelScaleBaseWidth = w;
        this.autoPixelScaleBaseHeight = h;
    }

    // Resources

    private static URL getResourceFromLocalSource(String name) {
        // For developers running from an IDE. Tested in Android Studio
        try {
            File file = new File(System.getProperty("user.dir") + "/src/main/resources/" + name);
            return file.toURI().toURL();
        } catch (IOException ex) {
            return null;
        }
    }

    public static URL getResource(String name) {
        URL url = App.class.getResource(name);
        if (url == null) {
            url = getResourceFromLocalSource(name);
        }
        return url;
    }

    public static InputStream getResourceAsStream(String name) {
        InputStream is = App.class.getResourceAsStream(name);
        if (is == null) {
            URL url = getResourceFromLocalSource(name);
            if (url != null) {
                try {
                    return url.openStream();
                } catch (IOException ex) {
                    return null;
                }
            }
        }
        return is;
    }

    /**
     * Get an audio file. The audio is cached in memory. If the audio couldn't be loaded, a blank
     * audio buffer is returned - this method never returns null.
     */
    public AudioBuffer getAudio(String audioName) {
        AudioBuffer audio = loadedAudio.get(audioName);
        if (audio == null && audioName != null) {
            try {
                URL url = getResource(audioName);
                if (url != null) {
                    audio = AudioBuffer.read(url);
                    if (audio != null) {
                        loadedAudio.put(audioName, audio);
                    }
                }
            } catch (IOException ex) {
                // Do nothing
            }
        }

        if (audio == null) {
            logError("Could not load audio: " + audioName);
            audio = AudioBuffer.BLANK_AUDIO;
        }
        return audio;
    }

    public void unloadAudio(String audioName) {
        loadedAudio.remove(audioName);
    }

    /**
     * Gets an image. Returns a previously-loaded cached image if available.
     */
    public BufferedImage getImage(String imageName) {
        BufferedImage image = null;
        WeakReference<BufferedImage> cachedImage = imageCache.get(imageName);
        if (cachedImage != null) {
            image = cachedImage.get();
        }
        if (image == null && imageName != null) {
            try {
                URL url = getResource(imageName);
                if (url != null) {
                    image = ImageIO.read(url);
                    if (image != null) {
                        imageCache.put(imageName, new WeakReference<>(image));
                    }
                }
            } catch (IOException ex) {
                // Do nothing
            }
        }

        if (image == null) {
            logError("Could not load image: " + imageName);
        }
        return image;
    }

    // Scene

    public abstract Scene createFirstScene();

    public boolean canPopScene() {
        return sceneStack.size() > 0;
    }

    public void popScene() {
        View scene = sceneStack.pop();
        scene.unload();
    }

    public void pushScene(Scene scene) {
        scene.setSize(getWidthForScene(), getHeightForScene());
        scene.load();
        sceneStack.push(scene);
    }

    public void setScene(Scene scene) {
        View oldScene;
        if (sceneStack.size() > 0) {
            oldScene = sceneStack.pop();
            oldScene.unload();
        }
        pushScene(scene);
    }

    private int getWidthForScene() {
        return (int) Math.ceil((float) getWidth() / pixelScale);
    }

    private int getHeightForScene() {
        return (int) Math.ceil((float) getHeight() / pixelScale);
    }

    // Input

    private KeyListener getFocusedViewKeyListener() {
        if (sceneStack.size() == 0) {
            return null;
        } else {
            Scene scene = sceneStack.peek();
            View focusedView = scene.getFocusedView();
            if (focusedView == null) {
                // No focusedView
                return null;
            } else if (focusedView.getRoot() != scene) {
                // The focusedView not in current scene graph
                return null;
            } else {
                return focusedView.getKeyListener();
            }
        }
    }

    private FocusListener getFocusedViewFocusListener() {
        if (sceneStack.size() == 0) {
            return null;
        } else {
            Scene scene = sceneStack.peek();
            View focusedView = scene.getFocusedView();
            if (focusedView == null) {
                // No focusedView
                return null;
            } else if (focusedView.getRoot() != scene) {
                // The focusedView not in current scene graph
                return null;
            } else {
                return focusedView.getFocusListener();
            }
        }
    }

    private View getMousePick(MouseEvent e) {
        View pick = null;
        mouseX = e.getX();
        mouseY = e.getY();
        if (sceneStack.size() > 0) {
            pick = sceneStack.peek().pick(e.getX(), e.getY());
        }

        return pick;
    }

    private void dispatchEnterEvents(View view, MouseEvent e) {
        while (view != null) {
            currViewsWithTouchInside.add(view);

            MouseListener l = view.getMouseListener();
            if (l != null && !prevViewsWithTouchInside.contains(view)) {
                l.mouseEntered(e);
            }
            view = view.getSuperview();
        }
    }

    private void dispatchExitEvents(MouseEvent e) {
        for (View oldView : prevViewsWithTouchInside) {
            MouseListener l = oldView.getMouseListener();
            if (l != null && !currViewsWithTouchInside.contains(oldView)) {
                l.mouseExited(e);
            }
        }

        // Swap
        List<View> temp = prevViewsWithTouchInside;
        prevViewsWithTouchInside = currViewsWithTouchInside;
        currViewsWithTouchInside = temp;
        currViewsWithTouchInside.clear();
    }

    // Propagate mouse events until it is consumed.

    public void mouseClicked(MouseEvent e) {
        View view = getMousePick(e);
        while (view != null) {
            if (view.isEnabled()) {
                MouseListener l = view.getMouseListener();
                if (l != null) {
                    l.mouseClicked(e);
                    if (e.isConsumed()) {
                        return;
                    }
                }
            }
            view = view.getSuperview();
        }
    }

    public void mousePressed(MouseEvent e) {
        if (canvas != null && !canvas.isFocusOwner()) {
            canvas.requestFocus();
        }
        View view = getMousePick(e);
        while (view != null) {
            if (view.isEnabled()) {
                MouseListener l = view.getMouseListener();
                if (l != null) {
                    l.mousePressed(e);
                    if (e.isConsumed()) {
                        return;
                    }
                }
            }
            view = view.getSuperview();
        }
    }

    public void mouseReleased(MouseEvent e) {
        View view = getMousePick(e);
        while (view != null) {
            if (view.isEnabled()) {
                MouseListener l = view.getMouseListener();
                if (l != null) {
                    l.mouseReleased(e);
                    if (e.isConsumed()) {
                        return;
                    }
                }
            }
            view = view.getSuperview();
        }
    }

    public void mouseMoved(MouseEvent e) {
        View view = getMousePick(e);
        dispatchEnterEvents(view, e);

        while (view != null) {
            if (view.isEnabled()) {
                MouseMotionListener l = view.getMouseMotionListener();
                if (l != null) {
                    l.mouseMoved(e);
                    if (e.isConsumed()) {
                        return;
                    }
                }
            }
            view = view.getSuperview();
        }

        dispatchExitEvents(e);
    }

    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseExited(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void keyPressed(KeyEvent e) {
        KeyListener keyListener = getFocusedViewKeyListener();
        if (keyListener != null) {
            keyListener.keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent e) {
        KeyListener keyListener = getFocusedViewKeyListener();
        if (keyListener != null) {
            keyListener.keyReleased(e);
        }
    }

    public void keyTyped(KeyEvent e) {
        KeyListener keyListener = getFocusedViewKeyListener();
        if (keyListener != null) {
            keyListener.keyTyped(e);
        }
    }

    public void focusGained(FocusEvent e) {
        FocusListener focusListener = getFocusedViewFocusListener();
        if (focusListener != null) {
            focusListener.focusGained(e);
        }
    }

    public void focusLost(FocusEvent e) {
        FocusListener focusListener = getFocusedViewFocusListener();
        if (focusListener != null) {
            focusListener.focusLost(e);
        }
    }
}
