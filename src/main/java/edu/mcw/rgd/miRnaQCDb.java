package edu.mcw.rgd;

import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

/**
 * @author mtutaj
 * @since 9/22/15
 * pre-qc of a single record identified by a unique MI (mirbase) id:
 * all information needed by QC from database is loaded here
 */
public class miRnaQCDb extends RecordProcessor {

    Logger log = Logger.getLogger("core");

    private int speciesTypeKey;
    private miRnaDAO dao;

    public int getSpeciesTypeKey() {
        return speciesTypeKey;
    }

    public void setSpeciesTypeKey(int speciesTypeKey) {
        this.speciesTypeKey = speciesTypeKey;
    }

    public miRnaDAO getDao() {
        return dao;
    }

    public void setDao(miRnaDAO dao) {
        this.dao = dao;
    }

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        miRnaRecord rec = (miRnaRecord) pipelineRecord;

        log.debug("QCDB "+rec.getRecNo()+". "+rec.getIdMI());

        try{
            qc(rec);
            log.debug("  QCDB "+rec.getRecNo()+". "+rec.getIdMI()+" OK!");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.printStackTrace(e, log);
            log.error("ERROR: QCDB "+rec.getRecNo()+". "+rec.getIdMI());
            throw e;
        }
    }

    void qc(miRnaRecord rec) throws Exception {

        // match miRna gene by mirbase id
        int miRnaRgdId = getDao().getGeneRgdIdByMirBaseId(rec.getIdMI(), getSpeciesTypeKey());
        if( miRnaRgdId!=0 ) {
            getSession().incrementCounter("MATCH_MIRNA_BY_MIRBASE", 1);
        } else {
            getSession().incrementCounter("MATCH_MIRNA_NONE", 1);
            return;
        }

        for( miRnaEntry entry: rec.getEntries() ) {
            // match target gene by gene symbol
            int geneRgdId = getDao().getGeneRgdIdBySymbol(entry.getSymbolHgnc());
            if( geneRgdId>0 ) {
                getSession().incrementCounter("MATCH_TARGET_BY_SYMBOL", 1);
            } else {
                geneRgdId = getDao().getGeneRgdIdByEnsemblGeneId(entry.getSymbolEnsembl(), getSpeciesTypeKey());
                if( geneRgdId>0 ) {
                    getSession().incrementCounter("MATCH_TARGET_BY_ENSEMBL_ID", 1);
                } else if( geneRgdId<0 ) {
                    getSession().incrementCounter("MATCH_TARGET_BY_ENSEMBL_ID_MULTIPLE", 1);
                } else {
                    getSession().incrementCounter("MATCH_TARGET_NONE", 1);
                }
            }
            entry.getMiRnaTarget().setGeneRgdId(geneRgdId);

            entry.getMiRnaTarget().setMiRnaRgdId(miRnaRgdId);
        }

        // qc mirna data in rgd against incoming data
        rec.setDataInRgd(getDao().getMiRnaTargets(miRnaRgdId));
    }
}
