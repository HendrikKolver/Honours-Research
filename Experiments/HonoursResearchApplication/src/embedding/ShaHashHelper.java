package embedding;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 *
 * @author Hendrik
 */
public class ShaHashHelper {
    public static String getBlockHash(ArrayList<Block> blockList) throws FileNotFoundException, NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String binaryString ="";
        for(Block block: blockList){
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                   binaryString+=block.getBlock()[0][i][j];
                   binaryString+=block.getBlock()[1][i][j];
                   binaryString+=block.getBlock()[2][i][j];
                }  
            }
        }
        
        byte[] array = new BigInteger(binaryString, 2).toByteArray();
        
        System.out.println("Length: "+array.length);
        md.update(array, 0, array.length);
        
        String binaryStringFinal = toBinary(md.digest());
        binaryStringFinal = binaryStringFinal.substring(0,96);
        return  binaryStringFinal;
    }
    
    private static String toBinary( byte[] bytes )
    {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }
}
