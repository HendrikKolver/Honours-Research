package psnrcalculate;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PSNR {
    final static String originalName = "lena.bmp";
    final static String modifiedName = "finalImage.bmp";
    public static void calculate() throws IOException{
    int     nrows, ncols;
    int     img1[][], img2[][];
    double  peakr,peakg,peakb, signalr,signalg,signalb, noiser,noiseg,noiseb, mse;

    nrows = 512;
    ncols = 512;
    img1 = getImageRGB(originalName,0);
    img2 = getImageRGB(modifiedName,0);
    

    signalr = noiser = peakr = 0;
    for (int i=0; i<nrows; i++) {
      for (int j=0; j<ncols; j++) {
        signalr += img1[i][j] * img1[i][j];
        noiser += (img1[i][j] - img2[i][j]) * (img1[i][j] - img2[i][j]);
        if (peakr < img1[i][j])
          peakr = img1[i][j];
      }
    }
    
    img1 = getImageRGB(originalName,1);
    img2 = getImageRGB(modifiedName,1);
    

    signalg = noiseg = peakg = 0;
    for (int i=0; i<nrows; i++) {
      for (int j=0; j<ncols; j++) {
        signalg += img1[i][j] * img1[i][j];
        noiseg += (img1[i][j] - img2[i][j]) * (img1[i][j] - img2[i][j]);
        if (peakg < img1[i][j])
          peakg = img1[i][j];
      }
    }
    
    img1 = getImageRGB(originalName,2);
    img2 = getImageRGB(modifiedName,2);
    

    signalb = noiseb = peakb = 0;
    for (int i=0; i<nrows; i++) {
      for (int j=0; j<ncols; j++) {
        signalb += img1[i][j] * img1[i][j];
        noiseb += (img1[i][j] - img2[i][j]) * (img1[i][j] - img2[i][j]);
        if (peakb < img1[i][j])
          peakb = img1[i][j];
      }
    }
    
    double peak = ((peakr+peakg+peakb)/3);
    double noise = ((noiser+noiseg+noiseb)/3);
    double signal = ((signalr+signalg+signalb)/3);

    mse = noise/(nrows*ncols); // Mean square error
    System.out.println("MSE: " + mse);
    System.out.println("SNR: " + 10*log10(signal/noise));
    System.out.println("PSNR(max=255): " + (10*log10(255*255/mse)));
    System.out.println("PSNR(max=" + peak + "): " + 10*log10((peak*peak)/mse));
    }
    
    public static double log10(double x) {
    return Math.log(x)/Math.log(10);
  }
    
  public static int[][] getImageRGB(String imagePath, int RGB) throws IOException
  {
       File img = new File(imagePath);
       BufferedImage bufferedImage = ImageIO.read(img);
       int[][] imgArray = new int[bufferedImage.getHeight()][bufferedImage.getWidth()];
       
       for (int i = 0; i < bufferedImage.getHeight(); i++) {
           for (int j = 0; j < bufferedImage.getWidth(); j++) {
               Color col = new Color(bufferedImage.getRGB(i, j));
              if(RGB==0)
                imgArray[i][j] = col.getRed();  
              else if(RGB == 1)
                imgArray[i][j] = col.getGreen(); 
              else
                 imgArray[i][j] = col.getBlue();
           }  
      }
      return imgArray;
  }
}
