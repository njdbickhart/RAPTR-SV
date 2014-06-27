/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedAbstract;
import file.BedFileException;
import EnumSetUtils.EnumStringParser;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import setWeightCover.WeightedBed;
import workers.FlatFile;

/**
 *
 * @author bickhart
 */
public class ReadPair extends WeightedBed{
    /**
     *
     */
    protected int innerStart = -1;
    /**
     *
     */
    protected int innerEnd = -1;
    /**
     *
     */
    protected int mapcount = 1;
    /**
     *
     */
    protected FlatFile group;
    /**
     *
     */
    protected double ProbBasedPhred = 0.0d;
    /**
     *
     */
    protected callEnum svType;
    /**
     *
     */
    protected int anchorStart = -1;
    /**
     *
     */
    protected String anchorChr = "NA";

    
    /**
     *
     * @param line
     * @param group
     * @param tag
     */
    public ReadPair(String line, FlatFile group, readEnum tag){
        this.rFlags = EnumSet.noneOf(readEnum.class);
        this.group = group;
        line = line.trim();
        String[] segs = line.split("\t");
        
        if(tag == readEnum.IsDisc){
            this.convertDisc(segs);
        }else if(tag == readEnum.IsSplit){
            this.convertSplit(segs);
        }
    }
    
    /**
     *
     * @param split
     * @param group
     * @param tag
     */
    public ReadPair(pairSplit split, FlatFile group, readEnum tag){
        this.group = group;
        if(tag == readEnum.IsSplit){
            this.convertSplit(split);
        }
    }
    
    /**
     *
     * @param segs
     */
    public ReadPair(String segs[]){
        this.name = segs[0];
        this.chr = segs[1];
        this.start = Integer.parseInt(segs[2]);
        this.innerStart = Integer.parseInt(segs[3]);
        this.innerEnd = Integer.parseInt(segs[4]);
        this.end = Integer.parseInt(segs[5]);
        // Make sure that the numbers are in the proper order for comparisons later!
        this.reformatNums();
        this.anchorStart = Integer.parseInt(segs[6]);
        this.anchorChr = segs[7];
        this.mapcount = Integer.parseInt(segs[8]);
        this.ProbBasedPhred = Double.parseDouble(segs[9]);
        this.rFlags = EnumSetUtils.EnumStringParser.valueOf(readEnum.class, segs[10]);
    }
    private void reformatNums(){
        int a[] = {this.start, this.innerStart, this.innerEnd, this.end};
        Arrays.sort(a);
        this.start = a[0];
        this.innerStart = a[1];
        this.innerEnd = a[2];
        this.end = a[3];
    }
    
    /**
     *
     * @param t
     * @return
     */
    @Override
    public int compareTo(BedAbstract t) {
        return this.start - t.Start();
    }

    /**
     *
     */
    @Override
    public void calcWeight() {
        this.weight = (double) 1 / (double) this.mapcount;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String toString(){
        StringBuilder temp = new StringBuilder();
        temp.append(name).append("\t").append(chr).append("\t");
        temp.append(start).append("\t").append(innerStart).append("\t");
        temp.append(innerEnd).append("\t").append(end).append("\t");
        temp.append(anchorStart).append("\t").append(anchorChr).append("\t");
        temp.append(mapcount).append("\t").append(ProbBasedPhred).append("\t");
        temp.append(rFlags.toString()).append(System.lineSeparator());
        return temp.toString();
    }
    
    /**
     *
     * @param readName
     * @return
     */
    public String getCloneName(String readName){
        String clone;
        String[] nameSplit = readName.split("[/_]");
        clone = nameSplit[0];
        return clone;
    }
    
    /*
     * Getters
     */
    /**
     *
     * @return
     */
    public int getInnerStart(){
        return this.innerStart;
    }
    /**
     *
     * @return
     */
    public int getInnerEnd(){
        return this.innerEnd;
    }
    /**
     *
     * @return
     */
    public Enum<callEnum> getSVType(){
        return this.svType;
    }
    /*
     * Setters
     */
    
    /**
     *
     * @param count
     */
    public void setMapCount(int count){
        this.mapcount = count;
    }
    
    /*
     * Private Converters
     */
    
    private void convertDisc(String[] segs){
        this.rFlags.add(readEnum.IsDisc);
        try {
            initialVals(segs[1], segs[2], segs[6]);
        } catch (BedFileException ex) {
            Logger.getLogger(divet.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.innerStart = Integer.parseInt(segs[3]);
        this.innerEnd = Integer.parseInt(segs[5]);
        // Ensure that the numbers are formatted correctly!
        this.reformatNums();
        
        if(segs[4].equals("F")){
            this.rFlags.add(readEnum.FirstForward);
        }else{
            this.rFlags.add(readEnum.FirstReverse);
        }
        
        if(segs[7].equals("F")){
            this.rFlags.add(readEnum.SecondForward);
        }else{
            this.rFlags.add(readEnum.SecondReverse);
        }
        
        this.ProbBasedPhred = Double.parseDouble(segs[11]);
        strToEnum(segs[8]);
        this.name = segs[0].trim();
    }
    
    private void convertSplit(String[] segs){
        this.rFlags.add(readEnum.IsSplit);
    }
    
    private void convertSplit(pairSplit split){
        this.rFlags.add(readEnum.IsSplit);
        
        this.anchorChr = split.retAnchor().Chr();
        this.anchorStart = split.retAnchor().Start();
        if(split.retAnchor().forward){
            this.rFlags.add(readEnum.HasOEAForward);
        }else{
            this.rFlags.add(readEnum.HasOEAReverse);
        }
        
        if(!split.isBalanced()){
            // Unbalanced split handling
            this.rFlags.add(readEnum.IsUnbalanced);
            if(split.retFirstSplit().splitFirst){
                this.rFlags.add(readEnum.SplitFirstHalf);
            }else{
                this.rFlags.add(readEnum.SplitSecondHalf);
            }
            if(split.retFirstSplit().forward){
                this.rFlags.add(readEnum.FirstForward);
            }else{
                this.rFlags.add(readEnum.FirstReverse);
            }
            this.svType = callEnum.UNBALANCEDINV;
            this.chr = split.Chr();
            this.start = split.Start();
            this.end = split.End();
            this.ProbBasedPhred = split.AvgProb();
        }else{
            // Balanced split handling
            this.chr = split.Chr();
            splitRead firstSplit = split.retFirstSplit();
            splitRead secondSplit = split.retSecondSplit();
            this.svType = orientToEnum(firstSplit.Start(), secondSplit.Start(), firstSplit.forward, secondSplit.forward);
            
            this.start = split.Start();
            int[] sorted = sortedCoords(firstSplit.Start(), firstSplit.End(), secondSplit.Start(), secondSplit.End());
            this.innerStart = sorted[1];
            this.innerEnd = sorted[2];
            this.end = split.End();
            this.reformatNums();
            this.ProbBasedPhred = split.AvgProb();   
        }
    }
    private int[] sortedCoords(int ... a){
        Arrays.sort(a);        
        return a;
    }
    
    private callEnum orientToEnum(int start1, int start2, boolean forward1, boolean forward2){
        if((start1 > start2 && forward1 && !forward2) ||
                (start1 < start2 && !forward1 && forward2)){
            return callEnum.EVERSION;
        }else if ((start1 < start2 && !forward1 && !forward2) ||
                (start1 > start2 && forward1 && forward2)){
            return callEnum.INVERSION;
        }else if ((start1 < start2 && forward1 && !forward2)
                || (start1 > start2 && !forward1 && forward2)){
            return callEnum.DELETION;
        }else{
            return callEnum.DELINV;
        }
    }
    
    private void strToEnum(String call){
        switch(call){
            case "insertion" :
                this.svType = callEnum.INSERTION; return;
            case "deletion" :
                this.svType = callEnum.DELETION; return;
            case "insinv" :
                this.svType = callEnum.INSINV; return;
            case "delinv" :
                this.svType = callEnum.DELINV; return;
            case "eversion" :
                this.svType = callEnum.EVERSION; return;
            case "inversion" :
                this.svType = callEnum.INVERSION; return;
            default :
                this.svType = callEnum.MAXDIST;            
        }
    }
    

}
