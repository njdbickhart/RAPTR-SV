/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import GetCmdOpt.SimpleModeCmdLineParser;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import modes.ClusterMode;
import modes.PreprocessMode;
import stats.LogFormat;

/**
 *
 * @author bickhart
 */
public class RPSRmain {
    private static final String version = "0.0.8";
    private static final Logger log = Logger.getLogger(RPSRmain.class.getName());
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        
        // Prepare and read command line options
        SimpleModeCmdLineParser cmd = PrepareCMDOptions();        
        cmd.GetAndCheckMode(args);
        
        if(cmd.HasOpt("p")){
            System.setProperty("java.io.tmpdir", cmd.GetValue("p"));
            System.out.println("[MAIN] Setting temporary file directory to: " + cmd.GetValue("p"));
        }
        
        if(cmd.HasOpt("t")){
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", cmd.GetValue("t"));
            System.out.println("[MAIN] Setting ForkJoin thread ceiling to: " + cmd.GetValue("t"));
        }
        
        boolean debug = cmd.GetValue("debug").equals("true");
        
        switch(cmd.CurrentMode){
            case "cluster":
                setFileHandler("cluster", args, debug);
                ClusterMode cluster = new ClusterMode(cmd);
                cluster.run();
                break;
            case "preprocess":
                setFileHandler("preprocess", args, debug);
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

    private static void setFileHandler(String type, String[] args, boolean debug) {
        // Create a log file and set levels for use with debugger
        FileHandler handler = null;
        ConsoleHandler console = null;
        try {
            handler = new FileHandler("RAPTR-SV." + type + ".%u.%g.log");
            handler.setFormatter(new LogFormat());
            console = new ConsoleHandler();
            
            if(debug){
                handler.setLevel(Level.ALL);
                console.setLevel(Level.INFO);
            }else{
                handler.setLevel(Level.INFO);
                console.setLevel(Level.WARNING);
            }
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(RPSRmain.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger mainLog = Logger.getLogger("");
        // This will display all messages, but the handlers should filter the rest
        mainLog.setLevel(Level.ALL);
        for(Handler h : mainLog.getHandlers()){
            mainLog.removeHandler(h);
        }
        mainLog.addHandler(handler);
        mainLog.addHandler(console);
        
        // Log input arguments
        log.log(Level.INFO, "[MAIN] Command line arguments supplied: ");
        log.log(Level.INFO, StrUtils.StrArray.Join(args, " "));
        log.log(Level.INFO, "[MAIN] Debug flag set to: " + debug);
    }
    
    private static SimpleModeCmdLineParser PrepareCMDOptions(){
        String nl = System.lineSeparator();
        SimpleModeCmdLineParser cmd = new SimpleModeCmdLineParser("RAPTR-SV\tA tool to cluster split and paired end reads" + nl
                + "Version: " + version + nl
            + "Usage: java -jar RAPTR-SV.jar [mode] [mode specific options]" + nl
                + "Modes:" + nl
                + "\tpreprocess\tInterprets BAM files to generate metadata for \"cluster\" mode" + nl 
                + "\tcluster\t\tThe mode that processes metadata generated from the main program" + nl,
                "cluster",
                "preprocess"
        );
        
        cmd.AddMode("cluster", 
                "RAPTR-SV cluster mode" + nl +
                "Usage: java -jar RAPTR-SV.jar cluster [-s filelist -c chromosome -g gap file -o output prefix] (optional: -g gmsfile list, -b buffer size)" + nl
                + "\t-s\tFlatfile containing records from the same reads" + nl
                + "\t-c\tChromosome to be processed [optional; will process all chromosomes]" + nl
                + "\t-g\tAssembly Gap bed file [optional; will not perform gap filtration]" + nl
                + "\t-o\tOutput file prefix and directory" + nl
                + "\t-m\tPair ProbBased Phred filter. All discordant read pairs below this are removed [optional floating point; default is 0.0001]" + nl
                + "\t-f\tFloating point value for set threshold of detection [optional; default is one]" + nl
                + "\t-b\tNumber of pairs to hold per set; reducing this will reduce memory overhead [optional; default is 10]" + nl
                + "\t-i\tThe threshold of raw supporting reads needed for a set to be considered for candidate SV calling [optional; default is 2]" + nl
                + "\t-t\tThe number of threads to devote to SetWeightCover screening [optional; default is 1]" + nl
                + "\t-p\tAn alternate temp directory to write temporary data [optional; use if your /tmp partition is too small]" + nl,
                "s:c:g:o:m:f:b:d|i:t:p:", 
                "so", 
                "scgomfbditp", 
                "flatfile", "chromosome", "gapfile", "outbase", "pfilter", "filter", "buffer", "debug", "thresh", "threads", "temp");
        
        cmd.AddMode("preprocess", 
                "RAPTR-SV preprocess mode" + nl +
                "Usage: java -jar RAPTR-SV.jar preprocess [-i input bam -o output base name -r ref genome] (optional argugments)" + nl
                + "\t-i\tInput BWA-aligned BAM file" + nl
                + "\t-o\tThe base output name and directory (all output files will start with this name)" + nl
                + "\t-r\tA MrsFAST indexed reference genome fasta file for realignment" + nl
                + "\t-g\tFlag: states if the BAM has read groups [optional; default is 'false']" + nl
                + "\t-t\tNumber of threads to use for preprocessing [optional; default is one thread]" + nl
                + "\t-m\tMaximum distance between readpairs on the same chromosome [optional; default is 1000000]" + nl
                + "\t-s\tMetadata sampling limit. Reducing this will reduce memory overhead. [optional; default is 10000]" + nl
                + "\t-p\tAn alternate temp directory to write temporary data [optional; use if your /tmp partition is too small]" + nl,
                "i:o:r:g|t:m:s:d|p:", 
                "ior", 
                "iorgtmsdp", 
                "input", "output", "reference", "checkRG", "threads", "maxdist", "samplimit", "debug", "temp");
        
        return cmd;
    }
}
