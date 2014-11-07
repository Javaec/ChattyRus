
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Settings that should only be changed if you know what you're doing, includes
 * a warning about that.
 * 
 * @author tduva
 */
public class AdvancedSettings extends SettingsPanel {
    
    public AdvancedSettings(SettingsDialog d) {

        JPanel warning = new JPanel();
        
        warning.add(new JLabel("<html><body style='width:300px'>"
                + "These settings can break Chatty if you change them, "
                + "so you should only change these settings if you "
                + "know what you are doing."));
        
        addPanel(warning, getGbc(0));
        
        JPanel connection = addTitledPanel("Connection", 1);

        connection.add(new JLabel("Server:"), d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("serverDefault", 20, true), d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("Port:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("portDefault", 10, true), d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("(These might be overridden by commandline parameters.)"), d.makeGbc(0, 2, 2, 1));
        
        JPanel other = addTitledPanel("Other", 2);

        final TcSelector tcSelector = new TcSelector(d);
        d.addBooleanSetting("tc3", tcSelector);
        
        final JButton tc = new JButton("Show/Change Twitch Client Version (TC1/TC3)");
        tc.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        tc.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tcSelector.showSelector();
            }
        });
        
        other.add(tc, d.makeGbc(0, 0, 1, 1));
        other.add(new JLabel("(affects joins/parts, userlist, info messages, sub detection ..)"), d.makeGbc(0, 1, 1, 1));
    }
    
    private static class TcSelector implements BooleanSetting {

        private static final String INFO = "<html><body style='width: 300px'>"
                + "This setting changes how the Twitch Chat Server behaves. You"
                + " have to reconnect for changes to take affect.";
        
        private static final String INFO_TC1 = "<html><body>"
                + "<ul style='margin-top:0;'>"
                + "<li>may show timeouts/info messages in wrong channel</li>"
                + "<li>no \"x is now hosting you\" notifications</li>"
                + "<li>accurate userlist</li>"
                + "<li>has joins/parts (you don't <em>have</em> to <em>show</em> them)</li>"
                + "</ul>";
        
        private static final String INFO_TC3 = "<html><body>"
                + "<ul style='margin-top:0;'>"
                + "<li>precise timeouts/info messages</li>"
                + "<li>\"x is now hosting you\" notifications</li>"
                + "<li>inaccurate userlist</li>"
                + "<li>no joins/parts</li>"
                + "</ul>";
        
        private final JDialog dialog;
        private final JRadioButton tc1;
        private final JRadioButton tc3;
        private final JButton close = new JButton("Close");
        private final Window owner;
        
        public TcSelector(Window owner) {
            this.owner = owner;
            dialog = new JDialog(owner);
            dialog.setResizable(false);
            dialog.setTitle("Twitch Client Version");
            
            ButtonGroup group = new ButtonGroup();
            tc1 = new JRadioButton("Twitch Client Version 1 (TC1)");
            tc3 = new JRadioButton("Twitch Client Version 3 (TC3)");
            group.add(tc1);
            group.add(tc3);
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(8, 8, 5, 8);
            dialog.add(new JLabel(INFO), gbc);

            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
            dialog.add(tc1, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(0, 5, 0, 5);
            dialog.add(new JLabel(INFO_TC1), gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST);
            dialog.add(tc3, gbc);
            
            gbc = GuiUtil.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(0, 5, 0, 5);
            dialog.add(new JLabel(INFO_TC3), gbc);
            
            gbc = GuiUtil.makeGbc(0, 5, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(close, gbc);
            
            close.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            
            dialog.pack();
        }
        
        @Override
        public Boolean getSettingValue() {
            return tc3.isSelected();
        }

        @Override
        public void setSettingValue(Boolean value) {
            tc1.setSelected(!value);
            tc3.setSelected(value);
        }
        
        public void showSelector() {
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
        }
        
    }
    
}
