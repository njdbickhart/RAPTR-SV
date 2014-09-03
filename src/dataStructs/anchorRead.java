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
public class anchorRead {
    public final String chr;
    public final int start;
    public final boolean forward; // true = forward, false = reverse
    public final double probPhred; // 
    
    public anchorRead(String chr, int start, int samFlag, int mapQ){
        this.chr = chr;
        this.start = start;
        this.forward = (samFlag & 0x10) != 0x10;
        this.probPhred = 1.0d - Math.pow(10d, mapQ / -10d);
    }
}
