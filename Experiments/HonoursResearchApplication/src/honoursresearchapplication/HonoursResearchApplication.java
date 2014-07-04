package honoursresearchapplication;

import embedding.SelfEmbed;
import extraction.Extract;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import loadImage.ImageHolder;

/**
 *
 * @author Hendrik
 */
public class HonoursResearchApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
       long start = System.currentTimeMillis();
       ImageHolder holder = new ImageHolder();
       holder.extractImageData("lena.bmp");
       
       SelfEmbed.selfEmbed(holder.getImage());
       System.out.println("Exiting After Successfull execution: "+ ((System.currentTimeMillis()-start)/1000.0)+"s");
       Extract.ExtractImage("finalImage.bmp");
    }
    
}
