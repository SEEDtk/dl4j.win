/**
 *
 */
package org.theseed.dl4j.win;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.custom.SashForm;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.theseed.dl4j.train.RunStats;
import org.theseed.io.LineReader;
import org.theseed.win.ShellUtils;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * @author Bruce Parrello
 *
 */
public class LogViewer extends Dialog {

    // FIELDS
    /** main window shell */
    protected Shell shell;
    /** text display of current section */
    private Text textSection;
    /** trial log being displayed */
    private File trialFile;
    /** default number of lines to allocate for each section */
    private static final int SECTION_SIZE = 80;
    /** TRUE if the window is created */
    private boolean created;

    // ICONS
    private static final Image EMPTY_JOB_ICO = SWTResourceManager.getImage(LogViewer.class, "/org/theseed/images/empty-job.ico");
    private static final Image RESULT_ICO = SWTResourceManager.getImage(LogViewer.class, "/org/theseed/images/result.ico");
    private static final Image SEARCH_JOB_ICO = SWTResourceManager.getImage(LogViewer.class, "/org/theseed/images/search-job.ico");
    private static final Image XVALIDATE_JOB_ICO = SWTResourceManager.getImage(LogViewer.class, "/org/theseed/images/xvalidate-job.ico");
    private static final Image SUMMARY_ICO = SWTResourceManager.getImage(LogViewer.class, "/org/theseed/images/summary.ico");

    /**
     * Create the dialog.
     *
     * @param parent	parent window
     * @param style		window style
     */
    public LogViewer(Shell parent, int style, String modelName, File modelDir) {
        super(parent, style);
        setText("Log Viewer for " + modelName);
        this.trialFile = new File(modelDir, "trials.log");
        this.created = false;
    }

    /**
     * Open the dialog.
     */
    public void open() {
        createContents();
        shell.open();
        shell.layout();
        if (created) {
            Display display = getParent().getDisplay();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } else {
            shell.close();
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shell = new Shell(getParent(), SWT.SHELL_TRIM);
        ShellUtils.persistPosition(shell, this, 1000, 1000);
        shell.setText(getText());
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));

        SashForm sashForm = new SashForm(shell, SWT.NONE);
        sashForm.setSashWidth(5);

        Tree treeDirectory = new Tree(sashForm, SWT.BORDER);
        // If a tree item has a section attached, display it when the item
        // is selected.
        treeDirectory.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String section = (String) e.item.getData();
                if (section != null)
                    textSection.setText(section);
            }
        });

        textSection = new Text(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
        textSection.setFont(SWTResourceManager.getFont("Consolas", 9, SWT.NORMAL));
        textSection.setEditable(false);
        sashForm.setWeights(new int[] {1, 2});

        // Load the trial log into the tree. If it works, denote we're good to go.
        if (loadTrialLog(treeDirectory))
            created = true;

    }

    /**
     * Load the trial log into the tree.
     *
     * @param treeDirectory		tree control to receive the sections of the trial log
     */
    private boolean loadTrialLog(Tree treeDirectory) {
        boolean retVal = false;
        try (LineReader reader = new LineReader(this.trialFile)) {
            // Read the first line.
            Iterator<String> lineIter = reader.iterator();
            // Find the first job.
            while (lineIter.hasNext() && ! lineIter.next().contentEquals(RunStats.JOB_START_MARKER));
            // We are now positioned at the end of file or immediately after a job-start marker.
            while (lineIter.hasNext()) {
                // Consume this job.
                boolean ok = readJob(treeDirectory, lineIter);
                // If it had sections, denote the tree is valid.
                if (ok) retVal = true;
            }
            if (! retVal)
                ShellUtils.showErrorBox(shell, "Error Loading Trial Log", "Trial log had no valid jobs in it.");
        } catch (IOException e) {
            ShellUtils.showErrorBox(shell, "Error Loading Trial Log", e.getMessage());
        }
        return retVal;
    }

    /**
     * Read a job from the trail log into the tree.  This will add a job node to the tree.
     * At the end, we will be positioned after a job marker or at the end of the file.
     *
     * @param treeDirectory		tree control to receive the job
     * @param lineIter			iterator through the trial log
     *
     * @return TRUE if we found data in the job, else FALSE
     */
    private boolean readJob(Tree treeDirectory, Iterator<String> lineIter) {
        boolean retVal = false;
        String line = lineIter.next();
        TreeItem jobItem = new TreeItem(treeDirectory, SWT.DEFAULT);
        jobItem.setText(line);
        if (line.startsWith("Search "))
            jobItem.setImage(SEARCH_JOB_ICO);
        else if (line.startsWith("Cross-Validate "))
            jobItem.setImage(XVALIDATE_JOB_ICO);
        else
            jobItem.setImage(EMPTY_JOB_ICO);
        // We will accumulate sections in here.
        List<String> currentText = new ArrayList<String>(SECTION_SIZE);
        TreeItem sectionItem = null;
        boolean endOfJob = false;
        while (lineIter.hasNext() && ! endOfJob) {
            // Get the next line.
            line = lineIter.next();
            switch (line) {
            case RunStats.JOB_START_MARKER :
                // Here we are at the end of the job.
                endOfJob = true;
                break;
            case RunStats.TRIAL_SECTION_MARKER :
                // Here we are at the start of a new section.
                if (sectionItem != null) {
                    storeSection(currentText, sectionItem);
                    // We have a section.  Return TRUE.
                    retVal = true;
                    // Denote we do not have a section in progress.
                    sectionItem = null;
                }
                // If we do NOT have premature end-of-file, start the new section.
                // The section title is on the next line.
                if (lineIter.hasNext()) {
                    sectionItem = new TreeItem(jobItem, SWT.DEFAULT);
                    line = lineIter.next();
                    sectionItem.setText(line);
                    if (line.startsWith("Summary "))
                        sectionItem.setImage(SUMMARY_ICO);
                    else
                        sectionItem.setImage(RESULT_ICO);
                    currentText.clear();
                    currentText.add(line);
                }
                break;
            default :
                // Here we have a data line.
                currentText.add(line);
            }
        }
        // Save the residual.
        if (sectionItem != null) {
            storeSection(currentText, sectionItem);
            retVal = true;
        }
        // Delete the job if it is empty.
        if (! retVal)
            jobItem.dispose();
        return retVal;
    }

    /**
     * Store the current section in the section item.
     *
     * @param currentText	text of the current section
     * @param sectionItem	tree item for the section
     */
    public void storeSection(List<String> currentText, TreeItem sectionItem) {
        // Store the old section text in the old section node.
        String section = StringUtils.join(currentText, System.getProperty("line.separator"));
        sectionItem.setData(section);
    }

}
