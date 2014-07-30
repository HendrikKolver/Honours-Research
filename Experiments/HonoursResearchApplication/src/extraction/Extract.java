package extraction;

import embedding.Block;
import static embedding.FragileWatermark.getBlockBinary;
import static embedding.ImageContentExtractionAndCompression.getScaledImage;
import static embedding.ImageContentExtractionAndCompression.scaleIntToRange;
import embedding.SelfEmbed;
import static embedding.SelfEmbed.*;
import static embedding.SelfEmbed.getBinary;
import embedding.ShaHashHelper;
import embedding.WatermarkHelper;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import loadImage.ImageHolder;
import loadImage.MyImage;

/**
 *
 * @author Hendrik
 */
public class Extract {
    public static void ExtractImage(String path) throws IOException, NoSuchAlgorithmException{
        ImageHolder holder = new ImageHolder();
        holder.extractImageData(path);
        
        //blocks of image
        ArrayList<Block> blocks = SelfEmbed.getBlocks(holder.getImage());
        extractImageContent(blocks);
        
    }
    
    public static void extractImageContent(ArrayList<Block> blocks) throws FileNotFoundException, NoSuchAlgorithmException, IOException{
        String blockBinary = "";
        for(int x=0 ; x<blocks.size();x++){
            Block block = blocks.get(x);
            if(block.lsbLayer!=7){
                blockBinary+=getBlockBinary(block);
            }
            else{
                //192 = 8x8x3
                String hashString = ShaHashHelper.getBlockHash(blockBinary,59);
                
                String extractedHash = getBlockBinary(block);
                extractedHash = extractedHash.substring(0,extractedHash.length()-133);
                
                String embeddingMap = getBlockBinary(block);
                embeddingMap = embeddingMap.substring(embeddingMap.length()-133,embeddingMap.length()-7);
                
                int[][] embeddingMapIntegers = new int[7][2];
                String indexNumber = embeddingMap;
                int blockStartIndex = x-7;
                for (int i = 0; i < 7; i++) {
                    String numberRow = indexNumber.substring(0,9);
                    embeddingMapIntegers[i][0] = getIntFromBinary(numberRow);
                    indexNumber = indexNumber.substring(9,indexNumber.length());
                    
                    String numberCol = indexNumber.substring(0,9);
                    embeddingMapIntegers[i][1] = getIntFromBinary(numberCol);
                    indexNumber = indexNumber.substring(9,indexNumber.length());
                    
                    Block tempBlock = blocks.get(blockStartIndex+i);
                    tempBlock.row = embeddingMapIntegers[i][0];
                    tempBlock.col = embeddingMapIntegers[i][1];
                    
                    blocks.set(blockStartIndex+i, tempBlock);
                }
                
                String blockConjugationMap = getBlockBinary(block);
                blockConjugationMap = blockConjugationMap.substring(blockConjugationMap.length()-7,blockConjugationMap.length());
                if(!hashString.equals(extractedHash)){
                    block.authentic = false;
                    for (int i = 1; i <= 7; i++) {
                        Block tempBlock = blocks.get(x-i);
                        tempBlock.authentic = false;
                        blocks.set(x-i, tempBlock);
                        
                    }
                    System.out.println("Tampered");
                }else{
                    //block is authentic check if there was conjugation
                    int charCount = 6;
                    
                    for (int i = 1; i <= 7; i++) {
                        
                        if(blockConjugationMap.charAt(charCount)=='1'){
                            //block has been conjugated
                            Block tempBlock = blocks.get(x-i);
                            tempBlock = SelfEmbed.conjugateBlock(tempBlock);
                            tempBlock.isComplex = true;
                            blocks.set(x-i,tempBlock);
                            
                        }
                        charCount--;
                    }
                   
                }
                blockBinary="";
            }
            block.calculateComplexity();
        }
       Color[][] reconColor = new Color[SelfEmbed.imageWidth][SelfEmbed.imageWidth];
       boolean nullAlreadyUsed = false;
        for(Block block: blocks){
            if((block.getComplexity()>SelfEmbed.embeddingRate && block.authentic && block.lsbLayer!=7) || block.isComplex){
                String blockBinaryString = getBlockBinary(block);
                    
                
                    //System.out.println(blockBinaryString);
                    int indexRow = block.row;
                    int indexCol = block.col;
                    if(indexRow == 0 && indexCol == 0 && nullAlreadyUsed){
                        //not embedded block simply left over
                    }else
                    {
   
                        if(indexRow==0 && indexCol==0)
                            nullAlreadyUsed = true;
                        
                        int currentRow = indexRow;
                        int currentCol = indexCol;
                        int substringStart = 0;
                        for (int i = 0; i < 12; i++) {
                            String redBinaryString = blockBinaryString.substring(substringStart,substringStart+5);
                            substringStart+=5;
                            String greenBinaryString = blockBinaryString.substring(substringStart,substringStart+6);
                            substringStart+=6;
                            String blueBinaryString = blockBinaryString.substring(substringStart,substringStart+5);
                            substringStart+=5;
                            
                            
                            int redColor = Integer.parseInt(redBinaryString, 2);
                            int greenColor = Integer.parseInt(greenBinaryString, 2);
                            int blueColor = Integer.parseInt(blueBinaryString, 2);
                            
                            redColor = scaleIntToRange(redColor,31,255);
                            greenColor = scaleIntToRange(greenColor,63,255);
                            blueColor = scaleIntToRange(blueColor,31,255);

                            Color tempPixelColor = new Color(redColor,greenColor,blueColor);
                           
                            reconColor[currentRow][currentCol] = tempPixelColor;

                            currentCol++;
                            if(currentCol>=SelfEmbed.imageWidth)
                            {
                                currentRow++;
                                currentCol = 0;
                            }

                        }
                    }
            }
        }
        BufferedImage bufImage = new BufferedImage(reconColor.length, reconColor[0].length,
                    BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < reconColor.length; i++) {
            for (int j = 0; j < reconColor[0].length; j++) {
               if(reconColor[i][j] == null)
               {
                  reconColor[i][j] = new Color(0,0,0);
                  
               }
               bufImage.setRGB(i, j, reconColor[i][j].getRGB());
            }
            
        }
        
        
        bufImage = getScaledImage(bufImage,512,512);
        ImageIO.write(bufImage, "BMP", new File("ReconstructedImageAfterScaleUp.bmp"));
        //SelfEmbed.saveImageFromColor(reconColor, "reconstructedImage.bmp",SelfEmbed.imageWidth);
        
        //reconstruct the image
        ImageHolder reconstructedHolder = new ImageHolder();
        reconstructedHolder.extractData(bufImage);
        ArrayList<Block> reconstructedBlocks = SelfEmbed.getBlocks(reconstructedHolder.getImage());
        
        for(int i=0;i<blocks.size();i++){
            if(!blocks.get(i).authentic)
            {
                blocks.set(i, reconstructedBlocks.get(i));
            }
        }
        Color[][] reAssembleImageColours = reAssembleImage(blocks,"finalImageAfterRestoration.bmp");
        
    }
    
    public static boolean checkBlockAuthenticity(Block block) throws FileNotFoundException, NoSuchAlgorithmException{
        char[][][] blockBits = block.getBlock();
        String blockBinary = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blockBinary+= blockBits[0][i][j];
                blockBinary+= blockBits[1][i][j];
                blockBinary+= blockBits[2][i][j];
            }  
        }
        
        String testHash = blockBinary.substring(blockBinary.length()-30,blockBinary.length());
        blockBinary = blockBinary.substring(0,blockBinary.length()-30);
        
        String calculatedHash = ShaHashHelper.getBlockHash(blockBinary,96);
        calculatedHash = calculatedHash.substring(0,30);
        
        return calculatedHash.equals(testHash);
    }
    
    //
    public static ArrayList<Integer> extractWatermark(MyImage image) throws NoSuchAlgorithmException, IOException{
        
        removeWaterMarkFromImage(image.getImagePixels());
        return null;
    }
    
    public static Color[][] removeWaterMarkFromImage(Color[][] imageGrid) throws NoSuchAlgorithmException, IOException{
        String blockBinaryHash ="";
        int confusionCount = 0;
        int notAuthenticCount = 0;
        //calculate hash for each 8x8 block
        int rowCorner = 0;
        int colCorner = 0;
        while(rowCorner <512 && colCorner <512){
            ArrayList<WatermarkHelper> helperList = new ArrayList<>();
            String blockContentString = "";
            String extractedWatermark = "";

            //embed hash into block
            for (int i = rowCorner; i < rowCorner+8; i++) {
                for (int j = colCorner; j < colCorner+8; j+=2){
                    int redLeft = imageGrid[i][j].getRed();
                    int greenLeft = imageGrid[i][j].getGreen();
                    int blueLeft = imageGrid[i][j].getBlue();
                    
                    int redRight = imageGrid[i][j+1].getRed();
                    int greenRight = imageGrid[i][j+1].getGreen();
                    int blueRight = imageGrid[i][j+1].getBlue();
                    
                    //System.out.println("["+redLeft+","+redRight+"]");
                    WatermarkHelper newRed = removeFragileWatermark(redLeft,redRight);
                    if(newRed.isEmbedded)
                        extractedWatermark += ""+newRed.embeddedBit;  
                    
                    WatermarkHelper newGreen = removeFragileWatermark(greenLeft,greenRight);
                    if(newGreen.isEmbedded)
                        extractedWatermark += ""+newGreen.embeddedBit; 
                    
                    WatermarkHelper newBlue = removeFragileWatermark(blueLeft,blueRight);
                    if(newBlue.isEmbedded)
                        extractedWatermark += ""+newBlue.embeddedBit; 
                    
                    if (newRed.confusion)
                        confusionCount++;
                    if (newGreen.confusion)
                        confusionCount++;
                    if (newBlue.confusion)
                        confusionCount++;
                    //restoring the image
                    Color newFirstColor = new Color(newRed.num1,newGreen.num1,newBlue.num1);
                    imageGrid[i][j] =newFirstColor;
                    Color newSecondColor = new Color(newRed.num2,newGreen.num2,newBlue.num2);
                    imageGrid[i][j+1] =newSecondColor;
                    
                    helperList.add(newRed);
                    helperList.add(newGreen);
                    helperList.add(newBlue);
                }  
            }
            
            //get hash for block
            for (int i = rowCorner; i < rowCorner+8; i++) {
                for (int j = colCorner; j < colCorner+8; j++){
                    blockContentString+= imageGrid[i][j].getRed();
                    blockContentString+= imageGrid[i][j].getGreen();
                    blockContentString+= imageGrid[i][j].getBlue();
                } 
            }
            
            //get hash for block
            boolean blockNotAuthentic = false;
            blockBinaryHash = ShaHashHelper.getBlockHash(blockContentString,96);

            if(blockBinaryHash.contains(extractedWatermark)){
                //System.out.println("Authentic");
            }else
            {
                System.out.println("Not Authentic");
                blockNotAuthentic=true;
            }
            

            if(colCorner+8 >=512)
            {
                colCorner = 0;
                rowCorner +=8;
            }else{
                colCorner+=8;
            }
            
        }
        
        
        return imageGrid;
    }
    
    private static WatermarkHelper removeFragileWatermark(int num1, int num2){
        
        WatermarkHelper helper = new WatermarkHelper();


        //l = avarage, h=difference
        boolean num1Larger = false;
        //System.out.println("before: ["+num1+"],["+num2+"]");
        int testL =(num1+num2)/2;
        int testH;
        if(num1>num2){
            num1Larger = true;
            testH =num1-num2;
        }
        else{
            num1Larger = false;
            testH=num2-num1;
        }
        
        if(!isExpandable(testH,testL))
        {
            helper.num1 = num1;
            helper.num2 = num2;
            helper.isEmbedded = false;
            helper.confusion = false;
            return helper;
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
       // System.out.println("after: ["+num1AfterTest+"],["+num2AfterTest+"]");
        

        helper.num1 = num1AfterTest;
        helper.num2 = num2AfterTest;
        helper.isEmbedded = true;
        helper.embeddedBit =Integer.parseInt(embeddedBitAfter);

        
        
        return helper;
    }
}
