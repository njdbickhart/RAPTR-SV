/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import TempFiles.TempBuffer;
import TempFiles.TempDataClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author bickhart
 */
public class SamRecordMatcher extends TempDataClass {
    private final Map<SAMReadGroupRecord, Map<String, Map<Short, ArrayList<SAMRecord>>>> buffer = new HashMap<>();
    private int overhead = 0;
    private final int threshold;
    private final boolean checkRGs;
    private final String defId = "D";
    
    public SamRecordMatcher(int threshold, boolean checkRGs){
        this.threshold = threshold;
        this.checkRGs = checkRGs;
    }
    
    public void bufferedAdd(SAMRecord a) {
        String clone = getCloneName(a.getReadName());
        short num = getCloneNum(a.getReadName());
        SAMReadGroupRecord r;
        if(this.checkRGs)
            r = a.getReadGroup();
        else
            r = new SAMReadGroupRecord(defId);
        if(!buffer.containsKey(r))
            buffer.put(r, new HashMap<String, Map<Short, ArrayList<SAMRecord>>>());
        if(!buffer.get(r).containsKey(clone))
            buffer.get(r).put(clone, new HashMap<Short, ArrayList<SAMRecord>>());
        if(!buffer.get(r).get(clone).containsKey(num))
            buffer.get(r).get(clone).put(num, new ArrayList<SAMRecord>());
        buffer.get(r).get(clone).get(num).add(a);
        overhead++;
        
        if(overhead >= threshold){
            dumpDataToDisk();
            overhead = 0;
        }
    }

    @Override
    public void readSequentialFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dumpDataToDisk() {
        this.openTemp('A');
        try{
            for(SAMReadGroupRecord r : buffer.keySet()){
                for(String clone : buffer.get(r).keySet()){
                    for(short num : buffer.get(r).get(clone).keySet()){
                        for(SAMRecord sam : buffer.get(r).get(clone).get(num)){
                            this.output.write(r.getId() + "\t" + clone + "\t" + num + "\t" + sam.getReadString());
                            this.output.newLine();
                        }
                    }
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            this.closeTemp('A');
        }
        this.buffer.clear();
    }
    
    public void convertToDivet(Map<String, DivetOutputHandle> divets){
        if(!this.buffer.isEmpty())
            this.dumpDataToDisk();
        
        /*
        Use Unix sort to sort the temp file by multiple columns
        */
        
        this.openTemp('R');
        try{
            String line;
            while((line = this.handle.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            this.closeTemp('A');
        }
        
    }
    
    private String getCloneName(String readName){
        String clone;
        String[] nameSplit = readName.split("[/_]");
        clone = nameSplit[0];
        return clone;
    }
    private short getCloneNum(String readName){
        String clone;
        String[] nameSplit = readName.split("[/_]");
        clone = nameSplit[1];
        return Short.parseShort(clone);
    }
}
