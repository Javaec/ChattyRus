
package chatty.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class LogUtil {
    
    private static final Logger LOGGER = Logger.getLogger(LogUtil.class.getName());
    
    public static void logMemoryUsage() {
        LOGGER.info(getMemoryUsage());
    }
    
    public static String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return String.format("[Memory] total: %2$,d used: %4$,d free: %3$,d max: %1$,d",
                runtime.maxMemory() / 1024,
                runtime.totalMemory() / 1024,
                runtime.freeMemory() / 1024,
                (runtime.totalMemory() - runtime.freeMemory()) / 1024);
    }
    
    /**
     * Log JVM memory information every 15 minutes.
     */
    public static void startMemoryUsageLogging() {
        Timer t = new Timer(true);
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                logMemoryUsage();
            }
        }, 10*1000, 900*1000);
    }
    
}
