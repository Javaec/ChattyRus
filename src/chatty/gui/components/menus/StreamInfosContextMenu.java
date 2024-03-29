
package chatty.gui.components.menus;

import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class StreamInfosContextMenu extends ContextMenu {

    private final List<StreamInfo> streamInfos;
    
    private static final String SORT_SUBMENU = "Sort by..";
    
    public StreamInfosContextMenu(List<StreamInfo> selected, boolean liveStreams) {
        this.streamInfos = selected;
        List<String> streams = new ArrayList<>();
        for (StreamInfo info : selected) {
            streams.add(info.getStream());
        }
        
        if (!selected.isEmpty()) {
            addItem("openChannelInfo", "Info: "+selected.get(0).getStream());
            addSeparator();
            
            ContextMenuHelper.addStreamsOptions(this, streams.size());
            
            
            if (liveStreams) {
                addSeparator();
            }
        }
        if (liveStreams) {
            addItem("sortRecent", "Recent", SORT_SUBMENU);
            addItem("sortName", "Name", SORT_SUBMENU);
            addItem("sortGame", "Game", SORT_SUBMENU);
            addItem("sortViewers", "Viewers", SORT_SUBMENU);
            addItem("showRemovedList", "Removed Streams..");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.streamInfosMenuItemClicked(e, streamInfos);
        }
    }
    
}
