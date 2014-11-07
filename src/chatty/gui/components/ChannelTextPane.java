
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.gui.HtmlColors;
import chatty.gui.LinkListener;
import chatty.gui.StyleServer;
import chatty.gui.UrlOpener;
import chatty.gui.MainGui;
import chatty.User;
import chatty.Usericon;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ChannelContextMenu;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.menus.UrlContextMenu;
import chatty.gui.components.menus.UserContextMenu;
import chatty.util.DateTime;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonUser;
import chatty.util.api.StreamInfo;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;



/**
 * Text pane that displays chat, provides auto-scrolling, styling, context
 * menus, clickable elements.
 * 
 * <p>Special Attributes:</p>
 * <ul>
 * <li>Elements containing a user name mostly (where available) contain the User
 * object in Attribute.USER (Chat messages, ban messages, joins/parts, etc.)</li>
 * <li>Chat messages (from a user) contain Attribute.USER_MESSAGE for the leaf
 * containing the user name</li>
 * <li>Ban messages ({@code <name> has been banned from talking}) contain
 * Attribute.BAN_MESSAGE=User in the first leaf and Attribute.BAN_MESSAGE_COUNT
 * with an int showing how many bans were combined in the leaf showing the
 * number of bans (if present, usually the last or second to last element)</li>
 * <li>Deleted lines contain Attribute.DELETED_LINE as paragraph attribute</li>
 * </ul>
 * 
 * @author tduva
 */
public class ChannelTextPane extends JTextPane implements LinkListener, EmoticonUser {
    
    private static final Logger LOGGER = Logger.getLogger(ChannelTextPane.class.getName());
    
    private final StyledDocument doc;
    
    private static final Color BACKGROUND_COLOR = new Color(250,250,250);
    
    // Compact mode
    private String compactMode = null;
    private long compactModeStart = 0;
    private int compactModeLength = 0;
    private static final int MAX_COMPACTMODE_LENGTH = 10;
    private static final int MAX_COMPACTMODE_TIME = 30*1000;
    
    private static final int MAX_BAN_MESSAGE_COMBINE_TIME = 10*1000;
    
    /**
     * Min and max buffer size to restrict the setting range
     */
    private static final int BUFFER_SIZE_MIN = 10;
    private static final int BUFFER_SIZE_MAX = 10000;
    
    /**
     * The regex String for finding URLs in messages.
     */
    private static final String urlRegex =
        "(?i)\\b(?:(?:(?:https?)://|www\\.)|(?:[A-Z0-9.]+\\.(tv|com|org|net)))[-A-Z0-9+&@#/%=~_|$?!:,.()]*[A-Z0-9+&@#/%=~_|$)]";
    /**
     * The Matcher to use for finding URLs in messages.
     */
    private static final Matcher urlMatcher =
            Pattern.compile(urlRegex).matcher("");
    
    public MainGui main;

    protected LinkController linkController = new LinkController();
    private static StyleServer styleServer;
    
    public enum Attribute {
        BAN_MESSAGE, BAN_MESSAGE_COUNT, TIMESTAMP, USER, USER_MESSAGE,
        URL_DELETED, DELETED_LINE, EMOTICON
    }
    
    public enum MessageType {
        REGULAR, HIGHLIGHTED, IGNORED_COMPACT
    }
    
    /**
     * Whether the next line needs a newline-character prepended
     */
    private boolean newlineRequired = false;
    
    public enum Setting {
        TIMESTAMP_ENABLED, EMOTICONS_ENABLED, AUTO_SCROLL, USERICONS_ENABLED, 
        SHOW_BANMESSAGES, COMBINE_BAN_MESSAGES, DELETE_MESSAGES,
        DELETED_MESSAGES_MODE, ACTION_COLORED, BUFFER_SIZE, AUTO_SCROLL_TIME
    }
    
    private static final long DELETED_MESSAGES_KEEP = 0;
    
    protected final Styles styles = new Styles();
    private final ScrollManager scrollManager = new ScrollManager();
    
    public ChannelTextPane(MainGui main, StyleServer styleServer) {
        ChannelTextPane.styleServer = styleServer;
        this.main = main;
        this.setBackground(BACKGROUND_COLOR);
        this.addMouseListener(linkController);
        this.addMouseMotionListener(linkController);
        linkController.setUserListener(main.getUserListener());
        linkController.setLinkListener(this);
        setEditorKit(new MyEditorKit());
        this.setDocument(new MyDocument());
        doc = getStyledDocument();
        setEditable(false);
        DefaultCaret caret = (DefaultCaret)getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        styles.setStyles();
    }
    
    public void setContextMenuListener(ContextMenuListener listener) {
        linkController.setContextMenuListener(listener);
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        linkController.setMouseClickedListener(listener);
    }
    
    /**
     * Can be called when an icon finished loading, so it is displayed correctly.
     * 
     * This seems pretty ineffecient, because it refreshes the whole document.
     */
    @Override
    public void iconLoaded() {
        ((MyDocument)doc).refresh();
    }
 
    /**
     * Prints a message from a user to the main text area
     * 
     * @param user The User this message is from
     * @param text The text of the message
     * @param action Whether this is an action message (/me)
     * @param specialType Whether this is a highlight or ignored message
     * @param color The color to use for highlight, if null the default color is
     * used
     */
    public void printMessage(final User user, final String text, boolean action,
            MessageType specialType, Color color) {

        boolean ignored = specialType == MessageType.IGNORED_COMPACT;
        if (ignored) {
            printCompact("IGNORED", user);
            return;
        }
        
        boolean highlighted = specialType == MessageType.HIGHLIGHTED;
        
        closeCompactMode();

        MutableAttributeSet style;
        if (highlighted) {
            style = styles.highlight(color);
        } else {
            style = styles.standard();
        }
        print(getTimePrefix(), style);
        printUser(user, action, ignored);
        
        // Change style for text if /me and no highlight (if enabled)
        if (!highlighted && action && styles.actionColored()) {
            style = styles.standard(user.getColor());
        }
        printSpecials(text, user, style);
        printNewline();
    }
    
    private long getTimeAgo(Element element) {
        Long timestamp = (Long)element.getAttributes().getAttribute(Attribute.TIMESTAMP);
        if (timestamp != null) {
            return System.currentTimeMillis() - timestamp;
        }
        return Long.MAX_VALUE;
    }
    
    /**
     * Gets the first element containing the given key in it's attributes, or
     * the last element if it wasn't found.
     * 
     * @param parent The Element whose subelements are searched
     * @param key The key of the attributes the searched element should have
     * @return The found Element
     */
    private static Element getElementContainingAttributeKey(Element parent, Object key) {
        Element result = null;
        for (int i = 0; i < parent.getElementCount(); i++) {
            result = parent.getElement(i);
            if (result.getAttributes().getAttribute(key) != null) {
                break;
            }
        }
        return result;
    }

    /**
     * Adds or increases the number behind the given ban message.
     * 
     * @param line 
     */
    private void increasePreviousBanMessage(Element line) {
        try {
            // Find the element that should contain the number
            Element countElement = getElementContainingAttributeKey(line,
                    Attribute.BAN_MESSAGE_COUNT);
            // Find out the number, if it exists
            Integer count = (Integer) countElement.getAttributes().getAttribute(Attribute.BAN_MESSAGE_COUNT);
            if (count == null) {
                // If it doesn't exist set to 2, because this will be the second
                // timeout this message represents
                count = 2;
            } else {
                // Otherwise increase number and removet text of previous number
                count++;
                doc.remove(countElement.getStartOffset(),
                        countElement.getEndOffset() - countElement.getStartOffset());
            }
            // Add number at the beginning of the count element, with the
            // appropriate attributes
            
            /**
             * Insert at the end of the countElement (which is either the last
             * element or the one that contains the count), but if it contains
             * a linebreak (which should be at the end), then start before the
             * linebreak.
             * 
             * With no next line of different style line (extra element with
             * linebreak, so starting at the beginning of that would work):
             * '[17:02] ''tduva'' has been banned from talking''
             * '
             * 
             * With same style line (info style) in the next line (linebreak at
             * the end of the last text containing element, starting at the
             * beginning of that would place it after the name):
             * '[17:02] ''tduva'' has been banned from talking
             * '
             * 
             * Once the count element is added properly (linebreak in it's own
             * element, probably because of different attributes):
             * '[17:02] ''tduva'' has been banned from talking'' (2)''
             * '
             */
            int start = countElement.getEndOffset();
            if (getText(countElement).contains("\n")) {
                start--;
            }
            doc.insertString(start, " (" + count + ")",
                    styles.banMessageCount(count));
        } catch (BadLocationException ex) {
            LOGGER.warning("Bad location");
        }
        scrollDownIfNecessary();
    }
    
    private String getText(Element element) {
        try {
            return doc.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
        } catch (BadLocationException ex) {
            LOGGER.warning("Bad location");
        }
        return "";
    }
    
    /**
     * Searches backwards from the newest message for a ban message from the
     * same user that is within the time threshold for combining ban messages
     * and if no message from that user was posted in the meantime.
     *
     * @param user
     * @return 
     */
    private Element findPreviousBanMessage(User user) {
        Element root = doc.getDefaultRootElement();
        for (int i=root.getElementCount()-1;i>=0;i--) {
            Element line = root.getElement(i);
            if (isLineFromUser(line, user)) {
                // Stop immediately a message from that user is found first
                return null;
            }
            // By convention, the first element of the ban message must contain
            // the info that it is a ban message and of which user (and a
            // timestamp)
            Element firstElement = line.getElement(0);
            if (firstElement != null) {
                AttributeSet attr = firstElement.getAttributes();
                if (attr.containsAttribute(Attribute.BAN_MESSAGE, user)
                        && getTimeAgo(firstElement) < MAX_BAN_MESSAGE_COMBINE_TIME) {
                    return line;
                }
            }
        }
        return null;
    }

    /**
     * Called when a user is banned or timed out and outputs a message as well
     * as deletes the lines of the user.
     * 
     * @param user 
     */
    public void userBanned(User user) {
        if (styles.showBanMessages()) {
            Element prevMessage = null;
            if (styles.combineBanMessages()) {
                prevMessage = findPreviousBanMessage(user);
            }
            if (prevMessage != null) {
                increasePreviousBanMessage(prevMessage);
            } else {
                closeCompactMode();
                print(getTimePrefix(), styles.banMessage(user));
                print(user.getDisplayNick(), styles.nick(user, styles.info()));
                print(" has been banned from talking", styles.info());
                printNewline();
            }
        }
        ArrayList<Integer> lines = getLinesFromUser(user);
        Iterator<Integer> it = lines.iterator();
        /**
         * values > 0 mean strike through, shorten message
         * value == 0 means strike through
         * value < 0 means delete message
         */
        boolean delete = styles.deletedMessagesMode() < DELETED_MESSAGES_KEEP;
        while (it.hasNext()) {
            if (delete) {
                deleteMessage(it.next());
            } else {
                deleteLine(it.next(), styles.deletedMessagesMode());
            }
        }
    }
    
    /**
     * Searches the Document for all lines by the given user.
     * 
     * @param nick
     * @return 
     */
    private ArrayList<Integer> getLinesFromUser(User user) {
        Element root = doc.getDefaultRootElement();
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0;i<root.getElementCount();i++) {
            Element line = root.getElement(i);
            if (isLineFromUser(line, user)) {
                result.add(i);
            }
        }
        return result;
    }
    
    /**
     * Checks if the given element is a line that is associated with the given
     * User.
     * 
     * @param line
     * @param user
     * @return 
     */
    private boolean isLineFromUser(Element line, User user) {
        for (int j = 0; j < 10; j++) {
            Element element = line.getElement(j);
            User elementUser = getUserFromElement(element);
            // If the User object matches, we're done
            if (elementUser == user) {
                return true;
            }
            // Stop if any User object was found
            if (elementUser != null) {
                return false;
            }
        }
        // No User object was found, so it's probably not a chat message
        return false;
    }
    
    /**
     * Gets the User-object from an element. If there is none, it returns null.
     * 
     * @param element
     * @return The User object or null if none was found
     */
    private User getUserFromElement(Element element) {
        if (element != null) {
            User elementUser = (User)element.getAttributes().getAttribute(Attribute.USER);
            Boolean isMessage = (Boolean)element.getAttributes().getAttribute(Attribute.USER_MESSAGE);
            if (isMessage != null && isMessage.booleanValue() == true) {
                return elementUser;
            }
        }
        return null;
    }
    
    /**
     * Crosses out the specified line. This is used for messages that are
     * removed because a user was banned/timed out. Optionally shortens the
     * message to maxLength.
     * 
     * @param line The number of the line in the document
     * @param maxLength The maximum number of characters to shorten the message
     *  to. If maxLength <= 0 then it is not shortened.
     */
    private void deleteLine(int line, int maxLength) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        
        // Determine the offsets of the whole line and the message part
        int[] offsets = getMessageOffsets(elementToRemove);
        if (offsets.length != 2) {
            return;
        }
        int startOffset = elementToRemove.getStartOffset();
        int endOffset = elementToRemove.getEndOffset();
        int messageStartOffset = offsets[0];
        int messageEndOffset = offsets[1];
        int length = endOffset - startOffset;
        int messageLength = messageEndOffset - messageStartOffset - 1;
        
        if (maxLength > 0 && messageLength > maxLength) {
            // Delete part of the message if it exceeds the maximum length
            try {
                int removedStart = messageStartOffset + maxLength;
                int removedLength = messageLength - maxLength;
                doc.remove(removedStart, removedLength);
                length = length - removedLength - 1;
                doc.insertString(removedStart, "..", styles.info());
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
        }
        doc.setCharacterAttributes(startOffset, length, styles.deleted(), false);
        setLineDeleted(startOffset);
        //styles.deleted().
    }
    
    /**
     * Deletes the message of the given line by replacing it with
     * <message deleted>.
     * 
     * @param line The number of the line in the document
     */
    private void deleteMessage(int line) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        int[] messageOffsets = getMessageOffsets(elementToRemove);
        if (messageOffsets.length == 2) {
            int startOffset = messageOffsets[0];
            int endOffset = messageOffsets[1];
            try {
                // -1 to length to not delete newline character (I think :D)
                doc.remove(startOffset, endOffset - startOffset - 1);
                doc.insertString(startOffset, "<message deleted>", styles.info());
                setLineDeleted(startOffset);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location: "+startOffset+"-"+endOffset+" "+ex.getLocalizedMessage());
            }
        }
    }
    
    /**
     * Checks if the given line contains an attribute indicating that the line
     * is already deleted.
     * 
     * @param line The element representing this line
     * @return 
     */
    private boolean isLineDeleted(Element line) {
        return line.getAttributes().containsAttribute(Attribute.DELETED_LINE, true);
    }
    
    /**
     * Adds a attribute to the paragraph at offset to prevent trying to delete
     * it again. 
    * 
     * @param offset 
     */
    private void setLineDeleted(int offset) {
        doc.setParagraphAttributes(offset, 1, styles.deletedLine(), false);
    }
    
    private int[] getMessageOffsets(Element line) {
        int count = line.getElementCount();
        int start = 0;
        for (int i=0;i<count;i++) {
            Element element = line.getElement(i);
            if (element.getAttributes().isDefined(Attribute.USER)) {
                start = i + 1;
            }
        }
        if (start < count) {
            int startOffset = line.getElement(start).getStartOffset();
            int endOffset = line.getElement(count - 1).getEndOffset();
            return new int[]{startOffset, endOffset};
        }
        return new int[0];
    }
    
    private Element lastSearchPos = null;
    
    private boolean doesLineExist(Object line) {
        int count = doc.getDefaultRootElement().getElementCount();
        for (int i=0;i<count;i++) {
            if (doc.getDefaultRootElement().getElement(i) == line) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Perform search in the chat buffer. Starts searching for the given text
     * backwards from the last found position.
     * 
     * @param searchText 
     */
    public boolean search(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return false;
        }
        clearSearchResult();
        int count = doc.getDefaultRootElement().getElementCount();
        if (lastSearchPos != null && !doesLineExist(lastSearchPos)) {
            //System.out.println(lastSearchPos+"doesnt exist");
            lastSearchPos = null;
        }
        // Determine if search should start immediately.
        boolean startSearch = lastSearchPos == null;
        searchText = Helper.toLowerCase(searchText);
        // Loop through all lines
        for (int i=count-1;i>=0;i--) {
            //System.out.println(i+"/"+count);
            Element element = doc.getDefaultRootElement().getElement(i);
            if (element == lastSearchPos) {
                // If this lines contained the last result, start searching
                // on next line
                startSearch = true;
                if (i == 0) {
                    lastSearchPos = null;
                }
                continue;
            }
            if (!startSearch) {
                continue;
            }
            int startOffset = element.getStartOffset();
            int endOffset = element.getEndOffset() - 1;
            int length = endOffset - startOffset;
            try {
                String text = doc.getText(startOffset, length);
                if (Helper.toLowerCase(text).contains(searchText)) {
                    //this.setCaretPosition(startOffset);
                    //this.moveCaretPosition(endOffset);
                    doc.setCharacterAttributes(startOffset, length, styles.searchResult(), false);
                    scrollManager.scrollToOffset(startOffset);
                    //System.out.println(text);
//                    if (i == 0) {
//                        lastSearchPos = null;
//                    } else {
                        lastSearchPos = element;
//                    }
                    break;
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
            lastSearchPos = null;
        }
        if (lastSearchPos == null) {
            scrollManager.scrollDown();
            return false;
        }
        return true;
    }
    
    /**
     * Remove any highlighted search results and start the search from the
     * beginning next time.
     */
    public void resetSearch() {
        clearSearchResult();
        lastSearchPos = null;
    }
    
    /**
     * Removes any prior style changes used to highlight a search result.
     */
    private void clearSearchResult() {
        doc.setCharacterAttributes(0, doc.getLength(), styles.clearSearchResult(), false);
    }

    /**
     * Outputs a clickable and colored nickname.
     * 
     * @param user
     * @param action 
     */
    public void printUser(User user, boolean action, boolean ignore) {
        String userName = user.toString();
        if (styles.showUsericons() && !ignore) {
            printUserIcons(user);
            userName = user.getDisplayNick();
        }
        if (user.hasCategory("rainbow")) {
            printRainbowUser(user, userName, action);
        } else {
            if (action) {
                print("* " + userName + " ", styles.nick(user, null));
            } else {
                print(userName + ": ", styles.nick(user, null));
            }
        }
    }
    
    /**
     * Output the username in rainbow colors. This means each character has to
     * be output on it's own, while changing the color. One style with the
     * appropriate User metadata is used and the color changed.
     * 
     * Prints the rest (what doesn't belong to the nick itself) based on the
     * default user style.
     * 
     * @param user
     * @param userName The username to actually output. This also depends on
     * whether badges are output or if the prefixes should be output.
     * @param action 
     */
    private void printRainbowUser(User user, String userName, boolean action) {
        SimpleAttributeSet userStyle = new SimpleAttributeSet(styles.nick());
        userStyle.addAttribute(Attribute.USER_MESSAGE, true);
        userStyle.addAttribute(Attribute.USER, user);

        int length = userName.length();
        double step = 2*Math.PI / length;
        if (action) {
            print("* ", styles.nick());
        }
        for (int i=0;i<length;i++) {
            Color c = makeRainbowColor3(i, step);
            StyleConstants.setForeground(userStyle, c);
            print(userName.substring(i, i+1), userStyle);
        }
        // Requires user style because it needs the metadata to detect the end
        // of the nick when deleting messages (and possibly other stuff)
        if (!action) {
            print(": ", styles.nick(user, null));
        } else {
            print(" ", styles.nick(user, null));
        }
    }
    
    private Color makeRainbowColor3(int i, double step) {
        double delta = 2 * Math.PI / 3;
        //System.out.println(delta);
        int r = (int) (Math.cos(i * step + 0 * delta) * 127.5 + 127.5);
        int g = (int) (Math.cos(i * step + 1 * delta) * 127.5 + 127.5);
        int b = (int) (Math.cos(i * step + 2 * delta) * 110 + 110);

        //System.out.println(r + " " + g + " " + b);
        return new Color(r, g, b);
    }
    
    /**
     * Prints the icons for the given User.
     * 
     * @param user 
     */
    private void printUserIcons(User user) {
        if (user.isBroadcaster()) {
            print("~", styles.getIconStyle(user, Usericon.TYPE_BROADCASTER));
        }
        else if (user.isStaff()) {
            print("!!", styles.getIconStyle(user, Usericon.TYPE_STAFF));
        }
        else if (user.isAdmin()) {
            print("!", styles.getIconStyle(user, Usericon.TYPE_ADMIN));
        }
        else if (user.isModerator()) {
            print("@", styles.getIconStyle(user, Usericon.TYPE_MOD));
        }
        if (user.hasTurbo()) {
            //print("+", styles.turboIcon());
            print("+", styles.getIconStyle(user, Usericon.TYPE_TURBO));
        }
        if (user.isSubscriber()) {
            print("%", styles.getIconStyle(user, Usericon.TYPE_SUB));
        }
        
        // Output addon usericons (if there are any)
        java.util.List<MutableAttributeSet> addonIcons = styles.getAddonIconStyles(user);
        for (MutableAttributeSet style : addonIcons) {
            print("*", style);
        }
    }
    
    /**
     * Removes some chat lines from the top, depending on the current
     * scroll position.
     */
    private void clearSomeChat() {
        int count = doc.getDefaultRootElement().getElementCount();
        int max = styles.bufferSize();
        if (count > max || ( count > max*0.75 && scrollManager.isScrollpositionAtTheEnd() )) {
            removeFirstLines(2);
        }
        //if (doc.getDefaultRootElement().getElementCount() > 500) {
        //    removeFirstLine();
        //}
    }

    /**
     * Removes the specified amount of lines from the top (oldest messages).
     * 
     * @param amount 
     */
    public void removeFirstLines(int amount) {
        if (amount < 1) {
            amount = 1;
        }
        Element firstToRemove = doc.getDefaultRootElement().getElement(0);
        Element lastToRemove = doc.getDefaultRootElement().getElement(amount - 1);
        int startOffset = firstToRemove.getStartOffset();
        int endOffset = lastToRemove.getEndOffset();
        try {
            doc.remove(startOffset,endOffset);
        } catch (BadLocationException ex) {
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
    
    public void clearAll() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Prints something in compact mode, meaning that nick events of the same
     * type appear in the same line, for as long as possible.
     * 
     * This is mainly used for a compact way of printing joins/parts/mod/unmod.
     * 
     * @param type 
     * @param user 
     */
    public void printCompact(String type, User user) {
        String seperator = ", ";
        if (startCompactMode(type)) {
            // If compact mode has actually been started for this print,
            // print prefix first
            print(getTimePrefix(), styles.compact());
            print(type+": ", styles.compact());
            seperator = "";
        }
        print(seperator, styles.compact());
        print(user.getDisplayNick(), styles.nick(user, styles.compact()));
        
        compactModeLength++;
        // If max number of compact prints happened, close compact mode to
        // start a new line
        if (compactModeLength >= MAX_COMPACTMODE_LENGTH) {
            closeCompactMode();
        }
    }
    
    /**
     * Enters compact mode, closes it first if necessary.
     *
     * @param type
     * @return
     */
    private boolean startCompactMode(String type) {
        
        // Check if max time has passed, and if so close first
        long timePassed = System.currentTimeMillis() - compactModeStart;
        if (timePassed > MAX_COMPACTMODE_TIME) {
            closeCompactMode();
        }

        // If this is another type, close first
        if (!type.equals(compactMode)) {
            closeCompactMode();
        }
        
        // Only start if not already/still going
        if (compactMode == null) {
            compactMode = type;
            compactModeStart = System.currentTimeMillis();
            compactModeLength = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Leaves compact mode (if necessary).
     */
    protected void closeCompactMode() {
        if (compactMode != null) {
            printNewline();
            compactMode = null;
        }
    }
    
    /*
     * ########################
     * # General purpose print
     * ########################
     */
    
    protected void printNewline() {
        newlineRequired = true;
    }
    
   /**
     * Prints a regular-styled line (ended with a newline).
     * @param line 
     */
    public void printLine(String line) {
        printLine(line, styles.info());
    }

    /**
     * Prints a line in the given style (ended with a newline).
     * @param line
     * @param style 
     */
    public void printLine(String line,AttributeSet style) {
        // Close compact mode, because this is definately a new line (timestamp)
        closeCompactMode();
        print(getTimePrefix()+line,style);
        newlineRequired = true;
    }

    /**
     * Print special stuff in the text like links and emoticons differently.
     * 
     * First a map of all special stuff that can be found in the text is built,
     * in a way that stuff doesn't overlap with previously found stuff.
     * 
     * Then all the special stuff in this map is printed accordingly, while
     * printing the stuff inbetween with regular style.
     * 
     * @param text 
     * @param user 
     * @param style 
     */
    protected void printSpecials(String text, User user, MutableAttributeSet style) {
        // Where stuff was found
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        // The style of the stuff (basicially metadata)
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        findLinks(text, ranges, rangesStyle);
        
        if (styles.showEmoticons()) {
            findEmoticons(text, user, ranges, rangesStyle);
        }
        
        // Actually print everything
        int lastPrintedPos = 0;
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            int start = range.getKey();
            int end = range.getValue();
            if (start > lastPrintedPos) {
                // If there is anything between the special stuff, print that
                // first as regular text
                print(text.substring(lastPrintedPos, start), style);
            }
            print(text.substring(start, end + 1),rangesStyle.get(start));
            lastPrintedPos = end + 1;
        }
        // If anything is left, print that as well as regular text
        if (lastPrintedPos < text.length()) {
            print(text.substring(lastPrintedPos), style);
        }
        
    }
    
    private void findLinks(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find links
        urlMatcher.reset(text);
        while (urlMatcher.find()) {
            int start = urlMatcher.start();
            int end = urlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end,ranges)) {
                String foundUrl = urlMatcher.group();
                if (checkUrl(foundUrl)) {
                    ranges.put(start, end);
                    rangesStyle.put(start, styles.url(foundUrl));
                }
            }
        }
    }
    
    
    private void findEmoticons(String text, User user, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        
        // Emoteset based
        for (Integer set : user.getEmoteSet()) {
            HashSet<Emoticon> emoticons = main.emoticons.getEmoticons(set);
            findEmoticons(emoticons, text, ranges, rangesStyle);
        }
        
        // Global emotes
        HashSet<Emoticon> emoticons = main.emoticons.getEmoticons();
        findEmoticons(emoticons, text, ranges, rangesStyle);
        
//        HashSet<Emoticon> otherEmoticons = main.emoticons.getEmoticons(null);
//        findEmoticons(otherEmoticons, text, ranges, rangesStyle);
        
        // Channel based (may also have a emoteset restriction)
        HashSet<Emoticon> channelEmotes = main.emoticons.getEmoticons(user.getStream());
        findEmoticons(user, channelEmotes, text, ranges, rangesStyle);
        
//        StreamInfo streamInfo = main.getStreamInfo(user.getStream());
//        System.out.println(streamInfo);
    }
    
    private void findEmoticons(HashSet<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        findEmoticons(null, emoticons, text, ranges, rangesStyle);
    }
    
    private void findEmoticons(User user, HashSet<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find emoticons
        Iterator<Emoticon> it = emoticons.iterator();
        while (it.hasNext()) {
            // Check the text for every single emoticon
            Emoticon emoticon = it.next();
            if (!emoticon.matchesUser(user)) {
                continue;
            }
            if (main.emoticons.isEmoteIgnored(emoticon)) {
                continue;
            }
            Matcher m = emoticon.getMatcher(text);
            while (m.find()) {
                // As long as this emoticon is still found in the text, add
                // it's position (if it doesn't overlap with something already
                // found) and move on
                int start = m.start();
                int end = m.end() - 1;
                if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
                    if (emoticon.getIcon(this) != null) {
                        ranges.put(start, end);
                        MutableAttributeSet attr = styles.emoticon(emoticon);
                        // Add an extra attribute, making this Style unique
                        // (else only one icon will be output if two of the same
                        // follow in a row)
                        attr.addAttribute("start", start);
                        rangesStyle.put(start,attr);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the given integer is within the range of any of the key=value
     * pairs of the Map (inclusive).
     * 
     * @param i
     * @param ranges
     * @return 
     */
    private boolean inRanges(int i, Map<Integer,Integer> ranges) {
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            if (i >= range.getKey() && i <= range.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the Url can be later used as a URI.
     * 
     * @param uriToCheck
     * @return 
     */
    private boolean checkUrl(String uriToCheck) {
        try {
            new URI(uriToCheck);
        } catch (URISyntaxException ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Prints the given text.
     * @param text 
     */
    public void print(String text) {
        print(text, styles.standard());
    }

    /**
     * Prints the given text in the given style. Runs the function that actually
     * adds the text in the Event Dispatch Thread.
     * 
     * @param text
     * @param style 
     */
    public void print(final String text,final AttributeSet style) {
        try {
            String newline = "";
            if (newlineRequired) {
                newline = "\n";
                newlineRequired = false;
                clearSomeChat();
            }
            doc.insertString(doc.getLength(), newline+text, style);
            // TODO: check how this works
            doc.setParagraphAttributes(doc.getLength(), 1, styles.paragraph(), true);
            scrollDownIfNecessary();
        } catch (BadLocationException e) {
            System.err.println("BadLocationException");
        }
    }

    private void scrollDownIfNecessary() {
        if ((scrollManager.isScrollpositionAtTheEnd() || scrollManager.scrolledUpTimeout())
                && lastSearchPos == null) {
            //if (false) {
            scrollManager.scrollDown();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scrollManager.scrollDown();
                }
            });

        }
    }
    
    
    /**
     * Sets the scrollpane used for this JTextPane. Should be possible to do
     * this more elegantly.
     * 
     * @param scroll
     */
    public void setScrollPane(JScrollPane scroll) {
        scrollManager.setScrollPane(scroll);
    }
    
    /**
     * Makes the time prefix.
     * 
     * @return 
     */
    protected String getTimePrefix() {
        if (styles.timestampFormat() != null) {
            return DateTime.currentTime(styles.timestampFormat())+" ";
        }
        return " ";
    }
    
    public void refreshStyles() {
        styles.refresh();
    }

    /**
     * Simply uses UrlOpener to prompt the user to open the given URL. The
     * prompt is centered on this object (the text pane).
     * 
     * @param url 
     */
    @Override
    public void linkClicked(String url) {
        UrlOpener.openUrlPrompt(this.getTopLevelAncestor(), url);
    }
    
    private class ScrollManager {
        
        private JScrollPane scrollpane;
        
        /**
         * When the scroll position was last changed.
         */
        private long lastChanged = 0;
        
        /**
         * The last scroll position.
         * 
         * TODO: This may not be necessariy anymore, since changes are recorded
         * using an AdjustmentListener.
         */
        private int lastScrollPosition = 0;
        
        /**
         * The number of second to allow it to be scrolled up without changing
         * the scroll position.
         */
        private static final int SCROLLED_UP_TIMEOUT = 30;
        
        /**
         * The most recent width/height of the scrollpane, used to determine
         * whether it is decreased.
         */
        private int width;
        private int height;
        
        public void setScrollPane(JScrollPane pane) {
            this.scrollpane = pane;
            addListeners();
        }
        
        private void addListeners() {
            
            // Listener to detect when the scrollpane was reduced in size, so
            // scrolling down might be necessary
            scrollpane.addComponentListener(new ComponentAdapter() {
                
                @Override
                public void componentResized(ComponentEvent e) {
                    Component c = e.getComponent();
                    if (c.getWidth() < width || c.getHeight() < height) {
                        scrollDown();
                    }
                    width = c.getWidth();
                    height = c.getHeight();
                }
            
            });
            
            // Listener to detect when the scroll position was last changed
            scrollpane.getVerticalScrollBar().addAdjustmentListener(
                    new AdjustmentListener() {

                        @Override
                        public void adjustmentValueChanged(AdjustmentEvent e) {
                            if (!e.getValueIsAdjusting()
                                    && e.getValue() != lastScrollPosition) {
                                lastChanged = System.currentTimeMillis();
                                lastScrollPosition = e.getValue();
                            }
                        }
                    });
        }
        
        /**
         * Checks if the scroll position is at the end of the document, with
         * some margin or error.
         *
         * @return true if scroll position is at the end, false otherwise
         */
        private boolean isScrollpositionAtTheEnd() {
            JScrollBar vbar = scrollpane.getVerticalScrollBar();
            return vbar.getMaximum() - 20 <= vbar.getValue() + vbar.getVisibleAmount();
        }

        /**
         * If enabled, checks whether the time that has passed since the scroll
         * position was last changed is greater than the defined timeout.
         *
         * @return {@code true} if the timeout was exceeded, {@code false}
         * otherwise
         */
        private boolean scrolledUpTimeout() {
            if (!styles.autoScroll()) {
                return false;
            }
            //JScrollBar vbar = scrollpane.getVerticalScrollBar();
            //int current = vbar.getValue();
            //System.out.println("Scroll"+current);
//            if (current != lastScrollPosition) {
//                lastChanged = System.currentTimeMillis();
//                //System.out.println("changed");
//                lastScrollPosition = current;
//            }
            long timePassed = System.currentTimeMillis() - lastChanged;
            //System.out.println(timePassed);
            if (timePassed > 1000 * styles.autoScrollTimeout()) {
                LOGGER.info("ScrolledUp Timeout (" + timePassed + ")");
                return true;
            }
            return false;
        }

        /**
         * Scrolls to the very end of the document.
         */
        private void scrollDown() {
            try {
                int endPosition = doc.getLength();
                Rectangle bottom = modelToView(endPosition);
                // Apparently bottom can be null if the component isn't yet
                // established correctly yet, which happens if you open a
                // channel from a popout dialog
                /**
[15:03] You have joined #tirean[15:03] , , , , 
[15:03] ~Stream offline~
                 */
                if (bottom != null) {
                    bottom.height = bottom.height + 100;
                    scrollRectToVisible(bottom);
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }

        /**
         * Scrolls to the given offset in the document.
         * 
         * @param offset 
         */
        private void scrollToOffset(int offset) {
            try {
                Rectangle rect = modelToView(offset);
                scrollRectToVisible(rect);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }
        
    }
    
    /**
     * Manages everything to do with styles (AttributeSets).
     */
    class Styles {
        /**
         * Styles that are get from the StyleServer
         */
        private final String[] baseStyles = new String[]{"standard","special","info","base","highlight","paragraph"};
        /**
         * Stores the styles
         */
        private final HashMap<String,MutableAttributeSet> styles = new HashMap<>();
        /**
         * Stores immutable/unmodified copies of the styles got from the
         * StyleServer for comparison
         */
        private final HashMap<String,AttributeSet> rawStyles = new HashMap<>();
        /**
         * Stores boolean settings
         */
        private final HashMap<Setting, Boolean> settings = new HashMap<>();
        
        private final Map<Setting, Integer> numericSettings = new HashMap<>();
        /**
         * Stores all the style types that were changed in the most recent update
         */
        private final ArrayList<String> changedStyles = new ArrayList<>();
        /**
         * Key for the style type attribute
         */
        private final String TYPE = "ChannelTextPanel Style Type";
        /**
         * Store the timestamp format
         */
        private SimpleDateFormat timestampFormat;

        /**
         * Icons that have been modified for use and saved into a style. Should
         * only be done once per icon.
         */
        private final HashMap<ImageIcon, MutableAttributeSet> savedIcons = new HashMap<>();
        
        /**
         * Creates a new ImageIcon based on the given ImageIcon that has a small
         * space on the side, so it can be displayed properly.
         * 
         * @param icon
         * @return 
         */
        private ImageIcon addSpaceToIcon(ImageIcon icon) {
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            int hspace = 3;
            BufferedImage res = new BufferedImage(width + hspace, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = res.getGraphics();
            g.drawImage(icon.getImage(), 0, 0, null);
            g.dispose();

            return new ImageIcon(res);
        }
        
        /**
         * Get the current styles from the StyleServer and also set some
         * other special styles based on that.
         * 
         * @return 
         */
        public boolean setStyles() {
            changedStyles.clear();
            boolean somethingChanged = false;
            for (String styleName : baseStyles) {
                if (loadStyle(styleName)) {
                    somethingChanged = true;
                    changedStyles.add(styleName);
                }
            }
            
            // Additional styles
            SimpleAttributeSet nick = new SimpleAttributeSet(base());
            StyleConstants.setBold(nick, true);
            styles.put("nick", nick);
            
            MutableAttributeSet paragraph = styles.get("paragraph");
            //StyleConstants.setLineSpacing(paragraph, 0.3f);
            paragraph.addAttribute(Attribute.DELETED_LINE, false);
            styles.put("paragrahp", paragraph);
            
            SimpleAttributeSet deleted = new SimpleAttributeSet();
            StyleConstants.setStrikeThrough(deleted, true);
            StyleConstants.setUnderline(deleted, false);
            deleted.addAttribute(Attribute.URL_DELETED, true);
            styles.put("deleted", deleted);
            
            SimpleAttributeSet deletedLine = new SimpleAttributeSet();
            deletedLine.addAttribute(Attribute.DELETED_LINE, true);
            //StyleConstants.setAlignment(deletedLine, StyleConstants.ALIGN_RIGHT);
            styles.put("deletedLine", deletedLine);
            
            SimpleAttributeSet searchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(searchResult, styleServer.getColor("searchResult"));
            styles.put("searchResult", searchResult);
            
            SimpleAttributeSet clearSearchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(clearSearchResult, new Color(0,0,0,0));
            styles.put("clearSearchResult", clearSearchResult);
            
            setBackground(styleServer.getColor("background"));
            
            // Load other stuff from the StyleServer
            setSettings();
            
            return somethingChanged;
        }
        
        /**
         * Loads some settings from the StyleServer.
         */
        private void setSettings() {
            addSetting(Setting.EMOTICONS_ENABLED,true);
            addSetting(Setting.USERICONS_ENABLED, true);
            addSetting(Setting.TIMESTAMP_ENABLED, true);
            addSetting(Setting.SHOW_BANMESSAGES, false);
            addSetting(Setting.AUTO_SCROLL, true);
            addSetting(Setting.DELETE_MESSAGES, false);
            addSetting(Setting.ACTION_COLORED, false);
            addSetting(Setting.COMBINE_BAN_MESSAGES, true);
            addNumericSetting(Setting.DELETED_MESSAGES_MODE, 30, -1, 9999999);
            addNumericSetting(Setting.BUFFER_SIZE, 250, BUFFER_SIZE_MIN, BUFFER_SIZE_MAX);
            addNumericSetting(Setting.AUTO_SCROLL_TIME, 30, 5, 1234);
            timestampFormat = styleServer.getTimestampFormat();
        }
        
        /**
         * Gets a single boolean setting from the StyleServer.
         * 
         * @param key
         * @param defaultValue 
         */
        private void addSetting(Setting key, boolean defaultValue) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            boolean result = defaultValue;
            if (obj != null && obj instanceof Boolean) {
                result = (Boolean)obj;
            }
            settings.put(key, result);
        }
        
        private void addNumericSetting(Setting key, int defaultValue, int min, int max) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            int result = defaultValue;
            if (obj != null && obj instanceof Number) {
                result = ((Number)obj).intValue();
            }
            if (result > max) {
                result = max;
            }
            if (result < min) {
                result = min;
            }
            numericSettings.put(key, result);
        }
        
        /**
         * Retrieves a single style from the StyleServer and compares it to
         * the previously saved style.
         * 
         * @param name
         * @return true if the style has changed, false otherwise
         */
        private boolean loadStyle(String name) {
            MutableAttributeSet newStyle = styleServer.getStyle(name);
            AttributeSet oldStyle = rawStyles.get(name);
            if (oldStyle != null && oldStyle.isEqual(newStyle)) {
                // Nothing in the style has changed, so nothing further to do
                return false;
            }
            // Save immutable copy of new style for next comparison
            rawStyles.put(name, newStyle.copyAttributes());
            // Add type attribute to the style, so it can be recognized when
            // refreshing the styles in the document
            newStyle.addAttribute(TYPE, name);
            styles.put(name, newStyle);
            return true;
        }
        
        /**
         * Retrieves the current styles and updates the elements in the document
         * as necessary. Scrolls down since font size changes and such could
         * move the scroll position.
         */
        public void refresh() {
            if (!setStyles()) {
                return;
            }

            LOGGER.info("Update styles (only types "+changedStyles+")");
            Element root = doc.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element line = root.getElement(i);
                for (int j = 0; j < line.getElementCount(); j++) {
                    Element element = line.getElement(j);
                    String type = (String)element.getAttributes().getAttribute(TYPE);
                    int start = element.getStartOffset();
                    int end = element.getEndOffset();
                    int length = end - start;
                    if (type == null) {
                        type = "base";
                    }
                    MutableAttributeSet style = styles.get(type);
                    // Only change style if this style type was different from
                    // the previous one
                    // (seems to be faster than just setting all styles)
                    if (changedStyles.contains(type)) {
                        if (type.equals("paragraph")) {
                            //doc.setParagraphAttributes(start, length, rawStyles.get(type), false);
                        } else {
                            doc.setCharacterAttributes(start, length, style, false);
                        }
                    }
                }
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    scrollManager.scrollDown();
                }
            });
        }
        
        public MutableAttributeSet base() {
            return styles.get("base");
        }
        
        public MutableAttributeSet info() {
            return styles.get("info");
        }
        
        public MutableAttributeSet compact() {
            return styles.get("special");
        }
        
        public MutableAttributeSet standard(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(standard());
                StyleConstants.setForeground(specialColor, color);
                return specialColor;
            }
            return standard();
        }
        
        public MutableAttributeSet standard() {
            return styles.get("standard");
        }
        
        public MutableAttributeSet banMessage(User user) {
            MutableAttributeSet style = new SimpleAttributeSet(standard());
            style.addAttribute(Attribute.BAN_MESSAGE, user);
            style.addAttribute(Attribute.TIMESTAMP, System.currentTimeMillis());
            return style;
        }
        
        public MutableAttributeSet banMessageCount(int count) {
            MutableAttributeSet style = new SimpleAttributeSet(info());
            style.addAttribute(Attribute.BAN_MESSAGE_COUNT, count);
            return style;
        }
        
        public MutableAttributeSet nick() {
            return styles.get("nick");
        }
        
        public MutableAttributeSet deleted() {
            return styles.get("deleted");
        }
        
        public MutableAttributeSet deletedLine() {
            return styles.get("deletedLine");
        }
        
        public MutableAttributeSet paragraph() {
            return styles.get("paragrahp");
        }
        
        public MutableAttributeSet highlight(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(highlight());
                StyleConstants.setForeground(specialColor, color);
                return specialColor;
            }
            return highlight();
        }
        
        public MutableAttributeSet highlight() {
            return styles.get("highlight");
        }
        
        public MutableAttributeSet searchResult() {
            return styles.get("searchResult");
        }
        
        public MutableAttributeSet clearSearchResult() {
            return styles.get("clearSearchResult");
        }
        
        /**
         * Makes a style for the given User, containing the User-object itself
         * and the user-color. Changes the color to hopefully improve readability.
         * 
         * @param user The User-object to base this style on
         * @return 
         */
        public MutableAttributeSet nick(User user, MutableAttributeSet style) {
            SimpleAttributeSet userStyle;
            if (style == null) {
                userStyle = new SimpleAttributeSet(nick());
                userStyle.addAttribute(Attribute.USER_MESSAGE, true);
                Color userColor = user.getColor();
                if (!user.hasChangedColor()) {
                    userColor = HtmlColors.correctReadability(userColor, getBackground());
                    user.setCorrectedColor(userColor);
                }
                StyleConstants.setForeground(userStyle, userColor);
            }
            else {
                userStyle = new SimpleAttributeSet(style);
            }
            userStyle.addAttribute(Attribute.USER, user);
            return userStyle;
        }
        
        public MutableAttributeSet subscriberIcon() {
            return styles.get("subscriber");
        }
        
        public java.util.List<MutableAttributeSet> getAddonIconStyles(User user) {
            java.util.List<ImageIcon> icons = user.getAddonIcons();
            java.util.List<MutableAttributeSet> iconStyles = new ArrayList<>();
            for (ImageIcon icon : icons) {
                iconStyles.add(makeIconStyle(icon));
            }
            return iconStyles;
        }
        
        public MutableAttributeSet getIconStyle(User user, int type) {
            return makeIconStyle(user.getIcon(type));
        }
        
        /**
         * Creates a style for the given icon. Also modifies the icon to add a
         * little space on the side, so it can be displayed easier. It caches
         * styles, so it only needs to create the style and modify the icon
         * once.
         * 
         * @param icon
         * @return The created style (or read from the cache)
         */
        public MutableAttributeSet makeIconStyle(ImageIcon icon) {
            MutableAttributeSet style = savedIcons.get(icon);
            if (style == null) {
                //System.out.println("Creating icon style: "+icon);
                style = new SimpleAttributeSet(nick());
                if (icon != null) {
                    StyleConstants.setIcon(style, addSpaceToIcon(icon));
                }
                savedIcons.put(icon, style);
            }
            return style;
        }

        public boolean showTimestamp() {
            return settings.get(Setting.TIMESTAMP_ENABLED);
        }
        
        public boolean showUsericons() {
            return settings.get(Setting.USERICONS_ENABLED);
        }
        
        public boolean showEmoticons() {
            return settings.get(Setting.EMOTICONS_ENABLED);
        }
        
        public boolean showBanMessages() {
            return settings.get(Setting.SHOW_BANMESSAGES);
        }
        
        public boolean combineBanMessages() {
            return settings.get(Setting.COMBINE_BAN_MESSAGES);
        }
        
        public boolean autoScroll() {
            return settings.get(Setting.AUTO_SCROLL);
        }
        
        public Integer autoScrollTimeout() {
            return numericSettings.get(Setting.AUTO_SCROLL_TIME);
        }
        
        public boolean actionColored() {
            return settings.get(Setting.ACTION_COLORED);
        }
        
        public boolean deleteMessages() {
            return settings.get(Setting.DELETE_MESSAGES);
        }

        public int deletedMessagesMode() {
            return (int)numericSettings.get(Setting.DELETED_MESSAGES_MODE);
        }
        
        /**
         * Make a link style for the given URL.
         * 
         * @param url
         * @return 
         */
        public MutableAttributeSet url(String url) {
            SimpleAttributeSet urlStyle = new SimpleAttributeSet(standard());
            StyleConstants.setUnderline(urlStyle, true);
            urlStyle.addAttribute(HTML.Attribute.HREF, url);
            return urlStyle;
        }
        
        /**
         * Make a style with the given icon.
         * 
         * @param icon
         * @return 
         */
        public MutableAttributeSet emoticon(Emoticon emoticon) {
            // Does this need any other attributes e.g. standard?
            SimpleAttributeSet emoteStyle = new SimpleAttributeSet();
            StyleConstants.setIcon(emoteStyle, emoticon.getIcon(ChannelTextPane.this));
            emoteStyle.addAttribute(Attribute.EMOTICON, emoticon);
            if (!emoticon.hasStreamSet()) {
                emoticon.setStream(main.emoticons.getStreamFromEmoteset(emoticon.emoteSet));
            }
            return emoteStyle;
        }
        
        public SimpleDateFormat timestampFormat() {
            return timestampFormat;
        }
        
        public int bufferSize() {
            return (int)numericSettings.get(Setting.BUFFER_SIZE);
        }
    }
    
    
}

/**
 * Adds a way to refresh the (whole) document.
 * 
 * This is currently used to display Icons after they are fully loaded, although
 * there should be a better way to do this.
 * 
 * @author tduva
 */
class MyDocument extends DefaultStyledDocument {
    
    public void refresh() {
        refresh(0, getLength());
    }
    
    public void refresh(int offset, int len) {
        DefaultDocumentEvent changes = new DefaultDocumentEvent(offset,len, DocumentEvent.EventType.CHANGE);
        Element root = getDefaultRootElement();
        Element[] removed = new Element[0];
        Element[] added = new Element[0];
        changes.addEdit(new ElementEdit(root, 0, removed, added));
        changes.end();
        fireChangedUpdate(changes);
    }

}

/**
 * Replaces some Views by custom ones to change display behaviour.
 * 
 * @author tduva
 */
class MyEditorKit extends StyledEditorKit {

    @Override
    public ViewFactory getViewFactory() {
        return new StyledViewFactory();
    }
 
    static class StyledViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new MyParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new ChatBoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new MyIconView(elem);
                }
            }
            return new LabelView(elem);
        }

    }
}

/**
 * Changes the FlowStrategy to increase performance when i18n is enabled in the
 * Document. Not quite sure why this works.. ;) (This may work because by
 * default the strategy is a singleton shared by all instances, which may reduce
 * performance if all instances have to use a i18n stragety when one character
 * that requires it is inserted.)
 * 
 * @author tduva
 */
class MyParagraphView extends ParagraphView {
    
    public static int MAX_VIEW_SIZE = 50;
    
    public MyParagraphView(Element elem) {
        super(elem);
        //System.out.println(strategy.getClass());
        strategy = new MyParagraphView.MyFlowStrategy();
        //System.out.println(strategy.getClass());
    }
    
    public static class MyFlowStrategy extends FlowStrategy {
        
        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View res = super.createView(fv, startOffset, spanLeft, rowIndex);
            
            if (res.getEndOffset() - res.getStartOffset() > MAX_VIEW_SIZE) {
                //res = res.createFragment(startOffset, startOffset + MAX_VIEW_SIZE);
            }
            return res;
        }
    }
    
//    @Override
//    public int getResizeWeight(int axis) {
//        return 0;
//    }
}


/**
 * Starts adding text at the bottom instead of at the top.
 * 
 * @author tduva
 */
class ChatBoxView extends BoxView {
    
    public ChatBoxView(Element elem, int axis) {
        super(elem,axis);
    }
    
    @Override
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {

        super.layoutMajorAxis(targetSpan,axis,offsets,spans);
        int textBlockHeight = 0;
        int offset = 0;
 
        for (int i = 0; i < spans.length; i++) {

            textBlockHeight += spans[i];
        }
        offset = (targetSpan - textBlockHeight);
        //System.out.println(offset);
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] += offset;
        }

    }
}    

/**
 * Always wrap long words.
 * 
 * @author tduva
 */
class WrapLabelView extends LabelView {

    public WrapLabelView(Element elem) {
        super(elem);
        //System.out.println(elem);
    }

    /**
     * Always return 0 for the X_AXIS of the minimum span, so long words are
     * always wrapped.
     * 
     * @param axis
     * @return 
     */
    @Override
    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }
    
//    public int getBreakWeight(int axis, float pos, float len) {
//        if (axis == View.X_AXIS) {
//            return View.ForcedBreakWeight;
//        }
//        return super.getBreakWeight(axis, pos, len);
//    }
}

/**
 * Changes the position of the icon slightly, so it overlaps with following text
 * as to not take as much space. Not perfect still, but ok.
 * 
 * @author tduva
 */
class MyIconView extends IconView {
    public MyIconView(Element elem) {
        super(elem);
    }
    
    private static final int lineHeight = 20;
    
    @Override
    public float getAlignment(int axis) {
        //System.out.println(this.getElement());

        if (axis ==  View.Y_AXIS) {
            //System.out.println(this.getElement());
//            float height = super.getPreferredSpan(axis);
//            double test = 1.5 - lineHeight / height * 0.5;
//            System.out.println(height+" "+test+" "+this.getAttributes());
//            return (float)test;
            return 1f;
            
        }
        return super.getAlignment(axis);
    }
    
    @Override
    public float getPreferredSpan(int axis) {
        if (axis == View.Y_AXIS) {
            float height = super.getPreferredSpan(axis);
//            float test = lineHeight / height;
            float test = 0.7f;
            //System.out.println(test);
            height *= test;
            return height;
        }
        return super.getPreferredSpan(axis);
    }
    
    /**
     * Wrap Icon Labels as well, to prevent horizontal scrolling when a row of
     * continuous emotes (without space) is present.
     * 
     * @param axis
     * @return 
     */
    @Override
    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }
}

/**
 * Detects any clickable text in the document and reacts accordingly. It shows
 * the appropriate cursor when moving over it with the mouse and reacts to
 * clicks on clickable text.
 * 
 * It knows to look for links and User objects at the moment.
 * 
 * @author tduva
 */
class LinkController extends MouseAdapter implements MouseMotionListener {
    
    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Cursor NORMAL_CURSOR = Cursor.getDefaultCursor();
    
    /**
     * When a User is clicked, the User object is send here
     */
    private UserListener userListener;
    /**
     * When a link is clicked, the String with the url is send here
     */
    private LinkListener linkListener;
    
    private MouseClickedListener mouseClickedListener;
    
    private ContextMenuListener contextMenuListener;
    
    private ContextMenu defaultContextMenu;
    
    /**
     * Set the object that should receive the User object once a User is clicked
     * 
     * @param listener 
     */
    public void setUserListener(UserListener listener) {
        userListener = listener;
    }
    
    /**
     * Set the object that should receive the url String once a link is clicked
     * 
     * @param listener 
     */
    public void setLinkListener(LinkListener listener) {
        linkListener = listener;
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        mouseClickedListener = listener;
    }
    
    /**
     * Set the listener for all context menus.
     * 
     * @param listener 
     */
    public void setContextMenuListener(ContextMenuListener listener) {
        contextMenuListener = listener;
        if (defaultContextMenu != null) {
            defaultContextMenu.addContextMenuListener(listener);
        }
    }
    
    /**
     * Set the context menu for when no special context menus (user, link) are
     * appropriate.
     * 
     * @param contextMenu 
     */
    public void setDefaultContextMenu(ContextMenu contextMenu) {
        defaultContextMenu = contextMenu;
        contextMenu.addContextMenuListener(contextMenuListener);
    }
   
    /**
     * Handles mouse presses. This is favourable to mouseClicked because it
     * might work better in a fast moving chat and you won't select text
     * instead of opening userinfo etc.
     * 
     * @param e 
     */
    @Override
    public void mousePressed(MouseEvent e) {
        
        if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
            String url = getUrl(e);
            if (url != null && !isUrlDeleted(e)) {
                if (linkListener != null) {
                    linkListener.linkClicked(url);
                }
                return;
            }
            User user = getUser(e);
            if (user != null) {
                if (userListener != null) {
                    userListener.userClicked(user);
                }
                return;
            }
        }
        else if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    /**
     * Handle clicks (pressed and released) on the text pane.
     * 
     * @param e 
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseClickedListener != null && e.getClickCount() == 1) {
            // Doing this on mousePressed will prevent selection of text,
            // because this is used to change the focus to the input
            mouseClickedListener.mouseClicked();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        
        JTextPane text = (JTextPane)e.getSource();
        
        String url = getUrl(e);
        if ((url != null && !isUrlDeleted(e)) || getUser(e) != null) {
            text.setCursor(HAND_CURSOR);
        } else {
            text.setCursor(NORMAL_CURSOR);
        }
    }
    
    /**
     * Gets the URL from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The URL or null if none was found.
     */
    private String getUrl(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (String)(attributes.getAttribute(HTML.Attribute.HREF));
        }
        return null;
    }
    
    private boolean isUrlDeleted(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            Boolean deleted = (Boolean)attributes.getAttribute(ChannelTextPane.Attribute.URL_DELETED);
            if (deleted == null) {
                return false;
            }
            return (Boolean)(deleted);
        }
        return false;
    }
    
    /**
     * Gets the User object from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The User object or null if none was found.
     */
    private User getUser(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (User)(attributes.getAttribute(ChannelTextPane.Attribute.USER));
        }
        return null;
    }
    
    private Emoticon getEmoticon(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (Emoticon)(attributes.getAttribute(ChannelTextPane.Attribute.EMOTICON));
        }
        return null;
    }
    
    /**
     * Gets the attributes from the element in the document the mouse is
     * pointing at.
     * 
     * @param e
     * @return The attributes of this element or null if the mouse wasn't
     *          pointing at an element
     */
    private AttributeSet getAttributes(MouseEvent e) {
        JTextPane text = (JTextPane)e.getSource();
        Point mouseLocation = new Point(e.getX(), e.getY());
        int pos = text.viewToModel(mouseLocation);
        
        if (pos >= 0) {
            
            /**
             * Check if the found element is actually located where the mouse
             * is pointing, and if not try the previous element.
             * 
             * This is a fix to make detection of emotes more reliable. The
             * viewToModel() method apparently searches for the closest element
             * to the given position, disregarding the size of the elements,
             * which means on the right side of an emote the next (non-emote)
             * element is nearer.
             * 
             * See also: http://stackoverflow.com/questions/24036650/detecting-image-on-current-mouse-position-only-works-on-part-of-image
             */
            try {
                Rectangle rect = text.modelToView(pos);
                if (e.getX() < rect.x && e.getY() < rect.y + rect.height && pos > 0) {
                    pos--;
                }
            } catch (BadLocationException ex) {
                
            }

            StyledDocument doc = text.getStyledDocument();
            Element element = doc.getCharacterElement(pos);
            return element.getAttributes();
        }
        return null;
    }
    
    private void openContextMenu(MouseEvent e) {
        // Component to show the context menu on has to be showing to determine
        // it's location (it might not be showing if the channel changed after
        // the click)
        if (!e.getComponent().isShowing()) {
            return;
        }
        User user = getUser(e);
        String url = getUrl(e);
        Emoticon emote = getEmoticon(e);
        JPopupMenu m;
        if (user != null) {
            m = new UserContextMenu(user, contextMenuListener);
        }
        else if (url != null) {
            m = new UrlContextMenu(url, isUrlDeleted(e), contextMenuListener);
        }
        else if (emote != null) {
            m = new EmoteContextMenu(emote, contextMenuListener);
        }
        else {
            if (defaultContextMenu == null) {
                m = new ChannelContextMenu(contextMenuListener);
            } else {
                m = defaultContextMenu;
            }
        }
        m.show(e.getComponent(), e.getX(), e.getY());
    }
    
}