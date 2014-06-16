/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import dataStructs.readEnum;
import file.BedAbstract;
import java.util.EnumSet;

/**
 *
 * @author bickhart
 */
public abstract class WeightedBed extends BedAbstract implements WeightedSet {
    protected EnumSet<readEnum> rFlags;
    protected double weight;


    @Override
    public void setUsed() {
        rFlags.add(readEnum.Used);
    }

    @Override
    public double retWeight() {
        return this.weight;
    }

    @Override
    public boolean checkUsed() {
        return (rFlags.contains(readEnum.Used)? true : false);
    }
    
    public EnumSet<readEnum> getReadFlags(){
        return this.rFlags;
    }
}
