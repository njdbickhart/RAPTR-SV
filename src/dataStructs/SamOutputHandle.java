/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import TempFiles.TempBinaryData;
import TempFiles.TempDataClass;
import TempFiles.binaryUtils.IntUtils;
import TempFiles.binaryUtils.LongUtils;
import dataStructs.compressedseq.CompressedSeq;
import htsjdk.samtools.SAMRecord;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.BamMetadataGeneration;

/**
 *
 * @author bickhart
 */
public class SamOutputHandle extends TempBinaryData{
    private final Map<Long, Map<Short, ArrayList<SAMRecord>>> buffer = new HashMap<>();
    private final int threshold;
    private final BamMetadataGeneration metadata;
    public final String readGroup;
    public int totrecords = 0;
    private int overhead = 0;
    
    private static final Logger log = Logger.getLogger(SamOutputHandle.class.getName());
    
    public SamOutputHandle(int threshold, String rg, String tmpoutname, BamMetadataGeneration metadata) {
        this.threshold = threshold;
        this.readGroup = rg;
        this.metadata = metadata;
        try {
            this.CreateTemp(Paths.get(tmpoutname + "." + rg + "."));
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "[SAMOUTPUT] Error creating temporary file!", ex);
        }
    }
    
    public void bufferedAdd(SAMRecord a, Long clone, short num) {
        if(!buffer.containsKey(clone))
            buffer.put(clone, new HashMap<Short, ArrayList<SAMRecord>>());
        if(!buffer.get(clone).containsKey(num))
            buffer.get(clone).put(num, new ArrayList<SAMRecord>());
        buffer.get(clone).get(num).add(a);
        overhead++;
        totrecords++;
        if(overhead >= threshold){
            dumpDataToDisk();
            overhead = 0;
        }
    }
    
    public RandomAccessFile getTempSamStats() throws IOException{
        this.dumpDataToDisk();
        return this.getFileForReading();
    }
    
    /*
        Sam information records take up 32 bytes + x, so I'll read and write
        the whole block in two steps
    */
    public void combineTempFiles(SamOutputHandle s){
        //s.dumpDataToDisk();
        RandomAccessFile file = null;
        try(RandomAccessFile input = s.getTempSamStats()){
            if(!this.buffer.isEmpty())
                this.dumpDataToDisk();
            
            file = this.getFileForWriting();
            byte[] locBuffer = new byte[32];
            for(int x = 0; x < s.totrecords; x++){
                int state = input.read(locBuffer);
                if(state == -1 && x != s.totrecords - 1){
                    log.log(Level.WARNING, "[SAMOUTPUT] Premature end of temp file in sam temp output merger! Reached record " + x + " out of " + s.totrecords);
                    break;
                }
                // There are 21 bases per every 8 bytes and the 32nd byte of our read record tells us how many to read!
                int readbytes = (int)Math.ceil(locBuffer[31] * ((double)8 / 21));
                byte[] seqbytes = new byte[readbytes];
                input.read(seqbytes);
                
                file.write(locBuffer);
                file.write(seqbytes);
                this.totrecords++;
            }
            
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            try {
                if(file != null)
                    file.close();
            } catch (IOException ex) {
                log.log(Level.WARNING, "[SAMOUTPUT] Error closing temp files!", ex);
            }
        }
        s.deleteTemp();
        
    } 
    
    
    public synchronized void dumpDataToDisk() {
        if(!this.buffer.isEmpty())
            this.DumpToTemp();
        this.buffer.clear();
    }
    
    public Path getTempFile(){
        return this.tempFile;
    }

    @Override
    public void DumpToTemp() {
        try{
            final RandomAccessFile file = this.getFileForWriting();
            buffer.keySet().stream()
                    .forEach((clone) -> {
                        buffer.get(clone).keySet().stream()
                                .forEach((num) -> {
                                    buffer.get(clone).get(num).forEach((sam) -> {
                                        try {
                                            processSAMInfoToBinary(clone, num, sam, file);
                                        } catch (IOException ex) {
                                            log.log(Level.SEVERE, "[SAMOUTPUT] Error interior loop of randomaccess write!", ex);
                                        }
                                    });
                                });
                    });
        }catch(IOException ex){
            log.log(Level.SEVERE, "[SAMOUTPUT] Error writing to temp file!", ex);
        }
    }
    
    /*
    The information I need: 
        clone hash (64 bit long), 
        read num (8 bit int), 
        chr (32 bit int identifier), 
        pos (64 bit long),
        align end (64 bit long),
        sam flags (8 bit int),          
        map quality (8bit int)
        readlen (8 bit int)
        read (x 64 bit longs)
    */    
    private void processSAMInfoToBinary(long clone, short num, SAMRecord sam, RandomAccessFile file) throws IOException{
        // clone hash
        file.write(LongUtils.longToByteArray(clone));        
        // read num
        file.writeShort(num);        
        // chr
        file.write(this.metadata.getIndexByChr(sam.getReferenceName()));        
        // pos
        file.writeLong(sam.getAlignmentStart());
        // align end
        file.writeLong(sam.getAlignmentEnd());        
        // flags
        file.write(IntUtils.Int16ToTwoByteArray(sam.getFlags()));        
        // Map qual
        file.write(sam.getMappingQuality());
        // readlen
        file.write(sam.getReadLength());
        // bases
        CompressedSeq seq = new CompressedSeq(sam.getReadString());
        file.write(seq.getByteList());
        seq.destroy();
    }

    protected void deleteTemp() {
        try {
            this.dataFile.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "[SAMOUTPUT] Could not delete temp file!", ex);
        }
    }
}
