/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import dataStructs.callEnum;
import dataStructs.divet;
import dataStructs.pairSplit;
import file.BedAbstract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import stats.MergerUtils;
import stats.SortWeightMap;

/**
 *
 * @author bickhart
 */
public abstract class finalSets extends BedAbstract{
    protected int innerStart;
    protected int innerEnd;
    protected int splitSupport;
    protected int unbalancedSplitSupport;
    protected int discSupport;
    protected double sumFullSupport;
    protected double sumUnbalSupport;
    protected InitialSet supportingReads;
    public callEnum svType;
    
    public void initialize(InitialSet a){
        this.chr = a.Chr();
        //refineMapsRemainingReads(a);
        setMapData(a);
    }
    private void setMapData(InitialSet a){
        this.start = a.Start();
        this.end = a.End();
        this.innerStart = a.innerStart();
        this.innerEnd = a.innerEnd();
        this.splitSupport = a.splitSupport();
        this.discSupport = a.divetSupport();
        this.unbalancedSplitSupport = a.unbalSplitSupport();
        this.sumFullSupport = a.SumFullSupport();
        this.sumUnbalSupport = a.SumUnbalSupport();
        a.SetAllReadsUsed();
    }
    private void refineMapsRemainingReads(InitialSet a){
        // Takes the subsets of the internal events that have not been used
        ArrayList<InitialSet> subSets = subSetInternalCoords(a);
        
        // Calculate support for each subset and then sort
        for(InitialSet set : subSets){
            set.calcInitialSupport();
        }
        SortWeightMap weightSort = new SortWeightMap();
        Collections.sort(subSets, weightSort);
        
        // The first element should have the best support
        InitialSet winner = subSets.get(0);
        
        winner.SetAllReadsUsed();
        
        this.start = winner.Start();
        this.end = winner.End();
        this.innerStart = winner.innerEnd();
        this.innerEnd = winner.innerEnd();
        this.splitSupport = winner.splitSupport();
        this.discSupport = winner.divetSupport();
        this.unbalancedSplitSupport = winner.unbalSplitSupport();
        this.sumFullSupport = winner.SumFullSupport();
        this.sumUnbalSupport = winner.SumUnbalSupport();
    }
    private ArrayList<InitialSet> subSetInternalCoords(InitialSet a){
        // This function determines direct overlap for the inner and outer coords and subsets the event
        ArrayList<InitialSet> subsets = new ArrayList<>();
        
        // Split reads should be our most accurate breakpoint maps, so we will set the subsets on them first
        for(pairSplit s: a.getSplits()){
            if(subsets.isEmpty()){
                subsets.add(new InitialSet(s));
                continue;
            }
            if(s.checkUsed()){
                continue;
            }
            boolean found = false;
            for(int i = 0; i < subsets.size(); i++){
                InitialSet set = subsets.get(i);
                boolean inStartOvlp = MergerUtils.checkOverlap(set.Start() -1, s.Start() - 5, set.innerStart, s.Start());
                boolean inEndOvlp = MergerUtils.checkOverlap(set.innerEnd, s.End() - 5, set.End(), s.End() + 5);
                if(inStartOvlp && inEndOvlp){
                    // giving a 5 bp leeway on both ends to try to merge splits together (because of unbalanced events)
                    set.addSplit(s);
                    found = true;
                    break;
                }
            }
            if(!found){
                subsets.add(new InitialSet(s));
            }
        }
        
        // Now to try to include any paired end data
        for(divet d : a.getDisc()){
            if(subsets.isEmpty()){
                subsets.add(new InitialSet(d));
                continue;
            }
            if(d.checkUsed()){
                continue;
            }
            boolean found = false;
            for(int i = 0; i < subsets.size(); i++){
                InitialSet set = subsets.get(i);
                boolean inStartOvlp = MergerUtils.checkOverlap(set.Start(), d.Start() - (int)(d.StDev() * 2.0d), set.innerStart, d.End1() + (int) (d.StDev() * 2.0d));
                boolean inEndOvlp = MergerUtils.checkOverlap(set.innerEnd, d.Start2() - (int) (d.StDev() * 2.0d), set.End(), d.End() +(int) (d.StDev() * 2.0d) );
                if(inStartOvlp && inEndOvlp){
                    // Using the given read coordinates from the divet files +/- the stdev of the read
                    set.addDisc(d);
                    found = true;
                    break;
                }
            }
            if(!found){
                subsets.add(new InitialSet(d));
            }
        }
        return subsets;
    }
    
    @Override
    public int compareTo(BedAbstract t) {
        finalSets working = (finalSets) t;
        return (int) working.SumFullSupport() - (int) this.sumFullSupport;
    }
    public int InnerStart(){
        return this.innerStart;
    }
    public int InnerEnd(){
        return this.innerEnd;
    }
    public int SplitSupport(){
            return this.splitSupport;
    }
    public int UnbalancedSplitSupport(){
        return this.unbalancedSplitSupport;
    }
    public int DiscSupport(){
        return this.discSupport;
    }
    public double SumFullSupport(){
        return this.sumFullSupport;
    }
    public double SumUnbalSupport(){
        return this.sumUnbalSupport;
    }
}
