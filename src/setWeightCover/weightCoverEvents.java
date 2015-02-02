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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 *
 * @author derek.bickhart
 */
public class weightCoverEvents{
    private final ArrayList<BufferedInitialSet> inputSets;
    private final String chr;
    private final ArrayList<Inversions> inversions;
    private final ArrayList<TandDup> tandup;
    private final ArrayList<Deletions> deletions;
    private final ArrayList<Insertions> insertions;
    private final HashSet<String> names;
    private final boolean debugmode;
    private final int thresh;
    private final double phredFilter;
    //private final ForkJoinPool pool;
    
    private int del = 0, ins = 0, inv = 0, tand = 0;
    
    private static final Logger log = Logger.getLogger(weightCoverEvents.class.getName());
    
    public weightCoverEvents(SetMap sets, String chr, boolean debug, int thresh, int threads, double phredFilter){
        this.inputSets = sets.getUnsortedBedList(chr);
        this.chr = chr;
        this.inversions = new ArrayList<>();
        this.tandup = new ArrayList<>();
        this.deletions = new ArrayList<>();
        this.insertions = new ArrayList<>();
        this.names = new HashSet<>();
        debugmode = debug;
        this.thresh = thresh;
        this.phredFilter = phredFilter;
        //this.pool = new ForkJoinPool(threads);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threads));
        log.log(Level.FINE, "[WEIGHT] System fork join pool set to: " + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
    }
    
    public void calculateInitialSetStats(){
        System.out.println("[RAPTR-SV WEIGHT] Calculating preliminary set values.");
        this.inputSets.stream().forEach((s) -> {
            s.preliminarySetCalcs();
        });
    }
    
    public void run() {
        // Create Array of elements sorted by coordinates
        ArrayList<CoordTree> coordsorted;
        coordsorted = new ArrayList<>(this.inputSets.size());
        // Only needed for inversions
        // Going to be greedy here and use INSINV and DELINV for Inversion finding, but will treat them
        // as deletions or insertions if they have high support in the main routine
        this.inputSets.stream()
                .filter(s -> s.svType == callEnum.INVERSION || s.svType == callEnum.INSINV || s.svType == callEnum.DELINV)
                .forEach(s -> coordsorted.add(new CoordTree(s)));
        
        // Parallel implementation to try to speed up calculations
        log.log(Level.FINE, "[WEIGHT] Calculating initial set weight values.");
        this.inputSets.parallelStream()
                .forEach(s -> s.reCalculateValues(names));
        
        // Now I'm going to try to do some dynamic programming to identify sets that need to be recalculated
        log.log(Level.FINE, "[WEIGHT] Identifying sets that need to be recalculated.");
        Set<String> lookout = this.inputSets.parallelStream()
                .map((s) -> s.getReadNames())
                .flatMap(ArrayList::stream)
                .collect(Collectors.groupingByConcurrent(o -> o, Collectors.counting()))
                // Now we have a HashMap<String, Long> count of the read name counts, let's turn this into our set of unique names
                .entrySet().stream()
                .filter((b) -> b.getValue() > 1)
                .map((v) -> v.getKey())
                .collect(Collectors.toCollection(HashSet::new));
        
        // Now we reduce the set down to just the entries that are in multiple sets
        /*Set<String> lookout = nameUsage.entrySet().stream()
        .filter((s) -> s.getValue() > 1)
        .map((s) -> s.getKey())
        .collect(Collectors.toCollection(HashSet::new));*/
        
        System.out.println("[RAPTR-SV WEIGHT] Reads that need recalculation: " + lookout.size());
        log.log(Level.FINE, "[WEIGHT] " + chr + " Identified: " + lookout.size() + " reads that need recalculation.");
        
        // Now its time to toggle the sets to see if they need to be recalculated
        this.inputSets.parallelStream()
                .forEach(s -> s.toggleRecalculateFlat(lookout));
        
        
        /*for(BufferedInitialSet s : this.inputSets){
        if(s.svType == callEnum.INVERSION || s.svType == callEnum.INSINV || s.svType == callEnum.DELINV){
        // Only needed for inversions
        // Going to be greedy here and use INSINV and DELINV for Inversion finding, but will treat them
        //  as deletions or insertions if they have high support in the main routine
        coordsorted.add(new CoordTree(s));
        }
        s.reCalculateValues(names);
        }*/
        //SortWeightMap supportSort = new SortWeightMap();
        int initialSize = this.inputSets.size();
        int finalCount = 0, removal = 0;
        // Initial removal to ensure that our list of sets is low for subsequent collection
        removal = this.setRemoval(removal);
        log.log(Level.FINE, "[WEIGHT] " + chr + " Removed: " + removal + " initial sets prior to weight cover.");
        for(int z = 0; z < initialSize; z++){
            if(this.inputSets.isEmpty()){
                break;
            }
            // Now sort list of elements for the weight cover algorithm 
            Collections.sort(this.inputSets, (BufferedInitialSet t, BufferedInitialSet t1) -> {
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
                    break;
            }
            
            if(working.needsRecalculation()){
                // If this was one of our sets that had read names overlapping another set
                // then store the read names to estimate new calculations down the road
                working.getReadNames().stream().forEach((s) -> names.add(s));
            }
            
            //ArrayList<BufferedInitialSet> toRemove = new ArrayList<>();
            this.inputSets.get(0).closeTemp('A');
            this.inputSets.remove(0);
            removal++;
            
            // Reduction based mapping methods to speed up calculation
            // Allows the devotion of more threads to the stream here
            if(working.needsRecalculation()){
                // Only collecting removal elements if this set had an overlapping read with another set
                this.inputSets.parallelStream()
                        .forEach(s -> s.reCalculateValues(names));

                removal = setRemoval(removal);
            }
            
            /*for(int i = 0; i < this.inputSets.size(); i++){
            this.inputSets.get(i).reCalculateValues(names);
            // Removes all sets that have less than 1 supporting read (and mapping quality)
            if(this.inputSets.get(i).sumFullSupport < thresh){
            this.inputSets.get(i).closeTemp('A');
            toRemove.add(this.inputSets.get(i));
            removal++;
            }
            }
            for(int x = 0; x < toRemove.size(); x++){
            this.inputSets.remove(toRemove.get(x));
            }*/
            /*if(this.inputSets.isEmpty()){
            break;
            }*/
            
            System.out.print("[RAPTR-SV WEIGHT] Working on set number: " + removal + " of " + initialSize + " and have retained: " + z + "\r");
        }
        
        log.log(Level.FINE, "[WEIGHT] " + chr + " Identified; del: " + del + " ins: " + ins + " tand: " + tand + " inv: " + inv);
        log.log(Level.FINE, "[WEIGHT] " + chr + " Found " + finalCount + " events out of " + initialSize + " initial events");
        System.out.println(System.lineSeparator() + "[RAPTR-SV WEIGHT] Finished with: " + this.chr + ": " + finalCount + " out of " + initialSize + " initial Events");
    }

    private int setRemoval(int removal) {
        List<BufferedInitialSet> remover = this.inputSets.parallelStream()
                .filter(s -> (s.rawReads < thresh || s.sumFullSupport < phredFilter))
                .collect(Collectors.toList());
        removal += remover.size();
        for(BufferedInitialSet r : remover){
            r.deleteTemp();
            this.inputSets.remove(r);
        }
        return removal;
    }
    
    private void ProcessTanDup(BufferedInitialSet a){
        tand++;
        this.tandup.add(new TandDup(a, names, debugmode));
    }
    private void ProcessDel(BufferedInitialSet a){
        del++;
        this.deletions.add(new Deletions(a, names, debugmode));
    }
    private void ProcessIns(BufferedInitialSet a){
        ins++;
        this.insertions.add(new Insertions(a, names, debugmode));
    }
    private void ProcessInv(BufferedInitialSet a, ArrayList<CoordTree> coords){
        // Find the starting coordinate for the inversion in the coord sorted array
        inv++;
        int initialCoord = 0;
        Inversions temp = new Inversions(a, names, debugmode);
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
                temp.AddReverseSupport(coords.get(i).RetReference(), names, debugmode);
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
