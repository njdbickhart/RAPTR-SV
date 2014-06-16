/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package finalSVTypes;

import dataStructs.callEnum;
import gnu.trove.set.hash.THashSet;
import setWeightCover.BufferedInitialSet;
import setWeightCover.InitialSet;
import setWeightCover.finalSets;

/**
 *
 * @author bickhart
 */
public class Insertions extends finalSets{
    
    public Insertions(BufferedInitialSet a, THashSet<String> names){
        super.initialize(a, names);
        this.svType = callEnum.INSERTION;
    }
}
