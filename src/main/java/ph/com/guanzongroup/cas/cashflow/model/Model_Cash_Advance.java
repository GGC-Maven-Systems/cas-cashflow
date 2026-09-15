/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.util.Date;
import java.sql.SQLException;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.constant.EditMode;
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

/**
 *
 * @author Team 1 06/05/2025
 */
public class Model_Cash_Advance extends Model {

    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Branch poBranch;
    Model_Department poDepartment;
    Model_Cash_Fund poCashFund;
    Model_Client_Master poIssued;
    Model_Client_Master poClient;
    Model_Client_Master poLiquidated;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            //assign default values
            poEntity.updateNull("dTransact");
            poEntity.updateNull("dIssuedxx");
            poEntity.updateNull("dLiquidtd");
            poEntity.updateNull("dModified");
            poEntity.updateObject("nAdvAmtxx", 0.0000);
            poEntity.updateObject("nLiqTotal", 0.0000);
            poEntity.updateString("cTranStat", CashAdvanceStatus.OPEN);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

            //poIndustry, poCompany, poBranch, poDepartment, poClient, poIssued, poLiquidated,
            //poCashFund are intentionally NOT constructed here - see their accessor methods
            //below, which build them lazily on first access so opening this record never
            //touches those tables.

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

    public JSONObject setIndustryId(String industryCode) {
        return setValue("sIndstCdx", industryCode);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setClientId(String clientId) {
        return setValue("sClientID", clientId);
    }

    public String getClientId() {
        return (String) getValue("sClientID");
    }

    public JSONObject setDepartmentRequest(String deptRequest) {
        return setValue("sDeptReqs", deptRequest);
    }

    public String getDepartmentRequest() {
        return (String) getValue("sDeptReqs");
    }

    public JSONObject setCashFundId(String cashFundId) {
        return setValue("sCashFIDx", cashFundId);
    }

    public String getCashFundId() {
        return (String) getValue("sCashFIDx");
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setAdvanceAmount(Double advanceAmount) {
        return setValue("nAdvAmtxx", advanceAmount);
    }

    public Double getAdvanceAmount() {
        if (getValue("nAdvAmtxx") == null || "".equals(getValue("nAdvAmtxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nAdvAmtxx").toString());
    }

    public JSONObject setIssuedBy(String issuedBy) {
        return setValue("sIssuedxx", issuedBy);
    }

    public String getIssuedBy() {
        return (String) getValue("sIssuedxx");
    }
    
    public JSONObject setIssuedDate(Date issuedDate) {
        return setValue("dIssuedxx", issuedDate);
    }

    public Date getIssuedDate() {
//        System.out.println("get Value ISSUED DATE : " + (Date) getValue("dIssuedxx"));
        if((Date) getValue("dIssuedxx") == null){
            try {
                String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(this), 
                                    " sTransNox = " + SQLUtil.toSQL((String) getValue("sTransNox"))
                                    );
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                if (loRS.next()) {
//                    System.out.println("DB Select ISSUED DATE : " + loRS.getTimestamp("dIssuedxx"));
                    setIssuedDate(loRS.getTimestamp("dIssuedxx"));
                }
            } catch (SQLException e) {
                return (Date) getValue("dIssuedxx");
            } 
        }
        return (Date) getValue("dIssuedxx");
    }
    
    public JSONObject setLiquidationTotal(Double liquidationTotal) {
        return setValue("nLiqTotal", liquidationTotal);
    }

    public Double getLiquidationTotal() {
        if (getValue("nLiqTotal") == null || "".equals(getValue("nLiqTotal"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nLiqTotal").toString());
    }

    public JSONObject setLiquidatedBy(String liquidatedBy) {
        return setValue("sLiquidtd", liquidatedBy);
    }

    public String getLiquidatedBy() {
        return (String) getValue("sLiquidtd");
    }

    public JSONObject setLiquidatedDate(Date liquidatedDate) {
        return setValue("dLiquidtd", liquidatedDate);
    }

    public Date getLiquidatedDate() {
//        System.out.println("get Value LIQUIDATED DATE : " + (Date) getValue("dIssuedxx"));
        if((Date) getValue("dLiquidtd") == null){
            try {
                String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(this), 
                                    " sTransNox = " + SQLUtil.toSQL((String) getValue("sTransNox"))
                                    );
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                if (loRS.next()) {
//                    System.out.println("DB select LIQUIDATED DATE : " + loRS.getTimestamp("dLiquidtd"));
                    setLiquidatedDate(loRS.getTimestamp("dLiquidtd"));
                }
            } catch (SQLException e) {
                return (Date) getValue("dLiquidtd");
            } 
        }
        return (Date) getValue("dLiquidtd");
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

    public Model_Client_Master Payee() throws SQLException, GuanzonException {
        if (poClient == null) {
            poClient = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals((String) getValue("sClientID"))) {
            if (poClient.getEditMode() == EditMode.READY
                    && poClient.getClientId().equals((String) getValue("sClientID"))) {
                return poClient;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sClientID"), poClient)) {
                    return poClient;
                }

                poJSON = poClient.openRecord((String) getValue("sClientID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sClientID"), poClient);
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

    public Model_Client_Master Issued() throws SQLException, GuanzonException {
        if (poIssued == null) {
            poIssued = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals((String) getValue("sIssuedxx"))) {
            if (poIssued.getEditMode() == EditMode.READY
                    && poIssued.getClientId().equals((String) getValue("sIssuedxx"))) {
                return poIssued;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sIssuedxx"), poIssued)) {
                    return poIssued;
                }

                poJSON = poIssued.openRecord((String) getValue("sIssuedxx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sIssuedxx"), poIssued);
                    return poIssued;
                } else {
                    poIssued.initialize();
                    return poIssued;
                }
            }
        } else {
            poIssued.initialize();
            return poIssued;
        }
    }

    public Model_Client_Master Liquidated() throws SQLException, GuanzonException {
        if (poLiquidated == null) {
            poLiquidated = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals((String) getValue("sLiquidtd"))) {
            if (poLiquidated.getEditMode() == EditMode.READY
                    && poLiquidated.getClientId().equals((String) getValue("sLiquidtd"))) {
                return poLiquidated;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sLiquidtd"), poLiquidated)) {
                    return poLiquidated;
                }

                poJSON = poLiquidated.openRecord((String) getValue("sLiquidtd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sLiquidtd"), poLiquidated);
                    return poLiquidated;
                } else {
                    poLiquidated.initialize();
                    return poLiquidated;
                }
            }
        } else {
            poLiquidated.initialize();
            return poLiquidated;
        }
    }
    
//    public String getSQLiquidatedDate() throws SQLException, GuanzonException{
//        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(this), 
//                                                                    " sTransNox = " + SQLUtil.toSQL((String) getValue("sTransNox"))
//                                                                    );
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        try {
//            if (MiscUtil.RecordCount(loRS) > 0) {
//                if(loRS.next()){
//                    return loRS.getDate("dLiquidtd") ;
//                }
//            }
//            MiscUtil.close(loRS);
//        } catch (SQLException e) {
//            System.out.println("No record loaded.");
//        }
//        return "";
//    }
}
