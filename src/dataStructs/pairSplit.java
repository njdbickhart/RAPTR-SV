/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import file.BedAbstract;
import java.util.Arrays;
import setWeightCover.WeightedBed;

/**
 *
 * @author bickhart
 */
public class pairSplit extends WeightedBed{
    private final anchorRead anchor;
    private splitRead split1;
    private splitRead split2;
    private final int readlength;
    private final boolean isBalanced; // flag for determining if there is one split or two in the set
    private int mappings = 0;
    private final double avgProb;

    public pairSplit(anchorRead anchor, splitRead split1, splitRead split2, String cloneName){
        this.anchor = anchor;
        this.chr = anchor.chr;
        this.split1 = split1;
        this.readlength = split1.End() - split1.Start();
        this.split2 = split2;
        this.setName(cloneName);
        Integer[] coords = {split1.Start(), split1.End(), split2.Start(), split2.End()};
        Arrays.sort(coords); // This is just to ensure that the exterior coordinates of the pair are represented
        this.start = coords[0];
        this.end = coords[3];
        this.isBalanced = true;
        this.avgProb = anchor.probPhred;
    }
    public pairSplit(anchorRead anchor, splitRead split1, String cloneName){
        this.anchor = anchor;
        this.chr = anchor.chr;
        this.start = split1.Start();
        this.end = split1.End();
        this.split1 = split1;
        this.readlength = split1.End() - split1.Start();
        this.setName(cloneName);
        this.isBalanced = false;
        this.avgProb = anchor.probPhred;
    }
    
    // I am replacing this method of calculating the value of the split read with just the anchor mapping prob
    // This should help reduce program runtime
    private double calcAvgProb(anchorRead anchor, splitRead split1, splitRead split2){
        double sum;
        sum = anchor.probPhred;
        sum += split1.ProbPhred();
        sum += split2.ProbPhred();
        sum /= 3;
        return sum;
    }
    private double calcAvgProb(anchorRead anchor, splitRead split1){
        double sum;
        sum = anchor.probPhred;
        sum += split1.ProbPhred();
        sum /= 2;
        return sum;
    }
    @Override
    public int compareTo(BedAbstract t) {
        pairSplit temp = (pairSplit) t;
        if(this.isBalanced && temp.isBalanced)
            return 0;
        else if(this.isBalanced && !temp.isBalanced)
            return -1;
        else if(!this.isBalanced && !temp.isBalanced)
            return 0;
        else
            return 1;
    }

    @Override
    public void calcWeight() {
        this.weight = (double) 1 / (double) this.mappings;
    }
    
    // Only calculated for the anchor read
    public void setMappings(int num){
        this.mappings = num;
        calcWeight();
    }
    public boolean isBalanced(){
        return this.isBalanced;
    }
    public splitRead retFirstSplit(){
        return this.split1;
    }
    public splitRead retSecondSplit(){
        return this.split2;
    }
    public anchorRead retAnchor(){
        return this.anchor;
    }
    public double AvgProb(){
        return this.avgProb;
    }
    public int retReadLen(){
        return this.readlength;
    }
}
