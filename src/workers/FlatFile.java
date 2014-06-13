/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bickhart
 */
public class FlatFile {
    private Path anchor;
    private Path divet;
    private Path splitsam;
    private double insert;
    private double stdev;

    public FlatFile(String flatFileLine) {
        flatFileLine = flatFileLine.trim();
        String[] segs = flatFileLine.split("\t");
        this.splitsam = Paths.get(segs[0]);
        this.divet = Paths.get(segs[1]);
        this.anchor = Paths.get(segs[2]);
        try{
            this.insert = Double.valueOf(segs[3]);
            this.stdev = Double.valueOf(segs[4]);
        }catch(NumberFormatException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, "Value of Insert or Stdev is not a number", ex);
        }
    }
    /*
     * Getters
     */
    public Path getAnchor(){
        return this.anchor;
    }
    public Path getDivet(){
        return this.divet;
    }
    public Path getSplitsam(){
        return this.splitsam;
    }
    public Double getInsert(){
        return this.insert;
    }
    public Double getStdev(){
        return this.stdev;
    }
}
