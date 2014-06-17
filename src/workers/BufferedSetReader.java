/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import dataStructs.BedSet;
import dataStructs.ReadPair;
import dataStructs.SetMap;
import dataStructs.anchorRead;
import dataStructs.divet;
import dataStructs.pairSplit;
import dataStructs.readEnum;
import dataStructs.readNameMappings;
import dataStructs.splitRead;
import file.BedMap;
import gziputils.ReaderReturn;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import setWeightCover.BufferedInitialSet;
import stats.GapOverlap;

/**
 *
 * @author bickhart
 */
public class BufferedSetReader {
    private String chr;
    private int buffer;
    
    private ArrayList<FlatFile> fileEntries = new ArrayList<>();
    private SetMap<BufferedInitialSet> sets = new SetMap<>();
    private SetMap<BufferedInitialSet> finalSets = new SetMap<>();
    
    private int splitcounter = 0;
    private int divetcounter = 0;
    private HashMap<String, ArrayList<splitRead>> soleSplits = new HashMap<>();
    //private HashMap<String, ArrayList<anchorRead>> anchors;
    private readNameMappings anchorMaps = new readNameMappings();
    private GapOverlap gaps;
    
    public BufferedSetReader(String flatFile, String gapFile, String chr, int buffer){
        // First, let's load the data file locations and create the gap intersection
        // tool.
        this.buffer = buffer;
        this.chr = chr;
        this.identifyFiles(flatFile);
        this.createGapOverlapTool(gapFile);
        
        int numLines = this.fileEntries.size();
        int counter = 0;
        
        // Now to loop through all of the variant files and associate values
        // with appropriate initial sets
        for(FlatFile f : this.fileEntries){
            counter++;
            // Load Divet files
            SetMap<BufferedInitialSet> dSet = this.populateDivets(f);
            // Load one end anchors
            HashMap<String, ArrayList<anchorRead>> anchors = this.populateAnchors(f);
            // Now, complex split read logic
            dSet = this.associateSplits(anchors, f, dSet);
            
            for(BedSet s : dSet.getUnsortedBedList(chr)){
                this.sets.checkAndCombineSets((BufferedInitialSet)s);
            }
            int curSets = this.sets.getCountElements(chr);
            
            System.out.print("[RPSR INPUT] Read list item: " + counter + " of " + numLines
                    + "; (D , S): (" + this.divetcounter + " , " + this.splitcounter + "). Current sets: " + curSets + "\r");
        }
        for(BedSet s : this.sets.getUnsortedBedList(chr)){
            this.finalSets.checkAndCombineSets((BufferedInitialSet)s);
        }
        int finSets = this.finalSets.getCountElements(chr);
        System.out.println("[RPSR INPUT] Finished loading all files! Final Sets: " + finSets + ".");
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
                divMaps.addRead(segs[0].trim());
                
                if(!(segs[1].equals(this.chr))){
                    continue;
                }
                if(segs[12].equals("1")){
                    //The divet has a perfect concordant read, so this pair should be ignored
                    continue;
                }else{
                    ReadPair rp = new ReadPair(line, file, readEnum.IsDisc);
                    tempholder.add(rp);
                }
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(ReadPair d : tempholder){
            this.divetcounter++;
            d.setMapCount(divMaps.retMap(d.Name()));
            if(gaps.checkGapOverlap(d)){
                // This read pair spanned a gap! Nothing to see here...
                continue;
            }
            if(!tSet.checkAndCombinePairs(d)){
                BufferedInitialSet temp = new BufferedInitialSet(this.buffer, "InitSet");
                temp.bufferedAdd(d);
                tSet.addBedData(temp);
            }
        }
        //System.out.println("[VHSR INPUT] Finished loading " + this.divets.size() + " discordant read pairs");
        return tSet;
    }
    
    private HashMap<String, ArrayList<anchorRead>> populateAnchors(FlatFile file){
        HashMap<String, ArrayList<anchorRead>> anchors = new HashMap<>();
        BufferedReader anchorReader = ReaderReturn.openFile(file.getAnchor().toFile());
        try{
            String line;
            String[] segs;
            while((line = anchorReader.readLine()) != null){
                segs = line.split("\t");
                if(!(segs[2].equals(this.chr))){
                    continue;
                }
                String end = String.valueOf(Integer.parseInt(segs[3]) + segs[9].length());
                anchorRead aR = new anchorRead(segs[2], segs[3], end, segs[0], segs[1], segs[11], segs[12], segs[10]);
                String clone = getCloneName(segs[0]);
                appendAnchorToConstruct(anchors, aR);
                
                this.anchorMaps.addRead(clone);
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return anchors;
        //System.out.println("[VHSR INPUT] Finished loading " + this.anchors.size() + " anchor keys");
    }
    private void appendAnchorToConstruct(HashMap<String, ArrayList<anchorRead>> map, anchorRead ar){
        String clone = getCloneName(ar.Name());
        if(map.containsKey(clone)){
            map.get(clone).add(ar);
        }else{
            map.put(clone, new ArrayList<anchorRead>());
            map.get(clone).add(ar);
        }
    }
    private SetMap<BufferedInitialSet> associateSplits(HashMap<String, ArrayList<anchorRead>> anchors, FlatFile file, SetMap<BufferedInitialSet> dSet){        
        
        try(SAMFileReader samReader = new SAMFileReader(file.getSplitsam().toFile())){
            SAMRecordIterator iterator = samReader.iterator();
            while(iterator.hasNext()){
                SAMRecord line = iterator.next();
                String ref = line.getReferenceName();
                if(!(line.getReferenceName().equals(this.chr))){
                    continue;
                }
                splitRead sR = new splitRead(
                        this.chr, 
                        line.getAlignmentStart(), 
                        line.getAlignmentEnd(), 
                        line.getReadName(), 
                        line.getFlags(), 
                        line.getIntegerAttribute("NM"), 
                        line.getStringAttribute("MD"), 
                        line.getBaseQualityString()
                );
                appendSplitToConstruct(sR);                    
            }
            for(String clone : anchors.keySet()){
                if(this.soleSplits.containsKey(clone)){
                    ArrayList<pairSplit> temp = pairSplits(anchors.get(clone), clone);
                    if(temp.isEmpty()){
                        //System.err.println("[BUFFSETREADER] Error with split pairing!");
                        continue;
                    }
                    for(pairSplit p : temp){
                        this.splitcounter++;
                        if(gaps.checkGapOverlap(p)){
                            // This read pair spanned a gap! Nothing to see here...
                            continue;
                        }
                        ReadPair work = new ReadPair(p, file, readEnum.IsSplit);
                        work.setMapCount(this.anchorMaps.retMap(clone));
                        if(!dSet.checkAndCombinePairs(work) 
                                && !work.getReadFlags().contains(readEnum.IsUnbalanced)){
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
                if(Math.abs(sArray.get(0).Start() - anchor.Start()) < 500000){
                    
                    splits.add(new pairSplit(anchor, sArray.get(0)));
                }
            }
        }
        
        else if(sArray.size() == 2 && 
                ((sArray.get(0).getSplitNum() && !(sArray.get(1).getSplitNum()))
                || (!(sArray.get(0).getSplitNum()) && sArray.get(1).getSplitNum()))){
            for(anchorRead anchor : aArray){
                // Easy balanced split pairing
                // Just make sure that it isnt too long!
                if(Math.abs(sArray.get(0).Start() - anchor.Start()) < 500000){
                    splits.add(new pairSplit(anchor, sArray.get(0), sArray.get(1)));
                }
            }
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
                        splits.add(new pairSplit(anchor, stemp));
                    }                    
                }
            }else{
                // Worst case scenario: a series of balanced splits
                ArrayList<splitRead> forward = splitSeg.get(true);
                ArrayList<splitRead> reverse = splitSeg.get(false);
                for(anchorRead anchor : aArray){
                    for(splitRead fs : forward){
                        for(splitRead rs: reverse){
                            if(Math.abs(fs.Start() - anchor.Start()) < 500000 &&
                                    Math.abs(rs.Start() - anchor.Start()) < 500000){
                                splits.add(new pairSplit(anchor, fs, rs));
                            }
                        }
                    }
                }
            }
        }
        return splits;
    }
    private void appendSplitToConstruct(splitRead sr){
        String clone = getCloneName(sr.Name());
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
    
    private void identifyFiles(String file){
        try(BufferedReader input = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"))){
            String line;
            while((line = input.readLine()) != null){
                FlatFile flat = new FlatFile(line);
                this.fileEntries.add(flat);
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void createGapOverlapTool(String gapFile){
        this.gaps = new GapOverlap(gapFile);
        System.out.println("[VHSR INPUT] Finished loading gap file: " + gapFile);
    }
    private String getCloneName(String readName){
        String clone;
        String[] nameSplit = readName.split("[/_]");
        clone = nameSplit[0];
        return clone;
    }
}
