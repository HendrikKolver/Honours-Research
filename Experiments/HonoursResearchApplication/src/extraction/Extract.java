package extraction;

import embedding.Block;
import embedding.SelfEmbed;
import static embedding.SelfEmbed.*;
import static embedding.SelfEmbed.getBinary;
import embedding.ShaHashHelper;
import embedding.WatermarkHelper;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
        checkAllBlockAuthenticity(blocks);
        //extractWatermark(holder.getImage());
        
    }
    
    public static void checkAllBlockAuthenticity(ArrayList<Block> blocks) throws FileNotFoundException, NoSuchAlgorithmException{
        for(Block block: blocks){
            if(checkBlockAuthenticity(block)){
                System.out.println("dammit"); 
            }else
            {
                //System.out.println("whoo");
            }
        }
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
        
        String calculatedHash = ShaHashHelper.getBlockHash(blockBinary);
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
            blockBinaryHash = ShaHashHelper.getBlockHash(blockContentString);

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
