
package chatty.util.api;

import chatty.Usericon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface definition for API response results.
 * 
 * @author tduva
 */
public interface TwitchApiResultListener {
    void receivedEmoticons(Set<Emoticon> emoticons);
    void receivedUsericons(List<Usericon> icons);
    void gameSearchResult(Set<String> games);
    void tokenVerified(String token, TokenInfo tokenInfo);
    void runCommercialResult(String stream, String text, int result);
    void putChannelInfoResult(int result);
    void receivedChannelInfo(String channel, ChannelInfo info, int result);
    void accessDenied();
    void receivedFollowers(FollowerInfo followerInfo);
    void newFollowers(FollowerInfo followerInfo);
    void receivedSubscribers(FollowerInfo info);
    
    /**
     * The correctly capitalized name for a user.
     * 
     * @param name All-lowercase name
     * @param displayName Correctly capitalized name
     */
    void receivedDisplayName(String name, String displayName);
}
