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
public class Deletions extends finalSets{
    
    public Deletions(InitialSet a){
        super.initialize(a);
        this.svType = callEnum.DELETION;
    }
}
