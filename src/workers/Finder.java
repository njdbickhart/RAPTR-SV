/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author derek.bickhart
 */
public class Finder implements FileFilter{
    private String _pattern;
    ArrayList<String> files = new ArrayList<>();
    
    public Finder (String pattern){
        _pattern = pattern.replace("*", ".*").replace("?", ".");
    }
    @Override
    public boolean accept(File file){
        return Pattern.compile(_pattern).matcher(file.getName()).find();
    }
}
