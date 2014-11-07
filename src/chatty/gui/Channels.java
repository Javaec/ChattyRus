
package chatty.gui;

import chatty.Helper;
import chatty.Helper.IntegerPair;
import chatty.gui.components.Channel;
import chatty.gui.components.ChannelDialog;
import chatty.gui.components.Tabs;
import chatty.gui.components.menus.ContextMenuListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Managing the Channel objects in the main window and popouts, providing a
 * default channel while no other is added.
 * 
 * @author tduva
 */
public class Channels {

    private static final String SEARCH_HOTKEY_KEY = "ChattySearchHotkey";
    
    private final MainGui gui;
            
    private final WindowListener windowListener;
    private ChangeListener changeListener;
    
    /**
     * Saves all added channels by name.
     */
    private final HashMap<String,Channel> channels = new HashMap<>();
    
    /**
     * Saves which channels are in a popout (and which dialog it is).
     */
    private final Map<Channel, JDialog> dialogs = new LinkedHashMap<>();
    
    /**
     * Saves attributes of closed popout dialogs.
     */
    private final List<LocationAndSize> dialogsAttributes = new ArrayList<>();
    private final Tabs tabs;
    private Channel defaultChannel;
    private final StyleManager styleManager;
    private final ContextMenuListener contextMenuListener;
    private final MouseClickedListener mouseClickedListener = new MyMouseClickedListener();
    
    /**
     * Default width of the userlist, given to Channel objects when created.
     */
    private int defaultUserlistWidth = 140;
    private boolean chatScrollbarAlaways;
    private Channel lastActiveChannel = null;
    
    private boolean savePopoutAttributes;
    private boolean closeLastChannelPopout;
    
    /**
     * Save channels whose state is new highlighted messages, so the color
     * doesn't get overwritten by new messages.
     */
    private final Set<Channel> highlighted = new HashSet<>();
    
    public Channels(MainGui gui, StyleManager styleManager,
            ContextMenuListener contextMenuListener) {
        windowListener = new MyWindowListener();
        tabs = new Tabs(contextMenuListener);
        this.styleManager = styleManager;
        this.contextMenuListener = contextMenuListener;
        this.gui = gui;
        tabs.addChangeListener(new TabChangeListener());
        gui.addWindowListener(windowListener);
        //tabs.setOpaque(false);
        //tabs.setBackground(new Color(0,0,0,0));
        addDefaultChannel();
    }
    
    public void setChangeListener(ChangeListener listener) {
        changeListener = listener;
    }
    
    private void channelChanged() {
        if (changeListener != null) {
            changeListener.stateChanged(new ChangeEvent(this));
        }
    }
    
    /**
     * Set channel to show a new highlight messages has arrived, changes color
     * of the tab. ONly if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelHighlighted(Channel channel) {
        if (getActiveTab() != channel) {
            tabs.setForegroundForComponent(channel, MainGui.COLOR_NEW_HIGHLIGHTED_MESSAGE);
            highlighted.add(channel);
        }
    }
    
    /**
     * Set channel to show a new message arrived, changes color of the tab.
     * Only if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelNewMessage(Channel channel) {
        if (getActiveTab() != channel && !highlighted.contains(channel)) {
            tabs.setForegroundForComponent(channel, MainGui.COLOR_NEW_MESSAGE);
        }
    }
    
    /**
     * Reset state (color, title suffixes) to default.
     * 
     * @param channel 
     */
    public void resetChannelTab(Channel channel) {
        tabs.setForegroundForComponent(channel, null);
        tabs.setTitleForComponent(channel, channel.getName());
        highlighted.remove(channel);
    }
    
    /**
     * Set channel to new status available, adds a suffix to indicate that.
     * Only if not currently the active tab.
     * 
     * @param channel 
     */
    public void setChannelNewStatus(Channel channel) {
        if (getActiveTab() != channel) {
            tabs.setTitleForComponent(channel, channel.getName()+"*");
        }
    }
    
    
    /**
     * This is the channel when no channel has been added yet.
     */
    private void addDefaultChannel() {
        defaultChannel = createChannel("", Channel.TYPE_NONE);
        tabs.addTab(defaultChannel);
    }
    
    private Channel createChannel(String name, int type) {
        Channel channel = new Channel(name,type,gui,styleManager, contextMenuListener, defaultUserlistWidth);
        channel.setMouseClickedListener(mouseClickedListener);
        channel.setScrollbarAlways(chatScrollbarAlaways);
        return channel;
    }
    
    public Component getComponent() {
        return tabs;
    }
    
    public Channel get(String key) {
        return channels.get(key);
    }
    
    public Collection<Channel> channels() {
        return channels.values();
    }
    
    public int getChannelCount() {
        return channels.size();
    }
    
    /**
     * Check if the given channel is added.
     * 
     * @param channel
     * @return 
     */
    public boolean isChannel(String channel) {
        if (channel == null) {
            return false;
        }
        return channels.get(channel) != null;
    }
    
    public Channel getChannel(String channel) {
        return getChannel(channel, Channel.TYPE_CHANNEL);
    }
    
    /**
     * Gets the Channel object for the given channel name. If none exists, the
     * channel is automatically added.
     * 
     * @param channel
     * @return 
     */
    public Channel getChannel(String channel, int type) {
        Channel panel = channels.get(channel);
        if (panel == null) {
            panel = addChannel(channel, type);
        }
        return panel;
    }
    
    public String getChannelNameFromPanel(Channel panel) {
        for (String key : channels.keySet()) {
            if (channels.get(key) == panel) {
                return key;
            }
        }
        return null;
    }
    
    public Channel getChannelFromWindow(Object dialog) {
        for (Channel channel : dialogs.keySet()) {
            if (dialogs.get(channel) == dialog) {
                return channel;
            }
        }
        if (dialog == gui) {
            return getActiveTab();
        }
        return null;
    }
    
    
    /**
     * Adds a channel with the given name. If the default channel is still there
     * it is used for this channel and renamed.
     * 
     * @param channelName
     * @return 
     */
    public Channel addChannel(String channelName, int type) {
        if (channels.get(channelName) != null) {
            return null;
        }
        Channel panel;
        if (defaultChannel != null) {
            // Reuse default channel
            panel = defaultChannel;
            defaultChannel = null;
            panel.setName(channelName);
            channelChanged();
        }
        else {
            // No default channel, so create a new one
            panel = createChannel(channelName, type);
            tabs.addTab(panel);
            tabs.setSelectedComponent(panel);
        }
        channels.put(channelName, panel);
        return panel;
    }
    
    public void removeChannel(final String channelName) {
        // TODO: Why invokeLater?
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                Channel panel = channels.get(channel);
//                if (panel == null) {
//                    return;
//                }
//                channels.remove(channel);
//                tabs.removeTab(panel);
//                if (tabs.getTabCount() == 0) {
//                    addDefaultChannel();
//                    gui.updateState();
//                }
//            }
//        });
        Channel channel = channels.get(channelName);
        if (channel == null) {
            return;
        }
        channels.remove(channelName);
        closePopout(channel);
        tabs.removeTab(channel);
        if (tabs.getTabCount() == 0) {
            if (dialogs.isEmpty() || !closeLastChannelPopout) {
                addDefaultChannel();
            } else {
                closePopout(dialogs.keySet().iterator().next());
            }
            channelChanged();
            gui.updateState();
        }
    }
    
    private static void addHotkey(JDialog dialog, int keyCode, Action action) {
        dialog.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, KeyEvent.CTRL_DOWN_MASK), action);
        dialog.getRootPane().getActionMap().put(action, action);
    }
    
    /**
     * Popout the given channel if it isn't already and if there is actually
     * more than one tab.
     * 
     * @param channel The {@code Channel} to popout
     */
    public void popout(final Channel channel) {
        if (channel == null) {
            return;
        }
        if (dialogs.containsKey(channel)) {
            return;
        }
        if (tabs.getTabCount() < 2) {
            return;
        }
        tabs.removeTab(channel);
        
        // Create and configure new dialog for the popout
        final JDialog newDialog = new ChannelDialog(gui, channel);
        newDialog.setLocationRelativeTo(gui);
        newDialog.addWindowListener(windowListener);
        addHotkey(newDialog, KeyEvent.VK_F, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                gui.openSearchDialog();
            }
        });
        addHotkey(newDialog, KeyEvent.VK_E, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                gui.toggleEmotesDialog();
            }
        });
        addHotkey(newDialog, KeyEvent.VK_W, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //gui.client.closeChannel(channel.getName());
                closePopout(channel);
            }
        });
        

        // Restore attributes if available
        if (!dialogsAttributes.isEmpty()) {
            LocationAndSize attr = dialogsAttributes.remove(0);
            if (GuiUtil.isPointOnScreen(attr.location, 5)) {
                newDialog.setLocation(attr.location);
            }
            newDialog.setSize(attr.size);
        }
        
        dialogs.put(channel, newDialog);
        
        // Making it visible directly apparently makes it not properly detect
        // it as active window
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                newDialog.setVisible(true);
            }
        });
        
        gui.updateState(true);
    }
    
    public void popoutActiveChannel() {
        if (getActiveChannel() != null) {
            popout(getActiveChannel());
        }
    }
    
    public void setSavePopoutAttributes(boolean save) {
        savePopoutAttributes = save;
        if (!save) {
            dialogsAttributes.clear();
        }
    }
    
    /**
     * Returns a list of Strings that contain the location/size of open dialogs
     * and of closed dialogs that weren't reused.
     * 
     * Format: "x,y;width,height"
     * 
     * @return 
     */
    public List getPopoutAttributes() {
        List<String> attributes = new ArrayList<>();
        for (JDialog dialog : dialogs.values()) {
            attributes.add(dialog.getX()+","+dialog.getY()
                    +";"+dialog.getWidth()+","+dialog.getHeight());
        }
        for (LocationAndSize attr : dialogsAttributes) {
            attributes.add(attr.location.x + "," + attr.location.y
                    + ";" + attr.size.width + "," + attr.size.height);
        }
        return attributes;
    }
    
    /**
     * Sets the attributes for popouts that will be opened, as a List of Strings
     * that have to be parsed into {@literal LocationAndSize} objects.
     * 
     * @param attributes 
     */
    public void setPopoutAttributes(List<String> attributes) {
        dialogsAttributes.clear();
        for (String attr : attributes) {
            if (attr == null) {
                continue;
            }
            String[] split = attr.split(";");
            if (split.length != 2) {
                continue;
            }
            IntegerPair location = Helper.getNumbersFromString(split[0]);
            IntegerPair size = Helper.getNumbersFromString(split[1]);
            if (location != null && size != null) {
                dialogsAttributes.add(
                        new LocationAndSize(
                                new Point(location.a, location.b),
                                new Dimension(size.a, size.b)));
            }
        }
    }
    
    public void setCloseLastChannelPopout(boolean close) {
        closeLastChannelPopout = close;
    }
    
    /**
     * Once the popout dialog was closed (either by the user or by the program)
     * add the channel to the tabs again and update the GUI.
     * 
     * @param channel 
     */
    private void popoutDisposed(Channel channel) {
        if (channel == null) {
            return;
        }
        dialogs.remove(channel);
        if (defaultChannel != null) {
            tabs.removeTab(defaultChannel);
            defaultChannel = null;
        }
        tabs.addTab(channel);
        tabs.setSelectedComponent(channel);
        gui.updateState(true);
    }
    
    /**
     * Close the popout for the given channel (if it exists) and move the
     * channel back to the main window.
     * 
     * @param channel 
     */
    public void closePopout(Channel channel) {
        if (channel == null) {
            return;
        }
        if (!dialogs.containsKey(channel)) {
            return;
        }
        JDialog dialog = dialogs.remove(channel);
        dialog.dispose();
        popoutDisposed(channel);
    }
    
    /**
     * Return the currently active Channel, which means the one that has focus,
     * either because it is a popout dialog that has focus, or the currently
     * selected tab if the focus is on the main window.
     * 
     * <p>
     * If the focus is not on a window containing a Channel, then the current
     * tab of the main window is returned.
     * </p>
     * 
     * @return The Channel object which is currently selected.
     */
    public Channel getActiveChannel() {
        for (Channel channel : dialogs.keySet()) {
            if (dialogs.get(channel).isActive()) {
                return channel;
            }
        }
        return getActiveTab();
    }
    
    /**
     * Returns the Channel that was last active. If the focus is on a Window
     * that contains a Channel, it should be the same as
     * {@link getActiveChannel()}, otherwise it is the Channel that was active
     * before a Window without a Channel was focused (e.g. an info dialog).
     * 
     * @return 
     */
    public Channel getLastActiveChannel() {
        if (lastActiveChannel == null) {
            return getActiveTab();
        }
        return lastActiveChannel;
    }
    
    /**
     * Returns channel of the active tab in the main window (as opposed to the
     * active channel, which might also be in a popout).
     * 
     * @return 
     */
    public Channel getActiveTab() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof Channel) {
            return (Channel) c;
        }
        return null;
    }
    
    /**
     * Returns a map of all channels and their respective dialog.
     * 
     * @return The {@literal Map} with {@literal Channel} objects as keys and
     * {@literal JDialog} objects as values
     */
    public Map<Channel, JDialog> getPopoutChannels() {
        return new HashMap<>(dialogs);
    }
    
    /**
     * Return the channel from the given input box.
     * 
     * @param input The reference to the input box.
     * @return The Channel object, or null if the given reference isn't an
     *  input box
     */
    public Channel getChannelFromInput(Object input) {
        if (defaultChannel != null && input == defaultChannel.getInput()) {
            return defaultChannel;
        }
        for (String key : channels.keySet()) {
            Channel value = channels.get(key);
            if (input == value.getInput()) {
                return value;
            }
        }
        return null;
    }

    /**
     * A list of all channels, be it in the main window or in popouts.
     * 
     * @return The {@code List} of {@code Channel} objects
     */
    public List<Channel> getChannels() {
        List<Channel> result = new ArrayList<>();
        if (defaultChannel != null) {
            result.add(defaultChannel);
        }
        result.addAll(channels.values());
        return result;
    }
    
    public void setInitialFocus() {
        getActiveChannel().requestFocusInWindow();
    }
    
    public void refreshStyles() {
        for (Channel channel : getChannels()) {
            channel.refreshStyles();
        }
    }
    
    public void switchToChannel(String channel) {
        if (isChannel(channel)) {
            tabs.setSelectedComponent(get(channel));
        }
    }
    
    public void switchToNextChannel() {
        tabs.setSelectedNext();
    }
    
    public void switchToPreviousChannel() {
        tabs.setSelectedPrevious();
    }
    
    public void setDefaultUserlistWidth(int width) {
        defaultUserlistWidth = width;
        if (defaultChannel != null) {
            // Set the width of the default channel because it's created before
            // the width is loaded from the settings
            defaultChannel.setUserlistWidth(width);
        }
    }
    
    public void setChatScrollbarAlways(boolean always) {
        chatScrollbarAlaways = always;
        for (Channel chan : channels.values()) {
            chan.setScrollbarAlways(always);
        }
        if (defaultChannel != null) {
            defaultChannel.setScrollbarAlways(always);
        }
    }
    
    public void setTabOrder(String order) {
        int setting = Tabs.ADD_ORDER;
        switch (order) {
            case "alphabetical": setting = Tabs.ALPHABETIC_ORDER; break;
        }
        tabs.setOrder(setting);
    }
    
    /**
     * When the active tab is changed, keeps track of the lastActiveChannel and
     * does some work necessary when tab is changed.
     */
    private class TabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            lastActiveChannel = getActiveTab();
            setInitialFocus();
            resetChannelTab(getActiveChannel());
            channelChanged();
        }
    }
    
    /**
     * Sets the focus to the input bar when clicked anywhere on the channel.
     */
    private class MyMouseClickedListener implements MouseClickedListener {

        @Override
        public void mouseClicked() {
            setInitialFocus();
        }
    }
    
    /**
     * Registered to popout dialogs and the main window, cleans up closed popout
     * dialogs and keeps the lastActiveChannel up-to-date.
     */
    private class MyWindowListener extends WindowAdapter {
        
        @Override
        public void windowClosed(WindowEvent e) {
            if (e.getSource() == gui) {
                return;
            }
            popoutDisposed(getChannelFromWindow(e.getSource()));
            if (savePopoutAttributes) {
                Window window = e.getWindow();
                dialogsAttributes.add(0, new LocationAndSize(
                        window.getLocation(), window.getSize()));
            }
        }
        
        @Override
        public void windowActivated(WindowEvent e) {
            Channel channel = getChannelFromWindow(e.getSource());
            if (channel != lastActiveChannel) {
                lastActiveChannel = channel;
                channelChanged();
            }
        }
        
    }
    
    private static class LocationAndSize {
        public final Point location;
        public final Dimension size;
        
        LocationAndSize(Point location, Dimension size) {
            this.location = location;
            this.size = size;
        }
    }
    
}
