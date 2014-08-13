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
public class anchorRead extends BedAbstract{
    protected boolean forward; // true = forward, false = reverse
    private double probPhred;
    
    public anchorRead(String chr, int start, int end, String readName, int samFlag, String mdVal, String qual){
        
        this.chr = chr;
        this.start = start;
        this.end = end;
        this.name = readName;
        this.forward = samFlag == 0;
        calcProbs(qual, mdVal);
    }
    
    @Override
    public int compareTo(BedAbstract t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    private void calcProbs(String qStr, String mmTag){
        this.probPhred = stats.probBasedPhred.calculateScore(mmTag, qStr, qStr.length());
    }
    public boolean getFor(){
        return this.forward;
    }
    public double ProbPhred(){
        return this.probPhred;
    }
}
