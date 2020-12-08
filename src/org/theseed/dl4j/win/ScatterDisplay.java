/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.model.CartesianSeriesModel;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.theseed.dl4j.train.RegressionTrainingProcessor;
import org.theseed.reports.RegressionValidationScatter;
import org.theseed.win.ShellUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * This displays a scatter diagram of the expected versus output values for a model when applied to the
 * entire training set.  If a "trained.tbl" file is available, the training set items will be shown in
 * blue and the testing set items in red.
 *
 * @author Bruce Parrello
 *
 */
public class ScatterDisplay extends Dialog {

    // FIELDS
    /** window shell */
    protected Shell shell;
    /** label selection combo box */
    private Combo cmbLabel;
    /** main scatter graph */
    private Chart chartMain;
    /** prediction report */
    private RegressionValidationScatter reporter;
    /** name of training file */
    private Text txtTrainingFile;
    /** model processor */
    private RegressionTrainingProcessor processor;
    /** current training file */
    private File trainFile;
    /** button to plot the graph */
    private Button btnReplot;

    /**
     * Create the dialog.
     *
     * @param parent		parent display object
     * @param style			window style
     * @param name			name of the model to display
     * @param processor		processor for the model to display
     */
    public ScatterDisplay(Shell parent, int style, String name, RegressionTrainingProcessor processor) {
        super(parent, style);
        setText("Validation Results for " + name);
        // Create the reporter.
        this.reporter = new RegressionValidationScatter();
        this.processor = processor;
        // Save the training file.
        this.trainFile = new File(processor.getModelDir(), "training.tbl");
    }

    /**
     * Open the dialog.
     */
    public void open() {
        createContents();
        shell.open();
        shell.layout();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shell = new Shell(getParent(), SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setImage(SWTResourceManager.getImage(ScatterDisplay.class, "/org/theseed/images/fig-gear.ico"));
        ShellUtils.persistPosition(shell, this, 850, 500);
        shell.setText(getText());
        shell.setLayout(new GridLayout(5, false));

        Label lblLabelColumn = new Label(shell, SWT.NONE);
        lblLabelColumn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLabelColumn.setText("Label Column");

        cmbLabel = new Combo(shell, SWT.NONE);
        GridData gd_cmbLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_cmbLabel.widthHint = 150;
        cmbLabel.setLayoutData(gd_cmbLabel);
        List<String> itemList = processor.getLabels();
        String[] items = new String[itemList.size()];
        cmbLabel.setItems(itemList.toArray(items));
        cmbLabel.setText(items[0]);

        Label lblTrainingFile = new Label(shell, SWT.NONE);
        lblTrainingFile.setText("Training File");

        Composite composite_1 = new Composite(shell, SWT.NONE);
        composite_1.setLayout(new GridLayout(2, false));
        GridData gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_composite_1.heightHint = 30;
        composite_1.setLayoutData(gd_composite_1);

        txtTrainingFile = new Text(composite_1, SWT.BORDER | SWT.READ_ONLY);
        GridData gd_txtTrainingFile = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_txtTrainingFile.widthHint = 200;
        txtTrainingFile.setLayoutData(gd_txtTrainingFile);
        txtTrainingFile.setText("training.tbl");

        Button btnSelectTrainFile = new Button(composite_1, SWT.NONE);
        btnSelectTrainFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectTrainingFile();
            }
        });
        GridData gd_btnSelectTrainFile = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_btnSelectTrainFile.heightHint = 20;
        btnSelectTrainFile.setLayoutData(gd_btnSelectTrainFile);
        btnSelectTrainFile.setText("...");

        btnReplot = new Button(shell, SWT.NONE);
        GridData gd_btnReplot = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_btnReplot.widthHint = 100;
        btnReplot.setLayoutData(gd_btnReplot);
        btnReplot.setText("Plot Graph");
        btnReplot.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                plotGraph();
            }
        });

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 5, 1));
        composite.setLayout(new GridLayout(1, false));

        chartMain = new Chart(composite, SWT.NONE);
        chartMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        new Label(composite, SWT.NONE);
        new Label(composite, SWT.NONE);

        // Initialize the graph.
        initGraph();
        // Try to run the predictions.
        runPredictions();
    }

    /**
     * Select a new training file to run.
     */
    protected void selectTrainingFile() {
        File dir = processor.getModelDir();
        File trainTestFile = ShellUtils.selectFile(shell, dir);
        // Do we have a new training file?
        if (trainTestFile != null) {
            this.trainFile = trainTestFile;
            txtTrainingFile.setText(trainTestFile.getName());
            runPredictions();
        }
    }

    /**
     * Run the predictions on the current training set.
     */
    private void runPredictions() {
        try {
            processor.runPredictions(this.reporter, this.trainFile);
            btnReplot.setEnabled(true);
        } catch (IOException e) {
            btnReplot.setEnabled(false);
            ShellUtils.showErrorBox(shell, "Prediction Error", e.getMessage());
        }
    }

    /**
     * Initialize the graph to an empty state.
     */
    private void initGraph() {
        chartMain.getTitle().setText("Expected vs. Output");
        chartMain.getAxisSet().getXAxis(0).getTitle().setText("Expected");
        chartMain.getAxisSet().getYAxis(0).getTitle().setText("Output");
    }

    /**
     * Plot the graph using the selected label.
     */
    private void plotGraph() {
        @SuppressWarnings("unchecked")
        ILineSeries<String> trainingSeries = (ILineSeries<String>) chartMain.getSeriesSet()
                .createSeries(ISeries.SeriesType.LINE, "training");
        trainingSeries.setLineStyle(LineStyle.NONE);
        trainingSeries.setSymbolType(ILineSeries.PlotSymbolType.CIRCLE);
        trainingSeries.setSymbolColor(new Color(0, 0, 255));
        trainingSeries.setSymbolSize(3);
        CartesianSeriesModel<String> trainingModel = this.reporter.new Model(cmbLabel.getSelectionIndex(), true);
        trainingSeries.setDataModel(trainingModel);
        @SuppressWarnings("unchecked")
        ILineSeries<String> testingSeries = (ILineSeries<String>) chartMain.getSeriesSet()
                .createSeries(ISeries.SeriesType.LINE, "testing");
        testingSeries.setLineStyle(LineStyle.NONE);
        testingSeries.setSymbolType(ILineSeries.PlotSymbolType.CIRCLE);
        testingSeries.setSymbolColor(new Color(255, 0, 0));
        testingSeries.setSymbolSize(3);
        CartesianSeriesModel<String> testingModel = this.reporter.new Model(cmbLabel.getSelectionIndex(), false);
        testingSeries.setDataModel(testingModel);
        chartMain.getAxisSet().adjustRange();
        chartMain.redraw();
    }
}
