package ph.com.guanzongroup.cas.cashflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Detail_Proposal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master_Proposal;
import org.guanzon.cas.parameter.AccountChart;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.JournalProposalStatus;
import ph.com.guanzongroup.cas.cashflow.status.JournalProposalStatus;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;

public class JournalProposal extends Transaction {
    public List<Model> paMaster;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psDepartmentName = "";
    public JSONObject InitTransaction() {
        SOURCE_CODE = "JREp";

        poMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
        poDetail = new CashflowModels(poGRider).Journal_Detail_Proposal();

        paMaster = new ArrayList<Model>();
        return super.initialize();
    }
    public void setIndustryId(String fsValue){ psIndustryId = fsValue; }
    public void setCompanyId(String fsValue){ psCompanyId = fsValue; }
    public void setSearchDepartmentName(String fsValue){
        psDepartmentName = fsValue;
    }
    public String getSearchDepartmentName(){
        return psDepartmentName;
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

    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalProposalStatus.CONFIRMED;

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
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    public JSONObject PostTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalProposalStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false,pbWthParent);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction posted successfully.");
        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalProposalStatus.RETURNED;
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
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalProposalStatus.CANCELLED;
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
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");
        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = JournalProposalStatus.VOID;
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
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
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
    
    public JSONObject SearchTransaction(String fsDepartmentName, String fsTransactionNo) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                    "  a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.sCompnyCd = " + SQLUtil.toSQL(psCompanyId)
                    + " AND e.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartmentName)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo));
        String lsCondition = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
             lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }
        lsSQL = lsSQL + " GROUP BY a.sTransNox "; // ORDER BY a.dTransact, a.sTransNox, e.sDeptName ASC
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»Source No»Deparment",
                "a.sTransNox»a.dTransact»a.sSourceNo»sDeptName",
                "a.sTransNox»a.dTransact»a.sSourceNo»e.sDeptName",
                0);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
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
                        if(!Detail(lnCtr).isReverse()){
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
    
    public JSONObject loadTransactionList(String fsDepartmentName, String fsTransactionNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                    "  a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.sCompnyCd = " + SQLUtil.toSQL(psCompanyId)
                    + " AND e.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartmentName)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo));
        String lsCondition = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
             lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }
        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact, a.sTransNox, e.sDeptName ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Journal_Master_Proposal loObject = new CashflowModels(poGRider).Journal_Master_Proposal();
            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loObject);
            } else {
                return poJSON;
            }
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    
    /**
    * Gets the full list of Journal_Master_Proposal records.
    *
    * @return list of Disbursement Master models
    */
    public List<Model_Journal_Master_Proposal> getTransactionList() {
        return (List<Model_Journal_Master_Proposal>) (List<?>) paMaster;
    }

    /**
    * Gets a specific Journal_Master_Proposal record by index.
    *
    * @param masterRow index of the master record
    * @return Journal_Master_Proposal model
    */
    public Model_Journal_Master_Proposal TransactionList(int masterRow) {
        return (Model_Journal_Master_Proposal) paMaster.get(masterRow);
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
        object.setRecordStatus(RecordStatus.ACTIVE);
        if(psCompanyId == null || "".equals(psCompanyId)){
            psCompanyId = poGRider.getCompnyId();
        }
       
        object.setCompanyId(psCompanyId);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))){
             Master().setBranchCode(object.getModel().getBranchCode());
        }    
       
        return poJSON;
    }

    public JSONObject SearchDepartment(String value, boolean byCode, boolean fbSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(fbSearch){
                psDepartmentName = object.getModel().getDescription();
            } else {
                Master().setDepartmentId(object.getModel().getDepartmentId());
            }
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
    
    public String getSourceDesc(){
        switch(Master().getSourceCode()){
            case JournalProposalStatus.SourceCode.DISBURSEMENT_VOUCHER:
                return "Dibursement Voucher";
            case JournalProposalStatus.SourceCode.CASH_DISBURSEMENT:
                return "Cash Dibursement";
        }
        return "";
    }

    @Override
    public Model_Journal_Master_Proposal Master() {
        return (Model_Journal_Master_Proposal) poMaster;
    }

    @Override
    public Model_Journal_Detail_Proposal Detail(int row) {
        return (Model_Journal_Detail_Proposal) paDetail.get(row);
    }

    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
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
    
//    public void ReloadDetail() throws CloneNotSupportedException, SQLException{
//        int lnCtr = getDetailCount() - 1;
//        while (lnCtr >= 0) {
//            if (Detail(lnCtr).getAccountCode() == null || "".equals(Detail(lnCtr).getAccountCode())) {
//                Detail().remove(lnCtr);
//            } else {
//            }
//            lnCtr--;
//        }
//        if ((getDetailCount() - 1) >= 0) {
//            if (Detail(getDetailCount() - 1).getAccountCode() != null && !"".equals(Detail(getDetailCount() - 1).getAccountCode())){
//                if(((Detail(getDetailCount() - 1).getDebitAmount() <= 0.0000 && Detail(getDetailCount() - 1).getCreditAmount() <= 0.0000)
//                    && Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE)
//                    || 
//                    ((Detail(getDetailCount() - 1).getDebitAmount() > 0.0000 || Detail(getDetailCount() - 1).getCreditAmount() > 0.0000)
//                    && (Detail(getDetailCount() - 1).getEditMode() == EditMode.ADDNEW || Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE))){
//                    AddDetail();
//                    Detail(getDetailCount() - 1).setForMonthOf(Master().getTransactionDate());
//                } 
//            }  
//        }
//        if ((getDetailCount() - 1) < 0) {
//            AddDetail();
//            Detail(getDetailCount() - 1).setForMonthOf(Master().getTransactionDate());
//        }
//    
//    }
    
    /** 
    * Cleans journal details by removing invalid or empty entries,
    * ensures at least one valid row exists, and adds a new detail row
    * when the last entry is valid and contains amounts.
    *
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    */
    public void ReloadDetail() throws CloneNotSupportedException, SQLException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getAccountCode() == null || "".equals(Detail(lnCtr).getAccountCode())) {
                Detail().remove(lnCtr);
            } else {
//                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    if(Detail(lnCtr).getDebitAmount() <= 0.0000 && Detail(lnCtr).getCreditAmount() <= 0.0000){
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
    
    /**
     * Requests approval if the current user lacks sufficient rights and
     * validates the approving officer.
     *
     * @return result as a {@link JSONObject}
     */
    public JSONObject callApproval(){
        poJSON = new JSONObject();
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer.");
                return poJSON;
            }
            setApproving((String) poJSON.get("sUserIDxx"));
//            psApprover = (String) poJSON.get("sUserIDxx");
        }   
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
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
        SQL_BROWSE = " SELECT 	" +
                    "   a.sTransNox,  " +
                    "	a.sIndstCdx,  " +
                    "	a.sCompnyCd,  " +
                    "	a.dTransact,  " +
                    "	a.sRemarksx,  " +
                    "	a.sActPerID,  " +
                    "	a.sBranchCd,  " +
                    "	a.sDeptIDxx,  " +
                    "	a.sSourceCD,  " +
                    "	a.sSourceNo,  " +
                    "	a.nEntryNox,  " +
                    "	a.cTranStat, " +
                    "	b.sDescript AS sIndustry, " +
                    "	c.sCompnyNm AS sCompnyNm, " +
                    "	d.sBranchNm AS sBranchNm, " +
                    "	e.sDeptName AS sDeptName " +
                    "	FROM Journal_Master_Proposal a " +
                    "	LEFT JOIN Industry b ON b.sIndstCdx = a.sIndstCdx	 " +
                    "	LEFT JOIN Company c ON c.sCompnyID = a.sCompnyCd	 " +
                    "	LEFT JOIN Branch d ON d.sBranchCd = a.sBranchCd	 " +
                    "	LEFT JOIN Department e ON e.sDeptIDxx = a.sDeptIDxx	 " ;
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
        if (Master().getDepartmentId().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Department must not be empty.");
            return poJSON;
        }

        poJSON.put("result", "success");

        return poJSON;
    }
    
    /**
    * Validates journal entries including debit/credit balance,
    * account code presence, and valid reporting dates.
    *
    * @return JSON validation result with continue flag
    */
    public JSONObject validateJournalProposal(){
        poJSON = new JSONObject();
        poJSON.put("continue", false);
        
        double ldblCreditAmt = 0.0000;
        double ldblDebitAmt = 0.0000;
        boolean lbHasJournal = false;
        boolean lbValidateJournal = false;
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            if(Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                ldblDebitAmt += Detail(lnCtr).getDebitAmount();
                ldblCreditAmt += Detail(lnCtr).getCreditAmount();

                if(Detail(lnCtr).getCreditAmount() > 0.0000 ||  Detail(lnCtr).getDebitAmount() > 0.0000){
                    if(Detail(lnCtr).getAccountCode() != null && !"".equals(Detail(lnCtr).getAccountCode())){
                        if(Detail(lnCtr).getForMonthOf() == null || "1900-01-01".equals(xsDateShort(Detail(lnCtr).getForMonthOf()))){
                            poJSON.put("result", "error");
                            poJSON.put("message", "Invalid reporting date of journal at row "+(lnCtr+1)+" for Journal Proposal <"+ Master().getTransactionNo()+">.");
                            return poJSON;
                        }
                    }
                }
                
                if(!lbValidateJournal){
                    lbValidateJournal = Detail(lnCtr).getAccountCode() != null && !"".equals(Detail(lnCtr).getAccountCode());
                } 
            }
            
            if(!lbHasJournal){
                lbHasJournal = Detail(lnCtr).getAccountCode() != null && !"".equals(Detail(lnCtr).getAccountCode());
            }   
        }
        
        if(lbValidateJournal){
            //Convert debit and credit amount
            ldblDebitAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblDebitAmt, true).replace(",", ""));
            ldblCreditAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblCreditAmt, true).replace(",", ""));
            
            if(ldblDebitAmt == 0.0000 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry debit amount for Journal Proposal <"+ Master().getTransactionNo()+">.");
                return poJSON;
            }

            if(ldblCreditAmt == 0.0000){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry credit amount for Journal Proposal <"+ Master().getTransactionNo()+">.");
                return poJSON;
            }

            if(ldblDebitAmt < ldblCreditAmt || ldblDebitAmt > ldblCreditAmt){
                poJSON.put("result", "error");
                poJSON.put("message", "Debit should be equal to credit amount for Journal Proposal <"+ Master().getTransactionNo()+">.");
                return poJSON;
            }

    //        if(ldblDebitAmt < Master().getTransactionTotal().doubleValue() || ldblDebitAmt > Master().getTransactionTotal().doubleValue()){
    //            poJSON.put("result", "error");
    //            poJSON.put("message", "Debit and credit amount should be equal to transaction total.");
    //            return poJSON;
    //        }
        }
        
        
        poJSON.put("result", "sucess");
        poJSON.put("message", "sucess");
        poJSON.put("continue", lbHasJournal);
        return poJSON;
    }
    
    /*Convert Date to String*/
    /**
    * Converts a Date to a short string format (yyyy-MM-dd).
    * Returns default value if input is null.
    *
    * @param fdValue date value
    * @return formatted date string
    */
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case JournalProposalStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case JournalProposalStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case JournalProposalStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case JournalProposalStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case JournalProposalStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case JournalProposalStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case JournalProposalStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case JournalProposalStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case JournalProposalStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case JournalProposalStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case JournalProposalStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case JournalProposalStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                    }
            }
            crs.updateRow(); 
        }
        
        JSONObject loJSON  = getEntryBy();
        String entryBy = "";
        String entryDate = "";
        
        if ("success".equals((String) loJSON.get("result"))){
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }
        
        showStatusHistoryUI("Journal Proposal", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM "+Master().getTable()+" a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsEntry = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsEntryDate =  dModified.format(formatter);
                }
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
          return poJSON;
        } 
        
        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL =   " SELECT b.sCompnyNm from xxxSysUser a " 
                       + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId)) ;
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                lsEntry = loRS.getString("sCompnyNm");
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
        } 
        return lsEntry;
    }
    
}
