package com.brackeen.scared.action;

import com.brackeen.app.App;
import com.brackeen.scared.Map;
import com.brackeen.scared.SoftTexture;
import com.brackeen.scared.Tile;

public class MovableWallAction implements Action {

    public static final int STATE_DONE = 0;
    private static final int STATE_MOVING = 1;

    private static final int TICKS_PER_TILE_MOVE = 60;

    private final Map map;
    private int x;
    private int y;
    private final int dx;
    private final int dy;
    private Tile tile;
    private final SoftTexture floorTexture;
    private int index;
    private int ticks;

    public MovableWallAction(Map map, int x, int y) {
        int playerTileX = (int) map.getPlayer().getX();
        int playerTileY = (int) map.getPlayer().getY();

        this.map = map;
        this.x = x;
        this.y = y;
        this.dx = x - playerTileX;
        this.dy = y - playerTileY;

        Tile playerTile = map.getTileAt(playerTileX, playerTileY);
        tile = map.getTileAt(x, y);
        tile.state = STATE_MOVING;

        map.setDefaultFloorTexture(playerTile.getTexture());
        floorTexture = playerTile.getTexture();

        App.getApp().getAudio("/sound/wallmove.wav", 1).play();
        index = 0;
        ticks = 0;
    }

    @Override
    public void unload() {
        // Do nothing
    }

    @Override
    public boolean isFinished() {
        return (index > 2);
    }

    @Override
    public void tick() {
        if (isFinished()) {
            return;
        }

        if (ticks < TICKS_PER_TILE_MOVE) {
            ticks++;
            tile.renderState = Tile.RENDER_STATE_MAX * ticks / TICKS_PER_TILE_MOVE;
        } else {
            index++;
            SoftTexture texture = tile.getTexture();
            tile.setTexture(floorTexture);
            tile.type = 0;
            tile.subtype = 0;
            tile.state = STATE_DONE;
            tile.renderState = 0;

            x += dx;
            y += dy;

            tile = map.getTileAt(x, y);
            tile.setTexture(texture);
            tile.type = Tile.TYPE_MOVABLE_WALL;
            tile.subtype = 0;
            tile.state = STATE_MOVING;
            tile.renderState = 0;

            if (index == 2) {
                index = 3;
                tile.type = Tile.TYPE_WALL;
                tile.state = STATE_DONE;
                tile.renderState = 0;
            }
            ticks = 0;
        }
    }
}
