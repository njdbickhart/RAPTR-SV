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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.sf.samtools.BAMIndexer;
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
    private final HashMap<String, Double[]> values = new HashMap<>();
    private final HashMap<String, ArrayList<Integer>> insertSizes = new HashMap<>();
    private SAMFileHeader header;
    private final boolean expectRG;
    
    /**
     * Object constructor
     * @param hasRG This is a boolean flag telling the MetadataGenerator to search the 
     * BAM file for read groups and to treat them separately. A value of "false" will
     * ignore all readgroups in the file.
     */
    public BamMetadataGeneration(boolean hasRG){
        expectRG = hasRG;
    }
    
    /**
     * This is a main, workhorse method designed to run through the BAM file and
     * calculate the read group average and stdev values.
     * @param input The string of the path to the BAM file to scan
     * @param samplimit A limit on the number of BAM file entries per readgroup
     * to sample statistics from. Higher limits require more memory.
     */
    public void ScanFile(String input, int samplimit){
        SAMFileReader sam = new SAMFileReader(new File(input));
        sam.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        if(!sam.hasIndex()){
            System.out.println("[METADATA] Could not find bam index file. Creating one now...");
            BAMIndexer b = new BAMIndexer(new File(input + ".bai"), sam.getFileHeader());
            sam.enableFileSource(true);
            sam.iterator().forEachRemaining((s) -> {
                b.processAlignment(s);});
            b.finish();
            sam.close();
            System.out.println("[METADATA] Finished with bam index generation.");
            sam = new SAMFileReader(new File(input));
        }
        //sam.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
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
            
            int rFlags = s.getFlags();
            // check if this is a proper pair to ensure that we dont get all zeroes!
            if((rFlags & 0x4) == 0x4 || (rFlags & 0x8) == 0x8 || !((rFlags & 0x1) == 0x1))
                continue;
            
            // If the insert size is zero, skip it
            if(s.getInferredInsertSize() == 0)
                continue;
            
            if(!insertSizes.containsKey(rgid))
                insertSizes.put(rgid, new ArrayList<>(samplimit));
            if(insertSizes.get(rgid).size() >= samplimit){
                if(checkIfSamplingDone(samplimit))
                    break;
            }else{
                insertSizes.get(rgid).add(Math.abs(s.getInferredInsertSize()));
            }
        }
        itr.close();
        sam.close();
    }

    /**
     * This method creates the Divet Output factories needed for discordant
     * read pairs
     * @param outbase The String representing the path to the output files
     * @return A Map of all Divet output factories. Key: the read group, Value:
     * the divet output factory class
     */
    public Map<String, DivetOutputHandle> generateDivetOuts(String outbase){
        Map<String, DivetOutputHandle> holder = new ConcurrentHashMap<>();
        this.rgList.stream().forEach((s) -> {
            holder.put(s, new DivetOutputHandle(outbase + "." + s + ".divet"));
            holder.get(s).OpenHandle();
        });
        return holder;
    }
    
    /**
     * This method generates the Split read output factories needed for putative
     * split read entries
     * @param outbase The String representing the path to the output files
     * @return A Map of all Split output factories. Key: the read group, Value:
     * the split output factory class
     */
    public Map<String, SplitOutputHandle> generateSplitOuts(String outbase){
        Map<String, SplitOutputHandle> holder = new ConcurrentHashMap<>();
        this.rgList.stream().forEach((s) -> {
            holder.put(s, new SplitOutputHandle(outbase + "." + s + ".split.fq", outbase + "." + s + ".anchor.bam", header));
            holder.get(s).OpenAnchorHandle();
        });
        return holder;
    }

    /**
     * This function is designed to retrieve the metadata created by this class
     * for use in subsequence paired end discordancy discovery
     * @param maxdist The maximum distance on the same chromosome to limit paired end
     * discordant read detection
     * @return A map of the threshold values for each read group. Map Key: 
     * the read group. Value: an integer array where the first value is the lower
     * threshold for read lengths (insertion detection) and the second value is
     * the upper threshold for detection (deletion detection).
     */
    public Map<String, Integer[]> getThresholds(int maxdist){
        // First value is the lower threshold (avg - 3 std)
        // second value is the higher threshold (avg + 3 std)
        // third is the maxdist
        Map<String, Integer[]> vs = new HashMap<>();
        // Due to some issues where repeats map all over the reference genome
        // I'm going to have to alter this routine to take a median estimation 
        // and a MAD 
        this.insertSizes.keySet().stream().forEach((r) -> {
            int median = stats.MedianAbsoluteDeviation.Median(this.insertSizes.get(r));
            int mad = stats.MedianAbsoluteDeviation.MAD(this.insertSizes.get(r));
            List<Integer> filtered = this.insertSizes.get(r).stream()
                    .filter(s -> s < median + (mad * 20))
                    .collect(Collectors.toList());
            
            long fvalues = this.insertSizes.get(r).parallelStream()
                    .filter(s -> s > median + (mad * 20))
                    .count();
            
            if(fvalues > 0)
                System.err.println("[METADATA] Identified " + fvalues + " sampled insert lengths greater than a Median (" + median + ") * 10 MAD (" + mad + ") in RG:" + r);
            
            double avg = stats.StdevAvg.IntAvg(filtered);
            double stdev = stats.StdevAvg.stdevInt(avg, filtered);
            Double[] d = {avg, stdev};
            this.values.put(r, d);
            int lower = (int) Math.round(avg - (3 * stdev));
            int upper = (int) Math.round(avg + (3 * stdev));
            if(lower < 0)
                lower = 0;
            Integer[] v = {lower, upper, maxdist};
            vs.put(r, v);
        });
        return vs;
    }
    private boolean checkIfSamplingDone(int samplimit){      
        int num = this.rgList.stream()
                .filter((r) -> (insertSizes.containsKey(r)))
                .filter((r) -> (insertSizes.get(r).size() >= samplimit))
                .map((r) -> 1)
                .reduce(0, (sum, next) -> sum + next);
        
        return num >= this.rgList.size();
    }

    /**
     * This getter retrieves the average insert size of the read group
     * @param key The readgroup string
     * @return The average insert size of the library
     */
    public double getSampleInsSize(String key){
        return this.values.get(key)[0];
    }

    /**
     * This getter retrieves the stdev of the insert size of the read group
     * @param key The readgroup string
     * @return The stdev of the library
     */
    public double getSampleInsStd(String key){
        return this.values.get(key)[1];
    }

    /**
     * This getter returns the list of all read groups found in this BAM
     * @return An ArrayList String containing all of the read group names
     */
    public List<String> getSampleIDs(){
        return this.rgList;
    }

    /**
     * This getter returns the SAMFileHeader extracted from this BAM file
     * @return A SAMJDK SAMFileHeader object
     */
    public SAMFileHeader getSamFileHeader(){
        return this.header;
    }
}
