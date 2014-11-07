
package chatty;

import java.util.logging.Logger;

/**
 * Initialize the actual HotkeySetter, if enabled.
 * 
 * @author tduva
 */
public class HotkeyManager {
    
    private static final Logger LOGGER = Logger.getLogger(HotkeyManager.class.getName());
    
    private HotkeySetter setter;
    
    public HotkeyManager(TwitchClient client, boolean enabled) {
        if (enabled) {
            try {
                setter = new HotkeySetter(client);
            } catch (NoClassDefFoundError ex) {
                LOGGER.warning("Failed to initialize hotkey setter ["+ex+"]");
                client.warning("Failed to initialize hotkey setter (if you don't"
                        + " run commercials via hotkey you can just ignore this).");
                setter = null;
            }
        }
    }

    public void setCommercialHotkey(String hotkey) {
        if (setter != null) {
            setter.setCommercialHotkey(hotkey);
        }
    }

}
