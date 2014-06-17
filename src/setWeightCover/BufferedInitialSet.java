/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import TempFiles.TempBuffer;
import dataStructs.BedSet;
import file.BedAbstract;
import java.nio.file.Paths;

/**
 *
 * @author bickhart
 */
public class BufferedInitialSet extends BedSet implements TempBuffer<BedAbstract>{
    
    
    public BufferedInitialSet(int buffer, String tempdir){
        this.maxBuffer = buffer;
        this.createTemp(Paths.get(tempdir));
        this.chr = null;
        this.start = 0;
        this.end = 0;
    }
    
    /*
     * Overriden methods
     */
    @Override
    public int compareTo(BedAbstract t) {
        BufferedInitialSet working = (BufferedInitialSet) t;
        return (int) working.sumFullSupport - (int) this.sumFullSupport;
    }

    
    
}
