/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckPrintRequestStatus;

/**
 *
 * @author User
 */
public class Model_Check_Printing_Request_Master extends Model {

    Model_Bank_Account_Master poBankAccountMaster;
    Model_Banks poBanks;
    String psCompanyID;
    String psBankAccountID;
    Model_Company poCompany;
    Model_Industry poIndustry;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nTotalAmt", CheckPrintRequestStatus.DefaultValues.default_value_double_0000);
            poEntity.updateObject("cIsUpload", CheckPrintRequestStatus.OPEN);
            poEntity.updateObject("cTranStat", CheckPrintRequestStatus.OPEN);  
            poEntity.updateObject("sModified", poGRider.getUserID());
            poEntity.updateNull("dModified");

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

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setEntryNumber(int entryNumber) {
        return setValue("nEntryNox", entryNumber);
    }

    public int getEntryNumber() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setTotalAmount(double TotalAmount) {
        return setValue("nTotalAmt", TotalAmount);
    }

    public double getTotalAmount() {
        return Double.parseDouble(String.valueOf(getValue("nTotalAmt")));
    }

    public JSONObject isUploaded(boolean isUploaded) {
        return setValue("cIsUpload", isUploaded ? "1" : "0");
    }

    public boolean isUploaded() {
        return ((String) getValue("cIsUpload")).equals("1");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
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

    public void setCompanyID(String companyID) {
        this.psCompanyID = companyID;
    }

    public String getCompanyID() {
        return psCompanyID;
    }

    public void setBankAccountID(String bankAccountID) {
        this.psBankAccountID = bankAccountID;
    }

    public String getBankAccountID() {
        return psBankAccountID;
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
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

    public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        if (!"".equals(psCompanyID)) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals(psCompanyID)) {
                return poCompany;
            } else {
                if (ReferenceCache.tryLoad("Company", psCompanyID, poCompany)) {
                    return poCompany;
                }
                poJSON = poCompany.openRecord(psCompanyID);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Company", psCompanyID, poCompany);
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

    public Model_Industry Industry() throws GuanzonException, SQLException {
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

    public Model_Bank_Account_Master Bank_Account_Master() throws GuanzonException, SQLException {
        if (poBankAccountMaster == null) {
            poBankAccountMaster = new CashflowModels(poGRider).Bank_Account_Master();
        }
        if (!"".equals(psBankAccountID)) {
            if (poBankAccountMaster.getEditMode() == EditMode.READY
                    && poBankAccountMaster.getBankAccountId().equals(psBankAccountID)) {
                return poBankAccountMaster;
            } else {
                poJSON = poBankAccountMaster.openRecord(psBankAccountID);
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
    
}
