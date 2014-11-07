
package chatty.util.api;

import static chatty.util.api.Emoticon.SET_UNDEFINED;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Add emoticons and get a list of them matching a certain emoteset.
 * 
 * <p>
 * Emotes are sorted into the {@code emoticons} map if they don't have a stream
 * restriction and into the {@code streamEmoticons} map if they have a stream
 * restriction. Emoticons in {@code streamEmoticons} may still have an emoteset
 * or other restrictions that should be checked. This is only done this way to
 * retrieve and iterate over a relatively small subset of all emotes.
 * </p>
 * 
 * <p>
 * This is generally not thread-safe and all methods should be only used from
 * the same thread (like in this case probably the EDT).
 * </p>
 * 
 * @author tduva
 */
public class Emoticons {
    
    private static final Logger LOGGER = Logger.getLogger(Emoticons.class.getName());
    
    private static final Map<String, String> EMOTICONS_MAP = new HashMap<>();
    
    static {
        EMOTICONS_MAP.put("B-?\\)", "B)");
        EMOTICONS_MAP.put("R-?\\)", "R)");
        EMOTICONS_MAP.put("\\:-?D", ":D");
        EMOTICONS_MAP.put("\\;-?\\)", ";)");
        EMOTICONS_MAP.put("\\:-?(o|O)", ":O");
        EMOTICONS_MAP.put("\\:-?\\)", ":)");
        EMOTICONS_MAP.put("\\;-?(p|P)", ";P");
        EMOTICONS_MAP.put("[o|O](_|\\.)[o|O]", "o_O");
        EMOTICONS_MAP.put(">\\(", ">(");
        EMOTICONS_MAP.put("\\:-?(?:\\/|\\\\)(?!\\/)", ":/");
        EMOTICONS_MAP.put("\\:-?\\(", ":(");
        EMOTICONS_MAP.put("\\:-?(p|P)", ":p");
        EMOTICONS_MAP.put("\\:-?[z|Z|\\|]", ":|");
        EMOTICONS_MAP.put(":-?(?:7|L)", ":7");
        EMOTICONS_MAP.put("\\:>", ":>");
        EMOTICONS_MAP.put("\\:-?(S|s)", ":S");
        EMOTICONS_MAP.put("#-?[\\\\/]", "#/");
        EMOTICONS_MAP.put("<\\]", "<]");
    }
    
    /**
     * Emoticons associated with an emoteset (Twitch Emotes)
     */
    private final HashMap<Integer,HashSet<Emoticon>> emoticons = new HashMap<>();
    /**
     * Emoticons associated with a channel (FrankerFaceZ)
     */
    private final HashMap<String,HashSet<Emoticon>> streamEmoticons = new HashMap<>();
    
    /**
     * Emoteset -> Stream association (from Twitchemotes.com).
     */
    private final Map<Integer, String> emotesetStreams = Collections.synchronizedMap(new HashMap<Integer, String>());
    
    private static final HashSet<Emoticon> EMPTY_SET = new HashSet<>();
    
    private final Set<String> ignoredEmotes = new HashSet<>();
    
    private final Map<String, Favorite> favoritesNotFound = new HashMap<>();
    
    private final HashMap<Favorite, Emoticon> favorites = new HashMap<>();
    
    private boolean loadedFavoritesFromSettings;
    
    /**
     * Adds the given emoticons and sorts them into different maps, depending
     * on their restrictions.
     * 
     * <ul>
     * <li>If they have a stream restriction, they will be put in the
     * {@code streamEmoticons} map, with the stream name as the key. If there
     * is more than one stream in the restriction, it will be added under
     * several keys.</li>
     * <li>If there is no stream restriction, then they will be put in the
     * {@code emoticons} map, with the emoteset as the key. If no emoteset is
     * defined, then {@code null} will be used as key.</li>
     * </ul>
     * 
     * <p>
     * This is not thread-safe, so it should only be called from the EDT.
     * </p>
     * 
     * @param newEmoticons 
     */
    public void addEmoticons(Set<Emoticon> newEmoticons) {
        for (Emoticon emote : newEmoticons) {
            Set<String> channelRestrictions = emote.getStreamRestrictions();
            if (channelRestrictions != null) {
                for (String channel : channelRestrictions) {
                    // Create channel set if necessary
                    if (!streamEmoticons.containsKey(channel)) {
                        streamEmoticons.put(channel, new HashSet<Emoticon>());
                    }
                    streamEmoticons.get(channel).remove(emote);
                    streamEmoticons.get(channel).add(emote);
                }
            } else {
                Integer emoteset = emote.emoteSet > -1 ? emote.emoteSet : null;
                if (!emoticons.containsKey(emoteset)) {
                    emoticons.put(emoteset, new HashSet<Emoticon>());
                }
                emoticons.get(emoteset).remove(emote);
                emoticons.get(emoteset).add(emote);
            }
        }
        LOGGER.info("Added "+newEmoticons.size()+" emotes."
                + " Now "+emoticons.size()+" emotesets and "
                +streamEmoticons.size()+" channels with exclusive emotes ("
                +getEmoticons().size()+" global emotes).");
        findFavorites();
    }

    /**
     * Gets a list of all emoticons that don't have an emoteset associated
     * with them. This returns the original Set, so it should not be modified.
     * 
     * @return 
     */
    public HashSet<Emoticon> getEmoticons() {
        HashSet<Emoticon> result = emoticons.get(null);
        if (result == null) {
            result = EMPTY_SET;
        }
        return result;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given emoteset.
     * This returns the original Set, so it should not be modified.
     *
     * @param emoteSet
     * @return
     */
    public HashSet<Emoticon> getEmoticons(int emoteSet) {
        HashSet<Emoticon> result = emoticons.get(emoteSet);
        if (result == null) {
            result = EMPTY_SET;
        }
        return result;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given channel. This
     * returns the original Set, so it should not be modified.
     *
     * @param channel The name of the channel
     * @return
     */
    public HashSet<Emoticon> getEmoticons(String channel) {
        HashSet<Emoticon> result = streamEmoticons.get(channel);
        if (result == null) {
            result = EMPTY_SET;
        }
        return result;
    }
    
    /**
     * Checks whether the given emoteset is a turbo emoteset. This may be
     * incomplete.
     * 
     * @param emoteSet The emoteset to check
     * @return true when it is a turbo emoteset, false otherwise
     */
    public static boolean isTurboEmoteset(int emoteSet) {
        if (emoteSet == 33 || emoteSet == 42
                    || emoteSet == 457 || emoteSet == 793) {
            return true;
        }
        return false;
    }
    
    /**
     * Adds the emoteset data that associates them with a stream name.
     * 
     * @param data 
     */
    public void addEmotesetStreams(Map<Integer, String> data) {
        emotesetStreams.putAll(data);
    }
    
    /**
     * Gets the name of the stream the given emoteset is associated with. This
     * of course only works if the emoteset data was actually successfully
     * requested before calling this.
     * 
     * @param emoteset The emoteset
     * @return The name of the stream, or null if none could be found for this
     * emoteset
     */
    public String getStreamFromEmoteset(int emoteset) {
        String stream = emotesetStreams.get(emoteset);
        if ("00000turbo".equals(stream) || "turbo".equals(stream)) {
            return "Turbo Emotes";
        }
        return emotesetStreams.get(emoteset);
    }
    
    /**
     * Gets the emoteset from the given stream name. This of course only works
     * if the emoteset data was actually successfully requested before calling
     * this.
     * 
     * @param stream The name of the stream to get the emoteset for
     * @return The emoteset, or -1 if none could be found
     */
    public int getEmotesetFromStream(String stream) {
        for (int emoteset : emotesetStreams.keySet()) {
            if (emotesetStreams.get(emoteset).equals(stream)) {
                return emoteset;
            }
        }
        return -1;
    }
    
    /**
     * Replaces the ignored emotes list with the given data.
     * 
     * @param ignoredEmotes A Collection of emote codes to ignore
     */
    public void setIgnoredEmotes(Collection<String> ignoredEmotes) {
        this.ignoredEmotes.clear();
        this.ignoredEmotes.addAll(ignoredEmotes);
    }
    
    /**
     * Adds the given emote to the list of ignored emotes, by adding it's code.
     * 
     * @param emote The emote to ignore
     */
    public void addIgnoredEmote(Emoticon emote) {
        addIgnoredEmote(emote.code);
    }
    
    /**
     * Adds the given emote code to the list of ignored emotes.
     * 
     * @param emoteCode The emote code to add
     */
    public void addIgnoredEmote(String emoteCode) {
        ignoredEmotes.add(emoteCode);
    }
    
    /**
     * Check if the given emote is on the list of ignored emotes. Compares the
     * emote code to the codes on the list.
     * 
     * @param emote The Emoticon to check
     * @return true if the emote is ignored, false otherwise
     */
    public boolean isEmoteIgnored(Emoticon emote) {
        return ignoredEmotes.contains(emote.code);
    }
    
    /**
     * Displays some information about the emotes that match the given emote
     * code (usually just one, but might be several if emotes with the same code
     * exist for different emotesets or channels).
     * 
     * @param emoteCode A single emote code to find the emote for
     * @return A String with a description of the found emote(s)
     */
    public String getEmoteInfo(String emoteCode) {
        if (emoteCode == null) {
            return "No emote specified.";
        }
        Set<Emoticon> found = findMatchingEmoticons(emoteCode);
        if (found.isEmpty()) {
            return "No matching emote found.";
        }
        StringBuilder b = new StringBuilder();
        b.append("Found ").append(found.size());
        if (found.size() == 1) {
            b.append(" emote");
        } else {
            b.append(" emotes");
        }
        b.append(" for '").append(emoteCode).append("': ");
        String sep = "";
        for (Emoticon emote : found) {
            Set<String> streams = emote.getStreamRestrictions();
            b.append(sep+"'"+emote.code+"'"
                +" / Type: "+emote.type+" / "
                +(emote.emoteSet == SET_UNDEFINED
                    ? "Usable by everyone"
                    : ("Emoteset: "+emote.emoteSet
                      +" ("+getStreamFromEmoteset(emote.emoteSet)+")"))
                
                +(streams == null
                    ? " / Usable in all channels"
                    : " / Only usable in: "+streams));
            sep = " ### ";
        }
        
        return b.toString();
    }
    
    /**
     * Finds all emotes matching the given emote code.
     * 
     * @param emoteCode
     * @return 
     */
    public Set<Emoticon> findMatchingEmoticons(String emoteCode) {
        Set<Emoticon> found = new HashSet<>();
        found.addAll(findMatchingEmoticons(emoteCode, emoticons.values()));
        found.addAll(findMatchingEmoticons(emoteCode, streamEmoticons.values()));
        return found;
    }
    
    /**
     * Finds all emotes matching the given emote code within the given data.
     * 
     * @param emoteCode
     * @param values
     * @return 
     */
    public Set<Emoticon> findMatchingEmoticons(String emoteCode,
            Collection<HashSet<Emoticon>> values) {
        Set<Emoticon> found = new HashSet<>();
        for (Collection<Emoticon> emotes : values) {
            for (Emoticon emote : emotes) {
                if (emote.getMatcher(emoteCode).matches()) {
                    found.add(emote);
                }
            }
        }
        return found;
    }
    
    /**
     * Creates a new Set that only contains the subset of the given emotes that
     * are of the given type (e.g. Twitch, FFZ or BTTV).
     * 
     * @param emotes The emotes to filter
     * @param type The emote type to allow through the filter
     * @return A new Set containing the emotes of the given type
     */
    public static final Set<Emoticon> filterByType(Set<Emoticon> emotes,
            Emoticon.Type type) {
        Set<Emoticon> filtered = new HashSet<>();
        for (Emoticon emote : emotes) {
            if (emote.type == type) {
                filtered.add(emote);
            }
        }
        return filtered;
    }
    
    public static final String toWriteable(String emoteCode) {
        String writeable = EMOTICONS_MAP.get(emoteCode);
        if (writeable == null) {
            return emoteCode;
        }
        return writeable;
    }
    
    /**
     * Adds the given Emoticon to the favorites.
     * 
     * @param emote The Emoticon to add
     */
    public void addFavorite(Emoticon emote) {
        favorites.put(createFavorite(emote), emote);
    }
    
    /**
     * Creates a Favorite object for the given Emoticon.
     * 
     * @param emote The Emoticon to create the Favorite object for
     * @return The created Favorite object
     */
    private Favorite createFavorite(Emoticon emote) {
        return new Favorite(emote.code, emote.emoteSet, 0);
    }
    
    /**
     * Loads the favorites from the settings.
     * 
     * @param settings The Settings object
     */
    public void loadFavoritesFromSettings(Settings settings) {
        List<List> entriesToLoad = settings.getList("favoriteEmotes");
        favoritesNotFound.clear();
        favorites.clear();
        for (List item : entriesToLoad) {
            Favorite f = listToFavorite(item);
            if (f != null) {
                favoritesNotFound.put(f.code, f);
            }
        }
        findFavorites();
        loadedFavoritesFromSettings = true;
    }

    /**
     * Saves the favorites to the settings, discarding any favorites that
     * haven't been found several times already.
     * 
     * @param settings The Settings object
     */
    public void saveFavoritesToSettings(Settings settings) {
        if (!loadedFavoritesFromSettings) {
            LOGGER.warning("Not saving favorite emotes, because they don't seem to have been loaded in the first place.");
            return;
        }
        List<List> entriesToSave = new ArrayList<>();
        for (Favorite f : favorites.keySet()) {
            entriesToSave.add(favoriteToList(f, true));
        }
        for (Favorite f : favoritesNotFound.values()) {
            if (f.notFoundCount > 10) {
                LOGGER.warning("Not saving favorite emote "+f+" (not found)");
            } else {
                entriesToSave.add(favoriteToList(f, false));
            }
        }
        settings.putList("favoriteEmotes", entriesToSave);
    }
    
    /**
     * Turns the given list into a single Favorite object. This is used to load
     * the favorites from the settings. The expected format is as detailed in
     * {@see favoriteToList(Favorite, boolean)}.
     * 
     * @param item The List to turn into a Favorite object
     * @return The created Favorite, or null if an error occured
     * @see favoriteToList(Favorite, boolean)
     */
    private Favorite listToFavorite(List item) {
        try {
            String code = (String) item.get(0);
            int emoteset = ((Number) item.get(1)).intValue();
            int notFoundCount = ((Number) item.get(2)).intValue();
            return new Favorite(code, emoteset, notFoundCount);
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }
    
    /**
     * Turns the given favorite into a list, so it can be saved to the settings.
     * The format is (arrayindex: value (Type)):
     * 
     * <ul>
     * <li>0: code (String),</li>
     * <li>1: emoteset (Number),</li>
     * <li>2: notFoundCount (Number)</li>
     * </ul>
     * 
     * The notFoundCount is increased if the emote was not found during this
     * session, otherwise it is set to 0.
     * 
     * @param f The favorite to turn into a list
     * @param found Whether this favorite was found during this session
     * @return The created list
     * @see listToFavorite(List)
     */
    private List favoriteToList(Favorite f, boolean found) {
        List list = new ArrayList();
        list.add(f.code);
        list.add(f.emoteset);
        if (found) {
            list.add(0);
        } else {
            list.add(f.notFoundCount+1);
        }
        return list;
    }
    
    /**
     * If there are still Favorites not yet associated with an actual Emoticon
     * object, then search through the current emoticons. This should be done
     * everytime new emotes are added (e.g. from request or loaded from cache).
     */
    private void findFavorites() {
        if (favoritesNotFound.isEmpty()) {
            return;
        }
        int count = favoritesNotFound.size();
        for (Collection<Emoticon> emotes : emoticons.values()) {
            for (Emoticon emote : emotes) {
                Favorite f = favoritesNotFound.get(emote.code);
                if (f != null && f.emoteset == emote.emoteSet) {
                    favorites.put(f, emote);
                    favoritesNotFound.remove(emote.code);
                    if (favoritesNotFound.isEmpty()) {
                        LOGGER.info("Emoticons: Found all remaining "+count+" favorites");
                        return;
                    }
                }
            }
        }
        LOGGER.info("Emoticons: "+favoritesNotFound.size()+" favorites still not found");
    }
    
    /**
     * Removes the given Emoticon from the favorites.
     * 
     * @param emote 
     */
    public void removeFavorite(Emoticon emote) {
        favorites.remove(createFavorite(emote));
        favoritesNotFound.remove(emote.code);
    }
    
    /**
     * Returns a copy of the favorites.
     * 
     * @return 
     */
    public Set<Emoticon> getFavorites() {
        return new HashSet<>(favorites.values());
    }
    
    /**
     * Gets the number of favorites that couldn't be found.
     * 
     * @return 
     */
    public int getNumNotFoundFavorites() {
        return favoritesNotFound.size();
    }
    
    /**
     * Checks whether the given Emoticon is a favorite.
     * 
     * @param emote
     * @return 
     */
    public boolean isFavorite(Emoticon emote) {
        return favoritesNotFound.containsKey(emote.code) || favorites.containsValue(emote);
    }
    
    /**
     * A favorite specifying the emote code, the emoteset and how often it
     * hasn't been found. The emote code and emoteset are required to find the
     * actual Emoticon object that corresponds to it.
     */
    private static class Favorite {
        
        public final String code;
        public final int emoteset;
        public final int notFoundCount;
        
        Favorite(String code, int emoteset, int notFoundCount) {
            this.code = code;
            this.emoteset = emoteset;
            this.notFoundCount = notFoundCount;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 19 * hash + Objects.hashCode(this.code);
            hash = 19 * hash + this.emoteset;
            return hash;
        }

        /**
         * A Favorite is considered equal when both the emote code and emoteset
         * are equal.
         * 
         * @param obj
         * @return 
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Favorite other = (Favorite) obj;
            if (!Objects.equals(this.code, other.code)) {
                return false;
            }
            if (this.emoteset != other.emoteset) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return code+"["+emoteset+"]";
        }
        
    }
    
}
