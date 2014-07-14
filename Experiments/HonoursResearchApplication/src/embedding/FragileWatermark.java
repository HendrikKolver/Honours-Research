/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package embedding;

import static embedding.SelfEmbed.embedFragileWatermark;
import static embedding.SelfEmbed.getBinary;
import static embedding.SelfEmbed.getBinary;
import static embedding.SelfEmbed.getChangableBits;
import static embedding.SelfEmbed.getChangableBitsCount;
import static embedding.SelfEmbed.getDifference;
import static embedding.SelfEmbed.getNewValuePair;
import static embedding.SelfEmbed.getStringForLocationMap;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 *
 * @author Hendrik
 */
public class FragileWatermark {
    public static ArrayList<Block> embedWaterMark(ArrayList<Block> imageBlocks) throws NoSuchAlgorithmException, IOException{
        return embedWaterMarkInLSBLayer(imageBlocks);
    }
    
    public static ArrayList<Block> embedWaterMarkInLSBLayer(ArrayList<Block> imageBlocks) throws NoSuchAlgorithmException, IOException{
        String blockBinary = "";
        ArrayList<Block> newBlocks = new ArrayList();
        for(Block block: imageBlocks){
            if(block.lsbLayer!=7){
                blockBinary+=getBlockBinary(block);
                newBlocks.add(block);
            }
            else{
                //192 = 8x8x3
                String hashString = ShaHashHelper.getBlockHash(blockBinary,192);
                char[][][] newLsbContent;
                newLsbContent = block.getBlock();
                int hashBitCounter = 0;
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        newLsbContent[0][i][j] = hashString.charAt(hashBitCounter);
                        hashBitCounter++;
                        newLsbContent[1][i][j] = hashString.charAt(hashBitCounter);
                        hashBitCounter++;
                        newLsbContent[2][i][j] = hashString.charAt(hashBitCounter);
                        hashBitCounter++;
                    }
                }
                block.setBlock(newLsbContent);
                newBlocks.add(block);
                blockBinary="";
            }
        }

        return newBlocks;
    }
    
    public static String getBlockBinary(Block block){
        String blockBinary = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blockBinary += block.getBlock()[0][i][j];
                blockBinary += block.getBlock()[1][i][j];
                blockBinary += block.getBlock()[2][i][j];
            }
        }
        return blockBinary;
    }
    
     public static Color[][] embedWaterMark(Color[][] imageGrid) throws NoSuchAlgorithmException, IOException{
         return WavletBased(imageGrid);
     }
     
     public static Color[][] WavletBased(Color[][] imageGrid) throws FileNotFoundException, NoSuchAlgorithmException{
         String blockBinaryHash;
        //calculate hash for each 8x8 block
        int rowCorner = 0;
        int colCorner = 0;
        while(rowCorner <512 && colCorner <512){

            String blockContentString = "";

            
            //get hash for block
            for (int i = rowCorner; i < rowCorner+8; i++) {
                for (int j = colCorner; j < colCorner+8; j++){
                    blockContentString+= imageGrid[i][j].getRed();
                    blockContentString+= imageGrid[i][j].getGreen();
                    blockContentString+= imageGrid[i][j].getBlue();
                } 
            }
            
            //get hash for block
            blockBinaryHash = ShaHashHelper.getBlockHash(blockContentString,96);
            int[][][] biLevelLocationMap = new int[3][8][8];
            String changableBitString ="";
            int binaryCount = 0;
            //embed hash into block and get changable bits and location map
            for (int i = rowCorner; i < rowCorner+8; i++) {
                for (int j = colCorner; j < colCorner+8; j+=2){
                    int redLeft = imageGrid[i][j].getRed();
                    int greenLeft = imageGrid[i][j].getGreen();
                    int blueLeft = imageGrid[i][j].getBlue();
                    
                    int redRight = imageGrid[i][j+1].getRed();
                    int greenRight = imageGrid[i][j+1].getGreen();
                    int blueRight = imageGrid[i][j+1].getBlue();
                    
                    
                    //System.out.println("["+redLeft+","+redRight+"]");
                    WatermarkHelper newRed = embedFragileWatermark(redLeft,redRight,blockBinaryHash.charAt(binaryCount));
                    if(newRed.isEmbedded){
                        biLevelLocationMap[0][(i-rowCorner)][(j-colCorner)] = 1;
                        biLevelLocationMap[0][(i-rowCorner)][(j-colCorner)+1] = 1;
                        binaryCount++;
                    }else{
                        biLevelLocationMap[0][(i-rowCorner)][(j-colCorner)] = 0;
                        biLevelLocationMap[0][(i-rowCorner)][(j-colCorner)+1] = 0; 
                    }
                    
                    WatermarkHelper newGreen = embedFragileWatermark(greenLeft,greenRight,blockBinaryHash.charAt(binaryCount));
                    if(newGreen.isEmbedded){
                        biLevelLocationMap[1][(i-rowCorner)][(j-colCorner)] = 1;
                        biLevelLocationMap[1][(i-rowCorner)][(j-colCorner)+1] = 1;
                        binaryCount++;
                    }else{
                        biLevelLocationMap[1][(i-rowCorner)][(j-colCorner)] = 0;
                        biLevelLocationMap[1][(i-rowCorner)][(j-colCorner)+1] = 0; 
                    }

                    WatermarkHelper newBlue = embedFragileWatermark(blueLeft,blueRight,blockBinaryHash.charAt(binaryCount));
                    if(newBlue.isEmbedded){
                        biLevelLocationMap[2][(i-rowCorner)][(j-colCorner)] = 1;
                        biLevelLocationMap[2][(i-rowCorner)][(j-colCorner)+1] = 1;
                        binaryCount++;
                    }else{
                        biLevelLocationMap[2][(i-rowCorner)][(j-colCorner)] = 0;
                        biLevelLocationMap[2][(i-rowCorner)][(j-colCorner)+1] = 0; 
                    }
                    
                    changableBitString += getChangableBits(newRed.num1,newRed.num2);
                    changableBitString += getChangableBits(newGreen.num1,newGreen.num2);
                    changableBitString += getChangableBits(newBlue.num1,newBlue.num2);

                    //embedding the watermark
                    Color newFirstColor = new Color(newRed.num1,newGreen.num1,newBlue.num1);
                    imageGrid[i][j] =newFirstColor;
                    Color newSecondColor = new Color(newRed.num2,newGreen.num2,newBlue.num2);
                    imageGrid[i][j+1] =newSecondColor;
                    
                } 
            }
            String locationMapString = getStringForLocationMap(biLevelLocationMap);
            String embeddingString = locationMapString+changableBitString;
            embeddingString = Compression.compress(embeddingString);
            //replace this string with the changableBitString, Location map & conjugation map
            String arbitraryBinaryString = "1011001010110010101100101101001100110101001110010100011010111010101110001010001010100101011110101010100000111101010";
            int binaryStringEmbedLocation = 0;
            //embed original changable bits into image along with location and conjugation map
            for (int i = rowCorner; i < rowCorner+8; i++) {
                for (int j = colCorner; j < colCorner+8; j+=2){
                    int redLeft = imageGrid[i][j].getRed();
                    int greenLeft = imageGrid[i][j].getGreen();
                    int blueLeft = imageGrid[i][j].getBlue();
                    
                    int redRight = imageGrid[i][j+1].getRed();
                    int greenRight = imageGrid[i][j+1].getGreen();
                    int blueRight = imageGrid[i][j+1].getBlue();
                    
                    
                    //red
                    int changableBitCount = getChangableBitsCount(redLeft,redRight);
                    int h = getDifference(redLeft,redRight);
                    
                    String originalDifString = getBinary(h);
                    
                    int originalHBinaryLength = originalDifString.length();
                    String notChangablebits = originalDifString.substring(0,originalDifString.length()-changableBitCount);
                    
                    WatermarkHelper red = getNewValuePair(redLeft,redRight,binaryStringEmbedLocation,arbitraryBinaryString);
                    binaryStringEmbedLocation += originalHBinaryLength-notChangablebits.length();
                    
                    //green
                    changableBitCount = getChangableBitsCount(greenLeft,greenRight);
                    h = getDifference(greenLeft,greenRight);
                    
                    originalDifString = getBinary(h);
                    originalHBinaryLength = originalDifString.length();
                    notChangablebits = originalDifString.substring(0,originalDifString.length()-changableBitCount);
                    
                    WatermarkHelper green = getNewValuePair(greenLeft,greenRight,binaryStringEmbedLocation,arbitraryBinaryString);
                    binaryStringEmbedLocation += originalHBinaryLength-notChangablebits.length();
                    
                    //blue
                    changableBitCount = getChangableBitsCount(blueLeft,blueRight);
                    h = getDifference(blueLeft,blueRight);
                    
                    originalDifString = getBinary(h);
                    originalHBinaryLength = originalDifString.length();
                    notChangablebits = originalDifString.substring(0,originalDifString.length()-changableBitCount);
                    
                    WatermarkHelper blue = getNewValuePair(blueLeft,blueRight,binaryStringEmbedLocation,arbitraryBinaryString);
                    binaryStringEmbedLocation += originalHBinaryLength-notChangablebits.length();
                    

                    //embedding the watermark
                    Color newFirstColor = new Color(red.num1,green.num1,blue.num1);
                    imageGrid[i][j] =newFirstColor;
                    Color newSecondColor = new Color(red.num2,green.num2,blue.num2);
                    imageGrid[i][j+1] =newSecondColor;
                } 
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
}

