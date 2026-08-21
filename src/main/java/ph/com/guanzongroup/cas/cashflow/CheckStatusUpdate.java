package ph.com.guanzongroup.cas.cashflow;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.validator.DisbursementValidator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.script.ScriptException;
import static jdk.nashorn.internal.runtime.JSType.toBoolean;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.parameter.Banks;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.rmj.cas.core.APTransaction;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Detail;
import ph.com.guanzongroup.cas.cashflow.status.APPaymentAdjustmentStatus;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatus;

public class CheckStatusUpdate extends Transaction {

    List<Model_Disbursement_Master> poDisbursementMaster;

    private CheckPayments checkPayments;
    private PaymentRequest PRFTrans;
    private PurchaseOrderReceiving PORTrans;
    private CachePayable CPTrans;
    private APPaymentAdjustment APPaymAdjustTrans;
    private SOATagging SOAMasterTrans;
    List<String> paSOATaggingMaster;
    private Journal poJournal;
    private JournalProposal poJournalProposal;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private JSONObject cachedCheckTrans;
    public String psApprover = "";

    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "DISb";

        poMaster = new CashflowModels(poGRider).DisbursementMaster();
        poDetail = new CashflowModels(poGRider).DisbursementDetail();
        poJournalProposal = new CashflowControllers(poGRider, logwrapr).JournalProposal();
        poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
        checkPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
//        disbursement = new CashflowControllers(poGRider, logwrapr).Disbursement();
        PRFTrans = new CashflowControllers(poGRider, logwrapr).PaymentRequest();
        PORTrans = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        CPTrans = new CashflowControllers(poGRider, logwrapr).CachePayable();
        APPaymAdjustTrans = new CashflowControllers(poGRider, logwrapr).APPaymentAdjustment();
        SOAMasterTrans = new CashflowControllers(poGRider, logwrapr).SOATagging();
        paDetail = new ArrayList<>();
        paSOATaggingMaster = new ArrayList<>();

        return initialize();
    }

    public void setIndustryID(String industryID) {
        psIndustryId = industryID;
    }

    public void setCompanyID(String companyID) {
        psCompanyId = companyID;
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public CheckPayments CheckPayments() {
        return (CheckPayments) checkPayments;
    }

//    public BankAccountMaster BankAccountMaster() {
//        return (BankAccountMaster) bankAccount;
//    }

//    public Disbursement Disbursement() {
//        return (Disbursement) disbursement;
//    }

    private Model_Disbursement_Master DisbursementMasterList() {
        return new CashflowModels(poGRider).DisbursementMaster();
    }

    public int getDisbursementMasterCount() {
        return this.poDisbursementMaster.size();
    }

    public Model_Disbursement_Master poDisbursementMaster(int row) {
        return (Model_Disbursement_Master) poDisbursementMaster.get(row);
    }

    /*End - Search Master References*/
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Disbursement_Master Master() {
        return (Model_Disbursement_Master) poMaster;
    }

    @Override
    public Model_Disbursement_Detail Detail(int row) {
        return (Model_Disbursement_Detail) paDetail.get(row);
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (Detail(getDetailCount() - 1).getSourceNo().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has empty item.");
            return poJSON;
        }

        return addDetail();
    }

    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryID(object.getModel().getIndustryId());
        }

        return poJSON;
    }

    public JSONObject SearchBanks(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Banks object = new ParamControllers(poGRider, logwrapr).Banks();
        object.setRecordStatus("1");
        if(pbWithUI)
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            CheckPayments().getModel().setBankID(object.getModel().getBankID());
        }

        return poJSON;
    }

    public JSONObject SearhBankAccount(String value, String BankID, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        BankAccountMaster object = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
        object.setRecordStatus("1");
        if (BankID != null && !BankID.isEmpty()) {
            poJSON = object.searchRecordbyBanks(value, BankID, byCode);
            if ("success".equals((String) poJSON.get("result"))) {
                CheckPayments().getModel().setBankAcountID(object.getModel().getBankAccountId());
            }
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Please enter Bank First.");
        }
        return poJSON;
    }

    @Override
    public JSONObject willSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject saveCheckPayments() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        checkPayments.setWithParentClass(true);
        checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
        checkPayments.getModel().setModifyingId(poGRider.getUserID());
        poJSON = checkPayments.saveRecord();
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject initFields() {
        poJSON = new JSONObject();
        Master().setBranchCode(poGRider.getBranchCode());
        Master().setIndustryID(psIndustryId);
        Master().setCompanyID(psCompanyId);
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(DisbursementStatic.OPEN);
    }

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = DisbursementStatic.RETURNED_I;
        boolean lbReturn = true;

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        poJSON = isEntryOkay(DisbursementStatic.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = saveUpdateOthers();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction saved successfully.");
        return poJSON;
    }

    private JSONObject saveUpdateOthers() throws CloneNotSupportedException {
        poJSON = new JSONObject();
        try {
            poJSON = saveCheckPayments();
            if ("error".equals(poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(CheckStatusUpdate.class.getName()).log(Level.SEVERE, null, ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        poJSON = new JSONObject();
        try {
            switch (CheckPayments().getModel().getTransactionStatus()) {
                case CheckStatus.POSTED:
                    break;
                default:
                    break;
            }
            poJSON = saveCheckPayments();
            if ("error".equals(poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(CheckStatusUpdate.class.getName()).log(Level.SEVERE, null, ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox,"
                + " a.dTransact,"
                + " c.sBranchNm,"
                + " d.sPayeeNme,"
                + " e.sCompnyNm AS supplier,"
                + " f.sDescript,"
                + " a.nNetTotal, "
                + " a.cDisbrsTp "
                + " FROM Disbursement_Master a "
                + " LEFT JOIN Disbursement_Detail b ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd "
                + " LEFT JOIN Payee d ON a.sPayeeIDx = d.sPayeeIDx "
                + " LEFT JOIN Client_Master e ON d.sClientID = e.sClientID "
                + " LEFT JOIN Particular f ON b.sPrtclrID = f.sPrtclrID"
                + " LEFT JOIN Check_Payments g ON a.sTransNox = g.sSourceNo"
                + " LEFT JOIN Banks i ON g.sBankIDxx = i.sBankIDxx "
                + " LEFT JOIN Bank_Account_Master j ON g.sBnkActID = j.sBnkActID";
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = new DisbursementValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }

    public JSONObject getDisbursementx(String fsBankID,
            String fsBankAccountID,
            String fsCheckNo) throws SQLException, GuanzonException {

        JSONObject loJSON = new JSONObject();
        JSONArray loArray = new JSONArray();

        String lsSQL = " SELECT "
                + " a.sTransNox, "
                + " a.dTransact, "
                + " b.sTransNox AS checktrans, "
                + " b.sCheckNox, "
                + " b.dCheckDte, "
                + " b.cProcessd, "
                + " b.cTranStat AS checkstat, "
                + " c.sBankIDxx, "
                + " c.sBankName, "
                + " d.sBnkActID, "
                + " d.sActNumbr, "
                + " d.sActNamex, "
                + " b.nAmountxx "
                + " FROM Disbursement_Master a "
                + " LEFT JOIN Check_Payments b "
                + "     ON a.sTransNox = b.sSourceNo "
                + " LEFT JOIN Banks c "
                + "     ON b.sBankIDxx = c.sBankIDxx "
                + " LEFT JOIN Bank_Account_Master d "
                + "     ON b.sBnkActID = d.sBnkActID ";

        List<String> loCondition = new ArrayList<String>();

        loCondition.add("a.cDisbrsTp = " + SQLUtil.toSQL(Logical.NO));
        loCondition.add("b.sCheckNox IS NOT NULL");
        loCondition.add("b.sCheckNox <> ''");
        loCondition.add("b.cProcessd = '1'");
        loCondition.add("b.cTranStat IN ('1', '5')");

        if (fsBankID != null && !fsBankID.trim().isEmpty()) {
            loCondition.add("b.sBankIDxx LIKE "
                    + SQLUtil.toSQL("%" + fsBankID.trim() + "%"));
        }

        if (fsBankAccountID != null && !fsBankAccountID.trim().isEmpty()) {
            loCondition.add("b.sBnkActID LIKE "
                    + SQLUtil.toSQL("%" + fsBankAccountID.trim() + "%"));
        }

        if (fsCheckNo != null && !fsCheckNo.trim().isEmpty()) {
            loCondition.add("b.sCheckNox LIKE "
                    + SQLUtil.toSQL("%" + fsCheckNo.trim() + "%"));
        }

        lsSQL = MiscUtil.addCondition(
                lsSQL,
                String.join(" AND ", loCondition)
                + " GROUP BY a.sTransNox "
                + " ORDER BY a.dTransact ASC "
        );

        System.out.println("Executing SQL: " + lsSQL);

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {

            while (loRS.next()) {
                JSONObject loData = new JSONObject();
                loData.put("sTransNox", loRS.getString("sTransNox"));
                loData.put("dTransact", loRS.getString("dTransact"));
                loData.put("checktrans", loRS.getString("checktrans"));
                loData.put("sCheckNox", loRS.getString("sCheckNox"));
                loData.put("dCheckDte", loRS.getString("dCheckDte"));
                loData.put("cProcessd", loRS.getString("cProcessd"));
                loData.put("checkstat", loRS.getString("checkstat"));
                loData.put("sBankIDxx", loRS.getString("sBankIDxx"));
                loData.put("sBankName", loRS.getString("sBankName"));
                loData.put("sBnkActID", loRS.getString("sBnkActID"));
                loData.put("sActNumbr", loRS.getString("sActNumbr"));
                loData.put("sActNamex", loRS.getString("sActNamex"));
                loData.put("nAmountxx", loRS.getString("nAmountxx"));
                loArray.add(loData);
            }
        } finally {
            MiscUtil.close(loRS);
        }

        if (loArray.size() > 0) {
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
            loJSON.put("payload", loArray);
        } else {
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found.");
            loJSON.put("payload", new JSONArray());
        }
        return loJSON;
    }

//    public JSONObject getDisbursement(String fsBankID,
//            String fsBankAccountID,
//            String fsCheckNo)
//            throws SQLException, GuanzonException {
//
//        poJSON = new JSONObject();
//        String lsSQL = " SELECT a.sTransNox, "
//                + " a.dTransact, "
//                + " b.sTransNox AS checktrans, "
//                + " b.sCheckNox, "
//                + " b.dCheckDte, "
//                + " b.cProcessd, "
//                + " b.cTranStat AS checkstat, "
//                + " c.sBankIDxx, "
//                + " c.sBankName, "
//                + " d.sBnkActID, "
//                + " d.sActNumbr, "
//                + " d.sActNamex "
//                + " FROM Disbursement_Master a "
//                + " LEFT JOIN Check_Payments b ON a.sTransNox = b.sSourceNo "
//                + " LEFT JOIN Banks c ON b.sBankIDxx = c.sBankIDxx "
//                + " LEFT JOIN Bank_Account_Master d ON b.sBnkActID = d.sBnkActID ";
//
//        List<String> lsCondition = new ArrayList<>();
//
//        lsCondition.add("a.cDisbrsTp = " + SQLUtil.toSQL(Logical.NO));
//        lsCondition.add("b.sCheckNox IS NOT NULL");
//        lsCondition.add("b.sCheckNox <> ''");
//        lsCondition.add("b.cProcessd = '1'");
//        lsCondition.add("b.cTranStat IN ('1', '5')");
//
//        if (fsBankID != null && !fsBankID.trim().isEmpty()) {
//            lsCondition.add("b.sBankIDxx LIKE " + SQLUtil.toSQL("%" + fsBankID + "%"));
//        }
//
//        if (fsBankAccountID != null && !fsBankAccountID.trim().isEmpty()) {
//            lsCondition.add("b.sBnkActID LIKE " + SQLUtil.toSQL("%" + fsBankAccountID + "%"));
//        }
//
//        if (fsCheckNo != null && !fsCheckNo.trim().isEmpty()) {
//            lsCondition.add("b.sCheckNox LIKE " + SQLUtil.toSQL("%" + fsCheckNo + "%"));
//        }
//
//        String lsFilterCondition = String.join(" AND ", lsCondition);
//
//        lsSQL = MiscUtil.addCondition(lsSQL,
//                lsFilterCondition + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ");
//
//        System.out.println("Executing SQL: " + lsSQL);
//
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//        if (MiscUtil.RecordCount(loRS) > 0) {
//
//            poDisbursementMaster = new ArrayList<>();
//
//            while (loRS.next()) {
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster
//                        .get(poDisbursementMaster.size() - 1)
//                        .openRecord(loRS.getString("sTransNox"));
//            }
//
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//
//        } else {
//
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found.");
//        }
//
//        MiscUtil.close(loRS);
//
//        return poJSON;
//    }

    public JSONObject setCheckpayment() throws GuanzonException, SQLException {
        if (Master().getOldDisbursementType().equals(DisbursementStatic.DisbursementType.CHECK)
                || Master().getDisbursementType().equals(DisbursementStatic.DisbursementType.CHECK)) {

            int editMode = Master().getEditMode();
            String transactionNo = Master().getTransactionNo();

            String checkPaymentTransactionNo = "";
            if (checkPayments == null) {
                checkPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
                checkPayments.setWithParentClass(true);
            }

            switch (editMode) {
                case EditMode.ADDNEW:
                    if (checkPayments.getEditMode() != EditMode.ADDNEW) {
                        checkPayments.newRecord();

                        checkPayments.getModel().setAmount(Master().getNetTotal());
                        checkPayments.getModel().setSourceNo(Master().getTransactionNo());
                        checkPayments.getModel().setSourceCode(Master().CheckPayments().getSourceCode());
                        checkPayments.getModel().setBranchCode(Master().getBranchCode());

                    }
                    break;
                case EditMode.READY:
                    if (checkPayments.getEditMode() != EditMode.READY) {
                        checkPaymentTransactionNo = checkPayments.getTransactionNoOfCheckPayment(transactionNo, Master().CheckPayments().getSourceCode());
                        checkPayments.openRecord(checkPaymentTransactionNo);
                    }
                    break;

                case EditMode.UPDATE:
                    if (checkPayments.getEditMode() != EditMode.UPDATE) {
                        checkPaymentTransactionNo = checkPayments.getTransactionNoOfCheckPayment(transactionNo, Master().CheckPayments().getSourceCode());
                        checkPayments.openRecord(checkPaymentTransactionNo);
                        checkPayments.updateRecord();

                        checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
                        checkPayments.getModel().setModifyingId(poGRider.getUserID());
                        boolean disbursementTypeChanged = !Master().getDisbursementType().equals(Master().getOldDisbursementType());
                        if (disbursementTypeChanged) {
                            if (Master().getDisbursementType().equals(DisbursementStatic.DisbursementType.CHECK)) {
                                checkPayments.getModel().setTransactionStatus(CheckStatus.FLOAT);
                                checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
                                checkPayments.getModel().setModifyingId(poGRider.getUserID());
                            } else {

                                checkPayments.getModel().setTransactionStatus(CheckStatus.VOID);
                                checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
                                checkPayments.getModel().setModifyingId(poGRider.getUserID());
                            }
                        }
                    } else {
                        boolean disbursementTypeChanged = !Master().getDisbursementType().equals(Master().getOldDisbursementType());
                        if (disbursementTypeChanged) {
                            if (Master().getDisbursementType().equals(DisbursementStatic.DisbursementType.CHECK)) {
                                checkPayments.getModel().setTransactionStatus(CheckStatus.OPEN);
                                checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
                                checkPayments.getModel().setModifyingId(poGRider.getUserID());
                            } else {
                                checkPayments.getModel().setTransactionStatus(CheckStatus.VOID);
                                checkPayments.getModel().setModifiedDate(poGRider.getServerDate());
                                checkPayments.getModel().setModifyingId(poGRider.getUserID());
                            }
                        }
                    }
                    break;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }

//    public JSONObject getDisbursement(String fsBankID, String fsBankAccountID) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        initSQL();
//        String lsFilterCondition = String.join(" AND ",
//                " a.cDisbrsTp = " + SQLUtil.toSQL(Logical.NO),
//                " g.sBankIDxx LIKE " + SQLUtil.toSQL("%" + fsBankID),
//                " g.sBnkActID LIKE " + SQLUtil.toSQL("%" + fsBankAccountID),
//                "g.sCheckNox IS NOT NULL",
//                "g.sCheckNox <> ''",
//                "g.cProcessd = '1'",
//                " g.cTranStat IN ('1', '5')");
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ");
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//        int lnCtr = 0;
//
//        if (MiscUtil.RecordCount(loRS) > 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//
//        MiscUtil.close(loRS);
//        return poJSON;
//    }



    public JSONObject cancelCheckPayment(String CheckRemarks) throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = CheckStatus.CANCELLED;
        CheckPayments checkTrans;
        checkTrans = new CashflowControllers(poGRider, logwrapr).CheckPayments();
//        poJSON = checkTrans.openRecord(Master().CheckPayments().getTransactionNo());
        poJSON = checkTrans.OpenTransaction(Master().CheckPayments().getTransactionNo(), true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        cachedCheckTrans = (JSONObject) poJSON.get("data");
        poJSON = checkTrans.updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = checkTrans.getModel().setRemarks(CheckRemarks);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setTransactionStatus(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifiedDate(poGRider.getServerDate());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifyingId(poGRider.getUserID());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = checkTrans.checkStatusChange(checkTrans.getModel().getTable(),
                (String) checkTrans.getModel().getValue("sTransNox"),
                CheckRemarks,
                lsStatus,
                false,
                true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updateJournalEntry() throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {
        poJSON = new JSONObject();
        poJournal.setWithParent(true);
        poJournal.setWithUI(false);
        if (psApprover != null && !"".equals(psApprover)) {
            poJournal.setApproving(psApprover);
        }
        poJournal.InitTransaction();
        poJSON = poJournal.OpenTransaction(getJournalTrans());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = poJournal.ReturnTransaction("");
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public JSONObject updateJournalProposal() throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {
        poJSON = new JSONObject();
        poJournalProposal.setWithParent(true);
        poJournalProposal.setWithUI(false);
        if (psApprover != null && !"".equals(psApprover)) {
            poJournalProposal.setApproving(psApprover);
        }
        poJournalProposal.InitTransaction();
        poJSON = poJournalProposal.OpenTransaction(getJournalTrans());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = poJournalProposal.ReturnTransaction("");
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public String getJournalTrans() throws SQLException, GuanzonException {
        String lsSQL = "SELECT sTransNox FROM Journal_Master";
        lsSQL = MiscUtil.addCondition(
                lsSQL,
                "sSourceNo = " + SQLUtil.toSQL(Master().CheckPayments().getSourceNo())
                + " ORDER BY sTransNox DESC LIMIT 1");

        System.out.println("EXECUTING SQL : " + lsSQL);
        ResultSet loRS = null;
        try {
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS != null && loRS.next()) {
                return loRS.getString("sTransNox");
            }
            return "";
        } finally {
            MiscUtil.close(loRS);
        }
    }
    public boolean hasJournalTrans() throws SQLException, GuanzonException {
        String lsSQL = "SELECT 1 FROM Journal_Master";
        lsSQL = MiscUtil.addCondition(
                lsSQL,
                "sSourceNo = " + SQLUtil.toSQL(Master().CheckPayments().getSourceNo())
                + " LIMIT 1");

        System.out.println("EXECUTING SQL : " + lsSQL);

        ResultSet loRS = null;
        try {
            loRS = poGRider.executeQuery(lsSQL);

            return loRS != null && loRS.next();
        } finally {
            MiscUtil.close(loRS);
        }
    }
    public boolean hasJournalProposal() throws SQLException, GuanzonException {
        String lsSQL = "SELECT 1 FROM Journal_Master_Proposal";
        lsSQL = MiscUtil.addCondition(
                lsSQL,
                "sSourceNo = " + SQLUtil.toSQL(Master().CheckPayments().getSourceNo())
                + " LIMIT 1");

        System.out.println("EXECUTING SQL : " + lsSQL);

        ResultSet loRS = null;
        try {
            loRS = poGRider.executeQuery(lsSQL);

            return loRS != null && loRS.next();
        } finally {
            MiscUtil.close(loRS);
        }
    }

    public JSONObject insertCheckPayment() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        CheckPayments checkTrans;
        checkTrans = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        poJSON = checkTrans.newRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        BigDecimal bd = new BigDecimal(
                cachedCheckTrans.get("nAmountxx")
                        .toString()
                        .replace(",", "")
        );

        double amount = bd.doubleValue();
        poJSON = checkTrans.getModel().setBranchCode((String) cachedCheckTrans.get("sBranchCd"));
        poJSON = checkTrans.getModel().setIndustryID((String) cachedCheckTrans.get("sIndstCdx"));
        poJSON = checkTrans.getModel().setTransactionDate(toDate(cachedCheckTrans.get("dTransact")));
        poJSON = checkTrans.getModel().setBankID((String) cachedCheckTrans.get("sBankIDxx"));
        poJSON = checkTrans.getModel().setBankAcountID((String) cachedCheckTrans.get("sBnkActID"));
        poJSON = checkTrans.getModel().setCheckDate(toDate(cachedCheckTrans.get("dCheckDte")));
        poJSON = checkTrans.getModel().setPayorID((String) cachedCheckTrans.get("sPayorIDx"));
        poJSON = checkTrans.getModel().setPayeeID((String) cachedCheckTrans.get("sPayeeIDx"));
        poJSON = checkTrans.getModel().setAmount(amount);
        poJSON = checkTrans.getModel().setSourceCode((String) cachedCheckTrans.get("sSourceCd"));
        poJSON = checkTrans.getModel().setSourceNo((String) cachedCheckTrans.get("sSourceNo"));
        poJSON = checkTrans.getModel().setLocation(CheckStatus.FLOAT);
        poJSON = checkTrans.getModel().setReleased((String) cachedCheckTrans.get("cReleased"));
        poJSON = checkTrans.getModel().setPayeeType((String) cachedCheckTrans.get("cPayeeTyp"));
        poJSON = checkTrans.getModel().setDesbursementMode((String) cachedCheckTrans.get("cDisbMode"));
        poJSON = checkTrans.getModel().setClaimant((String) cachedCheckTrans.get("cClaimant"));
        poJSON = checkTrans.getModel().setAuthorize((String) cachedCheckTrans.get("sAuthorze"));
        poJSON = checkTrans.getModel().setTransactionStatus(CheckStatus.FLOAT);
        poJSON = checkTrans.getModel().setProcessed(CheckStatus.FLOAT);
        poJSON = checkTrans.getModel().setPrint(DisbursementStatic.OPEN);
        poJSON = checkTrans.getModel().setDatePrint(null);
        poJSON = checkTrans.getModel().setModifiedDate(poGRider.getServerDate());
        poJSON = checkTrans.getModel().setModifyingId(poGRider.getUserID());

        Object crossObj = cachedCheckTrans.get("cIsCrossx");
        Object payeeObj = cachedCheckTrans.get("cIsPayeex");
        Object replObj = cachedCheckTrans.get("cIsReplcd");
        if (crossObj != null && !crossObj.toString().trim().isEmpty()) {
            boolean isCross = toBoolean(crossObj);
            poJSON = checkTrans.getModel().isCross(isCross);
        }

        if (payeeObj != null && !payeeObj.toString().trim().isEmpty()) {
            boolean isPayee = toBoolean(payeeObj);
            poJSON = checkTrans.getModel().isPayee(isPayee);
        }

        if (replObj != null && !replObj.toString().trim().isEmpty()) {
            boolean isReplaced = toBoolean(replObj);
            poJSON = checkTrans.getModel().isReplaced(isReplaced);
        }

        poJSON = checkTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private Date toDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof java.sql.Timestamp) {
            return new Date(((java.sql.Timestamp) value).getTime());
        }

        try {
            return javax.xml.bind.DatatypeConverter.parseDateTime(value.toString()).getTime();
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject updateLinkedTransactions() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        poJSON = new JSONObject();
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            switch (Detail(lnCtr).getSourceCode()) {
                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                    poJSON = updatePRF(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    if (DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(
                            Detail(lnCtr).PRF().getSourceCode())) {
                        poJSON = updatePO(lnCtr);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                    break;

                case DisbursementStatic.SourceCode.PO_RECEIVING:
                    poJSON = updatePOReceiving(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }

                    poJSON = updateCachePayable(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    break;

                case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                    poJSON = updateAPPayment(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }

                    poJSON = updateCachePayable(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    break;

                case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                    poJSON = updateSOADetail(lnCtr);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    if (!paSOATaggingMaster.contains(Detail(lnCtr).getSourceNo())) {
                        paSOATaggingMaster.add(Detail(lnCtr).getSourceNo());
                    }
                    break;
            }
        }
        //SAVE SOA TAGGING MASTER
        for (int lnCtr = 0; lnCtr <= paSOATaggingMaster.size() - 1; lnCtr++) {
            poJSON = updateSOAMaster(paSOATaggingMaster.get(lnCtr));
            if ("error".equals((String) poJSON.get("result"))) {
                poJSON.put("message", "System error while updating SOA Master.\n\n" + (String) poJSON.get("message"));
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updatePRF(int Row) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        PRFTrans.InitTransaction();
        poJSON = PRFTrans.OpenTransaction(Detail(Row).getSourceNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = PRFTrans.UpdateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        Double AmountPaid = Detail(Row).getAmountApplied();
        AmountPaid = PRFTrans.Master().getAmountPaid() - AmountPaid;

        poJSON = PRFTrans.Master().setAmountPaid(AmountPaid);
        poJSON = PRFTrans.Master().setProcess(DisbursementStatic.OPEN);
        poJSON = PRFTrans.Master().setTransactionStatus(PaymentRequestStatus.CONFIRMED);
        poJSON = PRFTrans.Master().setModifiedDate(poGRider.getServerDate());
        poJSON = PRFTrans.Master().setModifyingId(poGRider.getUserID());

        poJSON = PRFTrans.changeStatus( PRFTrans.Master().getTable(),
                                PRFTrans.Master().getTransactionNo(),
                                 "PRF was updated during Check Status Update",
                            PaymentRequestStatus.CONFIRMED,
                          false,
                              true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = PRFTrans.Master().saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }



    public JSONObject updatePO(int Row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_PO_Master POTrans;
        POTrans = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();

        poJSON = POTrans.openRecord(Detail(Row).PRF().getSourceNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = POTrans.updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        Double AmountPaid = Detail(Row).getAmountApplied() + Detail(Row).PRF().getDiscountAmount();
        AmountPaid = POTrans.getAmountPaid().doubleValue() - AmountPaid;

        poJSON = POTrans.setAmountPaid(AmountPaid);
        poJSON = POTrans.setModifiedDate(poGRider.getServerDate());
        poJSON = POTrans.setModifyingId(poGRider.getUserID());

        poJSON = POTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updatePOReceiving(int Row) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        PORTrans.InitTransaction();
        poJSON = PORTrans.OpenTransaction(Detail(Row).getSourceNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        PORTrans.setWithUI(false);
        poJSON = PORTrans.UpdateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        Double AmountPaid = Detail(Row).getAmountApplied() + Detail(Row).POReceiving().getDiscount().doubleValue();
        AmountPaid = PORTrans.Master().getAmountPaid().doubleValue() - AmountPaid;

        poJSON = PORTrans.Master().setAmountPaid(AmountPaid);
        poJSON = PORTrans.Master().setTransactionStatus(PurchaseOrderReceivingStatus.CONFIRMED);
        poJSON = PORTrans.Master().setModifiedDate(poGRider.getServerDate());
        poJSON = PORTrans.Master().setModifyingId(poGRider.getUserID());
        
        poJSON = PORTrans.changeStatus( PORTrans.Master().getTable(),
                                PORTrans.Master().getTransactionNo(),
                                 "Purchase Order Receiving was updated during Check Status Update",
                            PurchaseOrderReceivingStatus.POSTED,
                          false,
                              true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = PORTrans.Master().saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updateCachePayable(int Row) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String cachePayableTrans = getCachePayable(Row);
        
        CPTrans.InitTransaction();
        poJSON = CPTrans.OpenTransaction(cachePayableTrans);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = CPTrans.UpdateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        Double AmountPaid = Detail(Row).getAmountApplied();
        AmountPaid = CPTrans.Master().getAmountPaid() - AmountPaid;

        poJSON = CPTrans.Master().setAmountPaid(AmountPaid);
        poJSON = CPTrans.Master().setTransactionStatus(CachePayableStatus.CONFIRMED);
        poJSON = CPTrans.Master().setModifiedDate(poGRider.getServerDate());
        poJSON = CPTrans.Master().setModifyingId(poGRider.getUserID());
        
        poJSON = CPTrans.changeStatus( CPTrans.Master().getTable(),
                                CPTrans.Master().getTransactionNo(),
                                 "Cache Payable was updated during Check Status Update",
                            CachePayableStatus.CONFIRMED,
                          false,
                              true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = CPTrans.Master().saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updateAPPayment(int Row) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        
        APPaymAdjustTrans.initialize();
        poJSON = APPaymAdjustTrans.OpenTransaction(Detail(Row).getSourceNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = APPaymAdjustTrans.UpdateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        Double AmountPaid = Detail(Row).getAmountApplied() + Detail(Row).APAdjustment().getDiscountAmount().doubleValue();
        AmountPaid = APPaymAdjustTrans.getModel().getAppliedAmount().doubleValue() - AmountPaid;

        poJSON = APPaymAdjustTrans.getModel().setAppliedAmount(AmountPaid);
        poJSON = APPaymAdjustTrans.getModel().setTransactionStatus(APPaymentAdjustmentStatus.CONFIRMED);
        poJSON = APPaymAdjustTrans.getModel().setModifiedDate(poGRider.getServerDate());
        poJSON = APPaymAdjustTrans.getModel().setModifyingBy(poGRider.getUserID());
        
        poJSON = APPaymAdjustTrans.changeStatus( APPaymAdjustTrans.getModel().getTable(),
                                APPaymAdjustTrans.getModel().getTransactionNo(),
                                 "Accounts Payable Ajustment was updated during Check Status Update",
                            APPaymentAdjustmentStatus.CONFIRMED,
                          false,
                              true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = APPaymAdjustTrans.SaveTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject updateSOADetail(int Row) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        Model_AP_Payment_Detail APPaymentDetail;
        APPaymentDetail = new CashflowModels(poGRider).SOATaggingDetails();

        poJSON = APPaymentDetail.openRecord(Detail(Row).getSourceNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = APPaymentDetail.updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        Double AmountPaid = Detail(Row).getAmountApplied();
        AmountPaid = APPaymentDetail.getAppliedAmount().doubleValue() - AmountPaid;

        poJSON = APPaymentDetail.setAppliedAmount(AmountPaid);
        switch (APPaymentDetail.getSourceCode()) {
            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                poJSON = updatePRF(Row);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                if (DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(
                        Detail(Row).PRF().getSourceCode())) {
                    poJSON = updatePO(Row);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
                break;
            case DisbursementStatic.SourceCode.PO_RECEIVING:
                poJSON = updatePOReceiving(Row);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                poJSON = updateCachePayable(Row);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                break;
            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                poJSON = updateAPPayment(Row);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                poJSON = updateCachePayable(Row);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                break;
            default:
                throw new AssertionError();
        }

        poJSON = APPaymentDetail.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private JSONObject updateSOAMaster(String fsTransactioNo) throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        poJSON = new JSONObject();
        SOAMasterTrans.InitTransaction();
        Double AmountPaid = 0.0000;
        poJSON = SOAMasterTrans.OpenTransaction(fsTransactioNo);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = SOAMasterTrans.UpdateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (SOAMasterTrans.Master().getTransactionNo().equals(Detail(lnCtr).getSourceNo())
                    && DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals(Detail(lnCtr).getSourceCode())) {
                AmountPaid += Detail(lnCtr).getAmountApplied();
            }
        }
        
        
        poJSON = SOAMasterTrans.Master().setAmountPaid(AmountPaid);
        poJSON = SOAMasterTrans.Master().setTransactionStatus(SOATaggingStatus.CONFIRMED);
        poJSON = SOAMasterTrans.Master().setModifiedDate(poGRider.getServerDate());
        poJSON = SOAMasterTrans.Master().setModifyingId(poGRider.getUserID());
        
        poJSON = SOAMasterTrans.changeStatus( SOAMasterTrans.Master().getTable(),
                                SOAMasterTrans.Master().getTransactionNo(),
                                 "SOA Tagging was updated during Check Status Update",
                            SOATaggingStatus.CONFIRMED,
                          false,
                              true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = APPaymAdjustTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private static String xsDateShort(Date fdValue) {
        if (fdValue == null) {
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    public JSONObject updateAPClients() throws SQLException, GuanzonException {
        System.out.println("----------AP CLIENT MASTER----------");
        APTransaction loAPTrans = new APTransaction(poGRider, Master().getBranchCode());

        String lsClientId = Master().CheckPayments().Payee().getAPClientID();
        if (lsClientId == null || "".equals(lsClientId)) {
            lsClientId = Master().CheckPayments().Payee().getAPClientID();
        }
        poJSON = loAPTrans.PaymentIssue(lsClientId,
                "",
                Master().getTransactionNo(),
                Master().getTransactionDate(),
                Master().getNetTotal(),
                true);
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }
        System.out.println("-----------------------------------");

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;

    }

    public JSONObject updateBankAccounts() throws SQLException, GuanzonException {
        System.out.println("Master().CheckPayments().getPrint(): " + Master().CheckPayments().getPrint());
        if (CheckStatus.PrintStatus.PRINTED.equals((String) cachedCheckTrans.get("cPrintxxx"))) {
            System.out.println("----------Bank Account Transaction----------");
            //Bank Account Transaction
            BankAccountTrans poBankAccountTrans = new BankAccountTrans(poGRider);
            poJSON = poBankAccountTrans.InitTransaction();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            poJSON = poBankAccountTrans.CheckDisbursement(
                    Master().CheckPayments().getBankAcountID(),
                    Master().CheckPayments().getSourceNo(),
                    SQLUtil.toDate(xsDateShort(Master().CheckPayments().getCheckDate()), SQLUtil.FORMAT_SHORT_DATE),
                    Master().CheckPayments().getAmount(),
                    (String) cachedCheckTrans.get("sCheckNox"),
                    Master().getVoucherNo(),
                    true);
            if ("error".equals(poJSON.get("result"))) {
                return poJSON;
            }
            System.out.println("--------------------------------------------");
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject ReplaceCheck(String CheckRemarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
        }
        poGRider.beginTrans("UPDATE STATUS", "Cancel Check", SOURCE_CODE, Master().CheckPayments().getTransactionNo());

        poJSON = cancelCheckPayment(CheckRemarks);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = insertCheckPayment();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = updateBankAccounts();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction saved successfully.");
        return poJSON;
    }

    public JSONObject updateBankAccountsClearing(Date dateClear) throws SQLException, GuanzonException {

        if (CheckStatus.PrintStatus.PRINTED.equals(Master().CheckPayments().getPrint())) {
            System.out.println("----------Bank Account Transaction----------");
            //Bank Account Transaction
            BankAccountTrans poBankAccountTrans = new BankAccountTrans(poGRider);
            poJSON = poBankAccountTrans.InitTransaction();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            poJSON = poBankAccountTrans.ClearCheckIssued(Master().CheckPayments().getBankAcountID(),
                    Master().CheckPayments().getTransactionNo(),
                    dateClear,
                    Master().CheckPayments().getAmount(),
                    false);
            if ("error".equals(poJSON.get("result"))) {
                return poJSON;
            }
            System.out.println("--------------------------------------------");
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks, String CheckRemarks)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = DisbursementStatic.RETURNED_I;
        boolean lbReturn = true;

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        poJSON = isEntryOkay(DisbursementStatic.RETURNED_I);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if (pbWithUI) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
                psApprover = (String) poJSON.get("sUserIDxx");
            }
        }
        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if (hasJournalTrans()) {
            poJSON = updateJournalEntry();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        if (hasJournalProposal()) {
            poJSON = updateJournalProposal();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        

        poJSON = cancelCheckPayment(CheckRemarks);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }


        poJSON = insertCheckPayment();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = updateLinkedTransactions();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = updateBankAccounts();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = updateAPClients();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction saved successfully.");
        return poJSON;
    }

    public JSONObject ClearTransaction(String Remarks, Date dateCleared, boolean isNeedApproval) throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        poJSON = new JSONObject();
        if (isNeedApproval) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
                psApprover = (String) poJSON.get("sUserIDxx");
            }
        }
        poGRider.beginTrans("UPDATE STATUS", "Cleared Check", SOURCE_CODE, Master().CheckPayments().getTransactionNo());

        poJSON = ClearCheck(Remarks, dateCleared);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = updateBankAccountsClearing(dateCleared);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Check cleared successfully.");
        return poJSON;
    }

    private JSONObject ClearCheck(String Remarks, Date dateCleared) throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = CheckStatus.CheckState.CLEAR;
        CheckPayments checkTrans;
        checkTrans = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        poJSON = checkTrans.OpenTransaction(Master().CheckPayments().getTransactionNo(), true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = checkTrans.updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = checkTrans.getModel().setTransactionStatus(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifiedDate(poGRider.getServerDate());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifyingId(poGRider.getUserID());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = checkTrans.checkStatusChange(checkTrans.getModel().getTable(),
                (String) checkTrans.getModel().getValue("sTransNox"),
                Remarks,
                lsStatus,
                false,
                true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject HoldTransaction(Date dateCleared) throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
            setApproving((String) poJSON.get("sUserIDxx"));
            psApprover = (String) poJSON.get("sUserIDxx");
        }
        poGRider.beginTrans("UPDATE STATUS", "Cancel Check", SOURCE_CODE, Master().CheckPayments().getTransactionNo());

        poJSON = HoldCheck(dateCleared);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Check Hold successfully.");
        return poJSON;
    }

    private JSONObject HoldCheck(Date dateCleared) throws SQLException, GuanzonException, ParseException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = CheckStatus.CheckState.HOLD;
        CheckPayments checkTrans;
        checkTrans = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        poJSON = checkTrans.OpenTransaction(Master().CheckPayments().getTransactionNo(), true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = checkTrans.updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = checkTrans.getModel().setTransactionStatus(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifiedDate(poGRider.getServerDate());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.getModel().setModifyingId(poGRider.getUserID());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = checkTrans.checkStatusChange(checkTrans.getModel().getTable(),
                (String) checkTrans.getModel().getValue("sTransNox"),
                "Hold Check",
                lsStatus,
                false,
                true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = checkTrans.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public String getCachePayable(int row) throws SQLException, GuanzonException {
        String lsSQL = "SELECT sTransNox FROM Cache_Payable_Master";
        lsSQL = MiscUtil.addCondition(
                lsSQL,
                "sSourceNo = " + SQLUtil.toSQL(Detail(row).POReceiving().getTransactionNo())
                + " ORDER BY sTransNox DESC LIMIT 1");

        System.out.println("EXECUTING SQL : " + lsSQL);
        ResultSet loRS = null;
        try {
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS != null && loRS.next()) {
                return loRS.getString("sTransNox");
            }
            return "";
        } finally {
            MiscUtil.close(loRS);
        }
    }

}
