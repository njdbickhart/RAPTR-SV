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
public class Deletions extends finalSets{
    
    public Deletions(BufferedInitialSet a, HashSet<String> names){
        super.initialize(a, names);
        this.svType = callEnum.DELETION;
    }
}
