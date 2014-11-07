
package chatty.gui;

import chatty.gui.components.ChannelTextPane.Setting;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.swing.text.*;

/**
 * Provides style information to other objects based on the settings.
 * 
 * @author tduva
 */
public class StyleManager implements StyleServer {
    
    private final static Logger LOGGER = Logger.getLogger(StyleManager.class.getName());
    
    public static final Set<String> settingNames = new HashSet<>(Arrays.asList(
            "font", "fontSize", "timestampEnabled", "emoticonsEnabled",
            "foregroundColor","infoColor","compactColor","backgroundColor",
            "inputBackgroundColor","inputForegroundColor","usericonsEnabled",
            "timestamp","highlightColor","showBanMessages","autoScroll",
            "deletedMessagesMode", "deletedMessagesMaxLength","searchResultColor",
            "lineSpacing", "bufferSize", "actionColored","combineBanMessages",
            "timestampTimezone", "autoScrollTimeout"
            ));
    
    private MutableAttributeSet baseStyle;
    private MutableAttributeSet standardStyle;
    private MutableAttributeSet specialStyle;
    private MutableAttributeSet infoStyle;
    private MutableAttributeSet paragraphStyle;
    private MutableAttributeSet other;
    private MutableAttributeSet highlightStyle;
    private Font font;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color inputBackgroundColor;
    private Color inputForegroundColor;
    private Color highlightColor;
    private Color searchResultColor;
    
    private final Settings settings;
    
    public StyleManager(Settings settings) {
        this.settings = settings;
        makeStyles();
    }
    
    /**
     * Remakes the styles, usually when a setting was changed.
     */
    public void refresh() {
        LOGGER.info("Refreshing styles..");
        makeStyles();
    }
    
    private void makeStyles() {
        foregroundColor = makeColor("foregroundColor", Color.BLACK);
        HtmlColors.setDefaultColor(foregroundColor);
        backgroundColor = makeColor("backgroundColor");
        inputBackgroundColor = makeColor("inputBackgroundColor");
        inputForegroundColor = makeColor("inputForegroundColor");
        highlightColor = makeColor("highlightColor");
        searchResultColor = makeColor("searchResultColor");
        
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        baseStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setFontFamily(baseStyle,settings.getString("font"));
        StyleConstants.setFontSize(baseStyle,(int)settings.getLong("fontSize"));
        
        standardStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(standardStyle, makeColor("foregroundColor"));
        
        highlightStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(highlightStyle, highlightColor);
        
        specialStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(specialStyle, makeColor("compactColor"));

        infoStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(infoStyle, makeColor("infoColor"));
        
        paragraphStyle = new SimpleAttributeSet();
        // Divide by 10 so integer values can be used for this setting
        float spacing = settings.getLong("lineSpacing") / (float)10.0;
        StyleConstants.setLineSpacing(paragraphStyle, spacing);
        
        other = new SimpleAttributeSet();
        //other.addAttribute(ChannelTextPane.TIMESTAMP_ENABLED, settings.getBoolean("timestampEnabled"));
        other.addAttribute(Setting.EMOTICONS_ENABLED, settings.getBoolean("emoticonsEnabled"));
        other.addAttribute(Setting.USERICONS_ENABLED, settings.getBoolean("usericonsEnabled"));
        other.addAttribute(Setting.SHOW_BANMESSAGES, settings.getBoolean("showBanMessages"));
        other.addAttribute(Setting.AUTO_SCROLL, settings.getBoolean("autoScroll"));
        other.addAttribute(Setting.AUTO_SCROLL_TIME, settings.getLong("autoScrollTimeout"));
        other.addAttribute(Setting.ACTION_COLORED, settings.getBoolean("actionColored"));
        other.addAttribute(Setting.BUFFER_SIZE, settings.getLong("bufferSize"));
        other.addAttribute(Setting.COMBINE_BAN_MESSAGES, settings.getBoolean("combineBanMessages"));
        // Deleted Messages Settings
        String deletedMessagesMode = settings.getString("deletedMessagesMode");
        long deletedMessagesModeNumeric = 0;
        if (deletedMessagesMode.equals("delete")) {
            deletedMessagesModeNumeric = -1;
        } else if (deletedMessagesMode.equals("keepShortened")) {
            deletedMessagesModeNumeric = settings.getLong("deletedMessagesMaxLength");
        }
        other.addAttribute(Setting.DELETED_MESSAGES_MODE, deletedMessagesModeNumeric);
        
        String fontFamily = settings.getString("font");
        int fontSize = (int)settings.getLong("fontSize");
        font = new Font(fontFamily,Font.PLAIN,fontSize);
    }
    
    private Color makeColor(String setting) {
        return makeColor(setting, foregroundColor);
    }
    
    private Color makeColor(String setting, Color defaultColor) {
        return HtmlColors.decode(settings.getString(setting),defaultColor);
    }

    @Override
    public MutableAttributeSet getStyle() {
        return getStyle("regular");
    }

    @Override
    public MutableAttributeSet getStyle(String type) {
        switch (type) {
            case "special":
                return specialStyle;
            case "standard":
                return standardStyle;
            case "info":
                return infoStyle;
            case "highlight":
                return highlightStyle;
            case "paragraph":
                return paragraphStyle;
            case "settings":
                return other;
        }
        return baseStyle;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public Color getColor(String type) {
        switch (type) {
            case "foreground":
                return foregroundColor;
            case "background":
                return backgroundColor;
            case "inputBackground":
                return inputBackgroundColor;
            case "inputForeground":
                return inputForegroundColor;
            case "searchResult":
                return searchResultColor;
        }
        return foregroundColor;
    }
    
    @Override
    public SimpleDateFormat getTimestampFormat() {
        String timestamp = settings.getString("timestamp");
        String timezone = settings.getString("timestampTimezone");
        if (!timestamp.equals("off")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(timestamp);
                if (!timezone.isEmpty() && !timezone.equalsIgnoreCase("local")) {
                    sdf.setTimeZone(TimeZone.getTimeZone(timezone));
                }
                return sdf;
            } catch (IllegalArgumentException ex) {
                LOGGER.warning("Invalid timestamp: "+timestamp);
            }
        }
        return null;
    }
    
}
