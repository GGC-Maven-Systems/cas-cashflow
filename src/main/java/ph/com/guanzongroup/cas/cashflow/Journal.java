package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import org.guanzon.cas.parameter.AccountChart;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.JournalStatus;

public class Journal extends Transaction {

    public JSONObject InitTransaction() {
        SOURCE_CODE = "JREn";

        poMaster = new CashflowModels(poGRider).Journal_Master();
        poDetail = new CashflowModels(poGRider).Journal_Detail();

        return super.initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return super.newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return super.saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return super.updateTransaction();
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
             if ((Detail(getDetailCount() - 1).getAccountCode() == null || "".equals(Detail(getDetailCount() - 1).getAccountCode()))
                    && (Detail(getDetailCount() - 1).getDebitAmount() == 0.00 && Detail(getDetailCount() - 1).getCreditAmount() == 0.00)) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has insufficient detail.");
                return poJSON;
            }
        }
        return addDetail();
    }
    
    public Double getTotalDebitAmount(){
        double ldblDebitAmt = 0.0000;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                ldblDebitAmt += Detail(lnCtr).getDebitAmount();
            }
        }
        return ldblDebitAmt;
    }
    
    public Double getTotalCreditAmount(){
        double ldblCreditAmt = 0.0000;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                ldblCreditAmt += Detail(lnCtr).getCreditAmount();
            }
        }
        return ldblCreditAmt;
    }

    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalStatus.CONFIRMED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(JournalStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

//        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
//
//        //change status
//        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalStatus.RETURNED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        
        //Do not re-update to return when it is already returned
        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "success");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

//        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
//
//        //change status
//        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction returning request submitted successfully.");
        }

        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalStatus.CANCELLED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(JournalStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
//        poGRider.beginTrans("UPDATE STATUS", "CancelledTransaction", SOURCE_CODE, Master().getTransactionNo());
//        //change status
//        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }
        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalStatus.VOID;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(JournalStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
//        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
//
//        //change status
//        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReopenTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalStatus.OPEN;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already reopened.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(JournalStatus.OPEN);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ReopenTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction reopened successfully.");
        } else {
            poJSON.put("message", "Transaction reopening request submitted successfully.");
        }

        return poJSON;
    }

    /*Seach Detail References*/
    public JSONObject SearchAccountCode(int row, String value, boolean byCode, String industryCode, String glCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        AccountChart object = new ParamControllers(poGRider, logwrapr).AccountChart();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode, industryCode, glCode);
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingAcctCode(row,object.getModel().getAccountCode());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            row = (int) poJSON.get("row");
            Detail(row).setAccountCode(object.getModel().getAccountCode());
        }

        return poJSON;
    }
    
    public JSONObject checkExistingAcctCode(int fnRow, String fsAcctCode){
        poJSON = new JSONObject();
        poJSON.put("row", fnRow);
        try {
            int lnRow = 0;
            for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
//                if(Detail(lnCtr).getCreditAmount() > 0.0000 || Detail(lnCtr).getDebitAmount() > 0.0000){
                if(Detail(lnCtr).isReverse()){
                    lnRow++;
                }
                if (lnCtr != fnRow) {
                    if(Detail(lnCtr).getAccountCode().equals(fsAcctCode)){
//                        if(Detail(lnCtr).getCreditAmount() <= 0.0000 && Detail(lnCtr).getDebitAmount() <= 0.0000){ 
                        if(!Detail(lnCtr).isReverse()){
//                            Model_Journal_Detail loObject = new CashflowModels(poGRider).Journal_Detail();
//                            loObject.initialize();
//                            poJSON = loObject.openRecord(Master().getTransactionNo(), lnCtr + 1);
//                            if ("error".equals((String) poJSON.get("result"))) {
//                                poJSON.put("row", fnRow);
//                                return poJSON;
//                            }
//                            
//                            if(loObject.getCreditAmount() <= 0.0000 && loObject.getDebitAmount() <= 0.0000){
//                                JSONObject loJSON = getOriginalValue(fsAcctCode);
//                                if (!"error".equals((String) loJSON.get("result"))) {
//                                    Detail(lnCtr).setCreditAmount((Double) loJSON.get("credit"));
//                                    Detail(lnCtr).setDebitAmount((Double) loJSON.get("debit"));
//                                }
//                            } else {
//                                Detail(lnCtr).setCreditAmount(loObject.getCreditAmount());
//                                Detail(lnCtr).setDebitAmount(loObject.getDebitAmount());
//                            }
//                            
//                            Detail(lnCtr).setForMonthOf(poGRider.getServerDate());
                            
                            Detail(lnCtr).isReverse(true);
                            Detail(lnCtr).setCreditAmount(0.0000);
                            Detail(lnCtr).setDebitAmount(0.0000);
                            Detail(lnCtr).setForMonthOf(poGRider.getServerDate());
                            poJSON.put("row", lnCtr);
                            break;
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", Detail(lnCtr).Account_Chart().getDescription() + " already exists at row "+ lnRow );
                            poJSON.put("row", lnCtr);
                            return poJSON;
                        }
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    private JSONObject getOriginalValue(String fsAcctCode){
        poJSON = new JSONObject();
        double ldblDebit = 0.0000;
        double ldblCredit = 0.0000;

        try {
            String lsSQL = "SELECT sPayLoadx FROM xxxauditlogdetail ";
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sPayLoadx LIKE " + SQLUtil.toSQL(
                            "%sAcctCode%" + Master().getTransactionNo() + "%" + fsAcctCode + "%")
                    + " AND sQryTypex = 'INSERT' "
                    + " AND sTableNme = 'Journal_Detail' "
            );

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            String lsPayload = "";

            if (loRS.next()) {
                lsPayload = loRS.getString("sPayLoadx");
            }

            MiscUtil.close(loRS);

            if (!lsPayload.isEmpty()) {
                // ==============================
                // STEP 1: Parse JSON
                // ==============================
                JSONParser parser = new JSONParser();
                JSONObject loJson = (JSONObject) parser.parse(lsPayload);
                String sql = (String) loJson.get("data");
                // ==============================
                // STEP 2: Extract column list
                // ==============================
                String columnPart = sql.substring(
                        sql.indexOf("Journal_Detail(") + "Journal_Detail(".length(),
                        sql.indexOf("VALUES")
                ).replace("(", "").replace(")", "").trim();

                String valuePart = sql.substring(
                        sql.indexOf("VALUES (") + 8,
                        sql.lastIndexOf(")")
                );

                // ==============================
                // STEP 3: Split into arrays
                // ==============================
                String[] columns = columnPart.split(",");
                String[] values = valuePart.split(",");

                // ==============================
                // STEP 4: Find indexes dynamically
                // ==============================
                int lnDebitIndex = -1;
                int lbCreditIndex = -1;

                for (int i = 0; i < columns.length; i++) {
                    String col = columns[i].trim();

                    if (col.equalsIgnoreCase("nDebitAmt")) {
                        lnDebitIndex = i;
                    }

                    if (col.equalsIgnoreCase("nCredtAmt")) {
                        lbCreditIndex = i;
                    }
                }

                // ==============================
                // STEP 5: Extract values safely
                // ==============================
                if (lnDebitIndex >= 0 && lnDebitIndex < values.length) {
                    ldblDebit = Double.parseDouble(
                            values[lnDebitIndex].replaceAll("[^0-9.\\-]", "").trim()
                    );
                }

                if (lbCreditIndex >= 0 && lbCreditIndex < values.length) {
                    ldblCredit = Double.parseDouble(
                            values[lbCreditIndex].replaceAll("[^0-9.\\-]", "").trim()
                    );
                }
            }

            // ==============================
            // RESULT
            // ==============================
            poJSON.put("result", "success");
            poJSON.put("message", "success");
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName())
                    .log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }

        poJSON.put("debit", ldblDebit);
        poJSON.put("credit", ldblCredit);
        return poJSON;
    }

    /*Search Master References*/
    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryCode(object.getModel().getIndustryId());
        }

        return poJSON;
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

    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyId(object.getModel().getCompanyId());
        }

        return poJSON;
    }

    public JSONObject SearchClient(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("result", "error");
        poJSON.put("message", "Object is not yet supported. Manually pass client id first on master table.");

        return poJSON;
    }

    /*End - Search Master References*/
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Journal_Master Master() {
        return (Model_Journal_Master) poMaster;
    }

    @Override
    public Model_Journal_Detail Detail(int row) {
        return (Model_Journal_Detail) paDetail.get(row);
    }

    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }
    
    public void ReloadDetail() throws CloneNotSupportedException, SQLException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getAccountCode() == null || "".equals(Detail(lnCtr).getAccountCode())) {
                Detail().remove(lnCtr);
            } else {
//                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    if(Detail(lnCtr).getDebitAmount() <= 0.0000
//                        && Detail(lnCtr).getCreditAmount() <= 0.0000){
//                        Detail().remove(lnCtr);
//                    }
//                }
            }
            lnCtr--;
        }
        if ((getDetailCount() - 1) >= 0) {
            if (Detail(getDetailCount() - 1).getAccountCode() != null && !"".equals(Detail(getDetailCount() - 1).getAccountCode())){
                if(((Detail(getDetailCount() - 1).getDebitAmount() <= 0.0000 && Detail(getDetailCount() - 1).getCreditAmount() <= 0.0000)
                    && Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE)
                    || 
                    ((Detail(getDetailCount() - 1).getDebitAmount() > 0.0000 || Detail(getDetailCount() - 1).getCreditAmount() > 0.0000)
                    && (Detail(getDetailCount() - 1).getEditMode() == EditMode.ADDNEW || Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE))){
                    AddDetail();
                    Detail(getDetailCount() - 1).setForMonthOf(Master().getTransactionDate());
                } 
            }  
        }
        if ((getDetailCount() - 1) < 0) {
            AddDetail();
            Detail(getDetailCount() - 1).setForMonthOf(Master().getTransactionDate());
        }
    
    }

    @Override
    public JSONObject willSave() throws SQLException, GuanzonException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
        }
        
        String lsDebitAmt = "";
        String lsCreditAmt = "";
        //remove items with no stockid or quantity order
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions

            if (item.getValue("nDebitAmt") != null && !"".equals(item.getValue("nDebitAmt"))) {
                lsDebitAmt = item.getValue("nDebitAmt").toString();
            }
            if (item.getValue("nCredtAmt") != null && !"".equals(item.getValue("nCredtAmt"))) {
                lsCreditAmt = item.getValue("nCredtAmt").toString();
            }

            if ((("".equals((String) item.getValue("sAcctCode")) || (String) item.getValue("sAcctCode") == null)
                    || (Double.valueOf(lsDebitAmt) <= 0.00 && Double.valueOf(lsCreditAmt) <= 0.00))
                && item.getEditMode() == EditMode.ADDNEW){
                detail.remove(); // Correctly remove the item
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNumber(lnCtr + 1);
        }

        Master().setEntryNumber(getDetailCount());

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getDebitAmount() == 0.00 && Detail(0).getCreditAmount() == 0.00) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your detail has zero debit and credit amount.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(TransactionStatus.STATE_OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "";
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        poJSON = new JSONObject();

//        if (Master().getIndustryCode().isEmpty()){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Industry must not be empty.");
//            return poJSON;
//        }
        if (Master().getCompanyId().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Company must not be empty.");
            return poJSON;
        }

//        if (Master().getAccountPerId().isEmpty()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Account per ID must not be empty.");
//            return poJSON;
//        }
        if (Master().getBranchCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Branch must not be empty.");
            return poJSON;
        }

        poJSON.put("result", "success");

        return poJSON;
    }
}
