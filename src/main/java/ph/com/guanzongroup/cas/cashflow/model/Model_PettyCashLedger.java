package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

public class Model_PettyCashLedger extends Model {

//    Model_Branch poBranch;
//    Model_Department poDepartment;
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

            poEntity.updateObject("nLedgerNo", 0);
            poEntity.updateObject("nDebtAmtx", 0.0000);
            poEntity.updateObject("nCrdtAmtx", 0.0000);

            poEntity.updateObject("cReversex", "0");

            //end - assign default values
            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sPettyIDx";
//            ID2 = "sBranchCD";
//            ID3 = "sDeptIDxx";
            ID2 = "sSourceCD";
            ID3 = "sSourceNo";

            //initialize reference objects
//            ParamModels model = new ParamModels(poGRider);
//            poBranch = model.Branch();
//            poDepartment = model.Department();

            CashflowModels gl = new CashflowModels(poGRider);
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
        return MiscUtil.getNextCode(this.getTable(), "nLedgerNo", false, poGRider.getGConnection().getConnection(), "");
    }

    public JSONObject setPettyID(String pettyIDx) {
        return setValue("sPettyIDx", pettyIDx);
    }

    public String getPettyID() {
        return (String) getValue("sPettyIDx");
    }

//    public JSONObject setBranchCode(String branchCD) {
//        return setValue("sBranchCD", branchCD);
//    }
//
//    public String getBranchCode() {
//        return (String) getValue("sBranchCD");
//    }
//
//    public JSONObject setDepartmentID(String deptIDxx) {
//        return setValue("sDeptIDxx", deptIDxx);
//    }
//
//    public String getDepartmentID() {
//        return (String) getValue("sDeptIDxx");
//    }

    public JSONObject setLedgerNo(int ledgerNo) {
        return setValue("nLedgerNo", ledgerNo);
    }

    public int getLedgerNo() {
        return (int) getValue("nLedgerNo");
    }

    public JSONObject setSourceCode(String sourceCD) {
        return setValue("sSourceCD", sourceCD);
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

    public JSONObject setTransactionDate(Date transact) {
        return setValue("dTransact", transact);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setDebitAmount(Double debtAmtx) {
        return setValue("nDebtAmtx", debtAmtx);
    }

    public Double getDebitAmount() {
        if (getValue("nDebtAmtx") == null || "".equals(getValue("nDebtAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nDebtAmtx").toString());
    }

    public JSONObject setCreditAmount(Double crdtAmtx) {
        return setValue("nCrdtAmtx", crdtAmtx);
    }

    public Double getCreditAmount() {
        if (getValue("nCrdtAmtx") == null || "".equals(getValue("nCrdtAmtx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nCrdtAmtx").toString());
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    public JSONObject isReverse(boolean reversex) {
        return setValue("cReversex", reversex ? "+" : "-");
    }

    public JSONObject setModified(Date modified) {
        return setValue("dModified", modified);
    }

    public Date getModified() {
        return (Date) getValue("dModified");
    }

    //reference object models
    public Model_PettyCash PettyCash() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sPettyIDx"))) {
            if (poPettyCash.getEditMode() == EditMode.READY
                    && poPettyCash.getPettyId().equals((String) getValue("sPettyIDx"))) {
                return poPettyCash;
            } else {
                poJSON = poPettyCash.openRecord((String) getValue("sPettyIDx"));

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

//    public Model_Branch Branch() throws SQLException, GuanzonException {
//        if (!"".equals((String) getValue("sBranchCD"))) {
//            if (poBranch.getEditMode() == EditMode.READY
//                    && poBranch.getBranchCode().equals((String) getValue("sBranchCD"))) {
//                return poBranch;
//            } else {
//                poJSON = poBranch.openRecord((String) getValue("sBranchCD"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poBranch;
//                } else {
//                    poBranch.initialize();
//                    return poBranch;
//                }
//            }
//        } else {
//            poBranch.initialize();
//            return poBranch;
//        }
//    }
//
//    public Model_Department Department() throws SQLException, GuanzonException {
//        if (!"".equals((String) getValue("sDeptIDxx"))) {
//            if (poDepartment.getEditMode() == EditMode.READY
//                    && poDepartment.getDepartmentId().equals((String) getValue("sDeptIDxx"))) {
//                return poDepartment;
//            } else {
//                poJSON = poDepartment.openRecord((String) getValue("sDeptIDxx"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poDepartment;
//                } else {
//                    poDepartment.initialize();
//                    return poDepartment;
//                }
//            }
//        } else {
//            poDepartment.initialize();
//            return poDepartment;
//        }
//    }
}
