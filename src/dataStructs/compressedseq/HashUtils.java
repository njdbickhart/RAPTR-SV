/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs.compressedseq;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author desktop
 */
public class HashUtils {
    public static List<Long> sectionByteArraysToLongList(byte[] b) throws Exception{
        if(b.length % 8 != 0)
            throw new Exception("[HASHUTILS] Input byte array is not a multiple of 64 bits!");
        List<Long> holder = Collections.synchronizedList(new ArrayList<>(b.length / 8));
        for(int i = 0; i < b.length; i += 8){
            long t = byteArrayToLong(getByteSlice(b, i, i + 8));
            holder.add(t);
        }
        return holder;
    }
    
    public static long byteArrayToLong(byte[] b){
        long value = 0l;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
    
    public static byte[] longToByteArray(long l){
        return ByteBuffer.allocate(8).putLong(0, l).array();
    }
    
    public static int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
    
    // TODO: check to see if this is an endian issue
    public static byte[] intToByteArray(int a){
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);   
        ret[2] = (byte) ((a >> 8) & 0xFF);   
        ret[1] = (byte) ((a >> 16) & 0xFF);   
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
    
    public static byte[] intToTwoByteArray(int a){
        byte[] ret = new byte[2];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        return ret;
    }
    
    public static byte[] getVariableByteArray(int len){
        return new byte[len];
    }
    
    public static byte[] getByteSlice(byte[] block, int startpos, int len){
        byte[] temp = new byte[len];
        int counter = 0;
        for(int i = startpos; i < startpos + len; i++){
            temp[counter] = block[i];
        }
        return temp;
    }
    
    // Returns number of bytes written to buffer
    public static int encodeVariableByte(byte[] buffer, int value){
            int t = 0;
            do {
                    buffer[t++] = (byte) (value & 127);
                    value /= 128;
            } while (value != 0);
            buffer[t-1] |= 128;
            return t;
    }
    
    // Returns number of bytes written from buffer
    public static int decodeVariableByte(byte[] buffer, int index, int result){
            int i = 0;
            byte[] temp = Arrays.copyOf(buffer, buffer.length + 4);
            byte[] subtemp = intToByteArray(index);
            
            for(int b = buffer.length - 1; b < subtemp.length; b++){
                temp[b] = subtemp[i++];
            }
            i = 0;
            byte t;
            result = 0;
            do {
                    t = buffer[i];
                    result |= ((t&127) <<(7*i));
                    i++;
            } while ((t & 128) == 0);
            return i;
    }
    
    public static int hashVal(char[] seq, int windowSize){
            int i=0;
            int val=0;

            while(i<windowSize)
            {
                    if (StringUtils.BaseHashVal(seq[i]) == -1)
                            return -1; 
                    val = (val << 2) | StringUtils.BaseHashVal(seq[i++]); 
            }
            return val;
    }
    
    public static int checkSumVal(char[] seq, int checkSumLength){
            int i=0;
            int val=0;

            while(i<checkSumLength)
            {
                    if (StringUtils.BaseHashVal(seq[i]) == -1)
                            return -1; 
                    val = (val << 2) | StringUtils.BaseHashVal(seq[i++]); 
            }
            return val;
    }
    
    // Divided by eight, because it's 8 bytes per 21 bases
    public static int calculateCompressedLen(int normalLen){
            return (((normalLen / 21) + ((normalLen%21 > 0)?1:0))/8);
    }
}
