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
public class Inversions extends finalSets{
    private finalSets ForwardSupport;
    private finalSets ReverseSupport;
    private boolean complete = false;
    
    public Inversions(BufferedInitialSet a, HashSet<String> names, boolean debug){
        // Inversions are strange, since they require evidence on both ends in order to be confirmed
        // I need to find a supporting inversion call downstream of my leftmost coordinate before completing the set
        // Unbalanced inversions will still be reported, but will be untrustworthy
        this.chr = a.Chr();
        this.ForwardSupport = new InversionSet(a, names, debug);
    }
    
    private class InversionSet extends finalSets{
        public InversionSet(BufferedInitialSet a, HashSet<String> names, boolean debug){
            super.initialize(a, names, debug);
        }
        
    }
    public void PresentAsIs(){
        // this is designed to finalize the unbalanced inversions for printout
        if(!complete){
            FinalizeValues(false);
        }
    }
    public void AddReverseSupport(BufferedInitialSet a, HashSet<String> names, boolean debug){
        this.ReverseSupport = new InversionSet(a, names, debug);
        this.complete = true;
        FinalizeValues(true);
    }
    private void FinalizeValues(boolean full){
        if(full){
            this.start = this.ForwardSupport.InnerStart();
            this.innerStart = this.ForwardSupport.InnerEnd();
            this.innerEnd = this.ReverseSupport.InnerStart();
            this.end = this.ReverseSupport.InnerEnd();
            this.splitSupport = this.ForwardSupport.SplitSupport() + this.ReverseSupport.SplitSupport();
            this.discSupport = this.ForwardSupport.DiscSupport() + this.ReverseSupport.DiscSupport();
            this.unbalancedSplitSupport = this.ForwardSupport.UnbalancedSplitSupport() + this.ReverseSupport.UnbalancedSplitSupport();
            this.sumFullSupport = this.ForwardSupport.SumFullSupport() + this.ReverseSupport.SumFullSupport();
            this.sumUnbalSupport = this.ForwardSupport.SumUnbalSupport() + this.ReverseSupport.SumUnbalSupport();
            this.svType = callEnum.INVERSION;
        }else{
            this.start = this.ForwardSupport.Start();
            this.innerStart = this.ForwardSupport.InnerStart();
            this.innerEnd = this.ForwardSupport.InnerEnd();
            this.end = this.ForwardSupport.End();
            this.splitSupport = this.ForwardSupport.SplitSupport();
            this.discSupport = this.ForwardSupport.DiscSupport();
            this.unbalancedSplitSupport = this.ForwardSupport.UnbalancedSplitSupport();
            this.sumFullSupport = this.ForwardSupport.SumFullSupport();
            this.sumUnbalSupport = this.ForwardSupport.SumUnbalSupport();
            this.svType = callEnum.UNBALANCEDINV;
        }
    }
    public boolean IsComplete(){
        return this.complete;
    }
    public void SetComplete(){
        this.complete = true;
    }
    public finalSets RetForward(){
        return this.ForwardSupport;
    }
    public finalSets RetReverse(){
        return this.ReverseSupport;
    }
}
