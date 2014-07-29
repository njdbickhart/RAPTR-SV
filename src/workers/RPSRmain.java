/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import GetCmdOpt.SimpleModeCmdLineParser;
import modes.ClusterMode;
import modes.PreprocessMode;
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
        // Prepare and read command line options
        SimpleModeCmdLineParser cmd = PrepareCMDOptions();        
        cmd.GetAndCheckMode(args);
        
        switch(cmd.CurrentMode){
            case "cluster":
                ClusterMode cluster = new ClusterMode(cmd);
                cluster.run();
                break;
            case "preprocess":
                PreprocessMode preprocess = new PreprocessMode(cmd);
                preprocess.run();
                break;
            default:
                System.err.println("Error! Must designate a usage mode!");
                System.err.println(cmd.GetUsage());
                System.exit(-1);
        }
        
        System.exit(0);
    }
    
    private static SimpleModeCmdLineParser PrepareCMDOptions(){
        String nl = System.lineSeparator();
        SimpleModeCmdLineParser cmd = new SimpleModeCmdLineParser("RPSR.jar\tA tool to cluster split and paired end reads" + nl
            + "Usage: java -jar RPSR.jar [mode] [mode specific options]" + nl
                + "Modes:" + nl
                + "\tpreprocess\tInterprets BAM files to generate metadata for \"cluster\" mode" + nl 
                + "\tcluster\t\tThe mode that processes metadata generated from the main program" + nl,
                "cluster",
                "preprocess"
        );
        
        cmd.AddMode("cluster", 
                "RPSR cluster mode" + nl +
                "Usage: java -jar RPSR.jar [-s filelist -c chromosome -g gap file -o output prefix] (optional: -g gmsfile list, -b buffer size)" + nl
                + "\t-s\tFlatfile containing records from the same reads" + nl
                + "\t-c\tChromosome to be processed" + nl
                + "\t-g\tAssembly Gap bed file" + nl
                + "\t-o\tOutput file prefix and directory" + nl
                + "\t-m\tGMS file for weight rebalancing [optional]" + nl
                + "\t-f\tFloating point value for threshold of detection [optional; default is one]" + nl
                + "\t-b\tNumber of pairs to hold per set; reducing this will reduce memory overhead [optional; default is 10]" + nl
                + "\t-i\tThe threshold of raw supporting reads needed for a set to be considered for candidate SV calling [optional; default is 2]" + nl
                + "\t-t\tThe number of threads to devote to SetWeightCover screening [optional; default is 1]" + nl,
                "s:c:g:o:m:f:b:d|i:t:", 
                "scgo", 
                "scgomfbdit", 
                "flatfile", "chromosome", "gapfile", "outbase", "gms", "filter", "buffer", "debug", "thresh", "threads");
        
        cmd.AddMode("preprocess", 
                "RPSR preprocess mode" + nl +
                "Usage: java -jar RPSR.jar [-i input bam -o output base name -r ref genome] (optional argugments)" + nl
                + "\t-i\tInput BWA-aligned BAM file" + nl
                + "\t-o\tThe base output name and directory (all output files will start with this name)" + nl
                + "\t-r\tA MrsFAST indexed reference genome fasta file for realignment" + nl
                + "\t-g\tFlag: states if the BAM has read groups [optional; default is 'false']" + nl
                + "\t-t\tNumber of threads to use for preprocessing [optional; default is one thread]" + nl
                + "\t-m\tMaximum distance between readpairs on the same chromosome [optional; default is 100000]" + nl
                + "\t-s\tMetadata sampling limit. Reducing this will reduce memory overhead. [optional; default is 1000]" + nl, 
                "i:o:r:g|t:m:s:d|", 
                "ior", 
                "iorgtmsd", 
                "input", "output", "reference", "checkRG", "threads", "maxdist", "samplimit", "debug");
        
        return cmd;
    }
}
