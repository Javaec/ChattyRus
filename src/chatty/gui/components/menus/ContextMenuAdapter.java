
package chatty.gui.components.menus;

import chatty.User;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author tduva
 */
public class ContextMenuAdapter implements ContextMenuListener {

    @Override
    public void userMenuItemClicked(ActionEvent e, User user) {

    }

    @Override
    public void urlMenuItemClicked(ActionEvent e, String url) {

    }

    @Override
    public void menuItemClicked(ActionEvent e) {

    }

    @Override
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {

    }

    @Override
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {

    }

    @Override
    public void emoteMenuItemClicked(ActionEvent e, Emoticon emote) {
    }
    
}
