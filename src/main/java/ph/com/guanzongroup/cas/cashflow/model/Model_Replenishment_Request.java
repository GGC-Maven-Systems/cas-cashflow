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
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.ReplenishmentRequestStatus;

/**
 *
 * @author Arsiela 03/16/2026
 */
public class Model_Replenishment_Request extends Model {
    
    private String psCompany = "";
    private String psIndustry = "";
    
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Cash_Fund poCashFund;
    Model_PettyCash poPettyCash;

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
            poEntity.updateObject("nTranAmtx", 0.0000);
            poEntity.updateString("cFundType", Logical.NO);
            poEntity.updateString("cTranStat", ReplenishmentRequestStatus.OPEN);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poIndustry = model.Industry();
            poCompany = model.Company();
            
            CashflowModels gl = new CashflowModels(poGRider);
            poCashFund = gl.CashFund();
            poPettyCash = gl.PettyCashMaster();
//            end - initialize reference objects

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
        
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }
    

    public JSONObject setFundId(String cashFundId) {
        return setValue("sFundIdxx", cashFundId);
    }

    public String getFundId() {
        return (String) getValue("sFundIdxx");
    }

    public JSONObject setFundType(String fundType) {
        return setValue("cFundType", fundType);
    }

    public String getFundType() {
        return (String) getValue("cFundType");
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }
    
    public JSONObject setTransactionStatus(String transactonStatus) {
        return setValue("cTranStat", transactonStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }
    
    public JSONObject setTransactionAmount(Double transactionAmount) {
        return setValue("nTranAmtx", transactionAmount);
    }

    public Double getTransactionAmount() {
        if (getValue("nTranAmtx") == null || "".equals(getValue("nTranAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nTranAmtx").toString());
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
    
    public void setCompanyId(String companyId) {
        psCompany = companyId;
    }

    public String getCompanyId() {
        return psCompany;
    }

    public void setIndustryId(String industryCode) {
        psIndustry = industryCode;
    }

    public String getIndustryId() {
        return psIndustry;
    }

    //reference object models
    public Model_Cash_Fund CashFund() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sFundIdxx"))) {
            if (poCashFund.getEditMode() == EditMode.READY
                    && poCashFund.getCashFundId().equals((String) getValue("sFundIdxx"))) {
                return poCashFund;
            } else {
                poJSON = poCashFund.openRecord((String) getValue("sFundIdxx"));

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
    
    public Model_PettyCash PettyCash() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sFundIdxx"))) {
            if (poPettyCash.getEditMode() == EditMode.READY
                    && poPettyCash.getPettyId().equals((String) getValue("sFundIdxx"))) {
                return poPettyCash;
            } else {
                poJSON = poPettyCash.openRecord((String) getValue("sFundIdxx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPettyCash;
                } else {
                    poPettyCash.initialize();
                    return poPettyCash;
                }
            }
        } else {
            poPettyCash.initialize();
            return poPettyCash;
        }
    }

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (Logical.YES.equals((String) getValue("cFundType"))) {
            if (!"".equals((String) getValue("sFundIdxx")) && (String) getValue("sFundIdxx") != null) {
                psIndustry = PettyCash().getIndustryId();
            }
        } else {
            if (!"".equals((String) getValue("sFundIdxx"))  && (String) getValue("sFundIdxx") != null) {
                psIndustry = CashFund().getIndustryId();
            }
        }
        
        if (!"".equals(psIndustry)) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals(psIndustry)) {
                return poIndustry;
            } else {
                poJSON = poIndustry.openRecord(psIndustry);

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

    public Model_Company Company() throws SQLException, GuanzonException {
        if (Logical.YES.equals((String) getValue("cFundType"))) {
            if (!"".equals((String) getValue("sFundIdxx")) && (String) getValue("sFundIdxx") != null) {
                psCompany = PettyCash().getCompanyId();
            }
        } else {
            if (!"".equals((String) getValue("sFundIdxx"))  && (String) getValue("sFundIdxx") != null) {
                psCompany = CashFund().getCompanyId();
            }
        }
        
        if (!"".equals(psCompany)) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals(psCompany)) {
                return poCompany;
            } else {
                poJSON = poCompany.openRecord(psCompany);

                if ("success".equals(psCompany)) {
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

}
