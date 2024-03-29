
package chatty.gui.components;

import chatty.User;
import chatty.gui.MainGui;
import chatty.gui.StyleServer;
import chatty.gui.components.ChannelTextPane.MessageType;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HighlightsContextMenu;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfo;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.text.MutableAttributeSet;

/**
 * Window showing all highlighted (or ignored) messages.
 * 
 * @author tduva
 */
public class HighlightedMessages extends JDialog {
    
    private final TextPane messages;
    private String currentChannel;
    private int currentChannelMessageCount = 0;
    
    /**
     * This may not be the count that is actually displayed, if messages have
     * been cleared automatically from the buffer in the meantime.
     */
    private int displayedCount;
    private int newCount;
    private boolean setChatIconsYet = false;
    
    private final String title;
    private final String label;
    
    private final ContextMenuListener contextMenuListener;
    
    /**
     * Creates a new dialog.
     * 
     * @param owner Reference to the MainGui, required for the text pane
     * @param styleServer The style server, style information for the text pane
     * @param title The title to display for the dialog
     * @param label What to show as description of the messges in the text pane
     * (when the channel name is output)
     * @param contextMenuListener
     */
    public HighlightedMessages(MainGui owner, StyleServer styleServer,
            String title, String label, ContextMenuListener contextMenuListener) {
        super(owner);
        this.title = title;
        this.label = label;
        this.contextMenuListener = contextMenuListener;
        updateTitle();
        
        this.addComponentListener(new MyVisibleListener());
        
        messages = new TextPane(owner, styleServer);
        messages.setContextMenuListener(new MyContextMenuListener());
        //messages.setLineWrap(true);
        //messages.setWrapStyleWord(true);
        //messages.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(messages);
        messages.setScrollPane(scroll);
        
        add(scroll);
        
        setPreferredSize(new Dimension(400,300));
        
        pack();
    }
    
    public void addMessage(String channel, User user, String text, boolean action) {
        if (currentChannel == null || !currentChannel.equals(channel)
                || currentChannelMessageCount > 12) {
            messages.printLine(label+" in " + channel + ":");
            currentChannel = channel;
            currentChannelMessageCount = 0;
        }
        currentChannelMessageCount++;
        messages.printMessage(user, text, action, MessageType.REGULAR, null);
        displayedCount++;
        updateTitle();
        if (!isVisible()) {
            newCount++;
        }
    }
    
    private void updateTitle() {
        if (displayedCount > 0) {
            setTitle(title+" ("+displayedCount+")");
        } else {
            setTitle(title);
        }
    }
    
    public void refreshStyles() {
        messages.refreshStyles();
    }
    
    /**
     * Removes all text from the window.
     */
    public void clear() {
        messages.clear();
        currentChannel = null;
        currentChannelMessageCount = 0;
        displayedCount = 0;
        updateTitle();
    }
    
    /**
     * Get the count of all messages added after the last clear of the window.
     * 
     * @return 
     */
    public int getDisplayedCount() {
        return displayedCount;
    }
    
    /**
     * Get the count of all messages added while the window wasn't visible.
     * 
     * @return 
     */
    public int getNewCount() {
        return newCount;
    }
    
    /**
     * Normal channel text pane modified a bit to fit the needs for this.
     */
    static class TextPane extends ChannelTextPane {
        
        public TextPane(MainGui main, StyleServer styleServer) {
            super(main, styleServer);
            linkController.setDefaultContextMenu(new HighlightsContextMenu());
        }

        public void printMessage(String channel, User user, String text, boolean action) {
            closeCompactMode();

            MutableAttributeSet style = styles.standard();
            print(getTimePrefix(), style);
            print(channel);
            printUser(user, action, false);
            printSpecials(text, user, style);
            printNewline();
        }
        
        public void clear() {
            setText("");
        }
        
    }
    
    private class MyContextMenuListener implements ContextMenuListener {
        
        @Override
        public void menuItemClicked(ActionEvent e) {
            if (e.getActionCommand().equals("clearHighlights")) {
                clear();
            }
            contextMenuListener.menuItemClicked(e);
        }

        @Override
        public void userMenuItemClicked(ActionEvent e, User user) {
            contextMenuListener.userMenuItemClicked(e, user);
        }

        @Override
        public void urlMenuItemClicked(ActionEvent e, String url) {
            contextMenuListener.urlMenuItemClicked(e, url);
        }

        @Override
        public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
            contextMenuListener.streamsMenuItemClicked(e, streams);
        }

        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
            contextMenuListener.streamInfosMenuItemClicked(e, streamInfos);
        }

        @Override
        public void emoteMenuItemClicked(ActionEvent e, Emoticon emote) {
            contextMenuListener.emoteMenuItemClicked(e, emote);
        }
    }
    
    /**
     * Checks if the window is being shown, so the new messages count can be
     * reset (which kind of indicates unread messages).
     */
    private class MyVisibleListener extends ComponentAdapter {
        
        @Override
        public void componentShown(ComponentEvent e) {
            newCount = 0;
        }
        
    }
    
}
