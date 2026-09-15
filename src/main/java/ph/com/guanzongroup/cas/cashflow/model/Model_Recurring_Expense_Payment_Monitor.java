package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

public class Model_Recurring_Expense_Payment_Monitor extends Model {

    Model_Recurring_Expense_Schedule poRecurringExpense;

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

            //assign default values
            poEntity.updateDouble("nBillMnth", 0);
            //end - assign default values

            ID = poEntity.getMetaData().getColumnLabel(1);

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    @Override
    public String getNextCode() {
//        return "";
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setRecurringNo(String recurringNo) {
        return setValue("sRecurrNo", recurringNo);
    }

    public String getRecurringNo() {
        return (String) getValue("sRecurrNo");
    }

    public JSONObject setSourceCode(String setSourceCode) {
        return setValue("sSourceCD", setSourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCD");
    }

    public JSONObject setBatchNo(String batchNo) {
        return setValue("sBatchNox", batchNo);
    }

    public String getBatchNo() {
        return (String) getValue("sBatchNox");
    }

    public int getBillMonth() {
        if(getValue("nBillMnth") == null || "".equals(getValue("nBillMnth"))){
            return 0;
        }
        return (int) getValue("nBillMnth");
    }

    public JSONObject setBillMonth(String batchNo) {
        return setValue("nBillMnth", batchNo);
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }
    
    public Model_Recurring_Expense_Schedule RecurringExpenseSchedule() throws GuanzonException, SQLException {
        if (poRecurringExpense == null) {
            poRecurringExpense = new CashflowModels(poGRider).Recurring_Expense_Schedule();
        }
        if (!"".equals((String) getValue("sRecurrNo"))) {
            if (poRecurringExpense.getEditMode() == EditMode.READY
                    && poRecurringExpense.getRecurringNo().equals((String) getValue("sRecurrNo"))) {
                return poRecurringExpense;
            } else {
                poJSON = poRecurringExpense.openRecord((String) getValue("sRecurrNo"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poRecurringExpense;
                } else {
                    poRecurringExpense.initialize();
                    return poRecurringExpense;
                }
            }
        } else {
            poRecurringExpense.initialize();
            return poRecurringExpense;
        }
    }
    
    public JSONObject openPendingRecurringExpense(String fsRecurringNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " (sBatchNox IS NULL OR TRIM(sBatchNox) = '') AND sRecurrNo = " + SQLUtil.toSQL(fsRecurringNo));

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
                    setValue(lnCtr, loRS.getObject(lnCtr));
                }
                MiscUtil.close(loRS);
                pnEditMode = EditMode.READY;
                poJSON = new JSONObject();
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }

}
