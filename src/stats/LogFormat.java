/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author desktop
 */
public class LogFormat extends Formatter{

    @Override
    public String format(LogRecord lr) {
        StringBuilder buff = new StringBuilder();
        buff.append(this.calcDate(lr.getMillis())).append(" -- ");
        buff.append(lr.getLevel()).append(" -- ");
        buff.append(lr.getSourceClassName()).append(" -> ").append(lr.getSourceMethodName()).append(" -- ");
        buff.append(formatMessage(lr)).append(System.lineSeparator());
        return buff.toString();
    }
    
    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }
}
