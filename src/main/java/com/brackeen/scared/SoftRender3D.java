package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.View;
import com.brackeen.scared.entity.Entity;
import com.brackeen.scared.entity.Player;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
Simple raycasting engine.
Internally uses sin/cos tables and fixed-point math.
*/
public class SoftRender3D extends View {
    
    // Angle. Instead of 0..360, it is 0..4096.
    
    private static final int NUM_DEGREES = 0x1000;
    private static final int NUM_DEGREES_MASK = NUM_DEGREES - 1;
    private static final int ANGLE_360 = NUM_DEGREES;
    private static final int ANGLE_270 = NUM_DEGREES * 3 / 4;
    private static final int ANGLE_180 = NUM_DEGREES / 2;
    private static final int ANGLE_90 = NUM_DEGREES / 4;
    private static final int ANGLE_0 = 0;
    
    private static final int DEPTH_SCALE = 48;

    private static int degreesToAngle(float degrees) {
        return Math.round(degrees * NUM_DEGREES / 360) & NUM_DEGREES_MASK;
    }
    
    private static int radiansToAngle(double radians) {
        return ((int)Math.round(radians * NUM_DEGREES / (2 * Math.PI))) & NUM_DEGREES_MASK;
    }
    
    private static double angleToRadians(int angle) {
        return angle * (2 * Math.PI) / NUM_DEGREES;
    }
    
    private static float angleToDegrees(int angle) {
        return angle * 360f / NUM_DEGREES;
    }

    // Fixed-point math
    
    private static final int FRACTION_BITS = 16;
    private static final int ONE = (1 << FRACTION_BITS);
    private static final int ONE_HALF = ONE >> 1;
    private static final int FRACTION_MASK = (1 << FRACTION_BITS) - 1;    
    
    private static int toFixedPoint(double n) {
        return (int)Math.round(n * ONE);
    }
    
    private static float toFloat(int f) {
        return (float)f / ONE;
    }
    
    private static int toIntFloor(int f) {
        return f >> FRACTION_BITS;
    }
    
    private static int toIntRound(int f) {
        return toIntFloor(f + ONE_HALF);
    }

    private static int toIntCeil(int f) {
        return -toIntFloor(-f);
    }
    
    private static int fracPart(int f) {
        return Math.abs(f) & FRACTION_MASK;
    }
    
    private static int floor(int f) {
        return f & ~FRACTION_MASK;
    }
    
    private static int ceil(int f) {
        return -floor(-f);
    }
    
    private static int mul(int f1, int f2) {
        return (int)(((long)f1 * f2) >> FRACTION_BITS);
    }
    
    private static int div(int f1, int f2) {
        return (int)(((long)f1 << FRACTION_BITS) / f2);
    }
    
    private static int mulDiv(int f1, int f2, int f3) {
        return (int)((long)f1 * f2 / f3);
    }

    // Ray casting 
    
    private static class Ray {
        int f_dist;
        int sliver;
        int floorDrawY;
        SoftTexture texture;
        
        public void reset() {
            f_dist = Integer.MAX_VALUE;
            sliver = 0;
            floorDrawY = 0;
            texture = null;
        }
    }

    private static final int WINDOW_WEST_EAST = 1;
    private static final int WINDOW_NORTH_SOUTH = 2;
    
    private SoftTexture dstBuffer;
    private BufferedImage bufferedImage;
    private int pixelScale = 1;
    
    private Map map;
    private SoftTexture background;
    private List<Tile> visibleFloors = new ArrayList<Tile>();
    
    private SoftTexture[] doorTextures = new SoftTexture[4];
    private SoftTexture doorSideTexture;
    private SoftTexture windowTexture;
    private SoftTexture[] generatorTextures = new SoftTexture[2];
    private SoftTexture[] exitTextures = new SoftTexture[2];
    
    private float fov;
    private float focalDistance;
    
    // Fixed point numbers start with 'f_'
    private int f_cameraX;
    private int f_cameraY;
    private int f_cameraZ;
    private int cameraAngle;
    
    private int[] rayAngleTable;
    private int[] f_cosTable;
    private int[] f_sinTable;
    private int[] f_tanTable;
    private int[] f_cotTable;
    
    private Ray[] rays;
    
    /**
    
    @param width width in pixels
    @param height height in pixels
    @param fovInDegrees field of view in degrees
    */
    public SoftRender3D(HashMap<String, SoftTexture> textureCache, float width, float height) {
        this.fov = 60;

        f_cosTable = new int[NUM_DEGREES];
        f_sinTable = new int[NUM_DEGREES];
        f_tanTable = new int[NUM_DEGREES];
        f_cotTable = new int[NUM_DEGREES];
        for (int i = 0; i < NUM_DEGREES; i++) {
            f_cosTable[i] = toFixedPoint(Math.cos(angleToRadians(i)));
            f_sinTable[i] = toFixedPoint(Math.sin(angleToRadians(i)));
            f_tanTable[i] = toFixedPoint(Math.tan(angleToRadians(i)));
            f_cotTable[i] = toFixedPoint(1 / Math.tan(angleToRadians(i)));
        }
        
        setSize(width, height);
        
        for (int i = 0; i < doorTextures.length; i++) {
            doorTextures[i] = textureCache.get("door0" + i + ".png");
        }
        for (int i = 0; i < generatorTextures.length; i++) {
            generatorTextures[i] = textureCache.get("generator0" + i + ".png");
        }
        for (int i = 0; i < exitTextures.length; i++) {
            exitTextures[i] = textureCache.get("exit0" + i + ".png");
        }
        windowTexture = textureCache.get("window00.png");
        doorSideTexture = textureCache.get("wall07.png");
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public int getPixelScale() {
        return pixelScale;
    }

    public void setPixelScale(int pixelScale) {
        this.pixelScale = pixelScale;
        updateSize();
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
        updateSize();
    }
    
    /**
    Gets the view angle, in degrees, at location x within the view.
    */
    public float getAngleAt(int x) {
        x /= pixelScale;
        x = Math.max(0, x);
        x = Math.min(x, rayAngleTable.length - 1);
        return angleToDegrees((rayAngleTable[x] - cameraAngle) & NUM_DEGREES_MASK);
    }
    
    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        updateSize();
    }
    
    private void updateSize() {
        int w = ((int)getWidth()) / pixelScale;
        int h = ((int)getHeight()) / pixelScale;
        dstBuffer = null;
        if (bufferedImage != null) {
            bufferedImage.flush();
            bufferedImage = null;
        }
        dstBuffer = new SoftTexture(w, h);
        bufferedImage = dstBuffer.getBufferedImageView();
        
        focalDistance = (float)(w / (2 * Math.tan(Math.toRadians(fov)/2)));
        
        rayAngleTable = new int[w];
        for (int i = 0; i < w; i++) {
            double d = Math.atan2(i - w / 2, focalDistance);
            rayAngleTable[i] = radiansToAngle(d);
        }
        
        rays = new Ray[w];
        for (int i = 0; i < w; i++) {
            rays[i] = new Ray();
        }
        
        // Scale the background so that it covers half the view height
        background = null;
        int backgroundHeight = h/2;
        BufferedImage bgImage = App.getApp().getImage("/background/background.png");
        if (bgImage.getHeight() != backgroundHeight) {
            int backgroundWidth = bgImage.getWidth()* backgroundHeight / bgImage.getHeight();
            bgImage = getScaledInstance(bgImage, backgroundWidth, backgroundHeight);
        }
        background = new SoftTexture(bgImage);
    }
    
    private static BufferedImage getScaledInstance(BufferedImage srcImage, int width, int height) {
        ImageFilter filter = new AreaAveragingScaleFilter(width, height);
        ImageProducer producer = new FilteredImageSource(srcImage.getSource(), filter);
        Image scaledImage = Toolkit.getDefaultToolkit().createImage(producer);

        boolean srcIsOpaque = srcImage.getTransparency() == Transparency.OPAQUE;
        BufferedImage buf = new BufferedImage(width, height,
                srcIsOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.drawImage(scaledImage,0,0,null);
        g.dispose();
        return buf;
    }
    
    public void setCamera(float x, float y, float z, float directionInDegrees) {
        f_cameraX = toFixedPoint(x);
        f_cameraY = toFixedPoint(y);
        f_cameraZ = toFixedPoint(z);
        cameraAngle = degreesToAngle(directionInDegrees);
    }
    
    @Override
    public void onDraw(Graphics2D g) {
        if (map != null) {
            List<Entity> visibleEntities = raycast();
 
            drawBackground();
            drawWalls();
            drawFloors();
            drawEntities(visibleEntities);
        
            if (pixelScale > 1) {
                Object oldValue = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                if (oldValue == null) {
                    oldValue = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(bufferedImage, 0, 0, bufferedImage.getWidth() * pixelScale, bufferedImage.getHeight() * pixelScale, null);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldValue);
            }
            else { 
                g.drawImage(bufferedImage, null, null);
            }
        }
    }
    
    private void drawBackground() {
        int bd = background.getWidth() * 2;
        int backgroundX = (cameraAngle & NUM_DEGREES_MASK) * bd / NUM_DEGREES - bd;
        dstBuffer.draw(background, backgroundX, 0, true);
        dstBuffer.draw(background, backgroundX + background.getWidth(), 0, true);
        dstBuffer.draw(background, backgroundX + background.getWidth() * 2, 0, true);
    }
    
    private void drawWalls() {
        int f_focalDistance = toFixedPoint(focalDistance);
        int viewWidth = dstBuffer.getWidth();
        int viewHeight = dstBuffer.getHeight();
        for (int x = 0; x < viewWidth; x++) {
            Ray ray = rays[x];
            if (ray.f_dist >= 0 && ray.f_dist < Integer.MAX_VALUE) {
                int wallHeight = toIntCeil(div(f_focalDistance, ray.f_dist));
                wallHeight = (wallHeight + 1) & ~1; // Make it even, rounding up
                if (wallHeight > 0) {
                    int depth = toIntFloor(ray.f_dist * DEPTH_SCALE);
                    int bottom = viewHeight / 2 + toIntFloor(wallHeight * f_cameraZ) - 1;
                    int top = bottom - wallHeight + 1;
                    ray.floorDrawY = bottom;
                    
                    drawTextureSliver(ray.texture, true, ray.sliver, depth, viewWidth - x - 1, top, wallHeight);
                }
            }
        }
    }
    
    private void drawFloors() {
        
        int[] dstData = dstBuffer.getData();
        int viewWidth = dstBuffer.getWidth();
        int viewHeight = dstBuffer.getHeight();
        
        // Basically, we know screen_x, screen_y, and z.  Solve for x and y
        //
        //          focalDistance * z
        // x = ----------------------------
        //      screen_y - screen_height/2
        //
        //      (screen_x - screen_width/2) * x
        // y = ---------------------------------
        //             focalDistance
        //
        //      (screen_x - screen_width/2) * z
        // y = ---------------------------------
        //       screen_y - screen_height/2
        //


        int f_focalDistance = toFixedPoint(focalDistance);
        long f_cosCameraAngle = f_cosTable[cameraAngle];
        long f_sinCameraAngle = f_sinTable[cameraAngle];
        
        int tx1 = mul(f_focalDistance, f_cameraZ);
        int ty1 = (viewWidth/2 - 1) * f_cameraZ;
        long txStart = tx1 * f_cosCameraAngle + ty1 * f_sinCameraAngle;
        long tyStart = -tx1 * f_sinCameraAngle + ty1 * f_cosCameraAngle;
        long tIncStartSin = -f_cameraZ * f_sinCameraAngle;
        long tIncStartCos = -f_cameraZ * f_cosCameraAngle;
     
        int firstY = viewHeight/2 + 1;
        int startDestOffset = firstY * viewWidth + (viewWidth - 1);
        int lastMapX = -1;
        int lastMapY = -1;

        SoftTexture defaultFloorTexture = map.getDefaultFloorTexture();
        int[] textureData = defaultFloorTexture.getData();
        int textureSizeBits = defaultFloorTexture.getSizeBits();
        
        int startX = 0;
        int endX = viewWidth;
        
        for (int currentY = firstY; currentY < viewHeight; currentY++) {
            int row = currentY - viewHeight / 2;
                        
            int tx = (int)((txStart / row) >> FRACTION_BITS) + f_cameraX;
            int ty = (int)((tyStart / row) >> FRACTION_BITS) + f_cameraY;
            
            long txInc1 = tIncStartSin / row;
            long tyInc1 = tIncStartCos / row;
            
            int txInc = (int)(txInc1 >> FRACTION_BITS);
            int tyInc = (int)(tyInc1 >> FRACTION_BITS);
            
            int fxInc = (int)(txInc1 & FRACTION_MASK);
            int fyInc = (int)(tyInc1 & FRACTION_MASK);
            int fx = startX * fxInc;
            int fy = startX * fyInc;
            
            int destOffset = startDestOffset - startX;
            tx += startX * txInc;
            ty += startX * tyInc;
            
            int f_dist = (int)(((long)f_cameraZ * f_focalDistance / row) >> FRACTION_BITS);
            int depth = toIntFloor(f_dist * DEPTH_SCALE);
            
            for (int x = startX; x < endX; x++) {
                if (currentY > rays[x].floorDrawY) {
                    int mapX = tx >> FRACTION_BITS;
                    int mapY = ty >> FRACTION_BITS;

                    if (mapX != lastMapX || mapY != lastMapY) {
                        Tile tile = map.getTileAt(mapX, mapY);
                        
                        if (tile == null || tile.type == Tile.TYPE_MOVABLE_WALL) {
                            textureData = defaultFloorTexture.getData();
                            textureSizeBits = defaultFloorTexture.getSizeBits();
                        }
                        else {
                            textureData = tile.getTexture().getData();
                            textureSizeBits = tile.getTexture().getSizeBits();
                        }
                        
                        lastMapX = mapX;
                        lastMapY = mapY;
                    }

                    int txTrans = ((tx & FRACTION_MASK) << textureSizeBits) >> FRACTION_BITS;
                    int tyTrans = ((ty & FRACTION_MASK) << textureSizeBits) >> FRACTION_BITS;
                    
                    int srcColor = textureData[txTrans + (tyTrans << textureSizeBits)];
                    drawPixel(dstData, destOffset, srcColor, depth);
                }
                fx += fxInc;
                fy += fyInc;
                
                tx += txInc + (fx >> FRACTION_BITS);
                ty += tyInc + (fy >> FRACTION_BITS);
                
                fx &= FRACTION_MASK;
                fy &= FRACTION_MASK;
    
                destOffset--;
            }
            startDestOffset += viewWidth;
        }
    }
    
    private void drawEntities(List<Entity> visibleEntities) {
        if (visibleEntities.size() > 0) {
            int viewWidth = dstBuffer.getWidth();
            int viewHeight = dstBuffer.getHeight();
            float cameraX = toFloat(f_cameraX);
            float cameraY = toFloat(f_cameraY);
            float cameraZ = toFloat(f_cameraZ);
            float cosAngle = (float)Math.cos(angleToRadians(cameraAngle));
            float sinAngle = (float)Math.sin(angleToRadians(cameraAngle));
            for (Entity entity : visibleEntities) {
                SoftTexture texture = entity.getTexture();
                float dist = entity.getDistanceFromCamera();
                if (dist > 0 && texture != null) {
                    float dx = entity.getX() - cameraX;
                    float dy = entity.getY() - cameraY;
                    float thing = dx * sinAngle + dy * cosAngle;

                    float w = texture.getWidth() * entity.getTextureScale();
                    float h = texture.getHeight() * entity.getTextureScale();
                    int renderWidth = (int)(focalDistance * w / dist);
                    int renderHeight = (int)(focalDistance * h / dist);
                    int renderX = (int)(focalDistance * thing / dist + (viewWidth - renderWidth) / 2);
                    
                    int renderY = (int)(viewHeight / 2 - 
                            renderHeight * (1 - cameraZ) -
                            entity.getZ() * focalDistance / dist +
                            cameraZ * (focalDistance * (1 - h) / dist));

                    if (renderWidth > viewWidth * 4) {
                        // Too big; too close to canera. Don't draw it.
                        continue;
                    }

                    int depth = (int)(dist * DEPTH_SCALE);
                    int f_dist = toFixedPoint(dist);
                    int f_renderWidth = toFixedPoint(renderWidth);
                    int renderX2 = Math.min(viewWidth, renderX + renderWidth);
                    for (int x = Math.max(renderX, 0); x < renderX2; x++) {
                        Ray ray = rays[viewWidth - x - 1];
                        if (f_dist < ray.f_dist) {
                            int sliver = div(toFixedPoint(x - renderX), f_renderWidth);
                            drawTextureSliver(texture, false, sliver, depth, x, renderY, renderHeight);
                        }
                    }
                }
            }
        }
    }
    
    private void drawTextureSliver(SoftTexture srcTexture, boolean srcOpaque, int sliver, int depth, int dstX, int dstY, int dstHeight) {
        
        // Mip-mapping. Use half-size textures if available
        while (dstHeight < srcTexture.getHeight() && srcTexture.hasHalfSizeTexture()) {
            srcTexture = srcTexture.getHalfSizeTexture();
        }
        
        int[] dstData = dstBuffer.getData();
        int dstViewWidth = dstBuffer.getWidth();
        int dstViewHeight = dstBuffer.getHeight();
        int[] srcData = srcTexture.getData();
        int srcViewWidth = srcTexture.getWidth();
        int srcViewHeight = srcTexture.getHeight();
        int srcSizeBits = srcTexture.getSizeBits();
        
        int srcX = toIntFloor(srcViewWidth * sliver);
        int f_y = 0;
        int f_dy = div(toFixedPoint(srcViewHeight), toFixedPoint(dstHeight));
        int renderX = dstX;
        int renderY = dstY;
        int renderHeight = dstHeight;
        if (renderY < 0) {
            f_y = f_dy * -renderY;
            renderHeight += renderY;
            renderY = 0;
        }
        if (renderY + renderHeight > dstViewHeight) {
            renderHeight = dstViewHeight - renderY;
        }
        
        if (renderHeight > 0) {
            int renderY2 = renderY + renderHeight;
            int renderOffset = renderX + renderY * dstViewWidth;
            
            if (srcOpaque && depth <= 256) {
                for (int y = renderY; y < renderY2; y++) {
                    dstData[renderOffset] = srcData[srcX + (toIntFloor(f_y) << srcSizeBits)];
                    renderOffset += dstViewWidth;
                    f_y += f_dy;
                }
            }
            else {
                for (int y = renderY; y < renderY2; y++) {
                    int srcColor = srcData[srcX + (toIntFloor(f_y) << srcSizeBits)];
                    
                    drawPixel(dstData, renderOffset, srcColor, depth);
                    renderOffset += dstViewWidth;
                    f_y += f_dy;
                }
            }
        }
    }
    
    private void drawPixel(int[] dstData, int dstOffset, int srcColor, int depth) {
                    
        int srcA = srcColor >>> 24;
        if (srcA == 0xff && depth <= 256) {
            dstData[dstOffset] = srcColor;
        }
        else if (srcA > 0) {
            int dstColor = dstData[dstOffset];
            int dstR = (dstColor >> 16) & 0xff;
            int dstG = (dstColor >> 8) & 0xff;
            int dstB = (dstColor & 0xff);
            int srcR = (srcColor >> 16) & 0xff;
            int srcG = (srcColor >> 8) & 0xff;
            int srcB = (srcColor & 0xff);
            if (depth > 256) {
                srcR = (srcR << 8) / depth;
                srcG = (srcG << 8) / depth;
                srcB = (srcB << 8) / depth;
            }
            int oneMinusSrcA = 0xff - srcA;

            // For pre-multiplied color
//          dstR = srcR + ((dstR * oneMinusSrcA) >> 8);
//          dstG = srcG + ((dstG * oneMinusSrcA) >> 8);
//          dstB = srcB + ((dstB * oneMinusSrcA) >> 8);

            dstR = (srcA * srcR + dstR * oneMinusSrcA) >> 8;
            dstG = (srcA * srcG + dstG * oneMinusSrcA) >> 8;
            dstB = (srcA * srcB + dstB * oneMinusSrcA) >> 8;

            dstData[dstOffset] = 0xff000000 | (dstR << 16) | (dstG << 8) | dstB;
        }
    }
    
    private List<Entity> getVisibileEntities() {
        float cameraX = toFloat(f_cameraX);
        float cameraY = toFloat(f_cameraY);
        float cosAngle = (float)Math.cos(angleToRadians(cameraAngle));
        float sinAngle = (float)Math.sin(angleToRadians(cameraAngle));
        // Get visible objects
        List<Entity> visibleEntities = new ArrayList<Entity>();
        for (Tile tile : visibleFloors) {
            tile.renderVisible = 0;
            List<Entity> entities = tile.getEntities();
            if (entities != null) {
                for (Entity entity : entities) {
                    if (!(entity instanceof Player)) {
                        float dx = entity.getX() - cameraX;
                        float dy = entity.getY() - cameraY;
                        entity.setDistanceFromCamera(dx * cosAngle - dy * sinAngle);
                        visibleEntities.add(entity);
                    }
                }
            }
        }
        visibleFloors.clear();
        
        // Sort visible entities from back to front
        Collections.sort(visibleEntities);
        
        return visibleEntities;
    }
    
    /**
    For each pixel across, cast a ray from the camera location, looking for x- and y-intersections.
    */
    private List<Entity> raycast() {
        visibleFloors.clear();
        addVisibleFloor(toIntFloor(f_cameraX), toIntFloor(f_cameraY));
        int viewWidth = dstBuffer.getWidth();
        for (int x = 0; x < viewWidth; x++) {
            Ray ray = rays[x];
            ray.reset();
            int angle = (rayAngleTable[x] + cameraAngle) & NUM_DEGREES_MASK;

            // Check for x intersections
            if (angle > ANGLE_0 && angle < ANGLE_180) {
                int f_rayY = floor(f_cameraY);
                int f_rayX = f_cameraX + mul(f_cameraY - f_rayY, f_cotTable[angle]);
                raycast(ray, -1, f_rayX, f_rayY, f_cotTable[angle], -ONE, false);
            }
            else if (angle > ANGLE_180 && angle < ANGLE_360) {
                int f_rayY = ONE + floor(f_cameraY);
                int f_rayX = f_cameraX + mul(f_cameraY - f_rayY, f_cotTable[angle]);
                raycast(ray, 1, f_rayX, f_rayY, -f_cotTable[angle], ONE, false);
            }

            // Check for y intersections
            if (angle > ANGLE_90 && angle < ANGLE_270) {
                int f_rayX = floor(f_cameraX);
                int f_rayY = f_cameraY + mul(f_cameraX - f_rayX, f_tanTable[angle]);
                raycast(ray, -1, f_rayX, f_rayY, -ONE, f_tanTable[angle], true);
            }
            else if (angle < ANGLE_90 || angle > ANGLE_270) {
                int f_rayX = ONE + floor(f_cameraX);
                int f_rayY = f_cameraY + mul(f_cameraX - f_rayX, f_tanTable[angle]);
                raycast(ray, 1, f_rayX, f_rayY, ONE, -f_tanTable[angle], true);
            }
        }
        
        return getVisibileEntities();
    }
    
    /**
    Cast a ray looking for an x- or y-intersection.
    */
    private void raycast(Ray ray, int dir, int f_rayX, int f_rayY, int f_rayDX, int f_rayDY, boolean checkingY) {
        
        final int windowMask = checkingY ? WINDOW_NORTH_SOUTH : WINDOW_WEST_EAST;
        int tileX;
        int tileY;
        int sliver = 0;
        SoftTexture texture = null;
        boolean found = false;
                
        if (checkingY) {
            if (dir == -1) {
                tileX = toIntFloor(f_rayX) - 1;
            }
            else {
                tileX = toIntFloor(f_rayX);
            }
            tileY = toIntFloor(f_rayY);
        }
        else {
            tileX = toIntFloor(f_rayX);
            if (dir == -1) {
                tileY = toIntFloor(f_rayY) - 1;
            }
            else {
                tileY = toIntFloor(f_rayY);
            }
        }
        
        while (true) {
            Tile tile = map.getTileAt(tileX, tileY);
            if (tile == null) {
                break;
            }
            
            if (tile.type == Tile.TYPE_NOTHING) {
                // Skip it
            }
            else if (tile.type == Tile.TYPE_WALL || tile.type == Tile.TYPE_EXIT || tile.type == Tile.TYPE_GENERATOR) {

                if (checkingY) {
                    sliver = fracPart(f_rayY);
                }
                else {
                    sliver = fracPart(f_rayX);
                }
                
                texture = tile.getTexture();
                
                if (checkingY) {
                    Tile sideTile = map.getTileAt(tileX - dir, tileY);
                    if (sideTile != null && sideTile.type == Tile.TYPE_DOOR) {
                        texture = doorSideTexture;
                    }
                }
                else {
                    Tile sideTile = map.getTileAt(tileX, tileY - dir);
                    if (sideTile != null && sideTile.type == Tile.TYPE_DOOR) {
                        texture = doorSideTexture;
                    }
                }

                found = true;
                break;
            }
            else if (tile.type == Tile.TYPE_DOOR) {
                int f_extraX = f_rayDX/2;
                int f_extraY = f_rayDY/2;
                int s = tile.renderState;
                if (checkingY) {
                    sliver = fracPart(f_rayY + f_extraY);
                }
                else {
                    sliver = fracPart(f_rayX + f_extraX);
                }

                if (s <= sliver) {
                    sliver -= s;
                    f_rayX += f_extraX;
                    f_rayY += f_extraY;
                    texture = doorTextures[tile.subtype];
                    found = true;
                    break;
                }
            }
            else if (tile.type == Tile.TYPE_WINDOW && (tile.subtype & windowMask) != 0) {
                int f_extraX = f_rayDX/2;
                int f_extraY = f_rayDY/2;
                if (checkingY) {
                    sliver = fracPart(f_rayY + f_extraY);
                }
                else {
                    sliver = fracPart(f_rayX + f_extraX);
                }

                int d = ONE >> 3;
                if ((((sliver  + d/2) / d) & 1) == 0) {
                    f_rayX += f_extraX;
                    f_rayY += f_extraY;
                    found = true;
                    texture = windowTexture;
                    break;
                }
            }
            else if (tile.type == Tile.TYPE_MOVABLE_WALL) {
                int f_extraX = mul(tile.renderState, f_rayDX);
                int f_extraY = mul(tile.renderState, f_rayDY);
                
                boolean visible;
                if (checkingY) {
                    visible = toIntFloor(f_rayY + f_extraY) == tileY;
                }
                else {
                    visible = toIntFloor(f_rayX + f_extraX) == tileX;
                }

                if (visible) {
                    
                    if (checkingY) {
                        sliver = fracPart(f_rayY + f_extraY);
                    }
                    else {
                        sliver = fracPart(f_rayX + f_extraX);
                    }

                    f_rayX += f_extraX;
                    f_rayY += f_extraY;
                    texture = tile.getTexture();
                    found = true;
                    break;
                }
            }
            
            addVisibleFloor(tileX, tileY);
            
            f_rayX += f_rayDX;
            f_rayY += f_rayDY;
            if (checkingY) {
                tileX += dir;
                tileY = toIntFloor(f_rayY);
            }
            else {
                tileX = toIntFloor(f_rayX);
                tileY += dir;
            }
        }
        
        if (found) {
            long f_cosCameraAngle = f_cosTable[cameraAngle];
            long f_sinCameraAngle = f_sinTable[cameraAngle];
            
            int f_dist = (int)(((f_rayX - f_cameraX) * f_cosCameraAngle - (f_rayY - f_cameraY) * f_sinCameraAngle) >> FRACTION_BITS);
                    
            if (f_dist < ray.f_dist) {
                ray.f_dist = f_dist;
                ray.sliver = sliver;
                ray.texture = texture;      
            }
        }
    }
    
    private void addVisibleFloor(int tileX, int tileY) {
        
        Tile centerTile = map.getTileAt(tileX, tileY);
        
        // If this tile has already been added as a center tile, do nothing.
        if (centerTile.renderVisible == 2) {
            return;
        }
        
        // Mark eight surrounding tiles as visible
        for (int x = tileX - 1; x <= tileX + 1; x++) {
            for (int y = tileY - 1; y <= tileY + 1; y++) {
                Tile tile = map.getTileAt(x, y);
                if (tile != null && tile.renderVisible == 0) {
                    tile.renderVisible = 1;
                    
                    visibleFloors.add(tile);
                }
            }
        }
        
        // Mark this tile as a center tile
        centerTile.renderVisible = 2;
    }
}
