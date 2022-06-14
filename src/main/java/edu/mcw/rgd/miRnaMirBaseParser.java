package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author mtutaj
 * @since 5/14/15
 * download a file from mirbase to get all mirbase ids for given species
 */
public class miRnaMirBaseParser extends RecordPreprocessor {

    Logger log = LogManager.getLogger("status");

    private String mirBaseGenomeFile;
    private int speciesTypeKey;

    public String getMirBaseGenomeFile() {
        return mirBaseGenomeFile;
    }

    public void setMirBaseGenomeFile(String mirBaseGenomeFile) {
        this.mirBaseGenomeFile = mirBaseGenomeFile;
    }

    public int getSpeciesTypeKey() {
        return speciesTypeKey;
    }

    public void setSpeciesTypeKey(int speciesTypeKey) {
        this.speciesTypeKey = speciesTypeKey;
    }

    @Override
    public void process() throws Exception {
        try {
            run();
        } catch(Exception e) {
            Utils.printStackTrace(e, log);
            throw e;
        }
    }

    void run() throws Exception {
        // download miRNA genome file
        log.debug("  downloading " + getMirBaseGenomeFile());
        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile(getMirBaseGenomeFile());
        downloader.setLocalFile("data/mirbase_"+ SpeciesType.getCommonName(getSpeciesTypeKey()).toLowerCase()+".gff3.gz");
        downloader.setUseCompression(true);
        downloader.setAppendDateStamp(true);
        String localFile = downloader.downloadNew();
        log.debug("  downloaded "+getMirBaseGenomeFile()+" to "+localFile);

        // download map of MIMATxxx ids mapped to MIRBASE ids
        Map<String,String> mimatMap = loadMimatMap(localFile);

        // randomize the values
        List<String> mimatIds = new ArrayList<>(mimatMap.values());
        Collections.shuffle(mimatIds);
        int i = 0;

        // go over all MI ids and download predicted and validated gene associations for them
        for( String idMI: mimatIds ) {
            i++;
            log.debug(i+"/"+mimatIds.size()+".  processing "+idMI);

            miRnaRecord rec = new miRnaRecord();
            rec.setRecNo(i);
            rec.setIdMI(idMI);

            getSession().putRecordToFirstQueue(rec);
        }
    }

    Map<String,String> loadMimatMap(String localFile) throws IOException {

        log.debug("  parsing "+localFile);
        Map<String,String> mimatMap = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(localFile))));
        String line;

        // sample line to be parsed:
        // chr1	.	miRNA	23272274	23272295	.	+	.	ID=MIMAT0000128;Alias=MIMAT0000128;Name=mmu-miR-30a-5p;Derives_from=MI0000144
        //
        // we need toString()  extract MIMAT and MI ids
        while( (line=reader.readLine())!=null ) {
            int posMI = line.lastIndexOf("Derives_from=");
            if( posMI<0 )
                continue;
            posMI += 13; // position right after 'Derives_from='

            int pos1MIMAT = line.lastIndexOf("Alias=");
            if( pos1MIMAT<0 )
                continue;
            pos1MIMAT += 6; // position right after 'Alias='

            int pos2MIMAT = line.indexOf(';', pos1MIMAT);

            String idMIMAT = line.substring(pos1MIMAT, pos2MIMAT);
            String idMI = line.substring(posMI).trim();
            mimatMap.put(idMIMAT, idMI);
        }
        log.info("  MIMAT->MI associations loaded: " + mimatMap.size());

        reader.close();
        log.debug("  parsed "+localFile);
        return mimatMap;
    }

}
