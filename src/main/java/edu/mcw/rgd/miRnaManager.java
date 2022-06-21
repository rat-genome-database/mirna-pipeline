package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.pipelines.PipelineManager;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.*;

/**
 * @author jdepons
 * @since 4/26/12
 */
public class miRnaManager {

    private miRnaDAO dao = new miRnaDAO();
    private String version;

    Logger log = LogManager.getLogger("status");
    private List<String> mirbaseGenomeFile;
    private List<String> mirgateUriConfirmed;
    private List<String> mirgateUriPredicted;
    private int qcdbThreadCount;
    private int qcThreadCount;
    private int ldThreadCount;
    private int queueSize;
    private int ppThreadCount;
    private int fdThreadCount;
    private List<String> speciesProcessed;
    private int maxFailedFiles;
    private int staleDataDeleteThreshold;

    public static void main(String[] args) throws Exception {

        // parse cmdline stats
        boolean runLoad = false;
        boolean computeStats = false;

        for( String arg: args ) {
            switch(arg) {
                case "--load":
                    runLoad = true;
                    break;
                case "--stats":
                    computeStats = true;
                    break;
            }
        }
        if( !runLoad && !computeStats ) {
            usage();
        }

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));

        // run
        try {
            if( runLoad ) {
                miRnaManager manager = (miRnaManager) (bf.getBean("manager"));
                System.out.println(manager.getVersion());

                manager.run();
            }
            if( computeStats ) {
                miRnaStatLoader statLoader = (miRnaStatLoader) (bf.getBean("statLoader"));
                System.out.println(statLoader.getVersion());

                statLoader.run();
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void usage() {

        System.out.println("USAGE: ");
        System.out.println("  java -jar miRnaPipeline.jar [--load] [--stats]");
        System.out.println("where");
        System.out.println("  --load   run load for all data");
        System.out.println("  --stats  compute stats for all data");
        System.out.println("  --help   displays usage");
        System.out.println("");
        System.out.println("Note: at least one parameter must be supplied.");
        System.out.println("Note: multiple parameters can be supplied, in any order.");

        System.exit(0);
    }

    public void run() throws Exception {
        log.info("miRna pipeline starting");

        List<String> speciesList = getSpeciesProcessed();
        Collections.shuffle(speciesList);
        for( String species: speciesList ) {
            run(SpeciesType.parse(species));
        }

        log.info("miRna pipeline complete");
        System.out.println("miRna pipeline complete");
    }

    void run(int speciesTypeKey) throws Exception {

        long startTime = System.currentTimeMillis();
        Date startDate = new Date();

        // every species has its own set of gene symbols
        miRnaDAO.clearCaches();
        miRnaDAO.initCaches(speciesTypeKey);

        String uriConfirmed = getMirgateUriConfirmed().get(speciesTypeKey);
        String uriPredicted = getMirgateUriPredicted().get(speciesTypeKey);

        String mirBaseGff3File = getMirbaseGenomeFile().get(speciesTypeKey);
        String species = SpeciesType.getCommonName(speciesTypeKey);
        log.info("miRna processing for "+species);

        // setup the mirBase file downloader and parser
        miRnaMirBaseParser mirBaseParser = new miRnaMirBaseParser();
        mirBaseParser.setMirBaseGenomeFile(mirBaseGff3File);
        mirBaseParser.setSpeciesTypeKey(speciesTypeKey);

        // setup the file downloader
        miRnaDownloader fileDownloader = new miRnaDownloader();
        fileDownloader.setUriConfirmed(uriConfirmed);
        fileDownloader.setUriPredicted(uriPredicted);
        fileDownloader.setSpeciesTypeKey(speciesTypeKey);

        // setup the file parser
        miRnaParser parser = new miRnaParser();

        // setup qc thread
        miRnaQCDb qcdb = new miRnaQCDb();
        qcdb.setSpeciesTypeKey(speciesTypeKey);
        qcdb.setDao(dao);

        // setup qc thread
        miRnaQC qc = new miRnaQC();

        // setup data loading thread
        miRnaDataLoad dl = new miRnaDataLoad();
        dl.setDao(dao);

        int queueSize = getQueueSize();
        PipelineManager manager = new PipelineManager();
        manager.getSession().setAllowedExceptions(getMaxFailedFiles());
        manager.getSession().registerUserException(new String[]{"nu.xom.ParsingException"});
        manager.getSession().registerUserException(new String[]{"java.util.zip.ZipException"});

        // thread group: download and parse a file from mirBase
        manager.addPipelineWorkgroup(mirBaseParser, "MB", 1, queueSize);
        // thread group: download xml files
        manager.addPipelineWorkgroup(fileDownloader, "FD", getFdThreadCount(), queueSize);
        // thread group: parse xml files for confirmed and predicted targets
        manager.addPipelineWorkgroup(parser, "PP", getPpThreadCount(), queueSize);
        // thread group: qc incoming data against rgd database
        manager.addPipelineWorkgroup(qcdb, "QCDB", getQcdbThreadCount(), queueSize);
        // thread group: qc incoming data against rgd
        manager.addPipelineWorkgroup(qc, "QC", getQcThreadCount(), queueSize);
        // thread group: incremental import
        manager.addPipelineWorkgroup(dl, "DL", getLdThreadCount(), queueSize);

        // run everything
        manager.run();

        deleteStaleData(speciesTypeKey, startDate);

        // dump counter statistics
        manager.dumpCounters(log);
        parser.printStats(log);

        int multisCount = dao.printMultis();
        if( multisCount>0 ) {
            log.info("Unique symbol multis: "+multisCount);
        }

        log.info("OK! elapsed "+ Utils.formatElapsedTime(startTime, System.currentTimeMillis()));
        log.info("=====");
    }

    void deleteStaleData(int speciesTypeKey, Date startDate) throws Exception {

        int miRnaDataCountForSpecies = dao.getCountOfMiRnaData(speciesTypeKey);
        log.info(" MIRNA DATA COUNT for species "+SpeciesType.getCommonName(speciesTypeKey)+": "+miRnaDataCountForSpecies);

        List<MiRnaTarget> staleData = dao.getMiRnaDataModifiedBefore(startDate, speciesTypeKey);
        log.info(" DELETING STALE DATA: "+staleData.size());

        // cannot delete more than 5% of the data
        int percentile = (100 * staleData.size()) / miRnaDataCountForSpecies;
        if( percentile>getStaleDataDeleteThreshold() ) {
            log.error(" STALE DATA DELETION ABORTED: cannot delete more than "+getStaleDataDeleteThreshold()+"% of data! It was: "+percentile+"%");
            return;
        }

        int deleted = dao.deleteMiRnaDataModifiedBefore(startDate, speciesTypeKey);
        log.info(" DELETED STALE DATA: "+deleted);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setMirbaseGenomeFile(List<String> mirbaseGenomeFile) {
        this.mirbaseGenomeFile = mirbaseGenomeFile;
    }

    public List<String> getMirbaseGenomeFile() {
        return mirbaseGenomeFile;
    }

    public void setMirgateUriConfirmed(List<String> mirgateUriConfirmed) {
        this.mirgateUriConfirmed = mirgateUriConfirmed;
    }

    public List<String> getMirgateUriConfirmed() {
        return mirgateUriConfirmed;
    }

    public void setMirgateUriPredicted(List<String> mirgateUriPredicted) {
        this.mirgateUriPredicted = mirgateUriPredicted;
    }

    public List<String> getMirgateUriPredicted() {
        return mirgateUriPredicted;
    }

    public int getQcdbThreadCount() {
        return qcdbThreadCount;
    }

    public void setQcdbThreadCount(int qcdbThreadCount) {
        this.qcdbThreadCount = qcdbThreadCount;
    }

    public void setQcThreadCount(int qcThreadCount) {
        this.qcThreadCount = qcThreadCount;
    }

    public int getQcThreadCount() {
        return qcThreadCount;
    }

    public void setLdThreadCount(int ldThreadCount) {
        this.ldThreadCount = ldThreadCount;
    }

    public int getLdThreadCount() {
        return ldThreadCount;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setPpThreadCount(int ppThreadCount) {
        this.ppThreadCount = ppThreadCount;
    }

    public int getPpThreadCount() {
        return ppThreadCount;
    }

    public void setSpeciesProcessed(List<String> speciesProcessed) {
        this.speciesProcessed = speciesProcessed;
    }

    public List<String> getSpeciesProcessed() {
        return speciesProcessed;
    }

    public void setFdThreadCount(int fdThreadCount) {
        this.fdThreadCount = fdThreadCount;
    }

    public int getFdThreadCount() {
        return fdThreadCount;
    }

    public void setMaxFailedFiles(int maxFailedFiles) {
        this.maxFailedFiles = maxFailedFiles;
    }

    public int getMaxFailedFiles() {
        return maxFailedFiles;
    }

    public void setStaleDataDeleteThreshold(int staleDataDeleteThreshold) {
        this.staleDataDeleteThreshold = staleDataDeleteThreshold;
    }

    public int getStaleDataDeleteThreshold() {
        return staleDataDeleteThreshold;
    }
}

