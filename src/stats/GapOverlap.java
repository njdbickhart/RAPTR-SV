/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

import file.BedAbstract;
import file.BedMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author bickhart
 */
public class GapOverlap {
    private final GapMap gapList;    
    
    public GapOverlap(String inFile){
        this.gapList = new GapMap(Paths.get(inFile), 0);
    }
    public boolean checkGapOverlap(BedAbstract bed){
        return gapList.overlap(bed);
    }
    
    protected class GapMap extends BedMap{
        public GapMap(Path inFile, int type){
            super(inFile, type);
        }
        public boolean overlap (BedAbstract bed){
            Set<Integer> bins = utils.BinBed.getBins(bed.Start(), bed.End());
            for(int b : bins){
                ArrayList<BedAbstract> bedList = this.getBedAbstractList(bed.Chr(), b);
                if(bedList == null){
                    continue;
                }
                for(BedAbstract ba : bedList){
                    if(stats.MergerUtils.checkOverlap(ba.Start(), bed.Start(), ba.End(), bed.End())){
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
