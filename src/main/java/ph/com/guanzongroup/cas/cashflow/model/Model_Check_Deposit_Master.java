package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.client.model.Model_AP_Client_Bank_Account;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckDepositStatus;

/**
 *
 * @author maynevval 08-09-2025
 */
public class Model_Check_Deposit_Master extends Model {

    //reference objects
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Bank_Account_Master poBankAccount;
    Model_AP_Client_Bank_Account poAPCBankAccount;
    Model_Branch poBranch;
    Model_Banks poBanks;
    private String bankid = "";
    private String compnyID;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("dTransact", poGRider.getServerDate());
            poEntity.updateObject("dReferDte", poGRider.getServerDate());
            poEntity.updateDouble("nTotalDep", 0.00d);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateString("cPrintedx", "0");
            poEntity.updateString("nClearing", "0");    
            poEntity.updateObject("sModified", poGRider.getUserID());
            poEntity.updateNull("dModified");
            poEntity.updateString("cTranStat", CheckDepositStatus.OPEN);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);

            //add model here
            pnEditMode = EditMode.UNKNOWN;

        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
        /**
     * Converts a Date to a string in yyyy-MM-dd format.
     * @param fdValue Date value
     * @return formatted date string
     */
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(fdValue);
    }


    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //sIndstCdx
    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

//    //sBranchCd
//    public JSONObject setBranchCode(String branchCode) {
//        return setValue("sBranchCd", branchCode);
//    }
//
//    public String getBranchCode() {
//        return (String) getValue("sBranchCd");
//    }
    //dTransact
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    //dReferDte
    public JSONObject setTransactionReferDate(Date transactionDate) {
        return setValue("dReferDte", transactionDate);
    }

    public Date getTransactionReferDate() {
        return (Date) getValue("dReferDte");
    }

    //sBnkActID
    public JSONObject setBankAccount(String bankAccount) {
        return setValue("sBnkActID", bankAccount);
    }

    public String getBankAccount() {
        return (String) getValue("sBnkActID");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //nTotalDep
    public JSONObject setTransactionTotalDeposit(Double transactionTotal) {
        return setValue("nTotalDep", transactionTotal);
    }

    public Double getTransactionTotalDeposit() {
        return Double.valueOf(getValue("nTotalDep").toString());
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    //nClearing
    public JSONObject setClearing(String clearStatus) {
        return setValue("nClearing", clearStatus);
    }

    public String getClearing() {
        return (String) getValue("nClearing");
    }

    public boolean isClear() {
        return RecordStatus.ACTIVE.equals(getValue("nClearing"));
    }

    //cTranStat
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    //cPrintedx
    public JSONObject setPrintStatus(String printStatus) {
        return setValue("cPrintedx", printStatus);
    }

    public String getPrintStatus() {
        return (String) getValue("cPrintedx");
    }

    public boolean isPrintedStatus() {
        return RecordStatus.ACTIVE.equals(getValue("cPrintedx"));
    }

    //sModified
    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }
    
    
    public String setBanks(String bankidx) {
        return bankid = bankidx;
    }

    public String getBanks() {
        return bankid;
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

    public Model_Bank_Account_Master BankAccount() throws SQLException, GuanzonException {
        if (poBankAccount == null) {
            poBankAccount = new CashflowModels(poGRider).Bank_Account_Master();
        }
        if (!"".equals(getValue("sBnkActID"))) {
            if (this.poBankAccount.getEditMode() == 1 && this.poBankAccount
                    .getBankAccountId().equals(getValue("sBnkActID"))) {
                return this.poBankAccount;
            }
            this.poJSON = this.poBankAccount.openRecord((String) getValue("sBnkActID"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poBankAccount;
            }
            this.poBankAccount.initialize();
            return this.poBankAccount;
        }
        this.poBankAccount.initialize();
        return this.poBankAccount;
    }

    public Model_AP_Client_Bank_Account APClientBankAccount() throws SQLException, GuanzonException {
        if (poAPCBankAccount == null) {
            poAPCBankAccount = new Model_AP_Client_Bank_Account();
            poAPCBankAccount.setApplicationDriver(poGRider);
            poAPCBankAccount.setXML("Model_AP_Client_Bank_Account");
            poAPCBankAccount.setTableName("AP_Client_Bank_Account");
            poAPCBankAccount.initialize();
        }
        if (!"".equals(getValue("sBnkActID"))) {
            if (this.poAPCBankAccount.getEditMode() == 1 && this.poAPCBankAccount
                    .getAPClientBankID().equals(getValue("sBnkActID"))) {
                return this.poAPCBankAccount;
            }
            this.poJSON = this.poAPCBankAccount.openRecord((String) getValue("sBnkActID"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poAPCBankAccount;
            }
            this.poAPCBankAccount.initialize();
            return this.poAPCBankAccount;
        }
        this.poAPCBankAccount.initialize();
        return this.poAPCBankAccount;
    }
    
    public Model_Banks Banks() throws GuanzonException, SQLException {
        if (poBanks == null) {
            poBanks = new ParamModels(poGRider).Banks();
        }
        if (!"".equals(getValue("sBnkActID"))) {
            bankid = BankAccount().getBankId();
            if (bankid == null || "".equals(bankid)) {
                bankid = APClientBankAccount().getBankID();
            }
        }
        if (!"".equals(bankid)) {
            if (poBanks.getEditMode() == EditMode.READY
                    && poBanks.getBankID().equals(bankid)) {
                return poBanks;
            } else {
                if (ReferenceCache.tryLoad("Banks", bankid, poBanks)) {
                    return poBanks;
                }
                poJSON = poBanks.openRecord(bankid);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Banks", bankid, poBanks);
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

    public Model_Branch Branch() throws SQLException, GuanzonException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        if (!"".equals(getValue("sTransNox"))) {
            if (this.poBranch.getEditMode() == 1 && this.poBranch
                    .getBranchCode().equals(getValue("sTransNox").toString().substring(0, 4))) {
                return this.poBranch;
            }
            if (ReferenceCache.tryLoad("Branch", getValue("sTransNox").toString().substring(0, 4), poBranch)) {
                return this.poBranch;
            }
            this.poJSON = this.poBranch.openRecord(getValue("sTransNox").toString().substring(0, 4));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Branch", getValue("sTransNox").toString().substring(0, 4), poBranch);
                return this.poBranch;
            }
            this.poBranch.initialize();
            return this.poBranch;
        }
        this.poBranch.initialize();
        return this.poBranch;
    }



    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        if (!"".equals(getValue("sIndstCdx"))) {
            if (this.poIndustry.getEditMode() == 1 && this.poIndustry
                    .getIndustryId().equals(getValue("sIndstCdx"))) {
                return this.poIndustry;
            }
            if (ReferenceCache.tryLoad("Industry", (String) getValue("sIndstCdx"), poIndustry)) {
                return this.poIndustry;
            }
            this.poJSON = this.poIndustry.openRecord((String) getValue("sIndstCdx"));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Industry", (String) getValue("sIndstCdx"), poIndustry);
                return this.poIndustry;
            }
            this.poIndustry.initialize();
            return this.poIndustry;
        }
        this.poIndustry.initialize();
        return this.poIndustry;
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

}
