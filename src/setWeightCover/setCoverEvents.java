/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import dataStructs.callEnum;
import dataStructs.divet;
import dataStructs.pairSplit;
import dataStructs.readNameMappings;
import file.BedAbstract;
import file.BedFileException;
import file.BedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import stats.GapOverlap;
import stats.MergerUtils;


/**
 *
 * @author derek.bickhart
 */
public class setCoverEvents {
    private BedMap<InitialSet> setMap;
    private ArrayList<InitialSet> setList; // Container for initial sets without set weight cover
    private ArrayList<pairSplit> splits; // bedmap of pairSplits
    private ArrayList<divet> divets;
    private GapOverlap gaps;
    private readNameMappings names;
    private boolean conflicts;
    
    public setCoverEvents(ArrayList<pairSplit> splits, ArrayList<divet> divets, GapOverlap gaps, readNameMappings readList, String chr){
        this.divets = divets;
        this.splits = splits;
        this.gaps = gaps;
        this.names = readList;
        this.setMap = new BedMap<InitialSet>();
        this.conflicts = false;
        try{
            setEventsSplit(); // add Splits
            setEventsDisc(); // add discords
        }catch(BedFileException ex){
            ex.printStackTrace();
        }
        this.setList = MergeSets(chr); // Iterate through the list refining possible overlaps
        
        // Conflict detection and sorting
        /*if(IdentifyConflicts()){
         * System.out.println("[VHSR SETCOVER] Identified putative conflicts; resolving events");
         * SpecificMerge();
         * }*/
        System.out.println("[VHSR SETCOVER] Finished placing reads into sets");
    }
    
    private void setEventsSplit() throws BedFileException{        
        
        //this.setList = new ArrayList<>();
        int num = 0;
        
        for(int i = 0; i < this.splits.size(); i++){


            if(this.gaps.checkGapOverlap(this.splits.get(i))){
                continue; //Intersected a gap
            }
            if(this.splits.get(i).AvgProb() < 0.001d){
                continue; //Average mapping quality was bad for the reads involved
            }

            // Read passed filter; now to add mapping frequency and calculate weight
            pairSplit working = this.splits.get(i);
            String cloneName = working.Name();
            int maps = this.names.retMap(cloneName);
            working.setMappings(maps);

            // Add set to list if list is empty
            if(this.setMap.isEmpty()){
                if(working.isBalanced()){
                    this.setMap.addBedData(new InitialSet(working));
                }else{
                    this.setMap.addBedData(new InitialSet(working, true));
                }
                num++;
                continue;
            }

            // Check to see if there is an overlap with existing list elements
            boolean foundMatch = false;
            for(String chr : this.setMap.getListChrs()){
                for(int bins : utils.BinBed.getBins(working.Start(), working.End())){
                    if(this.setMap.containsBin(chr, bins)){
                        ArrayList<InitialSet> beds = this.setMap.getBedAbstractList(chr, bins);
                        for(int x = 0; x < beds.size(); x++){
                            InitialSet iSet = (InitialSet) beds.get(x);
                            if(MergerUtils.checkSVOverlap(iSet.innerStart, working.Start(), iSet.innerEnd, working.End(), 0.5d, iSet.svType, callEnum.DELETION)){
                                iSet.addSplit(working, "setCoverEvents", 93);
                                if(iSet.binChanged()){
                                    this.setMap.removeBedData(iSet, bins, x);
                                    this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                }
                                foundMatch = true;
                                //continue;
                            }else if(!working.isBalanced() &&
                                    (MergerUtils.checkOverlap(iSet.Start(), working.Start() - working.retReadLen() / 2, iSet.innerStart(), working.End() +  working.retReadLen() / 2)
                                     || MergerUtils.checkOverlap(iSet.innerEnd, working.Start() - working.retReadLen() / 2, iSet.End(), working.End() + working.retReadLen() / 2))){
                                // Is an unbalanced split that falls within the read coordinates of the balanced splits
                                iSet.addSplit(working, "setCoverEvents", 104);
                                if(iSet.binChanged()){
                                    this.setMap.removeBedData(iSet, bins, x);
                                    this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                }
                                foundMatch = true;
                                //continue;
                            } else if (iSet.onlyUnbalanced &&
                                    (MergerUtils.checkOverlap(iSet.Start(), working.Start() - working.retReadLen() / 2, iSet.innerStart(), working.Start() + working.retReadLen() / 2)
                                     || MergerUtils.checkOverlap(iSet.innerEnd, working.End() - working.retReadLen() / 2, iSet.End(), working.End() + working.retReadLen() / 2))){
                                // Is a split (unbalanced or not) that falls within the reed coordinates of an unbalanced split
                                iSet.addSplit(working, "setCoverEvents", 115);
                                if(iSet.binChanged()){
                                    this.setMap.removeBedData(iSet, bins, x);
                                    this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                }
                                foundMatch = true;
                                //continue;
                            }
                        }
                    }else{
                        continue;
                    }
                }
            }

            // If the split mapping doesn't match any current set, make a new one
            if(!foundMatch){
                if(working.isBalanced()){
                    this.setMap.addBedData(new InitialSet(working));
                }else{
                    this.setMap.addBedData(new InitialSet(working, true));
                }
                num++;
            }
            System.out.print("[VHSR SETCOVER] Current split sets:\t" + num + "\r");
        }
        
        System.out.println(System.lineSeparator() + "[VHSR SETCOVER] Finished loading split reads into initial containers");                
        
    }
    private void setEventsDisc() throws BedFileException{
        
        int num = 0;
        for(int i = 0; i < this.divets.size(); i++){
            // Gap check and filtration
            if(this.gaps.checkGapOverlap(this.divets.get(i))){
                continue; //Intersected a gap
            }
            if(this.divets.get(i).ProbPhred() < 0.01d){
                continue; //Average mapping quality was bad for the reads involved
            }
            
            // Read passed filter
            divet working = this.divets.get(i);
            
            
            // Add set to list if list is empty
            if(this.setMap.isEmpty()){
                this.setMap.addBedData(new InitialSet(working));
                num++;
                continue;
                
            }

            // Check to see if there is an overlap with existing list elements
            boolean foundMatch = false;
            for(String chr : this.setMap.getListChrs()){
                for(int bins : utils.BinBed.getBins(working.Start(), working.End())){
                    if(this.setMap.containsBin(chr, bins)){
                        ArrayList<InitialSet> beds = this.setMap.getBedAbstractList(chr, bins);
                        for(int x = 0; x < beds.size(); x++){
                            InitialSet iSet = (InitialSet) beds.get(x);
                            if(iSet.isDiscOnly()){
                                if(MergerUtils.checkSVOverlap(iSet.Start(), working.Start(), iSet.End(), working.End(), 0.75d, iSet.svType, working.SvType())){
                                    // The set item contains only divets that overlap the current divet by 75%
                                    iSet.addDisc(working, "setCover", 181);
                                    if(iSet.binChanged()){
                                        this.setMap.removeBedData(iSet, bins, x);
                                        this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                    }
                                    foundMatch = true;
                                    //continue;
                                }
                            }else if(iSet.onlyUnbalanced){
                                if(MergerUtils.checkOverlap(iSet.Start(), working.End1(), iSet.End(), working.Start2()) && 
                                        (working.SvType() == callEnum.DELETION || working.SvType() == callEnum.DELINV)){
                                    // The Set item was comprised only of unbalanced splits that just happened to be within the inner coordinates of the divet
                                    if(iSet.innerStart() < working.Start2() && iSet.innerEnd() > working.End1()){
                                        // Adding the divet to this set would not cause the inner coordinates to collapse
                                        iSet.addDisc(working, true, "setCover", 195);
                                        if(iSet.binChanged()){
                                            this.setMap.removeBedData(iSet, bins, x);
                                            this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                        }
                                        foundMatch = true;
                                        //continue;
                                    }
                                }
                            }else{
                                if(MergerUtils.checkSVOverlap(iSet.innerStart, working.End1(), iSet.innerEnd, working.Start2(), 0.05d, iSet.SVType(), working.SvType())){
                                    // The set item contains splits (and possibly divets) that overlap the current divet by at least 5% on the inner coordinates
                                    iSet.addDisc(working, "setCover", 207);
                                    if(iSet.binChanged()){
                                        this.setMap.removeBedData(iSet, bins, x);
                                        this.setMap.addBedData(chr, iSet.Bin(), iSet);
                                    }
                                    foundMatch = true;
                                    //continue;
                                }
                            }
                        }
                    }else{
                        continue;
                    }
                }
            }
            
            // If the divet mapping doesn't match any current set, make a new one
            if(!foundMatch){
                this.setMap.addBedData(new InitialSet(working));
                num++;
            }
            System.out.print("[VHSR SETCOVER] Current divet set additions:\t" + num + "\r");
        }
        System.out.println(System.lineSeparator() + "[VHSR SETCOVER] Finished loading discordant reads into initial containers");
    }
    
    private ArrayList<InitialSet> MergeSets(String chr){
        // Create new arraylist and sort elements into it;
        // Conflicts should fall out and be merged into the existing sets;
        ArrayList<InitialSet> safeList = new ArrayList<>();
        ArrayList<InitialSet> starterSets = this.setMap.getSortedBedAbstractList(chr);
        Collections.sort(starterSets);
        int sI = 0; // safeList iterator
        for(int i = 0; i < starterSets.size(); i++){
            InitialSet working = (InitialSet) starterSets.get(i);
            if(safeList.isEmpty()){
                safeList.add(working);
                continue;
            }
            if(MergerUtils.checkSVOverlap(safeList.get(sI).Start(), working.Start(), 
                    safeList.get(sI).End(), working.End(), 0.5, safeList.get(sI).svType, working.svType)
                 && (working.innerStart() < safeList.get(sI).innerEnd()
                    && working.innerEnd() > safeList.get(sI).innerStart())){
                    safeList.get(sI).mergeSet(working, "setCover", 245);
                              
            }else{
                safeList.add(working);
                sI++;
            }
        }
        System.out.println("[VHSR SETCOVER] Finished sorting sets and resolving conflicts");
        return safeList;
    }
    
    private boolean IdentifyConflicts(){
        //The array should be sorted, so all I have to do is check merger with the next elements downstream
        //This should cut down on computation time.
        boolean needsMerge = false;
        for(int i = 0; i < this.setList.size() - 1; i++){
            InitialSet ISet = this.setList.get(i);
            for(int j = i + 1; j < this.setList.size() - i;j++){
                if(this.setList.get(i).Start() > this.setList.get(i + j).End()){
                    // Will not find any further conflicts
                    break;
                }
                
                InitialSet JSet = this.setList.get(i+j);
                
                // Check for merger
                if(MergerUtils.checkOverlap(this.setList.get(i).Start(), this.setList.get(i+j).Start(), 
                        this.setList.get(i).End(), this.setList.get(i+j).End())){
                    // Can we resolve the conflict?
                    if((ISet.SVType() == callEnum.DELETION && JSet.SVType() == callEnum.DELINV)
                            || (ISet.SVType() == callEnum.DELINV && JSet.SVType() == callEnum.DELETION)){
                        ISet.setSVType(callEnum.DELINV);
                        JSet.setSVType(callEnum.DELINV);
                        ISet.addToMerge(i+j);
                        needsMerge = true;
                        continue;
                    }else if ((ISet.SVType() == callEnum.INSERTION && JSet.SVType() == callEnum.INSINV)
                            || (ISet.SVType() == callEnum.INSINV && JSet.SVType() == callEnum.INSERTION)){
                        ISet.setSVType(callEnum.INSINV);
                        JSet.setSVType(callEnum.INSINV);
                        ISet.addToMerge(i+j);
                        needsMerge = true;
                        continue;
                    }
                    
                    // Now we add conflicts to the pile
                    ISet.addToConflicts(JSet);
                }
            }
            System.out.print("Checking for conflicts in set: " + i + " out of: " + this.setList.size() + "\r");
        }
        return needsMerge;
    }
    
    private ArrayList<InitialSet> SpecificMerge(){
        ArrayList<InitialSet> safeList = new ArrayList<>();
        HashMap<Integer, Boolean> avoidList = new HashMap<>();
        for(int i = 0; i < this.setList.size(); i++){
            if(avoidList.containsKey(i)){
                // This element was merged with another
                continue;
            }
            InitialSet working = this.setList.get(i);
            if(working.shouldMerge){
                for(Integer j : working.SetsToMerge()){
                    working.mergeSet(this.setList.get(j), "setCover", 309);
                    avoidList.put(j, Boolean.TRUE);
                }
            }
        }
        return safeList;
    }
    
    public ArrayList<InitialSet> SetList(){
        return this.setList;
    }
}
