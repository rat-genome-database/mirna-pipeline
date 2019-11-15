package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MiRnaTarget;
import edu.mcw.rgd.xml.XomAnalyzer;
import nu.xom.Element;

/**
 * @author mtutaj
 * @since 5/1/15
 * parser for confirmed miRna targets
 */
public class miRnaConfirmedTargetParser extends XomAnalyzer {

    private miRnaRecord rec;

    public void setRecord(miRnaRecord rec) {
        this.rec = rec;
    }

    public void initRecord(String name) {
    }

    public Element parseRecord(Element element) {

        miRnaEntry entry = new miRnaEntry();
        entry.setId(Integer.parseInt(element.getAttributeValue("id")));

        // legacy code
        entry.setSymbolHgnc(element.getAttributeValue("HGNC"));
        entry.setSymbolEnsembl(element.getAttributeValue("EnsEMBL"));

        // new code
        if( entry.getSymbolHgnc()==null ) {
            entry.setSymbolHgnc(element.getFirstChildElement("HGNC").getValue());
        }
        if( entry.getSymbolEnsembl()==null ) {
            entry.setSymbolEnsembl(element.getFirstChildElement("EnsEMBL").getValue());
        }

        MiRnaTarget miRnaTarget = entry.getMiRnaTarget();
        miRnaTarget.setTargetType("confirmed");
        miRnaTarget.setMiRnaSymbol(element.getFirstChildElement("miRNA").getValue());
        miRnaTarget.setMethodName(element.getFirstChildElement("method_name").getValue());
        miRnaTarget.setResultType(element.getFirstChildElement("result_type").getValue());
        miRnaTarget.setDataType(element.getFirstChildElement("data_type").getValue());
        miRnaTarget.setSupportType(element.getFirstChildElement("support_type").getValue());
        miRnaTarget.setPmid(element.getFirstChildElement("pmid").getValue());

        rec.addUniqueEntry(entry);

        return null;
    }
}
