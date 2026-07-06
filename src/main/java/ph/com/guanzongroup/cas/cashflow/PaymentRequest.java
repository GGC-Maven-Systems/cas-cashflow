package ph.com.guanzongroup.cas.cashflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.Model_Transaction_Attachment;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.SysTableModels;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payee;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Recurring_Expense_Payment_Monitor;
import ph.com.guanzongroup.cas.cashflow.model.Model_Recurring_Issuance;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStaticData;
import static ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStaticData.recurring_expense_payment;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.validator.PaymentRequestValidator;

public class PaymentRequest extends Transaction {
    private String psIndustryId = "";
    private String psCompanyId = "";
    
    List<TransactionAttachment> paAttachments;
    List<Model_PO_Master> paPOMaster;
    List<Model_Payment_Request_Master> poPRFMaster;
    List<RecurringIssuance> poRecurringIssuances;

    /**
     * Initializes payment request models and working lists for transaction use.
     *
     * @return JSON result from transaction initialization.
     */
    public JSONObject InitTransaction() {
        SOURCE_CODE = "PRFx";

        poMaster = new CashflowModels(poGRider).PaymentRequestMaster();
        poDetail = new CashflowModels(poGRider).PaymentRequestDetail();
        paDetail = new ArrayList<>();
        paAttachments = new ArrayList<>();
        paPOMaster = new ArrayList<>();

        return initialize();
    }
    
    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, CloneNotSupportedException, GuanzonException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }
    
    /**
     * Confirms the current transaction and applies updates to linked recurring records.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {

        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.CONFIRMED;
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
        poJSON = isEntryOkay(PaymentRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
               return poJSON; 
            }
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
            setApproving((String) poJSON.get("sUserIDxx"));
        }
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Arsiela 02-26-2026
        List<String> laRecurringObj = new ArrayList<>();
        Model_Recurring_Expense_Payment_Monitor loObject = new CashflowModels(poGRider).Recurring_Expense_Payment_Monitor();
        if(Detail(getDetailCount() - 1).getRecurringNo() != null && !"".equals(Detail(getDetailCount() - 1).getRecurringNo())){
            for(int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++){
                if(Detail(lnCtr).getRecurringNo() != null && !"".equals(Detail(lnCtr).getRecurringNo()) && Detail(lnCtr).isReverse()){
                    poJSON = checkExistingPRF(Detail(lnCtr).getRecurringNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    if(!laRecurringObj.contains(Detail(lnCtr).getRecurringNo())){
                        laRecurringObj.add(Detail(lnCtr).getRecurringNo());
                    }
                }
                
            }
        } else {
            switch(Master().getSourceCode()){
                case recurring_expense_payment:  
                    if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
                        poJSON = checkExistingPRF(Master().getSourceNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        
                        if(!laRecurringObj.contains(Master().getSourceNo())){
                            laRecurringObj.add(Master().getSourceNo());
                        }
                    }
                break;
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Arsiela 02-27-2026
        for(int lnCtr = 0; lnCtr < laRecurringObj.size(); lnCtr++){
            poJSON = loObject.openRecord(laRecurringObj.get(lnCtr)); 
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poJSON = loObject.updateRecord();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            loObject.setModifiedDate(poGRider.getServerDate());
            loObject.setBatchNo(Master().getTransactionNo());
            poJSON = loObject.saveRecord();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = saveUpdates();
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

    /**
     * Marks the current payment request as paid.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject PaidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {

        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.PAID;
        boolean lbConfirm = true;

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
        poJSON = isEntryOkay(PaymentRequestStatus.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction Paid successfully.");
        } else {
            poJSON.put("message", "Transaction Paid request submitted successfully.");
        }

        return poJSON;
    }

    /**
     * Cancels the current payment request and performs dependency checks.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.CANCELLED;
        boolean lbConfirm = true;

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

        poJSON = isEntryOkay(PaymentRequestStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Arsiela 02-26-2026
        List<String> laRecurringObj = new ArrayList<>();
        Model_Recurring_Expense_Payment_Monitor loObject = new CashflowModels(poGRider).Recurring_Expense_Payment_Monitor();
        if(Detail(getDetailCount() - 1).getRecurringNo() != null && !"".equals(Detail(getDetailCount() - 1).getRecurringNo())){
            for(int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++){
                if(Detail(lnCtr).getRecurringNo() != null && !"".equals(Detail(lnCtr).getRecurringNo())){
                    poJSON = checkExistingPRF(Detail(lnCtr).getRecurringNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    if(!laRecurringObj.contains(Detail(lnCtr).getRecurringNo())){
                        laRecurringObj.add(Detail(lnCtr).getRecurringNo());
                    }
                }
                
            }
        } else {
            switch(Master().getSourceCode()){
                case recurring_expense_payment:  
                    if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
                        poJSON = checkExistingPRF(Master().getSourceNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        
                        if(!laRecurringObj.contains(Master().getSourceNo())){
                            laRecurringObj.add(Master().getSourceNo());
                        }
                    }
                break;
            }
        }
        
        if (PaymentRequestStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Arsiela 02-27-2026
        for(int lnCtr = 0; lnCtr < laRecurringObj.size(); lnCtr++){
            poJSON = loObject.openRecord(laRecurringObj.get(lnCtr)); 
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poJSON = loObject.updateRecord();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            loObject.setModifiedDate(poGRider.getServerDate());
            loObject.setBatchNo(null);
            poJSON = loObject.saveRecord();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    /**
     * Voids the current payment request transaction.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.VOID;
        boolean lbConfirm = true;

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

        poJSON = isEntryOkay(PaymentRequestStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (PaymentRequestStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    /**
     * Posts the current payment request transaction.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.POSTED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already posted.");
            return poJSON;
        }

        poJSON = isEntryOkay(PaymentRequestStatus.POSTED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    /**
     * Returns the current transaction to a previous workflow status.
     *
     * @param remarks Remarks for status history.
     * @return JSON result containing status and message.
     * @throws ParseException If date parsing fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PaymentRequestStatus.RETURNED;
        boolean lbConfirm = true;

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

        poJSON = isEntryOkay(PaymentRequestStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (PaymentRequestStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction return request submitted successfully.");
        }

        return poJSON;
    }

    /**
     * Adds a new detail row after validating the previous row is complete.
     *
     * @return JSON result containing status and message.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if ((Detail(getDetailCount() - 1).getParticularID() == null || "".equals(Detail(getDetailCount() - 1).getParticularID())) && Detail(getDetailCount() - 1).getAmount() == 0.0000) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }
        return addDetail();
    }
    
    /*Validate*/
    /**
     * Reverses or removes a detail row based on duplicate particular detection.
     *
     * @param row Detail row index to process.
     */
    public void ReverseItem(int row){
        int lnExist = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(lnCtr != row){
                if(Detail(row).getParticularID().equals(Detail(lnCtr).getParticularID())){
                    lnExist++;
                    break; 
                }
            }
        }
        
        if(lnExist >= 1){
            Detail().remove(row);
        } else {
            Detail(row).isReverse(false);
        }
    }

    private TransactionAttachment TransactionAttachment() throws SQLException, GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }

    /**
     * Adds a new blank transaction attachment entry.
     *
     * @return JSON result containing status and message.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Removes or deactivates an attachment entry by row index.
     *
     * @param fnRow Attachment row index.
     * @return JSON result containing status and message.
     * @throws GuanzonException If attachment model operations fail.
     * @throws SQLException If a database access error occurs.
     */
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction attachment to be removed.");
            return poJSON;
        }
        
        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
            System.out.println("Attachment :"+ fnRow+" Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :"+ fnRow+" Deactivate");
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Adds or reactivates an attachment record for the provided file name.
     *
     * @param fFileName Attachment file name.
     * @return Row index of the active attachment entry.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :"+ lnCtr+" Activate");
                return lnCtr;
            }
        }
        
        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(Master().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }
    
    /**
     * Copies a local file into the temporary attachment directory.
     *
     * @param fsPath Source file path.
     */
    public void copyFile(String fsPath){
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads and downloads existing attachments of the current transaction.
     *
     * @return JSON result containing status and message.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, Master().getTransactionNo());
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());
            }
            
            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                    , SOURCE_CODE
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if ("success".equals((String) poJSON.get("result"))) {
                
                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println("File downloaded succesfully.");
                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                    System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                }
                
            } else {
                System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }
        return poJSON;
    }
    
    /**
     * Arsiela - 02-27-2026
     * Populate Recurring Expense based on Recurring Expense Monitor
     * @param fsRecurringTransNo
     * @return
     * @throws CloneNotSupportedException
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject populateRecurringDetail(String fsRecurringTransNo) throws CloneNotSupportedException, SQLException, GuanzonException{
        poJSON = new JSONObject();
        if(fsRecurringTransNo == null || "".equals(fsRecurringTransNo)){
            poJSON.put("result", "error");
            poJSON.put("message", "No recurring detail to load.");
            return poJSON;
        }
        
        if(pnEditMode == EditMode.UPDATE){
            if(Detail(0).getRecurringNo() == null || "".equals(Detail(0).getRecurringNo())){
                if(!PaymentRequestStaticData.recurring_expense_payment.equals(Master().getSourceCode())){    
                    poJSON.put("result", "error");
                    poJSON.put("message", "Recurring expense schedule cannot be mix with non recurring expense transaction source.");
                    return poJSON;
                } else {
                    Detail(0).setRecurringNo(Master().getSourceNo());
                }
            }
        }
        
        int lnError = 0; //Use for handling error code
        String lsRecurringNo = "";
        boolean lbExist = false;
        boolean lbAddedNew = false;
        ArrayList<String> laTransNo = new ArrayList<>(Arrays.asList(fsRecurringTransNo.split(",")));
        for(int lnCtr = 0; lnCtr < laTransNo.size(); lnCtr++){
            Model_Recurring_Expense_Payment_Monitor loObject = new CashflowModels(poGRider).Recurring_Expense_Payment_Monitor();
            poJSON = loObject.openRecord(laTransNo.get(lnCtr));
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            lsRecurringNo = laTransNo.get(lnCtr);
            
            //Check Existing Recurring No
            for(int lnRow = 0; lnRow < getDetailCount(); lnRow++){
                if(Detail(lnRow).getParticularID() != null && !"".equals(Detail(lnRow).getParticularID())){
                    if(Master().getPayeeID() != null && !"".equals(Master().getPayeeID())){
                        if(Detail(lnRow).getRecurringNo() != null && !"".equals(Detail(lnRow).getRecurringNo())){
                            if(!Master().getPayeeID().equals(Detail(lnRow).RecurringExpensePaymentMonitor().RecurringExpenseSchedule().getPayeeId())){
                                lnError = 1; //Recurring schedule payee must be equal to the payment request payee.
                                break;
                            }
                            if(Detail(lnRow).RecurringExpensePaymentMonitor().getBillMonth() != loObject.getBillMonth()){
                                lnError = 2; //Bill month must be the same with the existing recurring expense in PRF detail.
                                break;
                            }
                            if(Detail(lnRow).RecurringExpensePaymentMonitor().RecurringExpenseSchedule().getDueDay() != loObject.RecurringExpenseSchedule().getDueDay()){
                                lnError = 3; //Due day must be the same with the existing recurring expense in PRF detail.
                                break;
                            }
                        } else {
                            if(!PaymentRequestStaticData.recurring_expense_payment.equals(Master().getSourceCode())){    
                                lnError = 4; //Recurring expense schedule cannot be mix with non recurring expense transaction source.
                                break;
                            }
                            if(!Master().getPayeeID().equals(loObject.RecurringExpenseSchedule().getPayeeId())){
                                lnError = 1; //Recurring schedule payee must be equal to the payment request payee.
                            }
                            if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
                                if(Master().RecurringExpensePaymentMonitor().getBillMonth() != loObject.getBillMonth()){
                                    lnError = 2; //Bill month must be the same with the existing recurring expense in PRF detail.
                                }
                                if(Master().RecurringExpensePaymentMonitor().RecurringExpenseSchedule().getDueDay() != loObject.RecurringExpenseSchedule().getDueDay()){
                                    lnError = 3; //Due day must be the same with the existing recurring expense in PRF detail.
                                    break;
                                }
                            }
                        }
                    }
                }
                
                lbExist = Detail(lnRow).getRecurringNo().equals(lsRecurringNo) || Master().getSourceNo().equals(lsRecurringNo);
                if(lbExist){
                    if(!Detail(lnRow).isReverse()){
                        Detail(lnRow).isReverse(true);
                        lbAddedNew = true;
                    }
                    break;
                } 
            }
            
            //Check if there's a error match
            switch(lnError){
                case 1:
                    poJSON.put("result", "error");
                    poJSON.put("message", "Recurring schedule payee must be equal to the payment request payee.");
                    return poJSON;
                case 2:
                    poJSON.put("result", "error");
                    poJSON.put("message", "Bill month must be the same with the existing recurring expense in PRF detail.");
                    return poJSON;
                case 3:
                    poJSON.put("result", "error");
                    poJSON.put("message", "Due day must be the same with the existing recurring expense in PRF detail.");
                    return poJSON;
                case 4:
                    poJSON.put("result", "error");
                    poJSON.put("message", "Recurring expense schedule cannot be mix with non recurring expense transaction source.");
                    return poJSON;
            }
            
            if(!lbExist){
                AddDetail();
                Detail(getDetailCount() - 1).isReverse(true);
                Detail(getDetailCount() - 1).setRecurringNo(lsRecurringNo);
                Detail(getDetailCount() - 1).setParticularID(
                Detail(getDetailCount() - 1).RecurringExpensePaymentMonitor().RecurringExpenseSchedule().RecurringExpense().getParticularId());
                Detail(getDetailCount() - 1).setAmount(Detail(getDetailCount() - 1).RecurringExpensePaymentMonitor().RecurringExpenseSchedule().getAmount());
                Master().setPayeeID(Detail(getDetailCount() - 1).RecurringExpensePaymentMonitor().RecurringExpenseSchedule().getPayeeId());
                
                if(getDetailCount() <= 1){
                    Master().setSource(lsRecurringNo);
                    Master().setSourceCode("REPM");
                } else {
                    Master().setSource("");
                    Master().setSourceCode("");
                }
                
                lbAddedNew = true;
            }
            
            lbExist = false;//Set false by default
        }
        
        if(!lbAddedNew){
            poJSON.put("result", "error");
            poJSON.put("message", "All remaining recurring expense already added.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /** Arsiela 02-27-2026
     * Check Existing Recurring No PRF
     * @param recurringNo
     * @return 
     */
    private JSONObject checkExistingPRF(String recurringNo){
        poJSON = new JSONObject();
        try {
            String lsSQL = "SELECT " +
                            "  a.sTransNox " +
                            ", a.sSeriesNo " +
                            ", a.sSourceNo " +
                            ", a.sSourceCd " +
                            ", a.cTranStat " +
                            ", b.sRecurrNo " +
                            ", b.sPrtclrID " +
                            ", d.sRecurrNo AS xRecurringNo " +
                            ", c.sDescript AS sPrtclrDc " +
                            " FROM Payment_Request_Master a " +
                            " LEFT JOIN Payment_Request_Detail b ON b.sTransNox = a.sTransNox " +
                            " LEFT JOIN  Particular c ON c.sPrtclrID = b.sPrtclrID " +
                            " LEFT JOIN Recurring_Expense_Payment_Monitor d ON (d.sTransNox = b.sRecurrNo OR (d.sTransNox  = a.sSourceNo AND a.sSourceCd = "+ SQLUtil.toSQL(PaymentRequestStaticData.recurring_expense_payment) + ")) ";
            lsSQL = MiscUtil.addCondition(lsSQL, 
                                " a.sTransNox != " + SQLUtil.toSQL(Master().getTransactionNo())
                                + " AND ( b.sRecurrNo = " + SQLUtil.toSQL(recurringNo)
                                + " OR ( a.sSourceNo = " + SQLUtil.toSQL(recurringNo)
                                + " AND a.sSourceCd = " + SQLUtil.toSQL(PaymentRequestStaticData.recurring_expense_payment)
                                + ")) AND a.cTranStat != " + SQLUtil.toSQL(PaymentRequestStatus.CANCELLED)
                                + " AND a.cTranStat != " + SQLUtil.toSQL(PaymentRequestStatus.VOID)
                                + " AND b.cReversex = " + SQLUtil.toSQL(PaymentRequestStaticData.Reverse.INCLUDE));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) > 0) {
                if (loRS.next()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Recurring no " + loRS.getString("xRecurringNo") + " is already exists in PRF " + loRS.getString("sSeriesNo"));
                    MiscUtil.close(loRS);
                    return poJSON;
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox,"
                + " a.dTransact,"
                + " a.sSeriesNo,"
                + " b.sBranchNm,"
                + " c.sDeptName,"
                + " d.sPayeeNme,"
                + " b.sBranchCd,"
                + " c.sDeptIDxx,"
                + " d.sPayeeIDx"
                + " FROM Payment_Request_Master a "
                + " LEFT JOIN Branch b ON a.sBranchCd = b.sBranchCd "
                + " LEFT JOIN Department c ON c.sDeptIDxx = a.sDeptIDxx "
                + " LEFT JOIN Payee d ON a.sPayeeIDx = d.sPayeeIDx";
    }

    /**
     * Opens a transaction selected from browse results.
     *
     * @param fsValue Search keyword for browse.
     * @return JSON result containing status and message.
     * @throws CloneNotSupportedException If detail cloning fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject SearchTransaction(String fsValue) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " ( a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%")
                                                    + " OR a.sSourceNo LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%") //add source no in condition requested by ma'am she - Arsiela 05-22-2026
                                                    + " ) "
                                                        );
        String lsFilterAll = "";
        if (psIndustryId != null && !"".equals(psIndustryId)) {
            lsFilterAll += " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
        }
        if (psCompanyId != null && !"".equals(psCompanyId)) {
            lsFilterAll += " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId);
        }
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!lsFilterAll.isEmpty() && !"".equals(lsFilterAll)) {
            lsSQL = lsSQL + lsFilterAll;
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Series No»Branch»Payee",
                "a.dTransact»a.sTransNox»a.sSeriesNo»b.sBranchNm»d.sPayeeNme",
                "a.dTransact»a.sTransNox»a.sSeriesNo»b.sBranchNm»d.sPayeeNme",
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
    
    
    /** - Added by Arsiela 05-16-2026
     * Searches for an active Branch record and updates the local branch state.
     * 
     * @param value The search criteria.
     * @param byCode {@code true} to search by branch code.
     * @return A {@link JSONObject} containing the search result.
     * @throws ExceptionInInitializerError, SQLException, GuanzonException If search fails.
     */
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
//        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
//        object.setRecordStatus(RecordStatus.ACTIVE);
//        poJSON = object.searchRecord(value, byCode);
//        if ("success".equals((String) poJSON.get("result"))) {
//            Master().setBranchCode(object.getModel().getBranchCode());
//        }
//
//        return poJSON;
        //Filter branch according to record status and company - request by ma'am she 05-18-2026 9:50AM
        String lsSQL = MiscUtil.addCondition("SELECT sBranchCD, sBranchNm, sCompnyID FROM Branch ", 
                                                " cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                                + " AND sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                                            );
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Branch Code»Branch Name",
                "sBranchCD»sBranchNm",
                "sBranchCD»sBranchNm",
                byCode ? 0 : 1);

        if (poJSON != null) {
            Master().setBranchCode((String) poJSON.get("sBranchCD"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            Master().setBranchCode("");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject SearchDepartment(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDepartmentID(object.getModel().getDepartmentId());
        }

        return poJSON;
    }

    public JSONObject SearchPayee(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setPayeeID(object.getModel().getPayeeID());
        }

        return poJSON;
    }

    /**
     * Searches a particular and assigns it to the requested detail row.
     *
     * @param value Search value.
     * @param byCode {@code true} when searching by code.
     * @param row Detail row index.
     * @return JSON result containing status, message, and row.
     * @throws ExceptionInInitializerError If search controller initialization fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject SearchParticular(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Particular object = new CashflowControllers(poGRider, logwrapr).Particular();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            int lnRowCount = 0;
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if(Detail(lnRow).isReverse()){
                    lnRowCount++;
                }
                if (lnRow != row) {
                    if (Detail(lnRow).getParticularID().equals(object.getModel().getParticularID())) {
                        if(!Detail(lnRow).isReverse()){
                            Detail(lnRow).isReverse(true);
                            poJSON.put("row", lnRow);
                            return poJSON;
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Particular: " + object.getModel().getDescription() + " already exist in table at row " + lnRowCount + ".");
                            poJSON.put("row", lnRow);
                            return poJSON;
                        }
                    }
                }
            }
            Detail(row).setParticularID(object.getModel().getParticularID());
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Payment_Request_Master Master() {
        return (Model_Payment_Request_Master) poMaster;
    }

    @Override
    public Model_Payment_Request_Detail Detail(int row) {
        return (Model_Payment_Request_Detail) paDetail.get(row);
    }
    
    /**
     * Cleans and rebuilds detail rows to keep the detail list valid.
     *
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getParticularID() == null || "".equals(Detail(lnCtr).getParticularID())
                && (Detail(lnCtr).getAmount() == 0.0000)){
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    deleteDetail(lnCtr); 
                }
            }
            lnCtr--;
        }
        
        if(PaymentRequestStaticData.recurring_expense_payment.equals(Master().getSourceCode())){
            return;
        }

        if ((getDetailCount() - 1) >= 0) {
            if(Detail(0).getRecurringNo() != null && !"".equals(Detail(0).getRecurringNo())){
                return;
            }
            
            if (((Detail(getDetailCount() - 1).getParticularID() != null && !"".equals(Detail(getDetailCount() - 1).getParticularID()))
                || !Detail(getDetailCount() - 1).isReverse())
                && Detail(getDetailCount() - 1).getAmount() > 0.0000){
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }

    @Override
    /**
     * Runs save-time validations and applies final field assignments.
     *
     * @return JSON result containing validation/save-precheck status.
     * @throws CloneNotSupportedException If detail cloning fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject willSave() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        //Re update transaction no
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        
        //remove items with no stockid or quantity order
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions
            double amount = Double.parseDouble(String.valueOf(item.getValue("nAmountxx")));
            if (amount <= 0.0000 || ( item.getEditMode() == EditMode.ADDNEW && "-".equals((String) item.getValue("cReversex")) )) {
                detail.remove(); // Correctly remove the item
            }
        }
        
        if(getDetailCount() <= 0 ){
            AddDetail();
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }
        
        if(Master().getNetTotal() <= 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid net total.");
            return poJSON;
        }
        
        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getAmount() == 0.00) {
                poJSON.put("result", "error");
                poJSON.put("message", "Particular has 0 amount.");
                return poJSON;
            }
        }
        if (PaymentRequestStatus.RETURNED.equals(Master().getTransactionStatus())) {
            boolean lbUpdated = false;
            PaymentRequest loRecord = new CashflowControllers(poGRider, null).PaymentRequest();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

            lbUpdated = loRecord.getDetailCount() == getDetailCount();
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getPayeeID().equals(Master().getPayeeID());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTranTotal() == Master().getTranTotal();
            }
            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getParticularID().equals(Detail(lnCtr).getParticularID());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getAmount() == Detail(lnCtr).getAmount();
                    }
                    if (!lbUpdated) {
                        break;
                    }
                }
            }

            if (lbUpdated) {
                poJSON.put("result", "error");
                poJSON.put("message", "No update has been made.");
                return poJSON;
            }

            Master().setTransactionStatus(PaymentRequestStatus.OPEN); //If edited update trasaction status into open
        }
        
        //Arsiela 02-26-2026 functionality for recurring expense and purchase order payments
        switch(Master().getSourceCode()){
            case InvTransCons.PURCHASE_ORDER:
                Model_PO_Master loObject = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
                poJSON = loObject.openRecord(Master().getSourceNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                double ldblBalance = loObject.getNetTotal().doubleValue() - loObject.getAmountPaid().doubleValue();
                if(ldblBalance < Master().getTranTotal()){
                    poJSON.put("result", "error");
                    poJSON.put("message", "PRF Net total cannot be greater than the purchase order balance.");
                    return poJSON;
                }
        
        }
        
        if(Detail(0).getRecurringNo() != null && !"".equals(Detail(0).getRecurringNo())){
            if(getDetailCount() > 1){
                Master().setSourceNo("");
                Master().setSourceCode("");
            } else {
                Master().setSourceNo(Detail(0).getRecurringNo());
                Master().setSourceCode(PaymentRequestStaticData.recurring_expense_payment);
                poJSON = checkExistingPRF(Master().getSourceNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                Detail(getDetailCount()-1).setRecurringNo("");
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            
            if(Detail(lnCtr).isReverse()){
                if(Detail(lnCtr).getRecurringNo() != null && !"".equals(Detail(lnCtr).getRecurringNo())){
                    poJSON = checkExistingPRF(Detail(lnCtr).getRecurringNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
        }

        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(Master().getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));
            
            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()
                && "0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                while ("error".equals((String)checkExistingFileName(lsNewFileName).get("result"))) {
                    lnCopies++;
                    //Rename the file
                    int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                    if (dotIndex == -1) {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() +"_"+lnCopies;
                    } else {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) +"_"+ lnCopies +TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                    }
                }

                if(lnCopies > 0){
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments")).resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        //check if file is existing
                        int lnChecker = 0;
                        File file = new File(TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + lsNewFileName);
                        while(!file.exists() && lnChecker < 5){
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);  
                            System.out.println("Re-Copying... " + lnChecker);
                            lnChecker++;
                        }
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            //Upload Attachment when send status is 0
            try {
                if("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr,lsOriginalFileName);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            }
            
        }

        if (PaymentRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = setValueToOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        return isEntryOkay(PaymentRequestStatus.OPEN);
    }
    
    /**
     * Checks whether a file name already exists in attachment records.
     *
     * @param fsFileName File name to validate.
     * @return JSON result indicating duplicate detection.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()), 
                                                                    " sFileName = " + SQLUtil.toSQL(fsFileName)
                                                                    );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))){
                        poJSON.put("result", "error");
                        poJSON.put("message", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }
    
    /**
     * Upload Attachment
     * @param instance
     * @param access 
     * @param fnRow 
     * @return  
     * @throws java.lang.Exception 
     */
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception{       
        poJSON = new JSONObject();
        System.out.println("Uploading... : fsOriginalFileName : " + fsOriginalFileName);
        System.out.println("New File Name... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        String lsFile = paAttachments.get(fnRow).getModel().getFileName();
        
        //check if new file is existing
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        if(!file.exists()){
            //check if original file is existing
            lsFile = fsOriginalFileName;
            file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
            if(!file.exists()){
                poJSON.put("result", "error");
                poJSON.put("message", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
                                        + ".\nContact system administrator for assistance.");
                return poJSON;  
            }
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if(paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())){
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access)
                                , "0032"
                                , ""
                                , paAttachments.get(fnRow).getModel().getFileName()
                                , instance.getBranchCode()
                                , hash
                                , encodeFileToBase64Binary(file)
                                , paAttachments.get(fnRow).getModel().getSourceCode()
                                , paAttachments.get(fnRow).getModel().getSourceNo()
                                , "");

        if("error".equalsIgnoreCase((String) result.get("result"))){
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
            poJSON.put("result", "error");
            poJSON.put("message", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
                                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Encodes a file into Base64 string content for upload.
     *
     * @param file File to encode.
     * @return Base64-encoded content.
     * @throws Exception If file reading fails.
     */
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
    } 
    
    private static JSONObject token = null;
    /**
     * Loads and refreshes API access token from a local token file.
     *
     * @param access Token file path.
     * @return Access key value, or {@code null} when unavailable.
     */
    private static String getAccessToken(String access){
        try {
            JSONParser oParser = new JSONParser();
            if(token == null){
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created") , SQLUtil.FORMAT_TIMESTAMP));
            
            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if(current_date.after(date_created)){
                String[] xargs = new String[] {(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            return (String)token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }

    @Override
    /**
     * Saves related child records after the master transaction is saved.
     *
     * @return JSON result containing status.
     */
    public JSONObject saveOthers() {
        poJSON = new JSONObject();
        try {
            //Save Attachments
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    System.out.println("CHECK ATTACHMENT RECORD STAT : " + paAttachments.get(lnCtr).getModel().getRecordStatus());
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }

        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = new PaymentRequestValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    private Model_PO_Master PurchaseOrderMaster() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }

    public Model_PO_Master Payable(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }

    public int getPayableCount() {
        if (paPOMaster == null) {
            return 0;
        }
        return paPOMaster.size();
    }
    
    /**
     * Loads payable purchase orders for the current branch/payee filters into the local payable list.
     *
     * @return JSON result with load status and message.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If PO model operations fail.
     */
    public JSONObject loadPayables() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsCompanyId = poGRider.getCompnyId();
        String lsIndustryId = poGRider.getIndustry();
        paPOMaster = new ArrayList<>();
       
        if (psIndustryId != null && !"".equals(psIndustryId)) {
            lsIndustryId = psIndustryId;
        }
        if (psCompanyId != null && !"".equals(psCompanyId)) {
            lsCompanyId = psCompanyId;
        }
        
        String lsSQL = " SELECT "
                        + "   a.sTransNox "
                        + " , a.dTransact "
                        + " , a.cTranStat "
                        + " , a.sBranchCd "
                        + " , b.sDescript AS xIndustry "
                        + " , c.sBranchNm AS xBranchNm "
                        + " , d.sDescript AS xCategory "
                        + " , e.sCompnyNm AS xCompnyNm " 
                        + " , f.sPayeeNme AS xPayeeNme "
                        + " FROM PO_Master a           "
                        + " LEFT JOIN Industry b ON b.sIndstCdx = a.sIndstCdx     "
                        + " LEFT JOIN Branch c ON c.sBranchCd = a.sBranchCd       "
                        + " LEFT JOIN Category d ON d.sCategrCd = a.sCategrCd     "
                        + " LEFT JOIN Client_Master e ON e.sClientID = a.sSupplier "
                        + " LEFT JOIN Payee f ON (f.sClientID = a.sSupplier OR f.sAPClntID = a.sSupplier) " ;

        lsSQL = MiscUtil.addCondition(lsSQL, " a.nAmtPaidx > 0.0000 AND a.nNetTotal > a.nAmtPaidx "
                                    +   " AND a.cTranStat != " + SQLUtil.toSQL(PurchaseOrderStatus.VOID)
                                    +   " AND a.cTranStat != " + SQLUtil.toSQL(PurchaseOrderStatus.CANCELLED)
                                    +   " AND a.cTranStat != " + SQLUtil.toSQL(PurchaseOrderStatus.POSTED)
                                    +   " AND a.sCompnyID = " + SQLUtil.toSQL(lsCompanyId)
                                    +   " AND a.sIndstCdx = " + SQLUtil.toSQL(lsIndustryId)
                                    +   " AND a.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
                                    +   " AND f.sPayeeIDx LIKE " + SQLUtil.toSQL("%" + Master().getPayeeID()));
        lsSQL = lsSQL + " ORDER BY a.dTransact, e.sCompnyNm ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            while (loRS.next()) {
                // Print the result set
//                System.out.println("sPrtclrID: " + loRS.getString("sPrtclrID")); //Hard code muna ito 
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("sBranchCd: " + loRS.getString("sBranchCd"));
                System.out.println("------------------------------------------------------------------------------");

                paPOMaster.add(PurchaseOrderMaster());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            poJSON.put("result", "success");
            poJSON.put("message", "Record loaded successfully.");
        } else {
            paPOMaster = new ArrayList<>();
            paPOMaster.add(PurchaseOrderMaster());
            poJSON.put("result", "error");
            poJSON.put("continue", true);
            poJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    
    /**
     * Loads a purchase order into PRF detail context and aligns source/payee fields.
     *
     * @param transactionNo Purchase order transaction number to load.
     * @return JSON result with status, message, and warning flags when applicable.
     * @throws CloneNotSupportedException If model cloning fails while preparing detail rows.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject populateDetail(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", 0);
        
        if(pnEditMode == EditMode.UPDATE){
            poJSON.put("message", "PRF transaction source cannot change in update mode cancellation of PRF will be require.");
            poJSON.put("result", "error");
            poJSON.put("warning", "true");
            return poJSON;
        }
        
        if(Master().getSourceCode() != null && !"".equals(Master().getSourceCode())){
            if(!InvTransCons.PURCHASE_ORDER.equals(Master().getSourceCode())){
                poJSON.put("message", "PRF transaction source cannot be mix with other expenses.");
                poJSON.put("result", "error");
                poJSON.put("warning", "true");
                return poJSON;
            }
        }
        
        Model_PO_Master loObject = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poJSON = loObject.openRecord(transactionNo);
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }
        
        Model_Payee loPayee = new CashflowModels(poGRider).Payee();
        poJSON = loPayee.openRecordByReference(loObject.getSupplierID());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            poJSON.put("message", ((String) poJSON.get("message") + "\nPlease contact system administrator to check data of Payee for supplier " + loObject.Supplier().getCompanyName() + "."));
            return poJSON;
        }
        
        // Validate if the payee in Master is different from the payee in the RecurringIssuance
        if (Master().getPayeeID() != null && !"".equals(Master().getPayeeID())) {
            if (Master().getSourceNo() != null && !"".equals(Master().getSourceNo())) {
                if (!Master().getPayeeID().equals(loPayee.getPayeeID())) {
                    poJSON.put("message", "Selected transaction must be equal to current payee.");
                    poJSON.put("result", "error");
                    poJSON.put("warning", "true");
                    return poJSON;
                }
            }
            
        }
        if(!transactionNo.equals(Master().getSourceNo())){
            Detail().clear();
            AddDetail();
        }
        Master().setBranchCode(loObject.getDestinationID()); //getBranchCode //Change by Arsiela 06-19-2026
        Master().setPayeeID(loPayee.getPayeeID());
        Master().setSourceNo(loObject.getTransactionNo());
        Master().setRemarks(loObject.getRemarks());
        Master().setSourceCode(InvTransCons.PURCHASE_ORDER);

        //Populate PO Attachment to PRF
        loadPOAttachment(loObject.getTransactionNo());

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    /**
     * Loads PO attachments, downloads files, and stages them for PRF attachment mapping.
     *
     * @param fsTransactionNo Purchase order transaction number.
     * @return JSON result from the last attachment processing step.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject loadPOAttachment(String fsTransactionNo)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();
        List<TransactionAttachment>  laAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(InvTransCons.PURCHASE_ORDER, fsTransactionNo);
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            TransactionAttachment loTransAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
            poJSON = loTransAttachment.openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                System.out.println("Attachment Record No: " + loTransAttachment.getModel().getTransactionNo());
                System.out.println("Attachment Source No: " + loTransAttachment.getModel().getSourceNo());
                System.out.println("Attachment Source Code: " + loTransAttachment.getModel().getSourceCode());
                System.out.println("Attachment File Name: " + loTransAttachment.getModel().getFileName());
            }

            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , loTransAttachment.getModel().getFileName()
                    , InvTransCons.PURCHASE_ORDER
                    , loTransAttachment.getModel().getSourceNo()
                    , "");
            if ("success".equals((String) poJSON.get("result"))) {

                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println(" PO File downloaded succesfully.");

                    //Populate Arraylist
                    laAttachments.add(TransactionAttachment());
                    laAttachments.set(laAttachments.size() - 1, loTransAttachment);

                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("PO ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                    System.out.println("PO poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                }

            } else {
                System.out.println("PO poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }

        //Populate PRF Attachment
        addPOAttchmentToPRF(laAttachments);

        return poJSON;
    }

    /**
     * Adds PO attachments into the PRF attachment list, skipping duplicate file names.
     *
     * @param faAttachments Attachments loaded from the selected purchase order.
     * @return JSON result with success status after merge.
     * @throws SQLException If a database access error occurs while adding attachments.
     * @throws GuanzonException If attachment model operations fail.
     */
    private JSONObject addPOAttchmentToPRF(List<TransactionAttachment>  faAttachments)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        //Add attachment to PRF
        for (int lnCtr = 0; lnCtr <= faAttachments.size() - 1; lnCtr++) {
            //Check if file name already exist in PRF attachment do not add
            for(int lnRow = 0; lnRow <= getTransactionAttachmentCount() - 1; lnRow++){
                if(faAttachments.get(lnCtr).getModel().getFileName().equals(paAttachments.get(lnRow).getModel().getFileName())){
                    lbExist = true;
                    break; //do not add
                }
            }

            if(!lbExist){
                addAttachment(faAttachments.get(faAttachments.size() - 1).getModel().getFileName());
                paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setDocumentType(faAttachments.get(faAttachments.size() - 1).getModel().getDocumentType());
                paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setMD5Hash(faAttachments.get(faAttachments.size() - 1).getModel().getMD5Hash());
                paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setImagePath(faAttachments.get(faAttachments.size() - 1).getModel().getImagePath());
                paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSendStatus("1");
            }
            //clear
            lbExist = false;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Recomputes transaction totals from detail amounts and discounts.
     *
     * @return JSON result containing status.
     */
    public JSONObject computeFields() {
        poJSON = new JSONObject();
        double ldblTransactionTotal = 0.0000;
        double ldblNetTotal = 0.0000;
        double ldblDiscountAmount = 0.0000;

        double ldblDetailDiscountRate = 0.0000;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                ldblTransactionTotal += Detail(lnCtr).getAmount();
                
                ldblDetailDiscountRate = 0.0000;
                if(Detail(lnCtr).getDiscount() > 0.0000){
                    ldblDetailDiscountRate = Detail(lnCtr).getAmount() * Detail(lnCtr).getDiscount();
                }
                ldblDiscountAmount += (Detail(lnCtr).getAddDiscount() + ldblDetailDiscountRate);
                
                ldblNetTotal += Detail(lnCtr).getNetTotal();

            }
        }
        
        Master().setTranTotal(ldblTransactionTotal);
        Master().setDiscountAmount(ldblDiscountAmount);
        Master().setNetTotal(ldblNetTotal);
        return poJSON;
    }
    
    /** Arsiela 03-03-2026
     * Validate setting of discount rate
     * @param fdblDiscountRate
     * @param row
     * @return 
     */
    public JSONObject setDiscountRate(double fdblDiscountRate, int row) {
        poJSON = new JSONObject();
        if (Detail(row).getAmount() <= 0.0000) {
            poJSON.put("message", "You're not allowed to enter discount rate, no amount entered.");
            poJSON.put("result", "error");
            return poJSON;
        }
        if (fdblDiscountRate < 0.00 || fdblDiscountRate > 1.00) {
            poJSON.put("message", "Invalid Discount Rate. Must be between 0.00 and 1.00 (1.00 = 100%)");
            poJSON.put("result", "error");
            return poJSON;
        }
//        double ldblDetailDiscountRate = Detail(row).getAmount() * (fdblDiscountRate / 100);
        double ldblDetailDiscountRate = Detail(row).getAmount() * fdblDiscountRate;
        if(Detail(row).getAmount() < (Detail(row).getAddDiscount() + ldblDetailDiscountRate)){
            poJSON.put("message", "Computed total discount cannot be greater than the detail amount.");
            poJSON.put("result", "error");
            return poJSON;
        }
        // Store rate (e.g., 0.10 = 10%) and amount
        poJSON = Detail(row).setDiscount(fdblDiscountRate);        // decimal: 1.00 = 100%
        return poJSON;
    }

    /** Arsiela 03-03-2026
     * Validate setting of discount amount
     * @param fdblAdditionalDiscount
     * @param row
     * @return 
     */
    public JSONObject setDiscountAmount(double fdblAdditionalDiscount, int row) {
        poJSON = new JSONObject();
        
        if (Detail(row).getAmount() <= 0.0000) {
            poJSON.put("message", "You're not allowed to enter discount rate, no amount entered.");
            poJSON.put("result", "error");
            return poJSON;
        }

        if (fdblAdditionalDiscount < 0.0000 || fdblAdditionalDiscount > Detail(row).getAmount()) {
            poJSON.put("message", "Invalid discount amount.");
            poJSON.put("result", "error");
            return poJSON;
        }

        double ldblDetailDiscountRate = Detail(row).getAmount() * Detail(row).getDiscount();
        if(Detail(row).getAmount() < (fdblAdditionalDiscount + ldblDetailDiscountRate)){
            poJSON.put("message", "Computed total discount cannot be greater than the detail amount.");
            poJSON.put("result", "error");
            return poJSON;
        }

        poJSON = Detail(row).setAddDiscount(fdblAdditionalDiscount);   
        return poJSON;   
    }

    /**
     * Checks if detail rows contain zero-amount entries.
     *
     * @return JSON result describing zero-amount validation state.
     */
    public JSONObject isDetailHasZeroAmount() {
        poJSON = new JSONObject();
        int zeroAmountRow = -1;
        boolean hasNonZeroAmount = false;
        boolean hasZeroAmount = false;
        int lastRow = getDetailCount() - 1;

        for (int lnRow = 0; lnRow <= lastRow; lnRow++) {
            double amount = Detail(lnRow).getAmount();
            String particularID = (String) Detail(lnRow).getValue("sPrtclrID");

            if (!particularID.isEmpty()) {
                if (amount == 0.00) {
                    hasZeroAmount = true;
                    if (zeroAmountRow == -1) {
                        zeroAmountRow = lnRow;
                    }
                } else {
                    hasNonZeroAmount = true;
                }
            }
        }

        if (!hasNonZeroAmount && hasZeroAmount) {
            poJSON.put("result", "error");
            poJSON.put("message", "All items have zero amount. Please enter a valid amount.");
            poJSON.put("tableRow", zeroAmountRow);
            poJSON.put("warning", "true");
        } else if (hasZeroAmount) {
            poJSON.put("result", "error");
            poJSON.put("message", "Some items have zero amount. Please review.");
            poJSON.put("tableRow", zeroAmountRow);
            poJSON.put("warning", "false");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "All items have valid amounts.");
            poJSON.put("tableRow", lastRow);
        }

        return poJSON;
    }

    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).PaymentRequestMaster();
    }

    public void resetOthers() {
        paAttachments = new ArrayList<>();
    }

    /**
     * Opens a transaction selected from browse results filtered by payee.
     *
     * @param fsValue Search keyword for browse.
     * @param fsPayeeID Payee filter value.
     * @return JSON result containing status and message.
     * @throws CloneNotSupportedException If detail cloning fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject SearchTransaction(String fsValue, String fsPayeeID) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " ( a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%")
                                                    + " OR a.sSourceNo LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%") //add source no in condition requested by ma'am she - Arsiela 05-22-2026
                                                    + " ) "
                                                    + " AND a.sPayeeIDx LIKE " + SQLUtil.toSQL("%" + fsPayeeID));
        String lsFilterAll = "";
        if (psIndustryId != null && !"".equals(psIndustryId)) {
            lsFilterAll += " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
        }
        if (psCompanyId != null && !"".equals(psCompanyId)) {
            lsFilterAll += " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId);
        }
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!lsFilterAll.isEmpty() && !"".equals(lsFilterAll)) {
            lsSQL = lsSQL + lsFilterAll;
        }
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Series No»Branch»Payee",
                "a.dTransact»a.sTransNox»a.sSeriesNo»b.sBranchNm»d.sPayeeNme",
                "a.dTransact»a.sTransNox»a.sSeriesNo»b.sBranchNm»d.sPayeeNme",
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

    /**
     * Loads payment request records matching transaction and payee filters.
     *
     * @param fsTransactionNo Transaction number filter.
     * @param fsPayee Payee filter.
     * @return JSON result containing load status.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    public JSONObject getPaymentRequest(String fsTransactionNo, String fsPayee) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        initSQL();
        String lsFilterCondition = String.join(" AND ",
                " a.sPayeeIDx LIKE " + SQLUtil.toSQL("%" + fsPayee),
                " a.sTransNox  LIKE " + SQLUtil.toSQL("%" + fsTransactionNo),
                " a.cProcessd = "  + SQLUtil.toSQL(Logical.NO));
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        
        lsSQL = lsSQL + " AND ( a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode()+ "%")
                    + " OR a.sSourceNo LIKE " + SQLUtil.toSQL(poGRider.getBranchCode()+ "%") //add source no in condition requested by ma'am she - Arsiela 05-22-2026
                    + " ) ";
        String lsFilterAll = "";
        if (psIndustryId != null && !"".equals(psIndustryId)) {
            lsFilterAll += " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
        }
        if (psCompanyId != null && !"".equals(psCompanyId)) {
            lsFilterAll += " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId);
        }
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!lsFilterAll.isEmpty() && !"".equals(lsFilterAll)) {
            lsSQL = lsSQL + lsFilterAll;
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            poPRFMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                poPRFMaster.add(PRFMasterList());
                poPRFMaster.get(poPRFMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            poPRFMaster = new ArrayList<>();
            poPRFMaster.add(PRFMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    private Model_Payment_Request_Master PRFMasterList() {
        return new CashflowModels(poGRider).PaymentRequestMaster();
    }

    public int getPRFMasterCount() {
        return poPRFMaster.size();
    }

    public Model_Payment_Request_Master poPRFMaster(int row) {
        return (Model_Payment_Request_Master) poPRFMaster.get(row);
    }

    /**
     * Applies status-based updates to related recurring issuance records.
     *
     * @param status Transaction status to propagate.
     * @return JSON result containing status.
     * @throws CloneNotSupportedException If detail cloning fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     */
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        poRecurringIssuances = new ArrayList<>();

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            String particularID = Detail(lnCtr).getParticularID();
            String branchCode = Master().getBranchCode();
            String payeeID = Master().getPayeeID();
            String accountNo = Detail(lnCtr).Recurring() != null
                    ? Detail(lnCtr).Recurring().getAccountNo()
                    : null;

            // Skip if accountNo is missing or not found in recurring_issuance
            if (accountNo == null || !isRecurringIssuance(particularID, branchCode, payeeID, accountNo)) {
                continue;
            }

            System.out.printf("RECURRING RECORD: #%d - PartID: %s | Branch: %s | Payee: %s | AccNo: %s%n",
                    lnCtr + 1, particularID, branchCode, payeeID, accountNo);

            updateRecurringIssuance(particularID, branchCode, payeeID, accountNo);
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Checks if a recurring issuance record exists for the given keys.
     *
     * @param particularID Particular id.
     * @param branch Branch code.
     * @param payee Payee id.
     * @param accountNo Account number.
     * @return {@code true} if matching issuance exists.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If recurring model operations fail.
     */
    private boolean isRecurringIssuance(String particularID, String branch, String payee, String accountNo)
            throws SQLException, GuanzonException {

        RecurringIssuance issuance = RecurringIssuance();
        JSONObject result = issuance.poModel.openRecord(particularID, branch, payee, accountNo);

        // Safe Java 8–compatible check without .has() or .optString()
        try {
            Object value = result.get("sPrtclrID");
            return value != null && !"".equals(String.valueOf(value));
        } catch (Exception e) {
            return false;
        }
    }

    private RecurringIssuance RecurringIssuance() throws GuanzonException, SQLException {
        return new CashflowControllers(poGRider, logwrapr).RecurringIssuance();
    }

    /**
     * Updates recurring issuance metadata for the current payment request.
     *
     * @param particularID Particular id.
     * @param branch Branch code.
     * @param payee Payee id.
     * @param accountNo Account number.
     * @throws GuanzonException If recurring model operations fail.
     * @throws SQLException If a database access error occurs.
     * @throws CloneNotSupportedException If detail cloning fails.
     */
    private void updateRecurringIssuance(String particularID, String branch, String payee, String accountNo)
            throws GuanzonException, SQLException, CloneNotSupportedException {

        RecurringIssuance issuance = RecurringIssuance();
        poRecurringIssuances.add(issuance);

        System.out.printf("Updating Recurring Issuance: PartID=%s | Branch=%s | Payee=%s | Account=%s%n",
                particularID, branch, payee, accountNo);

        JSONObject record = issuance.poModel.openRecord(particularID, branch, payee, accountNo);
        System.out.println("Record Loaded: " + record.toString());
        System.out.println("Edit Mode (before): " + issuance.poModel.getEditMode());

        issuance.poModel.updateRecord();

        // Set updated values
        issuance.poModel.setParticularID(particularID);
        issuance.poModel.setBranchCode(branch);
        issuance.poModel.setPayeeID(payee);
        issuance.poModel.setAccountNo(accountNo);
        issuance.poModel.setLastPRFTrans(Master().getTransactionNo());
        issuance.poModel.setModifyingId(poGRider.getUserID());
        issuance.poModel.setModifiedDate(poGRider.getServerDate());

        System.out.println("Edit Mode (after): " + issuance.poModel.getEditMode());
    }

    /**
     * Saves pending recurring issuance updates collected during status changes.
     *
     * @return JSON result containing save status.
     * @throws CloneNotSupportedException If detail cloning fails.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If recurring model operations fail.
     */
    private JSONObject saveUpdates()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnCtr;
        for (lnCtr = 0; lnCtr <= poRecurringIssuances.size() - 1; lnCtr++) {

            poRecurringIssuances.get(lnCtr).setWithParentClass(true);

            System.out.println("editmode = " + poRecurringIssuances.get(lnCtr).poModel.getEditMode());
            poJSON = poRecurringIssuances.get(lnCtr).poModel.saveRecord();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Gets the next branch-specific PRF series number.
     *
     * @return Next series number padded to 10 digits.
     * @throws SQLException If a database access error occurs.
     */
    public String getSeriesNoByBranch() throws SQLException {
        String lsSQL = "SELECT sSeriesNo FROM Payment_Request_Master";
        lsSQL = MiscUtil.addCondition(lsSQL,
                "sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                + " ORDER BY sSeriesNo DESC LIMIT 1");

        String branchSeriesNo = PaymentRequestStaticData.default_Branch_Series_No;  // default value

        ResultSet loRS = null;
        try {
            loRS = poGRider.executeQuery(lsSQL);
            if (loRS != null && loRS.next()) {
                String sSeries = loRS.getString("sSeriesNo");
                if (sSeries != null && !sSeries.trim().isEmpty()) {
                    long seriesNumber = Long.parseLong(sSeries);
                    seriesNumber += 1;
                    branchSeriesNo = String.format("%010d", seriesNumber); // format to 10 digits
                }

            }
        } finally {
            MiscUtil.close(loRS);  // Always close the ResultSet
        }
        return branchSeriesNo;
    }

    /**
     * Retrieves payment status using recurring issuance last PRF reference.
     *
     * @param lastPRFNo Last PRF number.
     * @return Payment status code or empty string when not found.
     * @throws SQLException If a database access error occurs.
     */
    public String getPaymentStatusFromIssuanceLastPRFNo(String lastPRFNo) throws SQLException {
        String status = "";
        String lsSQL = "SELECT b.cTranStat "
                + "FROM Recurring_Issuance a "
                + "LEFT JOIN Payment_Request_Master b ON b.sTransNox = a.sLastRqNo "
                + MiscUtil.addCondition("", "a.sLastRqNo = " + SQLUtil.toSQL(lastPRFNo));

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                String tranStat = loRS.getString("cTranStat");
                status = tranStat != null ? tranStat : "";
            }
        } finally {
            MiscUtil.close(loRS);
        }

        return status;
    }
    
    /**
     * Loads and displays status history for the current payment request.
     *
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If model operations fail.
     * @throws Exception If history UI rendering fails.
     */
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst(); 
	
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case PaymentRequestStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case PaymentRequestStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case PaymentRequestStatus.PAID:
                    crs.updateString("cRefrStat", "PAID");
                    break;
                case PaymentRequestStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case PaymentRequestStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case PaymentRequestStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case PaymentRequestStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                    case PaymentRequestStatus.OPEN:
                        crs.updateString("cRefrStat", "OPEN");
                        break;
                    case PaymentRequestStatus.CONFIRMED:
                        crs.updateString("cRefrStat", "CONFIRMED");
                        break;
                    case PaymentRequestStatus.PAID:
                        crs.updateString("cRefrStat", "PAID");
                        break;
                    case PaymentRequestStatus.CANCELLED:
                        crs.updateString("cRefrStat", "CANCELLED");
                        break;
                    case PaymentRequestStatus.VOID:
                        crs.updateString("cRefrStat", "VOID");
                        break;
                    case PaymentRequestStatus.POSTED:
                        crs.updateString("cRefrStat", "POSTED");
                        break;
                    case PaymentRequestStatus.RETURNED:
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
        
        showStatusHistoryUI("Payment Request", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    /**
     * Resolves entry user and entry datetime from audit logs.
     *
     * @return JSON result containing entry user and entry date.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If user lookup operations fail.
     */
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM Payment_Request_Master a "
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
    /**
     * Gets display name of a system user by user id.
     *
     * @param fsId User id.
     * @return User display name, or empty string if not found.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If user lookup operations fail.
     */
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

}
