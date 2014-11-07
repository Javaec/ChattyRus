
package chatty;

import chatty.util.Replacer;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JOptionPane;

/**
 * Some static helper methods.
 * 
 * @author tduva
 */
public class Helper {
    
    public static final DecimalFormat VIEWERCOUNT_FORMAT = new DecimalFormat();
    
    public static String formatViewerCount(int viewerCount) {
        return VIEWERCOUNT_FORMAT.format(viewerCount);
    }
    
    public static String[] parseChannels(String channels, boolean prepend) {
        String[] parts = channels.split(",");
        Vector<String> result = new Vector<>();
        for (String part : parts) {
            String channel = part.trim();
            if (validateChannel(channel)) {
                if (prepend && !channel.startsWith("#")) {
                    channel = "#"+channel;
                }
                result.add(Helper.toLowerCase(channel));
            }
        }
        String[] resultArray = new String[result.size()];
        result.copyInto(resultArray);
        return resultArray;
    }
    
    public static String[] parseChannels(String channels) {
        return parseChannels(channels, true);
    }
    
    /**
     * Takes a Set of Strings and builds a single comma-seperated String of
     * streams out of it.
     * 
     * @param set
     * @return 
     */
    public static String buildStreamsString(Set<String> set) {
        String result = "";
        String sep = "";
        for (String channel : set) {
            result += sep+channel.replace("#", "");
            sep = ", ";
        }
        return result;
    }
    
    public static boolean validateChannel(String channel) {
        try {
            return channel.matches("(?i)^#{0,1}[a-z0-9_]+$");
        } catch (PatternSyntaxException | NullPointerException ex) {
            return false;
        }
    }
    
    public static boolean validateStream(String stream) {
        try {
            return stream.matches("(?i)^[a-z0-9_]+$");
        } catch (PatternSyntaxException | NullPointerException ex) {
            return false;
        }
    }
    
    /**
     * Checks if the given stream/channel is valid and turns it into a channel
     * if necessary.
     *
     * @param channel The channel, valid or invalid, leading # or not.
     * @return The channelname with leading #, or null if channel was invalid.
     */
    public static String checkChannel(String channel) {
        if (channel == null) {
            return null;
        }
        if (!validateChannel(channel)) {
            return null;
        }
        if (!channel.startsWith("#")) {
            channel = "#"+channel;
        }
        return Helper.toLowerCase(channel);
    }
    
    /**
     * Removes any # from the String.
     * 
     * @param channel
     * @return 
     */
    public static String toStream(String channel) {
        if (channel == null) {
            return null;
        }
        return channel.replace("#", "");
    }
    
    /**
     * Makes a readable message out of the given reason code.
     * 
     * @param reason
     * @param reasonMessage
     * @return 
     */
    public static String makeDisconnectReason(int reason, String reasonMessage) {
        String result = "";
        
        switch (reason) {
            case Irc.ERROR_UNKNOWN_HOST:
                result = "Unknown host";
                break;
            case Irc.REQUESTED_DISCONNECT:
                result = "Requested";
                break;
            case Irc.ERROR_CONNECTION_CLOSED:
                result = "";
                break;
            case Irc.ERROR_REGISTRATION_FAILED:
                result = "Failed to complete login.";
                break;
            case Irc.ERROR_SOCKET_TIMEOUT:
                result = "Connection timed out.";
                break;
        }
        
        if (!result.isEmpty()) {
            result = " ("+result+")";
        }
        
        return result;
    }
    
    /**
     * Tries to turn the given Object into a List of Strings.
     * 
     * If the given Object is a List, go through all items and copy those
     * that are Strings into a new List of Strings.
     * 
     * @param obj
     * @return 
     */
    public static List<String> getStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
        }
        return result;
    }
    
    /**
     * Copy the given text to the clipboard.
     * 
     * @param text 
     */
    public static void copyToClipboard(String text) {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(text), null);
    }
    
    public static void openFolder(File folder, Component parent) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Opening folder failed.");
        }
    }
    
    public static String join(Collection<String> items, String delimiter) {
        StringBuilder b = new StringBuilder();
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(delimiter);
            }
        }
        return b.toString();
    }
    
    
    /**
     * http://stackoverflow.com/questions/5609500/remove-jargon-but-keep-real-characters/5609532#5609532
     */
    
    //private static final Pattern TEST = Pattern.compile("[\\u0300-\\u036f\\u0483-\\u0489\\u1dc0-\\u1dff\\u20d0-\\u20ff\\ufe20-\\ufe2f]{3,}");
    private static final Pattern COMBINING_CHARACTERS = Pattern.compile("[\\u0300-\\u036f\\u0483-\\u0489\\u1dc0-\\u1dff\\u20d0-\\u20ff\\ufe20-\\ufe2f]");
    
//    public static String removeTest(String text) {
//        return TEST.matcher(text).replaceAll("");
//    }
    
    public static String removeCombiningCharacters(String text) {
        return COMBINING_CHARACTERS.matcher(text).replaceAll("");
    }
    
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    
    public static String removeDuplicateWhitespace(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ");
    }
    
    private static final Pattern ALL_UPERCASE_LETTERS = Pattern.compile("[A-Z]+");
    
    public static boolean isAllUppercaseLetters(String text) {
        return ALL_UPERCASE_LETTERS.matcher(text).matches();
    }
    
    private static final Replacer HTMLSPECIALCHARS_ENCODE;
    private static final Replacer HTMLSPECIALCHARS_DECODE;
    
    static {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("&amp;", "&");
        replacements.put("&lt;", "<");
        replacements.put("&gt;", ">");
        replacements.put("&quot;", "\"");
        
        Map<String, String> replacementsReverse = new HashMap<>();
        for (String key : replacements.keySet()) {
            replacementsReverse.put(replacements.get(key), key);
        }
        HTMLSPECIALCHARS_ENCODE = new Replacer(replacementsReverse);
        HTMLSPECIALCHARS_DECODE = new Replacer(replacements);
    }
    
    public static String htmlspecialchars_decode(String s) {
        if (s == null) {
            return null;
        }
        return HTMLSPECIALCHARS_DECODE.replace(s);
    }
    
    public static String htmlspecialchars_encode(String s) {
        if (s == null) {
            return null;
        }
        return HTMLSPECIALCHARS_ENCODE.replace(s);
    }
    
    private static final Pattern REMOVE_LINEBREAKS = Pattern.compile("\\r?\\n");
    
    /**
     * Removes any linebreaks from the given String.
     * 
     * @param s The String (can be empty or null)
     * @return The modified String or null if the given String was null
     */
    public static String removeLinebreaks(String s) {
        if (s == null) {
            return null;
        }
        return REMOVE_LINEBREAKS.matcher(s).replaceAll(" ");
    }
    
    private static final Pattern UNDERSCORE = Pattern.compile("_");
    
    public static String replaceUnderscoreWithSpace(String input) {
        return UNDERSCORE.matcher(input).replaceAll(" ");
    }
    
    public static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }
    
    public static String toLowerCase(String s) {
        return s != null ? s.toLowerCase(Locale.ENGLISH) : null;
    }
    
    public static <T> List<T> subList(List<T> list, int start, int end) {
        List<T> subList = new ArrayList<>();
        for (int i=start;i<end;i++) {
            if (list.size() > i) {
                subList.add(list.get(i));
            } else {
                break;
            }
        }
        return subList;
    }
    
    public static void unhandledException() {
        String[] a = new String[0];
        String b = a[1];
    }
    
    public static boolean arrayContainsInt(int[] array, int test) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == test) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Splits up a String in the format "Integer1,Integer2" and returns the
     * {@code Integer}s.
     *
     * @param input The input String
     * @return Both {@code Integer} values as a {@code IntegerPair} or
     * {@code null} if the format was invalid
     * @see IntegerPair
     */
    public static IntegerPair getNumbersFromString(String input) {
        String[] split = input.split(",");
        if (split.length != 2) {
            return null;
        }
        try {
            int a = Integer.parseInt(split[0]);
            int b = Integer.parseInt(split[1]);
            return new IntegerPair(a, b);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /**
     * Gets two {@code Integer} values on creation, which can be accessed with
     * the {@code final} attributes {@code a} and {@code b}.
     */
    public static class IntegerPair {
        public final int a;
        public final int b;
        
        public IntegerPair(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
    
    /**
     * Shortens the given {@code input} to the {@code max} length. Only changes
     * the {@code input} if it actually exceeds {@code max} length, but if it
     * does, the returning text is 2 shorter than {@code max} because it also adds
     * ".." where it shortened the text.
     * 
     * Positive {@code max} length values shorten the {@code input} at the end,
     * negative values shorten the {@code input} at the start.
     * 
     * @param input The {@code String} to shorten
     * @param max The maximum length the String should have after this
     * @return The modified {@code String} if {@code input} exceeds the
     * {@code max} length, the original value otherwise
     */
    public static String shortenTo(String input, int max) {
        if (input != null && input.length() > Math.abs(max)) {
            if (max > 2) {
                return input.substring(0, max-2)+"..";
            } else if (max < -2) {
                return ".."+input.substring(input.length() + max + 2 ); // abcd      -3
            } else {
                return "..";
            }
        }
        return input;
    }
    
    public static final void main(String[] args) {
//        System.out.println(htmlspecialchars_encode("< >"));
//        System.out.println(shortenTo("abcd", 0));
//        System.out.println(shortenTo("abcd", 1));
//        System.out.println(shortenTo("abcd", 2));
//        System.out.println(shortenTo("abcd", 3));
//        System.out.println(shortenTo("abcd", 4));
//        System.out.println(shortenTo("abcd", 5));
//        System.out.println(shortenTo("abcd", -2));
//        System.out.println(shortenTo("abcd", -3));
//        System.out.println(shortenTo("abcd", -4));
    }
    
    public static boolean matchUserStatus(String id, User user) {
        if (id.equals("$mod")) {
            if (user.isModerator()) {
                return true;
            }
        } else if (id.equals("$sub")) {
            if (user.isSubscriber()) {
                return true;
            }
        } else if (id.equals("$turbo")) {
            if (user.hasTurbo()) {
                return true;
            }
        } else if (id.equals("$admin")) {
            if (user.isAdmin()) {
                return true;
            }
        } else if (id.equals("$broadcaster")) {
            if (user.isBroadcaster()) {
                return true;
            }
        } else if (id.equals("$staff")) {
            if (user.isStaff()) {
                return true;
            }
        }
        return false;
    }
    
    public static String checkHttpUrl(String url) {
        if (url.startsWith("//")) {
            url = "http:"+url;
        }
        return url;
    }
    
    public static String systemInfo() {
        return "Java: "+System.getProperty("java.version")+" ("
                +System.getProperty("java.vendor")
                +") OS: "+System.getProperty("os.name")+" ("
                +System.getProperty("os.version")
                +"/"+System.getProperty("os.arch")+")";
    }
    
}
