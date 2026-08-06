package ph.com.guanzongroup.cas.cashflow;

import java.sql.SQLException;
import java.util.Iterator;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cache_Payable_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cache_Payable_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;

public class CachePayable extends Transaction{   
    public JSONObject InitTransaction(){      
        SOURCE_CODE = "CcPy";
        
        poMaster = new CashflowModels(poGRider).Cache_Payable_Master();
        poDetail = new CashflowModels(poGRider).Cache_Payable_Detail();
        
        return super.initialize();
    }
    
    public JSONObject NewTransaction() throws CloneNotSupportedException{        
        return super.newTransaction();
    }
    
    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException{
        return super.saveTransaction();
    }
    
    
    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException{        
        return openTransaction(transactionNo);
    }
    
    public JSONObject UpdateTransaction(){
        return super.updateTransaction();
    }
    
    public JSONObject PaidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = CachePayableStatus.PAID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) Master().getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already paid.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(CachePayableStatus.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        

        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction paid successfully.");
        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = CachePayableStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) Master().getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already paid.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(CachePayableStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        

        //change status
        poJSON = statusChange(Master().getTable(), (String) Master().getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction paid successfully.");
        return poJSON;
    }

    public JSONObject AddDetail() throws CloneNotSupportedException{
        if (Detail(getDetailCount() - 1).getTransactionType().isEmpty() && 
            Detail(getDetailCount() - 1).getGrossAmount() == 0.00) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has insufficient detail.");
            return poJSON;
        }
        
        return addDetail();
    }
    
    /*Search Master References*/    
    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setIndustryCode(object.getModel().getIndustryId());
        }    
        
        return poJSON;
    }
    
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setBranchCode(object.getModel().getBranchCode());
        }    
        
        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setCompanyId(object.getModel().getCompanyId());
        }    
        
        return poJSON;
    }
    
    public JSONObject SearchClient(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException{
        poJSON = new JSONObject();
        poJSON.put("result", "error");
        poJSON.put("message", "Object is not yet supported. Manually pass client id first on master table.");
        
        return poJSON;
    }

    /*End - Search Master References*/
        
    @Override
    public String getSourceCode(){
        return SOURCE_CODE;
    }    
    
    @Override
    public Model_Cache_Payable_Master Master() {
        return (Model_Cache_Payable_Master) poMaster;
    }

    @Override
    public Model_Cache_Payable_Detail Detail(int row) {
        return (Model_Cache_Payable_Detail) paDetail.get(row);
    }    
        
    @Override
    public JSONObject willSave() throws SQLException, GuanzonException{
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
        }
        
        //remove items with no stockid or quantity order       
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions

            if ("".equals((String) item.getValue("sTranType"))
                    || Double.parseDouble(String.valueOf(item.getValue("nGrossAmt"))) <= 0.00) {
                detail.remove(); // Correctly remove the item
            }
        }
        
        if (getDetailCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }
        
        if (getDetailCount() == 1){
            //do not allow a single item detail with no quantity order
            if (Detail(0).getGrossAmount() == 0.00) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your detail has zero gross amount.");
                return poJSON;
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr ++){   
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNumber(lnCtr + 1);
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
    public void initSQL(){
        SQL_BROWSE = "";
    }
    
    @Override
    protected JSONObject isEntryOkay(String status){
        poJSON = new JSONObject();
        
//        if (Master().getIndustryCode().isEmpty()){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Industry must not be empty.");
//            return poJSON;
//        }
        
        if (Master().getBranchCode().isEmpty()){
            poJSON.put("result", "error");
            poJSON.put("message", "Branch must not be empty.");
            return poJSON;
        }
        
        if (Master().getCompanyId().isEmpty()){
            poJSON.put("result", "error");
            poJSON.put("message", "Company must not be empty.");
            return poJSON;
        }
        
        if (Master().getClientId().isEmpty()){
            poJSON.put("result", "error");
            poJSON.put("message", "Client must not be empty.");
            return poJSON;
        }
        
        if (Master().getClientId().isEmpty()){
            poJSON.put("result", "error");
            poJSON.put("message", "Client must not be empty.");
            return poJSON;
        }
        
//        if (Master().getBankId().isEmpty()){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Bank must not be empty.");
//            return poJSON;
//        }
        
        if (Master().getGrossAmount() == 0.00){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction gross amount is zero.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
                
        return poJSON;
    }
    /**
     * Changes the status of a transaction after validating the requested status
     * change and updates the linked transaction for the disbursement process.
     * <p>
     * This method invokes {@code statusChange()} to perform all necessary
     * validations and processing before allowing the status update. If the
     * validation fails, the corresponding error {@link org.json.JSONObject}
     * is returned immediately. Otherwise, a successful response is returned.
     * </p>
     *
     * <b>Purpose:</b> Used to check and validate status changes before updating
     * linked transactions on disbursement.
     *
     * @author TEEJEI DECELIS
     * @date Added: August 1, 2026
     *
     * @param tableName the name of the transaction table to be updated
     * @param sourceNo the source transaction number
     * @param remarks the remarks or reason associated with the status change
     * @param statusRequest the requested transaction status
     * @param needConfirmation {@code true} if the status change requires confirmation;
     *                         {@code false} otherwise
     * @param withParent {@code true} if the transaction has a parent transaction
     *                   that should also be considered during validation;
     *                   {@code false} otherwise
     * @return a {@link org.json.JSONObject} containing the operation result.
     *         Returns {@code "success"} if the status change is valid and processed;
     *         otherwise returns the validation error generated by
     *         {@code statusChange()}.
     *
     * @throws GuanzonException if a business rule or transaction validation fails
     * @throws SQLException if a database access error occurs
     * @throws CloneNotSupportedException if cloning of an internal object is not supported
     */
    public JSONObject changeStatus(String tableName, String sourceNo, String remarks, String statusRequest, boolean needConfirmation, boolean withParent)
            throws GuanzonException, SQLException, CloneNotSupportedException {
        poJSON = new  JSONObject();
        statusChange( tableName,
                sourceNo,
                remarks,
                statusRequest,
                needConfirmation,
                withParent);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
}