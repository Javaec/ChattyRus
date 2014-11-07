
package chatty;

import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Logger;

/**
 * Provides methods to get a (maybe new) User object for a channel/username 
 * combination and search for User objects by channel, username etc.
 * 
 * <p>
 * <strong>Subscriber detection</strong> works in three different ways. For the
 * local user, the SPECIALUSER message is send after he joins a channel, which
 * means checking on the SPECIALUSER message which of several possible channels
 * may have been meant (save channel join times, have some buffer time, remove
 * all old ones and check if only one remains).</p>
 * 
 * <p>
 * For other users (of which messages are received from the server), the
 * SPECIALUSER message is send before every chat message, which means checking
 * on every chat message, if a SPECIALUSER message is cached for that user. So
 * in this case, the channel will already be known when checking, checking finds
 * out whether the user in the channel the chat message was received on should
 * be set as a subscriber (again some buffer time for which SUBSCRIBER messages
 * are cached).</p>
 *
 * <p>
 * The third way is using the emotesets and the third-party source of
 * emoteset-channel association (by twitchemotes.com) to set subscriber status
 * when the emotesets are received.</p>
 * 
 * <p>
 * <strong>Turbo, Admin, Staff detection</strong> does not have to be channel
 * specific, so that status can just be set for every user with the same
 * name.</p>
 * 
 * @author tduva
 */
public class UserManager {

    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    
    /**
     * How many milliseconds after the subscriber message the actual channel
     * message has to be received from the same user to make him a subscriber
     */
    private static final int SUBSCRIBER_BUFFER_TIME = 500;
    
    /**
     * How long for the modlist requests to expire. Saving the modlist requests
     * is also a measure to prevent lag from mixing up requests/responses, so
     * this should be relatively high compared to the valid time.
     */
    private static final int MODLIST_EXPIRE_TIME = 15*1000;
    
    /**
     * How long after the request should a modlist response be assumed to belong
     * to the request (and thus the channel).
     */
    private static final int MODLIST_VALID_TIME = 5*1000;
    
    private final Set<UserManagerListener> listeners = new HashSet<>();
    
    private final HashMap<String, HashMap<String, User>> users = new HashMap<>();
    private final HashMap<String, String> cachedEmoteSets = new HashMap<>();
    private final HashMap<String, String> cachedColors = new HashMap<>();
    private final HashSet<String> cachedTurbo = new HashSet<>();
    private final HashSet<String> cachedAdmin = new HashSet<>();
    private final HashSet<String> cachedStaff = new HashSet<>();
    private final HashMap<String, Long> cachedSubscriber = new HashMap<>();
    private final HashMap<String, Long> modsListRequested = new HashMap<>();
    private final HashMap<String, Long> channelJoined = new HashMap<>();
    private boolean capitalizedNames = false;
    
    private final Map<Integer, String> emotesets = Collections.synchronizedMap(new HashMap<Integer, String>());
    
    private final User errorUser = new User("[Error]", "#[error]");

    private CapitalizedNames capitalizedNamesManager;
    private UsericonManager usericonManager;
    private UsercolorManager usercolorManager;
    private Addressbook addressbook;
    
    public void addListener(UserManagerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    private void userUpdated(User user) {
        for (UserManagerListener listener : listeners) {
            listener.userUpdated(user);
        }
    }
    
    public void setCapitalizedNames(boolean capitalized) {
        capitalizedNames = capitalized;
    }
    
    public void setUsericonManager(UsericonManager manager) {
        usericonManager = manager;
    }
    
    public void setUsercolorManager(UsercolorManager manager) {
        usercolorManager = manager;
    }
    
    public void setAddressbook(Addressbook addressbook) {
        this.addressbook = addressbook;
    }
    
    public void setEmotesets(Map<Integer, String> newEmotesets) {
        emotesets.putAll(newEmotesets);
    }
    
    public void setCapitalizedNamesManager(CapitalizedNames m) {
        if (m != null) {
            this.capitalizedNamesManager = m;
            m.addListener(new CapitalizedNames.CapitalizedNamesListener() {

                @Override
                public void setName(String name, String capitalizedName) {
                    List<User> users = getUsersByName(name);
                    for (User user : users) {
                        user.setDisplayNick(capitalizedName);
                        userUpdated(user);
                    }
                }
            });
        }
    }
    
    /**
     * Gets a Map of all User objects in the given channel.
     * 
     * @param channel
     * @return 
     */
    public synchronized HashMap<String, User> getUsersByChannel(String channel) {
        HashMap<String, User> result = users.get(channel);
        if (result == null) {
            result = new HashMap<>();
            users.put(channel, result);
        }
        return result;
    }

    /**
     * Searches all channels for the given username and returns a List of all
     * the associated User objects.
     * 
     * @param name The username to search for
     * @return The List of User-objects.
     */
    public synchronized List<User> getUsersByName(String name) {
        List<User> result = new ArrayList<>();
        Iterator<HashMap<String, User>> it = users.values().iterator();
        while (it.hasNext()) {
            HashMap<String, User> channelUsers = it.next();
            User user = channelUsers.get(name.toLowerCase());
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * Returns the user for the given channel and name, but only if an object
     * already exists.
     * 
     * @param channel
     * @param name
     * @return The {@code User} object or null if none exists
     */
    public synchronized User getUserIfExists(String channel, String name) {
        return getUsersByChannel(channel).get(name);
    }
    
    /**
     * Returns the User with the given name or creates a new User object if none
     * exists for this name.
     *
     * @param channel
     * @param name The name of the user
     * @return The matching User object
     * @see User
     */
    public synchronized User getUser(String channel, String name) {
        // Not sure if this makes sense
        if (name == null || name.isEmpty()) {
            return errorUser;
        }
        String displayName = name;
        name = name.toLowerCase(Locale.ENGLISH);
        User user = getUserIfExists(channel, name);
        if (user == null) {
            String capitalizedName = capitalizedNamesManager != null
                    ? capitalizedNamesManager.getName(name) : null;
            if (displayName.equals(name)) {
                if (capitalizedName != null) {
                    displayName = capitalizedName;
                } else if (capitalizedNames) {
                    displayName = name.substring(0, 1).toUpperCase() + name.substring(1);
                }
            }
            user = new User(displayName, capitalizedName, channel);
            user.setUsercolorManager(usercolorManager);
            user.setAddressbook(addressbook);
            user.setUsericonManager(usericonManager);
            // Initialize some values if present for this name
            if (cachedEmoteSets.containsKey(name)) {
                user.setEmoteSets(cachedEmoteSets.get(name));
            }
            if (cachedColors.containsKey(name)) {
                user.setColor(cachedColors.get(name));
            }
            if (cachedAdmin.contains(name)) {
                user.setAdmin(true);
            }
            if (cachedStaff.contains(name)) {
                user.setStaff(true);
            }
            if (cachedTurbo.contains(name)) {
                user.setTurbo(true);
            }
            // Put User into the map for the channel
            getUsersByChannel(channel).put(name, user);
        } else if (capitalizedNamesManager != null) {
            //System.out.println(name);
            capitalizedNamesManager.activity(name);
        }
        return user;
    }
    
    /**
     * Searches all channels for the given username and returns a Map with
     * all channels the username was found in and the associated User objects.
     * 
     * @param name The username to be searched for
     * @return A Map with channel->User association
     */
    public synchronized HashMap<String,User> getChannelsAndUsersByUserName(String name) {
        String lowercaseName = name.toLowerCase(Locale.ENGLISH);
        HashMap<String,User> result = new HashMap<>();
        
        Iterator<Entry<String, HashMap<String, User>>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, HashMap<String, User>> channel = it.next();
            
            String channelName = channel.getKey();
            HashMap<String,User> channelUsers = channel.getValue();
            
            User user = channelUsers.get(lowercaseName);
            if (user != null) {
                result.put(channelName,user);
            }
        }
        return result;
    }
    
    public synchronized void clear() {
        users.clear();
    }
    
    public synchronized void clear(String channel) {
        getUsersByChannel(channel).clear();
    }
    
    public synchronized void setAllOffline() {
        Iterator<HashMap<String,User>> it = users.values().iterator();
        while (it.hasNext()) {
            HashMap<String,User> channel = it.next();
            for (User user : channel.values()) {
                user.setOnline(false);
            }
        }
    }
    
    
    protected synchronized void setEmoteSetForUsername(String userName, String emoteSet) {
        cachedEmoteSets.put(userName.toLowerCase(),emoteSet);
        List<User> userAllChans = getUsersByName(userName);
        for (User user : userAllChans) {
            user.setEmoteSets(emoteSet);
            setSubByEmoteset(user);
        }
    }
    
    private void setSubByEmoteset(User user) {
        if (user.isSubscriber()) {
            return;
        }
        for (int emoteset : user.getEmoteSet()) {
            // channel might be null if emoteset isn't found
            String stream = emotesets.get(emoteset);
            if (user.getStream().equals(stream)) {
                user.setSubscriber(true);
                userUpdated(user);
            }
        }
    }
    
    /**
     * Sets the color of a user across all channels.
     * 
     * @param userName String The name of the user
     * @param color String The color as a string representation
     */
    protected synchronized void setColorForUsername(String userName, String color) {
        userName = userName.toLowerCase();
        cachedColors.put(userName,color);
        
        List<User> userAllChans = getUsersByName(userName);
        for (User user : userAllChans) {
            user.setColor(color);
        }
    }
    
    /**
     * Sets a user as a special user across all channels.
     * 
     * @param userName The name of the user
     * @param type The type of SPECIALUSER message
     * @param channel The channel context, or null if none is given
     * @param isLocalUser Whether this is the name of the local user
     */
    protected synchronized void userSetSpecialUser(String userName, String type, String channel, boolean isLocalUser) {
        
        userName = userName.toLowerCase();
        List<User> userAllChans;
        
        // If the local user joined a channel, it should send special user stuff
        // shortly after, so check if there is a channel joined in the last x ms
        // and set that one as channel (unless one is already set)
        if (isLocalUser && channel != null) {
            String lastJoinedChannel = getLastChannelJoined();
            if (lastJoinedChannel != null) {
                channel = lastJoinedChannel;
            }
        }
        
        // Depending on whether a channel context is given, get all users with
        // this name or the user of the specified channel
        if (channel == null) {
             userAllChans = getUsersByName(userName);
        } else {
            userAllChans = new ArrayList<>();
            userAllChans.add(getUser(channel, userName));
        }
        
        for (User user : userAllChans) {
            if (type.equals("admin")) {
                user.setAdmin(true);
            } else if (type.equals("staff")) {
                user.setStaff(true);
            } else if (type.equals("turbo")) {
                user.setTurbo(true);
            } else if (type.equals("subscriber")) {
                // If channel context is given, this means the user in the
                // specified channel was selected and sub status can be set
                if (channel != null) {
                    if (!user.isSubscriber()) {
                        user.setSubscriber(true);
                        userUpdated(user);
                    }
                }
            }
        }
        
        // Cache status, so it can be set immediately for new users with the
        // same name (and to do sub detection with the sub status)
        switch (type) {
            case "admin":
                cachedAdmin.add(userName);
                break;
            case "staff":
                cachedStaff.add(userName);
                break;
            case "turbo":
                cachedTurbo.add(userName);
                break;
            case "subscriber":
                cachedSubscriber.put(userName, System.currentTimeMillis());
        }
    }
    
    /**
     * When a channel message is received, check if a SPECIALUSER subscriber
     * message was sent for a user with that name directly before.
     * 
     * @param user 
     */
    protected synchronized void channelMessage(User user) {
        if (cachedSubscriber.isEmpty()) {
            return;
        }
        Long cachedSubscriberTime = cachedSubscriber.remove(user.nick);
        if (cachedSubscriberTime != null) {
            long passedTime = System.currentTimeMillis() - cachedSubscriberTime;
            if (passedTime < SUBSCRIBER_BUFFER_TIME) {
                if (user.setProbablySubscriber()) {
                    userUpdated(user);
                }
            }
        }
        clearExpired(cachedSubscriber, SUBSCRIBER_BUFFER_TIME);
    }
    
    /**
     * Add this channel to the joined channels.
     * 
     * @param channel 
     */
    protected synchronized void channelJoined(String channel) {
        channelJoined.put(channel, System.currentTimeMillis());
    }
    
    /**
     * Returns the channel that was joined within the last
     * SUBSCRIBER_BUFFER_TIME milliseconds, if it is the only channel joined
     * within that time.
     *
     * @return A {@code String} containing the channel name, or null if no or
     * more than one channel was found
     */
    private String getLastChannelJoined() {
        // Clear all channel joins not within the expire time
        clearExpired(channelJoined, SUBSCRIBER_BUFFER_TIME);
        
        if (channelJoined.size() == 1) {
            return channelJoined.keySet().iterator().next();
        }
        return null;
    }
    
    /**
     * Helper function that removes all entries of the given map that are
     * expired, based on the given bufferTime.
     * 
     * This assumes that the value of the entries is a
     * System.currentTimeMillis() time value.
     * 
     * @param map The map to clear
     * @param bufferTime The time in milliseconds after which an entry is
     * handled as expired
     */
    private static void clearExpired(Map<String, Long> map, long bufferTime) {
        Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > bufferTime) {
                it.remove();
            }
        }
    }
    
    /**
     * Set the modlist as requested for the given channel.
     * 
     * @param channel 
     */
    protected synchronized void modsListRequested(String channel) {
        modsListRequested.put(channel, System.currentTimeMillis());
    }
    
    /**
     * Received list of mods without channel context, which means having to
     * check which channel may have been meant based on recent requests send for
     * a modlist (/mods).
     * 
     * @param modsList
     * @return 
     */
    protected synchronized ModListInfo modsListReceived(List<String> modsList) {
        // Remove all entries from modlist requests that have expired
        clearExpired(modsListRequested, MODLIST_VALID_TIME);
        
        // If this is the only entry after removing expired requests, then assume
        // this as the response to the request and thus determine the channel
        if (modsListRequested.size() == 1) {
            Map.Entry<String, Long> entry = modsListRequested.entrySet().iterator().next();
            String channel = entry.getKey();
            long ago = System.currentTimeMillis() - entry.getValue();
            if (ago < MODLIST_VALID_TIME) {
                return new ModListInfo(channel, modsListReceived(channel, modsList));
            }
            modsListRequested.clear();
        }
        return null;
    }
    
    protected static class ModListInfo {
        public final String channel;
        public final List<User> users;
        
        public ModListInfo(String channel, List<User> users) {
            this.channel = channel;
            this.users = users;
        }
        
    }
    
    /**
     * The list of mods received with channel context, set the containing names
     * as mod. Returns the changed users so they can be updated in the GUI.
     * 
     * @param channel
     * @param modsList
     * @return 
     */
    protected synchronized List<User> modsListReceived(String channel, List<String> modsList) {
        // Demod everyone on the channel
        Map<String,User> usersToDemod = getUsersByChannel(channel);
        for (User user : usersToDemod.values()) {
            user.setModerator(false);
        }
        // Mod everyone in the list
        LOGGER.info("Setting users as mod for "+channel+": "+modsList);
        List<User> changedUsers = new ArrayList<>();
        for (String userName : modsList) {
            if (Helper.validateChannel(userName)) {
                User user = getUser(channel, userName);
                if (!user.isModerator()) {
                    user.setModerator(true);
                    userUpdated(user);
                }
                changedUsers.add(user);
            }
        }
        return changedUsers;
    }
    
    public static interface UserManagerListener {
        public void userUpdated(User user);
    }
    
}
