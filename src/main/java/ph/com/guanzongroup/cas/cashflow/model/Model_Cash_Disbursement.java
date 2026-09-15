/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.util.Date;
import java.sql.SQLException;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CashAdvanceStatus;
import ph.com.guanzongroup.cas.cashflow.status.CashDisbursementStatus;

/**
 *
 * @author Arsiela 03-24-2026
 */
public class Model_Cash_Disbursement extends Model {

    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Department poDepartment;
    Model_Cash_Fund poCashFund;
    Model_Client_Master poClient;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            //assign default values
            poEntity.updateNull("dTransact");
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nVATSales", 0.0000);
            poEntity.updateObject("nVATAmtxx", 0.0000);
            poEntity.updateObject("nZroVATSl", 0.0000);
            poEntity.updateObject("nVatExmpt", 0.0000);
            poEntity.updateObject("nWTaxTotl", 0.0000);
            poEntity.updateObject("nNetTotal", 0.0000);
            poEntity.updateString("cTranStat", CashDisbursementStatus.OPEN);
            poEntity.updateString("cCollectd", Logical.NO);
            poEntity.updateString("cVchrPrnt", Logical.NO);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

            //poBranch, poIndustry, poCompany, poDepartment, poClient, poCashFund are
            //intentionally NOT constructed here - see their accessor methods below, which
            //build them lazily on first access so opening this record never touches those
            //tables.
//            poCreditToOthers = gl.Payee();
//            poPettyCash = gl.PettyCashMaster();

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(String entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public String getEntryNo() {
        return (String) getValue("nEntryNox");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setVoucherNo(String voucherNo) {
        return setValue("sVoucherx", voucherNo);
    }

    public String getVoucherNo() {
        return (String) getValue("sVoucherx");
    }

    public JSONObject setCompanyId(String companyId) {
        return setValue("sCompnyID", companyId);
    }

    public String getCompanyId() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setIndustryId(String industryCode) {
        return setValue("sIndstCdx", industryCode);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setPettyId(String pettyId) {
        return setValue("sPettyIDx", pettyId);
    }

    public String getPettyId() {
        return (String) getValue("sPettyIDx");
    }
    
    public JSONObject setClientId(String clientId) {
        return setValue("sClientID", clientId);
    }

    public String getClientId() {
        return (String) getValue("sClientID");
    }

    public JSONObject setPayeeName(String payeeName) {
        return setValue("sPayeeNme", payeeName);
    }

    public String getPayeeName() {
        return (String) getValue("sPayeeNme");
    }

    public JSONObject setCreditedTo(String creditedTo) {
        return setValue("sCrdtedTo", creditedTo);
    }

    public String getCreditedTo() {
        return (String) getValue("sCrdtedTo");
    }

    public JSONObject setDepartmentRequest(String deptRequest) {
        return setValue("sDeptReqs", deptRequest);
    }

    public String getDepartmentRequest() {
        return (String) getValue("sDeptReqs");
    }

    public JSONObject setAddress(String address) {
        return setValue("sAddressx", address);
    }

    public String getAddress() {
        return (String) getValue("sAddressx");
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setReferNo(String referNo) {
        return setValue("sReferNox", referNo);
    }

    public String getReferNo() {
        return (String) getValue("sReferNox");
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

    public JSONObject setCashFundId(String cashFundId) {
        return setValue("sCashFIDx", cashFundId);
    }

    public String getCashFundId() {
        return (String) getValue("sCashFIDx");
    }

    public JSONObject setTransactionTotal(Double transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Double getTransactionTotal() {
        if (getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nTranTotl").toString());
    }

    public JSONObject setVatableSales(Double vatableSales) {
        return setValue("nVATSales", vatableSales);
    }

    public Double getVatableSales() {
        if (getValue("nVATSales") == null || "".equals(getValue("nVATSales"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nVATSales").toString());
    }

    public JSONObject setVatAmount(Double vatAmount) {
        return setValue("nVATAmtxx", vatAmount);
    }

    public Double getVatAmount() {
        if (getValue("nVATAmtxx") == null || "".equals(getValue("nVATAmtxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nVATAmtxx").toString());
    }

    public JSONObject setZeroVatSales(Double zeroVatSales) {
        return setValue("nZroVATSl", zeroVatSales);
    }

    public Double getZeroVatSales() {
        if (getValue("nZroVATSl") == null || "".equals(getValue("nZroVATSl"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nZroVATSl").toString());
    }

    public JSONObject setVatExempt(Double vatExempt) {
        return setValue("nVatExmpt", vatExempt);
    }

    public Double getVatExempt() {
        if (getValue("nVatExmpt") == null || "".equals(getValue("nVatExmpt"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nVatExmpt").toString());
    }

    public JSONObject setWithTaxTotal(Double withholdingTaxTotal) {
        return setValue("nWTaxTotl", withholdingTaxTotal);
    }

    public Double getWithTaxTotal() {
        if (getValue("nWTaxTotl") == null || "".equals(getValue("nWTaxTotl"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nWTaxTotl").toString());
    }

    public JSONObject setNetTotal(Double netTotal) {
        return setValue("nNetTotal", netTotal);
    }

    public Double getNetTotal() {
        if (getValue("nNetTotal") == null || "".equals(getValue("nNetTotal"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nNetTotal").toString());
    }

    public boolean isPrinted() {
        return ((String) getValue("cVchrPrnt")).equals("1");
    }

    public JSONObject isPrinted(boolean isPrinted) {
        return setValue("cVchrPrnt", isPrinted ? "1" : "0");
    }

    public JSONObject isCollected(boolean isCollected) {
        return setValue("cCollectd", isCollected ? "1" : "0");
    }

    public boolean isCollected() {
        return ((String) getValue("cProcessd")).equals("1");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    public JSONObject setModifiedBy(String modifiedBy) {
        return setValue("sModified", modifiedBy);
    }

    public String getModifiedBy() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    //reference object models
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

    public Model_Client_Master Credited() throws SQLException, GuanzonException {
        if (poClient == null) {
            poClient = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals((String) getValue("sCrdtedTo"))) {
            if (poClient.getEditMode() == EditMode.READY
                    && poClient.getClientId().equals((String) getValue("sCrdtedTo"))) {
                return poClient;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sCrdtedTo"), poClient)) {
                    return poClient;
                }

                poJSON = poClient.openRecord((String) getValue("sCrdtedTo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sCrdtedTo"), poClient);
                    return poClient;
                } else {
                    poClient.initialize();
                    return poClient;
                }
            }
        } else {
            poClient.initialize();
            return poClient;
        }
    }

    public Model_Department Department() throws SQLException, GuanzonException {
        if (poDepartment == null) {
            poDepartment = new ParamModels(poGRider).Department();
        }
        if (!"".equals((String) getValue("sDeptReqs"))) {
            if (poDepartment.getEditMode() == EditMode.READY
                    && poDepartment.getDepartmentId().equals((String) getValue("sDeptReqs"))) {
                return poDepartment;
            } else {
                if (ReferenceCache.tryLoad("Department", (String) getValue("sDeptReqs"), poDepartment)) {
                    return poDepartment;
                }

                poJSON = poDepartment.openRecord((String) getValue("sDeptReqs"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Department", (String) getValue("sDeptReqs"), poDepartment);
                    return poDepartment;
                } else {
                    poDepartment.initialize();
                    return poDepartment;
                }
            }
        } else {
            poDepartment.initialize();
            return poDepartment;
        }
    }

    public Model_Cash_Fund CashFund() throws SQLException, GuanzonException {
        if (poCashFund == null) {
            poCashFund = new CashflowModels(poGRider).CashFund();
        }
        if (!"".equals((String) getValue("sCashFIDx"))) {
            if (poCashFund.getEditMode() == EditMode.READY
                    && poCashFund.getCashFundId().equals((String) getValue("sCashFIDx"))) {
                return poCashFund;
            } else {
                poJSON = poCashFund.openRecord((String) getValue("sCashFIDx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCashFund;
                } else {
                    poCashFund.initialize();
                    return poCashFund;
                }
            }
        } else {
            poCashFund.initialize();
            return poCashFund;
        }
    }

}
