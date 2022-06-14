package edu.mcw.rgd;

import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * @author mtutaj
 * @since 5/4/15
 * file parser, for confirmed and predicted miRna targets
 */
public class miRnaParser extends RecordProcessor {

    Logger log = LogManager.getLogger("status");

    int confirmedFilesWithParseError = 0;
    int predictedFilesWithParseError = 0;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        miRnaRecord rec = (miRnaRecord) pipelineRecord;

        log.debug("PP "+rec.getRecNo()+". "+rec.getIdMI());

        try {
            run(rec);
        } catch(Exception e) {
            Utils.printStackTrace(e, log);
            log.error("ERROR: PP "+rec.getRecNo()+". "+rec.getIdMI());
            throw e;
        }
    }

    public void run(miRnaRecord rec) throws Exception {

        log.debug("  PP "+rec.getRecNo()+". "+rec.getIdMI()+" confirmed...");
        parseConfirmed(rec);
        log.debug("  PP "+rec.getRecNo()+". "+rec.getIdMI()+" predicted...");
        parsePredicted(rec);
        log.debug("  PP "+rec.getRecNo()+". "+rec.getIdMI()+" OK!");
    }

    public void printStats(Logger log) {
        log.info("CONFIRMED FILES WITH PARSE ERROR "+confirmedFilesWithParseError);
        log.info("PREDICTED FILES WITH PARSE ERROR "+predictedFilesWithParseError);
    }

    void parseConfirmed(miRnaRecord rec) throws Exception {

        String fileName = rec.getLocalFileConfirmed();
        File file = new File(fileName);

        try {
            miRnaConfirmedTargetParser parser = new miRnaConfirmedTargetParser();
            parser.setRecord(rec);
            parser.setValidate(false);
            parser.setRecordDepth(3);
            parser.parse(file);
        } catch(nu.xom.ParsingException |java.util.zip.ZipException e) {
            confirmedFilesWithParseError++;
            handleFileCausingException(fileName, file, e);
        }
    }

    void parsePredicted(miRnaRecord rec) throws Exception {

        String fileName = rec.getLocalFilePredicted();
        File file = new File(fileName);

        try {
            miRnaPredictedTargetParser parser = new miRnaPredictedTargetParser();
            parser.setRecord(rec);
            parser.setValidate(false);
            parser.setRecordDepth(3);
            parser.parse(file);
        } catch(nu.xom.ParsingException |java.util.zip.ZipException e) {
            predictedFilesWithParseError++;
            handleFileCausingException(fileName, file, e);
        }
    }

    void handleFileCausingException(String fileName, File file, Exception e) throws Exception {
        System.out.println("File will be reprocessed: "+fileName);
        log.debug("File will be reprocessed: "+fileName);
        if( e instanceof nu.xom.ParsingException ) {
            // parsing problem: append '.error' to file name
            File errorFile = new File(fileName+".error");
            log.debug("  error file "+errorFile.getAbsolutePath());
            if( !errorFile.exists() ) {
                log.debug("  renaming ...");
                if( file.renameTo(errorFile) ) {
                    log.debug("  rename OK!");
                } else {
                    log.debug("  rename failed!");
                }
            } else {
                log.debug("  deleting ...");
                if( file.delete() ) {
                    log.debug("  delete OK!");
                } else {
                    log.debug("  delete failed!");
                }
            }
        } else {
            log.debug("  deleting ...");
            if( file.delete() ) {
                log.debug("  delete OK!");
            } else {
                log.debug("  delete failed!");
            }
        }
        throw e;
    }
}
