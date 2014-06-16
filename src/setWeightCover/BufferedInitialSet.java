/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import TempFiles.TempBed.BufferedBed;
import TempFiles.TempBuffer;
import dataStructs.BedSet;
import dataStructs.ReadPair;
import dataStructs.callEnum;
import file.BedAbstract;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import stats.MergerUtils;

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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    
}
