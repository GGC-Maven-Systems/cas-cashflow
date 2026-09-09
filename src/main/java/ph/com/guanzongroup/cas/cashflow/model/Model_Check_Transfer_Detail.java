package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 *
 * @author maynevval 08-09-2025
 */
public class Model_Check_Transfer_Detail extends Model {

    private Model_Check_Payments poCheckPayment;
    private Model_Payee poPayee;
    private Model_Banks poBanks;
//    private Model_Check_Receive poCheckReceive;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("cReversex", "+");
            poEntity.updateString("cReceived", "0");
            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(2);

            poCheckPayment = new CashflowModels(poGRider).CheckPayments();
            poPayee = new CashflowModels(poGRider).Payee();
            poBanks = new ParamModels(poGRider).Banks();
//            poCheckReceive =  new CashflowModels(poGRider).CheckReceive();

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //nEntryNox*
    //sSourceCd*
    //sSourceNo*
    //cReceived*

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //sSourceCd
    public JSONObject setSourceCode(String sourceCd) {
        return setValue("sSourceCd", sourceCd);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    //sSourceNo
    public JSONObject setSourceNo(String originalid) {
        return setValue("sSourceNo", originalid);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    //sPayloadx
    public JSONObject setPayload(String payload) {
        return setValue("sPayloadx", payload);
    }

    public String getPayload() {
        return (String) getValue("sPayloadx");
    }

    //sRemarksx
    public JSONObject setRemarks(String originalid) {
        return setValue("sRemarksx", originalid);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    //cReceived
    public JSONObject isReceived(boolean isRequired) {
        return setValue("cReceived", isRequired ? "1" : "0");
    }
    
    public boolean isReceived() {
        Object value = getValue("cReceived");
        return "1".equals(String.valueOf(value));
    }

    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public Model_Check_Payments CheckPayment() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sSourceNo"))) {
            if (this.poCheckPayment.getEditMode() == 1 && this.poCheckPayment
                    .getTransactionNo().equals(getValue("sSourceNo"))) {
                return this.poCheckPayment;
            }
            this.poJSON = this.poCheckPayment.openRecord((String) getValue("sSourceNo"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poCheckPayment;
            }
            this.poCheckPayment.initialize();
            return this.poCheckPayment;
        }
        poCheckPayment.initialize();
        return this.poCheckPayment;
    }
    
    public Model_Banks Banks() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sBankIDxx"))) {
            if (poBanks.getEditMode() == EditMode.READY
                    && poBanks.getBankID().equals((String) getValue("sBankIDxx"))) {
                return poBanks;
            } else {
                poJSON = poBanks.openRecord((String) getValue("sBankIDxx"));
                if ("success".equals((String) poJSON.get("result"))) {
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
    
        public Model_Payee Payee() throws GuanzonException, SQLException {
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

//    public Model_Check_Receive CheckPayment() throws SQLException, GuanzonException {
//        if (!"".equals(getValue("sSourceNo"))) {
//            if (this.poCheckReceive.getEditMode() == 1 && this.poCheckReceive
//                    .getTransactionNo().equals(getValue("sSourceNo"))) {
//                return this.poCheckReceive;
//            }
//            this.poJSON = this.poCheckReceive.openRecord((String) getValue("sSourceNo"));
//            if ("success".equals(this.poJSON.get("result"))) {
//                return this.poCheckReceive;
//            }
//            this.poCheckReceive.initialize();
//            return this.poCheckReceive;
//        }
//        poCheckReceive.initialize();
//        return this.poCheckReceive;
//    }
}
