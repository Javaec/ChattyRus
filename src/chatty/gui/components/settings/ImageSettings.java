
package chatty.gui.components.settings;

import chatty.Usericon;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ImageSettings extends SettingsPanel {
    
    private final UsericonEditor usericonsData;
    
    public ImageSettings(SettingsDialog d) {
        
        GridBagConstraints gbc;
        
        JPanel usericons = addTitledPanel("Usericon Settings (Badges)", 0);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        usericons.add(d.addSimpleBooleanSetting("customUsericonsEnabled", "Enable Custom Usericons", ""), gbc);
        
                gbc = d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(d.addSimpleBooleanSetting("usericonsEnabled","Show Usericons",
                "Show mod/turbo/.. as icons. Changing this only affects new lines."),
                gbc);
        
        usericonsData = new UsericonEditor(d);
        usericonsData.setPreferredSize(new Dimension(150, 250));
        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        usericons.add(usericonsData, gbc);
    }
    
    public void setData(List<Usericon> data) {
        usericonsData.setData(data);
    }
    
    public List<Usericon> getData() {
        return usericonsData.getData();
    }
    
}
