/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedAbstract;
import file.BedFileException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import setWeightCover.WeightedBed;
import setWeightCover.WeightedSet;
import workers.FlatFile;

/**
 *
 * @author bickhart
 */
public class divet extends WeightedBed{
    protected int end1; // the end of the first read
    protected int start2; // the end of the second read
    protected boolean forward1; // the orientation of the first read
    protected boolean forward2; // the orientation of the second read
    protected callEnum svType;
    protected int sumEdit;
    protected int mapping;
    protected FlatFile group;
    protected double avgPhred;
    protected double probPhred;
    public String[] divout;
    
    public divet (String divet, FlatFile group){
        divet = divet.replaceAll("\n", "");
        String[] segs = divet.split("\t");
        try {
            initialVals(segs[1], segs[2], segs[6]);
        } catch (BedFileException ex) {
            Logger.getLogger(divet.class.getName()).log(Level.SEVERE, null, ex);
        }
        insertCoords(segs[3], segs[5]);
        this.forward1 = orientDetermine(segs[4]);
        this.forward2 = orientDetermine(segs[7]);
        getStats(segs[9], segs[10], segs[11]);
        strToEnum(segs[8]);
        this.name = segs[0].trim();
        this.group = group;
    }
    
    /**
     * Constructor for SamToDivet converter
     * @param values 
     */
    public divet(String ... values){
        divout = values;
    }
    @Override
    public int compareTo(BedAbstract t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    private void insertCoords(String end1, String start2){
        try{
            this.end1 = Integer.parseInt(end1);
            this.start2 = Integer.parseInt(start2);
        }catch(NumberFormatException ex){
            Logger.getLogger(divet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Since the read mappings do not follow coordinate conventions, sort the coordinates and reassign values
        int[] coords = {this.start, this.end, this.end1, this.start2};
        Arrays.sort(coords);
        this.start = coords[0];
        this.end1 = coords[1];
        this.start2 = coords[2];
        this.end = coords[3];
    }
    private boolean orientDetermine(String orientStr){
        if(orientStr.equals("F")){
            return true;
        }else{
            return false;
        }
    }
    private void getStats(String sumEdit, String avgPhred, String probPhred){
        try{
            this.sumEdit = Integer.parseInt(sumEdit);
            this.avgPhred = Double.parseDouble(avgPhred);
            this.probPhred = Double.parseDouble(probPhred);
        }catch(NumberFormatException ex){
            Logger.getLogger(divet.class.getName()).log(Level.SEVERE, null, ex);
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
                this.svType = callEnum.INSERTION; return;
            default :
                this.svType = callEnum.MAXDIST;            
        }
    }
    public void setMapping(int num){
        this.mapping = num;
        calcWeight();
    }
    @Override
    public void calcWeight() {
        this.weight = (double) 1 / (double) this.mapping;
    }
    public int End1(){
        return this.end1;
    }
    public int Start2(){
        return this.start2;
    }
    public callEnum SvType(){
        return this.svType;
    }
    public double ProbPhred(){
        return this.probPhred;
    }
    public double AvgPhred(){
        return this.avgPhred;
    }
    public double StDev(){
        return this.group.getStdev();
    }
}
