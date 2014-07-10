/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

import dataStructs.DivetOutputHandle;
import dataStructs.SplitOutputHandle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author bickhart
 */
public class BamMetadataGeneration {
    private final List<String> rgList = new ArrayList<>();
    private HashMap<String, ArrayList<Integer>> insertSizes = new HashMap<>();
    private SAMFileHeader header;
    private final boolean expectRG;
    
    public BamMetadataGeneration(boolean hasRG){
        expectRG = hasRG;
    }
    
    public void ScanFile(String input, int samplimit){
        SAMFileReader sam = new SAMFileReader(new File(input));
        header = sam.getFileHeader();
        if(expectRG){
            
            List<SAMReadGroupRecord> temp = header.getReadGroups();
            temp.forEach((e) -> rgList.add(e.getId()));
        }else
            rgList.add("D");
        
        SAMRecordIterator itr = sam.iterator();
        while(itr.hasNext()){
            SAMRecord s = itr.next();
            String rgid;
            if(expectRG)
                rgid = s.getReadGroup().getId();
            else
                rgid = "D";
            
            if(!insertSizes.containsKey(rgid))
                insertSizes.put(rgid, new ArrayList<>(samplimit));
            if(insertSizes.get(rgid).size() >= samplimit){
                if(checkIfSamplingDone(samplimit))
                    break;
            }else
                insertSizes.get(rgid).add(s.getInferredInsertSize());
        }
        itr.close();
        sam.close();
    }
    public Map<String, DivetOutputHandle> generateDivetOuts(String outbase){
        Map<String, DivetOutputHandle> holder = new HashMap<>();
        this.rgList.stream().forEach((s) -> {
            holder.put(s, new DivetOutputHandle(outbase + "." + s + ".divet"));
        });
        return holder;
    }
    
    public Map<String, SplitOutputHandle> generateSplitOuts(String outbase){
        Map<String, SplitOutputHandle> holder = new HashMap<>();
        this.rgList.stream().forEach((s) -> {
            holder.put(s, new SplitOutputHandle(outbase + "." + s + ".split.fq", outbase + "." + s + ".anchor.bam", header));
        });
        return holder;
    }
    public Map<String, Integer[]> getThresholds(int maxdist){
        // First value is the lower threshold (avg - 3 std)
        // second value is the higher threshold (avg + 3 std)
        // third is the maxdist
        Map<String, Integer[]> values = new HashMap<>();
        this.insertSizes.keySet().stream().forEach((r) -> {
            double avg = stats.StdevAvg.IntAvg(this.insertSizes.get(r));
            double stdev = stats.StdevAvg.stdevInt(avg, this.insertSizes.get(r));
            int lower = (int) Math.round(avg - (3 * stdev));
            int upper = (int) Math.round(avg + (3 * stdev));
            if(lower < 0)
                lower = 0;
            Integer[] v = {lower, upper, maxdist};
            values.put(r, v);
        });
        return values;
    }
    private boolean checkIfSamplingDone(int samplimit){      
        int num = this.rgList.stream()
                .filter((r) -> (insertSizes.containsKey(r)))
                .filter((r) -> (insertSizes.get(r).size() >= samplimit))
                .map((r) -> 1)
                .reduce(0, (sum, next) -> sum + next);
        
        return num >= this.rgList.size();
    }
}
