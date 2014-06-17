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
    private anchorRead anchor;
    private splitRead split1;
    private splitRead split2;
    private int readlength;
    private boolean isBalanced; // flag for determining if there is one split or two in the set
    private int mappings = 0;
    private double avgProb;

    public pairSplit(anchorRead anchor, splitRead split1, splitRead split2){
        this.anchor = anchor;
        this.chr = anchor.Chr();
        this.split1 = split1;
        this.readlength = split1.End() - split1.Start();
        this.split2 = split2;
        this.setName(getCloneName(anchor.Name()));
        SplitCoords coords = new SplitCoords(split1.Start(), split1.End(), split2.Start(), split2.End());
        this.start = coords.OutStart();
        this.end = coords.OutEnd();
        this.isBalanced = true;
        this.avgProb = calcAvgProb(anchor, split1, split2);
    }
    public pairSplit(anchorRead anchor, splitRead split1){
        this.anchor = anchor;
        this.chr = anchor.Chr();
        this.start = split1.Start();
        this.end = split1.End();
        this.split1 = split1;
        this.readlength = split1.End() - split1.Start();
        this.setName(getCloneName(anchor.Name()));
        this.isBalanced = false;
        this.avgProb = calcAvgProb(anchor, split1);
    }
    private double calcAvgProb(anchorRead anchor, splitRead split1, splitRead split2){
        double sum;
        sum = anchor.ProbPhred();
        sum += split1.ProbPhred();
        sum += split2.ProbPhred();
        sum /= 3;
        return sum;
    }
    private double calcAvgProb(anchorRead anchor, splitRead split1){
        double sum;
        sum = anchor.ProbPhred();
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
    private class SplitCoords{
        public int outStart;
        public int inStart;
        public int outEnd;
        public int inEnd;
        public SplitCoords(int ... a){
            Arrays.sort(a);
            this.outStart = a[0];
            this.inStart = a[1];
            this.inEnd = a[2];
            this.outEnd = a[3];
        }
        public int OutStart(){
            return this.outStart;
        }
        public int InStart(){
            return this.inStart;
        }
        public int InEnd(){
            return this.inEnd;
        }
        public int OutEnd(){
            return this.outEnd;
        }
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
    private String getCloneName(String readName){
        String clone;
        String[] nameSplit = readName.split("[/_]");
        clone = nameSplit[0];
        return clone;
    }
    public int retReadLen(){
        return this.readlength;
    }
}
