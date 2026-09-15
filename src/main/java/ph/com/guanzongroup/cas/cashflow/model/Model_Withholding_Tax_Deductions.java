/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.util.Date;
import java.sql.SQLException;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 *
 * @author Arsiela 06/05/2025
 */
public class Model_Withholding_Tax_Deductions extends Model {

    Model_Disbursement_Master poDisbursement_Master;
    Model_Withholding_Tax poWithholdingTax;
    
    String psTaxCode = "";

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("dPeriodFr");
            poEntity.updateNull("dPeriodto");
            poEntity.updateNull("dRemitted");
            poEntity.updateNull("dModified");
            poEntity.updateObject("nBaseAmtx", 0.0000);
            poEntity.updateObject("nTaxAmtxx", 0.0000);
            poEntity.updateObject("cReversex", "+");
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setTaxRateId(String taxRate) {
        return setValue("sTaxRteID", taxRate);
    }

    public String getTaxRateId() {
        return (String) getValue("sTaxRteID");
    }

    public JSONObject setBaseAmount(Double baseAmount) {
        return setValue("nBaseAmtx", baseAmount);
    }

    public Double getBaseAmount() {
        if (getValue("nBaseAmtx") == null || "".equals(getValue("nBaseAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nBaseAmtx").toString());
    }

    public JSONObject setTaxAmount(Double taxAmount) {
        return setValue("nTaxAmtxx", taxAmount);
    }

    public Double getTaxAmount() {
        if (getValue("nTaxAmtxx") == null || "".equals(getValue("nTaxAmtxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nTaxAmtxx").toString());
    }

    public JSONObject setBIRForm(String BIRForm) {
        return setValue("sBirFormx", BIRForm);
    }

    public String getBIRForm() {
        return (String) getValue("sBirFormx");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCD", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCD");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setPeriodFrom(Date periodFrom) {
        return setValue("dPeriodFr", periodFrom);
    }

    public Date getPeriodFrom() {
        return (Date) getValue("dPeriodFr");
    }

    public JSONObject setPeriodTo(Date periodTo) {
        return setValue("dPeriodto", periodTo);
    }

    public Date getPeriodTo() {
        return (Date) getValue("dPeriodto");
    }

    public JSONObject setRemittedDate(Date remittedDate) {
        return setValue("dRemitted", remittedDate);
    }

    public Date getRemittedDate() {
        return (Date) getValue("dRemitted");
    }

    public JSONObject isRemitted(boolean isRemitted) {
        return setValue("cRemitted", isRemitted ? "1" : "0");
    }

    public boolean isRemitted() {
        return ((String) getValue("cRemitted")).equals("1");
    }

    public JSONObject setReferenceNo(String referenceNo) {
        return setValue("sReferNox", referenceNo);
    }

    public String getReferenceNo() {
        return (String) getValue("sReferNox");
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    public JSONObject setModifyingBy(String modified) {
        return setValue("sModified", modified);
    }

    public String getModifyingBy() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modified) {
        return setValue("dModified", modified);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    public void setTaxCode(String taxCode) {
       psTaxCode = taxCode ;
    }

    public String getTaxCode() {
        return psTaxCode;
    }

    @Override
    public String getNextCode() {
//        return "";
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    //reference object models
    public Model_Withholding_Tax WithholdingTax() throws SQLException, GuanzonException {
        if (poWithholdingTax == null) {
            poWithholdingTax = new CashflowModels(poGRider).Withholding_Tax();
        }
        if (!"".equals((String) getValue("sTaxRteID"))) {
            if (poWithholdingTax.getEditMode() == EditMode.READY
                    && poWithholdingTax.getTaxRateId().equals((String) getValue("sTaxRteID"))) {
                setTaxCode(poWithholdingTax.getTaxCode());
                return poWithholdingTax;
            } else {
                poJSON = poWithholdingTax.openRecord((String) getValue("sTaxRteID"));
                setTaxCode(poWithholdingTax.getTaxCode());
                if ("success".equals((String) poJSON.get("result"))) {
                    return poWithholdingTax;
                } else {
                    poWithholdingTax.initialize();
                    return poWithholdingTax;
                }
            }
        } else {
            poWithholdingTax.initialize();
            return poWithholdingTax;
        }
    }
    
    public Model_Disbursement_Master Disbursement() throws SQLException, GuanzonException {
        if (poDisbursement_Master == null) {
            poDisbursement_Master = new CashflowModels(poGRider).DisbursementMaster();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poDisbursement_Master.getEditMode() == EditMode.READY
                    && poDisbursement_Master.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poDisbursement_Master;
            } else {
                poJSON = poDisbursement_Master.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poDisbursement_Master;
                } else {
                    poDisbursement_Master.initialize();
                    return poDisbursement_Master;
                }
            }
        } else {
            poDisbursement_Master.initialize();
            return poDisbursement_Master;
        }
    }
}
