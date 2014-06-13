/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

import java.util.Comparator;
import setWeightCover.InitialSet;

/**
 *
 * @author bickhart
 */
public class SortWeightMap implements Comparator<InitialSet>{

    @Override
    public int compare(InitialSet t, InitialSet t1) {
        /*// If the set has no support from both groups, but the other does, move it towards the bottom
         * if(t.divetSupport() == 0 && (t1.divetSupport() > 0 && t1.splitSupport() > 0)){
         * return 1;
         * }else if(t.splitSupport() == 0 && (t1.divetSupport() > 0 && t1.splitSupport() > 0)){
         * return 1;
         * }else if(t1.divetSupport() == 0 && (t.divetSupport() > 0 && t.splitSupport() > 0)){
         * return -1;
         * }else if(t1.splitSupport() == 0 && (t.divetSupport() > 0 && t.splitSupport() > 0)){
         * return -1;
         * }*/

        // Check the full support first, then use the unbalanced split support as a tie breaker
        if(t.SumFullSupport() < t1.SumFullSupport()) {
            return 1;
        }else if (t.SumFullSupport() > t1.SumFullSupport()) {
            return -1;
        }else{
            if (t.SumUnbalSupport() < t1.SumUnbalSupport()) {
                return 1;
            }else if (t.SumUnbalSupport() > t1.SumUnbalSupport()) {
                return -1;
            }else {
                return 0;
            }
        }
    }
    
}
