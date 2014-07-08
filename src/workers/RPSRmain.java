/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import GetCmdOpt.SimpleModeCmdLineParser;
import setWeightCover.weightCoverEvents;

/**
 *
 * @author bickhart
 */
public class RPSRmain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Read in command line arguments; parity check
        CommandLineParser cmd = new CommandLineParser(args);
        if(!cmd.isComplete){
            System.out.print(cmd.usage);
            System.exit(0);
        }
        
        // Read input files and place into preliminary containers
        //readInputFiles fileParser = new readInputFiles(cmd.flatFile, cmd.gapFile, cmd.chr);
        // TODO: implement a commandline parser argument for the buffer 
        BufferedSetReader reader = new BufferedSetReader(cmd.flatFile, cmd.gapFile, cmd.chr, 10);
        
        // Create initial sets
        //setCoverEvents initialEvents = new setCoverEvents(fileParser.PairSplit(), fileParser.Divet(), fileParser.Gaps(), fileParser.ReadNameMappings(), cmd.chr);
        
        // Run set weight cover to cluster sets
        weightCoverEvents finalEvents = new weightCoverEvents(reader.getMap(), cmd.chr);
        finalEvents.calculateInitialSetStats();
        finalEvents.run();
        
        // Output results
        OutputEvents insertions = new OutputEvents(finalEvents.RetIns(), cmd.outBase + ".vhsr.insertions");
        insertions.WriteOut();
        
        OutputEvents deletions = new OutputEvents(finalEvents.RetDel(), cmd.outBase + ".vhsr.deletions");
        deletions.WriteOut();
        
        OutputEvents tanddup = new OutputEvents(finalEvents.RetTand(), cmd.outBase + ".vhsr.tand");
        tanddup.WriteOut();
        
        OutputInversion inversions = new OutputInversion(finalEvents.RetInv(), cmd.outBase + ".vhsr.inversions");
        inversions.WriteOut();
        System.exit(0);
    }
    
    private static SimpleModeCmdLineParser PrepareCMDOptions(){
        String nl = System.lineSeparator();
        SimpleModeCmdLineParser cmd = new SimpleModeCmdLineParser("RPSR.jar\tA tool to cluster split and paired end reads" + nl
            + "Usage: java -jar RPSR.jar [mode] [mode specific options]" + nl
                + "Modes:" + nl
                + "\tpreprocess\tInterprets BAM files to generate metadata for \"cluster\" mode" + nl 
                + "\tcluster\tThe mode that processes metadata generated from the main program" + nl,
                "cluster",
                "preprocess"
        );
        
        cmd.AddMode("cluster", 
                "RPSR cluster mode" + nl +
                "Usage: java -jar RPSR.jar [-s filelist -c chromosome -g gap file -o output prefix] (optional: -g gmsfile list)" + nl
                + "\t-s\tFlatfile containing records from the same reads" + nl
                + "\t-c\tChromosome to be processed" + nl
                + "\t-g\tAssembly Gap bed file" + nl
                + "\t-o\tOutput file prefix and directory" + nl
                + "\t-m\tGMS file for weight rebalancing[optional]" + nl
                + "\t-f\tFloating point value for threshold of detection [optional; default is one]" + nl,
                "s:c:g:o:m:f:", 
                "scgo", 
                "scgomf", 
                "flatfile", "chromosome", "gapfile", "outbase", "gms", "filter");
        
        return cmd;
    }
}
