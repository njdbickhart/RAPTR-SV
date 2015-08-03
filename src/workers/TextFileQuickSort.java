/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import StrUtils.StrArray;
import TempFiles.TempDataClass;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that can be used to sort a large file by splitting said file into several temporary sorted files and 
 * merging those files.
 * Changed to allow line segment sorting
 * @author Greg Cope
 *
 */
public class TextFileQuickSort {
        private final Comparator<String[]> sorter;
	private int maxChunkSize = 100000000;
	private List<File> outputs = new ArrayList<>();
	private String tempDirectory = System.getProperty("java.io.tmpdir");
        private final String delimiter;
        private final int[] colOrder;
        private boolean hasData = false;
        
        private static final Logger log = Logger.getLogger(TextFileQuickSort.class.getName());
        private Path tempFile;

        public TextFileQuickSort(String delimiter, int[] colOrder, String tmpoutbase){
		this.sorter = new ComparatorDelegate(colOrder);
                this.delimiter = delimiter;
                this.colOrder = colOrder;
                this.createTemp(Paths.get(tmpoutbase));
	}
        
	public TextFileQuickSort(Comparator<String[]> sorter, String delimiter, int[] colOrder, String tmpoutbase){
		this.sorter = sorter;
                this.delimiter = delimiter;
                this.colOrder = colOrder;
                this.createTemp(Paths.get(tmpoutbase));
	}

	/**
	 * Sets the temporary directory
	 * @param temp
	 */
	public void setTempDirectory(String temp){
		tempDirectory = temp;
		File file = new File(tempDirectory);
		if ( !file.exists() || !file.isDirectory() ){
			throw new IllegalArgumentException("Parameter director is not a directory or does not exist");
		}
	}

	/**
	 * Sets the chunk size for temporary files
	 * @param size
	 */
	public void setMaximumChunkSize(int size){
		this.maxChunkSize = size;
	}	

	/**
	 * Reads the input io stream and splits it into sorted chunks which are written to temporary files. 
	 * @param in
         * @param identifier
	 * @throws IOException
	 */
	public void splitChunks(InputStream in, String identifier){
		outputs.clear();
		BufferedReader br = null;
		List<String[]> lines = new ArrayList<>(maxChunkSize);
                log.log(Level.INFO, "[TXTFILESORT] Beginning sort routine for bin: " + identifier);
		try{
			br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			int currChunkSize = 0;
			while ((line = br.readLine() ) != null ){
                                this.hasData = true;
				lines.add(line.split("\t"));
				currChunkSize += line.length() + 1;
				if ( currChunkSize >= maxChunkSize ){
					currChunkSize = 0;
					Collections.sort(lines, sorter);
                                        double rand = Math.random();
                                        String tmpfile = tempDirectory + "tempsplit" + System.currentTimeMillis() + "." + rand;
					File file = new File(tmpfile);
                                        log.log(Level.FINE, "[TXTFILESORT] Created new chunk temp file: " + tmpfile + " for bin: " + identifier);
					outputs.add(file);
					writeOut(lines, new FileOutputStream(file));
					lines.clear();
				}
			}
			//write out the remaining chunk
			Collections.sort(lines, sorter);
                        double rand = Math.random();
                        String tmpfile = tempDirectory + "tempsplit" + System.currentTimeMillis() + "." + rand;
			File file = new File(tmpfile);
                        log.log(Level.FINE, "[TXTFILESORT] Created new chunk temp file: " + tmpfile + " for bin: " + identifier);
			outputs.add(file);
			writeOut(lines, new FileOutputStream(file));
                        log.log(Level.FINE, "[TXTFILESORT] Finished split chunk routine. Had files? " + this.hasData);
			lines.clear();
		}catch(IOException io){
			log.log(Level.SEVERE, "[TXTFILESORT] Error reading from inputstream: " + in.toString(), io);
		}finally{
			if ( br != null )try{br.close();}catch(Exception e){
                            log.log(Level.SEVERE, "[TXTFILESORT] Error closing inputstream: " + in.toString(), e);
                        }
		}
	}

	/**
	 * Writes the list of lines out to the output stream, append new lines after each line.
	 * @param list
	 * @param os
	 * @throws IOException
	 */
	private void writeOut(List<String[]> list, OutputStream os) throws IOException{
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new OutputStreamWriter(os));
			for ( String[] s : list ){
				writer.write(StrArray.Join(s, delimiter));
				writer.write(System.lineSeparator());
			}
			writer.flush();
		}catch(IOException io){
			log.log(Level.SEVERE, "[TXTFILESORT] Error writing to outputstream: " + os.toString(), io);
		}finally{
			if ( writer != null ){
				try{writer.close();}catch(Exception e){
                                    log.log(Level.SEVERE, "[TXTFILESORT] Error closing output stream!", e);
                                }
			}
		}
	}

	/**
	 * Reads the temporary files created by splitChunks method and merges them in a sorted manner into the output stream.
	 * @param os
	 * @throws IOException
	 */
	public void mergeChunks() throws IOException{
            OutputStream os = new FileOutputStream(this.tempFile.toFile());
		Map<StringWrapper, BufferedReader> map = new HashMap<>();
		List<BufferedReader> readers = new ArrayList<>();
		BufferedWriter writer = null;
		//ComparatorDelegate delegate = new ComparatorDelegate();
		try{
			writer = new BufferedWriter(new OutputStreamWriter(os));
			for ( int i = 0; i < outputs.size(); i++ ){
				BufferedReader reader = new BufferedReader(new FileReader(outputs.get(i)));
                                
				String line = reader.readLine();
				if ( line != null ){
                                        readers.add(reader);
					map.put(new StringWrapper(line.split(delimiter), colOrder), readers.get(readers.size() - 1));
				}
			}

			///continue to loop until no more lines lefts
			List<StringWrapper> sorted = new LinkedList<>(map.keySet());
			while ( map.size() > 0 ){
				Collections.sort(sorted);
				StringWrapper line = sorted.remove(0);
                                
                                String outstr = StrArray.Join(line.string, delimiter);
				writer.write(outstr);
				writer.write(System.lineSeparator());
				BufferedReader reader = map.remove(line);
				String nextLine = reader.readLine();
				if ( nextLine != null ){
					StringWrapper sw = new StringWrapper(nextLine.split(delimiter), colOrder);
					map.put(sw,  reader);
					sorted.add(sw);
				}
			}
			writer.flush();
		}catch(IOException io){
			log.log(Level.SEVERE, "[TXTFILESORT] Error merging " + readers.size() + " files to: " + os.toString(), io);
		}finally{
			for ( int i = 0; i < readers.size(); i++ ){
				try{readers.get(i).close();}catch(Exception e){
                                    log.log(Level.SEVERE, "[TXTFILESORT] Could not close buffered reader merger: " + readers.get(i).toString());
                                }
			}
			for ( int i = 0; i < outputs.size(); i++ ){
				outputs.get(i).delete();
			}
                        
			try{writer.close();}catch(Exception e){
                            log.log(Level.SEVERE, "[TXTFILESORT] Erorr closing merge output writer!", e);
                        }
		}
                os.close();
	}
        
        /*
        * return the temporary merged file
        */
        public Path getTemp(){
            return this.tempFile;
        }
        
        private void createTemp(Path path){
        try {
                this.tempFile = Files.createTempFile(path.toString(), "sort.tmp");
                this.tempFile.toFile().deleteOnExit();
                
                log.log(Level.FINE, "[TXTFILESORT] Private temp file: " + this.tempFile.toString());
            } catch (IOException ex) {
                Logger.getLogger(TempDataClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

	/**
	 * Delegate comparator to be able to sort String arrays. Delegates its behavior to 
	 * the sorter field.
	 * @author Greg Cope
	 *
	 */
	public class ComparatorDelegate implements Comparator<String[]>{
            private final int[] cols;
            public ComparatorDelegate(int[] cols){
                this.cols = cols;
            }

            @Override
            public int compare(String[] t, String[] t1) {
                for(int i : cols){
                    if(t[i].equals(t1[i]));
                    else
                        return t[i].compareTo(t1[i]);
                }
                return t[0].compareTo(t1[0]);
            }
            
	}

	/**
	 * Class which is a wrapper class for a String. This is necessary for String duplicates, which may cause equals/hashCode
	 * conflicts within the HashMap used in the file merge.
	 * @author Greg Cope
	 *
	 */
	private class StringWrapper implements Comparable<StringWrapper>{		

            public final String[] string;
            private final int[] cols;

            public StringWrapper(String[] line, int[] cols){
                    this.string = line;
                    this.cols = cols;
            }		

            @Override
            public int compareTo(StringWrapper t) {
                for(int i : cols){
                    if(this.string[i].equals(t.string[i]));
                    else
                        return this.string[i].compareTo(t.string[i]);
                }
                return this.string[0].compareTo(t.string[0]);
            }
	}

}
