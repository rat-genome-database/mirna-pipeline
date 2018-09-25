package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/11/15
 * Time: 8:31 AM
 * <p>
 * incoming data: predicted/confirmed miRna target entry;
 * usually multiple per one MI
 */
public class miRnaEntry {

    private int id;
    private String symbolHgnc;   // ABHD11
    private String symbolEnsembl;// ENSG00000106077

    private MiRnaTarget miRnaTarget = new MiRnaTarget();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbolHgnc() {
        return symbolHgnc;
    }

    public void setSymbolHgnc(String symbolHgnc) {
        this.symbolHgnc = symbolHgnc;
    }

    public String getSymbolEnsembl() {
        return symbolEnsembl;
    }

    public void setSymbolEnsembl(String symbolEnsembl) {
        this.symbolEnsembl = symbolEnsembl;
    }

    public MiRnaTarget getMiRnaTarget() {
        return miRnaTarget;
    }

    public void setMiRnaTarget(MiRnaTarget miRnaTarget) {
        this.miRnaTarget = miRnaTarget;
    }
}
