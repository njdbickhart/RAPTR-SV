/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

/**
 *
 * @author bickhart
 */
public class CommandLineParser {
    public String flatFile = null;
    public String chr = null;
    public String outBase = null;
    public String gapFile = null;
    public boolean useGMS = false;
    public String GMSFile = null;
    public String usage = "SetWeightCoverVHSRDiscovery.jar\tA tool to cluster split and paired end reads\n"
            + "\t-s\tFlatfile containing records from the same reads\n"
            + "\t-c\tChromosome to be processed\n"
            + "\t-g\tAssembly Gap bed file\n"
            + "\t-o\tOutput file prefix and directory\n"
            + "\t-m\tGMS file for weight rebalancing[optional]\n";
    public boolean isComplete = false;
    
    public CommandLineParser(String[] args){
        for(int i = 0; i < args.length; i++){
            switch(args[i]){
                case "-s": this.flatFile = args[i+1]; break;
                case "-c": this.chr = args[i+1]; break;
                case "-o": this.outBase = args[i+1]; break;
                case "-g": this.gapFile = args[i+1]; break;
                case "-m": this.GMSFile = args[i+1]; useGMS = true; break;
            }
        }
        if(this.flatFile == null || this.chr == null || 
                this.outBase == null || this.gapFile == null ){
            this.isComplete = false;
        }else{
            this.isComplete = true;
        }
    }
}
