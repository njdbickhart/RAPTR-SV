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
    /**
     * The beginning of the interior, high confidence, coordinates
     */
    public int innerStart = -1;
    /**
     * The end of the interior, high confidence, coordinates
     */
    public int innerEnd = -1;
    /**
     * The type of SV this indicates
     */
    public callEnum svType;   
    
    /**
     * The number of rawReads that were added to this set
     */
    public int rawReads = 0;
    /**
     * The maximum amount of read pairs to store before spilling to disk
     */
    protected int maxBuffer = 10;
    /**
     * Container for identified read pairs (both split and discordant reads
     */
    protected ArrayList<ReadPair> pairs = new ArrayList<>(10);
    
    /**
     * Split read support
     */
    protected int splitSup = -1;
    /**
     * Discordant read pair support
     */
    protected int divSup = -1;
    /**
     * Number of unbalanced split reads that overlap this region
     */
    protected int unbalSplit = -1;
    /**
     * Total number of balanced splits and discordant reads divided by their mappings
     */
    public double sumFullSupport = -1.0d;
    /**
     * Total number of unbalanced split reads divided by their mappings
     */
    public double sumUnbalSupport = -1.0d;
    /**
     * Container for read names, to ensure that the object has the read mapping numbers and can tell if a read has been previously used
     */
    protected HashMap<String, Integer> readNames = new HashMap<>();
    
    /**
     * Adds a ReadPair object to this container and modifies the coordinates appropriately
     * @param bed A ReadPair object
     */
    public void addReadPair(ReadPair bed){
        //refineCoords(bed.Start(), bed.End(), bed.innerStart, bed.innerEnd);
        this.svType = (this.svType == null)? bed.svType : this.svType;
        this.bufferedAdd(bed);
    }
    /**
     * Determines if a ReadPair intersects with this candidate SV
     * @param b A ReadPair Object
     * @return
     */
    public boolean pairOverlaps(ReadPair b){
        if(this.makesReadRegionTooLong(start, innerStart, b.Start(), b.getInnerStart(), (b.innerStart - b.Start()))
                || this.makesReadRegionTooLong(innerEnd, end, b.getInnerEnd(), b.End(), (b.innerStart - b.Start())))
            return false; // Added in to prevent logic that expanded read uncertainty regions because of overlapping exterior coordinates
        if(b.getReadFlags().contains(readEnum.IsDisc)){
            if((this.start <= b.getInnerStart() && this.innerStart >= b.Start())
                    && (this.innerEnd <= b.End() && this.End() >= b.getInnerEnd())
                    && !(b.innerStart >= this.innerStart || b.innerEnd <= this.innerEnd)
                    //&& !this.makesReadRegionTooLong(start, innerStart, b.Start(), b.getInnerStart(), (b.innerStart - b.Start()))
                    //&& !this.makesReadRegionTooLong(innerEnd, end, b.getInnerEnd(), b.End(), (b.innerStart - b.Start()))
                    && svTypeConsistency(this.svType, b.getSVType())){
                // Discordant read overlap
                return true;
            }
        }else if(b.getReadFlags().contains(readEnum.IsSplit)){
            if((this.innerStart < b.innerStart
                && this.innerEnd > b.innerEnd)
                && svTypeConsistency(this.svType, b.getSVType())
                && b.innerEnd - b.innerStart > 0
                && divSup > -1
                && splitSup == -1 ){
                // Split read overlap with divet read pair
                return true;
            }else if(this.innerStart > b.Start()
                && this.innerEnd < b.End()
                && this.Start() < b.innerStart
                && this.End() > b.innerEnd
                && svTypeConsistency(this.svType, b.getSVType())
                && splitSup > 1){
                // Split read overlap with container with existing splits
                return true;
            }else if(this.innerStart == b.innerStart && this.innerEnd == b.innerEnd
                    && svTypeConsistency(this.svType, b.getSVType())){
                return true;
            }
        }else{
            if((b.Start() > this.Start() && b.Start() < this.innerStart)
                || (b.End() > this.innerEnd && b.End() < this.end)){
                // Discordant split read overlap
                if(readFlagConsistency(b.getReadFlags(), b.Start(), b.End()))
                    return true;
            }
        }
        return false;
    }
    /*protected boolean splitCoordConsistency(this.svType, ReadPair b){
    if(svTypeConsistency(this.svType, b.getSVType())){
    switch(this.svType){
    case TANDEM:
    case INSINV:
    return (this.splitSup == -1
    && b.innerStart <= this.Start()
    && b.innerEnd >= this.End()
    && !makesReadRegionTooLong())
    }
    }
    return false;
    }*/
    
    /**
     * Determines if a ReadPair overlaps with this candidate SV
     * @param rflags
     * @param start
     * @param end
     * @return
     */
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
    /**
     *
     * @param a
     * @param b
     * @return
     */
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
    
    private boolean makesReadRegionTooLong(int s1, int s2, int e1, int e2, int insert){
        int[] i = {s1, s2, e1, e2};
        Arrays.sort(i);
        return (i[3] - i[0] > insert * 100);
    }
    
    private void refineStartCoords(int ... a){
        Arrays.sort(a);
        if(a.length != 4){
            throw new NullPointerException("[BEDSET SORT] Array size of 4 expected!");
        }
        this.start = a[0];
        this.innerStart = a[3];
    }
    
    private void refineEndCoords(int ... a){
        Arrays.sort(a);
        if(a.length != 4){
            throw new NullPointerException("[BEDSET SORT] Array size of 4 expected!");
        }
        this.innerEnd = a[0];
        this.end = a[3];
    }
    
    /**
     *
     * @param start
     * @param end
     * @param innerStart
     * @param innerEnd
     */
    protected void refineCoords(int start, int end, int innerStart, int innerEnd){
        if(this.start == 0 && this.end == 0){
            this.start = start;
            this.end = end;
            this.innerStart = innerStart;
            this.innerEnd = innerEnd;
        }else{
            this.start = MergerUtils.getRefineCoords(this.start, start, false);
            this.end = MergerUtils.getRefineCoords(this.end, end, true);
            this.innerStart = MergerUtils.getRefineCoords(this.innerStart, innerStart, true);
            this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, innerEnd, false);
        }
    }
    
    /**
     *
     * @param <T>
     * @param bedSet
     */
    public <T extends BedSet> void mergeBedSet(T bedSet){
        int[] a = {bedSet.start, bedSet.end, bedSet.innerStart, bedSet.innerEnd};
         //this.refineCoords(bedSet.start, bedSet.end, bedSet.innerStart, bedSet.innerEnd);
        Arrays.sort(a);
        
        
        this.refineStartCoords(this.start, this.innerStart, a[0], a[1]);
        this.refineEndCoords(this.end, this.innerEnd, a[2], a[3]);
        this.svType = (this.svType == null)? bedSet.svType : this.svType;
        
        bedSet.readSequentialFile();
        bedSet.pairs.stream().forEach((r) -> {
            this.bufferedAdd(r);
        });
        bedSet.deleteTemp();
    }
    
    /**
     *
     * @param names
     */
    public void reCalculateValues(HashSet<String> names){
        populateCalculations(names);
    }
    /*
     * Overriden methods
     */
    /**
     *
     */
    @Override
    public void readSequentialFile() {
        if(this.hasTemp()){
            this.openTemp('R');
            try{
                String line;
                while((line = this.handle.readLine()) != null){
                    line = line.trim();
                    String[] segs = line.split("\t");
                    ReadPair temp = new ReadPair(segs);
                    if(temp.getReadFlags().contains(readEnum.IsSplit))
                        this.splitSup += 1;
                    if(temp.getReadFlags().contains(readEnum.IsUnbalanced)){
                        //this.unbalSplit += 1;
                        this.sumUnbalSupport += (double) 1 / (double) Integer.parseInt(segs[8]);
                    }
                    if(temp.getReadFlags().contains(readEnum.IsDisc))
                        this.divSup += 1;
                    this.readNames.put(segs[0], Integer.parseInt(segs[8]));
                    this.pairs.add(temp);
                }
            }catch(IOException ex){
                java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                this.closeTemp('R');
            }
        }
    }

    /**
     *
     */
    @Override
    public void dumpDataToDisk() {
        if(!this.hasTemp())
            this.createTemp(tempFile);
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
            if(this.hasTemp())
                this.dumpDataToDisk();
            else{
                this.createTemp(tempFile);
                this.dumpDataToDisk();
            }
        }
        this.rawReads += 1;
        ReadPair working = (ReadPair)a;
        //this.refineBedCoords(working.Start(), working.End(), working.getInnerEnd(), working.getInnerEnd());
        if(this.start == 0 && this.end == 0){
            this.start = working.Start();
            this.innerStart = working.innerStart;
            this.innerEnd = working.innerEnd;
            this.end = working.End();
        }else{
            this.refineStartCoords(this.start, this.innerStart, working.Start(), working.innerStart);
            this.refineEndCoords(this.end, this.innerEnd, working.End(), working.innerEnd);
        }
        this.chr = working.Chr();
        this.svType = (this.svType == null)? working.svType : this.svType;
        if(working.getReadFlags().contains(readEnum.IsDisc)){
            if(this.divSup == -1)
                divSup = 0;
            divSup++;
        }else if(working.getReadFlags().contains(readEnum.IsSplit)){
            if(this.splitSup == -1)
                splitSup = 0;
            splitSup++;
        }else if(working.getReadFlags().contains(readEnum.IsUnbalanced)){
            if(this.unbalSplit == -1)
                unbalSplit = 0;
            unbalSplit++;
        }
            
        this.pairs.add(working);
    }
    
    public boolean separateSplitSet(){
        if(this.splitSup > 0)
            return true;
        return false;
    }

    /**
     *
     */
    @Override
    public void restoreAll() {
        if(this.readNames.isEmpty()){
            for(ReadPair r : this.pairs){
                this.readNames.put(r.Name(), r.mapcount);
            }
        }
        if(this.hasTemp()){
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

    /**
     *
     */
    @Override
    public void pushAllToDisk() {
        dumpDataToDisk();
    }
    
    /*
     * Lazy loader
     */
    /**
     *
     */
    public void preliminarySetCalcs(){
        
        if(this.splitSup == -1)
            this.splitSup = 0;
        if(this.divSup == -1)
            this.divSup = 0;
        if(this.unbalSplit == -1)
            this.unbalSplit = 0;
        this.restoreAll();
        
    }
    private void populateCalculations(HashSet<String> names){
        this.restoreAll();
        this.sumFullSupport = 0;
        for(String r : this.readNames.keySet()){
            if(!names.contains(r)){
                this.sumFullSupport += (double) 1 / (double) this.readNames.get(r);
            }
        }
        //this.dumpDataToDisk();
    }
    
    /*
     * Getters
     */
    /**
     *
     * @param names
     * @return
     */
    public int splitSupport(HashSet<String> names){
        if(this.splitSup == -1){
            this.populateCalculations(names);
        }
        return this.splitSup;
    }
    
    /**
     *
     * @param names
     * @return
     */
    public int divetSupport(HashSet<String> names){
        if(this.divSup == -1){
            this.populateCalculations(names);
        }
        return this.divSup;
    }
    
    /**
     *
     * @param names
     * @return
     */
    public int unbalSplitSupport(HashSet<String> names){
        if(this.unbalSplit == -1){
            this.populateCalculations(names);
        }
        return this.unbalSplit;
    }
    
    /**
     *
     * @param names
     * @return
     */
    public double SumFullSupport(HashSet<String> names){
        if(this.sumFullSupport == -1.0d){
            this.populateCalculations(names);
        }
        return this.sumFullSupport;
    }
    
    /**
     *
     * @param names
     * @return
     */
    public double SumUnbalSupport(HashSet<String> names){
        if(this.sumUnbalSupport == -1.0d){
            this.populateCalculations(names);
        }
        return this.sumUnbalSupport;
    }
    
    /**
     *
     * @return
     */
    public ArrayList<String> getReadNames(){
        ArrayList<String> names = new ArrayList<>();
        this.readNames.keySet().stream().forEach((s) -> names.add(s));
        return names;
    }
    
    /**
     * Debugging method
     * @return 
     */
    public boolean hasSplitUnbalSupport(){
        return this.splitSup > 0 || this.unbalSplit > 0;
    }
}
