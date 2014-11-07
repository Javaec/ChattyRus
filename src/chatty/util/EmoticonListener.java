
package chatty.util;

import chatty.util.api.Emoticon;
import java.util.List;
import java.util.Set;

/**
 *
 * @author tduva
 */
public interface EmoticonListener {
    void receivedEmoticons(Set<Emoticon> emoticons);
}
