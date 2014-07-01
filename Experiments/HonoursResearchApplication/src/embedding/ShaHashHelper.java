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
    public static String getBlockHash(String binaryString) throws FileNotFoundException, NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] array = binaryString.getBytes();
        
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
