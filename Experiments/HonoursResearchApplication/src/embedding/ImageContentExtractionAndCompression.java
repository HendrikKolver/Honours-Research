package embedding;

import loadImage.Image;

/**
 *
 * @author Hendrik
 */
public class ImageContentExtractionAndCompression {
    public static void getCompressedImageContent(Image image, double embeddingCapacity){
        System.out.println(embeddingCapacity );
        double newPixelAmount = embeddingCapacity/24.0;
        double percentageLoss = (newPixelAmount/(512*512)*100);
        System.out.println("Size of embedded image: "+percentageLoss);
        
    }
}
