/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.LineReader;

/**
 * This is the base class for the two validation reports.  It mostly handles the testing/training divide.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ValidationDisplayReport implements IValidationReport {

    /** IDs of records that were used in training */
    private Set<String> trained;
    /** ID column index in metadata */
    private int idColIdx;
    /** next available ID number */
    private int idNum;

    /**
     * Construct a blank scatter object.
     */
    public ValidationDisplayReport() {
        this.trained = Collections.emptySet();
        this.idNum = 1;
    }

    /**
     * @return the ID string from a row's metadata
     *
     * @param metaData	metadata string for this row
     */
    public String getId(String metaData) {
        String[] metaCols = StringUtils.split(metaData, '\t');
        String retVal;
        if (this.idColIdx >= 0 && metaCols.length > this.idColIdx)
            retVal = metaCols[this.idColIdx];
        else
            retVal = String.format("item %d", this.idNum++);
        return retVal;
    }

    /**
     * @return TRUE if the specified ID is in the training set, else FALSE
     *
     * @param id		ID to check
     */
    public boolean isTrained(String id) {
        return this.trained.contains(id);
    }

    @Override
    public void setupIdCol(File modelDir, String idCol, List<String> metaList, Collection<String> trainList) throws IOException {
        this.idColIdx = metaList.indexOf(idCol);
        if (trainList != null)
            this.trained = new HashSet<String>(trainList);
        else {
            File trainedFile = new File(modelDir, "trained.tbl");
            this.trained = LineReader.readSet(trainedFile);
        }
    }

    @Override
    public void close() {
    }

}
