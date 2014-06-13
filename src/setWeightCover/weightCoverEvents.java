/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import dataStructs.callEnum;
import file.BedAbstract;
import finalSVTypes.*;
import java.util.ArrayList;
import stats.SortWeightMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 *
 * @author derek.bickhart
 */
public class weightCoverEvents{
    private ArrayList<InitialSet> inputSets;
    private String chr;
    private ArrayList<Inversions> inversions;
    private ArrayList<TandDup> tandup;
    private ArrayList<Deletions> deletions;
    private ArrayList<Insertions> insertions;
    
    public weightCoverEvents(ArrayList<InitialSet> sets, String chr){
        this.inputSets = sets;
        this.chr = chr;
        this.inversions = new ArrayList<>();
        this.tandup = new ArrayList<>();
        this.deletions = new ArrayList<>();
        this.insertions = new ArrayList<>();
        run();
    }
    
    
    private void run() {
        // Create Array of elements sorted by coordinates
        ArrayList<CoordTree> coordsorted;
        coordsorted = new ArrayList<>(this.inputSets.size());
        for(InitialSet s : this.inputSets){
            if(s.SVType() == callEnum.INVERSION || s.SVType() == callEnum.INSINV || s.SVType() == callEnum.DELINV){
                // Only needed for inversions
                // Going to be greedy here and use INSINV and DELINV for Inversion finding, but will treat them
                //  as deletions or insertions if they have high support in the main routine
                coordsorted.add(new CoordTree(s));
            }
            s.calcInitialSupport();
        }
        SortWeightMap supportSort = new SortWeightMap();
        int initialSize = this.inputSets.size();
        int finalCount = 0;
        for(int z = 0; z < initialSize; z++){
            // Now sort list of elements for the weight cover algorithm 
            Collections.sort(this.inputSets, supportSort);

            // Since everything is sorted based on score, loop through and process events
            InitialSet working = this.inputSets.get(0);
            finalCount++;
            switch(working.SVType()){
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
                    System.out.println("Error with enum! " + working.SVType());
            }
            
            ArrayList<InitialSet> toRemove = new ArrayList<>();
            this.inputSets.remove(0);
            for(int i = 0; i < this.inputSets.size(); i++){
                InitialSet s = this.inputSets.get(i);
                this.inputSets.get(i).calcInitialSupport();
                if(this.inputSets.get(i).sumFullSupport == 0d){
                    toRemove.add(this.inputSets.get(i));
                }
            }
            for(int x = 0; x < toRemove.size(); x++){
                this.inputSets.remove(toRemove.get(x));
            }
            if(this.inputSets.isEmpty()){
                break;
            }
            System.out.print("[VHSR WEIGHT] Working on set number: " + z + "\r");
        }
        
        System.out.println(System.lineSeparator() + "[VHSR WEIGHT] Finished with: " + this.chr + ": " + finalCount + " out of " + initialSize + " initial Events");
    }
    
    private void ProcessTanDup(InitialSet a){
        this.tandup.add(new TandDup(a));
    }
    private void ProcessDel(InitialSet a){
        this.deletions.add(new Deletions(a));
    }
    private void ProcessIns(InitialSet a){
        this.insertions.add(new Insertions(a));
    }
    private void ProcessInv(InitialSet a, ArrayList<CoordTree> coords){
        // Find the starting coordinate for the inversion in the coord sorted array
        int initialCoord = 0;
        Inversions temp = new Inversions(a);
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
                temp.AddReverseSupport(coords.get(i).RetReference());
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
        private InitialSet reference;
        public CoordTree(InitialSet reference){
            this.start = reference.Start();
            this.end = reference.End();
            this.reference = reference;
        }
        @Override
        public int compareTo(BedAbstract t) {
            return this.start - t.Start();
        }
        public InitialSet RetReference(){
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
