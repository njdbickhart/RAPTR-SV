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
    
    public anchorRead(String chr, String start, String end, String readName, String samFlag, String nmTag, String mmTag, String qual){
        try {
            initialVals(chr, start, end);
            this.name = readName;
        } catch (BedFileException ex) {
            Logger.getLogger(anchorRead.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(samFlag.equals("16")){
            this.forward = false;
        }else{
            this.forward = true;
        }
        String[] mm = mmTag.split(":");
        calcProbs(qual, mm[2]);
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
