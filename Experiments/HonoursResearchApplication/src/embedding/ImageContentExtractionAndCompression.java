package embedding;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import loadImage.MyImage;

/**
 *
 * @author Hendrik
 */
public class ImageContentExtractionAndCompression {
    public static ArrayList<Block> getCompressedImageContent(MyImage image, double embeddingCapacity) throws IOException{
        System.out.println(embeddingCapacity );
        /*24 = bit per pixel
        *18 = the two position integers that get embedded
        *7 the two position integers only get embedded once for every 7 pixels
        */
        double newPixelAmount = embeddingCapacity/(24.0);
        double percentageLoss = (newPixelAmount/(512*512)*100);
        System.out.println("Size of embedded image: "+percentageLoss);
        int newWidth = (int) Math.sqrt(newPixelAmount);
        
        System.out.println("New Width: "+ newWidth);
        SelfEmbed.imageWidth = newWidth;
        
        BufferedImage bufImage = new BufferedImage(512, 512,
                    BufferedImage.TYPE_INT_RGB);
        
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                bufImage.setRGB(i, j, image.getImagePixels()[i][j].getRGB());
                
            }    
        }

        BufferedImage scaledBuf = getScaledImage(bufImage,newWidth,newWidth);
        int perBlockCheck = 8;
        String binary = "";
        ArrayList<Block> blockList = new ArrayList<>();
        int rowIndex = 0;
        int colIndex = 0;
        for (int i = 0; i < scaledBuf.getHeight(); i++) {
            for (int j = 0; j < scaledBuf.getWidth(); j++) {
                //7 is used because that is the amount of pixels that will be stored per block
                if(perBlockCheck==8)
                {
                    //System.out.println("binarysize:" + binary.length());
                    if(perBlockCheck!=0 && !binary.equals("")){
//                        System.out.println("binary: "+ binary);
//                        System.out.println("ImediateBinary: "+ FragileWatermark.getBlockBinary(makeBlockRGBTogether(binary)));
                        Block tmpBlock = makeBlockRGBTogether(binary);
                        //setting the index of the embedded pixels that will be added to the embedding map in the lsb layer
                        tmpBlock.row = rowIndex;
                        tmpBlock.col = colIndex;
                        tmpBlock.calculateComplexity();
                        blockList.add(tmpBlock);
                        
                    }
                    rowIndex = i;
                    colIndex = j;
                    perBlockCheck = 0;
                    binary = "";
                    
                }
                    
                Color pixelColor = new Color(scaledBuf.getRGB(i, j));

                binary+=(getBinary(pixelColor.getRed(),8));
                binary+=(getBinary(pixelColor.getGreen(),8));
                binary+=(getBinary(pixelColor.getBlue(),8));
                perBlockCheck++;
                    
                    
            }
   
        }
        scaledBuf = getScaledImage(scaledBuf,512,512);
        ImageIO.write(scaledBuf, "BMP", new File("scaledBlockNew.bmp"));
        
        return blockList;

    }
    
    public static Block makeBlockRGBTogether(String binary){
        char[][][] grid = new char[3][8][8];
        int binaryCount = 0;
        Block block = new Block();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(binaryCount>=binary.length()){
                    grid[0][i][j] = '0'; 
                }else{
                    grid[0][i][j] = binary.charAt(binaryCount);
                    binaryCount++;
                }
                if(binaryCount>=binary.length()){
                    grid[1][i][j] = '0'; 
                }else{
                    grid[1][i][j] = binary.charAt(binaryCount);
                    binaryCount++;
                }
                if(binaryCount>=binary.length()){
                    grid[2][i][j] = '0'; 
                }else{
                    grid[2][i][j] = binary.charAt(binaryCount);
                    binaryCount++;
                }
                
            }
        }
        block.setBlock(grid);
        return block;
    }
    
    
    //do RGB seperately
    public static Block makeBlock(String binary){
        Block block = new Block();
        int count = 0;
        int rgbCount = 0;
        int rgbNum=0;
        char[][][] grid = new char[3][8][8];
        //redLayer
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(count<binary.length())
                {
                    if(rgbCount>=64){
                        rgbNum++;
                        rgbCount=0;
                    }
                    grid[rgbNum][i][j] = binary.charAt(count);
                    rgbCount++;
                    count++;
                }else{
                    break;
                }
            }
            if(count>=binary.length())
                break;
        }
        
        //greenLayer
        for (int i = 0; i < block.getBlockSize(); i++) {
            for (int j = 0; j < block.getBlockSize(); j++) {
                if(count<binary.length())
                {
                    if(rgbCount>=64){
                        rgbNum++;
                        rgbCount=0;
                    }
                    grid[rgbNum][i][j] = binary.charAt(count);
                    rgbCount++;
                    count++;
                }else{
                    break;
                }
            }
            if(count>=binary.length())
                break;
        }
        
        //blueLayer
        for (int i = 0; i < block.getBlockSize(); i++) {
            for (int j = 0; j < block.getBlockSize(); j++) {
                if(count<binary.length())
                {
                    if(rgbCount>=64){
                        rgbNum++;
                        rgbCount=0;
                    }
                    grid[rgbNum][i][j] = binary.charAt(count);
                    rgbCount++;
                    count++;
                }else{
                   grid[rgbNum][i][j] = '0'; 
                }
            }
            if(count>=binary.length())
                break;
        }
        //System.out.println(rgbCount);
        
        block.setBlock(grid);
        block.message = true;
        
        block.calculateComplexity();
        return block;
    }
    
    public static BufferedImage getScaledImage(BufferedImage image, int width, int height) throws IOException {
        int imageWidth  = image.getWidth();
        int imageHeight = image.getHeight();

        double scaleX = (double)width/imageWidth;
        double scaleY = (double)height/imageHeight;
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        return bilinearScaleOp.filter(
            image,
            new BufferedImage(width, height, image.getType()));
    }
    
    public static String getBinary(int val, int padTo)
    {
        String binaryValue =Integer.toBinaryString(val);
        binaryValue = padBinaryZero(binaryValue,padTo);
        return binaryValue;
    }
    
    public static String padBinaryZero(String val, int padTo)
    {
        if (val.length() ==padTo)
            return val;
        else{
            val = "0"+val;
            return padBinaryZero(val,padTo);
        }
    }
    
}
