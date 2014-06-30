package embedding;

import java.awt.Color;
import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import loadImage.MyImage;
import static loadImage.ImageHolder.encodeGray;

/**
 *
 * @author Hendrik
 */
public class SelfEmbed {
    private static int cornerRow =0;
    private static int cornerCol=0;
    //0.636
    private static final double embeddingRate = 0.65;
    
    public static MyImage selfEmbed(MyImage image) throws IOException, NoSuchAlgorithmException{
        ArrayList<Block> blockList = getBlocks(image);
        int suitableBlockCount = 0;
        int lsbPlane = 0;
        for(Block block : blockList){
            if(block.getComplexity()>embeddingRate){
                suitableBlockCount++;
            }
        }
        
        System.out.println("There are "+suitableBlockCount+ " complex blocks out of: "+ blockList.size() + " blocks");
         
        double embeddingCapacity = 0;
        //actual capacity
        //embeddingCapacity = ((suitableBlockCount *((8*8)-1))*3);
        //capacity avaliable with my block wise embedding fasion
        embeddingCapacity = (suitableBlockCount *186);
        System.out.println("Available embedding capacity (bits): "+embeddingCapacity );
        ArrayList<Block> messageBlocks = ImageContentExtractionAndCompression.getCompressedImageContent(image, embeddingCapacity);
        ArrayList<Block> embeddedImageList = embedMessage(blockList,messageBlocks);
        embeddedImageList = embedWaterMark(embeddedImageList);
        reAssembleImage(embeddedImageList);
        return null;
    }
    
    public static ArrayList<Block> embedWaterMark(ArrayList<Block> blockList) throws NoSuchAlgorithmException, IOException{
        ArrayList<Block> tmpBlockList = new ArrayList<>();
        String blockBinaryHash;
        for(Block block: blockList)
        {
            System.out.println(block.lsbLayer);
            if(block.lsbLayer <7)
                tmpBlockList.add(block);
            else{
                //Embed the hash into this block
                blockBinaryHash = ShaHashHelper.getBlockHash(tmpBlockList);
                System.out.println("blockBinaryHash: "+ blockBinaryHash);
                blockBinaryHash = null;
                tmpBlockList = new ArrayList<>(); 
            }
        }
        return blockList;
    }
    
    public static ArrayList<Block> embedMessage(ArrayList<Block> imageBlocks,ArrayList<Block> messageBlocks){
        //TODO
        //remember the message is not in grey code
        int messageBlockCount =0;
        for (int i = 0; i < imageBlocks.size(); i++) {
            if(imageBlocks.get(i).getComplexity()>embeddingRate){
                Block imageBlock = imageBlocks.get(i);
                Block messageBlock = messageBlocks.get(messageBlockCount);
                messageBlock.lsbLayer = imageBlock.lsbLayer;
                imageBlocks.set(i, messageBlock);
                messageBlockCount++;
            }
            if(messageBlockCount==messageBlocks.size())
                break;
        }
        return imageBlocks;
    }
         
    
    public static void reAssembleImage(ArrayList<Block> blocks) throws IOException{
        int blockLayerCount = 0;
        int row = 0;
        int col = 0;
        int rowCorner=0;
        int colCorner=0;
        int bitsExtracted = 0;
        Color[][] imageColors = new Color[512][512];
        String[][][] PixelValue = new String[3][8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PixelValue[0][i][j] = "";
                PixelValue[1][i][j] = "";
                PixelValue[2][i][j] = "";
            } 
        }
        
        for(Block block : blocks){
            if(block.getComplexity()<embeddingRate && block.message)
                block = conjugateBlock(block);
            
            char[][][] blockContent = block.getBlock();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    
                    PixelValue[0][i][j] += blockContent[0][i][j];
                    PixelValue[1][i][j] += blockContent[1][i][j];
                    PixelValue[2][i][j] += blockContent[2][i][j];
                }
            } 
            blockLayerCount++; 
            
            if(blockLayerCount>=8)
            {
                blockLayerCount=0;
                col = colCorner;
                row = rowCorner;
                
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        int red;
                        int green;
                        int blue;
                        red = decodeGray(Integer.parseInt(PixelValue[0][i][j], 2));
                        green = decodeGray(Integer.parseInt(PixelValue[1][i][j], 2));
                        blue = decodeGray(Integer.parseInt(PixelValue[2][i][j], 2));

                        Color pixelColor = new Color(red,green,blue);
                        imageColors[row][col] = pixelColor;
                        
                        col++;
                        if(col >= colCorner+8){
                            col = colCorner;
                            row++;
                        }
                        PixelValue[0][i][j] = "";
                        PixelValue[1][i][j] = "";
                        PixelValue[2][i][j] = "";
                    } 
                }
                colCorner+=8;
                if(colCorner >=512){
                    colCorner =0;
                    rowCorner+=8;
                }
                
            }
        }
        
        BufferedImage saveImage = new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                saveImage.setRGB(i, j, imageColors[i][j].getRGB());
            }
        }
        boolean success = ImageIO.write(saveImage, "BMP", new File("reAssembledAndSavedImage.bmp"));
    } 
    
    public static ArrayList<Block> getBlocks(MyImage image)
    {
        
        ArrayList<Block> blockList = new ArrayList<>();
        while(cornerRow <512 && cornerCol <512){
            int row=0;
            int col=0;
            
            ArrayList<Block> tempBlocks = new ArrayList<>();
            for (int k = 0; k < 8; k++) { 
                Block tmpLayerBlock = new Block();
                tmpLayerBlock.lsbLayer = k;
                tempBlocks.add(tmpLayerBlock);
            }
            
            for (int i = cornerRow; i < cornerRow+8; i++) {
                for (int j = cornerCol; j < cornerCol+8; j++){
                    
                    for (int k = 0; k < 8; k++) { 
                        Block blockObject = tempBlocks.get(k);
                        char[][][] blockContent = blockObject.getBlock();
                        blockContent[0][row][col] = image.getBitPlanePixels()[k][0][i][j];  
                        blockContent[1][row][col] = image.getBitPlanePixels()[k][1][i][j];   
                        blockContent[2][row][col] = image.getBitPlanePixels()[k][2][i][j]; 
                        blockObject.setBlock(blockContent);
                        tempBlocks.set(k, blockObject);
                    }
                    col++;
                }
                col =0;
                row++;  
            }
            
            for (int k = 0; k < 8; k++) { 
                tempBlocks.get(k).calculateComplexity();               
                blockList.add(tempBlocks.get(k));
            }
            
            if(cornerCol+8 >=512)
            {
                cornerCol = 0;
                cornerRow +=8;
            }else{
                cornerCol+=8;
            }
            
        }
        cornerRow = 0;
        cornerCol =0;
        return blockList;
    }
    
    public static String getBinary(String value)
    {
       
        byte[] bytes = value.getBytes();
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
            binary.append(' ');
        }
        
        return binary.toString();
    }
    
    public static Block conjugateBlock(Block block)
    {
        char compareValue= '0';
        char [][][] blockValues = block.getBlock();
        for (int i = 0; i < block.getBlockSize(); i++) {
            if(i == 0 | i%2==0){
               compareValue='1'; 
            }
            else{
               compareValue='0'; 
            }
            for (int j = 0; j < block.getBlockSize(); j++) {
                if(blockValues[0][i][j] == compareValue)
                  blockValues[0][i][j] = '0';
                else
                  blockValues[0][i][j] = '1';
                
                if(compareValue == '0')
                    compareValue = '1';
                else
                    compareValue = '0';   
            }
        }
        block.setBlock(blockValues);
        return block;
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
