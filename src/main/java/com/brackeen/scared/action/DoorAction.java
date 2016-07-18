package com.brackeen.scared.action;

import com.brackeen.scared.Map;
import com.brackeen.scared.SoundPlayer3D;
import com.brackeen.scared.Tile;
import com.brackeen.scared.entity.Player;

public class DoorAction implements Action {

    private static final int DONE = 0;
    private static final int OPENING = 1;
    private static final int OPEN = 2;
    private static final int CLOSING = 3;
    private static final int STAY_OPEN_FOREVER = 4;

    private static final int TICKS_TO_OPEN = 12; // Fast enough to not stop the player
    private static final int TICKS_TO_CLOSE = 24;
    private static final int TICKS_WAIT_BEFORE_CLOSING = 200;

    private final Map map;
    private final int x;
    private final int y;
    private final Tile tile;
    private int state;
    private int startRenderState;
    private int ticks;

    public DoorAction(Map map, int x, int y) {
        this.map = map;
        this.x = x;
        this.y = y;
        tile = map.getTileAt(x, y);

        setState(OPENING);
        if (tile.isDoorUnlocked()) {
            SoundPlayer3D.play("/sound/doorwoosh.wav", map.getPlayer(), x, y);
        } else {
            tile.setDoorUnlocked(true);
            SoundPlayer3D.play("/sound/door_unlock.wav", map.getPlayer(), x, y);
            ticks = -4; // Delay a bit before opening
        }
    }

    public int getTileX() {
        return x;
    }

    public int getTileY() {
        return y;
    }

    private void setState(int state) {
        this.state = state;
        tile.state = state;
        startRenderState = tile.renderState;
        ticks = 0;
    }

    @Override
    public void unload() {

    }

    @Override
    public boolean isFinished() {
        return (state == tile.state && (state == DONE || state == STAY_OPEN_FOREVER));
    }

    @Override
    public void tick() {
        if (isFinished()) {
            return;
        }

        ticks++;

        if (state == OPENING && ticks == 0) {
            SoundPlayer3D.play("/sound/doorwoosh.wav", map.getPlayer(), x, y);
        }

        // State set outside of this handler
        if (state != tile.state) {
            setState(tile.state);
        }

        if (ticks < 0) {
            return;
        }

        switch (state) {
            case OPENING:
                tile.renderState = startRenderState + ticks * Tile.RENDER_STATE_MAX / TICKS_TO_OPEN;
                if (tile.renderState >= Tile.RENDER_STATE_MAX) {
                    tile.renderState = Tile.RENDER_STATE_MAX;
                    setState(OPEN);
                }
                break;

            case OPEN:
                if (ticks >= TICKS_WAIT_BEFORE_CLOSING) {
                    if (shouldClose()) {
                        setState(CLOSING);
                    }
                }
                break;

            case CLOSING:
                if (!shouldClose()) {
                    setState(OPENING);
                } else {
                    if (tile.renderState == Tile.RENDER_STATE_MAX) {
                        SoundPlayer3D.play("/sound/doorwoosh.wav", map.getPlayer(), x, y);
                    }

                    tile.renderState = startRenderState - ticks * Tile.RENDER_STATE_MAX / TICKS_TO_CLOSE;
                    if (tile.renderState <= 0) {
                        tile.renderState = 0;
                        setState(DONE);
                        SoundPlayer3D.play("/sound/doorclose.wav", map.getPlayer(), x, y);
                    }
                }
                break;

            case STAY_OPEN_FOREVER:
                tile.renderState = 0;
                break;
        }
    }

    private boolean shouldClose() {
        if (tile.hasEntities()) {
            return false;
        }
        Player p = map.getPlayer();
        float dx = Math.abs(p.getX() - (x + 0.5f));
        float dy = Math.abs(p.getY() - (y + 0.5f));
        return (dx >= 1.5f || dy >= 1.5f);
    }
}
