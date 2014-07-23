/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package setWeightCover;

import StrUtils.StrArray;
import dataStructs.callEnum;
import file.BedAbstract;
import java.util.HashSet;

/**
 *
 * @author bickhart
 */
public abstract class finalSets extends BedAbstract{
    protected int innerStart;
    protected int innerEnd;
    protected int splitSupport;
    protected int unbalancedSplitSupport;
    protected int discSupport;
    protected double sumFullSupport;
    protected double sumUnbalSupport;
    protected BufferedInitialSet supportingReads;
    public callEnum svType;
    
    public void initialize(BufferedInitialSet a, HashSet<String> names, boolean debug){
        this.chr = a.Chr();
        //refineMapsRemainingReads(a);
        if(debug)
            supportingReads = a;
        setMapData(a, names);
    }
    private void setMapData(BufferedInitialSet a, HashSet<String> names){
        this.start = a.Start();
        this.end = a.End();
        this.innerStart = a.innerStart;
        this.innerEnd = a.innerEnd;
        this.splitSupport = a.splitSupport(names);
        this.discSupport = a.divetSupport(names);
        this.unbalancedSplitSupport = a.unbalSplitSupport(names);
        this.sumFullSupport = a.SumFullSupport(names);
        this.sumUnbalSupport = a.SumUnbalSupport(names);
        names.addAll(a.getReadNames());
    }
    
    public String getSupportReadStr(){
        BufferedInitialSet a = supportingReads;
        a.restoreAll();
        String[] values = {a.Chr(), String.valueOf(a.Start()), String.valueOf(a.innerStart),
        String.valueOf(a.innerEnd), String.valueOf(a.End()), svType.toString(),
        String.valueOf(this.discSupport), String.valueOf(this.splitSupport),
        String.valueOf(this.unbalancedSplitSupport), StrArray.Join(a.getReadNames(), ";")};        
        return StrArray.Join(values, "\t");
    }
    
    @Override
    public int compareTo(BedAbstract t) {
        finalSets working = (finalSets) t;
        return (int) working.SumFullSupport() - (int) this.sumFullSupport;
    }
    public int InnerStart(){
        return this.innerStart;
    }
    public int InnerEnd(){
        return this.innerEnd;
    }
    public int SplitSupport(){
            return this.splitSupport;
    }
    public int UnbalancedSplitSupport(){
        return this.unbalancedSplitSupport;
    }
    public int DiscSupport(){
        return this.discSupport;
    }
    public double SumFullSupport(){
        return this.sumFullSupport;
    }
    public double SumUnbalSupport(){
        return this.sumUnbalSupport;
    }
}
