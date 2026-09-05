/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.ClientInfo;
import org.guanzon.cas.client.services.ClientControllers;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cache_Payable_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatic;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatus;
import ph.com.guanzongroup.cas.cashflow.validator.APPaymentValidator;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Adjustment;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payee;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;

/**
 *
 * @author Aldrich & Arsiela Team 2 05232025
 */
public class SOATagging extends Transaction {

    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    private boolean pbValidatePayment = false;

    List<Model_AP_Payment_Master> paMasterList;
    List<PaymentRequest> paPaymentRequest;
    List<CachePayable> paCachePayable; 
    List<PurchaseOrderReceiving> paPOReceiving; 
    List<APPaymentAdjustment> paAPAdjustment; 
    List<Model> paPayablesList;
    List<String> paPayablesType;
    List<Model> paDetailRemoved;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "SOAt";

        poMaster = new CashflowModels(poGRider).SOATaggingMaster();
        poDetail = new CashflowModels(poGRider).SOATaggingDetails();

        paMasterList = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paPaymentRequest = new ArrayList<>();
        paCachePayable = new ArrayList<>();
        paAPAdjustment = new ArrayList<>();
        paPayablesList = new ArrayList<>();
        paPayablesType = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction()
            throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        //Clear data
        resetMaster();
        resetOthers();
        Detail().clear();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.CONFIRMED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        
        //Update others
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = saveUpdateOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.RETURNED;
        boolean lbReturn = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(SOATaggingStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (SOATaggingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 

//            poJSON = setValueToOthers(lsStatus);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

//        if (SOATaggingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
//            //Update Purchase Order, Inventory, Serial Ledger
//            poJSON = saveUpdateOthers(SOATaggingStatus.CONFIRMED);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                poGRider.rollbackTrans();
//                return poJSON;
//            }
//        }
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbReturn) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction return request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PaidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.PAID;
        boolean lbPaid = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already paid.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Update others
//        poJSON = setValueToOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
        
        //poGRider.beginTrans("UPDATE STATUS", "PaidTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPaid, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

//        poJSON = saveUpdateOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
        
        //poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbPaid) {
            poJSON.put("message", "Transaction paid successfully.");
        } else {
            poJSON.put("message", "Transaction paid request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ProcessTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.PROCESSED;
        boolean lbPosted = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(SOATaggingStatus.PROCESSED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPosted);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbPosted) {
            poJSON.put("message", "Transaction processed successfully.");
        } else {
            poJSON.put("message", "Transaction processing request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.CANCELLED;
        boolean lbCancelled = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(SOATaggingStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (SOATaggingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }

        //update 
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelledTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbCancelled, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = saveUpdateOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbCancelled) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = SOATaggingStatus.VOID;
        boolean lbVoid = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(SOATaggingStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (SOATaggingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }
        
        //Update Others
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbVoid, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = saveUpdateOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbVoid) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }

    public void setCategoryId(String categoryId) {
        psCategorCd = categoryId;
    }

    public JSONObject searchTransaction()
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                                                    + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId));
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»SOA No»Supplier»Payee",
                "dTransact»sTransNox»sSOANoxxx»sSupplrNm»sPayeeNme",
                "a.dTransact»a.sTransNox»a.sSOANoxxx»c.sCompnyNm»b.sCompnyNm»e.sPayeeNme",
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

    public JSONObject searchTransaction(String industryId, String company, String supplier, String sReferenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        if (supplier == null) {
            supplier = "";
        }
        if (sReferenceNo == null) {
            sReferenceNo = "";
        }

        if (company == null || "".equals(company)) {
            company = "";
        }

        if (industryId == null || "".equals(industryId)) {
            industryId = psIndustryId;
        }

        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(industryId)
                                                    + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + sReferenceNo));
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»SOA No»Supplier»Payee",
                "dTransact»sTransNox»sSOANoxxx»sSupplrNm»sPayeeNme",
                "a.dTransact»a.sTransNox»a.sSOANoxxx»c.sCompnyNm»b.sCompnyNm»e.sPayeeNme",
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
    
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if (item.getEditMode() == EditMode.UPDATE) {
                if(!paDetailRemoved.contains(item)){
                    paDetailRemoved.add(item);
                }
            }
            detail.remove();
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }

    public JSONObject AddDetail()
            throws CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getSourceNo() != null) {
                if (Detail(getDetailCount() - 1).getSourceNo().isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Last row has empty item.");
                    return poJSON;
                }
            }
        }

        return addDetail();
    }

    public int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    public Model_AP_Payment_Detail DetailRemove(int row) {
        return (Model_AP_Payment_Detail) paDetailRemoved.get(row);
    }

    /*Search Master References*/
    public JSONObject SearchCompany(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyId(object.getModel().getCompanyId());
        }
        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
        object.Master().setClientType(Logical.YES);
        poJSON = object.Master().searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setClientId(object.Master().getModel().getClientId());
        }

        return poJSON;
    }

    public JSONObject SearchPayee(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIssuedTo(object.getModel().getPayeeID());
        }

        return poJSON;
    }

    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        double ldblTransactionTotal = 0.0000;
        double ldblDiscountAmount = Master().getDiscountAmount().doubleValue();
        double ldblNetTotal = 0.0000;
        double ldblFreight = 0.0000;
        double ldblVatAmt = 0.0000;
        double ldblVatExemptAmt = 0.0000;
        double ldblZeroRtedAmt = 0.0000;

        //Compute Transaction Total
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                switch (Detail(lnCtr).getSourceCode()) {
                    case SOATaggingStatic.PaymentRequest:
                        break;
                    case SOATaggingStatic.POReceiving:
                        //Do not get the vat amount if there's already amount paid value
                        if(Detail(lnCtr).PurchasOrderReceivingMaster().getAmountPaid().doubleValue() <= 0.0000){
                            if(Detail(lnCtr).PurchasOrderReceivingMaster().getSupplierId().equals(Master().getClientId())){
                                if(Detail(lnCtr).PurchasOrderReceivingMaster().getTruckingId() == null
                                        || "".equals(Detail(lnCtr).PurchasOrderReceivingMaster().getTruckingId())
                                        || Detail(lnCtr).PurchasOrderReceivingMaster().getSupplierId().equals(Detail(lnCtr).PurchasOrderReceivingMaster().getTruckingId())){
                                    ldblFreight = ldblFreight + Detail(lnCtr).PurchasOrderReceivingMaster().getFreight().doubleValue();
                                }
                                ldblVatAmt = ldblVatAmt + Detail(lnCtr).PurchasOrderReceivingMaster().getVatAmount().doubleValue();
                                ldblVatExemptAmt = ldblVatExemptAmt + Detail(lnCtr).PurchasOrderReceivingMaster().getVatExemptSales().doubleValue();
                                ldblZeroRtedAmt = ldblZeroRtedAmt + Detail(lnCtr).PurchasOrderReceivingMaster().getZeroVatSales().doubleValue();
                            }
                        }
                        break;
                }

                ldblTransactionTotal = ldblTransactionTotal + Detail(lnCtr).getAppliedAmount().doubleValue();
            }
        }
        
        if(ldblTransactionTotal < 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Total Amount.");
            return poJSON;
        }

        Master().setTransactionTotal(ldblTransactionTotal);
        ldblNetTotal = ldblTransactionTotal - ldblDiscountAmount;
        Master().setNetTotal(ldblNetTotal);
        Master().setFreightAmount(ldblFreight);
        Master().setVatAmount(ldblVatAmt);
        Master().setVatExempt(ldblVatExemptAmt);
        Master().setZeroRatedVat(ldblZeroRtedAmt);

        return poJSON;
    }

    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }

    public JSONObject removeSOATaggingDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject loadSOATagging(String industryId, String company, String supplier, String referenceNo) {
        try {
            if (industryId == null) {
                industryId = psIndustryId;
            }
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
            if (psTranStat != null) {
                if (psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                        lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                    }
                    lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
                } else {
                    lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
                }
            }

            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(industryId)
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                    + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                    + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                    + " AND a.cProcessd = '0' "
            );

            if (lsTransStat != null && !"".equals(lsTransStat)) {
                lsSQL = lsSQL + lsTransStat;
            }

            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paMasterList = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sCompnyNm: " + loRS.getString("sSupplrNm"));
                    System.out.println("------------------------------------------------------------------------------");

                    paMasterList.add(SOATaggingMaster());
                    paMasterList.get(paMasterList.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paMasterList = new ArrayList<>();
                paMasterList.add(SOATaggingMaster());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    private Model_AP_Payment_Master SOATaggingMaster() {
        return new CashflowModels(poGRider).SOATaggingMaster();
    }

    public Model_AP_Payment_Master APPaymentMasterList(int row) {
        return (Model_AP_Payment_Master) paMasterList.get(row);
    }

    public int getSOATaggingCount() {
        if (paMasterList == null) {
            paMasterList = new ArrayList<>();
        }

        return paMasterList.size();
    }

    public String PayableType(int row) {
        return (String) paPayablesType.get(row);
    }

    private Model_Payment_Request_Master PaymentRequestMaster() {
        return new CashflowModels(poGRider).PaymentRequestMaster();
    }

    public Model_Payment_Request_Master PaymentRequestList(int row) {
        return (Model_Payment_Request_Master) paPayablesList.get(row);
    }

    private Model_Cache_Payable_Master CachePayableMaster() {
        return new CashflowModels(poGRider).Cache_Payable_Master();
    }

    public Model_Cache_Payable_Master CachePayableList(int row) {
        return (Model_Cache_Payable_Master) paPayablesList.get(row);
    }
    
    private Model_POR_Master POReceivingMaster() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
    }

    public Model_POR_Master POReceivingList(int row) {
        return (Model_POR_Master) paPayablesList.get(row);
    }
    
    private Model_AP_Payment_Adjustment APPaymentAdjustmentMaster() {
        return new CashflowModels(poGRider).APPaymentAdjustment();
    }

    public Model_AP_Payment_Adjustment APPaymentAdjustmentList(int row) {
        return (Model_AP_Payment_Adjustment) paPayablesList.get(row);
    }

    public int getPayablesCount() {
        if (paPayablesList == null) {
            paPayablesList = new ArrayList<>();
        }
        if (paPayablesType == null) {
            paPayablesType = new ArrayList<>();
        }

        return paPayablesList.size();
    }

//    public JSONObject loadPayables(String supplier, String company, String payee, String referenceNo) {
//        try {
//            paPayablesList = new ArrayList<>();
//            paPayablesType = new ArrayList<>();
//
//            if (company == null) {
//                company = "";
//            }
//            if (supplier == null) {
//                supplier = "";
//            }
//            if (referenceNo == null) {
//                referenceNo = "";
//            }
//
//            String lsSQL = getPayableSQL(supplier, company, payee, referenceNo) + " ORDER BY dTransact DESC ";
//            System.out.println("Executing SQL: " + lsSQL);
//            System.out.println("Payment Request List");
//            ResultSet loRS = poGRider.executeQuery(lsSQL);
//            poJSON = new JSONObject();
//            int lnctr = 0;
//            if (MiscUtil.RecordCount(loRS) >= 0) {
//                while (loRS.next()) {
//                    // Print the result set
//                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
//                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
//                    System.out.println("sPayablNm: " + loRS.getString("sPayablNm"));
//                    System.out.println("sPayablTp: " + loRS.getString("sPayablTp"));
//                    System.out.println("------------------------------------------------------------------------------");
//
//                    switch (loRS.getString("sPayablTp")) {
//                        case SOATaggingStatic.PaymentRequest:
//                            paPayablesList.add(PaymentRequestMaster());
//                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sTransNox"));
//                            paPayablesType.add(SOATaggingStatic.PaymentRequest);
//                            break;
//                        case SOATaggingStatic.POReceiving:
//                            paPayablesList.add(POReceivingMaster());
//                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sTransNox"));
//                            paPayablesType.add(SOATaggingStatic.POReceiving);
//                            break;
//                        case SOATaggingStatic.APPaymentAdjustment:
//                            paPayablesList.add(APPaymentAdjustmentMaster());
//                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sTransNox"));
//                            paPayablesType.add(SOATaggingStatic.POReceiving);
//                            break;
//                    }
//                    lnctr++;
//                }
//
//                System.out.println("Records found: " + lnctr);
//                poJSON.put("result", "success");
//                poJSON.put("message", "Record loaded successfully.");
//            } else {
//                poJSON.put("result", "error");
//                poJSON.put("continue", true);
//                poJSON.put("message", "No record found.");
//            }
//
//            MiscUtil.close(loRS);
//        } catch (SQLException e) {
//            poJSON.put("result", "error");
//            poJSON.put("message", e.getMessage());
//        } catch (GuanzonException ex) {
//            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//            poJSON.put("result", "error");
//            poJSON.put("message", MiscUtil.getException(ex));
//        }
//        return poJSON;
//    }

    public JSONObject loadPayables(String supplier, String company, String payee, String referenceNo, String payableType) {
        try {
            paPayablesList = new ArrayList<>();
            paPayablesType = new ArrayList<>();

            if (company == null) {
                company = "";
            }
            if (supplier == null) {
                supplier = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }

            String lsSQL = getPayableSQL(supplier, company, payee, referenceNo) + " ORDER BY dDueDatex, sPayablNm ASC ";
            switch(payableType){
                case SOATaggingStatic.APPaymentAdjustment:
                case SOATaggingStatic.POReceiving:
                    lsSQL = getCachePayableSQL(supplier, company, payee, referenceNo, payableType) + " ORDER BY dDueDatex, sPayablNm ASC ";
                    break;
                case SOATaggingStatic.PaymentRequest:
                    lsSQL = getPRFSQL(supplier, company, payee, referenceNo) + " ORDER BY dDueDatex, sPayablNm  ASC ";
                    break;
            }
            System.out.println("Executing SQL: " + lsSQL);
            System.out.println("Payment Request List");
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            int lnctr = 0;
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sPayblNox: " + loRS.getString("sPayblNox"));
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sPayablNm: " + loRS.getString("sPayablNm"));
                    System.out.println("sPayablTp: " + loRS.getString("sPayablTp"));
                    System.out.println("------------------------------------------------------------------------------");

                    switch (loRS.getString("sPayablTp")) {
                        case SOATaggingStatic.PaymentRequest:
                            paPayablesList.add(PaymentRequestMaster());
                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sTransNox"));
                            paPayablesType.add(SOATaggingStatic.PaymentRequest);
                            break;
                        case SOATaggingStatic.POReceiving:
                        case SOATaggingStatic.APPaymentAdjustment:
                            paPayablesList.add(CachePayableMaster());
                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sTransNox"));
                            paPayablesType.add(loRS.getString("sPayablTp"));
                            break;
//                        case SOATaggingStatic.POReceiving:
//                            paPayablesList.add(POReceivingMaster());
//                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sPayblNox"));
//                            paPayablesType.add(SOATaggingStatic.POReceiving);
//                            break;
//                        case SOATaggingStatic.APPaymentAdjustment:
//                            paPayablesList.add(APPaymentAdjustmentMaster());
//                            paPayablesList.get(paPayablesList.size() - 1).openRecord(loRS.getString("sPayblNox"));
//                            paPayablesType.add(SOATaggingStatic.APPaymentAdjustment);
//                            break;
                    }
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }

            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    public JSONObject searchPayables(String sourceNo, String payableType, int row) {
        try {
            poJSON = new JSONObject();
            poJSON.put("row", row);
            paPayablesList = new ArrayList<>();
            paPayablesType = new ArrayList<>();
            String lsCriteria = "";
            if (sourceNo == null) {
                sourceNo = "";
            }
            
            if(payableType == null || "".equals(payableType)){
                poJSON.put("result", "error");
                poJSON.put("message", "Source Code cannot be empty.");
                poJSON.put("row", row);
                return poJSON;
            }

            String lsSQL = getPayableSQL(Master().Supplier().getCompanyName(), Master().Company().getCompanyName(), Master().Payee().getPayeeName(), sourceNo);
            switch(payableType){
                case SOATaggingStatic.APPaymentAdjustment:
                case SOATaggingStatic.POReceiving:
                    lsSQL = getCachePayableSQL(Master().Supplier().getCompanyName(), Master().Company().getCompanyName(), Master().Payee().getPayeeName(), sourceNo, payableType) ;
                    lsCriteria = "a.dTransact»a.sSourceNo»a.sReferNox»b.sCompnyNm»c.sCompnyNm";
                    break;
                case SOATaggingStatic.PaymentRequest:
                    lsSQL = getPRFSQL(Master().Supplier().getCompanyName(), Master().Company().getCompanyName(), Master().Payee().getPayeeName(), sourceNo) ;
                    lsCriteria = "a.dTransact»a.sTransNox»a.sSeriesNo»b.sPayeeNme»c.sCompnyNm";
                    break;
                default:
                    lsCriteria = "a.dTransact";
            }
            System.out.println("Executing SQL: " + lsSQL);
            poJSON = ShowDialogFX.Browse(poGRider,
                    lsSQL,
                    "",
                    "Transaction Date»Transaction No»Reference No»Payable»Company",
                    "dTransact»sPayblNox»sReferenc»sPayablNm»sCompnyNm", 
                    lsCriteria,
                    1);
            if (poJSON != null) {
                poJSON = addPayablesToSOADetail((String) poJSON.get("sTransNox"), (String) poJSON.get("sPayablTp"));
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
                poJSON.put("row", row);
                return poJSON;
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            poJSON.put("row", row);
        } catch (GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            poJSON.put("row", row);
        }
        poJSON.put("row", row);
        return poJSON;
    }
    
    
    //Added by Arsiela - 05-27-2026 11:50 AM
    //Load Payee check primary contact person of selected supplier
    private String findPrimaryContactPerson(String fsClientId) throws CloneNotSupportedException, GuanzonException, SQLException{
        ClientInfo loClient = new ClientControllers(poGRider, logwrapr).ClientInfo();
        loClient.initialize();
        loClient.setWithParentClass(true);
        loClient.setWithUI(false);

        poJSON = loClient.openClientRecord(fsClientId);
        if ("success".equals((String) poJSON.get("result"))) {
            for(int lnCheck = 0;lnCheck < loClient.getInstiContactCount(); lnCheck++){
                if(Logical.YES.equals(loClient.InstiContact(lnCheck).getcPayeexxx())
                    && loClient.InstiContact(lnCheck).isPrimaryContactPersion()){
                    return loClient.InstiContact(lnCheck).getcCPrsonID();
                }
            }
        } 
    
        return fsClientId;
    }
    
    public JSONObject addPayablesToSOADetail(String transactionNo, String payableType)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", 0);
        int lnCtr = 0;
        int lnRow = getDetailCount() - 1;
        String lsCompanyId = "";
        String lsClientId = "";
        String lsIssuedTo = "";
        String lsTransNo = "";
        String lsSourceCd = "";
        Double ldblBalance = 0.0000;
        Number ldblTranTotal = 0.0000;
        Number ldblDebitAmt = 0.0000;
        Number ldblCreditAmt = 0.0000;

        switch (payableType) {
            case SOATaggingStatic.PaymentRequest:
                //Check if transaction already exists in the list
                for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                    if (transactionNo.equals(Detail(lnCtr).getSourceNo())
                        && payableType.equals(Detail(lnCtr).getSourceCode())) {
                        if(Detail(lnCtr).isReverse()) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Selected transaction no " + transactionNo + " already exists in SOA detail.");
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        } else {
                            lnRow = lnCtr;
                            break;
                        }
                    }
                }
                
                PaymentRequest loPaymentRequest = new CashflowControllers(poGRider, logwrapr).PaymentRequest();
                loPaymentRequest.setWithParent(true);
                loPaymentRequest.InitTransaction();
                poJSON = loPaymentRequest.OpenTransaction(transactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("row", 0);
                    return poJSON;
                }

                lsCompanyId = loPaymentRequest.Master().getCompanyID();
                lsIssuedTo = loPaymentRequest.Master().getPayeeID();
                lsTransNo = loPaymentRequest.Master().getTransactionNo();
                lsSourceCd = loPaymentRequest.getSourceCode();
                ldblTranTotal = loPaymentRequest.Master().getNetTotal();
                ldblDebitAmt = loPaymentRequest.Master().getNetTotal();
                lsClientId = loPaymentRequest.Master().Payee().getAPClientID();
                if(lsClientId == null || "".equals(lsClientId)){
                    lsClientId = loPaymentRequest.Master().Payee().getClientID();
                }
                ldblBalance = loPaymentRequest.Master().getNetTotal() - loPaymentRequest.Master().getAmountPaid();
                
                if ((lsCompanyId == null || lsCompanyId.isEmpty()) && (Master().getCompanyId() == null || "".equals(Master().getCompanyId()))){
                    poJSON.put("result", "error");
                    poJSON.put("message", "The selected PRF does not have an associated company. Please select a company before choosing a PRF transaction.");
                    poJSON.put("row", lnCtr);
                    return poJSON;
                }
                
                if(Master().getClientId() == null || "".equals(Master().getClientId())){
                    Master().setClientId(lsClientId);
                } else {
                    if (!Master().getClientId().equals(lsClientId)) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Supplier of payables is not equal to transaction supplier.");
                        poJSON.put("row", lnCtr);
                        return poJSON;
                    }
                }

                if (Master().getIssuedTo() == null || "".equals(Master().getIssuedTo())) {
                    Master().setIssuedTo(lsIssuedTo);
                } else {
                    if (!Master().getIssuedTo().equals(lsIssuedTo)) {
                        //Check if payee ap client is equal to the ap client linked to disbursement voucher - Arsiela 05-28-2026
                        boolean lbSuccess = true;
                        Model_Payee loObj = new CashflowModels(poGRider).Payee();
                        loObj.initialize();
                        loObj.openRecord(lsIssuedTo);
                        if(loObj.getEditMode() == EditMode.READY){
                            if(Master().Payee().getAPClientID() != null && !"".equals(Master().Payee().getAPClientID())){
                                lbSuccess = Master().Payee().getAPClientID().equals(loObj.getAPClientID());
                            } else {
                                lbSuccess = Master().Payee().getClientID().equals(loObj.getClientID());
                            }
                        } else {
                            lbSuccess = false;
                        }
                        if(!lbSuccess){
                            poJSON.put("result", "error");
                            poJSON.put("message", "Selected Payee of payables is not equal to transaction payee.");
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        }
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Payee of payables is not equal to transaction payee.");
//                        poJSON.put("row", lnCtr);
//                        return poJSON;
                    }
                }
                break;
            case SOATaggingStatic.APPaymentAdjustment:
            case SOATaggingStatic.POReceiving:
                CachePayable loCachePayable = new CashflowControllers(poGRider, logwrapr).CachePayable();
                loCachePayable.InitTransaction();
                poJSON = loCachePayable.OpenTransaction(transactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("row", 0);
                    return poJSON;
                }
                
                //Check if transaction already exists in the list
                for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                    if (loCachePayable.Master().getSourceNo().equals(Detail(lnCtr).getSourceNo())
                        && loCachePayable.Master().getClientId().equals(Master().getClientId())
                        && payableType.equals(Detail(lnCtr).getSourceCode())) {
                        if(Detail(lnCtr).isReverse()) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Selected transaction no " + loCachePayable.Master().getSourceNo() + " already exists in SOA detail.");
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        } else {
                            lnRow = lnCtr;
                            break;
                        }
                    }
                }
                
                lsCompanyId = loCachePayable.Master().getCompanyId();
                lsClientId = loCachePayable.Master().getClientId();
                lsTransNo = loCachePayable.Master().getSourceNo();
                lsSourceCd = loCachePayable.Master().getSourceCode();
                ldblTranTotal = loCachePayable.Master().getNetTotal();
                ldblDebitAmt = loCachePayable.Master().getPayables();
                ldblCreditAmt = loCachePayable.Master().getReceivables();
                ldblBalance = loCachePayable.Master().getNetTotal() - loCachePayable.Master().getAmountPaid();
                
                if(Master().getClientId() == null || "".equals(Master().getClientId())){
                    Master().setClientId(lsClientId);
                } else {
                    if (!Master().getClientId().equals(lsClientId)) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Supplier of payables is not equal to transaction supplier.");
                        poJSON.put("row", lnCtr);
                        return poJSON;
                    }
                }
                
                //populate payee of client
                Payee loPayee = new CashflowControllers(poGRider, logwrapr).Payee();
                loPayee.initialize();
                poJSON = loPayee.getModel().openRecordByReference(lsClientId);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON = loPayee.getModel().openRecordByReference(findPrimaryContactPerson(lsClientId));
                }
                
                if (!"error".equals((String) poJSON.get("result"))) {
                    if(Master().getIssuedTo() == null || "".equals(Master().getIssuedTo())){
                        Master().setIssuedTo(loPayee.getModel().getPayeeID());
                    } else {
                        if (!Master().getIssuedTo().equals(loPayee.getModel().getPayeeID())) {
                            //Check if payee ap client is equal to the ap client linked to disbursement voucher - Arsiela 05-28-2026
                            boolean lbSuccess = true;
                            Model_Payee loObj = new CashflowModels(poGRider).Payee();
                            loObj.initialize();
                            loObj.openRecord(loPayee.getModel().getPayeeID());
                            if(loObj.getEditMode() == EditMode.READY){
                                if(Master().Payee().getAPClientID() != null && !"".equals(Master().Payee().getAPClientID())){
                                    lbSuccess = Master().Payee().getAPClientID().equals(loObj.getAPClientID());
                                } else {
                                    lbSuccess = Master().Payee().getClientID().equals(loObj.getClientID());
                                }
                            } else {
                                lbSuccess = false;
                            }
                            if(!lbSuccess){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Selected Supplier payee of payables is not equal to transaction supplier.");
                                poJSON.put("row", lnCtr);
                                return poJSON;
                            }
                        }
                    }
                }

                break;
        }
        
        if(ldblBalance <= 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "No remaining balance for the selected transaction.\n\nContact System Administrator to address the issue.");
            poJSON.put("row", lnCtr);
            return poJSON;
        }
        
        if (Master().getCompanyId() == null || "".equals(Master().getCompanyId())) {
            Master().setCompanyId(lsCompanyId);
        } else {
            if(lsCompanyId != null && !"".equals(lsCompanyId) && !lsCompanyId.equals(Master().getCompanyId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Selected Company of payables is not equal to transaction company.");
                poJSON.put("row", lnCtr);
                return poJSON;
            }
        }

        if (Master().getClientId() == null || "".equals(Master().getClientId())) {
            Master().setClientId(lsClientId);
        }
        
        if(ldblCreditAmt.doubleValue() > 0.0000){
            ldblBalance = -ldblBalance;
        }

        Detail(lnRow).isReverse(true);
        Detail(lnRow).setSourceNo(lsTransNo);
        Detail(lnRow).setSourceCode(lsSourceCd);
        Detail(lnRow).setTransactionTotal(ldblTranTotal);
        Detail(lnRow).setAppliedAmount(ldblBalance); //Set transaction balance as default applied amount
        Detail(lnRow).setDebitAmount(ldblDebitAmt);
        Detail(lnRow).setCreditAmount(ldblCreditAmt);
        
        JSONObject loJSON = computeFields();
        if ("error".equals((String) loJSON.get("result"))) {
            loJSON.put("row", lnRow);
            Detail().remove(lnRow);
            AddDetail();
            return loJSON;
        }
        
        AddDetail();

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
   
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getSourceNo() == null || "".equals(Detail(lnCtr).getSourceNo())) {
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    deleteDetail(lnCtr); 
                } 
            }
                
            lnCtr--;
        }

          if ((getDetailCount() - 1) >= 0) {
            if (Detail(getDetailCount() - 1).getSourceNo() != null
                    && !"".equals(Detail(getDetailCount() - 1).getSourceNo())) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
     
//    public JSONObject addPayablesToSOADetail(String transactionNo, String payableType)
//            throws CloneNotSupportedException,
//            SQLException,
//            GuanzonException {
//        poJSON = new JSONObject();
//        poJSON.put("row", 0);
//        int lnCtr = 0;
//        String lsCompanyId = "";
//        String lsClientId = "";
//        String lsIssuedTo = "";
//        String lsTransNo = "";
//        String lsSourceCd = "";
//        Number ldblTranTotal = 0.0000;
//        Number ldblDebitAmt = 0.0000;
//        Number ldblCreditAmt = 0.0000;
//
//        //Check if transaction already exists in the list
//        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
//            if (transactionNo.equals(Detail(lnCtr).getSourceNo())) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "Selected transaction no " + transactionNo + " already exists in SOA detail.");
//                poJSON.put("row", lnCtr);
//                return poJSON;
//            }
//        }
//
//        switch (payableType) {
//            case SOATaggingStatic.PaymentRequest:
//                PaymentRequest loPaymentRequest = new CashflowControllers(poGRider, logwrapr).PaymentRequest();
//                loPaymentRequest.setWithParent(true);
//                loPaymentRequest.InitTransaction();
//                poJSON = loPaymentRequest.OpenTransaction(transactionNo);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    poJSON.put("row", 0);
//                    return poJSON;
//                }
//
//                lsCompanyId = loPaymentRequest.Master().getCompanyID();
//                lsIssuedTo = loPaymentRequest.Master().getPayeeID();
//                lsTransNo = loPaymentRequest.Master().getTransactionNo();
//                lsSourceCd = loPaymentRequest.getSourceCode();
//                ldblTranTotal = loPaymentRequest.Master().getTranTotal();
//                ldblDebitAmt = loPaymentRequest.Master().getTranTotal();
//                lsClientId = loPaymentRequest.Master().Payee().getClientID();
//                
//                if ((lsCompanyId == null || lsCompanyId.isEmpty()) && (Master().getCompanyId() == null || "".equals(Master().getCompanyId()))){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "The selected PRF does not have an associated company. Please select a company before choosing a PRF transaction.");
//                    poJSON.put("row", lnCtr);
//                    return poJSON;
//                }
//                
//                if(Master().getClientId() == null || "".equals(Master().getClientId())){
//                    Master().setClientId(lsClientId);
//                } else {
//                    if (!Master().getClientId().equals(lsClientId)) {
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Supplier of payables is not equal to transaction supplier.");
//                        poJSON.put("row", lnCtr);
//                        return poJSON;
//                    }
//                }
//
//                if (Master().getIssuedTo() == null || "".equals(Master().getIssuedTo())) {
//                    Master().setIssuedTo(lsIssuedTo);
//                } else {
//                    if (!Master().getIssuedTo().equals(lsIssuedTo)) {
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Payee of payables is not equal to transaction payee.");
//                        poJSON.put("row", lnCtr);
//                        return poJSON;
//                    }
//                }
//                break;
//            case SOATaggingStatic.POReceiving:
//                CachePayable loCachePayable = new CashflowControllers(poGRider, logwrapr).CachePayable();
//                loCachePayable.InitTransaction();
//                poJSON = loCachePayable.OpenTransaction(transactionNo);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
//                lsCompanyId = loCachePayable.Master().getCompanyId();
//                lsClientId = loCachePayable.Master().getClientId();
//                lsTransNo = loCachePayable.Master().getTransactionNo();
//                lsSourceCd = loCachePayable.getSourceCode();
//                ldblTranTotal = loCachePayable.Master().getNetTotal();
//                ldblDebitAmt = loCachePayable.Master().getPayables();
//                ldblCreditAmt = loCachePayable.Master().getReceivables();
//                
//                if(Master().getClientId() == null || "".equals(Master().getClientId())){
//                    Master().setClientId(lsClientId);
//                } else {
//                    if (!Master().getClientId().equals(lsClientId)) {
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Supplier of payables is not equal to transaction supplier.");
//                        poJSON.put("row", lnCtr);
//                        return poJSON;
//                    }
//                }
//
//                break;
//        }
//
//        if (Master().getCompanyId() == null || "".equals(Master().getCompanyId())) {
//            Master().setCompanyId(lsCompanyId);
//        } else {
//            if(lsCompanyId != null && !"".equals(lsCompanyId) && !lsCompanyId.equals(Master().getCompanyId())){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Selected Company of payables is not equal to transaction company.");
//                poJSON.put("row", lnCtr);
//                return poJSON;
//            }
//        }
//
//        if (Master().getClientId() == null || "".equals(Master().getClientId())) {
//            Master().setClientId(lsClientId);
//        }
//
//        Detail(getDetailCount() - 1).setSourceNo(lsTransNo);
//        Detail(getDetailCount() - 1).setSourceCode(lsSourceCd);
//        Detail(getDetailCount() - 1).setTransactionTotal(ldblTranTotal);
//        Detail(getDetailCount() - 1).setDebitAmount(ldblDebitAmt);
//        Detail(getDetailCount() - 1).setCreditAmount(ldblCreditAmt);
//        AddDetail();
//
//        poJSON.put("result", "success");
//        poJSON.put("message", "success");
//        return poJSON;
//    }

    public void resetOthers() {
//        paOthers = new ArrayList<>();
    }

    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).SOATaggingMaster();
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_AP_Payment_Master Master() {
        return (Model_AP_Payment_Master) poMaster;
    }

    @Override
    public Model_AP_Payment_Detail Detail(int row) {
        return (Model_AP_Payment_Detail) paDetail.get(row);
    }

    public Model_AP_Payment_Detail getDetail() {
        return (Model_AP_Payment_Detail) poDetail;
    }
    
    public JSONObject callApproval(){
        poJSON = new JSONObject();
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
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        setApproving((String) poJSON.get("sUserIDxx"));
        return poJSON;
    }

    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();

        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        if (Master().getTransactionTotal().doubleValue() <= 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid transaction total amount.");
            return poJSON;
        }

        if (getDetailCount() == 0) {
            //do not allow a detail with no applied amount
            if (Detail(0).getAppliedAmount().doubleValue() == 0.0000) {
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid transaction total amount.");
                return poJSON;
            }
        }
        
        //Check detail
        boolean lbWillDelete = true;
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            if(Detail(lnCtr).isReverse()){
                if ((Detail(lnCtr).getAppliedAmount().doubleValue() > 0.00) && Detail(lnCtr).isReverse() ) {
                    lbWillDelete = false;
                }
            }
        }
        
        if(lbWillDelete){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction applied amount to be save.");
            return poJSON;
        }
        
        if (SOATaggingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = callApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
        }
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
        }

        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        Iterator<Model> detail = Detail().iterator();
        String lsAppliedAmount = "0.0000";
        while (detail.hasNext()) {
            Model item = detail.next();
            if (item.getEditMode() == EditMode.UPDATE) {
                if (item.getValue("cReversex").equals(SOATaggingStatic.Reverse.EXCLUDE)) {
                    if(!paDetailRemoved.contains(item)){
                        paDetailRemoved.add(item);
                        continue;
                    }
                }
            }
            
            if (item.getValue("nAppliedx") != null && !"".equals(item.getValue("nAppliedx"))) {
                lsAppliedAmount = item.getValue("nAppliedx").toString();
            }
            if (Double.valueOf(lsAppliedAmount) == 0.0000) {
                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                } else if (item.getEditMode() == EditMode.UPDATE) {
                    if(!paDetailRemoved.contains(item)){
                        paDetailRemoved.add(item);
                    }
                    item.setValue("cReversex", SOATaggingStatic.Reverse.EXCLUDE);
                }
            }
            
            lsAppliedAmount = "0.0000";
        }

        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No SOA detail to be save.");
            return poJSON;
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                if(pbValidatePayment){
                    poJSON = validatePayableAmt(lnCtr);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }

            //Set value to por detail
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
//            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        computeFields();

        //Update linked transactions
        poJSON = setValueToOthers(Master().getTransactionStatus());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        try {
            /*Only modify this if there are other tables to modify except the master and detail tables*/
            poJSON = new JSONObject();

            poJSON = saveUpdateOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(SOATaggingStatus.OPEN);
    }

    private PaymentRequest PaymentRequest() throws SQLException, GuanzonException {
        return new CashflowControllers(poGRider, logwrapr).PaymentRequest();
    }

    private CachePayable CachePayable() throws SQLException, GuanzonException {
        return new CashflowControllers(poGRider, logwrapr).CachePayable();
    }
    
    private PurchaseOrderReceiving PurchaseOrderReceiving() throws SQLException, GuanzonException {
        return new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
    }
    
    private APPaymentAdjustment APPaymentAdjustment() throws SQLException, GuanzonException {
        return new CashflowControllers(poGRider, logwrapr).APPaymentAdjustment();
    }
    
    public void validatePayment(boolean validatePayment) {
        pbValidatePayment = validatePayment;
    }

    private JSONObject validatePayableAmt(int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        double ldblBalance = 0.0000;
        switch (Detail(row).getSourceCode()) {
            case SOATaggingStatic.PaymentRequest:
                ldblBalance = Detail(row).PaymentRequestMaster().getTranTotal()
                        - (Detail(row).getAppliedAmount().doubleValue()
                        + getPayment(Detail(row).getSourceNo(),Detail(row).getSourceCode(), true));
                if (ldblBalance < 0.0000) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Invalid transaction balance " + setIntegerValueToDecimalFormat(ldblBalance,true) + " for source no " + Detail(row).getSourceNo() + ".");
                    poJSON.put("row", row);
                    return poJSON;
                }
                break;
            case SOATaggingStatic.POReceiving:
                if(Detail(row).PurchasOrderReceivingMaster().getSupplierId().equals(Master().getClientId())){
                    if(Detail(row).PurchasOrderReceivingMaster().getTruckingId() != null && !"".equals(Detail(row).PurchasOrderReceivingMaster().getTruckingId())
                        && !Detail(row).PurchasOrderReceivingMaster().getSupplierId().equals(Detail(row).PurchasOrderReceivingMaster().getTruckingId())){
                        ldblBalance = Detail(row).PurchasOrderReceivingMaster().getNetTotal() - Detail(row).PurchasOrderReceivingMaster().getFreight().doubleValue();
                    } else {
                        ldblBalance = Detail(row).PurchasOrderReceivingMaster().getNetTotal();
                    }
                } else {
                    if(Detail(row).PurchasOrderReceivingMaster().getTruckingId().equals(Master().getClientId())){
                        ldblBalance = Detail(row).PurchasOrderReceivingMaster().getFreight().doubleValue();
                    }
                }
                
                ldblBalance = ldblBalance
                        - (Detail(row).getAppliedAmount().doubleValue()
                        + getPayment(Detail(row).getSourceNo(),Detail(row).getSourceCode(),  true));
                if (ldblBalance < 0.0000) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Invalid transaction balance " + setIntegerValueToDecimalFormat(ldblBalance,true) + " for source no " + Detail(row).getSourceNo() + ".");
                    poJSON.put("row", row);
                    return poJSON;
                }
                break;
            case SOATaggingStatic.APPaymentAdjustment:
                ldblBalance = Detail(row).APPaymentAdjustmentMaster().getNetTotal().doubleValue()
                        - (Detail(row).getAppliedAmount().doubleValue()
                        + getPayment(Detail(row).getSourceNo(), Detail(row).getSourceCode(), true));
                if (ldblBalance < 0.0000) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Invalid transaction balance " + setIntegerValueToDecimalFormat(ldblBalance,true) + " for source no " + Detail(row).getSourceNo() + ".");
                    poJSON.put("row", row);
                    return poJSON;
                }
                break;
        }

        return poJSON;
    }
    
    public static String setIntegerValueToDecimalFormat(Object foObject, boolean fbIs4Decimal) {
        String lsDecimalFormat = fbIs4Decimal ? "#,##0.0000" : "#,##0.00";
        DecimalFormat format = new DecimalFormat(lsDecimalFormat);
        try {
            if (foObject != null) {
                return format.format(Double.parseDouble(String.valueOf(foObject)));
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format for input - " + foObject);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
        return fbIs4Decimal ? "0.0000" : "0.00";
    }

    private double getPayment(String sourceNo, String sourceCode, boolean validate) {
        String lsSQL = "";
        double ldPayment = 0.0000;
        try {
            if(validate){
                lsSQL = MiscUtil.addCondition(getAPPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sClientID = " + SQLUtil.toSQL(Master().getClientId())
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                    + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.CANCELLED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.VOID)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.RETURNED)
                );
            
            } else {
                lsSQL = MiscUtil.addCondition(getAPPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sClientID = " + SQLUtil.toSQL(Master().getClientId())
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE)
                    + " AND ( a.cTranStat = " + SQLUtil.toSQL(SOATaggingStatus.PROCESSED)
                    + " OR a.cTranStat = " + SQLUtil.toSQL(SOATaggingStatus.CONFIRMED)
                    + " OR a.cTranStat = " + SQLUtil.toSQL(SOATaggingStatus.PAID)
                    + " ) "
                );
            }
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------AP PAYMENT--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    ldPayment = ldPayment + loRS.getDouble("nAppliedx");
                }
            }
            MiscUtil.close(loRS);

            lsSQL = MiscUtil.addCondition(getDVPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                    + " AND a.sPayeeIDx = " + SQLUtil.toSQL(Master().getClientId()) //TODO
//                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE) //TODO
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.CANCELLED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.VOID)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.DISAPPROVED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.RETURNED)
            );
            System.out.println("Executing SQL: " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------DV--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    ldPayment = ldPayment + loRS.getDouble("nAppliedx");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return ldPayment;
    }
    
    private boolean getLinkedPayment(String sourceNo, String sourceCode){
        try {
            String lsSQL = MiscUtil.addCondition(getAPPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sClientID = " + SQLUtil.toSQL(Master().getClientId())
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE)
                    + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.CANCELLED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(SOATaggingStatus.VOID)
            );
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------AP PAYMENT--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        return true;
                    }
                }
            }
            MiscUtil.close(loRS);

            lsSQL = MiscUtil.addCondition(getDVPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sPayeeIDx = " + SQLUtil.toSQL(Master().getClientId()) //TODO
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
//                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE) //TODO
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.CANCELLED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.VOID)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.DISAPPROVED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.RETURNED)
            );
            System.out.println("Executing SQL: " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------DV--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        return true;
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return false;
    } 
    
    private boolean isWithSOA(String sourceNo, String sourceCode){
        try {
            String lsSQL = MiscUtil.addCondition(getAPPaymentSQL(),
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                    + " AND a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                    + " AND a.sClientID = " + SQLUtil.toSQL(Master().getClientId())
                    + " AND b.cReversex = " + SQLUtil.toSQL(SOATaggingStatic.Reverse.INCLUDE)
                    + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND a.cTranStat = " + SQLUtil.toSQL(SOATaggingStatus.CONFIRMED)
            );
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------AP PAYMENT WITH SOA--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        return true;
                    }
                }
            }
            MiscUtil.close(loRS);
    
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return false;
    } 

//    private JSONObject setValueToOthers(String status)
//            throws CloneNotSupportedException,
//            SQLException,
//            GuanzonException {
//        poJSON = new JSONObject();
//        paPaymentRequest = new ArrayList<>();
//        paCachePayable = new ArrayList<>();
//        paAPAdjustment = new ArrayList<>();
//        int lnCtr;
//
//        //Update Purchase Order exist in PO Receiving Detail
//        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
//            System.out.println("----------------------SOA Detail---------------------- ");
//            System.out.println("Source No : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceNo());
//            System.out.println("Source Code : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceCode());
//            System.out.println("------------------------------------------------------------------ ");
//
//            switch (Detail(lnCtr).getSourceCode()) {
//                case SOATaggingStatic.PaymentRequest:
//                    paPaymentRequest.add(PaymentRequest());
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).InitTransaction();
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).UpdateTransaction();
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess("1");
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa("1");
//
//                    switch (status) {
//                        case SOATaggingStatus.VOID:
//                        case SOATaggingStatus.CANCELLED:
//                        case SOATaggingStatus.RETURNED:
//                            if(getLinkedPayment(paPaymentRequest.get(paPaymentRequest.size() - 1).Master().getTransactionNo()) ){
//                                paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess("1");
//                            } else {
//                                paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess("0");
//                                paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa("0");
//                            }
//                            break;
//                        case SOATaggingStatus.CONFIRMED:
//                            paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa("1");
//                            break;
//                    }
//
//                    break;
//                case SOATaggingStatic.CachePayable:
//                    paCachePayable.add(CachePayable());
//                    paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
//                    paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
//                    paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
//                    paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(true);
//                    
//                    if(SOATaggingStatic.APPaymentAdjustment.equals(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceCode())){
//                        paAPAdjustment.add(APPaymentAdjustment());
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).initialize();
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).OpenTransaction(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceNo());
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).UpdateTransaction();
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(true);
//                    }
//                    
//                    switch (status) {
//                        case SOATaggingStatus.VOID:
//                        case SOATaggingStatus.CANCELLED:
//                        case SOATaggingStatus.RETURNED:
//                            paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(getLinkedPayment(paCachePayable.get(paCachePayable.size() - 1).Master().getTransactionNo()) );
//                            paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(false);
//                            if(SOATaggingStatic.APPaymentAdjustment.equals(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceCode())){
//                                paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(paCachePayable.get(paCachePayable.size() - 1).Master().isProcessed());
//                            }
//                            break;
//                        case SOATaggingStatus.CONFIRMED:
//                            paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(true);
//                            break;
//                    }
//                    break;
//            }
//
//        }
//
//        //Update purchase order removed in purchase order receiving
//        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
//
//            System.out.println("----------------------Removed SOA Detail---------------------- ");
//            System.out.println("Source No : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceNo());
//            System.out.println("Source Code : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceCode());
//            System.out.println("------------------------------------------------------------------ ");
//
//            switch (Detail(lnCtr).getSourceCode()) {
//                case SOATaggingStatic.PaymentRequest:
//                    paPaymentRequest.add(PaymentRequest());
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).InitTransaction();
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
//                    paPaymentRequest.get(paPaymentRequest.size() - 1).UpdateTransaction();
//                    if(!getLinkedPayment(paPaymentRequest.get(paPaymentRequest.size() - 1).Master().getTransactionNo())){
//                        paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess("0");
//                    }
//                    break;
//                case SOATaggingStatic.CachePayable:
//                    paCachePayable.add(CachePayable());
//                    paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
//                    paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
//                    paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
//                    paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(getLinkedPayment(paCachePayable.get(paCachePayable.size() - 1).Master().getTransactionNo()));
//                        
//                    if(SOATaggingStatic.APPaymentAdjustment.equals(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceCode())){
//                        paAPAdjustment.add(APPaymentAdjustment());
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).initialize();
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).OpenTransaction(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceNo());
//                        paAPAdjustment.get(paAPAdjustment.size() - 1).UpdateTransaction();
//                        if(SOATaggingStatic.APPaymentAdjustment.equals(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceCode())){
//                            paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(paCachePayable.get(paCachePayable.size() - 1).Master().isProcessed());
//                        }
//                    }
//                    break;
//            }
//        }
//
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paPaymentRequest = new ArrayList<>();
        paCachePayable = new ArrayList<>();
        paPOReceiving = new ArrayList<>();
        paAPAdjustment = new ArrayList<>();
        int lnCtr;
        boolean lbIsLinked = false;

        //Update Purchase Order exist in PO Receiving Detail
        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                System.out.println("----------------------SOA Detail---------------------- ");
                System.out.println("Source No : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceNo());
                System.out.println("Source Code : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSourceCode());
                System.out.println("------------------------------------------------------------------ ");

                switch (Detail(lnCtr).getSourceCode()) {
                    case SOATaggingStatic.PaymentRequest:
                        paPaymentRequest.add(PaymentRequest());
                        poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).InitTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).UpdateTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess(Logical.YES);
                        lbIsLinked = getLinkedPayment(paPaymentRequest.get(paPaymentRequest.size() - 1).Master().getTransactionNo(),Detail(lnCtr).getSourceCode()) ;
                        switch (status) {
                            case SOATaggingStatus.VOID:
                            case SOATaggingStatus.CANCELLED:
                            case SOATaggingStatus.RETURNED:
                                if(lbIsLinked){
                                    paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess(Logical.YES);
                                } else {
                                    paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess(Logical.NO);
                                }
                                
                                if(!isWithSOA(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode())){
                                    paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa(Logical.NO); 
                                }
                                break;
                            case SOATaggingStatus.CONFIRMED:
                                paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa(Logical.YES);
                                break;
                        }

                        break;
                    case SOATaggingStatic.POReceiving:
                        //Populate cache payable
                        paCachePayable.add(CachePayable());
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(getCachePayable(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode()));
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(true);
                        
                        
                        //Update PO Receiving 
                        //Nothing to update in PO Receiving
//                        paPOReceiving.add(PurchaseOrderReceiving());
//                        paPOReceiving.get(paPOReceiving.size() - 1).InitTransaction();
//                        paPOReceiving.get(paPOReceiving.size() - 1).OpenTransaction(Detail(lnCtr).getSourceNo());
//                        paPOReceiving.get(paPOReceiving.size() - 1).UpdateTransaction();
//                        paPOReceiving.get(paPOReceiving.size() - 1).Master().isProcessed(true);

                        switch (status) {
                            case SOATaggingStatus.VOID:
                            case SOATaggingStatus.CANCELLED:
                            case SOATaggingStatus.RETURNED:
                                lbIsLinked = getLinkedPayment(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode()) ;
                                paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(lbIsLinked);
                                if(!lbIsLinked){
                                    paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(false); 
                                } else {
                                    paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(isWithSOA(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode())); 
                                }
                                break;
                            case SOATaggingStatus.CONFIRMED:
                                paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(true);
                                break;
                        }
                        break;

                    case SOATaggingStatic.APPaymentAdjustment:
                        //Populate cache payable
                        paCachePayable.add(CachePayable());
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(getCachePayable(Detail(lnCtr).getSourceNo(), Detail(lnCtr).getSourceCode()));
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(true);
                        
                        //Update AP Payment Adjustment
                        paAPAdjustment.add(APPaymentAdjustment());
                        paAPAdjustment.get(paAPAdjustment.size() - 1).initialize();
                        poJSON = paAPAdjustment.get(paAPAdjustment.size() - 1).OpenTransaction(paCachePayable.get(paCachePayable.size() - 1).Master().getSourceNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        poJSON = paAPAdjustment.get(paAPAdjustment.size() - 1).UpdateTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        
                        lbIsLinked = getLinkedPayment(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode()) ;
                        switch (status) {
                            case SOATaggingStatus.VOID:
                            case SOATaggingStatus.CANCELLED:
                            case SOATaggingStatus.RETURNED:
                                paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(lbIsLinked);
                                paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(lbIsLinked);
                                if(!lbIsLinked){
                                    paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(false); 
                                } else {
                                    paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(isWithSOA(Detail(lnCtr).getSourceNo(),Detail(lnCtr).getSourceCode())); 
                                }
                                break;
                            case SOATaggingStatus.CONFIRMED:
                                paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(true);
                                paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(true);
                                break;
                        }
                        break;
                }
            }
        }

        //Update payable removed
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {

            System.out.println("----------------------Removed SOA Detail---------------------- ");
            System.out.println("Source No : " + (lnCtr + 1) + " : " + DetailRemove(lnCtr).getSourceNo());
            System.out.println("Source Code : " + (lnCtr + 1) + " : " + DetailRemove(lnCtr).getSourceCode());
            System.out.println("------------------------------------------------------------------ ");

            switch (DetailRemove(lnCtr).getSourceCode()) {
                case SOATaggingStatic.PaymentRequest:
                    paPaymentRequest.add(PaymentRequest());
                    poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).InitTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).OpenTransaction(DetailRemove(lnCtr).getSourceNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paPaymentRequest.get(paPaymentRequest.size() - 1).UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    lbIsLinked = getLinkedPayment(paPaymentRequest.get(paPaymentRequest.size() - 1).Master().getTransactionNo(),DetailRemove(lnCtr).getSourceCode());
                    if(!lbIsLinked){
                        paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setProcess(Logical.NO);
                        paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa(Logical.NO);
                    } else {
                        if(!isWithSOA(Detail(lnCtr).getSourceNo(),DetailRemove(lnCtr).getSourceCode())){
                            paPaymentRequest.get(paPaymentRequest.size() - 1).Master().setWithSoa(Logical.NO); 
                        }
                    }
                    
                    break;
                case SOATaggingStatic.POReceiving:
                    paCachePayable.add(CachePayable());
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(getCachePayable(DetailRemove(lnCtr).getSourceNo(), DetailRemove(lnCtr).getSourceCode()));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    lbIsLinked = getLinkedPayment(DetailRemove(lnCtr).getSourceNo(),DetailRemove(lnCtr).getSourceCode());
                    paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(lbIsLinked);
                    if(!lbIsLinked){
                        paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(false);
                    } else {
                        paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(isWithSOA(DetailRemove(lnCtr).getSourceNo(),DetailRemove(lnCtr).getSourceCode())); 
                    }
                    //Nothing to update in PO Receiving
//                    paPOReceiving.add(PurchaseOrderReceiving());
//                    paPOReceiving.get(paPOReceiving.size() - 1).InitTransaction();
//                    paPOReceiving.get(paPOReceiving.size() - 1).OpenTransaction(getCachePayable(DetailRemove(lnCtr).getSourceNo()));
//                    paPOReceiving.get(paPOReceiving.size() - 1).UpdateTransaction();
                        
                    break;
                case SOATaggingStatic.APPaymentAdjustment:
                    paCachePayable.add(CachePayable());
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).InitTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).OpenTransaction(getCachePayable(DetailRemove(lnCtr).getSourceNo(), DetailRemove(lnCtr).getSourceCode()));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paCachePayable.get(paCachePayable.size() - 1).UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    paAPAdjustment.add(APPaymentAdjustment());
                    paAPAdjustment.get(paAPAdjustment.size() - 1).initialize();
                    poJSON = paAPAdjustment.get(paAPAdjustment.size() - 1).OpenTransaction(DetailRemove(lnCtr).getSourceNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    poJSON = paAPAdjustment.get(paAPAdjustment.size() - 1).UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    lbIsLinked = getLinkedPayment(DetailRemove(lnCtr).getSourceNo(),DetailRemove(lnCtr).getSourceCode());
                    paCachePayable.get(paCachePayable.size() - 1).Master().setProcessed(lbIsLinked);
                    paAPAdjustment.get(paAPAdjustment.size() - 1).getModel().isProcessed(lbIsLinked);
                    if(!lbIsLinked){
                        paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(false);
                    } else {
                        paCachePayable.get(paCachePayable.size() - 1).Master().setWithSoa(isWithSOA(DetailRemove(lnCtr).getSourceNo(),DetailRemove(lnCtr).getSourceCode())); 
                    }
                    
                    break;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    private String getCachePayable(String fsSourceNo, String fsSourceCode){
        String lsTransactionNo = "";
        try {
            Model_Cache_Payable_Master object = new CashflowModels(poGRider).Cache_Payable_Master();
            String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(object),
                    " sSourceNo = " + SQLUtil.toSQL(fsSourceNo)
                   + " AND sSourceCd = " + SQLUtil.toSQL(fsSourceCode)
                   + " AND sClientID = " + SQLUtil.toSQL(Master().getClientId())); 
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (loRS.next()) {
                lsTransactionNo = loRS.getString("sTransNox");
                MiscUtil.close(loRS);
            } 
        } catch (SQLException e) {
            return lsTransactionNo;
        } 
            
        return lsTransactionNo;
    }

    private JSONObject saveUpdateOthers(String status)
            throws CloneNotSupportedException {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr;
        try {

            //Save Update Payment Request
            for (lnCtr = 0; lnCtr <= paPaymentRequest.size() - 1; lnCtr++) {
                paPaymentRequest.get(lnCtr).setWithParent(true);
//                paPaymentRequest.get(lnCtr).Master().setAmountPaid(getPayment(paPaymentRequest.get(lnCtr).Master().getTransactionNo(), false));  //Sa DV mag a update ng amount paid
                paPaymentRequest.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                paPaymentRequest.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poJSON = paPaymentRequest.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Save Payment Request " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

            //Save Update Cache Payable
            for (lnCtr = 0; lnCtr <= paCachePayable.size() - 1; lnCtr++) {
                paCachePayable.get(lnCtr).setWithParent(true);
//                paCachePayable.get(lnCtr).Master().setAmountPaid(getPayment(paCachePayable.get(lnCtr).Master().getTransactionNo(), false));  //Sa DV mag a update ng amount paid
                paCachePayable.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                paCachePayable.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poJSON = paCachePayable.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Save Cache Payable " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

            //Save Update PO Receiving
            for (lnCtr = 0; lnCtr <= paPOReceiving.size() - 1; lnCtr++) {
                paPOReceiving.get(lnCtr).setWithParent(true);
//                paPOReceiving.get(lnCtr).Master().setAmountPaid(getPayment(paPOReceiving.get(lnCtr).Master().getTransactionNo(), false)); //Sa DV mag a update ng amount paid
                paPOReceiving.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                paPOReceiving.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poJSON = paPOReceiving.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Save PO Receiving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

            //Save Update AP Payment Adjustment
            for (lnCtr = 0; lnCtr <= paAPAdjustment.size() - 1; lnCtr++) {
                paAPAdjustment.get(lnCtr).setWithParentClass(true);
                paAPAdjustment.get(lnCtr).isWithParent(true);
                paAPAdjustment.get(lnCtr).getModel().setModifyingBy(poGRider.Encrypt(poGRider.getUserID()));
                paAPAdjustment.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                poJSON = paAPAdjustment.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Save AP Payment Adjustment " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            System.out.println("Dept ID : " + poGRider.getDepartment());
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(SOATaggingStatus.OPEN);

        } catch (SQLException ex) {
            Logger.getLogger(SOATagging.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = " SELECT "
                + "   a.dTransact  "
                + " , a.sTransNox  "
                + " , a.sIndstCdx  "
                + " , a.sCompnyID  "
                + " , a.sClientID  "
                + " , a.sSOANoxxx  "
                + " , a.sCompnyID  "
                + " , b.sCompnyNm  AS sSupplrNm"
                + " , c.sCompnyNm  AS sCompnyNm"
                + " , d.sDescript  AS sIndustry"
                + " , e.sPayeeNme  AS sPayeeNme"
                + " FROM AP_Payment_Master a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sClientID "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx "
                + " LEFT JOIN Payee e ON e.sPayeeIDx = a.sIssuedTo ";
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
                + " , sum(b.nAmtAppld) AS nAppliedx"
                + " FROM Disbursement_Master a "
                + " LEFT JOIN Disbursement_Detail b ON b.sTransNox = a.sTransNox ";
    }

    public String getPayableSQL(String supplier, String company, String payee, String referenceNo) {
        String lsSQL = " SELECT        "
                + "   a.sTransNox "
                + " , a.sSourceNo AS sPayblNox"
                + " , a.dTransact "
                + " , IFNULL(a.dDueDatex,a.dTransact) AS dDueDatex "
                + " , a.sIndstCdx "
                + " , a.cTranStat "
                + " , a.nAmtPaidx "
                + " , a.sReferNox AS sReferenc  "
                + " , a.nNetTotal AS nPayblAmt  "
                + " , b.sCompnyNm AS sPayablNm  "
                + " , c.sCompnyNm AS sCompnyNm  "
                + " , a.sSourceCd AS sPayablTp  "
                + " FROM Cache_Payable_Master a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sClientID "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " WHERE "
                + " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.cTranStat = " + SQLUtil.toSQL(CachePayableStatus.CONFIRMED) 
                + " AND a.nAmtPaidx < a.nNetTotal "
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                + " AND a.sReferNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                + " AND a.cProcessd = " + SQLUtil.toSQL(Logical.NO)
                + " UNION  "
                + " SELECT "
                + "   a.sTransNox "
                + " , a.sTransNox AS sPayblNox"
                + " , a.dTransact "
                + " , a.dTransact AS dDueDatex "
                + " , a.sIndstCdx "
                + " , a.cTranStat "
                + " , a.nAmtPaidx "
                + " , a.sSeriesNo AS sReferenc  "
                + " , a.nTranTotl AS nPayblAmt    "
                + " , b.sPayeeNme AS sPayablNm    "
                + " , c.sCompnyNm AS sCompnyNm  "
                + " ,  " + SQLUtil.toSQL(SOATaggingStatic.PaymentRequest) + " AS sPayablTp  "
                + " FROM Payment_Request_Master a "
                + " LEFT JOIN Payee b ON b.sPayeeIDx = a.sPayeeIDx "
                + " LEFT JOIN Client_Master bb ON bb.sClientID = b.sAPClntID "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " WHERE "
                + " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.cTranStat = " + SQLUtil.toSQL(PaymentRequestStatus.CONFIRMED)
                + " AND a.nAmtPaidx < a.nTranTotl "
//                + " AND ( bb.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
//                + " OR b.sClientID IS NULL OR b.sClientID = '' )" 
//                + " AND b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee)
                + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                + " AND a.sSeriesNo LIKE " + SQLUtil.toSQL("%" + referenceNo)
                + " AND a.cProcessd = " + SQLUtil.toSQL(Logical.NO);
        
                if((supplier == null || "".equals(supplier)) || (payee == null || "".equals(payee))){
                    if(supplier != null && !"".equals(supplier)){
                        lsSQL = lsSQL + " AND IFNULL(bb.sCompnyNm,b.sPayeeNme) LIKE " + SQLUtil.toSQL("%" + supplier);
//                                    + " OR b.sClientID IS NULL OR b.sClientID = '' " ;
                    }
                    if(payee != null && !"".equals(payee)){
                        lsSQL = lsSQL + " AND b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee) ;
                    }
                } else {
                    lsSQL = lsSQL + " AND ( IFNULL(bb.sCompnyNm,b.sPayeeNme) LIKE " + SQLUtil.toSQL("%" + supplier)
                                + " OR b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee)
                                + " ) ";
                } 
                
        return lsSQL;
    }
    
    public String getCachePayableSQL(String supplier, String company, String payee, String referenceNo, String payableType) {
        return " SELECT        "
                + "   a.sTransNox "
                + " , a.sSourceNo AS sPayblNox"
                + " , a.dTransact "
                + " , IFNULL(a.dDueDatex,a.dTransact) AS dDueDatex "
                + " , a.sIndstCdx "
                + " , a.cTranStat "
                + " , a.nAmtPaidx "
                + " , a.sReferNox AS sReferenc  "
                + " , a.nNetTotal AS nPayblAmt  "
                + " , b.sCompnyNm AS sPayablNm  "
                + " , c.sCompnyNm AS sCompnyNm  "
                + " , a.sSourceCd AS sPayablTp  "
                + " FROM Cache_Payable_Master a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sClientID "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " WHERE "
                + " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.cTranStat = " + SQLUtil.toSQL(CachePayableStatus.CONFIRMED) 
                + " AND a.nAmtPaidx < a.nNetTotal "
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                + " AND a.sReferNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                + " AND a.sSourceCd = " + SQLUtil.toSQL(payableType)
                + " AND a.cProcessd = " + SQLUtil.toSQL(Logical.NO);
    }
    
    public String getPRFSQL(String supplier, String company, String payee, String referenceNo) {
        String lsSQL = " SELECT        "
                + "   a.sTransNox "
                + " , a.sTransNox AS sPayblNox"
                + " , a.dTransact "
                + " , a.dTransact AS dDueDatex "
                + " , a.sIndstCdx "
                + " , a.cTranStat "
                + " , a.nAmtPaidx "
                + " , a.sSeriesNo AS sReferenc  "
                + " , a.nTranTotl AS nPayblAmt    "
                + " , b.sPayeeNme AS sPayablNm    "
                + " , c.sCompnyNm AS sCompnyNm  "
                + " ,  " + SQLUtil.toSQL(SOATaggingStatic.PaymentRequest) + " AS sPayablTp  "
                + " FROM Payment_Request_Master a "
                + " LEFT JOIN Payee b ON b.sPayeeIDx = a.sPayeeIDx "
                + " LEFT JOIN Client_Master bb ON bb.sClientID = b.sClientID "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN PO_Master e ON e.sTransNox = a.sSourceNo "
                + " WHERE "
                + " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.cTranStat = " + SQLUtil.toSQL(PaymentRequestStatus.CONFIRMED)
                + " AND a.nAmtPaidx < a.nTranTotl "
//                + " AND ( bb.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
//                + " OR b.sClientID IS NULL OR b.sClientID = '' ) " 
//                + " AND b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee)
                + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                + " AND a.sSeriesNo LIKE " + SQLUtil.toSQL("%" + referenceNo)
                + " AND a.cProcessd = " + SQLUtil.toSQL(Logical.NO)
                + " AND (e.sTermCode IS NULL OR e.sTermCode = '' OR e.sTermCode != 'M0W2003')"; //Not equal to COD / Cash on delivery hardcoded according to sir mac and ma'am grace 09-03-2026;
                
                if((supplier == null || "".equals(supplier)) || (payee == null || "".equals(payee))){
                    if(supplier != null && !"".equals(supplier)){
                        lsSQL = lsSQL + " AND ( bb.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                                    + " OR b.sClientID IS NULL OR b.sClientID = '' ) " ;
                    }
                    if(payee != null && !"".equals(payee)){
                        lsSQL = lsSQL + " AND b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee) ;
                    }
                } else {
                    lsSQL = lsSQL + " AND ( bb.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                                + " OR b.sPayeeNme LIKE " + SQLUtil.toSQL("%" + payee)
                                + " ) ";
                }
                
                        
        return lsSQL;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        poJSON = new JSONObject();

        GValidator loValidator = new APPaymentValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case SOATaggingStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case SOATaggingStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case SOATaggingStatus.PROCESSED:
                    crs.updateString("cRefrStat", "PROCESSED");
                    break;
                case SOATaggingStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case SOATaggingStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case SOATaggingStatus.PAID:
                    crs.updateString("cRefrStat", "PAID");
                    break;
                case SOATaggingStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case SOATaggingStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case SOATaggingStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case SOATaggingStatus.PROCESSED:
                            crs.updateString("cRefrStat", "PROCESSED");
                            break;
                        case SOATaggingStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case SOATaggingStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case SOATaggingStatus.PAID:
                            crs.updateString("cRefrStat", "PAID");
                            break;
                        case SOATaggingStatus.RETURNED:
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
        
        showStatusHistoryUI("SOA Tagging", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM AP_Payment_Master a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
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
