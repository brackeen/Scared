package com.brackeen.app.view;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

public class Button extends View implements MouseListener {
    
    public interface Listener {
        public void buttonClicked(Button button);
    }

    private enum State {
        NORMAL,
        HOVER,
        PRESSED,
    }
    
    private BufferedImage normalImage;
    private BufferedImage hoverImage;
    private BufferedImage pressedImage;
    private BufferedImage normalSelectedImage;
    private BufferedImage hoverSelectedImage;
    private BufferedImage pressedSelectedImage;
    private State state = State.NORMAL;
    private boolean selected = false;
    private boolean armed = false;
    private View rootWhenArmed;
    private Listener buttonListener;
    
    public Button(BufferedImage normalImage) {
        setNormalImage(normalImage);
        setMouseListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sizeToFit();
    }
    
    @Override
    public void sizeToFit() {
        BufferedImage image = getDisplayedImage();
        if (image == null) {
            setSize(0, 0);
        }
        else {
            setSize(image.getWidth(), image.getHeight());
        }
    }

    public Listener getButtonListener() {
        return buttonListener;
    }

    public void setButtonListener(Listener buttonListener) {
        this.buttonListener = buttonListener;
    }
    
    private void setState(State state) {
        this.state = state;
    }
    
    public BufferedImage getHoverImage() {
        return hoverImage;
    }

    public void setHoverImage(BufferedImage hoverImage) {
        this.hoverImage = hoverImage;
    }

    public BufferedImage getHoverSelectedImage() {
        return hoverSelectedImage;
    }

    public void setHoverSelectedImage(BufferedImage hoverSelectedImage) {
        this.hoverSelectedImage = hoverSelectedImage;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean isSelected) {
        this.selected = isSelected;
    }

    public BufferedImage getNormalImage() {
        return normalImage;
    }

    public void setNormalImage(BufferedImage normalImage) {
        this.normalImage = normalImage;
    }

    public BufferedImage getNormalSelectedImage() {
        return normalSelectedImage;
    }

    public void setNormalSelectedImage(BufferedImage normalSelectedImage) {
        this.normalSelectedImage = normalSelectedImage;
    }

    public BufferedImage getPressedImage() {
        return pressedImage;
    }

    public void setPressedImage(BufferedImage pressedImage) {
        this.pressedImage = pressedImage;
    }

    public BufferedImage getPressedSelectedImage() {
        return pressedSelectedImage;
    }

    public void setPressedSelectedImage(BufferedImage pressedSelectedImage) {
        this.pressedSelectedImage = pressedSelectedImage;
    }
    
    private BufferedImage getDisplayedImage() {
        if (selected) {
            BufferedImage defaultImage = normalSelectedImage;
            if (defaultImage == null) {
                defaultImage = normalImage;
            }
            if (state == State.NORMAL) {
                return defaultImage;
            }
            else if (state == State.HOVER) {
                if (hoverSelectedImage != null) {
                    return hoverSelectedImage;
                }
                else {
                    return defaultImage;
                }
            }
            else {
                if (pressedSelectedImage != null) {
                    return pressedSelectedImage;
                }
                else {
                    return defaultImage;
                }
            }
        }
        else {
            if (state == State.NORMAL) {
                return normalImage;
            }
            else if (state == State.HOVER) {
                if (hoverImage != null) {
                    return hoverImage;
                }
                else {
                    return normalImage;
                }
            }
            else {
                if (pressedImage != null) {
                    return pressedImage;
                }
                else {
                    return normalImage;
                }
            }
        }
    }

    @Override
    public void onDraw(Graphics2D g) {
        g.drawImage(getDisplayedImage(), null, null);
    }

    public void mouseEntered(MouseEvent me) {
        if (armed && me.getID() == MouseEvent.MOUSE_DRAGGED) {
            setState(State.PRESSED);
        }
        else if (me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_ENTERED) {
            setState(State.HOVER);
        }
    }

    public void mouseExited(MouseEvent me) {
        setState(State.NORMAL);
    }

    public void mouseClicked(MouseEvent me) {
        // Do nothing - press+release events sent
    }

    public void mousePressed(MouseEvent me) {
        setState(State.PRESSED);
        rootWhenArmed = getRoot();
        armed = true;
    }

    public void mouseReleased(MouseEvent me) {
        View root = getRoot();
        View view = root.pick(me.getX(), me.getY());
        boolean isOver = isAncestorOf(view);
        boolean isSameRoot = root != null && rootWhenArmed == root;
        boolean isTap = armed && state == State.PRESSED && isOver && isSameRoot;

        if (isOver) {
            setState(State.HOVER);
        }
        else {
            setState(State.NORMAL);
        }
        rootWhenArmed = null;
        armed = false;

        if (isTap && buttonListener != null) {
            buttonListener.buttonClicked(this);
        }    
    }
}
