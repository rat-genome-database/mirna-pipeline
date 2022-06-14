package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since 5/4/15
 */
public class miRnaDataLoad extends RecordProcessor {

    Logger log = LogManager.getLogger("status");

    private miRnaDAO dao;

    public miRnaDAO getDao() {
        return dao;
    }

    public void setDao(miRnaDAO dao) {
        this.dao = dao;
    }

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {
        miRnaRecord rec = (miRnaRecord) pipelineRecord;

        log.debug("LD "+rec.getRecNo()+". "+rec.getIdMI());

        try{
            if( !rec.getEntries().isEmpty() )
                load(rec);
        } catch (Exception e) {
            Utils.printStackTrace(e, log);
            log.error("ERROR: LD "+rec.getRecNo()+". "+rec.getIdMI());
            throw e;
        }
    }

    void load(miRnaRecord rec) throws Exception {

        List<MiRnaTarget> targetsToInsert = new ArrayList<>();
        List<Integer> targetsMatching = new ArrayList<>();

        for( miRnaEntry entry: rec.getEntries() ) {
            MiRnaTarget target = entry.getMiRnaTarget();
            if( target.getGeneRgdId()<=0 || target.getMiRnaRgdId()<=0 ) {
                getSession().incrementCounter("LOAD_SKIPPED", 1);
            }
            else if( target.getKey()==0 ) {
                targetsToInsert.add(target);
                getSession().incrementCounter("LOAD_INSERTED", 1);
            } else {
                targetsMatching.add(target.getKey());
                getSession().incrementCounter("LOAD_MATCHING", 1);
            }
        }

        getDao().insertMiRna(targetsToInsert);
        getDao().updateMiRnaModifiedDate(targetsMatching);
    }
}
