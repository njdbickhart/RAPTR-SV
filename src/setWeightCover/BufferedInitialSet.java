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
