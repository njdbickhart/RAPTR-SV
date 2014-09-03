/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedAbstract;
import file.BedFileException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bickhart
 */
public class splitRead extends BedAbstract{
    protected boolean splitFirst; //true = first half or read // false = second half
    protected boolean forward; // true = forward, false = reverse
    protected double probPhred = 0.0d;
    
    public splitRead(String chr, String start, String end, String name, String samFlag, String nmTag, String mmTag, String qual){
        try {
            initialVals(chr, start, end);
        } catch (BedFileException ex) {
            Logger.getLogger(splitRead.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.name = name;
        determineSplit(name);
        if(samFlag.equals("16")){
            this.forward = false;
        }else{
            this.forward = true;
        }
        String[] mm = mmTag.split(":");
        calcProbs(qual, mm[2]);
    }
    public splitRead(String chr, int start, int end, String name, int samFlag){
        this.chr = chr;
        this.start = start;
        this.end = end;
        this.name = name;
        determineSplit(name);
        if((samFlag & 0x10) == 0x10){
            this.forward = false;
        }else{
            this.forward = true;
        }
        // I removed probability calculation and instead rely on the anchor read to provide this
        //calcEd(nmTag);
        //String[] mm = mmTag.split(":"); 
        //calcProbs(qual, mmTag);
    }
    @Override
    public int compareTo(BedAbstract t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    private void determineSplit(String name){
        String num = name.substring(name.length() - 1);
        if(num.equals("1")){
            this.splitFirst = true;
        }
    }
    private void calcProbs(String qStr, String mmTag){
        this.probPhred = stats.probBasedPhred.calculateScore(mmTag, qStr, qStr.length());
    }
    public boolean getSplitOrient(){
        return this.forward;
    }
    public boolean getSplitNum(){
        return this.splitFirst;
    }
    public double ProbPhred(){
        return this.probPhred;
    }
}
