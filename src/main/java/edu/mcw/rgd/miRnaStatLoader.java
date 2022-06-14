package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.datamodel.MiRnaTargetStat;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by mtutaj on 8/12/2016.
 */
public class miRnaStatLoader {

    private miRnaDAO dao = new miRnaDAO();
    private String version;
    private List<String> speciesProcessed;

    Logger log = LogManager.getLogger("stats");

    public void run() throws Exception {

        Date cutOffDate = new Date();

        for( String species: speciesProcessed ) {
            computeStatsForMiRnaGenes(species);
            computeStatsForGeneTargets(species);
            handleStaleStats(species, cutOffDate);
        }
    }

    void computeStatsForMiRnaGenes(String species) throws Exception {
        log.info("computing stats for "+species+" miRna genes");

        int statsUpdated = 0;
        int statsInserted = 0;
        int statsDeleted = 0;
        int statsMatched = 0;

        log.debug("retrieving list of miRna genes for "+species);
        List<Gene> miRnaGenes = dao.getMiRnaGenes(SpeciesType.parse(species));
        log.debug("  retrieved "+miRnaGenes.size()+ " miRna genes for "+species);

        int i=0;
        for( Gene miRnaGene: miRnaGenes ) {

            String nr = "  "+(++i)+"/"+miRnaGenes.size()+".";
            log.debug(nr+" processing "+miRnaGene.getSymbol()+" RGD:"+miRnaGene.getRgdId());

            List<MiRnaTargetStat> statsIncoming = getIncomingStatsForMiRnaGene(miRnaGene);
            List<MiRnaTargetStat> statsInRgd = dao.getStats(miRnaGene.getRgdId());

            Collection<MiRnaTargetStat> statsMatching = CollectionUtils.intersection(statsInRgd, statsIncoming);
            Collection<MiRnaTargetStat> statsForInsert = CollectionUtils.subtract(statsIncoming, statsInRgd);
            Collection<MiRnaTargetStat> statsForDelete = CollectionUtils.subtract(statsInRgd, statsIncoming);
            Collection<MiRnaTargetStat> statsForUpdate = getStatsForUpdate(statsForInsert, statsInRgd, statsForDelete);

            statsMatched += dao.updateStatsModifiedDate(statsMatching);
            statsInserted += dao.insertStats(statsForInsert);
            statsDeleted += dao.deleteStats(statsForDelete);
            statsUpdated += dao.updateStats(statsForUpdate);

            log.debug("   load state: matched="+statsMatched+" updated="+statsUpdated+" inserted="+statsInserted+" deleted="+statsDeleted);
        }

        log.info("computed stats for "+miRnaGenes.size()+" "+species+" miRna genes!");

        log.info("  MIRNA_GENES_STATS_MATCHING: "+statsMatched);
        log.info("  MIRNA_GENES_STATS_INSERTED: "+statsInserted);
        log.info("  MIRNA_GENES_STATS_DELETED: "+statsDeleted);
        log.info("  MIRNA_GENES_STATS_UPDATED: "+statsUpdated);
    }

    void computeStatsForGeneTargets(String species) throws Exception {
        log.info("computing stats for "+species+" gene targets");

        int statsMatched = 0;
        int statsInserted = 0;
        int statsDeleted = 0;
        int statsUpdated = 0;

        log.debug("retrieving list of gene targets for "+species);
        List<Gene> geneTargets = dao.getTargetGenes(SpeciesType.parse(species));
        log.debug("  retrieved "+geneTargets.size()+ " genes targets for "+species);

        int i=0;
        for( Gene geneTarget: geneTargets ) {

            String nr = "  "+(++i)+"/"+geneTargets.size()+".";
            log.debug(nr+" processing "+geneTarget.getSymbol()+" RGD:"+geneTarget.getRgdId());

            List<MiRnaTargetStat> statsIncoming = getIncomingStatsForGeneTarget(geneTarget);
            List<MiRnaTargetStat> statsInRgd = dao.getStats(geneTarget.getRgdId());

            Collection<MiRnaTargetStat> statsMatching = CollectionUtils.intersection(statsInRgd, statsIncoming);
            Collection<MiRnaTargetStat> statsForInsert = CollectionUtils.subtract(statsIncoming, statsInRgd);
            Collection<MiRnaTargetStat> statsForDelete = CollectionUtils.subtract(statsInRgd, statsIncoming);
            Collection<MiRnaTargetStat> statsForUpdate = getStatsForUpdate(statsForInsert, statsInRgd, statsForDelete);

            statsMatched += dao.updateStatsModifiedDate(statsMatching);
            statsInserted += dao.insertStats(statsForInsert);
            statsDeleted += dao.deleteStats(statsForDelete);
            statsUpdated += dao.updateStats(statsForUpdate);

            log.debug("   load state: matched="+statsMatched+" updated="+statsUpdated+" inserted="+statsInserted+" deleted="+statsDeleted);
        }

        log.info("computed stats for "+geneTargets.size()+" "+species+" gene targets!");

        log.info("  GENE_TARGETS_STATS_MATCHING: "+statsMatched);
        log.info("  GENE_TARGETS_STATS_INSERTED: "+statsInserted);
        log.info("  GENE_TARGETS_STATS_DELETED: "+statsDeleted);
        log.info("  GENE_TARGETS_STATS_UPDATED: "+statsUpdated);
    }

    List<MiRnaTargetStat> getIncomingStatsForMiRnaGene(Gene miRnaGene) throws Exception {

        Set<Integer> geneRgdIds = new HashSet<>();
        Set<String> miRnas = new TreeSet<>();
        Set<String> methods = new TreeSet<>();
        Set<String> utrs = new TreeSet<>();
        Set<String> resultTypes = new TreeSet<>();

        // get predicted mirna targets
        List<MiRnaTarget> miRnaTargets = dao.getMiRnaTargets(miRnaGene.getRgdId(), "predicted");
        for( MiRnaTarget t: miRnaTargets ) {
            geneRgdIds.add(t.getGeneRgdId());
            miRnas.add(t.getMiRnaSymbol());
            methods.add(t.getMethodName());
            utrs.add(t.getTranscriptAcc());
            resultTypes.add(t.getResultType());
        }

        List<MiRnaTargetStat> incomingStats = new ArrayList<>();
        MiRnaTargetStat stat = new MiRnaTargetStat();
        stat.setName("Count of predictions");
        stat.setValue(Integer.toString(miRnaTargets.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Count of gene targets");
        stat.setValue(Integer.toString(geneRgdIds.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Count of transcripts");
        stat.setValue(Integer.toString(utrs.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Interacting mature miRNAs");
        stat.setValue(Utils.concatenate(miRnas, ", "));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Prediction methods");
        stat.setValue(Utils.concatenate(methods, ", "));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Result types");
        stat.setValue(Utils.concatenate(resultTypes, ", "));
        incomingStats.add(stat);

        for( MiRnaTargetStat istat: incomingStats ) {
            istat.setRgdId(miRnaGene.getRgdId());
        }
        return incomingStats;
    }

    List<MiRnaTargetStat> getIncomingStatsForGeneTarget(Gene geneTarget) throws Exception {

        Set<Integer> geneRgdIds = new HashSet<>();
        Set<String> miRnas = new TreeSet<>();
        Set<String> methods = new TreeSet<>();
        Set<String> utrs = new TreeSet<>();
        Set<String> resultTypes = new TreeSet<>();

        // get predicted mirna genes
        List<MiRnaTarget> miRnaGenes = dao.getMiRnaGenes(geneTarget.getRgdId(), "predicted");
        for( MiRnaTarget t: miRnaGenes ) {
            geneRgdIds.add(t.getMiRnaRgdId());
            miRnas.add(t.getMiRnaSymbol());
            methods.add(t.getMethodName());
            utrs.add(t.getTranscriptAcc());
            resultTypes.add(t.getResultType());
        }

        List<MiRnaTargetStat> incomingStats = new ArrayList<>();
        MiRnaTargetStat stat = new MiRnaTargetStat();
        stat.setName("Count of predictions");
        stat.setValue(Integer.toString(miRnaGenes.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Count of miRNA genes");
        stat.setValue(Integer.toString(geneRgdIds.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Interacting mature miRNAs");
        stat.setValue(Integer.toString(miRnas.size()));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Transcripts");
        stat.setValue(Utils.concatenate(utrs, ", "));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Prediction methods");
        stat.setValue(Utils.concatenate(methods, ", "));
        incomingStats.add(stat);

        stat = new MiRnaTargetStat();
        stat.setName("Result types");
        stat.setValue(Utils.concatenate(resultTypes, ", "));
        incomingStats.add(stat);

        for( MiRnaTargetStat istat: incomingStats ) {
            istat.setRgdId(geneTarget.getRgdId());
        }
        return incomingStats;
    }

    Collection<MiRnaTargetStat> getStatsForUpdate(Collection<MiRnaTargetStat> statsForInsert,
                                                  Collection<MiRnaTargetStat> statsInRgd,
                                                  Collection<MiRnaTargetStat> statsForDelete) {

        // convert
        List<MiRnaTargetStat2> statsForInsert2 = new ArrayList<>();
        for( MiRnaTargetStat stat: statsForInsert ) {
            statsForInsert2.add(new MiRnaTargetStat2(stat));
        }
        List<MiRnaTargetStat2> statsInRgd2 = new ArrayList<>();
        for( MiRnaTargetStat stat: statsInRgd ) {
            statsInRgd2.add(new MiRnaTargetStat2(stat));
        }

        Collection<MiRnaTargetStat> statsForUpdate = CollectionUtils.intersection(statsInRgd2, statsForInsert2);

        if( !statsForUpdate.isEmpty() ) {
            Iterator<MiRnaTargetStat> it = statsForInsert.iterator();
            while (it.hasNext()) {
                MiRnaTargetStat stat = it.next();

                // see if this stat is on for-update list
                for (MiRnaTargetStat ustat : statsForUpdate) {
                    if( ustat.getRgdId()==stat.getRgdId() &&
                            Utils.stringsAreEqual(ustat.getName(), stat.getName()) ) {

                        ustat.setValue(stat.getValue());
                        it.remove();
                        break;
                    }
                }
            }

            it = statsForDelete.iterator();
            while (it.hasNext()) {
                MiRnaTargetStat stat = it.next();

                // see if this stat is on for-update list
                for (MiRnaTargetStat ustat : statsForUpdate) {
                    if( ustat.getKey()==stat.getKey() ) {
                        it.remove();
                        break;
                    }
                }
            }
        }
        return statsForUpdate;
    }

    void handleStaleStats(String species, Date cutOffDate) throws Exception {
        log.info("computing stale stats for "+species);

        int speciesTypeKey = SpeciesType.parse(species);
        List<MiRnaTargetStat> staleStats = dao.getStatsModifiedBefore(speciesTypeKey, cutOffDate);
        log.info("  stale stats: "+staleStats.size());

        log.debug("  deleting stale stats ...");
        int staleStatsDeleted = dao.deleteStatsModifiedBefore(speciesTypeKey, cutOffDate);
        log.info("  stale stats deleted: "+staleStatsDeleted);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSpeciesProcessed(List<String> speciesProcessed) {
        this.speciesProcessed = speciesProcessed;
    }

    public List<String> getSpeciesProcessed() {
        return speciesProcessed;
    }

    class MiRnaTargetStat2 extends MiRnaTargetStat {

        public MiRnaTargetStat2(MiRnaTargetStat s) {
            setKey(s.getKey());
            setRgdId(s.getRgdId());
            setName(s.getName());
            setValue(s.getValue());
        }

        @Override
        public boolean equals(Object o) {
            MiRnaTargetStat s = (MiRnaTargetStat)o;

            return this.getRgdId() == s.getRgdId()
                    && Utils.stringsAreEqual(this.getName(), s.getName());
        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + getRgdId();
            return result;
        }

    }
}
