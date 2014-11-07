
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import java.awt.event.ActionEvent;

/**
 * Shows information about the emote that was right-clicked on.
 * 
 * @author tduva
 */
public class EmoteContextMenu extends ContextMenu {
    
    private static Emoticons emoteManager;
    private final ContextMenuListener listener;
    private final Emoticon emote;
    
    public EmoteContextMenu(Emoticon emote, ContextMenuListener listener) {
        this.emote = emote;
        this.listener = listener;

        addItem("code", emote.code);
        addItem("", emote.getWidth()+"x"+emote.getHeight());
        
        // Non-Twitch Emote Information
        if (emote.type != Emoticon.Type.TWITCH) {
            addSeparator();
            if (emote.type == Emoticon.Type.FFZ) {
                addItem("ffzlink", "FrankerFaceZ Emote");
                if (emote.hasStreamSet()) {
                    addItem("", emote.getStream());
                }
            } else if (emote.type == Emoticon.Type.BTTV) {
                addItem("bttvlink", "BetterTTV Emote");
                if (emote.hasStreamSet() && emote.emoteSet == Emoticon.SET_UNDEFINED) {
                    addItem("", emote.getStream());
                }
            }
        }
        
        // Emoteset information
        if (emote.emoteSet != Emoticon.SET_UNDEFINED) {
            addSeparator();
            if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
                addItem("twitchturbolink", "Turbo Emoticon");
            } else {
                addItem("", "Subscriber Emoticon");
                if (emote.hasStreamSet()) {
                    String subMenu = emote.getStream();
                    addItem("profile", "Twitch Profile", subMenu);
                    addItem("join", "Join "+Helper.checkChannel(emote.getStream()), subMenu);
                }
            }
            addItem("", "Emoteset: "+emote.emoteSet);
        }
        
        addSeparator();
        addItem("ignoreEmote", "Ignore");
        if (emote.type == Emoticon.Type.TWITCH) {
            if (emoteManager.isFavorite(emote)) {
                addItem("unfavoriteEmote", "UnFavorite");
            } else {
                addItem("favoriteEmote", "Favorite");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.emoteMenuItemClicked(e, emote);
        }
    }
    
    public static void setEmoteManager(Emoticons emotes) {
        emoteManager = emotes;
    }
    
}
