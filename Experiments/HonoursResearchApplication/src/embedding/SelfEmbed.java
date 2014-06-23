package embedding;

import java.awt.Color;
import java.awt.List;
import java.util.ArrayList;
import loadImage.Image;
import static loadImage.ImageHolder.encodeGray;

/**
 *
 * @author Hendrik
 */
public class SelfEmbed {
    private static int cornerRow =0;
    private static int cornerCol=0;
    
    public static Image selfEmbed(Image image){
        ArrayList<Block> blockList = getBlocks(image);
        int suitableBlockCount = 0;
        for(Block block : blockList){
            if(block.getComplexity()>0.1){
                suitableBlockCount++;
            }
        }
        System.out.println("There are "+suitableBlockCount+ " complex blocks");
        return null;
    }
    
    public static ArrayList<Block> getBlocks(Image image)
    {
        ArrayList<Block> blockList = new ArrayList<>();
        while(cornerRow <512 && cornerCol <512){
            int row=0;
            int col=0;
            
            ArrayList<Block> tempBlocks = new ArrayList<>();
            for (int k = 0; k < 8; k++) { 
                tempBlocks.add(new Block());
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
        
        return blockList;
    }
    
        public String getBinary(String value)
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
    
}
