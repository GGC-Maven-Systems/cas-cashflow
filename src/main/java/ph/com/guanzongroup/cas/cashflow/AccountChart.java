package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Parameter;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Account_Chart;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 * Controller class for Account Chart maintenance.
 *
 * This class handles business logic, validation, searching, status updates,
 * and audit-related operations for Account Chart records.
 *
 * It extends {@link Parameter} and uses {@link Model_Account_Chart}
 * as its underlying data model.
 *
 * Responsibilities:
 * - Initialize Account Chart model
 * - Validate entries before saving
 * - Search records with various filters
 * - Handle status transitions (Confirm, Deactivate, Void)
 * - Manage approval workflow
 * - Retrieve audit and status history
 *
 * Related Components:
 * - {@link Model_Account_Chart}
 * - {@link CashflowModels}
 * - {@link CashflowControllers}
 * - {@link ParamControllers}
 *
 * Status Codes:
 * - OPEN (0)
 * - DEACTIVATED (1)
 * - CONFIRMED (2)
 * - VOID (3)
 *
 * @author
 */
public class AccountChart extends Parameter {

    Model_Account_Chart poModel;
    /**
     * Initializes the Account Chart controller.
     *
     * Sets default record status filter and initializes the model instance.
     *
     * @throws SQLException if database access fails
     * @throws GuanzonException if initialization fails
     */
    @Override
    public void initialize() throws SQLException, GuanzonException {
        psRecdStat = Logical.YES;

        CashflowModels model = new CashflowModels(poGRider);
        poModel = model.Account_Chart();

        super.initialize();
    }
    /**
     * Validates if the current entry is ready for saving.
     *
     * Performs validation on required fields such as:
     * - Account Code
     * - Description
     * - Industry
     * - GL Code
     * - Account Type
     * - Balance Type
     * - Nature
     *
     * Also sets audit fields such as modifying user and modification date.
     *
     * @return JSONObject containing:
     *         - "success" if validation passes
     *         - "error" with message if validation fails
     * @throws SQLException if database error occurs
     */
    @Override
    public JSONObject isEntryOkay() throws SQLException, GuanzonException {
        poJSON = new JSONObject();

//        if (poGRider.getUserLevel() < UserRight.SYSADMIN) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "User is not allowed to save record.");
//            return poJSON;
//        } 

        if (poModel.getAccountCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Account code must not be empty.");
            return poJSON;
        }

        if (poModel.getDescription() == null || poModel.getDescription().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Description must not be empty.");
            return poJSON;
        }

        if (poModel.getIndustryId()== null || poModel.getIndustryId().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "industry must not be empty.");
            return poJSON;
        }

        if (poModel.getGLCode() == null || poModel.getGLCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "GL code must not be empty.");
            return poJSON;
        }         

        if (poModel.getAccountType() == null || poModel.getAccountType().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Account Type must not be empty.");
            return poJSON;
        }
        

        if (poModel.getBalanceType() == null || poModel.getBalanceType().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Balance Type must not be empty.");
            return poJSON;
        }

        if (poModel.getNature()== null || poModel.getNature().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Account Nature must not be empty.");
            return poJSON;
        }
        

        poModel.setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        poModel.setModifiedDate(poGRider.getServerDate());
        
//        poJSON = checkAccountDuplicate(poModel.getAccountCode(), poModel.getIndustryId());
//        if ("error".equals(poJSON.get("result"))) {
//            return poJSON;
//        }

        poJSON.put("result", "success");
        return poJSON;
    }
    /**
     * Retrieves the current Account Chart model.
     *
     * @return Model_Account_Chart instance
     */
    @Override
    public Model_Account_Chart getModel() {
        return poModel;
    }
    
     /**
     * Searches for an Account Chart record.
     *
     * Displays a search dialog and loads the selected record.
     *
     * @param value search keyword
     * @param byCode true if searching by account code, false for description
     * @return JSONObject result of the search operation
     * @throws SQLException if database error occurs
     * @throws GuanzonException if search fails
     */
    @Override
    public JSONObject searchRecord(String value, boolean byCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Account»Industry",
                "sAcctCode»sDescript»sGLCodexx»xIndustry",
                "a.sAcctCode»a.sDescript»a.sGLCodexx»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sAcctCode"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
     /* - If {@code glCode} is not null, results are filtered by GL code.
    *
    * A search dialog is displayed allowing the user to select a record.
    * Once selected, the corresponding Account Chart record is loaded
    * into the model.
    *
    * @param value search keyword used for lookup (either code or description)
    * @param byCode true if searching by account code, false if by description
    * @param industryCode optional industry code filter (can be null)
    * @param glCode optional GL code filter (can be null)
    * @return JSONObject containing:
    *         - "success" if a record is selected and loaded
    *         - "error" with message if no record is selected
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic or search operation fails
    */
    public JSONObject searchRecord(String value, boolean byCode, String industryCode, String glCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();

        if (industryCode != null) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCde = " + SQLUtil.toSQL(industryCode));
        }

        if (glCode != null) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sGLCodexx = " + SQLUtil.toSQL(glCode));
        }

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Account»Industry",
                "sAcctCode»sDescript»sGLCodexx»xIndustry",
                "a.sAcctCode»a.sDescript»a.sGLCodexx»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sAcctCode"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
     * Searches for an Account Chart record with optional filtering by Industry
     * Code.
     *
     * This method builds a SQL query and applies an additional filter if
     * {@code industryCode} is provided. It then displays a search dialog where
     * the user can select a record.
     *
     * Once a record is selected, the corresponding Account Chart record is
     * loaded into the model.
     *
     * @param value search keyword used for lookup (account code or description)
     * @param byCode true if searching by account code, false if by description
     * @param industryCode optional industry code filter (can be null)
     * @return JSONObject containing: - "success" if a record is selected and
     * loaded - "error" with message if no record is selected
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchRecord(String value, boolean byCode, String industryCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();

        if (industryCode != null) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCde = " + SQLUtil.toSQL(industryCode));
        }

        System.out.print("Executing SQL : " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Account»Industry",
                "sAcctCode»sDescript»sGLCodexx»xIndustry",
                "a.sAcctCode»a.sDescript»a.sGLCodexx»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sAcctCode"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
     * Searches for a Parent Account Chart record.
     *
     * This method builds a SQL query for browsing account records and applies a
     * filter to include only records with a specific record status (e.g.,
     * {@code cRecdStat = '2'}).
     *
     * The filtered results are then displayed in a search dialog where the user
     * can select a parent account record. Once selected, the corresponding
     * record is loaded into the model.
     *
     * @param value search keyword used for lookup (account code or description)
     * @param byCode true if searching by account code, false if by description
     * @return JSONObject containing: - "success" if a record is selected and
     * loaded - "error" with message if no record is selected
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchRecordParent(String value, boolean byCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();
//        List<String> lsFilter = new ArrayList<>();
//
//        lsFilter.add("  a.cRecdStat = '1'");
//        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
//            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
//        }
//        System.out.println("SearchParent : " + lsSQL ) ;

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Account»Industry",
                "sAcctCode»sDescript»sGLCodexx»xIndustry",
                "a.sAcctCode»a.sDescript»a.sGLCodexx»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sAcctCode"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
     * Searches for an Account Chart record filtered by active status and the
     * current user's industry.
     *
     * This method builds a SQL query for browsing account records and applies:
     * - A filter to include only active records ({@code cRecdStat = '1'}) - A
     * filter based on the current user's industry, if available
     *
     * The filtered results are displayed in a search dialog where the user can
     * select a record. Once selected, the corresponding Account Chart record is
     * loaded into the model.
     *
     * @param value search keyword used for lookup (account code or description)
     * @param byCode true if searching by account code, false if by description
     * @return JSONObject containing: - "success" if a record is selected and
     * loaded - "error" with message if no record is selected
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchRecordByIndustry(String value, boolean byCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();
//        List<String> lsFilter = new ArrayList<>();
//
//        lsFilter.add("  a.cRecdStat = '1'");
//
//        if (poGRider.getIndustry() != null) {
//            lsFilter.add(" a.sIndstCde  = " + SQLUtil.toSQL(poGRider.getIndustry()));
//        }
//
//        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
//            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
//        }
//        System.out.println("SearchParent : " + lsSQL ) ;

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Account»Industry",
                "sAcctCode»sDescript»sGLCodexx»xIndustry",
                "a.sAcctCode»a.sDescript»a.sGLCodexx»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sAcctCode"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    /**
     * Builds and returns the base SQL query used for browsing Account Chart
     * records.
     *
     * This query retrieves the following fields: - Account Code - Description -
     * General Ledger (GL) Code - Record Status - Industry Description (via LEFT
     * JOIN)
     *
     * The query joins the {@code Account_Chart} table with the {@code Industry}
     * table to include the industry description.
     *
     * Note: - Record status filtering logic is currently commented out. -
     * Additional conditions may be appended dynamically by calling methods
     * using {@code MiscUtil.addCondition()}.
     *
     * @return base SQL query string for browsing Account Chart records
     */
    @Override
    public String getSQ_Browse() {
        String lsCondition = "";
//
        if (psRecdStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
            }

            lsCondition = "a.cRecdStat IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "a.cRecdStat = " + SQLUtil.toSQL(psRecdStat);
        }

        String lsSQL = "SELECT"
                + "  a.sAcctCode"
                + ", a.sDescript"
                + ", IFNULL(a.sGLCodexx,'') sGLCodexx"
                + ", a.cRecdStat"
                + ", IFNULL(b.sDescript, '') xIndustry"
                + " FROM Account_Chart a"
                + " LEFT JOIN Industry b ON a.sIndstCde = b.sIndstCdx";
        return lsSQL;
//        return MiscUtil.addCondition(lsSQL, lsCondition);
    }
    
    /**
     * Searches for an Industry record and assigns the selected Industry ID to
     * the current Account Chart model.
     *
     * This method initializes an {@code Industry} controller, sets it to
     * retrieve only active records ({@code cRecdStat = '1'}), and performs a
     * search operation based on the provided parameters.
     *
     * If a record is successfully found and selected, the corresponding
     * Industry ID is set in the current model.
     *
     * @param value search keyword used for lookup (industry code or
     * description)
     * @param byCode true if searching by industry code, false if by description
     * @return JSONObject containing: - "success" if a record is selected -
     * "error" if no record is selected or search fails
     * @throws ExceptionInInitializerError if controller initialization fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
           poModel.setIndustryId(object.getModel().getIndustryId());
        }
        return poJSON;
    }
    
    /**
     * Searches for a Parent Account Chart record and assigns the selected
     * parent account code to the current Account Chart model.
     *
     * This method initializes an {@code AccountChart} controller, sets it to
     * retrieve only records with the specified record status
     * ({@code cRecdStat = '2'}), and performs a parent account search.
     *
     * If a record is successfully found and selected, the corresponding parent
     * account code is set in the current model.
     *
     * @param value search keyword used for lookup (account code or description)
     * @param byCode true if searching by account code, false if by description
     * @return JSONObject containing: - "success" if a record is selected -
     * "error" if no record is selected or search fails
     * @throws ExceptionInInitializerError if controller initialization fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchParent(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        AccountChart object = new CashflowControllers(poGRider, logwrapr).AccountChart();
        object.setRecordStatus("1");

        poJSON = object.searchRecordParent(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
           poModel.setParentAccountCode(object.getModel().getAccountCode());
        }
        return poJSON;
    }
    
    /**
     * Searches for a General Ledger (GL) Account Chart record and assigns the
     * selected GL code to the current Account Chart model.
     *
     * This method initializes a {@code TransactionAccountChart} controller,
     * sets it to retrieve only active records ({@code cRecdStat = '1'}), and
     * performs a search operation based on the provided parameters.
     *
     * If a record is successfully found and selected, the corresponding GL code
     * is set in the current model.
     *
     * @param value search keyword used for lookup (GL code or description)
     * @param byCode true if searching by GL code, false if by description
     * @return JSONObject containing: - "success" if a record is selected -
     * "error" if no record is selected or search fails
     * @throws ExceptionInInitializerError if controller initialization fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic or search operation fails
     */
    public JSONObject searchGLCode(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        TransactionAccountChart object = new CashflowControllers(poGRider, logwrapr).TransactionAccountChart();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
           poModel.setGLCode(object.getModel().getGLCode());
        }
        return poJSON;
    }
    
    /**
     * Deactivates the current Account Chart record.
     *
     * This method updates the record status to DEACTIVATED, performs validation
     * checks, requests approval if necessary, and saves changes within a
     * database transaction.
     *
     * The method ensures that: - The record is loaded and in an editable state.
     * - The record is not already deactivated. - All required fields pass
     * validation via {@link #isEntryOkay()}. - Approval is obtained from a
     * higher-level user if required via {@link #seekApproval()}. - The status
     * change is persisted using
     * {@link #statusChange(String, String, String, String, boolean, boolean)}.
     *
     * @param remarks remarks or reason for deactivation
     * @return JSONObject containing: - "success" if the deactivation completes
     * successfully - "error" with a message if the process fails
     * @throws SQLException if a database error occurs during the operation
     * @throws GuanzonException if business logic or validation fails
     * @throws ParseException if date parsing fails
     * @throws CloneNotSupportedException if object cloning fails during
     * processing
     */
    public JSONObject DeactivateRecord(String remarks)
            throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {

        String lsStatus = AccountChart.AccountChartConstant.DEACTIVATED;
        poJSON = new JSONObject();
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY
                || getEditMode() != EditMode.UPDATE) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }

        if (lsStatus.equals(poModel.getRecordStatus())) {
            poJSON.put("error", "Record was already deactivated.");
            return poJSON;
        }

        poJSON = isEntryOkay();
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equals(poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON = statusChange(poModel.getTable(),
                (String) poModel.getValue("sAcctCode"),
                remarks, lsStatus, !lbConfirm, false);

        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "Record successfully deactivated.");
        return poJSON;
    }

    /**
     * Confirms the current Account Chart record.
     *
     * This method updates the record status to CONFIRMED, performs validation
     * checks, requests approval if necessary, and persists the change to the
     * database.
     *
     * The method ensures that: - The record is loaded and in an editable state.
     * - The record is not already confirmed. - All required fields pass
     * validation via {@link #isEntryOkay()}. - Approval is obtained from a
     * higher-level user if required via {@link #seekApproval()}. - The status
     * change is persisted using
     * {@link #statusChange(String, String, String, String, boolean, boolean)}.
     *
     * @param remarks remarks or reason for confirmation
     * @return JSONObject containing: - "success" if the confirmation completes
     * successfully - "error" with a message if the process fails
     * @throws SQLException if a database error occurs during the operation
     * @throws GuanzonException if business logic or validation fails
     * @throws ParseException if date parsing fails
     * @throws CloneNotSupportedException if object cloning fails during
     * processing
     */
    public JSONObject ConfirmRecord(String remarks)
            throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {

        String lsStatus = AccountChart.AccountChartConstant.CONFIRMED;
        poJSON = new JSONObject();
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY
                || getEditMode() != EditMode.UPDATE) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }

        if (lsStatus.equals(poModel.getRecordStatus())) {
            poJSON.put("error", "Record was already confirmed.");
            return poJSON;
        }
        
        poJSON = isEntryOkay();
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equals(poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON = statusChange(poModel.getTable(),
                (String) poModel.getValue("sAcctCode"),
                remarks, lsStatus, !lbConfirm, false);

        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record successfully confirm.");
        return poJSON;
    }

    /**
     * Voids the current Account Chart record.
     *
     * This method updates the record status to VOID, performs validation
     * checks, requests approval if required, and persists the change to the
     * database.
     *
     * The method ensures that: - The record is loaded and in an editable state.
     * - The record is not already voided. - All required fields pass validation
     * via {@link #isEntryOkay()}. - Approval is obtained from a higher-level
     * user if required via {@link #seekApproval()}. - The status change is
     * persisted using
     * {@link #statusChange(String, String, String, String, boolean, boolean)}.
     *
     * @param remarks remarks or reason for voiding
     * @return JSONObject containing: - "success" if the voiding completes
     * successfully - "error" with a message if the process fails
     * @throws SQLException if a database error occurs during the operation
     * @throws GuanzonException if business logic or validation fails
     * @throws ParseException if date parsing fails
     * @throws CloneNotSupportedException if object cloning fails during
     * processing
     */
    public JSONObject VoidRecord(String remarks)
            throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {

        String lsStatus = AccountChart.AccountChartConstant.VOID;
        poJSON = new JSONObject();
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY
                || getEditMode() != EditMode.UPDATE) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }

        if (lsStatus.equals(poModel.getRecordStatus())) {
            poJSON.put("error", "Record was already voided.");
            return poJSON;
        }

        poJSON = isEntryOkay();
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poJSON = seekApproval();
            if ("error".equals(poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON = statusChange(poModel.getTable(),
                (String) poModel.getValue("sAcctCode"),
                remarks, lsStatus, !lbConfirm, false);

        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record successfully void.");
        return poJSON;
    }

    /**
     * Requests approval from a higher-level user if the current user is not
     * authorized to approve the operation.
     *
     * Logic: 1. Checks if the current user's level is ENCODER or below. 2. If
     * so, it opens a dialog to get approval from a higher-level user. 3.
     * Validates that the approving user has a level above ENCODER.
     *
     * @return JSONObject with: - "success" if approval is granted or not
     * required - "error" with a message if approval is denied or user is not
     * authorized
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if approval process fails
     */
    public JSONObject seekApproval() throws SQLException, GuanzonException {

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);

            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (Integer.parseInt(poJSON.get("nUserLevl").toString())
                    <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message",
                        "User is not an authorized approving officer..");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    /**
     * Constants representing Account Chart record statuses.
     */
    public static class AccountChartConstant {

        /**
         * Open/Active status
         */
        public static final String OPEN = "0";

        /**
         * Deactivated status
         */
        public static final String DEACTIVATED = "2";

        /**
         * Confirmed status
         */
        public static final String CONFIRMED = "1";

        /**
         * Void status
         */
        public static final String VOID = "3";

    }
    
    /**
     * Displays the status history of the current Account Chart record.
     *
     * This method performs the following steps:
     * <ol>
     * <li>Retrieves the record's status history via
     * {@link #getStatusHistory()}.</li>
     * <li>Iterates through each status record and maps its code to a
     * human-readable string (e.g., OPEN, DEACTIVATED, CONFIRMED, VOID).</li>
     * <li>Handles legacy or empty status values by providing appropriate
     * defaults.</li>
     * <li>Retrieves entry information (entered by and entry date) using
     * {@link #getEntryBy()}.</li>
     * <li>Displays the status history in the UI using
     * {@link #showStatusHistoryUI(String, String, String, String, CachedRowSet)}.</li>
     * </ol>
     *
     * @throws SQLException if a database access error occurs while retrieving
     * status history.
     * @throws GuanzonException if a business logic error occurs.
     * @throws Exception if any other error occurs during processing or UI
     * display.
     */
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case AccountChartConstant.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case AccountChartConstant.DEACTIVATED:
                    crs.updateString("cRefrStat", "DEACTIVATED");
                    break;
                case AccountChartConstant.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case AccountChartConstant.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case AccountChartConstant.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case AccountChartConstant.DEACTIVATED:
                            crs.updateString("cRefrStat", "DEACTIVATED");
                            break;
                        case AccountChartConstant.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case AccountChartConstant.VOID:
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
        
        showStatusHistoryUI("Account Chart", (String) poModel.getValue("sAcctCode"), entryBy, entryDate, crs);
    }
    
    /**
     * Retrieves the user and timestamp of the entry (creation) for the current
     * Account Chart record.
     *
     * This method performs the following:
     * <ul>
     * <li>Builds a SQL query to fetch the user who created the record and the
     * corresponding modification date from the audit log table
     * {@code xxxAuditLogMaster}.</li>
     * <li>Decrypts the username if necessary using {@link #getSysUser(String)}
     * and {@link poGRider#Decrypt(String)}.</li>
     * <li>Formats the modification timestamp into {@code MM-dd-yyyy HH:mm:ss}
     * format.</li>
     * <li>Returns the result as a {@link JSONObject} containing the user and
     * entry date.</li>
     * </ul>
     *
     * @return a {@link JSONObject} with the following keys:
     * <ul>
     * <li>{@code result} – "success" if entry is found, "error" otherwise</li>
     * <li>{@code sCompnyNm} – the username of the person who entered the
     * record</li>
     * <li>{@code sEntryDte} – the formatted entry date</li>
     * </ul>
     *
     * @throws SQLException if a database access error occurs.
     * @throws GuanzonException if a business logic error occurs during
     * processing.
     */
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM Account_Chart a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sAcctCode AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(poModel.getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sAcctCode =  " + SQLUtil.toSQL(poModel.getAccountCode())) ;
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
     * Retrieves the company name of a user based on the provided user ID.
     *
     * This method queries the {@code xxxSysUser} table and joins with
     * {@code Client_Master} to obtain the company name of the employee
     * associated with the given user ID.
     *
     * @param fsId the user ID to look up
     * @return the company name of the user, or an empty string if not found
     * @throws SQLException if a database access error occurs
     * @throws GuanzonException if a business logic error occurs during
     * processing
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
    /**
     * Retrieves information about who confirmed the current Account Chart
     * record and when.
     *
     * This method queries the {@code Parameter_Status_History} table to find
     * the user who set the record status to CONFIRMED. It returns the user’s
     * company name and the timestamp of confirmation.
     *
     * @return a JSONObject containing:
     * <ul>
     * <li>{@code result} - "success" if the record was found, "error"
     * otherwise</li>
     * <li>{@code sConfirmed} - the company name of the user who confirmed the
     * record</li>
     * <li>{@code sConfrmDte} - the confirmation date and time in MM-dd-yyyy
     * HH:mm:ss format</li>
     * </ul>
     * @throws SQLException if a database access error occurs
     * @throws GuanzonException if a business logic error occurs during
     * processing
     */
    public JSONObject getConfirmedBy() throws SQLException, GuanzonException {
        String lsConfirm = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM Account_Chart a "
                     + " LEFT JOIN Parameter_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = 'Account_Chart' "
                     + " AND ( b.cRefrStat = "+ SQLUtil.toSQL(AccountChartConstant.CONFIRMED) 
                     + " OR (ASCII(b.cRefrStat) - 64)  = "+ SQLUtil.toSQL(AccountChartConstant.CONFIRMED) + " )";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(poModel.getAccountCode())) ;
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsConfirm = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsConfirm = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsDate = dModified.format(formatter);
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
        poJSON.put("sConfirmed", lsConfirm);
        poJSON.put("sConfrmDte", lsDate);
        return poJSON;
    }
    
    /**
    * Checks if an account combination already exists in the Account_Chart
    * table.
    *
    * <p>
    * This method validates the uniqueness of the combination of
    * <code>sAcctCode</code> (Account Code) and <code>sIndstCde</code>
    * (Industry Code) for active records (cRecdStat = '1'). It returns a
    * {@link JSONObject} indicating whether a duplicate exists or not.
    * </p>
    *
    * <p>
    * Validation rules:
    * <ul>
    * <li>The account combination (sAcctCode + sIndstCde) must be unique.</li>
    * <li>Only active records (cRecdStat = '1') are considered in the duplicate
    * check.</li>
    * </ul>
    * </p>
    *
    * @param fsAcctCode the Account Code to check for duplicates; must not be
    * null or empty
    * @param fsIndstCde the Industry Code to check for duplicates; must not be
    * null or empty
    * @return a {@link JSONObject} containing:
    * <ul>
    * <li><b>result</b>: "success" if no duplicate is found, "error" if a
    * duplicate exists</li>
    * <li><b>message</b>: description of the validation outcome</li>
    * </ul>
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if any business logic or execution error occurs
    */
    public JSONObject checkAccountDuplicate(String fsAcctCode, String fsIndstCde) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();

        String lsSQL = "SELECT sAcctCode, sIndstCde, cRecdStat "
                + "FROM Account_Chart "
                + "WHERE sAcctCode = " + SQLUtil.toSQL(fsAcctCode)
                + " AND sIndstCde = " + SQLUtil.toSQL(fsIndstCde)
                + " AND cRecdStat IN ('0','1')";

        System.out.println("EXECUTING SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS.next()) {
            loJSON.put("result", "error");
            loJSON.put("message", "This account combination already exists.\n"
                    + "Account Code: " + loRS.getString("sAcctCode")
                    + ", Industry Code: " + loRS.getString("sIndstCde"));
        } else {
            loJSON.put("result", "success");
            loJSON.put("message", "No duplicate found. You can proceed.");
        }

        loRS.close();
        return loJSON;
    }




}
