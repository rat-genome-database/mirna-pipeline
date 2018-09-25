package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.pipelines.PipelineRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/1/15
 * Time: 4:55 PM
 */
public class miRnaRecord extends PipelineRecord {

    private String idMI;
    private String localFileConfirmed;
    private String localFilePredicted;

    private List<miRnaEntry> entries = new ArrayList<>();
    private List<MiRnaTarget> dataInRgd;

    public String getIdMI() {
        return idMI;
    }

    public void setIdMI(String idMI) {
        this.idMI = idMI;
    }

    public String getLocalFileConfirmed() {
        return localFileConfirmed;
    }

    public void setLocalFileConfirmed(String localFileConfirmed) {
        this.localFileConfirmed = localFileConfirmed;
    }

    public String getLocalFilePredicted() {
        return localFilePredicted;
    }

    public void setLocalFilePredicted(String localFilePredicted) {
        this.localFilePredicted = localFilePredicted;
    }

    public List<miRnaEntry> getEntries() {
        return entries;
    }

    public List<MiRnaTarget> getDataInRgd() {
        return dataInRgd;
    }

    public void setDataInRgd(List<MiRnaTarget> dataInRgd) {
        this.dataInRgd = dataInRgd;
    }

    /**
     * add a new entry to list of entries only if it is unique
     * @param entry entry to be added
     * @return true if the new entry was unique and was added,
     * or false if the entry was not added because it was already in the list
     */
    public boolean addUniqueEntry(miRnaEntry entry) {
        // check if the entry to be added is unique
        for( miRnaEntry e: getEntries() ) {
            if( e.getMiRnaTarget().equalsByContent2(entry.getMiRnaTarget()) ) {
                return false; // entry already on the list
            }
        }

        return getEntries().add(entry);
    }
}
