
package chatty;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * On a connection attempt a timer can be started that will join the channel
 * again, unless the timer is canceled, which can be done if the channel join
 * actually succeeds.
 * 
 * @author tduva
 */
public class JoinChecker {
    
    private static final Logger LOGGER = Logger.getLogger(JoinChecker.class.getName());
    
    /**
     * How long to wait before trying to join again.
     */
    private static final int DELAY = 7*1000;
    
    private final Irc irc;
    
    /**
     * Map of timers for channels.
     */
    private final HashMap<String, Timer> pendingChecks = new HashMap<>();
    
    public JoinChecker(Irc irc) {
        this.irc = irc;
    }
    
    /**
     * Starts a timer that will JOIN {@code channel} once it runs out.
     * 
     * @param channel The name of the channel to start the timer for
     */
    public synchronized void joinAttempt(final String channel) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                LOGGER.warning("Join may have failed ("+channel+")");
                irc.joinChannel(channel);
            }
        }, DELAY);
        pendingChecks.put(channel, timer);
    }
    
    /**
     * Cancels the timer for {@code channel} if one was running.
     * 
     * @param channel Then name of the channel to cancel the timer for
     */
    public synchronized void joined(String channel) {
        Timer timer = pendingChecks.remove(channel);
        if (timer != null) {
            timer.cancel();
        }
    }
}
