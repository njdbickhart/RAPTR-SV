/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import dataStructs.divet;
import dataStructs.pairSplit;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import setWeightCover.BufferedInitialSet;
import stats.GapOverlap;

/**
 *
 * @author bickhart
 */
public class BufferedSetReader {
    private String chr;
    
    private ArrayList<FlatFile> fileEntries = new ArrayList<>();
    private BedMap<BufferedInitialSet> sets;
    private ArrayList<pairSplit> splits; 
    private ArrayList<divet> divets;
    private int splitcounter = 0;
    private int divetcounter = 0;
    private HashMap<String, ArrayList<splitRead>> soleSplits;
    //private HashMap<String, ArrayList<anchorRead>> anchors;
    private readNameMappings anchorMaps;
    private GapOverlap gaps;
    private Finder samFinder;
    
    public BufferedSetReader(String flatFile, String gapFile, String chr){
        // First, let's load the data file locations and create the gap intersection
        // tool.
        this.identifyFiles(flatFile);
        this.createGapOverlapTool(gapFile);
        
        int numLines = this.fileEntries.size();
        int counter = 0;
        
        // Now to loop through all of the variant files and associate values
        // with appropriate initial sets
        for(FlatFile f : this.fileEntries){
            counter++;
            
            System.out.print("[RPSR INPUT] Read list item: " + counter + " of " + numLines
                    + "; (D , S): (" + this.divetcounter + " , " + this.splitcounter + "\r");
        }
        System.out.println("[RPSR INPUT] Finished loading all files!");
    }
    
    /*
     * Loader methods
     */
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
