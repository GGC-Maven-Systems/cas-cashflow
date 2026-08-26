package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cash_Fund_Ledger;
import ph.com.guanzongroup.cas.cashflow.model.Model_PettyCashLedger;
import ph.com.guanzongroup.cas.cashflow.model.Model_Replenishment_Request;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CashFundStatus;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.status.PettyCashStatus;
import ph.com.guanzongroup.cas.cashflow.status.ReplenishmentRequestStatus;

//Arsiela 08-22-2026
public class ReplenishmentRequest extends Parameter {
    public String psIndustryId = "";
    public String psCompanyId = "";
    public String psFund = "";
    public String psApprover = "";
    
    Model_Replenishment_Request poModel;
    public List<Model_Replenishment_Request> paModel;
    
    public List<Model_Cash_Fund_Ledger> paCashFundLedger;
    public List<Model_Cash_Fund_Ledger> paRemovedCashFundLedger;
    public List<Model_PettyCashLedger> paPettyCashLedger;
    public List<Model_PettyCashLedger> paRemovedPettyCashLedger;
    
    public List<Model_Cash_Fund_Ledger> paLoadCashFundLedger;
    public List<Model_PettyCashLedger> paLoadPettyCashLedger;
    
    /**
    * Initializes the Replenishment Request controller and its model.
    *
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    @Override
    public void initialize() throws SQLException, GuanzonException {
        psRecdStat = Logical.YES;

        CashflowModels model = new CashflowModels(poGRider);
        poModel = model.Replenishment_Request();
        paCashFundLedger = new ArrayList<Model_Cash_Fund_Ledger>();
        paRemovedCashFundLedger = new ArrayList<Model_Cash_Fund_Ledger>();
        paPettyCashLedger = new ArrayList<Model_PettyCashLedger>();
        paRemovedPettyCashLedger = new ArrayList<Model_PettyCashLedger>();
        paModel = new ArrayList<Model_Replenishment_Request>();
        psApprover = "";
        super.initialize();
    }
    
    /**
    * Initializes default values for Replenishment Request fields.
    *
    * @return JSONObject result container
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    @Override
    public JSONObject initFields()
            throws SQLException,
            GuanzonException {
        
        poModel.setIndustryId(psIndustryId);
        poModel.setCompanyId(psCompanyId);
        poModel.setTransactionDate(SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE)); 
        
        return poJSON;
    }
    
    //Set default values for filtering data
    public void setIndustryId(String industryId) { psIndustryId = industryId; }
    public void setCompanyId(String companyId) { psCompanyId = companyId; }
    public void setFund(String Fund) { psFund = Fund; }
    public String getfund() { return psFund; }
    
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
//            setApproving(lsUserIDxx);
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
     * Completely clears the current transaction state.
     * 
     * Resets the master model, clears all detail and attachment collections, 
     * and wipes the industry and payee search filters.
     */
    public void resetTransaction(){
        paCashFundLedger = new ArrayList<>();
        paPettyCashLedger = new ArrayList<>();
        paRemovedCashFundLedger = new ArrayList<>();
        paRemovedPettyCashLedger = new ArrayList<>();
        paLoadCashFundLedger = new ArrayList<>();
        paLoadPettyCashLedger = new ArrayList<>();
    }
    
    public JSONObject OpenRecord(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        //Reset Transaction
        resetTransaction();
        
        poJSON = openRecord(transactionNo);
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load record. " + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadLedger(false);
        if (!isJSONSuccess(poJSON)) {
            System.out.println("Unable to load ledger list. " + (String) poJSON.get("message"));
//            poJSON = setJSON((String) poJSON.get("result"),"Unable to load ledger list. " + (String) poJSON.get("message"));
//            return poJSON;
        }
        poJSON = setJSON("success","success");
        return poJSON;
    }
    
    public JSONObject SaveRecord() throws SQLException{
        try {
            poJSON = new JSONObject();
            poJSON = saveRecord();
        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }
        
        return poJSON;
    }
    
    /**
    * Activate the current Replenishment Request record.
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
    */
    public JSONObject ApproveRecord()
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = ReplenishmentRequestStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }

        if (lsStatus.equals(poModel.getTransactionStatus())) {
            poJSON = setJSON("error", "Record was already approved.");
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
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
            if(!lsDepartment.equals(poGRider.getDepartment())){ //Approval of the Custodian's Supervisor / Manager //need to check custodian's supervisor
                poJSON.put("result", "error" );
                poJSON.put("message", "User or approving officer is not authorized to approved the record." );
                return poJSON;
            }
        }

        //validator
        poJSON = isEntryOkay();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ApproveRecord", SOURCE_CODE, getModel().getTransactionNo());
        
        //Generate PRF
        poJSON = generatePRF(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Record approved successfully.");
        return poJSON;
    }
    
    /**
    * Void the current record.
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
    */
    public JSONObject VoidRecord() throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = ReplenishmentRequestStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }

        if (lsStatus.equals(poModel.getTransactionStatus())) {
            poJSON = setJSON("error", "Record was already voided.");
            return poJSON;
        }
        if (ReplenishmentRequestStatus.APPROVED.equals(poModel.getTransactionStatus())) {
            poJSON = setJSON("error", "Record was already approved.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidRecord", SOURCE_CODE, getModel().getTransactionNo());
        
        //Removed batch no for saved ledger
        poJSON = updateLedger(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Record voided successfully.");
        return poJSON;
    }
    
    /**
    * Cancel the current record.
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
    */
    public JSONObject CancelRecord() throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = ReplenishmentRequestStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }

        if (lsStatus.equals(poModel.getTransactionStatus())) {
            poJSON = setJSON("error", "Record was already cancelled.");
            return poJSON;
        }
        
        if(ReplenishmentRequestStatus.APPROVED.equals(poModel.getTransactionStatus())){
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
//                if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
                if(!lsDepartment.equals(poGRider.getDepartment())){ //Approval of the Custodian's Supervisor / Manager //need to check custodian's supervisor
                    poJSON.put("result", "error" );
                    poJSON.put("message", "User or approving officer is not authorized to cancelled the record." );
                    return poJSON;
                }
            }
        }

        //validator
        poJSON = isEntryOkay();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelRecord", SOURCE_CODE, getModel().getTransactionNo());
        
        //Removed batch no for saved ledger
        poJSON = updateLedger(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = cancelPRF();
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Record cancelled successfully.");
        return poJSON;
    }
    
    
    /**
    * POST the current Replenishment Request record.
    *
    * @return JSONObject containing the result of the confirmation process
    * @throws ParseException if date parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    * @throws CloneNotSupportedException if cloning is not supported
    */
    public JSONObject PostRecord() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = ReplenishmentRequestStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No record was loaded.");
            return poJSON;
        }

        if (lsStatus.equals(poModel.getTransactionStatus())) {
            poJSON = setJSON("error", "Record was already posted.");
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
//            if(!lsDepartment.equals(System.getProperty("sys.dept.finance"))){
            if(!lsDepartment.equals(poGRider.getDepartment())){ //Approval of the Custodian's Supervisor / Manager //need to check custodian's supervisor
                poJSON.put("result", "error" );
                poJSON.put("message", "User or approving officer is not authorized to post the record." );
                return poJSON;
            }
        }

        //validator
        poJSON = isEntryOkay();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "PostRecord", SOURCE_CODE, getModel().getTransactionNo());
        
        try {
            if(Logical.YES.equals(getModel().getFundType())){
                CashFundTrans loCashFundTrans = new CashFundTrans(poGRider);
                loCashFundTrans.InitTransaction(getModel().getFundId(), poGRider.getBranchCode(), poGRider.getDepartment());
                    poJSON = loCashFundTrans.Replenishment(getModel().getTransactionNo(), LocalDate.parse(xsDateShort(poGRider.getServerDate())),  getModel().getTransactionAmount(), false);

                if (!isJSONSuccess(poJSON)) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
            } else {
                PettyCashTrans loPettyCashTrans = new PettyCashTrans(poGRider);
                loPettyCashTrans.InitTransaction(getModel().getFundId(), poGRider.getBranchCode(), poGRider.getDepartment());
                poJSON = loPettyCashTrans.Replenishment(getModel().getTransactionNo(), LocalDate.parse(xsDateShort(poGRider.getServerDate())),  getModel().getTransactionAmount(), false);
                if (!isJSONSuccess(poJSON)) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
            }
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            poGRider.rollbackTrans();
            return poJSON;
        } 
        
        poJSON = statusChange(poModel.getTable(), (String) poModel.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Record posted successfully.");
        return poJSON;
    }
    
    /**
    * Validates if the Replenishment Request entry is ready to be saved.
    *
    * @return JSONObject containing validation result and message if invalid
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    @Override
    public JSONObject isEntryOkay() throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        if (poModel.getTransactionNo()== null || "".equals(poModel.getTransactionNo())) {
            poJSON = setJSON("error", "Transaction No must not be empty.");
            return poJSON;
        }

        if (poModel.getFundType() == null || "".equals(poModel.getFundType())) {
            poJSON = setJSON("error", "Fund type must not be empty.");
            return poJSON;
        }

        if (poModel.getFundId() == null || "".equals(poModel.getFundId())) {
            poJSON = setJSON("error", "Cash fund ID must not be empty.");
            return poJSON;
        }

        if (poModel.getTransactionAmount()<= 0.0000) {
            poJSON = setJSON("error", "Invalid transaction amount.");
            return poJSON;
        }
        
        poModel.setModifiedBy(poGRider.getUserID());
        poModel.setModifiedDate(poGRider.getServerDate());
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Returns the Replenishment Request model instance.
     *
     * @return Model_Replenishment_Request object
     */
    @Override
    public Model_Replenishment_Request getModel() {
        return poModel;
    }
    
    /**
    * Searches a Replenishment Request record using the given value.
    *
    * @param value   the search key
    * @param byCode  true to search by code, false to search by description
    * @return JSONObject containing the selected record or an error message if none was selected
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    @Override
    public JSONObject searchRecord(String value, boolean byCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();
        
        String lsCondition = "";
        if(psCompanyId != null && !"".equals(psCompanyId)){
//                lsCondition = " AND IF(a.cFundType = '1',b.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + " ,c.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ")";
            lsCondition = " AND ("
                        + " (a.cFundType = '1' AND b.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                        + " OR (a.cFundType <> '1' AND c.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                        + " )";
            
        }
        if(psIndustryId != null && !"".equals(psIndustryId)){
            if(lsCondition.isEmpty()){
//                lsCondition = " AND IF(a.cFundType = '1',b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + " ,c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ")";
                lsCondition = " AND ("
                            + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " )";
                
            } else {
//                lsCondition = lsCondition + " AND IF(a.cFundType = '1',b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + " ,c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ")";
                lsCondition = lsCondition + " AND ("
                            + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " )";
            }
        }
        
        if(!lsCondition.isEmpty()){
            lsSQL = lsSQL + " " + lsCondition;
        }
        
        System.out.println("MySQL : " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Date»Fund Type»Description",
                "sTransNox»dTransact»sFundType»sFundDesc",
                "a.sTransNox»a.dTransact»(CASE WHEN a.cFundType = '1' THEN 'CASH FUND' ELSE 'PETTY CASH' END)»(CASE WHEN a.cFundType = '1' THEN b.sCashFDsc ELSE c.sPettyDsc END)",
                byCode ? 0 : 3);

        if (poJSON != null) {
            try {
                return OpenRecord((String) poJSON.get("sTransNox"));
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON = new JSONObject();
                poJSON = setJSON("error", MiscUtil.getException(ex));
                return poJSON;
            }
        } else {
            poJSON = new JSONObject();
            poJSON = setJSON("error", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
    * Searches for a branch and assigns it to the current Replenishment Request model.
    *
    * @param value     the search key
    * @param byCode    true to search by code, false to search by description
    * @param isSearch  indicates if the action is triggered from search
    * @return JSONObject containing the search result
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    public JSONObject SearchFund(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        CashflowControllers loController = new CashflowControllers(poGRider, logwrapr);
        if(!isSearch){
            if(getModel().getFundType() == null || "".equals(getModel().getFundType())){
                poJSON = setJSON("error", "Fund Type cannot be empty.");
                return poJSON;
            }

            if (Logical.YES.equals(getModel().getFundType())) {
                CashFund loCashFund = loController.CashFund();
                loCashFund.setRecordStatus(RecordStatus.ACTIVE);
                loCashFund.setDepartmentId(poGRider.getDepartment());
                loCashFund.setBranchCode(poGRider.getBranchCode());
                loCashFund.setCompanyId(psCompanyId);
                loCashFund.setIndustryId(psIndustryId);

                poJSON = loCashFund.searchRecord(value, byCode);
                if (isJSONSuccess(poJSON)) {
                    getModel().setFundId(loCashFund.getModel().getCashFundId());
                }
            } else {
                PettyCash loPettyCash = loController.PettyCash();
                loPettyCash.setRecordStatus(RecordStatus.ACTIVE);
                loPettyCash.setDepartmentId(poGRider.getDepartment());
                loPettyCash.setBranchCode(poGRider.getBranchCode());
                loPettyCash.setCompanyId(psCompanyId);
                loPettyCash.setIndustryId(psIndustryId);

                poJSON = loPettyCash.searchRecord(value, byCode);
                if (isJSONSuccess(poJSON)) {
                    getModel().setFundId(loPettyCash.getModel().getPettyId());
                }
            }
        } else {
            poJSON = searchRecord(value, false);
            if (isJSONSuccess(poJSON)) {
                setFund((String) poJSON.get("fund"));
            }
        
        }
        return poJSON;
    }
    
    /**
    * Searches a Replenishment Request custodian using the given value.
    *
    * @param value   the search key
    * @param byCode  true to search by code, false to search by description
    * @return JSONObject containing the selected record or an error message if none was selected
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    private JSONObject searchFund(String value) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsFund = "";
//        String lsSQL = "SELECT " 
//                    + "  a.sCashFIDx as fundId, " 
//                    + "  a.sBranchCD, " 
//                    + "  a.sDeptIDxx, " 
//                    + "  a.sCompnyID, " 
//                    + "  a.sIndstCdx, " 
//                    + "  a.sCashFDsc as description, " 
//                    + "  a.cTranStat "
//                    + " FROM CashFund a " 
//                    + " WHERE a.sDeptIDxx = " + SQLUtil.toSQL( poGRider.getDepartment())
//                    + " AND a.sCashFDsc LIKE " + SQLUtil.toSQL( "%"+value+"%")
//                    + " AND a.cTranStat = " + SQLUtil.toSQL(CashFundStatus.ACTIVE)
//                    + " AND a.sCompnyID = " + SQLUtil.toSQL( psCompanyId)
//                    + " AND a.sIndstCdx = " + SQLUtil.toSQL( psIndustryId)
//                    + " UNION " 
//                    + " SELECT " 
//                    + "  a.sPettyIDx as fundId, " 
//                    + "  a.sBranchCD, " 
//                    + "  a.sDeptIDxx, " 
//                    + "  a.sCompnyID, " 
//                    + "  a.sIndstCdx, " 
//                    + "  a.sPettyDsc as description, " 
//                    + "  a.cTranStat " 
//                    + " FROM PettyCash a "
//                    + " WHERE a.sDeptIDxx = " + SQLUtil.toSQL( poGRider.getDepartment())
//                    + " AND a.sPettyDsc LIKE " + SQLUtil.toSQL( "%"+value+"%")
//                    + " AND a.cTranStat = " + SQLUtil.toSQL(PettyCashStatus.ACTIVE)
//                    + " AND a.sCompnyID = " + SQLUtil.toSQL( psCompanyId)
//                    + " AND a.sIndstCdx = " + SQLUtil.toSQL( psIndustryId)
//                    ;
            String lsCondition = "";
            if(psCompanyId != null && !"".equals(psCompanyId)){
                lsCondition = " AND ("
                            + " (a.cFundType = '1' AND b.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                            + " OR (a.cFundType <> '1' AND c.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                            + " )";

            }
            if(psIndustryId != null && !"".equals(psIndustryId)){
                if(lsCondition.isEmpty()){
                    lsCondition = " AND ("
                                + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                                + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                                + " )";

                } else {
                    lsCondition = lsCondition + " AND ("
                                + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                                + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                                + " )";
                }
            }

            String lsSQL = MiscUtil.addCondition(getSQ_Browse(),  " ("
                            + " (a.cFundType = '1' AND b.sCashFDsc LIKE " + SQLUtil.toSQL("%"+value+"%") + ") "
                            + " OR (a.cFundType <> '1' AND c.sPettyDsc LIKE " + SQLUtil.toSQL("%"+value+"%") + ") "
                            + " )");
            lsSQL = lsSQL + lsCondition;
            lsSQL = lsSQL + " AND ("
                        + " (a.cFundType = '1' AND b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) + ") "
                        + " OR (a.cFundType <> '1' AND c.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) + ") "
                        + " )";
            lsSQL = lsSQL + " ORDER BY a.dTransact, a.sTransNox ASC ";
            System.out.println("Executing SQL: " + lsSQL);
        System.out.println("Executing SQL: " + lsSQL);
        JSONObject loJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Transaction No»Description",
                "sTransNox»description",
                "a.sTransNox»IFNULL(a.sPettyDsc,a.sCashFDsc)",
                1);
        if (loJSON != null) {
            lsFund = (String) loJSON.get("description");
        } else {
            loJSON = setJSON("error", "No record loaded.");
            return loJSON;
        }
        
        poJSON = setJSON("success", "success");
        poJSON.put("fund", lsFund);
        return poJSON;
    }
    
    /**
    * Loads a list of transactions based on the provided filters.
    * 
    * @return A {@link JSONObject} indicating "success" if records were loaded, 
    *         otherwise returns an "error" status with a descriptive message.
    * @throws SQLException     If a database access error occurs.
    * @throws GuanzonException If an application-level error occurs during record opening.
    */
    public JSONObject loadTransactionList(String fsFund, String fsTransactionNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paModel = new ArrayList<>();
        if (fsFund == null) { fsFund = ""; }
        if (fsTransactionNo == null) { fsTransactionNo = ""; }
        
        String lsCondition = "";
        if(psCompanyId != null && !"".equals(psCompanyId)){
//                lsCondition = " AND IF(a.cFundType = '1',b.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + " ,c.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ")";
            lsCondition = " AND ("
                        + " (a.cFundType = '1' AND b.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                        + " OR (a.cFundType <> '1' AND c.sCompnyID = " + SQLUtil.toSQL(psCompanyId) + ") "
                        + " )";
            
        }
        if(psIndustryId != null && !"".equals(psIndustryId)){
            if(lsCondition.isEmpty()){
//                lsCondition = " AND IF(a.cFundType = '1',b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + " ,c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ")";
                lsCondition = " AND ("
                            + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " )";
                
            } else {
//                lsCondition = lsCondition + " AND IF(a.cFundType = '1',b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + " ,c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ")";
                lsCondition = lsCondition + " AND ("
                            + " (a.cFundType = '1' AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " OR (a.cFundType <> '1' AND c.sIndstCdx = " + SQLUtil.toSQL(psIndustryId) + ") "
                            + " )";
            }
        }
        
        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo + "%"));
        lsSQL = lsSQL + lsCondition;
//        lsSQL = lsSQL + " AND IF(a.cFundType = '1',b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) + " ,c.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) +  ")";
//        lsSQL = lsSQL + " AND IF(a.cFundType = '1',b.sCashFDsc LIKE " + SQLUtil.toSQL("%"+fsFund+"%") + " ,c.sPettyDsc LIKE " + SQLUtil.toSQL("%"+fsFund+"%") +  ")";

        lsSQL = lsSQL + " AND ("
                    + " (a.cFundType = '1' AND b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) + ") "
                    + " OR (a.cFundType <> '1' AND c.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()) + ") "
                    + " )";
        lsSQL = lsSQL + " AND ("
                    + " (a.cFundType = '1' AND b.sCashFDsc LIKE " + SQLUtil.toSQL("%"+fsFund+"%") + ") "
                    + " OR (a.cFundType <> '1' AND c.sPettyDsc LIKE " + SQLUtil.toSQL("%"+fsFund+"%") + ") "
                    + " )";
        
        lsSQL = lsSQL + " ORDER BY a.dTransact, a.sTransNox ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
//            poJSON = setJSON("error", "No record found.");
//            return poJSON;
        }

        while (loRS.next()) {
            Model_Replenishment_Request loObject = new CashflowModels(poGRider).Replenishment_Request();
            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
            if (isJSONSuccess(poJSON)) {
                paModel.add((Model_Replenishment_Request) loObject);
            } else {
                return poJSON;
            }
        }
        MiscUtil.close(loRS);
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
    * Retrieves a specific record from the transaction list.
    * 
    * @param row The index of the record to retrieve.
    * @return The Model_Replenishment_Request instance at the specified row.
    */
    public Model_Replenishment_Request TransactionList(int row) {
        return (Model_Replenishment_Request) paModel.get(row);
    }
    /**
     * Returns the total number of records in the transaction list.
     * 
     * @return The size of the transaction list.
     */
    public int getTransactionListCount() {
        return this.paModel.size();
    }
    
    /**
    * Loads ledger records
    *
    * @return JSONObject containing status or error message
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if business logic fails
    */
    public JSONObject loadLedger(boolean fbIsUI) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(getModel().getFundId() == null || "".equals(getModel().getFundId())){
            poJSON = setJSON("error", "Fund cannot be empty.");
            return poJSON;
        }
        
        if(paCashFundLedger == null){
            paLoadCashFundLedger = new ArrayList<>();
        }

        if(paPettyCashLedger == null){
            paPettyCashLedger = new ArrayList<>();
        }
        
        if(paRemovedCashFundLedger == null){
            paRemovedCashFundLedger = new ArrayList<>();
        }
        if(paRemovedPettyCashLedger == null){
            paRemovedPettyCashLedger = new ArrayList<>();
        }
        
        paLoadCashFundLedger = new ArrayList<>();
        paLoadPettyCashLedger = new ArrayList<>();
        ResultSet loRS;
        String lsSQL = "";
        if(Logical.YES.equals(getModel().getFundType())){
            if(fbIsUI){
                lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new CashflowModels(poGRider).CashFundLedger()),
                    " sCashFIDx = " + SQLUtil.toSQL(getModel().getFundId())
                    + " AND cReversex = "  + SQLUtil.toSQL(CashFundStatus.Reverse.INCLUDE)
                    + " AND (sBatchNox IS NULL OR sBatchNox = '') "
                );
            } else {
                lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new CashflowModels(poGRider).CashFundLedger()),
                    " sBatchNox = " + SQLUtil.toSQL(getModel().getTransactionNo())
                    + " AND cReversex = "  + SQLUtil.toSQL(CashFundStatus.Reverse.INCLUDE)
                );
            }
            
            lsSQL = lsSQL + " GROUP BY sCashFIDx, sSourceCD, sSourceNo ORDER BY dTransact, nLedgerNo ASC ";
            System.out.println("Executing SQL: " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                poJSON = setJSON("error", "No record found.");
                return poJSON;
            }

            while (loRS.next()) {
                Model_Cash_Fund_Ledger loObject = new CashflowModels(poGRider).CashFundLedger();
                poJSON = loObject.openRecord(loRS.getString("sCashFIDx"),loRS.getString("sSourceCD"),loRS.getString("sSourceNo"));
                if (isJSONSuccess(poJSON)) {
                    if(fbIsUI){
                        if(!paCashFundLedger.contains((Model_Cash_Fund_Ledger) loObject)){
                            paLoadCashFundLedger.add((Model_Cash_Fund_Ledger) loObject);
                        }
                    } else {
                        if(!paCashFundLedger.contains((Model_Cash_Fund_Ledger) loObject)){
                            paCashFundLedger.add((Model_Cash_Fund_Ledger) loObject);
                        }
                    }
                }
            }
            MiscUtil.close(loRS);
        } else {
            if(fbIsUI){
                lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new CashflowModels(poGRider).PettyCashFundLedger()),
                    " sPettyIDx = " + SQLUtil.toSQL(getModel().getFundId())
                    + " AND cReversex = "  + SQLUtil.toSQL(PettyCashStatus.Reverse.INCLUDE)
                    + " AND (sBatchNox IS NULL OR sBatchNox = '') "
                );
            } else {
                lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new CashflowModels(poGRider).PettyCashFundLedger()),
                    " sBatchNox = " + SQLUtil.toSQL(getModel().getTransactionNo())
                    + " AND cReversex = "  + SQLUtil.toSQL(PettyCashStatus.Reverse.INCLUDE)
                );
            }
            lsSQL = lsSQL + " GROUP BY sPettyIDx, sSourceCD, sSourceNo ORDER BY dTransact, nLedgerNo ASC ";
            System.out.println("Executing SQL: " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                poJSON = setJSON("error", "No record found.");
                return poJSON;
            }

            while (loRS.next()) {
                Model_PettyCashLedger loObject = new CashflowModels(poGRider).PettyCashFundLedger();
                poJSON = loObject.openRecord(loRS.getString("sPettyIDx"),loRS.getString("sSourceCD"),loRS.getString("sSourceNo"));
                if (isJSONSuccess(poJSON)) {
                    if(fbIsUI){
                        if(!paPettyCashLedger.contains((Model_PettyCashLedger) loObject)){ 
                            paLoadPettyCashLedger.add((Model_PettyCashLedger) loObject);
                        }
                    } else {
                        if(!paPettyCashLedger.contains((Model_PettyCashLedger) loObject)){ 
                            paPettyCashLedger.add((Model_PettyCashLedger) loObject);
                        }
                    }
                }
            }
            MiscUtil.close(loRS);
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
    * Retrieves a specific ledger record from the transaction list.
    * 
    * @param row The index of the record to retrieve.
    * @return The Model_Cash_Fund_Ledger instance at the specified row.
    */
    public Model_Cash_Fund_Ledger LoadCashFundLedgerList(int row) {
        return (Model_Cash_Fund_Ledger) paLoadCashFundLedger.get(row);
    }
    public Model_Cash_Fund_Ledger CashFundLedgerList(int row) {
        return (Model_Cash_Fund_Ledger) paCashFundLedger.get(row);
    }
    public Model_Cash_Fund_Ledger RemovedCashFundLedgerList(int row) {
        return (Model_Cash_Fund_Ledger) paRemovedCashFundLedger.get(row);
    }
    /**
    * Retrieves a specific ledger record from the transaction list.
    * 
    * @param row The index of the record to retrieve.
    * @return The Model_Cash_Fund_Ledger instance at the specified row.
    */
    public Model_PettyCashLedger LoadPettyCashLedgerList(int row) {
            return (Model_PettyCashLedger) paLoadPettyCashLedger.get(row);
    }
    public Model_PettyCashLedger PettyCashLedgerList(int row) {
            return (Model_PettyCashLedger) paPettyCashLedger.get(row);
    }
    public Model_PettyCashLedger RemovedPettyCashLedgerList(int row) {
            return (Model_PettyCashLedger) paRemovedPettyCashLedger.get(row);
    }
    
    /**
     * Returns the total number of records in the ledger transaction list.
     * 
     * @return The size of the transaction list.
     */
    public int getLoadCashFundLedgerListCount() {
        return this.paLoadCashFundLedger.size();
    }
    public int getCashFundLedgerListCount() {
        return this.paCashFundLedger.size();
    }
    public int getRemovedCashFundLedgerListCount() {
        return this.paRemovedCashFundLedger.size();
    }
    /**
     * Returns the total number of records in the ledger transaction list.
     * 
     * @return The size of the transaction list.
     */
    public int getLoadPettyCashLedgerListCount() {
        return this.paLoadPettyCashLedger.size();
    }
    public int getPettyCashLedgerListCount() {
        return this.paPettyCashLedger.size();
    }
    public int getRemovedPettyCashLedgerListCount() {
        return this.paRemovedPettyCashLedger.size();
    }
    
    public void RemoveCashFundLedger(List<Model_Cash_Fund_Ledger> faModel){
        if(paRemovedCashFundLedger == null){
            paRemovedCashFundLedger = new ArrayList<>();
        }
        for(int lnCtr = 0; lnCtr < faModel.size(); lnCtr++){
            Model_Cash_Fund_Ledger loModel = faModel.get(lnCtr);
            if(loModel.getBatchNo() != null && !"".equals(loModel.getBatchNo())){
                paRemovedCashFundLedger.add((Model_Cash_Fund_Ledger) loModel);
            }
            
            paCashFundLedger.remove((Model_Cash_Fund_Ledger) loModel);
        }
    }
    
    public void RemovePettyCashLedger(List<Model_PettyCashLedger> faModel){
        if(paRemovedPettyCashLedger == null){
            paRemovedPettyCashLedger = new ArrayList<>();
        }
        
        for(int lnCtr = 0; lnCtr < faModel.size(); lnCtr++){
            Model_PettyCashLedger loModel = faModel.get(lnCtr);
            if(loModel.getBatchNo() != null && !"".equals(loModel.getBatchNo())){
                paRemovedPettyCashLedger.add((Model_PettyCashLedger) loModel);
            }
            
            paPettyCashLedger.remove((Model_PettyCashLedger) loModel);
        }
    }
    
    public JSONObject AddPettyCashLedger(List<Model_PettyCashLedger> faModel){
        poJSON = new JSONObject();
        if(paRemovedPettyCashLedger == null){
            paRemovedPettyCashLedger = new ArrayList<>();
        }
        if(paPettyCashLedger == null){
            paPettyCashLedger = new ArrayList<>();
        }
        for(int lnCtr = 0; lnCtr < faModel.size(); lnCtr++){
            if(!paPettyCashLedger.contains(faModel.get(lnCtr))){ 
                paPettyCashLedger.add(faModel.get(lnCtr));
//                if(faModel.get(lnCtr).getEditMode() != EditMode.UPDATE){
//                    poJSON = paPettyCashLedger.get(paPettyCashLedger.size()-1).updateRecord();
//                    if (!isJSONSuccess(poJSON)) {
//                        return poJSON;
//                    }
//                }
            }
            if(paRemovedPettyCashLedger.contains(faModel.get(lnCtr))){ 
                paRemovedPettyCashLedger.remove(faModel.get(lnCtr));
            }
        }
        
        computeFields();
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    public JSONObject AddCashFundLedger(List<Model_Cash_Fund_Ledger> faModel){
        poJSON = new JSONObject();
        if(paRemovedCashFundLedger == null){
            paRemovedCashFundLedger = new ArrayList<>();
        }
        if(paCashFundLedger == null){
            paCashFundLedger = new ArrayList<>();
        }
        for(int lnCtr = 0; lnCtr < faModel.size(); lnCtr++){
            if(!paCashFundLedger.contains(faModel.get(lnCtr))){ 
                paCashFundLedger.add(faModel.get(lnCtr));
//                if(faModel.get(lnCtr).getEditMode() != EditMode.UPDATE){
//                    poJSON = paCashFundLedger.get(paCashFundLedger.size()-1).updateRecord();
//                    if (!isJSONSuccess(poJSON)) {
//                        return poJSON;
//                    }
//                }
            }
            if(paRemovedCashFundLedger.contains(faModel.get(lnCtr))){ 
                paRemovedCashFundLedger.remove(faModel.get(lnCtr));
            }
        }
        computeFields();
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    public void computeFields(){
        Double lsTransactionAmount = 0.0000;
        if(Logical.YES.equals(getModel().getFundType())){
            //Get Added cash fund ledger
            for(int lnCtr = 0; lnCtr < getCashFundLedgerListCount(); lnCtr++){
                lsTransactionAmount += CashFundLedgerList(lnCtr).getCreditAmount();
            }

        } else {
            //Get Added petty cash ledger
            for(int lnCtr = 0; lnCtr < getPettyCashLedgerListCount(); lnCtr++){
                lsTransactionAmount += PettyCashLedgerList(lnCtr).getCreditAmount();
            }

        }
        
        getModel().setTransactionAmount(lsTransactionAmount);
    }
    
    /**
    * Returns a readable status of the current Replenishment Request transaction.
    *
    * @return String representing the transaction status (e.g., "OPEN", "ACTIVE", "DEACTIVATED", or "UNKNOWN")
    */
    public String getStatus(String fsStatus){
        switch(fsStatus){
            case ReplenishmentRequestStatus.OPEN:
                return "Open";
            case ReplenishmentRequestStatus.APPROVED:
                return "Approved";
            case ReplenishmentRequestStatus.POSTED:
                return "Posted";
            case ReplenishmentRequestStatus.CANCELLED:
                return "Cancelled";
            case ReplenishmentRequestStatus.VOID:
                return "Voided";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Handles the saving of supplementary data
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
            poJSON = updateLedger(getModel().getTransactionStatus());
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        
        } catch (SQLException | GuanzonException   ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    private JSONObject updateLedger(String fsStatus) throws SQLException, GuanzonException{
        boolean lbIsRemoved;
        switch(fsStatus){
            case ReplenishmentRequestStatus.VOID:
            case ReplenishmentRequestStatus.CANCELLED:
                lbIsRemoved = true;
            break;
            default:
                lbIsRemoved = false;
            break;
        }
        
        if(Logical.YES.equals(getModel().getFundType())){
            System.out.println("--------------------------SAVE ADDED CASH FUND LEDGER---------------------------------------------");
                //Update Added cash fund ledger
                for(int lnCtr = 0; lnCtr < getCashFundLedgerListCount(); lnCtr++){
                    if(CashFundLedgerList(lnCtr).getBatchNo() == null || "".equals(CashFundLedgerList(lnCtr).getBatchNo())){
                        if(CashFundLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                            poJSON = CashFundLedgerList(lnCtr).updateRecord();
                            if (!isJSONSuccess(poJSON)) {
                                return poJSON;
                            }
                        }

                        poJSON = CashFundLedgerList(lnCtr).setBatchNo(getModel().getTransactionNo());
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                    } else {
                        if(lbIsRemoved){
                            if(CashFundLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                                poJSON = CashFundLedgerList(lnCtr).updateRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }

                            poJSON = CashFundLedgerList(lnCtr).setBatchNo(null);
                            if (!isJSONSuccess(poJSON)) {
                                return poJSON;
                            }
                        }
                    }
                    
                    if(CashFundLedgerList(lnCtr).getEditMode() == EditMode.UPDATE){
                        poJSON = CashFundLedgerList(lnCtr).saveRecord();
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                    }
                }

                if(!lbIsRemoved){
                    System.out.println("--------------------------SAVE REMOVED CASH FUND LEDGER---------------------------------------------");
                    //Update Removed cash fund ledger
                    for(int lnCtr = 0; lnCtr < getRemovedCashFundLedgerListCount(); lnCtr++){
                        if(RemovedCashFundLedgerList(lnCtr).getBatchNo() != null && !"".equals(RemovedCashFundLedgerList(lnCtr).getBatchNo())){
                            if(RemovedCashFundLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                                poJSON = RemovedCashFundLedgerList(lnCtr).updateRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }
                            if(RemovedCashFundLedgerList(lnCtr).getEditMode() == EditMode.UPDATE){
                                poJSON = RemovedCashFundLedgerList(lnCtr).setBatchNo(null);
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }

                                poJSON = RemovedCashFundLedgerList(lnCtr).saveRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("--------------------------SAVE ADDED PETTY CASH LEDGER---------------------------------------------");
                //Update Added petty cash ledger
                for(int lnCtr = 0; lnCtr < getPettyCashLedgerListCount(); lnCtr++){
                    if(PettyCashLedgerList(lnCtr).getBatchNo() == null || "".equals(PettyCashLedgerList(lnCtr).getBatchNo())){
                        if(PettyCashLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                            poJSON = PettyCashLedgerList(lnCtr).updateRecord();
                            if (!isJSONSuccess(poJSON)) {
                                return poJSON;
                            }
                        }

                        poJSON = PettyCashLedgerList(lnCtr).setBatchNo(getModel().getTransactionNo());
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                    } else {
                        if(lbIsRemoved){
                            if(PettyCashLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                                poJSON = PettyCashLedgerList(lnCtr).updateRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }

                            poJSON = PettyCashLedgerList(lnCtr).setBatchNo(null);
                            if (!isJSONSuccess(poJSON)) {
                                return poJSON;
                            }
                        }
                    }
                    
                    if(PettyCashLedgerList(lnCtr).getEditMode() == EditMode.UPDATE){
                        poJSON = PettyCashLedgerList(lnCtr).saveRecord();
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                    }
                }

                if(!lbIsRemoved){
                    System.out.println("--------------------------SAVE REMOVED PETTY CASH LEDGER---------------------------------------------");
                    //Update Removed petty cash ledger
                    for(int lnCtr = 0; lnCtr < getRemovedPettyCashLedgerListCount(); lnCtr++){
                        if(RemovedPettyCashLedgerList(lnCtr).getBatchNo() != null && !"".equals(RemovedPettyCashLedgerList(lnCtr).getBatchNo())){
                            if(RemovedPettyCashLedgerList(lnCtr).getEditMode() != EditMode.UPDATE){
                                poJSON = RemovedPettyCashLedgerList(lnCtr).updateRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }
                            if(RemovedPettyCashLedgerList(lnCtr).getEditMode() == EditMode.UPDATE){
                                poJSON = RemovedPettyCashLedgerList(lnCtr).setBatchNo(null);
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }

                                poJSON = RemovedPettyCashLedgerList(lnCtr).saveRecord();
                                if (!isJSONSuccess(poJSON)) {
                                    return poJSON;
                                }
                            }
                        }
                    }
                }

            }
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    
    private JSONObject generatePRF(String fsStatus)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Added validation to generate PRF only if the Status is APPROVED.
        if (!ReplenishmentRequestStatus.APPROVED.equals(fsStatus)) {
            poJSON.put("result", "success");
            return poJSON;
        }

        try {
            System.out.println("poJSON = generatePRF()");
            if (getModel().getTransactionAmount() > 0.0000) {
                PaymentRequest loPaymentRequest = new CashflowControllers(poGRider, null).PaymentRequest();
                poJSON = loPaymentRequest.InitTransaction();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                poJSON = loPaymentRequest.NewTransaction();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                
                String lsPayee = "";
                String lsFund = "";
                String lsIndustryId = "";
                String lsCompanyId = "";
                String lsPayeeName = "";
                if (Logical.YES.equals(getModel().getFundType())) {
                    lsPayee = getModel().CashFund().getCashFundManager();
                    lsFund = "Cash Fund "+ getModel().CashFund().getDescription();
                    lsIndustryId = getModel().CashFund().getIndustryId();
                    lsCompanyId = getModel().CashFund().getCompanyId();
                    lsPayeeName = getModel().CashFund().Custodian().getCompanyName();
                } else {
                    lsPayee = getModel().PettyCash().getPettyManager();
                    lsFund = "Petty Cash"+ getModel().PettyCash().getDescription();
                    lsIndustryId = getModel().PettyCash().getIndustryId();
                    lsCompanyId = getModel().PettyCash().getCompanyId();
                    lsPayeeName = getModel().PettyCash().Custodian().getCompanyName();
                }

                Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
                object.setRecordStatus(RecordStatus.ACTIVE);
                poJSON = object.searchPayee(lsPayee);
                if (isJSONSuccess(poJSON)) {
                    lsPayee = (String) poJSON.get("sPayeeIDx");
                } else {
                    poJSON = setJSON("error", "Payee configuration for custodian <" + lsPayeeName + "> has not been configured.");
                    return poJSON;
                }
                if(lsPayee == null || "".equals(lsPayee)){
                    poJSON = setJSON("error", "Payee configuration for custodian <" + lsPayeeName + "> has not been configured.");
                    return poJSON;
                }
                
                loPaymentRequest.Master().setTransactionDate(SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE)); 
                loPaymentRequest.Master().setBranchCode(poGRider.getBranchCode());
                loPaymentRequest.Master().setDepartmentID(poGRider.getDepartment());
                loPaymentRequest.Master().setRemarks("Replenishment Request for "+lsFund);
                loPaymentRequest.Master().setIndustryID(lsIndustryId);
                loPaymentRequest.Master().setCompanyID(lsCompanyId);
                loPaymentRequest.Master().setSourceCode(ReplenishmentRequestStatus.SourceCode.REPLENISHMENT); 
                loPaymentRequest.Master().setSourceNo(getModel().getTransactionNo());
                loPaymentRequest.Master().setPayeeID(lsPayee); //Master().getSupplierID()
                loPaymentRequest.Master().setEntryNo(1);
                loPaymentRequest.Master().setSeriesNo(loPaymentRequest.getSeriesNoByBranch());
                loPaymentRequest.Master().setTranTotal(getModel().getTransactionAmount());
                loPaymentRequest.Master().setNetTotal(getModel().getTransactionAmount());
                loPaymentRequest.Master().setTransactionStatus(PaymentRequestStatus.CONFIRMED);

                loPaymentRequest.Detail(0).setEntryNo((int) 1);
                loPaymentRequest.Detail(0).setParticularID("");
                loPaymentRequest.Detail(0).setAmount(getModel().getTransactionAmount());
                loPaymentRequest.Detail(0).setDiscount(0.0000);
                loPaymentRequest.Detail(0).setAddDiscount(0.0000);
                loPaymentRequest.Detail(0).setVatable("0");
                loPaymentRequest.Detail(0).setWithHoldingTax(0.0000);
                loPaymentRequest.Detail(0).setPRFRemarks("Replenishment Request for "+lsFund); 
                loPaymentRequest.Detail(0).isReverse(true); 
                loPaymentRequest.AddDetail();
                
                loPaymentRequest.setWithParent(true);
                loPaymentRequest.setWithUI(false);
                poJSON = loPaymentRequest.SaveTransaction();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
            }

        } catch (Exception e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    
    private JSONObject cancelPRF() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException{
        //Get generated PRF
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(new CashflowModels(poGRider).PaymentRequestMaster()),
                " sSourceNo = " + SQLUtil.toSQL(getModel().getTransactionNo())
                + " AND sSourceCd = "  + SQLUtil.toSQL(ReplenishmentRequestStatus.SourceCode.REPLENISHMENT)
                + " AND ( cTranStat != "  + SQLUtil.toSQL(PaymentRequestStatus.VOID)
                + " OR cTranStat != "  + SQLUtil.toSQL(PaymentRequestStatus.CANCELLED)
                + " )" 
            );
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON = setJSON("success", "success");
            return poJSON;
        }
        String lsTransNo = "";
        if (loRS.next()) {
            lsTransNo = loRS.getString("sTransNox");
        }
        MiscUtil.close(loRS);
        
        if(lsTransNo != null && !"".equals(lsTransNo)){
            PaymentRequest loPaymentRequest = new CashflowControllers(poGRider, null).PaymentRequest();
            poJSON = loPaymentRequest.InitTransaction();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            poJSON = loPaymentRequest.OpenTransaction(lsTransNo);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            loPaymentRequest.setWithParent(true);
            poJSON = loPaymentRequest.CancelPRFTransaction("Replenishement Request Cancellation");
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    /**
     * Builds the SQL query used for browsing Replenishment Request records.
     *
     * @return SQL query string with record status condition applied
     */
    @Override
    public String getSQ_Browse() {
        String lsCondition = "";

        if (psRecdStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
            }

            lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(psRecdStat);
        }

        String lsSQL = "SELECT " +
                        "  a.sTransNox, " +
                        "  a.dTransact, " +
                        "  a.cFundType, " +
                        "  a.sFundIdxx, " +
                        "  a.sRemarksx, " +
                        "  a.nTranAmtx, " +
                        "  a.cTranStat, " +
                        "  a.sModified, " +
                        "  a.dModified, " +
                        "  b.sCashFDsc, " +
                        "  c.sPettyDsc," +
                        " CASE WHEN a.cFundType = '1' THEN 'CASH FUND' ELSE 'PETTY CASH' END AS sFundType, " +
                        " CASE WHEN a.cFundType = '1' THEN b.sCashFDsc ELSE c.sPettyDsc END AS sFundDesc, " +
                        "  b.sIndstCdx, " +
                        "  b.sCompnyID, " +
                        "  c.sIndstCdx, " +
                        "  c.sCompnyID " +
//                        "  IF(a.cFundType = '1','CASH FUND', 'PETTY CASH') AS sFundType, " +
//                        "  IF(a.cFundType = '1',b.sIndstCdx, c.sIndstCdx) AS sIndstCdx, " +
//                        "  IF(a.cFundType = '1',b.sCompnyID, c.sCompnyID) AS sCompnyID " +
                        " FROM Replenishment_Request a " +
                        " LEFT JOIN CashFund b ON b.sCashFIDx = a.sFundIdxx " +
                        " LEFT JOIN PettyCash c ON c.sPettyIDx = a.sFundIdxx ";

        return MiscUtil.addCondition(lsSQL, lsCondition);
    }
    
    /**
     * Displays the status history of the current Replenishment Request record.
     * <p>
     * Retrieves status changes, maps internal codes to readable values,
     * fetches the entry user and date, and displays the history via the UI.
     *
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if a system error occurs
     * @throws Exception for other unexpected errors
     */
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            
            switch(crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case ReplenishmentRequestStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case ReplenishmentRequestStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case ReplenishmentRequestStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case ReplenishmentRequestStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case ReplenishmentRequestStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    switch (stat){
                        case "":
                            crs.updateString("cRefrStat", "-");
                            break;
                        case ReplenishmentRequestStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case ReplenishmentRequestStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                        case ReplenishmentRequestStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case ReplenishmentRequestStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case ReplenishmentRequestStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
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
        
        showStatusHistoryUI("Replenishment Request", (String) poModel.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    /**
    * Retrieves information about who created the current Replenishment Request record and when.
    *
    * @return JSONObject containing "sCompnyNm" (user) and "sEntryDte" (date) if successful
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
    */
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM "+ poModel.getTable()+" a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(poModel.getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(poModel.getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
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
            poJSON = setJSON("error", e.getMessage());
            return poJSON;
        } 
        
        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    
    /**
    * Retrieves the company name of a system user based on their user ID.
    *
    * @param fsId the system user ID
    * @return the company name of the user
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a system error occurs
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
            poJSON = setJSON("error", e.getMessage());
        } 
        return lsEntry;
    }
    
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
}
