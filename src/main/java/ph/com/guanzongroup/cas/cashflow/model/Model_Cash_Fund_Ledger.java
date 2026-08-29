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
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CashFundStatus;

/**
 *
 * @author Arsiela 04/01/2026
 */
public class Model_Cash_Fund_Ledger extends Model {
    Model_Cash_Fund poCashFund;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            //assign default values
            poEntity.updateNull("dModified");
            poEntity.updateObject("nDebtAmtx", 0.0000);
            poEntity.updateObject("nCrdtAmtx", 0.0000);
            poEntity.updateObject("nLedgerNo", 0);
            poEntity.updateString("cReversex", CashFundStatus.Reverse.INCLUDE);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sCashFIDx";
            ID2 = "sSourceCD";
            ID3 = "sSourceNo";
//            ID4 = "cReversex";

            //initialize reference objects
            CashflowModels gl = new CashflowModels(poGRider);
            poCashFund = gl.CashFund();
//            end - initialize reference objects

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), "nLedgerNo", false, poGRider.getGConnection().getConnection(), "");
    }

    public JSONObject setCashFundId(String cashFundId) {
        return setValue("sCashFIDx", cashFundId);
    }

    public String getCashFundId() {
        return (String) getValue("sCashFIDx");
    }

    public JSONObject setLedgerNo(int ledgerNo) {
        return setValue("nLedgerNo", ledgerNo);
    }

    public int getLedgerNo() {
        return (int) getValue("nLedgerNo");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCD", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCD");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setBatchNo(String batchNo) {
        return setValue("sBatchNox", batchNo);
    }

    public String getBatchNo() {
        return (String) getValue("sBatchNox");
    }

    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    public JSONObject setCashFundManager(String cashFundManager) {
        return setValue("sCashFMgr", cashFundManager);
    }

    public String getCashFundManager() {
        return (String) getValue("sCashFMgr");
    }

    public JSONObject setDebitAmount(Double debitAmount) {
        return setValue("nDebtAmtx", debitAmount);
    }

    public Double getDebitAmount() {
        if (getValue("nDebtAmtx") == null || "".equals(getValue("nDebtAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDebtAmtx").toString());
    }

    public JSONObject setCreditAmount(Double creditAmount) {
        return setValue("nCrdtAmtx", creditAmount);
    }

    public Double getCreditAmount() {
        if (getValue("nCrdtAmtx") == null || "".equals(getValue("nCrdtAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nCrdtAmtx").toString());
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    //reference object models
    public Model_Cash_Fund CashFund() throws SQLException, GuanzonException {
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
