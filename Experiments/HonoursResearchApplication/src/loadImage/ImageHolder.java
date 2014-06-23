package loadImage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Hendrik
 */
public class ImageHolder {
    private Image image;

    public ImageHolder() {
        image = null;
    }
    
    public void extractImageData(String path) throws IOException{
        //extract the normal image colours
        File img = new File(path);
        BufferedImage bufferedImage = ImageIO.read(img);
        Color colorArray[][] = new Color[bufferedImage.getHeight()][bufferedImage.getWidth()];
        
        char[][][][] bitPlanesPixels = new char[8][3][bufferedImage.getHeight()][bufferedImage.getWidth()];
       
        for (int i = 0; i < bufferedImage.getHeight(); i++) {
            for (int j = 0; j < bufferedImage.getWidth(); j++) {
                colorArray[i][j] = (new Color(bufferedImage.getRGB(i, j)));
                
                //Red colour planes
                //Get binary representaion in grey coding
                String colourBinary = getBinary(encodeGray(colorArray[i][j].getRed()));
                for (int k = 0; k < 8; k++) {
                    bitPlanesPixels[k][0][i][j] = colourBinary.charAt(k);  
                }
                
                //Green colour planes
                //Get binary representaion in grey coding
                colourBinary = getBinary(encodeGray(colorArray[i][j].getGreen()));
                for (int k = 0; k < 8; k++) {
                    bitPlanesPixels[k][1][i][j] = colourBinary.charAt(k);  
                }
                
                //Blue colour planes
                //Get binary representaion in grey coding
                colourBinary = getBinary(encodeGray(colorArray[i][j].getBlue()));
                for (int k = 0; k < 8; k++) {
                    bitPlanesPixels[k][2][i][j] = colourBinary.charAt(k);
                }
            }
        }
       
        image = new Image();
        image.setImagePixels(colorArray);
        image.setBitPlanePixels(bitPlanesPixels);
    }
    
    
    
    public Image getImage() {
        return image;
    }
    
    public String getBinary(int val)
    {
        String binaryValue =Integer.toBinaryString(val);
        binaryValue = padBinaryZero(binaryValue);
        return binaryValue;
    }
    
    public String padBinaryZero(String val)
    {
        if (val.length() ==8)
            return val;
        else{
            val = "0"+val;
            return padBinaryZero(val);
        }
    }
    
    public static int encodeGray(int natural) {
        return natural ^ natural >>> 1;
    }
    
     public static int decodeGray(int gray) {
        int natural = 0;
        while (gray != 0) {
            natural ^= gray;
            gray >>>= 1;
        }
        return natural;
    }


    
    
}
