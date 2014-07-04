package extraction;

import embedding.Block;
import embedding.SelfEmbed;
import static embedding.SelfEmbed.getBinary;
import static embedding.SelfEmbed.getBinary;
import embedding.ShaHashHelper;
import embedding.WatermarkHelper;
import java.awt.Color;
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
        extractWatermark(holder.getImage());
        
    }
    
    //
    public static ArrayList<Integer> extractWatermark(MyImage image) throws NoSuchAlgorithmException, IOException{
        
        removeWaterMarkFromImage(image.getImagePixels());
        return null;
    }
    
    public static Color[][] removeWaterMarkFromImage(Color[][] imageGrid) throws NoSuchAlgorithmException, IOException{
        String blockBinaryHash ="";
        //calculate hash for each 8x8 block
        int rowCorner = 0;
        int colCorner = 0;
        while(rowCorner <512 && colCorner <512){

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
                    
                    //restoring the image
                    Color newFirstColor = new Color(newRed.num1,newGreen.num1,newBlue.num1);
                    imageGrid[i][j] =newFirstColor;
                    Color newSecondColor = new Color(newRed.num2,newGreen.num2,newBlue.num2);
                    imageGrid[i][j+1] =newSecondColor;
                    
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
            
            blockBinaryHash = ShaHashHelper.getBlockHash(blockContentString);
            //System.out.println("1: "+ blockBinaryHash);
           // System.out.println("2: "+ extractedWatermark);
            if(blockBinaryHash.contains(extractedWatermark)){
                //System.out.println("Authentic");
            }else
            {
                System.out.println("Not authentic");
//                 System.out.println("1: "+ blockBinaryHash);
//                 System.out.println("2: "+ extractedWatermark);
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
        if(couldThisbeOutOfBounds(num1,num2))
        {
            WatermarkHelper helper = new WatermarkHelper();
            helper.num1 = num1;
            helper.num2 = num2;
            helper.isEmbedded = false;
            return helper;
        }

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
        
        WatermarkHelper helper = new WatermarkHelper();

        helper.num1 = num1AfterTest;
        helper.num2 = num2AfterTest;
        helper.isEmbedded = true;
        helper.embeddedBit =Integer.parseInt(embeddedBitAfter);

        
        
        return helper;
    }
     
    
    public static boolean couldThisbeOutOfBounds(int num1, int num2){
        int binaryBit = 0;
        boolean num1Larger = false;
        int l = (num1+num2)/2;
        int h;
        if(num1>num2){
            num1Larger=true;
            h =num1-num2;
        }
        else
            h=num2-num1;
        
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
        
        if(num1After>255 | num1After<0 || num2After>255 | num2After<0 )
            return true;
        
        binaryBit = 1;
        num1Larger = false;
        l = (num1+num2)/2;
        h =0;
        if(num1>num2){
            num1Larger=true;
            h =num1-num2;
        }
        else
            h=num2-num1;
        
        //System.out.println("Original bit: "+ binaryBit);
        //System.out.println("bb["+num1+","+num2+"]");
        
        differenceBinary = getBinary(h);
        differenceBinary = differenceBinary.substring(0, 1) + binaryBit + differenceBinary.substring(1, differenceBinary.length());
        hAfter = Integer.parseInt(differenceBinary, 2);
        num1After=0;
        num2After=0;
        if(num1Larger){
            num1After = l+((hAfter+1)/2);
            num2After = num1After-hAfter;
        }
        else{
            num2After = l+((hAfter+1)/2);
            num1After = num2After-hAfter;
        }
        
        if(num1After>255 | num1After<0 || num2After>255 | num2After<0 )
            return true;
        
        return false;

    }
    
    public static boolean ultimateEmbeddingCheck(int num1,int num2){

        WatermarkHelper helper1 = dummyEmbedFragileWatermark(num1,num2,'0');
        WatermarkHelper helper2 = dummyEmbedFragileWatermark(num1,num2,'1');
        
        if(couldThisbeOutOfBounds(helper1.num1,helper1.num2)||couldThisbeOutOfBounds(helper2.num1,helper2.num2)){
            return false;
        }else{
            return true;
        }
    }
    
        private static WatermarkHelper dummyEmbedFragileWatermark(int num1, int num2, char binaryBit){
        
        boolean num1Larger = false;
        int l = (num1+num2)/2;
        int h;
        if(num1>num2){
            num1Larger=true;
            h =num1-num2;
        }
        else
            h=num2-num1;
        
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

        WatermarkHelper helper = new WatermarkHelper();
        helper.num1 = num1After;
        helper.num2 = num2After;
        helper.isEmbedded = true;
        return helper;

    }
}
