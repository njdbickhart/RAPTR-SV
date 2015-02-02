/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import dataStructs.ReadPair;
import dataStructs.SetMap;
import dataStructs.anchorRead;
import dataStructs.pairSplit;
import dataStructs.readEnum;
import dataStructs.readNameMappings;
import dataStructs.splitRead;
import gziputils.ReaderReturn;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import setWeightCover.BufferedInitialSet;
import stats.GapOverlap;
import stats.ReadNameUtility;

/**
 *
 * @author bickhart
 */
public class BufferedSetReader {
    private final String chr;
    private final int buffer;
    
    private final ArrayList<FlatFile> fileEntries;
    private final SetMap<BufferedInitialSet> sets = new SetMap<>();
    private SetMap<BufferedInitialSet> finalSets = new SetMap<>();
    
    private int splitcounter = 0;
    private int divetcounter = 0;
    private final HashMap<String, ArrayList<splitRead>> soleSplits = new HashMap<>();
    //private HashMap<String, ArrayList<anchorRead>> anchors;
    private final readNameMappings anchorMaps = new readNameMappings();
    private GapOverlap gaps;
    private boolean useGapOverlap = false;
    private final ReadNameUtility rn = new ReadNameUtility();
    private final double pfilter;
    
    private int unbal = 0, easBal = 0, hardUnbal = 0, hardBal = 0;
    
    private static final Logger log = Logger.getLogger("");
    
    public BufferedSetReader(ArrayList<FlatFile> files, String gapFile, String chr, int buffer, double pfilter){
        // First, let's load the data file locations and create the gap intersection
        // tool.
        this.buffer = buffer;
        this.chr = chr;
        this.fileEntries = files;
        this.createGapOverlapTool(gapFile);
        this.pfilter = pfilter;
        
        int numLines = this.fileEntries.size();
        int counter = 0;
        
        final SetMap<BufferedInitialSet> dSet = this.fileEntries.parallelStream()
                .map((s) -> {
                    log.log(Level.INFO, "[BUFFSET] Working on Flatfile divet file: " + s.getDivet().toString());
                    return this.populateDivets(s);})
                .sequential()
                .reduce(new SetMap<>(), (a, b) -> {
                   a.checkAndCombineMaps(b, chr);
                   log.log(Level.INFO, "[BUFFSET] Divet reduction produced number of sets: " + a.getUnsortedBedList(chr).size());
                   return a;
                });
                
        int divsets = dSet.getCountElements(chr);
        System.out.println("[RAPTR-SV INPUT] Finished loading divet sets. Identified: " + divsets + " discordant sets");
        log.log(Level.INFO, "[BUFFSET] Loaded " + divsets + " divet sets in total.");
        
        System.gc();
        
        this.finalSets = this.fileEntries.parallelStream()
                .map((s) -> {
                    log.log(Level.INFO, "[BUFFSET] Working on Flatfile split file: " + s.getSplitsam().toString());
                    HashMap<String, ArrayList<anchorRead>> anchors = this.populateAnchors(s);
                    return this.associateSplits(anchors, s);})
                .sequential()
                .reduce(dSet, (a, b) -> {
                    a.checkAndCombineMaps(b, chr);
                    log.log(Level.INFO, "[BUFFSET] Split reduction produced number of sets: " + a.getUnsortedBedList(chr).size());
                    return a;
                });
        
        
        System.gc();
        // Now to loop through all of the variant files and associate values
        // with appropriate initial sets
        /*for(FlatFile f : this.fileEntries){
        counter++;
        // Load Divet files
        SetMap<BufferedInitialSet> dSet = this.populateDivets(f);
        // Load one end anchors
        // I am making a change to the logic here as the anchor reads are of minimal value apart from orienting proper splits
        HashMap<String, ArrayList<anchorRead>> anchors = this.populateAnchors(f);
        // Now, complex split read logic
        dSet = this.associateSplits(anchors, f, dSet);
        anchors.clear();
        
        for(BedSet s : dSet.getUnsortedBedList(chr)){
        this.sets.checkAndCombineSets((BufferedInitialSet)s);
        
        }
        int curSets = this.sets.getCountElements(chr);
        
        System.out.print("[RPSR INPUT] Read list item: " + counter + " of " + numLines
        + "; (D , S): (" + this.divetcounter + " , " + this.splitcounter + "). Current sets: " + curSets + "\r");
        }*/
        /*System.out.println();
        for(BedSet s : this.sets.getUnsortedBedList(chr)){
        this.finalSets.checkAndCombineSets((BufferedInitialSet)s);
        
        }*/
        int finSets = this.finalSets.getCountElements(chr);
        System.out.println("[RAPTR-SV INPUT] Finished loading all files! Final Sets: " + finSets + ".");
        log.log(Level.INFO, "[BUFFSET] Final number of sets: " + finSets);
    }
    /*
     * Getters
     */
    public SetMap<BufferedInitialSet> getMap(){
        return this.finalSets;
    }
    
    /*
     * Loader methods
     */
    private SetMap<BufferedInitialSet> populateDivets(FlatFile file){
        SetMap<BufferedInitialSet> tSet = new SetMap<>();
        readNameMappings divMaps = new readNameMappings();
        ArrayList<ReadPair> tempholder = new ArrayList<>();
        BufferedReader divetReader = ReaderReturn.openFile(file.getDivet().toFile());
        try{
            String line;
            String[] segs;
            while((line = divetReader.readLine()) != null){
                segs = line.split("\t");
                if(segs.length < 13)
                    continue;
                
                if(!(segs[1].equals(this.chr))){
                    continue;
                }
                divMaps.addRead(segs[0].trim());
                if(!segs[13].equals("1") && Double.valueOf(segs[12]) > pfilter){
                    ReadPair rp = new ReadPair(line, file, readEnum.IsDisc);
                    tempholder.add(rp);
                }
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int gapCount = 0;
        for(ReadPair d : tempholder){
            //this.divetcounter++;
            d.setMapCount(divMaps.retMap(d.Name()));
            if(this.useGapOverlap){
                if(gaps.checkGapOverlap(d)){
                    // This read pair spanned a gap! Nothing to see here...
                    gapCount++;
                    continue;
                }
            }
            if(!tSet.checkAndCombinePairs(d)){
                BufferedInitialSet temp = new BufferedInitialSet(this.buffer, "InitSet");
                temp.bufferedAdd(d);
                tSet.addBedData(temp);
            }
        }
        //System.out.println("[VHSR INPUT] Finished loading " + this.divets.size() + " discordant read pairs");
        log.log(Level.INFO, "[BUFFSET] Identified " + gapCount + " gap file overlaps.");
        return tSet;
    }
    
    private HashMap<String, ArrayList<anchorRead>> populateAnchors(FlatFile file){
        HashMap<String, ArrayList<anchorRead>> anchors = new HashMap<>();
        //BufferedReader anchorReader = ReaderReturn.openFile(file.getAnchor().toFile());
        SAMFileReader sam = new SAMFileReader(file.getAnchor().toFile());
        sam.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        //try{
            //String line;
            SAMRecordIterator itr = sam.iterator();
            while(itr.hasNext()){
                SAMRecord rec = itr.next();
                if(!(rec.getReferenceName().equals(this.chr))){
                    continue;
                }
                anchorRead aR = new anchorRead(rec.getReferenceName(),
                    rec.getAlignmentStart(),
                    rec.getFlags(),
                    rec.getMappingQuality());
                //String clone = rn.GetCloneName(segs[0], Integer.valueOf(segs[1]));
                appendAnchorToConstruct(anchors, aR, rec.getReadName());
                
                this.anchorMaps.addRead(rec.getReadName());
            }
        //}catch(IOException ex){
            //Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        //}
            log.log(Level.FINE, "[INPUT] " + file.getAnchor().toString() + " Currently holding " + this.anchorMaps.getSize() + " anchor read name mappings");
        return anchors;
        //System.out.println("[VHSR INPUT] Finished loading " + this.anchors.size() + " anchor keys");
    }
    private void appendAnchorToConstruct(HashMap<String, ArrayList<anchorRead>> map, anchorRead ar, String clone){
        if(map.containsKey(clone)){
            map.get(clone).add(ar);
        }else{
            map.put(clone, new ArrayList<anchorRead>());
            map.get(clone).add(ar);
        }
    }
    private SetMap<BufferedInitialSet> associateSplits(HashMap<String, ArrayList<anchorRead>> anchors, FlatFile file){        
        SetMap<BufferedInitialSet> tSet = new SetMap<BufferedInitialSet>();
        
        // Set diagnostic counter variables
        int splitCnt = 0;
        int gapRmv = 0;
        this.unbal = 0;
        this.easBal = 0;
        this.hardBal = 0;
        this.hardUnbal = 0;
        
        try(SAMFileReader samReader = new SAMFileReader(file.getSplitsam().toFile())){
            SAMRecordIterator iterator = samReader.iterator();
            while(iterator.hasNext()){
                SAMRecord line = iterator.next();
                String ref = line.getReferenceName();
                if(!(ref.equals(this.chr))){
                    continue;
                }
                splitRead sR = new splitRead(
                        ref, 
                        line.getAlignmentStart(), 
                        line.getAlignmentEnd(), 
                        line.getReadName(), 
                        line.getFlags()
                );
                appendSplitToConstruct(sR, rn.GetCloneName(sR.Name(), line.getFlags()));                    
            }
            for(String clone : anchors.keySet()){
                if(this.soleSplits.containsKey(clone)){
                    ArrayList<pairSplit> temp = pairSplits(anchors.get(clone), clone);
                    if(temp.isEmpty()){
                        //System.err.println("[BUFFSETREADER] Error with split pairing!");
                        continue;
                    }
                    // Sorting so that balanced splits appear first
                    Collections.sort(temp);
                    for(pairSplit p : temp){
                        //this.splitcounter++;
                        splitCnt++;
                        if(this.useGapOverlap){
                            if(gaps.checkGapOverlap(p)){
                                gapRmv++;
                                // This read pair spanned a gap! Nothing to see here...
                                continue;
                            }
                        }
                        ReadPair work = new ReadPair(p, file, readEnum.IsSplit);
                        work.setMapCount(this.anchorMaps.retMap(clone));
                        if(!tSet.checkAndCombinePairs(work)){
                            if(work.getReadFlags().contains(readEnum.IsUnbalanced))
                                continue;
                            // This balanced split pair does not intersect any known set
                            // Time to create a new set for it
                            BufferedInitialSet set = new BufferedInitialSet(this.buffer, "InitSet");
                            set.addReadPair(work);
                            tSet.addBedData(set);
                        }
                    }
                }
            }
            this.soleSplits.clear();
        }catch(Exception ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        log.log(Level.FINE, "[INPUT] " + file.getSplitsam().toString() + " Split read statistics; unbal: " + this.unbal + " bal: " + this.easBal + " hardUnbal: " + this.hardUnbal + " hardBal: " + this.hardBal );
        log.log(Level.FINE, "[INPUT] " + file.getSplitsam().toString() + " Identified " + splitCnt + " split mappings and removed " + gapRmv + " due to gap filtration.");
        //System.out.println("[VHSR INPUT] Finished loading " + this.splits.size() + " paired splits");
        return tSet;
    }
    private SetMap<BufferedInitialSet> associateSplits(HashMap<String, ArrayList<anchorRead>> anchors, FlatFile file, SetMap<BufferedInitialSet> dSet){        
        
        try(SAMFileReader samReader = new SAMFileReader(file.getSplitsam().toFile())){
            SAMRecordIterator iterator = samReader.iterator();
            while(iterator.hasNext()){
                SAMRecord line = iterator.next();
                String ref = line.getReferenceName();
                if(!(ref.equals(this.chr))){
                    continue;
                }
                splitRead sR = new splitRead(
                        ref, 
                        line.getAlignmentStart(), 
                        line.getAlignmentEnd(), 
                        line.getReadName(), 
                        line.getFlags()
                );
                appendSplitToConstruct(sR, rn.GetCloneName(sR.Name(), line.getFlags()));                    
            }
            for(String clone : anchors.keySet()){
                if(this.soleSplits.containsKey(clone)){
                    ArrayList<pairSplit> temp = pairSplits(anchors.get(clone), clone);
                    if(temp.isEmpty()){
                        //System.err.println("[BUFFSETREADER] Error with split pairing!");
                        continue;
                    }
                    // Sorting so that balanced splits appear first
                    Collections.sort(temp);
                    for(pairSplit p : temp){
                        this.splitcounter++;
                        if(gaps.checkGapOverlap(p)){
                            // This read pair spanned a gap! Nothing to see here...
                            continue;
                        }
                        ReadPair work = new ReadPair(p, file, readEnum.IsSplit);
                        work.setMapCount(this.anchorMaps.retMap(clone));
                        if(!dSet.checkAndCombinePairs(work)){
                            if(work.getReadFlags().contains(readEnum.IsUnbalanced))
                                continue;
                            // This balanced split pair does not intersect any known set
                            // Time to create a new set for it
                            BufferedInitialSet set = new BufferedInitialSet(this.buffer, "InitSet");
                            set.addReadPair(work);
                            dSet.addBedData(set);
                        }
                    }
                }
            }
            this.soleSplits.clear();
        }catch(Exception ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //System.out.println("[VHSR INPUT] Finished loading " + this.splits.size() + " paired splits");
        return dSet;
    }
    private ArrayList<pairSplit> pairSplits(ArrayList<anchorRead> aArray, String clone){
        ArrayList<splitRead> sArray = this.soleSplits.get(clone);
        ArrayList<pairSplit> splits = new ArrayList<>();
        
        
        
        if(sArray.size() == 1){
            for(anchorRead anchor : aArray){
                // Easy unbalanced split pairing
                // Just make sure that it isnt too long!
                if(anchorConsistency(anchor.chr, sArray.get(0).Chr(), anchor.start, sArray.get(0).Start())){
                    unbal++;
                    splits.add(new pairSplit(anchor, sArray.get(0), clone));
                }
            }
        }
        
        else if(sArray.size() == 2 && 
                ((sArray.get(0).getSplitNum() && !(sArray.get(1).getSplitNum()))
                || (!(sArray.get(0).getSplitNum()) && sArray.get(1).getSplitNum()))){
            for(anchorRead anchor : aArray){
                // Easy balanced split pairing
                // Just make sure that it isnt too long!
                // Added in a logical test to ensure that the split read isn't 5 bp long
                // INDEL detection is best done at the alignment stage
                if(anchorConsistency(anchor.chr, sArray.get(0).Chr(), anchor.start, sArray.get(0).Start())
                        && subtractClosest(sArray.get(0).Start(), sArray.get(0).End(), 
                                sArray.get(1).Start(), sArray.get(1).End()) > 5){
                    easBal++;
                    splits.add(new pairSplit(anchor, sArray.get(0), sArray.get(1), clone));
                    /*if(sArray.get(0).Start() <= 20707037 && sArray.get(0).Start() >= 20706839
                    && sArray.get(1).Start() <= 21002974 && sArray.get(1).End() >= 21002776){
                    System.out.println("Deletion split pair: " + sArray.get(0).toString() + ";" + sArray.get(1).toString());
                    }*/
                }
            }
        }
        
        else if(sArray.size() >= 30){
            // this is a likely repetitive read mapping, so we're going to ignore it
            // and hope it goes away!
        }
        
        else{
            // Gotta do this the hard way
            // Separate splits into map of true and false segments
            HashMap<Boolean, ArrayList<splitRead>> splitSeg = new HashMap<>();
            for(splitRead sr : sArray){
                if(splitSeg.containsKey(sr.getSplitNum())){
                    splitSeg.get(sr.getSplitNum()).add(sr);
                }else{
                    splitSeg.put(sr.getSplitNum(), new ArrayList<splitRead>());
                    splitSeg.get(sr.getSplitNum()).add(sr);
                }
            }
            Set<Boolean> splitNums = splitSeg.keySet();
            if(splitNums.size() == 1){
                // Just a series of unbalanced splits
                ArrayList<splitRead> temp = splitSeg.get(splitNums.iterator().next());
                for(anchorRead anchor : aArray){
                    for(splitRead stemp : temp){
                        hardUnbal++;
                        splits.add(new pairSplit(anchor, stemp, clone));
                    }                    
                }
            }else{
                // Worst case scenario: a series of balanced splits
                ArrayList<splitRead> forward = splitSeg.get(true);
                ArrayList<splitRead> reverse = splitSeg.get(false);
                for(anchorRead anchor : aArray){
                    for(splitRead fs : forward){
                        for(splitRead rs: reverse){
                            if((anchorConsistency(anchor.chr, fs.Chr(), anchor.start, fs.Start()) ||
                                    anchorConsistency(anchor.chr, rs.Chr(), anchor.start, rs.Start()))
                                    && subtractClosest(fs.Start(), fs.End(), 
                                        rs.Start(), rs.End()) > 5){
                                splits.add(new pairSplit(anchor, fs, rs, clone));
                                hardBal++;
                                /*if(sArray.get(0).Start() <= 20707037 && sArray.get(0).Start() >= 20706839
                                && sArray.get(1).Start() <= 21002974 && sArray.get(1).End() >= 21002776){
                                System.out.println("Deletion split pair: " + sArray.get(0).toString() + ";" + sArray.get(1).toString());
                                }*/
                            }
                        }
                    }
                }
            }
        }
        
        return splits;
    }
    
    private boolean anchorConsistency(String achr, String schr, int astart, int sstart){
        if(achr.equals(schr)){
            return (Math.abs(sstart - astart) < 1000000);
        }
        return false;
    }
    
    private int subtractClosest(int s1, int e1, int s2, int e2){
        if(Math.abs(s1 - e2) > Math.abs(s2 - e1))
            return Math.abs(s2 - e1);
        else
            return Math.abs(s1 - e2);
    }
    
    private void appendSplitToConstruct(splitRead sr, String clone){
        if(this.soleSplits.containsKey(clone)){
            this.soleSplits.get(clone).add(sr);
        }else{
            this.soleSplits.put(clone, new ArrayList<splitRead>());
            this.soleSplits.get(clone).add(sr);
        }
    }
    /*
     * Private housekeeping methods
     */
    
    
    private void createGapOverlapTool(String gapFile){
        if(gapFile.equals("NULL")){
            log.log(Level.FINE, "[INPUT] Not running gap file filtration.");
            System.out.println("[RAPTR-SV INPUT] Not using Gap filtration for this dataset.");
            return;
        }
        log.log(Level.FINE, "[INPUT] Using gap filtration. Input file is: " + gapFile);
        this.gaps = new GapOverlap(gapFile);
        System.out.println("[RAPTR-SV INPUT] Finished loading gap file: " + gapFile);
        this.useGapOverlap = true;
    }
}
