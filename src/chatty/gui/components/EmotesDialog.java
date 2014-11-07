
package chatty.gui.components;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

/**
 * Dialog showing emoticons that can be clicked on to insert them in the last
 * active channel inputbox. Also allows to open the emote context menu.
 * 
 * @author tduva
 */
public class EmotesDialog extends JDialog {
    
    private static final Insets TITLE_INSETS = new Insets(5,8,0,8);
    private static final Insets SUBTITLE_INSETS = new Insets(6,4,2,4);
    private static final Insets SUBTITLE_INSETS_SMALLER_MARGIN = new Insets(1,2,0,2);
    private static final Insets EMOTE_INSETS = new Insets(4,10,4,10);
    
    private static final String FAVORITE_EMOTES = "Favorites";
    private static final String MY_EMOTES = "My Emotes";
    private static final String CHANNEL_EMOTES = "Channel Emotes";
    
    private final JPanel emotesPanel;
    private final Emoticons emoteManager;
    
    private final JPanel favoritesPanel;
    private final JPanel myEmotes;
    private final JPanel channelEmotesPanel;
    
    private final CardLayout cardLayout = new CardLayout();
    
    private final JToggleButton favButton = new JToggleButton("Favorites");
    private final JToggleButton channelButton = new JToggleButton("Channel Emotes");
    private final JToggleButton subscriberButton = new JToggleButton("My Emotes");
    
    private final MouseAdapter mouseListener;
    private final ContextMenuListener contextMenuListener;
    
    private JPanel targetPanel;
    private boolean loadFavorites = true;
    private boolean loadMyEmotes = true;
    private boolean loadChannelEmotes = true;
    
    /**
     * GridBagConstraints for adding titles/emotes.
     */
    private final GridBagConstraints gbc;
    
    private Set<Integer> emotesets = new HashSet<>();
    private String stream;
   
    public EmotesDialog(Window owner, Emoticons emotes, final MainGui main, ContextMenuListener contextMenuListener) {
        super(owner);
        
        // TODO: Focusable or maybe just when clicked on emote to insert code?
        this.setFocusable(false);
        this.setFocusableWindowState(false);
        this.contextMenuListener = contextMenuListener;
        this.emoteManager = emotes;
        setResizable(true);

        // Buttons
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(favButton);
        buttonGroup.add(channelButton);
        buttonGroup.add(subscriberButton);

        favButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        channelButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        subscriberButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(favButton);
        buttonPanel.add(subscriberButton);
        buttonPanel.add(channelButton);
        
        add(buttonPanel, BorderLayout.NORTH);

        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == subscriberButton) {
                    showSubemotes();
                } else if (e.getSource() == channelButton) {
                    showChannelEmotes();
                } else if (e.getSource() == favButton) {
                    showFavorites();
                }
            }
        };
        favButton.addActionListener(buttonAction);
        subscriberButton.addActionListener(buttonAction);
        channelButton.addActionListener(buttonAction);
        
        mouseListener = new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.getClickCount() == 2) {
                        setVisible(false);
                    } else {
                        JLabel label = (JLabel) e.getSource();
                        main.insert(Emoticons.toWriteable(label.getToolTipText()), true);
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
            
        };
        
        // Emotes
        myEmotes = new JPanel(new GridBagLayout());
        channelEmotesPanel = new JPanel(new GridBagLayout());
        favoritesPanel = new JPanel(new GridBagLayout());
        
        emotesPanel = new JPanel();
        emotesPanel.setBackground(Color.WHITE);
        emotesPanel.setLayout(cardLayout);
        emotesPanel.add(wrapPanel(favoritesPanel), FAVORITE_EMOTES);
        emotesPanel.add(wrapPanel(myEmotes), MY_EMOTES);
        emotesPanel.add(wrapPanel(channelEmotesPanel), CHANNEL_EMOTES);
        //emotesPanel.setSize(0, 0);
        add(emotesPanel, BorderLayout.CENTER);
        
        gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 0;
        
        pack();
        setMinimumSize(getPreferredSize());
        
        setSize(320,300);
    }
    
    /**
     * Wrap the given component into a JPanel, which aligns it at the top. There
     * may be an easier/more direct way of doing this. Also add it to a scroll
     * pane.
     * 
     * @param panel
     * @return 
     */
    private static JComponent wrapPanel(JComponent panel) {
        panel.setBackground(Color.WHITE);
        JPanel outer = new JPanel();
        outer.setLayout(new GridBagLayout());
        outer.setBackground(Color.WHITE);
        GridBagConstraints gbcTest = new GridBagConstraints();
        gbcTest.fill = GridBagConstraints.HORIZONTAL;
        gbcTest.weightx = 1;
        gbcTest.weighty = 1;
        gbcTest.anchor = GridBagConstraints.NORTH;
        outer.add(panel, gbcTest);
        //outer.setSize(0, 0);
        
        // Add and configure scroll pane
        JScrollPane scroll = new JScrollPane(outer);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }
    
    /**
     * On right-click on an emote, open the appropriate context menu.
     * 
     * @param e 
     */
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Emoticon emote = ((Emote)e.getSource()).emote;
            JPopupMenu m = new EmoteContextMenu(emote, contextMenuListener);
            m.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * Opens the dialog, using the given emotesets and stream.
     *
     * @param emotesets
     * @param stream
     */
    public void showDialog(Set<Integer> emotesets, String stream) {
        if (stream != null && !stream.equals(this.stream)) {
            loadChannelEmotes = true;
        }
        this.stream = stream;
        if (emotesets != null && !emotesets.equals(this.emotesets)) {
            loadChannelEmotes = true;
            loadMyEmotes = true;
            loadFavorites = true;
        }
        this.emotesets = new HashSet<>(emotesets);
        updateTitle();
        showEmotes();
        setVisible(true);
        //update(); // Only for testing if the layouting still works if updated
    }
    
    /**
     * Reloads the current emotes if visible. This can be used if e.g. new
     * emotes have been added.
     */
    public void update() {
        loadFavorites = true;
        loadMyEmotes = true;
        loadChannelEmotes = true;
        if (isVisible()) {
            showEmotes();
        }
    }

    /**
     * Changes the current stream and updates the channel-specific emotes if
     * necessary.
     * 
     * @param stream The name of the stream
     */
    public void updateStream(String stream) {
        if (!isVisible()) {
            return;
        }
        if (stream != null && stream.equals(this.stream)) {
            return;
        }
        this.stream = stream;
        updateTitle();
        loadChannelEmotes = true;
        showEmotes();
    }
    
    /**
     * Updates the emotesets that are used to display the correct subemotes and
     * refreshes the subscriber emotes if necessary.
     *
     * @param emotesets The Set of emotesets
     */
    public void updateEmotesets(Set<Integer> emotesets) {
        if (!isVisible() || emotesets == null || emotesets.equals(this.emotesets)) {
            return;
        }
        this.emotesets = new HashSet<>(emotesets);
        loadMyEmotes = true;
        loadChannelEmotes = true;
        loadFavorites = true;
        showEmotes();
    }
    
    public void favoritesUpdated() {
        loadFavorites = true;
        if (isVisible()) {
            showEmotes();
        }
    }

    /**
     * Sets the title according to the current stream.
     */
    private void updateTitle() {
        if (stream == null) {
            setTitle("Emoticons (Subscriber/Turbo)");
        } else {
            setTitle("Emoticons (Subscriber/Turbo/#"+stream+")");
        }
    }

    /**
     * Shows the selected emotes page (depending on the pressed button),
     * subemotes by default.
     */
    private void showEmotes() {
        if (channelButton.isSelected()) {
            showChannelEmotes();
        } else if (subscriberButton.isSelected()) {
            showSubemotes();
        } else if (favButton.isSelected()) {
            showFavorites();
        } else {
            if (!emoteManager.getFavorites().isEmpty()) {
                favButton.setSelected(true);
                showFavorites();
            } else {
                subscriberButton.setSelected(true);
                showSubemotes();
            }
        }
    }
    
    private void showFavorites() {
        if (loadFavorites) {
            loadFavorites();
        }
        loadFavorites = false;
        cardLayout.show(emotesPanel, FAVORITE_EMOTES);
    }
    
    /**
     * Shows the panel with sub emotes and loads them if necessary.
     */
    private void showSubemotes() {
        if (loadMyEmotes) {
            //System.out.println("Loading subemotes");
            loadSubemotes();
        }
        loadMyEmotes = false;
        cardLayout.show(emotesPanel, MY_EMOTES);
    }
    
    /**
     * Shows the panel with channel emotes and loads them if necessary.
     */
    private void showChannelEmotes() {
        if (loadChannelEmotes) {
            //System.out.println("Loading channelemotes");
            loadChannelEmotes();
        }
        loadChannelEmotes = false;
        cardLayout.show(emotesPanel, CHANNEL_EMOTES);
    }
    
    private void loadFavorites() {
        reset(favoritesPanel);
        Set<Emoticon> emotes = emoteManager.getFavorites();
        if (emotes.isEmpty()) {
            addTitle("You don't have any favorite emotes");
            if (emoteManager.getNumNotFoundFavorites() > 0) {
                addSubtitle("(Emotes may not have been loaded yet.)", false);
            }
        }
        
        // Sort emotes by emoteset
        List<Emoticon> sorted = new ArrayList<>(emotes);
        Collections.sort(sorted, new SortEmotes());

        // Sort out emotes that the user probably doesn't have access to
        List<Emoticon> subEmotesNotSubbedTo = new ArrayList<>();
        for (Emoticon emote : sorted) {
            if (emote.emoteSet != Emoticon.SET_UNDEFINED && !emotesets.contains(emote.emoteSet)) {
                subEmotesNotSubbedTo.add(emote);
            }
        }
        sorted.removeAll(subEmotesNotSubbedTo);
        
        // Add emotes
        addEmotesPanel(sorted);
        if (!subEmotesNotSubbedTo.isEmpty()) {
            addTitle("You need to subscribe to use these emotes:");
            addEmotesPanel(subEmotesNotSubbedTo);
        }
        relayout();
    }
    
    /**
     * Fills the dialog with the subemotes, based on the currently set
     * emotesets.
     */
    private void loadSubemotes() {
        reset(myEmotes);
        if (emotesets.isEmpty()) {
            addTitle("You don't seem to have any sub or turbo emotes");
            if (stream == null) {
                addSubtitle("(Must join a channel for them to be recognized.)", false);
            }
        }
        // Put turbo emotes at the end
        Set<Integer> turboEmotes = new HashSet<>();
        for (Integer emoteset : emotesets) {
            if (Emoticons.isTurboEmoteset(emoteset)) {
                turboEmotes.add(emoteset);
            } else {
                addEmotes(emoteset);
            }
        }
        for (Integer emoteset : turboEmotes) {
            addEmotes(emoteset);
        }
        relayout();
    }
    
    /**
     * Fills the dialog with the emotes specific to the currently set channel,
     * as well as subemotes of that channel if it can be resolved to an
     * emoteset.
     */
    private void loadChannelEmotes() {
        reset(channelEmotesPanel);
        if (stream == null) {
            addTitle("No Channel.");
        } else {
            Set<Emoticon> channelEmotes = emoteManager.getEmoticons(stream);
            if (channelEmotes.isEmpty()) {
                addTitle("No emotes found for #"+stream);
                addSubtitle("No FFZ or BTTV emotes found.", false);
            } else {
                addTitle("Emotes specific to #"+stream);
                addEmotesPanel(channelEmotes);
            }
            int emoteset = emoteManager.getEmotesetFromStream(stream);
            if (emoteset != -1) {
                Set<Emoticon> subEmotes = emoteManager.getEmoticons(emoteset);
                if (!subEmotes.isEmpty()) {
                    addTitle("Subscriber emotes of "+stream+" ("+subEmotes.size()+")");
                    addEmotesPanel(subEmotes);
                    if (!emotesets.contains(emoteset)) {
                        addSubtitle("(Need to be subscribed to use these.)", true);
                    }
                }
            }
        }
        relayout();
    }
    
    /**
     * Clears everything to get ready to add emotes.
     */
    private void reset(JPanel panel) {
        targetPanel = panel;
        panel.removeAll();
        //panel.setSize(1,1);
        //panel.revalidate();
        gbc.gridy = 0;
    }
    
    /**
     * Adds the emotes of the given emoteset. Includes the name of the stream
     * if available.
     * 
     * @param emoteset The emoteset
     */
    private void addEmotes(int emoteset) {
        String stream = emoteManager.getStreamFromEmoteset(emoteset);
        if (stream == null) {
            stream = "-";
        }
        Set<Emoticon> found = emoteManager.getEmoticons(emoteset);
        addTitle(stream+" ["+emoteset+"] ("+found.size()+" emotes)");
        addEmotesPanel(found);
    }
    
    /**
     * Adds a title (label with seperating line).
     * 
     * @param title The text of the title
     */
    private void addTitle(String title) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = TITLE_INSETS;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        targetPanel.add(titleLabel, gbc);
        gbc.gridy++;
    }
    
    /**
     * Adds some centered text without a seperating line and with a slightly
     * subdued color.
     * 
     * @param title The text to add
     * @param smallMargin If true, uses smaller margins (for use below emotes)
     */
    private void addSubtitle(String title, boolean smallMargin) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = smallMargin ? SUBTITLE_INSETS_SMALLER_MARGIN : SUBTITLE_INSETS;
        gbc.anchor = GridBagConstraints.CENTER;
        targetPanel.add(titleLabel, gbc);
        gbc.gridy++;
    }
    
    /**
     * Adds the given emotes to a new panel.
     * 
     * @param emotes The emotes to add
     */
    private void addEmotesPanel(Collection<Emoticon> emotes) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(250,250,250));
        panel.setLayout(new WrapLayout());
        /**
         * Using getParent() twice to get to JScrollPane viewport width, however
         * it still doesn't always seem to work, depending on the width of the
         * dialog. Substracting too much increases the gap in between panels
         * (probably because it layouts the emotes in a narrower but higher
         * panel). Manually resizing the dialog fixes the layout.
         */
        panel.setSize(targetPanel.getParent().getParent().getWidth() - 20, 1);
        //System.out.println(targetPanel.getParent().getParent());
        for (Emoticon emote : emotes) {
            final JLabel label = new Emote(emote, mouseListener);
            panel.add(label);
        }
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = EMOTE_INSETS;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        targetPanel.add(panel, gbc);
        gbc.gridy++;
    }
    
    /**
     * Show layout changes after adding emotes.
     */
    private void relayout() {
        targetPanel.revalidate();
        targetPanel.repaint();
    }
    
    /**
     * A single emote displayed in a JLabel. Saves a reference to the actual
     * Emoticon object, so it can be retrieved when opening the context menu.
     */
    private static class Emote extends JLabel {
        
        private static final Border BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        
        public final Emoticon emote;
        
        public Emote(Emoticon emote, MouseListener mouseListener) {
            this.emote = emote;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(mouseListener);
            setIcon(emote.getIcon(new Emoticon.EmoticonUser() {

                @Override
                public void iconLoaded() {
                    Emote.this.repaint();
                    //EmotesDialog.this.repaint();
                }
            }));
            setToolTipText(emote.code);
            setBorder(BORDER);
            
        }
        
    }
    
    private static class SortEmotes implements Comparator<Emoticon> {

        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            return o1.emoteSet - o2.emoteSet;
        }
        
    }
    
}
