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
import java.util.concurrent.Callable;

/**
 *
 * @author bickhart
 */
public class MrsFastExecutable implements Callable<String>{
    private final String reference;
    private final String fastq;
    private final String sam;
    
    public MrsFastExecutable(String ref, String fq, String mrsfastsam){
        reference = ref;
        fastq = fq;
        sam = mrsfastsam;
    }
    
    @Override
    public String call() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("mrsfast", "--search", reference, "--seq", fastq, "-o", sam);
        pb.redirectErrorStream(true);
        //pb.redirectOutput(ProcessBuilder.Redirect.PIPE.file());
        Process p = pb.start();
        try(BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))){
            BufferedWriter out = Files.newBufferedWriter(Paths.get("Mrsfastoutputstream.out"), Charset.defaultCharset());
            String line;
            while((line = in.readLine()) != null){
                out.write(line);
            }
            out.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        p.waitFor();
        Files.deleteIfExists(Paths.get(sam + ".nohit"));
        return sam;
    }
    
}
