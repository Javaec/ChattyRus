
package chatty.util;

import chatty.Chatty;
import chatty.util.api.Emoticon;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Requests and parses the BTTV emotes.
 * 
 * @author tduva
 */
public class BTTVEmotes {
    
    private static final Logger LOGGER = Logger.getLogger(BTTVEmotes.class.getName());
    
    private static final String URL = "https://cdn.betterttv.net/emotes/emotes.json";
    //private static final String URL = "http://127.0.0.1/twitch/emotes.json";
    
    private static final int CACHE_EXPIRES_AFTER = 60 * 60 * 24;
    private static final String FILE = Chatty.getUserDataDirectory() + "bttvemotes";
    
    private final EmoticonListener listener;
    private final SimpleCache cache;
    private volatile boolean pendingRequest;
    
    public BTTVEmotes(EmoticonListener listener) {
        this.listener = listener;
        this.cache = new SimpleCache("BTTV", FILE, CACHE_EXPIRES_AFTER);
    }
    
    public synchronized void requestEmotes(boolean forcedUpdate) {
        String cached = null;
        if (!forcedUpdate) {
            cached = cache.load();
        }
        if (cached != null) {
            loadEmotes(cached);
        } else {
            requestEmotesFromAPI();
        }
    }
    
    private void requestEmotesFromAPI() {
        if (pendingRequest) {
            return;
        }
        UrlRequest request = new UrlRequest(URL) {

            @Override
            public void requestResult(String result, int responseCode) {
                if (responseCode == 200) {
                    if (loadEmotes(result) > 0) {
                        cache.save(result);
                    }
                }
                pendingRequest = false;
            }
        };
        new Thread(request).start();
        pendingRequest = true;
    }
    
    private int loadEmotes(String json) {
        Set<Emoticon> emotes = parseEmotes(json);
        LOGGER.info("BTTV: Found " + emotes.size() + " emotes");
        listener.receivedEmoticons(emotes);
        return emotes.size();
    }
    
    private Set<Emoticon> parseEmotes(String json) {
        Set<Emoticon> emotes = new HashSet<>();
        if (json == null) {
            return emotes;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONArray root = (JSONArray)parser.parse(json);
            for (Object o : root) {
                if (o instanceof JSONObject) {
                    Emoticon emote = parseEmote((JSONObject)o);
                    if (emote != null) {
                        emotes.add(emote);
                    }
                }
            }
        } catch (ParseException | ClassCastException ex) {
            // ClassCastException is also caught in parseEmote(), so it won't
            // quit completely when one emote is invalid.
            LOGGER.warning("BTTV: Error parsing emotes: "+ex);
        }
        return emotes;
    }
    
    private Emoticon parseEmote(JSONObject o) {
        try {
            String url = (String)o.get("url");
            int width = ((Number)o.get("width")).intValue();
            int height = ((Number)o.get("height")).intValue();
            String code = (String)o.get("regex");
            String stream = (String)o.get("channel");
            
            int emoteset = Emoticon.SET_UNDEFINED;
            Object emoticon_set = o.get("emoticon_set");
            if (emoticon_set != null) {
                if (emoticon_set instanceof String) {
                    // This also includes "night"
                    return null;
                } else {
                    emoteset = ((Number) emoticon_set).intValue();
                }
            }
            //Emoticon emote = new Emoticon(Emoticon.Type.BTTV, code, url, width, height, emoteset);
            Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.BTTV,
                    code, url, width, height);
            builder.setStream(stream);
            builder.setEmoteset(emoteset);
            
            // Adds restrictions to emote (if present)
            Object restriction = o.get("restriction");
            if (restriction != null && restriction instanceof JSONObject) {
                Object channels = ((JSONObject)restriction).get("channels");
                if (channels != null && channels instanceof JSONArray) {
                    // Streams restriction
                    for (Object chan : (JSONArray)channels) {
                        if (chan instanceof String) {
                            builder.addStreamRestriction((String)chan);
                        }
                    }
                } else {
                    // Unknown restriction
                    return null;
                }
            }
            return builder.build();
        } catch (ClassCastException ex) {
            LOGGER.warning("BTTV: Error parsing emote: "+o+" ["+ex+"]");
            return null;
        }
    }
    
}
