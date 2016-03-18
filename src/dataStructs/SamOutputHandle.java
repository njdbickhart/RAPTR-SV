/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import TempFiles.TempDataClass;
import htsjdk.samtools.SAMRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bickhart
 */
public class SamOutputHandle extends TempDataClass{
    private final Map<Long, Map<Short, ArrayList<SAMRecord>>> buffer = new HashMap<>();
    private final int threshold;
    public final String readGroup;
    private int overhead = 0;
    
    public SamOutputHandle(int threshold, String rg, String tmpoutname) {
        this.threshold = threshold;
        this.readGroup = rg;
        this.createTemp(Paths.get(tmpoutname + "." + rg + "."));
    }
    
    public void bufferedAdd(SAMRecord a, Long clone, short num) {
        if(!buffer.containsKey(clone))
            buffer.put(clone, new HashMap<Short, ArrayList<SAMRecord>>());
        if(!buffer.get(clone).containsKey(num))
            buffer.get(clone).put(num, new ArrayList<SAMRecord>());
        buffer.get(clone).get(num).add(a);
        overhead++;
        if(overhead >= threshold){
            dumpDataToDisk();
            overhead = 0;
        }
    }
    
    public void combineTempFiles(SamOutputHandle s){
        s.dumpDataToDisk();
        try(BufferedReader input = Files.newBufferedReader(s.getTempFile(), Charset.defaultCharset())){
            if(!this.buffer.isEmpty())
                this.dumpDataToDisk();
            
            this.openTemp('A');
            String line;
            while((line = input.readLine()) != null){
                line = line.trim();
                
                /*String[] segs = line.split("\t");
                if(segs.length < 13){
                System.err.println("DEBUG error!");
                }*/
                this.output.write(line);
                this.output.newLine();
            }
            this.output.flush();
            this.closeTemp('A');
        }catch(IOException ex){
            ex.printStackTrace();
        }
        s.deleteTemp();
    }
    
    @Override
    public void readSequentialFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public synchronized void dumpDataToDisk() {
        this.openTemp('A');
        try{
            for(Long clone : buffer.keySet()){
                for(short num : buffer.get(clone).keySet()){
                    for(SAMRecord sam : buffer.get(clone).get(num)){
                        this.output.write(clone + "\t" + num + "\t" + sam.toString());
                        //this.output.newLine();
                    }
                }
            }
            this.output.flush();
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            this.closeTemp('A');
        }
        this.buffer.clear();
    }
    
    public Path getTempFile(){
        return this.tempFile;
    }
}
