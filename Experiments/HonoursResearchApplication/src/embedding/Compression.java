package embedding;

/**
 *
 * @author Hendrik
 */
public class Compression {
    public static String compress(String text){
       return RLE(text); 
    }
    
    public static String RLE(String text){
        return text;
    }
}
