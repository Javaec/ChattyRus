
package chatty.gui.components;

import chatty.Helper;
import chatty.User;
import chatty.User.Message;
import chatty.User.TextMessage;
import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import chatty.util.DateTime;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class UserInfo extends JDialog {
    
    private static final SimpleDateFormat TIMESTAMP_MESSAGE = new SimpleDateFormat("[HH:mm:ss] ");
    private static final SimpleDateFormat TIMESTAMP_SPECIAL = new SimpleDateFormat("[HH:mm:ss]>");
    
    public static final int ACTION_NONE = 0;
    public static final int ACTION_TIMEOUT = 1;
    public static final int ACTION_BAN = 2;
    public static final int ACTION_UNBAN = 3;
    public static final int ACTION_MOD = 4;
    public static final int ACTION_UNMOD = 5;
    public static final int ACTION_COMMAND = 6;
    
    private final JLabel firstSeen = new JLabel("");
    private final JLabel numberOfLines = new JLabel("");
    private final JTextArea lines = new JTextArea();
    private final JLabel colorInfo = new JLabel("Color: #123456");
    
    private final JButton banButton = new JButton("Ban");
    private final JButton unbanButton = new JButton("Unban");
    private final JButton modButton = new JButton("Mod");
    private final JButton unmodButton = new JButton("Unmod");
    private final HashMap<JButton, Integer> timeoutButtons = new HashMap<>();
    private final HashMap<JButton, String> commandButtons = new HashMap<>();
    private final JButton closeButton = new JButton("Close");
    
    private final JPanel buttonPane;
    private final JPanel buttonPane2;
    private final JPanel infoPane;

    private final ActionListener actionListener;
    
    private User currentUser;
    private String currentLocalUsername;
    
    private float fontSize;
   
    public UserInfo(final MainGui owner, final ContextMenuListener contextMenuListener) {
        super(owner);
        
        actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                if (getAction(e.getSource()) != ACTION_NONE) {
                    owner.getActionListener().actionPerformed(e);
                }
            }
        };
        banButton.addActionListener(actionListener);
        unbanButton.addActionListener(actionListener);
        modButton.addActionListener(actionListener);
        unmodButton.addActionListener(actionListener);
        closeButton.addActionListener(actionListener);

        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;

        buttonPane = new JPanel();
        buttonPane.add(banButton);
        buttonPane.add(unbanButton);
        gbc = makeGbc(0,0,3,1);
        add(buttonPane,gbc);
        
        lines.setEditable(false);
        lines.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(lines);
        scrollPane.setPreferredSize(new Dimension(300,200));
        gbc = makeGbc(0,1,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.9;
        add(scrollPane,gbc);
        
        buttonPane2 = new JPanel();
        gbc = makeGbc(0,2,3,1);
        gbc.insets = new Insets(0,0,0,0);
        add(buttonPane2, gbc);
        
        infoPane = new JPanel();
        gbc = makeGbc(0,3,3,1);
        add(infoPane,gbc);
        
        infoPane.add(numberOfLines);
        infoPane.add(firstSeen);
        infoPane.add(colorInfo);

        gbc = makeGbc(0,4,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,5,3,5);
        add(closeButton,gbc);

        addUserDefinedButtons("30,120,600,1800");
        
        finishDialog();
        
        
        // Open context menu
        this.getContentPane().addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            private void showPopupMenu(MouseEvent e) {
                JPopupMenu menu = new UserContextMenu(currentUser, contextMenuListener);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
    
    public void setFontSize(float size) {
        if (size != fontSize) {
            GuiUtil.setFontSize(size, this);
            pack();
            finishDialog();
        }
        this.fontSize = size;
    }
    
    private void addUserDefinedButtons(String def) {
        updateButtonPane2Margin();
        Pattern p = Pattern.compile("([0-9]+)([smhd]?)|/?/?([^,\\s]+)");
        Matcher m = p.matcher(def);
        while (m.find()) {
            //System.out.println(m.group(1)+" "+m.group(2));
            String match = m.group();
            if (match.startsWith("/")) {
                addCommandButton(m.group(3), match.startsWith("//"));
            } else {
                addTimeoutButton(m.group(), m.group(1), m.group(2));
            }
        }
        updateButtonPane2Margin();
    }
    
    private void updateButtonPane2Margin() {
        FlowLayout layout = (FlowLayout) buttonPane2.getLayout();
        if (buttonPane2.getComponentCount() == 0) {
            layout.setVgap(0);
        } else {
            layout.setVgap(4);
        }
    }
    
    private void addTimeoutButton(String timeAndFactor, String timeString, String factor) {
        try {
            int time = Integer.parseInt(timeString);
            String label;
            if (!factor.isEmpty()) {
                time *= getFactor(factor);
                label = timeAndFactor;
            } else {
                label = timeFormat(time);
            }
            JButton button = new JButton(label);
            buttonPane.add(button);
            timeoutButtons.put(button, time);
            button.addActionListener(actionListener);
            button.setToolTipText("Timeout for " + time + " seconds");
        } catch (NumberFormatException ex) {
            // Shouldn't happen, but if it does just don't add the button
        }
    }
    
    private void addCommandButton(String command, boolean secondMenu) {
        JButton button = new JButton(Helper.replaceUnderscoreWithSpace(command));
        if (secondMenu) {
            button.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            buttonPane2.add(button);
        } else {
            buttonPane.add(button);
        }
        commandButtons.put(button, command);
        button.addActionListener(actionListener);
        button.setToolTipText("Custom command");
    }
    
    private int getFactor(String factorString) {
        switch (factorString) {
            case "s": return 1;
            case "m": return 60;
            case "h": return 60*60;
            case "d": return 60*60*24;
            default: return 1;
        }
    }
    
    /**
     * Remove current custom created buttons from the button panes.
     */
    private void removeUserDefinedButtons() {
        for (JButton button : commandButtons.keySet()) {
            buttonPane.remove(button);
            buttonPane2.remove(button);
        }
        commandButtons.clear();
        for (JButton button : timeoutButtons.keySet()) {
            buttonPane.remove(button);
        }
        timeoutButtons.clear();
    }
    
    /**
     * Sets the new custom buttons definition, which is just a single String
     * that is parsed accordingly. Removes current buttons, then adds the new
     * ones, resizing the dialog if necessary.
     * 
     * @param def 
     */
    public void setUserDefinedButtonsDef(String def) {
        removeUserDefinedButtons();
        addUserDefinedButtons(def);
        updateModButtons();
        GuiUtil.setFontSize(fontSize, this);
        // Pack because otherwise the dialog won't be sized correctly when
        // displaying it for the first time (not sure why)
        pack();
        finishDialog();
    }
    
    private void finishDialog() {
        setMinimumSize(getPreferredSize());
    }
    
    private String timeFormat(int seconds) {
        if (seconds < 60) {
            return seconds+"s";
        }
        if (seconds < 60*60) {
            int minutes = seconds / 60;
            return String.format("%dm", (int) minutes);
        }
        if (seconds < 60*60*24*2+1) {
            return String.format("%dh", seconds / (60*60));
        }
        return String.format("%dd", seconds / (60*60*24));
    }
    
    public boolean isTimeoutButton(Object button) {
        return timeoutButtons.containsKey(button);
    }
    
    public Integer getTimeoutButtonTime(Object button) {
        return timeoutButtons.get(button);
    }
    
    public boolean isCommandButton(Object button) {
        return commandButtons.containsKey(button);
    }
    
    public String getCommandButtonCommand(Object button) {
        return commandButtons.get(button);
    }
    
    public JButton getBanButton() {
        return banButton;
    }
    
    public JButton getUnbanButton() {
        return unbanButton;
    }
    
    public int getAction(Object source) {
        if (source == banButton) {
            return ACTION_BAN;
        } else if (source == unbanButton) {
            return ACTION_UNBAN;
        } else if (source == modButton) {
            return ACTION_MOD;
        } else if (source == unmodButton) {
            return ACTION_UNMOD;
        } else if (isTimeoutButton(source)) {
            return ACTION_TIMEOUT;
        } else if (isCommandButton(source)) {
            return ACTION_COMMAND;
        }
        return ACTION_NONE;
    }
    
    public void setUser(User user, String localUsername) {
        currentUser = user;
        currentLocalUsername = localUsername;
        
        //infoPane.setComponentPopupMenu(new UserContextMenu(user, contextMenuListener));
        
        String categoriesString = "";
        Set<String> categories = user.getCategories();
        if (categories != null && !categories.isEmpty()) {
            categoriesString = categories.toString();
        }
        String displayNickInfo = user.hasDisplayNickSet() ? "" : "*";
        this.setTitle("User: "+user.toString()+displayNickInfo+" / "+user.getChannel()+" "+categoriesString);
        lines.setText(null);
        lines.setText(makeLines());
        firstSeen.setText(" First seen: "+DateTime.format(user.getCreatedAt()));
        numberOfLines.setText(" Number of messages: "+user.getNumberOfMessages());
        updateColor();
        updateModButtons();
        finishDialog();
    }
    
    private void updateModButtons() {
        if (currentUser == null) {
            return;
        }
        buttonPane.remove(modButton);
        buttonPane.remove(unmodButton);
        // Check that local user is the streamer here
        if (currentUser.getStream().equalsIgnoreCase(currentLocalUsername)) {
            if (currentUser.isModerator()) {
                buttonPane.add(unmodButton);
            } else {
                buttonPane.add(modButton);
            }
        }
    }
    
    private void updateColor() {
        Color color = currentUser.getColor();
        Color correctedColor = currentUser.getCorrectedColor();
        
        String colorNamed = HtmlColors.getNamedColorString(color);
        String correctedColorNamed = HtmlColors.getNamedColorString(correctedColor);
        
        String colorCode = HtmlColors.getColorString(color);
        String correctedColorCode = HtmlColors.getColorString(correctedColor);
        
        String colorText;
        String colorTooltipText;

        if (currentUser.hasChangedColor()) {
            Color plainColor = currentUser.getPlainColor();
            colorText = "Color: "+colorNamed+"**";
            colorTooltipText = "Custom Color: "+colorCode
                    +" (Original: "+HtmlColors.getNamedColorString(plainColor)+"/"
                    + HtmlColors.getColorString(plainColor)+")";
        } else if (currentUser.hasDefaultColor()) {
            colorText = "Color: "+colorNamed+"*";
            colorTooltipText = "Color: "+colorCode+" (default)";
        } else if (currentUser.hasCorrectedColor() && !colorCode.equals(correctedColorCode)) {
            colorText = "Color: "+correctedColorNamed+" ("+colorNamed+")";
            colorTooltipText = "Corrected Color: "+correctedColorCode
                    +" (Original: "+colorNamed+"/"+colorCode+")";
        } else {
            colorText = "Color: "+colorNamed;
            colorTooltipText = "Color: "+colorCode;
        }
        colorInfo.setText(colorText);
        colorInfo.setToolTipText(colorTooltipText);
    }
    
    private String makeLines() {
        StringBuilder b = new StringBuilder();
        if (currentUser.maxNumberOfLinesReached()) {
            b.append("<only last ");
            b.append(currentUser.getMaxNumberOfLines());
            b.append(" lines are saved>\n");
        }
        List<Message> messages = currentUser.getMessages();
        for (Message m : messages) {
            if (m.getType() == Message.MESSAGE) {
                b.append(DateTime.format(m.getTime(), TIMESTAMP_MESSAGE));
                b.append(((TextMessage)m).getText());
                b.append("\n");
            }
            else if (m.getType() == Message.BAN) {
                b.append(DateTime.format(m.getTime(), TIMESTAMP_SPECIAL));
                b.append("Banned from talking");
                b.append("\n");
            }
        }
        return b.toString();
    }
    
    public void show(Component owner, User user, String localUsername) {
        setUser(user, localUsername);
        setLocationRelativeTo(owner);
        setVisible(true);
    }
    
    /**
     * Update sets the dialog to the given User, but only if the dialog is open
     * and it's the same User as the currently set User. This allows for chat
     * events that would need to update this to call this with any User.
     * 
     * @param user
     * @param localUsername 
     */
    public void update(User user, String localUsername) {
        if (currentUser == user && isVisible()) {
            setUser(user, localUsername);
        }
    }
    
    public User getUser() {
        return currentUser;
    }
    
    public String getChannel() {
        return currentUser.getChannel();
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(2,2,2,2);
        return gbc;
    }
}
