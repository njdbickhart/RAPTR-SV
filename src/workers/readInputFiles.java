/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import dataStructs.anchorRead;
import dataStructs.divet;
import dataStructs.pairSplit;
import dataStructs.readNameMappings;
import dataStructs.splitRead;
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
import stats.GapOverlap;

/**
 *
 * @author bickhart
 */
public class readInputFiles {
    private String chr;
    
    private ArrayList<FlatFile> fileEntries;
    private ArrayList<pairSplit> splits; 
    private ArrayList<divet> divets;
    private int splitcounter;
    private int divetcounter;
    private HashMap<String, ArrayList<splitRead>> soleSplits;
    //private HashMap<String, ArrayList<anchorRead>> anchors;
    private readNameMappings anchorMaps;
    private GapOverlap gaps;
    private Finder samFinder;
    
    public readInputFiles(String flatFile, String gapFile, String chr){
        this.chr = chr;
        this.fileEntries = new ArrayList<>();
        this.splitcounter = 0;
        this.divetcounter = 0;
        identifyFiles(flatFile);
        
        createGapOverlapTool(gapFile);
        
        // Initialize all fields
        //this.anchors = new HashMap<>();
        this.anchorMaps = new readNameMappings();        
        this.divets = new ArrayList<>();
        this.soleSplits = new HashMap<>();
        this.splits = new ArrayList<>();
        
        int numLines = this.fileEntries.size();
        int counter = 0;
        for(FlatFile f : this.fileEntries){
            counter++;
            populateDivets(f);

            HashMap<String, ArrayList<anchorRead>> anchors = populateAnchors(f);

            associateSplits(anchors, f);
            System.out.print("[VHSR INPUT] Loaded list item:\t" + counter +" of " + numLines + "; (D, S): (" 
                    + this.divets.size() + "," + this.splits.size() + ")\r");
        }
        
        System.out.println(System.lineSeparator() + "[VHSR INPUT] Finished loading all files");
    }
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
    private void populateDivets(FlatFile file){
        readNameMappings divMaps = new readNameMappings();
        ArrayList<divet> tempholder = new ArrayList<>();
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
                    divet vh = new divet(line, file);
                    tempholder.add(vh);
                }
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(divet d : tempholder){
            d.setMapping(divMaps.retMap(d.Name()));
            this.divets.add(d);
        }
        //System.out.println("[VHSR INPUT] Finished loading " + this.divets.size() + " discordant read pairs");
    }
    private void associateSplits(HashMap<String, ArrayList<anchorRead>> anchors, FlatFile file){        
        
        String segs[];
        
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
                    pairSplits(anchors.get(clone), clone);
                }
            }
        }catch(Exception ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(pairSplit s : this.splits){
            
            s.setMappings(this.anchorMaps.retMap(s.Name()));
        }
        
        //System.out.println("[VHSR INPUT] Finished loading " + this.splits.size() + " paired splits");
    }
    private void pairSplits(ArrayList<anchorRead> aArray, String clone){
        ArrayList<splitRead> sArray = this.soleSplits.get(clone);
        
        
        if(sArray.size() == 1){
            for(anchorRead anchor : aArray){
                // Easy unbalanced split pairing
                // Just make sure that it isnt too long!
                if(Math.abs(sArray.get(0).Start() - anchor.Start()) < 500000){
                    this.splits.add(new pairSplit(anchor, sArray.get(0)));
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
                    this.splits.add(new pairSplit(anchor, sArray.get(0), sArray.get(1)));
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
                        this.splits.add(new pairSplit(anchor, stemp));
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
                                this.splits.add(new pairSplit(anchor, fs, rs));
                            }
                        }
                    }
                }
            }
        }
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
    private void appendSplitToConstruct(splitRead sr){
        String clone = getCloneName(sr.Name());
        if(this.soleSplits.containsKey(clone)){
            this.soleSplits.get(clone).add(sr);
        }else{
            this.soleSplits.put(clone, new ArrayList<splitRead>());
            this.soleSplits.get(clone).add(sr);
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
    
    /*
     * Getters
     */
    public ArrayList<pairSplit> PairSplit(){
        return this.splits;
    }
    public ArrayList<divet> Divet(){
        return this.divets;
    }
    public GapOverlap Gaps(){
        return this.gaps;
    }
    public readNameMappings ReadNameMappings(){
        return this.anchorMaps;
    }
}
