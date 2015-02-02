/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import finalSVTypes.Inversions;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import setWeightCover.finalSets;

/**
 *
 * @author bickhart
 */
public class OutputInversion {
    protected ArrayList<Inversions> sets;
    protected Path outfile;
    
    public OutputInversion(String outfile){
        //this.sets = sets;
        this.outfile = Paths.get(outfile);
    }
    
    public void AddSets(ArrayList<Inversions> sets ){
        this.sets = sets;
    }
    
    public void PurgeSets(){
        this.sets = null;
    }
    
    public void WriteOut (){
        if(this.sets == null){
            return;
        }
        try (BufferedWriter output = Files.newBufferedWriter(outfile, Charset.forName("UTF-8")) ){
            for(Inversions event : this.sets){
                if(!event.IsComplete()){
                    continue;
                }
                String outLine = join(event.Chr(), String.valueOf(event.Start()), String.valueOf(event.InnerStart()),
                        String.valueOf(event.InnerEnd()), String.valueOf(event.End()), String.valueOf(event.svType),
                        String.valueOf(event.DiscSupport()), String.valueOf(event.SplitSupport()), String.valueOf(event.UnbalancedSplitSupport()),
                        String.valueOf(event.SumFullSupport()), "Forward:", String.valueOf(event.RetForward().svType), 
                        String.valueOf(event.RetForward().InnerStart()), String.valueOf(event.RetForward().InnerEnd()),
                        "Reverse:", String.valueOf(event.RetReverse().svType), String.valueOf(event.RetReverse().InnerStart()),
                        String.valueOf(event.RetReverse().InnerEnd()));
                output.write(outLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(OutputEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected String join (String ... t){
        StringBuilder holder = new StringBuilder();
        for(int i = 0; i < t.length; i++){
            if(i == 0){
                holder.append(t[i]);
            }else{
                holder.append("\t").append(t[i]);
            }
        }
        holder.append("\n");
        return holder.toString();        
    }
}
