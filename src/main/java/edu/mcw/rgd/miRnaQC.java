package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

/**
 * @author mtutaj
 * @since 5/4/15
 * actual qc of a single record identified by a unique MI (mirbase) id:
 * all information from database is already loaded in
 */
public class miRnaQC extends RecordProcessor {

    Logger log = Logger.getLogger("core");

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        miRnaRecord rec = (miRnaRecord) pipelineRecord;

        log.debug("QC "+rec.getRecNo()+". "+rec.getIdMI());

        try{
            qc(rec);
            log.debug("  QC "+rec.getRecNo()+". "+rec.getIdMI()+" OK!");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.printStackTrace(e, log);
            log.error("ERROR: QC "+rec.getRecNo()+". "+rec.getIdMI());
            throw e;
        }
    }

    void qc(miRnaRecord rec) throws Exception {

        if( rec.getDataInRgd()==null ) {
            // no data in RGD -- nothing to do in this stage
            return;
        }

        // qc mirna data in rgd against incoming data
        for( MiRnaTarget miRnaInRgd: rec.getDataInRgd() ) {
            // see if miRnaInRgd matches incoming data
            for( miRnaEntry entry: rec.getEntries() ) {
                if( miRnaInRgd.equalsByContent2(entry.getMiRnaTarget()) ) {
                    entry.getMiRnaTarget().setKey(miRnaInRgd.getKey());
                    break;
                }
            }
        }
    }
}
