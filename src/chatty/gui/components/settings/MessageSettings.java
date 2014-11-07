
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.util.DateTime;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class MessageSettings extends SettingsPanel {
    
    private final Map<String,String> timestampOptions = new LinkedHashMap<>();
    
    public MessageSettings(final SettingsDialog d) {

        GridBagConstraints gbc;

        JPanel timeoutSettingsPanel = addTitledPanel("Deleted Messages (Timeouts/Bans)", 0);
        JPanel otherSettingsPanel = addTitledPanel("Other", 1);

        /*
         * Other settings (Panel)
         */
        // Timestamp
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(new JLabel("Timestamp: "), gbc);

        gbc = d.makeGbc(1, 0, 1, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        String[] options = new String[]{"off", "[HH:mm:ss]", "[HH:mm]"};
//        otherSettingsPanel.add(
//                d.addComboStringSetting("timestamp", 15, false, options),
//                gbc);
        

        addTimestampFormat("off");
        addTimestampFormat("[HH:mm:ss]");
        addTimestampFormat("[HH:mm]");
        addTimestampFormat("[hh:mm:ss a]");
        addTimestampFormat("[hh:mm a]");
        addTimestampFormat("[h:mm a]");
        addTimestampFormat("[hh:mm:ssa]");
        addTimestampFormat("[hh:mma]");
        addTimestampFormat("[h:mma]");
        ComboStringSetting combo = new ComboStringSetting(timestampOptions);
        combo.setEditable(false);
        d.addStringSetting("timestamp", combo);
        otherSettingsPanel.add(combo, gbc);
        
        final JDialog capitalizedNamesSettingsDialog = new CapitalizedNamesSettings(d);
        JButton openCapitalizedNamesSettingsDialogButton = new JButton("Name Capitalization Settings..");
        openCapitalizedNamesSettingsDialogButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        openCapitalizedNamesSettingsDialogButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                capitalizedNamesSettingsDialog.setLocationRelativeTo(d);
                capitalizedNamesSettingsDialog.setVisible(true);
            }
        });
        otherSettingsPanel.add(openCapitalizedNamesSettingsDialogButton, d.makeGbc(2, 0, 2, 1));
        

//        gbc = d.makeGbc(0, 3, 2, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        otherSettingsPanel.add(
//                ,
//                gbc);
        
//        gbc = d.makeGbc(2, 3, 2, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        otherSettingsPanel.add(
//                ,
//                gbc);

        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addSimpleBooleanSetting("showModMessages", "Show mod/unmod messages",
                        "Whether to show when someone was modded/unmodded."),
                gbc);

        gbc = d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addSimpleBooleanSetting("printStreamStatus", "Show stream status in chat",
                "Output stream status when you join a channel and when it changes"), gbc);
        
                        
        gbc = d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addSimpleBooleanSetting("actionColored", "/me messages colored",
                "If enabled, action messages (/me) have the same color as the nick"), gbc);
        
                gbc = d.makeGbc(2, 2, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addSimpleBooleanSetting("removeCombiningCharacters", "Filter combining characters",
                "Tries to filter out combining characters that is used to create vertical text in some languages (may prevent errors)"), gbc);

        /**
         * Timeout settings
         */
        gbc = d.makeGbc(0, 0, 2, 1);
        DeletedMessagesModeSetting deletedMessagesModeSetting = new DeletedMessagesModeSetting(d);
        timeoutSettingsPanel.add(deletedMessagesModeSetting, gbc);

        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(
                d.addSimpleBooleanSetting("showBanMessages", "Show ban messages",
                        "If enabled, shows '<user> has been banned from talking' for bans/timeouts"),
                gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(
                d.addSimpleBooleanSetting("combineBanMessages", "Combine ban messages",
                        "If enabled, combines ban messages for the same user into one, appending the number of bans"),
                gbc);

    }
    
    private void addTimestampFormat(String format) {
        String label = format;
        if (!format.equals("off")) {
            int hour = DateTime.currentHour12Hour();
            if (hour > 0 && hour < 10) {
                label = DateTime.currentTime(format);
            } else {
                label = DateTime.format(System.currentTimeMillis() - 4*60*60*1000, new SimpleDateFormat(format));
            }
        }
        timestampOptions.put(format, label);
    }
    
    private static class CapitalizedNamesSettings extends JDialog {

        private static final String INFO0 = "<html><body style='width:300px;'>"
                + "Names in Twitch Chat are send all-lowercase by default, "
                + "the following options can be used to change capitalization:";
        
        private static final String INFO1 = "<html><body style='width:280px;'>"
                + "If enabled, simply makes the first letter of the name uppercase by "
                + "default.";
        
        private static final String INFO2 = "<html><body style='width:280px;'>"
                + "If enabled, requests the correct capitalization for names "
                + "from the Twitch API. To prevent spamming the API too much, "
                + "names are requested rather carefully (prioritized by chat activity) "
                + "and cached locally for a long time, which means capitalization "
                + "will not always be correct.<br /><br />"
                + "This may negatively affect performance and it is a rather experimental feature, "
                + "so only use it if you really want capitalized names. [help:capitalization More information..]";
        
        public CapitalizedNamesSettings(SettingsDialog d) {
            super(d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Name Capitalization Settings");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            gbc.insets = new Insets(5, 5, 5, 5);
            add(new JLabel(INFO0), gbc);
            
            add(d.addSimpleBooleanSetting("capitalizedNames", "Capitalized Names (First Letter)",
                        "Requires a restart of Chatty to have any effect."),
                    d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
            
            gbc = d.makeGbc(0, 2, 1, 1);
            gbc.insets = new Insets(0, 30, 5, 10);
            add(new JLabel(INFO1), gbc);
            
            add(d.addSimpleBooleanSetting("correctlyCapitalizedNames", "Correctly Capitalized Names",
                        "This may have a negative impact on performance. Requires a restart of Chatty to have any effect."),
                    d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
            
            gbc = d.makeGbc(0, 4, 1, 1);
            gbc.insets = new Insets(0, 30, 5, 10);
            add(new LinkLabel(INFO2, d.getSettingsHelpLinkLabelListener()), gbc);
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            gbc = d.makeGbc(0, 5, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(closeButton, gbc);
            
            pack();
        }
        
    }
    
}
