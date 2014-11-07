
package chatty;

import chatty.gui.HtmlColors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * A single usericon (badge) with an image and information on where (channel)
 * and for who (user properties) it should be displayed.
 * 
 * @author tduva
 */
public class Usericon implements Comparable {
    
    private static final Logger LOGGER = Logger.getLogger(Usericon.class.getName());
    
    private static final Color TWITCH_MOD_COLOR = HtmlColors.decode("#34ae0a");
    private static final Color TWITCH_TURBO_COLOR = HtmlColors.decode("#6441a5");
    private static final Color TWITCH_ADMIN_COLOR = HtmlColors.decode("#faaf19");
    private static final Color TWITCH_BROADCASTER_COLOR = HtmlColors.decode("#e71818");
    private static final Color TWITCH_STAFF_COLOR = HtmlColors.decode("#200f33");
    
    private static final Set<String> statusDef = new HashSet<>(Arrays.asList(
            "$mod", "$sub", "$admin", "$staff", "$turbo", "$broadcaster"));
    
    /**
     * The type determines whether it should replace any of the default icons
     * (which also assumes they are mainly requested if the user is actually
     * mod, turbo, etc.) or if it should be shown in addition to the default
     * icons (addon).
     */
    public static final int TYPE_MOD = 0;
    public static final int TYPE_TURBO = 1;
    public static final int TYPE_BROADCASTER = 2;
    public static final int TYPE_STAFF = 3;
    public static final int TYPE_ADMIN = 4;
    public static final int TYPE_SUB = 5;
    public static final int TYPE_ADDON = 6;
    
    /**
     * On creation the match type is determined, which means what type of
     * restriction is set. This is done for easier handling later, so the
     * restriction doesn't have to be parsed everytime.
     */
    public static final int MATCHTYPE_CATEGORY = 0;
    public static final int MATCHTYPE_UNDEFINED = 1;
    public static final int MATCHTYPE_ALL = 2;
    public static final int MATCHTYPE_STATUS = 3;
    public static final int MATCHTYPE_NAME = 4;
    
    /**
     * The type of icon based on the source.
     */
    public static final int SOURCE_FALLBACK = 0;
    public static final int SOURCE_TWITCH = 5;
    public static final int SOURCE_FFZ = 10;
    public static final int SOURCE_CUSTOM = 20;
    
    /**
     * Fields directly saved from the constructor arguments (or only slightly
     * modified).
     */
    /**
     * Which kind of icon (replacing mod, sub, turbo, .. or addon).
     */
    public final int type;
    
    /**
     * Where the icon comes from (Twitch, FFZ, Custom, ..).
     */
    public final int source;
    
    /**
     * The channel restriction, which determines which channel(s) the icon
     * should be used for. This doesn't necessarily only contain the channel
     * itself, but possibly also modifiers.
     */
    public final String channelRestriction;
    
    /**
     * The URL the image is loaded from
     */
    public final URL url;
    
    /**
     * The restriction
     */
    public final String restriction;
    
    /**
     * The filename of a locally loaded custom emoticon. This can be used to
     * more easily load/save that setting.
     */
    public final String fileName;
    
    
    /**
     * Data that is derived from other fields and not directly saved from
     * constructor arguments.
     */
    /**
     * The image loaded from the given {@literal url}
     */
    public final ImageIcon image;
    
    /**
     * The match type is derived from {@literal id}, to make it easier to check
     * what to match later.
     */
    public final int matchType;
    
    /**
     * The addressbook category to match (if given) in {@literal id}.
     */
    public final String category;
    
    /**
     * The actual channel from the channel restriction. If no or an invalid
     * channel is specified in the channel restriction, then this is empty.
     */
    public final String channel;
    
    /**
     * This is {@code true} if the channel restriction should be reversed, which
     * means all channels BUT the one specified should match.
     */
    public final boolean channelInverse;
    
    public final String restrictionValue;
    
    public final boolean stop;
    
    
    /**
     * Creates a new Icon from the Twitch API, which the appropriate default
     * values for the stuff that isn't specified in the arguments.
     * 
     * @param type
     * @param channel
     * @param urlString
     * @return 
     */
    public static Usericon createTwitchIcon(int type, String channel, String urlString) {
        //return createTwitchLikeIcon(type, channel, urlString, SOURCE_TWITCH);
        return createIconFromUrl(type, channel, urlString, type, null);
    }
    
    /**
     * Creates a new icon with the given values, with appropriate default values
     * for the stuff that isn't specified in the arguments. It determines the
     * background color based on the default Twitch settings, so it should only
     * be used for icons that should match that behaviour.
     * 
     * @param type
     * @param channel
     * @param urlString
     * @param source
     * @return 
     */
    public static Usericon createTwitchLikeIcon(int type, String channel,
            String urlString, int source) {
        return createIconFromUrl(type, channel, urlString, source,
                getColorFromType(type));
    }
    
    public static Usericon createIconFromUrl(int type, String channel,
            String urlString, int source, Color color) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            Usericon icon = new Usericon(type, channel, url, color, source);
            return icon;
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    /**
     * Creates an icon based on a filename, which is resolved with the image
     * directory (if necessary). It also takes a restriction parameter and
     * stuff and sets the other values to appropriate values for custom icons.
     * 
     * @param type
     * @param restriction
     * @param fileName
     * @param channel
     * @return 
     */
    public static Usericon createCustomIcon(int type, String restriction, String fileName, String channel) {
        if (fileName == null) {
            return null;
        }
        try {
            Path path = Paths.get(Chatty.getImageDirectory()).resolve(Paths.get(fileName));
            Usericon icon = new Usericon(type, channel, path.toUri().toURL(), null, SOURCE_CUSTOM, restriction, fileName);
            return icon;
        } catch (MalformedURLException | InvalidPathException ex) {
            LOGGER.warning("Invalid icon file: " + fileName);
        }
        return null;
    }

    public static Usericon createFallbackIcon(int type, URL url) {
        Usericon icon = new Usericon(type, null, url, getColorFromType(type), SOURCE_FALLBACK);
        return icon;
    }
    
    /**
     * Convenience constructor which simply omits the two arguments mainly used
     * for custom icons.
     * 
     * @param type
     * @param channel
     * @param url
     * @param color
     * @param source 
     */
    public Usericon(int type, String channel, URL url, Color color, int source) {
        this(type, channel, url, color, source, null, null);
    }
    
    /**
     * Creates a new {@literal Userimage}, which will try to load the image from
     * the given URL. If the loading fails, the {@literal image} field will be
     * {@literal null}.
     * 
     * @param type The type of userimage (Addon, Mod, Sub, etc.)
     * @param channel The channelRestriction the image applies to
     * @param url The url to load the image from
     * @param color The color to use as background
     * @param source The source of the image (like Twitch, Custom, FFZ)
     * @param restriction Additional restrictions (like $mod, $sub, $cat)
     * @param fileName The name of the file to load the icon from (this is used
     * for further reference, probably only for custom icons)
     */
    public Usericon(int type, String channel, URL url, Color color, int source,
            String restriction, String fileName) {
        this.type = type;
        this.fileName = fileName;
        this.source = source;

        // Channel Restriction
        if (channel != null) {
            channel = channel.trim();
            channelRestriction = channel;
            if (channel.startsWith("!")) {
                channelInverse = true;
                channel = channel.substring(1);
            } else {
                channelInverse = false;
            }
        } else {
            channelRestriction = "";
            channelInverse = false;
        }
        channel = Helper.checkChannel(channel);
        if (channel == null) {
            channel = "";
        }
        this.channel = channel;
        
        
        this.url = url;
        
        if (fileName != null && fileName.startsWith("$")) {
            image = null;
        } else {
            image = addColor(getIcon(url), color);
        }
        
        // Restriction
        if (restriction != null) {
            restriction = restriction.trim();
            this.restriction = restriction;
            if (restriction.contains("$stop")) {
                restriction = restriction.replace("$stop", "").trim();
                stop = true;
            } else {
                stop = false;
            }
            restrictionValue = restriction;
            
            // Check if a category was specified as id
            if (restriction.startsWith("$cat:") && restriction.length() > 5) {
                category = restriction.substring(5);
            } else {
                category = null;
            }

            // Save the type
            if (restriction.startsWith("$cat:") && restriction.length() > 5) {
                matchType = MATCHTYPE_CATEGORY;
            } else if (statusDef.contains(restriction)) {
                matchType = MATCHTYPE_STATUS;
            } else if (Helper.validateChannel(restriction)) {
                matchType = MATCHTYPE_NAME;
            } else if (restriction.equals("$all") || restriction.isEmpty()) {
                matchType = MATCHTYPE_ALL;
            } else {
                matchType = MATCHTYPE_UNDEFINED;
            }
        } else {
            matchType = MATCHTYPE_UNDEFINED;
            category = null;
            this.restriction = null;
            restrictionValue = null;
            stop = false;
        }
    }
    
    /**
     * Loads the icon from the given url.
     * 
     * @param url The URL to load the icon from
     * @return The loaded icon or {@literal null} if no URL was specified or the
     * icon couldn't be loaded
     */
    private ImageIcon getIcon(URL url) {
        if (url == null) {
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            return icon;
        } else {
            LOGGER.warning("Could not load icon: " + url);
        }
        return null;
    }
    
    /**
     * Adds a background color to the given icon, if an icon and color is
     * actually given, otherwise the original icon is returned.
     * 
     * @param icon
     * @param color
     * @return 
     */
    private ImageIcon addColor(ImageIcon icon, Color color) {
        if (icon == null || color == null) {
            return icon;
        }
        BufferedImage image = new BufferedImage(icon.getIconWidth(),
                icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.drawImage(icon.getImage(), 0, 0, color, null);
        g.dispose();
        return new ImageIcon(image);
    }

    /**
     * Used for sorting the default icons in the {@code TreeSet}, which means no
     * two icons that should both appear in there at the same time can return 0,
     * because the {@code TreeSet} uses this to determine the order as well as
     * equality.
     * 
     * @param o The object to compare this object against
     * @return 
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof Usericon) {
            Usericon icon = (Usericon)o;
            if (this.image == null && icon.image == null) {
                return 0;
            } else if (this.image == null) {
                return 1;
            } else if (icon.image == null) {
                return -1;
            } else if (icon.source > source) {
                return 1;
            } else if (icon.source < source) {
                return -1;
            } else if (icon.type > type) {
                return 1;
            } else if (icon.type < type) {
                return -1;
            } else {
                return icon.channelRestriction.compareTo(channelRestriction);
            }
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return typeToString(type)+"/"+source+"/"+channelRestriction+"/"+restriction+"("+(image != null ? "L" : "E")+")";
    }
    
    public static String typeToString(int type) {
        switch (type) {
            case TYPE_MOD: return "MOD";
            case TYPE_ADDON: return "ADD";
            case TYPE_ADMIN: return "ADM";
            case TYPE_BROADCASTER: return "BRC";
            case TYPE_STAFF: return "STA";
            case TYPE_SUB: return "SUB";
            case TYPE_TURBO: return "TRB";
        }
        return "UDF";
    }
    
    public static Color getColorFromType(int type) {
        switch (type) {
            case TYPE_MOD:
                return TWITCH_MOD_COLOR;
            case TYPE_TURBO:
                return TWITCH_TURBO_COLOR;
            case TYPE_ADMIN:
                return TWITCH_ADMIN_COLOR;
            case TYPE_BROADCASTER:
                return TWITCH_BROADCASTER_COLOR;
            case TYPE_STAFF:
                return TWITCH_STAFF_COLOR;
        }
        return null;
    }
}
