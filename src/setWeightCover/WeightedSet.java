/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

/**
 *
 * @author bickhart
 */
public interface WeightedSet {
    
    public void calcWeight ();
    
    public void setUsed();
    
    public double retWeight();
    
    public boolean checkUsed();
}
