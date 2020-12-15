/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.dl4j.train.ClassTrainingProcessor;
import org.theseed.dl4j.train.CrossValidateProcessor;
import org.theseed.dl4j.train.ITrainReporter;
import org.theseed.dl4j.train.RegressionTrainingProcessor;
import org.theseed.dl4j.train.SearchProcessor;
import org.theseed.dl4j.train.TrainingProcessor;
import org.theseed.io.LineReader;
import org.theseed.utils.ICommand;
import org.theseed.utils.Parms;
import org.theseed.win.ShellUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Label;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;

/**
 * This is the application for training a dl4j.run model.  It provides a GUI interface and displays the run status conveniently
 * during searches.
 *
 * @author Bruce Parrello
 *
 */
public class TrainingManager implements AutoCloseable, ITrainReporter {

    // PROPERTIES
    private static final String MODEL_DIRECTORY = "model_directory";

    // FIELDS
    /** main application window */
    protected Shell shlTrainingManager;
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TrainingManager.class);
    /** model directory display box */
    private Text txtModelDirectory;
    /** persistent properties */
    private Preferences trainingProperties;
    /** parameter file for model */
    private File parmFile;
    /** current model directory */
    private File modelDir;
    /** type of model */
    private TrainingProcessor.Type modelType;
    /** radio button for class models */
    private Button btnClassifier;
    /** radio button for regression models */
    private Button btnRegression;
    /** controlling display object */
    private Display display;
    /** button for starting training */
    private Button btnRunSearch;
    /** progress bar for displaying score */
    private ProgressBar barScore;
    /** current status */
    private Text txtStatus;
    /** results of last training run */
    private Text txtResults;
    /** current command thread */
    private Thread backgrounder;
    /** button to launch cross-validation */
    private Button btnXValidate;
    /** current epoch */
    private Text txtEpoch;
    /** best epoch */
    private Text txtBestEpoch;
    /** current score */
    private Text txtScore;
    /** button to abort the run */
    private Button btnAbort;
    /** button to edit parameter file */
    private Button btnEditParms;
    /** button to display scatter graph or confusion matrix */
    private Button btnGraph;
    /** upper limit for progress bar */
    private static final double LOG10_UPPER = 1;
    /** lower limit for progress bar */
    private static final double LOG10_LOWER = -10;
    /** minimum displayable score */
    private static final double MIN_SCORE = Math.pow(10.0, LOG10_LOWER);

    /**
     * Initialize the training manager.
     *
     * @throws IOException
     */
    public TrainingManager() throws IOException {
        // Read in the persistent properties.
        trainingProperties = Preferences.userNodeForPackage(TrainingManager.class);
        modelDir = null;
        modelType = TrainingProcessor.Type.CLASS;
        backgrounder = null;
    }

    /**
     * Launch the application.
     *
     * @param args	command-line options
     */
    public static void main(String[] args) {
        try {
            // Configure logging.
            Level logLevel = Level.ERROR;
            if (args.length >= 1 && args[0].contentEquals("-v")) logLevel = Level.INFO;
            LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
            logging.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(logLevel);
            try (TrainingManager window = new TrainingManager()) {
                window.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
    }

    /**
     * Load the properties and open the window.
     */
    public void open() {
        display = Display.getDefault();
        createContents();
        shlTrainingManager.open();
        shlTrainingManager.layout();
        while (!shlTrainingManager.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {

        shlTrainingManager = new Shell();
        shlTrainingManager.setImage(SWTResourceManager.getImage(TrainingManager.class, "/org/theseed/images/fig-gear.ico"));
        ShellUtils.persistPosition(shlTrainingManager, this, 662, 452);
        shlTrainingManager.setText("Training Manager");
        shlTrainingManager.setLayout(new GridLayout(2, false));

        Composite fixedRegion = new Composite(shlTrainingManager, SWT.NONE);
        GridData gd_fixedRegion = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_fixedRegion.heightHint = 99;
        gd_fixedRegion.widthHint = 700;
        fixedRegion.setLayoutData(gd_fixedRegion);

        Label lblModelDirectory = new Label(fixedRegion, SWT.NONE);
        lblModelDirectory.setLocation(10, 10);
        lblModelDirectory.setSize(94, 15);
        lblModelDirectory.setText("Model Directory");

        Group group = new Group(fixedRegion, SWT.NONE);
        group.setLocation(310, 0);
        group.setSize(200, 31);
        group.setLayout(new FillLayout(SWT.HORIZONTAL));

        btnClassifier = new Button(group, SWT.RADIO);
        //btnClassifier.setLocation(100, 5);
        //btnClassifier.setSize(78, 16);
        btnClassifier.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                modelType = TrainingProcessor.Type.CLASS;
                configureType();
            }
        });
        btnClassifier.setText("Classifier");

        btnRegression = new Button(group, SWT.RADIO);
        //btnRegression.setLocation(5, 5);
        //btnRegression.setSize(90, 16);
        btnRegression.setText("Regression");
        btnRegression.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                modelType = TrainingProcessor.Type.REGRESSION;
                configureType();
            }
        });
        group.pack();
        txtModelDirectory = new Text(fixedRegion, SWT.BORDER | SWT.READ_ONLY);
        txtModelDirectory.setLocation(110, 7);
        txtModelDirectory.setSize(158, 21);
        txtModelDirectory.setEditable(false);

        Button btnGetDirectory = new Button(fixedRegion, SWT.NONE);
        btnGetDirectory.setLocation(267, 7);
        btnGetDirectory.setSize(36, 21);
        btnGetDirectory.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (backgrounder == null)
                    selectModelDirectory();
                else
                    showError("Directory Change Error", "Cannot change models while a command is running.");
            }
        });
        btnGetDirectory.setText("...");

        btnRunSearch = new Button(fixedRegion, SWT.NONE);
        btnRunSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runSearch();
            }
        });
        btnRunSearch.setLocation(526, 5);
        btnRunSearch.setSize(130, 25);
        btnRunSearch.setEnabled(false);
        btnRunSearch.setText("Training Search");

        btnXValidate = new Button(fixedRegion, SWT.NONE);
        btnXValidate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runXValidate();
            }
        });
        btnXValidate.setText("Cross Validate");
        btnXValidate.setEnabled(false);
        btnXValidate.setBounds(526, 36, 130, 25);

        Label lblEpoch = new Label(fixedRegion, SWT.NONE);
        lblEpoch.setBounds(43, 41, 40, 15);
        lblEpoch.setText("Epoch");

        Label lblBestEpoch = new Label(fixedRegion, SWT.NONE);
        lblBestEpoch.setBounds(345, 41, 68, 15);
        lblBestEpoch.setText("Best Epoch");

        txtEpoch = new Text(fixedRegion, SWT.BORDER | SWT.READ_ONLY);
        txtEpoch.setBounds(89, 38, 48, 21);

        Label lblScore = new Label(fixedRegion, SWT.NONE);
        lblScore.setBounds(176, 41, 40, 15);
        lblScore.setText("Score");

        txtBestEpoch = new Text(fixedRegion, SWT.BORDER | SWT.READ_ONLY);
        txtBestEpoch.setBounds(419, 38, 48, 21);

        txtScore = new Text(fixedRegion, SWT.BORDER | SWT.READ_ONLY);
        txtScore.setBounds(222, 38, 76, 21);

        btnAbort = new Button(fixedRegion, SWT.NONE);
        btnAbort.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (backgrounder != null) {
                    backgrounder.interrupt();
                }
            }
        });
        btnAbort.setEnabled(false);
        btnAbort.setBounds(355, 67, 150, 25);
        btnAbort.setText("Abort Command");

        btnEditParms = new Button(fixedRegion, SWT.NONE);
        btnEditParms.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ParmsDialog parmEditor = new ParmsDialog(shlTrainingManager, SWT.DEFAULT, parmFile, modelType);
                parmEditor.open();
            }
        });
        btnEditParms.setBounds(10, 67, 100, 25);
        btnEditParms.setText("Edit Parms");

        Button btnViewLog = new Button(fixedRegion, SWT.NONE);
        btnViewLog.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                LogViewer logViewer = new LogViewer(shlTrainingManager, SWT.DEFAULT, txtModelDirectory.getText(), modelDir);
                logViewer.open();
            }
        });
        btnViewLog.setBounds(116, 67, 100, 25);
        btnViewLog.setText("View Log");

        btnGraph = new Button(fixedRegion, SWT.NONE);
        btnGraph.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                if (modelType == TrainingProcessor.Type.REGRESSION) {
                    try {
                        RegressionTrainingProcessor processor = new RegressionTrainingProcessor();
                        processor.setProgressMonitor(TrainingManager.this);
                        boolean ok = processor.initializeForPredictions(modelDir);
                        if (ok) {
                            ScatterDisplay scatterDialog = new ScatterDisplay(shlTrainingManager, SWT.DEFAULT,
                                    txtModelDirectory.getText(), processor);
                            scatterDialog.open();
                        } else {
                            showError("Invalid Parameter File", "Parameter file not set up for evaluation.");
                        }
                    } catch (IOException e) {
                        showError("Parameter File Error", e.getMessage());
                    }
                } else {
                    try {
                        ClassTrainingProcessor processor = new ClassTrainingProcessor();
                        processor.setProgressMonitor(TrainingManager.this);
                        boolean ok = processor.initializeForPredictions(modelDir);
                        if (ok) {
                            ConfusionDisplay confusionDialog = new ConfusionDisplay(shlTrainingManager, SWT.DEFAULT,
                                    processor);
                            confusionDialog.open();
                        } else {
                            showError("Invalid Parameter File", "Parameter file not set up for evaluation.");
                        }
                    } catch (IOException e) {
                        showError("Parameter File Error", e.getMessage());
                    }
                }

            }
        });
        btnGraph.setBounds(222, 67, 100, 25);
        btnGraph.setText("Confusion Matrix");

        barScore = new ProgressBar(shlTrainingManager, SWT.VERTICAL);
        barScore.setMaximum(100);
        barScore.setMinimum(0);
        GridData gd_barScore = new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 2);
        gd_barScore.widthHint = 29;
        gd_barScore.heightHint = 256;
        barScore.setLayoutData(gd_barScore);

        Composite composite_1 = new Composite(shlTrainingManager, SWT.NONE);
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout gl_composite_1 = new GridLayout(1, false);
        gl_composite_1.marginRight = 5;
        composite_1.setLayout(gl_composite_1);

        txtStatus = new Text(composite_1, SWT.BORDER | SWT.READ_ONLY);
        txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Composite composite = new Composite(shlTrainingManager, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        GridLayout gl_composite = new GridLayout(1, false);
        gl_composite.marginRight = 5;
        gl_composite.marginBottom = 5;
        composite.setLayout(gl_composite);

        txtResults = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
        txtResults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        txtResults.setFont(SWTResourceManager.getFont("Consolas", 9, SWT.NORMAL));

        // Compute the current model directory.  Note all the display controls have to
        // be in place before we do this.  If an error occurs trying to analyze
        // the directory, we ignore it.
        String modelDirName = trainingProperties.get(MODEL_DIRECTORY, null);
        if (modelDirName != null) try {
            analyzeModelDirectory(modelDirName);
        } catch (IOException e) { }

    }

    /**
     * Configure the results display button.
     */
    protected void configureType() {
        if (modelType == TrainingProcessor.Type.CLASS)
            btnGraph.setText("Confusion Matrix");
        else
            btnGraph.setText("Scatter Plot");
    }

    /**
     * Run a search processor.
     */
    protected void runSearch() {
        ICommand processor = new SearchProcessor(this);
        String[] parms = new String[] { "-t", modelType.toString(), modelDir.toString() };
        executeCommand(processor, "SEARCH", parms);
    }

    /**
     * Run a cross-validation processor.
     */
    private void runXValidate() {
        ICommand processor = new CrossValidateProcessor(this);
        try {
            String idCol = getIdCol();
            List<String> parmList = new ArrayList<String>(10);
            parmList.add("-t");
            parmList.add(modelType.toString());
            if (idCol != null) {
                parmList.add("--id");
                parmList.add(idCol);
            }
            parmList.add(modelDir.toString());
            String[] parms = new String[parmList.size()];
            parms = parmList.toArray(parms);
            executeCommand(processor, "Cross-Validate", parms);
        } catch (IOException e) {
            showError("Error Reading Parm File", e.getMessage());
        }
    }

    /**
     * @return the ID column for the current model, or NULL if there is none
     *
     * @throws IOException
     */
    private String getIdCol() throws IOException {
        String retVal = null;
        Parms parms = new Parms(parmFile);
        String[] cols = StringUtils.split(parms.getValue("--metaCols"), ',');
        if (cols.length >= 1)
            retVal = cols[0];
        return retVal;
    }

    /**
     * Execute a command.
     *
     * @param processor		processor on which to execute command
     * @param parms			parameters to pass
     */
    private void executeCommand(ICommand processor, String name, String[] parms) {
        boolean ok = processor.parseCommand(parms);
        if (! ok)
            showError("Command Error", "Invalid parameter combination.");
        else {
            // Insure the user doesn't start anything else.
            enableButtons(false);
            // Tell the user what we're up to.
            txtStatus.setText("Running " + name + " command.");
            // Start the thread.
            backgrounder = new Background(this, processor);
            backgrounder.start();
        }

    }

    /**
     * Use a file chooser to locate the model directory.
     */
    protected void selectModelDirectory() {
        // Get the model directory.  This is either the user's document directory or the parent of the last model directory.
        File curDir;
        if (modelDir == null)
            curDir = new File(System.getenv("HOME"), "Documents");
        else
            curDir = modelDir.getParentFile();
        DirectoryDialog dirChooser = new DirectoryDialog(shlTrainingManager, SWT.OPEN);
        dirChooser.setText("Model Directory");
        dirChooser.setFilterPath(curDir.toString());
        String newDir = dirChooser.open();
        // Loop until the user quits or we find a valid directory.
        boolean valid = false;
        while (newDir != null && ! valid) {
            File labelFile = new File(newDir, "labels.txt");
            File trainFile = new File(newDir, "training.tbl");
            if (! labelFile.exists())
                showError("Model Directory Error", "Selected folder does not have a label file.");
            else if (! trainFile.exists())
                showError("Model Directory Error", "Selected folder does not have a training file.");
            else
                valid = true;
            if (! valid)
                newDir = dirChooser.open();
        }
        // Do we have a new directory?
        if (newDir != null) {
            try {
                analyzeModelDirectory(newDir);
                trainingProperties.put(MODEL_DIRECTORY, modelDir.getAbsolutePath());
            } catch (IOException e) {
                showError("Error in Model Directory", e.getMessage());
            }
        }
    }

    /**
     * Display an error message box.
     *
     * @param title		title to show in the box
     * @param message	error message text
     */
    public void showError(String title, String message) {
        ShellUtils.showErrorBox(shlTrainingManager, title, message);
    }

    /**
     * Process a change to a new model directory.  We update the controls and determine the model type.
     *
     * @param newDir		new model directory
     * @throws IOException
     */
    protected void analyzeModelDirectory(String newDir) throws IOException {
        modelDir = new File(newDir);
        txtModelDirectory.setText(modelDir.getName());
        // Now we need to determine what type of model this is.
        File labelFile = new File(modelDir, "labels.txt");
        Set<String> labels = LineReader.readSet(labelFile);
        File trainFile = new File(modelDir, "training.tbl");
        try (LineReader reader = new LineReader(trainFile)) {
            String header = reader.next();
            // If all of the labels are in this header line, it is a regression model.
            List<String> headers = Arrays.asList(StringUtils.split(header, '\t'));
            int count = 0;
            for (String head : headers) {
                if (labels.contains(head)) count++;
            }
            if (count == labels.size()) {
                // Here we have a regression model.
                modelType = TrainingProcessor.Type.REGRESSION;
                btnRegression.setSelection(true);
                btnClassifier.setSelection(false);
            } else {
                modelType = TrainingProcessor.Type.CLASS;
                btnRegression.setSelection(false);
                btnClassifier.setSelection(true);
            }
            configureType();
            // Check for a parms.prm file.
            parmFile = new File(modelDir, "parms.prm");
            if (! parmFile.exists()) {
                // Here we must create one.
                TrainingProcessor processor = TrainingProcessor.create(modelType);
                // Set the defaults.
                processor.setAllDefaults();
                // Count the training set.
                int size = 0;
                while (reader.hasNext()) {
                    reader.next();
                    size++;
                }
                // Compute the testing set size.
                int testSize = size / 10;
                if (testSize < 1) testSize = 1;
                processor.setTestSize(testSize);
                // Pull up the meta-column dialog.  We need to delete the label columns from the header list first.
                List<String> availableHeaders;
                if (modelType != TrainingProcessor.Type.REGRESSION)
                    availableHeaders = headers;
                else
                    availableHeaders = headers.stream().filter(x -> ! labels.contains(x)).collect(Collectors.toList());
                MetaDialog metaColFinder = new MetaDialog(shlTrainingManager, SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL,
                        availableHeaders, modelType);
                String[] metaCols = metaColFinder.open();
                processor.setMetaCols(metaCols);
                if (metaCols.length > 0)
                    processor.setIdCol(metaCols[0]);
                processor.writeParms(parmFile);
            }
            // If we made it here without any errors, we can enable the buttons.
            enableButtons(true);
        }
    }

    /**
     * Enable or disable the run buttons.
     *
     * @param enabled	TRUE if the buttons can be enabled
     */
    private void enableButtons(boolean enabled) {
        btnRunSearch.setEnabled(enabled);
        btnXValidate.setEnabled(enabled);
        btnClassifier.setEnabled(enabled);
        btnRegression.setEnabled(enabled);
        btnEditParms.setEnabled(enabled);
        btnGraph.setEnabled(enabled);
        btnAbort.setEnabled(! enabled);
    }

    @Override
    public void showMessage(String message) {
        display.asyncExec(new Message(message));
    }

    @Override
    public void showResults(String paragraph) {
        display.asyncExec(new Report(paragraph));
    }

    @Override
    public void displayEpoch(int epoch, double score, boolean saved) {
        display.asyncExec(new Progress(epoch, score, saved));
    }

    /**
     * This method is called when a background command terminates.
     */
    public void reportCommandEnded() {
        backgrounder = null;
        display.asyncExec(new Enable());
    }

    /**
     * Runnable to store a report.
     */
    private class Report implements Runnable {

        private String report;
        private Pattern LINE_END = Pattern.compile("\\r?\\n");

        public Report(String report) {
            this.report = report;
        }

        @Override
        public void run() {
            String[] lines = LINE_END.split(this.report);
            txtResults.setText(StringUtils.join(lines, System.getProperty("line.separator")));
        }

    }

    /**
     * Runnable to set the progress controls.
     */
    private class Progress implements Runnable {

        private int epoch;
        private double score;
        private boolean saved;

        public Progress(int epoch, double score, boolean saved) {
            this.epoch = epoch;
            this.score = score;
            this.saved = saved;
        }

        @Override
        public void run() {
            String eString = Integer.toString(this.epoch);
            txtEpoch.setText(eString);
            txtScore.setText(String.format("%10.6g", this.score));
            if (this.saved)
                txtBestEpoch.setText(eString);
            int intScore = 0;
            if (this.score > MIN_SCORE)
                intScore = (int) ((Math.log10(this.score) - LOG10_LOWER) * 100 / (LOG10_UPPER - LOG10_LOWER));
            if (intScore >  100) intScore = 100;
            barScore.setSelection(intScore);
        }

    }

    /**
     * Runnable to update the status message.
     */
    private class Message implements Runnable {

        private String text;

        public Message(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            txtStatus.setText(text);
        }

    }

    /**
     * Runnable to turn the buttons back on.
     */
    private class Enable implements Runnable {

        @Override
        public void run() {
            enableButtons(true);
            barScore.setSelection(0);
        }

    }
}
