package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.MiRnaTargetDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Map;

/**
 * @author mtutaj
 * @since 9/3/13
 * wrapper to handle all miRnaDAO code
 */
public class miRnaDAO {

    Logger logInserted = Logger.getLogger("insertedIds");
    Logger logDeleted = Logger.getLogger("deletedIds");

    MiRnaTargetDAO mdao = new MiRnaTargetDAO();
    XdbIdDAO xdao = new XdbIdDAO();

    public miRnaDAO() {
        System.out.println(xdao.getConnectionInfo());
    }

    static public void clearCaches() {
        _cacheGenes.clear();
        _cacheGeneSymbols.clear();
    }

    static public void initCaches(int speciesTypeKey) throws Exception {

        Logger logCore = Logger.getLogger("core");
        logCore.info("  initializing symbol-to-gene cache ...");
        List<Gene> genes = new GeneDAO().getActiveGenes(speciesTypeKey);
        for (Gene gene : genes) {
            Gene multiGene = _cacheGeneSymbols.put(gene.getSymbol(), gene);
            if (multiGene != null) {
                _multis.add("  Symbol " + gene.getSymbol() + " matches multiple genes! ");
                _cacheGeneSymbols.remove(gene.getSymbol());
            }
        }
        logCore.info("  loaded symbol-to-gene cache for " + SpeciesType.getCommonName(speciesTypeKey) + ": " + _cacheGeneSymbols.size());
    }

    public int getGeneRgdIdBySymbol(String geneSymbol) throws Exception {

        Gene gene = _cacheGeneSymbols.get(geneSymbol);
        return gene == null ? 0 : gene.getRgdId();
    }

    static final Map<String, Gene> _cacheGeneSymbols = new HashMap<>();

    static Set<String> _multis = new HashSet<>();

    public int printMultis() {
        Logger log = Logger.getLogger("multis");
        for (String msg : _multis) {
            log.info(msg);
        }

        int cnt = _multis.size();
        _multis.clear();
        return cnt;
    }

    public int getGeneRgdIdByEnsemblGeneId(String ensemblGeneId, int speciesTypeKey) throws Exception {
        return getGeneRgdIdByXdbId(ensemblGeneId, XdbId.XDB_KEY_ENSEMBL_GENES, speciesTypeKey);
    }

    public int getGeneRgdIdByMirBaseId(String mirBaseId, int speciesTypeKey) throws Exception {
        return getGeneRgdIdByXdbId(mirBaseId, XdbId.XDB_KEY_MIRBASE, speciesTypeKey);
    }

    int getGeneRgdIdByXdbId(String accId, int xdbKey, int speciesTypeKey) throws Exception {

        String cacheKey = xdbKey + "|" + accId + "|" + speciesTypeKey;
        List<Gene> genes;
        synchronized (_cacheGenes) {
            genes = _cacheGenes.get(cacheKey);
            if (genes == null) { // not in cache
                genes = xdao.getGenesByXdbId(xdbKey, accId, speciesTypeKey);
                _cacheGenes.put(cacheKey, genes);
            }
        }

        if (genes.isEmpty())
            return 0;
        if (genes.size() > 1) {
            System.out.println(accId + " matches multiple genes: " + Utils.concatenate(" ", genes, "getSymbol"));
            return -1; // Accession matches multiple genes!
        }
        return genes.get(0).getRgdId();
    }

    static final Map<String, List<Gene>> _cacheGenes = new HashMap<>();

    public List<MiRnaTarget> getMiRnaTargets(int miRnaRgdId) throws Exception {
        return mdao.getTargets(miRnaRgdId);
    }

    public List<MiRnaTarget> getMiRnaTargets(int miRnaRgdId, String targetType) throws Exception {
        return mdao.getTargets(miRnaRgdId, targetType);
    }

    public List<MiRnaTarget> getMiRnaGenes(int miRnaRgdId, String targetType) throws Exception {
        return mdao.getMiRnaGenes(miRnaRgdId, targetType);
    }

    public void updateMiRnaModifiedDate(List<Integer> keys) throws Exception {
        if (!keys.isEmpty())
            mdao.updateModifiedDate(keys);
    }

    public void insertMiRna(List<MiRnaTarget> miRnas) throws Exception {
        if (!miRnas.isEmpty()) {
            for (MiRnaTarget t : miRnas) {
                logInserted.info(t.getMiRnaSymbol() + "RGD:" + t.getMiRnaRgdId() + ", GENE_RGD_ID:" + t.getGeneRgdId()
                        + " " + t.getTargetType() + " " + t.getMethodName() + " " + t.getTranscriptAcc());
            }
            mdao.insert(miRnas);
        }
    }

    public List<MiRnaTarget> getMiRnaDataModifiedBefore(Date cutOffDate, int speciesTypeKey) throws Exception {
        List<MiRnaTarget> result = mdao.getDataModifiedBefore(speciesTypeKey, cutOffDate);
        for (MiRnaTarget t : result) {
            logDeleted.info(t.getMiRnaSymbol() + "RGD:" + t.getMiRnaRgdId() + ", GENE_RGD_ID:" + t.getGeneRgdId()
                    + " " + t.getTargetType() + " " + t.getMethodName() + " " + t.getTranscriptAcc());
        }
        return result;
    }

    public int deleteMiRnaDataModifiedBefore(Date cutOffDate, int speciesTypeKey) throws Exception {
        return mdao.deleteDataModifiedBefore(speciesTypeKey, cutOffDate);
    }

    public int getCountOfMiRnaData(int speciesTypeKey) throws Exception {
        return mdao.getCountOfData(speciesTypeKey);
    }

    public List<Gene> getTargetGenes(int speciesTypeKey) throws Exception {
        List<Gene> genes = mdao.getTargetGenes("predicted", speciesTypeKey);
        // stat loader: remove all miRNA genes from target genes
        //  currently we don't show on gene report pages mirna-to-mirna summaries
        Iterator<Gene> it = genes.iterator();
        while( it.hasNext() ) {
            Gene gene = it.next();
            boolean isMirnaGene = Utils.stringsAreEqualIgnoreCase(gene.getType(), "ncrna") &&
                Utils.defaultString(gene.getName()).startsWith("microRNA");
            if( isMirnaGene ) {
                it.remove();
            }
        }
        Collections.shuffle(genes);
        return genes;
    }

    public List<Gene> getMiRnaGenes(int speciesTypeKey) throws Exception {
        List<Gene> genes = mdao.getMiRnaGenes("predicted", speciesTypeKey);
        Collections.shuffle(genes);
        return genes;
    }

    public List<MiRnaTargetStat> getStats(int rgdId) throws Exception {
        return mdao.getStats(rgdId);
    }

    public int updateStatsModifiedDate(Collection<MiRnaTargetStat> stats) throws Exception {
        if( stats.isEmpty() ) {
            return 0;
        }

        List<Integer> keys = new ArrayList<>(stats.size());
        for( MiRnaTargetStat stat: stats ) {
            keys.add(stat.getKey());
        }
        mdao.updateStatsModifiedDate(keys);
        return keys.size();
    }

    public int deleteStats(Collection<MiRnaTargetStat> stats) throws Exception {
        if( stats.isEmpty() ) {
            return 0;
        }

        List<Integer> keys = new ArrayList<>(stats.size());
        for( MiRnaTargetStat stat: stats ) {
            keys.add(stat.getKey());
        }
        mdao.deleteStats(keys);
        return keys.size();
    }

    public int insertStats(Collection<MiRnaTargetStat> stats) throws Exception {
        if( stats.isEmpty() ) {
            return 0;
        }

        mdao.insertStats(stats);
        return stats.size();
    }

    public int updateStats(Collection<MiRnaTargetStat> stats) throws Exception {
        if( stats.isEmpty() ) {
            return 0;
        }

        mdao.updateStats(stats);
        return stats.size();
    }

    public List<MiRnaTargetStat> getStatsModifiedBefore(int speciesTypeKey, Date cutOffDate) throws Exception {
        return mdao.getStatsModifiedBefore(speciesTypeKey, cutOffDate);
    }

    public int deleteStatsModifiedBefore(int speciesTypeKey, Date cutOffDate) throws Exception {
        return mdao.deleteStatsModifiedBefore(speciesTypeKey, cutOffDate);
    }
}