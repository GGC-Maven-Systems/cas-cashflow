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
import java.util.Objects;
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
import org.apache.commons.lang3.StringUtils;
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
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.TaxCode;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.cas.core.GLTransaction;
import ph.com.guanzongroup.cas.cashflow.model.*;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.*;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;
import ph.com.guanzongroup.cas.cashflow.validator.CashAdvanceValidator;
import ph.com.guanzongroup.cas.cashflow.validator.CashDisbursementValidator;

/**
 *
 * @author Arsiela 03/24/2026
 */
public class CashDisbursement extends Transaction {
    public String psIndustryId = "";
    public String psCompanyId = "";
    public String psDepartmentId = "";
    public String psIndustry = "";
    public String psBranch = "";
    public String psPayee = "";
    public String psApprover = "";
    
    public Journal poJournal;
    public List<WithholdingTaxDeductions> paWTaxDeductions;
    public List<Model> paMaster;
    public List<Model> paCashAdvances;
    public List<TransactionAttachment> paAttachments;
    public List<JournalProposal> paJournalProposal; //Arsiela 06-27-2026
    
    /**
    * Initializes a new Cash Disbursement transaction.
    * 
    * This method sets the source code, instantiates the master, detail, and journal 
    * controllers, and resets all associated data lists (Cash Advances, W-Tax, and Attachments).
    * 
    * @return a {@link JSONObject} containing the status of the initialization.
    * @throws SQLException if a database access error occurs.
    * @throws GuanzonException if a business logic or validation error occurs.
    */
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "CFDv";

        poMaster = new CashflowModels(poGRider).CashDisbursementMaster();
        poDetail = new CashflowModels(poGRider).CashDisbursementDetail();
        poJournal = new CashflowControllers(poGRider, logwrapr).Journal();

        paMaster = new ArrayList<Model>();
        paCashAdvances = new ArrayList<Model>();
        paWTaxDeductions = new ArrayList<WithholdingTaxDeductions>();
        paAttachments = new ArrayList<>();
        paJournalProposal = new ArrayList<JournalProposal>();
        psApprover = "";
        setApproving("");
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
        if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){ //BR: Authorized users from the Finance
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not authorized to create the transaction." );
            return poJSON;
        }

        String lsUserId = poGRider.getUserID();
        String lsPosition = checkPosition(CashDisbursementStatus.OPEN, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not authorized to create a disbursement voucher." );
            return poJSON;
        }

        resetMaster();
        Detail().clear();
        resetJournal();
        WTaxDeduction().clear();
        
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
        
        poJSON = populateJournal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        poJSON = loadJournalProposal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal proposal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateWithholdingTaxDeduction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load withholding tax deduction.\n" + (String) poJSON.get("message"));
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
        if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){ //BR: Authorized users from the Finance
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not authorized to update the transaction." );
            return poJSON;
        }
        
       poJSON = updateTransaction();
       if (!isJSONSuccess(poJSON)) {
           poJSON = setJSON((String) poJSON.get("result"),"Unable to update transaction.\n" + (String) poJSON.get("message"));
           return poJSON;
       }

        poJSON = populateJournal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

       poJSON = loadJournalProposal();
       if (!"success".equals((String) poJSON.get("result"))) {
           poJSON.put("message", "Unable to load journal proposal.\n" + (String) poJSON.get("message"));
           return poJSON;
       }
        
        poJSON = populateWithholdingTaxDeduction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load withholding tax deduction.\n" + (String) poJSON.get("message"));
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
            psApprover = lsUserIDxx;
        }   
        
        poJSON = setJSON("success","success");
        return poJSON;
    }
    
    /**
    * Checks if a user has an allowed position for a specific transaction status.
    *
    * @param fsUserId user ID
    * @return department name if authorized, otherwise empty string
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if query execution fails
    */
    public String checkApprover(String fsUserId) throws SQLException, GuanzonException{
        String lsDepartment = "";
        String lsSQL = " SELECT   " +
                    "  a.sUserIDxx, " +
                    "  d.sCompnyNm, " +
                    "  e.sDeptName, " +
                    "  c.sPositnNm, " +
                    "  b.dFiredxxx, " +
                    "  b.sDeptIDxx, " +
                    "  b.sPositnID " +
                    "FROM xxxSysUser a " +
                    "LEFT JOIN Employee_Master001 b ON b.sEmployID = a.sEmployNo " +
                    "LEFT JOIN Position c ON c.sPositnID = b.sPositnID  " +
                    "LEFT JOIN Client_Master d ON d.sClientID = b.sEmployID  " +
                    "LEFT JOIN Department e ON e.sDeptIDxx = b.sDeptIDxx  ";
        
        lsSQL = MiscUtil.addCondition(lsSQL,
                " a.sUserIDxx = " + SQLUtil.toSQL(fsUserId)
//                + " AND b.sDeptIDxx = " + SQLUtil.toSQL(System.getProperty("sys.dept.finance")) 
                 );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sDeptIDxx") != null && !"".equals(loRS.getString("sDeptIDxx"))){
                        lsDepartment = loRS.getString("sDeptIDxx");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            return lsDepartment;
        }
        return lsDepartment;
    }

    /**
     * Checks if a user has an allowed position for a specific transaction status.
     *
     * @param fsStatus transaction status
     * @param fsUserId user ID
     * @return position name if authorized, otherwise empty string
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if query execution fails
     */
    public String checkPosition(String fsStatus, String fsUserId) throws SQLException, GuanzonException{
        String lsSQL = " SELECT   " +
                "  a.sUserIDxx, " +
                "  d.sCompnyNm, " +
                "  e.sDeptName, " +
                "  c.sPositnNm, " +
                "  b.dFiredxxx, " +
                "  b.sDeptIDxx, " +
                "  b.sPositnID " +
                "FROM xxxSysUser a " +
                "LEFT JOIN Employee_Master001 b ON b.sEmployID = a.sEmployNo " +
                "LEFT JOIN Position c ON c.sPositnID = b.sPositnID  " +
                "LEFT JOIN Client_Master d ON d.sClientID = b.sEmployID  " +
                "LEFT JOIN Department e ON e.sDeptIDxx = b.sDeptIDxx  ";

        lsSQL = MiscUtil.addCondition(lsSQL,
                " a.sUserIDxx = " + SQLUtil.toSQL(fsUserId)
                        + " AND b.sDeptIDxx = " + SQLUtil.toSQL(System.getProperty("sys.dept.finance"))
        );
        String lsPosition = "";
        switch(fsStatus){
            case CashDisbursementStatus.OPEN:
                //Allow to entry cash disbursement only when user is form finance department
                break;
            //Who can modify and confirm the transaction
            case CashDisbursementStatus.CONFIRMED:
                lsPosition = "%Payable%";
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition) );
                break;
            case CashDisbursementStatus.VERIFIED:
            case CashDisbursementStatus.APPROVED:
            case CashDisbursementStatus.RETURNED:
                //Who can verify and approve the transaction
                lsPosition = "%Account%";
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition)
                        + " AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%")
                );
                break;
            case CashDisbursementStatus.CANCELLED:
                if(CashDisbursementStatus.VERIFIED.equals(Master().getTransactionStatus())){
                    //Who can cancel when the transaction status was verified
                    lsPosition = "%Account%";
                    lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition)
                            + " AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%")
                    );
                } else if(CashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())){
                    lsPosition = "%Payable%";
                    lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition) );
                }
                break;
        }
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sPositnNm") != null && !"".equals(loRS.getString("sPositnNm"))){
                        return loRS.getString("sPositnNm");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return "";
    }

    /**
     * Sets the form identifier for the transaction.
     *
     * @param fsForm the form name or code
     */
    private String psForm = "";
    public void setForm(String fsForm){
        psForm = fsForm;
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
        
        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        String lsCurrentStatus = loObject.getTransactionStatus();
        if (isUpdateRestricted(lsCurrentStatus, isEntry)) {
            return setTransactionAlreadyUpdatedJSON(lsCurrentStatus);
        }
        
        if(!Objects.equals(lsCurrentStatus, Master().getTransactionStatus())){
            poJSON = OpenTransaction(Master().getTransactionNo());
            if (!isJSONSuccess(poJSON)) {
                poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
                return poJSON;
            }
        }
        
        // Prevent stale form state from affecting succeeding checks.
//        psForm = "";
        poJSON = setJSON("success", "success");
        return poJSON;
    }

    private JSONObject setTransactionAlreadyUpdatedJSON(String fsStatus) {
        return setJSON("error", "Transaction status was already " + getStatus(fsStatus) + "!\nCheck transaction history.");
    }

    private boolean isUpdateRestricted(String fsStatus, boolean isEntry) {
        switch(fsStatus){
            case CashDisbursementStatus.VOID:
            case CashDisbursementStatus.CANCELLED:
            case CashDisbursementStatus.APPROVED:
                return true; //Do not allow to update
            case CashDisbursementStatus.CONFIRMED:
                return isEntry
                        || CashDisbursementStatus.APPROVED.equals(psForm); //Do not allow to update when entry is true
            case CashDisbursementStatus.VERIFIED://Do not allow to update verified transaction from entry, confirm and verified form
                return isEntry
                        || CashDisbursementStatus.CONFIRMED.equals(psForm)
                        || CashDisbursementStatus.VERIFIED.equals(psForm);
            case CashDisbursementStatus.RETURNED: //Do not allow to update returned transaction from entry, verified and approved form
                return isEntry
                        || CashDisbursementStatus.VERIFIED.equals(psForm)
                        || CashDisbursementStatus.APPROVED.equals(psForm);
            default:
                return false;
        }
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
            case CashDisbursementStatus.VOID:
                return "Voided";
            case CashDisbursementStatus.CANCELLED:
                return "Cancelled";
            case CashDisbursementStatus.APPROVED:
                return "Approved";
            case CashDisbursementStatus.CONFIRMED:
                return "Confirmed";
            case CashDisbursementStatus.OPEN:
                return "Open";
            case CashDisbursementStatus.VERIFIED:
                return "Verified";
            case CashDisbursementStatus.RETURNED:
                return "Return";
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
            case CashDisbursementStatus.VOID:
                return current.equals(CashDisbursementStatus.OPEN); //Allow void when current status is open

            case CashDisbursementStatus.CANCELLED:
                return current.equals(CashDisbursementStatus.CONFIRMED)
                        || current.equals(CashDisbursementStatus.RETURNED)
                        || current.equals(CashDisbursementStatus.VERIFIED); //Allow void when current status is confirmed

            case CashDisbursementStatus.CONFIRMED:
                return current.equals(CashDisbursementStatus.OPEN)
                        || current.equals(CashDisbursementStatus.RETURNED); //Allow confirm when current status is open

            case CashDisbursementStatus.VERIFIED:
                return current.equals(CashDisbursementStatus.CONFIRMED); //Allow verify when current status is confirm

            case CashDisbursementStatus.RETURNED:
                return current.equals(CashDisbursementStatus.VERIFIED); //Allow return when current status is verify

            case CashDisbursementStatus.APPROVED:
                return current.equals(CashDisbursementStatus.VERIFIED); //Allow approve when current status is confirmed

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
        String lsStatus = CashDisbursementStatus.CONFIRMED;
        
        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
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
            //1. Check the position of the current user
            String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
            if(lsPosition1 == null || "".equals(lsPosition1) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            //2. Check the position of the approving officer
            String lsPosition = checkPosition(lsStatus, psApprover);
            if(lsPosition == null || "".equals(lsPosition) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }
            
//            String lsDepartment = poGRider.getDepartment();
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                lsDepartment = checkApprover(psApprover);
//            }
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//                poJSON.put("result", "error" );
//                poJSON.put("message", "User or approving officer is not authorized to confirm the transaction." );
//                return poJSON;
//            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
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
        poJSON = setJSON("success", "Transaction confirmed successfully.");
        return poJSON;
    }

    /**
     * Verify the current cash disbursement transaction.
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
    public JSONObject VerifyTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = CashDisbursementStatus.VERIFIED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }

        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already verify.");
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

        poJSON = verifyJournals(lsStatus,false);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(!pbWthParent){
            //1. Check the position of the current user
            String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
            if(lsPosition1 == null || "".equals(lsPosition1) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

//            psApprover = poGRider.getUserID();
//            poJSON = callApproval();
//            if (!isJSONSuccess(poJSON)) {
//                return poJSON;
//            }
//
//            //2. Check the position of the approving officer
//            String lsPosition = checkPosition(lsStatus, psApprover);
//            if(lsPosition == null || "".equals(lsPosition) ){
//                poJSON.put("result", "error" );
//                poJSON.put("message", "User is not an authorized officer." );
//                return poJSON;
//            }

//            String lsDepartment = poGRider.getDepartment();
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                lsDepartment = checkApprover(psApprover);
//            }
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//                poJSON.put("result", "error" );
//                poJSON.put("message", "User or approving officer is not authorized to confirm the transaction." );
//                return poJSON;
//            }
        }

        poGRider.beginTrans("UPDATE STATUS", "VerifyTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
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
        poJSON = setJSON("success", "Transaction verify successfully.");
        return poJSON;
    }



    /**
     * Return the current cash disbursement transaction.
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
    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = CashDisbursementStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }

        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already returned.");
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
            //1. Check the position of the current user
            String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
            if(lsPosition1 == null || "".equals(lsPosition1) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            //2. Check the position of the approving officer
            String lsPosition = checkPosition(lsStatus, psApprover);
            if(lsPosition == null || "".equals(lsPosition) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

//            String lsDepartment = poGRider.getDepartment();
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                lsDepartment = checkApprover(psApprover);
//            }
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//                poJSON.put("result", "error" );
//                poJSON.put("message", "User or approving officer is not authorized to confirm the transaction." );
//                return poJSON;
//            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
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
        poJSON = setJSON("success", "Transaction returned successfully.");
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

        String lsStatus = CashDisbursementStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
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

        poJSON = verifyJournals(lsStatus,false);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if(!pbWthParent){
            //1. Check the position of the current user
            String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
            if(lsPosition1 == null || "".equals(lsPosition1) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            //2. Check the position of the approving officer
            String lsPosition = checkPosition(lsStatus, psApprover);
            if(lsPosition == null || "".equals(lsPosition) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }
            
//            String lsDepartment = poGRider.getDepartment();
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                lsDepartment = checkApprover(psApprover);
//            }
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//                poJSON.put("result", "error" );
//                poJSON.put("message", "User or approving officer is not authorized to approve the transaction." );
//                return poJSON;
//            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ApproveTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Related transaction to cash disbursement
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        try {
            //Update CASH FUND Balance and Generate Ledger
            LocalDate transDate = strToDate(xsDateShort(Master().getTransactionDate()));
            CashFundTrans loTrans = new CashFundTrans(poGRider);
            loTrans.InitTransaction(Master().getCashFundId(), Master().getBranchCode(), Master().getDepartmentRequest());
            loTrans.Disbursement(Master().getTransactionNo(), transDate,  Master().getTransactionTotal(), false);
            
            GLTransaction loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
            System.out.println("----------JOURNAL PROPOSAL ACCOUNT MASTER / LEDGER----------");

            List<Model_Journal_Detail_Proposal> loList = new ArrayList<>();
            boolean lbMatch = false;
            if(getJournalProposalList() != null){
                //Group Journal Proposal Based on the same Branch Code, Account Code and Detail Date
                for(int lnJEP = 0; lnJEP < getJournalProposalList().size(); lnJEP++){
//                    if(JournalProposal(lnJEP).getEditMode() == EditMode.UPDATE){
                        lbMatch = false;
                        for(int lnCtr = 0; lnCtr <= JournalProposal(lnJEP).getDetailCount() - 1; lnCtr++){
                            if(JournalProposal(lnJEP).Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                                for(Model_Journal_Detail_Proposal loModel : loList){
                                    if(loModel.getAccountCode().equals(JournalProposal(lnJEP).Detail(lnCtr).getAccountCode())){
                                        if(xsDateShort(loModel.getForMonthOf()).equals(xsDateShort(JournalProposal(lnJEP).Detail(lnCtr).getForMonthOf()))){
                                            Model_Journal_Master_Proposal loJEPMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
                                            poJSON = loJEPMaster.openRecord(loModel.getTransactionNo());
                                            if ("error".equals((String) poJSON.get("result"))) {
                                                return poJSON;
                                            }         
                                            if(loJEPMaster.getBranchCode().equals(JournalProposal(lnJEP).Master().getBranchCode())){
                                                loModel.setCreditAmount(loModel.getCreditAmount() + JournalProposal(lnJEP).Detail(lnCtr).getCreditAmount());
                                                loModel.setDebitAmount(loModel.getDebitAmount()+ JournalProposal(lnJEP).Detail(lnCtr).getDebitAmount());
                                                lbMatch = true;
                                            }

                                        }
                                    }
                                }

                                if(!lbMatch){
                                    loList.add(new CashflowModels(poGRider).Journal_Detail_Proposal());
                                    loList.get(loList.size()-1).setTransactionNo(JournalProposal(lnJEP).Detail(lnCtr).getTransactionNo());
                                    loList.get(loList.size()-1).setAccountCode(JournalProposal(lnJEP).Detail(lnCtr).getAccountCode());
                                    loList.get(loList.size()-1).setCreditAmount(JournalProposal(lnJEP).Detail(lnCtr).getCreditAmount());
                                    loList.get(loList.size()-1).setDebitAmount(JournalProposal(lnJEP).Detail(lnCtr).getDebitAmount());
                                    loList.get(loList.size()-1).setForMonthOf(JournalProposal(lnJEP).Detail(lnCtr).getForMonthOf());
                                }
                            }
                        }
                    }
//                }
            }


            System.out.println("-----------------------------------");

            System.out.println("----------JOURNAL ACCOUNT MASTER / LEDGER----------");

            loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
            loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());

            if(poJournal != null){
//                if(poJournal.getEditMode() == EditMode.UPDATE){
                    for(int lnCtr = 0; lnCtr <= Journal().getDetailCount() - 1; lnCtr++){
                        if(Journal().Detail(lnCtr).isReverse()){
                            lbMatch = false;
                            for(Model_Journal_Detail_Proposal loModel : loList){
                                Model_Journal_Master_Proposal loJEPMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
                                poJSON = loJEPMaster.openRecord(loModel.getTransactionNo());
                                if ("error".equals((String) poJSON.get("result"))) {
                                    return poJSON;
                                } 

                                if(loJEPMaster.getBranchCode().equals(Journal().Master().getBranchCode())){
                                    if(loModel.getAccountCode().equals(Journal().Detail(lnCtr).getAccountCode())){
                                        if(xsDateShort(loModel.getForMonthOf()).equals(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()))){
                                            lbMatch = true;
                                            loModel.setCreditAmount(loModel.getCreditAmount() + Journal().Detail(lnCtr).getCreditAmount());
                                            loModel.setDebitAmount(loModel.getDebitAmount()+ Journal().Detail(lnCtr).getDebitAmount());
                                        }
                                    }
                                }
                            }

                            if(!lbMatch){

                                System.out.println("Branch Code : " + Journal().Master().getBranchCode());
                                System.out.println("Account Code : " + Journal().Detail(lnCtr).getAccountCode());
                                System.out.println("Date : " + Journal().Detail(lnCtr).getForMonthOf());
                                System.out.println("Debit Amount : " + Journal().Detail(lnCtr).getDebitAmount());
                                System.out.println("Credit Amount : " + Journal().Detail(lnCtr).getCreditAmount());
                                
                                //GL Transaction Account Ledger
                                loGLTrans.addDetail(Journal().Master().getBranchCode(), 
                                            Journal().Detail(lnCtr).getAccountCode(),
                                            SQLUtil.toDate(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
                                            Journal().Detail(lnCtr).getDebitAmount(), 
                                            Journal().Detail(lnCtr).getCreditAmount());
                            }
                        }
                    }
//                }
            }
            System.out.println("-----------------------------------");


            //Generate Account Ledger 
            for(Model_Journal_Detail_Proposal loModel : loList){
                if(loModel.getTransactionNo() != null && !"".equals(loModel.getTransactionNo())){
                    Model_Journal_Master_Proposal loJEPMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
                    poJSON = loJEPMaster.openRecord(loModel.getTransactionNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    } 
                    
                    System.out.println("Branch Code : " + loJEPMaster.getBranchCode());
                    System.out.println("Account Code : " + loModel.getAccountCode());
                    System.out.println("Date : " + loModel.getForMonthOf());
                    System.out.println("Debit Amount : " + loModel.getDebitAmount());
                    System.out.println("Credit Amount : " + loModel.getCreditAmount());

                    //GL Transaction Account Ledger
                    loGLTrans.addDetail(loJEPMaster.getBranchCode(), 
                                    loModel.getAccountCode(),
                                    SQLUtil.toDate(xsDateShort(loModel.getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
                                    loModel.getDebitAmount(), 
                                    loModel.getCreditAmount());
                }

            }

            poJSON = loGLTrans.saveTransaction();
            if ("Failed".equals((String) poJSON.get("result"))) {
                return poJSON;
            } 
            
//            System.out.println("----------ACCOUNT MASTER / LEDGER----------");
//            //GL Transaction Account Ledger
//            GLTransaction loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
//            loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());
//            for(int lnCtr = 0; lnCtr <= Journal().getDetailCount() - 1; lnCtr++){
////                if(Journal().Detail(lnCtr).getCreditAmount() > 0.0000 || Journal().Detail(lnCtr).getDebitAmount() > 0.0000){
//                if(Journal().Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
//                    loGLTrans.addDetail(Journal().Master().getBranchCode(), 
//                            Journal().Detail(lnCtr).getAccountCode(),
//                            SQLUtil.toDate(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
//                            Journal().Detail(lnCtr).getDebitAmount(), 
//                            Journal().Detail(lnCtr).getCreditAmount());
//                }
//            }
//            loGLTrans.saveTransaction();
//            System.out.println("-----------------------------------");
            
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

        String lsStatus = CashDisbursementStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
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

        if(CashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())){
            if(!pbWthParent){
                //1. Check the position of the current user
                String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
                if(lsPosition1 == null || "".equals(lsPosition1) ){
                    poJSON.put("result", "error" );
                    poJSON.put("message", "User is not an authorized officer." );
                    return poJSON;
                }

                psApprover = poGRider.getUserID();
                poJSON = callApproval();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }

                //2. Check the position of the approving officer
                String lsPosition = checkPosition(lsStatus, psApprover);
                if(lsPosition == null || "".equals(lsPosition) ){
                    poJSON.put("result", "error" );
                    poJSON.put("message", "User is not an authorized officer." );
                    return poJSON;
                }
            }
        }
        
//        String lsDepartment = poGRider.getDepartment();
//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//            lsDepartment = checkApprover(psApprover);
//        }
//        if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User or approving officer is not authorized to void the transaction." );
//            return poJSON;
//        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
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

        String lsStatus = CashDisbursementStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }
        
        Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
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

        if(!pbWthParent){
            //1. Check the position of the current user
            String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
            if(lsPosition1 == null || "".equals(lsPosition1) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }

            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            //2. Check the position of the approving officer
            String lsPosition = checkPosition(lsStatus, psApprover);
            if(lsPosition == null || "".equals(lsPosition) ){
                poJSON.put("result", "error" );
                poJSON.put("message", "User is not an authorized officer." );
                return poJSON;
            }
        }
        
//        String lsDepartment = poGRider.getDepartment();
//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//            lsDepartment = checkApprover(psApprover);
//        }
//        if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User or approving officer is not authorized to cancel the transaction." );
//            return poJSON;
//        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,psApprover);
        if (!"success".equals((String) poJSON.get("result"))) {
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
        Model_Cash_Fund loObj = new CashflowModels(poGRider).CashFund();
        int lnCountCashFund = 0;
        String lsCashFundID = "";
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
                    lsCashFundID = loRS.getString("sCashFIDx") ;
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        
        //BR: Use the Cash Fund ID based on the log in branch and dept ng requesting employee or if 1 Cash Fund ID is defined, use this as the default
        if(lnCountCashFund == 1){
            if(lsCashFundID != null && !"".equals(lsCashFundID)){
                Master().setCashFundId(lsCashFundID);
            }
        } else {
            lsCashFundID = "";
        }
        
        return lsCashFundID;
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
                Master().setCashFundId("");
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
    public JSONObject SearchCashFund(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
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
        
        CashFund object = new CashflowControllers(poGRider, logwrapr).CashFund();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setIndustryId(Master().getIndustryId());
        object.setCompanyId(Master().getCompanyId());
        object.setBranchCode(Master().getBranchCode());
        object.setDepartmentId(Master().getDepartmentRequest());
        object.setCashFundUse(true);
        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            Master().setCashFundId(object.getModel().getCashFundId());
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
            if(Master().getCashFundId() == null || "".equals(Master().getCashFundId())){
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
            String lsCashFund = setCashFund() ;
            if(lsCashFund != null && !"".equals(lsCashFund)){
                Master().setCashFundId(lsCashFund);
            } else {
                Master().setCashFundId("");
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
                            Detail(lnCtr).setReferNo(Detail(fnRow).getReferNo());
                            Detail(lnCtr).setAmount(Detail(fnRow).getAmount());
                            Detail(lnCtr).setDetailVatAmount(Detail(fnRow).getDetailVatAmount());
                            Detail(lnCtr).setDetailVatExempt(Detail(fnRow).getDetailVatExempt());
                            Detail(lnCtr).setDetailVatRate(Detail(fnRow).getDetailVatRate());
                            Detail(lnCtr).setDetailVatSales(Detail(fnRow).getDetailVatSales());
                            Detail(lnCtr).setDetailZeroVat(Detail(fnRow).getDetailZeroVat());

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
    
    public JSONObject SearchTaxCode(String value, int row, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(WTaxDeduction(row).getModel().getPeriodFrom() == null || WTaxDeduction(row).getModel().getPeriodTo() == null){
            poJSON.put("result", "error");
            poJSON.put("message", "Period date is not set.");
            return poJSON;
        }
        
        //validate period
        poJSON = checkPeriodDate(row);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        int lnRow = 1;
        for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
            if(WTaxDeduction(lnCtr).getModel().isReverse()){
                lnRow++;
            }
            if(lnCtr != row){
                if(WTaxDeduction(lnCtr).getModel().getTaxCode() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxCode())){
                    if(WTaxDeduction(lnCtr).getModel().getTaxRateId() == null || "".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Particular at row "+lnRow+" is not set.");
                        return poJSON;
                    }
                }
            }
        }
        
        TaxCode object = new ParamControllers(poGRider, logwrapr).TaxCode();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            WTaxDeduction(row).getModel().setTaxCode(object.getModel().getTaxCode());
            System.out.println("Tax Code : " + WTaxDeduction(row).getModel().getTaxCode());
        }
        poJSON.put("row", row);
        return poJSON;
    }

    public JSONObject SearchWTParticular(String value, int row, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        if(WTaxDeduction(row).getModel().getTaxCode() == null || "".equals(WTaxDeduction(row).getModel().getTaxCode())){
            poJSON.put("result", "error");
            poJSON.put("message", "Tax Code is not set.");
            poJSON.put("row", row);
            return poJSON;
        }
        
        //validate period
        poJSON = checkPeriodDate(row);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        WithholdingTax object = new CashflowControllers(poGRider, logwrapr).WithholdingTax();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setIndustryId(Master().getIndustryId());
        poJSON = object.searchRecord(value, byCode,WTaxDeduction(row).getModel().getTaxCode());
        if ("success".equals((String) poJSON.get("result"))) {
            JSONObject loJSON = checkExistTaxRate(row, object.getModel().getTaxRateId(),object.getModel().getTaxType());
            if (!isJSONSuccess(loJSON)) {
                if((boolean) loJSON.get("reverse")){
                    return loJSON;
                } else {
                    row = (int) loJSON.get("row");
                    WTaxDeduction(row).getModel().isReverse(true);
                    WTaxDeduction(row).getModel().setBaseAmount(0.00);
                    WTaxDeduction(row).getModel().setTaxAmount(0.00);
                }
            }
            
            WTaxDeduction(row).getModel().setTaxRateId(object.getModel().getTaxRateId());
            WTaxDeduction(row).getModel().setBIRForm(object.getModel().getTaxType());
            System.out.println("Tax Code : " + WTaxDeduction(row).getModel().getTaxRateId());
            System.out.println("Particular : " + WTaxDeduction(row).getModel().WithholdingTax().AccountChart().getDescription());
        }
        
        poJSON.put("row", row);
        return poJSON;
    }
    
    /*Validate detail exisitence*/
    /**
    * Validates if an account code already exists in other detail rows to prevent duplicates.
    * 
    * If a duplicate is found, the account code for the current row is cleared 
    * and an error message identifying the conflicting row is returned.
    * 
    * @param fnRow      The index of the current row being edited.
    * @param fsAcctCode The account code to check.
    * @return A {@link JSONObject} with a "success" status if unique, or "error" 
    *         if the code already exists in another row.
    */
    public JSONObject checkExistAcctCode(int fnRow, String fsAcctCode){
        poJSON = new JSONObject();

        for(int lnCtr = 0;lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            if(fsAcctCode.equals(poJournal.Detail(lnCtr).getAccountCode()) && fnRow != lnCtr){
                poJSON.put("row", lnCtr);
                poJSON.put("result", "error");
                poJSON.put("message", "Account code " + fsAcctCode + " already exists at row " + (lnCtr+1) + ".");
                poJournal.Detail(fnRow).setAccountCode("");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }


    /**
     * Checks if an account code already exists in the journal proposal details.
     *
     * @param fnJEProposalRow      current row index of JE Proposal
     * @param fsBranch
     * @param fsDept
     * @return JSONObject indicating if duplicate exists or not
     */
    public JSONObject checkJEPExistBranchDept(int fnJEProposalRow, String fsOrigBranchCode, String fsOrigDeptId) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        String lbBranch = "";
        String lbDepartment = "";
        int lnRow = 0;

        for(int lnCtr = 0;lnCtr < getJournalProposalList().size() ; lnCtr++){
            lnRow++;
            if((fnJEProposalRow == lnCtr || JournalProposalStatus.VOID.equals(JournalProposal(lnCtr).Master()))
                    || JournalProposalStatus.CANCELLED.equals(JournalProposal(lnCtr).Master())
            ){
            } else {
                lbBranch = JournalProposal(lnCtr).Master().getBranchCode();
                lbDepartment = JournalProposal(lnCtr).Master().getDepartmentId();

                if(lbBranch != null && !"".equals(lbBranch)){
                    if(lbBranch.equals(JournalProposal(fnJEProposalRow).Master().getBranchCode())){
                        if(lbDepartment != null && !"".equals(lbDepartment)){
                            if(lbDepartment.equals(JournalProposal(fnJEProposalRow).Master().getDepartmentId())){
                                poJSON.put("row", lnCtr);
                                poJSON.put("result", "error");
                                poJSON.put("message", "Branch " + JournalProposal(fnJEProposalRow).Master().Branch().getBranchName() + " and Deparment " + JournalProposal(fnJEProposalRow).Master().Department().getDescription() + " already exist at row " + lnRow);
                                paJournalProposal.get(fnJEProposalRow).Master().setBranchCode(fsOrigBranchCode);
                                paJournalProposal.get(fnJEProposalRow).Master().setDepartmentId(fsOrigDeptId);
                                return poJSON;
                            }
                        }
                    }
                }
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Validates withholding tax deductions for consistency and duplicates within a specific period.
    * This method ensures:
    * The tax type remains consistent across all withholding tax deductions.
    * The same tax rate is not duplicated within the same fiscal year and quarter.
    * @param fnRow      The index of the current row being validated.
    * @param fsTaxRated The Tax Rate ID to check for duplicates.
    * @param fsTaxType  The Tax Type to verify against existing entries.
    * @return A {@link JSONObject} containing "success" if valid, or an "error" status with 
    *         details regarding the conflicting row and reversal status.
    */
    private JSONObject checkExistTaxRate(int fnRow, String fsTaxRated, String fsTaxType){
        JSONObject loJSON = new JSONObject();
        try {
            int lnRow = 0;
            for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1; lnCtr++){
                if(WTaxDeduction(lnCtr).getModel().isReverse()){
                    lnRow++;
                }
                if(WTaxDeduction(lnCtr).getModel().getTaxRateId() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                    if(WTaxDeduction(lnCtr).getModel().isReverse() && !fsTaxType.equals(WTaxDeduction(lnCtr).getModel().WithholdingTax().getTaxType()) ){
                        loJSON.put("result", "error");
                        loJSON.put("reverse", true);
                        loJSON.put("row", lnCtr);
                        loJSON.put("message", "Tax type must be equal to other withholding tax deductions.");
                        return loJSON;
                    }
                    
                    //Check the tax rate
                    if(fsTaxRated.equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())
                        && fnRow != lnCtr){
                        //Check Period Date do not allow when taxratedid was already covered of the specific period date
                        if(strToDate(xsDateShort(WTaxDeduction(lnCtr).getModel().getPeriodFrom())).getYear() 
                            == strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())).getYear()){
                            //Check Period date per quarter
                            if( getQuarter(strToDate(xsDateShort(WTaxDeduction(lnCtr).getModel().getPeriodFrom()))) 
                                == getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom()))) 
                                    ){
                                if(WTaxDeduction(lnCtr).getModel().isReverse()){
                                    loJSON.put("result", "error");
                                    loJSON.put("message", "Particular " + WTaxDeduction(lnCtr).getModel().WithholdingTax().AccountChart().getDescription() + " already exists at row " + (lnRow) + ".");
                                    loJSON.put("row", lnCtr);
                                    loJSON.put("reverse", true);
                                    return loJSON;
                                } else {
                                    loJSON.put("result", "error");
                                    loJSON.put("reverse", false);
                                    loJSON.put("row", lnCtr);
                                    return loJSON;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            loJSON.put("result", "error");
            loJSON.put("message", MiscUtil.getException(ex));
            return loJSON;
        }
    
        loJSON.put("result", "success");
        return loJSON;
    }
    
    /**
     * Validate period date
     * @param fnRow pass withholding tax deduction selected row
     * @return 
     */
    public JSONObject checkPeriodDate(int fnRow){
        //Validate period from must be per quarter per year.
        if(WTaxDeduction(fnRow).getModel().getPeriodFrom() != null){
            if(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodTo())).getYear() 
                != strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())).getYear() ){
                poJSON.put("row", fnRow);
                poJSON.put("result", "error");
                poJSON.put("message", "Period Date must be with the same year at row "+(fnRow + 1)+".");
                return poJSON;
            }
            
            if( (getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodTo()))) 
                != getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())))) ){
                poJSON.put("row", fnRow);
                poJSON.put("result", "error");
                poJSON.put("message", "Period date must be in the same quarter at row "+(fnRow + 1)+".");
                return poJSON;
            }
        }
        
        poJSON.put("row", fnRow);
        poJSON.put("result", "success");
        return poJSON;
    }
    /**
    * Determines the calendar quarter (1 to 4) for a given date.
    * 
    * @param fdDate The date to evaluate.
    * @return The quarter index: 1 (Jan-Mar), 2 (Apr-Jun), 3 (Jul-Sep), or 4 (Oct-Dec).
    */
    private int getQuarter(LocalDate fdDate){
        int month = fdDate.getMonthValue();
        int quarter = ((month - 1) / 3) + 1;
        return quarter;
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
        Double ldblVATSalesTotal = 0.0000;
        Double ldblVATAmountTotal = 0.0000;
        Double ldblVATExemptTotal = 0.0000;
        Double ldblZeroVATSales = 0.0000;
        computeTaxAmount();
        computeDetailFields(isValidate);
        
        for (int lnCntr = 0; lnCntr <= getDetailCount() - 1; lnCntr++) {
            if(Detail(lnCntr).isReverse()){
                ldblTransactionTotal += Detail(lnCntr).getAmount();
                ldblVATSalesTotal += Detail(lnCntr).getDetailVatSales();
                ldblVATAmountTotal += Detail(lnCntr).getDetailVatAmount();
                ldblVATExemptTotal += Detail(lnCntr).getDetailVatExempt();

            }
        }
        
        if(ldblTransactionTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Transaction Total.");
            poJSON.put("column", "nTranTotl");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATSalesTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Vat Sales Total.");
            poJSON.put("column", "nVATSales");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATAmountTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Vat Amount Total.");
            poJSON.put("column", "nVATAmtxx");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATExemptTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Vat Exempt Total.");
            poJSON.put("column", "nVatExmpt");
            if(isValidate){
                return poJSON;
            }
        }
        
        double lnNetAmountDue = ldblTransactionTotal - Master().getWithTaxTotal();
        if (lnNetAmountDue < 0.0000) {
            poJSON = setJSON("error", "Invalid Net Total Amount.");
            poJSON.put("column", "nNetTotal");
            if(isValidate){
                return poJSON;
            }
        }

        Master().setTransactionTotal(ldblTransactionTotal);
        Master().setVatableSales(ldblVATSalesTotal);
        Master().setVatAmount(ldblVATAmountTotal);
        Master().setVatExempt(ldblVATExemptTotal);
        Master().setZeroVatSales(ldblZeroVATSales);
        Master().setNetTotal(lnNetAmountDue);
        
        poJSON = setJSON("success", "computed successfully");
        poJSON.put("column", "");
        return poJSON;
    }
    /**
    * Calculates withholding tax amounts for all applicable deductions and updates the master total.
    * 
    * This method computes individual tax amounts based on the assigned tax rates and base amounts. 
    * It validates that neither the total base amount nor the total tax amount exceeds the 
    * transaction's vatable sales before updating the master record.
    * 
    * @return A {@link JSONObject} containing a "success" status if the calculation is valid, 
    *         or an "error" status with a descriptive message if validation fails or an exception occurs.
    */
    public JSONObject computeTaxAmount(){
        poJSON = new JSONObject();
        
//        set/compute value to tax amount
        Double ldblTaxAmount = 0.0000;
        Double ldblDetTaxAmt = 0.0000;
        Double ldblTotalBaseAmount = 0.0000;
        for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
            if(WTaxDeduction(lnCtr).getModel().isReverse()){
                if(WTaxDeduction(lnCtr).getModel().getBaseAmount() > 0.0000 && 
                    WTaxDeduction(lnCtr).getModel().getTaxRateId() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                    try {
                        ldblDetTaxAmt = WTaxDeduction(lnCtr).getModel().getBaseAmount() * (WTaxDeduction(lnCtr).getModel().WithholdingTax().getTaxRate() / 100);
                    } catch (SQLException | GuanzonException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                        poJSON = setJSON("error", MiscUtil.getException(ex));
                        return poJSON;
                    }
                    WTaxDeduction(lnCtr).getModel().setTaxAmount(ldblDetTaxAmt);
                }
                ldblTaxAmount += WTaxDeduction(lnCtr).getModel().getTaxAmount();
                ldblTotalBaseAmount += WTaxDeduction(lnCtr).getModel().getBaseAmount(); 
            }
        }
        ldblTotalBaseAmount = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblTotalBaseAmount, false).replace(",", ""));
        double ldblVatSales = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVatableSales(), false).replace(",", ""));
        if(ldblTotalBaseAmount > ldblVatSales){
            poJSON = setJSON("error", "Tax Base amount cannot be greater than the net vatable sales.");
            return poJSON;
        }
        
        if(ldblTaxAmount > ldblVatSales){
            poJSON = setJSON("error", "Tax amount cannot be greater than the net vatable sales.");
            return poJSON;
        }
        
        Master().setWithTaxTotal(ldblTaxAmount);
        
        poJSON = setJSON("success", "Tax computed successfully");
        return poJSON;
    }
    /**
    * Iterates through all detail rows to compute their respective VAT and tax values.
    * 
    * @param isValidate Set to true to stop execution and return an error immediately 
    *                   if any row calculation fails validation.
    * @return A {@link JSONObject} indicating "success" if all rows are processed, 
    *         or an "error" status if a specific row fails validation.
    */
    public JSONObject computeDetailFields(boolean isValidate){
                
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            //Compute VAT
            poJSON = computeDetail(lnCtr);
            if ("error".equals((String) poJSON.get("result"))) {
                if(isValidate){
                    return poJSON;
                }
            }
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
    * Calculates the VAT Sales, VAT Amount, and VAT Exempt values for a specific detail row.
    * 
    * <p>This method applies a 12% VAT calculation on the non-exempt portion of the row's total 
    * amount and updates the row model with the results.</p>
    * 
    * @param fnRow The index of the detail row to calculate.
    * @return A {@link JSONObject} with "success" status, or "error" if the input 
    *         values (such as negative exempt amounts) are invalid.
    */
    private JSONObject computeDetail(int fnRow){
        poJSON = new JSONObject();
        Double ldblAmount = Detail(fnRow).getAmount();
        Double ldblVATExempt = Detail(fnRow).getDetailVatExempt();
        Double ldblVATSales = Detail(fnRow).getDetailVatSales();
        Double ldblVATAmount = 0.0000;
        
        if(ldblVATExempt < 0.0000){
            poJSON = setJSON("error", "Vat Exempt amount cannot be negative.");
            return poJSON;
        }
        
        if(Detail(fnRow).getReferNo() != null && !"".equals(Detail(fnRow).getReferNo())){
            if(ldblVATExempt > 0.0000 && ldblVATExempt < ldblAmount){
                ldblAmount = ldblAmount - ldblVATExempt;
                ldblVATAmount = ldblAmount - (ldblAmount / 1.12);
                ldblVATSales = ldblAmount - ldblVATAmount;

                Detail(fnRow).setDetailVatAmount(ldblVATAmount);
                Detail(fnRow).setDetailVatSales(ldblVATSales);
            } else if(ldblVATExempt == 0.0000){
                ldblVATAmount = ldblAmount - (ldblAmount / 1.12);
                ldblVATSales = ldblAmount - ldblVATAmount;

                Detail(fnRow).setDetailVatAmount(ldblVATAmount);
                Detail(fnRow).setDetailVatSales(ldblVATSales);
                Detail(fnRow).setDetailVatExempt(0.0000);
            } else {
                Detail(fnRow).setDetailVatAmount(0.0000);
                Detail(fnRow).setDetailVatSales(0.0000);
                Detail(fnRow).setDetailVatExempt(ldblAmount);
            }
        } else {
            Detail(fnRow).setDetailVatAmount(0.0000);
            Detail(fnRow).setDetailVatSales(0.0000);
            Detail(fnRow).setDetailVatExempt(ldblAmount);
        }
        
        poJSON = setJSON("success", "success");
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
            Model_Cash_Disbursement loObject = new CashflowModels(poGRider).CashDisbursementMaster();
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
    * Loads a list of liquidated cash advances that have not yet been linked to a cash disbursement.
    * 
    * <p>This method filters records based on industry, payee, and transaction number. 
    * It specifically excludes cash advances that are already present in the current 
    * master table to prevent duplicate processing.</p>
    * 
    * @param fsIndustry The industry description to filter by.
    * @param fsPayee    The payee name keyword to filter by.
    * @param fsTransNo  The transaction number keyword to filter by.
    * @return A {@link JSONObject} indicating "success" if records were found and loaded 
    *         into {@code paCashAdvances}, otherwise returns an "error" status.
    * @throws SQLException     If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs while opening records.
    */
    public JSONObject loadCashAdvanceList(String fsIndustry, String fsPayee, String fsTransNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paCashAdvances = new ArrayList<>();
        if (fsIndustry == null) { fsIndustry = ""; }
        if (fsPayee == null) { fsPayee = ""; }
        if (fsTransNo == null) { fsTransNo = ""; }
        
        String lsSQL = MiscUtil.addCondition(cashAdvanceSQL(),
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransNo + "%")
                + " AND a.cTranStat = " + SQLUtil.toSQL(CashAdvanceStatus.LIQUIDATED)
            );
        
        lsSQL = lsSQL 
                + " AND a.sTransNox NOT IN ( SELECT sSourceNo FROM " + Master().getTable()
                + " WHERE sSourceNo =  a.sTransNox "
                + " AND sSourceCd = " + SQLUtil.toSQL(CashDisbursementStatus.SourceCode.CASHADVANCE)
                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.VOID)
                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.CANCELLED)
                + " )";
        
        lsSQL = lsSQL + " ORDER BY a.dTransact ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON = setJSON("error", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
           Model_Cash_Advance loObject = new CashflowModels(poGRider).CashAdvanceMaster();
            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
            if (isJSONSuccess(poJSON)) {
                paCashAdvances.add((Model) loObject);
            } else {
                return poJSON;
            }
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
//    /**
//    * Loads a list of liquidated cash advances that have not yet been linked to a cash disbursement.
//    * 
//    * <p>This method filters records based on industry, payee, and transaction number. 
//    * It specifically excludes cash advances that are already present in the current 
//    * master table to prevent duplicate processing.</p>
//    * 
//    * @param fsIndustry The industry description to filter by.
//    * @param fsPayee    The payee name keyword to filter by.
//    * @param fsTransNo  The transaction number keyword to filter by.
//    * @return A {@link JSONObject} indicating "success" if records were found and loaded 
//    *         into {@code paCashAdvances}, otherwise returns an "error" status.
//    * @throws SQLException     If a database access error occurs.
//    * @throws GuanzonException If an application-level error occurs while opening records.
//    */
//    public JSONObject loadCashAdvanceList(String fsIndustry, String fsPayee, String fsTransNo) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        paCashAdvances = new ArrayList<>();
//        if (fsIndustry == null) { fsIndustry = ""; }
//        if (fsPayee == null) { fsPayee = ""; }
//        if (fsTransNo == null) { fsTransNo = ""; }
//        
//        String lsSQL = MiscUtil.addCondition(cashAdvanceSQL(),
//                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
//                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
//                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransNo + "%")
//                + " AND a.cTranStat = " + SQLUtil.toSQL(CashAdvanceStatus.LIQUIDATED)
//            );
//        
//        lsSQL = lsSQL 
//                + " AND a.sTransNox NOT IN ( SELECT sSourceNo FROM " + Master().getTable()
//                + " WHERE sSourceNo =  a.sTransNox "
//                + " AND sSourceCd = " + SQLUtil.toSQL(CashDisbursementStatus.SourceCode.CASHADVANCE)
//                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.VOID)
//                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.CANCELLED)
//                + " )";
//        lsSQL = lsSQL + " UNION ";
//        lsSQL = lsSQL + prfSQL();
//        lsSQL = lsSQL + " WHERE a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
//                + " AND e.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
//                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransNo + "%")
//                + " AND a.cTranStat = " + SQLUtil.toSQL(PaymentRequestStatus.CONFIRMED)
//                + " AND a.sSourceCd = " + SQLUtil.toSQL(InvTransCons.PURCHASE_ORDER)
//                + " AND h.sTermCode = 'M0W2003'" ;
//        
//        lsSQL = lsSQL + " ORDER BY dTransact ASC ";
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        if (MiscUtil.RecordCount(loRS) <= 0) {
//            poJSON = setJSON("error", "No record found.");
//            return poJSON;
//        }
//
////        while (loRS.next()) {
////           Model_Cash_Advance loObject = new CashflowModels(poGRider).CashAdvanceMaster();
////            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
////            if (isJSONSuccess(poJSON)) {
////                paCashAdvances.add((Model) loObject);
////            } else {
////                return poJSON;
////            }
////        }
//        
//        
//        int lnctr = 0;
//        JSONArray dataArray = new JSONArray();
//        while (loRS.next()) {            
//            JSONObject record = new JSONObject();
//            record.put("sTransNox", loRS.getString("sTransNox"));
//            record.put("sBranchNme", loRS.getString("sBranchNm"));
//            record.put("dTransact", loRS.getDate("dTransact"));
//            record.put("sCashFIDx", loRS.getString("sCashFIDx"));
//            record.put("sCompanyx", loRS.getString("sCompanyx"));
//            record.put("sIndustry", loRS.getString("sIndustry"));
//            record.put("sDeptName", loRS.getString("sDeptName"));
//            record.put("sPayeexxx", loRS.getString("sPayeexxx"));
//            record.put("sCashFund", loRS.getString("sCashFund"));
//            record.put("sTranType", loRS.getString("sTranType"));
//
//            dataArray.add(record);
//            lnctr++;
//        }
//        MiscUtil.close(loRS);
//        if (lnctr > 0) {
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record(s) loaded successfully.");
//            poJSON.put("data", dataArray);
//        } else {
//            poJSON.put("result", "error");
//            poJSON.put("message", "No records found.");
//            poJSON.put("data", new JSONArray());
//        }
//        
//        return poJSON;
//    }
    /**
    * Populates the current transaction's master and detail records using data from a specific Cash Advance.
    * 
    * <p>This method retrieves the Cash Advance master and its non-reversed details, maps them to the 
    * current object, sets the source reference, and recomputes all transaction totals.</p>
    * 
    * @param fsTransNo The transaction number of the source Cash Advance record.
    * @return A {@link JSONObject} indicating "success" or an "error" if the records cannot be loaded.
    * @throws SQLException If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs during data mapping.
    * @throws CloneNotSupportedException If an error occurs while duplicating record models.
    */
    public JSONObject populateDetail(String fsTransNo, String fsType) throws SQLException, GuanzonException, CloneNotSupportedException{
        poJSON = new JSONObject();
        
        if(getEditMode() != EditMode.ADDNEW){
            poJSON = setJSON("error", "Invalid update mode.\nUnable to populate detail.");
            return poJSON;
        }
        
        String lsIndustyId = "";
        String lsCompanyId = "";
        String lsBranchCode = "";
        String lsFundId = "";
        String lsClientId = "";
        String lsPayeeName = "";
        String lsSourceNo = "";
        String lsSourceCode = "";
        String lsDepartmentId = "";
        if("PRF".equals(fsType)){
//            String lsCashDisbursement = existCashDisbursement(fsTransNo, CashDisbursementStatus.SourceCode.CASHADVANCE);
//            if(lsCashDisbursement != null && !"".equals(lsCashDisbursement)){
//                poJSON = setJSON("error", "The selected cash advance has already been processed with a cash disbursement.\nKindly refer to Cash Disbursement No. <" + lsCashDisbursement + ">.");
//                return poJSON;
//            }

            Model_Payment_Request_Master loMaster = new CashflowModels(poGRider).PaymentRequestMaster();
            poJSON = loMaster.openRecord(fsTransNo);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            Model_Payment_Request_Detail loDetail = new CashflowModels(poGRider).PaymentRequestDetail();
            String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loDetail),
                    " sTransNox = " + SQLUtil.toSQL(fsTransNo)
                    + " AND cReversex = " + SQLUtil.toSQL(CashAdvanceStatus.Reverse.INCLUDE)
                );

            lsSQL = lsSQL + " ORDER BY nEntryNox ASC ";
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                poJSON = setJSON("error", "No payment request detail found.");
                return poJSON;
            }

            removeDetails(); // Remove all details

            while (loRS.next()) {
                poJSON = loDetail.openRecord(loRS.getString("sTransNox"),loRS.getInt("nEntryNox"));
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                AddDetail();
                System.out.println("Detail Count : " + getDetailCount());
                Detail(getDetailCount()-1).setReferNo("");
                Detail(getDetailCount()-1).setAmount(loDetail.getNetTotal());
                Detail(getDetailCount()-1).setDetailVatExempt(loDetail.getNetTotal());
                Detail(getDetailCount()-1).setEntryNo(getDetailCount());
            }
            MiscUtil.close(loRS);
            
            //Populate Master
            lsIndustyId = loMaster.getIndustryID();
            lsCompanyId = loMaster.getCompanyID();
            lsBranchCode = loMaster.getBranchCode();
//            lsFundId = loMaster.getCashFundId();
            lsClientId = loMaster.Payee().getClientID();
            lsPayeeName = loMaster.Payee().getPayeeName();
            lsSourceNo = loMaster.getTransactionNo();
            lsSourceCode = CashDisbursementStatus.SourceCode.PAYMENTREQUEST;
            lsDepartmentId = loMaster.getDepartmentID();
        
        } else {
            String lsCashDisbursement = existCashDisbursement(fsTransNo, CashDisbursementStatus.SourceCode.CASHADVANCE);
            if(lsCashDisbursement != null && !"".equals(lsCashDisbursement)){
                poJSON = setJSON("error", "The selected cash advance has already been processed with a cash disbursement.\nKindly refer to Cash Disbursement No. <" + lsCashDisbursement + ">.");
                return poJSON;
            }

            Model_Cash_Advance loMaster = new CashflowModels(poGRider).CashAdvanceMaster();
            poJSON = loMaster.openRecord(fsTransNo);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }

            Model_Cash_Advance_Detail loDetail = new CashflowModels(poGRider).CashAdvanceDetail();
            String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loDetail),
                    " sTransNox = " + SQLUtil.toSQL(fsTransNo)
                    + " AND cReversex = " + SQLUtil.toSQL(CashAdvanceStatus.Reverse.INCLUDE)
                );

            lsSQL = lsSQL + " ORDER BY nEntryNox ASC ";
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                poJSON = setJSON("error", "No cash advance detail found.");
                return poJSON;
            }

            removeDetails(); // Remove all details

            while (loRS.next()) {
                poJSON = loDetail.openRecord(loRS.getString("sTransNox"),loRS.getInt("nEntryNox"));
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                AddDetail();
                System.out.println("Detail Count : " + getDetailCount());
                Detail(getDetailCount()-1).setReferNo(loDetail.getORNo());
                Detail(getDetailCount()-1).setAmount(loDetail.getTransactionAmount());
                Detail(getDetailCount()-1).setDetailVatExempt(loDetail.getTransactionAmount());
                Detail(getDetailCount()-1).setEntryNo(getDetailCount());
            }
            MiscUtil.close(loRS);
            
            //Populate Master
            lsIndustyId = loMaster.getIndustryId();
            lsCompanyId = loMaster.getCompanyId();
            lsBranchCode = loMaster.getBranchCode();
            lsFundId = loMaster.getCashFundId();
            lsClientId = loMaster.getClientId();
            lsPayeeName = loMaster.Payee().getCompanyName();
            lsSourceNo = loMaster.getTransactionNo();
            lsSourceCode = CashDisbursementStatus.SourceCode.CASHADVANCE;
            lsDepartmentId = loMaster.getDepartmentRequest();
        }
        
        //Populate Master
        Master().setIndustryId(lsIndustyId);
        Master().setCompanyId(lsCompanyId);
        Master().setBranchCode(lsBranchCode);
        Master().setCashFundId(lsFundId);
        Master().setClientId(lsClientId);
        Master().setPayeeName(lsPayeeName);
        Master().setSourceNo(lsSourceNo);
        Master().setSourceCode(lsSourceCode);
        Master().setDepartmentRequest(lsDepartmentId);
        Master().setVoucherNo(getVoucherNo());
        
        computeFields(false);
        
        return poJSON;
    }
    
    
    /**
    * Populates the current transaction's master and detail records using data from a specific Cash Advance.
    * 
    * <p>This method retrieves the Cash Advance master and its non-reversed details, maps them to the 
    * current object, sets the source reference, and recomputes all transaction totals.</p>
    * 
    * @param fsTransNo The transaction number of the source Cash Advance record.
    * @return A {@link JSONObject} indicating "success" or an "error" if the records cannot be loaded.
    * @throws SQLException If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs during data mapping.
    * @throws CloneNotSupportedException If an error occurs while duplicating record models.
    */
    public JSONObject populateDetail(String fsTransNo) throws SQLException, GuanzonException, CloneNotSupportedException{
        poJSON = new JSONObject();
        
        if(getEditMode() != EditMode.ADDNEW){
            poJSON = setJSON("error", "Invalid update mode.\nUnable to populate detail.");
            return poJSON;
        }
        
        String lsCashDisbursement = existCashDisbursement(fsTransNo, CashDisbursementStatus.SourceCode.CASHADVANCE);
        if(lsCashDisbursement != null && !"".equals(lsCashDisbursement)){
            poJSON = setJSON("error", "The selected cash advance has already been processed with a cash disbursement.\nKindly refer to Cash Disbursement No. <" + lsCashDisbursement + ">.");
            return poJSON;
        }
        
        Model_Cash_Advance loMaster = new CashflowModels(poGRider).CashAdvanceMaster();
        poJSON = loMaster.openRecord(fsTransNo);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        Model_Cash_Advance_Detail loDetail = new CashflowModels(poGRider).CashAdvanceDetail();
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loDetail),
                " sTransNox = " + SQLUtil.toSQL(fsTransNo)
                + " AND cReversex = " + SQLUtil.toSQL(CashAdvanceStatus.Reverse.INCLUDE)
            );
        
        lsSQL = lsSQL + " ORDER BY nEntryNox ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON = setJSON("error", "No cash advance detail found.");
            return poJSON;
        }

        removeDetails(); // Remove all details
        
        //Populate Master
        Master().setIndustryId(loMaster.getIndustryId());
        Master().setCompanyId(loMaster.getCompanyId());
        Master().setBranchCode(loMaster.getBranchCode());
        Master().setCashFundId(loMaster.getCashFundId());
        Master().setClientId(loMaster.getClientId());
        Master().setPayeeName(loMaster.Payee().getCompanyName());
        Master().setSourceNo(loMaster.getTransactionNo());
        Master().setSourceCode(CashDisbursementStatus.SourceCode.CASHADVANCE);
        Master().setDepartmentRequest(loMaster.getDepartmentRequest());
        Master().setVoucherNo(getVoucherNo());
        
        while (loRS.next()) {
            poJSON = loDetail.openRecord(loRS.getString("sTransNox"),loRS.getInt("nEntryNox"));
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            AddDetail();
            System.out.println("Detail Count : " + getDetailCount());
            Detail(getDetailCount()-1).setReferNo(loDetail.getORNo());
            Detail(getDetailCount()-1).setAmount(loDetail.getTransactionAmount());
            Detail(getDetailCount()-1).setDetailVatExempt(loDetail.getTransactionAmount());
            Detail(getDetailCount()-1).setEntryNo(getDetailCount());
        }
        MiscUtil.close(loRS);
        
        computeFields(false);
        
        return poJSON;
    }
    
    /**
     * Check existing Cash Disbursement
     * @return
     * @throws SQLException 
     */
    public String existCashDisbursement(String fsSourceNo, String fsSourceCode) throws SQLException{
        if(fsSourceNo == null || "".equals(fsSourceNo)){
            return "";
        }
        Model_Cash_Disbursement loMaster = new CashflowModels(poGRider).CashDisbursementMaster();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        lsSQL = MiscUtil.addCondition(lsSQL,
                " sSourceNo = " + SQLUtil.toSQL(fsSourceNo)
                + " AND sSourceCD = " + SQLUtil.toSQL(fsSourceCode)
                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.CANCELLED)
                + " AND cTranStat != " + SQLUtil.toSQL(CashDisbursementStatus.VOID)
                + " AND sTransNox != " + SQLUtil.toSQL(Master().getTransactionNo())
        );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------CASH DISBURSEMENT--------------------------");
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("------------------------------------------------------------------------------");
                if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                    return loRS.getString("sTransNox");
                }  
            }
        }
        MiscUtil.close(loRS);

        return "";
    }
    
    private static String psNoCategory = "EMPTY";
    /**
     * Populate Journal information
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject populateJournal() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON = setJSON("error", "No record to load");
            return poJSON;
        }
        
        if(poJournal == null || getEditMode() == EditMode.READY){
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        }
        
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            switch(getEditMode()){
                case EditMode.READY:
                    poJSON = poJournal.OpenTransaction(lsJournal);
                    if (!isJSONSuccess(poJSON)){
                        return poJSON;
                    }
                break;
                case EditMode.UPDATE:
                    if(poJournal.getEditMode() == EditMode.READY || poJournal.getEditMode() == EditMode.UNKNOWN){
                        poJSON = poJournal.OpenTransaction(lsJournal);
                        if (!isJSONSuccess(poJSON)){
                            return poJSON;
                        }
                        poJournal.UpdateTransaction();
                    } 
                break;
            }
        } else {
            if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poJournal.getEditMode() != EditMode.ADDNEW){
                poJSON = poJournal.NewTransaction();
                if (!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                
                //retreiving using column index
                JSONObject jsonmaster = new JSONObject();
                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++){
                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
                    jsonmaster.put(Master().getColumn(lnCtr),  Master().getValue(lnCtr));
                }
                    
                JSONArray jsondetails = new JSONArray();
                JSONObject jsondetail = new JSONObject();
                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                    jsondetail = new JSONObject();
                    for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++){
                        System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
                        jsondetail.put(Detail(lnCtr).getColumn(lnCol),  Detail(lnCtr).getValue(lnCol));
                    }
                    jsondetails.add(jsondetail);
                }

                jsondetail = new JSONObject();
                jsondetail.put("Cash_Disbursement", jsonmaster);
                jsondetail.put("Cash_Disbursement_Detail", jsondetails);
                    
                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE,Master().getIndustryId(), ""); 
                tbj.setGRiderCAS(poGRider);
                tbj.setData(jsondetail);
                jsonmaster = tbj.processRequest();

                if(jsonmaster.get("result").toString().equalsIgnoreCase("success")){
                    List<TBJEntry> xlist = tbj.getJournalEntries();
                    for (TBJEntry xlist1 : xlist) {
                        System.out.println("Account:" + xlist1.getAccount() );
                        System.out.println("Debit:" + xlist1.getDebit());
                        System.out.println("Credit:" + xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setForMonthOf(poGRider.getServerDate());
                        poJournal.Detail(poJournal.getDetailCount()-1).setAccountCode(xlist1.getAccount());
                        poJournal.Detail(poJournal.getDetailCount()-1).setCreditAmount(xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setDebitAmount(xlist1.getDebit());
                        poJournal.AddDetail();
                    }
                } else {
                    System.out.println(jsonmaster.toJSONString());
                }

                //Journa Entry Master
                poJournal.Master().setAccountPerId("");
                poJournal.Master().setIndustryCode(Master().getIndustryId());
                poJournal.Master().setBranchCode(Master().getBranchCode());
                poJournal.Master().setDepartmentId(poGRider.getDepartment());
                poJournal.Master().setTransactionDate(poGRider.getServerDate()); 
                poJournal.Master().setCompanyId(Master().getCompanyId());
                poJournal.Master().setSourceCode(getSourceCode());
                poJournal.Master().setSourceNo(Master().getTransactionNo());
                
            } else if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poJournal.getEditMode() == EditMode.ADDNEW) {
                poJSON.put("result", "success");
                return poJSON;
            } 
//            else {
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record to load");
//                return poJSON;
//            }
        
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Check existing Journal
     * @return
     * @throws SQLException 
     */
    public String existJournal() throws SQLException{
        Model_Journal_Master loMaster = new CashflowModels(poGRider).Journal_Master();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        lsSQL = MiscUtil.addCondition(lsSQL,
                " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
        );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------JOURNAL ENTRY--------------------------");
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("------------------------------------------------------------------------------");
                if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                    return loRS.getString("sTransNox");
                }  
            }
        }
        MiscUtil.close(loRS);

        return "";
    }

    /**
     * Check existing Journal Proposal
     * @return
     * @throws SQLException
     */
    public JSONObject loadJournalProposal() throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.READY || getEditMode() == EditMode.UPDATE){ //Load saved journal proposal
            paJournalProposal = new ArrayList<JournalProposal>();

            Model_Journal_Master_Proposal loMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
            String lsSQL = MiscUtil.makeSelect(loMaster);
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                            + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
            );

            if(!CashDisbursementStatus.VOID.equals(Master().getTransactionStatus())
                    && !CashDisbursementStatus.CANCELLED.equals(Master().getTransactionStatus())){
                lsSQL = lsSQL + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.VOID)
                        + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.CANCELLED);
            }

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            boolean lbExist = false;
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------JOURNAL ENTRY PROPOSAL--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        lbExist = false;
                        int lnCtr = 0;
                        for(lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                            if(JournalProposal(lnCtr).Master().getTransactionNo().equals(loRS.getString("sTransNox"))){
                                lbExist = true;
                                break;
                            }
                        }

                        if(!lbExist){
                            AddJournalProposal();
                            lnCtr = getJournalProposalList().size() - 1;
                            poJSON = JournalProposal(lnCtr).OpenTransaction(loRS.getString("sTransNox"));
                            if("error".equals((String) poJSON.get("result"))){
                                return poJSON;
                            }
                        }

                        if(getEditMode() == EditMode.UPDATE && JournalProposal(lnCtr).getEditMode() == EditMode.READY){
                            poJSON = JournalProposal(getJournalProposalList().size() - 1).UpdateTransaction();
                            if("error".equals((String) poJSON.get("result"))){
                                return poJSON;
                            }
                        }
                    }
                }
            }
            MiscUtil.close(loRS);
        }

        poJSON.put("result", "success");
        poJSON.put("message", "No record to load");
        return poJSON;
    }
    
    /**
    * Manages the withholding tax deduction list based on the current edit mode.
    * 
    * In READY mode, it fetches and loads existing linked deductions from the database. 
    * In ADDNEW mode, it initializes the list with a default entry using the transaction date. 
    * In UPDATE mode, it transitions all existing deduction records into an editable state.
    * 
    * @return A JSONObject indicating "success" or an "error" message if the operation fails.
    * @throws SQLException If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs.
    * @throws CloneNotSupportedException If a model cloning error occurs.
    * @throws ScriptException If a scripting error occurs during processing.
    */
    public JSONObject populateWithholdingTaxDeduction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        switch(getEditMode()){
            case EditMode.READY:
                paWTaxDeductions = new ArrayList<>();
                Model_Withholding_Tax_Deductions loMaster = new CashflowModels(poGRider).Withholding_Tax_Deductions();
                String lsSQL = MiscUtil.makeSelect(loMaster);
                lsSQL = MiscUtil.addCondition(lsSQL,
                        " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                        + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
                );
                System.out.println("Executing SQL: " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                if (MiscUtil.RecordCount(loRS) > 0) {
                    while (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------WITHHOLDING TAX DEDUCTIONS--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            paWTaxDeductions.add( new CashflowControllers(poGRider,logwrapr).WithholdingTaxDeductions());
                            poJSON = paWTaxDeductions.get(paWTaxDeductions.size() - 1).openRecord(loRS.getString("sTransNox"));
                            if ("error".equals((String) poJSON.get("result"))){
                                if(Master().getWithTaxTotal() > 0.0000){
                                    return poJSON;
                                } 
                            } else {
                                //add tax code
                                paWTaxDeductions.get(paWTaxDeductions.size() - 1).getModel().setTaxCode(paWTaxDeductions.get(paWTaxDeductions.size() - 1).getModel().WithholdingTax().getTaxCode());
                            }
                        }  
                    }
                }
                MiscUtil.close(loRS);
            break;
            case EditMode.ADDNEW:   
                if(paWTaxDeductions.isEmpty()){
                    paWTaxDeductions.add(new CashflowControllers(poGRider,logwrapr).WithholdingTaxDeductions());
                    //set default period date
                    paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(Master().getTransactionDate());
                    paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(Master().getTransactionDate());
                }
            break;
            case EditMode.UPDATE:
                for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                    if(WTaxDeduction(lnCtr).getEditMode() == EditMode.READY){
                        poJSON = WTaxDeduction(lnCtr).updateRecord();
                        if ("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                    }
                }
            break;
        }
        
        poJSON.put("result", "success");
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
        List loList;
        if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
            loList = loAttachment.getAttachments(Master().getSourceCode(), Master().getSourceNo());
            lsSourceCode = Master().getSourceCode();
        } else {
            loList = loAttachment.getAttachments(getSourceCode(), Master().getTransactionNo());
            lsSourceCode = getSourceCode();
        }
        
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

        String branchVoucherNo = CashDisbursementStatus.DEFAULT_VOUCHER_NO;  // default value

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
    * @return The {@link Model_Cash_Disbursement} instance representing the transaction header.
    */
    @Override
    public Model_Cash_Disbursement Master() { 
        return (Model_Cash_Disbursement) poMaster; 
    }
    /**
    * Returns the detail record model at the specified row index.
    * 
    * @param row The index of the detail row to retrieve.
    * @return The {@link Model_Cash_Disbursement_Detail} instance for the given row.
    */
    @Override
    public Model_Cash_Disbursement_Detail Detail(int row) {
        return (Model_Cash_Disbursement_Detail) paDetail.get(row); 
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
            if ((Detail(getDetailCount() - 1).getParticularId() == null || "".equals(Detail(getDetailCount() - 1).getParticularId()))
                && (Master().getSourceNo() == null || "".equals(Master().getSourceNo()))){
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
        
        Iterator<WithholdingTaxDeductions> wtDeduction = WTaxDeduction().iterator();
        while (wtDeduction.hasNext()) {
            WithholdingTaxDeductions item = wtDeduction.next();
            wtDeduction.remove();
        }
        
        //Reset Journal when all details was removed
        resetJournal();
//        resetJournalProposal();
        paAttachments = new ArrayList<>();
//        setSearchIndustry("");
//        setSearchPayee("");
//        Master().setIndustryId("");
        Master().setSourceNo("");
        Master().setSourceCode("");
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
    * @return The Model_Cash_Disbursement instance at the specified row.
    */
    public Model_Cash_Disbursement TransactionList(int row) {
        return (Model_Cash_Disbursement) paMaster.get(row);
    }
    /**
     * Returns the total number of records in the cash disbursement transaction list.
     * 
     * @return The size of the transaction list.
     */
    public int getTransactionListCount() {
        return this.paMaster.size();
    }
    /**
    * Retrieves a specific cash advance record from the cash advance list.
    * 
    * @param row The index of the record to retrieve.
    * @return The Model_Cash_Advance instance at the specified row.
    */
    public Model_Cash_Advance CashAdvancesList(int row) {
        return (Model_Cash_Advance) paCashAdvances.get(row);
    }
    /**r
     * Returns the total number of records in the cash advance list.
     * 
     * @return The size of the cash advance list.
     */
    public int getCashAdvancesCount() {
        return this.paCashAdvances.size();
    }
    /**
    * Returns the complete list of withholding tax deductions.
    * 
    * @return A List of WithholdingTaxDeductions.
    */
    public List<WithholdingTaxDeductions> WTaxDeduction() {
        return paWTaxDeductions; 
    }
    /**
    * Retrieves a specific withholding tax deduction record by row index.
    * 
    * @param row The index of the deduction to retrieve.
    * @return The WithholdingTaxDeductions instance at the specified row.
    */
    public WithholdingTaxDeductions WTaxDeduction(int row) {
        return (WithholdingTaxDeductions) paWTaxDeductions.get(row); 
    }
    
    public int getWTaxDeductionsCount() {
        return paWTaxDeductions.size();
    }
    
    public Journal Journal(){
        try{
            if (poJournal == null) {
                poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
                poJournal.InitTransaction();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poJournal;
    }

    /**
     * Gets or initializes the Journal proposal controller.
     *
     * @return Journal instance
     */
    public JournalProposal JournalProposal(int fnRow){
        if(paJournalProposal == null){
            paJournalProposal = new ArrayList<JournalProposal>();
        }
        return paJournalProposal.get(fnRow);
    }


    /**
     * Adds a new journal detail record after validating the last entry.
     *
     * @return JSONObject indicating success or error status
     * @throws CloneNotSupportedException if cloning fails
     */
    public JSONObject AddJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        if (getJournalProposalList().size() > 0) {
            if (JournalProposal(getJournalProposalList().size() - 1).Master().getBranchCode().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }
        paJournalProposal.add( new CashflowControllers(poGRider, logwrapr).JournalProposal());
        paJournalProposal.get(getJournalProposalList().size() - 1).InitTransaction();
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Gets the full list of Journal Proposal records.
     *
     * @return list of Journal Proposal models
     */
    public List<JournalProposal> getJournalProposalList() {
        return (List<JournalProposal>) (List<?>) paJournalProposal;
    }
    
    public JSONObject AddWTaxDeduction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        if (getWTaxDeductionsCount() > 0) {
            if (WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxRateId().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }
        paWTaxDeductions.add(new CashflowControllers(poGRider, logwrapr).WithholdingTaxDeductions());
        //set default period date
        paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(Master().getTransactionDate());
        paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(Master().getTransactionDate());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject removeWTDeduction(int fnRow) {
        if (WTaxDeduction(fnRow).getEditMode() == EditMode.ADDNEW) {
            WTaxDeduction().remove(fnRow);
        } else {
            WTaxDeduction(fnRow).getModel().isReverse(false);
        }
        //Compute Tax Amount
        computeTaxAmount();
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    /*RESET CACHE ROW SET*/
    /**
     * Resets the master record to its default initial state.
     */
    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).CashDisbursementMaster();
    }
    
    /**
     * Resets the other record to its default initial state.
     */
    public void resetOthers() {
        paAttachments = new ArrayList<>();
    }
    
    public void resetJournal() {
        try {
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Resets journal transaction.
     */
    public void resetJournalProposal() {
        try {
            paJournalProposal = new ArrayList<JournalProposal>();
            paJournalProposal.set(paJournalProposal.size()-1, new CashflowControllers(poGRider, logwrapr).JournalProposal());
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Completely clears the current transaction state.
     * 
     * Resets the master model, clears all detail and attachment collections, 
     * and wipes the industry and payee search filters.
     */
    public void resetTransaction(){
        resetMaster();
        resetJournal();
//        resetJournalProposal();
        Detail().clear();
        getJournalProposalList().clear();
        WTaxDeduction().clear();
        paAttachments = new ArrayList<>();
        psApprover = "";
        setApproving("");
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
            if ((Detail(lnCtr).getParticularId() == null || "".equals(Detail(lnCtr).getParticularId()))
                && (Detail(lnCtr).getReferNo() == null || "".equals(Detail(lnCtr).getReferNo()))
               && (Master().getSourceCode() == null || "".equals(Master().getSourceCode()))) {
                deleteDetail(lnCtr);
            } 
            lnCtr--;
        }
            
        if ((getDetailCount() - 1) >= 0) {
            if (
                (Detail(getDetailCount() - 1).getParticularId() != null && !"".equals(Detail(getDetailCount() - 1).getParticularId()))
                && Detail(getDetailCount() - 1).getAmount() > 0.0000
                && (Master().getSourceCode() == null || "".equals(Master().getSourceCode()))
                ) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
    
    public void ReloadJournal() throws CloneNotSupportedException, SQLException{
        int lnCtr = Journal().getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Journal().Detail(lnCtr).getAccountCode() == null || "".equals(Journal().Detail(lnCtr).getAccountCode())) {
                Journal().Detail().remove(lnCtr);
            } else {
                if(Journal().Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    if(Journal().Detail(lnCtr).getDebitAmount() <= 0.0000
                        && Journal().Detail(lnCtr).getCreditAmount() <= 0.0000){
                        Journal().Detail().remove(lnCtr);
                    }
                }
            }
            lnCtr--;
        }
        if ((Journal().getDetailCount() - 1) >= 0) {
            if (Journal().Detail(getDetailCount() - 1).getAccountCode() != null && !"".equals(Journal().Detail(getDetailCount() - 1).getAccountCode())
                && (Journal().Detail(getDetailCount() - 1).getDebitAmount() > 0.0000 || Journal().Detail(getDetailCount() - 1).getCreditAmount() > 0.0000)) {
                Journal().AddDetail();
                Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
            }
        }
        if ((Journal().getDetailCount() - 1) < 0) {
            Journal().AddDetail();
            Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
        }
    
    }


    /**
     * Cleans journal details by removing invalid or empty entries,
     * ensures at least one valid row exists, and adds a new detail row
     * when the last entry is valid and contains amounts.
     *
     * @throws CloneNotSupportedException if cloning fails
     * @throws SQLException if a database error occurs
     */
    public void ReloadJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException, GuanzonException{
        int lnCtr = getJournalProposalList().size() - 1;
        Model_Journal_Detail_Proposal loJEPDetail;
        while (lnCtr >= 0) {
            if (JournalProposal(lnCtr) == null ) {
                getJournalProposalList().remove(lnCtr);
            } else {
            }
            lnCtr--;
        }

        if(getJournalProposalList() == null){
            if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                createNewJournalProposal();
            }
        } else {
            if ((getJournalProposalList().size() - 1) >= 0) {
                lnCtr = getJournalProposalList().size() - 1;

                boolean hasEmptyBranchOrDepartment = getJournalProposalList().stream()
                        .anyMatch(jp ->
                                StringUtils.isBlank(jp.Master().getBranchCode()) ||
                                        StringUtils.isBlank(jp.Master().getDepartmentId())
                        );

                if(JournalProposal(lnCtr).Master().getBranchCode() != null && !"".equals(JournalProposal(lnCtr).Master().getBranchCode())
                        && JournalProposal(lnCtr).Master().getDepartmentId() != null && !"".equals(JournalProposal(lnCtr).Master().getDepartmentId())
                        && JournalProposal(lnCtr).getTotalDebitAmount() > 0.0000 && JournalProposal(lnCtr).getTotalCreditAmount()> 0.0000
                        && !hasEmptyBranchOrDepartment ){
                    if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                        createNewJournalProposal();
                    }
                }
            }
            if ((getJournalProposalList().size() - 1) < 0) {
                if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                    createNewJournalProposal();
                }
            }
        }
    }


    private void createNewJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException {
        AddJournalProposal();

        JournalProposal proposal = JournalProposal(getJournalProposalList().size() - 1);
        proposal.NewTransaction();
        proposal.Master().setTransactionDate(poGRider.getServerDate());
        proposal.Master().setCompanyId(Master().getCompanyId());
        proposal.Master().setIndustryCode(Master().getIndustryId());
        proposal.Master().setSourceNo(Master().getTransactionNo());
        proposal.Master().setSourceCode(getSourceCode());
        proposal.ReloadDetail();
    }
    
    public void ReloadWTDeductions() throws CloneNotSupportedException, SQLException, GuanzonException{
        int lnCtr = getWTaxDeductionsCount() - 1;
        Date fromdate = null, todate = null;
        boolean lbProceed = false;
        while (lnCtr >= 0) {
            if (!lbProceed) {
                fromdate = null;
                todate = null;
            }
            if (WTaxDeduction(lnCtr).getModel().getTaxCode() == null
                    || "".equals(WTaxDeduction(lnCtr).getModel().getTaxCode())) {
                System.out.println("REMOVE WTAX : " + WTaxDeduction(lnCtr).getModel().getTransactionNo());
                fromdate = WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getPeriodFrom();
                todate = WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getPeriodTo();
                WTaxDeduction().remove(lnCtr);
                lbProceed = true;
            }
            lnCtr--;
        }
        if ((getWTaxDeductionsCount() - 1) >= 0) {
            if (WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxCode() != null
                    && !"".equals(WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxCode())) {
                AddWTaxDeduction();
            }
        }

        if ((getWTaxDeductionsCount() - 1) < 0) {
            AddWTaxDeduction();
        }
        if (lbProceed) {
            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(fromdate);
            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(todate);
        }
    }
    
    
    public JSONObject updateRelatedTransactions(String fsStatus, String fsApprovalId) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            poJournal.setWithParent(true);
            poJournal.setWithUI(false);
            if(fsApprovalId == null || "".equals(fsApprovalId)){
                fsApprovalId = poGRider.getUserID();
            }
            poJournal.setApproving(fsApprovalId);
            //Update Journal
            switch(fsStatus){
                case CashDisbursementStatus.CONFIRMED:
                    //Confirm Journal
                    poJSON = poJournal.ConfirmTransaction("");
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    break;
                case CashDisbursementStatus.VOID:
                    //Void Journal
                    poJSON = poJournal.VoidTransaction("");
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }

                    break;
                case CashDisbursementStatus.CANCELLED:
                    //Cancel Journal
                    poJSON = poJournal.CancelTransaction("");
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    break;
            }
        }

        //Update Journal Proposal
        for(int lnCtr = 0; lnCtr < getJournalProposalList().size(); lnCtr++){
            JournalProposal loObj = JournalProposal(lnCtr);
            loObj.setWithParent(true);
            loObj.setWithUI(false);
            if(fsApprovalId == null || "".equals(fsApprovalId)){
                fsApprovalId = poGRider.getUserID();
            }
            loObj.setApproving(fsApprovalId);
            switch(fsStatus){
                case CashDisbursementStatus.APPROVED:
                    poJSON = loObj.ConfirmTransaction("");
                   if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    break;
                case CashDisbursementStatus.VOID:
                    poJSON = loObj.VoidTransaction("");
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    break;
                case CashDisbursementStatus.CANCELLED:
                    poJSON = loObj.CancelTransaction("");
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    break;
                case CashDisbursementStatus.RETURNED:
                    if(JournalProposalStatus.OPEN.equals(loObj.Master().getTransactionStatus())){
                    } else {
                        poJSON = loObj.ReturnTransaction("");
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                    }
                    break;
            }
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
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
     * server date, and initializes the status to {@link CashDisbursementStatus#OPEN}.
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
            Master().setTransactionStatus(CashDisbursementStatus.OPEN);
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
        GValidator loValidator = new CashDisbursementValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    
    /**
    * Validates journal entries including debit/credit balance,
    * account code presence, and valid reporting dates.
    *
    * @return JSON validation result with continue flag
    */
    private JSONObject validateJournal(){
        poJSON = new JSONObject();
        poJSON.put("continue", false);
        
        double ldblCreditAmt = 0.0000;
        double ldblDebitAmt = 0.0000;
        boolean lbHasJournal = false;
        boolean lbValidateJournal = false;
        for(int lnCtr = 0; lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            if(poJournal.Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                ldblDebitAmt += poJournal.Detail(lnCtr).getDebitAmount();
                ldblCreditAmt += poJournal.Detail(lnCtr).getCreditAmount();

                if(poJournal.Detail(lnCtr).getCreditAmount() > 0.0000 ||  poJournal.Detail(lnCtr).getDebitAmount() > 0.0000){
                    if(poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode())){
                        if(poJournal.Detail(lnCtr).getForMonthOf() == null || "1900-01-01".equals(xsDateShort(poJournal.Detail(lnCtr).getForMonthOf()))){
                            poJSON.put("result", "error");
                            poJSON.put("message", "Invalid reporting date of journal at row "+(lnCtr+1)+" .");
                            return poJSON;
                        }
                    }
                }
                
                if(!lbValidateJournal){
                    lbValidateJournal = poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode());
                } 
            }
            
            if(!lbHasJournal){
                lbHasJournal = poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode());
            }   
        }
        
        if(lbValidateJournal){
            //Convert debit and credit amount
            ldblDebitAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblDebitAmt, true).replace(",", ""));
            ldblCreditAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblCreditAmt, true).replace(",", ""));
            
            if(ldblDebitAmt == 0.0000 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry debit amount.");
                return poJSON;
            }

            if(ldblCreditAmt == 0.0000){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry credit amount.");
                return poJSON;
            }

            if(ldblDebitAmt < ldblCreditAmt || ldblDebitAmt > ldblCreditAmt){
                poJSON.put("result", "error");
                poJSON.put("message", "Debit should be equal to credit amount.");
                return poJSON;
            }
        }
        
        
        poJSON.put("result", "sucess");
        poJSON.put("message", "sucess");
        poJSON.put("continue", lbHasJournal);
        return poJSON;
    }

    private JSONObject verifyJournals(String fsStatus, boolean fbIsWillSave){
        boolean lbCheckJEP = false;
        switch(fsStatus){
            case CashDisbursementStatus.VERIFIED:
            case CashDisbursementStatus.APPROVED:
                if(poJournal != null){
                    if(fbIsWillSave){
                        if(poJournal.getEditMode() != EditMode.ADDNEW && poJournal.getEditMode() != EditMode.UPDATE){
                            lbCheckJEP = true;
                        } else {
                            if(poJournal.getTotalCreditAmount() <= 0.0000 && poJournal.getTotalDebitAmount() <= 0.0000){
                                lbCheckJEP = true;
                            }
                        }
                    } else {
                        if(poJournal.getEditMode() != EditMode.READY){
                            lbCheckJEP = true;
                        }
                    }
                } else {
                    lbCheckJEP = true;
                }

                //Check Journal Proposal
                if(lbCheckJEP){
                    for(int lnCtr = 0; lnCtr < getJournalProposalList().size(); lnCtr++){
                        JournalProposal loObj = JournalProposal(lnCtr);
                        if(fbIsWillSave){
                            if(loObj.getEditMode() != EditMode.ADDNEW && loObj.getEditMode() != EditMode.UPDATE){
                                poJSON.put("result", "error" );
                                poJSON.put("message", "Invalid update mode for journal proposal." );
                                return poJSON;
                            } else {
                                if(loObj.getTotalCreditAmount() > 0.0000 && loObj.getTotalDebitAmount() > 0.0000){
                                    lbCheckJEP = false;
                                    break;
                                }
                            }
                        } else {
                            if(loObj.getEditMode() != EditMode.READY){
                                poJSON.put("result", "error" );
                                poJSON.put("message", "Invalid update mode for journal." );
                                return poJSON;
                            }
                        }
                    }
                }
                
                if(lbCheckJEP){
                    poJSON.put("result", "error" );
                    poJSON.put("message", "Journal details cannot be empty." );
                    return poJSON;
                }
                
                break;
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    
    public JSONObject checkValidAccountCode(String fsAccountCode, Double fdblDebitAmt, Double fdblCreditAmt){
        poJSON = new JSONObject();
        boolean isDebit = fdblDebitAmt > 0.0000;
        if(fsAccountCode == null || "".equals(fsAccountCode)){
            poJSON.put("result", "success");
            poJSON.put("message", "success");
            return poJSON;
        }
        try {
        
            if(poJournal != null){
                if(poJournal.getEditMode() == EditMode.UPDATE){
                    for(int lnCtr = 0; lnCtr < Journal().getDetailCount(); lnCtr++){
                        if(fsAccountCode.equals(Journal().Detail(lnCtr).getAccountCode())){
                            if(Journal().Detail(lnCtr).getCreditAmount() > 0.0000 && isDebit){
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Both debit and credit amounts cannot be greater than zero for the same account code within a transaction."
                                            + "\nJournal Entry"
                                            + "\nAccount: '" + Journal().Detail(lnCtr).Account_Chart().getDescription() + "', row " + (lnCtr + 1) + ".");
                                    return poJSON;
                            }
                        }
                    }
                }
            }
            
            for(int lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                for(int lnRow = 0;lnRow < JournalProposal(lnCtr).getDetailCount();lnRow++){
                    if(fsAccountCode.equals(JournalProposal(lnCtr).Detail(lnRow).getAccountCode())){
                        if(JournalProposal(lnCtr).Detail(lnRow).getCreditAmount() > 0.0000 && isDebit){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Both debit and credit amounts cannot be greater than zero for the same account code within a transaction."
                                        + "\nJournal Proposal"
                                        + "\nAccount: '" + Journal().Detail(lnCtr).Account_Chart().getDescription() + "', row " + (lnCtr + 1) + ".");
                                return poJSON;
                        }
                    }
                }
            
            }
    
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        } 
        poJSON.put("result", "success");
        poJSON.put("message", "success");
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
        System.out.println("Journal Class Edit Mode : " + poJournal.getEditMode());
        System.out.println("Journal Master Edit Mode : " + poJournal.Master().getEditMode());
        //Re-set the transaction no and voucher no
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
            Master().setVoucherNo(getVoucherNo());
        }
        
        String lsCashDisbursement = existCashDisbursement(Master().getSourceNo(), Master().getSourceCode());
        if(lsCashDisbursement != null && !"".equals(lsCashDisbursement)){
            poJSON = setJSON("error", "The selected cash advance has already been processed with a cash disbursement.\nKindly refer to Cash Disbursement No. <" + lsCashDisbursement + ">.");
            return poJSON;
        }

        poJSON = verifyJournals(psForm,true);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = isEntryOkay(Master().getTransactionStatus());
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //Recompute tax amount
        poJSON = computeTaxAmount();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //Recompute fields to validate
        poJSON = computeFields(true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //Validate Detail
        for(int lnCtr = 0;lnCtr <= getDetailCount()-1;lnCtr++){
            if ((Detail(lnCtr).getParticularId() == null || "".equals(Detail(lnCtr).getParticularId()))
                ){
                if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo()) 
                    || Detail(lnCtr).getAmount() > 0.0000){
                    poJSON = setJSON("error", "Particular cannot be empty at row " + (lnCtr+1) );
                    return poJSON;
                }
            }
        }
        
        //Validate Withholding Tax Deductions
        Double ldblTotalBaseAmount = 0.0000;
        if(Master().getWithTaxTotal() > 0.0000){
            for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                if (WTaxDeduction(lnCtr).getModel().isReverse()
                    && WTaxDeduction(lnCtr).getModel().getBaseAmount() > 0.0000 
                    && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId()) 
                    && WTaxDeduction(lnCtr).getModel().getTaxRateId() != null
                    ) {
                    if(WTaxDeduction(lnCtr).getEditMode() == EditMode.ADDNEW || WTaxDeduction(lnCtr).getEditMode() == EditMode.UPDATE){
                        //validate period
                        poJSON = checkPeriodDate(lnCtr);
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                    
                    ldblTotalBaseAmount += WTaxDeduction(lnCtr).getModel().getBaseAmount(); 
                }
            }
            
            double ldblVatSales = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVatableSales(), false).replace(",", ""));
            if(!Objects.equals(ldblTotalBaseAmount, ldblVatSales)){
                poJSON = setJSON("error", "Total tax base amount must be equal to net vatable sales.");
                return poJSON;
            }
        }
        
        /*Put system validations and other assignments here*/
        boolean lbUpdated = false;
        if (CashDisbursementStatus.RETURNED.equals(Master().getTransactionStatus())) {
            try {
                CashDisbursement loRecord = new CashflowControllers(poGRider, null).CashDisbursement();
                poJSON = loRecord.InitTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                poJSON = loRecord.OpenTransaction(Master().getTransactionNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                lbUpdated = loRecord.getDetailCount() == getDetailCount()-1;
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getTransactionDate().equals(Master().getTransactionDate());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDepartmentRequest().equals(Master().getDepartmentRequest());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getCashFundId().equals(Master().getCashFundId());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getCreditedTo().equals(Master().getCreditedTo());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getPayeeName().equals(Master().getPayeeName());
                }
                if (lbUpdated) {
                    lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Master().getNetTotal()), String.format("%.4f", Master().getNetTotal())));
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.getDetailCount() == getDetailCount()-1;
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Journal().getDetailCount() == Journal().getDetailCount()-1;
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.getWTaxDeductionsCount() == getWTaxDeductionsCount()-1;
                }

                //Check disbursement detail
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Detail(lnCtr).getAmount()), String.format("%.4f", Detail(lnCtr).getAmount())));
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }

                //Check Journal
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.Journal().getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getDebitAmount()), String.format("%.4f", Journal().Detail(lnCtr).getDebitAmount())));
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getCreditAmount()), String.format("%.4f", Journal().Detail(lnCtr).getCreditAmount())));
                        }
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }

                //Check Journal Proposal
                if (lbUpdated) {
                    lbUpdated = loRecord.getJournalProposalList().size() == (getJournalProposalList().size() - 1);
                    if (lbUpdated) {
                        for (int lnCtr = 0; lnCtr < loRecord.getJournalProposalList().size(); lnCtr++) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).getTotalDebitAmount()), String.format("%.4f", JournalProposal(lnCtr).getTotalDebitAmount())));
                            if (lbUpdated) {
                                lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).getTotalCreditAmount()), String.format("%.4f", JournalProposal(lnCtr).getTotalCreditAmount())));
                            }
                            if (!lbUpdated) {
                                break;
                            }

                            for (int lnRow = 0; lnRow < loRecord.JournalProposal(lnCtr).getDetailCount(); lnRow++) {
                                lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).Detail(lnRow).getDebitAmount()), String.format("%.4f", JournalProposal(lnCtr).Detail(lnRow).getDebitAmount())));
                                if (lbUpdated) {
                                    lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).Detail(lnRow).getCreditAmount()), String.format("%.4f", JournalProposal(lnCtr).Detail(lnRow).getCreditAmount())));
                                }
                                if (lbUpdated) {
                                    lbUpdated = (Objects.equals(loRecord.JournalProposal(lnCtr).Detail(lnRow).getAccountCode(), JournalProposal(lnCtr).Detail(lnRow).getAccountCode()));
                                }
                                if (!lbUpdated) {
                                    break;
                                }
                            }

                        }
                    }
                }
                //Check Witholding Tax
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.getWTaxDeductionsCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.2f", loRecord.WTaxDeduction(lnCtr).getModel().getTaxAmount()), String.format("%.2f", WTaxDeduction(lnCtr).getModel().getTaxAmount())));
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.2f", loRecord.WTaxDeduction(lnCtr).getModel().getBaseAmount()), String.format("%.2f", WTaxDeduction(lnCtr).getModel().getBaseAmount())));
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
            } catch (ScriptException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON.put("result", "error");
                poJSON.put("message", MiscUtil.getException(ex));
                return poJSON;
            }

        }

        //Request Approval
        if(!pbWthParent) {
            if(CashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())
                    || CashDisbursementStatus.RETURNED.equals(Master().getTransactionStatus())
                    || CashDisbursementStatus.VERIFIED.equals(Master().getTransactionStatus())){

                if(CashDisbursementStatus.CONFIRMED.equals(Master().getTransactionStatus())
                        && CashDisbursementStatus.VERIFIED.equals(psForm)){
                } else {
                    String lsUserId = poGRider.getUserID();
                    poJSON = callApproval();
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                    //2. Check the position of the approving officer
                    if(psApprover != null && !"".equals(psApprover)){
                        lsUserId = psApprover;
                    }
                    String lsPosition = checkPosition(psForm, lsUserId);
                    if(lsPosition == null || "".equals(lsPosition) ){
                        poJSON.put("result", "error" );
                        if(psApprover != null && !"".equals(psApprover)){
                            poJSON.put("message", "User is not an authorized approving officer." );
                        } else {
                            poJSON.put("message", "User is not an authorized officer." );
                        }
                        return poJSON;
                    }
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
                    item.setValue("cReversex", CashDisbursementStatus.Reverse.EXCLUDE);
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
        
        Iterator<WithholdingTaxDeductions> loObject = WTaxDeduction().iterator();
        while (loObject.hasNext()) {
            WithholdingTaxDeductions item = loObject.next(); // Store the item before checking conditions
            String lsTaxRateId = (String) item.getModel().getTaxRateId();
            double lsAmount = item.getModel().getBaseAmount();
            if (lsAmount <= 0.0000 || "".equals(lsTaxRateId) || lsTaxRateId == null) {
                if(item.getEditMode() == EditMode.ADDNEW){
                    loObject.remove(); // Correctly remove the item
                } else {
                    item.getModel().isReverse(false);
                }
            }
        }
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        //Recompute amounts
        computeTaxAmount();
        computeFields(false);
        
        Master().setModifiedBy(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        //Save attachment only for manual entry
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
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
        return isEntryOkay(CashDisbursementStatus.OPEN);
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
            
            if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
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
            }
            
            System.out.println("--------------------------SAVE WITHHOLDING TAX DEDUCTION---------------------------------------------");
            //Save Withholding Tax Deductions
            for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                if(WTaxDeduction(lnCtr).getEditMode() == EditMode.ADDNEW || WTaxDeduction(lnCtr).getEditMode() == EditMode.UPDATE){
                    WTaxDeduction(lnCtr).getModel().setSourceCode(getSourceCode());
                    WTaxDeduction(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
                    WTaxDeduction(lnCtr).getModel().setModifyingBy(poGRider.getUserID());
                    WTaxDeduction(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    WTaxDeduction(lnCtr).setWithParentClass(true);
                    WTaxDeduction(lnCtr).setWithUI(false);
                    poJSON = WTaxDeduction(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("Save Withholding Tax Deduction : " + poJSON.get("message"));
                        return poJSON;
                    }
                }
            }

            //Save Journal Proposal
            System.out.println("--------------------------SAVE JOURNAL PROPOSAL---------------------------------------------");
            poJSON = saveJournalProposal();
            if ("error".equals((String) poJSON.get("result"))) {
                paOrigJEP = null;
                return poJSON;
            }
            System.out.println("-----------------------------------------------------------------------");
            
//            Save Journal
            System.out.println("--------------------------SAVE JOURNAL---------------------------------------------");
            if(poJournal != null){
                if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                    poJSON = validateJournal();
                    boolean lbContinue = (boolean) poJSON.get("continue");
                    if ("error".equals((String) poJSON.get("result"))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", poJSON.get("message").toString());
                        return poJSON;
                    } 
                    if(lbContinue){
                        poJournal.Master().setSourceNo(Master().getTransactionNo());
                        poJournal.Master().setModifyingId(poGRider.getUserID());
                        poJournal.Master().setModifiedDate(poGRider.getServerDate());
                        poJournal.setWithParent(true);
                        poJSON = poJournal.SaveTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Save Journal : " + poJSON.get("message"));
                            return poJSON;
                        }
                    }
                } else {
                    if (poGRider.getUserLevel() > UserRight.ENCODER) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Invalid Update mode for Journal.");
                        return poJSON;
                    }
                }
            } else {
                if (poGRider.getUserLevel() > UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Journal is not set.");
                    return poJSON;
                }
            }
            System.out.println("-----------------------------------------------------------------------");
            
        
        } catch (SQLException | GuanzonException | CloneNotSupportedException | ParseException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }

        //Clear value
        paOrigJEP = null;
        poJSON = setJSON("success", "success");
        return poJSON;
    }

    public List<JournalProposal> paOrigJEP;
    private JSONObject populateOriginalValue(){
        poJSON = new JSONObject();
        paOrigJEP = new ArrayList<JournalProposal>();
        try {
            for(int lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                paOrigJEP.add( new CashflowControllers(poGRider, logwrapr).JournalProposal());
                paOrigJEP.get(paOrigJEP.size() - 1).InitTransaction();
                JournalProposal loObj = paOrigJEP.get(paOrigJEP.size() - 1);
                if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW){
                    poJSON = loObj.NewTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("populateOriginalValue : " + poJSON.get("message"));
                        return poJSON;
                    }
                } else if(JournalProposal(lnCtr).getEditMode() == EditMode.UPDATE){
                    poJSON = loObj.OpenTransaction(JournalProposal(lnCtr).Master().getTransactionNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("populateOriginalValue : " + poJSON.get("message"));
                        return poJSON;
                    }
                    poJSON = loObj.UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("populateOriginalValue : " + poJSON.get("message"));
                        return poJSON;
                    }
                }

                System.out.println("---------SET ORIGINAL VALUE FOR JOURNAL PROPOSAL---------------");

                System.out.println("---------MASTER JOURNAL PROPOSAL---------------");
                for (int lnCol = 1; lnCol <= loObj.Master().getColumnCount(); lnCol++) {
                    System.out.println(loObj.Master().getColumn(lnCol) + " ->> " + JournalProposal(lnCtr).Master().getValue(loObj.Master().getColumn(lnCol)));
                    loObj.Master().setValue(loObj.Master().getColumn(lnCol), JournalProposal(lnCtr).Master().getValue(loObj.Master().getColumn(lnCol)));
                }
                System.out.println("---------------------------------------------------------------");
                for (int lnRow = 0; lnRow < JournalProposal(lnCtr).getDetailCount(); lnRow++){
                    System.out.println("---------DETAIL JOURNAL PROPOSAL---------------");
                    int lnDet = -1;
                    if(JournalProposal(lnCtr).Detail(lnRow).getEditMode() == EditMode.ADDNEW){
                        loObj.ReloadDetail();
                        lnDet = loObj.getDetailCount()-1;
                    } else {
//                            lnDet = loObj.Detail().indexOf(JournalProposal(lnCtr).Detail(lnRow));
                        lnDet = lnRow;
                    }
                    System.out.println("Detail : " + lnDet);
                    if(lnDet >= 0){
                        for (int lnCol = 1; lnCol <= loObj.Detail(lnDet).getColumnCount(); lnCol++) {
                            System.out.println(loObj.Detail(lnDet).getColumn(lnCol) + " ->> " + JournalProposal(lnCtr).Detail(lnRow).getValue(loObj.Detail(lnDet).getColumn(lnCol)));
                            loObj.Detail(lnDet).setValue(loObj.Detail(lnDet).getColumn(lnCol), JournalProposal(lnCtr).Detail(lnRow).getValue(loObj.Detail(lnDet).getColumn(lnCol)));
                        }
                    }
                    System.out.println("---------------------------------------------------------------");
                }
                System.out.println("---------------------------------------------------------------");

            }

        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private JSONObject saveJournalProposal() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException, ParseException{
        poJSON = new JSONObject();
        if(paOrigJEP != null){
            if(!paOrigJEP.isEmpty()){
                for(int lnCtr = 0;lnCtr < paOrigJEP.size();lnCtr++){
                    System.out.println("Original JEP ROW : " + lnCtr );
                    System.out.println("Original JEP Edit Mode : " + paOrigJEP.get(lnCtr).getEditMode());
                    System.out.println("Original Transnox : " + paOrigJEP.get(lnCtr).Master().getTransactionNo());
                }
                paJournalProposal = null;
                paJournalProposal = new ArrayList<JournalProposal>();
                paJournalProposal.addAll(paOrigJEP);
            }
        }
        if(getJournalProposalList() != null){
            poJSON = populateOriginalValue();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            for(int lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                System.out.println("JEP ROW : " + lnCtr );
                System.out.println("JEP Edit Mode : " + JournalProposal(lnCtr).getEditMode() );
                if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW || JournalProposal(lnCtr).getEditMode() == EditMode.UPDATE){
                    if(JournalProposal(lnCtr).Master().isReverse()){
                        if(JournalProposal(lnCtr).getTotalDebitAmount() > 0.0000 || JournalProposal(lnCtr).getTotalCreditAmount() > 0.0000){
                            poJSON = JournalProposal(lnCtr).validateJournalProposal();
                            boolean lbContinue = (boolean) poJSON.get("continue");
                            if ("error".equals((String) poJSON.get("result"))) {
                                poJSON.put("result", "error");
                                poJSON.put("message", poJSON.get("message").toString());
                                return poJSON;
                            }
                            if(lbContinue){
                                JournalProposal(lnCtr).Master().setSourceNo(Master().getTransactionNo());
                                JournalProposal(lnCtr).Master().setModifyingId(poGRider.getUserID());
                                JournalProposal(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                                JournalProposal(lnCtr).setWithParent(true);
                                poJSON = JournalProposal(lnCtr).SaveTransaction();
                                if ("error".equals((String) poJSON.get("result"))) {
                                    System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                                    return poJSON;
                                }
                            }
                        }
                    } else {
                        JournalProposal loObj = new CashflowControllers(poGRider, logwrapr).JournalProposal();
                        poJSON = loObj.InitTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                            return poJSON;
                        }
                        poJSON = loObj.OpenTransaction(JournalProposal(lnCtr).Master().getTransactionNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                            return poJSON;
                        }
                        loObj.setWithParent(true);
                        loObj.setWithUI(false);
                        if(psApprover == null || "".equals(psApprover)){
                            psApprover = poGRider.getUserID();
                        }
                        loObj.setApproving(psApprover);
                        switch(loObj.Master().getTransactionStatus()){
                            case JournalProposalStatus.OPEN:
                                poJSON = loObj.VoidTransaction("");
                                if ("error".equals((String) poJSON.get("result"))) {
                                    System.out.println("Void Journal Proposal : " + poJSON.get("message"));
                                    return poJSON;
                                }
                                break;
                            case JournalProposalStatus.CONFIRMED:
                            case JournalProposalStatus.RETURNED:
                                poJSON = loObj.CancelTransaction("");
                                if ("error".equals((String) poJSON.get("result"))) {
                                    System.out.println("Cancel Journal Proposal : " + poJSON.get("message"));
                                    return poJSON;
                                }
                                break;
                        }
                    }
                }
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
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

        SQL_BROWSE =  "  SELECT "
                    + "   a.sTransNox "
                    + " , a.sCompnyID "
                    + " , a.sBranchCd "
                    + " , a.sIndstCdx "
                    + " , a.nEntryNox "
                    + " , a.dTransact "
                    + " , a.sVoucherx "
                    + " , a.sCashFIDx "
                    + " , a.sClientID "
                    + " , a.sPayeeNme "
                    + " , a.sCrdtedTo "
                    + " , a.sDeptReqs "
                    + " , a.sAddressx "
                    + " , a.sRemarksx "
                    + " , a.sReferNox "
                    + " , a.sSourceCd "
                    + " , a.sSourceNo "
                    + " , a.nTranTotl "
                    + " , a.nVATSales "
                    + " , a.nVATAmtxx "
                    + " , a.nZroVATSl "
                    + " , a.nVatExmpt "
                    + " , a.nWTaxTotl "
                    + " , a.nNetTotal "
                    + " , a.cVchrPrnt "
                    + " , a.cCollectd "
                    + " , a.cTranStat "
                    + " , a.sApproved "
                    + " , b.sCompnyNm AS sCompanyx "
                    + " , c.sDescript AS sIndustry "
                    + " , d.sDeptName AS sDeptName "
                    + " , e.sCompnyNm AS sPayeexxx "
                    + " , f.sCashFDsc AS sCashFund "
                    + " , g.sBranchNm AS sBranchNm "
                    + " , h.sCompnyNm AS sCreditTo "
                    + " FROM Cash_Disbursement a   "
                    + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID       "
                    + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx      "
                    + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptReqs    "
                    + " LEFT JOIN Client_Master e ON e.sClientID = a.sClientID "
                    + " LEFT JOIN CashFund f ON f.sCashFIDx = a.sCashFIDx      "
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
                + ", 'CASHFUND' AS sTranType "
                + " FROM CashAdvance a         "
                + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID       "
                + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx      "
                + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptReqs    "
                + " LEFT JOIN Client_Master e ON e.sClientID = a.sClientID "
                + " LEFT JOIN CashFund f ON f.sCashFIDx = a.sCashFIDx      "
                + " LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd ";
    }
    
     public String prfSQL() {
        return  " SELECT        "
                + " a.sTransNox   "
                + " , a.dTransact AS dTransact"
                + " , a.sSeriesNo AS sCashFIDx "
                + " , a.cTranStat "
                + " , b.sCompnyNm AS sCompanyx "
                + " , c.sDescript AS sIndustry "
                + " , d.sDeptName AS sDeptName "
                + " , e.sPayeeNme AS sPayeexxx "
                + " , f.sPayeeNme AS sCashFund "
                + " , g.sBranchNm AS sBranchNm "
                + ", 'PRF' AS sTranType "
                + " FROM Payment_Request_Master a         "
                + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID       "
                + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx      "
                + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptIDxx    "
                + " LEFT JOIN Payee e ON e.sPayeeIDx = a.sPayeeIDx "
                + " LEFT JOIN Payee f ON f.sPayeeIDx = a.sPayeeIDx      "
                + " LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd "
                + " LEFT JOIN PO_Master h ON h.sTransNox = a.sSourceNo ";
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
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/CashDisbursement.jrxml";
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
            JSONObject loJSONConfirm = getUpdateStatusBy(CashDisbursementStatus.CONFIRMED);
            if(!isJSONSuccess(loJSONConfirm)){
                return loJSONConfirm;
            } else {
                if((String) loJSONConfirm.get("sUpdateByx") != null && !"".equals((String) loJSONConfirm.get("sUpdateByx"))){
                    params.put("sConfirmer", (String) loJSONConfirm.get("sUpdateByx") + " " + String.valueOf((String) loJSONConfirm.get("sUpdateDte"))); 
                }
            }
            //Get Approver
            JSONObject loJSONApprover = getUpdateStatusBy(CashDisbursementStatus.APPROVED);
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
//                if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
//                    lsParticular = Detail(lnCtr).CashAdvanceDetail(Master().getSourceNo()).getParticular().toUpperCase();
//                    if(!laParticular.contains(lsParticular)){
//                        laParticular.add(lsParticular);
//                    }
//                } else { 
                    //All detail will have a particular description - Arsiela 04/06/2026
                    lsParticular = Detail(lnCtr).Particular().getDescription().toUpperCase();
                    if(!laParticular.contains(lsParticular)){
                        laParticular.add(lsParticular);
                    }
//                }
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
                case CashDisbursementStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case CashDisbursementStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case CashDisbursementStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case CashDisbursementStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case CashDisbursementStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case CashDisbursementStatus.VERIFIED:
                    crs.updateString("cRefrStat", "VERIFIED");
                    break;
                case CashDisbursementStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case CashDisbursementStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case CashDisbursementStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case CashDisbursementStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case CashDisbursementStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case CashDisbursementStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                        case CashDisbursementStatus.VERIFIED:
                            crs.updateString("cRefrStat", "VERIFIED");
                            break;
                        case CashDisbursementStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
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

        showStatusHistoryUI("Cash Disbursement", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
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
