/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import dataStructs.divet;
import dataStructs.pairSplit;
import dataStructs.readNameMappings;
import dataStructs.splitRead;
import java.util.ArrayList;
import java.util.HashMap;
import stats.GapOverlap;

/**
 *
 * @author bickhart
 */
public class BufferedSetReader {
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
    
    public BufferedSetReader(String flatFile, String gapFile, String chr){
        
    }
}
