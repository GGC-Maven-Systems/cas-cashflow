/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckStatus;

/**
 *
 * @author User
 */
public class Model_Check_Printing_Request_Detail extends Model {
    Model_Disbursement_Master poDVMaster;
    Model_Disbursement_Detail poDVDetail;
    Model_Check_Payments poCheckPayments;
    String lsremarks = "";

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

//            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
//            poEntity.updateObject("dCheckDte", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
//            poEntity.updateObject("nAmountxx", DisbursementStatic.DefaultValues.default_value_double_0000);
              poEntity.updateObject("cTranStat", CheckStatus.OPEN);
//            poEntity.updateObject("sBranchCd", poGRider.getBranchCode());

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            ID = "sTransNox";
            ID2 = "nEntryNox";
            ID3 = "sSourceNo";

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

    public JSONObject setEntryNumber(int entryNumber){
        return setValue("nEntryNox", entryNumber);
    }

    public int getEntryNumber() {
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setBankReference(String bankReference) {
        return setValue("sBankRefr", bankReference);
    }

    public String getBankReference() {
        return (String) getValue("sBankRefr");
    }
    
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }
    
    public String setdetailRemarks(String remarks) {
        return this.lsremarks = remarks;
    }

    public String getdetailRemarks() throws SQLException, GuanzonException {
        if(lsremarks.isEmpty()){
                lsremarks = CheckPayments().getRemarks();
        }
        return this.lsremarks;
    }


    @Override
    public String getNextCode(){
        return ""; 
    }

    public Model_Disbursement_Master DisbursementMaster() throws SQLException, GuanzonException {
        if (poDVMaster == null) {
            poDVMaster = new CashflowModels(poGRider).DisbursementMaster();
        }
        poJSON = poCheckPayments.openRecord((String) getValue("sSourceNo"));
        String Transaction = poCheckPayments.getSourceNo();
        
        
        
        if (!"".equals(Transaction)) {
            if (poDVMaster.getEditMode() == EditMode.READY
                    && poDVMaster.getTransactionNo().equals(Transaction)) {
                return poDVMaster;
            } else {
                poJSON = poDVMaster.openRecord(Transaction);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poDVMaster;
                } else {
                    poDVMaster.initialize();
                    return poDVMaster;
                }
            }
        } else {
            poDVMaster.initialize();
            return poDVMaster;
        }
    }
    
    public Model_Disbursement_Detail DisbursementDetail() throws SQLException, GuanzonException {
        if (poDVDetail == null) {
            poDVDetail = new CashflowModels(poGRider).DisbursementDetail();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poDVDetail.getEditMode() == EditMode.READY
                    && poDVDetail.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poDVDetail;
            } else {
                poJSON = poDVDetail.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poDVDetail;
                } else {
                    poDVDetail.initialize();
                    return poDVDetail;
                }
            }
        } else {
            poDVDetail.initialize();
            return poDVDetail;
        }
    }
    
    public Model_Check_Payments CheckPayments() throws SQLException, GuanzonException {
        if (poCheckPayments == null) {
            poCheckPayments = new CashflowModels(poGRider).CheckPayments();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poCheckPayments.getEditMode() == EditMode.READY
                    && poCheckPayments.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poCheckPayments;
            } else {
                poJSON = poCheckPayments.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCheckPayments;
                } else {
                    poCheckPayments.initialize();
                    return poCheckPayments;
                }
            }
        } else {
            poCheckPayments.initialize();
            return poCheckPayments;
        }
    }
 
}
