/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_PettyCash_Disbursement;
import ph.com.guanzongroup.cas.cashflow.model.Model_PettyCash_Disbursement_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cash_Fund;
import ph.com.guanzongroup.cas.cashflow.model.Model_PettyCash;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.PettyCashDisbursementStatus;
import ph.com.guanzongroup.cas.cashflow.status.CashFundStatus;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;
import ph.com.guanzongroup.cas.cashflow.validator.CashAdvanceValidator;
import ph.com.guanzongroup.cas.cashflow.validator.PettyCashDisbursementValidator;

/**
 *
 * @author Arsiela 04/06/2026
 */
public class PettyCashDisbursement extends Transaction {
    public String psIndustryId = "";
    public String psCompanyId = "";
    public String psDepartmentId = "";
    public String psIndustry = "";
    public String psBranch = "";
    public String psPayee = "";
    
    public List<Model> paMaster;
    public List<TransactionAttachment> paAttachments;
    
    /**
    * Initializes a new Petty Cash Disbursement transaction.
    * 
    * This method sets the source code, instantiates the master, detail, and journal 
    * controllers, and resets all associated data lists (Cash Advances, W-Tax, and Attachments).
    * 
    * @return a {@link JSONObject} containing the status of the initialization.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if a business logic or validation error occurs.
    */
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "PCDv";

        poMaster = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poDetail = new CashflowModels(poGRider).PettyCashDisbursementDetail();

        paMaster = new ArrayList<Model>();
        paAttachments = new ArrayList<>();

        return initialize();
    }

    //Transaction Source Code 
    @Override
    public String getSourceCode() { return SOURCE_CODE; }
    
    //Set value for private strings used in searching / filtering data
    public void setIndustryId(String industryId) { psIndustryId = industryId; }
    public void setCompanyId(String companyId) { psCompanyId = companyId; }
    public void setDepartmentId(String departmentId) { psDepartmentId = departmentId; }
    public void setSearchIndustry(String industry) { psIndustry = industry; }
    public void setSearchBranch(String branch) { psBranch = branch; }
    public void setSearchPayee(String payeeName) { psPayee = payeeName; }
    public String getSearchIndustry() { return psIndustry; }
    public String getSearchPayee() { return psPayee; }
    public String getSearchBranch() { return psBranch; }
    
    /**
    * Creates a JSONObject with "result" and "message" fields.
    *
    * @param fsResult  The result value (e.g., "success", "error")
    * @param fsMessage The message describing the result
    * @return JSONObject containing the result and message
    */
    private JSONObject setJSON(String fsResult, String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", fsResult);
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    /**
     * Checks whether a JSONObject indicates a successful result.
     *
     * Returns true if the "result" field equals "success" or is not "error".
     *
     * @param foJSON The JSONObject to check
     * @return true if successful, false otherwise
     */
    public boolean isJSONSuccess(JSONObject foJSON) {
        return ("success".equals((String) foJSON.get("result")) || !"error".equals((String) foJSON.get("result")));
    }
    
    /**
    * Creates a new transaction record.
    *
    * @return JSONObject result of the operation
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if application-specific error occurs
    */
    public JSONObject NewTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        resetMaster();
        Detail().clear();
        
        poJSON = newTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        return poJSON;
    }
    
    /**
    * Opens an existing transaction and loads its associated data.
    * 
    * This method resets the current transaction state, retrieves the specified transaction 
    * record, and automatically loads any related attachments.
    * 
    * @param transactionNo the unique identifier of the transaction to be opened.
    * @return a {@link JSONObject} containing the success status or an error message if the 
    *         transaction or its attachments fail to load.
    * @throws CloneNotSupportedException if an error occurs during object cloning.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if a business logic or validation error occurs.
    * @throws ScriptException if an error occurs during script execution.
    */
    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        //Reset Transaction
        resetTransaction();
        
        poJSON = openTransaction(transactionNo);
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        poJSON = setJSON("success","success");
        return poJSON;
    }

    /**
    * Prepares the current transaction for modification.
    * 
    * This method initiates the update state for the transaction and refreshes 
    * its associated attachments to ensure data consistency during editing.
    * 
    * @return a {@link JSONObject} indicating the success or failure of the update request.
    * @throws SQLException if a database error occurs.
    * @throws GuanzonException if business logic validation fails.
    * @throws CloneNotSupportedException if an error occurs during data cloning.
    * @throws ScriptException if an error occurs during script-based processing.
    */
   public JSONObject UpdateTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = updateTransaction();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to update transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
       
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

       poJSON = setJSON("success","success");
       return poJSON;
   }
    
    /**
     * Commits the current transaction changes to the database.
     * 
     * @return A {@link JSONObject} containing the result of the save operation.
     * @throws SQLException, GuanzonException, CloneNotSupportedException 
     *          If a database constraint is violated or business logic validation fails.
     */
    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }
    
    /**
    * Requests user approval for the current transaction.
    *
    * @return JSONObject containing approval result and message
    */
    public JSONObject callApproval(){
        poJSON = new JSONObject();
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON = setJSON("error", "User is not an authorized approving officer.");
                return poJSON;
            }
            setApproving(lsUserIDxx);
        }   
        
        poJSON = setJSON("success","success");
        return poJSON;
    }
    
    /**
    * Validates if the current transaction can be updated based on its latest database status.
    * 
    * This method checks if the transaction is in a restricted state (Voided, Cancelled, 
    * or Approved). It also synchronizes the local transaction data by calling 
    * {@link #OpenTransaction(String)} if the database status differs from the local state.
    * 
    * @param isEntry set to {@code true} to prevent updates if the transaction is already "Confirmed".
    * @return a {@link JSONObject} with "success" if the update can proceed, or an error message if restricted.
    * @throws CloneNotSupportedException if an error occurs during data cloning.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if business logic validation fails.
    * @throws ScriptException if an error occurs during script processing.
    */
    public JSONObject checkUpdateTransaction(boolean isEntry) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        
        Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        switch(loObject.getTransactionStatus()){
            case PettyCashDisbursementStatus.VOID:
            case PettyCashDisbursementStatus.CANCELLED:
                poJSON = setJSON("error","Transaction status was already "+getStatus(loObject.getTransactionStatus())+"\nCheck transaction history.");
                return poJSON;
            case PettyCashDisbursementStatus.CONFIRMED:
                if(isEntry){
                    poJSON = setJSON("error", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                    return poJSON;
                }
                break;
            case PettyCashDisbursementStatus.APPROVED:
                poJSON = setJSON("error","Transaction status was already approved!\nCheck transaction history.");
                return poJSON;
        }
        
        if(!loObject.getTransactionStatus().equals(Master().getTransactionStatus())){
            poJSON = OpenTransaction(Master().getTransactionNo());
            if (!isJSONSuccess(poJSON)) {
                poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
                return poJSON;
            }
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
    * Converts a numeric or short-code transaction status into a human-readable string.
    * 
    * @param lsStatus the status code to be converted.
    * @return the descriptive name of the status (e.g., "Voided", "Confirmed"), 
    *         or "Unknown" if the code is not recognized.
    */
    public String getStatus(String lsStatus) {
        switch (lsStatus) {
            case PettyCashDisbursementStatus.VOID:
                return "Voided";
            case PettyCashDisbursementStatus.CANCELLED:
                return "Cancelled";
            case PettyCashDisbursementStatus.APPROVED:
                return "Approved";
            case PettyCashDisbursementStatus.CONFIRMED:
                return "Confirmed";
            case PettyCashDisbursementStatus.OPEN:
                return "Open";
            default:
                return "Unknown";
        }
    }
    
    /**
    * Validates whether a transaction status transition is permitted.
    * 
    * This method enforces the workflow rules for cash disbursements (e.g., a transaction 
    * must be "Open" before it can be "Confirmed" or "Voided").
    * 
    * @param current the current status of the transaction.
    * @param target the proposed status to transition to.
    * @return {@code true} if the transition is valid based on business rules; {@code false} otherwise.
    */
    public boolean isAllowed(String current, String target) {
        switch (target) {
            case PettyCashDisbursementStatus.VOID:
                return current.equals(PettyCashDisbursementStatus.OPEN); //Allow void when current status is open

            case PettyCashDisbursementStatus.CANCELLED:
                return current.equals(PettyCashDisbursementStatus.CONFIRMED); //Allow void when current status is confirmed

            case PettyCashDisbursementStatus.CONFIRMED:
                return current.equals(PettyCashDisbursementStatus.OPEN); //Allow confirm when current status is open

            case PettyCashDisbursementStatus.APPROVED:
                return current.equals(PettyCashDisbursementStatus.CONFIRMED); //Allow approve when current status is confirmed

            default:
                return false;
        }
    }
    
    /**
    * Confirms the current cash disbursement transaction.
    * 
    * This method validates the transaction's current state, ensures it hasn't been 
    * confirmed already, performs necessary approval workflows, and updates the 
    * status to {@code CONFIRMED} in the database.
    * 
    * @param remarks additional notes or comments for the confirmation process.
    * @return a {@link JSONObject} containing the success status or a specific error message.
    * @throws ParseException if an error occurs during data parsing.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if business logic or validation fails.
    * @throws CloneNotSupportedException if an error occurs during object cloning.
    * @throws ScriptException if an error occurs during script execution.
    */
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = PettyCashDisbursementStatus.CONFIRMED;
        
        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already confirmed.");
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON = setJSON("error",  "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        if(!pbWthParent){
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"),"", lsStatus, false,true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();
        
        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    /**
    * Approve transaction
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
     * @throws javax.script.ScriptException
    */
    public JSONObject ApproveTransaction()
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException,
            ScriptException {
        poJSON = new JSONObject();

        String lsStatus = PettyCashDisbursementStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already approved.");
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        if(!pbWthParent){
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ApproveTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        try {
            //Update Petty Cash Balance and Generate Ledger
            LocalDate transDate = strToDate(xsDateShort(Master().getTransactionDate()));
            PettyCashTrans loTrans = new PettyCashTrans(poGRider);
            loTrans.InitTransaction(Master().getPettyId(), Master().getBranchCode(), Master().getDepartmentRequest());
            loTrans.Disbursement(Master().getTransactionNo(), transDate,  Master().getTransactionTotal(), false);
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message",MiscUtil.getException(ex));
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"),"", lsStatus, false,true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction approved successfully.");
        return poJSON;
    }
    
    /**
    * Void transaction
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
     * @throws javax.script.ScriptException
    */
    public JSONObject VoidTransaction()
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException,
            ScriptException {
        poJSON = new JSONObject();

        String lsStatus = PettyCashDisbursementStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already voided.");
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        if (lsStatus.equals(Master().getTransactionStatus())) {
            poJSON = setJSON("error", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        if(PettyCashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())){
            if(!pbWthParent){
                poJSON = callApproval();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
       
        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"),"", lsStatus, false,true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction voided successfully.");
        return poJSON;
    }
    
    /**
    * Cancel transaction
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
     * @throws javax.script.ScriptException
    */
    public JSONObject CancelTransaction()
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException,
            ScriptException {
        poJSON = new JSONObject();

        String lsStatus = PettyCashDisbursementStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already cancelled.");
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        if(PettyCashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())){
            if(!pbWthParent){
                poJSON = callApproval();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"),"", lsStatus, false,true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();
        

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction cancelled successfully.");
        return poJSON;
    }
    
    /**
    * Retrieves Cash Fund based on branch, department, company, and industry,
    * then sets the Cash Fund ID to the model if found.
    *
    * @return empty string
    * @throws SQLException if database error occurs
    * @throws GuanzonException if application error occurs
    */
    private String setCashFund() throws SQLException, GuanzonException{
        Model_PettyCash loObj = new CashflowModels(poGRider).PettyCashMaster();
        int lnCountCashFund = 0;
        String lsPettyID = "";
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loObj), 
                    " sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND sDeptIDxx = " + SQLUtil.toSQL(Master().getDepartmentRequest())
                    + " AND cTranStat = " + SQLUtil.toSQL(CashFundStatus.ACTIVE)
                    + " AND DATE(dBegDatex) <= " + SQLUtil.toSQL(xsDateShort(poGRider.getServerDate()))
                    );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    lnCountCashFund++;
                    lsPettyID = loRS.getString("sPettyIDx") ;
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        
        //BR: Use the Cash Fund ID based on the log in branch and dept ng requesting employee or if 1 Cash Fund ID is defined, use this as the default
        if(lnCountCashFund == 1){
            if(lsPettyID != null && !"".equals(lsPettyID)){
                Master().setPettyId(lsPettyID);
            }
        } else {
            lsPettyID = "";
        }
        
        return lsPettyID;
    }
    
    /*Search Master References*/
    public JSONObject SearchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                );
        
        if(!psIndustryId.equals(System.getProperty("sys.main.industry"))){
           lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»Voucher No»Payee»Requesting Department",
                "sTransNox»dTransact»sVoucherx»sPayeexxx»sDeptName",
                "a.sTransNox»a.dTransact»a.sVoucherx»e.sCompnyNm»d.sDeptName",
                0);

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
    * Searches for transactions based on multiple criteria and opens the selected record.
    * 
    * This method filters records by industry, branch, payee, and transaction number, 
    * then displays a browse dialog for user selection. If a record is selected, 
    * it is automatically loaded using {@link #OpenTransaction(String)}.
    * 
    * @param fsIndustry the industry category to filter (required).
    * @param fsPayee the payee name to filter.
    * @param fsVoucherNo the specific voucher number to search for.
    * @return a {@link JSONObject} containing the status of the search or the loaded transaction data.
    * @throws CloneNotSupportedException if an error occurs during object cloning.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if business logic or validation fails.
    * @throws ScriptException if an error occurs during script execution.
    */
    public JSONObject SearchTransaction(String fsIndustry, String fsPayee, String fsVoucherNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        int lnSort = 0;
        if(fsPayee != null && !"".equals(fsPayee)){
            lnSort = 2;
        }
        if(fsVoucherNo != null && !"".equals(fsVoucherNo)){
            lnSort = 0;
        }
        if (fsIndustry == null || "".equals(fsIndustry)) { 
            poJSON = setJSON("error", "Industry cannot be empty.");
            return poJSON;
        }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
                + " AND a.sVoucherx LIKE " + SQLUtil.toSQL("%" + fsVoucherNo + "%"));
        
        if(psDepartmentId != null && !"".equals(psDepartmentId)){
            lsSQL = lsSQL +  " AND a.sDeptReqs = " + SQLUtil.toSQL(psDepartmentId);
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»Voucher No»Payee»Branch»Requesting Department",
                "sTransNox»dTransact»sVoucherx»sPayeexxx»sBranchNm»sDeptName",
                "a.sTransNox»a.dTransact»a.sVoucherx»e.sCompnyNm»g.sBranchNm»d.sDeptName",
                lnSort);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON = setJSON("error", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
     * Searches for an active Industry record.
     * 
     * @param value The search criteria (code or description).
     * @param byCode {@code true} to search by code, {@code false} by description.
     * @return A {@link JSONObject} containing the search result.
     * @throws ExceptionInInitializerError, SQLException, GuanzonException If search fails.
     */
    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            setSearchIndustry(object.getModel().getDescription());
        }

        return poJSON;
    }
    
    /**
     * Searches for an active Branch record and updates the local branch state.
     * 
     * @param value The search criteria.
     * @param byCode {@code true} to search by branch code.
     * @param isSearch {@code true} to search by filter.
     * @return A {@link JSONObject} containing the search result.
     * @throws ExceptionInInitializerError, SQLException, GuanzonException If search fails.
     */
    public JSONObject SearchBranch(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            if(isSearch){
                setSearchBranch(object.getModel().getBranchName()); 
            } else {
                Master().setBranchCode(object.getModel().getBranchCode());
            }
        }

        return poJSON;
    }
    /**
    * Searches for an active cash fund and assigns it to the current transaction.
    * 
    * This method validates that the necessary transaction headers (Industry, Company, 
    * Branch, and Department) are set before performing the search. If a record is 
    * successfully selected, the {@code CashFundId} is automatically updated in the master model.
    * 
    * @param value the search keyword or code.
    * @param byCode {@code true} if the search is by code; {@code false} if by description.
    * @return a {@link JSONObject} containing the search result or validation error messages.
    * @throws ExceptionInInitializerError if an error occurs during controller initialization.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if business logic validation fails.
    */
    public JSONObject SearchPettyCash(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        if(Master().getIndustryId() == null || "".equals(Master().getIndustryId())){
            poJSON = setJSON("error", "Industry cannot be empty.");
            return poJSON;
        }
        if(Master().getCompanyId() == null || "".equals(Master().getCompanyId())){
            poJSON = setJSON("error", "Company cannot be empty.");
            return poJSON;
        }
        if(Master().getBranchCode() == null || "".equals(Master().getBranchCode())){
            poJSON = setJSON("error", "Branch cannot be empty.");
            return poJSON;
        }
        if(Master().getDepartmentRequest() == null || "".equals(Master().getDepartmentRequest())){
            poJSON = setJSON("error", "Department cannot be empty.");
            return poJSON;
        }
        
        PettyCash object = new CashflowControllers(poGRider, logwrapr).PettyCash();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setIndustryId(Master().getIndustryId());
        object.setCompanyId(Master().getCompanyId());
        object.setBranchCode(Master().getBranchCode());
        object.setDepartmentId(Master().getDepartmentRequest());
        object.setPettyCashUse(true);
        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            Master().setPettyId(object.getModel().getPettyId());
        }

        return poJSON;
    }
   /**
    * Searches for an active employee to assign as the transaction payee.
    * 
    * This method queries the employee database (excluding terminated staff) and displays 
    * a browse dialog. Depending on the {@code isSearch} flag, the selection will either 
    * update a temporary search field or the actual master transaction record.
    * 
    * @param value the search keyword (ID or name).
    * @param byCode {@code true} to search by Employee ID; {@code false} to search by Name.
    * @param isSearch {@code true} to update the search field; {@code false} to update 
    *                 the master record's Client ID and Payee Name.
    * @return a {@link JSONObject} indicating "success" or an error if no record was selected.
    * @throws ExceptionInInitializerError if an error occurs during initialization.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if business logic validation fails.
    */
    public JSONObject SearchPayee(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();

        poJSON = new JSONObject();
        String lsSQL = "SELECT " 
                + "   a.sEmployID "
                + " , b.sCompnyNm AS EmployNme" 
                + " FROM Employee_Master001 a" 
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployID" ; 
        lsSQL = MiscUtil.addCondition(lsSQL, " a.dFiredxxx IS NULL "
                                            );
        lsSQL = lsSQL + " GROUP BY sEmployID";
        System.out.println("Executing SQL: " + lsSQL);
        JSONObject loJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Employee ID»Employee Name",
                "sEmployID»EmployNme",
                "a.sEmployID»b.sCompnyNm",
                byCode ? 0 : 1);
        if (loJSON != null) {
            System.out.println("Employee ID " + (String) loJSON.get("sEmployID"));
            System.out.println("Employee Name " + (String) loJSON.get("EmployNme"));
            if(isSearch){
                setSearchPayee((String) loJSON.get("EmployNme"));
            } else {
                Master().setClientId((String) loJSON.get("sEmployID"));
                Master().setPayeeName((String) loJSON.get("EmployNme"));
            }
        } else {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "No record loaded.");
            return loJSON;
        }
    
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
    * Searches for an active employee to credit using a lookup dialog.
    * 
    * @param value  The search keyword (ID or Name).
    * @param byCode Set to true to search by Employee ID, false to search by Name.
    * @return A {@link JSONObject} containing "success" if a record is selected, 
    *         otherwise returns an "error" status with a message.
    * @throws SQLException If a database error occurs.
    * @throws GuanzonException If a general application error occurs.
    */
    public JSONObject SearchCreditTo(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();

        poJSON = new JSONObject();
        String lsSQL = "SELECT " 
                + "   a.sEmployID "
                + " , b.sCompnyNm AS EmployNme" 
                + " FROM Employee_Master001 a" 
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployID" ; 
        lsSQL = MiscUtil.addCondition(lsSQL, " a.dFiredxxx IS NULL "
                                            );
        lsSQL = lsSQL + " GROUP BY sEmployID";
        System.out.println("Executing SQL: " + lsSQL);
        JSONObject loJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Employee ID»Employee Name",
                "sEmployID»EmployNme",
                "a.sEmployID»b.sCompnyNm",
                byCode ? 0 : 1);
        if (loJSON != null) {
            System.out.println("Employee ID " + (String) loJSON.get("sEmployID"));
            System.out.println("Employee Name " + (String) loJSON.get("EmployNme"));
            Master().setCreditedTo((String) loJSON.get("sEmployID"));
        } else {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "No record loaded.");
            return loJSON;
        }
    
        poJSON = setJSON("success", "success");
        return poJSON;
    }


    /**
    * Searches for a "Particular" record and assigns it to a specific row in the details.
    * 
    * @param value  The search keyword (Code or Description).
    * @param byCode Set to true to search by Code, false by Description.
    * @param row    The index of the detail row to update.
    * @return A {@link JSONObject} containing the result of the search and update operation.
    * @throws SQLException If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs.
    */
    public JSONObject SearchParticular(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
//        poJSON = validateMaster();
//        if(!isJSONSuccess(poJSON)){
//            return poJSON;
//        }
        Particular object = new CashflowControllers(poGRider, logwrapr).Particular();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            JSONObject loJSON = setDetail(row, object.getModel().getParticularID());
            if (!isJSONSuccess(loJSON)) {
                return poJSON;
            }
        }
        
        poJSON.put("success", "success");
        return poJSON;
    }
    /**
    * Validates required fields in the Master object.
    *
    * @return JSONObject with "error" and message if a field is empty,
    *         or "success" if all fields are valid.
    */
    public JSONObject validateMaster(){
        poJSON = new JSONObject();
            if(Master().getIndustryId() == null || "".equals(Master().getIndustryId())){
                poJSON = setJSON("error", "Industry cannot be empty.");
                return poJSON;
            }
            if(Master().getCompanyId() == null || "".equals(Master().getCompanyId())){
                poJSON = setJSON("error", "Company cannot be empty.");
                return poJSON;
            }
            if(Master().getBranchCode() == null || "".equals(Master().getBranchCode())){
                poJSON = setJSON("error", "Branch cannot be empty.");
                return poJSON;
            }
            if(Master().getDepartmentRequest() == null || "".equals(Master().getDepartmentRequest())){
                poJSON = setJSON("error", "Department cannot be empty.");
                return poJSON;
            }
            if(Master().getPettyId() == null || "".equals(Master().getPettyId())){
                poJSON = setJSON("error", "Cash fund cannot be empty.");
                return poJSON;
            }
            if(Master().getPayeeName() == null || "".equals(Master().getPayeeName())){
                poJSON = setJSON("error", "Payee cannot be empty.");
                return poJSON;
            }
            if(Master().getCreditedTo() == null || "".equals(Master().getCreditedTo())){
                poJSON = setJSON("error", "Credited to cannot be empty.");
                return poJSON;
            }
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
    * Searches a department by code or name and sets the selected department ID if found.
    *
    * @param value   search keyword or code
    * @param byCode  true to search by code, false to search by name
    * @return JSONObject containing the search result
    * @throws SQLException if database error occurs
    * @throws GuanzonException if application error occurs
    */
    public JSONObject SearchDepartment(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            Master().setDepartmentRequest(object.getModel().getDepartmentId());
            String lsPettyId = setCashFund();
            if(lsPettyId != null && !"".equals(lsPettyId)){
                Master().setPettyId(lsPettyId);
            } else {
                Master().setPettyId("");
            }
        }
        return poJSON;
    }
    /**
    * Sets the "Particular" ID for a specific detail row and checks for duplicates.
    * 
    * If the item already exists in another row, this method flags that existing 
    * row as a reversal and resets the current row's selection to prevent duplication.
    * 
    * @param fnRow        The index of the detail row being updated.
    * @param fsParticular The unique ID of the Particular item to assign.
    * @return A {@link JSONObject} indicating success and the index of the affected duplicate row, if any.
    * @throws SQLException     If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs.
    */
    public JSONObject setDetail(int fnRow, String fsParticular) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnRow = 0;
        
        if(Detail(fnRow).getEditMode() == EditMode.ADDNEW){
            for(int lnCtr = 0;lnCtr <= getDetailCount()-1; lnCtr++){
                if(Detail(lnRow).isReverse()){
                    lnRow++;
                }
                if(fnRow != lnCtr){
                    if(fsParticular.equals(Detail(lnCtr).getParticularId())){
                        if(!Detail(lnCtr).isReverse()){
                            Detail(lnCtr).isReverse(true);
                            Detail(lnCtr).setAmount(Detail(fnRow).getAmount());

                            //Reset value of the current selected row
                            Detail().remove(fnRow);
                            poJSON.put("result", "success");
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        }
                    }
                }
            }
        }
        
        poJSON = Detail(fnRow).setParticularId(fsParticular);
        if(!isJSONSuccess(poJSON)){
            poJSON.put("row", fnRow);
            return poJSON;
        }
        System.out.println("Particular : " + Detail(fnRow).Particular().getDescription());
        poJSON.put("result", "success");
        poJSON.put("row", fnRow);
        return poJSON;
    }
    
    /**
    * Calculates and validates transaction totals, including VAT components and net amounts.
    *
    * This method aggregates values from detail rows, performs non-negative 
    * validation for all calculated totals, and updates the master record summary fields.
    *
    * @param isValidate Set to true to return an error immediately if any calculated total is negative.
    * @return A {@link JSONObject} indicating "success" or "error", including the specific column 
    *         name where a validation failure occurred.
    */
    public JSONObject computeFields(boolean isValidate) {
        poJSON = new JSONObject();
        poJSON.put("column", "");
        Double ldblTransactionTotal = 0.0000;
        
        for (int lnCntr = 0; lnCntr <= getDetailCount() - 1; lnCntr++) {
            if(Detail(lnCntr).isReverse()){
                ldblTransactionTotal += Detail(lnCntr).getAmount();

            }
        }
        
        if(ldblTransactionTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Transaction Total.");
            poJSON.put("column", "nTranTotl");
            if(isValidate){
                return poJSON;
            }
        }
        
        Master().setTransactionTotal(ldblTransactionTotal);
        
        poJSON = setJSON("success", "computed successfully");
        poJSON.put("column", "");
        return poJSON;
    }
    /**
    * Loads a list of cash disbursement transactions based on the provided filters.
    * 
    * <p>This method queries the database using industry, payee, and voucher number criteria. 
    * If matches are found, it populates the internal collection ({@code paMaster}) 
    * with the corresponding transaction models.</p>
    * 
    * @param fsIndustry  The industry description to filter by (required).
    * @param fsPayee     The payee name keyword to filter by.
    * @param fsVoucherNo The voucher number keyword to filter by.
    * @return A {@link JSONObject} indicating "success" if records were loaded, 
    *         otherwise returns an "error" status with a descriptive message.
    * @throws SQLException     If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs during record opening.
    */
    public JSONObject loadTransactionList(String fsIndustry, String fsPayee, String fsVoucherNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        if (fsIndustry == null || "".equals(fsIndustry)) { 
            poJSON = setJSON("error", "Industry cannot be empty.");
            return poJSON;
        }
        if (fsPayee == null) { fsPayee = ""; }
        if (fsVoucherNo == null) { fsVoucherNo = ""; }
        
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
                + " AND a.sVoucherx LIKE " + SQLUtil.toSQL("%" + fsVoucherNo + "%")
            );
        
        if(!psIndustryId.equals(System.getProperty("sys.main.industry"))){
           lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON = setJSON("error", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_PettyCash_Disbursement loObject = new CashflowModels(poGRider).PettyCashDisbursementMaster();
            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
            if (isJSONSuccess(poJSON)) {
                paMaster.add((Model) loObject);
            } else {
                return poJSON;
            }
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    /**
     * Retrieves and downloads all attachments associated with the current transaction.
     * 
     * This method fetches attachment metadata from the system tables, populates the
     * {@code paAttachments} collection, and performs a web download for each file. 
     * Downloaded files are decoded from Base64 and saved to the system's temporary 
     * attachment directory defined in the system properties.
     * 
     * @return A {@link JSONObject} indicating the overall success of the attachment loading process.
     * @throws SQLException If a database error occurs while fetching attachment records.
     * @throws GuanzonException If an error occurs during file synchronization or processing.
     */
    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();
        String lsSourceCode = "";
        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(getSourceCode(), Master().getTransactionNo());
        lsSourceCode = getSourceCode();
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if (isJSONSuccess(poJSON)) {
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
                    , lsSourceCode
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if (isJSONSuccess(poJSON)) {
                
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
        
        poJSON = setJSON("success","success");
        return poJSON;
    }
    /**
     * Instantiates a new TransactionAttachment controller.
     */
    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }
    /**
     * Retrieves the attachment record at the specified index.
     * @param row The zero-based index of the attachment.
     */
    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }
    /**
     * Returns the total count of attachments in the current list.
     */
    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }
    /**
     * Appends a new, empty attachment record to the collection.
     * @return A {@link JSONObject} indicating the success of the addition.
     * @throws SQLException, GuanzonException if initialization fails.
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
                poJSON = setJSON("error", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
     * Removes a new attachment or marks an existing one as Inactive.
     * @param fnRow Index of the attachment to remove/deactivate.
     * @return Result status as a {@link JSONObject}.
     */    
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON = setJSON("error", "No transaction attachment to be removed.");
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
     * Adds an attachment by filename or reactivates it if it already exists as Inactive.
     * @param fFileName The name of the file to add.
     * @return The index of the added or reactivated attachment.
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
     * Copies a file from the source path to the system's temporary attachment directory.
     * <p>
     * Includes a retry mechanism that attempts to re-copy the file up to 5 times 
     * if the target file is not immediately detected after the initial operation.
     * 
     * @param fsPath The absolute path of the source file to be copied.
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

            //check if file is existing
            int lnChecker = 0;
            File file = new File(targetDir+ "/" + source.getFileName());
            System.out.println("File Path : " + file.getPath());
            while(!file.exists() && lnChecker < 5){
                Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);  
                System.out.println("Re-Copying... " + lnChecker);
                lnChecker++;
            }
            
            if(!file.exists()){
                System.out.println("File did not copy!");
                return;
            } else {
                System.out.println("File copied successfully!");
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if a filename already exists in the attachment database records.
     * 
     * @param fsFileName The name of the file to validate.
     * @return A {@link JSONObject} containing an error message if the filename exists, 
     *         otherwise an empty success object.
     * @throws SQLException, GuanzonException If a database access error occurs.
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
                        poJSON = setJSON("error", "File name already exist in database.\nTry changing the file name to upload.");
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
    * Generates the next sequential voucher number for the current branch.
    * 
    * This method retrieves the latest voucher number from the database, increments 
    * it by one, and returns it as a zero-padded 8-digit string. If no records are 
    * found, it returns a default starting voucher number.
    * 
    * @return The next formatted 8-digit voucher number.
    * @throws SQLException If a database access error occurs.
    */
    public String getVoucherNo() throws SQLException {
        String lsSQL = "SELECT sVoucherx FROM "+ Master().getTable();
        //Branch code is not stated in BR
        //Do not include branch code according to ma'am grace 03/28/2026
//        lsSQL = MiscUtil.addCondition(lsSQL,
//                "sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode()));

        lsSQL = MiscUtil.addCondition(lsSQL,
                "sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())); //get voucher no account to company ma'am she 03-31-2026
        lsSQL = lsSQL + " ORDER BY sVoucherx DESC LIMIT 1";

        String branchVoucherNo = PettyCashDisbursementStatus.DEFAULT_VOUCHER_NO;  // default value

        ResultSet loRS = null;
        try {
            System.out.println("EXECUTING SQL :  " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS != null && loRS.next()) {
                String sSeries = loRS.getString("sVoucherx");
                if (sSeries != null && !sSeries.trim().isEmpty()) {
                    long voucherNumber = Long.parseLong(sSeries);
                    voucherNumber += 1;
                    branchVoucherNo = String.format("%08d", voucherNumber); // format to 6 digits
                }
            }
        } finally {
            MiscUtil.close(loRS);  // Always close the ResultSet
        }
        return branchVoucherNo;
    }
    /**
    * Returns the master record model for the current cash disbursement transaction.
    * 
    * @return The {@link Model_PettyCash_Disbursement} instance representing the transaction header.
    */
    @Override
    public Model_PettyCash_Disbursement Master() { 
        return (Model_PettyCash_Disbursement) poMaster; 
    }
    /**
    * Returns the detail record model at the specified row index.
    * 
    * @param row The index of the detail row to retrieve.
    * @return The {@link Model_PettyCash_Disbursement_Detail} instance for the given row.
    */
    @Override
    public Model_PettyCash_Disbursement_Detail Detail(int row) {
        return (Model_PettyCash_Disbursement_Detail) paDetail.get(row); 
    }
    /**
    * Adds a new detail row to the transaction list after validating the current entries.
    * 
    * This method ensures that a new row can only be added if the preceding row has a 
    * valid "Particular" item selected. If the last row is incomplete, an error is returned.
    * 
    * @return A JSONObject indicating the result of the operation.
    * @throws CloneNotSupportedException If an error occurs while creating the new detail model.
    */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if ((Detail(getDetailCount() - 1).getParticularId() == null || "".equals(Detail(getDetailCount() - 1).getParticularId()))){
                poJSON = new JSONObject();
                poJSON = setJSON("error", "Last row has empty item.");
                return poJSON;
            }
        }

        return addDetail();
    }
    
    /**
    * Clears all detail rows, withholding tax deductions, and resets transaction-related fields.
    * 
    * This method removes all items from the details and tax deduction lists, clears attachments, 
    * resets journal entries, and clears master industry and search metadata to return to an initial state.
    * 
    * @return A JSONObject indicating "success" after all components have been cleared.
    */
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }
        
        //Reset Journal when all details was removed
        paAttachments = new ArrayList<>();
//        setSearchIndustry("");
//        setSearchPayee("");
//        Master().setIndustryId("");
        Master().setCreditedTo("");
        Master().setRemarks("");
        initFields();
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
    * Retrieves a specific cash disbursement record from the transaction list.
    * 
    * @param row The index of the record to retrieve.
    * @return The Model_PettyCash_Disbursement instance at the specified row.
    */
    public Model_PettyCash_Disbursement TransactionList(int row) {
        return (Model_PettyCash_Disbursement) paMaster.get(row);
    }
    /**
     * Returns the total number of records in the cash disbursement transaction list.
     * 
     * @return The size of the transaction list.
     */
    public int getTransactionListCount() {
        return this.paMaster.size();
    }

    /*RESET CACHE ROW SET*/
    /**
     * Resets the master record to its default initial state.
     */
    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).PettyCashDisbursementMaster();
    }
    
    /**
     * Resets the other record to its default initial state.
     */
    public void resetOthers() {
        paAttachments = new ArrayList<>();
    }

    /**
     * Completely clears the current transaction state.
     * 
     * Resets the master model, clears all detail and attachment collections, 
     * and wipes the industry and payee search filters.
     */
    public void resetTransaction(){
        resetMaster();
        Detail().clear();
        paAttachments = new ArrayList<>();
    }
   
    /**
     * Refines and validates the transaction detail list.
     * 
     * This method prunes invalid rows (those with empty particulars or zero amounts for new records) 
     * and automatically appends a new detail row if the list is empty or the last entry is valid.
     * 
     * @throws CloneNotSupportedException If an error occurs while adding a new detail row.
     */
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getParticularId() == null || "".equals(Detail(lnCtr).getParticularId())
               ) {
                deleteDetail(lnCtr);
            } 
            lnCtr--;
        }
            
        if ((getDetailCount() - 1) >= 0) {
            if (
                (Detail(getDetailCount() - 1).getParticularId() != null && !"".equals(Detail(getDetailCount() - 1).getParticularId()))
                && Detail(getDetailCount() - 1).getAmount() > 0.0000
                ) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }
    /**
     * Sets default master record values for a new transaction.
     * 
     * Configures the branch, industry, and company identifiers, sets the current 
     * server date, and initializes the status to {@link PettyCashDisbursementStatus#OPEN}.
     * 
     * @return A {@link JSONObject} indicating the initialization result.
     */
    @Override
    public JSONObject initFields() {
        //Put initial model values here/
        poJSON = new JSONObject();
        try {
            poJSON = new JSONObject();
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setDepartmentRequest(Master().getDepartmentRequest());
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(PettyCashDisbursementStatus.OPEN);
            Master().setVoucherNo(getVoucherNo());

        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Validates the transaction record using the {@link CashAdvanceValidator}.
     * 
     * @param status The target transaction status to be validated against.
     * @return A {@link JSONObject} containing success or specific validation error messages.
     */
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = new PettyCashDisbursementValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    
    /**
     * Prepares and validates the transaction data before committing to the database.
     * 
     * This method performs final integrity checks, generates transaction numbers for new records, 
     * prunes empty detail rows, and synchronizes metadata across master, details, and 
     * attachments. It also handles attachment filename collisions by renaming duplicates 
     * and triggers the file upload process for unsent attachments.
     * 
     * @return A {@link JSONObject} indicating success or detailing validation/upload errors.
     * @throws SQLException, GuanzonException, CloneNotSupportedException 
     *          If an error occurs during data processing, file operations, or validation.
     */
    @Override
    public JSONObject willSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        
        /*Put system validations and other assignments here*/
        System.out.println("Class Edit Mode : " + getEditMode());
        System.out.println("Master Edit Mode : " + Master().getEditMode());
        //Re-set the transaction no and voucher no
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
            Master().setVoucherNo(getVoucherNo());
        }
        
        poJSON = isEntryOkay(Master().getTransactionStatus());
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        //Validate Detail
        for(int lnCtr = 0;lnCtr <= getDetailCount()-1;lnCtr++){
            if ((Detail(lnCtr).getParticularId() == null || "".equals(Detail(lnCtr).getParticularId()))
                ){
                if(Detail(lnCtr).getAmount() > 0.0000){
                    poJSON = setJSON("error", "Particular cannot be empty at row " + (lnCtr+1) );
                    return poJSON;
                }
            }
        }
        
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions
            String lsDetailNo = (String) item.getValue("sPrtclrID");
            double lsAmount = Double.parseDouble(String.valueOf(item.getValue("nAmountxx")));
            if ((lsAmount == 0.0000 || (lsDetailNo == null || "".equals(lsDetailNo)))){
                if(item.getEditMode() == EditMode.ADDNEW){
                    detail.remove(); // Correctly remove the item
                } else {
                    item.setValue("cReversex", PettyCashDisbursementStatus.Reverse.EXCLUDE);
                }
            }
        }
        
        if (getDetailCount() <= 0) {
            poJSON = setJSON("error", "Transaction amount cannot be zero.");
            return poJSON;
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with transaction amount detail
            if (Detail(0).getAmount() == 0.0000) {
                poJSON = setJSON("error", "Transaction amount cannot be zero.");
                return poJSON;
            }
        }
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        //Recompute amounts
        computeFields(false);
        
        Master().setModifiedBy(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(Master().getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));

            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()){
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
                poJSON = setJSON("error", MiscUtil.getException(ex));
                return poJSON;
            }

        }
        
        System.out.println("--------------------------WILL SAVE---------------------------------------------");
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            System.out.println("COUNTER : " + lnCtr);
            System.out.println("Transaction No : " + Detail(lnCtr).getTransactionNo());
            System.out.println("Particular ID : " + Detail(lnCtr).getParticularId());
            System.out.println("Amount : " + Detail(lnCtr).getAmount());
            System.out.println("-----------------------------------------------------------------------");
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Executes business rule validations prior to the final save.
     * 
     * @return A {@link JSONObject} containing the validation result.
     * @throws CloneNotSupportedException, SQLException, GuanzonException If validation fails.
     */
    @Override
    public JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put saving business rules here*/
        return isEntryOkay(PettyCashDisbursementStatus.OPEN);
    }

    /**
     * Handles the saving of supplementary data, specifically transaction attachments.
     * 
     * Iterates through the attachment list and commits any new or modified records 
     * to the database after updating audit metadata (User ID and Server Date).
     * 
     * @return A {@link JSONObject} indicating the success or failure of the auxiliary save.
     */
    @Override
    public JSONObject saveOthers() {
        try {
            System.out.println("--------------------------SAVE OTHERS---------------------------------------------");
            System.out.println("Class Edit Mode : " + getEditMode());
            System.out.println("Master Edit Mode : " + Master().getEditMode());
            System.out.println("-----------------------------------------------------------------------");
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            System.out.println("COUNTER : " + lnCtr);
            System.out.println("Transaction No : " + Detail(lnCtr).getTransactionNo());
            System.out.println("Particular ID : " + Detail(lnCtr).getParticularId());
            System.out.println("Amount : " + Detail(lnCtr).getAmount());
            System.out.println("-----------------------------------------------------------------------");
            }
            
            //Save Attachments
            System.out.println("-----------------------------SAVE TRANSACTION ATTACHMENT------------------------------------------");
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                }
            }
            System.out.println("-----------------------------------------------------------------------");
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }
            
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Initializes the base SQL query used for browsing Cash Advance records.
     * 
     * This method constructs a complex SELECT statement joining tables for Company, 
     * Industry, Department, Client Master, Cash Fund, and Branch. It dynamically 
     * appends filtering conditions based on the current transaction status {@code psTranStat}, 
     * supporting both single-status equality and multi-status {@code IN} clauses.
     */
    @Override
    public void initSQL() {
        String lsCondition = "";
        
        if(psTranStat != null && !"".equals(psTranStat)){
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }

                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        SQL_BROWSE = " SELECT "
                    + "   a.sTransNox "
                    + " , a.sCompnyID "
                    + " , a.sBranchCd "
                    + " , a.sIndstCdx "
                    + " , a.nEntryNox "
                    + " , a.dTransact "
                    + " , a.sVoucherx "
                    + " , a.sPettyIDx "
                    + " , a.sClientID "
                    + " , a.sPayeeNme "
                    + " , a.sCrdtedTo "
                    + " , a.sDeptReqs "
                    + " , a.sAddressx "
                    + " , a.sRemarksx "
                    + " , a.sReferNox "
                    + " , a.nTranTotl "
                    + " , a.cVchrPrnt "
                    + " , a.cCollectd "
                    + " , a.cTranStat "
                    + " , a.sApproved "
                    + " , b.sCompnyNm AS sCompanyx    "
                    + " , c.sDescript AS sIndustry    "
                    + " , d.sDeptName AS sDeptName    "
                    + " , e.sCompnyNm AS sPayeexxx    "
                    + " , f.sPettyDsc AS sPettyDsc    "
                    + " , g.sBranchNm AS sBranchNm    "
                    + " , h.sCompnyNm AS sCreditTo    "
                    + " FROM PettyCash_Disbursement a "  
                    + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID       "
                    + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx      "
                    + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptReqs    "
                    + " LEFT JOIN Client_Master e ON e.sClientID = a.sClientID "
                    + " LEFT JOIN PettyCash f ON f.sPettyIDx = a.sPettyIDx     "
                    + " LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd        "
                    + " LEFT JOIN Client_Master h ON h.sClientID = a.sCrdtedTo ";
        if(lsCondition != null && !"".equals(lsCondition)){
            SQL_BROWSE = MiscUtil.addCondition(SQL_BROWSE, lsCondition);
        }
    }
    
    public String cashAdvanceSQL() {
        return  " SELECT        "
                + " a.sTransNox   "
                + " , a.dTransact "
                + " , a.sCashFIDx "
                + " , a.cTranStat "
                + " , b.sCompnyNm AS sCompanyx "
                + " , c.sDescript AS sIndustry "
                + " , d.sDeptName AS sDeptName "
                + " , e.sCompnyNm AS sPayeexxx "
                + " , f.sCashFDsc AS sCashFund "
                + ", g.sBranchNm AS sBranchNm "
                + " FROM CashAdvance a         "
                + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID       "
                + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx      "
                + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptReqs    "
                + " LEFT JOIN Client_Master e ON e.sClientID = a.sClientID "
                + " LEFT JOIN CashFund f ON f.sCashFIDx = a.sCashFIDx      "
                + " LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd ";
    }
    
    /**
     * Upload Attachment
     * @param instance
     * @param access 
     * @param fnRow 
     * @return  
     * @throws java.lang.Exception 
     */
    /**
     * Uploads a specific transaction attachment to the web server.
     * <p>
     * This method verifies the file's existence (handling renames), generates an MD5 hash 
     * for data integrity, and transmits the file via the {@link WebFile} API. Upon successful 
     * upload, it updates the record's hash and sets the send status to "1" (Sent).
     * 
     * @param instance The application driver instance.
     * @param access The access token for web service authentication.
     * @param fnRow The index of the attachment in the local collection.
     * @param fsOriginalFileName The original filename to use as a fallback if the new file is missing.
     * @return A {@link JSONObject} containing the upload result status.
     * @throws Exception If an error occurs during file reading, encoding, or web transmission.
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
                poJSON = setJSON("error", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
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
                poJSON = setJSON("error", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
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
     * Converts a file's content into a Base64 encoded string.
     * 
     * @param file The file object to be encoded.
     * @return A Base64 string representation of the file.
     * @throws Exception If an I/O error occurs during file reading.
     */
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
     }
         
    private static JSONObject token = null;
    /**
     * Retrieves a valid access token, refreshing it if it has expired.
     * <p>
     * The token is considered stale if it was created more than 25 minutes ago. 
     * If expired, it triggers an external request to update the token file before 
     * returning the new access key.
     * 
     * @param access The file path to the JSON formatted token storage.
     * @return The access key string, or {@code null} if the file cannot be read or parsed.
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
    
    public JSONObject getUpdateStatusBy(String fsStatus) throws SQLException, GuanzonException {
        String lsUpdateBy = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM "+Master().getTable()+" a "
                     + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = "+ SQLUtil.toSQL(Master().getTable())
                     + " AND b.cRefrStat = "+ SQLUtil.toSQL(fsStatus) ;
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL STATUS : "+fsStatus+" : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsUpdateBy = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsUpdateBy = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsDate =  dModified.format(formatter);
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
        poJSON.put("sUpdateByx", lsUpdateBy);
        poJSON.put("sUpdateDte", lsDate);
        return poJSON;
    }
    
    public JSONObject printTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        JasperPrint masterPrint = null;       
        JasperReport jasperReport = null;      
        pbShowed = false; 
        pbIsPrinted = false;
        
        try {
            String watermarkPath = "";
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/PettyCashDisbursement.jrxml";
            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            watermarkPath = System.getProperty("sys.default.path.config") + "/Reports/images/"; 
            poJSON = OpenTransaction(Master().getTransactionNo());
            if ("error".equals((String) poJSON.get("result"))){
                proceedAfterViewerClosed();
                return poJSON;
            }
            poJSON = UpdateTransaction();
            if ("error".equals((String) poJSON.get("result"))){
                proceedAfterViewerClosed();
                return poJSON;
            }

            Map<String, Object> params = new HashMap<>();
            System.out.println("voucher No : " + Master().getVoucherNo());
            System.out.println("transaction No : " + Master().getTransactionNo());
            System.out.println("payee : " + Master().getPayeeName());
            params.put("sVouchrNo", Master().getVoucherNo());
            params.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            params.put("sPayeeNme", Master().getPayeeName());
            params.put("sCreditTo", Master().Credited().getCompanyName());
            params.put("nAdvAmntx", Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getTransactionTotal(), false).replace(",", "")));

            //Set Default value to empty to prevent null in display
            params.put("sEncoder","");
            params.put("sConfirmer","");
            params.put("sApprover","");

            //Get Encoder
            JSONObject loJSONEntry = getEntryBy();
            if(!isJSONSuccess(loJSONEntry)){
                return loJSONEntry;
            }
            if((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))){
                params.put("sEncoder",(String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte"))); 
            }
            //Get Confirmer
            JSONObject loJSONConfirm = getUpdateStatusBy(PettyCashDisbursementStatus.CONFIRMED);
            if(!isJSONSuccess(loJSONConfirm)){
                return loJSONConfirm;
            } else {
                if((String) loJSONConfirm.get("sUpdateByx") != null && !"".equals((String) loJSONConfirm.get("sUpdateByx"))){
                    params.put("sConfirmer", (String) loJSONConfirm.get("sUpdateByx") + " " + String.valueOf((String) loJSONConfirm.get("sUpdateDte"))); 
                }
            }
            //Get Approver
            JSONObject loJSONApprover = getUpdateStatusBy(PettyCashDisbursementStatus.APPROVED);
            if(!isJSONSuccess(loJSONApprover)){
                return loJSONApprover;
            } else {
                if((String) loJSONApprover.get("sUpdateByx") != null && !"".equals((String) loJSONApprover.get("sUpdateByx"))){
                    params.put("sApprover", (String) loJSONApprover.get("sUpdateByx") + " " + String.valueOf((String) loJSONApprover.get("sUpdateDte"))); 
                }
            }

            if(Master().isPrinted()){
                watermarkPath = watermarkPath + "reprint.png"; 
            } else {
                watermarkPath = watermarkPath + "none.png" ; 
            }
            params.put("watermarkImagePath", watermarkPath);
            List<TransactionDetail> Details = new ArrayList<>();
            List<String> laParticular = new ArrayList<>();
            String lsParticular = "";
            for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
                lsParticular = Detail(lnCtr).Particular().getDescription().toUpperCase();
                if(!laParticular.contains(lsParticular)){
                    laParticular.add(lsParticular);
                }
            }

            //Particular
            lsParticular = ""; //Reset value
            for(int lnCtr = 0;lnCtr <= laParticular.size()-1;lnCtr++){
                if(lsParticular.isEmpty()){
                    lsParticular = laParticular.get(lnCtr);
                } else {
                    if(lnCtr < laParticular.size()-1){
                        lsParticular = lsParticular + " , " + laParticular.get(lnCtr);
                    } else {
                        if(lnCtr > 2){
                            lsParticular = lsParticular + ", AND " + laParticular.get(lnCtr);
                        } else {
                            lsParticular = lsParticular + " AND " + laParticular.get(lnCtr);
                        }
                    }
                }
            }

            Details.add(new TransactionDetail(
                    1,
                    lsParticular,
                    "",
                    Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getTransactionTotal(), false).replace(",", ""))
            ));

            JasperPrint currentPrint = JasperFillManager.fillReport(
                    jasperReport,
                    params,
                    new JRBeanCollectionDataSource(Details)
            );
            if (currentPrint != null) {
                CustomJasperViewer viewer = new CustomJasperViewer(currentPrint);
                viewer.setVisible(true);
                viewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        proceedAfterViewerClosed();
                    }

                });
            }
            
        } catch (JRException | SQLException | GuanzonException | ScriptException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } 

        return poJSON;
    }
    
    private boolean pbShowed = false;
    private void proceedAfterViewerClosed() {
        Platform.runLater(() -> {
            System.out.println("SHOWED!!!!!!!!!!!");

            if (pbShowed) {
                return;
            } else {
                pbShowed = true;
            }

            if ("error".equals((String) poJSON.get("result"))) {
                ShowMessageFX.Warning(null, "Computerized Accounting System",
                    (String) poJSON.get("message"));
            } else {
                if (pbIsPrinted) {
                    ShowMessageFX.Information(null, "Computerized Accounting System",
                        "Transaction Printed Successfully");
                } else {
                    ShowMessageFX.Warning(null, "Computerized Accounting System",
                        "Printing was canceled by the user.");
                }
            }
        });
    }

    private void showViewerAndWait(JasperPrint print) {
        // create viewer on Swing thread
        final CustomJasperViewer viewer = new CustomJasperViewer(print);
        viewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final CountDownLatch latch = new CountDownLatch(1);
        viewer.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                latch.countDown();             // release when user closes
                proceedAfterViewerClosed();
            }
        });

        javax.swing.SwingUtilities.invokeLater(() -> viewer.setVisible(true));

        try {
            latch.await();                     // 🔴 blocks here until viewer closes
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // keep interruption status
        }
    }

    public static class TransactionDetail {

        private final Integer nRowNo;
        private final String sParticular;
        private final String sRemarks;
        private final Double nTotalAmount;

        public TransactionDetail(Integer rowNo, String particular, String remarks, Double totalAmt) {
            this.nRowNo = rowNo;
            this.sParticular = particular;
            this.sRemarks = remarks;
            this.nTotalAmount = totalAmt;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsParticular() {
            return sParticular;
        }

        public String getsRemarks() {
            return sRemarks;
        }

        public Double getnTotalAmount() {
            return nTotalAmount;
        }
    }
    
    private boolean pbIsPrinted = false;
    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(final JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        /* ---- toolbar patch ------------------------------------------ */
        private JSONObject customizePrintButton(final JasperPrint jasperPrint) {

            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "JRViewer not found!");
                    return poJSON;
                }
                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {

                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {

                                final JButton button = (JButton) toolbar.getComponent(j);

                                if ("Print".equals(button.getToolTipText())) {
                                    /* remove existing handlers */
                                    ActionListener[] old = button.getActionListeners();
                                    for (int k = 0; k < old.length; k++) {
                                        button.removeActionListener(old[k]);
                                    }

                                    /* add our own (anonymous inner‑class, not lambda) */
                                    button.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                pbIsPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                                if (pbIsPrinted) {
                                                    Master().isPrinted(true);
                                                    Master().setModifiedBy(poGRider.Encrypt(poGRider.getUserID()));
                                                    Master().setModifiedDate(poGRider.getServerDate());
                                                    poJSON = SaveTransaction();
                                                    if(!"error".equals((String) poJSON.get("result"))){
                                                        poJSON.put("result", "success");
                                                        poJSON.put("message",  "Transaction Printed Successfully.");
                                                    }
                                                    CustomJasperViewer.this.dispose();
                                                } else {
                                                    poJSON.put("result", "error");
                                                    poJSON.put("message",  "Printing was canceled by the user.");
                                                }
                                            } catch (SQLException | GuanzonException | CloneNotSupportedException | JRException ex) {
                                                poJSON.put("result", "error");
                                                poJSON.put("message",  "Print Failed: " + ex.getMessage());
                                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    });
                                } else {
                                    // Disable all other buttons
                                    button.setEnabled(false);
                                }
                            }
                        }
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
                poJSON.put("result", "error");
                poJSON.put("message", "Error customizing print button: " + e.getMessage());
            }
            
            return poJSON;
        }

        private void warnAndRefocus(final String m) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
//                    ShowMessageFX.Warning(m, "Print Disbursement", null);
                    poJSON.put("result", "error");
                    poJSON.put("message", m);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            CustomJasperViewer.this.toFront();
                        }
                    });
                }
            });
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] comps = ((Container) parent).getComponents();
                for (int i = 0; i < comps.length; i++) {
                    JRViewer v = findJRViewer(comps[i]);
                    if (v != null) {
                        return v;
                    }
                }
            }
            return null;
        }
    }
    
    
    /**
    * Displays the status history of a Cash Advance transaction.
    *
    * Retrieves status records, maps status codes to readable text, and
    * shows them in the UI along with entry details.
    *
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if application-specific error occurs
    * @throws Exception for other unexpected errors
    */
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case PettyCashDisbursementStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case PettyCashDisbursementStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case PettyCashDisbursementStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case PettyCashDisbursementStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case PettyCashDisbursementStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case PettyCashDisbursementStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case PettyCashDisbursementStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case PettyCashDisbursementStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case PettyCashDisbursementStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case PettyCashDisbursementStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                    }
            }
            crs.updateRow();
        }

        JSONObject loJSON = getEntryBy();
        String entryBy = "";
        String entryDate = "";

        if (isJSONSuccess(loJSON)) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        showStatusHistoryUI("Petty Cash Disbursement", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    /**
     * Retrieves the user and timestamp of who created the current transaction.
     *
     * @return JSONObject containing "sCompnyNm" (user) and "sEntryDte" (timestamp)
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if application-specific error occurs
     */
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM "+Master().getTable()+" a "
                + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo()));
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsEntry = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsEntryDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON = setJSON("error", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    /**
     * Retrieves the company name of a system user based on user ID.
     *
     * @param fsId User ID to lookup
     * @return Company name of the user
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if application-specific error occurs
     */
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL = " SELECT b.sCompnyNm from xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId));
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    lsEntry = loRS.getString("sCompnyNm");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON = setJSON("error", e.getMessage());
        }
        return lsEntry;
    }
}
