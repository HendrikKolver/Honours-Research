package embedding;


import static embedding.FragileWatermark.getBlockBinary;
import java.awt.Color;
import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import javax.imageio.ImageIO;
import static loadImage.ImageHolder.encodeGray;
import loadImage.MyImage;

/**
 *
 * @author Hendrik
 */
public class SelfEmbed {
    private static int cornerRow =0;
    private static int cornerCol=0;
    public static int imageWidth = 0;
    //0.636
    public static final double embeddingRate = 0.65;
    
    public static MyImage selfEmbed(MyImage image) throws IOException, NoSuchAlgorithmException{
        ArrayList<Block> blockList = getBlocks(image);
        int suitableBlockCount = 0;
        int lsbPlane = 0;
        for(Block block : blockList){
            if(block.getComplexity()>embeddingRate && block.lsbLayer !=7){
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
        Color[][] reAssembleImageColours = reAssembleImage(embeddedImageList,"finalImage.bmp");
        //Color[][] imageColors = FragileWatermark.embedWaterMark(reAssembleImageColours);
        //saveImageFromColor(imageColors,"finalImage.bmp");
        
        return null;
    }
    
    public static ArrayList<Block> shuffelList(ArrayList<Block>  blockList){
        long seed = System.nanoTime();
        Collections.shuffle(blockList, new Random(seed));
        return blockList;
    }
    
    public static String getStringForLocationMap(int[][][] locationMap){
        String locationMapString = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                locationMapString  += locationMap[0][i][j];
                locationMapString  += locationMap[1][i][j];
                locationMapString  += locationMap[2][i][j];
            }
        }
        return locationMapString;
    }
    
    public static WatermarkHelper getNewValuePair(int num1, int num2, int currentLocation, String embeddingString){
        int changableBitCount = getChangableBitsCount(num1,num2);
        int h = getDifference(num1,num2);

        String originalDifString = getBinary(h);
        int originalHBinaryLength = originalDifString.length();
        String notChangablebits = originalDifString.substring(0,originalDifString.length()-changableBitCount);
        originalDifString= notChangablebits;
        for (int k = 0; k < originalHBinaryLength-notChangablebits.length(); k++) {
            if(currentLocation>= embeddingString.length()){
                originalDifString+=0;
            }else{
                originalDifString += embeddingString.charAt(currentLocation);
            }
           currentLocation++;
        }

        int newH = getIntFromBinary(originalDifString);

        return calculateValuesUsingNewDifference(num1,num2,newH);
    }
    
    public static ArrayList<Block> embedMessage(ArrayList<Block> imageBlocks,ArrayList<Block> messageBlocks) throws NoSuchAlgorithmException, IOException{
        //TODO
        //remember the message is not in grey code
        int messageBlockCount =0;
        for (int i = 0; i < imageBlocks.size(); i++) {
            if(imageBlocks.get(i).getComplexity()>embeddingRate  && imageBlocks.get(i).lsbLayer !=7){
                Block imageBlock = imageBlocks.get(i);
                Block messageBlock = messageBlocks.get(messageBlockCount);
                if(messageBlock.getComplexity()<embeddingRate){
                    //TODO should conjugate here and save record of this in conjugation map
                    //System.out.println("messageBlock binary before: "+getBlockBinary(messageBlock));
                    messageBlock = conjugateBlock(messageBlock);
                    //System.out.println("messageBlock binary after: "+getBlockBinary(messageBlock));
                    messageBlock.conjugated = true;
                    messageBlock.calculateComplexity();
                }
               
                if(messageBlock.getComplexity()<embeddingRate){
                    //System.out.println("dammit");
                }
                
                messageBlock.lsbLayer = imageBlock.lsbLayer;
                imageBlocks.set(i, messageBlock);
                //System.out.println(FragileWatermark.getBlockBinary(imageBlocks.get(i)));
                messageBlockCount++;
            }
            if(messageBlockCount==messageBlocks.size())
                break;
        }
        //embed the fragile watermark
        FragileWatermark.embedWaterMark(imageBlocks);
        return imageBlocks;
    }
         
    
    public static Color[][] reAssembleImage(ArrayList<Block> blocks, String name) throws IOException{
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
        saveImageFromColor(imageColors,name,512);
        return imageColors;
    }
    
    public static void saveImageFromColor(Color [][] imageColorGrid,String name, int imageWidth) throws IOException{
        BufferedImage saveImage = new BufferedImage(imageWidth,imageWidth,BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageWidth; j++) {
                saveImage.setRGB(i, j, imageColorGrid[i][j].getRGB());
            }
        }
        boolean success = ImageIO.write(saveImage, "BMP", new File(name));
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
            for (int j = 0; j < block.getBlockSize(); j++) {
                if(blockValues[0][i][j] == compareValue)
                  blockValues[0][i][j] = '1';
                else
                  blockValues[0][i][j] = '0';
                
                 if(compareValue == '0')
                    compareValue = '1';
                else
                    compareValue = '0';  
                
                if(blockValues[1][i][j] == compareValue)
                  blockValues[1][i][j] = '1';
                else
                  blockValues[1][i][j] = '0';
                
                 if(compareValue == '0')
                    compareValue = '1';
                else
                    compareValue = '0';  
                
                if(blockValues[2][i][j] == compareValue)
                  blockValues[2][i][j] = '1';
                else
                  blockValues[2][i][j] = '0';
                
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
    
    public static boolean isExpandable(int h, int l){
        if(h != 0){
            double val1 = (Math.pow(2, ((Math.log(h) / Math.log(2)))+2))-1;
            double val2 = Math.min((2*(255-l)),((2*l)+1));
            if(val1<=val2)
                return true;
        }
        return false;
    }
     
    public static WatermarkHelper embedFragileWatermark(int num1, int num2, char binaryBit){
        //l = avarage, h=difference
        
        
        boolean num1Larger = false;
        int l = (num1+num2)/2;
        int h;
        if(num1>num2){
            num1Larger=true;
            h =num1-num2;
        }
        else
            h=num2-num1;
        
        if(!isExpandable(h,l))
        {
            //System.out.println("bb["+num1+","+num2+"]");
            WatermarkHelper helper = new WatermarkHelper();
            helper.num1 = num1;
            helper.num2 = num2;
            helper.isEmbedded = false;
            return helper;
        }
        
        //System.out.println("Original bit: "+ binaryBit);
        //System.out.println("bb["+num1+","+num2+"]");
        String differenceBinary = getBinary(h);
        differenceBinary = differenceBinary.substring(0, 1) + binaryBit + differenceBinary.substring(1, differenceBinary.length());
        int hAfter = Integer.parseInt(differenceBinary, 2);
        
        int num1After=0;
        int num2After=0;
        if(num1Larger){
            num1After = l+((hAfter+1)/2);
            num2After = num1After-hAfter;
        }
        else{
            num2After = l+((hAfter+1)/2);
            num1After = num2After-hAfter;
        }
        
        //System.out.println("af["+num1After+","+num2After+"]");
        
        int testL =(num1After+num2After)/2;
        int testH;
        if(num1After>num2After){
            num1Larger = true;
            testH =num1After-num2After;
        }
        else{
            num1Larger = false;
            testH=num2After-num1After;
        }
        
        String differenceBinaryTest = getBinary(testH);
        String embeddedBitAfter="";
        int checkHBeforeBitExtracted= Integer.parseInt(differenceBinaryTest, 2);
        if(differenceBinaryTest.length()==2){
            embeddedBitAfter += differenceBinaryTest.charAt(1);
            differenceBinaryTest = ""+differenceBinaryTest.charAt(0);
            
        }else if(differenceBinaryTest.length()>2){
            embeddedBitAfter += differenceBinaryTest.charAt(1);
            differenceBinaryTest = differenceBinaryTest.substring(0, 1)+ differenceBinaryTest.substring(2, differenceBinaryTest.length());
        }else
        {
          embeddedBitAfter += differenceBinaryTest;  
        }
        
        int hAfterTest = Integer.parseInt(differenceBinaryTest, 2);
        
        int num1AfterTest=0;
        int num2AfterTest=0;
        if(num1Larger){
            num1AfterTest = testL+((hAfterTest+1)/2);
            num2AfterTest = num1AfterTest-hAfterTest;
        }
        else if(checkHBeforeBitExtracted==1){
            num2AfterTest = testL+((hAfterTest+1)/2);
            num1AfterTest = num2AfterTest-hAfterTest;
            num2AfterTest = num1AfterTest;
        }else{
            num2AfterTest = testL+((hAfterTest+1)/2);
            num1AfterTest = num2AfterTest-hAfterTest;
        }
        
        //if(num2AfterTest!= num2 || num1AfterTest!=num1 || !embeddedBitAfter.contains(binaryBit+""))
            //System.out.println("false");
        
         //System.out.println("af2["+num1AfterTest+","+num2AfterTest+"]");
        // System.out.println("Extracted bit: "+ embeddedBitAfter);

        WatermarkHelper helper = new WatermarkHelper();
        helper.num1 = num1After;
        helper.num2 = num2After;
        helper.isEmbedded = true;
        return helper;

    }
    
    public static String getBinary(int val)
    {
        String binaryValue =Integer.toBinaryString(val);
        return binaryValue;
    }
    
    
    
    public static int getChangableBitsCount(int num1, int num2){
        int h = 0;
        int l = ((num1+num2)/2);
        int maxH = 0;
        
        
        
        if (num1>num2){
            h= num1-num2;
        }else{
            h= num2-num1;
        }
        
        if(h == 0 || h == 1)
            return 0;
        
        
        int count = 0;
        boolean maxCount = false;
        String maxHBinary = getBinary(h);
        int currentLocation = maxHBinary.length();
        if(l>=128){
            while(maxH<=(2*(255-l))){
                if(currentLocation == 1 || currentLocation == 0){
                    maxCount = true;
                    break;
                }
                if(currentLocation == maxHBinary.length())
                    maxHBinary = maxHBinary.substring(0,currentLocation-1)+1;
                else
                    maxHBinary = maxHBinary.substring(0,currentLocation-1)+1+maxHBinary.substring(currentLocation);
                currentLocation--;
                count++;
                maxH=getIntFromBinary(maxHBinary);
            }
            if(!maxCount)
                count--;
            else
                count =getBinary(h).length()-1;
            
        }else{
           while(maxH<=(2*l)+1){
            if(currentLocation == 1 || currentLocation == 0){
                 maxCount = true;
                 break;
            }
            if(currentLocation == maxHBinary.length())
                maxHBinary = maxHBinary.substring(0,currentLocation-1)+1;
            else
                maxHBinary = maxHBinary.substring(0,currentLocation-1)+1+maxHBinary.substring(currentLocation);
                currentLocation--;
                count++;
                maxH=getIntFromBinary(maxHBinary);
            }
            if(!maxCount)
                count--;
            else
                count =getBinary(h).length()-1;
        }

        return count;
      
    }
    
    public static String getChangableBits(int num1, int num2){
        //h = 0 and thus there are no changable bits
        if(num1 == num2)
            return "";
        
        int h = 0;
        
        if (num1>num2){
            h= num1-num2;
        }else{
            h= num2-num1;
        }
        
        int changableBitsLength = getChangableBitsCount(num1,num2);
        
        String changableBits = getBinary(h);
        //System.out.println("Original: "+changableBits);
        changableBits = changableBits.substring(changableBits.length()-changableBitsLength, changableBits.length());
        //System.out.println("changableBits: "+changableBits);
        
        return changableBits;
    }
    
    public static WatermarkHelper calculateValuesUsingNewDifference(int num1, int num2, int newH){
        boolean num1Larger = false;
        int l = (num1+num2)/2;
        if(num1>num2){
            num1Larger=true;
        }
        
         int num1After=0;
        int num2After=0;
        if(num1Larger){
            num1After = l+((newH+1)/2);
            num2After = num1After-newH;
        }
        else{
            num2After = l+((newH+1)/2);
            num1After = num2After-newH;
        }
       // System.out.println("["+num1After+"]["+num2After+"]");
        
        WatermarkHelper helper = new WatermarkHelper();
        helper.num1 = num1After;
        helper.num2 = num2After;
        return helper;
    }
    
    public static int getDifference(int num1, int num2){
        if (num1>num2){
            return num1-num2;
        }else{
            return num2-num1;
        }
    }
    
    public static int getIntFromBinary(String binary){
        return Integer.parseInt(binary, 2);
    }
    
    
    
}
