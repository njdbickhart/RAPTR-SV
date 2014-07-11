/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import TempFiles.TempBuffer;
import TempFiles.TempDataClass;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import stats.ReadNameUtility;
import workers.MrsFastRuntimeFactory;

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
    private final ReadNameUtility rn = new ReadNameUtility();
    
    public SamRecordMatcher(int threshold, boolean checkRGs){
        this.threshold = threshold;
        this.checkRGs = checkRGs;
    }
    
    public void bufferedAdd(SAMRecord a) {
        
        String clone = rn.GetCloneName(a.getReadName(), a.getFlags());
        short num = rn.GetCloneNum(a.getReadName(), a.getFlags());
        
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
                            this.output.write(r.getId() + "\t" + clone + "\t" + num + "\t" + sam.getSAMString());
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
    
    public void convertToVariant(Map<String, DivetOutputHandle> divets, Map<String, SplitOutputHandle> splits, Map<String, Integer[]> thresholds){
        if(!this.buffer.isEmpty())
            this.dumpDataToDisk();
        
        try {
            // Use Unix sort to sort the file by the multiple beginning columns
            ProcessBuilder p = new ProcessBuilder("sort", "-k1,1", "-k2,2", "-k3,3", this.tempFile.toString());
            p.redirectError(new File("sort.error.log"));
            Process sort = p.start();
            sort.waitFor();
            
            String line, lastrg = "none", last = "none";
            ArrayList<String[]> records = new ArrayList<>();
            BufferedReader input = new BufferedReader(new InputStreamReader(sort.getInputStream()));
            while((line = input.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                
                // clone name is not the same as the last one
                if(!segs[1].equals(last)){
                    if(!records.isEmpty()){
                        if(this.isSplit(segs)){
                            if(this.isAnchor(segs))
                                splits.get(lastrg).AddAnchor(segs);
                            else
                                splits.get(lastrg).AddSplit(segs);
                        }else{
                            Integer[] t = thresholds.get(lastrg);
                            SamToDivet converter = new SamToDivet(last, t[0], t[1], t[2]);
                            for(String[] r : records)
                                converter.addLines(r);
                            divets.get(lastrg).PrintDivetOut(converter.getDivets());
                        }
                        records.clear();
                    }
                }
                
                records.add(segs);
                last = segs[1];
                lastrg = segs[0];
            }
            
            input.close();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private boolean isSplit(String[] segs){
        int fflags = Integer.parseInt(segs[4]);
        return (fflags & 0x8) == 0x8 || (fflags & 0x4) == 0x4;
    }
    
    private boolean isAnchor(String[] segs){
        int fflags = Integer.parseInt(segs[4]);
        return (fflags & 0x8) == 0x8;
    }
}
