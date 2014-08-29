/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author bickhart
 * @param <T>
 */
public class SetMap<T extends BedSet> extends BedMap<T>{
    public SetMap(){
        super();
    }
    
    public <V extends ReadPair> boolean checkAndCombinePairs(V pair){
        if(this.containsChr(pair.Chr())){
            for(int b : utils.BinBed.getBins(pair.Start(), pair.End())){
                if(this.containsBin(pair.Chr(), b)){
                    for(T set : this.getBedAbstractList(pair.Chr(), b)){
                        if(set.pairOverlaps(pair)){
                            if(!pair.getReadFlags().contains(readEnum.IsUnbalanced) && pair.innerEnd > set.innerStart)
                                set.addReadPair(pair); // Only if the pair does not make the interior coordinates squished!
                            if(pair.getReadFlags().contains(readEnum.IsUnbalanced))
                                set.addReadPair(pair); // Just in case our read is an unbalanced split!
                            return true; // Even if the pair wasn't added, we reached the interior of the conditional, so we should report an overlap
                        }
                    }
                }
            }
        }
       return false;
    }
    
    public boolean checkAndCombineSets(T bed){
        boolean found = false;
        if(this.containsChr(bed.Chr())){
            for(int b : utils.BinBed.getBins(bed.innerStart, bed.innerEnd)){
                if(this.containsBin(bed.Chr(), b)){
                    for(T set : this.getBedAbstractList(bed.Chr(), b)){
                        if(setOverlaps(set, bed)){
                            found = true;
                            if(bed.innerEnd > set.innerStart)
                                set.mergeBedSet(bed);// Only add the bed to the set if does not scrunch coordinates
                            // Otherwise, skip it!
                            break;
                        }
                    }
                }
            }
        }
        if(!found)
            this.addBedData(bed);
        return found;
    }
    
    private boolean exteriorOverlap(T a, T b){
        return((a.Start() < b.End()
                && a.End() > b.Start())
                && svTypeConsistency(a.svType, b.svType));
    }
    
    private boolean setOverlaps(T a, T b){
        return((a.innerStart < b.innerEnd
                && a.innerEnd > b.innerStart)
                && !makesReadRegionTooLong(a.Start(), a.innerStart, b.Start(), b.innerStart, 200)
                && !makesReadRegionTooLong(a.innerEnd, a.End(), b.innerEnd, b.End(), 200)
                && svTypeConsistency(a.svType, b.svType));
    }
    
    private boolean makesReadRegionTooLong(int s1, int s2, int e1, int e2, int insert){
        int[] i = {s1, s2, e1, e2};
        Arrays.sort(i);
        return (i[3] - i[0] > insert * 100);
    }
    public ArrayList<BedSet> getUnsortedBedList(String chr){
        List<BedSet> working = this.getBins(chr)
                .parallelStream()
                .flatMap((s) -> this.getBedAbstractList(chr, s).stream())
                .collect(Collectors.toList());
        return (ArrayList<BedSet>)working;
    }
    
    public int getCountElements(String chr){
        int c = this.getBins(chr)
                .parallelStream()
                .flatMap((s) -> this.getBedAbstractList(chr, s).parallelStream())
                .mapToInt((s) -> 1).sum();
        return c;
    }
    
    protected boolean svTypeConsistency(Enum<callEnum> a, Enum<callEnum> b){
        if(a.equals(b)){
            return true;
        }else if ((a.equals(callEnum.DELETION) && b.equals(callEnum.DELINV))
                || (a.equals(callEnum.DELINV) && b.equals(callEnum.DELETION))
                || (a.equals(callEnum.INSERTION) && b.equals(callEnum.INSINV))
                || (a.equals(callEnum.INSINV) && b.equals(callEnum.INSERTION))
                || (a.equals(callEnum.EVERSION) && b.equals(callEnum.TANDEM))
                || (a.equals(callEnum.TANDEM) && b.equals(callEnum.EVERSION))){
            return true;
        }
        return false;
    }
}
