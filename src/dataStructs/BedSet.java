/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import TempFiles.TempBed.BufferedBed;
import TempFiles.TempBuffer;
import file.BedAbstract;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import setWeightCover.BufferedInitialSet;
import stats.MergerUtils;

/**
 *
 * @author bickhart
 */
public abstract class BedSet extends BufferedBed implements TempBuffer<BedAbstract>{
    public int innerStart = -1;
    public int innerEnd = -1;
    public callEnum svType;   
    protected int maxBuffer = 10;
    protected ArrayList<ReadPair> pairs = new ArrayList<>(10);
    
    protected int splitSup = -1;
    protected int divSup = -1;
    protected int unbalSplit = -1;
    public double sumFullSupport = -1.0d;
    public double sumUnbalSupport = -1.0d;
    protected HashMap<String, Integer> readNames = new HashMap<>();
    
    public void addReadPair(ReadPair bed){
        refineBedCoords(start, end, innerStart, innerEnd, bed.Start(), bed.End(), bed.innerStart, bed.innerEnd);
        this.svType = (this.svType == null)? bed.svType : this.svType;
        this.bufferedAdd(bed);
    }
    public boolean pairOverlaps(ReadPair b){
        if((this.innerStart < b.innerEnd
                && this.innerEnd > b.innerStart)
                && svTypeConsistency(this.svType, b.getSVType())){
            // A disc read or balanced split that is within the read aligment regions
            return true;
        }else if((b.Start() > this.Start() && b.Start() < this.innerStart)
                || (b.End() > this.innerEnd && b.End() < this.end)){
            // Could be an unbalanced split that we want to add here
            if(b.getReadFlags().contains(readEnum.IsUnbalanced)
                && readFlagConsistency(b.getReadFlags(), b.Start(), b.End()))
                return true;
        }else if(b.getReadFlags().contains(readEnum.IsSplit)
                && !b.getReadFlags().contains(readEnum.IsUnbalanced)
                && (b.Start() > this.start && b.End() < this.end)
                && (b.innerStart >= this.innerStart && b.innerEnd <= this.innerEnd)){
            // A balanced split that is within the read alignment regions of a disc read
            return true;
        }
        return false;
    }
    protected boolean readFlagConsistency(EnumSet<readEnum> rflags, int start, int end){
        if(rflags.contains(readEnum.FirstForward)
                && start < this.innerEnd){
            return true;
        }else if (rflags.contains(readEnum.FirstReverse)
                && end > this.innerStart){
            return true;
        }            
        return false;
    }
    protected boolean svTypeConsistency(Enum<callEnum> a, Enum<callEnum> b){
        if(a.equals(b)){
            return true;
        }else if ((a.equals(callEnum.DELETION) && b.equals(callEnum.DELINV))
                || (a.equals(callEnum.DELINV) && b.equals(callEnum.DELETION))
                || (a.equals(callEnum.INSERTION) && b.equals(callEnum.INSINV))
                || (a.equals(callEnum.INSINV) && b.equals(callEnum.INSERTION))
                || (a.equals(callEnum.EVERSION) && b.equals(callEnum.TANDEM))
                || (a.equals(callEnum.TANDEM) && b.equals(callEnum.EVERSION))){
            return true;
        }
        return false;
    }
    
    /*
     * Coordinate modifiers
     */
    private void refineBedCoords(int ... a){
        Arrays.sort(a);
        if(a.length == 8){
            // Adding to previously initialized set
            this.start = a[0];
            this.innerStart = a[3];
            this.innerEnd = a[4];
            this.end = a[7];
        }else if(a.length == 4){
            this.start = a[0];
            this.innerStart = a[1];
            this.innerEnd = a[2];
            this.end = a[3];
        }else{
            throw new NullPointerException("[BEDSET SORT] Array size of 4 or 8 expected!");
        }
    }
    
    protected void refineCoords(int start, int end, int innerStart, int innerEnd){
        this.start = MergerUtils.getRefineCoords(this.start, start, false);
        this.end = MergerUtils.getRefineCoords(this.end, end, true);
        this.innerStart = MergerUtils.getRefineCoords(this.innerStart, innerStart, true);
        this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, innerEnd, false);
    }
    
    public <T extends BedSet> void mergeBedSet(T bedSet){
        int[] a = {this.start, this.end, this.innerStart, this.innerEnd, bedSet.start,
            bedSet.end, bedSet.innerStart, bedSet.innerEnd};
        Arrays.sort(a);
        this.start = a[0];
        this.innerStart = a[3];
        this.innerEnd = a[4];
        this.end = a[7];
        this.svType = (this.svType == null)? bedSet.svType : this.svType;
        
        bedSet.restoreAll();
        for(ReadPair r : bedSet.pairs){
            this.bufferedAdd(r);
        }
        bedSet.deleteTemp();
    }
    
    public void reCalculateValues(HashSet<String> names){
        populateCalculations(names);
    }
    /*
     * Overriden methods
     */
    @Override
    public void readSequentialFile() {
        this.openTemp('R');
        try{
            String line;
            while((line = this.handle.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                ReadPair temp = new ReadPair(segs);
                this.readNames.put(segs[0], Integer.parseInt(segs[8]));
                this.pairs.add(temp);
            }
        }catch(IOException ex){
            java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            this.closeTemp('R');
        }
        
    }

    @Override
    public void dumpDataToDisk() {
        this.openTemp('A');
        try{
            for(ReadPair a : this.pairs){
                this.output.write(a.toString());
            }
        }catch(IOException ex){
            java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            this.closeTemp('A');
        }
        this.pairs.clear();
    }

    @Override
    public <BedAbstract> void bufferedAdd(BedAbstract a) {
        if(this.pairs.size() >= this.maxBuffer){
            this.dumpDataToDisk();            
        }
        ReadPair working = (ReadPair)a;
        this.refineBedCoords(working.Start(), working.End(), working.getInnerEnd(), working.getInnerEnd());
        this.chr = working.Chr();
        this.svType = (this.svType == null)? working.svType : this.svType;
        this.pairs.add(working);
    }

    @Override
    public void restoreAll() {
        if(this.hasTemp() && this.readNames.isEmpty()){
            this.openTemp('R');
            try{
                String line;
                while((line = this.handle.readLine()) != null){
                    line = line.trim();
                    String[] segs = line.split("\t");
                    this.readNames.put(segs[0], Integer.parseInt(segs[8]));
                }
            }catch(IOException ex){
                java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                this.closeTemp('R');
            }
        }
    }

    @Override
    public void pushAllToDisk() {
        dumpDataToDisk();
    }
    
    /*
     * Lazy loader
     */
    public void preliminarySetCalcs(){
        this.readSequentialFile();
        this.splitSup = 0;
        this.divSup = 0;
        this.unbalSplit = 0;
        for(ReadPair r : this.pairs){
            EnumSet<readEnum> rflags = r.getReadFlags();
            if(rflags.contains(readEnum.IsDisc))
                this.divSup++;
            else if(rflags.contains(readEnum.IsSplit))
                this.splitSup++;
            else if(rflags.contains(readEnum.IsUnbalanced)){
                this.unbalSplit++;
                this.sumUnbalSupport += (double) 1 / (double) r.mapcount;
            }
        }
    }
    private void populateCalculations(HashSet<String> names){
        this.restoreAll();
        this.sumFullSupport = 0;
        for(String r : this.readNames.keySet()){
            if(!names.contains(r)){
                this.sumFullSupport += (double) 1 / (double) this.readNames.get(r);
            }
        }
        this.dumpDataToDisk();
    }
    
    /*
     * Getters
     */
    public int splitSupport(HashSet<String> names){
        if(this.splitSup == -1){
            this.populateCalculations(names);
        }
        return this.splitSup;
    }
    
    public int divetSupport(HashSet<String> names){
        if(this.divSup == -1){
            this.populateCalculations(names);
        }
        return this.divSup;
    }
    
    public int unbalSplitSupport(HashSet<String> names){
        if(this.unbalSplit == -1){
            this.populateCalculations(names);
        }
        return this.unbalSplit;
    }
    
    public double SumFullSupport(HashSet<String> names){
        if(this.sumFullSupport == -1.0d){
            this.populateCalculations(names);
        }
        return this.sumFullSupport;
    }
    
    public double SumUnbalSupport(HashSet<String> names){
        if(this.sumUnbalSupport == -1.0d){
            this.populateCalculations(names);
        }
        return this.sumUnbalSupport;
    }
    
    public ArrayList<String> getReadNames(){
        ArrayList<String> names = new ArrayList<>();
        for(ReadPair r : this.pairs){
            names.add(r.Name());
        }
        return names;
    }
}
