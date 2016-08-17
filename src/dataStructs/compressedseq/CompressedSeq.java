/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs.compressedseq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import static dataStructs.compressedseq.StringUtils.BaseToIndex;

/**
 *
 * @author desktop
 */
public class CompressedSeq {
    // Every 21 bases are stored within a 64bit unsigned integer
    public List<Long> CompSeq;
    
    public CompressedSeq(byte[] b) throws Exception{
        this.CompSeq = HashUtils.sectionByteArraysToLongList(b);
    }
    
    public CompressedSeq(String seq){
        int i = 0, pos = 0, lidx = 0;
        this.CompSeq = Collections.synchronizedList(new ArrayList<Long>(seq.length() / 21));
        // Start with forward sequence string
        CompSeq.add(0l);
        while(pos < seq.length()){
            CompSeq.set(lidx, ((CompSeq.get(lidx) << 3)) | BaseToIndex(seq.charAt(pos++)));
            
            if(++i == 21){
                i = 0;
                
                if(pos < seq.length()){  // This prevents the addition of a new element if the sequence is exactly 21 chars
                    CompSeq.add(0l);
                    lidx++;
                }
            }
        }
        if(i > 0){
            CompSeq.set(lidx, CompSeq.get(lidx) << (3 * (21 - i)));
        }
    }
    public CompressedSeq(char[] seq){
        int i = 0, pos = 0;
        this.CompSeq = Collections.synchronizedList(new ArrayList<Long>(seq.length / 21));
        // Start with forward sequence string
        CompSeq.add(0l);
        while(pos < seq.length){
            CompSeq.set(i, ((CompSeq.get(i) << 3)) | BaseToIndex(seq[pos++]));
            
            if(++i == 21){
                i = 0;
                
                if(pos < seq.length)  // This prevents the addition of a new element if the sequence is exactly 21 chars
                    CompSeq.add(0l);
            }
        }
        if(i > 0){
            CompSeq.set(i, CompSeq.get(i) << (3 * (21 - i)));
        }
    }
    
    public String DeCompressSeq(int readlen) throws Exception{
        if(this.CompSeq.isEmpty())
            throw new Exception("Error! Compressed seq entry is empty! Array was either not initialized or data is corrupted!");
        
        char[] seq = new char[readlen];
        int i = 1, lidx = 0;
        for(int x = 0; x < readlen; x++){
            // Left shift to remove the bases 
            seq[x] = StringUtils.IndexToBase(GetBaseBits(this.CompSeq.get(lidx), (3 * (21 - i))));
            
            if(++i == 22){
                // We're reached the end of a long so now move onto the next
                i = 1; 
                lidx++;
            }
        }
        return new String(seq);
    }
    
    public int GetBaseBits(long l, int bitPosition){
        // Shift bits all the way to the three we want
        long RightShift = l >>> bitPosition;
        final int mask = 7;
        return (int) (RightShift & mask);
    }
    
    public Stream<Long> getStream(){
        return this.CompSeq.stream();
    }
    
    public void destroy(){
        this.CompSeq.clear();
        this.CompSeq = null;
    }
    
    public long retrieveCSeqFromList(int seqLocation) throws Exception{
        long current;
        int cSeqALS = seqLocation * 3;
        int cSeqARS = 63 - cSeqALS;
        if(seqLocation < 0)
            throw new Exception("[COMPSEQ] ERROR! Sequence location less than zero!");
        
        if(seqLocation % 64 != 0){  
            // The location on the sequence is NOT a multiple of 64, so we need to offset longs from the list
            int idx1 = Math.floorDiv(seqLocation, 64);
            int idx2 = idx1 + 1;
            
            if(idx2 > this.CompSeq.size())
                throw new Exception("[COMPSEQ] ERROR! second index overrun!");
            
            current = (this.CompSeq.get(idx1) << cSeqALS) | (this.CompSeq.get(idx2) >> cSeqARS);
        }else{
            current = this.CompSeq.get(seqLocation / 64);
        }
        return current;
    }
    
    public byte[] getByteList(){
        byte[] ret = new byte[this.CompSeq.size() * 8];
        for(int i = 0; i < this.CompSeq.size(); i++){
            byte[] temp = HashUtils.longToByteArray(this.CompSeq.get(i));
            for(int j = 0; j < temp.length; j++){
                int retidx = (8 * i) + j;
                ret[retidx] = temp[j];
            }
        }
        return ret;
    }
}
