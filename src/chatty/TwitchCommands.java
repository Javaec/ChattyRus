
package chatty;

import chatty.gui.MainGui;
import chatty.util.DateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Twitch Chat commands. All the Twitch specific commands like /mod, /timeout..
 * 
 * @author tduva
 */
public class TwitchCommands {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchCommands.class.getName());
    
    /**
     * The delay between /mods requests. This is the delay in between each
     * request, not how often it is requested for one channel (it is currently
     * only requested once for each channel).
     */
    private static final int REQUEST_MODS_DELAY = 30*1000;
    
    /**
     * Channels which currently wait for a /mods response that should be silent
     * (no message output).
     */
    private final Set<String> silentModsRequestChannel
            = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Channels for which the /mods list has already been requested.
     */
    private final Set<String> modsAlreadyRequested
            = Collections.synchronizedSet(new HashSet<String>());
    
    private MainGui g;
    private TwitchClient c;
    
    public TwitchCommands(MainGui g, TwitchClient c) {
        this.g = g;
        this.c = c;
    }
    
    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }
    
    private void sendMessage(String channel, String message, String echo) {
        c.sendCommandMessage(channel, message, echo);
    }
    
    protected void commandTimeout(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /to <nick> [time]");
            return;
        }
        String[] parts = parameter.split(" ");
        if (parts.length < 1) {
            g.printLine("Usage: /to <nick> [time]");
        }
        else if (parts.length < 2) {
            timeout(channel, parts[0], 0);
        }
        else {
            try {
                int time = Integer.parseInt(parts[1]);
                timeout(channel, parts[0], time);
            } catch (NumberFormatException ex) {
                g.printLine("Usage: /to <nick> [time] (no valid time specified)");
            }
        }
    }
    
    protected void commandSlowmodeOn(String channel, String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            slowmodeOn(channel, 0);
        }
        else {
            try {
                int time = Integer.parseInt(parameter);
                slowmodeOn(channel, time);
            } catch (NumberFormatException ex) {
                g.printLine("Usage: /slow [time] (invalid time specified)");
            }
        }
    }
    
    protected void commandUnban(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /unban <nick>");
        }
        else {
            unban(channel, parameter);
        }
    }
    
    protected void commandBan(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /ban <nick>");
        }
        else {
            ban(channel, parameter);
        }
    }
    
    protected void commandMod(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /mod <nick>");
        }
        else {
            mod(channel, parameter);
        }
    }
    
    protected void commandUnmod(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /unmod <nick>");
        }
        else {
            unmod(channel, parameter);
        }
    }
    
    protected void commandHostmode(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /host <stream>");
        } else {
            hostmode(channel, parameter);
        }
    }
    
    public void hostmode(String channel, String target) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".host "+target, "Trying to host "+target+"..");
        }
    }
    
    public void hostmodeOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".unhost", "Trying to turn off host mode..");
        }
    }
    
    /**
     * Turn on slowmode with the given amount of seconds or the default time
     * (without specifying a time).
     * 
     * @param channel The name of the channel
     * @param time The time in seconds, 0 or negative numbers will make it give
     *  not time at all
     */
    public void slowmodeOn(String channel, int time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".slow", "Trying to turn on slowmode..");
            }
            else {
                sendMessage(channel,".slow "+time, "Trying to turn on slowmode ("+time+"s)");
            }
        }
    }
    
    /**
     * Turns off slowmode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void slowmodeOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".slowoff", "Trying to turn off slowmode..");
        }
    }
    
    /**
     * Turns on subscriber only mode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void subscribersOn(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribers", "Trying to turn on subscribers mode..");
        }
    }
    
    public void subscribersOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribersoff", "Trying to turn off subscribers mode..");
        }
    }
    
    public void clearChannel(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".clear", "Trying to clear channel..");
        }
    }

    public void ban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".ban "+name, "Trying to ban "+name+"..");
        }
    }
    
    public void mod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mod "+name, "Trying to mod "+name+"..");
        }
    }
    
    public void unmod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unmod "+name, "Trying to unmod "+name+"..");
        }
    }
    
    /**
     * Sends a timeout command to the server.
     * 
     * @param channel
     * @param name
     * @param time 
     */
    public void timeout(String channel, String name, int time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".timeout "+name, "Trying to timeout "+name+"..");
            }
            else {
                String formatted = DateTime.duration(time, true, false);
                String onlySeconds = time+"s";
                String timeString = formatted.equals(onlySeconds)
                        ? onlySeconds : onlySeconds+"/"+formatted;
                sendMessage(channel,".timeout "+name+" "+time,
                        "Trying to timeout "+name+" ("+timeString+")");
            }
            
        }
    }
    
    public void unban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unban "+name, "Trying to unban "+name+"..");
        }
    }
    
    public void mods(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mods", "Requesting moderator list..");
        }
    }
    
    public void modsSilent(String channel) {
        if (onChannel(channel, true)) {
            g.printLine(channel, "Trying to fix moderators..");
            requestModsSilent(channel);
        }
    }
    
    public void requestModsSilent(String channel) {
        if (onChannel(channel, false)) {
            silentModsRequestChannel.add(channel);
            c.sendSpamProtectedMessage(channel, ".mods");
        }
    }
    
    public boolean removeModsSilent(String channel) {
        return silentModsRequestChannel.remove(channel);
    }
    
    public boolean waitingForModsSilent() {
        return !silentModsRequestChannel.isEmpty();
    }
    
    /**
     * Prase the list of mods as returned from the Twitch Chat. The
     * comma-seperated list should start after the first colon ("The moderators
     * of this room are: ..").
     *
     * @param text The text as received from the Twitch Chat
     * @return A List of moderator names
     */
    public static List<String> parseModsList(String text) {
        int start = text.indexOf(":") + 1;
        List<String> modsList = new ArrayList<>();
        if (start > 1 && text.length() > start) {
            String mods = text.substring(start);
            if (!mods.trim().isEmpty()) {
                String[] modsArray = mods.split(",");
                for (String mod : modsArray) {
                    modsList.add(mod.trim());
                }
            }
        }
        return modsList;
    }
    
    /**
     * Starts the timer which requests the /mods list for joined channels.
     */
    public void startAutoRequestMods() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                autoRequestMods();
            }
        }, 1000, REQUEST_MODS_DELAY);
    }
    
    /**
     * If enabled in the settings, requests /mods for one currently joined
     * channel (and only one), ignoring the ones it was already requested for.
     */
    private void autoRequestMods() {
        if (!c.settings.getBoolean("autoRequestMods")) {
            return;
        }
        Set<String> joinedChannels = c.getJoinedChannels();
        for (String channel : joinedChannels) {
            if (!modsAlreadyRequested.contains(channel)) {
                LOGGER.info("Auto-requesting mods for "+channel);
                modsAlreadyRequested.add(channel);
                requestModsSilent(channel);
                return;
            }
        }
    }
    
    /**
     * Removes one or all entries from the list of channels the /mods list was
     * already requested for. This can be used on part/disconnect, since users
     * are removed then.
     * 
     * @param channel The name of the channel to remove, or null to remove all
     * entries
     */
    public void clearModsAlreadyRequested(String channel) {
        if (channel == null) {
            modsAlreadyRequested.clear();
        } else {
            modsAlreadyRequested.remove(channel);
        }
    }
    
}
