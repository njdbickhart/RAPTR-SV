/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import StrUtils.StrArray;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import setWeightCover.weightCoverEvents;
import workers.BufferedSetReader;
import workers.FlatFile;
import workers.OutputEvents;
import workers.OutputInversion;

/**
 *
 * @author bickhart
 */
public class ClusterMode {
    private final String flatFile;
    private String chr;
    private boolean processChr = false;
    private final String gapFile;
    private final String outBase;
    private final boolean debug;
    private int buffer = 10;
    private int threshold = 2;
    private double phredFilter = 1d;
    private int threads = 1;
    private double rpPhredFilter = 0.0001d;
    
    private static final Logger log = Logger.getLogger(ClusterMode.class.getName());
    
    public ClusterMode(SimpleModeCmdLineParser values){
        flatFile = values.GetValue("flatfile");
        if(values.HasOpt("chromosome")){
            chr = values.GetValue("chromosome");
            processChr = true;
            log.log(Level.FINE, "[CLUSTER] use of single chromosome option: -c " + chr);
        }
        if(values.HasOpt("gapfile")){
            gapFile = values.GetValue("gapfile");
            log.log(Level.FINE, "[CLUSTER] use of gap file for filtration: -g " + gapFile);
        }else{
            gapFile = "NULL";
            log.log(Level.FINE, "[CLUSTER] gap file not submitted.");
        }
        outBase = values.GetValue("outbase");
        if(values.HasOpt("buffer"))
            buffer = Integer.parseInt(values.GetValue("buffer"));
        
        if(values.HasOpt("pfilter"))
            this.rpPhredFilter = Double.parseDouble(values.GetValue("pfilter"));
        
        debug = values.GetValue("debug").equals("true");
        if(values.HasOpt("thresh"))
            threshold = Integer.parseInt(values.GetValue("thresh"));
        if(values.HasOpt("threads"))
            threads = Integer.parseInt(values.GetValue("threads"));
        if(values.HasOpt("filter"))
            phredFilter = Double.parseDouble(values.GetValue("filter"));
    }
    
    public void run(){
        ArrayList<FlatFile> files = this.identifyFiles(flatFile);
        log.log(Level.INFO, "[CLUSTER] identified: " + files.size() + " flatfile lines");
        
        // Create output file holders
        OutputEvents insertions = createOutputEvents(outBase + ".raptr.insertions", debug);
        OutputEvents deletions = createOutputEvents(outBase + ".raptr.deletions", debug);
        OutputEvents tanddup = createOutputEvents(outBase + ".raptr.tand", debug);
        //OutputInversion inversions = new OutputInversion(outBase + ".raptr.inversions");
        
        if(this.processChr){
            log.log(Level.FINE, "[CLUSTER] processing chromosome.");
            processChr(files, chr, insertions, deletions, tanddup);
        }else{
            // Determine chromosomes from split read bam file
            Set<String> chrs = this.identifyChrs(files);
            for(String c : chrs){
                log.log(Level.FINE, "[CLUSTER] processing multiple chromosomes. Now chromosome: " + c);
                processChr(files, c, insertions, deletions, tanddup);
            }
        }
    }

    private void processChr(ArrayList<FlatFile> files, String chr, OutputEvents insertions, OutputEvents deletions, OutputEvents tanddup) {
        // Read input files and place into preliminary containers
        System.out.println("[CLUSTER] Working on chromosome: " + chr + " ...");
        BufferedSetReader reader = new BufferedSetReader(files, gapFile, chr, buffer, rpPhredFilter);
        log.log(Level.FINE, "[CLUSTER] exited reader creation for chr: " + chr);
        
        // Run set weight cover to cluster sets
        weightCoverEvents finalEvents = new weightCoverEvents(reader.getMap(), chr, debug, threshold, threads, phredFilter);
        log.log(Level.FINE, "[CLUSTER] exited weightCoverEvents creation for chr: " + chr);
        finalEvents.calculateInitialSetStats();
        log.log(Level.FINE, "[CLUSTER] exited weightCoverEvents.calculateInitialSetStats for chr: " + chr);
        finalEvents.run();
        log.log(Level.FINE, "[CLUSTER] exited weightCoverEvents.run for chr: " + chr);
        
        // Output results
        //OutputEvents insertions = new OutputEvents(finalEvents.RetIns(), outBase + ".rpsr.insertions", debug);
        insertions.AddSets(finalEvents.RetIns());
        insertions.WriteOut();
        
        //OutputEvents deletions = new OutputEvents(finalEvents.RetDel(), outBase + ".rpsr.deletions", debug);
        deletions.AddSets(finalEvents.RetDel());
        deletions.WriteOut();
        
        //OutputEvents tanddup = new OutputEvents(finalEvents.RetTand(), outBase + ".rpsr.tand", debug);
        tanddup.AddSets(finalEvents.RetTand());
        tanddup.WriteOut();
        
        //OutputInversion inversions = new OutputInversion(finalEvents.RetInv(), outBase + ".rpsr.inversions");
        //inversions.AddSets(finalEvents.RetInv());
        //inversions.WriteOut();
        
        log.log(Level.FINE, "[CLUSTER] wrote output to event files for chr: " + chr);
    }
    
    private OutputEvents createOutputEvents(String outbaseExt, boolean debug){
        return new OutputEvents(outbaseExt, debug);
    }
    
    private ArrayList<FlatFile> identifyFiles(String file){
        ArrayList<FlatFile> output = new ArrayList<>();
        try(BufferedReader input = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"))){
            String line;
            while((line = input.readLine()) != null){
                FlatFile flat = new FlatFile(line);
                output.add(flat);
            }
        }catch(IOException ex){
            Logger.getLogger(BufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output;
    }
    
    private Set<String> identifyChrs(ArrayList<FlatFile> files){
        Set<String> set =  files.stream()
                .map(s -> this.getChrsFromBam(s.getSplitsam().toFile()))
                .flatMap(List::stream)
                .collect(Collectors.toCollection(HashSet::new));
        log.log(Level.INFO, "[CLUSTER] identified: " + set.size() + " chromosomes from split bam header.");
        return set;
    }
    
    private List<String> getChrsFromBam(File sam){
        SamReader samr = SamReaderFactory.makeDefault().open(sam);
        log.log(Level.FINE, "[CLUSTER] extracting chromosomes from bam header: " + sam.toString());
        List<String> headers = samr.getFileHeader()
                .getSequenceDictionary()
                .getSequences().stream()
                    .map(s -> s.getSequenceName())
                    .collect(Collectors.toList());
        log.log(Level.FINE, "[CLUSTER] using the following chromosomes for cluster automation: " + StrArray.Join((ArrayList<String>) headers, ", "));
        return headers;
    }
    
}
