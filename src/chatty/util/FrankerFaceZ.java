
package chatty.util;

import chatty.Helper;
import chatty.Usericon;
import chatty.util.api.Emoticon;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request FrankerFaceZ emoticons and mod icons.
 * 
 * @author tduva
 */
public class FrankerFaceZ {
    
    private static final Logger LOGGER = Logger.getLogger(FrankerFaceZ.class.getName());
    
    //private static final String BASE_URL = "http://frankerfacez.storage.googleapis.com/";
    //private static final String BASE_URL = "http://127.0.0.1/twitch/ffz";
    
    /**
     * URL which is used to construct the actual URL for the channel.css.
     */
    private static final String BASE_URL = "http://cdn.frankerfacez.com/channel/";
    
    
    /**
     * The channels that have already been requested in this session.
     */
    private final Set<String> alreadyRequested = new HashSet<>();
    
    /**
     * The channels whose request is currently pending. Channels get removed
     * from here again once the request result is received.
     */
    private final Set<String> requestPending = new HashSet<>();

    private final FrankerFaceZListener listener;
    
    public FrankerFaceZ(FrankerFaceZListener listener) {
        this.listener = listener;
    }
    
    /**
     * Requests the emotes for the given channel and global emotes. It only
     * requests each set of emotes once, unless {@code forcedUpdate} is true.
     * 
     * @param stream The name of the channel/stream
     * @param forcedUpdate Whether to update even if it was already requested
     */
    public synchronized void requestEmotes(String stream, boolean forcedUpdate) {
        stream = Helper.toStream(stream);
        if (stream != null && stream.isEmpty()) {
            return;
        }
        requestForChannel(stream, forcedUpdate);
        requestForChannel("global", false);
        requestForChannel("globalevent", false);
    }
    
    public synchronized void refreshGlobalEmotes() {
        requestEmotes("global", true);
        requestEmotes("globalevent", true);
    }
    
    /**
     * Requests the emotes for the given channel. It only requests the emotes
     * for each channel once, unless {@code forcedUpdate} is true. It also
     * prevents more than one pending request for the same channel.
     * 
     * @param stream
     * @param forcedUpdate 
     */
    private void requestForChannel(final String stream, boolean forcedUpdate) {
        if (requestPending.contains(stream)
                || (alreadyRequested.contains(stream) && !forcedUpdate)) {
            return;
        }
        alreadyRequested.add(stream);
        requestPending.add(stream);
        EmotesRequest request = new EmotesRequest(stream) {
            
            @Override
            public void requestResult(String result, int responseCode) {
                requestPending.remove(stream);
                requestResult2(stream, result);
            }
        };
        new Thread(request).start();
    }
    
    /**
     * Manages the request result, giving the result to the parser, making info
     * messages and sending the parsed result to the listener.
     * 
     * @param channel
     * @param result 
     */
    private void requestResult2(String channel, String result) {
        if (result == null) {
            return;
        }
        FFZParser parser = new FFZParser(channel, result);
        HashSet<Emoticon> emotes = parser.getEmotes();
        List<Usericon> usericons = parser.getUsericons();
        
        LOGGER.info("FFZ ("+channel+"): "+emotes.size()+" emotes received.");
        if (!usericons.isEmpty()) {
            LOGGER.info("FFZ ("+channel+"): "+usericons.size()+" usericons received.");
        }
        
        listener.channelEmoticonsReceived(emotes);
        // Return icons if mod icon was found (will be empty otherwise)
        listener.usericonsReceived(usericons);
    }

    private static abstract class EmotesRequest extends UrlRequest {
        
        public EmotesRequest(String channel) {
            // Make the URL based on the channel
            String url = BASE_URL + channel + ".css";
            setUrl(url);
            setLabel("FFZ");
        }
        
    }
    
    /**
     * Parses the CSS for emotes and mod icon. First finds all the parts in { }
     * and then checks what attributes (content, image, ..) are in there.
     */
    private static class FFZParser {
        
        /**
         * Names of channels that should be used as global emotes (so no stream
         * restriction is added to the Emoticon objects requested from this
         * channel).
         */
        private static final Set<String> GLOBAL = new HashSet<>(Arrays.asList(
                new String[]{"global","globalevent"}));
        
        private static final Pattern PARTS = Pattern.compile("\\{[^}]+(\\}|!important)");
        
        private static final Pattern CODE = Pattern.compile("content:\"([^\"]+)");
        private static final Pattern HEIGHT = Pattern.compile("height:([0-9]+)px");
        private static final Pattern WIDTH = Pattern.compile("width:([0-9]+)px");
        private static final Pattern IMAGE = Pattern.compile("background-image:url\\(\"([^\"]+)\"\\)");
        
        private final String stream;
        private final HashSet<Emoticon> emotes = new HashSet<>();
        private final List<Usericon> usericons = new ArrayList<>();
        
        public FFZParser(String stream, String input) {
            if (GLOBAL.contains(stream)) {
                this.stream = null;
            } else {
                this.stream = stream;
            }
            
            // Remove all whitespace to be able to parse stuff without worrying
            // about spaces
            input = input.replaceAll("\\s", "");
            
            // Find possible emotes/icons, which is just stuff inside { }
            Matcher m = PARTS.matcher(input);
            while (m.find()) {
                parsePart(m.group());
            }
        }
        
        public HashSet<Emoticon> getEmotes() {
            return emotes;
        }
        
        public List<Usericon> getUsericons() {
            return usericons;
        }
        
        /**
         * Parses one possible emote or mod icon. It is assumed to be an emote
         * when there are content, url and width/height attributes. A mod icon
         * is assumed when there is no content attribute and the url contains
         * "modicon.png".
         * 
         * @param part 
         */
        private void parsePart(String part) {
            String code = get(CODE, part);
            String image = get(IMAGE, part);
            Integer width = getInteger(WIDTH, part);
            Integer height = getInteger(HEIGHT, part);
            if (image != null) {
                if (code == null && image.contains("modicon.png")) {
                    usericons.add(Usericon.createTwitchLikeIcon(Usericon.TYPE_MOD,
                            stream, image, Usericon.SOURCE_FFZ));
                } else if (width != null && height != null) {
                    Emoticon.Builder builder = new Emoticon.Builder(
                            Emoticon.Type.FFZ, code, image, width, height);
//                    Emoticon emote = new Emoticon(Emoticon.Type.FFZ, code, image, width, height,
//                            Emoticon.SET_UNDEFINED);
//                    emote.setStream(stream);
//                    emote.addStreamRestriction(stream);
                    builder.addStreamRestriction(stream);
                    emotes.add(builder.build());
                }
            }
        }
        
        /**
         * Retrieves the text contained in this Patterns first match group from
         * the input.
         *
         * @param pattern
         * @param input
         * @return
         */
        private String get(Pattern pattern, String input) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        /**
         * Retrieves an Integer instead of a String.
         *
         * @param pattern
         * @param input
         * @return The Integer or null if it is not a valid number.
         */
        private Integer getInteger(Pattern pattern, String input) {
            try {
                return Integer.parseInt(get(pattern, input));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        
    }
}
