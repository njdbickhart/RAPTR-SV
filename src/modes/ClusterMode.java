/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import java.util.HashMap;
import setWeightCover.weightCoverEvents;
import workers.BufferedSetReader;
import workers.OutputEvents;
import workers.OutputInversion;

/**
 *
 * @author bickhart
 */
public class ClusterMode {
    private final String flatFile;
    private final String chr;
    private final String gapFile;
    private final String outBase;
    private int buffer = 10;
    
    public ClusterMode(SimpleModeCmdLineParser values){
        flatFile = values.GetValue("flatFile");
        chr = values.GetValue("chr");
        gapFile = values.GetValue("gapFile");
        outBase = values.GetValue("outBase");
        if(values.HasOpt("buffer"))
            buffer = Integer.parseInt(values.GetValue("buffer"));
    }
    
    public void run(){
        // Read input files and place into preliminary containers
        //readInputFiles fileParser = new readInputFiles(cmd.flatFile, cmd.gapFile, cmd.chr);
        BufferedSetReader reader = new BufferedSetReader(flatFile, gapFile, chr, buffer);
        
        // Run set weight cover to cluster sets
        weightCoverEvents finalEvents = new weightCoverEvents(reader.getMap(), chr);
        finalEvents.calculateInitialSetStats();
        finalEvents.run();
        
        // Output results
        OutputEvents insertions = new OutputEvents(finalEvents.RetIns(), outBase + ".vhsr.insertions");
        insertions.WriteOut();
        
        OutputEvents deletions = new OutputEvents(finalEvents.RetDel(), outBase + ".vhsr.deletions");
        deletions.WriteOut();
        
        OutputEvents tanddup = new OutputEvents(finalEvents.RetTand(), outBase + ".vhsr.tand");
        tanddup.WriteOut();
        
        OutputInversion inversions = new OutputInversion(finalEvents.RetInv(), outBase + ".vhsr.inversions");
        inversions.WriteOut();
        System.exit(0);
    }
}
