/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import TempFiles.TempBed.BufferedBed;
import dataStructs.callEnum;
import dataStructs.divet;
import dataStructs.pairSplit;
import file.BedAbstract;
import java.util.ArrayList;

/**
 *
 * @author bickhart
 */
public class BufferedInitialSet extends BufferedBed{
    protected int innerStart;
    protected int innerEnd;
    protected callEnum svType;
    protected ArrayList<divet> discReads;
    protected ArrayList<pairSplit> splitReads;
    
    @Override
    public int compareTo(BedAbstract t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readSequentialFile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void dumpDataToDisk() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> void bufferedAdd(T a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void restoreAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pushAllToDisk() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
