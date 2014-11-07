
package chatty.util.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class Util {
    
    private static final SimpleDateFormat PARSE_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    /**
     * Parses the time returned from the Twitch API.
     * 
     * @param time The time string
     * @return The timestamp
     * @throws java.text.ParseException if the time could not be parsed
     */
    public static long parseTime(String time) throws java.text.ParseException {
        Date parsed = PARSE_DATE.parse(time);
        return parsed.getTime();
    }
    
    public static final void main(String[] args) {
        try {
            Date test = PARSE_DATE.parse("2014-08-25T19:22:57Z");
            long time = test.getTime();
            System.out.println(System.currentTimeMillis() - time);
        } catch (java.text.ParseException ex) {
            Logger.getLogger(FollowerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
