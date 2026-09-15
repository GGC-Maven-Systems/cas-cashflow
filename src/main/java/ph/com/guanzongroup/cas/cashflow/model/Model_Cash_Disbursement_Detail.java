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
import ph.com.guanzongroup.cas.cashflow.status.CashDisbursementStatus;

/**
 *
 * @author Arsiela 03-24-2026
 */
public class Model_Cash_Disbursement_Detail extends Model {

    Model_Particular poParticular;
    Model_Cash_Advance_Detail poCashAdvanceDetail;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
//            poEntity.updateObject("nDetailNo", 0);
            poEntity.updateObject("nAmountxx", 0.0000);
            poEntity.updateObject("nDetVatSl", 0.0000);
            poEntity.updateObject("nDetVatRa", 0.0000);
            poEntity.updateObject("nDetVatAm", 0.0000);
            poEntity.updateObject("nDetZroVa", 0.0000);
            poEntity.updateObject("nDetVatEx", 0.0000);
            poEntity.updateObject("cReversex", CashDisbursementStatus.Reverse.INCLUDE);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

            //poParticular, poCashAdvanceDetail are intentionally NOT constructed here - see
            //their accessor methods below, which build them lazily on first access so
            //opening this record never touches those tables.

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNumber){
        return setValue("nEntryNox", entryNumber);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }
    
//    public JSONObject setDetailNo(int detailNo){
//        return setValue("nDetailNo", detailNo);
//    }
//
//    public int getDetailNo() {
//        return (int) getValue("nDetailNo");
//    }
    
    public JSONObject setReferNo(String referNo) {
        return setValue("sReferNox", referNo);
    }

    public String getReferNo() {
        return (String) getValue("sReferNox");
    }

    public JSONObject setParticularId(String accountCode) {
        return setValue("sPrtclrID", accountCode);
    }

    public String getParticularId() {
        return (String) getValue("sPrtclrID");
    }

    public JSONObject setAmount(Double amount) {
        return setValue("nAmountxx", amount);
    }

    public Double getAmount() {
        if (getValue("nAmountxx") == null || "".equals(getValue("nAmountxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nAmountxx").toString());
    }

    public JSONObject setDetailVatSales(Double detailVatSales) {
        return setValue("nDetVatSl", detailVatSales);
    }

    public Double getDetailVatSales() {
        if (getValue("nDetVatSl") == null || "".equals(getValue("nDetVatSl"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDetVatSl").toString());
    }

    public JSONObject setDetailVatRate(Double detailVatRate) {
        return setValue("nDetVatRa", detailVatRate);
    }

    public Double getDetailVatRate() {
        if (getValue("nDetVatRa") == null || "".equals(getValue("nDetVatRa"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDetVatRa").toString());
    }

    public JSONObject setDetailVatAmount(Double detailVatAmount) {
        return setValue("nDetVatAm", detailVatAmount);
    }

    public Double getDetailVatAmount() {
        if (getValue("nDetVatAm") == null || "".equals(getValue("nDetVatAm"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDetVatAm").toString());
    }

    public JSONObject setDetailZeroVat(Double detailZeroVat) {
        return setValue("nDetZroVa", detailZeroVat);
    }

    public Double getDetailZeroVat() {
        if (getValue("nDetZroVa") == null || "".equals(getValue("nDetZroVa"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDetZroVa").toString());
    }

    public JSONObject setDetailVatExempt(Double detailVatExempt) {
        return setValue("nDetVatEx", detailVatExempt);
    }

    public Double getDetailVatExempt() {
        if (getValue("nDetVatEx") == null || "".equals(getValue("nDetVatEx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDetVatEx").toString());
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

//    public JSONObject setModifyingBy(String modified) {
//        return setValue("sModified", modified);
//    }
//
//    public String getModifyingBy() {
//        return (String) getValue("sModified");
//    }

    public JSONObject setModifiedDate(Date modified) {
        return setValue("dModified", modified);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    //reference object models

    public Model_Particular Particular() throws SQLException, GuanzonException {
        if (poParticular == null) {
            poParticular = new CashflowModels(poGRider).Particular();
        }
        if (!"".equals((String) getValue("sPrtclrID"))) {
            if (poParticular.getEditMode() == EditMode.READY
                    && poParticular.getParticularID().equals((String) getValue("sPrtclrID"))) {
                return poParticular;
            } else {
                poJSON = poParticular.openRecord((String) getValue("sPrtclrID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poParticular;
                } else {
                    poParticular.initialize();
                    return poParticular;
                }
            }
        } else {
            poParticular.initialize();
            return poParticular;
        }
    }
    
    public Model_Cash_Advance_Detail CashAdvanceDetail(String fsSourceNo) throws SQLException, GuanzonException{
        if (poCashAdvanceDetail == null) {
            poCashAdvanceDetail = new CashflowModels(poGRider).CashAdvanceDetail();
        }
        if(fsSourceNo != null && !"".equals(fsSourceNo) && (int) getValue("nEntryNox") > 0){
            if (poCashAdvanceDetail.getEditMode() == EditMode.READY
                    && poCashAdvanceDetail.getTransactionNo().equals(fsSourceNo)
                    && poCashAdvanceDetail.getEntryNo() == (int) getValue("nEntryNox")) {
                return poCashAdvanceDetail;
            } else {
                poJSON = poCashAdvanceDetail.openRecord(fsSourceNo,(int) getValue("nEntryNox"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCashAdvanceDetail;
                } else {
                    poCashAdvanceDetail.initialize();
                    return poCashAdvanceDetail;
                }
            }
        } else {
            poCashAdvanceDetail.initialize();
            return poCashAdvanceDetail;
        }
    }
}
