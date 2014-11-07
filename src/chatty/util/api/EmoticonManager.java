
package chatty.util.api;

import chatty.Chatty;
import chatty.util.SimpleCache;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class EmoticonManager {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonManager.class.getName());
    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    private static final String FILE = Chatty.getUserDataDirectory()+"emoticons";
    
    private final SimpleCache cache;
    private final TwitchApiResultListener listener;
    
    public EmoticonManager(TwitchApiResultListener listener) {
        this.listener = listener;
        this.cache = new SimpleCache("emoticons", FILE, CACHED_EMOTICONS_EXPIRE_AFTER);
    }
    
    /**
     * Tries to read the emoticons from file.
     * 
     * @return true if emoticons were loaded, false if emoticons should be
     *  requested from the API.
     */
    protected boolean loadEmoticons() {
        String fromFile = loadEmoticonsFromFile();
        if (fromFile != null) {
            Set<Emoticon> parsed = parseEmoticons(fromFile);
            if (parsed == null) {
                return false;
            }
            listener.receivedEmoticons(parsed);
            LOGGER.info("Using emoticons list from file.");
            return true;
        }
        return false;
    }
    
    protected void emoticonsReceived(String result) {
        Set<Emoticon> parsed = parseEmoticons(result);
        if (parsed != null) {
            if (!parsed.isEmpty()) {
                saveEmoticonsToFile(result);
            }
            if (listener != null) {
                listener.receivedEmoticons(parsed);
            }
        }
    }
    
    /**
     * Saves the given json text (which should be the list of emoticons as
     * received from the Twitch API v2) into a file.
     *
     * @param json
     */
    private void saveEmoticonsToFile(String json) {
        cache.save(json);
    }
    
    /**
     * Loads emoticons list from the file.
     * 
     * @return The json as received from the Twitch API v2 or null if the file
     * isn't recent enough or an error occured
     */
    private String loadEmoticonsFromFile() {
        return cache.load();
    }
    
    /**
     * Parses the emoticon list returned from the TwitchAPI.
     * 
     * @param json
     * @return 
     */
    private Set<Emoticon> parseEmoticons(String json) {
        
        Set<Emoticon> result = new HashSet<>();
        if (json == null) {
            LOGGER.warning("Error parsing emoticons (null).");
            return result;
        }
        JSONParser parser = new JSONParser();
        JSONObject root = null;
        try {
            root = (JSONObject) parser.parse(json);
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing emoticons: "+ex);
            return null;
        }

        Object obj = root.get("emoticons");
        if (!(obj instanceof JSONArray)) {
            LOGGER.warning("Error parsing emoticons, should be Array.");
            return null;
        }
        
        // Go through all emoticons
        JSONArray emoticonsArray = (JSONArray) obj;
        Iterator emoticonsIterator = emoticonsArray.iterator();
        while (emoticonsIterator.hasNext()) {
            Object element = emoticonsIterator.next();
            if (element instanceof JSONObject) {
                JSONObject emoticon = (JSONObject) element;
                parseEmoticon(result, emoticon);
            }
        }
        return result;
    }
    
    /**
     * Parse a single emoticon.
     * 
     * @param result
     * @param emoticon 
     */
    private void parseEmoticon(Set<Emoticon> result,
            JSONObject emoticon) {
        // Get smiley text
        Object regexObj = emoticon.get("regex");
        Object imagesObj = emoticon.get("images");
        if (!(regexObj instanceof String) || regexObj == null) {
            LOGGER.warning("Error parsing emoticon regex, should be String");
        } else if (!(imagesObj instanceof JSONArray) || imagesObj == null) {
            LOGGER.warning("Error parsing emoticon images, should be Array");
        } else {
            String regex = (String)regexObj;
            // Get image url
            JSONArray images = (JSONArray)imagesObj;
            Iterator imagesIt = images.iterator();
            while (imagesIt.hasNext()) {
                JSONObject image = (JSONObject) imagesIt.next();
                try {
                    parseImage(result, image, regex);
                } catch (NullPointerException ex) {
                    LOGGER.warning("Error parsing image for emoticon '"+regex+"'");
                } catch (ClassCastException ex) {
                    LOGGER.warning("Error parsing image for emoticon '"+regex+
                            "' ("+ex.getLocalizedMessage()+")");
                }
            }
        }
    }
    
    /**
     * Parse a single image of an emoticon. Directly saves the icon into the
     * Map of emotesets.
     * 
     * @param result The Map of emotesets this image should be save into
     * @param image The JSONObject that directly contains the image
     * @param regex The pattern for this emoticon
     */
    private void parseImage(Set<Emoticon> result,
            JSONObject image, String regex) {
        
        int emoticonSet = Emoticon.SET_UNDEFINED;
        if (image.get("emoticon_set") != null) {
            emoticonSet = ((Number) image.get("emoticon_set")).intValue();
        }
        String url = (String) image.get("url");
        int height = ((Number) image.get("height")).intValue();
        int width = ((Number) image.get("width")).intValue();
        
        Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.TWITCH,
                regex, url, width, height);
        builder.setEmoteset(emoticonSet);
        result.add(builder.build());
//        Emoticon emoticon = new Emoticon(Emoticon.Type.TWITCH, regex,url,width,height,
//                emoticonSet);
//        
//        result.add(emoticon);
    }
    
}
