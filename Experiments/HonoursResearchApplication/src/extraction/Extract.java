package extraction;

import embedding.Block;
import embedding.SelfEmbed;
import java.io.IOException;
import java.util.ArrayList;
import loadImage.ImageHolder;

/**
 *
 * @author Hendrik
 */
public class Extract {
    public static void ExtractImage(String path) throws IOException{
        ImageHolder holder = new ImageHolder();
        holder.extractImageData(path);
        ArrayList<Block> blocks = SelfEmbed.getBlocks(holder.getImage());
        System.out.println("blocks size: "+ blocks.size());
        int embeddedBlockCount = 0;
        for(Block block: blocks){
            char[][][] blockContent = block.getBlock();

            if(blockContent[0][0][0] == '0' && blockContent[0][1][2] == '1' && blockContent[2][7][7] == '0' && blockContent[2][7][6] == '0' && blockContent[2][7][5] == '0' && blockContent[2][7][4] == '0'){
                embeddedBlockCount++;
            }
        }
        System.out.println(embeddedBlockCount);
    }
}
