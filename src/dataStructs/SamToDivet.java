/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bickhart
 */
public class SamToDivet {
    private final String clone;
    private final ArrayList<String[]> lines;
    private final ArrayList<divet> divets = new ArrayList<>();
    private final int lower;
    private final int upper;
    private final int cutoff;
    
    public SamToDivet(String clone, int lower, int upper, int cutoff){
        this.lines = new ArrayList<>();
        this.clone = clone;
        this.lower = lower;
        this.upper = upper;
        this.cutoff = cutoff;
    }
    
    public void addLines(String[] line){
        this.lines.add(line);
    }
    
    public void processLinesToDivets() {
        Map<Short, ArrayList<String[]>> holder = new HashMap<>();
        // Put lines into temporary holder for numerical sorting
        for(String[] array : lines){
            short num = Short.parseShort(array[1]);
            if(!holder.containsKey(num))
                holder.put(num, new ArrayList<String[]>());
            holder.get(num).add(array);
        }
        
        // Since we know that there should be only two keys, lets grab the first one
        short comp = 1;
        if(!holder.containsKey(comp)){
            System.err.println("Sam file did not have a first clone!");
            return;
        }
            
        short second = 2;
        if(!holder.containsKey(second)){
            System.err.println("Sam file did not have a second clone!");
            return;
        }
            
        // Divet file format:
    // 20VQ5P1:104:D09KFACXX:7:2106:7435:121010:0      chr27   23699238        23699288        F       23695946        23695996        F       delinv  0       37.47  1.00000000000000000000   1
        
        for(String[] first : holder.get(comp)){
            String forient, fchr = first[4], fstart = first[5], 
                    fend = String.valueOf(Integer.parseInt(first[5]) + first[11].length()),
                    fmdz = this.getMDZTag(first, first[11]);
            int fedit = Integer.parseInt(this.getNMITag(first));
            //double fprob = stats.probBasedPhred.calculateScore(fmdz, first[11], first[11].length());
            // TESTING if mapping probability is better estimate of read mapping
            double fprob = 1.0d - Math.pow(10d, Double.parseDouble(first[6]) / -10d);
            int fflags = Integer.parseInt(first[3]);
            if((fflags & 0x10) == 0x10)
                forient = "R";
            else
                forient = "F";
            
            for(String[] sec : holder.get(second)){
                if(sec[5].startsWith("chr"))
                    System.out.println(StrUtils.StrArray.Join(sec, "\t"));
                String sorient, schr = sec[4], sstart = sec[5], 
                        send = String.valueOf(Integer.parseInt(sec[5]) + sec[11].length()),
                        smdz = this.getMDZTag(sec, sec[11]);
                int concordant = 0, sedit = Integer.parseInt(this.getNMITag(sec));
                //double sprob = stats.probBasedPhred.calculateScore(smdz, sec[12], sec[12].length());
                // TESTING if mapping probability is better estimate of read mapping
                double sprob = 1.0d - Math.pow(10d, Double.parseDouble(sec[6]) / -10d);
                int sflags = Integer.parseInt(sec[3]);
                if((sflags & 0x10) == 0x10)
                    sorient = "R";
                else
                    sorient = "F";
                
                String svcall = this.generateSVCall(fchr, schr, 
                        Integer.parseInt(fstart), Integer.parseInt(sstart), 
                        Integer.parseInt(fend), Integer.parseInt(send), forient, sorient);
                
                if(svcall.equals("concordant"))
                    continue;
                
                double avgphred = stats.calcAvgPhred.calcAvgPhred(first[12], sec[12]);
                
                divet d = new divet(clone, fchr, fstart, fend, forient, schr,
                        sstart, send, sorient, 
                        svcall, String.valueOf((fedit + sedit) / 2d), String.valueOf(avgphred), 
                        String.valueOf((sprob + fprob) / 2d), String.valueOf(concordant));
                
                divets.add(d);
            }
        }
    }
    
    private String generateSVCall(String fchr, String schr, int fstart, int sstart, int fend, int send, String forient, String sorient){
        if(!fchr.equals(schr)){
            // Translocation
            return "transchr";
        }else{
            int span = max4(fstart, fend, sstart, send) - min4(fstart, fend, sstart, send) + 1;
            if(span > this.cutoff){
                // Maxdist
                return "maxdist";
            }else if(span < this.lower){
                if(isEverted(forient, sorient, fstart, sstart))
                    // Eversion
                    return "eversion";
                else if(!forient.equals(sorient)){
                    //Insertion
                    return "insertion";
                }else{
                    //Insinv
                    return "insinv";
                }
            }else if (span > this.upper){
                if (!isEverted(forient, sorient, fstart, sstart)){
                    // Deletion
                    return "deletion";
                }else if (forient.equals(sorient)){
                    // Delinv
                    return "delinv";
                }else{
                    return "eversion";
                }
            }else{
                if (forient.equals(sorient)){
                    // Inversion
                    return "inversion";
                }else if(isEverted(forient, sorient, fstart, sstart)){
                    // Eversion
                    return "eversion";                                
                }else{
                    // Concordant
                    return "concordant";
                }
            }
        }
    }
    
    private boolean isEverted(String forient, String sorient, int fstart, int sstart){
        if(fstart < sstart){
            return forient.equals("R") && sorient.equals("F");
        }else{
            return forient.equals("F") && sorient.equals("R");
        }
    }
    
    private int max4(int a, int b, int c, int d){
	if (a >= b && a >= c && a >= d) {
            return a;
        }
	if (b >= a && b >= c && b >= d) {
            return b;
        }
	if (c >= a && c >= b && c >= d) {
            return c;
        }
	if (d >= a && d >= b && d >= c) {
            return d;
        }
        return 0;
    }

    private int min4(int a, int b, int c, int d){
        if (a <= b && a <= c && a <= d) {
            return a;
        }
        if (b <= a && b <= c && b <= d) {
            return b;
        }
        if (c <= a && c <= b && c <= d) {
            return c;
        }
        if (d <= a && d <= b && d <= c) {
            return d;
        }
        return 0;
    }
    
    private String getMDZTag(String[] array, String read){
        for(String s : array){
            if(s.matches("MD:Z:.*")){
                String[] tokens = s.split(":");
                return tokens[2];
            }
        }
        return String.valueOf(read.length());
    }
    
    private String getNMITag(String[] array){
        for(String s : array){
            if(s.matches("NM:I:.*")){
                String[] tokens = s.split(":");
                return tokens[2];
            }
        }
        return "0";
    }
    
    public ArrayList<divet> getDivets(){
        return this.divets;
    }
}
