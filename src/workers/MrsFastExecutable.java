/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

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
        ProcessBuilder pb = new ProcessBuilder("mrsfast", "--search", reference, "-seq", fastq, "-o", sam);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE.file());
        Process p = pb.start();
        p.waitFor();
        return sam;
    }
    
}
