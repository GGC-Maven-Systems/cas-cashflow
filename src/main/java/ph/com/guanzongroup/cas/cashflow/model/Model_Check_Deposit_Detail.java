package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 *
 * @author maynevval 09-27-2025
 */
public class Model_Check_Deposit_Detail extends Model {

    private Model_Check_Payments poCheckPayment;
    private Model_Disbursement_Master poDisbursement;
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
            poEntity.updateObject("dModified", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));

            poEntity.updateObject("nEntryNox", 0);         
            poEntity.updateObject("cReversex", "+");
            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(2);

//            poCheckReceive =  new CashflowModels(poGRider).CheckReceive();

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
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

    //sRemarksx
    public JSONObject setRemarks(String originalid) {
        return setValue("sRemarksx", originalid);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
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
        if (poCheckPayment == null) {
            poCheckPayment = new CashflowModels(poGRider).CheckPayments();
        }
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
    
    
    public Model_Disbursement_Master Disbursement() throws SQLException, GuanzonException {
        if (poDisbursement == null) {
            poDisbursement = new CashflowModels(poGRider).DisbursementMaster();
        }
        if (!"".equals(CheckPayment().getSourceNo())) {
            if (this.poDisbursement.getEditMode() == 1 && this.poDisbursement.getTransactionNo().equals(CheckPayment().getSourceNo())) {
                return this.poDisbursement;
            }
            this.poJSON = this.poDisbursement.openRecord(CheckPayment().getSourceNo());
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poDisbursement;
            }
            this.poDisbursement.initialize();
            return this.poDisbursement;
        }
        poDisbursement.initialize();
        return this.poDisbursement;
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
