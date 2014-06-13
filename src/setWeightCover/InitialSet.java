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
import stats.MergerUtils;

/**
 *
 * @author bickhart
 */
public class InitialSet extends BedAbstract{
    protected int innerStart;
    protected int innerEnd;
    protected callEnum svType;
    protected ArrayList<divet> discReads;
    protected ArrayList<pairSplit> splitReads;
    protected boolean onlyUnbalanced = false;
    protected double sumFullSupport;
    protected double sumUnbalSupport;
    protected ArrayList<InitialSet> conflicts = null;
    protected boolean hasConflicts = false;
    protected ArrayList<Integer> setsToMerge = null;
    protected boolean shouldMerge = false;
    protected int bin;
    protected boolean ismessed = false;
    
    public InitialSet(divet d){
        this.discReads = new ArrayList<>();
        this.splitReads = new ArrayList<>();
        this.chr = d.Chr();
        this.discReads.add(d);
        this.start = d.Start();
        this.end = d.End();
        this.innerStart = d.End1();
        this.innerEnd = d.Start2();
        this.svType = d.SvType();
        this.bin = utils.BinBed.getBin(start, end);
    }
    public InitialSet(pairSplit s){
        this.discReads = new ArrayList<>();
        this.splitReads = new ArrayList<>();
        this.splitReads.add(s);
        this.chr = s.Chr();
        this.start = s.Start();
        this.end = s.End();
        this.innerStart = s.Start();
        this.innerEnd = s.End();
        this.svType = callEnum.DELETION;
        this.bin = utils.BinBed.getBin(start, end);
    }
    public InitialSet(pairSplit s, boolean unbalanced){
        this.discReads = new ArrayList<>();
        this.splitReads = new ArrayList<>();
        this.splitReads.add(s);
        this.chr = s.Chr();
        this.start = s.Start();
        this.end = s.End();
        this.innerStart = s.Start();
        this.innerEnd = s.End();
        this.onlyUnbalanced = unbalanced;
        this.svType = callEnum.DELETION;
        this.bin = utils.BinBed.getBin(start, end);
    }
    
    @Override
    public int compareTo(BedAbstract t) {
        return this.start - t.Start();
    }
    public boolean isUnbalanced(){
        return this.onlyUnbalanced;
    }
    
    public void calcInitialSupport(){
        this.sumFullSupport = 0.0d;
        this.sumUnbalSupport = 0.0d;
        if(this.discReads.isEmpty()){
            this.sumFullSupport = 0.0d;
        }else{
            for(divet d : this.discReads){
                if(!d.checkUsed()){
                    this.sumFullSupport += d.retWeight();
                }
            }
        }
        if(this.splitReads.isEmpty()){
            this.sumUnbalSupport = 0.0d;
        }else{
            for(pairSplit s : this.splitReads){
                if(!s.checkUsed()){
                    if(s.isBalanced()){
                        this.sumFullSupport += s.retWeight();
                    }else{
                        this.sumUnbalSupport += s.retWeight();
                    }
                }
            }
        }
        if((this.sumFullSupport == 0.0d && this.sumUnbalSupport == 0.0d) || 
                (Double.isNaN(sumFullSupport) || Double.isNaN(sumUnbalSupport))){
            System.out.println("[DEBUG] initialsupport " + start + " " + innerStart + " " + innerEnd + " " + end + " " + sumFullSupport + " " + sumUnbalSupport);
        }
    }
    public void setReadsUsed(){
        if(!this.discReads.isEmpty()){
            for(divet d : this.discReads){
                d.setUsed();
            }
        }
        if(!this.splitReads.isEmpty()){
            for(pairSplit s : this.splitReads){
                s.setUsed();
            }
        }
    }
    public void addDisc(divet d){
        this.start = MergerUtils.getRefineCoords(this.start, d.Start(), false);
        this.end = MergerUtils.getRefineCoords(this.end, d.End(), true);
        this.innerStart = MergerUtils.getRefineCoords(this.innerStart, d.End1(), true);
        this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, d.Start2(), false);
        this.discReads.add(d);
    }
    public void addDisc(divet d, String cname, int lnnum){
        this.start = MergerUtils.getRefineCoords(this.start, d.Start(), false);
        this.end = MergerUtils.getRefineCoords(this.end, d.End(), true);
        this.innerStart = MergerUtils.getRefineCoords(this.innerStart, d.End1(), true);
        this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, d.Start2(), false);
        if(innerEnd - innerStart < 0 && !ismessed){
            System.out.println("[DEBUG] " + cname + ":" + lnnum + " " + d.Name() + " " + d.Start() + " " + d.End1() + " " + d.Start2() + " " + d.End() + 
                    " current " + start + " " + innerStart + " " + innerEnd + " " + end + " " + splitReads.size() + " " + discReads.size());
            ismessed = true;
        }
        this.discReads.add(d);
    }
    public void addDisc(divet d, boolean unbalanced){
        if(this.discReads.isEmpty()){
            this.start = d.Start();
            this.end = d.End();
            // Since the set is unbalanced, and only one half of the deletion region is known, 
            // the inner coordinates need to be selected carefully
            if(MergerUtils.isCloserLeft(d.End1(), d.Start2(), this.innerStart)){
                this.innerEnd = d.Start2();
            }else{
                this.innerStart = d.End1();
            }
        }else{
            this.start = MergerUtils.getRefineCoords(this.start, d.Start(), false);
            this.end = MergerUtils.getRefineCoords(this.end, d.End(), true);
            this.innerStart = MergerUtils.getRefineCoords(this.innerStart, d.End1(), true);
            this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, d.Start2(), false);
        }
        this.discReads.add(d);
    }
    public void addDisc(divet d, boolean unbalanced, String cname, int lnnum){
        if(this.discReads.isEmpty()){
            this.start = d.Start();
            this.end = d.End();
            // Since the set is unbalanced, and only one half of the deletion region is known, 
            // the inner coordinates need to be selected carefully
            if(MergerUtils.isCloserLeft(d.End1(), d.Start2(), this.innerStart)){
                this.innerEnd = d.Start2();
            }else{
                this.innerStart = d.End1();
            }
        }else{
            this.start = MergerUtils.getRefineCoords(this.start, d.Start(), false);
            this.end = MergerUtils.getRefineCoords(this.end, d.End(), true);
            this.innerStart = MergerUtils.getRefineCoords(this.innerStart, d.End1(), true);
            this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, d.Start2(), false);
        }
        if(innerEnd - innerStart < 0 && !ismessed){
            System.out.println("[DEBUG]u " + cname + ":" + lnnum + " " + d.Name() + " " + d.Start() + " " + d.End1() + " " + d.Start2() + " " + d.End() + 
                    " current " + start + " " + innerStart + " " + innerEnd + " " + end + " " + splitReads.size() + " " + discReads.size());
            ismessed = true;
        }
        this.discReads.add(d);
    }
    public void addSplit(pairSplit s){
        coordRefine(s, !s.isBalanced());
    }
    public void addSplit(pairSplit s, String cname, int lnnum){
        coordRefine(s, !s.isBalanced());
        if(innerEnd - innerStart < 0 && !ismessed){
            System.out.println("[DEBUG] " + cname + ":" + lnnum + " " + s.Name() + " " + s.Start()  + " " + s.End() + 
                    " current " + start + " " + innerStart + " " + innerEnd + " " + end + " " + splitReads.size() + " " + discReads.size());
            ismessed = true;
        }
    }
    private void coordRefine(pairSplit s, boolean unbal){
        if(this.onlyUnbalanced && !unbal){
            this.start = MergerUtils.getRefineCoords(this.start, s.Start(), false);
            this.end = MergerUtils.getRefineCoords(this.end, s.End(), true);
            this.innerStart = MergerUtils.getRefineCoords(this.innerStart, s.Start(), false);
            this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, s.End(), true);
            this.splitReads.add(s);
            this.onlyUnbalanced = false;
        }else if(!this.onlyUnbalanced && unbal){
            if(MergerUtils.isCloserLeft(start, end, s.Start()) && s.End() < this.innerStart){
                this.innerStart = MergerUtils.getRefineCoords(this.innerStart, s.End(), true);
            }else if(!MergerUtils.isCloserLeft(start, end, s.Start()) && s.Start() > this.innerEnd){
                this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, s.Start(), false);
            }else{
                return; // Was an unbalanced split in the middle of this event; ignoring for now
            }
            this.start = MergerUtils.getRefineCoords(this.start, s.Start(), false);
            this.end = MergerUtils.getRefineCoords(this.end, s.End(), true);            
            this.splitReads.add(s);
        }else{
            this.start = MergerUtils.getRefineCoords(this.start, s.Start(), false);
            this.end = MergerUtils.getRefineCoords(this.end, s.End(), true);
            if(!unbal && !this.onlyUnbalanced){
                this.innerStart = MergerUtils.getRefineCoords(this.innerStart, s.Start(), false);
                this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, s.End(), true);
            }else{
                if(MergerUtils.isCloserLeft(start, end, s.Start()) && s.End() < this.innerStart){
                    this.innerStart = MergerUtils.getRefineCoords(this.innerStart, s.End(), true);
                }else if(!MergerUtils.isCloserLeft(start, end, s.Start()) && s.Start() > this.innerEnd){
                    this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, s.Start(), false);
                }else{
                    return; // Was an unbalanced split in the middle of this event; ignoring for now
                }
            }
            this.splitReads.add(s);            
        }
        
    }
    public void mergeSet(InitialSet r){
        for(divet d : r.getDisc()){
            this.addDisc(d);
        }
        for(pairSplit s : r.getSplits()){
            this.addSplit(s);
        }
        if(r.hasConflicts){
            for(InitialSet c : r.Conflicts()){
                this.addToConflicts(c);
            }
            this.hasConflicts = true;
        }
    }
    public void mergeSet(InitialSet r, String cname, int lnnum){
        for(divet d : r.getDisc()){
            this.addDisc(d);
        }
        for(pairSplit s : r.getSplits()){
            this.addSplit(s);
        }
        if(innerEnd - innerStart < 0){
            System.out.println("[DEBUG] " + cname + ":" + lnnum + " " +  
                    " current " + start + " " + innerStart + " " + innerEnd + " " + end + " " + splitReads.size() + " " + discReads.size());
        }
        if(r.hasConflicts){
            for(InitialSet c : r.Conflicts()){
                this.addToConflicts(c);
            }
            this.hasConflicts = true;
        }
    }
    private void refineCoords(int start, int end, int innerStart, int innerEnd){
        this.start = MergerUtils.getRefineCoords(this.start, start, false);
        this.end = MergerUtils.getRefineCoords(this.end, end, true);
        this.innerStart = MergerUtils.getRefineCoords(this.innerStart, innerStart, true);
        this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, innerEnd, false);
    }
    public int innerStart(){
        return this.innerStart;
    }
    public int innerEnd(){
        return this.innerEnd;
    }
    public int divetSupport(){
        return this.discReads.size();
    }
    public int splitSupport(){
        return this.splitReads.size();
    }
    public int unbalSplitSupport(){
        int unbal = 0;
        for(pairSplit s : this.splitReads){
            if(!s.isBalanced()){
                unbal++;
            }
        }
        return unbal;
    }
    public ArrayList<divet> getDisc(){
        return this.discReads;
    }
    public ArrayList<pairSplit> getSplits(){
        return this.splitReads;
    }
    public double SumFullSupport(){
        return this.sumFullSupport;
    }
    public double SumUnbalSupport(){
        return this.sumUnbalSupport;
    }
    public void addToConflicts(InitialSet set){
        if(this.conflicts == null){
            this.conflicts = new ArrayList<>();
        }
        this.hasConflicts = true;
        this.conflicts.add(set);
    }
    public ArrayList<InitialSet> Conflicts(){
        return this.conflicts;
    }
    public boolean HasConflicts(){
        return this.hasConflicts;
    }
    public callEnum SVType(){
        return this.svType;
    }
    public void addToMerge(int index){
        if(this.setsToMerge == null){
            this.setsToMerge = new ArrayList<>();
        }
        this.shouldMerge = true;
        this.setsToMerge.add(index);
    }
    public boolean ShouldMerge(){
        return this.shouldMerge;
    }
    public void setSVType(callEnum e){
        this.svType = e;
    }
    public ArrayList<Integer> SetsToMerge(){
        return this.setsToMerge;
    }
    public void SetAllReadsUsed(){
        if(!this.discReads.isEmpty()){
            for(divet d : this.discReads){
                d.setUsed();
            }
        }
        if(!this.splitReads.isEmpty()){
            for(pairSplit s : this.splitReads){
                s.setUsed();
            }
        }
    }
    public boolean isDiscOnly(){
        if(this.discReads.isEmpty()){
            return false;
        }else if(this.splitReads.isEmpty()){
            return true;
        }
        return false;
    }
    public boolean binChanged(){
        int newbin = getNewBin();
        if(bin != newbin){
            bin = newbin;
            return true;
        }else{
            return false;
        }
    }
    private int getNewBin(){
        return utils.BinBed.getBin(start, end);
    }
    public int Bin(){
        return bin;
    }
    public boolean checkParity(){
        if(Double.isNaN(sumFullSupport) || Double.isNaN(sumUnbalSupport)
                || Double.isInfinite(sumFullSupport) || Double.isInfinite(sumUnbalSupport)){
            return false;
        }
        return true;
    }
}
