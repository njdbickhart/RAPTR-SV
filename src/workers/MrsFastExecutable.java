/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bickhart
 */
public class MrsFastExecutable implements Callable<String>{
    private final String reference;
    private final String fastq;
    private final String sam;
    private final String rg;
    private final boolean isUltra;
    
    private static final Logger log = Logger.getLogger(MrsFastExecutable.class.getName());
    
    public MrsFastExecutable(String ref, String fq, String mrsfastsam, String rg){
        reference = ref;
        fastq = fq;
        sam = mrsfastsam;
        this.rg = rg;
        isUltra = this.CheckVersion();
    }
    
    @Override
    public String call() throws Exception {
        ProcessBuilder pb = null;
        
        // Treat the versions of MrsFAST differently
        if(isUltra){
            //String threads = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            //It didn't make sense to fork off Mrsfast runtimes with parallel mrsfast submission jobs!
            log.log(Level.FINE, "[MRSFASTEXE] Running ultra parameters on " + rg);
            pb = new ProcessBuilder("mrsfast", "--search", reference, "--threads", String.valueOf(2), "--mem", "4", "--seq", fastq, "-o", sam);
        }else{
            log.log(Level.FINE, "[MRSFASTEXE] Running normal parameters on " + rg);
            pb = new ProcessBuilder("mrsfast", "--search", reference, "--seq", fastq, "-o", sam);
        }
        pb.redirectErrorStream(true);
        //pb.redirectOutput(ProcessBuilder.Redirect.PIPE.file());
        final Process p = pb.start();
        // input stream thread
        new Thread( () -> {
            try{
                //final BufferedWriter out = Files.newBufferedWriter(Paths.get("Mrsfastoutputstream." + fastq + ".out"), Charset.defaultCharset());
                final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                try {
                    while((line = in.readLine()) != null){
                        //out.write(line);
                        //out.newLine();
                        log.log(Level.INFO, rg + "> " + line);
                        
                    }
                } catch (IOException ex) {            
                    log.log(Level.SEVERE, "[MRSFASTEX] Issue with STDIN stream!", ex);
                }finally{
                    if(in != null){
                        in.close();
                    }
                }
                //out.close();
            }catch(Exception ex){
                log.log(Level.SEVERE, "[MRSFASTEXE] Uknown exception with STDIN stream!", ex);
            }
        }).start();
        
        // error stream thread
        new Thread(() -> {
               try(final BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()))){
                //final BufferedWriter out = Files.newBufferedWriter(Paths.get("Mrsfasterrorstream." + fastq + ".err"), Charset.defaultCharset());
            
                String line;
                try {
                    while((line = in.readLine()) != null){
                        //out.write(line);
                        //out.newLine();
                        log.log(Level.INFO, rg + "> " + line);
                    }
                } catch (IOException ex) {            
                    log.log(Level.SEVERE, "[MRSFASTEXE] Error! Did not receive data from MrsFAST runtime process!", ex);
                }
                //out.close();
            }catch(IOException ex){
                log.log(Level.SEVERE, "[MRSFAST EXE] Error with STERR stream!", ex);
            }         
        }).start();
        
        p.waitFor();
        p.destroy();
        
        if(! Files.exists(Paths.get(sam))){
            if(! Files.exists(Paths.get(fastq))){
                throw new Exception("Error! Did not have access to fastq file: " + fastq + " and did not generate sam file: " + sam + " in MrsFAST runtime executable wrapper.");
            }else{
                throw new Exception("Error! Had access to fastq file: " + fastq + " but did not generate sam file: " + sam + " in MrsFAST runtime executable wrapper.");
            }
        }
        
        Files.deleteIfExists(Paths.get(sam + ".nohit"));
        return sam;
    }
    
    
    private boolean CheckVersion(){
        ProcessBuilder pb = new ProcessBuilder("mrsfast", "--version");
        pb = pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            line = in.readLine();
            
            if(p.isAlive())
                p.destroyForcibly();
            p.waitFor();
            if(line == null)
                throw new Exception("Could not find MrsFAST in system path! Please download and install MrsFAST!");
            if(line.matches("^2\\..+")){
                log.log(Level.INFO, "[MRSFAST EXE] Found MrsFAST version: " + line + " in system path");
                return false;
            }else if(line.matches("Version: 3\\..+")){
                String[] token = line.split("\\s");
                log.log(Level.INFO, "[MRSFAST EXE] Found MrsFAST version: " + token[1] + " in system path");
                return true;
            }else{
                throw new Exception("Could not determine version from MrsFAST execution! Do you have MrsFAST installed in your PATH?");
            }
            
            
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[MRSFASTEXE] Error! Could not invoke MrsFAST to check version!", ex);
        } catch(Exception ex){
            log.log(Level.SEVERE, "[MRSFASTEXE] Error! General issue with MrsFAST version checking!", ex);
        }   
        
        return false;
    }
}
