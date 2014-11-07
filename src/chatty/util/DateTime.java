
package chatty.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Stuff to do with dates.
 * 
 * @author tduva
 */
public class DateTime {
    
    private static final SimpleDateFormat FULL_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm");
    public static final int MINUTE = 60;
    public static final int HOUR = MINUTE * 60;
    public static final int DAY = HOUR * 24;
    
    public static int currentHour12Hour() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR);
    }
    
    public static String currentTime(SimpleDateFormat sdf) {
        Calendar cal = Calendar.getInstance();
    	
        return sdf.format(cal.getTime());
    }
    
    public static String fullDateTime() {
        return currentTime(FULL_DATETIME);
    }
    
    public static String currentTime() {
        return currentTime(SDF);
    }
    
    public static String currentTime(String format) {
        return currentTime(new SimpleDateFormat(format));
    }
    
    public static String format(long time, SimpleDateFormat sdf) {
        return sdf.format(new Date(time));
    }
    
    public static String format(long time) {
        return SDF.format(new Date(time));
    }
    
    public static String formatFullDatetime(long time) {
        return FULL_DATETIME.format(new Date(time));
    }
    
    public static String format2(long time) {
        return SDF2.format(new Date(time));
    }
    
    public static String ago(long time) {
        long timePassed = System.currentTimeMillis() - time;
        return ago2(timePassed);
        
    }
    
    public static String ago2(long timePassed) {
        long seconds = timePassed / 1000;
        if (seconds < MINUTE*10) {
            return "только что";
        }
        if (seconds < HOUR) {
            return "недавно";
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+" "+(hours == 1 ? "час" : "часов")+" назад";
        }
        int days = (int)seconds / DAY;
        return days+" "+(days == 1 ? "день" : "дней")+" назад";
    }
    
    public static String ago4(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;
        if (seconds < MINUTE) {
            return seconds+" "+(seconds == 1 ? "секунд" : "секунд");
        }
        if (seconds < HOUR) {
            int minutes = (int)seconds / MINUTE;
            return minutes+" "+(minutes == 1 ? "минут" : "минут");
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+" "+(hours == 1 ? "час" : "часов");
        }
        int days = (int)seconds / DAY;
        return days+" "+(days == 1 ? "день" : "дней");
    }
    
    public static String ago4compact(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;
        if (seconds < MINUTE) {
            return seconds+"с";
        }
        if (seconds < HOUR) {
            int minutes = (int)seconds / MINUTE;
            return minutes+"м";
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+"ч";
        }
        int days = (int)seconds / DAY;
        return days+"д";
    }
    
    public static String ago3(long time, boolean showSeconds) {
        long timePassed = System.currentTimeMillis() - time;
        long seconds = timePassed / 1000;
        
        return duration2(seconds, showSeconds);
    }
    
    public static String duration2(long seconds, boolean showSeconds) {
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        seconds = seconds % MINUTE;
        
        if (showSeconds)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", hours, minutes, seconds);
    }
    
    public static String ago5(long time) {
        long timePassed = System.currentTimeMillis() - time;
        long seconds = timePassed / 1000;
        
        return duration3(seconds);
    }
    
    public static String duration3(long seconds) {
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        
        if (hours == 0)
            return String.format("%dm", minutes);
        return String.format("%dh %dm", hours, minutes);
    }
    
    public static String duration(long time, boolean detailed) {
        return duration(time, detailed, true);
    }
    
    public static String duration(long time, boolean detailed, boolean milliseconds) {
        long seconds = time;
        if (milliseconds) {
            seconds = time / 1000;
        }
        if (seconds < MINUTE) {
            return seconds+"с";
        }
        if (seconds < HOUR) {
            int s = (int)seconds % MINUTE;
            if (detailed && s > 0) {
                return seconds / MINUTE+"м "+s+"с";
            }
            return seconds / MINUTE+"м";
        }
        if (seconds < DAY) {
            return seconds / HOUR+"ч";
        }
        return seconds / DAY+"д";
    }
}
