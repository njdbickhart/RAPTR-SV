/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package finalSVTypes;

import dataStructs.callEnum;
import setWeightCover.InitialSet;
import setWeightCover.finalSets;

/**
 *
 * @author bickhart
 */
public class Insertions extends finalSets{
    
    public Insertions(InitialSet a){
        super.initialize(a);
        this.svType = callEnum.INSERTION;
    }
}
