
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Helper;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class LogSettings extends SettingsPanel {
    
    private final JLabel info;
    private final ComboStringSetting modeSetting;
    private final CardLayout cardManager;
    private final JPanel cards;
    
    public LogSettings(final SettingsDialog d) {
        
        GridBagConstraints gbc;
        
        JPanel mode = createTitledPanel("Channels to log to file");
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 0.4;
        mode.add(new JLabel("Logging Mode: "), gbc);
        
        modeSetting = d.addComboStringSetting("logMode", 1, false, new String[]{"always", "blacklist", "whitelist", "off"});
        modeSetting.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        gbc.weightx = 0.6;
        mode.add(modeSetting, gbc);
        
        // Lists
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.setPreferredSize(new Dimension(220,150));
        
        final ChannelFormatter formatter = new ChannelFormatter();
        ListSelector whitelist = d.addListSetting("logWhitelist", 1, 1, true);
        whitelist.setDataFormatter(formatter);
        ListSelector blacklist = d.addListSetting("logBlacklist", 1, 1, true);
        blacklist.setDataFormatter(formatter);
        
        cards.add(whitelist, "whitelist");
        cards.add(blacklist, "blacklist");
        JPanel empty = new JPanel(new GridBagLayout());
        JLabel emptyLabel = new JLabel("<No List in this mode>");
        emptyLabel.setForeground(Color.gray);
        empty.add(emptyLabel, d.makeGbc(0,0,1,1));
        cards.add(empty, "none");
        
        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.insets = new Insets(5,10,5,5);
        mode.add(cards, gbc);
        
        // Info Text
        info = new JLabel();
        gbc = d.makeGbc(0, 2, 3, 1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        mode.add(info, gbc);
        
        JPanel types = createTitledPanel("Message Types");
        
        types.add(d.addSimpleBooleanSetting("logInfo", "Chat Info",
                "Log infos like stream title, messages from twitch, connecting, disconnecting."), d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.NORTHWEST));
        types.add(d.addSimpleBooleanSetting("logBan", "Bans/Timeouts",
                "Log Bans/Timeouts as BAN messages."), d.makeGbcCloser(0, 1, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting("logMod", "Mod/Unmod",
                "Log MOD/UNMOD messages."), d.makeGbcCloser(0, 2, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting("logJoinPart", "Joins/Parts",
                "Log JOIN/PART messages."), d.makeGbcCloser(0, 3, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting("logSystem", "System Info",
                "Messages that concern Chatty rather than chat."), d.makeGbcCloser(0, 4, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting("logViewerstats", "Viewerstats",
                "Log viewercount stats in a semi-regular interval."), d.makeGbcCloser(0, 5, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting("logViewercount", "Viewercount",
                "Log the viewercount as it is updated."), d.makeGbcCloser(0, 6, 1, 1, GridBagConstraints.WEST));


        JPanel directory = createTitledPanel("Logs Folder");
        
        PathSetting logPath = new PathSetting(d, Chatty.getUserDataDirectory()+"logs");
        d.addStringSetting("logPath", logPath);
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        directory.add(logPath, gbc);
        
        gbc = getGbc(0);
        gbc.anchor = GridBagConstraints.NORTH;
        addPanel(mode, gbc);
        
        gbc = getGbc(0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 1;
        addPanel(types, gbc);
        
        gbc = getGbc(2);
        gbc.gridwidth = 2;
        addPanel(directory, gbc);
        
        update();
    }
    
    private void update() {
        String mode = modeSetting.getSettingValue();
        String infoText = "";
        String switchTo = "none";
        switch (mode) {
            case "off":
                infoText = "Nothing is logged.";
                switchTo = "none";
                break;
            case "always":
                infoText = "All channels are logged.";
                switchTo = "none";
                break;
            case "blacklist":
                infoText = "All channels but those on the list are logged.";
                switchTo = "blacklist";
                break;
            case "whitelist":
                infoText = "Only the channels on the list are logged.";
                switchTo = "whitelist";
                break;
        }
        info.setText("<html><body style='width: 200px;text-align:center;'>"+infoText);
        cardManager.show(cards, switchTo);
    }
    
    private static class ChannelFormatter implements DataFormatter<String> {

        /**
         * Prepends the input with a "#" if not already present. Returns
         * {@code null} if the length after prepending is only 1, which means
         * it only consists of the "#" and is invalid.
         * 
         * @param input The input to be formatted
         * @return The formatted input, which has the "#" prepended, or
         * {@code null} or any empty String if the input was invalid
         */
        @Override
        public String format(String input) {
            if (input != null && !input.isEmpty() && !input.startsWith("#")) {
                input = "#"+input;
            }
            if (input.length() == 1) {
                input = null;
            }
            return input;
        }
        
    }
    
}
