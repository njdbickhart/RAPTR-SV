/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedMap;

/**
 *
 * @author bickhart
 */
public class SetMap<T extends BedSet> extends BedMap<T>{
    
    public SetMap(){
        super();
    }
    
    public <V extends ReadPair> boolean checkAndCombinePairs(V pair){
        if(this.containsChr(pair.Chr())){
            for(int b : utils.BinBed.getBins(pair.innerStart, pair.innerEnd)){
                if(this.containsBin(pair.Chr(), b)){
                    for(T set : this.getBedAbstractList(pair.Chr(), b)){
                        if(set.pairOverlaps(pair)){
                            set.addReadPair(pair);
                            return true;
                        }
                    }
                }
            }
        }
       return false;
    }
    
    public void checkAndCombineSets(T bed){
       if(this.containsChr(bed.Chr())){
            for(int b : utils.BinBed.getBins(bed.innerStart, bed.innerEnd)){
                for(T set : this.getBedAbstractList(bed.Chr(), b)){
                    if(setOverlaps(set, bed)){
                        set.mergeBedSet(bed);
                        return;
                    }
                }
            }
        }
       this.addBedData(bed);
    }
    
    private boolean setOverlaps(T a, T b){
        if((a.innerStart < b.innerEnd
                && a.innerEnd > b.innerStart)){
            return true;
        }
        return false;
    }
}
