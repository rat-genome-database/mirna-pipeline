package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/13/15
 * Time: 10:37 AM
 * <p>
 * downloads data from the external website
 */
public class miRnaDownloader extends RecordProcessor {

    Log log = LogFactory.getLog("core");

    private String uriConfirmed;
    private String uriPredicted;
    private int speciesTypeKey;

    public String getUriConfirmed() {
        return uriConfirmed;
    }

    public void setUriConfirmed(String uriConfirmed) {
        this.uriConfirmed = uriConfirmed;
    }

    public String getUriPredicted() {
        return uriPredicted;
    }

    public void setUriPredicted(String uriPredicted) {
        this.uriPredicted = uriPredicted;
    }

    public int getSpeciesTypeKey() {
        return speciesTypeKey;
    }

    public void setSpeciesTypeKey(int speciesTypeKey) {
        this.speciesTypeKey = speciesTypeKey;
    }

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        miRnaRecord rec = (miRnaRecord) pipelineRecord;

        log.debug("FD "+rec.getRecNo()+". "+rec.getIdMI());

        try {
            run(rec);
            log.debug("  FD "+rec.getRecNo()+". "+rec.getIdMI()+" OK!");
        } catch(Exception e) {
            e.printStackTrace();
            Utils.printStackTrace(e, log);
            log.error("ERROR: FD "+rec.getRecNo()+". "+rec.getIdMI());
            throw e;
        }
    }

    public void run(miRnaRecord rec) throws Exception {

        String filePrefix = buildFilePrefix();

        FileDownloader downloader = new FileDownloader();
        downloader.setUseCompression(true);
        downloader.setMaxRetryCount(1);
        downloader.setDownloadRetryInterval(20); // 20s: retry download after 20s (default was 60s)
        downloader.setSoTimeout(300000); // set SO_TIMEOUT to 300s (5 minutes) to wait for the data to be downloaded

        // go over all MI ids and download predicted and validated gene associations for them
        downloader.setExternalFile(getUriConfirmed()+rec.getIdMI());
        downloader.setLocalFile(filePrefix+"confirmed_"+rec.getIdMI()+".xml.gz");
        String localFile = downloadFile(downloader);
        rec.setLocalFileConfirmed(localFile);

        downloader.setExternalFile(getUriPredicted() + rec.getIdMI());
        downloader.setLocalFile(filePrefix+"predicted_" + rec.getIdMI() + ".xml.gz");
        localFile = downloadFile(downloader);
        rec.setLocalFilePredicted(localFile);
    }

    String buildFilePrefix() {
        // in the file prefix, include the current year and quarter, and the species
        Date dt = new Date();
        int year = dt.getYear()+1900;
        int quarter = 1 + dt.getMonth()/3;
        String filePrefix = "data/"+year+"q"+quarter;
        String species = SpeciesType.getCommonName(getSpeciesTypeKey()).toLowerCase();
        return filePrefix+"_"+species+"_";
    }

    String downloadFile(FileDownloader downloader) throws Exception {

        // if the file has 0 size, remove it, in order to download it from the remote site
        File localFile = new File(downloader.getLocalFile());
        if( localFile.length()==0 ) {
            localFile.delete();
        }

        try {
            return downloader.downloadNew();
        } catch( FileDownloader.PermanentDownloadErrorException e ) {
            // create empty file
            String localFileName = downloader.getLocalFile();
            new File(localFileName).createNewFile();
            return localFileName;
        }
    }
}