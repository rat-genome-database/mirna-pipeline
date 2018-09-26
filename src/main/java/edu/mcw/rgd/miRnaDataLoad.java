package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/4/15
 * Time: 12:14 PM
 */
public class miRnaDataLoad extends RecordProcessor {

    Log log = LogFactory.getLog("core");

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
            e.printStackTrace();
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