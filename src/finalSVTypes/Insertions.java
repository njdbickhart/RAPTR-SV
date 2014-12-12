/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package finalSVTypes;

import dataStructs.callEnum;
import java.util.HashSet;
import setWeightCover.BufferedInitialSet;
import setWeightCover.finalSets;

/**
 *
 * @author bickhart
 */
public class Insertions extends finalSets{
    
    public Insertions(BufferedInitialSet a, HashSet<String> names, boolean debug){
        super.initialize(a, names, debug);
        this.svType = callEnum.INSERTION;
    }
}
