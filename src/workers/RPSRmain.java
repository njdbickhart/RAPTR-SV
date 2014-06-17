/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import setWeightCover.setCoverEvents;
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
}
