/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bickhart
 */
public class StdevAvg {
    public static double IntAvg(List<Integer> sum){
        if(sum.isEmpty()){
            return 0.0d;
        }
        double s = 0.0d;
        for(int d : sum){
            s += d;
        }
        return s / (double) sum.size();
    } 
    
    public static double DoubleAvg(List<Double> sum){
        if(sum.isEmpty()){
            return 0.0d;
        }
        double s = 0.0d;
        for(double d : sum){
            s += d;
        }
        return s / sum.size();
    }
    public static float FloatAvg (float[] sum){
        if(sum.length == 0){
            return 0.0f;
        }
        float s = 0.0f;
        for(int x = 0; x < sum.length; x++){
            s += sum[x];
        }
        return s / sum.length;
    }
    
    public static double convertFltAvg(ArrayList<Float> sum){
        if(sum.size() == 0){
            return 0.0d;
        }
        double d = 0.0d;
        for(int x = 0; x < sum.size(); x++){
            d += (double) sum.get(x);
        }
        return d / (double) sum.size();
    }
    
    public static double stdevFlt(ArrayList<Float> sum){
        if(sum.size() == 0){
            return 0;
        }
        double mean = convertFltAvg(sum);
        double dev = 0.0d;
        for(int x = 0; x < sum.size(); x++){
            dev += (double) Math.pow(sum.get(x) - mean, 2.0d);
        }
        double variance = dev / (double) (sum.size() - 1);
        return Math.sqrt(variance);
    }
    
    public static double stdevFlt(double avg, ArrayList<Float> sum){
        if(sum.isEmpty() || sum.size() == 1){
            return 0;
        }        
        double dev = 0.0d;
        for(int x = 0; x < sum.size(); x++){
            dev += (double) Math.pow(sum.get(x) - avg, 2.0d);
        }
        double variance = dev / (double) (sum.size() - 1);
        return Math.sqrt(variance);
    }
    
    public static double stdevInt(double avg, ArrayList<Integer> sum){
        if(sum.isEmpty() || sum.size() == 1){
            return 0;
        }        
        double dev = 0.0d;
        for(int x = 0; x < sum.size(); x++){
            dev += (double) Math.pow(sum.get(x) - avg, 2.0d);
        }
        double variance = dev / (double) (sum.size() - 1);
        return Math.sqrt(variance);
    }
    
    public static double stdevDBL(double avg, List<Double> sum){
        if(sum.isEmpty() || sum.size() == 1){
            return 0;
        }        
        double dev = 0.0d;
        for(double d : sum){
            dev += (double) Math.pow(d - avg, 2.0d);
        }
        double variance = dev / (double) (sum.size() - 1);
        return Math.sqrt(variance);
    }
}
