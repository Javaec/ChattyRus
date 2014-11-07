
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class MainSettings extends SettingsPanel implements ActionListener {
    
    private final JButton selectFontButton = new JButton("Выбрать шрифт");
    private final FontChooser fontChooser;
    private final ComboLongSetting onStart;
    private final JTextField channels;
    private final SettingsDialog d;
    
    public MainSettings(SettingsDialog d) {
        
        fontChooser = new FontChooser(d);
        this.d = d;
        
        GridBagConstraints gbc;
        
        JPanel fontSettingsPanel = addTitledPanel("Шрифт", 0);
        JPanel startSettingsPanel = addTitledPanel("Запуск", 1);
        
        /*
         * Font settings (Panel)
         */
        // Font Name
        gbc = d.makeGbc(0,0,1,1);
        fontSettingsPanel.add(new JLabel("Название шрифта:"),gbc);
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        fontSettingsPanel.add(new JLabel("Размер шрифта:"),gbc);
        
        // Font Size
        gbc = d.makeGbc(1,0,1,1);
        SimpleStringSetting fontSetting = new SimpleStringSetting(15, false);
        d.addStringSetting("font", fontSetting);
        fontSettingsPanel.add(fontSetting ,gbc);
        gbc = d.makeGbc(1,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(d.addSimpleLongSetting("fontSize",7,false),gbc);
        
        // Select Font button
        selectFontButton.addActionListener(this);
        gbc = d.makeGbc(2,0,1,1);
        fontSettingsPanel.add(selectFontButton,gbc);
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel("При запуске:"), gbc);
        
        Map<Long, String> onStartDef = new LinkedHashMap<>();
        onStartDef.put((long)0, "Ничего не делать");
        onStartDef.put((long)1, "Открыть окно подключения");
        onStartDef.put((long)2, "Подключиться к определенным каналам");
        onStartDef.put((long)3, "Подключиться к предыдущим каналам");
        onStartDef.put((long)4, "Подключиться к избранным каналам");
        onStart = new ComboLongSetting(onStartDef);
        onStart.addActionListener(this);
        d.addLongSetting("onStart", onStart);
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(onStart, gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel("Каналы:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        channels = d.addSimpleStringSetting("autojoinChannel", 25, true);
        startSettingsPanel.add(channels, gbc);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFontButton) {
            String font = d.getStringSetting("font");
            int fontSize = d.getLongSetting("fontSize").intValue();
            int result = fontChooser.showDialog(font, fontSize);
            if (result == FontChooser.ACTION_OK) {
                d.setStringSetting("font", fontChooser.getFontName());
                d.setLongSetting("fontSize", fontChooser.getFontSize().longValue());
            }
        } else if (e.getSource() == onStart) {
            boolean channelsEnabled = onStart.getSettingValue().equals(Long.valueOf(2));
            channels.setEnabled(channelsEnabled);
        }
    }
    
}
