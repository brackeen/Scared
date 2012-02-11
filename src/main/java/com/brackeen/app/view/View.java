package com.brackeen.app.view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class View {
        
    private View superview = null;
    private List<View> subviews = new ArrayList<View>();
    private Color backgroundColor = null;
    private float x = 0;
    private float y = 0;
    private float width = 0;
    private float height = 0;
    private float anchorX = 0;
    private float anchorY = 0;
    private float opacity = 1;
    private boolean visible = true;
    private boolean enabled = true;
    
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private KeyListener keyListener;
    private FocusListener focusListener;
    private Cursor cursor;
    
    private AffineTransform worldTransform = new AffineTransform();
    private boolean localTransformDirty = true;
    private long localTransformModCount = 0;
    private long superviewTransformModCount = 0;
    
    public View() {
        
    }
    
    public View(float x, float y, float width, float height) {
        setLocation(x, y);
        setSize(width, height);
    }
    
    // Input

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Cursor getCursor() {
        if (cursor == null) {
            if (superview != null) {
                return superview.getCursor();
            }
            else {
                return Cursor.getDefaultCursor();
            }
        }
        else {
            return cursor;
        }
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public KeyListener getKeyListener() {
        return keyListener;
    }

    public void setKeyListener(KeyListener keyListener) {
        this.keyListener = keyListener;
    }

    public MouseListener getMouseListener() {
        return mouseListener;
    }

    public void setMouseListener(MouseListener mouseListener) {
        this.mouseListener = mouseListener;
    }

    public MouseMotionListener getMouseMotionListener() {
        return mouseMotionListener;
    }

    public void setMouseMotionListener(MouseMotionListener mouseMotionListener) {
        this.mouseMotionListener = mouseMotionListener;
    }

    public FocusListener getFocusListener() {
        return focusListener;
    }

    public void setFocusListener(FocusListener focusListener) {
        this.focusListener = focusListener;
    }
        
    // Dimensions

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
        localTransformDirty = true;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
        localTransformDirty = true;
    }
    
    public void setLocation(float x, float y) {
        setX(x);
        setY(y);
    }

    public float getAnchorX() {
        return anchorX;
    }

    public void setAnchorX(float anchorX) {
        this.anchorX = anchorX;
        localTransformDirty = true;
    }

    public float getAnchorY() {
        return anchorY;
    }

    public void setAnchorY(float anchorY) {
        this.anchorY = anchorY;
        localTransformDirty = true;
    }
    
    public void setAnchor(float anchorX, float anchorY) {
        setAnchorX(anchorX);
        setAnchorY(anchorY);
    }
    
    public float getWidth() {
        return this.width;
    }

    public void setWidth(float width) {
        this.width = width;
        localTransformDirty = true;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
        localTransformDirty = true;
    }

    public void setSize(float width, float height) {
        setWidth(width);
        setHeight(height);
    }
    
    /**
    Sizes this View to fit it's contents. Subclasses may use this. Does nothing by default.
    */
    public void sizeToFit() {
        // Do nothing
    }
    
    // Color

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    // Subviews and superviews
    
    public View getSuperview() {
        return superview;
    }
    
    public View getRoot() {
        View root = this;
        while (root.superview != null) {
            root = root.superview;
        }
        return root;
    }
    
    public boolean isAncestorOf(View view) {
        while (view != null) {
            if (view == this) {
                return true;
            }
            else {
                view = view.getSuperview();
            }
        }
        return false;
    }

    public void removeFromSuperview() {
        if (superview != null) {
            superview.subviews.remove(this);
            superview = null;
        }
    }
    
    public List<View> getSubviews() {
        return Collections.unmodifiableList(subviews);
    }
        
    public void removeAllSubviews() {
        for (View subview : subviews) {
            subview.removeFromSuperview();
        }
        subviews.clear();
    }
    
    public void addSubview(View subview) {
        subview.removeFromSuperview();
        subviews.add(subview);
        subview.superview = this;
        superviewTransformModCount = -1;
    }

    // Notifications
    
    public final void load() {
        onLoad();
        for (View subview : subviews) {
            subview.load();
        }
    }
    
    public void onLoad() {
        
    }
    
    public final void unload() {
        onUnload();
        for (View subview : subviews) {
            subview.unload();
        }
    }
    
    public void onUnload() {
        
    }
    
    public final void tick() {
        onTick();
        for (View subview : subviews) {
            subview.tick();
        }
    }
    
    public void onTick() {
        
    }
    
    // Transforms
    
    private void updateTransforms() {
        
        boolean worldTransformDirty = false;
        
        if (superview == null) {
            if (localTransformDirty || superviewTransformModCount != 0) {
                worldTransform.setToIdentity();
                superviewTransformModCount = 0;
                worldTransformDirty = true;
            }
        }
        else {
            superview.updateTransforms();
            if (localTransformDirty || superviewTransformModCount != superview.localTransformModCount) {
                worldTransform.setTransform(superview.worldTransform);
                superviewTransformModCount = superview.localTransformModCount;
                worldTransformDirty = true;
            }
        }
        
        if (worldTransformDirty) {
            worldTransform.translate(x, y);
            
            if (anchorX != 0 || anchorY != 0) {
                float anchorLocalX = anchorX * getWidth();
                float anchorLocalY = anchorY * getHeight();
                worldTransform.translate(-anchorLocalX, -anchorLocalY);
            }
            
            localTransformModCount++;
            localTransformDirty = false;
        }
    }
    
    private boolean isClippedToBounds() {
        return false;
    }
    
    public Point2D.Float getLocalLocation(float worldX, float worldY) {
        updateTransforms();
        Point2D.Float src = new Point2D.Float(worldX, worldY);
        Point2D.Float dst = new Point2D.Float(0, 0);
        try {
            worldTransform.inverseTransform(src, dst);
            return dst;
        }
        catch (NoninvertibleTransformException ex) {
            return null;
        }
    }
    
    public boolean contains(float worldX, float worldY) {
        Point2D.Float local = getLocalLocation(worldX, worldY);
        if (local == null) {
            return false;
        }
        return (local.x >= 0 && local.x < getWidth() && local.y >= 0 && local.y < getHeight());
    }
    
    public View pick(float worldX, float worldY) {
        return pick(worldX, worldY, false);
    }

    public View pick(float worldX, float worldY, boolean allowDisabledViews) {
        if (!isVisible() || getOpacity() <= 0) {
            return null;
        }
        else {
            boolean inside = contains(worldX, worldY);
            if (isClippedToBounds() && !inside) {
                return null;
            }
            for (int i = subviews.size() - 1; i >= 0; i--) {
                View pickedView = subviews.get(i).pick(worldX, worldY, allowDisabledViews);
                if (pickedView != null) {
                    return pickedView;
                }
            }
            
            boolean isPick = inside && (allowDisabledViews || isEnabled());

            return isPick ? this : null;
        }
    }
    
    // Drawing
    
    public final void draw(Graphics2D g) {
        if (!visible || opacity <= 0) {
            return;
        }

        updateTransforms();
        
        Composite oldComposite = null;
        
        if (opacity < 1) {
            oldComposite = g.getComposite();
            if (oldComposite instanceof AlphaComposite) {
                AlphaComposite ac = (AlphaComposite)oldComposite;
                g.setComposite(ac.derive(opacity * ac.getAlpha()));
            }
        }
        
        g.setTransform(worldTransform);
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fill(new Rectangle2D.Float(0, 0, getWidth(), getHeight()));
        }
        onDraw(g);
        
        for (View subview : subviews) {
            subview.draw(g);
        }
        
        if (oldComposite != null) {
            g.setComposite(oldComposite);
        }
    }
    
    /**
    Draws the view. The background of the View, if any, is already drawn. The Graphics transform is
    already set, and there's no need to offset the rendering. This View's subviews, if any,
    are drawn afterwards.
    */
    public void onDraw(Graphics2D g) {
        
    }
}
