
package chatty.util.api;

import chatty.Helper;
import chatty.User;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

/**
 * A single emoticon, that contains a pattern, an URL to the image and
 * a width/height.
 * 
 * It also includes a facility to load the image in a seperate thread once
 * it is needed.
 * 
 * @author tduva
 */
public class Emoticon {
    
    private static final Logger LOGGER = Logger.getLogger(Emoticon.class.getName());
    
    public static final int SET_UNDEFINED = -1;
    
    public static enum Type {
        TWITCH, FFZ, BTTV
    }
    
    private static final Pattern WORD = Pattern.compile("[^\\w]");
    
    /**
     * Try loading the image these many times, which will be tried if an error
     * occurs.
     */
    private static final int MAX_LOADING_ATTEMPTS = 3;
    
    /**
     * After these many attempts, try the alternate way of loading the image, so
     * we can get a better error message and possibly also load it. This has to
     * be lower than MAX_LOADING_ATTEMPTS or it will never happen.
     */
    private static final int TRY_ALTERNATE_AFTER_ATTEMPTS = 2;
    
    /**
     * How much time (milliseconds) has to pass in between loading attempts,
     * which will only happen if an error occured.
     */
    private static final int LOADING_ATTEMPT_DELAY = 30*1000;
    
    public final Type type;
    public final String code;
    public final int emoteSet;
    private final Set<String> streamRestrictions;
    public final String url;
    public final int width;
    public final int height;
    
    private String stream;
    
    private Matcher matcher;
    private ImageIcon icon;
    private Set<EmoticonUser> users;
    private boolean loading = false;
    private boolean loadingError = false;
    private volatile int loadingAttempts = 0;
    private long lastLoadingAttempt;

    /**
     * Set required values in contructor and optional values via methods, then
     * construct the Emoticon object with a private constructor. This ensures
     * that the Emoticon object is fully build when it is used (while not having
     * to pass all values to one constructor).
     */
    public static class Builder {
        
        private final Type type;
        private final String search;
        private final String url;
        private final int width;
        private final int height;
        
        private String stream;
        private Set<String> streamRestrictions;
        private int emoteset = SET_UNDEFINED;
        
        public Builder(Type type, String search, String url, int width, int height) {
            this.type = type;
            this.search = search;
            this.url = url;
            this.width = width;
            this.height = height;
        }
        
        public Builder addStreamRestriction(String stream) {
            if (stream != null) {
                if (streamRestrictions == null) {
                    streamRestrictions = new HashSet<>();
                }
                streamRestrictions.add(Helper.toLowerCase(stream));
            }
            return this;
        }
        
        public Builder setEmoteset(int emoteset) {
            this.emoteset = emoteset;
            return this;
        }
        
        public Builder setStream(String stream) {
            this.stream = stream;
            return this;
        }
        
        public Emoticon build() {
            return new Emoticon(this);
        }
    }

    /**
     * Private constructor specifically for use with the Builder.
     * 
     * @param builder The Emoticon.Builder object containing the values to
     * construct this object with
     */
    private Emoticon(Builder builder) {
        
        String code = builder.search;
        
        // Replace some HTML entities (Twitch matches on HTML, we do not)
        code = code.replace("\\&lt\\;", "<");
        code = code.replace("\\&gt\\;", ">");

        // Save before adding word boundary matching
        this.code = code;

        this.type = builder.type;
        this.emoteSet = builder.emoteset;
        this.url = Helper.checkHttpUrl(builder.url);
        this.width = builder.width;
        this.height = builder.height;
        this.streamRestrictions = builder.streamRestrictions;
        this.stream = builder.stream;
    }
    
    private void createMatcher() {
        if (matcher == null) {
            // Only match at word boundaries, unless there is a character that
            // isn't a word character
            String search = code;
            if (!WORD.matcher(code).find()) {
                search = "\\b" + code + "\\b";
            }
            // Actually compile a Pattern from it
            try {
                matcher = Pattern.compile(search).matcher("");
            } catch (PatternSyntaxException ex) {
                LOGGER.warning("Error compiling pattern for '" + search + "' [" + ex.getLocalizedMessage() + "]");
                // Compile a pattern that doesn't match anything, so a Matcher
                // is still available
                matcher = Pattern.compile("(?!)").matcher("");
            }
        }
    }
    
    /**
     * Gets the stream restrictions set for this Emoticon.
     * 
     * @return A copy of the restrictions (defensive copying) or null if no
     * restrictions were set.
     */
    public synchronized Set<String> getStreamRestrictions() {
        if (streamRestrictions == null) {
            return null;
        }
        return new HashSet<>(streamRestrictions);
    }
    
    /**
     * The name of the stream. Used for display purposes, so it isn't
     * necessarily lowercase.
     * 
     * @return 
     */
    public String getStream() {
        return stream;
    }
    
    /**
     * Whether a stream has been set.
     * 
     * @return true if a stream has been set, false otherwise
     */
    public boolean hasStreamSet() {
        return stream != null;
    }
    
    /**
     * Sets the name of the stream associated with this emote. This is only for
     * display/joining the channel.
     * 
     * @param stream The name of the channel
     */
    public void setStream(String stream) {
        this.stream = stream;
    }
    
    /**
     * Should probably only be used out of the EDT.
     * 
     * @param text
     * @return 
     */
    public Matcher getMatcher(String text) {
        createMatcher();
        return matcher.reset(text);
    }
    
    /**
     * Requests an ImageIcon to be loaded, returns the default icon at first,
     * but starts a SwingWorker to get the actual image.
     * 
     * @param user
     * @return 
     */
    public ImageIcon getIcon(EmoticonUser user) {
        addUser(user);
        if (icon == null) {
            icon = getDefaultIcon();
            loadImage();
        } else if (loadingError) {
            if (loadImage()) {
                LOGGER.warning("Trying to load " + code + " again (" + url + ")");
            }
        }
        return icon;
    }
    
    private void addUser(EmoticonUser user) {
        if (users == null) {
            users = Collections.newSetFromMap(
            new WeakHashMap<EmoticonUser, Boolean>());
        }
        users.add(user);
    }
    
    /**
     * Try to load the image, if it's not already loading and if the max loading
     * attempts are not exceeded.
     * 
     * @return true if the image will be attempted to be loaded, false otherwise
     */
    private boolean loadImage() {
        if (!loading && loadingAttempts < MAX_LOADING_ATTEMPTS &&
                System.currentTimeMillis() - lastLoadingAttempt > LOADING_ATTEMPT_DELAY) {
            loading = true;
            loadingError = false;
            loadingAttempts++;
            lastLoadingAttempt = System.currentTimeMillis();
            (new IconLoader()).execute();
            return true;
        }
        return false;
    }
    
    /**
     * Construct a default image based on the size of this emoticon.
     * 
     * @param error If true, uses red color to indicate an error.
     * @return 
     */
    private Image getDefaultImage(boolean error) {
        BufferedImage res=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = res.getGraphics();
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(Color.LIGHT_GRAY);
        if (error) {
            g.setColor(Color.red);
        }
        g.drawString("[x]", width / 2, height / 2);

        g.dispose();
        return res;
    }
    
    /**
     * Construct a default icon based on the size of this emoticon.
     * 
     * @return 
     */
    private ImageIcon getDefaultIcon() {
        return new ImageIcon(getDefaultImage(false));
    }
    
    /**
     * A Worker class to load the Icon. Not doing this in it's own thread
     * can lead to lag when a lot of new icons are being loaded.
     */
    private class IconLoader extends SwingWorker<ImageIcon,Object> {

        @Override
        protected ImageIcon doInBackground() throws Exception {
            ImageIcon loadedIcon = null;
            
//            if (true) {
//                try (InputStream in = new BufferedInputStream(new URL(url).openStream())) {
//                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//                    byte[] buffer = new byte[1024];
//                    while (in.read(buffer) != -1) {
//                        bytes.write(buffer);
//                    }
//                    byte[] response = bytes.toByteArray();
//                    System.out.println(response);
//                    loadedIcon = new ImageIcon(response);
//                    
//                    try (FileOutputStream fos = new FileOutputStream(
//                            Chatty.getUserDataDirectory()+File.separator+"emotecache"+File.separator+Helper.md5(url))) {
//                        fos.write(response);
//                    } catch (IOException ex) {
//                        LOGGER.warning(ex.toString());
//                    }
//                    
//                } catch (IOException ex) {
//                    LOGGER.warning(ex.toString());
//                }
//            }
            
            if (loadingAttempts <= TRY_ALTERNATE_AFTER_ATTEMPTS) {
                /**
                 * Primary method, which should be used if no error occurs. For
                 * some reason this way loads some images (e.g. Kappa) with more
                 * contrast. And it overall seems to work well, so changing the
                 * method completely may not be wise.
                 */
                try {
                    // Use createImage() instead of just using "new ImageIcon()"
                    // so it doesn't try to load a higher resolution image for
                    // Retina displays (the Twitch CDN returns a 404 image)
                    loadedIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(new URL(url)));
                    
//                    Image img = loadedIcon.getImage();
//                    BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
//
//                    Graphics2D g2 = bi.createGraphics();
//                    g2.drawImage(img, 0, 0, null);
//                    g2.dispose();
//                    ImageIO.write(bi, "png",
//                            new File(Chatty.getUserDataDirectory()+File.separator+"emotecache"+File.separator+Helper.md5(url)));
                } catch (MalformedURLException ex) {
                    LOGGER.warning("Invalid url for " + code + ": " + url);
                    return null;
                }
            } else {
                /**
                 * Secondary method, with slightly better error messages (well,
                 * any at all). Not quite sure if there is any way this may work
                 * while the other one doesn't, but at least it should give a
                 * better error response. Still, just in case, this also creates
                 * an ImageIcon if successfull (of course it might just happen
                 * to attempt this when the server is reachable again).
                 */
                try {
                    // Using URLConnection because just ImageIO.read(URL) didn't
                    // seem to give very useful error messages
                    URLConnection c = new URL(url).openConnection();
                    c.setReadTimeout(10 * 1000);
                    c.setConnectTimeout(10 * 1000);
                    
                    // Try-with-resources on the input stream should also close
                    // the connection
                    try (InputStream input = c.getInputStream()) {
                        Image image = ImageIO.read(input);
                        if (image != null) {
                            loadedIcon = new ImageIcon(image);
                            LOGGER.warning("Loaded emoticon " + code + " via alternate way.");
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.warning("Error loading emoticon " + code + " (" + url + "): " + ex);
                }
            }
            return loadedIcon;
        }
        
        /**
         * The image should be done loading, replace the defaulticon with the
         * actual loaded icon and tell the user that it's loaded.
         */
        @Override
        protected void done() {
            try {
                ImageIcon loadedIcon = get();
                if (loadedIcon == null
                        || loadedIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    /**
                     * Only doing this on ERRORED, waiting for COMPLETE would
                     * not allow animated GIFs to load
                     */
                    icon.setImage(getDefaultImage(true));
                    loadingError = true;
                    if (loadedIcon != null) {
                        loadedIcon.getImage().flush();
                    }
                } else {
                    icon.setImage(loadedIcon.getImage());
                }
                for (EmoticonUser user : users) {
                    user.iconLoaded();
                }
                loading = false;
                //users.clear();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public String toString() {
        return code;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean matchesUser(User user) {
        if (user == null) {
            return true;
        }
        if (emoteSet != Emoticon.SET_UNDEFINED
                && !user.getEmoteSet().contains(emoteSet)) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Emoticon other = (Emoticon) obj;
        if (!Objects.equals(this.code, other.code)) {
            return false;
        }
        if (this.emoteSet != other.emoteSet) {
            return false;
        }
        if (!Objects.equals(this.streamRestrictions, other.streamRestrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.code);
        hash = 79 * hash + this.emoteSet;
        hash = 79 * hash + Objects.hashCode(this.streamRestrictions);
        return hash;
    }

    public static interface EmoticonUser {

        void iconLoaded();
    }
}
