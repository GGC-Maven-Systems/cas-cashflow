package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.model.Model_Banks_Branch;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_Bank_Account_Master extends Model {

    Model_Industry poIndustry;
    Model_Branch poBranch;
    Model_Company poCompany;
    Model_Banks poBanks;
    Model_Banks_Branch poBanksBranch;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
//            poEntity.updateNull("nEntryNox");
            poEntity.updateObject("dBegBalxx" , SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateNull("dDueDatex");
            poEntity.updateNull("dLastTran");
            poEntity.updateNull("dLastPost");
            poEntity.updateNull("sAcctCode");
            poEntity.updateObject("sBranchCd", poGRider.getBranchCode());
            poEntity.updateObject("sIndstCdx", poGRider.getIndustry());
            poEntity.updateObject("nOBegBalx", 0.00);
            poEntity.updateObject("nABegBalx", 0.00);
            poEntity.updateObject("nOBalance", 0.00);
            poEntity.updateObject("nABalance", 0.00);
            poEntity.updateObject("cBankPrnt", Logical.NO);
            poEntity.updateObject("cMonitorx", Logical.NO);
            poEntity.updateObject("cDefaultx", Logical.NO);
            poEntity.updateObject("nClearDay", 1);
            poEntity.updateObject("nSgnatory", 1);
            poEntity.updateObject("sSlipType", "DS");
            poEntity.updateObject("cRecdStat", RecordStatus.ACTIVE);
            
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);

            //poIndustry, poBranch, poCompany, poBanks, poBanksBranch are intentionally NOT
            //constructed here - see their accessor methods below, which build them lazily
            //on first access so opening this record never touches those tables.

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

    public JSONObject setBankAccountId(String id) {
        return setValue("sBnkActID", id);
    }

    public String getBankAccountId() {
        return (String) getValue("sBnkActID");
    }

    public JSONObject setIndustryCode(String industryCode) {
        return setValue("sIndstCdx", industryCode);
    }

    public String getIndustryCode() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setCompanyId(String companyId) {
        return setValue("sCompnyID", companyId);
    }

    public String getCompanyId() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setEntryNumber(int entryNumber) {
        return setValue("nEntryNox", entryNumber);
    }

    public String getEntryNumber() {
        return (String) getValue("nEntryNox");
    }

    public JSONObject setBankId(String bankId) {
        return setValue("sBankIDxx", bankId);
    }

    public String getBankId() {
        return (String) getValue("sBankIDxx");
    }

    public JSONObject setAccountNo(String accountNo) {
        return setValue("sActNumbr", accountNo);
    }

    public String getAccountNo() {
        return (String) getValue("sActNumbr");
    }

    public JSONObject setAccountName(String accountName) {
        return setValue("sActNamex", accountName);
    }

    public String getAccountName() {
        return (String) getValue("sActNamex");
    }

    public JSONObject setAccountCode(String code) {
        return setValue("sAcctCode", code);
    }

    public String getAccountCode() {
        return (String) getValue("sAcctCode");
    }

    public JSONObject setAccountType(String type) {
        return setValue("cAcctType", type);
    }

    public String getAccountType() {
        return (String) getValue("cAcctType");
    }

    public JSONObject setPartnerAccount(String parent) {
        return setValue("sPartnrAc", parent);
    }

    public String getPartnerAccountNo() {
        return (String) getValue("sPartnrAc");
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setCheckNo(String remarks) {
        return setValue("sCheckNox", remarks);
    }

    public String getCheckNo() {
        return (String) getValue("sCheckNox");
    }

    public JSONObject setBeginningBalanceDate(Date date) {
        return setValue("dBegBalxx", date);
    }

    public Date getBeginningBalanceDate() {
        return (Date) getValue("dBegBalxx");
    }

    public JSONObject setOutstandingBeginningBalance(double amount) {
        return setValue("nOBegBalx", amount);
    }

    public double getOutstandingBeginningBalance() {
        return Double.parseDouble(String.valueOf(getValue("nOBegBalx")));
    }

    public JSONObject setAccountBeginningBalance(double amount) {
        return setValue("nABegBalx", amount);
    }

    public double getAccountBeginningBalance() {
        return Double.parseDouble(String.valueOf(getValue("nABegBalx")));
    }

    public JSONObject setOutstandingBalance(double amount) {
        return setValue("nOBalance", amount);
    }

    public double getOutstandingBalance() {
        return Double.parseDouble(String.valueOf(getValue("nOBalance")));
    }

    public JSONObject setAccountBalance(double amount) {
        return setValue("nABalance", amount);
    }

    public double getAccountBalance() {
        return Double.parseDouble(String.valueOf(getValue("nABalance")));
    }

    public JSONObject setDueDate(Date date) {
        return setValue("dDueDatex", date);
    }

    public Date getDueDate() {
        return (Date) getValue("dDueDatex");
    }

    public JSONObject setBankPrinting(boolean value) {
        return setValue("cBankPrnt", value ? "1" : "0");
    }

    public boolean isBankPrinting() {
        return ((String) getValue("cBankPrnt")).equals("1");
    }

    public JSONObject setMonitor(boolean value) {
        return setValue("cMonitorx", value ? "1" : "0");
    }

    public boolean isMonitor() {
        return ((String) getValue("cMonitorx")).equals("1");
    }

    public JSONObject setDefault(boolean value) {
        return setValue("cDefaultx", value ? "1" : "0");
    }

    public boolean isDefault() {
        return ((String) getValue("cDefaultx")).equals("1");
    }

    public JSONObject setClearingDays(int days) {
        return setValue("nClearDay", days);
    }

    public int getClearingDays() {
        return (int) getValue("nClearDay");
    }

    public JSONObject setSignatoryCount(int count) {
        return setValue("nSgnatory", count);
    }

    public int getSignatoryCount() {
        return (int) getValue("nSgnatory");
    }

    public JSONObject setLastTransactionDate(Date date) {
        return setValue("dLastTran", date);
    }

    public Date getLastTransactionDate() {
        return (Date) getValue("dLastTran");
    }

    public JSONObject setLastPostingDate(Date date) {
        return setValue("dLastPost", date);
    }

    public Date getLastPostingDate() {
        return (Date) getValue("dLastPost");
    }

    public JSONObject setBranch(String branch) {
        return setValue("sBranchxx", branch);
    }

    public String getBranch() {
        return (String) getValue("sBranchxx");
    }

    public JSONObject setSerialNo(String serial) {
        return setValue("sSerialNo", serial);
    }

    public String getSerialNo() {
        return (String) getValue("sSerialNo");
    }

    public JSONObject setSlipType(String type) {
        return setValue("sSlipType", type);
    }

    public String getSlipType() {
        return (String) getValue("sSlipType");
    }

    public JSONObject setRecordStatus(String status) {
        return setValue("cRecdStat", status);
    }

    public String getRecordStatus() {
        return (String) getValue("cRecdStat");
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

    public Model_Banks Banks() throws SQLException, GuanzonException {
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

    public Model_Banks_Branch BanksBranh() throws SQLException, GuanzonException {
        if (poBanksBranch == null) {
            poBanksBranch = new ParamModels(poGRider).BanksBranch();
        }
        if (!"".equals((String) getValue("sBankIDxx"))) {
            if (poBanksBranch.getEditMode() == EditMode.READY
                    && poBanksBranch.getBankID().equals((String) getValue("sBankIDxx"))) {
                return poBanksBranch;
            } else {
                if (ReferenceCache.tryLoad("Banks_Branches", (String) getValue("sBankIDxx"), poBanksBranch)) {
                    return poBanksBranch;
                }

                poJSON = poBanksBranch.openRecord((String) getValue("sBankIDxx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Banks_Branches", (String) getValue("sBankIDxx"), poBanksBranch);
                    return poBanksBranch;
                } else {
                    poBanksBranch.initialize();
                    return poBanksBranch;
                }
            }
        } else {
            poBanksBranch.initialize();
            return poBanksBranch;
        }
    }

    public Model_Branch Branch() throws SQLException, GuanzonException {
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

    public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        if (!"".equals((String) getValue("sCompnyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompnyID"))) {
                return poCompany;
            } else {
                if (ReferenceCache.tryLoad("Company", (String) getValue("sCompnyID"), poCompany)) {
                    return poCompany;
                }

                poJSON = poCompany.openRecord((String) getValue("sCompnyID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Company", (String) getValue("sCompnyID"), poCompany);
                    return poCompany;
                } else {
                    poCompany.initialize();
                    return poCompany;
                }
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(getTable(), "sBnkActID", true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
}
