/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.client.AP_Client_Bank_Account;
import org.guanzon.cas.parameter.Banks;
import org.guanzon.cas.parameter.model.Model_Banks;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.cas.core.GLTransaction;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Deposit_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Deposit_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CashDisbursementStatus;
import ph.com.guanzongroup.cas.cashflow.status.CheckDepositStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;
import ph.com.guanzongroup.cas.cashflow.validator.CheckDepositValidatorFactory;

/**
 *
 * @author Arsiela
 */
public class CheckDeposit extends Transaction {
    private String psCompanyId = "";
    private String psSearchBankId = "";
    private String psSearchBankAcctNo = "";
    private String psApprover = "";
    private boolean pbSupplier = false;
    private boolean pbPrint = false;
    
    List<Model_Disbursement_Master> paChecks;
    List<Model_Check_Deposit_Master> paMaster;
    public List<TransactionAttachment> paAttachments;
    public Journal poJournal;
    
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "Ckdp";

        poMaster = new CashflowModels(poGRider).CheckDepositMaster();
        poDetail = new CashflowModels(poGRider).CheckDepositDetail();
        poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
        
        paDetail = new ArrayList<>();
        paMaster = new ArrayList<>();
        paChecks = new ArrayList<>();
        paAttachments = new ArrayList<>();

        return initialize();
    }
    
    public JSONObject NewTransaction() throws CloneNotSupportedException {
        resetMaster();
        Detail().clear();
        resetJournal();
        
        poJSON = newTransaction();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        return poJSON;
    }

    public JSONObject SaveTransaction() throws SQLException, CloneNotSupportedException, GuanzonException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        //Reset Transaction
        resetTransaction();
        
        poJSON = openTransaction(transactionNo);
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateJournal();
        if (!isJSONSuccess(poJSON)) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        poJSON = setJSON("success","success");
        return poJSON;
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = updateTransaction();
       if (!isJSONSuccess(poJSON)) {
           poJSON = setJSON((String) poJSON.get("result"),"Unable to update transaction.\n" + (String) poJSON.get("message"));
           return poJSON;
       }

        poJSON = populateJournal();
        if (!isJSONSuccess(poJSON)) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"),"Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

       poJSON = setJSON("success","success");
       return poJSON;
    }
    
    //************************UPDATE TRANSACTION STATUS***************************************
    
    public JSONObject ConfirmTransaction() throws  SQLException, GuanzonException, CloneNotSupportedException, ParseException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = CheckDepositStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error","No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON = setJSON("error","Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //check JE
        if(!pbSupplier){ //Check Deposit from Inter Branch
            if(poJournal == null){
                poJSON = setJSON("error","Please review journal entry before posting.");
                return poJSON;
            } else {
                switch(poJournal.getEditMode()){
                    case EditMode.ADDNEW:
                    case EditMode.UPDATE:
                    break;
                    case EditMode.READY:
                        if(poJournal.Master().getTransactionNo() == null || "".equals(poJournal.Master().getTransactionNo())){
                            poJSON = setJSON("error","Please review journal entry before posting.");
                            return poJSON; 
                        }
                    break;
                    default:
                        poJSON = setJSON("error","Please review journal entry before posting.");
                        return poJSON;
                }
            }

            poJSON = validateJournal();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        poJSON = callApproval();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        poJSON = updateCheckPayments(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = updateRelatedTransactions(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success","Transaction confirmed successfully.");
        return poJSON;
    }
    
    public JSONObject VoidTransaction() throws  SQLException, GuanzonException, CloneNotSupportedException, ParseException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = CheckDepositStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error","No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON = setJSON("error","Transaction was already void.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        poJSON = updateCheckPayments(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = updateRelatedTransactions(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success","Transaction voided successfully.");
        return poJSON;
    }
    
    public JSONObject CancelTransaction() throws  SQLException, GuanzonException, CloneNotSupportedException, ParseException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = CheckDepositStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error","No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON = setJSON("error","Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poJSON = callApproval();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        poJSON = updateCheckPayments(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = updateRelatedTransactions(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success","Transaction cancelled successfully.");
        return poJSON;
    }
    
    public JSONObject PostTransaction() throws  SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = CheckDepositStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error","No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON = setJSON("error","Transaction was already posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //check JE
        if(!pbSupplier){ //Check Deposit from Inter Branch
            if(poJournal == null){
                poJSON = setJSON("error","Please review journal entry before posting.");
                return poJSON;
            } else {
                switch(poJournal.getEditMode()){
                    case EditMode.ADDNEW:
                    case EditMode.UPDATE:
                    break;
                    case EditMode.READY:
                        if(poJournal.Master().getTransactionNo() == null || "".equals(poJournal.Master().getTransactionNo())){
                            poJSON = setJSON("error","Please review journal entry before posting.");
                            return poJSON; 
                        }
                    break;
                    default:
                        poJSON = setJSON("error","Please review journal entry before posting.");
                        return poJSON;
                }
            }

            poJSON = validateJournal();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        poJSON = callApproval();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        poJSON = updateCheckPayments(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        try {
            System.out.println("----------SAVE BANK ACCOUNT TRANSACTION FOR CHECK DEPOSIT----------");
//            for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
//                if(Detail(lnCtr).isReverse()){
//                    
//                }
//            }

            if(!pbSupplier){
                BankAccountTrans poBankAccountTrans = new BankAccountTrans(poGRider);
                poJSON = poBankAccountTrans.InitTransaction();
                if (!isJSONSuccess(poJSON)) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }

                poJSON = poBankAccountTrans.CheckDeposit(
                    Master().getBankAccount(),
                        Master().getTransactionNo(),
                   SQLUtil.toDate(xsDateShort(Master().getTransactionDate()), SQLUtil.FORMAT_SHORT_DATE),
                         Master().getTransactionTotalDeposit(),
                         false);
                if ("error".equals(poJSON.get("result"))) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }

                System.out.println("--------------------------------------------");

                System.out.println("----------ACCOUNT MASTER / LEDGER----------");
                    //GL Transaction Account Ledger
                    GLTransaction loGLTrans = new GLTransaction(poGRider, poGRider.getBranchCode()); //Get the branch code of the posting branch since check payments is on the detail and possible for multiple branch
                    loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());
                    for (int lnCtr = 0; lnCtr <= Journal().getDetailCount() - 1; lnCtr++) {
                        if(Journal().Detail(lnCtr).isReverse()){
                            loGLTrans.addDetail(Journal().Master().getBranchCode(),
                                    Journal().Detail(lnCtr).getAccountCode(),
                                    SQLUtil.toDate(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE),
                                    Journal().Detail(lnCtr).getDebitAmount(),
                                    Journal().Detail(lnCtr).getCreditAmount());
                        }
                    }
                    loGLTrans.saveTransaction();
                System.out.println("------------------------------------------------------");
            }
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON = setJSON("error",MiscUtil.getException(ex));
            return poJSON;
        }
        
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success","Transaction posted successfully.");
        return poJSON;
    }
    
    private JSONObject updateCheckPayments(String fsStatus) throws GuanzonException, SQLException, CloneNotSupportedException {
        poJSON = new JSONObject();
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if(Detail(lnCtr).getSourceNo() != null && !"".equals(Detail(lnCtr).getSourceNo())){
                CheckPayments loObject = new CashflowControllers(poGRider,logwrapr).CheckPayments();
                loObject.initialize();
                poJSON = loObject.openRecord(Detail(lnCtr).getSourceNo());
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                poJSON = loObject.updateRecord();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
                
                if(Detail(lnCtr).isReverse()){
                    // Set updated values
                    switch (fsStatus) {
                        case CheckDepositStatus.CONFIRMED:
                            loObject.getModel().setLocation("4");
                            break;
                        case CheckDepositStatus.POSTED:
                            loObject.getModel().setLocation("2");
                            loObject.getModel().setReleased("1");
                            break;    
                        case CheckDepositStatus.VOID:
                        case CheckDepositStatus.CANCELLED:
                            loObject.getModel().setLocation("1");
                            loObject.getModel().setReleased("0");
                            break;
                    }
                } else {
                    //Set to previous value if check was reversed (-)
                    loObject.getModel().setLocation("1");
                    loObject.getModel().setReleased("0");
                }

                loObject.getModel().setModifyingId(poGRider.getUserID());
                loObject.getModel().setModifiedDate(poGRider.getServerDate());
                loObject.setWithParentClass(true);
                loObject.setWithUI(false);
                poJSON = loObject.saveRecord();
                if (!isJSONSuccess(poJSON)) {
                    return poJSON;
                }
            }
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "success.");
        return poJSON;
    }
    
    public JSONObject updateRelatedTransactions(String fsStatus) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(!pbSupplier){
            String lsJournal = existJournal();
            if(lsJournal != null && !"".equals(lsJournal)){
                poJournal.setWithParent(true);
                poJournal.setWithUI(false);
                if(psApprover == null || "".equals(psApprover)){
                    psApprover = poGRider.getUserID();
                }
                poJournal.setApproving(psApprover);
                //Update Journal
                switch(fsStatus){
                    case CheckDepositStatus.CONFIRMED:
                        //Confirm Journal
                        poJSON = poJournal.ConfirmTransaction("");
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                        break;
                    case CheckDepositStatus.VOID:
                        //Void Journal
                        poJSON = poJournal.VoidTransaction("");
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }

                        break;
                    case CheckDepositStatus.CANCELLED:
                        //Cancel Journal
                        poJSON = poJournal.CancelTransaction("");
                        if (!isJSONSuccess(poJSON)) {
                            return poJSON;
                        }
                        break;
                }
            }
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
   
    //******************************GETTERS AND DETAILS***************************************
    
    public void setCompanyId(String fsCompanyId){ psCompanyId = fsCompanyId; }
    public void setPrint(boolean isPrint) { pbPrint = isPrint; }
    public void isCheckDepositSupplier(boolean fbIsCheckDepSupplier){ pbSupplier = fbIsCheckDepSupplier; }
    public void setSearchBank(String bank) { psSearchBankId = bank; }
    public String getSearchBankAccountNo() { return psSearchBankAcctNo; }
    public void setSearchBankAccountNo(String fsAccountNo) { psSearchBankAcctNo = fsAccountNo; }
    public String getSearchBank(){
        try { 
            poJSON = new JSONObject();
            if(psSearchBankId != null && !"".equals(psSearchBankId)){
                Model_Banks loObj = new ParamModels(poGRider).Banks();
                loObj.initialize();
                poJSON = loObj.openRecord(psSearchBankId);
                if(isJSONSuccess(poJSON)){
                    return loObj.getBankName();
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return "";
        }
        return "";
    }
    public String getCompanyName(){
        try { 
            poJSON = new JSONObject();
            String lsCompanyId = poGRider.getCompnyId();
            if(psCompanyId != null && !"".equals(psCompanyId)){
                lsCompanyId = psCompanyId;
            }
            Model_Company loObj = new ParamModels(poGRider).Company();
            loObj.initialize();
            poJSON = loObj.openRecord(lsCompanyId);
            if(isJSONSuccess(poJSON)){
                return loObj.getCompanyName();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return "";
        }
        return "";
    }
    
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getSourceNo().isEmpty()) {
                poJSON = new JSONObject();
                poJSON = setJSON("error","Last row has empty item.");
                return poJSON;
            }
        }
        return addDetail();
    }
   
    /**
     * Refines and validates the transaction detail list.
     * 
     * This method prunes invalid rows (those with empty particulars or zero amounts for new records) 
     * and automatically appends a new detail row if the list is empty or the last entry is valid.
     * 
     * @throws CloneNotSupportedException If an error occurs while adding a new detail row.
     */
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getSourceNo() == null || "".equals(Detail(lnCtr).getSourceNo())) {
                deleteDetail(lnCtr);
            } 
            lnCtr--;
        }
            
        if ((getDetailCount() - 1) >= 0) {
            if (Detail(getDetailCount() - 1).getSourceNo() != null && !"".equals(Detail(getDetailCount() - 1).getSourceNo())) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
    
    public void ReloadJournal() throws CloneNotSupportedException, SQLException{
        int lnCtr = Journal().getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Journal().Detail(lnCtr).getAccountCode() == null || "".equals(Journal().Detail(lnCtr).getAccountCode())) {
                Journal().Detail().remove(lnCtr);
            } else {
                if(Journal().Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    if(Journal().Detail(lnCtr).getDebitAmount() <= 0.0000
                        && Journal().Detail(lnCtr).getCreditAmount() <= 0.0000){
                        Journal().Detail().remove(lnCtr);
                    }
                }
            }
            lnCtr--;
        }
        if ((Journal().getDetailCount() - 1) >= 0) {
            if (Journal().Detail(getDetailCount() - 1).getAccountCode() != null && !"".equals(Journal().Detail(getDetailCount() - 1).getAccountCode())
                && (Journal().Detail(getDetailCount() - 1).getDebitAmount() > 0.0000 || Journal().Detail(getDetailCount() - 1).getCreditAmount() > 0.0000)) {
                Journal().AddDetail();
                Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
            }
        }
        if ((Journal().getDetailCount() - 1) < 0) {
            Journal().AddDetail();
            Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
        }
    
    }
    
    /**
    * Clears all detail rows, withholding tax deductions, and resets transaction-related fields.
    * 
    * This method removes all items from the details and tax deduction lists, clears attachments, 
    * resets journal entries, and clears master industry and search metadata to return to an initial state.
    * 
    * @return A JSONObject indicating "success" after all components have been cleared.
    */
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }
        
        //Reset Journal when all details was removed
        resetJournal();
        paAttachments = new ArrayList<>();
        initFields();
        
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject deleteDetail(int rowNumber) {
        return super.deleteDetail(rowNumber);
    }
    
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Check_Deposit_Master Master() {
        return (Model_Check_Deposit_Master) poMaster;
    }

    @Override
    public Model_Check_Deposit_Detail Detail(int row) {
        return (Model_Check_Deposit_Detail) paDetail.get(row);
    }
    
    private Model_Check_Deposit_Master TransactionList() {
        return new CashflowModels(poGRider).CheckDepositMaster();
    }
    public int getTransactionListCount() {
        if (paMaster == null) {
            return 0;
        }
        return paMaster.size();
    }

    public Model_Check_Deposit_Master TransactionList(int row) {
        return (Model_Check_Deposit_Master) paMaster.get(row);
    }
    
    private Model_Disbursement_Master ChecksPaymentList() {
        return new CashflowModels(poGRider).DisbursementMaster();
    }
    
    public Model_Disbursement_Master ChecksPaymentList(int row) {
        return (Model_Disbursement_Master) paChecks.get(row);
    }

    public int getCheckPaymentCount() {
        if (paChecks == null) {
            return 0;
        }
        return paChecks.size();
    }
    
    public Journal Journal(){
        try{
            if (poJournal == null) {
                poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
                poJournal.InitTransaction();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poJournal;
    }
    
    /*RESET CACHE ROW SET*/
    /**
     * Resets the master record to its default initial state.
     */
    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).CheckDepositMaster();
    }
    
    /**
     * Resets the other record to its default initial state.
     */
    public void resetOthers() {
        paAttachments = new ArrayList<>();
    }
    
    public void resetJournal() {
        try {
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Completely clears the current transaction state.
     * 
     * Resets the master model, clears all detail and attachment collections, 
     * and wipes the industry and payee search filters.
     */
    public void resetTransaction(){
        resetMaster();
        resetJournal();
        Detail().clear();
        paAttachments = new ArrayList<>();
        psApprover = "";
        setApproving("");
        pbPrint = false;
    }
    
    /*Search References*/
    public JSONObject SearchBanks(String value, boolean byCode, boolean fbSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Banks object = new ParamControllers(poGRider, logwrapr).Banks();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(fbSearch){
                psSearchBankId = object.getModel().getBankID();
            } else {
                Master().setBanks(object.getModel().getBankID());
            }
        }

        return poJSON;
    }
    
    public JSONObject SearchBankAccount(String value, boolean byCode, boolean fbSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        if(pbSupplier){
            AP_Client_Bank_Account loObj = new AP_Client_Bank_Account();
            loObj.setApplicationDriver(poGRider);
            loObj.initialize();
            loObj.setRecordStatus(RecordStatus.ACTIVE);
            
            if(fbSearch){
                poJSON = loObj.searchRecord(value, byCode);
            } else {
                if( Master().getBanks() == null || "".equals(Master().getBanks() )){
                    poJSON = loObj.searchRecord(value, byCode);
                } else {
                    poJSON = loObj.searchRecordbyBanks(value,Master().getBanks(),byCode);
                }
            }
            if ("success".equals((String) poJSON.get("result"))) {
                if(fbSearch){
                    psSearchBankAcctNo = loObj.getModel().getAccountNumber();
                } else {
                    Master().setBankAccount(loObj.getModel().getAPClientBankID());   
                }
            }
        } else {
            BankAccountMaster object = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
            object.setRecordStatus(RecordStatus.ACTIVE);
            object.setCompanyId(psCompanyId);
            
            if(fbSearch){
                poJSON = object.searchRecord(value, byCode);
            } else {
                if( Master().getBanks() == null || "".equals(Master().getBanks() )){
                    poJSON = object.searchRecord(value, byCode);
                } else {
                    poJSON = object.searchRecordbyBanks(value,Master().getBanks(),byCode);
                }
            }
            if ("success".equals((String) poJSON.get("result"))) {
                if(fbSearch){
                    psSearchBankAcctNo = object.getModel().getAccountNo();
                } else {
                    Master().setBankAccount(object.getModel().getBankAccountId());   
                }
            }
        }
        return poJSON;
    }
    
    //*********************TRANSACTION RETRIEVAL**************************
    
    public JSONObject SearchTransaction(String fsTransactionNo, String fsAccountNo, String fsDate) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        List<String> lsFilter = new ArrayList<>();
        initSQL();
        String lsSQL = SQL_BROWSE;

        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
           lsFilter.add( "  a.cTranStat IN (" + lsTransStat.substring(2) + ")");
        } else {
            lsFilter.add("  a.cTranStat = " + SQLUtil.toSQL(psTranStat));
        }
        
        if (fsTransactionNo != null && !"".equals(fsTransactionNo)) {
            lsFilter.add(" a.sTransNox LIKE " + SQLUtil.toSQL("%"+ fsTransactionNo));
        }
        if (fsAccountNo != null && !"".equals(fsAccountNo)) {
            if(pbSupplier){
                lsFilter.add(" cc.sActNumbr  LIKE " + SQLUtil.toSQL("%" + fsAccountNo));
            } else {
                lsFilter.add(" c.sActNumbr  LIKE " + SQLUtil.toSQL("%" + fsAccountNo));
            }
        }
        if (fsDate != null && !"".equals(fsDate)) {
            lsFilter.add(" a.dTransact = " + SQLUtil.toSQL(fsDate));
        }
        String lsColCriteria = "a.dTransact»a.sTransNox»c.sActNumbr»c.sActNamex";
        if(pbSupplier){
            //lsColCriteria = "a.dTransact»a.sTransNox»IFNULL(c.sActNumbr,cc.sActNumbr)»IFNULL(c.sActNamex,cc.sActNamex)";
            lsColCriteria = "a.dTransact»a.sTransNox»cc.sActNumbr»cc.sActNamex";
            lsFilter.add("g.sCompnyNm NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%")); 
        } else {
            // BR: table connection: Payee.sClientID(get the sCompnyNm) = Company Name selected during system log in; Client_Master.sCompnyNm = System Log in.sCompnyNm
            lsFilter.add("g.sCompnyNm LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%"));
        }
        
        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }
        if(psCompanyId == null || "".equals(psCompanyId)){
            psCompanyId = poGRider.getCompnyId();
        }
        
        lsSQL = lsSQL   + " AND e.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " GROUP BY a.sTransNox ";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "", 
                "Transaction Date»Transaction No»Account No»Account Name",
                "a.dTransact»a.sTransNox»sActNumbr»sActNamex",
                lsColCriteria,
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON = setJSON("error","No record loaded.");
            return poJSON;
        }
    }
    
    public JSONObject loadTransactionList(String fsTransactionNo, String fsAccountNo, String fsDate) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        List<String> lsFilter = new ArrayList<>();
        String lsTransStat = "";
        initSQL();
        String lsSQL = SQL_BROWSE;
        
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
           lsFilter.add( "  a.cTranStat IN (" + lsTransStat.substring(2) + ")");
        } else {
            lsFilter.add("  a.cTranStat = " + SQLUtil.toSQL(psTranStat));
        }
        
        if (fsTransactionNo != null && !"".equals(fsTransactionNo)) {
            lsFilter.add(" a.sTransNox LIKE " + SQLUtil.toSQL("%"+ fsTransactionNo));
        }
        if (fsAccountNo != null && !"".equals(fsAccountNo)) {
            if(pbSupplier){
                lsFilter.add(" cc.sActNumbr  LIKE " + SQLUtil.toSQL("%" + fsAccountNo));
            } else {
                lsFilter.add(" c.sActNumbr  LIKE " + SQLUtil.toSQL("%" + fsAccountNo));
            }
        }
        if (fsDate != null && !"".equals(fsDate)) {
            lsFilter.add(" a.dTransact = " + SQLUtil.toSQL(fsDate));
        }
        
        if(pbSupplier){
            lsFilter.add("g.sCompnyNm NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%")); 
        } else {
            // BR: table connection: Payee.sClientID(get the sCompnyNm) = Company Name selected during system log in; Client_Master.sCompnyNm = System Log in.sCompnyNm
            lsFilter.add("g.sCompnyNm LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%"));
        }
        
        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }

        if(psCompanyId == null || "".equals(psCompanyId)){
            psCompanyId = poGRider.getCompnyId();
        }
        
        lsSQL = lsSQL   + " AND e.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " GROUP BY a.sTransNox "
                        + " ORDER BY a.dTransact DESC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                paMaster.add(TransactionList());
                paMaster.get(paMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            poJSON = setJSON("success","Record loaded successfully.");
        } else {
            paMaster = new ArrayList<>();
            paMaster.add(TransactionList());
            poJSON.put("result", "error");
            poJSON.put("continue", true);
            poJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    
    //****************POPULATE CHECK DEPOSIT***************************
    
    public JSONObject loadCheckPayment(String fsBankName, String fsDateFrom, String fsDateThru) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paChecks = new ArrayList<>();
        String lsSQL = getSQ_CheckPayment();

        // Build filter conditions dynamically
        List<String> lsFilter = new ArrayList<>();
        if (fsBankName != null && !"".equals(fsBankName)) {
            lsFilter.add("b.sBankName LIKE " + SQLUtil.toSQL(fsBankName + "%"));
        }

        if (fsDateFrom != null && !"".equals(fsDateFrom) 
            && fsDateThru != null && !"".equals(fsDateThru)) {
            lsFilter.add("a.dCheckDte BETWEEN "
                        + SQLUtil.toSQL(fsDateFrom)
                        + " AND "
                        + SQLUtil.toSQL(fsDateThru));
        }
        
        if(psCompanyId == null || "".equals(psCompanyId)){
            psCompanyId = poGRider.getCompnyId();
        }
        lsFilter.add(" c.sCompnyID = " + SQLUtil.toSQL(psCompanyId));
        lsFilter.add("a.cReleased = '0' AND a.cTranStat <> 3 AND a.cPrintxxx = '1' AND c.cDisbrsTp = " + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT) );
        if(pbSupplier){
            lsFilter.add("( d.sPayeeNme NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " AND IFNULL(e.sCompnyNm,d.sPayeeNme) NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " ) ");
        } else { 
            // BR: table connection: Payee.sClientID(get the sCompnyNm) = Company Name selected during system log in; Client_Master.sCompnyNm = System Log in.sCompnyNm
            lsFilter.add("( d.sPayeeNme LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " OR IFNULL(e.sCompnyNm,d.sPayeeNme) LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " ) ");
        }

        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }
        lsSQL = lsSQL + " ORDER BY a.dCheckDte ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paChecks = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sVouchrNo: " + loRS.getString("sVouchrNo"));
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("sPayeeIDx: " + loRS.getString("sPayeeIDx"));
                System.out.println("sCheckNox: " + loRS.getString("sCheckNox"));
                System.out.println("------------------------------------------------------------------------------");

                paChecks.add(ChecksPaymentList());
                paChecks.get(paChecks.size() - 1).openRecord(loRS.getString("sSourceNo"));
                lnCtr++;
            }
            
            System.out.println("Records found: " + lnCtr);
            poJSON = setJSON("success","Record loaded successfully.");
        } else {
            paChecks = new ArrayList<>();
            paChecks.add(ChecksPaymentList());
            poJSON.put("result", "error");
            poJSON.put("continue", true);
            poJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    
    public JSONObject searchCheckPayment(String fsDVNo, String fsCheckNo, boolean fbByDVno) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        paChecks = new ArrayList<>();
        String lsSQL = getSQ_CheckPayment();

        // Build filter conditions dynamically
        List<String> lsFilter = new ArrayList<>();
        if (fsDVNo != null && !"".equals(fsDVNo)) {
            lsFilter.add("c.sVouchrNo LIKE " + SQLUtil.toSQL(fsDVNo + "%"));
        }
        if (fsCheckNo != null && !"".equals(fsCheckNo)) {
            lsFilter.add("a.sCheckNox LIKE " + SQLUtil.toSQL(fsCheckNo + "%"));
        }

        if(psCompanyId == null || "".equals(psCompanyId)){
            psCompanyId = poGRider.getCompnyId();
        }
        lsFilter.add(" c.sCompnyID = " + SQLUtil.toSQL(psCompanyId));
        lsFilter.add("a.cReleased = '0' AND a.cTranStat <> 3 AND a.cPrintxxx = '1' AND c.cDisbrsTp = " + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT));
        if(pbSupplier){
            lsFilter.add("( d.sPayeeNme NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " AND IFNULL(e.sCompnyNm,d.sPayeeNme) NOT LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " ) ");
        } else { 
            // BR: table connection: Payee.sClientID(get the sCompnyNm) = Company Name selected during system log in; Client_Master.sCompnyNm = System Log in.sCompnyNm
            lsFilter.add("( d.sPayeeNme LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " OR IFNULL(e.sCompnyNm,d.sPayeeNme) LIKE " + SQLUtil.toSQL("%"+getCompanyName()+"%") + " ) ");
        }
        
        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }
        System.out.println("Executing SQL: " + lsSQL);
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "", 
                "Check Date»Check No»DV No»Account No»Account Name",
                "a.dCheckDte»a.sCheckNox»c.sVouchrNo»f.sActNumbr»f.sActNamex",
                "a.dCheckDte»a.sCheckNox»c.sVouchrNo»f.sActNumbr»f.sActNamex",
                fbByDVno ? 2 : 1);

        if (poJSON != null) {
            poJSON = populateDetail((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON = setJSON("error","No record loaded.");
            return poJSON;
        }
        return poJSON;
    }
    
    public JSONObject populateDetail(String fsTrasactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        int lnRow = 0;
        
        // Check if the checkpayment already exists in the details
        for (lnRow = 0; lnRow < getDetailCount(); lnRow++) {
            if (Detail(lnRow).getSourceNo() == null || "".equals(Detail(lnRow).getSourceNo())) {
                continue;
            }

            if (Detail(lnRow).getSourceNo().equals(fsTrasactionNo)) {
                lbExist = true;
                break;
            }
        }

        if (!lbExist) {
            // Initialize and load the record
            Model_Check_Payments loObject = new CashflowModels(poGRider).CheckPayments();
            loObject.initialize();
            poJSON = loObject.openRecord(fsTrasactionNo);
            if ("error".equals(poJSON.get("result"))) {
                poJSON = setJSON("error",(String) poJSON.get("message"));
                poJSON.put("row", getDetailCount()-1);
                return poJSON;
            }
            
            poJSON = checkExistingCheckDeposit(fsTrasactionNo, loObject.getSourceCode()) ;
            if(!isJSONSuccess(poJSON)){
                poJSON.put("row", getDetailCount()-1);
                return poJSON;
            }
            
            // Make sure you're writing to an empty row
            Detail(getDetailCount() - 1).setSourceNo(loObject.getTransactionNo());
            Detail(getDetailCount() - 1).setSourceCode(loObject.getSourceCode()); //enable this when setting the source code
            
            // Only add the detail if it's not empty
            AddDetail();
        } else {
            if (!Detail(lnRow).isReverse()) {
                Detail(lnRow).isReverse(true);
                poJSON.put("result", "success");
                poJSON.put("row", lnRow);
                return poJSON;
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Checkpayment: " + Detail(lnRow).getSourceNo() + " already exists in table at row " + (lnRow + 1) + ".");
                poJSON.put("row", lnRow);
                poJSON.put("warning", "false");
                return poJSON;
            }
        }
        computeFields();
        // Return success
        poJSON.put("row", getDetailCount()-1);
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private static String psNoCategory = "EMPTY";
    /**
     * Populate Journal information
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject populateJournal() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON = setJSON("error", "No record to load");
            return poJSON;
        }
        
        if(poJournal == null || getEditMode() == EditMode.READY){
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        }
        
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            switch(getEditMode()){
                case EditMode.READY:
                    poJSON = poJournal.OpenTransaction(lsJournal);
                    if (!isJSONSuccess(poJSON)){
                        return poJSON;
                    }
                break;
                case EditMode.UPDATE:
                    if(poJournal.getEditMode() == EditMode.READY || poJournal.getEditMode() == EditMode.UNKNOWN){
                        poJSON = poJournal.OpenTransaction(lsJournal);
                        if (!isJSONSuccess(poJSON)){
                            return poJSON;
                        }
                        poJournal.UpdateTransaction();
                    } 
                break;
            }
        } else {
            if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poJournal.getEditMode() != EditMode.ADDNEW){
                poJSON = poJournal.NewTransaction();
                if (!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                
                //retreiving using column index
                JSONObject jsonmaster = new JSONObject();
                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++){
                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
                    jsonmaster.put(Master().getColumn(lnCtr),  Master().getValue(lnCtr));
                }
                    
                JSONArray jsondetails = new JSONArray();
                JSONObject jsondetail = new JSONObject();
                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                    jsondetail = new JSONObject();
                    for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++){
                        System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
                        jsondetail.put(Detail(lnCtr).getColumn(lnCol),  Detail(lnCtr).getValue(lnCol));
                    }
                    jsondetails.add(jsondetail);
                }

                jsondetail = new JSONObject();
                jsondetail.put("Check_Deposit_Master", jsonmaster);
                jsondetail.put("Check_Deposit_Detail", jsondetails);
                    
                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE,Master().getIndustryId(), ""); 
                tbj.setGRiderCAS(poGRider);
                tbj.setData(jsondetail);
                jsonmaster = tbj.processRequest();

                if(jsonmaster.get("result").toString().equalsIgnoreCase("success")){
                    List<TBJEntry> xlist = tbj.getJournalEntries();
                    for (TBJEntry xlist1 : xlist) {
                        System.out.println("Account:" + xlist1.getAccount() );
                        System.out.println("Debit:" + xlist1.getDebit());
                        System.out.println("Credit:" + xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setForMonthOf(poGRider.getServerDate());
                        poJournal.Detail(poJournal.getDetailCount()-1).setAccountCode(xlist1.getAccount());
                        poJournal.Detail(poJournal.getDetailCount()-1).setCreditAmount(xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setDebitAmount(xlist1.getDebit());
                        poJournal.AddDetail();
                    }
                } else {
                    System.out.println(jsonmaster.toJSONString());
                }

                //Journa Entry Master
                poJournal.Master().setAccountPerId("");
                poJournal.Master().setIndustryCode(Master().getIndustryId());
                poJournal.Master().setBranchCode(poGRider.getBranchCode());
                poJournal.Master().setDepartmentId(poGRider.getDepartment());
                poJournal.Master().setTransactionDate(poGRider.getServerDate()); 
                poJournal.Master().setCompanyId(psCompanyId);
                poJournal.Master().setSourceCode(getSourceCode());
                poJournal.Master().setSourceNo(Master().getTransactionNo());
                
            } else if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poJournal.getEditMode() == EditMode.ADDNEW) {
                poJSON.put("result", "success");
                return poJSON;
            } 
//            else {
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record to load");
//                return poJSON;
//            }
        
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Check existing Journal
     * @return
     * @throws SQLException 
     */
    public String existJournal() throws SQLException{
        Model_Journal_Master loMaster = new CashflowModels(poGRider).Journal_Master();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        lsSQL = MiscUtil.addCondition(lsSQL,
                " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
        );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------JOURNAL ENTRY--------------------------");
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("------------------------------------------------------------------------------");
                if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                    return loRS.getString("sTransNox");
                }  
            }
        }
        MiscUtil.close(loRS);

        return "";
    }
    
    /**
     * Retrieves and downloads all attachments associated with the current transaction.
     * 
     * This method fetches attachment metadata from the system tables, populates the
     * {@code paAttachments} collection, and performs a web download for each file. 
     * Downloaded files are decoded from Base64 and saved to the system's temporary 
     * attachment directory defined in the system properties.
     * 
     * @return A {@link JSONObject} indicating the overall success of the attachment loading process.
     * @throws SQLException If a database error occurs while fetching attachment records.
     * @throws GuanzonException If an error occurs during file synchronization or processing.
     */
    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();
        String lsSourceCode = "";
        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList;
        
        loList = loAttachment.getAttachments(getSourceCode(), Master().getTransactionNo());
        lsSourceCode = getSourceCode();
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if (isJSONSuccess(poJSON)) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());
            }
            
            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                    , lsSourceCode
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if (isJSONSuccess(poJSON)) {
                
                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println("File downloaded succesfully.");
                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                    System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                }
                
            } else {
                System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }
        
        poJSON = setJSON("success","success");
        return poJSON;
    }
    /**
     * Instantiates a new TransactionAttachment controller.
     */
    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }
    /**
     * Retrieves the attachment record at the specified index.
     * @param row The zero-based index of the attachment.
     */
    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }
    /**
     * Returns the total count of attachments in the current list.
     */
    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }
    /**
     * Appends a new, empty attachment record to the collection.
     * @return A {@link JSONObject} indicating the success of the addition.
     * @throws SQLException, GuanzonException if initialization fails.
     */    
    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON = setJSON("error", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON = setJSON("success", "success");
        return poJSON;
    }
    /**
     * Removes a new attachment or marks an existing one as Inactive.
     * @param fnRow Index of the attachment to remove/deactivate.
     * @return Result status as a {@link JSONObject}.
     */    
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON = setJSON("error", "No transaction attachment to be removed.");
            return poJSON;
        }
        
        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
            System.out.println("Attachment :"+ fnRow+" Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :"+ fnRow+" Deactivate");
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    /**
     * Adds an attachment by filename or reactivates it if it already exists as Inactive.
     * @param fFileName The name of the file to add.
     * @return The index of the added or reactivated attachment.
     */    
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :"+ lnCtr+" Activate");
                return lnCtr;
            }
        }
        
        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(Master().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }
    
    /**
     * Copies a file from the source path to the system's temporary attachment directory.
     * <p>
     * Includes a retry mechanism that attempts to re-copy the file up to 5 times 
     * if the target file is not immediately detected after the initial operation.
     * 
     * @param fsPath The absolute path of the source file to be copied.
     */
    public void copyFile(String fsPath){
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            //check if file is existing
            int lnChecker = 0;
            File file = new File(targetDir+ "/" + source.getFileName());
            System.out.println("File Path : " + file.getPath());
            while(!file.exists() && lnChecker < 5){
                Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);  
                System.out.println("Re-Copying... " + lnChecker);
                lnChecker++;
            }
            
            if(!file.exists()){
                System.out.println("File did not copy!");
                return;
            } else {
                System.out.println("File copied successfully!");
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if a filename already exists in the attachment database records.
     * 
     * @param fsFileName The name of the file to validate.
     * @return A {@link JSONObject} containing an error message if the filename exists, 
     *         otherwise an empty success object.
     * @throws SQLException, GuanzonException If a database access error occurs.
     */
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()), 
                                                                    " sFileName = " + SQLUtil.toSQL(fsFileName)
                                                                    );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))){
                        poJSON = setJSON("error", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }
    
    
    public JSONObject computeFields() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        double totalAmount = 0.0000;
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            // include only reversed ("+")
            if (Detail(lnCtr).isReverse()) {
                totalAmount += Detail(lnCtr).CheckPayment().getAmount();
            }
        }

        Master().setTransactionTotalDeposit(totalAmount);
        return poJSON;
    }
    
    public JSONObject checkExistingCheckDeposit(String fsTransactionNo, String fsSourceCode) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                                                " b.sSourceNo = " + SQLUtil.toSQL(fsTransactionNo)
                                               + " AND b.sSourceCd = " + SQLUtil.toSQL(fsSourceCode)
                                               + " AND b.cReversex = " + SQLUtil.toSQL("+") //Included
                                               + " AND a.sTransNox != " + SQLUtil.toSQL(Master().getTransactionNo())
                                               + " AND a.cTranStat != " + SQLUtil.toSQL(CheckDepositStatus.VOID)
                                               + " AND a.cTranStat != " + SQLUtil.toSQL(CheckDepositStatus.CANCELLED)
                                                );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sCheckNox") != null && !"".equals(loRS.getString("sCheckNox"))){
                        poJSON = setJSON("error", "Check no "+loRS.getString("sCheckNox")+" is already linked thru check deposit <"+loRS.getString("sTransNox")+">.\n"
                                                        + "Contact system administrator for assistance.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }
    
    @Override
    public JSONObject willSave() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        boolean lbHasActiveDetail = false;
        //Recompute fields
        computeFields(); 
        
        /*Put system validations and other assignments here*/
        System.out.println("Class Edit Mode : " + getEditMode());
        System.out.println("Master Edit Mode : " + Master().getEditMode());
        System.out.println("Journal Class Edit Mode : " + poJournal.getEditMode());
        System.out.println("Journal Master Edit Mode : " + poJournal.Master().getEditMode());
        //Re-set the transaction no and voucher no
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        poJSON = isEntryOkay(Master().getTransactionStatus());
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        String bankCode = getDocumentCode(Master().getBankAccount()); //CheckPayments().getModel().Banks().getBankCode()+"Chk"+;
        if(bankCode == null || "".equals(bankCode)){
            poJSON.put("result", "error");
            poJSON.put("message", "Please configure the document code for bank account.");
            return poJSON;
        }

        DocumentMapping poDocumentMapping = new CashflowControllers(poGRider, logwrapr).DocumentMapping();
        poDocumentMapping.InitTransaction();
        poJSON = poDocumentMapping.OpenTransaction(bankCode);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
         for (int i = 0; i < poDocumentMapping.Detail().size(); i++) {
            String fieldName = poDocumentMapping.Detail(i).getFieldCode();
            int rowspace = poDocumentMapping.Detail(i).getMaxRow();
            switch (fieldName) {
                //Transaction Detail
                case "sBankIDxx":
                case "sCheckNox":
                case "nAmountxx":
                    if(rowspace < (getDetailCount()-1)){
                        poJSON = setJSON("error", "Transaction detail cannot be greater than the maximum row for asign bank account document mapping.");
                        return poJSON;
                    }
                break;
            }

        }
        
        //remove items with no stockid or quantity order
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            Object rawValue = item.getValue("sSourceNo");
            if (rawValue == null || "".equals(rawValue)) {
                detail.remove();
            }
        }
        
        if(getDetailCount() <= 0 ){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }
        
        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
            
            if(Detail(lnCtr).isReverse()){
                poJSON = checkExistingCheckDeposit(Detail(lnCtr).getSourceNo(), Detail(lnCtr).getSourceCode()) ;
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
            }
            
            if (Detail(lnCtr).isReverse()) {
                lbHasActiveDetail = true; // at least one is reversed
            }
        }
        if (!lbHasActiveDetail) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }
        
        if (CheckDepositStatus.CONFIRMED.equals(Master().getTransactionStatus()) && !pbPrint) {
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(poGRider.getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));

            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()){
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                while ("error".equals((String)checkExistingFileName(lsNewFileName).get("result"))) {
                    lnCopies++;
                    //Rename the file
                    int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                    if (dotIndex == -1) {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() +"_"+lnCopies;
                    } else {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) +"_"+ lnCopies +TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                    }
                }

                if(lnCopies > 0){
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments")).resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        //check if file is existing
                        int lnChecker = 0;
                        File file = new File(TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + lsNewFileName);
                        while(!file.exists() && lnChecker < 5){
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);  
                            System.out.println("Re-Copying... " + lnChecker);
                            lnChecker++;
                        }
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            //Upload Attachment when send status is 0
            try {
                if("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr,lsOriginalFileName);
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON = setJSON("error", MiscUtil.getException(ex));
                return poJSON;
            }

        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Requests user approval for the current transaction.
    *
    * @return JSONObject containing approval result and message
    */
    public JSONObject callApproval(){
        poJSON = new JSONObject();
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer.");
                return poJSON;
            }
            setApproving(lsUserIDxx);
            psApprover = lsUserIDxx;
        }   
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        return isEntryOkay(CheckDepositStatus.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        poJSON = new JSONObject();
        try {
            System.out.println("--------------------------SAVE OTHERS---------------------------------------------");
            System.out.println("Class Edit Mode : " + getEditMode());
            System.out.println("Master Edit Mode : " + Master().getEditMode());
            System.out.println("-----------------------------------------------------------------------");
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
                System.out.println("COUNTER : " + lnCtr);
                System.out.println("Transaction No : " + Detail(lnCtr).getTransactionNo());
                System.out.println("Source No : " + Detail(lnCtr).getSourceNo());
                System.out.println("-----------------------------------------------------------------------");
            }
            
            //Save Attachments
            System.out.println("-----------------------------SAVE TRANSACTION ATTACHMENT------------------------------------------");
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                }
            }
            System.out.println("-----------------------------------------------------------------------");
            
            if(!pbSupplier){
    //            Save Journal
                System.out.println("--------------------------SAVE JOURNAL---------------------------------------------");
                if(poJournal != null){
                    if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                        poJSON = validateJournal();
                        boolean lbContinue = (boolean) poJSON.get("continue");
                        if (!isJSONSuccess(poJSON)) {
                            poJSON.put("result", "error");
                            poJSON.put("message", poJSON.get("message").toString());
                            return poJSON;
                        } 
                        if(lbContinue){
                            poJournal.Master().setSourceNo(Master().getTransactionNo());
                            poJournal.Master().setModifyingId(poGRider.getUserID());
                            poJournal.Master().setModifiedDate(poGRider.getServerDate());
                            poJournal.setWithParent(true);
                            poJSON = poJournal.SaveTransaction();
                            if (!isJSONSuccess(poJSON)) {
                                System.out.println("Save Journal : " + poJSON.get("message"));
                                return poJSON;
                            }
                        }
                    } else {
    //                    if (poGRider.getUserLevel() > UserRight.ENCODER) {
    //                        poJSON.put("result", "error");
    //                        poJSON.put("message", "Invalid Update mode for Journal.");
    //                        return poJSON;
    //                    }
                    }
                } else {
    //                if (poGRider.getUserLevel() > UserRight.ENCODER) {
    //                    poJSON.put("result", "error");
    //                    poJSON.put("message", "Journal is not set.");
    //                    return poJSON;
    //                }
                }
                System.out.println("-----------------------------------------------------------------------");
            }
        
            System.out.println("--------------------------UPDATE CHECK PAYMENTS---------------------------------------------");
                poJSON = updateCheckPayments(Master().getTransactionStatus());
                if (!isJSONSuccess(poJSON)) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
            System.out.println("-----------------------------------------------------------------------");
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }
            
        poJSON = setJSON("success", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        System.out.println("Transaction saved successfully.");
    }

    /**
     * Sets default master record values for a new transaction.
     * 
     * Configures the branch, industry, and company identifiers, sets the current 
     * server date, and initializes the status to {@link CashDisbursementStatus#OPEN}.
     * 
     * @return A {@link JSONObject} indicating the initialization result.
     */
    @Override
    public JSONObject initFields() {
        //Put initial model values here/
        poJSON = new JSONObject();
        try {
            poJSON = new JSONObject();
            Master().setIndustryId("08"); //Mandatory industry is set to 08
//            Master().setCompanyId(psCompanyId);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(CheckDepositStatus.OPEN);

        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = (GValidator) new CheckDepositValidatorFactory();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox, "
                + " a.dTransact, "
                + " IFNULL(c.sActNumbr,cc.sActNumbr) as sActNumbr, "
                + " IFNULL(c.sActNamex,cc.sActNamex) as sActNamex, "
//                + " c.sActNamex, "
                + " a.cTranStat, "
                + " e.sCompnyID, "
                + " g.sCompnyNm,  "
                + " d.sCheckNox,  "
                + " e.sVouchrNo  "
                + " FROM Check_Deposit_Master a "
                + " INNER JOIN Check_Deposit_Detail b ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Bank_Account_Master c ON a.sBnkActID = c.sBnkActID "
                + " LEFT JOIN AP_Client_Bank_Account cc ON cc.sAPBnkIDx = a.sBnkActID "
                + " INNER JOIN Check_Payments d ON d.sTransNox = b.sSourceNo " 
                + " INNER JOIN Disbursement_Master e ON e.sTransNox = d.sSourceNo "
                + " INNER JOIN Payee f ON f.sPayeeIDx = d.sPayeeIDx "
                + " LEFT JOIN Client_Master g ON g.sClientID = f.sClientID ";
    }
    
    public String getSQ_CheckPayment() {
        return "SELECT DISTINCT "
                + "  a.sTransNox, "
                + "  a.sBranchCd, "
                + "  a.sBankIDxx, "
                + "  a.sPayeeIDx, "
                + "  a.dCheckDte, "
                + "  a.sCheckNox, "
                + "  a.nAmountxx, "
                + "  a.sSourceNo, "
                + "  b.sBankName AS bankname, "
                + "  d.sPayeeNme AS payeename, "
                + "  a.cReleased, "
                + "  a.cTranStat, "
                + "  c.sVouchrNo,  "
                + "  f.sActNumbr, "
                + "  f.sActNamex  "
                + "  FROM Check_Payments a "
                + "  LEFT JOIN Banks b ON a.sBankIDxx = b.sBankIDxx "
                + "  INNER JOIN Disbursement_Master c ON c.sTransNox = a.sSourceNo "
                + "  INNER JOIN Payee d ON a.sPayeeIDx = d.sPayeeIDx "
                + "  LEFT JOIN Client_Master e ON e.sClientID = d.sClientID "
                + "  LEFT JOIN Bank_Account_Master f ON f.sBnkActID = a.sBnkActID ";
    }
    
    /**
    * Creates a JSONObject with "result" and "message" fields.
    *
    * @param fsResult  The result value (e.g., "success", "error")
    * @param fsMessage The message describing the result
    * @return JSONObject containing the result and message
    */
    private JSONObject setJSON(String fsResult, String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", fsResult);
        loJSON.put("message", fsMessage);
        return loJSON;
    }
    
    /**
     * Checks whether a JSONObject indicates a successful result.
     *
     * Returns true if the "result" field equals "success" or is not "error".
     *
     * @param foJSON The JSONObject to check
     * @return true if successful, false otherwise
     */
    public boolean isJSONSuccess(JSONObject foJSON) {
        return ("success".equals((String) foJSON.get("result")) || !"error".equals((String) foJSON.get("result")));
    }
    
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
    
    /**
    * Validates journal entries including debit/credit balance,
    * account code presence, and valid reporting dates.
    *
    * @return JSON validation result with continue flag
    */
    private JSONObject validateJournal(){
        poJSON = new JSONObject();
        poJSON.put("continue", false);
        
        double ldblCreditAmt = 0.0000;
        double ldblDebitAmt = 0.0000;
        boolean lbHasJournal = false;
        boolean lbValidateJournal = false;
        for(int lnCtr = 0; lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            if(poJournal.Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                ldblDebitAmt += poJournal.Detail(lnCtr).getDebitAmount();
                ldblCreditAmt += poJournal.Detail(lnCtr).getCreditAmount();

                if(poJournal.Detail(lnCtr).getCreditAmount() > 0.0000 ||  poJournal.Detail(lnCtr).getDebitAmount() > 0.0000){
                    if(poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode())){
                        if(poJournal.Detail(lnCtr).getForMonthOf() == null || "1900-01-01".equals(xsDateShort(poJournal.Detail(lnCtr).getForMonthOf()))){
                            poJSON.put("result", "error");
                            poJSON.put("message", "Invalid reporting date of journal at row "+(lnCtr+1)+" .");
                            return poJSON;
                        }
                    }
                }
                
                if(!lbValidateJournal){
                    lbValidateJournal = poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode());
                } 
            }
            
            if(!lbHasJournal){
                lbHasJournal = poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode());
            }   
        }
        
        if(lbValidateJournal){
            //Convert debit and credit amount
            ldblDebitAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblDebitAmt, true).replace(",", ""));
            ldblCreditAmt = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblCreditAmt, true).replace(",", ""));
            
            if(ldblDebitAmt == 0.0000 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry debit amount.");
                return poJSON;
            }

            if(ldblCreditAmt == 0.0000){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid journal entry credit amount.");
                return poJSON;
            }

            if(ldblDebitAmt < ldblCreditAmt || ldblDebitAmt > ldblCreditAmt){
                poJSON.put("result", "error");
                poJSON.put("message", "Debit should be equal to credit amount.");
                return poJSON;
            }
        }
        
        
        poJSON.put("result", "sucess");
        poJSON.put("message", "sucess");
        poJSON.put("continue", lbHasJournal);
        return poJSON;
    }

    
    /**
     * Upload Attachment
     * @param instance
     * @param access 
     * @param fnRow 
     * @return  
     * @throws java.lang.Exception 
     */
    /**
     * Uploads a specific transaction attachment to the web server.
     * <p>
     * This method verifies the file's existence (handling renames), generates an MD5 hash 
     * for data integrity, and transmits the file via the {@link WebFile} API. Upon successful 
     * upload, it updates the record's hash and sets the send status to "1" (Sent).
     * 
     * @param instance The application driver instance.
     * @param access The access token for web service authentication.
     * @param fnRow The index of the attachment in the local collection.
     * @param fsOriginalFileName The original filename to use as a fallback if the new file is missing.
     * @return A {@link JSONObject} containing the upload result status.
     * @throws Exception If an error occurs during file reading, encoding, or web transmission.
     */
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception{       
        poJSON = new JSONObject();
        System.out.println("Uploading... : fsOriginalFileName : " + fsOriginalFileName);
        System.out.println("New File Name... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        String lsFile = paAttachments.get(fnRow).getModel().getFileName();
        
        //check if new file is existing
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        if(!file.exists()){
            //check if original file is existing
            lsFile = fsOriginalFileName;
            file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
            if(!file.exists()){
                poJSON = setJSON("error", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
                                        + ".\nContact system administrator for assistance.");
                return poJSON;  
            }
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if(paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())){
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access)
                                , "0032"
                                , ""
                                , paAttachments.get(fnRow).getModel().getFileName()
                                , instance.getBranchCode()
                                , hash
                                , encodeFileToBase64Binary(file)
                                , paAttachments.get(fnRow).getModel().getSourceCode()
                                , paAttachments.get(fnRow).getModel().getSourceNo()
                                , "");

        if("error".equalsIgnoreCase((String) result.get("result"))){
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
                poJSON = setJSON("error", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
                                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Converts a file's content into a Base64 encoded string.
     * 
     * @param file The file object to be encoded.
     * @return A Base64 string representation of the file.
     * @throws Exception If an I/O error occurs during file reading.
     */
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
     }
         
    private static JSONObject token = null;
    /**
     * Retrieves a valid access token, refreshing it if it has expired.
     * <p>
     * The token is considered stale if it was created more than 25 minutes ago. 
     * If expired, it triggers an external request to update the token file before 
     * returning the new access key.
     * 
     * @param access The file path to the JSON formatted token storage.
     * @return The access key string, or {@code null} if the file cannot be read or parsed.
     */
    private static String getAccessToken(String access){
        try {
            JSONParser oParser = new JSONParser();
            if(token == null){
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created") , SQLUtil.FORMAT_TIMESTAMP));
            
            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if(current_date.after(date_created)){
                String[] xargs = new String[] {(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            return (String)token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }
    
    //**************************PRINT DEPOSIT SLIP BASED ON DOCUMENT MAPPING**********************************
    public JSONObject PrintDepositSlip() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        
        poJSON = OpenTransaction(Master().getTransactionNo());
        if(!isJSONSuccess(poJSON)){
            return poJSON;
        }
        
        if(Logical.YES.equals(Master().getPrintStatus())){
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                ShowMessageFX.Warning(null, "Deposit Slip Already Printed", "This deposit slip has already been printed. User approval is required to continue.");
            }
            poJSON = callApproval();
            if(!isJSONSuccess(poJSON)){
                return poJSON;
            }
        }

        String bankCode = getDocumentCode(Master().getBankAccount()); //CheckPayments().getModel().Banks().getBankCode()+"Chk"+;
        if(bankCode == null || "".equals(bankCode)){
            poJSON.put("result", "error");
            poJSON.put("message", "Please configure the document code for bank account.");
            return poJSON;
        }

//            bankCode = "MBTDSChk";
        System.out.println("===============================================");
        System.out.println("transactionNo No : " + Master().getTransactionNo());
        System.out.println("account no : " + Master().BankAccount().getAccountNo());
        System.out.println("account name : " + Master().BankAccount().getAccountName());
        System.out.println("total deposit amount : " + Master().getTransactionTotalDeposit());
        System.out.println("===============================================");

        DocumentMapping poDocumentMapping = new CashflowControllers(poGRider, logwrapr).DocumentMapping();
        poDocumentMapping.InitTransaction();
        poJSON = poDocumentMapping.OpenTransaction(bankCode);
        if ("error".equals((String) poJSON.get("result"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Please configure the document mapping for "+bankCode+".\n"+ (String) poJSON.get("message"));
            return poJSON;
        }

        // Store transaction for printing
        String lsBankAccountNo = Master().BankAccount().getAccountNo();
        String lsBankAccountName = Master().BankAccount().getAccountName();
        if(pbSupplier){
            lsBankAccountNo = Master().APClientBankAccount().getAccountNumber();
            lsBankAccountName = Master().APClientBankAccount().getAccountName();
        }
        Transaction transaction = new Transaction(lsBankAccountNo, lsBankAccountName, xsDateShort(Master().getTransactionReferDate()), new BigDecimal(Master().getTransactionTotalDeposit()));
        List<TransactionDetail> transactiondetail = new ArrayList<>();
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if(!Detail(lnCtr).isReverse()){
                continue;
            }
            transactiondetail.add(new TransactionDetail(
                    lnCtr+1,
                    Detail(lnCtr).CheckPayment().Banks().getBankName(),
                    Detail(lnCtr).CheckPayment().getCheckNo(),
                    Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Detail(lnCtr).CheckPayment().getAmount(), false).replace(",", ""))
            ));
        }
        // Now print the voucher using PrinterJob
        if (showPrintPreview(transaction, transactiondetail, poDocumentMapping)) {
            poJSON = PrintCheck(transaction,transactiondetail,poDocumentMapping);
            if ("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }

            if (Logical.NO.equals(Master().getPrintStatus())) {
                CheckDeposit loObject = new CashflowControllers(poGRider,logwrapr).CheckDeposit();
                loObject.InitTransaction();
                loObject.isCheckDepositSupplier(pbSupplier);
                poJSON = loObject.OpenTransaction(Master().getTransactionNo());
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                poJSON = loObject.UpdateTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                loObject.Master().setPrintStatus(Logical.YES);
                loObject.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                loObject.Master().setModifiedDate(poGRider.getServerDate());
                loObject.setPrint(true);
                poJSON = loObject.SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Print check aborted.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "Check deposit slip printed successfully");
        return poJSON;
    }
    
    /*Removes comma character existing in a string containing number*/
    public static String removeComma(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) {
            return "0";
        }

        // Remove commas
        String clean = numberStr.replace(",", "");

        // Check if it's exactly negative zero
        if (clean.matches("-0+(\\.0+)?")) {
            return "0";
        }

        return clean.isEmpty() ? "0" : clean;
    }
    
    /**
     * get Inventory Type Code value
     * @param fsValue description
     * @return 
     */
    private String getDocumentCode(String fsBankAccountId){
        try {
            String lsSQL = "";
            //New query from ma'am she 04-11-2026 08:59 AM MBTChkDS
            if(pbSupplier){
                lsSQL = " SELECT CONCAT(c.sBankCode,'Chk','DS') AS sDocCodex " //TODO
                            + " FROM AP_Client_Bank_Account a " 
                            + "	LEFT JOIN Banks c ON a.sBankIDxx = c.sBankIDxx " ;
                lsSQL = MiscUtil.addCondition(lsSQL,  " a.sAPBnkIDx = " + SQLUtil.toSQL(fsBankAccountId));
            } else {
                 lsSQL = " SELECT CONCAT(c.sBankCode,'Chk',b.sSlipType) AS sDocCodex " 
                            + " FROM Bank_Account_Master a " 
                            + "	LEFT JOIN Banks c ON a.sBankIDxx = c.sBankIDxx " 
                            + ", Branch_Bank_Account b ";
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sBnkActID = b.sBnkActID "
                                                        + " AND a.sBnkActID = " + SQLUtil.toSQL(fsBankAccountId)
                                                        + " AND b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
            }
            
            
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return  loRS.getString("sDocCodex");
                    }
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
                return  "";
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return  "";
        }
            
        return  "";
    }
    
    private JSONObject PrintCheck(Transaction tx, List<TransactionDetail> txd, DocumentMapping poDocumentMapping) throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            System.err.println("No default printer.");
            poJSON.put("result", "error");
            poJSON.put("message", "No default printer.");
            return poJSON;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            System.err.println("Cannot create job.");
            poJSON.put("result", "error");
            poJSON.put("message", "Cannot create job.");
            return poJSON;
        }

        PageLayout layout = printer.createPageLayout(Paper.NA_LETTER,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);

        // Get printable area
        double printableWidth = layout.getPrintableWidth();   // points
        double printableHeight = layout.getPrintableHeight();

        Node voucherNode = buildVoucherNode(tx,txd, printableWidth, printableHeight,poDocumentMapping);
        
//        // Get your content size
//        Bounds contentBounds = voucherNode.getBoundsInParent();
//
//        // Compute scale
//        double scaleX = printableWidth / contentBounds.getWidth();
//        double scaleY = printableHeight / contentBounds.getHeight();
//        double scale = Math.min(scaleX, scaleY);
//        //KEEP EXACT POSITIONING
//        voucherNode.setScaleX(1);
//        voucherNode.setScaleY(1);
//
//        //APPLY PRINTER OFFSET
//        voucherNode.setTranslateX(layout.getLeftMargin());
//        voucherNode.setTranslateY(layout.getTopMargin());

        job.getJobSettings().setPageLayout(layout);
        job.getJobSettings().setJobName("CheckDeposit-" + Master().getTransactionNo());

        boolean okay = job.printPage(layout, voucherNode);
        if (okay) {
            job.endJob();

            System.out.println("[SUCCESS] Printed transaction " + Master().getTransactionNo()
                    + " for " + tx.accountName
                    + " | Amount: ₱" + tx.nAmountxxValue);
            poJSON.put("result", "success");
        } else {
            job.cancelJob();
            System.err.println("[FAILED] Printing failed for transaction "+ Master().getTransactionNo());
            poJSON.put("result", "error");
            poJSON.put("message", "[FAILED] Printing failed for transaction " + Master().getTransactionNo());
            return poJSON;
        }
        
        return poJSON;
    }

    private boolean showPrintPreview(Transaction tx,List<TransactionDetail> txd, DocumentMapping poDocumentMapping) throws SQLException, GuanzonException, CloneNotSupportedException {
        Printer printer = Printer.getDefaultPrinter();
        PageLayout layout = printer.createPageLayout(Paper.NA_LETTER,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);

         // Get printable area
        double printableWidth = layout.getPrintableWidth();
        double printableHeight = layout.getPrintableHeight();
        
        Node voucher = buildVoucherNode(tx,txd, printableWidth, printableHeight,poDocumentMapping);
        
        // Get your content size
        Bounds contentBounds = voucher.getBoundsInParent();

        // Compute scale
        double scaleX = printableWidth / contentBounds.getWidth();
        double scaleY = printableHeight / contentBounds.getHeight();
        double scale = Math.min(scaleX, scaleY);
        //KEEP EXACT POSITIONING
        voucher.setScaleX(1);
        voucher.setScaleY(1);

        //APPLY PRINTER OFFSET
        voucher.setTranslateX(layout.getLeftMargin());
        voucher.setTranslateY(layout.getTopMargin());

        // Wrap in a Group so zooming keeps proportions if the user resizes the window
        Group zoomRoot = new Group(voucher);
        ScrollPane scroll = new ScrollPane(zoomRoot);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        Button btnPrint = new Button("Print");
        Button btnCancel = new Button("Cancel");
        btnPrint.setOnAction(e -> {
            ((Stage) btnPrint.getScene().getWindow()).setUserData(Boolean.TRUE);
            ((Stage) btnPrint.getScene().getWindow()).close();
        });
        btnCancel.setOnAction(e -> {
            ((Stage) btnPrint.getScene().getWindow()).setUserData(Boolean.FALSE);
            ((Stage) btnPrint.getScene().getWindow()).close();
        });
//        btnCancel.setOnAction(e -> ((Stage) btnCancel.getScene().getWindow()).close());

        HBox footer = new HBox(10, btnPrint, btnCancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Print Preview " + tx.accountNo);
        stage.setScene(new Scene(new BorderPane(scroll, null, null, footer, null), 820, 820));
        stage.showAndWait();

        return Boolean.TRUE.equals(stage.getUserData());
    }

    /**
     * Builds a voucher as a vector node hierarchy – no Canvas, no signature
     * box.
     */
    /**
     * Builds the voucher layout. Each text node is placed by (row, col) where
     * row = line number (0‑based) → Y = TOP + row * LINE_HEIGHT col = character
     * column (0‑based) → X = col * CHAR_WIDTH
     */
    private Node buildVoucherNode(Transaction tx, List<TransactionDetail> txd,
            double widthPts,
            double heightPts,
    DocumentMapping poDocumentMapping)
            throws SQLException, GuanzonException, CloneNotSupportedException {

        // Root container for all voucher text nodes
        Pane root = new Pane();
        root.setPrefSize(widthPts, heightPts);

        final double TOP_MARGIN = 21;   // distance from top edge to “row 0”
        final double LINE_HEIGHT = 18;   // row‑to‑row spacing
        final double CHAR_WIDTH = 7;    // col‑to‑col spacing
        
        //Transaction Detail
        int lnCtr= 0;
        for (TransactionDetail txd1 : txd) {
            for (int i = 0; i < poDocumentMapping.Detail().size(); i++) {
                String fieldName = poDocumentMapping.Detail(i).getFieldCode();
                String fontName = poDocumentMapping.Detail(i).getFontName();
                double fontSize = poDocumentMapping.Detail(i).getFontSize();
                double topRow = poDocumentMapping.Detail(i).getTopRow();
                double leftCol = poDocumentMapping.Detail(i).getLeftColumn();
                double colSpace = poDocumentMapping.Detail(i).getColumnSpace();
                double rowspace = poDocumentMapping.Detail(i).getRowSpace();
                double maxrow = poDocumentMapping.Detail(i).getMaxRow();
                
                if(lnCtr > 0){ //First loop for transaction detail
                    topRow = topRow + (rowspace*lnCtr);
                }
                
                // Determine font per field
                Font fieldFont = Font.font(fontName, fontSize);
                String textValue = "";
                switch (fieldName) {
                    //Transaction Detail
                    case "sBankIDxx":
                        textValue = txd1.sBank == null ? "" : txd1.sBank;
                        break;
                    case "sCheckNox":
                        textValue = txd1.sCheckNox == null ? "" : txd1.sCheckNox;
                        break;
                    case "nAmountxx":
                        textValue = CustomCommonUtil.setIntegerValueToDecimalFormat(txd1.nTotalAmount, false);
                        break;
                }

                //Alignment
                Text textNode = new Text(textValue == null ? "" : textValue);
                textNode.setFont(fieldFont);

                double x = leftCol * CHAR_WIDTH;
                double y = TOP_MARGIN + topRow * LINE_HEIGHT;
                double SAFE_RIGHT_MARGIN = 20; // tweak (10–30)
                double SAFE_BOTTOM_MARGIN = 20;

                double safeWidth = widthPts - SAFE_RIGHT_MARGIN;
                double safeHeight = heightPts - SAFE_BOTTOM_MARGIN;

                if ("nAmountxx".equals(fieldName)) {
                    // RIGHT ALIGN
                    Bounds bounds = textNode.getLayoutBounds();
//                    textNode.setX((x+colSpace) - bounds.getWidth());
                    double rightEdge = (leftCol * CHAR_WIDTH) + colSpace;

                    // enforce safe boundary
                    rightEdge = Math.min(rightEdge, safeWidth);

                    textNode.setX(rightEdge - bounds.getWidth());
                    textNode.setY(y);
                } else {
                    // LEFT ALIGN (default)
                    textNode.setX(x+colSpace);
                    textNode.setY(y);
                }
                
                textNode.setFont(fieldFont);
                root.getChildren().add(textNode);
                
            }
            lnCtr++;
        }
        
        //Transaction
        for (int i = 0; i < poDocumentMapping.Detail().size(); i++) {
            String fieldName = poDocumentMapping.Detail(i).getFieldCode();
            String fontName = poDocumentMapping.Detail(i).getFontName();
            double fontSize = poDocumentMapping.Detail(i).getFontSize();
            double topRow = poDocumentMapping.Detail(i).getTopRow();
            double leftCol = poDocumentMapping.Detail(i).getLeftColumn();
            double colSpace = poDocumentMapping.Detail(i).getColumnSpace();
            double rowspace = poDocumentMapping.Detail(i).getRowSpace();

            // Determine font per field
            Font fieldFont = Font.font(fontName, fontSize);
            String textValue = "";
            switch (fieldName) {
                case "sActNumbr":
                    int spaceCount = (int) Math.round(colSpace);
                    if (spaceCount < 0) {
                        throw new IllegalArgumentException("spaceCount must be non-negative");
                    }
                    if (spaceCount == 0) {
                        textValue = tx.accountNo == null ? "" : tx.accountNo.toUpperCase();
                    } else {
                        String rawDate = tx.accountNo == null ? "" : tx.accountNo;

                        textValue =
                                addSpaceBetweenChars(safeSubstring(rawDate, 0, 3), spaceCount)
                                + repeatSpace(spaceCount + 2)
                                + addSpaceBetweenChars(safeSubstring(rawDate, 3, 4), spaceCount)
                                + repeatSpace(spaceCount + 2)
                                + addSpaceBetweenChars(safeSubstring(rawDate, 4, 12), spaceCount)
                                + repeatSpace(spaceCount + 2)
                                + addSpaceBetweenChars(safeSubstring(rawDate, 12, 13), spaceCount);
                        System.out.println("textValue : " + textValue);
                    }
                    break;
                case "sActNamex":
                    textValue = tx.accountName == null ? "" : tx.accountName.toUpperCase();
                    break;
                case "dReferDte":
                    textValue = tx.transactionDate == null ? "" : tx.transactionDate;
                    break;
                case "nTtlChDep":
                    textValue = CustomCommonUtil.setIntegerValueToDecimalFormat(tx.nAmountxxValue, false);
                    break;
                case "nTotalDep":
                    textValue = CustomCommonUtil.setIntegerValueToDecimalFormat(tx.nAmountxxValue, false);
                    break;
            }
            
            double x = leftCol * CHAR_WIDTH;
            double y = TOP_MARGIN + topRow * LINE_HEIGHT;
            Text textNode = new Text(x, y, textValue == null ? "" : textValue);
            textNode.setFont(fieldFont);
            root.getChildren().add(textNode);
        }

        return root;
    }
    
    private String safeSubstring(String str, int start, int end) {
        if (str == null || str.length() <= start) {
            return "";
        }
    return str.substring(start, Math.min(end, str.length()));
}
    
    private static String addSpaceBetweenChars(String input, int spaceCount) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String space = repeatSpace(spaceCount);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            if (i < input.length() - 1) {
                sb.append(space);
            }
        }
        return sb.toString();
    }

    private static String repeatSpace(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
    
    
    private static class Transaction {
        final String accountNo, accountName, transactionDate;
        final BigDecimal nAmountxxValue;

        Transaction(String accountNo, String accountName, String transactionDate, BigDecimal nAmountxxValue) {
            this.accountNo = accountNo;
            this.accountName = accountName;
            this.transactionDate = transactionDate;
            this.nAmountxxValue = nAmountxxValue;
        }
    }
   
    public static class TransactionDetail {

        private final Integer nRowNo;
        private final String sBank;
        private final String sCheckNox;
        private final Double nTotalAmount;

        public TransactionDetail(Integer fnRowNo, String fsBank, String fsCheckNox, Double totalAmt) {
            this.nRowNo = fnRowNo;
            this.sBank = fsBank;
            this.sCheckNox = fsCheckNox;
            this.nTotalAmount = totalAmt;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsBank() {
            return sBank;
        }

        public String getsCheckNox() {
            return sCheckNox;
        }

        public Double getnTotalAmount() {
            return nTotalAmount;
        }
    }
    
    /**
    * Displays the status history of a transaction.
    *
    * Retrieves status records, maps status codes to readable text, and
    * shows them in the UI along with entry details.
    *
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if application-specific error occurs
    * @throws Exception for other unexpected errors
    */
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case CheckDepositStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case CheckDepositStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case CheckDepositStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case CheckDepositStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case CheckDepositStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    switch (stat) {
                        case CheckDepositStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case CheckDepositStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case CheckDepositStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case CheckDepositStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case CheckDepositStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                    }
            }
            crs.updateRow();
        }

        JSONObject loJSON = getEntryBy();
        String entryBy = "";
        String entryDate = "";

        if (isJSONSuccess(loJSON)) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        showStatusHistoryUI("Check Deposit", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    /**
     * Retrieves the user and timestamp of who created the current transaction.
     *
     * @return JSONObject containing "sCompnyNm" (user) and "sEntryDte" (timestamp)
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if application-specific error occurs
     */
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM "+Master().getTable()+" a "
                + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo()));
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsEntry = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsEntryDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON = setJSON("error", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    /**
     * Retrieves the company name of a system user based on user ID.
     *
     * @param fsId User ID to lookup
     * @return Company name of the user
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if application-specific error occurs
     */
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL = " SELECT b.sCompnyNm from xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId));
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    lsEntry = loRS.getString("sCompnyNm");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON = setJSON("error", e.getMessage());
        }
        return lsEntry;
    }
    
}
