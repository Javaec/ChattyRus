
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A String setting that represents a path. A JPanel with textfield and buttons
 * to change, reset or open the path.
 * 
 * @author tduva
 */
public class PathSetting extends JPanel implements StringSetting {

    private final JTextField display = new JTextField();
    private final JButton changeButton = new JButton("Change");
    private final JButton resetButton = new JButton("Default");
    private final JButton openButton = new JButton("Open");
    
    private String value;
    private final String defaultPath;
    private final Component parentComponent;
    
    /**
     * Create a new PathSetting instancen.
     * 
     * @param parentComponent The component to open the file chooser on
     * @param defaultPath The path to display as default path, if path is empty
     */
    public PathSetting(final Component parentComponent, String defaultPath) {
        this.defaultPath = defaultPath;
        this.parentComponent = parentComponent;
        
        display.setEditable(false);
        changeButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        resetButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        openButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        
        add(display, gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        add(changeButton, gbc);
        gbc.gridx = 2;
        add(resetButton, gbc);
        gbc.gridx = 3;
        add(openButton, gbc);
        
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == changeButton) {
                    chooseDirectory();
                } else if (e.getSource() == resetButton) {
                    setSettingValue("");
                } else if (e.getSource() == openButton) {
                    Helper.openFolder(getCurrentPath().toFile(), parentComponent);
                }
            }
        };
        changeButton.addActionListener(buttonAction);
        resetButton.addActionListener(buttonAction);
        openButton.addActionListener(buttonAction);
    }
    
    /**
     * The current setting value, either the path or an empty String to
     * represent the default path.
     * 
     * @return The current setting value
     */
    @Override
    public String getSettingValue() {
        return value;
    }

    /**
     * Set the setting and update the display.
     * 
     * @param value The new value to set (should not be null)
     * @throws NullPointerException if value is null
     */
    @Override
    public void setSettingValue(String value) {
        this.value = value;
        display.setText((value.isEmpty() ? "[default] " : "")+getCurrentPathValue());
    }
    
    /**
     * Get the current path as a String. If the setting value is empty, then use
     * the default path.
     * 
     * @return The setting value or the default path if the setting value is
     * empty
     */
    private String getCurrentPathValue() {
        if (value.isEmpty()) {
            return defaultPath;
        } else {
            return value;
        }
    }
    
    /**
     * Gets the current Path.
     * 
     * @return The Path based on the current setting value, or the default Path
     * if the setting value is empty
     * @see getCurrentPathValue()
     */
    private Path getCurrentPath() {
        try {
            return Paths.get(getCurrentPathValue());
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    /**
     * Open a JFileChooser to select a directory to use.
     */
    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(getCurrentPath().toFile());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showDialog(parentComponent, "Select folder") == JFileChooser.APPROVE_OPTION) {
            setSettingValue(chooser.getSelectedFile().getPath());
        }
    }
    
}
