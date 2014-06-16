/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import TempFiles.TempBed.BufferedBed;
import TempFiles.TempBuffer;
import TempFiles.TempDataClass;
import com.sun.istack.internal.logging.Logger;
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
public class BufferedInitialSet extends BufferedBed implements TempBuffer<BedAbstract>{
    protected int innerStart;
    protected int innerEnd;
    protected callEnum svType;
    protected ArrayList<ReadPair> pairs = new ArrayList<>(10);
    protected int maxBuffer = 10;
    
    public BufferedInitialSet(int buffer, String tempdir){
        this.maxBuffer = buffer;
        this.createTemp(Paths.get(tempdir));
    }
    
    private void refineCoords(int start, int end, int innerStart, int innerEnd){
        this.start = MergerUtils.getRefineCoords(this.start, start, false);
        this.end = MergerUtils.getRefineCoords(this.end, end, true);
        this.innerStart = MergerUtils.getRefineCoords(this.innerStart, innerStart, true);
        this.innerEnd = MergerUtils.getRefineCoords(this.innerEnd, innerEnd, false);
    }
    
    /*
     * Overriden methods
     */
    @Override
    public int compareTo(BedAbstract t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readSequentialFile() {
        this.openTemp('R');
        try{
            String line;
            while((line = this.handle.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                ReadPair temp = new ReadPair(segs);
                this.pairs.add(temp);
            }
        }catch(IOException ex){
            java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            this.closeTemp('R');
        }
        
    }

    @Override
    public void dumpDataToDisk() {
        this.openTemp('A');
        try{
            for(ReadPair a : this.pairs){
                this.output.write(a.toString());
            }
        }catch(IOException ex){
            java.util.logging.Logger.getLogger(BufferedInitialSet.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            this.closeTemp('A');
        }
        this.pairs.clear();
    }

    @Override
    public <BedAbstract> void bufferedAdd(BedAbstract a) {
        if(this.pairs.size() >= this.maxBuffer){
            this.dumpDataToDisk();            
        }
        ReadPair working = (ReadPair)a;
        refineCoords(working.Start(), working.End(), working.getInnerEnd(), working.getInnerEnd());
        this.pairs.add(working);
    }

    @Override
    public void restoreAll() {
        readSequentialFile();
    }

    @Override
    public void pushAllToDisk() {
        dumpDataToDisk();
    }
    
}
