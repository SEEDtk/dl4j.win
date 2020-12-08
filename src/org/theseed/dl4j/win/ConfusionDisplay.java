/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.theseed.dl4j.train.ClassTrainingProcessor;
import org.theseed.reports.ClassValidationConfusion;
import org.theseed.win.ShellUtils;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bruce Parrello
 *
 */
public class ConfusionDisplay extends Dialog {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ConfusionDisplay.class);
    /** window for this dialog */
    protected Shell shell;
    /** training file name display control */
    private Text txtTrainingFile;
    /** training file */
    private File trainFile;
    /** processor to run */
    private ClassTrainingProcessor processor;
    /** confusion matrix table controls */
    private Table[] tblMatrix;
    /** reporting object for computing matrices */
    private ClassValidationConfusion reporter;
    /** number of labels in this model */
    private int nLabels;
    /** labels in this model */
    private List<String> labels;
    /** list of table columns */
    private List<TableColumn> columns;
    /** array of tab display composites */
    private Composite[] tabHolders;
    /** main tab folder */
    private TabFolder tabFolder;
    /** color for heading rows and columns */
    private static final Color HEADER_COLOR = SWTResourceManager.getColor(192, 192, 192);
    /** color for total rows and columns */
    private static final Color TOTAL_COLOR = SWTResourceManager.getColor(255, 255, 192);
    /** layout for table holders */
    private static final GridLayout HOLDER_LAYOUT = buildHolderLayout();
    /** indices for tables */
    private static final int TEST_MATRIX = 0;
    private static final int TRAIN_MATRIX = 1;
    private static final int ALL_MATRIX = 2;

    /**
     * Create the dialog.
     *
     * @param parent		parent window
     * @param style			dialog style
     * @param processor		classification model processor
     */
    public ConfusionDisplay(Shell parent, int style, ClassTrainingProcessor processor) {
        super(parent, style);
        this.processor = processor;
        setText("Confusion Matrix for " + processor.getModelDir().getName());
        this.trainFile = new File(processor.getModelDir(), "training.tbl");
        this.reporter = new ClassValidationConfusion();
        // Get the labels.
        this.labels = processor.getLabels();
        this.nLabels = labels.size();

    }

    /**
     * @return the layout for a table holder
     */
    private static GridLayout buildHolderLayout() {
        GridLayout retVal = new GridLayout(2, false);
        retVal.marginTop = 20;
        retVal.marginLeft = 20;
        retVal.marginRight = 20;
        retVal.marginBottom = 20;
        return retVal;
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
        shell = new Shell(getParent(), SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
        shell.setImage(SWTResourceManager.getImage(ConfusionDisplay.class, "/org/theseed/images/fig-gear.ico"));
        ShellUtils.persistLocation(shell, this, 600, 450);
        shell.setText(getText());
        shell.setLayout(new GridLayout(4, false));
        Label lblTrainingFile = new Label(shell, SWT.NONE);
        lblTrainingFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblTrainingFile.setText("Training File");

        txtTrainingFile = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
        txtTrainingFile.setText(trainFile.getName());
        GridData gd_txtTrainingFile = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_txtTrainingFile.widthHint = 150;
        txtTrainingFile.setLayoutData(gd_txtTrainingFile);

        Button btnSelectFile = new Button(shell, SWT.NONE);
        btnSelectFile.setText("...");
        btnSelectFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectTrainingFile();
            }
        });

        Button btnCompute = new Button(shell, SWT.NONE);
        btnCompute.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runPredictions();
            }
        });
        btnCompute.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        btnCompute.setText("Compute");

        CLabel lblNewLabel = new CLabel(shell, SWT.NONE);
        GridData gd_lblNewLabel = new GridData(SWT.CENTER, SWT.BOTTOM, false, false, 4, 1);
        gd_lblNewLabel.heightHint = 30;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText("Matrix shows actual value (columns) vs. predicted value (rows)");

        tabFolder = new TabFolder(shell, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        TabItem tbtmTesting = new TabItem(tabFolder, SWT.NONE);
        tbtmTesting.setText("Testing");

        TabItem tbtmTraining = new TabItem(tabFolder, SWT.NONE);
        tbtmTraining.setText("Training");

        TabItem tbtmAll = new TabItem(tabFolder, SWT.NONE);
        tbtmAll.setText("All");

        // Create the matrix tables.
        this.columns = new ArrayList<TableColumn>(nLabels * 3 + 6);
        this.tblMatrix = new Table[3];
        this.tabHolders = new Composite[3];
        for (int i = 0; i < 3; i++) {
            this.tabHolders[i] = new Composite(tabFolder, SWT.NONE);
            this.tabHolders[i].setLayout(HOLDER_LAYOUT);
            this.tabHolders[i].setBackground(shell.getBackground());
            new Label(this.tabHolders[i], SWT.NONE);
            Label lblActual = new Label(this.tabHolders[i], SWT.NONE);
            lblActual.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
            lblActual.setText("actual");
            Label lblPredicted = new Label(this.tabHolders[i], SWT.VERTICAL);
            lblPredicted.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
            lblPredicted.setText("p\nr\ne\nd\ni\nc\nt\ne\nd");
            lblPredicted.pack();
            this.tblMatrix[i] = new Table(this.tabHolders[i], SWT.BORDER | SWT.SINGLE | SWT.NO_SCROLL);
            tblMatrix[i].setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false, 1, 1));
            this.tblMatrix[i].setHeaderBackground(HEADER_COLOR);
            this.tblMatrix[i].setHeaderVisible(true);
            this.tblMatrix[i].setLinesVisible(true);
            // We have a header column, one data column per label, and a total column.
            TableColumn column = new TableColumn(this.tblMatrix[i], SWT.LEFT | SWT.BORDER);
            column.setText("");
            this.columns.add(column);
            for (int r = 0; r < nLabels; r++) {
                column = new TableColumn(this.tblMatrix[i], SWT.RIGHT | SWT.BORDER);
                column.setText(labels.get(r));
                this.columns.add(column);
            }
            column = new TableColumn(this.tblMatrix[i], SWT.RIGHT | SWT.BORDER);
            column.setText("TOTAL");
            this.columns.add(column);
        }
        tbtmTesting.setControl(this.tabHolders[TEST_MATRIX]);
        tbtmTraining.setControl(this.tabHolders[TRAIN_MATRIX]);
        tbtmAll.setControl(this.tabHolders[ALL_MATRIX]);
        // Here we have a couple of events designed to fix the tables when the
        // container state changes.
        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                packCurrentTable();
            }
        });
        shell.addListener(SWT.Resize,  new Listener () {
            public void handleEvent (Event e) {
                packTables();
                packCurrentTable();
            }
          });
        packTables();
    }

    /**
     * Repack all the table columns.
     */
    private void packTables() {
        this.columns.stream().forEach(x -> x.pack());
        this.tblMatrix[tabFolder.getSelectionIndex()].pack();
    }

    /**
     * Make the predictions and fill in the three tables.
     */
    protected void runPredictions() {
        try {
            processor.runPredictions(reporter, this.trainFile);
        } catch (IOException e) {
            ShellUtils.showErrorBox(shell, "Prediction Computation Error", e.getMessage());
        }
        // Refill all of the tables.
        for (int type = 0; type < 3; type++) {
            // Clear this table.
            this.tblMatrix[type].removeAll();
            // We will track the column totals in here.
            int[] total = new int[nLabels+1];
            Arrays.fill(total, 0);
            // Add the data rows, one per label.
            for (int o = 0; o < nLabels; o++) {
                String[] cells = new String[nLabels + 2];
                cells[0] = labels.get(o);
                int rowTotal = 0;
                for (int e = 0; e < nLabels; e++) {
                    int count = this.reporter.getCount(type, o, e);
                    cells[e+1] = Integer.toString(count);
                    total[e] += count;
                    rowTotal += count;
                    total[nLabels] += count;
                }
                TableItem item = new TableItem(this.tblMatrix[type], SWT.BORDER);
                item.setBackground(0, HEADER_COLOR);
                item.setBackground(nLabels+1, TOTAL_COLOR);
                cells[nLabels+1] = Integer.toString(rowTotal);
                item.setText(cells);
            }
            // Add the total row.
            TableItem item = new TableItem(this.tblMatrix[type], SWT.BORDER);
            item.setBackground(TOTAL_COLOR);
            item.setBackground(0, HEADER_COLOR);
            String[] cells = new String[nLabels + 2];
            cells[0] = "TOTAL";
            for (int e = 0; e <= nLabels; e++)
                cells[e+1] = Integer.toString(total[e]);
            item.setText(cells);
        }
        packTables();
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
        }
    }

    /**
     * Repack the currently-displayed table to compensate for a change in display state.
     */
    private void packCurrentTable() {
        int type = tabFolder.getSelectionIndex();
        tblMatrix[type].pack();
        log.info("Repacking table {}.", type);
    }

}
