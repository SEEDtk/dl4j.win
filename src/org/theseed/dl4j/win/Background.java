/**
 *
 */
package org.theseed.dl4j.win;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.utils.ICommand;

/**
 * This object maintains a thread to run a command in the background.
 *
 * @author Bruce Parrello
 *
 */
public class Background extends Thread {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(Background.class);
    /** parent context for error messages */
    private TrainingManager parent;
    /** processor to run */
    private ICommand processor;

    /**
     * Construct this thread to run the specified processor.
     *
     * @param parent		parent window manager
     * @param processor		command object to run
     */
    public Background(TrainingManager parent, ICommand processor) {
        this.processor = processor;
        this.parent = parent;
    }

    @Override
    public void run() {
        try {
            // Run the command.
            processor.run();
        } catch (Exception e) {
            log.error("Error running command.", e);
        }
        // Denote the command is done.
        parent.reportCommandEnded();
    }

}
