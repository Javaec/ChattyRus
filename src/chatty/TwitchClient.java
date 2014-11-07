
package chatty;

import chatty.UserManager.ModListInfo;
import chatty.util.api.TwitchApiResultListener;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfoListener;
import chatty.util.api.TokenInfo;
import chatty.util.api.StreamInfo;
import chatty.util.api.ChannelInfo;
import chatty.util.api.TwitchApi;
import chatty.Version.VersionListener;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.Channel;
import chatty.util.BTTVEmotes;
import chatty.util.DateTime;
import chatty.util.EmoticonListener;
import chatty.util.FrankerFaceZ;
import chatty.util.FrankerFaceZListener;
import chatty.util.StreamStatusWriter;
import chatty.util.TwitchEmotes;
import chatty.util.TwitchEmotes.TwitchEmotesListener;
import chatty.util.Webserver;
import chatty.util.api.Emoticons;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.StreamInfo.ViewerStats;
import chatty.util.chatlog.ChatLog;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import chatty.util.srl.SpeedrunsLive;
import java.io.File;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Logger;

/**
 * The main client class, responsible for managing most parts of the program.
 * 
 * @author tduva
 */
public class TwitchClient extends Irc {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchClient.class.getName());
    
    private volatile boolean shuttingDown = false;
    
    /**
     * The URL to get a token.
     */
    public static final String REQUEST_TOKEN_URL = ""
            + "https://api.twitch.tv/kraken/oauth2/authorize"
            + "?response_type=token"
            + "&client_id="+Chatty.CLIENT_ID
            + "&redirect_uri="+Chatty.REDIRECT_URI
            + "&scope=chat_login";
    
    /**
     * The interval to check version in (seconds)
     */
    private static final int CHECK_VERSION_INTERVAL = 60*60*24*2;

    /**
     * Holds the Settings object, which is used to store and retrieve renametings
     */
    public final Settings settings;
    
    public final ChatLog chatLog;
    
    /**
     * Holds the TwitchApi object, which is used to make API requests
     */
    public final TwitchApi api;
    
    public final TwitchEmotes twitchemotes;
    
    public final BTTVEmotes bttvEmotes;
    
    public final FrankerFaceZ frankerFaceZ;
    
    public final ChannelFavorites channelFavorites;
    
    private final TwitchCommands twitchCommands;
    
    public final UsercolorManager usercolorManager;
    
    public final UsericonManager usericonManager;
    
    public final Addressbook addressbook;
    
    public final SpeedrunsLive speedrunsLive;
    
    public final StatusHistory statusHistory;
    
    public final StreamStatusWriter streamStatusWriter;
    
    protected final CapitalizedNames capitalizedNames;
    
    /**
     * Holds the UserManager instance, which manages all the user objects.
     */
    protected UserManager users = new UserManager();
    
    /**
     * The username to send to the server. This is stored to reconnect.
     */
    private String username;
    
    /**
     * The actual password to send to the server. This can be a token as well
     * as a password. This is stored to reconnect.
     */
    private String password;
    
    private String serverPorts = "6667";
    
    /**
     * A reference to the Main Gui.
     */
    protected MainGui g;
    
    /**
     * Channels that should be joined after connecting.
     */
    private String[] autojoin;
    /**
     * Channels that are open in the program (in tabs if it's more than one).
     */
    private final Set<String> openChannels = new HashSet<>();
    /**
     * Channels that are not only open, but also actually joined.
     */
    private final Set<String> joinedChannels = Collections.synchronizedSet(new HashSet<String>());
    private final Vector<String> servers = new Vector<>();
    private final JoinChecker joinChecker = new JoinChecker(this);
    
    private List<String> cachedDebugMessages = new ArrayList<>();
    private List<String> cachedWarningMessages = new ArrayList<>();
    
    /**
     * The server id in servers next to connect to
     */
    private int serverCycle = 0;
    
    /**
     * How many times was tried to reconnect
     */
    private int reconnectionAttempts = 0;
    /**
     * How many times to try to reconnect
     */
    private final int maxReconnectionAttempts = 20;
    /**
     * The time between reconnection attempts. The time for the first attempt,
     * second time for the second attempt etc..
     */
    private final static int[] RECONNECTION_DELAY = new int[]{1,5,5,10,10,60};
    
    /**
     * User used for testing without connecting.
     */
    private User testUser;
    private final StreamInfo testStreamInfo = new StreamInfo("testStreamInfo", null);
    
    public final User specialUser = new User("[specialUser]","[nochannel]");

    private volatile ReconnectionTimer reconnectionTimer;
    
    private Webserver webserver;
    private final HotkeyManager hotkeyManager;
    private final SettingsManager settingsManager;
    private final SpamProtection spamProtection;
    public final CustomCommands customCommands;
    
    private final Set<String> refreshRequests = new HashSet<>();
    
    public TwitchClient(HashMap<String, String> args) {

        // Logging
        new Logging(this);
        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());

        LOGGER.info("### Log start ("+DateTime.fullDateTime()+")");
        LOGGER.info(Chatty.chattyVersion());
        LOGGER.info(Helper.systemInfo());
        LOGGER.info("Working Directory: "+System.getProperty("user.dir")
                +" Settings Directory: "+Chatty.getUserDataDirectory()
                +" Classpath: "+System.getProperty("java.class.path")
                +" Library Path: "+System.getProperty("java.library.path"));
        LOGGER.info("Retina Display: "+GuiUtil.hasRetinaDisplay());

        createTestUser("tduva", "#bacon_donut");
        
        settings = new Settings(Chatty.getUserDataDirectory()+"settings");
        api = new TwitchApi(new TwitchApiResults(), new MyStreamInfoListener());
        twitchemotes = new TwitchEmotes(new TwitchemotesListener());
        bttvEmotes = new BTTVEmotes(new EmoteListener());
        frankerFaceZ = new FrankerFaceZ(new EmoticonsListener());
        
        
        // Settings
        settingsManager = new SettingsManager(settings);
        settingsManager.defineSettings();
        settingsManager.loadSettingsFromFile();
        settingsManager.backupFiles();
        settingsManager.loadCommandLineSettings(args);
        settingsManager.overrideSettings();
        settingsManager.debugSettings();
        channelFavorites = new ChannelFavorites(settings);
        usercolorManager = new UsercolorManager(settings);
        usericonManager = new UsericonManager(settings);
        customCommands = new CustomCommands(settings);
        customCommands.loadFromSettings();
        settings.addSettingsListener(new SettingSaveListener());
        
        if (settings.getBoolean("correctlyCapitalizedNames")) {
            capitalizedNames = new CapitalizedNames(api);
        } else {
            capitalizedNames = null;
        }
        
        chatLog = new ChatLog(settings);
        chatLog.start();
        
        testUser.setUsericonManager(usericonManager);
        testUser.setUsercolorManager(usercolorManager);
        
        addressbook = new Addressbook(Chatty.getUserDataDirectory()+"addressbook");
        addressbook.loadFromFile();
        addressbook.setSomewhatUniqueCategories(settings.getString("abUniqueCats"));
        testUser.setAddressbook(addressbook);
        
        speedrunsLive = new SpeedrunsLive();
        
        statusHistory = new StatusHistory(settings);
        settings.addSettingsListener(statusHistory);
        
        spamProtection = new SpamProtection();
        spamProtection.setLinesPerSeconds(settings.getString("spamProtection"));
        
        users.setCapitalizedNames(settings.getBoolean("capitalizedNames"));
        users.setUsercolorManager(usercolorManager);
        users.setAddressbook(addressbook);
        users.setUsericonManager(usericonManager);
        users.setCapitalizedNamesManager(capitalizedNames);
        users.addListener(new UserManager.UserManagerListener() {

            @Override
            public void userUpdated(User user) {
                if (user.isOnline()) {
                    g.updateUser(user.getChannel(), user);
                }
            }
        });
        
        streamStatusWriter = new StreamStatusWriter(Chatty.getUserDataDirectory(), api);
        streamStatusWriter.setSetting(settings.getString("statusWriter"));
        streamStatusWriter.setEnabled(settings.getBoolean("enableStatusWriter"));
        settings.addSettingChangeListener(streamStatusWriter);
        
        initDxSettings();
        
        // Create GUI
        LOGGER.info("Create GUI..");
        g = new MainGui(this);
        g.loadSettings();
        g.showGui();
        
        twitchCommands = new TwitchCommands(g, this);
        twitchCommands.startAutoRequestMods();
        
        // Output any cached warning messages
        warning(null);
        
        // Check version, if enabled in this build
        if (Chatty.VERSION_CHECK_ENABLED) {
            checkNewVersion();
        }

        // Connect or open connect dialog
        if (settings.getBoolean("connectOnStartup")) {
            prepareConnection();
        } else {
            switch ((int)settings.getLong("onStart")) {
                case 1:
                    g.openConnectDialog(null);
                    break;
                case 2:
                    prepareConnectionWithChannel(settings.getString("autojoinChannel"));
                    break;
                case 3:
                    prepareConnectionWithChannel(settings.getString("previousChannel"));
                    break;
                case 4:
                    prepareConnectionWithChannel(Helper.buildStreamsString(channelFavorites.getFavorites()));
                    break;
            }
            
        }
        
        new UpdateTimer(g);
        
        // Hotkey
        hotkeyManager = new HotkeyManager(this, Chatty.HOTKEY);
        updateCommercialHotkey();
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown(this)));
        
        version5Info();
        
        if (Chatty.DEBUG) {
            specialUser.setEmoteSets("130,4280,793,33,42");
        }
    }
    

    /**
     * Based on the current renametings, rename the system properties to disable
     * Direct3D and/or DirectDraw.
     */
    private void initDxSettings() {
        try {
            Boolean d3d = !settings.getBoolean("nod3d");
            System.setProperty("sun.java2d.d3d", d3d.toString());
            Boolean ddraw = settings.getBoolean("noddraw");
            System.setProperty("sun.java2d.noddraw", ddraw.toString());
            //System.out.println(System.getProperty("sun.java2d.opengl"));
            LOGGER.info("Drawing settings: d3d: "+d3d+" / noddraw: "+ddraw);
        } catch (SecurityException ex) {
            LOGGER.warning("Error setting drawing settings: "+ex.getLocalizedMessage());
        }
    }
    
    /**
     * Checks for a new version if the last check was long enough ago.
     */
    private void checkNewVersion() {
        //g.setUpdateAvailable();
        if (!settings.getBoolean("checkNewVersion")) {
            return;
        }
        long ago = System.currentTimeMillis() - settings.getLong("versionLastChecked");
        if (ago/1000 < CHECK_VERSION_INTERVAL) {
            return;
        }
        settings.setLong("versionLastChecked", System.currentTimeMillis());
        g.printSystem("Checking for new version..");
        
        new Version(new VersionListener() {

            @Override
            public void versionChecked(String version, String info, boolean isNewVersion) {
                if (isNewVersion) {
                    String infoText = "";
                    if (!info.isEmpty()) {
                        infoText = "[" + info + "] ";
                    }
                    g.printSystem("New version available: "+version+" "+infoText
                            +"(Go to <Help-Website> to download)");
                    g.setUpdateAvailable(version);
                } else {
                    g.printSystem("You already have the newest version.");
                }
            }
        });
    }
    
    /**
     * Creates the test user, also allowing it to be recreated with another name
     * or channel for testing while the programm is running.
     * 
     * @param name
     * @param channel 
     */
    private void createTestUser(String name, String channel) {
        testUser = new User(name, channel);
        testUser.setColor("blue");
        testUser.setTurbo(true);
        testUser.setModerator(true);
        testUser.setSubscriber(true);
        testUser.setEmoteSets("4280");
        //testUser.setAdmin(true);
        //testUser.setStaff(true);
        testUser.setBroadcaster(true);
    }
    

    @Override
    void onConnectionAttempt(String server, int port) {
        if (server != null) {
            g.printLineAll("Trying to connect to "+server+":"+port);
        } else {
            g.printLineAll("Failed to connect (server or port invalid)");
        }
    }
    
    @Override
    void onConnect() {
        if (settings.getBoolean("tc3")) {
            send("TWITCHCLIENT 3");
        } else {
            send("TWITCHCLIENT");
        }
    }
    
    @Override
    void onRegistered() {
        reconnectionAttempts = 0;
        if (!openChannels.isEmpty()) {
            joinChannels(openChannels);
        }
        else if (autojoin != null) {
            for (String channel : autojoin) {
                joinChannel(channel);
            }
        }
        g.updateHighlightSetUsername(username);
    }
    
    /**
     * Close all channels except the ones in the given Array.
     * 
     * @param except 
     */
    private void closeChannels(String[] except) {
        // Is copy here necessary?
        Set<String> copy = new HashSet<>(openChannels);
        for (String channel : copy) {
            if (!Arrays.asList(except).contains(channel)) {
                closeChannel(channel);
            }
        }
        openChannels.clear();
    }
    
    /**
     * Close a channel by either parting it if it is currently joined or
     * just closing the tab.
     * 
     * @param channel 
     */
    public void closeChannel(String channel) {
        if (onChannel(channel)) {
            partChannel(channel);
        }
        else { // Always remove channel (or try to), so it can be closed even if it bugged out
            logViewerstats(channel);
            openChannels.remove(channel);
            g.removeChannel(channel);
            chatLog.closeChannel(channel);
        }
    }
    
    public Set<String> getJoinedChannels() {
        synchronized(joinedChannels) {
            return new HashSet<>(joinedChannels);
        }
    }
    
    /**
     * When disconnected, output a message and if necessary start the timer
     * to reconnect.
     * 
     * @param reason
     * @param reasonMessage 
     */
    @Override
    void onDisconnect(int reason, String reasonMessage) {
        joinedChannels.clear();
        users.clear();
        twitchCommands.clearModsAlreadyRequested(null);
        g.printLineAll("Disconnected"+Helper.makeDisconnectReason(reason,reasonMessage));
        g.clearUsers();
        
        if (reason != Irc.REQUESTED_DISCONNECT) {
            //g.printLineAll("dns pool");
            //Security.setProperty("networkaddress.cache.ttl", "1");
            if (reconnectionTimer == null) {
                if (reconnectionAttempts >= maxReconnectionAttempts) {
                    g.printLineAll("Gave up reconnecting. :(");
                }
                else {
                    int delay = getReconnectionDelay(reconnectionAttempts);
                    if (reason == Irc.ERROR_UNKNOWN_HOST) {
                        delay = getMaxReconnectionDelay();
                    }
                    g.printLineAll(
                            "Reconnection attempt in " +
                            delay + " seconds"
                    );
                    setState(Irc.STATE_RECONNECTING);
                    reconnectionTimer = new ReconnectionTimer(this, reason, delay);
                }
            }
        }
        if (reason == Irc.ERROR_REGISTRATION_FAILED) {
            checkToken();
        }
    }
    
    /**
     * Gets the reconnection delay based on the number of attempts.
     * 
     * @param attempt The number of attempts
     * @return The delay in seconds
     */
    private int getReconnectionDelay(int attempt) {
        if (attempt < 0 || attempt >= RECONNECTION_DELAY.length) {
            return getMaxReconnectionDelay();
        }
        return RECONNECTION_DELAY[attempt];
    }
    
    /**
     * Gets the maximum reconnection delay defined.
     * 
     * @return The delay in seconds
     */
    private int getMaxReconnectionDelay() {
        return RECONNECTION_DELAY[RECONNECTION_DELAY.length - 1];
    }
    
    @Override
    void onJoinAttempt(String channel) {
        /**
         * This should be the event where the channel is first opened, and the
         * stream info should be output then. If the stream info is already
         * valid, then it is output now, otherwise it is requested by this and
         * output once it is received. Doing this later, like onJoin, won't work
         * because opening the channel will always request stream info, so it
         * might be output twice (once onJoin, a second time because it is new).
         */
        if (!isChannelOpen(channel)) {
            g.printStreamInfo(channel);
        }
        
        g.printLine(channel, "Joining "+channel+"..");
        joinChecker.joinAttempt(channel);
        openChannels.add(channel);
    }
    
    @Override
    void onJoin(String channel, String nick, String prefix) {
        if (nick.equalsIgnoreCase(this.username)) {
            joinChecker.joined(channel);
            // The local user has joined a channel
            LOGGER.info("JOINED: "+channel);
            joinedChannels.add(channel);
            users.channelJoined(channel);
            if (joinedChannels.size() == 2) {
                g.channelsWarning();
            }
            channelFavorites.addChannelToHistory(channel);
            userJoined(channel, nick);
            g.printLine(channel,"You have joined " + channel);
            // Icons and FFZ
            api.requestChatIcons(Helper.toStream(channel), false);
            if (settings.getBoolean("ffz")) {
                frankerFaceZ.requestEmotes(channel, false);
            }
        } else if (!settings.getBoolean("ignoreJoinsParts")) {
            // Another user has joined a joined channel
            User user = userJoined(channel, nick);
            if (settings.getBoolean("showJoinsParts")) {
                g.printCompact(channel,"JOIN", user);
            }
            g.playSound("joinPart", channel);
            chatLog.compact(channel, "JOIN", user);
        }
        
    }
    
    @Override
    void onPart(String channel, String nick, String prefix, String message) {

        if (!onChannel(channel)) {
            return;
        }
        if (nick.equalsIgnoreCase(this.username)) {
            userOffline(channel, nick);
            joinedChannels.remove(channel);
            chatLog.info(channel, "You have left "+channel);
            closeChannel(channel);
            // Remove users for this channel
            users.clear(channel);
            twitchCommands.clearModsAlreadyRequested(channel);
        } else if (!settings.getBoolean("ignoreJoinsParts")) {
            User user = userOffline(channel, nick);
            if (settings.getBoolean("showJoinsParts")) {
                g.printCompact(channel, "PART", user);
            }
            g.playSound("joinPart", channel);
            chatLog.compact(channel, "PART", user);
        }
        
    }
    
    @Override
    void onModeChange(String channel, String nick, boolean modeAdded, String mode, String prefix) {
        if (onChannel(channel)) {
            User user = users.getUser(channel, nick);
            boolean modMessagesEnabled = settings.getBoolean("showModMessages");
            if (modeAdded) {
                user.setMode(mode);
                if (mode.equals("o")) {
                    if (modMessagesEnabled) {
                        g.printCompact(channel,"MOD", user);
                    }
                    chatLog.compact(channel, "MOD", user);
                    // Set user as online if TC3 is enabled, because it's better
                    // than nothing, but not very exact of course
                    if (settings.getBoolean("tc3")) {
                        userJoined(user);
                    }
                }
            } else {
                user.setMode("");
                if (mode.equals("o")) {
                    if (modMessagesEnabled) {
                        g.printCompact(channel,"UNMOD", user);
                    }
                    chatLog.compact(channel, "UNMOD", user);
                }
            }
            g.userUpdated(user);
            // Notify userlist to update the changed user, but only if he is still
            // in the channel
            if (user.isOnline()) {
                g.updateUser(channel, user);
            }
        }
    }
    
    @Override
    void onChannelMessage(String channel, String nick, String from, String text) {
        if (onChannel(channel)) {
            if (settings.getBoolean("twitchnotifyAsInfo") && nick.equals("twitchnotify")) {
                g.printLine(channel, "[Notification] "+text);
            } else if (nick.equals("jtv")) {
                specialMessage(text, channel);
            } else {
                text = Helper.removeDuplicateWhitespace(text);
                text = Helper.htmlspecialchars_decode(text);
                //System.out.println(text);
                if (settings.getBoolean("removeCombiningCharacters")) {
                    String prev = text;
                    text = Helper.removeCombiningCharacters(text);
                    if (!prev.equals(text)) {
                        LOGGER.info("Filtered combining characters: ["+prev+"] -> ["+text+"]");
                    }
                }
                //System.out.println(text);
                User user = userJoined(channel, nick);
                users.channelMessage(user);
                g.printMessage(channel,user,text,false);
                addressbookCommands(channel, user, text);
            }
        }
    }
    
    private void addressbookCommands(String channel, User user, String text) {
        if (settings.getString("abCommandsChannel").equals(channel)
                && user.isModerator()) {
            text = text.trim();
            if (text.length() < 2) {
                return;
            }
            String command = text.split(" ")[0].substring(1);
            List<String> activatedCommands =
                    Arrays.asList(settings.getString("abCommands").split(","));
            if (activatedCommands.contains(command)) {
                String commandText = text.substring(1);
                g.printSystem("[Ab/Mod] "+addressbook.command(commandText));
            }
//            String[] split = text.split(" ", 2);
//            String[] parameters = new String[0];
//            if (split.length == 2) {
//                
//            }
//            if (split[0].equals("!add")) {
//                
//            }
//            if (split.length == 2) {
//                if (split[0].equals("!add")) {
//                    g.printSystem("[Ab/Mod] "+addressbook.commandAdd(split[1].split(" ")));
//                }
//            }
        }
    }
    
    @Override
    void onChannelAction(String channel, String nick, String from, String text) {
        if (onChannel(channel)) {
            User user = userJoined(channel, nick);
            g.printMessage(channel, user, text, true);
        }
    }
    
    @Override
    void onNotice(String nick, String from, String text) {
        // Should only be from the server for now
        g.printLine("[Notice] "+text);
    }

    
    @Override
    void onQueryMessage(String nick, String from, String text) {
        // Any messages from jtv shown directly or used appropriatly, don't think
        // there can be any private messages from other users anyway
        // (although it might be renamed some time)
        if (nick.equals("jtv")) {
            specialMessage(text, null);
        }
    }
    
    private void specialMessage(String text, String channel) {
        String[] split = text.split(" ");
        if (split[0].equals("USERCOLOR") && split.length == 3) {
            String colorNick = split[1];
            String color = split[2];
            users.setColorForUsername(colorNick, color);
        } else if (split[0].equals("CLEARCHAT") && split.length == 2) {
            String nickname = split[1];
            if (channel == null) {
                userBanned(nickname);
            } else {
                User user = users.getUserIfExists(channel, nickname);
                if (user != null) {
                    userBanned(user);
                }
            }
        } else if (split[0].equals("CLEARCHAT") && split.length == 1) {
            channelCleared(channel);
        } else if (split[0].equals("SPECIALUSER") && split.length == 3) {
            String specialuserNick = split[1];
            String type = split[2];
            boolean singleChannel = joinedChannels.size() == 1;
            String onlyOpenChannel = channel;
            if (singleChannel && channel == null) {
                onlyOpenChannel = joinedChannels.iterator().next();
            }
            boolean localUser = username.equalsIgnoreCase(specialuserNick);
            users.userSetSpecialUser(specialuserNick, type, onlyOpenChannel, localUser);
        } else if (split[0].equals("EMOTESET") && split.length == 3) {
            String emotesetNick = split[1];
            String emoteset = split[2];
            users.setEmoteSetForUsername(emotesetNick, emoteset);
            if (username.equalsIgnoreCase(emotesetNick)) {
                specialUser.setEmoteSets(emoteset);
                g.updateEmotesDialog();
            }
        } // Commands are usually all uppercase, so don't show any of those
        else if (Helper.isAllUppercaseLetters(split[0])) {
            return;
        } // Show anything else, since it might be a useful message
        // (Possibly add some translation later)
        else {
            if (text.startsWith("The moderators of this room are:")) {
                parseModeratorsList(text, channel);
            } else {
                g.printLine(channel, "[Info] " + text);
            }
        }
    }
    
    /**
     * Counts the moderators in the /mods response and outputs the count.
     * 
     * @param text The mesasge from jtv containing the comma-seperated moderator
     *  list.
     * @param channel The channel the moderators list was received on, or
     * {@literal null} if the channel is unknown
     */
    private void parseModeratorsList(String text, String channel) {
        
        // Get list of users from message
        List<String> modsList = TwitchCommands.parseModsList(text);

        // Give the usermanager the userlist, depending on whether the channel
        // is known or not, getting the resulting channel if it isn't
        List<User> changedUsers;
        if (channel == null) {
             ModListInfo info = users.modsListReceived(modsList);
             if (info == null) {
                 changedUsers = null;
                 channel = null;
             } else {
                changedUsers = info.users;
                channel = info.channel;
             }
             
        } else {
            changedUsers = users.modsListReceived(channel, modsList);
        }
        
        // Update users in the userlist whose mod status may have changed
//        if (changedUsers != null) {
//            for (User user : changedUsers) {
//                if (user.isOnline()) {
//                    g.updateUser(user.getChannel(), user);
//                }
//            }
//        }
        
        /**
         * Output messages only if either:
         * 
         * a) No /mod response is currently expected to be silent
         * Or
         * b) The channel was detected (TC3 or through guessing) and is not on
         *    the list of channels with a /mod response expected to be silent
         * 
         * a) has to be checked first, because b) might remove the channel, so
         * a) might be true even if it shouldn't be
         */
        if (!twitchCommands.waitingForModsSilent() || 
                (channel != null && !twitchCommands.removeModsSilent(channel))) {
            g.printLine(channel, "[Info] " + text);
            
            // Output appropriate message
            if (modsList.size() > 0) {
                g.printLine(channel, "There are " + modsList.size() + " mods for this channel.");
            } else {
                g.printLine(channel, "There are no mods for this channel.");
            }
        } else {
            LOGGER.info("Silent mods list ("+channel+")");
        }
    }
    
   /**
     * When a user is banned, find all users in all channels with that name and
     * perform the ban stuff there.
     *
     * Unfortunately there is no channel parameter in this message so this is
     * the best way to do this without opening a connection for each channel.
     * 
     * @param userName A String with the name of the banned user
     */
    private void userBanned(String userName) {
        HashMap<String, User> channelsAndUsers = users.getChannelsAndUsersByUserName(userName);
        Iterator<Entry<String, User>> it = channelsAndUsers.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, User> entry = it.next();
            userBanned(entry.getValue());
        }
    }
    
    /**
     * When a user is banned, and the channel is known, thus an actual User
     * object can be used (that is associated with a channel).
     * 
     * @param user The {@literal User} that was banned
     */
    private void userBanned(User user) {
        if (isChannelOpen(user.getChannel())) {
            g.userBanned(user.getChannel(), user);
            chatLog.compact(user.getChannel(), "BAN", user);
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    /**
     * Inform the user that a channel was cleared. If {@literal channel} is not
     * {@literal null}, then it is output to that channel. Otherwise it is output
     * to the current channel.
     * 
     * @param channel The channel that was cleared, or {@literal null} if the
     * channel is unknown
     */
    private void channelCleared(String channel) {
        if (channel != null || joinedChannels.size() == 1) {
            g.printLine(channel, "Channel was cleared by a moderator.");
        } else {
            g.printLineAll("One of the channels you joined was cleared by a moderator.");
        }
    }
    
    @Override
    void onSystemMessage(String message) {
        g.printLine(message);
    }
    
    @Override
    void onError(int error) {
        if (error == ERROR_UNKNOWN_HOST) {
            g.printLine("Unknown host.");
        }
    }
    
    @Override
    void onUserlist(String channel, String[] nicknames) {
        if (!settings.getBoolean("ignoreJoinsParts")) {
            for (String nick : nicknames) {
                userJoined(channel, nick);
            }
        }
    }
    
    @Override
    void onWhoResponse(String channel, String nickname) {
        if (!settings.getBoolean("ignoreJoinsParts")) {
            userJoined(channel, nickname);
        }
    }
    
    /**
     * Sets a user as online, add the user to the userlist if not already
     * online.
     * 
     * @param channel The channel the user joined
     * @param name  The name of the user
     * @return      The User
     */
    public User userJoined(String channel, String name) {
        User user = users.getUser(channel, name);
        return userJoined(user);
    }
    
    public User userJoined(User user) {
        if (!user.isOnline()) {
            String channel = user.getChannel();
            if (Helper.toLowerCase(channel).substring(1).equals(user.nick)) {
                user.setBroadcaster(true);
            }
            user.setOnline(true);
            g.addUser(channel, user);
        }
        return user;
    }
    
    /**
     * Sets a user as offline, removing the user from the userlist, the user
     * won't be deleted though, for possible further reference
     * 
     * @param channel
     * @param name 
     * @return  
     */
    public User userOffline(String channel, String name) {
        User user = users.getUser(channel, name);
        if (user != null) {
            user.setOnline(false);
            g.removeUser(channel,user);
        }
        return user;
    }
    
    public void clearUserList() {
        users.setAllOffline();
        g.clearUsers();
    }
    
    private String getServer() {
        String serverDefault = settings.getString("serverDefault");
        String serverTemp = settings.getString("server");
        return serverTemp.length() > 0 ? serverTemp : serverDefault;
    }
    
    private String getPorts() {
        String portDefault = settings.getString("portDefault");
        String portTemp = settings.getString("port");
        return portTemp.length() > 0 ? portTemp : portDefault;
    }
    
    /**
     * Prepare connection using renametings and default server.
     * 
     * @return 
     */
    public final boolean prepareConnection() {
        return prepareConnection(getServer(), getPorts());
    }
    
    public boolean prepareConnection(boolean rejoinOpenChannels) {
        if (rejoinOpenChannels) {
            return prepareConnection(getServer(), getPorts(), null);
        } else {
            return prepareConnection();
        }
    }
    
    public final boolean prepareConnectionWithChannel(String channel) {
        return prepareConnection(getServer(), getPorts(), channel);
    }
    
    /**
     * Prepares the connection while getting everything from the renametings,
 except the server.
     * 
     * @param server
     * @return 
     */
    public boolean prepareConnection(String server) {
        return prepareConnection(server, getPorts());
    }
    
    public boolean prepareConnection(String server, String ports) {
        return prepareConnection(server, ports, settings.getString("channel"));
    }
    
    /**
     * Prepares the connection while getting everything from the renametings,
 except the server/port.
     * 
     * @param server
     * @param ports
     * @return 
     */
    public boolean prepareConnection(String server, String ports, String channel) {
        String username = settings.getString("username");
        String password = settings.getString("password");
        boolean usePassword = settings.getBoolean("usePassword");
        String token = settings.getString("token");
        
        String login = "oauth:"+token;
        if (token.isEmpty()) {
            login = "";
        }
        
        if (usePassword) {
            login = password;
            LOGGER.info("Using password instead of token.");
        }
        
        return prepareConnection(username,login,channel,server, ports);
    }
    
    /**
     * Prepare connection using given credentials and channel, but use default
     * server.
     * 
     * @param name
     * @param password
     * @param channel
     * @return 
     */
//    public boolean prepareConnection(String name, String password, String channel) {
//        return prepareConnection(name, password, channel, getServer(), getPorts());
//    }
    
    /**
     * Prepares the connection to the given channel with the given credentials.
     * 
     * This does stuff that should only be done once, unless the given parameters
     * change. So this shouldn't be repeated for just reconnecting.
     * 
     * @param name The username to use for connecting.
     * @param password The password to connect with.
     * @param channel The channel(s) to join after connecting.
     * @param server The server to connect to.
     * @param ports The port to connect to.
     */
    public boolean prepareConnection(String name, String password,
            String channel, String server, String ports) {
        
        if (getState() > Irc.STATE_OFFLINE) {
            g.showMessage("Cannot connect: Already connected.");
            return false;
        }
        
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            g.showMessage("Cannot connect: Incomplete login data.");
            return false;
        }
        
        if (channel == null) {
            autojoin = new String[openChannels.size()];
            openChannels.toArray(autojoin);
        } else {
            autojoin = Helper.parseChannels(channel);
        }
        if (autojoin.length == 0) {
            g.showMessage("A channel to join has to be specified.");
            return false;
        }
        
        if (server == null || server.isEmpty()) {
            g.showMessage("Invalid server specified.");
            return false;
        }
        this.username = name;
        this.password = password;
        this.serverPorts = ports;
        //this.servers.add(0, autojoin[0].substring(1) + serverSuffix);
        this.servers.add(0, server);
        
        serverCycle = 0;
        reconnectionAttempts = 0; 
        
        closeChannels(autojoin);
        
        settings.setString("username", name);
        if (channel != null) {
            settings.setString("channel", channel);
        }
        
        connect();
        return true;
    }
    
    private void addServer(String server) {
        this.servers.add(server);
    }
    
    /**
     * Actually performs the reconnect.
     * 
     * @param reason 
     */
    protected void reconnect(int reason) {
        if (reconnectionTimer != null) {
            reconnectionTimer.cancel();
            reconnectionTimer = null;
        }
        if (reason == Irc.ERROR_UNKNOWN_HOST) {
            serverCycle++;
        }
        reconnectionAttempts++;
        
        g.printLineAll("Attempting to reconnect.. ("+reconnectionAttempts+"/"+maxReconnectionAttempts+")");
        connect();
    }
    
    /**
     * This actually connects to the server. All data necessary for connecting
     * should already be present at this point, however it still checks again if
     * it exists.
     */
    public void connect() {
        String server;
        if (servers.isEmpty()) {
            g.printLine("Cannot connect, no server specified.");
            return;
        }
        
        
        if (serverCycle >= servers.size()) {
            serverCycle = 0;
        }
        server = servers.get(serverCycle);
        connect(server,serverPorts,username,password);
        
    }
    
    /**
     * Disconnect from the server or cancel trying to reconnect.
     * 
     * @return true if the disconnect did something, or false if not actually
     * connected
     */
    @Override
    public boolean disconnect() {
        
        if (reconnectionTimer != null) {
            reconnectionTimer.cancel();
            reconnectionTimer = null;
            g.printLine("Canceled reconnecting");
            setState(Irc.STATE_OFFLINE);
        }
        return super.disconnect();
    }

    @Override
    public void raw(String text) {
        if (settings.getBoolean("debugLogIrc")) {
            g.printDebugIrc("<< "+text);
        }
    }
    
    @Override
    public void sent(String text) {
        if (settings.getBoolean("debugLogIrc")) {
            if (text.startsWith("PASS")) {
                g.printDebugIrc(">> PASS <password>");
            } else {
                g.printDebugIrc(">> " + text);
            }
        }
    }
    
    
    /**
     * Gives the Gui the changed connection state, so it can change it's
     * elements appropriatly (such as changing the title)
     * 
     * @param state 
     */
    @Override
    protected void setState(int state) {
        super.setState(state);
        g.updateState();
        
    }
    
    /**
     * Send a spam protected command to a channel, with the given echo message
     * that will be displayed to the user.
     * 
     * @param channel The channel to send the message to
     * @param message The message to send (e.g. a moderation command)
     * @param echo The message to display to the user
     */
    public void sendCommandMessage(String channel, String message, String echo) {
        if (sendSpamProtectedMessage(channel, message)) {
            g.printLine(channel, echo);
        } else {
            g.printLine("# Command not sent to prevent ban: "+message);
        }
    }
    
    /**
     * Tries to send a spam protected message, which will either be send or not,
     * depending on the status of the spam protection.
     * 
     * @param channel The channel to send the message to
     * @param message The message to send
     * @return true if the message was send, false otherwise
     */
    public boolean sendSpamProtectedMessage(String channel, String message) {
        if (!spamProtection.check()) {
            //g.printLine("# Message/command not send to prevent you from being banned.");
            return false;
        } else {
            spamProtection.increase();
            // This is used to associate a received modlist with a request and
            // thus with a channel (if there is no channel context due to TC1)
            if (message.equals(".mods")) {
                users.modsListRequested(channel);
            }
            super.sendMessage(channel, message);
            return true;
        }
    }
    
    /**
     * Sends a spam protected action message, which is either send with the
     * normal output or not send with a warning (if the spam protection doesn't
     * allow it).
     * 
     * @param channel The channel to send the message to
     * @param message The message
     */
    @Override
    public void sendActionMessage(String channel, String message) {
        if (onChannel(channel, true)) {
            if (sendSpamProtectedActionMessage(channel, message)) {
                g.printMessage(channel, userJoined(channel, username), message, true);
            } else {
                g.printLine("# Action Message not sent to prevent ban: " + message);
            }
        }
    }
    
    /**
     * Sends a spam protected action message, which may or may not be send,
     * depending on the status of the spam protection.
     * 
     * @param channel The channel to send the message to
     * @param message The message
     * @return true if the message was send, false otherwise
     */
    public boolean sendSpamProtectedActionMessage(String channel, String message) {
        if (!spamProtection.check()) {
            return false;
        } else {
            spamProtection.increase();
            super.sendActionMessage(channel, message);
            return true;
        }
    }
    
    /**
     * Joins the channel with the given name, but only if the channel name
     * is deemed valid, it's possible to join channels at this point and we are
     * not already on the channel.
     * 
     * @param channel The name of the channel, with or without leading '#'.
     */
    @Override
    public void joinChannel(String channel) {
        Set<String> channels = new HashSet<>();
        channels.add(channel);
        joinChannels(channels);
    }
    
    /**
     * Join a rename of channels. Sorts out invalid channels and outputs an error
     * message, then joins the valid channels.
     *
     * @param channels Set of channelnames (valid/invalid, leading # or not).
     */
    public void joinChannels(Set<String> channels) {
        Set<String> valid = new HashSet<>();
        Set<String> invalid = new HashSet<>();
        for (String channel : channels) {
            String checkedChannel = Helper.checkChannel(channel);
            if (checkedChannel == null) {
                invalid.add(channel);
            } else {
                valid.add(checkedChannel);
            }
        }
        for (String channel : invalid) {
            g.printLine("Can't join '"+channel+"' (invalid channelname)");
        }
        joinValidChannels(valid);
    }
    
    /**
     * Joins the valid channels. If offline, opens the connect dialog with the
     * valid channels already entered.
     * 
     * @param valid A Set of valid channels (valid names, with leading #).
     */
    private void joinValidChannels(Set<String> valid) {
        if (valid.isEmpty()) {
            return;
        } else if (!isRegistered()) {
            String validChannels = Helper.buildStreamsString(valid);
            if (isOffline()) {
                g.openConnectDialog(validChannels);
            }
            g.printLine("Can't join '" + validChannels + "' (not connected)");
        } else {
            for (String channel : valid) {
                if (onChannel(channel)) {
                    if (valid.size() == 1) {
                        g.switchToChannel(channel);
                    } else {
                        g.printLine("Can't join '" + channel + "' (already joined)");
                    }
                } else {
                    super.joinChannel(channel);
                }
            }
        }
    }

    public void clearChannel(String channel) {
        twitchCommands.clearChannel(channel);
    }

    public void mod(String channel, String name) {
        twitchCommands.mod(channel, name);
    }
    
    public void unmod(String channel, String name) {
        twitchCommands.unmod(channel, name);
    }
    
    public void timeout(String channel, String name, int time) {
        twitchCommands.timeout(channel, name, time);
    }
    
    public void ban(String channel, String name) {
        twitchCommands.ban(channel, name);
    }
    
    public void unban(String channel, String name) {
        twitchCommands.unban(channel, name);
    }
    

    
    /**
     * Anything entered in a channel input box is reacted to here.
     * 
     * @param channel
     * @param text 
     */
    public void textInput(String channel, String text) {
        if (text.isEmpty()) {
            return;
        }
        if (text.startsWith("/")) {
            commandInput(channel, text);
        }
        else {
            if (onChannel(channel)) {
                if (sendSpamProtectedMessage(channel, text)) {
                    g.printMessage(channel,userJoined(channel,username),text,false);
                } else {
                    g.printLine("# Message not sent to prevent ban: "+text);
                }
            }
            else {
                g.printLine("Not in a channel");
                // For testing:
                // (Also creates a channel with an empty string)
                if (Chatty.DEBUG) {
                    g.printMessage(channel,testUser,text,false);
                }
            }
        }     
    }
    
    /**
     * Checks if actually joined to the given channel.
     * 
     * @param channel
     * @return 
     */
    public boolean onChannel(String channel) {
        return onChannel(channel, false);
    }
    
    /**
     * Checks if actually joined to the given channel and also, if not,
     * optionally outputs a message to inform the user about it.
     * 
     * @param channel
     * @param showMessage
     * @return 
     */
    public boolean onChannel(String channel, boolean showMessage) {
        boolean onChannel = joinedChannels.contains(channel);
        if (showMessage && !onChannel) {
            if (channel == null || channel.isEmpty()) {
                g.printLine("Not in a channel");
            } else {
                g.printLine("Not in this channel ("+channel+")");
            }
        }
        return onChannel;
    }
    
    /**
     * Checks if the given channel should be open.
     * 
     * @param channel The channel name
     * @return 
     */
    public boolean isChannelOpen(String channel) {
        return openChannels.contains(channel);
    }
    
    /**
     * Text has to start with a /.
     * 
     * @param channel
     * @param text
     * @return 
     */
    public boolean commandInput(String channel, String text) {
        String[] split = text.trim().split(" ", 2);
        String command = split[0].substring(1);
        String parameter = null;
        if (split.length == 2) {
            parameter = split[1];
        }
        return command(channel, command, parameter);
    }
    
    /**
     * Reacts on all of the commands entered in the channel input box.
     * 
     * @param channel
     * @param command
     * @param parameter
     * @return 
     */
    public boolean command(String channel, String command, String parameter) {
        command = Helper.toLowerCase(command);
        
        // IRC Commands
        
        if (command.equals("quit")) {
            quit();
        }
        else if (command.equals("server")) {
            commandServer(parameter);
        }
        else if (command.equals("connection")) {
            g.printLine(getConnectionInfo());
        }
        else if (command.equals("join")) {
            commandJoinChannel(parameter);
        }
        else if (command.equals("part") || command.equals("close")) {
            commandPartChannel(channel);
        }
        else if (command.equals("raw")) {
            if (parameter != null) {
                send(parameter);
            }
        }
        else if (command.equals("me")) {
            commandActionMessage(channel,parameter);
        }
//        else if (command.equals("slap")) {
//            commandSlap(channel, parameter);
//        }
        
        else if (command.equals("to") || command.equals("timeout")) {
            twitchCommands.commandTimeout(channel,parameter);
        }
        else if (command.equals("unban")) {
            twitchCommands.commandUnban(channel, parameter);
        }
        else if (command.equals("ban")) {
            twitchCommands.commandBan(channel, parameter);
        }
        else if (command.equals("slow")) {
            twitchCommands.commandSlowmodeOn(channel, parameter);
        }
        else if (command.equals("slowoff")) {
            twitchCommands.slowmodeOff(channel);
        }
        else if (command.equals("subscribers")) {
            twitchCommands.subscribersOn(channel);
        }
        else if (command.equals("subscribersoff")) {
            twitchCommands.subscribersOff(channel);
        }
        else if (command.equals("mod")) {
            twitchCommands.commandMod(channel, parameter);
        }
        else if (command.equals("unmod")) {
            twitchCommands.commandUnmod(channel, parameter);
        }
        else if (command.equals("clear")) {
            twitchCommands.clearChannel(channel);
        }
        else if (command.equals("mods")) {
            twitchCommands.mods(channel);
        }
        else if (command.equals("fixmods")) {
            twitchCommands.modsSilent(channel);
        }
        else if (command.equals("host")) {
            twitchCommands.commandHostmode(channel, parameter);
        }
        else if (command.equals("unhost")) {
            twitchCommands.hostmodeOff(channel);
        }
        else if (command.equals("clearchat")) {
            g.clearChat();
        }
        else if (command.equals("resortuserlist")) {
            g.resortUsers(channel);
        }
        
        else if (command.equals("testnotification")) {
            g.showTestNotification(parameter);
        }
        
        // Server/Connecting
        else if (command.equals("tempserver")) {
            if (parameter != null) {
                addServer(parameter);
            }
        }
        else if (command.equals("nextserver")) {
            serverCycle++;
        }
        
        // Misc
        else if (command.equals("dir")) {
            g.printSystem("Settings directory: '"+Chatty.getUserDataDirectory()+"'");
        }
        else if (command.equals("wdir")) {
            g.printSystem("Working directory: '"+Chatty.getWorkingDirectory()+"'");
        }
        else if (command.equals("opendir")) {
            Helper.openFolder(new File(Chatty.getUserDataDirectory()), g);
        }
        else if (command.equals("openwdir")) {
            Helper.openFolder(new File(Chatty.getWorkingDirectory()), g);
        }
        
        // Settings
        else if (command.equals("set")) {
            g.printSystem(settings.setTextual(parameter));
        }
        else if (command.equals("get")) {
            g.printSystem(settings.getTextual(parameter));
        }
        else if (command.equals("clearsetting")) {
            g.printSystem(settings.clearTextual(parameter));
        }
        else if (command.equals("reset")) {
            g.printSystem(settings.resetTextual(parameter));
        }
        
        else if (command.equals("setcolor")) {
            if (parameter != null) {
                g.setColor(parameter);
            }
        }
        
//        else if (command.equals("userlist")) {
//            g.printSystem(addressbook.getEntries().toString());
//        }
        
        else if (command.equals("users") || command.equals("ab")) {
            g.printSystem("[Addressbook] "
                    +addressbook.command(parameter != null ? parameter : ""));
        }
        
        else if (command.equals("changetoken")) {
            g.changeToken(parameter);
        }
        
        else if (command.equals("reconnect")) {
            commandReconnect();
        }
        
        else if (command.equals("refresh")) {
            commandRefresh(channel, parameter);
        }
        
        else if (command.equals("refreshcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandRefreshCase(parameter));
            }
        }
        else if (command.equals("setcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandSetCase(parameter));
            }
        }
        else if (command.equals("getcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandGetCase(parameter));
            }
        }
        
        else if (command.equals("myemotes")) {
            commandMyEmotes();
        }
        else if (command.equals("emoteinfo")) {
            g.printSystem(g.emoticons.getEmoteInfo(parameter));
        }
        else if (command.equals("ffz")) {
            commandFFZ(channel);
        }
        
        else if (command.equals("releaseinfo")) {
            g.openReleaseInfo(true);
        }
        
        else if (command.equals("echo")) {
            g.printLine(parameter);
        }
        
        else if (command.equals("uptime")) {
            g.printSystem("Chatty has been running for "+Chatty.uptime());
        }
        
        else if (g.commandGui(command, parameter)) {
            // Already done if true :P
        }

        // Has to be tested last, so regular commands with the same name take
        // precedence
        else if (customCommands.containsCommand(command)) {
            customCommand(channel, command, parameter);
        }
        
        //--------------------
        // Only for testing
        else if (Chatty.DEBUG || settings.getBoolean("debugCommands")) {
            testCommands(channel, command, parameter);
        }
        //----------------------
        
        else {
            g.printLine("Unknown command: "+command+" (Remember you can also "
                    + "enter Twitch Chat Commands with a point in front: \".mods\")");
            return false;
        }
        return true;
    }
    
    private void testCommands(String channel, String command, String parameter) {
        if (command.equals("add")) {
            String[] splitSpace = parameter.split(" ");
            int type = Channel.TYPE_CHANNEL;
            if (splitSpace.length == 2) {
                type = Integer.parseInt(splitSpace[1]);
            }
            String[] split2 = splitSpace[0].split(",");
            for (String chan : split2) {
                g.addChannel(chan, type);
            }
        } else if (command.equals("settestuser")) {
            String[] split = parameter.split(" ");
            createTestUser(split[0], split[1]);
        } else if (command.equals("setemoteset")) {
            testUser.setEmoteSets(parameter);
        } else if (command.equals("setemoteset2")) {
            specialUser.setEmoteSets(parameter);
        } else if (command.equals("getemoteset")) {
            g.printLine(g.emoticons.getEmoticons(Integer.parseInt(parameter)).toString());
        } else if (command.equals("loadchaticons")) {
            api.requestChatIcons(parameter, true);
        } else if (command.equals("testcolor")) {
            testUser.setColor(parameter);
        } else if (command.equals("testupdatenotification")) {
            g.setUpdateAvailable("[test]");
        } else if (command.equals("remove")) {
            g.removeChannel(parameter);
        } else if (command.equals("testtimer")) {
            new Thread(new TestTimer(this, new Integer(parameter))).start();
        } //        else if (command.equals("usertest")) {
        //            System.out.println(users.getChannelsAndUsersByUserName(parameter));
        //        }
        //        else if (command.equals("insert2")) {
        //            g.printLine("\u0E07");
        //        }
        else if (command.equals("bantest")) {
            g.userBanned("", testUser);
        } else if (command.equals("bantest2")) {
            g.userBanned(channel, users.getUser(channel, parameter));
        } else if (command.equals("channelswarning")) {
            g.channelsWarning();
        } else if (command.equals("userjoined")) {
            userJoined("#test", parameter);
        } else if (command.equals("echo")) {
            String[] parts = parameter.split(" ");
            g.printMessage(parts[0], testUser, parts[1], false);
        } else if (command.equals("loadffz")) {
            frankerFaceZ.requestEmotes(parameter, true);
        } else if (command.equals("testtw")) {
            g.showTokenWarning();
        } else if (command.equals("tsonline")) {
            testStreamInfo.set(parameter, "Game", 123, -1);
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("tsoffline")) {
            testStreamInfo.setOffline();
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("testspam")) {
            g.printLine("test" + spamProtection.getAllowance() + spamProtection.tryMessage());
        } else if (command.equals("tsv")) {
            testStreamInfo.set("Title", "Game", Integer.parseInt(parameter), -1);
        } else if (command.equals("tsvs")) {
            System.out.println(testStreamInfo.getViewerStats(true));
        } else if (command.equals("tsaoff")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.setOffline();
        } else if (command.equals("tsaon")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.set("Test", "Game", 12, System.currentTimeMillis() - 1000);
        } else if (command.equals("usericonsInfo")) {
            usericonManager.debug();
        } else if (command.equals("userlistTest")) {
            g.printMessage("test1", testUser, "short message", false);
            g.printMessage("test2", testUser, "short message2", false);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false);
            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwoeifjwefiowejfef wefjoiwefj", false);
            g.printMessage("test3", testUser, "longer wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false);
            g.printCompact("test4", "MOD", testUser);
            g.printCompact("test5", "MOD", testUser);
            g.printCompact("test6", "MOD", testUser);
            g.printCompact("test7", "MOD", testUser);
            g.printCompact("test8", "MOD", testUser);
            g.printCompact("test9", "MOD", testUser);
            g.printMessage("test10", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false);
        } else if (command.equals("requestfollowers")) {
            api.getFollowers(parameter);
        }
    }
    
    public void customCommand(String channel, String command, String parameter) {
        if (!customCommands.containsCommand(command)) {
            g.printLine("Custom command not found: "+command);
            return;
        }
        String result = customCommands.command(command, parameter);
        if (result == null) {
            g.printLine("Custom command '"+command+"': Invalid parameters");
        } else if (result.isEmpty()) {
            // This shouldn't actually happen if edited through the settings,
            // which should trim() out whitespace, so that the command won't
            // have a result if it's empty and thus won't be added as a command.
            // Although it can also happen if the command just contains a \
            // (which is interpreted as an escape character).
            g.printLine("Custom command '"+command+"': No action specified");
        } else {
            // Check what command is called in the result of this command
            String[] resultSplit = result.trim().split(" ", 2);
            String resultCommand = resultSplit[0];
            if (resultCommand.startsWith("/")
                    && customCommands.containsCommand(resultCommand.substring(1))) {
                g.printLine("Custom command '"+command+"': Calling another custom "
                        + "command ('"+resultCommand.substring(1)+"') is not allowed");
            } else {
                textInput(channel, result);
            }
        }
    }
    
    private void commandServer(String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /server <address>[:port]");
            return;
        }
        String[] split = parameter.split(":");
        if (split.length == 1) {
            prepareConnection(split[0]);
        } else if (split.length == 2) {
            prepareConnection(split[0], split[1]);
        } else {
            g.printLine("Invalid format. Usage: /server <address>[:port]");
        }
    }
    
    /**
     * Command to join channel entered.
     * 
     * @param channel 
     */
    public void commandJoinChannel(String channel) {
        if (channel == null) {
            g.printLine("A channel to join needs to be specified.");
        } else {
            channel = Helper.toLowerCase(channel.trim());
            joinChannel(channel);
        }
    }
    
    /**
     * Command to part channel entered.
     * 
     * @param channel 
     */
    private void commandPartChannel(String channel) {
        if (channel == null || channel.isEmpty()) {
            g.printLine("No channel to leave.");
        } else {
            closeChannel(channel);
        }
    }

    /**
     * React to action message (/me) command and send message if on channel.
     * 
     * @param channel The channel to send the message to
     * @param message The message to send
     */
    private void commandActionMessage(String channel, String message) {
        if (message != null) {
            sendActionMessage(channel, message);
        } else {
            g.printLine("Usage: /me <message>");
        }
    }
    
    public void commandReconnect() {
        if (disconnect()) {
            reconnect(Irc.REQUESTED_RECONNECT);
        } else {
            g.printLine("Could not reconnect.");
        }
    }
    
    private void commandRefresh(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /refresh <type> (see help)");
        } else if (parameter.equals("emoticons")) {
            g.printLine("Refreshing emoticons.. (this can take a few seconds)");
            refreshRequests.add("emoticons");
            api.requestEmoticons(true);
        } else if (parameter.equals("badges")) {
            if (channel == null || channel.isEmpty()) {
                g.printLine("Must be on a channel to use this.");
            } else {
                g.printLine("Refreshing badges for " + channel + "..");
                refreshRequests.add("badges");
                api.requestChatIcons(Helper.toStream(channel), true);
            }
        } else if (parameter.equals("ffz")) {
            if (channel == null || channel.isEmpty()) {
                g.printLine("Must be on a channel to use this.");
            } else {
                g.printLine("Refreshing FFZ emotes for "+channel+"..");
                refreshRequests.add("ffz");
                frankerFaceZ.requestEmotes(channel, true);
            }
        } else if (parameter.equals("ffzglobal")) {
            g.printLine("Refreshing global FFZ emotes..");
            refreshRequests.add("ffzglobal");
            frankerFaceZ.refreshGlobalEmotes();
        } else if (parameter.equals("bttvemotes")) {
            g.printLine("Refreshing BTTV emotes..");
            refreshRequests.add("bttvemotes");
            bttvEmotes.requestEmotes(true);
        }
    }
    

    
    /**
     * Outputs the emotesets for the local user. This might not work correctly
     * if the user is changed or the emotesets change during the session.
     */
    private void commandMyEmotes() {
        Set<Integer> emotesets = specialUser.getEmoteSet();
        if (emotesets.isEmpty()) {
            g.printLine("No subscriber emotes found. (Only works if you joined"
                    + " any channel before.)");
        } else {
            StringBuilder b = new StringBuilder("Your subemotes: ");
            String sep = "";
            for (Integer emoteset : emotesets) {
                b.append(sep);
                if (emoteset == 33 || emoteset == 42 || emoteset == 457) {
                    b.append("Turbo emotes");
                } else {
                    String sep2 = "";
                    for (Emoticon emote : g.emoticons.getEmoticons(emoteset)) {
                        b.append(sep2);
                        b.append(emote.code);
                        sep2 = ", ";
                    }
                }
                sep = " / ";
            }
            g.printLine(b.toString());
        }
    }
    
    private void commandFFZ(String channel) {
        Set<Emoticon> emotes = g.emoticons.getEmoticons(Helper.toStream(channel));
        Set<Emoticon> filtered = Emoticons.filterByType(emotes, Emoticon.Type.FFZ);
        StringBuilder b = new StringBuilder("This channel's FFZ emotes: ");
        if (filtered.isEmpty()) {
            b.append("None found.");
        }
        String sep = "";
        for (Emoticon emote : filtered) {
            b.append(sep);
            b.append(emote.code);
            sep = ", ";
        }
        g.printLine(channel, b.toString());
    }

    /**
     * Add a debugmessage to the GUI. If the GUI wasn't created yet, add it
     * to a cache that is send to the GUI once it is created. This is done
     * automatically when a debugmessage is added after the GUI was created.
     * 
     * @param line 
     */
    @Override
    public void debug(String line) {
        if (shuttingDown) {
            return;
        }
        if (g == null) {
            cachedDebugMessages.add(line);
        } else {
            if (cachedDebugMessages != null) {
                g.printDebug("[Start of cached messages]");
                for (String cachedLine : cachedDebugMessages) {
                    g.printDebug(cachedLine);
                }
                g.printDebug("[End of cached messages]");
                // No longer used
                cachedDebugMessages = null;
            }
            g.printDebug(line);
        }
    }
    
    /**
     * Output a warning.
     * 
     * @param line 
     */
    public final void warning(String line) {
        if (shuttingDown) {
            return;
        }
        if (g == null) {
            cachedWarningMessages.add(line);
        } else {
            if (cachedWarningMessages != null) {
                for (String cachedLine : cachedWarningMessages) {
                    g.printLine(cachedLine);
                }
                cachedWarningMessages = null;
            }
            if (line != null) {
                g.printLine(line);
            }
        }
    }
    
    
    /**
     * Redirects request results from the API.
     */
    private class TwitchApiResults implements TwitchApiResultListener {
        
        @Override
        public void receivedEmoticons(Set<Emoticon> emoticons) {
            g.addEmoticons(emoticons);
            if (refreshRequests.contains("emoticons")) {
                g.printLine("Emoticons list updated.");
                refreshRequests.remove("emoticons");
            }
        }
        
        @Override
        public void tokenVerified(String token, TokenInfo tokenInfo) {
            g.tokenVerified(token, tokenInfo);
        }
        
        @Override
        public void runCommercialResult(String stream, String text, int result) {
            commercialResult(stream, text, result);
        }
 
        @Override
        public void gameSearchResult(Set<String> games) {
            g.gameSearchResult(games);
        }
        
        @Override
        public void receivedChannelInfo(String channel, ChannelInfo info, int result) {
            g.setChannelInfo(channel, info, result);
        }
    
        @Override
        public void putChannelInfoResult(int result) {
            g.putChannelInfoResult(result);
        }

        @Override
        public void accessDenied() {
            checkToken();
        }

        @Override
        public void receivedUsericons(List<Usericon> icons) {
            usericonManager.addDefaultIcons(icons);
            if (refreshRequests.contains("badges")) {
                g.printLine("Badges updated.");
                refreshRequests.remove("badges");
            }
        }

        @Override
        public void receivedFollowers(FollowerInfo followerInfo) {
            g.setFollowerInfo(followerInfo);
            followerInfoNames(followerInfo);
            
        }

        @Override
        public void newFollowers(FollowerInfo followerInfo) {
            g.followerSound(Helper.checkChannel(followerInfo.stream));
        }

        @Override
        public void receivedSubscribers(FollowerInfo info) {
            g.setSubscriberInfo(info);
            followerInfoNames(info);
        }
        
        private void followerInfoNames(FollowerInfo info) {
            if (capitalizedNames != null && info.followers != null) {
                for (Follower f : info.followers) {
                    capitalizedNames.setName(Helper.toLowerCase(f.name), f.name);
                }
            }
        }

        @Override
        public void receivedDisplayName(String name, String displayName) {
            capitalizedNames.setName(name, displayName);
        }
    }
    
    private void checkToken() {
        api.checkToken(settings.getString("token"));
    }
    
    // Webserver
    
    public void startWebserver() {
        if (webserver == null) {
            webserver = new Webserver(this);
            new Thread(webserver).start();
        }
        else {
            LOGGER.warning("Webserver already running");
            // When webserver is already running, it should be started
            g.webserverStarted();
        }
    }
    
    public void stopWebserver() {
        if (webserver != null) {
            webserver.stop();
        }
        else {
            LOGGER.info("No webserver running, can't stop it");
        }
    }
    
    public void webserverStarted() {
        g.webserverStarted();
    }
    
    public void webserverError(String error) {
        g.webserverError(error);
        webserver = null;
    }
    
    public void webserverTokenReceived(String token) {
        g.webserverTokenReceived(token);
    }
    
    public void webserverStopped() {
        webserver = null;
    }

    private class MyStreamInfoListener implements StreamInfoListener {
        
        /**
         * The StreamInfo has been updated with new data from the API.
         * 
         * @param info 
         */
        @Override
        public void streamInfoUpdated(StreamInfo info) {
            g.updateState(true);
            g.updateChannelInfo();
            g.addStreamInfo(info);
            String channel = "#"+info.getStream();
            if (isChannelOpen(channel)) {
                // Log viewerstats if channel is still open and thus a log
                // is being written
                chatLog.viewerstats(channel, info.getViewerStats(false));
                if (info.getOnline() && info.isValid()) {
                    chatLog.viewercount(channel, info.getViewers());
                }
            }
            streamStatusWriter.streamStatus(info);
            if (capitalizedNames != null && info.hasDisplayName()) {
                capitalizedNames.setName(info.stream, info.getDisplayName());
            }
        }

        /**
         * Displays the new stream status. Prints to the channel of the stream,
         * which should usually be open because only current channels get stream
         * data requested, but check if its still open (request response is
         * delayed and it could have been closed in the meantime).
         *
         * @param info
         * @param newStatus
         */
        @Override
        public void streamInfoStatusChanged(StreamInfo info, String newStatus) {
            String channel = "#" + info.getStream();
            if (isChannelOpen(channel)) {
                if (settings.getBoolean("printStreamStatus")) {
                    g.printLine(channel, "~" + newStatus + "~");
                }
                g.setChannelNewStatus(channel, newStatus);
            }
            g.statusNotification(channel, newStatus);
        }
    }
    
    /**
     * Log viewerstats for any open channels, which can be used to log any
     * remaining data on all channels when the program is closed.
     */
    private void logAllViewerstats() {
        for (String channel : openChannels) {
            logViewerstats(channel);
        }
    }
    
    /**
     * Gets the viewerstats for the given channel and logs them. This can be
     * used to log any remaining data when a channel is closed or the program is
     * exited.
     *
     * @param channel
     */
    private void logViewerstats(String channel) {
        if (Helper.validateChannel(channel)) {
            ViewerStats stats = api.getStreamInfo(Helper.toStream(channel), null).getViewerStats(true);
            chatLog.viewerstats(channel, stats);
        }
    }

    /**
     * For testing. This requires Chatty.DEBUG and Chatty.HOTKEY to be enabled.
     * 
     * If enabled, AltGr+T can be used to trigger this method.
    */
    public void testHotkey() {
//        g.printMessage("", testUser, "abc 1", false);
        //Helper.unhandledException();
        g.showTokenWarning();
        //g.showTestNotification();
        //logViewerstats("test");
        //g.showTokenWarning();
    }
    
    /**
     * Try to run a standard 30s commercial on the currently active stream.
     */
    public void runCommercial() {
        g.runCommercial();
//        String stream = g.getActiveStream();
//        runCommercial(stream, 30);
    }
    
    /**
     * Tries to run a commercial on the given stream with the given length.
     * 
     * Outputs a message about it in the appropriate channel.
     * 
     * @param stream The stream to run the commercial in
     * @param length The length of the commercial in seconds
     */
    public void runCommercial(String stream, int length) {
        if (stream == null || stream.isEmpty()) {
            commercialResult(stream, "Can't run commercial, not on a channel.", TwitchApi.FAILED);
        }
        else {
            String channel = "#"+stream;
            if (isChannelOpen(channel)) {
                g.printLine(channel, "Trying to run "+length+"s commercial..");
            } else {
                g.printLine("Trying to run "+length+"s commercial.. ("+stream+")");
            }
            api.runCommercial(stream, settings.getString("token"), length);
        }
    }
    
    /**
     * Work with the result on trying to run a commercial, which mostly is
     * returned by the Twitch API, but may also be immediately called if
     * something is formally wrong (like no or empty stream name specified).
     * 
     * Outputs an info text about the result to the appropriate channel and
     * tells the GUI so a message can be displayed in the admin dialog.
     * 
     * @param stream
     * @param text
     * @param result 
     */
    private void commercialResult(String stream, String text, int result) {
        String channel = "#"+stream;
        if (isChannelOpen(channel)) {
            g.printLine(channel, text);
        } else {
            g.printLine(text+" ("+stream+")");
        }
        g.commercialResult(stream, text, result);
    }
    
    /**
     * Saves a new commercial hotkey to the renametings.
     * 
     * @param hotkey 
     */
    public void setCommercialHotkey(String hotkey) {
        settings.setString("commercialHotkey", hotkey);
        updateCommercialHotkey();
    }
    
    /**
     * Updates the actual hotkey (renames/unrenames) based on the current renametings
 value.
     */
    public final void updateCommercialHotkey() {
        hotkeyManager.setCommercialHotkey(settings.getString("commercialHotkey"));
    }
    
    /**
     * Receive FrankerFaceZ emoticons and icons.
     */
    private class EmoticonsListener implements FrankerFaceZListener {

        @Override
        public void channelEmoticonsReceived(Set<Emoticon> emotes) {
            g.addEmoticons(emotes);
            if (refreshRequests.contains("ffz")) {
                g.printLine("FFZ emotes updated.");
                refreshRequests.remove("ffz");
            }
            if (refreshRequests.contains("ffzglobal")) {
                g.printLine("Global FFZ emotes updated.");
                refreshRequests.remove("ffzglobal");
            }
        }

        @Override
        public void usericonsReceived(List<Usericon> icons) {
            usericonManager.addDefaultIcons(icons);
        }
    }
    
    private class EmoteListener implements EmoticonListener {

        @Override
        public void receivedEmoticons(Set<Emoticon> emoticons) {
            g.addEmoticons(emoticons);
            if (refreshRequests.contains("bttvemotes")) {
                g.printLine("BTTV emotes updated.");
                refreshRequests.remove("bttvemotes");
            }
        }
        
    }
    
    private class TwitchemotesListener implements TwitchEmotesListener {

        @Override
        public void emotesetsReceived(Map<Integer, String> emotesetStreams) {
            g.setEmotesets(emotesetStreams);
            users.setEmotesets(emotesetStreams);
        }
        
    }
    
    public void setLinesPerSeconds(String value) {
        spamProtection.setLinesPerSeconds(value);
    }
    
    private void version5Info() {
        long count = settings.getLong("v0.5");
        if (!settings.getString("token").isEmpty()
                && !settings.getBoolean("token_user")
                && count < 2) {
            g.printSystem("With Version 0.5, Chatty can notify you about streams "
                    + "you follow and show a list of them. You have to request "
                    + "new login data containing <Read user info> access to be "
                    + "able to use that. Go to "
                    + "<Main - Connect - Configure login..>, remove the login "
                    + "and request it again.");
            g.printSystem("You can enable/disable this feature under "
                    + "<Main - Settings - Notifications> if you have the "
                    + "necessary access.");
            settings.setLong("v0.5", ++count);
        }
    }
    
    /**
     * Exit the program. Do some cleanup first and save stuff to file (settings,
     * addressbook, chatlogs).
     * 
     * Should run in EDT.
     */
    public void exit() {
        if (capitalizedNames != null) {
            capitalizedNames.saveToFileOnce();
        }
        addressbook.saveToFileOnce();
        if (g != null && g.guiCreated) {
            g.saveWindowStates();
        }
        logAllViewerstats();
        disconnect();
        shuttingDown = true;
        if (!settings.getBoolean("dontSaveSettings")) {
            settings.saveSettingsToJson();
        }
        g.cleanUp();
        chatLog.close();
        System.exit(0);
    }
    
    private class SettingSaveListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            settings.setString("previousChannel", Helper.buildStreamsString(openChannels));
        }
        
    }
}
