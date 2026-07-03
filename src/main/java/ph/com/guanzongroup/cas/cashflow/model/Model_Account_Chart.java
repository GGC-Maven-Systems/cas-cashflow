package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 * Model class for Account Chart.
 * 
 * This class represents the Account Chart entity used in the cashflow module.
 * It provides getter and setter methods for all fields and handles related
 * entity loading such as General Ledger, Parent Account, and Industry.
 * 
 * It extends the base {@link Model} class and follows the standard lifecycle
 * including initialization, value setting, and record retrieval.
 * 
 * Relationships:
 * - {@link Model_Transaction_Account_Chart} (General Ledger)
 * - {@link Model_Account_ChartX} (Parent Account)
 * - {@link Model_Industry} (Industry)
 * 
 * Fields managed include:
 * - Account Code
 * - Description
 * - Parent Account
 * - Account Type
 * - Balance Type
 * - Nature
 * - Remarks
 * - Cash Indicator
 * - GL Code
 * - Industry Code
 * - Record Status
 * - Audit Information (Modified By / Date)
 * 
 * @author Teejei De Celis
 * @since 2026-03-28
 */
public class Model_Account_Chart extends Model {

    /** General Ledger model reference */
    Model_Transaction_Account_Chart poGL;

    /** Industry model reference */
    Model_Industry poIndustry;

    /** Parent Account model reference */
    Model_Account_ChartX poAccountParent;
    protected Object oldID;
    protected Object oldID2;
    protected Object oldID3;
    protected Object oldID4;
    protected Object oldID5;
    /**
     * Initializes the model.
     * 
     * Loads metadata, initializes the row set, assigns default values,
     * and prepares related models.
     */
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            // assign default values
            poEntity.updateString("cRecdStat", "0");
            poEntity.updateNull("sContraTo");
            poEntity.updateString("sParentCd", "");
            // end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = "sAcctCode";
            ID2 = "sParentCd";
            ID3 = "sIndstCde";
            oldID = null;
            oldID2 = null;
            oldID3 = null;
            oldID4 = null;
            oldID5 = null;
            CashflowModels model = new CashflowModels(poGRider);
            poGL = model.Transaction_Account_Chart();
            poAccountParent = model.Account_ChartX();

            poIndustry = new ParamModels(poGRider).Industry();

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sets the account code.
     * 
     * @param accountCode account code
     * @return result JSON object
     */
    public JSONObject setAccountCode(String accountCode) {
        return setValue("sAcctCode", accountCode);
    }

    /**
     * Retrieves the account code.
     * 
     * @return account code
     */
    public String getAccountCode() {
        return (String) getValue("sAcctCode");
    }

    /**
     * Sets the account description.
     * 
     * @param description account description
     * @return result JSON object
     */
    public JSONObject setDescription(String description) {
        return setValue("sDescript", description);
    }

    /**
     * Retrieves the account description.
     * 
     * @return account description
     */
    public String getDescription() {
        return (String) getValue("sDescript");
    }

    /**
     * Sets the parent account code.
     * 
     * @param accountCode parent account code
     * @return result JSON object
     */
    public JSONObject setParentAccountCode(String accountCode) {
        return setValue("sParentCd", accountCode);
    }

    /**
     * Retrieves the parent account code.
     * 
     * @return parent account code
     */
    public String getParentAccountCode() {
        return (String) getValue("sParentCd");
    }

    /**
     * Sets the account type.
     * 
     * @param accountType account type
     * @return result JSON object
     */
    public JSONObject setAccountType(String accountType) {
        return setValue("cAcctType", accountType);
    }

    /**
     * Retrieves the account type.
     * 
     * @return account type
     */
    public String getAccountType() {
        return (String) getValue("cAcctType");
    }

    /**
     * Sets the balance type.
     * 
     * @param balanceType balance type
     * @return result JSON object
     */
    public JSONObject setBalanceType(String balanceType) {
        return setValue("cBalTypex", balanceType);
    }

    /**
     * Retrieves the balance type.
     * 
     * @return balance type
     */
    public String getBalanceType() {
        return (String) getValue("cBalTypex");
    }

    /**
     * Sets the contra account reference.
     * 
     * @param contraTo contra account code
     * @return result JSON object
     */
    public JSONObject setContraTo(String contraTo) {
        return setValue("sContraTo", contraTo);
    }

    /**
     * Retrieves the contra account reference.
     * 
     * @return contra account code
     */
    public String getContraTo() {
        return (String) getValue("sContraTo");
    }

    /**
     * Sets the nature of the account.
     * 
     * @param nature account nature
     * @return result JSON object
     */
    public JSONObject setNature(String nature) {
        return setValue("cNaturexx", nature);
    }

    /**
     * Retrieves the nature of the account.
     * 
     * @return account nature
     */
    public String getNature() {
        return (String) getValue("cNaturexx");
    }

    /**
     * Sets remarks.
     * 
     * @param remarks remarks text
     * @return result JSON object
     */
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    /**
     * Retrieves remarks.
     * 
     * @return remarks text
     */
    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    /**
     * Sets whether the account is a cash account.
     * 
     * @param iscash true if cash account, false otherwise
     * @return result JSON object
     */
    public JSONObject isCash(boolean iscash) {
        return setValue("cIsCashxx", iscash ? "1" : "0");
    }

    /**
     * Checks if the account is a cash account.
     * 
     * @return true if cash account, false otherwise
     */
    public boolean isCash() {
        Object value = getValue("cIsCashxx");
        return "1".equals(String.valueOf(value));
    }

    /**
     * Sets the General Ledger code.
     * 
     * @param glCode GL code
     * @return result JSON object
     */
    public JSONObject setGLCode(String glCode) {
        return setValue("sGLCodexx", glCode);
    }

    /**
     * Retrieves the General Ledger code.
     * 
     * @return GL code
     */
    public String getGLCode() {
        return (String) getValue("sGLCodexx");
    }

    /**
     * Sets the industry ID.
     * 
     * @param industryId industry code
     * @return result JSON object
     */
    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCde", industryId);
    }

    /**
     * Retrieves the industry ID.
     * 
     * @return industry code
     */
    public String getIndustryId() {
        return (String) getValue("sIndstCde");
    }

    /**
     * Sets the record status.
     * 
     * @param recordStatus status code
     * @return result JSON object
     */
    public JSONObject setRecordStatus(String recordStatus) {
        return setValue("cRecdStat", recordStatus);
    }

    /**
     * Retrieves the record status.
     * 
     * @return record status
     */
    public String getRecordStatus() {
        return (String) getValue("cRecdStat");
    }

    /**
     * Sets the modifying user ID.
     * 
     * @param modifyingId user ID
     * @return result JSON object
     */
    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    /**
     * Retrieves the modifying user ID.
     * 
     * @return user ID
     */
    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    /**
     * Sets the modified date.
     * 
     * @param modifiedDate modification date
     * @return result JSON object
     */
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    /**
     * Retrieves the modified date.
     * 
     * @return modification date
     */
    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    /**
     * Retrieves the next account code.
     * 
     * @return next code (currently not implemented)
     */
    @Override
    public String getNextCode() {
        return "";
    }

    /**
     * Retrieves the related General Ledger record.
     * 
     * @return Model_Transaction_Account_Chart instance
     * @throws SQLException if database error occurs
     * @throws GuanzonException if business logic fails
     */
    public Model_Transaction_Account_Chart General_Ledger() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sGLCodexx"))) {
            if (poGL.getEditMode() == EditMode.READY
                    && poGL.getGLCode().equals((String) getValue("sGLCodexx"))) {
                return poGL;
            } else {
                poJSON = poGL.openRecord((String) getValue("sGLCodexx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poGL;
                } else {
                    poGL.initialize();
                    return poGL;
                }
            }
        } else {
            poGL.initialize();
            return poGL;
        }
    }

    /**
     * Retrieves the parent account chart record.
     * 
     * @return Model_Account_ChartX instance
     * @throws SQLException if database error occurs
     * @throws GuanzonException if business logic fails
     */
    public Model_Account_ChartX ParentAccountChart() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sParentCd"))) {
            if (poAccountParent.getEditMode() == EditMode.READY
                    && poAccountParent.getAccountCode().equals((String) getValue("sParentCd"))) {
                return poAccountParent;
            } else {
                poJSON = poAccountParent.openRecord((String) getValue("sParentCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poAccountParent;
                } else {
                    poAccountParent.initialize();
                    return poAccountParent;
                }
            }
        } else {
            poAccountParent.initialize();
            return poAccountParent;
        }
    }

    /**
     * Retrieves the related industry record.
     * 
     * @return Model_Industry instance
     * @throws SQLException if database error occurs
     * @throws GuanzonException if business logic fails
     */
    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sIndstCde"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCde"))) {
                return poIndustry;
            } else {
                poJSON = poIndustry.openRecord((String) getValue("sIndstCde"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
    }
    @Override
    public JSONObject saveRecord() throws SQLException, GuanzonException {
        this.poJSON = new JSONObject();
        if (this.pnEditMode != 0 && this.pnEditMode != 2) {
            this.poJSON = new JSONObject();
            this.poJSON.put("result", "error");
            this.poJSON.put("message", "Invalid update mode. Unable to save record.");
            return this.poJSON;
        } else {
            if (this.pnEditMode == 0) {
                if (!this.getNextCode().isEmpty()) {
                    this.setValue(this.ID, this.getNextCode());
                }

                String lsSQL = MiscUtil.makeSQL(this);
                if (!lsSQL.isEmpty()) {
                    if (this.poGRider.executeQuery(lsSQL, this.getTable(), this.poGRider.getBranchCode(), "", "") > 0L) {
                        this.poJSON = new JSONObject();
                        this.poJSON.put("result", "success");
                        this.poJSON.put("message", "Record saved successfully.");
                    } else {
                        this.poJSON = new JSONObject();
                        this.poJSON.put("result", "error");
                        this.poJSON.put("message", this.poGRider.getMessage());
                    }
                } else {
                    this.poJSON = new JSONObject();
                    this.poJSON.put("result", "error");
                    this.poJSON.put("message", "No record to save.");
                }
            } else {
                Model_Account_Chart loOldEntity = new Model_Account_Chart();
                loOldEntity.setApplicationDriver(this.poGRider);
                loOldEntity.setXML(this.XML);
                loOldEntity.setTableName(this.TABLE);
                loOldEntity.initialize();
                if (!this.ID5.isEmpty()) {

                    loOldEntity.ID5 = this.ID5;
                    loOldEntity.ID4 = this.ID4;
                    loOldEntity.ID3 = this.ID3;
                    loOldEntity.ID2 = this.ID2;
                    loOldEntity.ID  = this.ID;

                    this.poJSON = loOldEntity.openRecord(
                            (String) oldID,
                            oldID2,
                            oldID3,
                            oldID4,
                            oldID5);

                } else if (!this.ID4.isEmpty()) {

                    loOldEntity.ID4 = this.ID4;
                    loOldEntity.ID3 = this.ID3;
                    loOldEntity.ID2 = this.ID2;
                    loOldEntity.ID  = this.ID;

                    this.poJSON = loOldEntity.openRecord(
                            (String) oldID,
                            oldID2,
                            oldID3,
                            oldID4);

                } else if (!this.ID3.isEmpty()) {

                    loOldEntity.ID3 = this.ID3;
                    loOldEntity.ID2 = this.ID2;
                    loOldEntity.ID  = this.ID;

                    this.poJSON = loOldEntity.openRecord(
                            (String) oldID,
                            oldID2,
                            oldID3);

                } else if (!this.ID2.isEmpty()) {

                    loOldEntity.ID2 = this.ID2;
                    loOldEntity.ID  = this.ID;

                    this.poJSON = loOldEntity.openRecord(
                            (String) oldID,
                            oldID2);

                } else {

                    loOldEntity.ID = this.ID;

                    this.poJSON = loOldEntity.openRecord(
                            (String) oldID);
                }

                if ("success".equals((String)this.poJSON.get("result"))) {
                    String lsSQL;
                    if (this.ID2.isEmpty()) {

                        lsSQL = MiscUtil.makeSQL(
                                this,
                                loOldEntity,
                                this.ID + " = " + SQLUtil.toSQL(oldID));

                    } else if (this.ID3.isEmpty()) {

                        lsSQL = MiscUtil.makeSQL(
                                this,
                                loOldEntity,
                                this.ID + " = " + SQLUtil.toSQL(oldID)
                                        + " AND "
                                        + this.ID2 + " = " + SQLUtil.toSQL(oldID2));

                    } else if (this.ID4.isEmpty()) {

                        lsSQL = MiscUtil.makeSQL(
                                this,
                                loOldEntity,
                                this.ID + " = " + SQLUtil.toSQL(oldID)
                                        + " AND "
                                        + this.ID2 + " = " + SQLUtil.toSQL(oldID2)
                                        + " AND "
                                        + this.ID3 + " = " + SQLUtil.toSQL(oldID3));

                    } else if (this.ID5.isEmpty()) {

                        lsSQL = MiscUtil.makeSQL(
                                this,
                                loOldEntity,
                                this.ID + " = " + SQLUtil.toSQL(oldID)
                                        + " AND "
                                        + this.ID2 + " = " + SQLUtil.toSQL(oldID2)
                                        + " AND "
                                        + this.ID3 + " = " + SQLUtil.toSQL(oldID3)
                                        + " AND "
                                        + this.ID4 + " = " + SQLUtil.toSQL(oldID4));

                    } else {

                        lsSQL = MiscUtil.makeSQL(
                                this,
                                loOldEntity,
                                this.ID + " = " + SQLUtil.toSQL(oldID)
                                        + " AND "
                                        + this.ID2 + " = " + SQLUtil.toSQL(oldID2)
                                        + " AND "
                                        + this.ID3 + " = " + SQLUtil.toSQL(oldID3)
                                        + " AND "
                                        + this.ID4 + " = " + SQLUtil.toSQL(oldID4)
                                        + " AND "
                                        + this.ID5 + " = " + SQLUtil.toSQL(oldID5));
                    }

                    if (!lsSQL.isEmpty()) {
                        if (this.poGRider.executeQuery(lsSQL, this.getTable(), this.poGRider.getBranchCode(), "", "") > 0L) {
                            this.poJSON = new JSONObject();
                            this.poJSON.put("result", "success");
                            this.poJSON.put("message", "Record saved successfully.");
                        } else {
                            this.poJSON = new JSONObject();
                            this.poJSON.put("result", "error");
                            this.poJSON.put("message", this.poGRider.getMessage());
                        }
                    } else {
                        this.poJSON = new JSONObject();
                        this.poJSON.put("result", "success");
                        this.poJSON.put("message", "No updates has been made.");
                    }
                } else {
                    this.poJSON = new JSONObject();
                    this.poJSON.put("result", "error");
                    this.poJSON.put("message", "Record discrepancy. Unable to save record.");
                }
            }

            return this.poJSON;
        }
    }

    protected void storeOriginalKeys() {
        oldID = getValue(ID);

        if (!ID2.isEmpty()) {
            oldID2 = getValue(ID2);
        }

        if (!ID3.isEmpty()) {
            oldID3 = getValue(ID3);
        }

        if (!ID4.isEmpty()) {
            oldID4 = getValue(ID4);
        }

        if (!ID5.isEmpty()) {
            oldID5 = getValue(ID5);
        }
    }
    @Override
    public JSONObject openRecord(String id) throws SQLException, GuanzonException {
        this.poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, this.ID + " = " + SQLUtil.toSQL(id));
        ResultSet loRS = this.poGRider.executeQuery(lsSQL);

        try {
            if (loRS.next()) {
                for(int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); ++lnCtr) {
                    this.setValue(lnCtr, loRS.getObject(lnCtr));
                }

                MiscUtil.close(loRS);
                this.storeOriginalKeys();
                this.pnEditMode = 1;
                this.poJSON = new JSONObject();
                this.poJSON.put("result", "success");
                this.poJSON.put("message", "Record loaded successfully.");
            } else {
                this.poJSON = new JSONObject();
                this.poJSON.put("result", "error");
                this.poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            this.logError(this.getCurrentMethodName() + "»" + e.getMessage());
            this.poJSON = new JSONObject();
            this.poJSON.put("result", "error");
            this.poJSON.put("message", e.getMessage());
        }

        return this.poJSON;
    }
}