/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.agent.systables.Model_Transaction_Status_History;
import org.guanzon.appdriver.agent.systables.TransactionStatusHistory;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.CheckPayments;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;

/**
 *
 * @author User
 */
public class Model_Check_Payments extends Model {

    Model_Client_Master poSupplier;
    Model_Payee poPayee;
    Model_Bank_Account_Master poBankAccountMaster;
    Model_Branch poBranch;
    Model_Banks poBanks;
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Transaction_Status_History poTransactionStatusHistory;
    private String compnyID;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dCheckDte", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateNull("dCheckDte");
            poEntity.updateObject("nAmountxx", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateString("cTranStat", DisbursementStatic.OPEN);
            poEntity.updateString("cProcessd", DisbursementStatic.OPEN);
            poEntity.updateString("cLocation", DisbursementStatic.OPEN);
            poEntity.updateString("cReleased", DisbursementStatic.OPEN);
            poEntity.updateString("cPrintxxx", CheckStatus.PrintStatus.OPEN);
            poEntity.updateNull("dPrintxxx");
            poEntity.updateObject("dModified", poGRider.getServerDate());

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            ID = "sTransNox";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setIndustryID(String industryID) {
        return setValue("sIndstCdx", industryID);
    }

    public String getIndustryID() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setBankID(String bankID) {
        return setValue("sBankIDxx", bankID);
    }

    public String getBankID() {
        return (String) getValue("sBankIDxx");
    }

    public JSONObject setBankAcountID(String bankAccountID) {
        return setValue("sBnkActID", bankAccountID);
    }

    public String getBankAcountID() {
        return (String) getValue("sBnkActID");
    }

    public JSONObject setCheckNo(String checkNo) {
        return setValue("sCheckNox", checkNo);
    }

    public String getCheckNo() {
        return (String) getValue("sCheckNox");
    }
    
    public JSONObject setCheckDate(Date checkDate) {
        if(checkDate == null){
            try {
                poEntity.updateNull("dCheckDte");
                return null;
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        return setValue("dCheckDte", checkDate);
    }

    public Date getCheckDate() {
        return (Date) getValue("dCheckDte");
    }

    public JSONObject setPayorID(String payorID) {
        return setValue("sPayorIDx", payorID);
    }

    public String getPayorID() {
        return (String) getValue("sPayorIDx");
    }

    public JSONObject setPayeeID(String payeeID) {
        return setValue("sPayeeIDx", payeeID);
    }

    public String getPayeeID() {
        return (String) getValue("sPayeeIDx");
    }

    public JSONObject setAmount(double amount) {
        return setValue("nAmountxx", amount);
    }

    public double getAmount() {
        return Double.parseDouble(String.valueOf(getValue("nAmountxx")));
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setLocation(String location) {
        return setValue("cLocation", location);
    }

    public String getLocation() {
        return (String) getValue("cLocation");
    }

    public JSONObject isReplaced(boolean replaced) {
        return setValue("cIsReplcd", replaced ? "1" : "0");
    }

    public boolean isReplaced() {
        Object value = getValue("cIsReplcd");
        return "1".equals(String.valueOf(value));
    }

    public JSONObject setReleased(String released) {
        return setValue("cReleased", released);
    }

    public String getReleased() {
        return (String) getValue("cReleased");
    }

    public JSONObject setPayeeType(String payeeType) {
        return setValue("cPayeeTyp", payeeType);
    }

    public String getPayeeType() {
        return (String) getValue("cPayeeTyp");
    }

    public JSONObject setDesbursementMode(String desbursementMode) {
        return setValue("cDisbMode", desbursementMode);
    }

    public String getDesbursementMode() {
        return (String) getValue("cDisbMode");
    }

    public JSONObject setClaimant(String claimant) {
        return setValue("cClaimant", claimant);
    }

    public String getClaimant() {
        return (String) getValue("cClaimant");
    }

    public JSONObject setAuthorize(String authorize) {
        return setValue("sAuthorze", authorize);
    }

    public String getAuthorize() {
        return (String) getValue("sAuthorze");
    }

    public JSONObject isCross(boolean iscross) {
        return setValue("cIsCrossx", iscross ? "1" : "0");
    }

    public boolean isCross() {
        Object value = getValue("cIsCrossx");
        return "1".equals(String.valueOf(value));
    }

    public JSONObject isPayee(boolean ispayee) {
        return setValue("cIsPayeex", ispayee ? "1" : "0");
    }

    public boolean isPayee() {
        Object value = getValue("cIsPayeex");
        return "1".equals(String.valueOf(value));
    }

//    cIsPayeex to verify
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    public JSONObject setProcessed(String processed) {
        return setValue("cProcessd", processed);
    }

    public String getProcessed() {
        return (String) getValue("cProcessd");
    }

    public JSONObject setPrint(String print) {
        return setValue("cPrintxxx", print);
    }

    public String getPrint() {
        return (String) getValue("cPrintxxx");
    }

    public JSONObject setDatePrint(Date datePrint) {
        return setValue("dPrintxxx", datePrint);
    }

    public Date getDatePrint() {
        return (Date) getValue("dPrintxxx");
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
    
    public String setCompany(String bankidx) {
        return compnyID = bankidx;
    }

    public String getCompany() {
        return compnyID;
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    public Model_Payee Payee() throws GuanzonException, SQLException {
        if (poPayee == null) {
            poPayee = new CashflowModels(poGRider).Payee();
        }
        if (!"".equals((String) getValue("sPayeeIDx"))) {
            if (poPayee.getEditMode() == EditMode.READY
                    && poPayee.getPayeeID().equals((String) getValue("sPayeeIDx"))) {
                return poPayee;
            } else {
                poJSON = poPayee.openRecord((String) getValue("sPayeeIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poPayee;
                } else {
                    poPayee.initialize();
                    return poPayee;
                }
            }
        } else {
            poPayee.initialize();
            return poPayee;
        }
    }

    public Model_Client_Master Supplier() throws GuanzonException, SQLException {
        if (poSupplier == null) {
            poSupplier = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals((String) getValue("sSupplier"))) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals((String) getValue("sSupplier"))) {
                return poSupplier;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sSupplier"), poSupplier)) {
                    return poSupplier;
                }
                poJSON = poSupplier.openRecord((String) getValue("sSupplier"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sSupplier"), poSupplier);
                    return poSupplier;
                } else {
                    poSupplier.initialize();
                    return poSupplier;
                }
            }
        } else {
            poSupplier.initialize();
            return poSupplier;
        }
    }

    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", (String) getValue("sBranchCd"), poBranch)) {
                    return poBranch;
                }
                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", (String) getValue("sBranchCd"), poBranch);
                    return poBranch;
                } else {
                    poBranch.initialize();
                    return poBranch;
                }
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
    }

    public Model_Banks Banks() throws GuanzonException, SQLException {
        if (poBanks == null) {
            poBanks = new ParamModels(poGRider).Banks();
        }
        if (!"".equals((String) getValue("sBankIDxx"))) {
            if (poBanks.getEditMode() == EditMode.READY
                    && poBanks.getBankID().equals((String) getValue("sBankIDxx"))) {
                return poBanks;
            } else {
                if (ReferenceCache.tryLoad("Banks", (String) getValue("sBankIDxx"), poBanks)) {
                    return poBanks;
                }
                poJSON = poBanks.openRecord((String) getValue("sBankIDxx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Banks", (String) getValue("sBankIDxx"), poBanks);
                    return poBanks;
                } else {
                    poBanks.initialize();
                    return poBanks;
                }
            }
        } else {
            poBanks.initialize();
            return poBanks;
        }
    }

    public Model_Bank_Account_Master Bank_Account_Master() throws GuanzonException, SQLException {
        if (poBankAccountMaster == null) {
            poBankAccountMaster = new CashflowModels(poGRider).Bank_Account_Master();
        }
        if (!"".equals((String) getValue("sBnkActID"))) {
            if (poBankAccountMaster.getEditMode() == EditMode.READY
                    && poBankAccountMaster.getBankAccountId().equals((String) getValue("sBnkActID"))) {
                return poBankAccountMaster;
            } else {
                poJSON = poBankAccountMaster.openRecord((String) getValue("sBnkActID"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poBankAccountMaster;
                } else {
                    poBankAccountMaster.initialize();
                    return poBankAccountMaster;
                }
            }
        } else {
            poBankAccountMaster.initialize();
            return poBankAccountMaster;
        }
    }
    
   public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        if (!"".equals(compnyID)) {
            if (this.poCompany.getEditMode() == 1 && this.poCompany
                    .getCompanyId().equals(compnyID)) {
                return this.poCompany;
            }
            if (ReferenceCache.tryLoad("Company", compnyID, poCompany)) {
                return this.poCompany;
            }
            this.poJSON = this.poCompany.openRecord(this.getCompany());
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Company", compnyID, poCompany);
                return this.poCompany;
            }
            this.poCompany.initialize();
            return this.poCompany;
        }
        this.poCompany.initialize();
        return this.poCompany;
    }

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                if (ReferenceCache.tryLoad("Industry", (String) getValue("sIndstCdx"), poIndustry)) {
                    return poIndustry;
                }
                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Industry", (String) getValue("sIndstCdx"), poIndustry);
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

    public Model_Transaction_Status_History TransactionStatusHistory() throws SQLException, GuanzonException {
        if (poTransactionStatusHistory == null) {
            poTransactionStatusHistory = new Model_Transaction_Status_History();
            poTransactionStatusHistory.setApplicationDriver(poGRider);
            poTransactionStatusHistory.setXML("Model_Transaction_Status_History");
            poTransactionStatusHistory.setTableName("Transaction_Status_History");
            poTransactionStatusHistory.initialize();
        }
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poTransactionStatusHistory.getEditMode() == EditMode.READY
                    && poTransactionStatusHistory.getSourceNo().equals((String) getValue("sTransNox"))) {
                return poTransactionStatusHistory;
            } else {
                poJSON = poTransactionStatusHistory.openRecord((String) getValue("sTransNox"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poTransactionStatusHistory;
                } else {
                    poTransactionStatusHistory.initialize();
                    return poTransactionStatusHistory;
                }
            }
        } else {
            poTransactionStatusHistory.initialize();
            return poTransactionStatusHistory;
        }
    }

    public JSONObject openRecordbySourceNo(String ssourceNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sSourceNo = " + SQLUtil.toSQL(ssourceNo)
                                                + " AND cTranStat != " + SQLUtil.toSQL(CheckStatus.CANCELLED)
                                                + " AND cTranStat != " + SQLUtil.toSQL(CheckStatus.VOID));

//        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
                    setValue(lnCtr, loRS.getObject(lnCtr));
                }
                MiscUtil.close(loRS);
                pnEditMode = EditMode.READY;
                poJSON = new JSONObject();
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }
    
    public JSONObject openRecordbyTranaction(String checkTrans)
            throws SQLException, GuanzonException {

        JSONObject json = new JSONObject();

        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(
                lsSQL,
                " sTransNox = " + SQLUtil.toSQL(checkTrans)
        );

        ResultSet loRS = null;

        try {
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS.next()) {

                JSONObject data = new JSONObject();

                for (int lnCtr = 1;
                        lnCtr <= loRS.getMetaData().getColumnCount();
                        lnCtr++) {

                    String column = loRS.getMetaData().getColumnLabel(lnCtr);
                    Object value = loRS.getObject(lnCtr);

                    data.put(column, value);

                    // still load model if needed
                    setValue(lnCtr, value);
                }

                pnEditMode = EditMode.READY;

                json.put("result", "success");
                json.put("message", "Record loaded successfully.");
                json.put("data", data);

            } else {

                json.put("result", "error");
                json.put("message", "No record to load.");
            }

        } catch (SQLException e) {

            json.put("result", "error");
            json.put("message", e.getMessage());

        } finally {
            MiscUtil.close(loRS);
        }

        return json;
    }
    
}
