/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ShowDialogFX;
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
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cash_Advance;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cash_Advance_Detail;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CashAdvanceStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.validator.CashAdvanceValidator;

/**
 *
 * @author Arsiela 03/20/2026
 */
public class CashLiquidation extends Transaction {
    public String psIndustryId = "";
    public String psCompanyId = "";
    public String psDepartmentId = "";
    public String psIndustry = "";
    public String psBranch = "";
    public String psPayee = "";
    public String psApprover = "";
    
    public List<Model> paMaster;
    public List<TransactionAttachment> paAttachments;
    
    /**
    * Sets up a new Cash Advance (CADV) transaction.
    * 
    * Instantiates the {@code poMaster} and {@code poDetail} models, resets the 
    * {@code paMaster} and {@code paAttachments} collections, and executes 
    * the base initialization logic.
    * 
    * @return A {@link JSONObject} containing the initialization result.
    * @throws SQLException If a database error occurs during model creation.
    * @throws GuanzonException If business logic validation fails.
    */
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "CADV";

        poMaster = new CashflowModels(poGRider).CashAdvanceMaster();
        poDetail = new CashflowModels(poGRider).CashAdvanceDetail();

        paMaster = new ArrayList<Model>();
        paAttachments = new ArrayList<>();
        psApprover = "";
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
     * Loads an existing Cash Advance record and its attachments.
     * 
     * @param transactionNo The unique identifier of the transaction to be loaded.
     * @return A {@link JSONObject} containing the operation status and any relevant error messages.
     * @throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException 
     *          If an error occurs during data retrieval or object state management.
     */
    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        //Reset Transaction
        resetTransaction();
        
        poJSON = openTransaction(transactionNo);
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load cash advance.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        System.out.println("Liquidated Date : " + Master().getLiquidatedDate());
        poJSON = setJSON("success","success");
        return poJSON;
    }

    /**
     * Prepares the current transaction for modification and refreshes its attachments.
     * 
     * @return A {@link JSONObject} indicating success or failure of the update initialization.
     * @throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException 
     *          If the record is locked, unavailable, or an error occurs during the update process.
     */
    public JSONObject UpdateTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){ //BR: Authorized users from the Finance
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not authorized to update transaction." );
            return poJSON;
        }
        
        poJSON = updateTransaction();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to update cash advance.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to update transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if(Master().getLiquidatedBy() == null || "".equals(Master().getLiquidatedBy())){
            Master().setLiquidatedBy(poGRider.getUserID());
            Master().setLiquidatedDate(poGRider.getServerDate());
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
     * Validates if the current transaction is eligible for updates based on its database status.
     * <p>
     * This method checks the latest record state to prevent modifications on transactions 
     * that are already Void, Cancelled, Liquidated, or (depending on {@code isEntry}) Confirmed. 
     * If the local state is outdated, it automatically re-opens the transaction to sync data.
     * 
     * @param isEntry Set to {@code true} to block updates if the status is already "Confirmed".
     * @return A {@link JSONObject} with "success" if the update can proceed, or an error message if blocked.
     * @throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException 
     *          If an error occurs during record retrieval or data synchronization.
     */
    public JSONObject checkUpdateTransaction(boolean isEntry) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        
        Model_Cash_Advance loObject = new CashflowModels(poGRider).CashAdvanceMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load cash advance.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        switch(loObject.getTransactionStatus()){
            case CashAdvanceStatus.VOID:
            case CashAdvanceStatus.CANCELLED:
                poJSON = setJSON("error","Transaction status was already "+getStatus(loObject.getTransactionStatus())+"\nCheck transaction history.");
                return poJSON;
            case CashAdvanceStatus.CONFIRMED:
                if(isEntry){
                    poJSON = setJSON("error", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                    return poJSON;
                }
                break;
            case CashAdvanceStatus.LIQUIDATED:
                poJSON = setJSON("error","Transaction status was already liquidated!\nCheck transaction history.");
                return poJSON;
        }
        
        if(!loObject.getTransactionStatus().equals(Master().getTransactionStatus())){
            poJSON = OpenTransaction(Master().getTransactionNo());
            if (!isJSONSuccess(poJSON)) {
                poJSON = setJSON((String) poJSON.get("result"), "Unable to load cash advance.\n" + (String) poJSON.get("message"));
                return poJSON;
            }
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Returns a readable string for a given Cash Advance status code.
     *
     * @param lsStatus Status code
     * @return Human-readable status ("Open", "Confirmed", "Approved", "Cancelled", "Voided", "Liquidated", "Released", or "Unknown")
     */
    public String getStatus(String lsStatus) {
        switch (lsStatus) {
            case CashAdvanceStatus.VOID:
                return "Voided";
            case CashAdvanceStatus.CANCELLED:
                return "Cancelled";
            case CashAdvanceStatus.CONFIRMED:
                return "Confirmed";
            case CashAdvanceStatus.APPROVED:
                if(getEditMode() == EditMode.READY || getEditMode() == EditMode.UPDATE){
                    if(Master().getIssuedBy() != null && !"".equals(Master().getIssuedBy())){
                        return "Released";
                    }
                }
                return "Approved";
            case CashAdvanceStatus.LIQUIDATED:
                return "Liquidated";
            case CashAdvanceStatus.OPEN:
                return "Open";
            default:
                return "Unknown";
        }
    }
    /**
     * Validates the transition logic between transaction statuses.
     * <p>
     * Ensures the record follows the prescribed workflow rules (e.g., only "Open" 
     * transactions can be "Confirmed" or "Voided").
     * 
     * @param current The existing status of the record.
     * @param target The intended status to transition to.
     * @return {@code true} if the state transition is permitted; otherwise {@code false}.
     */
    public boolean isAllowed(String current, String target) {
        switch (target) {
//            case CashAdvanceStatus.VOID:
//                return current.equals(CashAdvanceStatus.OPEN);
//
//            case CashAdvanceStatus.CANCELLED:
//                return current.equals(CashAdvanceStatus.CONFIRMED) || current.equals(CashAdvanceStatus.APPROVED);
//
//            case CashAdvanceStatus.CONFIRMED:
//                return current.equals(CashAdvanceStatus.OPEN);
//                
//            case CashAdvanceStatus.APPROVED:
//                return current.equals(CashAdvanceStatus.CONFIRMED);
                
            case CashAdvanceStatus.LIQUIDATED:
                return current.equals(CashAdvanceStatus.APPROVED); //Allow liquidate when current status is approved

            default:
                return false;
        }
    }
    /**
     * Processes the confirmation of the current Cash Advance transaction.
     * <p>
     * This method validates the transaction's readiness, checks the latest status from the 
     * database, triggers approval workflows if necessary, and transitions the record 
     * status to "Liquidated".
     * 
     * @param remarks Additional notes or justification for the confirmation.
     * @return A {@link JSONObject} indicating the success of the confirmation or an error message.
     * @throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException 
     *          If an error occurs during data parsing, database communication, or business validation.
     */
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = CashAdvanceStatus.LIQUIDATED;
        
        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_Cash_Advance loObject = new CashflowModels(poGRider).CashAdvanceMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load cash advance.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (loObject.getTransactionStatus().equals(lsStatus)) {
            poJSON = setJSON("error", "Transaction was already approved.");
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
            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            
            String lsDepartment = poGRider.getDepartment();
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                lsDepartment = checkApprover(psApprover);
            }
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){ //Disable by Arsiela replaced by script below according to new BR 08-22-2026
            if(!lsDepartment.equals(poGRider.getDepartment())){ //Approver must be equal to the department of the current user
                poJSON.put("result", "error" );
                poJSON.put("message", "User or approving officer is not authorized to approve the transaction." );
                return poJSON;
            }
        }
        
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"),"", lsStatus, false,pbWthParent);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction approved successfully.");
        return poJSON;
    }
    
    public double getCashAdvanceBalance(){
        return Master().getAdvanceAmount() - Master().getLiquidationTotal();
    }
    
    /*Search Master References*/
    /**
     * Searches for transactions based on various filters and displays a selection dialog.
     * <p>
     * Filters include industry, branch, payee, and transaction number. If a record is selected 
     * from the browse dialog, the method automatically calls {@link #OpenTransaction(String)} 
     * to load the data.
     * 
     * @param fsIndustry Industry description filter.
     * @param fsBranch Branch name filter.
     * @param fsPayee Payee name filter.
     * @param fsTransactionNo Transaction number filter.
     * @return A {@link JSONObject} containing the loaded transaction or an error message if no record is selected.
     * @throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException 
     *          If an error occurs during SQL construction, record browsing, or data retrieval.
     */
    public JSONObject SearchTransaction(String fsIndustry, String fsBranch, String fsPayee, String fsTransactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        int lnSort = 0;
        if(fsPayee != null && !"".equals(fsPayee)){
            lnSort = 2;
        }
        if(fsTransactionNo != null && !"".equals(fsTransactionNo)){
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
                + " AND g.sBranchNm LIKE " + SQLUtil.toSQL("%" + fsBranch + "%")
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo + "%"));
        
        if(psDepartmentId != null && !"".equals(psDepartmentId)){
            lsSQL = lsSQL +  " AND a.sDeptReqs = " + SQLUtil.toSQL(psDepartmentId);
        }
        
        //Arsiela 08-22-2026
        //BR: Entry by the Cash Advance requestor	makikita lang nya yung entry nya  /finance can view all the entry
        if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                lsSQL = lsSQL + " AND a.sClientID = " + SQLUtil.toSQL(poGRider.getEmployeeNo()); //Filter by user or requestor
            }
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»Payee»Branch»Requesting Department",
                "sTransNox»dTransact»sPayeexxx»sBranchNm»sDeptName",
                "a.sTransNox»a.dTransact»e.sCompnyNm»g.sBranchNm»d.sDeptName",
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
     * Searches for transactions based on various filters and displays a selection dialog.
     * <p>
     * Filters include industry, branch, payee, and transaction number. If a record is selected 
     * from the browse dialog, the method automatically calls {@link #OpenTransaction(String)} 
     * to load the data.
     * 
     * @param fsPayee Payee name filter.
     * @param fsTransactionNo Transaction number filter.
     * @return A {@link JSONObject} containing the loaded transaction or an error message if no record is selected.
     * @throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException 
     *          If an error occurs during SQL construction, record browsing, or data retrieval.
     */
    public JSONObject SearchTransaction( String fsPayee, String fsTransactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        
        if(System.getProperty("sys.dept.finance") == null || "".equals(System.getProperty("sys.dept.finance"))){
            poJSON = setJSON("error", "The Finance Department configuration is missing. This field is required to proceed.\nPlease contact your system administrator for assistance.");
            return poJSON;
        }
        
        int lnSort = 0;
        if(fsPayee != null && !"".equals(fsPayee)){
            lnSort = 2;
        }
        if(fsTransactionNo != null && !"".equals(fsTransactionNo)){
            lnSort = 0;
        }
        initSQL();
        
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo + "%"));
        
        //Kung accounting yung dept ni user makikita lahat else kung hindi mag base sa department ng user - ma'am grace 03/26/2026 3:51pm
        if(!poGRider.getDepartment().equals( System.getProperty("sys.dept.finance"))){
            lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
                    
            //Filter by Department
            System.out.println("DEPARTMENT : " + poGRider.getDepartment());
            if(poGRider.getDepartment() != null && !"".equals(poGRider.getDepartment())){
                lsSQL = lsSQL +  " AND a.sDeptReqs = " + SQLUtil.toSQL(poGRider.getDepartment());
            }
                   
        }
        
        //Arsiela 08-22-2026
        //BR: Entry by the Cash Advance requestor makikita lang nya yung entry nya  /finance can view all the entry
        if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                lsSQL = lsSQL + " AND a.sClientID = " + SQLUtil.toSQL(poGRider.getEmployeeNo()); //Filter by user or requestor
            }
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»Payee»Branch»Requesting Department",
                "sTransNox»dTransact»sPayeexxx»sBranchNm»sDeptName",
                "a.sTransNox»a.dTransact»e.sCompnyNm»g.sBranchNm»d.sDeptName",
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
     * @return A {@link JSONObject} containing the search result.
     * @throws ExceptionInInitializerError, SQLException, GuanzonException If search fails.
     */
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if (isJSONSuccess(poJSON)) {
            setSearchBranch(object.getModel().getBranchName()); 
        }

        return poJSON;
    }
    
    /**
    * Searches and selects a payee (employee).
    *
    * Displays matching records and returns the selected result.
    * Updates either the search value or model based on parameters.
    *
    * @param value    Search keyword (Employee ID or Name)
    * @param byCode   true = search by ID, false = search by Name
    * @return JSONObject with "success" or "error" result
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if application-specific error occurs
    */
    public JSONObject SearchPayee(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
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
            setSearchPayee((String) loJSON.get("EmployNme"));
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
     * Searches for an active Account and assigns it to a specific detail row.
     * <p>
     * Includes a validation check to prevent duplicate account codes within the 
     * transaction details before finalizing the selection.
     * 
     * @param value Search criteria.
     * @param byCode {@code true} to search by account code.
     * @param row The index of the detail row being updated.
     * @return A {@link JSONObject} containing the status and the affected row index.
     * @throws ExceptionInInitializerError, SQLException, GuanzonException If search fails.
     */
    public JSONObject SearchAccount(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        AccountChart object = new CashflowControllers(poGRider, logwrapr).AccountChart();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode,Master().getIndustryId(),null);
        poJSON.put("row", row);
        if (isJSONSuccess(poJSON)) {
            JSONObject loJSON = setDetail(row, Detail(row).getParticular(), Detail(row).getORNo(), object.getModel().getAccountCode());
            if (!isJSONSuccess(loJSON)) {
                return poJSON;
            }
            System.out.println("Account : " +  Detail(row).Account().getDescription());
        }
        
        poJSON.put("success", "success");
        return poJSON;
    }
    
    public JSONObject setDetail(int fnRow, String fsParticular, String fsReceiptNo, String fsAccountCode ) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnRow = 0;
        
        if(Detail(fnRow).getEditMode() == EditMode.ADDNEW){
            for(int lnCtr = 0;lnCtr <= getDetailCount()-1; lnCtr++){
                if(Detail(lnRow).isReverse()){
                    lnRow++;
                }
                if(fnRow != lnCtr){
                    if(fsParticular.equals(Detail(lnCtr).getParticular()) 
                        && fsReceiptNo.equals(Detail(lnCtr).getORNo())
                        && fsAccountCode.equals(Detail(lnCtr).getAccountCode())){
                        if(!Detail(lnCtr).isReverse()){
                            Detail(lnCtr).isReverse(true);

                            //Reset value of the current selected row
                            Detail(fnRow).setAccountCode("");
                            Detail(fnRow).setORNo("");
                            Detail(fnRow).setTransactionDate(null);
                            Detail(fnRow).setParticular("");
                            poJSON.put("result", "success");
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        }
                    }
                }
            }
        }
        
        poJSON = Detail(fnRow).setParticular(fsParticular);
        if(!isJSONSuccess(poJSON)){
            poJSON.put("row", fnRow);
            return poJSON;
        }
        
        poJSON = Detail(fnRow).setORNo(fsReceiptNo);
        if(!isJSONSuccess(poJSON)){
            poJSON.put("row", fnRow);
            return poJSON;
        }
        
        poJSON = Detail(fnRow).setAccountCode(fsAccountCode);
        if(!isJSONSuccess(poJSON)){
            poJSON.put("row", fnRow);
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("row", fnRow);
        return poJSON;
    }
    
    /**
     * Calculates the total transaction amount by summing up all detail records.
     * <p>
     * This method iterates through the transaction details to compute the aggregate total. 
     * If the total is negative, it flags a validation error for the "nTranTotl" field.
     * 
     * @param isValidate If {@code true}, returns an error {@link JSONObject} immediately 
     *                   when a negative total is encountered.
     * @return A {@link JSONObject} containing the calculation status and the target column name.
     */
    public JSONObject computeFields(boolean isValidate) {
        poJSON = new JSONObject();
        poJSON.put("column", "");
        
        Double ldblTransactionTotal = 0.0000;
        
        for (int lnCntr = 0; lnCntr <= getDetailCount() - 1; lnCntr++) {
            if(Detail(lnCntr).isReverse()){
                ldblTransactionTotal += Detail(lnCntr).getTransactionAmount();
            }
        }
        
        if(ldblTransactionTotal < 0.0000) {
            poJSON = setJSON("error", "Invalid Transaction Total.");
            poJSON.put("column", "nTranTotl");
            if(isValidate){
                return poJSON;
            }
        }
        
        Master().setLiquidationTotal(ldblTransactionTotal);
        poJSON = setJSON("success", "computed successfully");
        poJSON.put("column", "");
        return poJSON;
    }
    
    /**
     * Retrieves a list of released cash advance transactions based on provided filters.
     * <p>
     * This method constructs a query to find records where the industry, payee, and 
     * transaction number match the criteria. It specifically filters for transactions 
     * that have been issued (released) and populates the {@code paMaster} list with the results.
     * 
     * @param fsIndustry Industry description filter (required).
     * @param fsPayee Payee name filter.
     * @param fsTransactionNo Transaction number filter.
     * @param fbisEntry if Form is Entry set true else if approval set false.
     * @return A {@link JSONObject} indicating the success of the retrieval or an error message if no records are found.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If business logic or validation (e.g., empty industry) fails.
     */
    public JSONObject loadTransactionList(String fsIndustry, String fsPayee, String fsTransactionNo, boolean fbisEntry) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        if (fsIndustry == null || "".equals(fsIndustry)) { 
            poJSON = setJSON("error", "Industry cannot be empty.");
            return poJSON;
        }
        if (fsPayee == null) { fsPayee = ""; }
        if (fsTransactionNo == null) { fsTransactionNo = ""; }
        
        initSQL();
        //set default retrieval for supplier / reference no
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND c.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry + "%")
                + " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
//                + " AND g.sBranchNm LIKE " + SQLUtil.toSQL("%" + fsBranch + "%")
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo + "%")
                + " AND (a.sIssuedxx IS NOT NULL AND TRIM(a.sIssuedxx) <> '') " //Retrieve all released cash advance
            );
        
        if(fbisEntry){
            lsSQL = lsSQL +  " AND (a.sLiquidtd IS NULL OR TRIM(a.sLiquidtd) = '') " ;
            
            //Arsiela 08-22-2026
            //BR: Entry by the Cash Advance requestor	makikita lang nya yung entry nya  /finance can view all the entry
            lsSQL = lsSQL + " AND a.sClientID = " + SQLUtil.toSQL(poGRider.getEmployeeNo()); //Filter by user or requestor
        
        } else {
            lsSQL = lsSQL +  " AND (a.sLiquidtd IS NOT NULL AND TRIM(a.sLiquidtd) <> '') " ;
            
            //Arsiela 08-22-2026
            //BR: Entry by the Cash Advance requestor	makikita lang nya yung entry nya  /finance can view all the entry
            if(!poGRider.getDepartment().equals(System.getProperty("sys.dept.finance"))){
                if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                    lsSQL = lsSQL + " AND a.sClientID = " + SQLUtil.toSQL(poGRider.getEmployeeNo()); //Filter by user or requestor
                }
                //Filter by Department
                System.out.println("DEPARTMENT : " + poGRider.getDepartment());
                if(poGRider.getDepartment() != null && !"".equals(poGRider.getDepartment())){
                    lsSQL = lsSQL +  " AND a.sDeptReqs = " + SQLUtil.toSQL(poGRider.getDepartment());
                }
            }
        }
            
        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
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
     * <p>
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

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, Master().getTransactionNo());
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
                    , SOURCE_CODE
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
     * Gets the master record for the Cash Advance transaction.
     * @return The {@link Model_Cash_Advance} master object.
     */
    @Override
    public Model_Cash_Advance Master() { 
        return (Model_Cash_Advance) poMaster; 
    }
    
    /**
     * Gets a specific detail record from the transaction.
     * @param row The index of the detail record.
     * @return The {@link Model_Cash_Advance_Detail} at the specified index.
     */
    @Override
    public Model_Cash_Advance_Detail Detail(int row) {
        return (Model_Cash_Advance_Detail) paDetail.get(row); 
    }
    
    /**
     * Adds a new detail row after validating that the current last row is not empty.
     * @return A {@link JSONObject} indicating success or a validation error.
     * @throws CloneNotSupportedException If the model cannot be instantiated.
     */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getParticular() == null || "".equals(Detail(getDetailCount() - 1).getParticular())) {
                poJSON = new JSONObject();
                poJSON = setJSON("error", "Last row has empty item.");
                return poJSON;
            }
        }

        return addDetail();
    }
    
    /**
     * Clears all transaction details and resets associated fields, attachments, and search criteria.
     * @return A {@link JSONObject} indicating the result of the operation.
     */
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }
        
        paAttachments = new ArrayList<>();
        setSearchIndustry("");
        setSearchPayee("");
        Master().setIndustryId("");
        initFields();
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Retrieves a Cash Advance record by index.
     *
     * @param row Index of the record
     * @return Model_Cash_Advance at the specified index
     */
    public Model_Cash_Advance CashAdvanceList(int row) {
        return (Model_Cash_Advance) paMaster.get(row);
    }

    /**
     * Gets the total number of Cash Advance records.
     *
     * @return number of records
     */
    public int getCashAdvanceCount() {
        return this.paMaster.size();
    }

    /*RESET CACHE ROW SET*/
    /**
     * Resets the master record to its default initial state.
     */
    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).CashAdvanceMaster();
    }
    
    /**
     * Resets the other record to its default initial state.
     */
    public void resetOthers() {
        paAttachments = new ArrayList<>();
    }

    /**
     * Completely clears the current transaction state.
     * <p>
     * Resets the master model, clears all detail and attachment collections, 
     * and wipes the industry and payee search filters.
     */
    public void resetTransaction(){
        resetMaster();
        Detail().clear();
        paAttachments = new ArrayList<>();
        psApprover = "";
        setApproving("");
    }
   
    /**
     * Refines and validates the transaction detail list.
     * <p>
     * This method prunes invalid rows (those with empty particulars or zero amounts for new records) 
     * and automatically appends a new detail row if the list is empty or the last entry is valid.
     * 
     * @throws CloneNotSupportedException If an error occurs while adding a new detail row.
     */
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if ((Detail(lnCtr).getORNo() == null || "".equals(Detail(lnCtr).getORNo()))
                && (Detail(lnCtr).getTransactionDate() == null || "".equals(Detail(lnCtr).getTransactionDate()) || "1900-01-01".equals(Detail(lnCtr).getTransactionDate()))
                && (Detail(lnCtr).getAccountCode() == null || "".equals(Detail(lnCtr).getAccountCode()))
                && (Detail(lnCtr).getParticular() == null || "".equals(Detail(lnCtr).getParticular()))) {
                deleteDetail(lnCtr);
            } else if ((Detail(lnCtr).getORNo() == null || "".equals(Detail(lnCtr).getORNo()))
                && (Detail(lnCtr).getAccountCode() == null || "".equals(Detail(lnCtr).getAccountCode()))
                && (Detail(lnCtr).getParticular() == null || "".equals(Detail(lnCtr).getParticular()))){
                if(Detail(lnCtr).getEditMode() == EditMode.UPDATE){
                    try {
                        Model_Cash_Advance_Detail loObj = new CashflowModels(poGRider).CashAdvanceDetail();
                        loObj.openRecord(Detail(lnCtr).getTransactionNo(),Detail(lnCtr).getEntryNo());
                        loObj.updateRecord();
                        loObj.isReverse(false);
                        Detail().set(lnCtr, loObj); //Set original value
                    } catch (SQLException | GuanzonException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                        Detail(lnCtr).isReverse(false);
                    }
                } 
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            if (Detail(getDetailCount() - 1).getParticular() != null && !"".equals(Detail(getDetailCount() - 1).getParticular())
                && Detail(getDetailCount() - 1).getTransactionAmount() > 0.00
                && Detail(getDetailCount() - 1).getTransactionDate() != null 
                && !"1900-01-01".equals(xsDateShort(Detail(getDetailCount() - 1).getTransactionDate()))
                && Detail(getDetailCount() - 1).isReverse()
                ) {
                AddDetail();
            } else if (Detail(getDetailCount() - 1).getParticular() != null && !"".equals(Detail(getDetailCount() - 1).getParticular())
                && Detail(getDetailCount() - 1).getTransactionAmount() > 0.00
                && Detail(getDetailCount() - 1).getTransactionDate() != null 
                && !"1900-01-01".equals(xsDateShort(Detail(getDetailCount() - 1).getTransactionDate()))
                && !Detail(getDetailCount() - 1).isReverse()
                ) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
    
    /** Formats a date to "yyyy-MM-dd"; returns "1900-01-01" if the input is null. */
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
    
    /**
     * Sets default master record values for a new transaction.
     * <p>
     * Configures the branch, industry, and company identifiers, sets the current 
     * server date, and initializes the status to {@link CashAdvanceStatus#OPEN}.
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
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(CashAdvanceStatus.OPEN);

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
        GValidator loValidator = new CashAdvanceValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    /**
     * Prepares and validates the transaction data before committing to the database.
     * <p>
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
        }
        
        poJSON = isEntryOkay(Master().getTransactionStatus());
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        if (Master().getLiquidationTotal() == 0.0000) {
            poJSON = setJSON("error", "Liquidation total cannot be zero.");
            return poJSON;
        }
        
        if (Master().getLiquidationTotal() > Master().CashFund().getBalance()) {
            poJSON = setJSON("error", "Advance amount cannot be greater than the cash fund balance.");
            return poJSON;
        }
        
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
                if(Detail(lnCtr).getTransactionAmount() > 0.00){
                    if(Detail(lnCtr).getTransactionDate() == null || "1900-01-01".equals(xsDateShort(Detail(lnCtr).getTransactionDate()))){
                        poJSON = setJSON("error", "Transaction date cannot be empty at row "+lnRow+".");
                        return poJSON;
                    }
                    if(Detail(lnCtr).getParticular() == null || "".equals(Detail(lnCtr).getParticular())){
                        poJSON = setJSON("error", "Particular cannot be empty at row "+lnRow+".");
                        return poJSON;
                    }
                }
            }
        }
        
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions
            String lsSourceNo = (String) item.getValue("sPartculr");
            double lsAmount = Double.parseDouble(String.valueOf(item.getValue("nTranAmtx")));
            if ((lsAmount == 0.0000 || "".equals(lsSourceNo) || lsSourceNo == null)
                ){
                if(item.getEditMode() == EditMode.ADDNEW ){
                    detail.remove(); // Correctly remove the item
                } else {
                    Model_Cash_Advance_Detail loObj = new CashflowModels(poGRider).CashAdvanceDetail();
                    loObj.openRecord((String) item.getValue("sTransNox"),(int) item.getValue("nEntryNox"));
                    loObj.updateRecord();
                    loObj.isReverse(false);
                    Detail().set((int) item.getValue("nEntryNox"), loObj); //Set original value 
                }
            }
        }
        
        if (getDetailCount() <= 0) {
            poJSON = setJSON("error", "Transaction amount cannot be zero.");
            return poJSON;
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with transaction amount detail
            if (Detail(0).getTransactionAmount() == 0.0000) {
                poJSON = setJSON("error", "Transaction amount cannot be zero.");
                return poJSON;
            }
        }
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
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
        System.out.println("Issued By : " + Master().getIssuedBy());
        System.out.println("Issued Date : " + Master().getIssuedDate());
        System.out.println("Liquidated By : " + Master().getLiquidatedBy());
        System.out.println("Liquidated Date : " + Master().getLiquidatedDate());
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            System.out.println("COUNTER : " + lnCtr);
            System.out.println("Transaction No : " + Detail(lnCtr).getTransactionNo());
            System.out.println("Transaction Date : " + Detail(lnCtr).getTransactionDate());
            System.out.println("Detail Account Code : " + Detail(lnCtr).getAccountCode());
            System.out.println("Detail Particular : " + Detail(lnCtr).getParticular());
            System.out.println("Amount : " + Detail(lnCtr).getTransactionAmount());
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
        return isEntryOkay(DisbursementStatic.OPEN);
    }

    /**
     * Handles the saving of supplementary data, specifically transaction attachments.
     * <p>
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
            System.out.println("Issued By : " + Master().getIssuedBy());
            System.out.println("Issued Date : " + Master().getIssuedDate());
            System.out.println("Liquidated By : " + Master().getLiquidatedBy());
            System.out.println("Liquidated Date : " + Master().getLiquidatedDate());
            
            System.out.println("-----------------------------------------------------------------------");
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
                System.out.println("COUNTER : " + lnCtr);
                System.out.println("Transaction No : " + Detail(lnCtr).getTransactionNo());
                System.out.println("Transaction Date : " + Detail(lnCtr).getTransactionDate());
                System.out.println("Detail Account Code : " + Detail(lnCtr).getAccountCode());
                System.out.println("Detail Particular : " + Detail(lnCtr).getParticular());
                System.out.println("Amount : " + Detail(lnCtr).getTransactionAmount());
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
     * <p>
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

        SQL_BROWSE =   " SELECT "
                    + "   a.sTransNox "
                    + " , a.sCompnyID "
                    + " , a.sBranchCd "
                    + " , a.sIndstCdx "
                    + " , a.dTransact "
                    + " , a.sClientID "
                    + " , a.sDeptReqs "
                    + " , a.sCashFIDx "
                    + " , a.sRemarksx "
                    + " , a.nAdvAmtxx "
                    + " , a.sIssuedxx "
                    + " , a.dIssuedxx "
                    + " , a.nLiqTotal "
                    + " , a.sLiquidtd "
                    + " , a.dLiquidtd "  
                    + " , a.cTranStat "
                    + " , a.sModified "
                    + " , a.dModified "
                    + " , b.sCompnyNm AS sCompanyx "
                    + " , c.sDescript AS sIndustry "
                    + " , d.sDeptName AS sDeptName "
                    + " , e.sCompnyNm AS sPayeexxx "
                    + " , f.sCashFDsc AS sCashFund "
                    + " , g.sBranchNm AS sBranchNm "
                    + " FROM CashAdvance a "
                    + " LEFT JOIN Company b ON b.sCompnyID = a.sCompnyID "
                    + " LEFT JOIN Industry c ON c.sIndstCdx = a.sIndstCdx "
                    + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptReqs "
                    + " LEFT JOIN Client_Master e ON e.sClientID = a.sClientID "
                    + " LEFT JOIN CashFund f ON f.sCashFIDx = a.sCashFIDx "
                    + " LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd ";
        if(lsCondition != null && !"".equals(lsCondition)){
            SQL_BROWSE = MiscUtil.addCondition(SQL_BROWSE, lsCondition);
        }
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
                case CashAdvanceStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case CashAdvanceStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case CashAdvanceStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case CashAdvanceStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case CashAdvanceStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case CashAdvanceStatus.LIQUIDATED:
                    crs.updateString("cRefrStat", "LIQUIDATED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case CashAdvanceStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case CashAdvanceStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case CashAdvanceStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case CashAdvanceStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case CashAdvanceStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                        case CashAdvanceStatus.LIQUIDATED:
                            crs.updateString("cRefrStat", "LIQUIDATED");
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

        showStatusHistoryUI("Cash Advance", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
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
