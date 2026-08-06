/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Parameter;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Adjustment;
import ph.com.guanzongroup.cas.cashflow.status.APPaymentAdjustmentStatus;
import ph.com.guanzongroup.cas.cashflow.validator.APPaymentAdjustmentValidator;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cache_Payable_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatus;

/**
 *
 * @author Arsiela 06052025
 */
public class APPaymentAdjustment extends Parameter {

    private String psIndustryId = "";
    private String psCompanyId = "";
    private boolean pbWithParent = false;
    public String psSource_Code = "";

    Model_AP_Payment_Adjustment poModel;
    List<Model_AP_Payment_Adjustment> paModel;
    
    private CachePayable poCachePayable;

    @Override
    public void initialize() {
        psSource_Code = "APAd";
//        SOURCE_CODE = "APAd"; //Conflict in parameter baseclass
        psRecdStat = Logical.YES;
        pbInitRec = true;

        poModel = new CashflowModels(poGRider).APPaymentAdjustment();

        paModel = new ArrayList<>();
    }

    public JSONObject NewTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        return newRecord();
    }

    public JSONObject SaveTransaction()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();
        poJSON = isEntryOkay(poModel.getTransactionStatus());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if(getModel().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + getModel().getNextCode());
            getModel().setTransactionNo(getModel().getNextCode());
        }
        
        getModel().setModifyingBy(poGRider.getUserID());
        getModel().setModifiedDate(poGRider.getServerDate());
        
        System.out.println("Modified By : " + getModel().getModifyingBy());
        System.out.println("Modified Date : " + getModel().getModifiedDate());
        
        if(getModel().getTransactionStatus().equals(APPaymentAdjustmentStatus.CONFIRMED)) {
            if(!pbWithParent){
                poJSON = callApproval();
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } 
            }
        }
        
        //Populate cache payables
        if(getModel().getTransactionStatus().equals(APPaymentAdjustmentStatus.CONFIRMED) 
                || getModel().getTransactionStatus().equals(APPaymentAdjustmentStatus.CANCELLED) ){
            poJSON = populateCachePayable(true, getModel().getTransactionStatus());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            if(getModel().getTransactionStatus().equals(APPaymentAdjustmentStatus.CANCELLED)){
                poJSON = checkLinkedPayment();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }
        
        return saveRecord();
    }

    public void isWithParent(boolean isWithParent){
        pbWithParent = isWithParent;
    }
    
    public JSONObject OpenTransaction(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        return openRecord(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateRecord();
    }
    
    public JSONObject callApproval(){
        poJSON = new JSONObject();
        if (pbWithUI) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer.");
                    return poJSON;
                }
            }
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
//        setApproving((String) poJSON.get("sUserIDxx"));
        return poJSON;
    }
    
    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = APPaymentAdjustmentStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poModel.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if(!pbWithParent){
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }
        
        //Populate cache payables
        poJSON = populateCachePayable(true, lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, poModel.getTransactionNo());
        
        poCachePayable.setWithParent(true);
        poJSON = poCachePayable.SaveTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    private String getInvTypeCode(String fsValue){
        try {
            String lsSQL = "SELECT sInvTypCd, sDescript FROM Inv_Type ";
            lsSQL = MiscUtil.addCondition(lsSQL, " cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                                + " AND lower(sDescript) LIKE " + SQLUtil.toSQL("%"+fsValue+"%"));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return  loRS.getString("sInvTypCd");
                    }
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
            
        return  "";
    }
    
    private JSONObject populateCachePayable(boolean isSave, String status) throws SQLException, GuanzonException, CloneNotSupportedException{
        poJSON = new JSONObject();
        poCachePayable = new CashflowControllers(poGRider, logwrapr).CachePayable();
        poCachePayable.InitTransaction();
        String lsTranType = getInvTypeCode("adjustment");
        if("".equals(lsTranType) || lsTranType == null){
            poJSON.put("result", "error");
            poJSON.put("message", "Adjustment transaction type not found in database.\nContact System Administrator to address the issue.");
            return poJSON;
        }
        
        if(isSave){
            String lsCachePayable = getCachePayable();
            //Update cache payables
            if(lsCachePayable.isEmpty()){
                if(status.equals(APPaymentAdjustmentStatus.CONFIRMED)){
                    poJSON = poCachePayable.NewTransaction();
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                }
            } else {
                poJSON = poCachePayable.OpenTransaction(lsCachePayable);
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }

                poJSON = poCachePayable.UpdateTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }

        } else {
            if(status.equals(APPaymentAdjustmentStatus.CONFIRMED)){
                poJSON = poCachePayable.NewTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        
        if(poCachePayable.getEditMode() == EditMode.ADDNEW || poCachePayable.getEditMode() == EditMode.UPDATE){
            //Cache Payable Master
            poCachePayable.Master().setIndustryCode(getModel().getIndustryId());
            poCachePayable.Master().setBranchCode(getModel().getBranchCode());
            poCachePayable.Master().setTransactionDate(poGRider.getServerDate()); 
            poCachePayable.Master().setCompanyId(getModel().getCompanyId());
            poCachePayable.Master().setClientId(getModel().getClientId());
            poCachePayable.Master().setSourceCode(psSource_Code); //TODO
            poCachePayable.Master().setSourceNo(getModel().getTransactionNo());
            poCachePayable.Master().setReferNo(getModel().getReferenceNo()); 
            poCachePayable.Master().setGrossAmount(getModel().getTransactionTotal().doubleValue()); 
            poCachePayable.Master().setNetTotal(getModel().getTransactionTotal().doubleValue()); 
            poCachePayable.Master().setVATExempt(getModel().getDebitAmount().doubleValue()); 
            poCachePayable.Master().setPayables(getModel().getDebitAmount().doubleValue()); 
            poCachePayable.Master().setReceivables(getModel().getCreditAmount().doubleValue()); 
            poCachePayable.Master().setTransactionStatus(CachePayableStatus.CONFIRMED);
            poCachePayable.Master().setModifyingId(poGRider.getUserID());
            poCachePayable.Master().setModifiedDate(poGRider.getServerDate());

            //Cache Payable Detail
            if(poCachePayable.getDetailCount() < 0){
                poCachePayable.AddDetail();
            }

            poCachePayable.Detail(poCachePayable.getDetailCount()-1).setTransactionType(lsTranType);
            poCachePayable.Detail(poCachePayable.getDetailCount()-1).setEntryNumber(1);
            poCachePayable.Detail(poCachePayable.getDetailCount()-1).setGrossAmount(getModel().getTransactionTotal().doubleValue());
            poCachePayable.Detail(poCachePayable.getDetailCount()-1).setPayables(getModel().getDebitAmount().doubleValue());
            poCachePayable.Detail(poCachePayable.getDetailCount()-1).setReceivables(getModel().getCreditAmount().doubleValue());
            
            
            if(status.equals(APPaymentAdjustmentStatus.CANCELLED)){
                poCachePayable.Master().setTransactionStatus(CachePayableStatus.CANCELLED);
            }
            
        }
        return poJSON;
    }
    
    private String getCachePayable() throws SQLException{
        String lsTransNo = "";
        poJSON = new JSONObject();
        String lsSQL = " SELECT "
                + "   sTransNox "
                + " , sSourceNo "
                + " , sSourceCd "
                + "   FROM Cache_Payable_Master ";
        lsSQL = MiscUtil.addCondition(lsSQL, " sSourceNo = " + SQLUtil.toSQL(getModel().getTransactionNo())
                                                + " AND sSourceCd = " + SQLUtil.toSQL(psSource_Code));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lsTransNo =loRS.getString("sTransNox");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            lsTransNo = "";
        }
        
        return lsTransNo;
    }

    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = APPaymentAdjustmentStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poModel.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(!pbWithParent){
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, poModel.getTransactionNo());

        //change status
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //TODO
        //Update Cache Payables?
        
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }

    public JSONObject PaidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = APPaymentAdjustmentStatus.PAID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poModel.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already paid.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(APPaymentAdjustmentStatus.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
//        poGRider.beginTrans("UPDATE STATUS", "PaidTransaction", SOURCE_CODE, poModel.getTransactionNo());

        //change status
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

//        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction paid successfully.");
        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = APPaymentAdjustmentStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poModel.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(APPaymentAdjustmentStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (APPaymentAdjustmentStatus.CONFIRMED.equals(poModel.getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }
        
        //Update Cache Payables
        poJSON = populateCachePayable(true, lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        String lsCachePayable = getCachePayable();
        //Update cache payables
        if(!lsCachePayable.isEmpty()){
            poJSON = poCachePayable.OpenTransaction(lsCachePayable);
            if ("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
        } 

        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, poModel.getTransactionNo());
        
        if(!lsCachePayable.isEmpty()){
            poCachePayable.setWithParent(true);
            poJSON = poCachePayable.CancelTransaction(remarks);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        //change status
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");
        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = APPaymentAdjustmentStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poModel.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(APPaymentAdjustmentStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (APPaymentAdjustmentStatus.CONFIRMED.equals(poModel.getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }

        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, poModel.getTransactionNo());

        //change status
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
        return poJSON;
    }

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }
    
    public void resetMaster() {
        poModel = new CashflowModels(poGRider).APPaymentAdjustment();
    }

    @Override
    public Model_AP_Payment_Adjustment getModel() {
        return poModel;
    }

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            poModel.setBranchCode(poGRider.getBranchCode());
            poModel.setIndustryId(psIndustryId);
            poModel.setCompanyId(psCompanyId);
            poModel.setTransactionDate(poGRider.getServerDate());
            poModel.setTransactionStatus(APPaymentAdjustmentStatus.OPEN);
            poModel.isProcessed(false);

        } catch (SQLException ex) {
            Logger.getLogger(APPaymentAdjustment.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject computeFields() {
        poJSON = new JSONObject();
        
        if(getModel().getDebitAmount().doubleValue() > 0.00){
            getModel().setTransactionTotal(getModel().getDebitAmount());
            getModel().setNetTotal(getModel().getDebitAmount());
        } else {
            getModel().setTransactionTotal(getModel().getCreditAmount());
            getModel().setNetTotal(getModel().getCreditAmount());
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            getModel().setCompanyId(object.getModel().getCompanyId());
        }
        return poJSON;
    }
    
    //Replaced by script below - Arsiela 05-05-2026 
//    public JSONObject SearchClient(String value, boolean byCode)
//            throws SQLException,
//            GuanzonException {
//        poJSON = new JSONObject();
//
//        Client object = new ClientControllers(poGRider, logwrapr).Client();
//        object.Master().setRecordStatus(RecordStatus.ACTIVE);
//        object.Master().setClientType("1");
//        poJSON = object.Master().searchRecord(value, byCode);
//        if ("success".equals((String) poJSON.get("result"))) {
//            getModel().setClientId(object.Master().getModel().getClientId());
////            getModel().setAddressId(object.ClientAddress().getModel().getAddressId()); //TODO
////            getModel().setContactId(object.ClientInstitutionContact().getModel().getClientId()); //TODO
//        }
//
//        return poJSON;
//    }

    public JSONObject SearchClient(String value, boolean byCode) throws SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        poJSON = object.searchRecordbyCompany(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(object.getModel().getAPClientID() == null || "".equals(object.getModel().getAPClientID())){
                getModel().setClientId(object.getModel().getClientID());
            } else {
                getModel().setClientId(object.getModel().getAPClientID());
            }
            getModel().setIssuedTo(object.getModel().getPayeeID());
        }

        return poJSON;
    }
    
    public JSONObject SearchPayee(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
//        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
//        object.setRecordStatus("1");
//
//        poJSON = object.searchRecord(value, byCode);
//        if ("success".equals((String) poJSON.get("result"))) {
//            getModel().setIssuedTo(object.getModel().getPayeeID());
//            getModel().setPayerCode(object.getModel().getRecordStatus());
//        }
        //Tester Suggestion : Arsiela 05-05-2026
        String lsClientID = getModel().getClientId();
        if(lsClientID == null || "".equals(lsClientID)){
            lsClientID = value;
        }
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus(RecordStatus.ACTIVE);
        System.out.println(" lsClientID : " + lsClientID);
        poJSON = object.searchRecordbyClientID(lsClientID, lsClientID != null && !"".equals(lsClientID));
        if ("success".equals((String) poJSON.get("result"))) {
            if ( (object.getModel().getClientID() == null || "".equals(object.getModel().getClientID()))
                && (object.getModel().getAPClientID() == null || "".equals(object.getModel().getAPClientID()))){
                poJSON.put("result", "error");
                poJSON.put("message", "AP Client / Client information for the selected payee is not configured.\nPlease contact the system administrator for assistance.");
                return poJSON;
            }
            
            getModel().setIssuedTo(object.getModel().getPayeeID());
            
            if(object.getModel().getAPClientID() == null || "".equals(object.getModel().getAPClientID())){
                getModel().setClientId(object.getModel().getClientID());
            } else {
                getModel().setClientId(object.getModel().getAPClientID());
            }
        }
        return poJSON;
    }

    public JSONObject searchTransaction()
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psRecdStat != null) {
            if (psRecdStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psRecdStat);
            }
        }
        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
                " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId));
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Company»Supplier»Payee",
                "dTransact»sTransNox»sCompnyNm»sSupplrNm»sPayeeNme",
                "a.dTransact»a.sTransNox»d.sCompnyNm»b.sCompnyNm»c.sPayeeNme",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject searchTransaction(String value, boolean byCode)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psRecdStat != null) {
            if (psRecdStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psRecdStat);
            }
        }
        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
                " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+ value));
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Company»Supplier»Payee",
                "dTransact»sTransNox»sCompnyNm»sSupplrNm»sPayeeNme",
                "a.dTransact»a.sTransNox»d.sCompnyNm»b.sCompnyNm»c.sPayeeNme",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject searchTransaction(String industryId, String companyName, String supplierName, String referenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if(industryId == null || "".equals(industryId)){
            industryId = psIndustryId;
        }
        
        if(supplierName == null){
            supplierName = "";
        }
        if(referenceNo == null){
            referenceNo = "";
        }
        
        if(companyName == null){
            companyName = "";
        }
        String lsTransStat = "";
        if (psRecdStat != null) {
            if (psRecdStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psRecdStat);
            }
        }
        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
                " a.sIndstCdx = " + SQLUtil.toSQL(industryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                + " AND d.sCompnyNm LIKE " + SQLUtil.toSQL("%" + companyName)
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplierName)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                ) ;
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Company»Supplier»Payee",
                "dTransact»sTransNox»sCompnyNm»sSupplrNm»sPayeeNme",
                "a.dTransact»a.sTransNox»d.sCompnyNm»b.sCompnyNm»c.sPayeeNme",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    private Model_AP_Payment_Adjustment APPaymentAdjustment() {
        return new CashflowModels(poGRider).APPaymentAdjustment();
    }

    public Model_AP_Payment_Adjustment APPaymentAdjustmentList(int row) {
        return (Model_AP_Payment_Adjustment) paModel.get(row);
    }

    public int getAPPaymentAdjustmentCount() {
        return this.paModel.size();
    }

    public JSONObject loadAPPaymentAdjustment(String company, String supplier, String referenceNo) {
        poJSON = new JSONObject();
        try {
            if (company == null) {
                company = "";
            }
            if (supplier == null) {
                supplier = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }

            String lsTransStat = "";
            if (psRecdStat != null) {
                if (psRecdStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                        lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
                    }
                    lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
                } else {
                    lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psRecdStat);
                }
            }

            String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
                    " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                    + " AND d.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                    + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                    + " AND a.cProcessd = '0' "
            );

            lsSQL = lsSQL + "" + lsTransStat + " ORDER BY a.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paModel = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sCompnyNm: " + loRS.getString("sCompnyNm"));
                    System.out.println("------------------------------------------------------------------------------");

                    paModel.add(APPaymentAdjustment());
                    paModel.get(paModel.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paModel = new ArrayList<>();
                paModel.add(APPaymentAdjustment());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(APPaymentAdjustment.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
//    @Override
//    public JSONObject willSave() throws SQLException, GuanzonException{
//        /*Put system validations and other assignments here*/
//        poJSON = new JSONObject();
////        try {
//
//
//            //Populate cache payables
////            poJSON = populateCachePayable(true, APPaymentAdjustmentStatus.CONFIRMED);
////            if (!"success".equals((String) poJSON.get("result"))) {
////                return poJSON;
////            }
//            
////        } catch (CloneNotSupportedException ex) {
////            Logger.getLogger(APPaymentAdjustment.class.getName()).log(Level.SEVERE, null, ex);
////        }
//        
//        return poJSON;
//    }
    
    @Override
    public JSONObject saveOthers() throws SQLException, GuanzonException{
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        try {
            if(poCachePayable != null){
                if(poCachePayable.getEditMode() == EditMode.ADDNEW || poCachePayable.getEditMode() == EditMode.UPDATE){
                    poCachePayable.setWithParent(true);
                    poCachePayable.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    poCachePayable.Master().setModifiedDate(poGRider.getServerDate());
                    poJSON = poCachePayable.SaveTransaction();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
            
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(APPaymentAdjustment.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject checkLinkedPayment(){
        poJSON = new JSONObject();
        try {
            //Cache payable
            String lsCachePayable = "";
            Model_Cache_Payable_Master loCachePayable = new CashflowModels(poGRider).Cache_Payable_Master();
            String lsSQL = MiscUtil.makeSelect(loCachePayable);
            lsSQL = MiscUtil.addCondition(lsSQL, " sSourceNo = " + SQLUtil.toSQL(getModel().getTransactionNo()) );
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) >= 0) {
                if (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------Cache Payable--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    lsCachePayable = loRS.getString("sTransNox");
                }
            } 
            
            MiscUtil.close(loRS);
            
            //SOA Tagging
            if(lsCachePayable != null && !"".equals(lsCachePayable)){
                lsSQL = MiscUtil.addCondition(getAPPaymentSQL(),
                        " b.sSourceNo = " + SQLUtil.toSQL(lsCachePayable)
                        + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.CANCELLED)
                        + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.VOID)
                );
                System.out.println("Executing SQL: " + lsSQL);
                loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------SOA Tagging--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            poJSON.put("result", "error");
                            poJSON.put("message", "AP Payment Adjustment already linked to SOA : " + loRS.getString("sTransNox"));
                            return poJSON;
                        }
                    }
                }
                MiscUtil.close(loRS);
            
                //Disbursement
                lsSQL = MiscUtil.addCondition(getDVPaymentSQL(),
                        " b.sSourceNo = " + SQLUtil.toSQL(lsCachePayable)
                        + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.CANCELLED)
                        + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.VOID)
                );
                System.out.println("Executing SQL: " + lsSQL);
                loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------DV--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            poJSON.put("result", "error");
                            poJSON.put("message", "AP Payment Adjustment already linked to DV : " + loRS.getString("sTransNox"));
                            return poJSON;
                        }
                    }
                }
                MiscUtil.close(loRS);
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }
    
    public JSONObject isEntryOkay(String status) throws SQLException {
        poJSON = new JSONObject();

        GValidator loValidator = new APPaymentAdjustmentValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poModel);
        poJSON = loValidator.validate();
        return poJSON;
    }

    @Override
    public JSONObject isEntryOkay() throws SQLException {
        poJSON = new JSONObject();
        poModel.setModifyingBy(poGRider.Encrypt(poGRider.getUserID()));
        poModel.setModifiedDate(poGRider.getServerDate());

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public String getSQ_Browse() {
        return " SELECT "
                + " a.dTransact "
                + " , a.sTransNox "
                + " , a.sIndstCdx "
                + " , a.sCompnyID "
                + " , b.sCompnyNm  AS sSupplrNm "
                + " , c.sPayeeNme  AS sPayeeNme "
                + " , d.sCompnyNm  AS sCompnyNm "
                + " , e.sDescript  AS sIndustry "
                + " FROM AP_Payment_Adjustment a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sClientID "
                + " LEFT JOIN Payee c ON c.sPayeeIDx = a.sIssuedTo "
                + " LEFT JOIN Company d ON d.sCompnyID = a.sCompnyID  "
                + " LEFT JOIN Industry e ON e.sIndstCdx = a.sIndstCdx ";
    }
    
    public String getAPPaymentSQL() {
        return " SELECT "
                + "   GROUP_CONCAT(DISTINCT a.sTransNox) AS sTransNox "
                + " , sum(b.nAppliedx) AS nAppliedx"
                + " FROM AP_Payment_Master a "
                + " LEFT JOIN AP_Payment_Detail b ON b.sTransNox = a.sTransNox ";
    }
    
    public String getDVPaymentSQL() {
        return " SELECT "
                + "   GROUP_CONCAT(DISTINCT a.sTransNox) AS sTransNox "
                + " , sum(b.nAmountxx) AS nAppliedx"
                + " FROM Disbursement_Master a "
                + " LEFT JOIN Disbursement_Detail b ON b.sTransNox = a.sTransNox ";
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case APPaymentAdjustmentStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case APPaymentAdjustmentStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case APPaymentAdjustmentStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case APPaymentAdjustmentStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case APPaymentAdjustmentStatus.PAID:
                    crs.updateString("cRefrStat", "PAID");
                    break;
                case APPaymentAdjustmentStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case APPaymentAdjustmentStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case APPaymentAdjustmentStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case APPaymentAdjustmentStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case APPaymentAdjustmentStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case APPaymentAdjustmentStatus.PAID:
                            crs.updateString("cRefrStat", "PAID");
                            break;
                        case APPaymentAdjustmentStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                    }
            }
            crs.updateRow(); 
        }
        
        JSONObject loJSON  = getEntryBy();
        String entryBy = "";
        String entryDate = "";
        
        if ("success".equals((String) loJSON.get("result"))){
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }
        
        showStatusHistoryUI("AP Payment Adjustment", (String) poModel.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM AP_Payment_Adjustment a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(poModel.getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(poModel.getTransactionNo())) ;
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsEntry = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsEntryDate =  dModified.format(formatter);
                }
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
          return poJSON;
        } 
        
        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL =   " SELECT b.sCompnyNm from xxxSysUser a " 
                       + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId)) ;
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                lsEntry = loRS.getString("sCompnyNm");
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
        } 
        return lsEntry;
    }

    /**
     * Changes the status of a transaction after validating the requested status
     * change and updates the linked transaction for the disbursement process.
     * <p>
     * This method invokes {@code statusChange()} to perform all necessary
     * validations and processing before allowing the status update. If the
     * validation fails, the corresponding error {@link org.json.JSONObject}
     * is returned immediately. Otherwise, a successful response is returned.
     * </p>
     *
     * <b>Purpose:</b> Used to check and validate status changes before updating
     * linked transactions on disbursement.
     *
     * @author TEEJEI DECELIS
     * @date Added: August 1, 2026
     *
     * @param tableName the name of the transaction table to be updated
     * @param sourceNo the source transaction number
     * @param remarks the remarks or reason associated with the status change
     * @param statusRequest the requested transaction status
     * @param needConfirmation {@code true} if the status change requires confirmation;
     *                         {@code false} otherwise
     * @param withParent {@code true} if the transaction has a parent transaction
     *                   that should also be considered during validation;
     *                   {@code false} otherwise
     * @return a {@link org.json.JSONObject} containing the operation result.
     *         Returns {@code "success"} if the status change is valid and processed;
     *         otherwise returns the validation error generated by
     *         {@code statusChange()}.
     *
     * @throws GuanzonException if a business rule or transaction validation fails
     * @throws SQLException if a database access error occurs
     * @throws CloneNotSupportedException if cloning of an internal object is not supported
     */
    public JSONObject changeStatus(String tableName, String sourceNo, String remarks, String statusRequest, boolean needConfirmation, boolean withParent)
            throws GuanzonException, SQLException, CloneNotSupportedException {
        poJSON = new  JSONObject();
        statusChange( tableName,
                sourceNo,
                remarks,
                statusRequest,
                needConfirmation,
                withParent);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
}
