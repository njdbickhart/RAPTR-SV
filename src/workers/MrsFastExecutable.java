/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
            String threads = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            pb = new ProcessBuilder("mrsfast", "--search", reference, "--threads", threads, "--mem", "4", "--seq", fastq, "-o", sam);
        }else{
            pb = new ProcessBuilder("mrsfast", "--search", reference, "--seq", fastq, "-o", sam);
        }
        //pb.redirectErrorStream(true);
        //pb.redirectOutput(ProcessBuilder.Redirect.PIPE.file());
        final Process p = pb.start();
        // input stream thread
        new Thread( () -> {
            try(final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))){
                //final BufferedWriter out = Files.newBufferedWriter(Paths.get("Mrsfastoutputstream." + fastq + ".out"), Charset.defaultCharset());
            
                String line;
                try {
                    while((line = in.readLine()) != null){
                        //out.write(line);
                        //out.newLine();
                        System.out.println(rg + "> " + line);
                    }
                } catch (IOException ex) {            
                    Logger.getLogger(MrsFastExecutable.class.getName()).log(Level.SEVERE, null, ex);
                }
                //out.close();
            }catch(IOException ex){
                ex.printStackTrace();
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
                        System.err.println(rg + "> " + line);
                    }
                } catch (IOException ex) {            
                    Logger.getLogger(MrsFastExecutable.class.getName()).log(Level.SEVERE, null, ex);
                }
                //out.close();
            }catch(IOException ex){
                ex.printStackTrace();
            }         
        }).start();
        
        p.waitFor();
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
            if(line == null)
                throw new Exception("Could not find MrsFAST in system path! Please download and install MrsFAST!");
            if(line.matches("^2\\..+")){
                System.out.println("[MRSFAST EXE] Found MrsFAST version: " + line + " in system path");
                return false;
            }else if(line.matches("Version: 3\\..+")){
                String[] token = line.split("\\s");
                System.out.println("[MRSFAST EXE] Found MrsFAST version: " + token[1] + " in system path");
                return true;
            }
            if(p.isAlive())
                p.destroyForcibly();
            p.waitFor();
        } catch (IOException ex) {
            Logger.getLogger(MrsFastExecutable.class.getName()).log(Level.SEVERE, null, ex);
        } catch(Exception ex){
            Logger.getLogger(MrsFastExecutable.class.getName()).log(Level.SEVERE, null, ex);
        }     
        return false;
    }
}
