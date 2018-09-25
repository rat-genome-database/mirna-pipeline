package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.xml.XomAnalyzer;
import nu.xom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/4/15
 * Time: 12:13 PM
 * <p>
 * parser for predicted miRna targets
 */
public class miRnaPredictedTargetParser extends XomAnalyzer {

    private miRnaRecord rec;

    public void setRecord(miRnaRecord rec) {
        this.rec = rec;
    }

    public void initRecord(String name) {
    }

    public Element parseRecord(Element element) {

        miRnaEntry entry = new miRnaEntry();

        String id = element.getAttributeValue("id");
        if( !id.isEmpty() ) {
            entry.setId(Integer.parseInt(id));
        }
        entry.setSymbolHgnc(element.getAttributeValue("HGNC"));
        entry.setSymbolEnsembl(element.getAttributeValue("EnsEMBL"));

        MiRnaTarget miRnaTarget = entry.getMiRnaTarget();
        miRnaTarget.setTargetType("predicted");
        miRnaTarget.setMiRnaSymbol(element.getFirstChildElement("miRNA").getValue());
        miRnaTarget.setMethodName(element.getFirstChildElement("method").getValue());
        miRnaTarget.setResultType(element.getFirstChildElement("result_type").getValue());

        miRnaTarget.setTranscriptAcc(element.getFirstChildElement("utr").getValue());
        miRnaTarget.setTranscriptBioType(element.getFirstChildElement("transcript_biotype").getValue());
        miRnaTarget.setIsoform(element.getFirstChildElement("isoform").getValue());
        miRnaTarget.setAmplification(element.getFirstChildElement("amplification").getValue());
        miRnaTarget.setTargetSite(element.getFirstChildElement("target_site").getValue());
        miRnaTarget.setUtrStart(Integer.parseInt(element.getFirstChildElement("utr_start").getValue()));
        miRnaTarget.setUtrEnd(Integer.parseInt(element.getFirstChildElement("utr_end").getValue()));

        String score = element.getFirstChildElement("score").getValue();
        if( !score.isEmpty() ) {
            miRnaTarget.setScore(Double.parseDouble(score));
        }
        // note: java allows for existence of both positive and negative zero
        //      but Oracle has only one zero, so we we must convert negative zero to positive zero
        if( miRnaTarget.getScore()!=null && miRnaTarget.getScore()==-0.0 ) {
            miRnaTarget.setScore(+0.0);
        }

        String normalizedScore = element.getFirstChildElement("normalized_score").getValue();
        if( !normalizedScore.isEmpty() ) {
            miRnaTarget.setNormalizedScore(Double.parseDouble(normalizedScore));
        }

        // load optional 'energy' element
        Element energy = element.getFirstChildElement("energy");
        if( energy!=null ) {
            miRnaTarget.setEnergy(Double.parseDouble(energy.getValue()));
        }

        rec.addUniqueEntry(entry);
        return null;
    }
}
