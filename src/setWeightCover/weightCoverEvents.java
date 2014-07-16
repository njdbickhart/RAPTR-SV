/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import dataStructs.SetMap;
import dataStructs.callEnum;
import file.BedAbstract;
import finalSVTypes.*;
import java.util.ArrayList;
import stats.SortWeightMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


/**
 *
 * @author derek.bickhart
 */
public class weightCoverEvents{
    private ArrayList<BufferedInitialSet> inputSets;
    private String chr;
    private ArrayList<Inversions> inversions;
    private ArrayList<TandDup> tandup;
    private ArrayList<Deletions> deletions;
    private ArrayList<Insertions> insertions;
    private HashSet<String> names;
    
    public weightCoverEvents(SetMap sets, String chr){
        this.inputSets = sets.getUnsortedBedList(chr);
        this.chr = chr;
        this.inversions = new ArrayList<>();
        this.tandup = new ArrayList<>();
        this.deletions = new ArrayList<>();
        this.insertions = new ArrayList<>();
        this.names = new HashSet<>();
 
    }
    
    public void calculateInitialSetStats(){
        System.out.println("[RPSR WEIGHT] Calculating preliminary set values.");
        this.inputSets.stream().forEach((s) -> {
            s.preliminarySetCalcs();
        });
    }
    
    public void run() {
        // Create Array of elements sorted by coordinates
        ArrayList<CoordTree> coordsorted;
        coordsorted = new ArrayList<>(this.inputSets.size());
        for(BufferedInitialSet s : this.inputSets){
            if(s.svType == callEnum.INVERSION || s.svType == callEnum.INSINV || s.svType == callEnum.DELINV){
                // Only needed for inversions
                // Going to be greedy here and use INSINV and DELINV for Inversion finding, but will treat them
                //  as deletions or insertions if they have high support in the main routine
                coordsorted.add(new CoordTree(s));
            }
            s.reCalculateValues(names);
            if(s.hasSplitUnbalSupport()){
                System.out.println("hey");
            }
        }
        //SortWeightMap supportSort = new SortWeightMap();
        int initialSize = this.inputSets.size();
        int finalCount = 0, removal = 0;
        for(int z = 0; z < initialSize; z++){
            // Now sort list of elements for the weight cover algorithm 
            Collections.sort(this.inputSets, new Comparator<BufferedInitialSet>() {

                @Override
                public int compare(BufferedInitialSet t, BufferedInitialSet t1) {
                    if(t.sumFullSupport < t1.sumFullSupport) {
                        return 1;
                    }else if (t.sumFullSupport > t1.sumFullSupport) {
                        return -1;
                    }else{
                        if (t.sumUnbalSupport < t1.sumUnbalSupport) {
                            return 1;
                        }else if (t.sumUnbalSupport > t1.sumUnbalSupport) {
                            return -1;
                        }else {
                            return 0;
                        }
                    }
                }
            });

            // Since everything is sorted based on score, loop through and process events
            BufferedInitialSet working = this.inputSets.get(0);
            finalCount++;
            switch(working.svType){
                case DELETION:
                case DELINV:
                    ProcessDel(working); break;
                case INSERTION:
                case INSINV:
                    ProcessIns(working); break;
                case INVERSION:
                    ProcessInv(working, coordsorted); break;
                case EVERSION:
                    ProcessTanDup(working); break;
                default:
                    System.out.println("Error with enum! " + working.svType);
            }
            
            ArrayList<BufferedInitialSet> toRemove = new ArrayList<>();
            this.inputSets.get(0).closeTemp('A');
            this.inputSets.remove(0);
            removal++;
            for(int i = 0; i < this.inputSets.size(); i++){
                this.inputSets.get(i).reCalculateValues(names);
                // Removes all sets that have less than 1 supporting read (and mapping quality)
                if(this.inputSets.get(i).sumFullSupport <= 1d){
                    this.inputSets.get(i).closeTemp('A');
                    toRemove.add(this.inputSets.get(i));
                    removal++;
                }
            }
            for(int x = 0; x < toRemove.size(); x++){
                this.inputSets.remove(toRemove.get(x));
            }
            if(this.inputSets.isEmpty()){
                break;
            }
            System.out.print("[RPSR WEIGHT] Working on set number: " + z + " of " + initialSize + " and removed: " + removal + "\r");
        }
        
        System.out.println(System.lineSeparator() + "[RPSR WEIGHT] Finished with: " + this.chr + ": " + finalCount + " out of " + initialSize + " initial Events");
    }
    
    private void ProcessTanDup(BufferedInitialSet a){
        this.tandup.add(new TandDup(a, names));
    }
    private void ProcessDel(BufferedInitialSet a){
        this.deletions.add(new Deletions(a, names));
    }
    private void ProcessIns(BufferedInitialSet a){
        this.insertions.add(new Insertions(a, names));
    }
    private void ProcessInv(BufferedInitialSet a, ArrayList<CoordTree> coords){
        // Find the starting coordinate for the inversion in the coord sorted array
        int initialCoord = 0;
        Inversions temp = new Inversions(a, names);
        for(int i = 0; i < coords.size(); i++){
            if(coords.get(i).Start() == a.Start()){
                initialCoord = i;
                break;
            }
        }
        
        // Now loop through the remainder of the array to try to find another inversion within 100kb
        boolean found = false;
        for(int i = initialCoord + 1; i < coords.size(); i++){
            if(coords.get(i).Start() > a.Start() + 100000){
                break;
            }else if(coords.get(i).Start() < a.Start() + 100000){
                temp.AddReverseSupport(coords.get(i).RetReference(), names);
                found = true;
                break;
            }
        }
        
        // Finalize inversion and add to pile
        if(!found){
            temp.PresentAsIs();
        }
        this.inversions.add(temp);
    }
    protected class CoordTree extends BedAbstract{
        private BufferedInitialSet reference;
        public CoordTree(BufferedInitialSet reference){
            this.start = reference.Start();
            this.end = reference.End();
            this.reference = reference;
        }
        @Override
        public int compareTo(BedAbstract t) {
            return this.start - t.Start();
        }
        public BufferedInitialSet RetReference(){
            return this.reference;
        }
    }
    
    public ArrayList<Inversions> RetInv(){
        return this.inversions;
    }
    public ArrayList<Deletions> RetDel(){
        return this.deletions;
    }
    public ArrayList<TandDup> RetTand(){
        return this.tandup;
    }
    public ArrayList<Insertions> RetIns(){
        return this.insertions;
    }
    
}
