
package chatty.gui;

import chatty.gui.components.UserInfo;
import chatty.gui.components.DebugWindow;
import chatty.gui.components.ChannelInfoDialog;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.help.About;
import chatty.gui.components.HighlightedMessages;
import chatty.gui.components.TokenDialog;
import chatty.gui.components.AdminDialog;
import chatty.gui.components.ConnectionDialog;
import chatty.gui.components.Channel;
import chatty.gui.components.TokenGetDialog;
import chatty.gui.components.HotKeyChooserListener;
import chatty.gui.components.FavoritesDialog;
import chatty.gui.components.ChannelsWarning;
import chatty.gui.components.JoinDialog;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfo;
import chatty.util.api.TokenInfo;
import chatty.util.api.Emoticons;
import chatty.util.api.ChannelInfo;
import java.util.List;
import chatty.Chatty;
import chatty.TwitchClient;
import chatty.Helper;
import chatty.User;
import chatty.Irc;
import chatty.StatusHistory;
import chatty.UsercolorItem;
import chatty.Usericon;
import chatty.gui.components.AddressbookDialog;
import chatty.gui.components.ChannelTextPane;
import chatty.gui.components.ChannelTextPane.MessageType;
import chatty.gui.components.EmotesDialog;
import chatty.gui.components.ErrorMessage;
import chatty.gui.components.FollowersDialog;
import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.LivestreamerDialog;
import chatty.gui.components.srl.SRL;
import chatty.gui.components.SearchDialog;
import chatty.gui.components.UpdateMessage;
import chatty.gui.components.menus.ContextMenuHelper;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.settings.SettingsDialog;
import chatty.gui.notifications.NotificationActionListener;
import chatty.gui.notifications.NotificationManager;
import chatty.util.ActivityTracker;
import chatty.util.Sound;
import chatty.util.api.FollowerInfo;
import chatty.util.settings.Setting;
import chatty.util.settings.SettingChangeListener;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.FontUIResource;

/**
 * The Main Hub for all GUI activity.
 * 
 * @author tduva
 */
public class MainGui extends JFrame implements Runnable { 
    
    public static final Color COLOR_NEW_MESSAGE = new Color(200,0,0);
    public static final Color COLOR_NEW_HIGHLIGHTED_MESSAGE = new Color(255,80,0);
    
    public final Emoticons emoticons = new Emoticons();
    
    // Reference back to the client to give back data etc.
    TwitchClient client = null;
    
    public volatile boolean guiCreated;
    
    // Parts of the GUI
    private Channels channels;
    private ConnectionDialog connectionDialog;
    private TokenDialog tokenDialog;
    private TokenGetDialog tokenGetDialog;
    private DebugWindow debugWindow;
    private UserInfo userInfoDialog;
    private About aboutDialog;
    private ChannelInfoDialog channelInfoDialog;
    private SettingsDialog settingsDialog;
    private AdminDialog adminDialog;
    private FavoritesDialog favoritesDialog;
    private JoinDialog joinDialog;
    private HighlightedMessages highlightedMessages;
    private HighlightedMessages ignoredMessages;
    private MainMenu menu;
    private SearchDialog searchDialog;
    private LiveStreamsDialog liveStreamsDialog;
    private NotificationManager<String> notificationManager;
    private ErrorMessage errorMessage;
    private AddressbookDialog addressbookDialog;
    private SRL srl;
    private LivestreamerDialog livestreamerDialog;
    private UpdateMessage updateMessage;
    private EmotesDialog emotesDialog;
    private FollowersDialog followerDialog;
    private FollowersDialog subscribersDialog;
    
    // Helpers
    private final Highlighter highlighter = new Highlighter();
    private final Highlighter ignoreChecker = new Highlighter();
    private StyleManager styleManager;
    private TrayIconManager trayIcon;
    private final StateUpdater state = new StateUpdater();
    private WindowStateManager windowStateManager;
    private final IgnoredMessages ignoredMessagesHelper = new IgnoredMessages(this);

    // Listeners that need to be returned by methods
    private ActionListener actionListener;
    private final WindowListener windowListener = new MyWindowListener();
    private final UserListener userListener = new MyUserListener();
    private final LinkLabelListener linkLabelListener = new MyLinkLabelListener();
    private final HotkeyUpdateListener hotkeyUpdateListener = new HotkeyUpdateListener();
    private final ContextMenuListener contextMenuListener = new MyContextMenuListener();
    
    // Remember state
    private boolean showedChannelsWarningThisSession = false;
    
    
    public MainGui(TwitchClient client) {
        this.client = client;
        SwingUtilities.invokeLater(this);
    }
    
    @Override
    public void run() {
        createGui();
    }

    
    private Image createImage(String name) {
        return Toolkit.getDefaultToolkit().createImage(getClass().getResource(name));
    }
    
    /**
     * Sets different sizes of the window icon.
     */
    private void setWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_16.png"));
        windowIcons.add(createImage("app_64.png"));
        this.setIconImages(windowIcons);
    }
    
    private void setLiveStreamsWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_live_16.png"));
        windowIcons.add(createImage("app_live_64.png"));
        liveStreamsDialog.setIconImages(windowIcons);
    }
    
    private void setHelpWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_help_16.png"));
        windowIcons.add(createImage("app_help_64.png"));
        aboutDialog.setIconImages(windowIcons);
    }
    
    /**
     * Creates the gui, run in the EDT.
     */
    private void createGui() {

        setWindowIcons();
        
        actionListener = new MyActionListener();
        
        // Error/debug stuff
        debugWindow = new DebugWindow(new DebugCheckboxListener());
        errorMessage = new ErrorMessage(this, linkLabelListener);
        
        // Dialogs and stuff
        connectionDialog = new ConnectionDialog(this);
        GuiUtil.installEscapeCloseOperation(connectionDialog);
        tokenDialog = new TokenDialog(this);
        tokenGetDialog = new TokenGetDialog(this);
        userInfoDialog = new UserInfo(this, contextMenuListener);
        aboutDialog = new About();
        setHelpWindowIcons();
        channelInfoDialog = new ChannelInfoDialog(this);
        channelInfoDialog.addContextMenuListener(contextMenuListener);
        adminDialog = new AdminDialog(this);
        favoritesDialog = new FavoritesDialog(this, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(favoritesDialog);
        joinDialog = new JoinDialog(this);
        GuiUtil.installEscapeCloseOperation(joinDialog);
        //searchDialog = new SearchDialog(this);
        //GuiUtil.installEscapeCloseOperation(searchDialog);
        liveStreamsDialog = new LiveStreamsDialog(contextMenuListener);
        setLiveStreamsWindowIcons();
        //GuiUtil.installEscapeCloseOperation(liveStreamsDialog);
        EmoteContextMenu.setEmoteManager(emoticons);
        emotesDialog = new EmotesDialog(this, emoticons, this, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(emotesDialog);
        followerDialog = new FollowersDialog(FollowersDialog.Type.FOLLOWERS,
                this, client.api, contextMenuListener);
        subscribersDialog = new FollowersDialog(FollowersDialog.Type.SUBSCRIBERS,
                this, client.api, contextMenuListener);
        
        // Tray/Notifications
        trayIcon = new TrayIconManager(createImage("app_16.png"));
        trayIcon.addActionListener(new TrayMenuListener());
        notificationManager = new NotificationManager<>(this);
        notificationManager.setNotificationActionListener(new MyNotificationActionListener());

        // Channels/Chat output
        styleManager = new StyleManager(client.settings);
        highlightedMessages = new HighlightedMessages(this, styleManager,
                "Highlighted Messages","Highlighted", contextMenuListener);
        ignoredMessages = new HighlightedMessages(this, styleManager,
                "Ignored Messages", "Ignored", contextMenuListener);
        channels = new Channels(this,styleManager, contextMenuListener);
        channels.getComponent().setPreferredSize(new Dimension(600,300));
        add(channels.getComponent(), BorderLayout.CENTER);
        channels.setChangeListener(new ChannelChangeListener());
        
        // Some newer stuff
        addressbookDialog = new AddressbookDialog(this, client.addressbook);
        srl = new SRL(this, client.speedrunsLive, contextMenuListener);
        livestreamerDialog = new LivestreamerDialog(this, linkLabelListener, client.settings);
        updateMessage = new UpdateMessage(this);
        
        client.settings.addSettingChangeListener(new MySettingChangeListener());
        client.settings.addSettingsListener(new MySettingsListener());
        
        //this.getContentPane().setBackground(new Color(0,0,0,0));

        getSettingsDialog();
        
        // Main Menu
        MainMenuListener menuListener = new MainMenuListener();
        menu = new MainMenu(menuListener,menuListener);
        setJMenuBar(menu);

        state.update();
        addListeners();
        pack();
        setLocationByPlatform(true);
        
        // Load some stuff
        client.api.requestEmoticons(false);
        client.twitchemotes.requestEmotesets(false);
        if (client.settings.getBoolean("bttvEmotes")) {
            client.bttvEmotes.requestEmotes(false);
        }
        
        // Window states
        windowStateManager = new WindowStateManager(this, client.settings);
        windowStateManager.addWindow(this, "main", true, true);
        windowStateManager.setPrimaryWindow(this);
        windowStateManager.addWindow(highlightedMessages, "highlights", true, true);
        windowStateManager.addWindow(ignoredMessages, "ignoredMessages", true, true);
        windowStateManager.addWindow(channelInfoDialog, "channelInfo", true, true);
        windowStateManager.addWindow(liveStreamsDialog, "liveStreams", true, true);
        windowStateManager.addWindow(adminDialog, "admin", true, true);
        windowStateManager.addWindow(addressbookDialog, "addressbook", true, true);
        windowStateManager.addWindow(emotesDialog, "emotes", true, true);
        windowStateManager.addWindow(followerDialog, "followers", true, true);
        windowStateManager.addWindow(subscribersDialog, "subscribers", true, true);
        
        guiCreated = true;
    }
    
    private SettingsDialog getSettingsDialog() {
        if (settingsDialog == null) {
            settingsDialog = new SettingsDialog(this,client.settings);
        }
        return settingsDialog;
    }
    
    private void addListeners() {
        WindowManager manager = new WindowManager(this);
        manager.addWindowOnTop(liveStreamsDialog);
        
        MainWindowListener mainWindowListener = new MainWindowListener();
        addWindowStateListener(mainWindowListener);
        addWindowListener(mainWindowListener);
        
        String switchTab = "switchTab";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl TAB"), switchTab);
        getRootPane().getActionMap().put(switchTab, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToNextChannel();
            }
        });
        
        String switchTabBack = "switchTabBack";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl shift TAB"), switchTabBack);
        getRootPane().getActionMap().put(switchTabBack, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToPreviousChannel();
            }
        });
        
        String closeTab = "closeTab";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl W"), closeTab);
        getRootPane().getActionMap().put(closeTab, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                client.closeChannel(channels.getActiveTab().getName());
            }
        });
    }
    
    public void showGui() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (!guiCreated) {
                    return;
                }
                setVisible(true);

                // Should be done when the main window is already visible, so
                // it can be centered on it correctly, if that is necessary
                reopenWindows();
                
                // This only seemed to jump to the correct reference (#latest)
                // if done after showing the GUI
                openReleaseInfo(false);
            }
        });
    }
    
    /**
     * Bring the main window into view by bringing it out of minimization (if
     * necessary) and bringing it to the front.
     */
    private void makeVisible() {
        // Set visible was required to show it again after being minimized to tray
        setVisible(true);
        setState(NORMAL);
        toFront();
        //cleanupAfterRestoredFromTray();
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    
    /**
     * Loads settings
     */
    public void loadSettings() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (guiCreated) {
                    loadSettingsInternal();
                }
            }
        });
    }

    /**
     * Initiates the GUI with settings
     */
    private void loadSettingsInternal() {
        setAlwaysOnTop(client.settings.getBoolean("ontop"));
        
        loadMenuSettings();
        updateConnectionDialog(null);
        userInfoDialog.setUserDefinedButtonsDef(client.settings.getString("timeoutButtons"));
        debugWindow.getLogIrcCheckBox().setSelected(client.settings.getBoolean("debugLogIrc"));
        updateLiveStreamsDialog();
        
        windowStateManager.loadWindowStates();
        
        // Set window maximized state
        if (client.settings.getBoolean("maximized")) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        updateHighlight();
        updateIgnore();
        updateHistoryRange();
        updateNotificationSettings();
        updateChannelsSettings();
        updateHighlightNextMessages();
        
        // This should be done before updatePopoutSettings() because that method
        // will delete the attributes correctly depending on the setting
        channels.setPopoutAttributes(client.settings.getList("popoutAttributes"));
        updatePopoutSettings();
        
        loadCommercialDelaySettings();
        UrlOpener.setPrompt(client.settings.getBoolean("urlPrompt"));
        channels.setTabOrder(client.settings.getString("tabOrder"));
        
        favoritesDialog.setSorting((int)client.settings.getLong("favoritesSorting"));
        
        updateCustomContextMenuEntries();
        
        emoticons.setIgnoredEmotes(client.settings.getList("ignoredEmotes"));
        emoticons.loadFavoritesFromSettings(client.settings);
        client.api.setToken(client.settings.getString("token"));
        
        userInfoDialog.setFontSize(client.settings.getLong("dialogFontSize"));
    }
    
    /**
     * Initiates the Main Menu with settings
     */
    private void loadMenuSettings() {
        loadMenuSetting("showJoinsParts");
        loadMenuSetting("ignoreJoinsParts");
        loadMenuSetting("ontop");
    }
    
    /**
     * Initiates a single setting in the Main Menu
     * @param name The name of the setting
     */
    private void loadMenuSetting(String name) {
        menu.setItemState(name,client.settings.getBoolean(name));
    }
    
    /**
     * Tells the highlighter the current list of highlight-items from the settings.
     */
    private void updateHighlight() {
        highlighter.update(Helper.getStringList(client.settings.getList("highlight")));
    }
    
    private void updateIgnore() {
        ignoreChecker.update(Helper.getStringList(client.settings.getList("ignore")));
    }
    
    private void updateCustomContextMenuEntries() {
        ContextMenuHelper.channelCustomCommands = client.settings.getString("channelContextMenu");
        ContextMenuHelper.userCustomCommands = client.settings.getString("userContextMenu");
        ContextMenuHelper.livestreamerQualities = client.settings.getString("livestreamerQualities");
        ContextMenuHelper.enableLivestreamer = client.settings.getBoolean("livestreamer");
    }
    
    private void updateChannelsSettings() {
        channels.setDefaultUserlistWidth((int)client.settings.getLong("userlistWidth"));
        channels.setChatScrollbarAlways(client.settings.getBoolean("chatScrollbarAlways"));
    }
    
    /**
     * Tells the highlighter the current username and whether it should be used
     * for highlight. Used to initialize on connect, when the username is fixed
     * for the duration of the connection.
     * 
     * @param username The current username.
     */
    public void updateHighlightSetUsername(String username) {
        highlighter.setUsername(username);
        highlighter.setHighlightUsername(client.settings.getBoolean("highlightUsername"));
    }
    
    /**
     * Tells the highlighter whether the current username should be used for
     * highlight. Used to set the setting when the setting is changed.
     * 
     * @param highlight 
     */
    private void updateHighlightSetUsernameHighlighted(boolean highlight) {
        highlighter.setHighlightUsername(highlight);
    }
    
    private void updateHighlightNextMessages() {
        highlighter.setHighlightNextMessages(client.settings.getBoolean("highlightNextMessages"));
    }
    
    private void updateNotificationSettings() {
        notificationManager.setDisplayTime((int)client.settings.getLong("nDisplayTime"));
        notificationManager.setMaxDisplayTime((int)client.settings.getLong("nMaxDisplayTime"));
        notificationManager.setMaxDisplayItems((int)client.settings.getLong("nMaxDisplayed"));
        notificationManager.setMaxQueueSize((int)client.settings.getLong("nMaxQueueSize"));
        int activityTime = client.settings.getBoolean("nActivity")
                ? (int)client.settings.getLong("nActivityTime") : -1;
        notificationManager.setActivityTime(activityTime);
        notificationManager.clearAll();
        notificationManager.setScreen((int)client.settings.getLong("nScreen"));
        notificationManager.setPosition((int)client.settings.getLong(("nPosition")));
    }
    
    private void updatePopoutSettings() {
        channels.setSavePopoutAttributes(client.settings.getBoolean("popoutSaveAttributes"));
        channels.setCloseLastChannelPopout(client.settings.getBoolean("popoutCloseLastChannel"));
    }
    
    /**
     * Saves location/size for windows/dialogs and whether it was open.
     */
    public void saveWindowStates() {
        windowStateManager.saveWindowStates();
        client.settings.putList("popoutAttributes", channels.getPopoutAttributes());
    }
    
    /**
     * Reopen some windows if enabled.
     */
    private void reopenWindows() {
        reopenWindow(liveStreamsDialog);
        reopenWindow(highlightedMessages);
        reopenWindow(ignoredMessages);
        reopenWindow(channelInfoDialog);
        reopenWindow(addressbookDialog);
        reopenWindow(adminDialog);
        reopenWindow(emotesDialog);
    }
    
    /**
     * Open the given Component if enabled and if it was open before.
     * 
     * @param window 
     */
    private void reopenWindow(Window window) {
        if (windowStateManager.shouldReopen(window)) {
            if (window == liveStreamsDialog) {
                openLiveStreamsDialog();
            } else if (window == highlightedMessages) {
                openHighlightedMessages();
            } else if (window == ignoredMessages) {
                openIgnoredMessages();
            } else if (window == channelInfoDialog) {
                openChannelInfoDialog();
            } else if (window == addressbookDialog) {
                openAddressbook(null);
            } else if (window == adminDialog) {
                openChannelAdminDialog();
            } else if (window == emotesDialog) {
                openEmotesDialog();
            } else if (window == followerDialog) {
                openFollowerDialog();
            } else if (window == subscribersDialog) {
                openSubscriberDialog();
            }
        }
    }
    
    /**
     * Saves whether the window is currently maximized.
     */
    private void saveState(Component c) {
        if (c == this) {
            client.settings.setBoolean("maximized", isMaximized());
        }
    }
    
    /**
     * Returns if the window is currently maximized.
     * 
     * @return true if the window is maximized, false otherwise
     */
    private boolean isMaximized() {
        return (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
    }
    
    /**
     * Updates the connection dialog with current settings
     */
    private void updateConnectionDialog(String channelPreset) {
        connectionDialog.setUsername(client.settings.getString("username"));
        if (channelPreset != null) {
            connectionDialog.setChannel(channelPreset);
        } else {
            connectionDialog.setChannel(client.settings.getString("channel"));
        }

        String password = client.settings.getString("password");
        String token = client.settings.getString("token");
        boolean usePassword = client.settings.getBoolean("usePassword");
        connectionDialog.update(password, token, usePassword);
        connectionDialog.setAreChannelsOpen(channels.getChannelCount() > 0);
    }
    
    private void updateChannelInfoDialog() {
        String stream = channels.getActiveChannel().getStreamName();
        StreamInfo streamInfo = getStreamInfo(stream);
        channelInfoDialog.set(streamInfo);
    }
    
    private void updateTokenDialog() {
        String username = client.settings.getString("username");
        String token = client.settings.getString("token");
        tokenDialog.update(username, token);
    }
    
    private void updateFavoritesDialog() {
        Set<String> favorites = client.channelFavorites.getFavorites();
        Map<String, Long> history = client.channelFavorites.getHistory();
        favoritesDialog.setData(favorites, history);
    }
    
    private void updateFavoritesDialogWhenVisible() {
        if (favoritesDialog.isVisible()) {
            updateFavoritesDialog();
        }
    }
    
    public void userUpdated(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateUserInfoDialog(user);
            }
        });
    }
    
    private void updateUserInfoDialog(User user) {
        userInfoDialog.update(user, client.getUsername());
    }
    
    private void updateLiveStreamsDialog() {
        liveStreamsDialog.setSorting(client.settings.getString("liveStreamsSorting"));
    }
    
    private void updateHistoryRange() {
        int range = (int)client.settings.getLong("historyRange");
        channelInfoDialog.setHistoryRange(range);
        liveStreamsDialog.setHistoryRange(range);
    }
    
    private void openTokenDialog() {
        updateTokenDialog();
        updateTokenScopes();
        tokenDialog.setLocationRelativeTo(connectionDialog);
        tokenDialog.setVisible(true);
    }
    
    public void addStreamInfo(final StreamInfo streamInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                liveStreamsDialog.addStream(streamInfo);
            }
        });
    }
    
    public ActionListener getActionListener() {
        return actionListener;
    }
    
    public StatusHistory getStatusHistory() {
        return client.statusHistory;
    }
    
    public boolean getSaveStatusHistorySetting() {
        return client.settings.getBoolean("saveStatusHistory");
    }
    
    class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // text input
            Channel chan = channels.getChannelFromInput(event.getSource());
            if (chan != null) {
                client.textInput(chan.getName(), chan.getInputText());
            }

            Object source = event.getSource();
            //---------------------------
            // Connection Dialog actions
            //---------------------------

            if (source == connectionDialog.getCancelButton()) {
                connectionDialog.setVisible(false);
                channels.setInitialFocus();
            } else if (source == connectionDialog.getConnectButton()
                    || source == connectionDialog.getChannelInput()) {
                String password = connectionDialog.getPassword();
                String channel = connectionDialog.getChannel();
                //client.settings.setString("username",name);
                client.settings.setString("password", password);
                client.settings.setString("channel", channel);
                if (client.prepareConnection(connectionDialog.rejoinOpenChannels())) {
                    connectionDialog.setVisible(false);
                    channels.setInitialFocus();
                }
            } else if (event.getSource() == connectionDialog.getGetTokenButton()) {
                openTokenDialog();
            } else if (event.getSource() == connectionDialog.getFavoritesButton()) {
                openFavoritesDialogFromConnectionDialog(connectionDialog.getChannel());
            } //---------------------------
            // Token Dialog actions
            //---------------------------
            else if (event.getSource() == tokenDialog.getDeleteTokenButton()) {
                client.settings.setString("token", "");
                client.settings.setString("username", "");
                resetTokenScopes();
                updateConnectionDialog(null);
                tokenDialog.update("", "");
                updateTokenScopes();
            } else if (event.getSource() == tokenDialog.getRequestTokenButton()) {
                tokenGetDialog.setLocationRelativeTo(tokenDialog);
                tokenGetDialog.reset();
                client.startWebserver();
                tokenGetDialog.setVisible(true);

            } else if (event.getSource() == tokenDialog.getDoneButton()) {
                tokenDialog.setVisible(false);
            } else if (event.getSource() == tokenDialog.getVerifyTokenButton()) {
                verifyToken(client.settings.getString("token"));
            } // Get token Dialog
            else if (event.getSource() == tokenGetDialog.getCloseButton()) {
                tokenGetDialogClosed();
            } //-----------------
            // Userinfo Dialog
            //-----------------
            else if (userInfoDialog.getAction(event.getSource()) != UserInfo.ACTION_NONE) {
                int action = userInfoDialog.getAction(event.getSource());
                User user = userInfoDialog.getUser();
                String nick = user.getNick();
                String channel = userInfoDialog.getChannel();
                if (action == UserInfo.ACTION_BAN) {
                    client.ban(channel, nick);
                } else if (action == UserInfo.ACTION_UNBAN) {
                    client.unban(channel, nick);
                } else if (action == UserInfo.ACTION_TIMEOUT) {
                    int time = userInfoDialog.getTimeoutButtonTime(event.getSource());
                    client.timeout(channel, nick, time);
                } else if (action == UserInfo.ACTION_COMMAND) {
                    String command = userInfoDialog.getCommandButtonCommand(source);
                    client.command(channel, command, nick);
                } else if (action == UserInfo.ACTION_MOD) {
                    client.mod(channel, nick);
                } else if (action == UserInfo.ACTION_UNMOD) {
                    client.unmod(channel, nick);
                }
            // Favorites Dialog
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_ADD_FAVORITES) {
                Set<String> channels = favoritesDialog.getChannels();
                client.channelFavorites.addChannelsToFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE_FAVORITES) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannelsFromFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannels(channels);
            }
        }
        
    }

    private class DebugCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean state = e.getStateChange() == ItemEvent.SELECTED;
            if (e.getSource() == debugWindow.getLogIrcCheckBox()) {
                client.settings.setBoolean("debugLogIrc", state);
            }
        }
    }
    

    private class MyLinkLabelListener implements LinkLabelListener {
        @Override
        public void linkClicked(String type, String ref) {
            if (type.equals("help")) {
                openHelp(ref);
            } else if (type.equals("help-settings")) {
                openHelp("help-settings.html", ref);
            } else if (type.equals("help-admin")) {
                openHelp("help-admin.html", ref);
            } else if (type.equals("help-livestreamer")) {
                openHelp("help-livestreamer.html", ref);
            } else if (type.equals("url")) {
                UrlOpener.openUrlPrompt(MainGui.this, ref);
            } else if (type.equals("update")) {
                if (ref.equals("show")) {
                    openUpdateDialog();
                }
            }
        }
    }
    
    public LinkLabelListener getLinkLabelListener() {
        return linkLabelListener;
    }
    
    public void clearHistory() {
        client.channelFavorites.clearHistory();
    }
    
    private class TrayMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd == null || cmd.equals("show")) {
                makeVisible();
            }
            else if (cmd.equals("exit")) {
                exit();
            }
        }
        
    }
    
    /**
     * Listener for the Main Menu
     */
    private class MainMenuListener implements ItemListener, ActionListener, MenuListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            String setting = menu.getSettingByMenuItem(e.getSource());
            boolean state = e.getStateChange() == ItemEvent.SELECTED;

            if (setting != null) {
                client.settings.setBoolean(setting, state);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("debug")) {
                if (!debugWindow.isShowing()) {
                    debugWindow.setLocationByPlatform(true);
                    debugWindow.setPreferredSize(new Dimension(500, 400));
                }
                debugWindow.setVisible(true);
            } else if (cmd.equals("connect")) {
                openConnectDialogInternal(null);
            } else if (cmd.equals("disconnect")) {
                client.disconnect();
            } else if (cmd.equals("exit")) {
                exit();
            } else if (cmd.equals("about")) {
                openHelp("");
            } else if (cmd.equals("channelInfoDialog")) {
                openChannelInfoDialog();
            } else if (cmd.equals("settings")) {
                getSettingsDialog().showSettings();
            } else if (cmd.equals("website")) {
                UrlOpener.openUrlPrompt(MainGui.this, Chatty.WEBSITE, true);
            } else if (cmd.equals("channelAdminDialog")) {
                openChannelAdminDialog();
            } else if (cmd.equals("favoritesDialog")) {
                openFavoritesDialogToJoin("");
            } else if (cmd.equals("unhandledException")) {
                String[] array = new String[0];
                String a = array[1];
            } else if (cmd.equals("joinChannel")) {
                openJoinDialog();
            } else if (cmd.equals("highlightedMessages")) {
                openHighlightedMessages();
            } else if (cmd.equals("ignoredMessages")) {
                openIgnoredMessages();
            } else if (cmd.equals("search")) {
                openSearchDialog();
            } else if (cmd.equals("onlineChannels")) {
                openLiveStreamsDialog();
            } else if (cmd.equals("addressbook")) {
                openAddressbook(null);
            } else if (cmd.equals("srlRaces")) {
                openSrlRaces();
            } else if (cmd.equals("srlRaceActive")) {
                srl.searchRaceWithEntrant(channels.getActiveTab().getStreamName());
            } else if (cmd.startsWith("srlRace4")) {
                String stream = cmd.substring(8);
                if (!stream.isEmpty()) {
                    srl.searchRaceWithEntrant(stream);
                }
            } else if (cmd.equals("livestreamer")) {
                livestreamerDialog.open(null, null);
            } else if (cmd.equals("emotes")) {
                toggleEmotesDialog();
            } else if (cmd.equals("followers")) {
                openFollowerDialog();
            } else if (cmd.equals("subscribers")) {
                openSubscriberDialog();
            }
        }

        @Override
        public void menuSelected(MenuEvent e) {
            if (e.getSource() == menu.srlStreams) {
                ArrayList<String> popoutStreams = new ArrayList<>();
                for (Channel channel : channels.getPopoutChannels().keySet()) {
                    popoutStreams.add(channel.getStreamName());
                }
                menu.updateSrlStreams(channels.getActiveTab().getStreamName(), popoutStreams);
            } else if (e.getSource() == menu.view) {
                menu.updateCount(highlightedMessages.getNewCount(),
                        highlightedMessages.getDisplayedCount(),
                        ignoredMessages.getNewCount(),
                        ignoredMessages.getDisplayedCount());
            }
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuCanceled(MenuEvent e) {
        }
    
    }
    
    /**
     * Listener for all kind of context menu events
     */
    class MyContextMenuListener implements ContextMenuListener {

        /**
         * User context menu event.
         * 
         * @param e
         * @param user 
         */
        @Override
        public void userMenuItemClicked(ActionEvent e, User user) {
            String cmd = e.getActionCommand();
            if (cmd.equals("userinfo")) {
                openUserInfoDialog(user);
            }
            else if (cmd.equals("addressbookEdit")) {
                openAddressbook(user.getNick());
            }
            else if (cmd.equals("addressbookRemove")) {
                client.addressbook.remove(user.getNick());
                updateUserInfoDialog(user);
            }
            else if (cmd.startsWith("cat")) {
                if (e.getSource() instanceof JCheckBoxMenuItem) {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    String catName = cmd.substring(3);
                    if (selected) {
                        client.addressbook.add(user.getNick(), catName);
                    } else {
                        client.addressbook.remove(user.getNick(), catName);
                    }
                }
                updateUserInfoDialog(user);
            }
            else if (cmd.startsWith("command")) {
                String command = cmd.substring(7);
                client.command(user.getChannel(), command, user.getNick());
            }
            nameBasedStuff(cmd, user.getNick());
        }
        
        /**
         * Event of an URL context menu.
         * 
         * @param e
         * @param url 
         */
        @Override
        public void urlMenuItemClicked(ActionEvent e, String url) {
            String cmd = e.getActionCommand();
            if (cmd.equals("open")) {
                UrlOpener.openUrlPrompt(MainGui.this, url);
            }
            else if (cmd.equals("copy")) {
                Helper.copyToClipboard(url);
            }
            else if (cmd.equals("join")) {
                client.commandJoinChannel(url);
            }
        }

        /**
         * Context menu event without any channel context, which means it just
         * uses the active one or performs some other action that doesn't
         * immediately require one.
         * 
         * @param e 
         */
        @Override
        public void menuItemClicked(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("channelInfo")) {
                openChannelInfoDialog();
            }
            else if (cmd.equals("channelAdmin")) {
                openChannelAdminDialog();
            }
            else if (cmd.equals("closeChannel")) {
                client.closeChannel(channels.getActiveChannel().getName());
            }
            else if (cmd.equals("popoutChannel")) {
                channels.popoutActiveChannel();
            }
            else if (cmd.startsWith("command")) {
                String command = cmd.substring(7);
                client.command(channels.getActiveChannel().getName(), command, null);
            }
            else if (cmd.startsWith("range")) {
                int range = -1;
                switch (cmd) {
                    case "range1h":
                        range = 60;
                        break;
                    case "range2h":
                        range = 120;
                        break;
                    case "range4h":
                        range = 240;
                        break;
                    case "range8h":
                        range = 480;
                        break;
                    case "range12h":
                        range = 720;
                        break;
                }
                // Change here as well, because even if it's the same value,
                // update may be needed. This will make it update twice often.
                updateHistoryRange();
                client.settings.setLong("historyRange", range);
            } else {
                nameBasedStuff(cmd, channels.getActiveChannel().getStreamName());
            }
        }

        /**
         * Context menu event associated with a list of stream or channel names.
         * 
         * @param e
         * @param streams 
         */
        @Override
        public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
            String cmd = e.getActionCommand();
            streamStuff(cmd, streams);
        }

        /**
         * Goes through the {@code StreamInfo} objects and adds the stream names
         * into a list, so it can be used by the more generic method.
         * 
         * @param e The event
         * @param streamInfos The list of {@code StreamInfo} objects associated
         * with this event
         * @see streamsMenuItemClicked(ActionEvent, Collection)
         */
        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
            String cmd = e.getActionCommand();
            String sorting = null;
            if (cmd.equals("sortName")) {
                sorting = "name";
            } else if (cmd.equals("sortGame")) {
                sorting = "game";
            } else if (cmd.equals("sortRecent")) {
                sorting = "recent";
            } else if (cmd.equals("sortViewers")) {
                sorting = "viewers";
            }
            if (sorting != null) {
                client.settings.setString("liveStreamsSorting", sorting);
            } else {
                Collection<String> streams = new ArrayList<>();
                for (StreamInfo info : streamInfos) {
                    streams.add(info.getStream());
                }
                streamsMenuItemClicked(e, streams);
            }
        }
        
        /**
         * Handles context menu events with a single name (stream/channel). Just
         * packs it into a list for use in another method.
         * 
         * @param cmd
         * @param name 
         */
        private void nameBasedStuff(String cmd, String name) {
            Collection<String> list = new ArrayList<>();
            list.add(name);
            streamStuff(cmd, list);
        }
        
        /**
         * Any commands that are equal to these Strings is supposed to have a
         * stream parameter.
         */
        private final Set<String> streamCmds = new HashSet<>(
                Arrays.asList("profile", "join"));
        
        /**
         * Any commands starting with these Strings is supposed to have a stream
         * parameter.
         */
        private final Set<String> streamCmdsPrefix = new HashSet<>(
                Arrays.asList("stream", "livestreamer"));
        
        /**
         * Check if this command requires at least one stream/channel parameter.
         * 
         * @param cmd
         * @return 
         */
        private boolean cmdRequiresStream(String cmd) {
            for (String prefix : streamCmdsPrefix) {
                if (cmd.startsWith(prefix)) {
                    return true;
                }
            }
            return streamCmds.contains(cmd);
        }
        
        /**
         * Handles context menu events that can be applied to one or more
         * streams or channels. Checks if any valid stream parameters are
         * present and outputs an error otherwise. Since this can also be called
         * if it's not one of the commands that actually require a stream (other
         * listeners may be registered), it also checks if it's actually one of
         * the commands it handles.
         * 
         * @param cmd The command
         * @param streams The list of stream or channel names
         */
        private void streamStuff(String cmd, Collection<String> streams) {
            TwitchUrl.removeInvalidStreams(streams);
            if (streams.isEmpty() && cmdRequiresStream(cmd)) {
                JOptionPane.showMessageDialog(getActiveWindow(), "Can't perform action: No stream/channel.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (cmd.equals("stream") || cmd.equals("streamPopout") || cmd.equals("profile")) {
                List<String> urls = new ArrayList<>();
                for (String stream : streams) {
                    String url;
                    switch (cmd) {
                        case "stream":
                            url = TwitchUrl.makeTwitchStreamUrl(stream, false);
                            break;
                        case "profile":
                            url = TwitchUrl.makeTwitchProfileUrl(stream);
                            break;
                        default:
                            url = TwitchUrl.makeTwitchStreamUrl(stream, true);
                            break;
                    }
                    urls.add(url);
                }
                UrlOpener.openUrlsPrompt(getActiveWindow(), urls, true);
            } else if (cmd.equals("join")) {
                Set<String> channels = new HashSet<>();
                for (String stream : streams) {
                    channels.add(stream);
                }
                makeVisible();
                client.joinChannels(channels);
            } else if (cmd.startsWith("streams")) {
                ArrayList<String> streams2 = new ArrayList<>();
                for (String stream : streams) {
                    streams2.add(stream);
                }
                String type = TwitchUrl.MULTITWITCH;
                switch (cmd) {
                    case "streamsSpeedruntv":
                        type = TwitchUrl.SPEEDRUNTV;
                        break;
                    case "streamsKadgar":
                        type = TwitchUrl.KADGAR;
                        break;
                }
                TwitchUrl.openMultitwitch(streams2, getActiveWindow(), type);
            } else if (cmd.startsWith("livestreamer")) {
                // quality null means select
                String quality = null;
                if (cmd.startsWith("livestreamerQ")) {
                    quality = Helper.toLowerCase(cmd.substring(13));
                    if (quality.equalsIgnoreCase("select")) {
                        quality = null;
                    }
                }
                for (String stream : streams) {
                    livestreamerDialog.open(stream, quality);
                }
            }
        }

        @Override
        public void emoteMenuItemClicked(ActionEvent e, Emoticon emote) {
            if (e.getActionCommand().equals("code")) {
                channels.getActiveChannel().insertText(emote.code, true);
            } else if (e.getActionCommand().equals("ffzlink")) {
                UrlOpener.openUrlPrompt(getActiveWindow(), TwitchUrl.makeFFZUrl(), true);
            } else if (e.getActionCommand().equals("twitchturbolink")) {
                UrlOpener.openUrlPrompt(getActiveWindow(), TwitchUrl.makeTwitchTurboUrl(), true);
            } else if (e.getActionCommand().equals("bttvlink")) {
                UrlOpener.openUrlPrompt(getActiveWindow(), TwitchUrl.makeBttvUrl(), true);
            }
            else if (e.getActionCommand().equals("ignoreEmote")) {
                int result = JOptionPane.showConfirmDialog(getActiveWindow(),
                          "<html><body style='width:200px'>Ignoring an emote "
                        + "means showing just the code instead of turning "
                        + "it into an image. The list of ignored emotes can be edited in "
                        + "the Settings under 'Emoticons'.\n\nDo you want to "
                        + "ignore '"+emote.code+"' from now on?",
                        "Ignore Emote", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    emoticons.addIgnoredEmote(emote);
                    client.settings.setAdd("ignoredEmotes", emote.code);
                }
            }
            else if (e.getActionCommand().equals("favoriteEmote")) {
                emoticons.addFavorite(emote);
                client.settings.setAdd("favoriteEmotes", emote.code);
                emotesDialog.favoritesUpdated();
            }
            else if (e.getActionCommand().equals("unfavoriteEmote")) {
                emoticons.removeFavorite(emote);
                client.settings.listRemove("favoriteEmotes", emote.code);
                emotesDialog.favoritesUpdated();
            }
            if (emote.hasStreamSet()) {
                nameBasedStuff(e.getActionCommand(), emote.getStream());
            }
        }
        
        
        
    }

    private class ChannelChangeListener implements ChangeListener {
        
        /**
         * When the focus changes to a different channel (either by changing
         * a tab in the main window or changing focus to a different popout
         * dialog).
         *
         * @param e
         */
        @Override
        public void stateChanged(ChangeEvent e) {
            state.update(true);
            updateChannelInfoDialog();
            emotesDialog.updateStream(channels.getLastActiveChannel().getStreamName());
        }
    }
    
    private class MyUserListener implements UserListener {
        
        @Override
        public void userClicked(User user) {
            openUserInfoDialog(user);
        }
    }
    
    private class MyNotificationActionListener implements NotificationActionListener<String> {

        /**
         * Right-clicked on a notification.
         * 
         * @param data 
         */
        @Override
        public void notificationAction(String data) {
            if (data != null) {
                makeVisible();
                client.joinChannel(data);
            }
        }
    }
    
    public UserListener getUserListener() {
        return userListener;
    }
    
    
    private class HotkeyUpdateListener implements HotKeyChooserListener {
        @Override
        public void hotkeyUpdated(String id, String hotkey) {
            if (id.equals("commercialHotkey")) {
                setCommercialHotkey(hotkey);
            }
        }
    }
    
    public HotKeyChooserListener getHotkeyUpdateListener() {
        return hotkeyUpdateListener;
    }
    
    public java.util.List<UsercolorItem> getUsercolorData() {
        return client.usercolorManager.getData();
    }
    
    public void setUsercolorData(java.util.List<UsercolorItem> data) {
        client.usercolorManager.setData(data);
    }
    
    public java.util.List<Usericon> getUsericonData() {
        return client.usericonManager.getData();
    }
    
    public void setUsericonData(java.util.List<Usericon> data) {
        client.usericonManager.setData(data);
    }
    
    /**
     * Should only be called out of EDT. All commands have to be defined
     * lowercase, because they are made lowercase when entered.
     * 
     * @param command
     * @param parameter
     * @return 
     */
    public boolean commandGui(String command, String parameter) {
        if (command.equals("settings")) {
            getSettingsDialog().showSettings();
        } else if (command.equals("livestreams")) {
            openLiveStreamsDialog();
        } else if (command.equals("channeladmin")) {
            openChannelAdminDialog();
        } else if (command.equals("channelinfo")) {
            openChannelInfoDialog();
        } else if (command.equals("search")) {
            openSearchDialog();
        } else if (command.equals("insert")) {
            insert(parameter, false);
        } else if (command.equals("insertword")) {
            insert(parameter, true);
        } else if (command.equals("openurl")) {
            if (!UrlOpener.openUrl(parameter)) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        } else if (command.equals("openurlprompt")) {
            // Could do in invokeLater() so command isn't visible in input box
            // while the dialog is open, but probably doesn't matter since this
            // is mainly for custom commands put in a context menu anyway.
            if (!UrlOpener.openUrlPrompt(getActiveWindow(), parameter, true)) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        } else if (command.equals("openfollowers")) {
            openFollowerDialog();
        } else if (command.equals("opensubscribers")) {
            openSubscriberDialog();
        } else {
            return false;
        }
        return true;
    }
    
    public void insert(final String text, final boolean spaces) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (text != null) {
                    channels.getLastActiveChannel().insertText(text, spaces);
                }
            }
        });
    }
    
    private void openLiveStreamsDialog() {
        windowStateManager.setWindowPosition(liveStreamsDialog);
        liveStreamsDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        liveStreamsDialog.setState(JFrame.NORMAL);
        liveStreamsDialog.setVisible(true);
    }
    
    private void openUserInfoDialog(User user) {
        userInfoDialog.show(getActiveWindow(), user, client.getUsername());
    }
    
    private void openChannelInfoDialog() {
        windowStateManager.setWindowPosition(channelInfoDialog, getActiveWindow());
        channelInfoDialog.setVisible(true);
    }
    
    private void openChannelAdminDialog() {
        windowStateManager.setWindowPosition(adminDialog, getActiveWindow());
        updateTokenScopes();
        adminDialog.loadCommercialHotkey(client.settings.getString("commercialHotkey"));
        String stream = channels.getActiveChannel().getStreamName();
        if (stream == null) {
            stream = client.settings.getString("username");
        }
        adminDialog.open(stream);
    }
    
    private void openHelp(String ref) {
        openHelp(null, ref);
    }
    
    public void openHelp(String page, String ref) {
        if (!aboutDialog.isVisible()) {
            aboutDialog.setLocationRelativeTo(this);
        }
        aboutDialog.open(page, ref);
        // Set ontop setting, so it won't be hidden behind the main window
        aboutDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        aboutDialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        aboutDialog.toFront();
        aboutDialog.setState(NORMAL);
        aboutDialog.setVisible(true);
    }
    
    public void openReleaseInfo(final boolean forced) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (forced || !client.settings.getString("currentVersion").equals(Chatty.VERSION)) {
                    client.settings.setString("currentVersion", Chatty.VERSION);
                    openHelp("help-releases.html", "latest");
                }
            }
        });
    }
    
    protected void openSearchDialog() {
//        searchDialog.setLocationRelativeTo(getActiveWindow());
//        searchDialog.setVisible(true);
        SearchDialog.showSearchDialog(channels.getActiveChannel(), this, getActiveWindow());
    }
    
    private void openEmotesDialog() {
        windowStateManager.setWindowPosition(emotesDialog, getActiveWindow());
        emotesDialog.showDialog(client.specialUser.getEmoteSet(), channels.getLastActiveChannel().getStreamName());
    }
    
    protected void toggleEmotesDialog() {
        if (emotesDialog.isVisible()) {
            emotesDialog.setVisible(false);
        } else {
            openEmotesDialog();
        }
    }
    
    private void openFollowerDialog() {
        windowStateManager.setWindowPosition(followerDialog);
        String stream = channels.getLastActiveChannel().getStreamName();
        if (stream == null || stream.isEmpty()) {
            stream = client.settings.getString("username");
        }
        if (stream != null && !stream.isEmpty()) {
            followerDialog.showDialog(stream);
        }
    }
    
    private void openSubscriberDialog() {
        windowStateManager.setWindowPosition(subscribersDialog);
        String stream = client.settings.getString("username");
        if (stream != null && !stream.isEmpty()) {
            subscribersDialog.showDialog(stream);
        }
    }
    
    private void openUpdateDialog() {
        updateMessage.setLocationRelativeTo(this);
        updateMessage.showDialog();
    }
    
    private void openFavoritesDialogFromConnectionDialog(String channel) {
        Set<String> channels = chooseFavorites(this, channel);
        if (!channels.isEmpty()) {
            connectionDialog.setChannel(Helper.buildStreamsString(channels));
        }
    }
    
    public Set<String> chooseFavorites(Component owner, String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(owner);
        int result = favoritesDialog.showDialog(channel, "Use chosen channels",
                "Use chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            return favoritesDialog.getChannels();
        }
        return new HashSet<>();
    }
    
    private void openFavoritesDialogToJoin(String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(this);
        int result = favoritesDialog.showDialog(channel, "Join chosen channels",
                "Join chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            Set<String> selectedChannels = favoritesDialog.getChannels();
            client.joinChannels(selectedChannels);
        }
    }
    
    private void openJoinDialog() {
        joinDialog.setLocationRelativeTo(this);
        Set<String> chans = joinDialog.showDialog();
        client.joinChannels(chans);
    }
    
    private void openHighlightedMessages() {
        windowStateManager.setWindowPosition(highlightedMessages);
        highlightedMessages.setVisible(true);
    }
    
    private void openIgnoredMessages() {
        windowStateManager.setWindowPosition(ignoredMessages);
        ignoredMessages.setVisible(true);
    }
    
    /**
     * Opens the addressbook, opening an edit dialog for the given name if it
     * is non-null.
     * 
     * @param name The name to edit or null.
     */
    private void openAddressbook(String name) {
        if (!addressbookDialog.isVisible()) {
            windowStateManager.setWindowPosition(addressbookDialog);
        }
        addressbookDialog.showDialog(name);
    }
    
    private void openSrlRaces() {
        srl.openRaceList();
    }

    
    /*
     * Channel Management
     */
    
    /**
     * Add the channel with the given name.
     * 
     * @param channel 
     */
    public void addChannel(final String channel, final int type) {
        SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    channels.addChannel(channel, type);
                    state.update();
                }
            });
    }
    
    public void removeChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.removeChannel(channel);
                state.update();
            }
        });
    }
    
    public void switchToChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.switchToChannel(channel);
            }
        });
    }
    
    private void messageSound(String channel) {
        playSound("message", channel);
    }
    
    private void playHighlightSound(String channel) {
        playSound("highlight", channel);
    }
    
    public void followerSound(String channel) {
        playSound("follower", channel);
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..),
     * if the requirements are met.
     * 
     * @param id The id of the sound
     * @param channel The channel this event originated from, to check
     *  requirements
     */
    public void playSound(final String id, final String channel) {
        if (SwingUtilities.isEventDispatchThread()) {
            playSoundInternal(id, channel);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    playSoundInternal(id, channel);
                }
            });
        }
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..),
     * if the requirements are met. For use in EDT, because it may have to check
     * which channel is currently selected, but not sure if that has to be in
     * the EDT.
     * 
     * @param id The id of the sound
     * @param channel The channel this event originated from, to check
     *  requirements
     */
    private void playSoundInternal(String id, String channel) {
        if (client.settings.getBoolean("sounds")
                && checkRequirements(client.settings.getString(id + "Sound"), channel)) {
            playSound(id);
        }
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..).
     * 
     * @param id 
     */
    private void playSound(String id) {
        String fileName = client.settings.getString(id + "SoundFile");
        long volume = client.settings.getLong(id + "SoundVolume");
        int delay = ((Long) client.settings.getLong(id + "SoundDelay")).intValue();
        Sound.play(fileName, volume, id, delay);
    }
    
    private void showHighlightNotification(String channel, User user, String text) {
        String setting = client.settings.getString("highlightNotification");
        if (checkRequirements(setting, channel)) {
            showNotification("[Highlight] " + user.getDisplayNick() + " in " + channel, text, channel);
        }
    }
    
    public void setChannelNewStatus(final String channel, final String newStatus) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.setChannelNewStatus(channels.getChannel(channel));
            }
        });
    }
    
    public void statusNotification(final String channel, final String status) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (checkRequirements(client.settings.getString("statusNotification"), channel)) {
                    showNotification("[Status] " + channel, status, channel);
                }
                playSound("status", channel);
            }
        });
    }
    
    private void showNotification(String title, String message, String channel) {
        if (client.settings.getBoolean("useCustomNotifications")) {
            notificationManager.showMessage(title, message, channel);
        } else {
            trayIcon.displayInfo(title, message);
        }
    }
    
    public void showTestNotification(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getString("username").equalsIgnoreCase("joshimuz")) {
                    showNotification("[Test] It works!", "Now you have your notifications Josh.. Kappa", channel);
                } else if (channel == null) {
                    showNotification("[Test] It works!", "This is where the text goes.", null);
                } else {
                    showNotification("[Status] "+Helper.checkChannel(channel), "Test Notification (this would pop up when a stream status changes)", channel);
                }
            }
        });
    }
    
    /**
     * Checks the requirements that depend on whether the app and/or the given
     * channel is active.
     * 
     * @param setting What the requirements are
     * @param channel The channel to check the requirement against
     * @return true if the requirements are met, false otherwise
     */
    private boolean checkRequirements(String setting, String channel) {
        boolean channelActive = channels.getLastActiveChannel().getName().equals(channel);
        boolean appActive = isAppActive();
        // These conditions check when the requirements are NOT met
        if (setting.equals("off")) {
            return false;
        }
        if (setting.equals("both") && (channelActive || appActive)) {
            return false;
        }
        if (setting.equals("channel") && channelActive) {
            return false;
        }
        if (setting.equals("app") && appActive) {
            return false;
        }
        if (setting.equals("either") && (channelActive && appActive)) {
            return false;
        }
        if (setting.equals("channelActive") && !channelActive) {
            return false;
        }
        return true;
    }
    
    private boolean isAppActive() {
        for (Window frame : Window.getWindows()) {
            if (frame.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    private Window getActiveWindow() {
        for (Window frame : Window.getWindows()) {
            if (frame.isActive()) {
                return frame;
            }
        }
        return this;
    }
    
    /* ############
     * # Messages #
     */
    
    public void printMessage(final String channel, final User user,
            final String text, final boolean action) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                client.chatLog.message(channel, user, text);
                Channel chan = channels.getChannel(channel);
                
                boolean isOwnMessage = isOwnUsername(user.getNick());
                boolean ignored = checkHighlight(user, text, ignoreChecker, "ignore", isOwnMessage);
                boolean highlighted = checkHighlight(user, text, highlighter, "highlight", isOwnMessage);
                
                // Do stuff if highlighted, without printing message
                if (highlighted) {
                    highlightedMessages.addMessage(channel, user, text, action);
                    playHighlightSound(channel);
                    showHighlightNotification(channel, user, text);
                    channels.setChannelHighlighted(chan);
                } else if (!ignored) {
                    messageSound(channel);
                    channels.setChannelNewMessage(chan);
                }
                
                // Do stuff if ignored, without printing message
                if (ignored) {
                    ignoredMessages.addMessage(channel, user, text, action);
                    ignoredMessagesHelper.ignoredMessage(channel);
                }
                long ignoreMode = client.settings.getLong("ignoreMode");
                
                // Print or don't print depending on ignore
                if (ignored && (ignoreMode <= IgnoredMessages.MODE_COUNT || 
                        !showIgnoredInfo())) {
                    // Don't print message
                    if (isOwnMessage) {
                        printLine(channel, "Own message ignored.");
                    }
                } else {
                    // Print message, but determine how exactly
                    MessageType specialType = MessageType.REGULAR;
                    if (highlighted) {
                        specialType = MessageType.HIGHLIGHTED;
                    } else if (ignored && ignoreMode == IgnoredMessages.MODE_COMPACT) {
                        specialType = MessageType.IGNORED_COMPACT;
                    }
                    chan.printMessage(user, text, action, specialType, highlighter.getLastMatchColor());
                }
                
                // Stuff independent of highlight/ignore
                user.addMessage(text);
                updateUserInfoDialog(user);
            }
        });
    }
    
    private boolean checkHighlight(User user, String text, Highlighter hl, String setting, boolean isOwnMessage) {
        if (client.settings.getBoolean(setting + "Enabled")) {
            if (client.settings.getBoolean(setting + "OwnText") ||
                    !isOwnMessage) {
                return hl.check(user, text);
            }
        }
        return false;
    }
    
    protected void ignoredMessagesCount(String channel, String message) {
        if (client.settings.getLong("ignoreMode") == IgnoredMessages.MODE_COUNT
                && showIgnoredInfo()) {
            if (channels.isChannel(channel)) {
                channels.getChannel(channel).printLine(message);
            }
        }
    }
    
    private boolean showIgnoredInfo() {
        return !client.settings.getBoolean("ignoreShowNotDialog") ||
                !ignoredMessages.isVisible();
    }
    
    private boolean isOwnUsername(String name) {
        String ownUsername = client.getUsername();
        return ownUsername != null && ownUsername.equalsIgnoreCase(name);
    }
    
    public void userBanned(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.getChannel(channel).userBanned(user);
                user.addBan();
                updateUserInfoDialog(user);
            }
        });
    }
    

    
    /**
     * Shows a warning about joining several channels, depending on the settings.
     */
    public void channelsWarning() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (client.settings.getBoolean("tc3")) {
                    return;
                }
                boolean show = client.settings.getBoolean("channelsWarning");
                if (showedChannelsWarningThisSession) {
                    show = false;
                }
                if (show) {
                    int result = ChannelsWarning.showWarning(MainGui.this, linkLabelListener);
                    boolean showAgain = true;
                    if (result == ChannelsWarning.DONT_SHOW_AGAIN) {
                        showAgain = false;
                    }
                    client.settings.setBoolean("channelsWarning", showAgain);
                    showedChannelsWarningThisSession = true;
                }
            }
        });
    }

    public void clearChat() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getActiveChannel();
                if (panel != null) {
                    panel.clearChat();
                }
            }
        });
    }
    
    public void printLine(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getLastActiveChannel();
                if (panel != null) {
                    panel.printLine(line);
                    client.chatLog.info(panel.getName(), line);
                }
            }
        });
    }
    
    public void printSystem(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getActiveChannel();
                if (panel != null) {
                    panel.printLine(line);
                    client.chatLog.system(panel.getName(), line);
                }
            }
        });
    }

    public void printLine(final String channel, final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (channel == null) {
                    printLine(line);
                } else {
                    channels.getChannel(channel).printLine(line);
                    client.chatLog.info(channel, line);
                }
            }
        });
    }
    
    public void printLineAll(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //client.chatLog.info(null, line);
                if (channels.getChannelCount() == 0) {
                    Channel panel = channels.getActiveChannel();
                    if (panel != null) {
                        panel.printLine(line);
                    }
                    return;
                }
                for (Channel channel : channels.channels()) {
                    channel.printLine(line);
                    client.chatLog.info(channel.getName(), line);
                }
            }
        });
    }
    
    
    /**
     * Calls the appropriate method from the given channel
     * 
     * @param channel The channel this even has happened in.
     * @param type The type of event.
     * @param user The User object of who was the target of this event (mod/..).
     */
    public void printCompact(final String channel, final String type, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).printCompact(type, user);
            }
        });
    }
    
    /**
     * Perform search in the currently selected channel. Should only be called
     * from the EDT.
     * 
     * @param window
     * @param searchText 
     * @return  
     */
    public boolean search(final Window window, final String searchText) {
        Channel chan = channels.getChannelFromWindow(window);
        if (chan == null) {
            return false;
        }
        return chan.search(searchText);
    }
    
    public void resetSearch(final Window window) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel chan = channels.getChannelFromWindow(window);
                if (chan != null) {
                    chan.resetSearch();
                }
            }
        });
    }
    
    public void showMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (connectionDialog.isVisible()) {
                    JOptionPane.showMessageDialog(connectionDialog, message);
                }
                else {
                    printLine(message);
                }
            }
        });
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebug(final String line) {
        if (SwingUtilities.isEventDispatchThread()) {
            debugWindow.printLine(line);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    debugWindow.printLine(line);
                }
            });
        }
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebugIrc(final String line) {
        if (SwingUtilities.isEventDispatchThread()) {
            debugWindow.printLineIrc(line);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    debugWindow.printLineIrc(line);
                }
            });
        }
    }
    
    // User stuff
    
    /**
     * Adds a user to a channel, adding to the userlist
     * 
     * @param channel
     * @param user 
     */
    public void addUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel c = channels.getChannel(channel);
                c.addUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Removes a user from a channel, removing from the userlist
     * 
     * @param channel
     * @param user 
     */
    public void removeUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel c = channels.getChannel(channel);
                c.removeUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Updates a user on the given channel.
     * 
     * @param channel
     * @param user 
     */
    public void updateUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).updateUser(user);
                state.update();
            }
        });
    }
    
    /**
     * Resort users in the userlist of the given channel.
     */
    public void resortUsers(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).resortUserlist();
            }
        });
    }
    
    /**
     * Clears the userlist on all channels.
     */
    public void clearUsers() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Channel channel : channels.channels()) {
                    channel.clearUsers();
                }
            }
        });
    }
    
    public void reconnect() {
        client.commandReconnect();
    }
    
    public void setUpdateAvailable(final String newVersion) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                menu.setUpdateAvailable(linkLabelListener);
                updateMessage.setNewVersion(newVersion);
            }
        });
    }
    
    public void showSettings() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog().showSettings();
            }
        });
    }
    
    public void setColor(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog().showSettings("editUsercolorItem", item);
            }
        });
    }

    public void updateChannelInfo() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateChannelInfoDialog();
           }
        });
    }
    
    public void updateState() {
        updateState(false);
    }
    
    public void updateState(final boolean forced) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                state.update(forced);
                //client.testHotkey();
            }
        });
    }
    
    /**
     * Manages updating the current state, mainly the titles and menus.
     */
    private class StateUpdater {
        
        /**
         * Saves when the state was last setd, so the delay can be measured.
         */
        private long stateLastUpdated = 0;
        
        /**
         * Update state no faster than this amount of milliseconds.
         */
        private static final int UPDATE_STATE_DELAY = 500;

        /**
         * Update the title and other things based on the current state and
         * stream/channel information. This is a convenience method that doesn't
         * force the update.
         * 
         * @see update(boolean)
         */
        protected void update() {
            update(false);
        }
        
        /**
         * Update the title and other things based on the current state and
         * stream/channel information.
         * 
         * <p>The update is only performed once every {@literal UPDATE_STATE_DELAY}
         * milliseconds, unless {@literal forced} is {@literal true}. This is meant
         * to prevent flickering of the titlebar when a lot of updates would
         * happen, for example when a lot of joins/parts happen at once.</p>
         * 
         * <p>Of course this means that the info might not be always up-to-date:
         * The chance is pretty high that the last update is skipped because it
         * came to close to the previous. The UpdateTimer updates every 10s so
         * it shouldn't take too long to be corrected. This also mainly affects
         * the chatter count because it gets updated in many small steps when
         * joins/parts happen (it also already isn't very up-to-date anyway from
         * Twitch's side though).</p>
         * 
         * @param forced If {@literal true} the update is performed with every call
         */
        protected void update(boolean forced) {
            if (!forced && System.currentTimeMillis() - stateLastUpdated < UPDATE_STATE_DELAY) {
                return;
            }
            stateLastUpdated = System.currentTimeMillis();

            int state = client.getState();

            requestFollowedStreams();
            updateMenuState(state);
            updateTitles(state);
        }

        /**
         * Disables/enables menu items based on the current state.
         *
         * @param state
         */
        private void updateMenuState(int state) {
            if (state > Irc.STATE_OFFLINE || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("connect").setEnabled(false);
            } else {
                menu.getMenuItem("connect").setEnabled(true);
            }

            if (state > Irc.STATE_CONNECTING || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("disconnect").setEnabled(true);
            } else {
                menu.getMenuItem("disconnect").setEnabled(false);
            }
        }
        
        /**
         * Updates the titles of both the main window and popout dialogs.
         * 
         * @param state 
         */
        private void updateTitles(int state) {
            // May be necessary to make the title either way, because it also
            // requests stream info
            String mainTitle = makeTitle(channels.getActiveTab(), state);
            String trayTooltip = makeTitle(channels.getLastActiveChannel(), state);
            trayIcon.setTooltipText(trayTooltip);
            if (client.settings.getBoolean("simpleTitle")) {
                setTitle("Chatty");
            } else {
                setTitle(mainTitle);
            }
            Map<Channel, JDialog> popoutChannels = channels.getPopoutChannels();
            for (Channel channel : popoutChannels.keySet()) {
                String title = makeTitle(channel, state);
                popoutChannels.get(channel).setTitle(title);
            }
        }

        /**
         * Assembles the title of the window based on the current state and chat
         * and stream info.
         *
         * @param channel The {@code Channel} object to create the title for
         * @param state The current state
         * @return The created title
         */
        private String makeTitle(Channel channel, int state) {
            String channelName = channel.getName();

            // Current state
            String stateText = "";

            if (state == Irc.STATE_CONNECTING) {
                stateText = "Connecting..";
            } else if (state == Irc.STATE_CONNECTED) {
                stateText = "Connecting...";
            } else if (state == Irc.STATE_REGISTERED) {
                if (channelName.isEmpty()) {
                    stateText = "Connected";
                }
            } else if (state == Irc.STATE_OFFLINE) {
                stateText = "Not connected";
            } else if (state == Irc.STATE_RECONNECTING) {
                stateText = "Reconnecting..";
            }

            String title = stateText;

            // Stream Info
            if (!channelName.isEmpty()) {
                if (!title.isEmpty()) {
                    title += " - ";
                }
                String numUsers = Helper.formatViewerCount(channel.getNumUsers());
                StreamInfo streamInfo = getStreamInfo(channel.getStreamName());
                if (streamInfo.isValid()) {
                    if (streamInfo.getOnline()) {
                        String numViewers = Helper.formatViewerCount(streamInfo.getViewers());
                        title += channelName + " [" + numUsers + "|" + numViewers + "]";
                    } else {
                        title += channelName + " [" + numUsers + "]";
                    }
                    title += " - " + streamInfo.getFullStatus();
                } else {
                    title += channelName + " [" + numUsers + "]";
                }
            }

            title += " - Chatty";
            return title;
        }
    }
    
//    private class ViewerStats {
//        private long lastTime;
//        private static final long DELAY = 10*60*1000;
//        
//        public void makeViewerStats(String channel) {
//            long timePassed = System.currentTimeMillis() - lastTime;
//            if (timePassed > DELAY) {
//                StreamInfo info = getStreamInfo();
//                lastTime = System.currentTimeMillis();
//            }
//        }
//    }

    

    
    public void openConnectDialog(final String channelPreset) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                openConnectDialogInternal(channelPreset);
            }
        });
    }
    
    private void openConnectDialogInternal(String channelPreset) {
        updateConnectionDialog(channelPreset);
        connectionDialog.setLocationRelativeTo(this);
        connectionDialog.setVisible(true);
    }
    
    public void updateEmotesDialog() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emotesDialog.updateEmotesets(client.specialUser.getEmoteSet());
            }
        });
    }
    
    public void addEmoticons(final Set<Emoticon> emotes) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addEmoticons(emotes);
                emotesDialog.update();
            }
        });
    }
    
    public void setEmotesets(final Map<Integer, String> emotesets) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addEmotesetStreams(emotesets);
            }
        });
    }

    /* ###############
     * Get token stuff
     */
    
    public void webserverStarted() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.ready();
                }
            }
        });
    }
    
    public void webserverError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.error(error);
                }
            }
        });
    }
    
    public void webserverTokenReceived(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenReceived(token);
            }
        });
    }
    
    private void tokenGetDialogClosed() {
        tokenGetDialog.setVisible(false);
        client.stopWebserver();
    }
    
    /**
     * Token received from the webserver.
     * 
     * @param token 
     */
    private void tokenReceived(String token) {
        client.settings.setString("token", token);
        if (tokenGetDialog.isVisible()) {
            tokenGetDialog.tokenReceived();
        }
        tokenDialog.update("",token);
        updateConnectionDialog(null);
        verifyToken(token);
    }
    
    /**
     * Verify the given Token. This sends a request to the TwitchAPI.
     * 
     * @param token 
     */
    private void verifyToken(String token) {
        client.api.verifyToken(token);
        tokenDialog.verifyingToken();
    }
    
    public void tokenVerified(final String token, final TokenInfo tokenInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenVerifiedInternal(token, tokenInfo);
            }
        });
    }
    
    private String manuallyChangedToken = null;
    
    public void changeToken(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (token == null || token.isEmpty()) {
                    printSystem("You have to supply a token.");
                } else if (manuallyChangedToken != null) {
                    printSystem("You already have changed the token, please wait..");
                } else if (token.equals(client.settings.getString("token"))) {
                    printSystem("The token you entered is already set.");
                } else {
                    printSystem("Setting new token. Please wait..");
                    client.settings.setString("username", null);
                    manuallyChangedToken = token;
                    tokenReceived(token);
                }
            }
        });
    }
    
    /**
     * This does the main work when a response for verifying the token is
     * received from the Twitch API.
     * 
     * A Token can be verified manually by pressing the button or automatically
     * when a new Token was received by the webserver. So when this is called
     * the original source can be both.
     * 
     * The tokenGetDialog is closed if necessary.
     * 
     * @param token The token that was verified
     * @param username The usernamed that was received for this token. If this
     *      is null then an error occured, if it is empty then the token was
     *      invalid.
     */
    private void tokenVerifiedInternal(String token, TokenInfo tokenInfo) {
        // Stopping the webserver here, because it allows the /tokenreceived/
        // page to be delievered, because of the delay of verifying the token.
        // This should probably be solved better.
        client.stopWebserver();
        
        String result;
        String currentUsername = client.settings.getString("username");
        // Check if a new token was requested (the get token dialog should still
        // be open at this point) If this is wrong, it just displays the wrong
        // text, this shouldn't be used for something critical.
        boolean getNewLogin = tokenGetDialog.isVisible();
        boolean showInDialog = tokenDialog.isVisible();
        boolean changedTokenResponse = token == null
                ? manuallyChangedToken == null : token.equals(manuallyChangedToken);
        boolean valid = false;
        if (tokenInfo == null) {
            // An error occured when verifying the token
            if (getNewLogin) {
                result = "An error occured completing getting login data.";
            }
            else {
                result = "An error occured verifying login data.";
            }
        }
        else if (!tokenInfo.isTokenValid()) {
            // There was an answer when verifying the token, but it was invalid
            if (getNewLogin) {
                result = "Invalid token received when getting login data. Please "
                    + "try again.";
            }
            else if (changedTokenResponse) {
                result = "Invalid token entered. Please try again.";
            }
            else {
                result = "Login data invalid. Should probably remove it and request "
                        + "it again.";
            }
            if (!showInDialog && !changedTokenResponse) {
                showTokenWarning();
            }
            client.settings.setString("token", "");
        }
        else if (!tokenInfo.chat_access()) {
            result = "No chat access (required) with token.";
        }
        else {
            // Everything is fine, so save username and token
            valid = true;
            String username = tokenInfo.getUsername();
            client.settings.setString("username", username);
            client.settings.setString("token", token);
            tokenDialog.update(username, token);
            updateConnectionDialog(null);
            if (!currentUsername.isEmpty() && !username.equals(currentUsername)) {
                result = "Login verified and ready to connect (replaced '" +
                        currentUsername + "' with '" + username + "').";
            }
            else {
                result = "Login verified and ready to connect.";
            }
        }
        if (changedTokenResponse) {
            printLine(result);
            manuallyChangedToken = null;
        }
        setTokenScopes(tokenInfo);
        // Always close the get token dialog, if it's not open, nevermind ;)
        tokenGetDialog.setVisible(false);
        // Show result in the token dialog
        tokenDialog.tokenVerified(valid, result);
    }
    
    /**
     * Sets the token scopes in the settings based on the given TokenInfo.
     * 
     * @param info 
     */
    private void setTokenScopes(TokenInfo info) {
        if (info == null) {
            return;
        }
        if (info.isTokenValid()) {
            client.settings.setBoolean("token_editor", info.channel_editor());
            client.settings.setBoolean("token_commercials", info.channel_commercials());
            client.settings.setBoolean("token_user", info.user_read());
            client.settings.setBoolean("token_subs", info.channel_subscriptions);
        }
        else {
            resetTokenScopes();
        }
        updateTokenScopes();
    }
    
    /**
     * Updates the token scopes in the GUI based on the settings.
     */
    private void updateTokenScopes() {
        boolean commercials = client.settings.getBoolean("token_commercials");
        boolean editor = client.settings.getBoolean("token_editor");
        boolean user = client.settings.getBoolean("token_user");
        boolean subscriptions = client.settings.getBoolean("token_subs");
        tokenDialog.updateAccess(editor, commercials, user, subscriptions);
        adminDialog.updateAccess(editor, commercials);
    }
    
    private void resetTokenScopes() {
        client.settings.setBoolean("token_commercials", false);
        client.settings.setBoolean("token_editor", false);
        client.settings.setBoolean("token_user", false);
        client.settings.setBoolean("token_subs", false);
    }
    
    public void showTokenWarning() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String message = "Login data (access token) was determined "
                        + "invalid and was removed.";
                String[] options = new String[]{"Close / Show Help","Just Close"};
                int result = GuiUtil.showNonAutoFocusOptionPane(MainGui.this, "Error",
                        message, JOptionPane.ERROR_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, options);
                if (result == 0) {
                    openHelp("help-troubleshooting.html","tokenDeleted");
                }
            }
        });
    }
    
    public void setSubscriberInfo(final FollowerInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                subscribersDialog.setFollowerInfo(info);
            }
        });
    }
    
    public void setFollowerInfo(final FollowerInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                followerDialog.setFollowerInfo(info);
            }
        });
    }
    
    public void setChannelInfo(final String channel, final ChannelInfo info, final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setChannelInfo(channel, info, result);
            }
        });
    }
    
    public void putChannelInfo(String stream, ChannelInfo info) {
        client.api.putChannelInfo(stream, info, client.settings.getString("token"));
    }
    
    public void getChannelInfo(String channel) {
        client.api.getChannelInfo(channel);
    }
    
    public void performGameSearch(String search) {
        client.api.getGameSearch(search);
    }
    
    public String getActiveStream() {
        return channels.getActiveChannel().getStreamName();
    }
    
    /**
     * Saves the Set game favorites to the settings.
     * 
     * @param favorites 
     */
    public void setGameFavorites(Set<String> favorites) {
        client.settings.putList("gamesFavorites", new ArrayList(favorites));
    }
    
    /**
     * Returns a Set of game favorites retrieved from the settings.
     * 
     * @return 
     */
    public Set<String> getGameFavorites() {
        return new HashSet<>(client.settings.getList("gamesFavorites"));
    }
    
    public void gameSearchResult(final Set<String> games) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.gameSearchResult(games);
            }
        });
    }
    
    public void putChannelInfoResult(final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setPutResult(result);
            }
        });
    }
    
    public void saveCommercialDelaySettings(boolean enabled, long delay) {
        client.settings.setBoolean("adDelay", enabled);
        client.settings.setLong("adDelayLength", delay);
    }
    
    private void loadCommercialDelaySettings() {
        boolean enabled = client.settings.getBoolean("adDelay");
        long length = client.settings.getLong("adDelayLength");
        adminDialog.updateCommercialDelaySettings(enabled, length);
    }
    
    public void runCommercial(String stream, int length) {
        client.runCommercial(stream, length);
    }
    
    public void runCommercial() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (adminDialog.isCommercialsTabVisible()) {
                    adminDialog.commercialHotkey();
                } else {
                    runCommercial(getActiveStream(), 30);
                }
            }
        });
    }
    
    public void commercialResult(final String stream, final String text, final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.commercialResult(stream, text, result);
            }
        });
    }
    
    public void setCommercialHotkey(String hotkey) {
        client.setCommercialHotkey(hotkey);
    }

    /**
     * Get StreamInfo for the given stream, but also request it for all open
     * channels.
     * 
     * @param stream
     * @return 
     */
    public StreamInfo getStreamInfo(String stream) {
        Set<String> streams = new HashSet<>();
        for (Channel chan : channels.getChannels()) {
            streams.add(chan.getStreamName());
        }
        return client.api.getStreamInfo(stream, streams);
    }
    
    /**
     * Outputs the full title if the StreamInfo for this channel is valid.
     * 
     * @param channel 
     */
    public void printStreamInfo(final String channel) {
        final String stream = channel.replace("#", "");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getBoolean("printStreamStatus")) {
                    StreamInfo info = getStreamInfo(stream);
                    if (info.isValid()) {
                        printLine(channel, "~" + info.getFullStatus() + "~");
                    }
                }
            }
        });
    }
    
    /**
     * Possibly request followed streams from the API, if enabled and access
     * was granted.
     */
    private void requestFollowedStreams() {
        if (client.settings.getBoolean("requestFollowedStreams") &&
                client.settings.getBoolean("token_user")) {
            client.api.getFollowedStreams(client.settings.getString("token"));
        }
    }

    private class MySettingChangeListener implements SettingChangeListener {
        /**
         * Since this can also be called from other threads, run in EDT if
         * necessary.
         *
         * @param setting
         * @param type
         * @param value
         */
        @Override
        public void settingChanged(final String setting, final int type, final Object value) {
            if (SwingUtilities.isEventDispatchThread()) {
                settingChangedInternal(setting, type, value);
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        settingChangedInternal(setting, type, value);
                    }
                });
            }
        }
        
        private void settingChangedInternal(String setting, int type, Object value) {
            if (type == Setting.BOOLEAN) {
                if (setting.equals("ontop")) {
                    setAlwaysOnTop((Boolean) value);
                } else if (setting.equals("ignoreJoinsParts")) {
                    if ((Boolean) value) {
                        client.clearUserList();
                    }
                } else if (setting.equals("highlightUsername")) {
                    updateHighlightSetUsernameHighlighted((Boolean) value);
                } else if (setting.equals("highlightNextMessages")) {
                    updateHighlightNextMessages();
                } else if (setting.equals("popoutSaveAttributes") || setting.equals("popoutCloseLastChannel")) {
                    updatePopoutSettings();
                } else if (setting.equals("livestreamer")) {
                    ContextMenuHelper.enableLivestreamer = (Boolean)value;
                }
                loadMenuSetting(setting);
            }

            if (StyleManager.settingNames.contains(setting)) {
                styleManager.refresh();
                channels.refreshStyles();
                highlightedMessages.refreshStyles();
                ignoredMessages.refreshStyles();
                //menu.setForeground(styleManager.getColor("foreground"));
                //menu.setBackground(styleManager.getColor("background"));
            }
            if (type == Setting.STRING) {
                if (setting.equals("timeoutButtons")) {
                    userInfoDialog.setUserDefinedButtonsDef((String) value);
                } else if (setting.equals("token")) {
                    client.api.setToken((String)value);
                }
            }
            if (type == Setting.LIST) {
                if (setting.equals("highlight")) {
                    updateHighlight();
                } else if (setting.equals("ignore")) {
                    updateIgnore();
                }
            }
            if (type == Setting.LONG) {
                if (setting.equals("dialogFontSize")) {
                    userInfoDialog.setFontSize((Long)value);
                }
            }
            if (setting.equals("channelFavorites") || setting.equals("channelHistory")) {
                // TOCONSIDER: This means that it is updated twice in a row when an action
                // requires both settings to be changed
                updateFavoritesDialogWhenVisible();
            }
            if (setting.equals("liveStreamsSorting")) {
                updateLiveStreamsDialog();
            }
            if (setting.equals("historyRange")) {
                updateHistoryRange();
            }
            Set<String> notificationSettings = new HashSet<>(Arrays.asList(
                "nScreen", "nPosition", "nDisplayTime", "nMaxDisplayTime",
                "nMaxDisplayed", "nMaxQueueSize", "nActivity", "nActivityTime"));
            if (notificationSettings.contains(setting)) {
                updateNotificationSettings();
            }
            if (setting.equals("spamProtection")) {
                client.setLinesPerSeconds((String)value);
            }
            if (setting.equals("urlPrompt")) {
                UrlOpener.setPrompt((Boolean)value);
            }
            if (setting.equals("abUniqueCats")) {
                client.addressbook.setSomewhatUniqueCategories((String)value);
            }
            if (setting.equals("commands")) {
                client.customCommands.loadFromSettings();
            }
            if (setting.equals("channelContextMenu") || setting.equals("userContextMenu") || setting.equals("livestreamerQualities")) {
                updateCustomContextMenuEntries();
            }
            else if (setting.equals("chatScrollbarAlways") || setting.equals("userlistWidth")) {
                updateChannelsSettings();
            }
            else if (setting.equals("ignoredEmotes")) {
                emoticons.setIgnoredEmotes(client.settings.getList("ignoredEmotes"));
            }
        }
    }
    
    private class MySettingsListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            if (SwingUtilities.isEventDispatchThread()) {
                System.out.println("Saving GUI settings.");
                client.settings.setLong("favoritesSorting", favoritesDialog.getSorting());
                emoticons.saveFavoritesToSettings(settings);
            }
        }
        
    }
    
    public WindowListener getWindowListener() {
        return windowListener;
    }
    
    private class MyWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (e.getSource() == tokenGetDialog) {
                tokenGetDialogClosed();
            }
        }
    }
    
    private class MainWindowListener extends WindowAdapter {
        
        @Override
        public void windowStateChanged(WindowEvent e) {
            if (e.getComponent() == MainGui.this) {
                saveState(e.getComponent());
                if (client.settings.getBoolean("minimizeToTray") && isMinimized()) {
                    minimizeToTray();
                } else {
                    cleanupAfterRestoredFromTray();
                }
            }
        }

        @Override
        public void windowClosing(WindowEvent evt) {
            if (evt.getComponent() == MainGui.this) {
                if (client.settings.getBoolean("closeToTray")) {
                    minimizeToTray();
                } else {
                    exit();
                }
            }
        }
    }
    
    /**
     * Checks if the main window is currently minimized.
     * 
     * @return true if minimized, false otherwise
     */
    private boolean isMinimized() {
        return (getExtendedState() & ICONIFIED) == ICONIFIED;
    }
    
    /**
     * Minimize window to tray.
     */
    private void minimizeToTray() {
        if (!isMinimized()) {
            setExtendedState(getExtendedState() | ICONIFIED);
        }
        trayIcon.setIconVisible(true);
        // Set visible to false, so it is removed from the taskbar
        setVisible(false);
        //trayIcon.displayInfo("Minimized to tray", "Double-click icon to show again..");
    }
    
    /**
     * Remove tray icon if applicable.
     */
    private void cleanupAfterRestoredFromTray() {
        if (client.settings.getBoolean("useCustomNotifications")) {
            trayIcon.setIconVisible(false);
        }
    }
    
    /**
     * Display an error dialog with the option to quit or continue the program
     * and to report the error.
     *
     * @param error The error as a LogRecord
     * @param previous Some previous debug messages as LogRecord, to provide
     * context
     */
    public void error(final LogRecord error, final LinkedList<LogRecord> previous) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int result = errorMessage.show(error, previous);
                if (result == ErrorMessage.QUIT) {
                    exit();
                }
            }
        });
    }
    
    /**
     * Exit the program.
     */
    private void exit() {
        client.exit();
    }
    
    public void cleanUp() {
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(false);
            dispose();
        }
    }
    
}
