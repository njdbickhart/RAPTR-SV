/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author bickhart
 */
public class MedianAbsoluteDeviation {
    public static int MAD(ArrayList<Integer> values){
        int med = Median(values);
        List<Integer> deviations = values.stream()
                .map((s) -> {return Math.abs(med - s);})
                .collect(Collectors.toList());
        return Median(deviations);
    }
    
    public static int Median(List<Integer> values){
        Collections.sort(values);
        if(values.size() % 2 == 0){
            return values.get(values.size() / 2);
        }else{
            List<Integer> temp = new ArrayList<>();
            temp.add(values.get(values.size() / 2));
            temp.add(values.get((values.size() / 2) + 1));
            return (int) Math.floor(stats.StdevAvg.IntAvg(temp));
        }
    }
}
