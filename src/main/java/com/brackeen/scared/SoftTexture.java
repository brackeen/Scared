package com.brackeen.scared;

import com.brackeen.app.App;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

/**
A software texture for software rendering. Stored in normal RAM, instead of video ram.
*/
public class SoftTexture {
    
    private static int[] getImageData(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = null;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            // Common case - use the existing array
            data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            if (data.length != w * h) {
                data = null;
            }
        }
        if (data == null) {
            // Copies the data to a new array
            data = image.getRGB(0, 0, w, h, null, 0, w);
        }
        return data;
    }
    
    private static boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }
    
    private static int log2(int n) {
        int count = 0;
        while (true) {
            n >>= 1;
            if (n == 0) {
                return count;
            }
            count++;
        }
    }
    
    private final int width;
    private final int height;
    private final int sizeBits;
    private final int[] data;
    private SoftTexture halfSizeTexture; // For mip-mapping
    
    public SoftTexture(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new int[width * height];
        if (isPowerOfTwo(width) && width == height) {
            sizeBits = log2(width);
        }
        else {
            sizeBits = -1;
        }
    }
    
    public SoftTexture(String imageName) {
        this(App.getApp().getImage(imageName));
    }
    
    public SoftTexture(BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.data = getImageData(image);
        if (isPowerOfTwo(width) && width == height) {
            sizeBits = log2(width);
        }
        else {
            sizeBits = -1;
        }
    }
    
    public boolean hasHalfSizeTexture() {
        return halfSizeTexture != null;
    }

    public SoftTexture getHalfSizeTexture() {
        return halfSizeTexture;
    }

    public void setHalfSizeTexture(SoftTexture halfSizeTexture) {
        this.halfSizeTexture = halfSizeTexture;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    public boolean isPowerOfTwo() {
        return sizeBits >= 0;
    }
    
    public int getSizeBits() {
        return sizeBits;
    }

    public int[] getData() {
        return data;
    }
    
    public BufferedImage getBufferedImageView() {
        DirectColorModel colorModel = new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, width, height, new int[] { 0xff0000, 0x00ff00, 0x0000ff });
        DataBuffer dataBuffer = new DataBufferInt(data, width * height);
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        return new BufferedImage(colorModel, raster, true, null);
    }
        
    /**
        Draws the specified texture (source) onto this texture (dest).
    */
    public void draw(SoftTexture src, int x, int y, boolean srcOpaque) {
        draw(src, x, y, 0, 0, src.width, src.height, srcOpaque);
    }
        
        
    /**
        Draws the specified texture (source) onto this texture (dest).
    */
    public void draw(SoftTexture src, int x, int y, int srcX, int srcY, int srcWidth, int srcHeight, boolean srcOpaque) {
        
        SoftTexture dest = this;
        int destX = x;
        int destY = y;
        
        // Clip to dest
        if (destX < 0) {
            srcX = -destX;
            srcWidth += destX;
            destX = 0;
        }
        if (destY < 0) {
            srcY = -destY;
            srcHeight += destY;
            destY = 0;
        }
        if (destX + srcWidth > dest.width) {
            srcWidth = dest.width - destX;
        }
        if (destY + srcHeight > dest.height) {
            srcHeight = dest.height - destY;
        }
        
        if (srcWidth <= 0 || srcHeight <= 0) {
            return;
        }
        
        // Draw
        int[] srcData = src.data;
        int[] destData = dest.data;
        int srcOffset = srcX + srcY * src.width;
        int destOffset = destX + destY * dest.width;
        
        for (int i = 0; i < srcHeight; i++) {
            
            if (srcOpaque) {
                System.arraycopy(srcData, srcOffset, destData, destOffset, srcWidth);
            }
            else {
                for (int j = 0; j < srcWidth; j++) {
                    int color = srcData[srcOffset + j];

                    if (color != 0) {
                        destData[destOffset + j] = color;
                    }
                }

            }
            
            srcOffset += src.width;
            destOffset += dest.width;
        }
    }
}
