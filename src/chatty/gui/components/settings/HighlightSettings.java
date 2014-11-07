
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class HighlightSettings extends SettingsPanel {
    
    public static final String INFO = "<html><body style='width:300px;font-weight:normal;'>"
            + "Quick Reference (see regular help for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>cs:</code> - match case sensitive</li>"
            + "<li><code>w:/wcs:</code> - match as whole word / case-sensitive</li>"
            + "<li><code>re:</code> - use regular expression</li>"
            + "<li><code>chan:chan1,chan2/!chan:</code> - restrict to channel(s) / inverted</li>"
            + "<li><code>user:name</code> - restrict to user with that name</li>"
            + "<li><code>cat:category</code> - restrict to users in that category</li>"
            + "</ul>";
    
    private static final String INFO_HIGHLIGHTS = INFO+"Example: <code>user:botimuz cs:Bets open</code>";
    
    public HighlightSettings(SettingsDialog d) {
        
        
        JPanel base = addTitledPanel("Highlight Messages", 0);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightEnabled", "Enable Highlight",
                "If enabled, shows messages that match the highlight criteria "
                + "in another color"), gbc);
        
        gbc = d.makeGbc(1,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,12,2,5);
        base.add(d.addSimpleBooleanSetting("highlightUsername", "Highlight "
                + "own name",
                "If enabled, highlights messages containing your current "
                + "username, even if you didn't add it to the list."), gbc);
        
        gbc = d.makeGbc(1,1,1,1);
        gbc.insets = new Insets(0,12,2,5);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightOwnText", "Highlight own text",
                "If enabled, highlights messages your wrote yourself. Good "
                        + "for testing."), gbc);
        
        gbc = d.makeGbc(1,2,1,1);
        gbc.insets = new Insets(0,12,2,5);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightNextMessages", "Highlight follow-up",
                "If enabled, highlights messages from the same user that are written"
                        + "shortly after the last highlighted one."), gbc);
        
        gbc = d.makeGbc(0,1,1,3);
        gbc.insets = new Insets(5,10,5,5);
        ListSelector items = d.addListSetting("highlight", 220, 250, true);
        items.setInfo(INFO_HIGHLIGHTS);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        base.add(items, gbc);
        
        gbc = d.makeGbc(1,3,1,1);
        base.add(new LinkLabel("<html><body style=\"width:120px;\">"
                + "Add words to highlight messages. "
                + " [help:highlight More info..]"
                + "<br />"
                + "<ul style='margin-left: 10px;'>"
                + "<li style='margin-top: 3px;'>Prepend with 'cs:' to make case-sensitive.</li>"
                + "<li style='margin-top: 3px;'>Prepend with 'w:'/'wcs:' to match words.</li>"
                + "<li style='margin-top: 3px;'>Prepend with 're:' to use regular expression.</li>"
                + "<li style='margin-top: 3px;'>Prepend with 'user:' to specify a username.</li>"
                + "</ul>", d.getLinkLabelListener()), gbc);
    }
}
