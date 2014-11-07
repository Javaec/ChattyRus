
package chatty.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * Some Utility functions or constants for GUI related stuff
 * 
 * @author tduva
 */
public class GuiUtil {
    
    public final static Insets SMALL_BUTTON_INSETS = new Insets(-1, 10, -1, 10);
    
    private static final String CLOSE_DIALOG_ACTION_MAP_KEY = "CLOSE_DIALOG_ACTION_MAP_KEY";
    private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    
    
    public static void installEscapeCloseOperation(final JDialog dialog) {
    Action closingAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(
                        new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING
                        ));
            }
        };
        
        JRootPane root = dialog.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                ESCAPE_STROKE,
                CLOSE_DIALOG_ACTION_MAP_KEY);
        root.getActionMap().put(CLOSE_DIALOG_ACTION_MAP_KEY, closingAction);
    }
    
    /**
     * Shows a JOptionPane that doesn't steal focus when opened, but is
     * focusable afterwards.
     * 
     * @param parent The parent Component
     * @param title The title
     * @param message The message
     * @param messageType The type of message as in JOptionPane
     * @param optionType The option type as in JOptionPane
     * @param options The options as in JOptionPane
     * @return The selected option or -1 if none was selected
     */
    public static int showNonAutoFocusOptionPane(Component parent, String title, String message,
            int messageType, int optionType, Object[] options) {
        JOptionPane p = new JOptionPane(message, messageType, optionType);
        p.setOptions(options);
        final JDialog d = p.createDialog(parent, title);
        d.setAutoRequestFocus(false);
        d.setFocusableWindowState(false);
        // Make focusable after showing the dialog, so that it can be focused
        // by the user, but doesn't steal focus from the user when it opens.
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                d.setFocusableWindowState(true);
            }
        });
        d.setVisible(true);
        // Find index of result
        Object value = p.getValue();
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Checks if the given {@code Point} is on a screen. The point can be moved
     * horizontally before checking by specifying a {@code xOffset}. The
     * original point is not modified.
     * 
     * @param p The {@code Point} to check
     * @param xOffset The horizontal offset in pixels
     * @return {@code true} if the point is on screen, {@code false} otherwise
     */
    public static boolean isPointOnScreen(Point p, int xOffset) {
        Point moved = new Point(p.x + xOffset, p.y);
        return isPointOnScreen(moved);
    }
    
    /**
     * Checks if the given {@code Point} is on a screen.
     * 
     * @param p The {@code Point} to check
     * @return {@code true} if the point is on screen, {@code false} otherwise
     */
    public static boolean isPointOnScreen(Point p) {
        GraphicsDevice[] screens = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice screen : screens) {
            if (screen.getDefaultConfiguration().getBounds().contains(p)) {
                return true;
            }
        }
        return false;
    }
    
    public static GridBagConstraints makeGbc(int x, int y, int w, int h) {
        return makeGbc(x, y, w, h, GridBagConstraints.EAST);
    }
    
    public static GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.anchor = anchor;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }
    
    /**
     * Output the text of the subelements of the given element.
     *
     * @param line
     */
    public static void debugLineContents(Element line) {
        Document doc = line.getDocument();
        for (int i = 0; i < line.getElementCount(); i++) {
            Element l = line.getElement(i);
            //System.out.println(l);
            try {
                System.out.print("'" + doc.getText(l.getStartOffset(), l.getEndOffset() - l.getStartOffset()) + "'");

            } catch (BadLocationException ex) {
                System.out.println("Bad location");
            }
        }
        System.out.println();
    }
    
    /**
     * Detect retina display.
     * 
     * http://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java
     * 
     * @return 
     */
    public static boolean hasRetinaDisplay() {
        Object obj = Toolkit.getDefaultToolkit().getDesktopProperty(
                "apple.awt.contentScaleFactor");
        if (obj instanceof Float) {
            int scale = ((Float)obj).intValue();
            return (scale == 2); // 1 indicates a regular mac display.
        }
        return false;
    }
    
    /**
     * Recursively set the font size of the given component and all
     * subcomponents.
     * 
     * @param fontSize
     * @param component 
     */
    public static void setFontSize(float fontSize, Component component) {
        if (fontSize <= 0) {
            return;
        }
        if (component instanceof Container) {
            synchronized(component.getTreeLock()) {
                for (Component c : ((Container) component).getComponents()) {
                    GuiUtil.setFontSize(fontSize, c);
                }
            }
        }
        component.setFont(component.getFont().deriveFont(fontSize));
    }
    
}
