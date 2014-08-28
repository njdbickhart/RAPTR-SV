/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
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
    private final boolean debug;
    private int buffer = 10;
    private int threshold = 2;
    private double phredFilter = 1d;
    private int threads = 1;
    
    public ClusterMode(SimpleModeCmdLineParser values){
        flatFile = values.GetValue("flatfile");
        chr = values.GetValue("chromosome");
        gapFile = values.GetValue("gapfile");
        outBase = values.GetValue("outbase");
        if(values.HasOpt("buffer"))
            buffer = Integer.parseInt(values.GetValue("buffer"));

        debug = values.GetValue("debug").equals("true");
        if(values.HasOpt("thresh"))
            threshold = Integer.parseInt(values.GetValue("thresh")) + 1;
        if(values.HasOpt("threads"))
            threads = Integer.parseInt(values.GetValue("threads"));
        if(values.HasOpt("filter"))
            phredFilter = Double.parseDouble(values.GetValue("filter"));
    }
    
    public void run(){
        // Read input files and place into preliminary containers
        //readInputFiles fileParser = new readInputFiles(cmd.flatFile, cmd.gapFile, cmd.chr);
        BufferedSetReader reader = new BufferedSetReader(flatFile, gapFile, chr, buffer);
        
        // Run set weight cover to cluster sets
        
        weightCoverEvents finalEvents = new weightCoverEvents(reader.getMap(), chr, debug, threshold, threads, phredFilter);
        finalEvents.calculateInitialSetStats();
        
        finalEvents.run();
        
        // Output results
        OutputEvents insertions = new OutputEvents(finalEvents.RetIns(), outBase + ".rpsr.insertions", debug);
        insertions.WriteOut();
        
        OutputEvents deletions = new OutputEvents(finalEvents.RetDel(), outBase + ".rpsr.deletions", debug);
        deletions.WriteOut();
        
        OutputEvents tanddup = new OutputEvents(finalEvents.RetTand(), outBase + ".rpsr.tand", debug);
        tanddup.WriteOut();
        
        OutputInversion inversions = new OutputInversion(finalEvents.RetInv(), outBase + ".rpsr.inversions");
        inversions.WriteOut();
        System.exit(0);
    }
}
