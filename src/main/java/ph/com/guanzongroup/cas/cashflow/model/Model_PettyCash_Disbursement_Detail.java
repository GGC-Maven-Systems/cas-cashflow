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
import org.guanzon.cas.parameter.model.Model_Account_Chart;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.PettyCashDisbursementStatus;

/**
 *
 * @author Arsiela 04-01-2026
 */
public class Model_PettyCash_Disbursement_Detail extends Model {

    Model_Account_Chart poAccount;
    Model_Particular poParticular;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nAmountxx", 0.0000);
            poEntity.updateObject("cReversex", PettyCashDisbursementStatus.Reverse.INCLUDE);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNumber){
        return setValue("nEntryNox", entryNumber);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setAccountCode(String accountCode) {
        return setValue("sAcctCode", accountCode);
    }

    public String getAccountCode() {
        return (String) getValue("sAcctCode");
    }

    public JSONObject setParticularId(String accountCode) {
        return setValue("sPrtclrID", accountCode);
    }

    public String getParticularId() {
        return (String) getValue("sPrtclrID");
    }

    public JSONObject setAmount(Double amount) {
        return setValue("nAmountxx", amount);
    }

    public Double getAmount() {
        if (getValue("nAmountxx") == null || "".equals(getValue("nAmountxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nAmountxx").toString());
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

//    public JSONObject setModifyingBy(String modified) {
//        return setValue("sModified", modified);
//    }
//
//    public String getModifyingBy() {
//        return (String) getValue("sModified");
//    }

    public JSONObject setModifiedDate(Date modified) {
        return setValue("dModified", modified);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    //reference object models
    public Model_Particular Particular() throws SQLException, GuanzonException {
        if (poParticular == null) {
            poParticular = new CashflowModels(poGRider).Particular();
        }
        if (!"".equals((String) getValue("sPrtclrID"))) {
            if (poParticular.getEditMode() == EditMode.READY
                    && poParticular.getParticularID().equals((String) getValue("sPrtclrID"))) {
                return poParticular;
            } else {
                poJSON = poParticular.openRecord((String) getValue("sPrtclrID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poParticular;
                } else {
                    poParticular.initialize();
                    return poParticular;
                }
            }
        } else {
            poParticular.initialize();
            return poParticular;
        }
    }
    
    public Model_Account_Chart AccountChart() throws SQLException, GuanzonException {
        if (poAccount == null) {
            poAccount = new ParamModels(poGRider).AccountChart();
        }
        if (!"".equals((String) getValue("sAcctCode"))) {
            if (poAccount.getEditMode() == EditMode.READY
                    && poAccount.getAccountCode().equals((String) getValue("sAcctCode"))) {
                return poAccount;
            } else {
                if (ReferenceCache.tryLoad("Account_Chart", (String) getValue("sAcctCode"), poAccount)) {
                    return poAccount;
                }
                poJSON = poAccount.openRecord((String) getValue("sAcctCode"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Account_Chart", (String) getValue("sAcctCode"), poAccount);
                    return poAccount;
                } else {
                    poAccount.initialize();
                    return poAccount;
                }
            }
        } else {
            poAccount.initialize();
            return poAccount;
        }
    }
    
}
