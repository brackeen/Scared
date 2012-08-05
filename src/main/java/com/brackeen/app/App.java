package com.brackeen.app;

import com.brackeen.app.view.Scene;
import com.brackeen.app.view.View;
import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.swing.Timer;

/**
The App class sets up the animation loop and provides methods to load images and set the 
current scene.
*/
public abstract class App extends Applet implements MouseListener, MouseMotionListener, 
        KeyListener, FocusListener {
    
    private static final InheritableThreadLocal<App> APP =
            new InheritableThreadLocal<App>();
    
    public static App getApp() {
        return APP.get();
    }
    
    private static int MAX_LOG_LINES = 1000;
    
    public static void log(String statement) {
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

    private final float frameRate = 60;
    private final Timer timer = new Timer((int)(1000 / frameRate), new ActionListener() {

        // Using Swing's Timer because it executes on the EDT, so there will be no threading issues.
        public void actionPerformed(ActionEvent ae) {
            tick();
        }
        
    });
    private long lastTime = 0;
    private double remainingTime = 0;
    
    private List<String> log = new ArrayList<String>();
    
    private final HashMap<String, WeakReference<BufferedImage>> imageCache =
            new HashMap<String, WeakReference<BufferedImage>>();
    private final HashMap<String, BufferedAudio> loadedAudio = new HashMap<String, BufferedAudio>();
    private final Stack<Scene> sceneStack = new Stack<Scene>();
    private List<View> prevViewsWithTouchInside = new ArrayList<View>();
    private List<View> currViewsWithTouchInside = new ArrayList<View>();
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
        
    @Override
    public synchronized void init() {
       
    }
    
    @Override
    public synchronized void start() {
        lastTime = System.nanoTime();
        remainingTime = 0;
        timer.start();
    }
    
    @Override
    public synchronized void stop() {
        timer.stop();
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
        for (BufferedAudio audio : loadedAudio.values()) {
            audio.dispose();
        }
        loadedAudio.clear();
        imageCache.clear();
        prevViewsWithTouchInside.clear();
        currViewsWithTouchInside.clear();
        log.clear();
        bufferStrategy = null;
        canvas = null;
        removeAll();
    }
    
    private synchronized void tick() {
        if (App.getApp() == null) {
            // For appletviewer
            APP.set(this);
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
            }
            catch (Exception ex) {
                // Do nothing
            }
            if (bufferStrategy == null) {
                canvas = null;
            }
            else {
                canvas.addMouseListener(this);
                canvas.addMouseMotionListener(this);
                canvas.addKeyListener(this);
                canvas.addFocusListener(this);
                canvas.setFocusTraversalKeysEnabled(false);
                lastTime = System.nanoTime();
                remainingTime = 0;
            }
        }
        if (bufferStrategy != null) {
            
            View scene = null;
            
            // Tick
            double elapsedTime = (System.nanoTime() - lastTime) / 1000000000.0 + remainingTime;
            int ticks = Math.max(1, (int)(frameRate * elapsedTime));
            if (ticks > 4) {
                ticks = 4;
                remainingTime = 0;
            }
            else {
                remainingTime = Math.max(0, elapsedTime - ticks / frameRate);
            }
            for (int i = 0; i < ticks; i++) {
                if (sceneStack.size() == 0) {
                    pushScene(createFirstScene());
                }
                scene = sceneStack.peek();
                scene.tick();
            }
            lastTime = System.nanoTime();
            
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
            setCursor(cursor);

            // Draw
            Graphics2D g = (Graphics2D)bufferStrategy.getDrawGraphics();
            if (scene == null) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            else {
                scene.draw(g);
            }
            g.dispose();
            bufferStrategy.show();
        }
    }
    
    // Resources
    
    /**
    Get an audio file. The first time the audio is loaded, the maxSimultaneousStreams param
    sets how many times the audio file can be played at the same time. If the audio file was
    previously loaded, the maxSimultaneousStreams is ignored.
    */
    public BufferedAudio getAudio(String audioName, int maxSimultaneousStreams) {
        BufferedAudio audio = loadedAudio.get(audioName);
        if (audio == null && audioName != null) {
            try {
                URL url = getClass().getResource(audioName);
                if (url != null) {
                    audio = BufferedAudio.read(url, maxSimultaneousStreams);
                    if (audio != null) {
                        loadedAudio.put(audioName, audio);
                    }
                }
            }
            catch (IOException ex) {
                // Do nothing
            }
        }
        
        if (audio == null) {
            log("Could not load audio: " + audioName);
        }
        return audio;
    }
    
    public void unloadAudio(String audioName) {
        BufferedAudio audio = loadedAudio.get(audioName);
        if (audio != null) {
            loadedAudio.remove(audioName);
            audio.dispose();
        }
    }
    
    /**
    Gets an image. Returns a previously-loaded cached image if available.
    */
    public BufferedImage getImage(String imageName) {
        BufferedImage image = null;
        WeakReference<BufferedImage> cachedImage = imageCache.get(imageName);
        if (cachedImage != null) {
            image = cachedImage.get();
        }
        if (image == null && imageName != null) {
            try {
                URL url = getClass().getResource(imageName);
                if (url != null) {
                    image = ImageIO.read(url);
                    if (image != null) {
                        imageCache.put(imageName, new WeakReference<BufferedImage>(image));
                    }
                }
            }
            catch (IOException ex) {
                // Do nothing
            }
        }
        
        if (image == null) {
            log("Could not load image: " + imageName);
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
        scene.setSize(getWidth(), getHeight());
        scene.load();
        sceneStack.push(scene);
    }
    
    public void setScene(Scene scene) {
        if (canPopScene()) {
            popScene();
        }
        pushScene(scene);
    }
    
    // Input
    
    private KeyListener getFocusedViewKeyListener() {
        if (sceneStack.size() == 0) {
            return null;
        }
        else {
            Scene scene = sceneStack.peek();
            View focusedView = scene.getFocusedView();
            if (focusedView == null) {
                // No focusedView
                return null;
            }
            else if (focusedView.getRoot() != scene) {
                // The focusedView not in current scene graph
                return null;
            }
            else {
                return focusedView.getKeyListener();
            }
        }
    }
    
    private FocusListener getFocusedViewFocusListener() {
        if (sceneStack.size() == 0) {
            return null;
        }
        else {
            Scene scene = sceneStack.peek();
            View focusedView = scene.getFocusedView();
            if (focusedView == null) {
                // No focusedView
                return null;
            }
            else if (focusedView.getRoot() != scene) {
                // The focusedView not in current scene graph
                return null;
            }
            else {
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
    
    private void dispatchExitEvents(View view, MouseEvent e) {
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
    
    // Propogate mouse events until it is consumed.
    
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
        
        dispatchExitEvents(view, e);
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
