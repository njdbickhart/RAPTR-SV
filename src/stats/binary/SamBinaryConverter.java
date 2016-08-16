/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stats.binary;

import TempFiles.binaryUtils.IntUtils;
import TempFiles.binaryUtils.LongUtils;
import htsjdk.samtools.SAMRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * @author Derek.Bickhart
 */
public class SamBinaryConverter {
    public static void processSAMInfoToBinary(long clone, short num, SAMRecord sam, RandomAccessFile file) throws IOException{
        // clone hash
        file.write(LongUtils.longToByteArray(clone));        
        // read num
        file.writeShort(num);        
        // chr
        // TODO: Need to create the chromosome table and share it among the different programs!
        
        // pos
        file.writeLong(sam.getAlignmentStart());
        // align end
        file.writeLong(sam.getAlignmentEnd());        
        // flags
        file.write(IntUtils.Int16ToTwoByteArray(sam.getFlags()));        
        // Map qual
        file.write(sam.getMappingQuality());
    }
    
    public static byte[] processSAMInfoToBinary(long clone, short num, SAMRecord sam) throws IOException{
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(LongUtils.longToByteArray(clone));
        byteStream.write(num);
        byteStream.write(sam.getAlignmentStart());
        byteStream.write(sam.getAlignmentEnd());
        byteStream.write(IntUtils.Int16ToTwoByteArray(sam.getFlags()));
        byteStream.write(sam.getMappingQuality());
        
        return byteStream.toByteArray();
    }
    
    public static short getShortSAMBinFromBinary(byte[] sambytes){
       return sambytes[8];
    }
    
    public static long getCloneHashSAMFromBinary(byte[] sambytes){
        byte[] longBytes = Arrays.copyOf(sambytes, 8);
        return LongUtils.byteArrayToLong(longBytes);
    }
    
    public static short getShortSAMBinFromBinary(Byte[] sambytes){
       return sambytes[8];
    }
    
    public static long getCloneHashSAMFromBinary(Byte[] sambytes){
        Byte[] longBytes = Arrays.copyOf(sambytes, 8);
        return LongUtils.byteArrayToLong(longBytes);
    }
}
