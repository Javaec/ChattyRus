
package chatty.gui.components.settings;

import chatty.gui.WindowStateManager;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class WindowSettings extends SettingsPanel {
    
    public WindowSettings(final SettingsDialog d) {
        
        JPanel dialogs = addTitledPanel("Dialogs Location/Size", 0);
        
        dialogs.add(new JLabel("Restore dialogs:"), d.makeGbc(0, 0, 1, 1));
        
        Map<Long, String> restoreModeOptions = new LinkedHashMap<>();
        restoreModeOptions.put((long)WindowStateManager.RESTORE_MAIN, "Open dialogs in default location");
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ALL, "Keep location during session");
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ON_START, "Restore dialogs from last session");
        restoreModeOptions.put((long)WindowStateManager.REOPEN_ON_START, "Reopen dialogs from last session");
        ComboLongSetting restoreMode = new ComboLongSetting(restoreModeOptions);
        d.addLongSetting("restoreMode", restoreMode);
        dialogs.add(restoreMode, d.makeGbc(1, 0, 1, 1));

        JPanel other = addTitledPanel("Other", 1);
        
        other.add(d.addSimpleBooleanSetting("urlPrompt", "Open URL Prompt",
                "Show a prompt to confirm you want to open an URL."),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("chatScrollbarAlways", "Always show chat scrollbar",
                "Always show scrollbar in chat window, even if no scrolling is necessary."),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("minimizeToTray", "Minimize to tray",
                "When minimizing Chatty, it will be hidden to the tray icon."),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("closeToTray", "Close to tray",
                "When closing the window, Chatty will not exit but instead be minimized to the tray icon."),
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        other.add(new JLabel("Tab Order:"), d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        other.add(
                d.addComboStringSetting("tabOrder", 1, false, new String[]{"normal", "alphabetical"}),
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST)
        );

        other.add(new JLabel("Default Userlist Width:"),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        other.add(d.addSimpleLongSetting("userlistWidth", 3, true),
                d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST));
        
        
        
        
        JPanel popout = addTitledPanel("Popout", 2);
        
        popout.add(d.addSimpleBooleanSetting("popoutSaveAttributes", "Restore location/size",
                "Save and restore the location and size of popout dialogs during the same session"),
                d.makeGbc(0,0,1,1));
        popout.add(d.addSimpleBooleanSetting("popoutCloseLastChannel", "Close popout when only channel",
                "Automatically close a popout if the last channel in the main window is closed"),
                d.makeGbc(1, 0, 1, 1));
        
    }
    
}
