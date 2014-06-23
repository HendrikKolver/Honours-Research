package loadImage;

import java.awt.Color;

/**
 *
 * @author Hendrik
 */
public class Image {
    private Color[][] imagePixels;
    //[bit planes 0-7][RGB = 0-1-2][Row]{Col]
    private char[][][][] bitPlanePixels;

    public Color[][] getImagePixels() {
        return imagePixels;
    }

    public void setImagePixels(Color[][] imagePixels) {
        this.imagePixels = imagePixels;
    }

    public char[][][][] getBitPlanePixels() {
        return bitPlanePixels;
    }

    public void setBitPlanePixels(char[][][][] bitPlanePixels) {
        this.bitPlanePixels = bitPlanePixels;
    }

}
