/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package finalSVTypes;

import dataStructs.callEnum;
import java.util.HashSet;
import setWeightCover.BufferedInitialSet;
import setWeightCover.InitialSet;
import setWeightCover.finalSets;

/**
 *
 * @author bickhart
 */
public class TandDup extends finalSets{
    
    public TandDup(BufferedInitialSet a, HashSet<String> names, boolean debug){
        super.initialize(a, names, debug);
        this.svType = callEnum.TANDEM;
    }
    
}
