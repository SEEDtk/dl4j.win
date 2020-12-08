/**
 *
 */
package org.theseed.win;

import java.io.File;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * This class contains simple utilities for managing shells and dialogs.
 *
 * @author Bruce Parrello
 *
 */
public class ShellUtils {

    /**
     * Display an error message box.
     *
     * @param shell		controlling shell
     * @param title		title to show in the box
     * @param message	error message text
     */
    public static void showErrorBox(Shell shell, String title, String message) {
        MessageBox msgbox = new MessageBox(shell, SWT.ICON_ERROR);
        msgbox.setText(title);
        msgbox.setMessage(message);
        msgbox.open();
    }

    /**
     * Make the current window size and position persistent.
     *
     * @param shell			window of interest
     * @param controller	object controlling the window
     * @param width			default width
     * @param height		default height
     */
    public static void persistPosition(Shell shell, Object controller, int width, int height) {
        // Get the preferences.
        Preferences prefs = getPrefs(controller);
        int w = prefs.getInt("_w", 0);
        int h = prefs.getInt("_h", 0);
        if (w == 0 && h == 0) {
            // Here no position has been stored yet.
            shell.setSize(width, height);
        } else {
            int x = prefs.getInt("_x", 300);
            int y = prefs.getInt("_y", 300);
            shell.setBounds(x, y, w, h);
            shell.setMaximized(prefs.getBoolean("_max", false));
        }
        // Add a close listener to save the position.
        shell.addListener(SWT.Close, new CloseListener(shell, controller));

    }

    /**
     * @return the preferences for a controlling object.
     *
     * @param controller	controlling object of interest
     */
    public static Preferences getPrefs(Object controller) {
        Class<? extends Object> oClass = controller.getClass();
        Preferences prefs = Preferences.userNodeForPackage(oClass);
        return prefs;
    }

    /**
     * Get an input file from a file dialog.
     *
     * @param dir	starting directory
     *
     * @return the new file, or NULL if none was selected
     */
    public static File selectFile(Shell shell, File dir) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        fileChooser.setFilterPath(dir.toString());
        fileChooser.setText("Choose a training file.");
        String trainName = fileChooser.open();
        File retVal = null;
        // Loop until the user quits or we find a valid input file.
        boolean valid = false;
        while (trainName != null && ! valid) {
            retVal = new File(trainName);
            if (! retVal.canRead())
                ShellUtils.showErrorBox(shell, "Training File", "Selected file is unreadable.");
            else
                valid = true;
            if (! valid)
                trainName = fileChooser.open();
        }
        if (trainName == null) retVal = null;
        return retVal;
    }



    /**
     * This class is the listener for saving a window position.
     */
    private static class CloseListener implements Listener {

        private Shell shell;
        private Object controller;

        /**
         * Save the shell and the controlling object.
         *
         * @param shell			window whose position is to be saved
         * @param controller	controlling object for the preferences
         */
        public CloseListener(Shell shell, Object controller) {
            this.shell = shell;
            this.controller = controller;
        }

        @Override
        public void handleEvent(Event arg0) {
            boolean maxed = shell.getMaximized();
            Preferences prefs = getPrefs(controller);
            if (! maxed) {
                Rectangle bounds = shell.getBounds();
                prefs.putInt("_x", bounds.x);
                prefs.putInt("_y", bounds.y);
                prefs.putInt("_w", bounds.width);
                prefs.putInt("_h", bounds.height);
            }
            prefs.putBoolean("_max", maxed);
        }

    }



    /**
     * Persist the location of a fixed-size window.
     *
     * @param shell			window of interest
     * @param controller	object controlling the window
     * @param width			default width
     * @param height		default height
     */
    public static void persistLocation(Shell shell, Object controller, int width, int height) {
        Preferences prefs = getPrefs(controller);
        int x = prefs.getInt("_x", 300);
        int y = prefs.getInt("_y", 300);
        shell.setBounds(x, y, width, height);
        // Add a close listener to save the position.
        shell.addListener(SWT.Close, new CloseListener(shell, controller));
    }

}
