/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.StringUtils;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.ClientInfo;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.parameter.*;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POReturn_Detail;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.rmj.cas.core.APTransaction;
import org.rmj.cas.core.GLTransaction;
import ph.com.guanzongroup.cas.cashflow.model.Model_AP_Payment_Adjustment;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cache_Payable_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Detail_Proposal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master_Proposal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Other_Payments;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payee;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Master;
import ph.com.guanzongroup.cas.cashflow.model.Model_Withholding_Tax_Deductions;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;
import ph.com.guanzongroup.cas.cashflow.status.CheckStatus;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.status.JournalProposalStatus;
import ph.com.guanzongroup.cas.cashflow.status.OtherPaymentStatus;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatic;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;
import ph.com.guanzongroup.cas.cashflow.utility.CustomJasperViewerReports;
import ph.com.guanzongroup.cas.cashflow.utility.NumberToWords;
import ph.com.guanzongroup.cas.cashflow.validator.DisbursementValidator;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jasperreports.engine.xml.JRXmlWriter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

/**
 *
 * @author TJ; Arsiela 10-17-2025 
 */
public class DisbursementVoucher extends Transaction {
    public String psIndustryId = "";
    public String psCompanyId = "";
    public String psCategorCd = "";
    public String psBranch = "";
    public String psIndustry = "";
    public String psClient = "";
    public String psPayee = "";
    public String psBankName = "";
    public String psBankAccountNo = "";
    public String psParticular = "";
    public String psApprover = "";
    public boolean pbIsUpdateAmountPaid = false;
    public boolean pbPrint = false;

    private String dfrom;
    private String dthru;
    
    public OtherPayments poOtherPayments;
    public CheckPayments poCheckPayments;
    public BankAccountMaster poBankAccount;
    public Journal poJournal;
    public List<WithholdingTaxDeductions> paWTaxDeductions;
    public List<JournalProposal> paJournalProposal;
    
    public List<Model> paMaster;
    public List<TransactionAttachment> paAttachments;
    /**
    * Initializes a disbursement transaction by setting up models, controllers,
    * and required data collections.
    *
    * @return initialized transaction data as a {@link JSONObject}
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if initialization fails due to business logic
    */
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "DISb";

        poMaster = new CashflowModels(poGRider).DisbursementMaster();
        poDetail = new CashflowModels(poGRider).DisbursementDetail();
        poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
        poCheckPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        poOtherPayments = new CashflowControllers(poGRider, logwrapr).OtherPayments();
        poBankAccount = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
        
        paMaster = new ArrayList<Model>();
        paWTaxDeductions = new ArrayList<WithholdingTaxDeductions>();
        paAttachments = new ArrayList<>();
        
        paJournalProposal = new ArrayList<JournalProposal>(); //Arsiela - 06092026
        
        return initialize();
    }
    
    //Transaction Source Code 
    @Override
    public String getSourceCode() { return SOURCE_CODE; }
    //Set value for private strings used in searching / filtering data
    public void setIndustryID(String industryId) { psIndustryId = industryId; }
    public void setCompanyID(String companyId) { psCompanyId = companyId; }
    public void setCategoryID(String categoryId) { psCategorCd = categoryId; }
    public void setSearchIndustry(String industry) { psIndustry = industry; }
    public void setSearchBranch(String branch) { psBranch = branch; }
    public void setSearchClient(String clientName) { psClient = clientName; }
    public void setSearchPayee(String payeeName) { psPayee = payeeName; }
    public void setSearchParticular(String particular) { psParticular = particular; }
    public void setSearchBankName(String bankName) { psBankName = bankName; }
    public void setSearchBankAccountNo(String bankAccountNo) { psBankAccountNo = bankAccountNo; }
    public String getSearchIndustry() { return psIndustry; }
    public String getSearchBranch() { return psBranch; }
    public String getSearchClient() { return psClient; }
    public String getSearchPayee() { return psPayee; }
    public String getSearchParticular() { return psParticular; }
    public String getSearchBankName() { return psBankName; }
    public String getSearchBankAccountNo() { return psBankAccountNo; }
    
    /**
    * Creates a new disbursement transaction after validating configuration
    * and user authorization, then initializes default values.
    *
    * @return transaction result as a {@link JSONObject}
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic validation fails
    */
    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        //Clear Data
        resetMaster();
        Detail().clear();
        resetJournal();
        resetCheckPayment();
        resetOtherPayment();
        WTaxDeduction().clear();
        
        if(System.getProperty("sys.dept.finance") == null || "".equals(System.getProperty("sys.dept.finance"))){
            poJSON.put("result", "error");
            poJSON.put("message", "The Finance Department configuration is missing. This field is required to proceed.\nPlease contact your system administrator for assistance.");
            return poJSON;
        }
        
        String lsUserId = poGRider.getUserID();
        String lsPosition = checkPosition(DisbursementStatic.OPEN, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not authorized to create a disbursement voucher." );
            return poJSON;
        }
        
        poJSON = newTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        try {
            //Generate Voucher Number
            Master().setVoucherNo(getVoucherNo());
            //Populate check by default
            poJSON = populateCheck();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        } catch (SQLException | GuanzonException | ScriptException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        initFields();
        return poJSON;
    }
    
    /**
     * Saves the current transaction.
     *
     * @return result as a {@link JSONObject}
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if saving fails due to business logic
     * @throws CloneNotSupportedException if cloning fails
     */
    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }
    
    /**
     * Opens an existing transaction and loads all related data including journal,
     * tax deductions, attachments, and payment details.
     *
     * @param transactionNo the transaction number to open
     * @return result as a {@link JSONObject}
     * @throws CloneNotSupportedException if cloning fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic fails
     * @throws ScriptException if script processing fails
     */
    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        //Reset Transaction
        resetTransaction();
        pbIsUpdateAmountPaid = false; //reset the value of update amount paid
        
        poJSON = openTransaction(transactionNo);
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateJournal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadJournalProposal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal proposal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateWithholdingTaxDeduction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load withholding tax deduction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadAttachments();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        switch(Master().getDisbursementType()){
            case DisbursementStatic.DisbursementType.CHECK:
            case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                    poJSON = populateCheck();
                    if (!"success".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", "Unable to load check payment.\n" + (String) poJSON.get("message"));
                        return poJSON;
                    }
                break;
            case DisbursementStatic.DisbursementType.WIRED:
            case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                    poJSON = populateOtherPayment();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        poJSON.put("message", "Unable to load other payment.\n" + (String) poJSON.get("message"));
                        return poJSON;
                    }
                break;
        }
        computeFields(false); //Recompute fields
        return poJSON;
    }
    
    /**
     * Updates the current transaction after validating configuration and
     * reloads related data such as journal, tax deductions, and payment details.
     *
     * @return result as a {@link JSONObject}
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic fails
     * @throws CloneNotSupportedException if cloning fails
     * @throws ScriptException if script processing fails
     */
    public JSONObject UpdateTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        
        if(System.getProperty("sys.dept.finance") == null || "".equals(System.getProperty("sys.dept.finance"))){
            poJSON.put("result", "error");
            poJSON.put("message", "The Finance Department configuration is missing. This field is required to proceed.\nPlease contact your system administrator for assistance.");
            return poJSON;
        }
        
        poJSON = updateTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateJournal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = loadJournalProposal();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load journal proposal.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        poJSON = populateWithholdingTaxDeduction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load withholding tax deduction.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        switch(Master().getDisbursementType()){
            case DisbursementStatic.DisbursementType.CHECK:
            case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                    poJSON = populateCheck();
                    if (!"success".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", "Unable to load check payment.\n" + (String) poJSON.get("message"));
                        return poJSON;
                    }
                break;
            case DisbursementStatic.DisbursementType.WIRED:
            case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                    poJSON = populateOtherPayment();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        poJSON.put("message", "Unable to load other payment.\n" + (String) poJSON.get("message"));
                        return poJSON;
                    }
                break;
        }
        
        return poJSON;
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
            psApprover = (String) poJSON.get("sUserIDxx");
        }   
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    /**
    * Sets the form identifier for the transaction.
    *
    * @param fsForm the form name or code
    */
    private String psForm = "";
    public void setForm(String fsForm){
        psForm = fsForm;
    }
    
    /**
    * Validates if the transaction can be updated based on its current status
    * and form context, reloading data if status has changed.
    *
    * @param isEntry true if called during entry mode, false otherwise
    * @return result as a {@link JSONObject}
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject checkUpdateTransaction(boolean isEntry) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }

        switch(loObject.getTransactionStatus()){
            case DisbursementStatic.VOID:
            case DisbursementStatic.CANCELLED:
            case DisbursementStatic.DISAPPROVED:
            case DisbursementStatic.CERTIFIED:
            case DisbursementStatic.AUTHORIZED:
                poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"\nCheck transaction history.");
                poJSON.put("result", "error");
                return poJSON;
            case DisbursementStatic.VERIFIED:
            case DisbursementStatic.APPROVED:
            case DisbursementStatic.CONFIRMED:
                if(isEntry){
                    poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                    poJSON.put("result", "error");
                    return poJSON;
                } else {
                    //Check FORM
                    if(DisbursementStatic.CONFIRMED.equals(psForm)){
                        if(!DisbursementStatic.CONFIRMED.equals(loObject.getTransactionStatus())
                            && !DisbursementStatic.OPEN.equals(loObject.getTransactionStatus())){
                            poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                            poJSON.put("result", "error");
                            return poJSON;
                        }
                    } else if(DisbursementStatic.VERIFIED.equals(psForm)){
                        if(!DisbursementStatic.VERIFIED.equals(loObject.getTransactionStatus())
                            && !DisbursementStatic.CONFIRMED.equals(loObject.getTransactionStatus())){
                            poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                            poJSON.put("result", "error");
                            return poJSON;
                        }
                    } else if(DisbursementStatic.APPROVED.equals(psForm)){
                        if(!DisbursementStatic.APPROVED.equals(loObject.getTransactionStatus())
                            && !DisbursementStatic.VERIFIED.equals(loObject.getTransactionStatus())){
                            poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                            poJSON.put("result", "error");
                            return poJSON;
                        }
                    }
                }
                break;
                
            case DisbursementStatic.RETURNED_I:
                if(!DisbursementStatic.APPROVED.equals(psForm)){
                    poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                    poJSON.put("result", "error");
                    return poJSON;
                }
                break;
            case DisbursementStatic.RETURNED:
                if(!DisbursementStatic.CONFIRMED.equals(psForm)){
                    poJSON.put("message", "Transaction status was already "+getStatus(loObject.getTransactionStatus())+"!\nCheck transaction history.");
                    poJSON.put("result", "error");
                    return poJSON;
                }
                break;
        }
        
        if(!loObject.getTransactionStatus().equals(Master().getTransactionStatus())){
            poJSON = OpenTransaction(Master().getTransactionNo());
            if (!"success".equals((String) poJSON.get("result"))) {
                poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
                return poJSON;
            }
        }
        //Clear value
        psForm = "";
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Returns the descriptive label of a transaction status code.
    *
    * @param lsStatus the status code
    * @return corresponding status description
    */
    public String getStatus(String lsStatus){
        switch(lsStatus){
            case DisbursementStatic.VOID:
                return "Voided";
            case DisbursementStatic.CANCELLED:
                return "Cancelled";
            case DisbursementStatic.DISAPPROVED:
                return "Dis-approved";
            case DisbursementStatic.VERIFIED:
                return "Verified";
            case DisbursementStatic.CERTIFIED:
                return "Certified";
            case DisbursementStatic.AUTHORIZED:
                return "Authorized";
            case DisbursementStatic.RETURNED_I:
            case DisbursementStatic.RETURNED:
                return "Returned";
            case DisbursementStatic.APPROVED:
                return "Approved";
            case DisbursementStatic.CONFIRMED:
                return "Confirmed";
            default:
                return "Open";
        }
    }
    
    /**
    * Checks if a transition from the current status to the target status is allowed.
    *
    * @param current current transaction status
    * @param target target status to transition into
    * @return true if transition is allowed, false otherwise
    */
    public boolean isAllowed(String current, String target) {
        switch (target) {
            case DisbursementStatic.RETURNED_I:
                return current.equals(DisbursementStatic.CERTIFIED)
                    || current.equals(DisbursementStatic.APPROVED)
                    || current.equals(DisbursementStatic.CANCELLED);
            case DisbursementStatic.RETURNED:
                return current.equals(DisbursementStatic.VERIFIED)
                    || current.equals(DisbursementStatic.RETURNED_I)
                    || current.equals(DisbursementStatic.CANCELLED);

            case DisbursementStatic.CANCELLED:
                return current.equals(DisbursementStatic.CONFIRMED)
                    || current.equals(DisbursementStatic.RETURNED);

            case DisbursementStatic.VOID:
                return current.equals(DisbursementStatic.OPEN);

            case DisbursementStatic.DISAPPROVED:
                return current.equals(DisbursementStatic.VERIFIED)
                    || current.equals(DisbursementStatic.CONFIRMED)
                    || current.equals(DisbursementStatic.APPROVED)
                    || current.equals(DisbursementStatic.RETURNED_I)
                    || current.equals(DisbursementStatic.CERTIFIED);

            case DisbursementStatic.CONFIRMED:
                return current.equals(DisbursementStatic.OPEN)
                    || current.equals(DisbursementStatic.RETURNED);

            case DisbursementStatic.VERIFIED:
                return current.equals(DisbursementStatic.CONFIRMED);
                
            case DisbursementStatic.APPROVED:
                return current.equals(DisbursementStatic.VERIFIED)
                    || current.equals(DisbursementStatic.RETURNED_I);
                
            case DisbursementStatic.CERTIFIED:
                return current.equals(DisbursementStatic.APPROVED);

            case DisbursementStatic.AUTHORIZED:
                return current.equals(DisbursementStatic.CERTIFIED);

            default:
                return false;
        }
    }
    
    /**
     * Update Disbursement Linked Payment Status
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws ParseException
     * @throws CloneNotSupportedException 
     */
    public JSONObject updatePaymentsStatus() throws SQLException, GuanzonException, ParseException, CloneNotSupportedException{
        poJSON = new JSONObject();
        String lsCheck = "";
        //If DV Type is Check : VOID linked OTHER PAYMENT
        if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
            || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
            lsCheck = existOtherPayments();
            if(lsCheck != null && !"".equals(lsCheck)){
                OtherPayments loObject = new CashflowControllers(poGRider, logwrapr).OtherPayments();
                loObject.initialize();
                loObject.setWithParentClass(true);
                poJSON = loObject.openRecord(lsCheck);
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                poJSON = loObject.VoidTransaction("");
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }
        } else {
            //Else DV Type is OTHER Payment : VOID linked CHECK PAYMENT
            lsCheck = existCheckPayments();
            if(lsCheck != null && !"".equals(lsCheck)){
                CheckPayments loObject = new CashflowControllers(poGRider, logwrapr).CheckPayments();
                loObject.initialize();
                loObject.setWithParentClass(true);
                poJSON = loObject.openRecord(lsCheck);
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                poJSON = loObject.VoidTransaction("");
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "Payment Update Status Successfully.");
        return poJSON;
    }
    
    /**
    * Retrieves the employee ID linked to a system user ID.
    *
    * @param fsUserId system user ID
    * @return employee number if found, otherwise empty string
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if query execution fails
    */
    public String getEmployeeID(String fsUserId) throws SQLException, GuanzonException {
        String lsUpdateBy = "";
        String lsDate = "";
        String lsSQL = "SELECT a.sEmployNo, a.sUserIDxx FROM xxxSysUser a ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx = "+ SQLUtil.toSQL(fsUserId)) ;
        System.out.println("Execute SQL Employee ID : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sEmployNo") != null && !"".equals(loRS.getString("sEmployNo"))){
                    return loRS.getString("sEmployNo");
                }
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
          return "";
        } 
        
        poJSON.put("result", "success");
        return "";
    }
    
    /**
    * Checks if a user has an allowed position for a specific transaction status.
    *
    * @param fsStatus transaction status
    * @param fsUserId user ID
    * @return position name if authorized, otherwise empty string
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if query execution fails
    */
    public String checkPosition(String fsStatus, String fsUserId) throws SQLException, GuanzonException{
        String lsSQL = " SELECT   " +
                    "  a.sUserIDxx, " +
                    "  d.sCompnyNm, " +
                    "  e.sDeptName, " +
                    "  c.sPositnNm, " +
                    "  b.dFiredxxx, " +
                    "  b.sDeptIDxx, " +
                    "  b.sPositnID " +
                    "FROM xxxSysUser a " +
                    "LEFT JOIN Employee_Master001 b ON b.sEmployID = a.sEmployNo " +
                    "LEFT JOIN Position c ON c.sPositnID = b.sPositnID  " +
                    "LEFT JOIN Client_Master d ON d.sClientID = b.sEmployID  " +
                    "LEFT JOIN Department e ON e.sDeptIDxx = b.sDeptIDxx  ";
        
        lsSQL = MiscUtil.addCondition(lsSQL,
                " a.sUserIDxx = " + SQLUtil.toSQL(fsUserId)
                + " AND b.sDeptIDxx = " + SQLUtil.toSQL(System.getProperty("sys.dept.finance")) 
                 );
        String lsPosition = "";
        switch(fsStatus){
            //Who can entry/update and confirmed the transaction
            case DisbursementStatic.OPEN:
            case DisbursementStatic.CONFIRMED:
                lsPosition = "%Payable%";
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition) );
                break;
            case DisbursementStatic.VERIFIED:
            case DisbursementStatic.APPROVED:
                //Who can verify and approve the transaction
                lsPosition = "%Account%";
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition) 
                                                + " AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%") 
                );
                break;
            case DisbursementStatic.CERTIFIED:
            case DisbursementStatic.AUTHORIZED:
                //Who can certify and authorized the transaction
                lsPosition = "%Manager%";
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL(lsPosition)
                                        + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Head%") 
                                        + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Executive%") 
                );
                break;
            case DisbursementStatic.RETURNED:
                //Who can return the transaction
                lsSQL = MiscUtil.addCondition(lsSQL, " ( c.sPositnNm LIKE " + SQLUtil.toSQL("%Account%") 
                                    + " ) AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%") 
                                    );
                break;
            case DisbursementStatic.RETURNED_I:
                //Who can initial return the transaction
                lsSQL = MiscUtil.addCondition(lsSQL, " (  c.sPositnNm LIKE " + SQLUtil.toSQL("%Manager%") 
                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Head%") 
                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Executive%") 
                                    + " ) AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%") 
                                    );
                break;
            case DisbursementStatic.DISAPPROVED:
                //Who can disapproved the transaction
                lsSQL = MiscUtil.addCondition(lsSQL, " (  c.sPositnNm LIKE " + SQLUtil.toSQL("%Manager%") 
                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Account%") 
                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Head%") 
                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Executive%") 
                                    + " ) AND c.sPositnNm NOT LIKE " + SQLUtil.toSQL("%Payable%") 
                                    );
                break;
            case DisbursementStatic.VOID:
            case DisbursementStatic.CANCELLED:
                //Who can VOID / Cancel Transaction
                lsSQL = MiscUtil.addCondition(lsSQL, " c.sPositnNm LIKE " + SQLUtil.toSQL("%Payable%") 
                                    );
//            case DisbursementStatic.CANCELLED:
//                lsSQL = MiscUtil.addCondition(lsSQL, "  c.sPositnNm LIKE " + SQLUtil.toSQL("%Manager%") 
//                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%General%Account%") 
//                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Account%Payable%") 
//                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Head%") 
//                                    + " OR c.sPositnNm LIKE " + SQLUtil.toSQL("%Executive%") 
//                                    );
//                break;
        }
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sPositnNm") != null && !"".equals(loRS.getString("sPositnNm"))){
                        return loRS.getString("sPositnNm");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return "";
    }
    
    /*Update Transaction Status*/
    
    /**
    * Confirms a disbursement transaction after validating status, payments,
    * user authorization, and required approvals.
    *
    * @param remarks confirmation remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = DisbursementStatic.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }
        
        switch(Master().getDisbursementType()){
            case DisbursementStatic.DisbursementType.CHECK:
            case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                if(existCheckPayments() == null || "".equals(existCheckPayments())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "No active check payment linked. Please update the disbursement voucher.");
                    return poJSON;
                }
                break;
            case DisbursementStatic.DisbursementType.WIRED:
                if(existOtherPayments() == null || "".equals(existOtherPayments())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "No active bank transfer payment linked. Please update the disbursement voucher.");
                    return poJSON;
                }
                break;
            case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                if(existOtherPayments() == null || "".equals(existOtherPayments())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "No active digital payment linked. Please update the disbursement voucher.");
                    return poJSON;
                }
                break;
        }   
        
        //validator
        poJSON = isEntryOkay(DisbursementStatic.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //1. Check the position of the current user
        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
        if(lsPosition1 == null || "".equals(lsPosition1) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        //2. Check the position of the approving officer
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    /**
    * Verifies a disbursement transaction after validating its status and user authorization.
    *
    * @param remarks verification remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject VerifyTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = DisbursementStatic.VERIFIED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }
        
        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = verifyJournals(lsStatus,false);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Check Department Section
        String lsUserId = poGRider.getUserID();
//        poJSON = callApproval();
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        } 
//        if(psApprover != null && !"".equals(psApprover)){
//            lsUserId = psApprover;
//        }
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction verified successfully.");
        return poJSON;
    }
    
    /**
    * Approves a disbursement transaction after validating status, authorization,
    * and related transactions, then commits the update.
    *
    * @param remarks approval remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject ApproveTransaction(String remarks)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        
        String lsStatus = DisbursementStatic.APPROVED;
        
        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = verifyJournals(lsStatus,false);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
                 
        //Update Journal Proposal
//        for(int lnCtr = 0; lnCtr < getJournalProposalList().size(); lnCtr++){
//            JournalProposal loObj = JournalProposal(lnCtr);
//            if(JournalProposalStatus.OPEN.equals(loObj.Master().getTransactionStatus())){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Journal proposal <"+loObj.Master().getTransactionNo()+"> at row "+(lnCtr+1)+" must be confirmed before approving the disbursement voucher.");
//                return poJSON;
//            }
//        }
        
//        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ApproveTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Update Related transaction to DV ex. JE
        poJSON = updateRelatedTransactions(lsStatus,lsUserId);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction approved successfully.");
        return poJSON;
    }
    
    /**
     * Certifies one or more disbursement transactions after validating approval,
     * authorization, and entry rules, then updates their status.
     *
     * @param remarks certification remarks
     * @param fasTransactionNo list of transaction numbers to certify
     * @return result as a {@link JSONObject}
     * @throws ParseException if parsing fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic fails
     * @throws CloneNotSupportedException if cloning fails
     * @throws ScriptException if script processing fails
     */
    public JSONObject CertifyTransaction(String remarks,List<String> fasTransactionNo)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        
        String lsStatus = DisbursementStatic.CERTIFIED;
        
//        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        for(int lnCtr = 0; lnCtr <= fasTransactionNo.size() - 1; lnCtr++){
            poJSON = OpenTransaction(fasTransactionNo.get(lnCtr));
            if (!"success".equals(poJSON.get("result"))) {
                return poJSON;
            }
            
            if (getEditMode() != EditMode.READY) {
                poJSON.put("result", "error");
                poJSON.put("message", "No transacton was loaded.");
                return poJSON;
            }
            
            if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
                return poJSON;
            }

            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        
            poJSON = verifyJournals(lsStatus,false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poGRider.beginTrans("UPDATE STATUS", "CertifyTransaction", SOURCE_CODE, Master().getTransactionNo());

            //Update Related transaction to DV ex. JE
//            poJSON = updateRelatedTransactions(lsStatus);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                poGRider.rollbackTrans();
//                return poJSON;
//            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction certified successfully.");
        return poJSON;
    }
    
    /**
    * Authorizes one or more disbursement transactions after validating approval,
    * user role, and linked/related transactions, then updates their status.
    *
    * @param remarks authorization remarks
    * @param fasTransactionNo list of transaction numbers to authorize
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject AuthorizeTransaction(String remarks,List<String> fasTransactionNo)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.AUTHORIZED;
//        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        for(int lnCtr = 0; lnCtr <= fasTransactionNo.size() - 1; lnCtr++){
            poJSON = OpenTransaction(fasTransactionNo.get(lnCtr));
            if (!"success".equals(poJSON.get("result"))) {
                return poJSON;
            }
            
            if (getEditMode() != EditMode.READY) {
                poJSON.put("result", "error");
                poJSON.put("message", "No transacton was loaded.");
                return poJSON;
            }
            
            if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
                return poJSON;
            }
        
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        
            poJSON = verifyJournals(lsStatus,false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poGRider.beginTrans("UPDATE STATUS", "AuthorizeTransaction", SOURCE_CODE, Master().getTransactionNo());

            //Update Related transaction to DV
            poJSON = updateLinkedTransactions(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //Update Related transaction to DV
            poJSON = updateRelatedTransactions(lsStatus,lsUserId);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction authorized successfully.");
        return poJSON;
    }
    
    /**
     * Disapproves one or more disbursement transactions after validating user
     * authorization, entry rules, and linked/related transactions, then updates status.
     *
     * @param remarks disapproval remarks
     * @param fasTransactionNo list of transaction numbers to disapprove
     * @return result as a {@link JSONObject}
     * @throws ParseException if parsing fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic fails
     * @throws CloneNotSupportedException if cloning fails
     * @throws ScriptException if script processing fails
     */
    public JSONObject DisApproveTransaction(String remarks,List<String> fasTransactionNo)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.DISAPPROVED;
        
        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        for(int lnCtr = 0; lnCtr <= fasTransactionNo.size() - 1; lnCtr++){
            poJSON = OpenTransaction(fasTransactionNo.get(lnCtr));
            if (!"success".equals(poJSON.get("result"))) {
                return poJSON;
            }
            
            if (getEditMode() != EditMode.READY) {
                poJSON.put("result", "error");
                poJSON.put("message", "No transacton was loaded.");
                return poJSON;
            }
            
            if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
                return poJSON;
            }
        
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poGRider.beginTrans("UPDATE STATUS", "DisApproveTransaction", SOURCE_CODE, Master().getTransactionNo());

            //Update Related transaction to DV
            poJSON = updateLinkedTransactions(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //Update Related transaction to DV
            poJSON = updateRelatedTransactions(lsStatus,lsUserId);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction disapproved successfully.");
        return poJSON;
    }
    
    /**
    * Disapproves a disbursement transaction after validating user authorization,
    * current status, and related transactions, then updates its status.
    *
    * @param remarks disapproval remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject DisApproveTransaction(String remarks)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.DISAPPROVED;
        
        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "DisApproveTransaction", SOURCE_CODE, Master().getTransactionNo());

        //Update Related transaction to DV
        poJSON = updateLinkedTransactions(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,lsUserId);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction disapproved successfully.");
        return poJSON;
    }
    
    /**
    * Voids a disbursement transaction after validating status, user authorization,
    * and related/linked transactions, then updates its status.
    *
    * @param remarks voiding remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */   
    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
            
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        //Allow to void transaction only if user is from account payable
        String lsPosition = checkPosition(lsStatus, poGRider.getUserID());
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        } 
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(DisbursementStatic.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Linked transaction to DV
        poJSON = updateLinkedTransactions(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,poGRider.getUserID());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
        return poJSON;
    }
    
    /**
    * Cancels a disbursement transaction after validating current status,
    * user authorization, approval hierarchy, and related transactions.
    *
    * @param remarks cancellation remarks
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.CANCELLED;

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
        
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //1. Check the position of the current user
        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
        if(lsPosition1 == null || "".equals(lsPosition1) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Linked transaction to DV
        poJSON = updateLinkedTransactions(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,lsUserId);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");

        return poJSON;
    }
    
    /**
     * Returns a disbursement transaction after validating status, user authorization,
     * approval level, and related transactions. Handles both initial and normal return cases.
     *
     * @param remarks return remarks
     * @return result as a {@link JSONObject}
     * @throws ParseException if parsing fails
     * @throws SQLException if a database error occurs
     * @throws GuanzonException if business logic fails
     * @throws CloneNotSupportedException if cloning fails
     * @throws ScriptException if script processing fails
     */
    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        
        Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poJSON.put("message", "Unable to load disbursement.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        
//        String lsCurrStat = loObject.getTransactionStatus();
//        if("ABCDEFGHIJ".contains(lsCurrStat)){
//            lsCurrStat = String.valueOf(lsCurrStat.getBytes()[0] - 64);
//        }
        switch(loObject.getTransactionStatus()){
            case DisbursementStatic.CERTIFIED:
            case DisbursementStatic.APPROVED:
                lsStatus = DisbursementStatic.RETURNED_I;
            break; 
        }
        
        if (!isAllowed(loObject.getTransactionStatus(), lsStatus)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already "+getStatus(loObject.getTransactionStatus())+".");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
//        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //Update Linked transaction to DV
        poJSON = updateLinkedTransactions(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Update Related transaction to DV
        poJSON = updateRelatedTransactions(lsStatus,lsUserId);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");

        return poJSON;
    }
    
    /**
    * Returns multiple disbursement transactions after validating approval authority,
    * status rules, and linked/related transactions. Processes each transaction in sequence.
    *
    * @param remarks return remarks
    * @param fasTransactionNo list of transaction numbers to return
    * @return result as a {@link JSONObject}
    * @throws ParseException if parsing fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws CloneNotSupportedException if cloning fails
    * @throws ScriptException if script processing fails
    */
    public JSONObject ReturnTransaction(String remarks,List<String> fasTransactionNo)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        String lsStatus = DisbursementStatic.RETURNED_I;
        
//        //1. Check the position of the current user
//        String lsPosition1 = checkPosition(lsStatus, poGRider.getUserID());
//        if(lsPosition1 == null || "".equals(lsPosition1) ){
//            poJSON.put("result", "error" );
//            poJSON.put("message", "User is not an authorized officer." );
//            return poJSON;
//        }
        
        String lsUserId = poGRider.getUserID();
        poJSON = callApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        } 
        if(psApprover != null && !"".equals(psApprover)){
            lsUserId = psApprover;
        }
        //2. Check the position of the approving officer
        String lsPosition = checkPosition(lsStatus, lsUserId);
        if(lsPosition == null || "".equals(lsPosition) ){
            poJSON.put("result", "error" );
            poJSON.put("message", "User is not an authorized officer." );
            return poJSON;
        }
        
        for(int lnCtr = 0; lnCtr <= fasTransactionNo.size() - 1; lnCtr++){
            poJSON = OpenTransaction(fasTransactionNo.get(lnCtr));
            if (!"success".equals(poJSON.get("result"))) {
                return poJSON;
            }
            
            if (getEditMode() != EditMode.READY) {
                poJSON.put("result", "error");
                poJSON.put("message", "No transacton was loaded.");
                return poJSON;
            }
            
            if (!isAllowed((String) poMaster.getValue("cTranStat"), lsStatus)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Transaction was already "+getStatus((String) poMaster.getValue("cTranStat"))+".");
                return poJSON;
            }
        
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

            //Update Related transaction to DV
            poJSON = updateLinkedTransactions(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //Update Related transaction to DV
            poJSON = updateRelatedTransactions(lsStatus,lsUserId);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }
    
    /*Search Master References*/
    
    /**
    * Searches disbursement transactions based on filters (company, branch, payee, client,
    * and transaction status) and opens the selected record.
    *
    * @return result as a {@link JSONObject}
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws ScriptException if script execution fails
    */
    public JSONObject SearchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        String lsTransStat = "";
        if(psTranStat != null){
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
//                        " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                         " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%" + psBranch)
                        + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + psPayee)
                        + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + psClient) 
                        + " ) "
                        );
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED xxx : " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction No»Transaction Date»DV No»Branch»Supplier",
                "a.sTransNox»a.dTransact»a.sVouchrNo»c.sBranchNm»supplier",
                "a.sTransNox»a.dTransact»a.sVouchrNo»IFNULL(c.sBranchNm, '')»IFNULL(e.sCompnyNm, IFNULL(d.sPayeeNme, ''))",
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
    
    /**
    * Searches disbursement transactions filtered by reference number and other
    * criteria (company, payee, and status), then opens the selected record.
    *
    * @param fsReferenceNo reference/transaction number filter
     * @param fsPayee
     * @param sortBy
    * @return result as a {@link JSONObject}
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if business logic fails
    * @throws ScriptException if script execution fails
    */
    public JSONObject SearchTransaction(String fsReferenceNo, String fsPayee, int sortBy) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();
        if(fsReferenceNo == null) { fsReferenceNo = ""; }
        if(fsPayee == null) { fsPayee = ""; }
        if(sortBy < 0 ) { sortBy = 0; }
        String lsTransStat = "";
        String lsValue = "";
//        switch(sortBy){
//            case 2:
//                lsValue = fsReferenceNo;
//                break;
//            case 4:
//                lsValue = fsPayee;
//                break;
//            default:
//                lsValue = "";
//        }
        if(psTranStat != null){
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
//                        " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                         " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferenceNo)
                        + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsPayee+"%")
                        + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsPayee+"%")
                        + " ) "
                        );
        if (!lsTransStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED xxx : " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                lsValue,
                "Transaction No»Transaction Date»DV No»Branch»Supplier",
                "a.sTransNox»a.dTransact»a.sVouchrNo»c.sBranchNm»supplier",
                "a.sTransNox»a.dTransact»a.sVouchrNo»IFNULL(c.sBranchNm, '')»IFNULL(e.sCompnyNm, IFNULL(d.sPayeeNme, ''))",
                sortBy);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    /**
    * Searches for an Industry record based on a value.
    *
    * @param value   the search keyword or code
    * @param byCode  true if searching by industry code, false if by description
    * @return JSONObject containing the result of the search operation
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            setSearchIndustry(object.getModel().getDescription());
        }

        return poJSON;
    }
    
    /**
    * Searches for a Branch record and updates relevant fields based on the result.
    *
    * @param value     the search keyword or branch code
    * @param byCode    true if searching by branch code, false if by branch name
    * @param isSearch  true to set search field, false to assign branch to master record
    * @return JSONObject containing the result of the search operation
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchBranch(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBranch(object.getModel().getBranchName());
            } else {
                Master().setBranchCode(object.getModel().getBranchCode());
                System.out.println("Branch : " +  Master().Branch().getBranchName());
            }
        }

        return poJSON;
    }
    
    /**
    * Searches for a Branch record based on a value.
    *
    * @param value     the search keyword or branch code
    * @param byCode    true if searching by branch code, false if by branch name
    * @param isSearch  true to set search display value, false to assign to master record
    * @return JSONObject containing the result of the search operation
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchPayee(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus(RecordStatus.ACTIVE);
        if(value == null){
            value = "";
        }
        System.out.println(" value " + value);
        System.out.println(" getSupplierClientID " + Master().getSupplierClientID());
        
        if(Master().getSupplierClientID().equals(Master().Payee().getAPClientID())){
            poJSON = object.searchRecordbyAPClientID(value, byCode);
        } else {
            poJSON = object.searchRecordbyClientID(value, byCode);
        }
        
//        poJSON = object.searchRecordbyAPClientID(value, Master().getSupplierClientID(), false);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchPayee(object.getModel().getPayeeName()); 
                setSearchClient(object.getModel().APClient().getCompanyName());
                if(object.getModel().APClient().getCompanyName() == null || "".equals(object.getModel().APClient().getCompanyName())){
                    setSearchClient(object.getModel().Client().getCompanyName());
                }
            } else {
                Master().setPayeeID(object.getModel().getPayeeID());
                System.out.println("Payee : " +  Master().Payee().getPayeeName());
                if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
                    || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
                    CheckPayments().getModel().setPayeeID(Master().getPayeeID());
                }
            }
        }

        return poJSON;
    }

    /**
    * Searches for a Supplier (Payee) record based on a company value.
    *
    * @param value     the search keyword or company name/code
    * @param byCode    true if searching by code, false if by company name
    * @param isSearch  true to set search display values only, false to assign values to master record
    * @return JSONObject containing the result of the search operation
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchSupplier(String value, boolean byCode, boolean isSearch) throws SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecordbyCompany(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchPayee(object.getModel().getPayeeName());
//                setSearchClient(object.getModel().Client().getCompanyName());
                setSearchClient(object.getModel().APClient().getCompanyName());
                if(object.getModel().APClient().getCompanyName() == null || "".equals(object.getModel().APClient().getCompanyName())){
                    setSearchClient(object.getModel().Client().getCompanyName());
                }
            } else {
                setSearchPayee(object.getModel().getPayeeName());
//                setSearchClient(object.getModel().Client().getCompanyName());
                setSearchClient(object.getModel().APClient().getCompanyName());
                if(object.getModel().APClient().getCompanyName() == null || "".equals(object.getModel().APClient().getCompanyName())){
                    setSearchClient(object.getModel().Client().getCompanyName());
                }
                Master().setPayeeID(object.getModel().getPayeeID());
                Master().setSupplierClientID(object.getModel().getAPClientID());
                if(object.getModel().getAPClientID() == null || "".equals(object.getModel().getAPClientID())){
                    Master().setSupplierClientID(object.getModel().getClientID());
                }
                if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
                    || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
                    CheckPayments().getModel().setPayeeID(Master().getPayeeID());
                }
            }
        }

        return poJSON;
    }

    /**
    * Searches for a Particular record and assigns or displays the result.
    *
    * @param value     the search keyword or particular code
    * @param row       the detail row index where the result will be applied
    * @param byCode    true if searching by code, false if by description
    * @param isSearch  true to set search display value, false to assign to detail record
    * @return JSONObject containing the result of the search operation
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database access error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchParticular(String value, int row, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Particular object = new CashflowControllers(poGRider, logwrapr).Particular();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchParticular(object.getModel().getDescription());
            } else {
                Detail(row).setParticularID(object.getModel().getParticularID());
                System.out.println("Particular : " +  Detail(row).Particular().getDescription());
            }
        }
        return poJSON;
    }
    
    /**
    * Searches and assigns a Tax Code after validating supplier and period data.
    *
    * @param value   tax code or keyword
    * @param row     target row index
    * @param byCode  true to search by code, false otherwise
    * @return JSONObject with result status and row index
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchTaxCode(String value, int row, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        //BR - vat registered and non vat client (supplier) ito ang pwede pagbasehan kung i-enable ang wtax
        //0: Not Vat Registered; 1: Vat Registered; 2: Not BIR Registered / Special Suppliers / Small Business
        if("2".equals((String) Master().Payee().APClientMaster().getValue("cVATRegis"))){ 
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not taxable.");
            return poJSON;
        }
        
        if(WTaxDeduction(row).getModel().getPeriodFrom() == null || WTaxDeduction(row).getModel().getPeriodTo() == null){
            poJSON.put("result", "error");
            poJSON.put("message", "Period date is not set.");
            return poJSON;
        }
        
        //validate period
        poJSON = checkPeriodDate(row);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        int lnRow = 1;
        for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
            if(WTaxDeduction(lnCtr).getModel().isReverse()){
                lnRow++;
            }
            if(lnCtr != row){
                if(WTaxDeduction(lnCtr).getModel().getTaxCode() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxCode())){
                    if(WTaxDeduction(lnCtr).getModel().getTaxRateId() == null || "".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Particular at row "+lnRow+" is not set.");
                        return poJSON;
                    }
                }
            }
        }
        
        TaxCode object = new ParamControllers(poGRider, logwrapr).TaxCode();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            WTaxDeduction(row).getModel().setTaxCode(object.getModel().getTaxCode());
            System.out.println("Tax Code : " + WTaxDeduction(row).getModel().getTaxCode());
        }
        poJSON.put("row", row);
        return poJSON;
    }

    /**
    * Searches and assigns a Withholding Tax Particular after validation.
    *
    * @param value   search keyword or code
    * @param row     target row index
    * @param byCode  true to search by code, false otherwise
    * @return JSONObject with result status and row index
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchParticular(String value, int row, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        if(WTaxDeduction(row).getModel().getTaxCode() == null || "".equals(WTaxDeduction(row).getModel().getTaxCode())){
            poJSON.put("result", "error");
            poJSON.put("message", "Tax Code is not set.");
            poJSON.put("row", row);
            return poJSON;
        }
        
        //validate period
        poJSON = checkPeriodDate(row);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        WithholdingTax object = new CashflowControllers(poGRider, logwrapr).WithholdingTax();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setIndustryId(Master().getIndustryID());
        poJSON = object.searchRecord(value, byCode,WTaxDeduction(row).getModel().getTaxCode());
        if ("success".equals((String) poJSON.get("result"))) {
            JSONObject loJSON = checkExistTaxRate(row, object.getModel().getTaxRateId(),object.getModel().getTaxType());
            if ("error".equals((String) loJSON.get("result"))) {
                if((boolean) loJSON.get("reverse")){
                    return loJSON;
                } else {
                    row = (int) loJSON.get("row");
                    WTaxDeduction(row).getModel().isReverse(true);
                    WTaxDeduction(row).getModel().setBaseAmount(0.00);
                    WTaxDeduction(row).getModel().setTaxAmount(0.00);
                }
            }
            
            WTaxDeduction(row).getModel().setTaxRateId(object.getModel().getTaxRateId());
            WTaxDeduction(row).getModel().setBIRForm(object.getModel().getTaxType());
            System.out.println("Tax Code : " + WTaxDeduction(row).getModel().getTaxRateId());
            System.out.println("Particular : " + WTaxDeduction(row).getModel().WithholdingTax().AccountChart().getDescription());
        }
        
        poJSON.put("row", row);
        return poJSON;
    }

    /**
    * Searches for a Bank Account and assigns or displays the result.
    *
    * @param value     search keyword or account number
    * @param Banks     optional bank filter
    * @param byCode    true to search by code, false otherwise
    * @param isSearch  true to set display value, false to assign to payment record
    * @return JSONObject with result status
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchBankAccount(String value, String Banks, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        BankAccountMaster object = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setCompanyId(Master().getCompanyID());
        
        if(Banks == null || "".equals(Banks)){
            poJSON = object.searchRecord(value, byCode);
        } else {
            poJSON = object.searchRecordbyBanks(value, Banks, byCode);
        }
        
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBankAccountNo(object.getModel().getAccountNo());
            } else {
                switch(Master().getDisbursementType()){
                case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                case DisbursementStatic.DisbursementType.WIRED:
                    OtherPayments().getModel().setBankID(object.getModel().getBankId());
                    OtherPayments().getModel().setBankAcountID(object.getModel().getBankAccountId());
                break;
                default:
                    CheckPayments().getModel().setBankID(object.getModel().getBankId());
                    CheckPayments().getModel().setBankAcountID(object.getModel().getBankAccountId());
                break;
                }
                Master().setBankPrint(String.valueOf(object.getModel().isBankPrinting() ? 1 : 0));
            }
        }
        return poJSON;
    }

    /**
    * Searches for a Bank and assigns or displays the result.
    *
    * @param value     search keyword or bank code
    * @param byCode    true to search by code, false otherwise
    * @param isSearch  true to set display value, false to assign to payment record
    * @return JSONObject with result status
    * @throws ExceptionInInitializerError if initialization fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject SearchBanks(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Banks object = new ParamControllers(poGRider, logwrapr).Banks();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBankName(object.getModel().getBankName());
            } else {
                CheckPayments().getModel().setBankID(object.getModel().getBankID());
                switch(Master().getDisbursementType()){
                    case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                    case DisbursementStatic.DisbursementType.WIRED:
                        OtherPayments().getModel().setBankID(object.getModel().getBankID());
                    break;
                    default:
                        CheckPayments().getModel().setBankID(object.getModel().getBankID());
                    break;
                }
            }
        }

        return poJSON;
    }
    /*END of search references*/
    
    /*Validate detail exisitence*/
    /**
    * Checks if an account code already exists in the journal details.
    *
    * @param fnRow      current row index
    * @param fsAcctCode account code to validate
    * @return JSONObject indicating if duplicate exists or not
    */
    public JSONObject checkExistAcctCode(int fnRow, String fsAcctCode){
        poJSON = new JSONObject();

        for(int lnCtr = 0;lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            if(fsAcctCode.equals(poJournal.Detail(lnCtr).getAccountCode()) && fnRow != lnCtr){
                poJSON.put("row", lnCtr);
                poJSON.put("result", "error");
                poJSON.put("message", "Account code " + fsAcctCode + " already exists at row " + (lnCtr+1) + ".");
                poJournal.Detail(fnRow).setAccountCode("");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Checks if an account code already exists in the journal proposal details.
    *
    * @param fnJEProposalRow      current row index of JE Proposal
     * @param
     * @param
    * @return JSONObject indicating if duplicate exists or not
    */
    public JSONObject checkJEPExistBranchDept(int fnJEProposalRow, String fsOrigBranchCode, String fsOrigDeptId) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        String lbBranch = "";
        String lbDepartment = "";
        int lnRow = 0;

        for(int lnCtr = 0;lnCtr < getJournalProposalList().size() ; lnCtr++){
            lnRow++;
            if((fnJEProposalRow == lnCtr || JournalProposalStatus.VOID.equals(JournalProposal(lnCtr).Master()))
                || JournalProposalStatus.CANCELLED.equals(JournalProposal(lnCtr).Master())    
                ){
            } else {
                lbBranch = JournalProposal(lnCtr).Master().getBranchCode();
                lbDepartment = JournalProposal(lnCtr).Master().getDepartmentId();
                
                if(lbBranch != null && !"".equals(lbBranch)){
                    if(lbBranch.equals(JournalProposal(fnJEProposalRow).Master().getBranchCode())){
                        if(lbDepartment != null && !"".equals(lbDepartment)){
                            if(lbDepartment.equals(JournalProposal(fnJEProposalRow).Master().getDepartmentId())){
                                poJSON.put("row", lnCtr);
                                poJSON.put("result", "error");
                                poJSON.put("message", "Branch " + JournalProposal(fnJEProposalRow).Master().Branch().getBranchName() + " and Deparment " + JournalProposal(fnJEProposalRow).Master().Department().getDescription() + " already exist at row " + lnRow);
                                paJournalProposal.get(fnJEProposalRow).Master().setBranchCode(fsOrigBranchCode);
                                paJournalProposal.get(fnJEProposalRow).Master().setDepartmentId(fsOrigDeptId);
                                return poJSON;
                            }
                        }
                    }
                }
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Check Existing tax rate per selected period date
     * @param fnRow
     * @param fsTaxRated
     * @return 
     */
    private JSONObject checkExistTaxRate(int fnRow, String fsTaxRated, String fsTaxType){
        JSONObject loJSON = new JSONObject();
        try {
            int lnRow = 0;
            for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1; lnCtr++){
                if(WTaxDeduction(lnCtr).getModel().isReverse()){
                    lnRow++;
                }
                if(WTaxDeduction(lnCtr).getModel().getTaxRateId() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                    if(WTaxDeduction(lnCtr).getModel().isReverse() && !fsTaxType.equals(WTaxDeduction(lnCtr).getModel().WithholdingTax().getTaxType()) ){
                        loJSON.put("result", "error");
                        loJSON.put("reverse", true);
                        loJSON.put("row", lnCtr);
                        loJSON.put("message", "Tax type must be equal to other withholding tax deductions.");
                        return loJSON;
                    }
                    
                    //Check the tax rate
                    if(fsTaxRated.equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())
                        && fnRow != lnCtr){
                        //Check Period Date do not allow when taxratedid was already covered of the specific period date
                        if(strToDate(xsDateShort(WTaxDeduction(lnCtr).getModel().getPeriodFrom())).getYear() 
                            == strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())).getYear()){
                            //Check Period date per quarter
                            if( getQuarter(strToDate(xsDateShort(WTaxDeduction(lnCtr).getModel().getPeriodFrom()))) 
                                == getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom()))) 
                                    ){
                                if(WTaxDeduction(lnCtr).getModel().isReverse()){
                                    loJSON.put("result", "error");
                                    loJSON.put("message", "Particular " + WTaxDeduction(lnCtr).getModel().WithholdingTax().AccountChart().getDescription() + " already exists at row " + (lnRow) + ".");
                                    loJSON.put("row", lnCtr);
                                    loJSON.put("reverse", true);
                                    return loJSON;
                                } else {
                                    loJSON.put("result", "error");
                                    loJSON.put("reverse", false);
                                    loJSON.put("row", lnCtr);
                                    return loJSON;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            loJSON.put("result", "error");
            loJSON.put("message", MiscUtil.getException(ex));
            return loJSON;
        }
    
        loJSON.put("result", "success");
        return loJSON;
    }
    
    /**
     * Validate period date
     * @param fnRow pass withholding tax deduction selected row
     * @return 
     */
    public JSONObject checkPeriodDate(int fnRow){
        //Validate period from must be per quarter per year.
        if(WTaxDeduction(fnRow).getModel().getPeriodFrom() != null){
            if(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodTo())).getYear() 
                != strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())).getYear() ){
                poJSON.put("row", fnRow);
                poJSON.put("result", "error");
                poJSON.put("message", "Period Date must be with the same year at row "+(fnRow + 1)+".");
                return poJSON;
            }
            
            if( (getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodTo()))) 
                != getQuarter(strToDate(xsDateShort(WTaxDeduction(fnRow).getModel().getPeriodFrom())))) ){
                poJSON.put("row", fnRow);
                poJSON.put("result", "error");
                poJSON.put("message", "Period date must be in the same quarter at row "+(fnRow + 1)+".");
                return poJSON;
            }
        }
        
        poJSON.put("row", fnRow);
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Gets the quarter (1–4) of the given date.
    *
    * @param fdDate the date to evaluate
    * @return the quarter number (1 to 4)
    */
    private int getQuarter(LocalDate fdDate){
        int month = fdDate.getMonthValue();
        int quarter = ((month - 1) / 3) + 1;
        return quarter;
    }
    
    /**
    * Generates the next voucher number based on the latest record per company.
    *
    * @return the next formatted voucher number
    * @throws SQLException if a database error occurs
    */
    public String getVoucherNo() throws SQLException {
        String lsSQL = "SELECT sVouchrNo FROM Disbursement_Master";
        //Branch code is not stated in BR
        //Do not include branch code according to ma'am grace 03/28/2026
        //According to ma'am she 03/31/2026 voucher number
//        lsSQL = MiscUtil.addCondition(lsSQL,
//                "sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode()));
        lsSQL = MiscUtil.addCondition(lsSQL,
                "sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())); //get voucher account to company ma'am she 03-31-2026
        lsSQL = lsSQL + " ORDER BY sVouchrNo DESC LIMIT 1";

        String branchVoucherNo = DisbursementStatic.DEFAULT_VOUCHER_NO;  // default value

        ResultSet loRS = null;
        try {
            System.out.println("EXECUTING SQL :  " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS != null && loRS.next()) {
                String sSeries = loRS.getString("sVouchrNo");
                if (sSeries != null && !sSeries.trim().isEmpty()) {
                    long voucherNumber = Long.parseLong(sSeries);
                    voucherNumber += 1;
                    branchVoucherNo = String.format("%08d", voucherNumber); // format to 6 digits
                }
            }
        } finally {
            MiscUtil.close(loRS);  // Always close the ResultSet
        }
        return branchVoucherNo;
    }
    
    /**
     * Computation of vat and transaction total
     * @param isValidate
     * @return JSON
     */
    public JSONObject computeFields(boolean isValidate) {
        poJSON = new JSONObject();
        poJSON.put("column", "");
//        pdblAdvancesAmount = 0.0000;
        Double ldblTransactionTotal = 0.0000;
        Double ldblVATSalesTotal = 0.0000;
        Double ldblVATAmountTotal = 0.0000;
        Double ldblVATExemptTotal = 0.0000;
        Double ldblVATSales = 0.0000;
        Double ldblVATAmount = 0.0000;
        Double ldblVATExempt = 0.0000;
        Double ldblZeroVATSales = 0.0000;
        Double ldblAppliedAmt = 0.0000;
        Double ldblTotalAdvancesAmount = 0.0000;
        computeTaxAmount();
        computeDetailFields(isValidate);
        
        for (int lnCntr = 0; lnCntr <= getDetailCount() - 1; lnCntr++) {
                poJSON = Detail(lnCntr).setDetailAdvances(Master().getTransactionNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                
                //Compute vat amounts when amount paid is not equal to 0.0000
                if(Detail(lnCntr).getAmountApplied() != 0.0000){
                    ldblTransactionTotal += Detail(lnCntr).getAmountApplied();
                    ldblTotalAdvancesAmount += Detail(lnCntr).getDetailAdvances();
                    ldblVATSalesTotal += Detail(lnCntr).getDetailVatSales();
                    ldblVATAmountTotal += Detail(lnCntr).getDetailVatAmount();
                    ldblVATExemptTotal += Detail(lnCntr).getDetailVatExempt();
                    
                    if (Detail(lnCntr).getAmountApplied() > Detail(lnCntr).getAmount()) {
                        poJSON.put("result", "error");
                        poJSON.put("column", "nAmtAppld");
                        poJSON.put("message", "Invalid Applied Amount.");
                        if(isValidate){
                            return poJSON;
                        }
                    }
                }
                
//                poJSON = getAdvancesAmount(lnCntr);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
               
//            } else {
//                ldblAppliedAmt = Detail(lnCntr).getAmountApplied();
//                ldblVATSales = Detail(lnCntr).getDetailVatSales();
//                ldblVATAmount = Detail(lnCntr).getDetailVatAmount();
//                ldblVATExempt = Detail(lnCntr).getDetailVatExempt();
//                if(ldblAppliedAmt < 0){
//                    ldblAppliedAmt = ldblAppliedAmt * -1;
//                }
//                if(ldblVATSales < 0){
//                    ldblVATSales = ldblVATSales * -1;
//                }
//                if(ldblVATAmount < 0){
//                    ldblVATAmount = ldblVATAmount * -1;
//                }
//                if(ldblVATExempt < 0){
//                    ldblVATExempt = ldblVATExempt * -1;
//                }
//                if(Detail(lnCntr).getAmountApplied() < 0){
//                    if(ldblAppliedAmt > ldblTransactionTotal){
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Invalid Total Amount.");
//                        return poJSON;
//                    } else {
//                        ldblTransactionTotal = ldblTransactionTotal - ldblAppliedAmt;
//                        ldblVATSalesTotal = ldblVATSalesTotal - ldblVATSales;
//                        ldblVATAmountTotal = ldblVATAmountTotal - ldblVATAmount;
//                        ldblVATExemptTotal = ldblVATExemptTotal - ldblVATExempt;
//                    }
//                }
//                if(ldblVATSalesTotal < 0 ){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "Invalid Vat Sales Total.");
//                    return poJSON;
//                }
//                if(ldblVATAmountTotal < 0 ){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "Invalid Vat Amount Total.");
//                    return poJSON;
//                }
//                if(ldblVATExemptTotal < 0 ){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "Invalid Vat Exempt Total.");
//                    return poJSON;
//                }
//            }
        }
        
        if(ldblTransactionTotal < 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("column", "nTranTotl");
            poJSON.put("message", "Invalid Transaction Total.");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATSalesTotal < 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("column", "nVATSales");
            poJSON.put("message", "Invalid Vat Sales Total.");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATAmountTotal < 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("column", "nVATAmtxx");
            poJSON.put("message", "Invalid Vat Amount Total.");
            if(isValidate){
                return poJSON;
            }
        }
        if(ldblVATExemptTotal < 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("column", "nVatExmpt");
            poJSON.put("message", "Invalid Vat Exempt Total.");
            if(isValidate){
                return poJSON;
            }
        }
        
        double lnNetAmountDue = ldblTransactionTotal - ( Master().getDiscountTotal() + Master().getWithTaxTotal());

        if (lnNetAmountDue < 0.0000) {
            poJSON.put("result", "error");
            poJSON.put("column", "nNetTotal");
            poJSON.put("message", "Invalid Net Total Amount.");
            if(isValidate){
                return poJSON;
            }
        }

        Master().setTransactionTotal(ldblTransactionTotal);
        Master().setVATSale(ldblVATSalesTotal);
        Master().setVATAmount(ldblVATAmountTotal);
        Master().setVATExmpt(ldblVATExemptTotal);
        Master().setZeroVATSales(ldblZeroVATSales);
        Master().setNetTotal(lnNetAmountDue);
        Master().setAdvancesTotal(ldblTotalAdvancesAmount);

        switch(Master().getDisbursementType()){
            case DisbursementStatic.DisbursementType.CHECK:
            case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                if (Logical.YES.equals(Master().getBankPrint())) {
                    poCheckPayments.getModel().setAmount(Master().getNetTotal());
                } else {
                    if(poCheckPayments.getModel().getCheckNo() != null && !"".equals(poCheckPayments.getModel().getCheckNo())){
                        poCheckPayments.getModel().setAmount(Master().getNetTotal());
                    } else {
                        poCheckPayments.getModel().setAmount(0.0000);
                    }
                }
                break;
            case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
            case DisbursementStatic.DisbursementType.WIRED:
                poOtherPayments.getModel().setTotalAmount(Master().getNetTotal());
                break;
        }
        
        poJSON.put("column", "");
        poJSON.put("result", "success");
        poJSON.put("message", "computed successfully");
        return poJSON;
    }
    
    /**
    * Computes withholding tax amount and validates tax base rules.
    *
    * @return JSONObject containing computation result and message
    */
    public JSONObject computeTaxAmount(){
        poJSON = new JSONObject();
        
        //set/compute value to tax amount
        Double ldblTaxAmount = 0.0000;
        Double ldblDetTaxAmt = 0.0000;
        Double ldblTotalBaseAmount = 0.0000;
        for(int lnCtr = 0;lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
            if(WTaxDeduction(lnCtr).getModel().isReverse()){
                if(WTaxDeduction(lnCtr).getModel().getBaseAmount() > 0.0000 && 
                    WTaxDeduction(lnCtr).getModel().getTaxRateId() != null && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
                    try {
                        ldblDetTaxAmt = WTaxDeduction(lnCtr).getModel().getBaseAmount() * (WTaxDeduction(lnCtr).getModel().WithholdingTax().getTaxRate() / 100);
                    } catch (SQLException | GuanzonException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                        poJSON.put("result", "error");
                        poJSON.put("message", MiscUtil.getException(ex));
                        return poJSON;
                    }
                    WTaxDeduction(lnCtr).getModel().setTaxAmount(ldblDetTaxAmt);
                }
                ldblTaxAmount += WTaxDeduction(lnCtr).getModel().getTaxAmount();
                ldblTotalBaseAmount += WTaxDeduction(lnCtr).getModel().getBaseAmount(); 
            }
        }
        

        
        double ldblVatSales = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVATSale(), false).replace(",", ""));
        double ldblTransTotal = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getTransactionTotal(), false).replace(",", ""));
        if(ldblVatSales > 0.0000){
            if(ldblTotalBaseAmount > ldblVatSales){
                poJSON.put("result", "error");
                poJSON.put("message", "Tax Base amount cannot be greater than the net vatable sales.");
                return poJSON;
            }

            if(ldblTaxAmount > ldblVatSales){
                poJSON.put("result", "error");
                poJSON.put("message", "Tax amount cannot be greater than the net vatable sales.");
                return poJSON;
            }
        } else {
            
            try { 
                //BR - vat registered and non vat client (supplier) ito ang pwede pagbasehan kung i-enable ang wtax
                //0: Not Vat Registered; 1: Vat Registered; 2: Not BIR Registered / Special Suppliers / Small Business
                if("2".equals((String) Master().Payee().APClientMaster().getValue("cVATRegis"))){ 
                    if(ldblTotalBaseAmount > 0.0000){
                       poJSON.put("result", "error");
                       poJSON.put("message", "Supplier is not taxable.");
                       return poJSON;
                   }
                }
                
                if(ldblTotalBaseAmount > ldblTransTotal){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Tax Base amount cannot be greater than the transaction total.");
                    return poJSON;
                }
                
                if(ldblTaxAmount > ldblTransTotal){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Tax amount cannot be greater than the transaction total.");
                    return poJSON;
                }
            } catch (GuanzonException | SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON.put("result", "error");
                poJSON.put("message", MiscUtil.getException(ex));
                return poJSON;
            } 
        }
        
        Master().setWithTaxTotal(ldblTaxAmount);
//        System.out.println("Withholding tax total : " + Master().getWithTaxTotal());
        
        poJSON.put("result", "success");
        poJSON.put("message", "Tax computed successfully");
        return poJSON;
    }
   
    /**
    * Computes detail fields and validates amounts per row.
    *
    * @param isValidate true to stop and return on validation error
    * @return JSONObject containing result status and message
    */
    public JSONObject computeDetailFields(boolean isValidate){
                
        try {
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                if(Detail(lnCtr).getAmountApplied() != 0.0000){
                    //Compute VAT
                    switch(Detail(lnCtr).getSourceCode()){
                        case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                            switch(Detail(lnCtr).SOADetail().getSourceCode()){
                                case SOATaggingStatic.PaymentRequest:
                                case SOATaggingStatic.APPaymentAdjustment:
                                    poJSON = computeDetail(lnCtr);
                                    if ("error".equals((String) poJSON.get("result"))) {
                                        if(isValidate){
                                            return poJSON;
                                        }
                                    }
                            }
                        break;
                        case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                        case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                            poJSON = computeDetail(lnCtr);
                            if ("error".equals((String) poJSON.get("result"))) {
                                if(isValidate){
                                    return poJSON;
                                }
                            }
                        break;
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "success");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Computes VAT, exempt, and applied amounts for a detail row.
    *
    * @param fnRow detail row index
    * @return JSONObject containing computation result and message
    */
    private JSONObject computeDetail(int fnRow){
        poJSON = new JSONObject();
        poJSON = Detail(fnRow).setDetailAdvances(Master().getTransactionNo());
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        Double ldblAdvances = Detail(fnRow).getDetailAdvances() - Detail(fnRow).getDetailSourceDiscount();
        Double ldblAmountApplied = Detail(fnRow).getAmountApplied();
        Double ldblAppliedAmtWithAdv = ldblAmountApplied + ldblAdvances;
        Double ldblVATExempt = Detail(fnRow).getDetailVatExempt() ;
        Double ldblVATSales = 0.0000;
        Double ldblVATAmount = 0.0000;
        
        if(ldblAmountApplied < 0.0000){
            if(ldblVATExempt > 0.0000){
                Detail(fnRow).setDetailVatAmount(0.0000);
                Detail(fnRow).setDetailVatSales(0.0000);
                //Check original value of vat exempt
                if(Detail(fnRow).getDetailVatExempt() < 0.0000){
                    Detail(fnRow).setDetailVatExempt(-ldblAppliedAmtWithAdv);
                } else {
                    if(ldblAppliedAmtWithAdv != 0.000){
                        Detail(fnRow).setDetailVatExempt(ldblAppliedAmtWithAdv);
                    }
                }
                Detail(fnRow).isWithVat(false);

                poJSON.put("result", "error");
                poJSON.put("message", "Vat Exempt amount cannot be greater than (applied amount plus advances amount).");
                return poJSON;
            }
            
            ldblAppliedAmtWithAdv = ldblAppliedAmtWithAdv * -1;
            ldblAmountApplied = ldblAmountApplied * -1;
        } else {
            if(ldblVATExempt < 0.0000){
                poJSON.put("result", "error");
                poJSON.put("message", "Vat Exempt amount cannot be negative.");
                return poJSON;
            }
        }
        if(ldblVATExempt < 0){
            ldblVATExempt = ldblVATExempt * -1;
        } 
        
        if(ldblVATExempt > ldblAppliedAmtWithAdv){
            Detail(fnRow).setDetailVatAmount(0.0000);
            Detail(fnRow).setDetailVatSales(0.0000);
            //Check original value of vat exempt
            if(Detail(fnRow).getDetailVatExempt() < 0.0000){
                Detail(fnRow).setDetailVatExempt(-ldblAppliedAmtWithAdv);
            } else {
                if(ldblAppliedAmtWithAdv != 0.000){
                    Detail(fnRow).setDetailVatExempt(ldblAppliedAmtWithAdv);
                }
            }
            Detail(fnRow).isWithVat(false);
            
            poJSON.put("result", "error");
            poJSON.put("message", "Vat Exempt amount cannot be greater than (applied amount plus advances amount).");
            return poJSON;
        } else if(ldblVATExempt < ldblAppliedAmtWithAdv){
            Detail(fnRow).isWithVat(true);
        } else if (Objects.equals(ldblVATExempt, ldblAppliedAmtWithAdv)){
            Detail(fnRow).isWithVat(false);
        }

        if(Detail(fnRow).isWithVat()){
            ldblAppliedAmtWithAdv = ldblAppliedAmtWithAdv - ldblVATExempt;
            ldblVATAmount = ldblAppliedAmtWithAdv - (ldblAppliedAmtWithAdv / 1.12);
            ldblVATSales = ldblAppliedAmtWithAdv - ldblVATAmount;
            
            if(Detail(fnRow).getAmountApplied() < 0){
                ldblVATAmount = -ldblVATAmount;
                ldblVATSales = -ldblVATSales;
            } 

            Detail(fnRow).setDetailVatAmount(ldblVATAmount);
            Detail(fnRow).setDetailVatSales(ldblVATSales);
        } else {
            Detail(fnRow).setDetailVatAmount(0.0000);
            Detail(fnRow).setDetailVatSales(0.0000);
            //Check original value of vat exempt
            if(Detail(fnRow).getDetailVatExempt() < 0.0000){
                Detail(fnRow).setDetailVatExempt(-ldblAppliedAmtWithAdv);
            } else {
                if(ldblAppliedAmtWithAdv != 0.000){
                    Detail(fnRow).setDetailVatExempt(ldblAppliedAmtWithAdv);
                }
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    //Moved to Model DV Detail need to add advances computation for detail
//    private double pdblAdvancesAmount = 0.0000;
//    public double getAdvancesAmount(){
//        return pdblAdvancesAmount;
//    }
//    
//    private JSONObject getAdvancesAmount(int lnRow){
//        poJSON = new JSONObject();
//        try {
//            List<String> llistPurchaseOrder = new ArrayList<>();
//            String lsPOTransNo = "";
//            switch(Detail(lnRow).getSourceCode()){
//                case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
//                    if(SOATaggingStatic.PaymentRequest.equals(Detail(lnRow).SOADetail().getSourceCode())){
//                        if(DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(Detail(lnRow).SOADetail().PaymentRequestMaster().getSourceCode())){
//                            lsPOTransNo = Detail(lnRow).SOADetail().PaymentRequestMaster().getSourceNo();
//                        }
//                    } else if(SOATaggingStatic.POReceiving.equals(Detail(lnRow).SOADetail().getSourceCode())){
//                        poJSON = getPOAdvancesInPOReceiving(Detail(lnRow).getDetailSource());
//                        if ("error".equals((String) poJSON.get("result"))) {
//                            return poJSON;
//                        }
//                        return poJSON;
//                    }
//                break;
//    //            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
//                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
//                    if(DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(Detail(lnRow).PRF().getSourceCode())){
//                        lsPOTransNo = Detail(lnRow).PRF().getSourceNo();
//                    }
//                    break;
//                case DisbursementStatic.SourceCode.PO_RECEIVING:
//                    poJSON = getPOAdvancesInPOReceiving(Detail(lnRow).getSourceNo());
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                    return poJSON;
//            }
//
//            if(!llistPurchaseOrder.contains(lsPOTransNo)){
//                llistPurchaseOrder.add(lsPOTransNo);
//            }
//
//            poJSON = getAdvancePayment(llistPurchaseOrder);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }
//
//        } catch (SQLException | GuanzonException ex) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//            poJSON.put("result", "error");
//            poJSON.put("message", MiscUtil.getException(ex));
//            return poJSON;
//        } 
//        return poJSON;
//    }
//    
//    private JSONObject getPOAdvancesInPOReceiving(String fsTransNo) throws SQLException, GuanzonException{
//        poJSON = new JSONObject();
//        Model_POR_Master loObject = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
//        poJSON = loObject.openRecord(fsTransNo);
//        if ("error".equals((String) poJSON.get("result"))) {
//            poJSON.put("message", (String) poJSON.get("message") + "\nWhile reloading PO Receiving Master." );
//            return poJSON;
//        }
//        
//        if(!PurchaseOrderReceivingStatus.Purpose.REGULAR.equals(loObject.getPurpose())){
//            return poJSON;
//        }
//        List<String> llistPurchaseOrder = new ArrayList<>();
//        for(int lnCtr = 0;lnCtr < loObject.getEntryNo();lnCtr++){
//            Model_POR_Detail loDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
//            poJSON = loDetail.openRecord(fsTransNo,(lnCtr+1) );
//            if ("error".equals((String) poJSON.get("result"))) {
//            poJSON.put("message",(String) poJSON.get("message") + "\nWhile reloading PO Receiving Detail.");
//                return poJSON;
//            }
//            
//            if(loDetail.getOrderNo() != null && !"".equals(loDetail.getOrderNo())){
//                if(!llistPurchaseOrder.contains(loDetail.getOrderNo())){
//                    llistPurchaseOrder.add(loDetail.getOrderNo());
//                } 
//            }
//        }
//        
//        
//        poJSON = getAdvancePayment(llistPurchaseOrder);
//        if ("error".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
//        
//        poJSON.put("result", "success");
//        poJSON.put("message", "success");
//        return poJSON;
//    }
//    
//    private JSONObject getAdvancePayment(List<String> faPOTransNo) throws SQLException, GuanzonException {
//        Model_PO_Master loObject = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
//        for(int lnCtr = 0; lnCtr < faPOTransNo.size(); lnCtr++){
//            if(faPOTransNo.get(lnCtr) != null && !"".equals(faPOTransNo.get(lnCtr))){
//                poJSON = loObject.openRecord(faPOTransNo.get(lnCtr));
//                if ("error".equals((String) poJSON.get("result"))) {
//                    poJSON.put("message",(String) poJSON.get("message") + "\nWhile reloading PO Purchase Order.");
//                    return poJSON;
//                }
//                pdblAdvancesAmount += loObject.getAmountPaid().doubleValue();
//            }
//        }
//        
//        poJSON.put("result", "success");
//        poJSON.put("message", "success");
//        return poJSON;
//    }
    
    /**
     * Load Transaction list based on supplier, reference no, bankId, bankaccountId or check no
     * @param fsIndustry pass the Industry Name
     * @param fsValue1 if isUpdateTransactionStatus is false pass the supplier else bank
     * @param fsValue2 if isUpdateTransactionStatus is false pass the reference no else bankAccount
     * @param fsValue3 if isUpdateTransactionStatus is false pass empty string else check no
     * @param isBank set TRUE if retrieval called at certification, check authorization and check update status else set FALSE for verification retrieval
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException 
     */
//    public JSONObject loadTransactionList(String fsIndustry, String fsValue1, String fsValue2, String fsValue3, boolean isBank, boolean isAuthorization) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        paMaster = new ArrayList<>();
//        if (fsIndustry == null) { fsIndustry = ""; }
//        if (fsValue1 == null) { fsValue1 = ""; }
//        if (fsValue2 == null) { fsValue2 = ""; }
//        if (fsValue3 == null) { fsValue3 = ""; }
//        initSQL();
//        //set default retrieval for supplier / reference no
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
//                    "  a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                    + " AND k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry)
//                    + " AND a.sVouchrNo LIKE " + SQLUtil.toSQL("%" + fsValue2)
//                    + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsValue1)
//                    + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue1) 
//                    + " ) ");
//        //if method was called in certification/checka auhorization/check update change the condition into bank and bank account and check no
//        if(isBank){
//            lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
//                    " i.sBankName LIKE " + SQLUtil.toSQL("%" + fsValue1)
//                    + " AND j.sActNumbr LIKE " + SQLUtil.toSQL("%" + fsValue2))
//                    + " AND k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry)
//                    + ( fsValue3.isEmpty() ? "" : " AND g.sCheckNox LIKE " + SQLUtil.toSQL("%" + fsValue3));
//        }
//        
//        if(isAuthorization){
//            lsSQL = lsSQL + " AND a.cDisbrsTp = "  + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK);
//        }
//        
//        String lsCondition = "";
//        if (psTranStat != null) {
//            if (psTranStat.length() > 1) {
//                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
//                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
//                }
//                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
//            } else {
//                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
//            }
//            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
//        }
//
//        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        if (MiscUtil.RecordCount(loRS) <= 0) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "No record found.");
//            return poJSON;
//        }
//
//        while (loRS.next()) {
//            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
//            poJSON = loObject.openRecord(loRS.getString("sTransNox"));
//            if ("success".equals((String) poJSON.get("result"))) {
//                paMaster.add((Model) loObject);
//            } else {
//                return poJSON;
//            }
//        }
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
    /**
    * Loads a list of disbursement transactions based on filters.
    *
    * @param fsIndustry Industry description filter (matched using LIKE).
    * @param fsValue1   Primary search value:
    *                   - Payee/Company name (CONFIRMED, VERIFIED, APPROVED)
    *                   - Bank name (CERTIFIED, AUTHORIZED)
    * @param fsValue2   Secondary search value:
    *                   - Voucher number (CONFIRMED, VERIFIED, APPROVED)
    *                   - Account number (CERTIFIED, AUTHORIZED)
    * @param fsForm     Transaction stage (e.g., CONFIRMED, VERIFIED, APPROVED, CERTIFIED, AUTHORIZED).
    *
    * @return JSONObject containing:
    *         - "result": "success" or "error"
    *         - "message": error message if no records found
    *
    * @throws SQLException        if database query fails
    * @throws GuanzonException    if business logic processing fails
    *
    * Process:
    * 1. Initialize filters and base SQL query.
    * 2. Apply company + industry filter.
    * 3. Apply transaction status filter (single or multiple).
    * 4. Apply form-specific conditions:
    *    - CONFIRMED/VERIFIED/APPROVED → filter by voucher + payee/company
    *    - CERTIFIED → filter by bank + account
    *    - AUTHORIZED → same as CERTIFIED + restrict disbursement type
    * 5. Execute query and validate results.
    * 6. For each record:
    *    - Load full transaction via sTransNox
    *    - Add to master list if successful
    * 7. Return final JSON result.
    */
    public JSONObject loadTransactionList(String fsIndustry, String fsValue1, String fsValue2, String fsForm) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        if (fsIndustry == null) { fsIndustry = ""; }
        if (fsValue1 == null) { fsValue1 = ""; }
        if (fsValue2 == null) { fsValue2 = ""; }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                    "  a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry));
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
        switch(fsForm){
            case DisbursementStatic.CONFIRMED:
            default:
                 lsSQL = MiscUtil.addCondition(lsSQL, 
                     " a.sVouchrNo LIKE " + SQLUtil.toSQL("%" + fsValue2)
                    + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsValue1)
                    + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue1) 
                    + " ) "
                    );
            break;
            case DisbursementStatic.VERIFIED:
                 lsSQL = MiscUtil.addCondition(lsSQL, 
                     " a.sVouchrNo LIKE " + SQLUtil.toSQL("%" + fsValue2)
                    + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsValue1)
                    + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue1) 
                    + " ) "
                    );
            break;
            case DisbursementStatic.APPROVED:
                 lsSQL = MiscUtil.addCondition(lsSQL, 
                     " a.sVouchrNo LIKE " + SQLUtil.toSQL("%" + fsValue2)
                    + " AND ( d.sPayeeNme LIKE " + SQLUtil.toSQL("%" + fsValue1)
                    + " OR e.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue1) 
                    + " ) "
                    );
            break;
            case DisbursementStatic.CERTIFIED:
                lsSQL = MiscUtil.addCondition(lsSQL, 
                        " i.sBankName LIKE " + SQLUtil.toSQL("%" + fsValue1)
                        + " AND j.sActNumbr LIKE " + SQLUtil.toSQL("%" + fsValue2)
                        );
            break;
            case DisbursementStatic.AUTHORIZED:
                lsSQL = MiscUtil.addCondition(lsSQL, 
                        " i.sBankName LIKE " + SQLUtil.toSQL("%" + fsValue1)
                        + " AND j.sActNumbr LIKE " + SQLUtil.toSQL("%" + fsValue2)
                        + " AND ( a.cDisbrsTp = "  + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK)
                        + " OR  a.cDisbrsTp = "  + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT)
                        + " ) "
                        );
            break;
            
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact, supplier ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
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
     * Load Transaction list based on supplier, reference no, bankId, bankaccountId or check no
     * @param fsIndustry pass the industry name
     * @param fsBankName  pass the bank name
     * @param fsBankAccount pass the bankAccount number
     * @param fsFromDate pass transaction date from
     * @param fsToDate pass transaction date to
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject loadCheckPrintTransactionList(String fsIndustry, String fsBankName, String fsBankAccount, String fsFromDate, String fsToDate) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        pbPrint = true;
        if (fsBankName == null) { fsBankName = ""; }
        if (fsBankAccount == null) { fsBankAccount = ""; }
        if (fsIndustry == null) { fsIndustry = ""; }
        if (fsFromDate == null) { fsFromDate = ""; }
        if (fsToDate == null) { fsToDate = ""; }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                    "  a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND a.cBankPrnt = '0' AND i.sBankName LIKE " + SQLUtil.toSQL("%" + fsBankName)
                    + " AND ( a.cDisbrsTp = " + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK)
                    + " OR a.cDisbrsTp = " + SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT)
                    + " ) AND ( g.cTranStat = " + SQLUtil.toSQL(CheckStatus.FLOAT)
                    + " OR g.cTranStat = " + SQLUtil.toSQL(CheckStatus.OPEN)
                    + " ) AND g.cPrintxxx = " + SQLUtil.toSQL(CheckStatus.PrintStatus.OPEN)
                    + " AND j.sActNumbr LIKE " + SQLUtil.toSQL("%" + fsBankAccount)
                    + " AND k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry)
                    + " AND a.dTransact BETWEEN " + SQLUtil.toSQL(fsFromDate)
                    + " AND " + SQLUtil.toSQL(fsToDate)
                );
        
        
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
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
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
     * Load Transaction list based on supplier, reference no, bankId, bankaccountId or check no
     * @param fsIndustry  pass the Industry Name
     * @param fsSupplier pass the Supplier Name
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject loadBIRPrintTransactionList(String fsIndustry, String fsSupplier) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        pbPrint = true;
        if (fsIndustry == null) { fsIndustry = ""; }
        if (fsSupplier == null) { fsSupplier = ""; }
        initSQL();
        
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                    "  a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND  k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry)
                    + " AND ( IFNULL('',d.sPayeeNme) LIKE " + SQLUtil.toSQL("%" + fsSupplier)
                    + " OR IFNULL('',e.sCompnyNm ) LIKE " + SQLUtil.toSQL("%" + fsSupplier) 
                    + " ) "
                    + " AND a.sTransNox IN (SELECT wtd.sSourceNo FROM Withholding_Tax_Deductions wtd WHERE wtd.sSourceNo = a.sTransNox AND wtd.cReversex = "+SQLUtil.toSQL(DisbursementStatic.Reverse.INCLUDE)+" ) "
                    + " AND a.cPrintBIR = '0'"
                    + " AND ( g.cTranStat = "+SQLUtil.toSQL(CheckStatus.OPEN)
                    + " OR h.cTranStat = "+SQLUtil.toSQL(OtherPaymentStatus.POSTED)+" ) "
                    );
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
//                    "  a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
//                    + " AND  k.sDescript LIKE " + SQLUtil.toSQL("%" + fsIndustry)
//                    + " AND ( IFNULL('',d.sPayeeNme) LIKE " + SQLUtil.toSQL("%" + fsSupplier)
//                    + " OR IFNULL('',e.sCompnyNm ) LIKE " + SQLUtil.toSQL("%" + fsSupplier) 
//                    + " ) "
//                    + " AND ( a.sTransNox IN (SELECT cp.sSourceNo FROM Check_Payments cp WHERE cp.sSourceNo = a.sTransNox AND cp.cTranStat = "+SQLUtil.toSQL(CheckStatus.OPEN)+" ) "
//                    + " OR a.sTransNox IN (SELECT op.sSourceNo FROM Other_Payments op WHERE op.sSourceNo = a.sTransNox AND op.cTranStat = "+SQLUtil.toSQL(OtherPaymentStatus.POSTED)+"  ) ) "
//                    + " AND a.sTransNox IN (SELECT wtd.sSourceNo FROM Withholding_Tax_Deductions wtd WHERE wtd.sSourceNo = a.sTransNox AND wtd.cReversex = "+SQLUtil.toSQL(DisbursementStatic.Reverse.INCLUDE)+" ) "
//                    + " AND a.cPrintBIR = '0'"
//                    );
        
        
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

        lsSQL = lsSQL + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
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
    
    List<String> paAttachmentsSource;
    /**
    * Loads and downloads all attachments for transaction details.
    *
    * @return JSONObject containing load result and message
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();
        paAttachmentsSource = new ArrayList<>();
        String lsSourceNo = "";
        String lsSourceCode = "";
        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        for(int lnRow = 0; lnRow <= getDetailCount() - 1;lnRow++){
            if(Detail(lnRow).getAmountApplied() != 0.0000){
                lsSourceNo = Detail(lnRow).getSourceNo();
                lsSourceCode = Detail(lnRow).getSourceCode();
                if(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals(Detail(lnRow).getSourceCode())){
                    lsSourceNo = Detail(lnRow).getDetailSource();
                    lsSourceCode = Detail(lnRow).SOADetail().getSourceCode();
//                    continue; //if source is from SOA / Account Payable no need to display the attachment based on ma'am she - 03-04-2026 -Disabled Arsiela 05-18-2026 Suggestion to view attachements by testers
                }

                List loList = loAttachment.getAttachments(lsSourceCode, lsSourceNo);
                for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
                    paAttachmentsSource.add(getSourceCodeDescription(Detail(lnRow).getSourceCode()) + " - " + getReferenceNo(lnRow));
                    paAttachments.add(TransactionAttachment());
                    poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
                    if ("success".equals((String) poJSON.get("result"))) {
                        if(Master().getEditMode() == EditMode.UPDATE){
                           poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                        }
                        System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                        System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                        System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                        System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());

                        //Download Attachments
                        poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                                , "0032" //Constant
                                , "" //Empty
                                , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                                , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode()
                                , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                                , "");
                        if ("success".equals((String) poJSON.get("result"))) {

                            poJSON = (JSONObject) poJSON.get("payload");
                            if(WebFile.Base64ToFile((String) poJSON.get("data")
                                    , (String) poJSON.get("hash")
                                    , System.getProperty("sys.default.path.temp.attachments") + "/"
                                    , (String) poJSON.get("filename"))){
                                System.out.println("poJSON success: " +  poJSON.toJSONString());
                                System.out.println("File downloaded succesfully.");
                            } else {
                                System.out.println("poJSON error: " + poJSON.toJSONString());
                                poJSON.put("result", "error");
                                poJSON.put("message", "Unable to download file.");
                            }

                        } else {
                            poJSON = (JSONObject) poJSON.get("error");
                            poJSON.put("result", "error");
                            System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                            System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                            System.out.println("SKIP THE FILE TO LOAD");
                        }
                    }
                }
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    /**
    * Creates a new TransactionAttachment instance.
    *
    * @return new TransactionAttachment object
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }
    
    /**
    * Gets the list of transaction attachments.
    *
    * @return list of TransactionAttachment objects
    */
    private List<TransactionAttachment> TransactionAttachmentList() {
        return paAttachments;
    }
    
    /**
    * Gets a transaction attachment by row index.
    *
    * @param row index of the attachment
    * @return TransactionAttachment object
    */
    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }
    
    /**
    * Gets the source description of a transaction attachment.
    *
    * @param row index of the attachment
    * @return source description string
    */
    public String TransactionAttachmentSource(int row) {
        return (String) paAttachmentsSource.get(row);
    }

    /**
    * Gets the total number of transaction attachments.
    *
    * @return attachment count
    */
    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }
    
    /**
    * Gets the master disbursement model.
    *
    * @return Disbursement master model
    */
    @Override
    public Model_Disbursement_Master Master() { 
        return (Model_Disbursement_Master) poMaster; 
    }
    
    /**
    * Gets a detail record by row index.
    *
    * @param row detail row index
    * @return disbursement detail model
    */
    @Override
    public Model_Disbursement_Detail Detail(int row) {
        return (Model_Disbursement_Detail) paDetail.get(row); 
    }
    
    /**
    * Gets the list of withholding tax deductions.
    *
    * @return list of WTax deduction models
    */
    public List<WithholdingTaxDeductions> WTaxDeduction() {
        return paWTaxDeductions; 
    }
    
    /**
    * Gets a withholding tax deduction by row index.
    *
    * @param row deduction row index
    * @return withholding tax deduction model
    */
    public WithholdingTaxDeductions WTaxDeduction(int row) {
        return (WithholdingTaxDeductions) paWTaxDeductions.get(row); 
    }
    
    /**
    * Gets or initializes the CheckPayments controller.
    *
    * @return CheckPayments instance
    */
    public CheckPayments CheckPayments() {
        try {
            if (poCheckPayments == null) {
                poCheckPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
                poCheckPayments.initialize();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poCheckPayments;
    }
    
    /**
    * Gets or initializes the OtherPayments controller.
    *
    * @return OtherPayments instance
    */
    public OtherPayments OtherPayments() {
        try{
            if (poOtherPayments == null) {
                poOtherPayments = new CashflowControllers(poGRider, logwrapr).OtherPayments();
                poOtherPayments.initialize();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poOtherPayments;
    }
    
    /**
    * Gets or initializes the Journal controller.
    *
    * @return Journal instance
    */
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
    
    /**
    * Gets or initializes the Journal controller.
    *
    * @return Journal instance
    */
    public JournalProposal JournalProposal(int fnRow){
//        try{
            if(paJournalProposal == null){
                paJournalProposal = new ArrayList<JournalProposal>();
            }
//            System.out.println("Array : " + paJournalProposal);
//            if (paJournalProposal.get(fnRow) == null) {
//                paJournalProposal.add(fnRow, new CashflowControllers(poGRider, logwrapr).JournalProposal());
//                paJournalProposal.get(fnRow).InitTransaction();
//            }
//        } catch (SQLException | GuanzonException ex) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//        }
        return paJournalProposal.get(fnRow);
    }
    
    
    /**
    * Adds a new journal detail record after validating the last entry.
    *
    * @return JSONObject indicating success or error status
    * @throws CloneNotSupportedException if cloning fails
    */
    public JSONObject AddJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        if (getJournalProposalList().size() > 0) {
            if (JournalProposal(getJournalProposalList().size() - 1).Master().getBranchCode().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }
        paJournalProposal.add( new CashflowControllers(poGRider, logwrapr).JournalProposal());
        paJournalProposal.get(getJournalProposalList().size() - 1).InitTransaction();
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Gets the full list of Journal Proposal records.
    *
    * @return list of Journal Proposal models
    */
    public List<JournalProposal> getJournalProposalList() {
        return (List<JournalProposal>) (List<?>) paJournalProposal;
    }
    
    /**
    * Gets the number of withholding tax deductions.
    *
    * @return deduction count
    */
    public int getWTaxDeductionsCount() {
        return paWTaxDeductions.size();
    }
    
    /**
    * Adds a new Withholding Tax Deduction row after validating the last entry.
    *
    * @return JSONObject indicating success or error status
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business logic error occurs
    */
    public JSONObject AddWTaxDeduction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        if (getWTaxDeductionsCount() > 0) {
            if (WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxRateId().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }
        paWTaxDeductions.add(new CashflowControllers(poGRider, logwrapr).WithholdingTaxDeductions());
        //set default period date
        paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(Master().getTransactionDate());
        paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(Master().getTransactionDate());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Removes or marks a Withholding Tax Deduction row as reversed.
    *
    * @param fnRow row index to remove or reverse
    * @return JSONObject indicating operation result
    */
    public JSONObject removeWTDeduction(int fnRow) {
        if (WTaxDeduction(fnRow).getEditMode() == EditMode.ADDNEW) {
            WTaxDeduction().remove(fnRow);
        } else {
            WTaxDeduction(fnRow).getModel().isReverse(false);
        }
        //Compute Tax Amount
        computeTaxAmount();
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Adds a new detail record after validating the last entry.
    *
    * @return JSONObject indicating success or error status
    * @throws CloneNotSupportedException if cloning fails
    */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getSourceNo().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
        }

        return addDetail();
    }
    
    /**
    * Removes all detail records and resets related data (WTax, journal, attachments).
    *
    * @return JSONObject indicating operation result
    */
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }
        
        Iterator<WithholdingTaxDeductions> wtDeduction = WTaxDeduction().iterator();
        while (wtDeduction.hasNext()) {
            WithholdingTaxDeductions item = wtDeduction.next();
            wtDeduction.remove();
        }
        
        //Reset Journal when all details was removed
        resetJournal();
        paAttachments = new ArrayList<>();
        setSearchPayee("");
        setSearchClient("");
        Master().setIndustryID("");
        initFields();
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Gets the full list of Disbursement Master records.
    *
    * @return list of Disbursement Master models
    */
    public List<Model_Disbursement_Master> getMasterList() {
        return (List<Model_Disbursement_Master>) (List<?>) paMaster;
    }

    /**
    * Gets a specific Disbursement Master record by index.
    *
    * @param masterRow index of the master record
    * @return Disbursement Master model
    */
    public Model_Disbursement_Master getMaster(int masterRow) {
        return (Model_Disbursement_Master) paMaster.get(masterRow);
    }

    /*RESET CACHE ROW SET*/
    /**
     * Resets the master record to default state.
     */
    public void resetMaster() {
        poMaster = new CashflowModels(poGRider).DisbursementMaster();
//        Master().setIndustryID(psIndustryId);
        Master().setCompanyID(psCompanyId);
    }
    /**
     * Resets journal transaction.
     */
    public void resetJournal() {
        try {
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Resets journal transaction.
     */
    public void resetJournalProposal() {
        try {
            paJournalProposal = new ArrayList<JournalProposal>();
            paJournalProposal.set(paJournalProposal.size()-1, new CashflowControllers(poGRider, logwrapr).JournalProposal());
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Resets check payment controller.
     */
    public void resetCheckPayment(){
        try {
            poCheckPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Resets other payment controller.
     */
    public void resetOtherPayment(){
        try {
            poOtherPayments = new CashflowControllers(poGRider, logwrapr).OtherPayments();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(Disbursement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Resets the entire transaction including master, details, WTax, and UI fields.
     */
    public void resetTransaction(){
        resetMaster();
        resetJournal();
        resetCheckPayment();
        resetOtherPayment();
        Detail().clear();
        WTaxDeduction().clear();
        getJournalProposalList().clear();
        paAttachments = new ArrayList<>();
        
        setSearchIndustry("");
        setSearchBranch("");
        setSearchClient("");
        setSearchParticular("");
        setSearchPayee("");
        setSearchBankAccountNo("");
        setSearchBankName("");
        
        psApprover = "";
        pbPrint = false;
    }
    
    /**
    * Reloads detail records by removing invalid entries and ensuring at least one valid row exists.
    *
    * @throws CloneNotSupportedException if cloning fails
    */
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (Detail(lnCtr).getSourceNo() == null || "".equals(Detail(lnCtr).getSourceNo())) {
                Detail().remove(lnCtr);
            } else {
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    if(Detail(lnCtr).getAmountApplied() == 0.0000){
                        Detail().remove(lnCtr);
                    }
                }
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            if (Detail(getDetailCount() - 1).getSourceNo() != null && !"".equals(Detail(getDetailCount() - 1).getSourceNo())) {
                if((Detail(getDetailCount() - 1).getAmountApplied() == 0.0000 && Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE)
                    || ((Detail(getDetailCount() - 1).getAmountApplied() < 0.0000 || Detail(getDetailCount() - 1).getAmountApplied() > 0.0000) && (Detail(getDetailCount() - 1).getEditMode() == EditMode.ADDNEW || Detail(getDetailCount() - 1).getEditMode() == EditMode.UPDATE))){
                    AddDetail();
                }
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
        
        if ((Detail(getDetailCount() - 1).getSourceNo() == null || "".equals(Detail(getDetailCount() - 1).getSourceNo()))
            && Detail(getDetailCount() - 1).getAmountApplied() == 0.0000
            && getDetailCount() <= 1){
            Master().setIndustryID("");
        }
    }
    
//    public void ReloadJournal() throws CloneNotSupportedException, SQLException{
//        int lnCtr = Journal().getDetailCount() - 1;
//        while (lnCtr >= 0) {
//            if (Journal().Detail(lnCtr).getAccountCode() == null || "".equals(Journal().Detail(lnCtr).getAccountCode())) {
//                Journal().Detail().remove(lnCtr);
//            }
//            lnCtr--;
//        }
//        if ((Journal().getDetailCount() - 1) >= 0) {
//            if (Journal().Detail(Journal().getDetailCount() - 1).getAccountCode() != null
//                    && !"".equals(Journal().Detail(Journal().getDetailCount() - 1).getAccountCode())) {
//                Journal().AddDetail();
//                Journal().Detail(Journal().getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
//            }
//        }
//        if ((Journal().getDetailCount() - 1) < 0) {
//            Journal().AddDetail();
//            Journal().Detail(Journal().getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
//        }
//    }
//    
//    
    /**
    * Cleans journal details by removing invalid or empty entries,
    * ensures at least one valid row exists, and adds a new detail row
    * when the last entry is valid and contains amounts.
    *
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    */
//    public void ReloadJournal() throws CloneNotSupportedException, SQLException{
//        int lnCtr = Journal().getDetailCount() - 1;
//        while (lnCtr >= 0) {
//            if (Journal().Detail(lnCtr).getAccountCode() == null || "".equals(Journal().Detail(lnCtr).getAccountCode())) {
//                Journal().Detail().remove(lnCtr);
//            } else {
//                if(Journal().Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    if(Journal().Detail(lnCtr).getDebitAmount() <= 0.0000
//                        && Journal().Detail(lnCtr).getCreditAmount() <= 0.0000){
//                        Journal().Detail().remove(lnCtr);
//                    }
//                }
//            }
//            lnCtr--;
//        }
//        if ((Journal().getDetailCount() - 1) >= 0) {
//            if (Journal().Detail(getDetailCount() - 1).getAccountCode() != null && !"".equals(Journal().Detail(getDetailCount() - 1).getAccountCode())
//                && (Journal().Detail(getDetailCount() - 1).getDebitAmount() > 0.0000 || Journal().Detail(getDetailCount() - 1).getCreditAmount() > 0.0000)) {
//                Journal().AddDetail();
//                Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
//            }
//        }
//        if ((Journal().getDetailCount() - 1) < 0) {
//            Journal().AddDetail();
//            Journal().Detail(getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
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
    public void ReloadJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException, GuanzonException{
        int lnCtr = getJournalProposalList().size() - 1;
        Model_Journal_Detail_Proposal loJEPDetail;
        while (lnCtr >= 0) {
            if (JournalProposal(lnCtr) == null ) {
               getJournalProposalList().remove(lnCtr);
            } else {
//                if((JournalProposal(lnCtr).Master().getBranchCode() == null || "".equals(JournalProposal(lnCtr).Master().getBranchCode()))
//                   && (JournalProposal(lnCtr).Master().getDepartmentId() == null || "".equals(JournalProposal(lnCtr).Master().getDepartmentId())  )
//                   && JournalProposal(lnCtr).getTotalDebitAmount() == 0.0000 
//                    && JournalProposal(lnCtr).getTotalCreditAmount() == 0.0000         
//                        ){
//                    if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW){
//                        getJournalProposalList().remove(lnCtr);
//                    } 
//                }
//                if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    loJEPDetail = JournalProposal(lnCtr).Detail(JournalProposal(lnCtr).getDetailCount() - 1);
//                    
//                    if(loJEPDetail.getDebitAmount() <= 0.0000 && loJEPDetail.getCreditAmount() <= 0.0000){
//                        getJournalProposalList().remove(lnCtr);
//                    }
//                }
            }
            lnCtr--;
        }
        
        if(getJournalProposalList() == null){
            if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                createNewJournalProposal();
            }
        } else {
            if ((getJournalProposalList().size() - 1) >= 0) {
                lnCtr = getJournalProposalList().size() - 1;
                
                boolean hasEmptyBranchOrDepartment = getJournalProposalList().stream()
                .anyMatch(jp ->
                    StringUtils.isBlank(jp.Master().getBranchCode()) ||
                    StringUtils.isBlank(jp.Master().getDepartmentId())
                );
                
                if(JournalProposal(lnCtr).Master().getBranchCode() != null && !"".equals(JournalProposal(lnCtr).Master().getBranchCode())
                   && JournalProposal(lnCtr).Master().getDepartmentId() != null && !"".equals(JournalProposal(lnCtr).Master().getDepartmentId())  
                   && JournalProposal(lnCtr).getTotalDebitAmount() > 0.0000 && JournalProposal(lnCtr).getTotalCreditAmount()> 0.0000         
                   && !hasEmptyBranchOrDepartment ){
                    if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                        createNewJournalProposal();
                    }
                }
                
//                if(JournalProposal(lnCtr).getEditMode() != EditMode.ADDNEW){
//                    loJEPDetail = JournalProposal(lnCtr).Detail(JournalProposal(lnCtr).getDetailCount() - 1);
//                    if (loJEPDetail.getAccountCode() != null && !"".equals(loJEPDetail.getAccountCode())
//                        && (loJEPDetail.getDebitAmount() > 0.0000 || loJEPDetail.getCreditAmount() > 0.0000)) {
//                        createNewJournalProposal();
//                    }
//                } else {
//                    if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
//                        createNewJournalProposal();
//                    }
//                }
            }
            if ((getJournalProposalList().size() - 1) < 0) {
                if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
                    createNewJournalProposal();
                }
            }
        }
    }
    
    
private void createNewJournalProposal() throws CloneNotSupportedException, SQLException, GuanzonException {
    AddJournalProposal();

    JournalProposal proposal = JournalProposal(getJournalProposalList().size() - 1);
    proposal.NewTransaction();
    proposal.Master().setTransactionDate(poGRider.getServerDate());
    proposal.Master().setCompanyId(Master().getCompanyID());
    proposal.Master().setIndustryCode(Master().getIndustryID());
    proposal.Master().setSourceNo(Master().getTransactionNo());
    proposal.Master().setSourceCode(getSourceCode());
    proposal.ReloadDetail();
}
    
    public void ReloadWTDeductions() throws CloneNotSupportedException, SQLException, GuanzonException{
        int lnCtr = getWTaxDeductionsCount() - 1;
        Date fromdate = null, todate = null;
        boolean lbProceed = false;
        while (lnCtr >= 0) {
            if (!lbProceed) {
                fromdate = null;
                todate = null;
            }
            if (WTaxDeduction(lnCtr).getModel().getTaxCode() == null
                    || "".equals(WTaxDeduction(lnCtr).getModel().getTaxCode())) {
                System.out.println("REMOVE WTAX : " + WTaxDeduction(lnCtr).getModel().getTransactionNo());
                fromdate = WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getPeriodFrom();
                todate = WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getPeriodTo();
                WTaxDeduction().remove(lnCtr);
                lbProceed = true;
            }
            lnCtr--;
        }
        if ((getWTaxDeductionsCount() - 1) >= 0) {
            if (WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxCode() != null
                    && !"".equals(WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxCode())) {
                AddWTaxDeduction();
            }
        }

        if ((getWTaxDeductionsCount() - 1) < 0) {
            AddWTaxDeduction();
        }
        if (lbProceed) {
            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(fromdate);
            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(todate);
        }
    }
    
    /** Arsiela - 05-06-2026
    * Automatically assigns the default withholding tax deduction
    * for the current transaction when applicable.
    *
    * Applies only during ADDNEW or UPDATE mode.
    * Skips VAT-registered payees.
    * Checks existing withholding tax entries before assigning
    * the default tax rate.
    * Computes the tax amount after setting the base amount
    * and BIR form.
    *
    * @throws SQLException if database access fails
    * @throws GuanzonException if transaction validation fails
    */
    public void setDefaultWithHoldingTax() throws SQLException, GuanzonException{
        //auto set value for withholding tax
        if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
            if(!"2".equals((String) Master().Payee().APClientMaster().getValue("cVATRegis"))){ 
                String lsTaxRateId = getDefaulTaxRateId();
                for(int lnCtr = 0; lnCtr < getWTaxDeductionsCount(); lnCtr++){
                    if(WTaxDeduction(lnCtr).getModel().isReverse()){
                        if (WTaxDeduction(lnCtr).getModel().getTaxRateId() != null
                            && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())) {
                            return;
                        }
                    } else {
                        if(lsTaxRateId.equals(WTaxDeduction(lnCtr).getModel().getTaxRateId())){
//                            WTaxDeduction(lnCtr).getModel().isReverse(true);
//                            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBIRForm(WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().WithholdingTax().getTaxType());
//                            if(Master().getVATSale() > 0.0000){
//                                WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBaseAmount(Master().getVATSale());
//                            } else {
//                                WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBaseAmount(Master().getTransactionTotal());
//                            }
//                            computeTaxAmount();
//                            computeFields(false);
                            return;
                        }
                    }
                }
                
                if(DisbursementStatic.OPEN.equals(Master().getTransactionStatus()) || DisbursementStatic.CONFIRMED.equals(Master().getTransactionStatus())){
                    if (WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxRateId() == null
                            || "".equals(WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().getTaxRateId())) {
                        if(lsTaxRateId != null && !"".equals(lsTaxRateId)){
                            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setTaxRateId(lsTaxRateId);
                            WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBIRForm(WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().WithholdingTax().getTaxType());
                            if(Master().getVATSale() > 0.0000){
                                WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBaseAmount(Master().getVATSale());
                            } else {
                                WTaxDeduction(getWTaxDeductionsCount() - 1).getModel().setBaseAmount(Master().getTransactionTotal());
                            }
                        }
                    }
                }
                
                computeTaxAmount();
                computeFields(false);
            }
        }
    }
    
    /**
    * Retrieves the default withholding tax rate ID based on the
    * current industry and matching account description.
    *
    * @return the default tax rate ID, or empty string if not found
    */
    private String getDefaulTaxRateId(){
        String lsTaxRateId = "";
        try {
            String lsSQL = "SELECT  " +
                    " a.sTaxRteID  " +
                    ", a.sTaxDescr " +
                    ", a.sTaxTypex " +
                    ", a.sATaxCode " +
                    ", b.sDescript AS Account_Name " +
                    " FROM Withholding_Tax a " +
                    " LEFT JOIN Account_Chart b ON b.sAcctCode = a.sAcctCode AND b.sIndstCde = " + SQLUtil.toSQL(Master().getIndustryID());
            lsSQL = MiscUtil.addCondition(lsSQL, " b.sDescript LIKE " + SQLUtil.toSQL("%Withholding%Tax%Goods%"));
//                    + " OR b.sDescript LIKE " + SQLUtil.toSQL("%Withholding%Tax%Goods%"));
            System.out.println("Execute SQL : " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                return "";
            }
            
            while (loRS.next()) {
                lsTaxRateId = loRS.getString("sTaxRteID");
            }
            MiscUtil.close(loRS);
            
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        return lsTaxRateId;
    }
    
    /**
    * Reloads withholding tax deductions by removing invalid entries,
    * ensuring at least one valid record exists, and preserving period
    * dates when a row is removed.
    *
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if a database error occurs
    * @throws GuanzonException if a business rule violation occurs
    */
    private JSONObject validateCheckPayment(){
        poJSON = new JSONObject();
        if(Logical.YES.equals(Master().getBankPrint())){
            if(CheckPayments().getModel().getPayeeType() == null || "".equals(CheckPayments().getModel().getPayeeType())){
                poJSON.put("result", "error");
                poJSON.put("message", "Payee type cannot be empty.");
                return poJSON;
            }
            if(CheckPayments().getModel().getDesbursementMode() == null || "".equals(CheckPayments().getModel().getDesbursementMode())){
                poJSON.put("result", "error");
                poJSON.put("message", "Disbursement mode cannot be empty.");
                return poJSON;
            }
            if(Logical.YES.equals(CheckPayments().getModel().getDesbursementMode())){
                if(CheckPayments().getModel().getClaimant() == null || "".equals(CheckPayments().getModel().getClaimant())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Claimant cannot be empty.");
                    return poJSON;
                }
                if(Logical.NO.equals(CheckPayments().getModel().getClaimant())){
                    if(CheckPayments().getModel().getAuthorize() == null || "".equals(CheckPayments().getModel().getAuthorize())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Authorize representative cannot be empty.");
                        return poJSON;
                    }
                }
            }
        } 
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Initializes disbursement master fields with default values such as branch,
    * company, transaction date, and status.
    *
    * @return JSON result indicating success or error details
    */
    @Override
    public JSONObject initFields() {
        //Put initial model values here/
        poJSON = new JSONObject();
        try {
            poJSON = new JSONObject();
            Master().setBranchCode(poGRider.getBranchCode());
//            Master().setIndustryID(psIndustryId);
            Master().setCompanyID(psCompanyId);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(DisbursementStatic.OPEN);

        } catch (SQLException ex) {
            Logger.getLogger(Disbursement.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
    * Validates the current entry using DisbursementValidator.
    *
    * @param status transaction status to validate against
    * @return JSON result of validation
    */
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = new DisbursementValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }
    
    private JSONObject verifyJournals(String fsStatus, boolean fbIsWillSave){
        boolean lbCheckJEP = false;
        switch(fsStatus){
            case DisbursementStatic.VERIFIED:
            case DisbursementStatic.APPROVED:
            case DisbursementStatic.CERTIFIED:
            case DisbursementStatic.AUTHORIZED:
                if(poJournal != null){
                    if(fbIsWillSave){
                        if(poJournal.getEditMode() != EditMode.ADDNEW && poJournal.getEditMode() != EditMode.UPDATE){
                            lbCheckJEP = true;
                        } else {
                            if(poJournal.getTotalCreditAmount() <= 0.0000 && poJournal.getTotalDebitAmount() <= 0.0000){
                                lbCheckJEP = true;
                            }
                        }
                    } else {
                        if(poJournal.getEditMode() != EditMode.READY){
                            lbCheckJEP = true;
                        }
                    }
                } else {
                    lbCheckJEP = true;
                }

                //Check Journal Proposal
                if(lbCheckJEP){
                    for(int lnCtr = 0; lnCtr < getJournalProposalList().size(); lnCtr++){
                        JournalProposal loObj = JournalProposal(lnCtr);
                        if(fbIsWillSave){
                            if(loObj.getEditMode() != EditMode.ADDNEW && loObj.getEditMode() != EditMode.UPDATE){
                                poJSON.put("result", "error" );
                                poJSON.put("message", "Invalid update mode for journal proposal." );
                                return poJSON;
                            } else {
                                if(loObj.getTotalCreditAmount() > 0.0000 && loObj.getTotalDebitAmount() > 0.0000){
                                    lbCheckJEP = false;
                                    break;
                                }
                            }
                        } else {
                            if(loObj.getEditMode() != EditMode.READY){
                                poJSON.put("result", "error" );
                                poJSON.put("message", "Invalid update mode for journal." );
                                return poJSON;
                            }
                        }
                    }
                }
                
                if(lbCheckJEP){
                    poJSON.put("result", "error" );
                    poJSON.put("message", "Journal details cannot be empty." );
                    return poJSON;
                }
                 
                break;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Validates and prepares disbursement data before saving.
    * Performs field validation, recomputation, and change detection
    * to ensure data integrity and prevent redundant updates.
    *
    * @return JSON result indicating success or validation error
    * @throws SQLException database error
    * @throws GuanzonException custom validation error
    * @throws CloneNotSupportedException cloning error
    */
    @Override
    public JSONObject willSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        
        System.out.println("Class Edit Mode : " + getEditMode());
        System.out.println("Master Edit Mode : " + Master().getEditMode());
        System.out.println("Journal Class Edit Mode : " + poJournal.getEditMode());
        System.out.println("Journal Master Edit Mode : " + poJournal.Master().getEditMode());
        System.out.println("Check Class Edit Mode : " + poCheckPayments.getEditMode());
        System.out.println("Check Master Edit Mode : " + poCheckPayments.getModel().getEditMode());
        
        //Re-set the transaction no and voucher no
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
            Master().setVoucherNo(getVoucherNo());
        }
        
        poJSON = verifyJournals(psForm,true);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = isEntryOkay(Master().getTransactionStatus());
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Validate Check payment
        if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
            || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
            poJSON = validateCheckPayment();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //Recompute tax amount
        poJSON = computeTaxAmount();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Recompute fields to validate
        poJSON = computeFields(true);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Validate Withholding Tax Deductions
        Double ldblTotalBaseAmount = 0.0000;
        if(Master().getWithTaxTotal() > 0.0000){
            for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                if (WTaxDeduction(lnCtr).getModel().isReverse()
                    && WTaxDeduction(lnCtr).getModel().getBaseAmount() > 0.0000 
                    && !"".equals(WTaxDeduction(lnCtr).getModel().getTaxRateId()) 
                    && WTaxDeduction(lnCtr).getModel().getTaxRateId() != null
                    ) {
                    if(WTaxDeduction(lnCtr).getEditMode() == EditMode.ADDNEW || WTaxDeduction(lnCtr).getEditMode() == EditMode.UPDATE){
                        //validate period
                        poJSON = checkPeriodDate(lnCtr);
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                    
                    ldblTotalBaseAmount += WTaxDeduction(lnCtr).getModel().getBaseAmount(); 
                }
            }
            
            ldblTotalBaseAmount = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblTotalBaseAmount, false).replace(",", ""));
            double ldblVatSales = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVATSale(), false).replace(",", ""));
            if(ldblVatSales > 0.0000){
                if(!Objects.equals(ldblTotalBaseAmount, ldblVatSales)){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Total tax base amount must be equal to net vatable sales.");
                    return poJSON;
                }
            }
        }
        
        //Seek Approval
//        poJSON = callApproval();
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }

        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions
            String lsSourceNo = (String) item.getValue("sSourceNo");
            double lsAmount = Double.parseDouble(String.valueOf(item.getValue("nAmountxx")));
            if ((lsAmount == 0.0000 || "".equals(lsSourceNo) || lsSourceNo == null)
                && item.getEditMode() == EditMode.ADDNEW ){
                detail.remove(); // Correctly remove the item
            }
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getAmount() == 0.0000) {
                poJSON.put("result", "error");
                poJSON.put("message", "Disbursement detail cannot be zero.");
                return poJSON;
            }
        }
        
        Iterator<WithholdingTaxDeductions> loObject = WTaxDeduction().iterator();
        while (loObject.hasNext()) {
            WithholdingTaxDeductions item = loObject.next(); // Store the item before checking conditions
            String lsTaxRateId = (String) item.getModel().getTaxRateId();
            double lsAmount = item.getModel().getBaseAmount();
            if (lsAmount <= 0.0000 || "".equals(lsTaxRateId) || lsTaxRateId == null) {
                if(item.getEditMode() == EditMode.ADDNEW){
                    loObject.remove(); // Correctly remove the item
                } else {
                    item.getModel().isReverse(false);
                }
            }
        }
        
        //Validate Withholding Tax Deductions
//        if(Master().getWithTaxTotal() > 0.0000){
//            for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
//                if(WTaxDeduction(lnCtr).getEditMode() == EditMode.ADDNEW || WTaxDeduction(lnCtr).getEditMode() == EditMode.UPDATE){
//                    //validate period
//                    poJSON = checkPeriodDate(lnCtr);
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                }
//            }
//        }

        /*Put system validations and other assignments here*/
        boolean lbUpdated = false;
        if (DisbursementStatic.RETURNED.equals(Master().getTransactionStatus())) {
            try {
                DisbursementVoucher loRecord = new CashflowControllers(poGRider, null).DisbursementVoucher();
                poJSON = loRecord.InitTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                poJSON = loRecord.OpenTransaction(Master().getTransactionNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                lbUpdated = loRecord.getDetailCount() == getDetailCount();
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getTransactionDate().equals(Master().getTransactionDate());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDisbursementType().equals(Master().getDisbursementType());
                }
                if (lbUpdated) {
                    lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Master().getNetTotal()), String.format("%.4f", Master().getNetTotal())));
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.getDetailCount() == getDetailCount();
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.Journal().getDetailCount() == Journal().getDetailCount()-1;
                }
                if (lbUpdated) {
                    lbUpdated = loRecord.getWTaxDeductionsCount() == getWTaxDeductionsCount();
                }
                
                //Check disbursement detail
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Detail(lnCtr).getAmount()), String.format("%.4f", Detail(lnCtr).getAmount())));
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }
                //Check Disbursement Payment
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDisbursementType().equals(Master().getDisbursementType());
                }
                
                if (lbUpdated) {
                    if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
                        || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
                        lbUpdated = loRecord.CheckPayments().getModel().getBankID().equals(CheckPayments().getModel().getBankID());
                        if (lbUpdated) {
                            lbUpdated = loRecord.CheckPayments().getModel().getBankAcountID().equals(CheckPayments().getModel().getBankAcountID());
                        }
                        if (lbUpdated) {
                            lbUpdated = loRecord.CheckPayments().getModel().getPayeeType().equals(CheckPayments().getModel().getPayeeType());
                        }
                        if (lbUpdated) {
                            lbUpdated = loRecord.CheckPayments().getModel().getClaimant().equals(CheckPayments().getModel().getClaimant());
                        }
                        if (lbUpdated) {
                            lbUpdated = loRecord.CheckPayments().getModel().getAuthorize().equals(CheckPayments().getModel().getAuthorize());
                        }
                    } else {
                        lbUpdated = loRecord.OtherPayments().getModel().getBankID().equals(OtherPayments().getModel().getBankID());
                        if (lbUpdated) {
                            lbUpdated = loRecord.OtherPayments().getModel().getBankAcountID().equals(OtherPayments().getModel().getBankAcountID());
                        }
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.OtherPayments().getModel().getTotalAmount()), String.format("%.4f", OtherPayments().getModel().getTotalAmount())));
                        }
                        if (lbUpdated) {
                            lbUpdated = loRecord.OtherPayments().getModel().getTransactionNo().equals(OtherPayments().getModel().getTransactionNo());
                        }
                    
                    }
                }
                
                //Check Journal
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.Journal().getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getDebitAmount()), String.format("%.4f", Journal().Detail(lnCtr).getDebitAmount())));
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getCreditAmount()), String.format("%.4f", Journal().Detail(lnCtr).getCreditAmount())));
                        }
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }
                
                //Check Journal Proposal
                if (lbUpdated) {
                    lbUpdated = loRecord.getJournalProposalList().size() == (getJournalProposalList().size() - 1);
                    if (lbUpdated) {
                        for (int lnCtr = 0; lnCtr < loRecord.getJournalProposalList().size(); lnCtr++) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).getTotalDebitAmount()), String.format("%.4f", JournalProposal(lnCtr).getTotalDebitAmount())));
                            if (lbUpdated) {
                                lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).getTotalCreditAmount()), String.format("%.4f", JournalProposal(lnCtr).getTotalCreditAmount())));
                            }
                            if (!lbUpdated) {
                                break;
                            }

                            for (int lnRow = 0; lnRow < loRecord.JournalProposal(lnCtr).getDetailCount(); lnRow++) {
                                lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).Detail(lnRow).getDebitAmount()), String.format("%.4f", JournalProposal(lnCtr).Detail(lnRow).getDebitAmount())));
                                if (lbUpdated) {
                                    lbUpdated = (Objects.equals(String.format("%.4f", loRecord.JournalProposal(lnCtr).Detail(lnRow).getCreditAmount()), String.format("%.4f", JournalProposal(lnCtr).Detail(lnRow).getCreditAmount())));
                                }
                                if (lbUpdated) {
                                    lbUpdated = (Objects.equals(loRecord.JournalProposal(lnCtr).Detail(lnRow).getAccountCode(), JournalProposal(lnCtr).Detail(lnRow).getAccountCode()));
                                }
                                if (!lbUpdated) {
                                    break;
                                }
                            }

                        }
                    }
                }
                //Check Witholding Tax
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.getWTaxDeductionsCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.2f", loRecord.WTaxDeduction(lnCtr).getModel().getTaxAmount()), String.format("%.2f", WTaxDeduction(lnCtr).getModel().getTaxAmount())));
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.2f", loRecord.WTaxDeduction(lnCtr).getModel().getBaseAmount()), String.format("%.2f", WTaxDeduction(lnCtr).getModel().getBaseAmount())));
                        }
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }
                
                if (lbUpdated) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No update has been made.");
                    return poJSON;
                }
            } catch (ScriptException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON.put("result", "error");
                poJSON.put("message", MiscUtil.getException(ex));
                return poJSON;
            }
        } else if (DisbursementStatic.RETURNED_I.equals(Master().getTransactionStatus())) {
            try {
                DisbursementVoucher loRecord = new CashflowControllers(poGRider, null).DisbursementVoucher();
                poJSON = loRecord.InitTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                poJSON = loRecord.OpenTransaction(Master().getTransactionNo());
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                
                if (lbUpdated) {
                    lbUpdated = loRecord.Journal().getDetailCount() == Journal().getDetailCount()-1;
                }
                
                //Check disbursement detail
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Detail(lnCtr).getAmount()), String.format("%.4f", Detail(lnCtr).getAmount())));
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }
                
                //Check Journal
                if (lbUpdated) {
                    for (int lnCtr = 0; lnCtr <= loRecord.Journal().getDetailCount() - 1; lnCtr++) {
                        lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getDebitAmount()), String.format("%.4f", Journal().Detail(lnCtr).getDebitAmount())));
                        if (lbUpdated) {
                            lbUpdated = (Objects.equals(String.format("%.4f", loRecord.Journal().Detail(lnCtr).getCreditAmount()), String.format("%.4f", Journal().Detail(lnCtr).getCreditAmount())));
                        }
                        if (!lbUpdated) {
                            break;
                        }
                    }
                }
                
                if (lbUpdated) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No update has been made.");
                    return poJSON;
                }
            } catch (ScriptException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON.put("result", "error");
                poJSON.put("message", MiscUtil.getException(ex));
                return poJSON;
            }
        }
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
        }
        
        //Recompute amounts
        computeTaxAmount();
        computeFields(false);
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        System.out.println("--------------------------WILL SAVE---------------------------------------------");
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            System.out.println("COUNTER : " + lnCtr);
            System.out.println("Source No : " + Detail(lnCtr).getSourceNo());
            System.out.println("Source Code : " + Detail(lnCtr).getSourceCode());
            System.out.println("Detail Source : " + Detail(lnCtr).getDetailSource());
            System.out.println("Detail Source No : " + Detail(lnCtr).getDetailNo());
            System.out.println("Amount : " + Detail(lnCtr).getAmount());
            System.out.println("-----------------------------------------------------------------------");
        }
        computeFields(false); //Recompute
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Saves the disbursement after validating entry rules.
    *
    * @return JSON result from entry validation
    * @throws CloneNotSupportedException if cloning fails
    * @throws SQLException if database error occurs
    * @throws GuanzonException if validation or business rule fails
    */
   @Override
    public JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put saving business rules here*/
        return isEntryOkay(DisbursementStatic.OPEN);

    }
    
    public List<JournalProposal> paOrigJEP;
    private JSONObject populateOriginalValue(){
        poJSON = new JSONObject();
        paOrigJEP = new ArrayList<JournalProposal>();
        try {
            for(int lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                    paOrigJEP.add( new CashflowControllers(poGRider, logwrapr).JournalProposal());
                    paOrigJEP.get(paOrigJEP.size() - 1).InitTransaction();
                    JournalProposal loObj = paOrigJEP.get(paOrigJEP.size() - 1);
                    if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW){
                        poJSON = loObj.NewTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("populateOriginalValue : " + poJSON.get("message"));
                            return poJSON;
                        }
                    } else if(JournalProposal(lnCtr).getEditMode() == EditMode.UPDATE){
                        poJSON = loObj.OpenTransaction(JournalProposal(lnCtr).Master().getTransactionNo());
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("populateOriginalValue : " + poJSON.get("message"));
                            return poJSON;
                        }
                        poJSON = loObj.UpdateTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("populateOriginalValue : " + poJSON.get("message"));
                            return poJSON;
                        }
                    }
                    
                    System.out.println("---------SET ORIGINAL VALUE FOR JOURNAL PROPOSAL---------------");
                    
                    System.out.println("---------MASTER JOURNAL PROPOSAL---------------");
                    for (int lnCol = 1; lnCol <= loObj.Master().getColumnCount(); lnCol++) {
                        System.out.println(loObj.Master().getColumn(lnCol) + " ->> " + JournalProposal(lnCtr).Master().getValue(loObj.Master().getColumn(lnCol)));
                        loObj.Master().setValue(loObj.Master().getColumn(lnCol), JournalProposal(lnCtr).Master().getValue(loObj.Master().getColumn(lnCol)));
                    }
                    System.out.println("---------------------------------------------------------------");
                    for (int lnRow = 0; lnRow < JournalProposal(lnCtr).getDetailCount(); lnRow++){
                        System.out.println("---------DETAIL JOURNAL PROPOSAL---------------");
                        int lnDet = -1;
                        if(JournalProposal(lnCtr).Detail(lnRow).getEditMode() == EditMode.ADDNEW){
                            loObj.ReloadDetail();
                            lnDet = loObj.getDetailCount()-1;
                        } else {
//                            lnDet = loObj.Detail().indexOf(JournalProposal(lnCtr).Detail(lnRow));
                            lnDet = lnRow;
                        }
                        System.out.println("Detail : " + lnDet);
                        if(lnDet >= 0){
                            for (int lnCol = 1; lnCol <= loObj.Detail(lnDet).getColumnCount(); lnCol++) {
                                System.out.println(loObj.Detail(lnDet).getColumn(lnCol) + " ->> " + JournalProposal(lnCtr).Detail(lnRow).getValue(loObj.Detail(lnDet).getColumn(lnCol)));
                                loObj.Detail(lnDet).setValue(loObj.Detail(lnDet).getColumn(lnCol), JournalProposal(lnCtr).Detail(lnRow).getValue(loObj.Detail(lnDet).getColumn(lnCol)));
                            }
                        }
                        System.out.println("---------------------------------------------------------------");
                    }
                    System.out.println("---------------------------------------------------------------");

            }
        
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            return poJSON;
        } 
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    /**
    * Saves all related disbursement transactions including check/other payments,
    * bank account updates, journal entries, withholding tax deductions, and linked records.
    *
    * @return JSON result indicating success or failure of save operation
    */
    @Override
    public JSONObject saveOthers() {
        try {
            System.out.println("--------------------------SAVE OTHERS---------------------------------------------");
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
                System.out.println("COUNTER : " + lnCtr);
                System.out.println("Source No : " + Detail(lnCtr).getSourceNo());
                System.out.println("Source Code : " + Detail(lnCtr).getSourceCode());
                System.out.println("Detail Source : " + Detail(lnCtr).getDetailSource());
                System.out.println("Detail Source No : " + Detail(lnCtr).getDetailNo());
                System.out.println("Amount : " + Detail(lnCtr).getAmount());
                System.out.println("-----------------------------------------------------------------------");
            }
            
            switch(Master().getDisbursementType()){
                case DisbursementStatic.DisbursementType.CHECK:
                case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                    System.out.println("--------------------------SAVE CHECK PAYMENT---------------------------------------------");
                    //Save Check Payment
                    if(poCheckPayments != null){
                        if(poCheckPayments.getEditMode() == EditMode.ADDNEW || poCheckPayments.getEditMode() == EditMode.UPDATE){
                            poCheckPayments.getModel().setIndustryID(Master().getIndustryID());
                            poCheckPayments.getModel().setSourceNo(Master().getTransactionNo());
                            poCheckPayments.getModel().setModifyingId(poGRider.getUserID());
                            poCheckPayments.getModel().setModifiedDate(poGRider.getServerDate());
                            poCheckPayments.setWithParentClass(true);
                            poCheckPayments.setWithUI(false);
                            poJSON = poCheckPayments.saveRecord();
                            if ("error".equals((String) poJSON.get("result"))) {
                                System.out.println("Save Check Payment : " + poJSON.get("message"));
                                return poJSON;
                            }
                        }
                        
                        if(CheckStatus.PrintStatus.PRINTED.equals(poCheckPayments.getModel().getPrint())){
                            System.out.println("----------Bank Account Transaction----------");
                            //Bank Account Transaction
                            BankAccountTrans poBankAccountTrans = new BankAccountTrans(poGRider);
                            poJSON = poBankAccountTrans.InitTransaction();
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }         
                            poJSON = poBankAccountTrans.CheckDisbursement(
                                poCheckPayments.getModel().getBankAcountID(),
                                    poCheckPayments.getModel().getSourceNo(),
                               SQLUtil.toDate(xsDateShort(poCheckPayments.getModel().getCheckDate()), SQLUtil.FORMAT_SHORT_DATE),
                                     poCheckPayments.getModel().getAmount(),
                                     poCheckPayments.getModel().getCheckNo(),
                                    Master().getVoucherNo(),
                                  false);
                            if ("error".equals(poJSON.get("result"))) {
                                return poJSON;
                            }
                            System.out.println("--------------------------------------------");
                            
                            System.out.println("----------AP CLIENT MASTER----------");
                            //Insert AP Client
                            APTransaction loAPTrans = new APTransaction(poGRider, Master().getBranchCode());
                            //get detail per category to pass on payment issue category
//                            List<String> laPerCategory = getCategoryDetail();
//                            for (int lnCategory = 0; lnCategory <= laPerCategory.size() - 1; lnCategory++){    
//                            }
                            String lsClientId = Master().Payee().getAPClientID();
                            if(lsClientId == null || "".equals(lsClientId)){
                                lsClientId = Master().Payee().getClientID();
                            } 
                            poJSON = loAPTrans.PaymentIssue(lsClientId, 
                                    "",
                                    Master().getTransactionNo(),
                                    Master().getTransactionDate(),  
                                    Master().getNetTotal(), 
                                    false);
                            if ("error".equals(poJSON.get("result"))) {
                                return poJSON;
                            }
                            System.out.println("-----------------------------------");
                            
                            try {
                                GLTransaction loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
                                System.out.println("----------JOURNAL PROPOSAL ACCOUNT MASTER / LEDGER----------");
                                if(getJournalProposalList() != null){
                                    for(int lnJEP = 0; lnJEP < getJournalProposalList().size(); lnJEP++){
                                        if(JournalProposal(lnJEP).getEditMode() == EditMode.UPDATE){
                                            //GL Transaction Account Ledger
                                            loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
                                            loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());
                                            for(int lnCtr = 0; lnCtr <= JournalProposal(lnJEP).getDetailCount() - 1; lnCtr++){
            //                                    if(Journal().Detail(lnCtr).getCreditAmount() > 0.0000 || Journal().Detail(lnCtr).getDebitAmount() > 0.0000){
                                                if(JournalProposal(lnJEP).Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                                                    loGLTrans.addDetail(JournalProposal(lnJEP).Master().getBranchCode(), 
                                                            JournalProposal(lnJEP).Detail(lnCtr).getAccountCode(),
                                                            SQLUtil.toDate(xsDateShort(JournalProposal(lnJEP).Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
                                                            JournalProposal(lnJEP).Detail(lnCtr).getDebitAmount(), 
                                                            JournalProposal(lnJEP).Detail(lnCtr).getCreditAmount());
                                                }
                                            }
                                            loGLTrans.saveTransaction();
                                        }
                                    }
                                }
                                System.out.println("-----------------------------------");
                                
                                System.out.println("----------JOURNAL ACCOUNT MASTER / LEDGER----------");
                                if(poJournal != null){
                                    if(poJournal.getEditMode() == EditMode.UPDATE){
                                        //GL Transaction Account Ledger
                                        loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
                                        loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());
                                        for(int lnCtr = 0; lnCtr <= Journal().getDetailCount() - 1; lnCtr++){
        //                                    if(Journal().Detail(lnCtr).getCreditAmount() > 0.0000 || Journal().Detail(lnCtr).getDebitAmount() > 0.0000){
                                            if(Journal().Detail(lnCtr).isReverse()){ //Added by Arsiela 05-16-2026 04:24PM
                                                loGLTrans.addDetail(Journal().Master().getBranchCode(), 
                                                        Journal().Detail(lnCtr).getAccountCode(),
                                                        SQLUtil.toDate(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
                                                        Journal().Detail(lnCtr).getDebitAmount(), 
                                                        Journal().Detail(lnCtr).getCreditAmount());
                                            }
                                        }
                                        loGLTrans.saveTransaction();
                                    }
                                }
                                System.out.println("-----------------------------------");
                            } catch (GuanzonException | SQLException | NullPointerException ex) {
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                                poJSON.put("result", "error");
                                poJSON.put("message", MiscUtil.getException(ex));
                                return poJSON;
                            }
                        }
                        
                    } else {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Check info is not set.");
                        return poJSON;
                    }
                    System.out.println("-----------------------------------------------------------------------");
                    //Save Bank Account : triggered only in assign check
                    if(poBankAccount != null){
                        System.out.println("--------------------------SAVE BANK ACCOUNT---------------------------------------------");
                        if(poBankAccount.getEditMode() == EditMode.UPDATE){
                            //get the latest check no existed in bank account
                            String lsCheckNo = poCheckPayments.getModel().getCheckNo();
                            if(lsCheckNo == null || "".equals(lsCheckNo)){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Check No is not set.");
                                return poJSON;
                            }
                            
                            String lsMaxCheckNo = getMaxCheckNo();
                            if (lsMaxCheckNo.matches("\\d+") && lsCheckNo.matches("\\d+")) {
                                if(Long.parseLong(lsCheckNo) > Long.parseLong(lsMaxCheckNo)){
                                    lsMaxCheckNo = lsCheckNo;
                                }
                            }
                            //set the latest assigned check no
                            poBankAccount.getModel().setCheckNo(lsMaxCheckNo);
                            poBankAccount.getModel().setLastTransactionDate(poGRider.getServerDate());    
                            poBankAccount.getModel().setModifyingId(poGRider.getUserID());
                            poBankAccount.getModel().setModifiedDate(poGRider.getServerDate());
                            poBankAccount.setWithParentClass(true);
                            poBankAccount.setWithUI(false);
                            poJSON = poBankAccount.saveRecord();
                            if ("error".equals((String) poJSON.get("result"))) {
                                System.out.println("Save Bank Account : " + poJSON.get("message"));
                                return poJSON;
                            }
                        }
                        System.out.println("-----------------------------------------------------------------------");
                    }
                    break;
                case DisbursementStatic.DisbursementType.WIRED:
                case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                    //Save Other Payment
                    if(poOtherPayments != null){
                        System.out.println("--------------------------SAVE OTHER PAYMENT---------------------------------------------");
                        if(poOtherPayments.getEditMode() == EditMode.ADDNEW || poOtherPayments.getEditMode() == EditMode.UPDATE){
                            if(OtherPaymentStatus.POSTED.equals(poOtherPayments.getModel().getTransactionStatus())){
                                poOtherPayments.getModel().setAmountPaid(poOtherPayments.getModel().getTotalAmount());
                                pbIsUpdateAmountPaid = true;
                            }
                            poOtherPayments.getModel().setSourceNo(Master().getTransactionNo());
                            poOtherPayments.getModel().setModifiedDate(poGRider.getServerDate());
                            poOtherPayments.setWithParentClass(true);
                            poOtherPayments.setWithUI(false);
                            poJSON = poOtherPayments.saveRecord();
                            if ("error".equals((String) poJSON.get("result"))) {
                                System.out.println("Save Other Payment : " + poJSON.get("message"));
                                return poJSON;
                            }
                        }
                        System.out.println("-----------------------------------------------------------------------");
                    } else {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Other Payment info is not set.");
                        return poJSON;
                    }
                    break;
            }
            
//            if(Master().getWithTaxTotal() > 0.0000){
                System.out.println("--------------------------SAVE WITHHOLDING TAX DEDUCTION---------------------------------------------");
                //Save Withholding Tax Deductions
                for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                    if(WTaxDeduction(lnCtr).getEditMode() == EditMode.ADDNEW || WTaxDeduction(lnCtr).getEditMode() == EditMode.UPDATE){
                        WTaxDeduction(lnCtr).getModel().setSourceCode(getSourceCode());
                        WTaxDeduction(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
                        WTaxDeduction(lnCtr).getModel().setModifyingBy(poGRider.getUserID());
                        WTaxDeduction(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                        WTaxDeduction(lnCtr).setWithParentClass(true);
                        WTaxDeduction(lnCtr).setWithUI(false);
                        poJSON = WTaxDeduction(lnCtr).saveRecord();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Save Withholding Tax Deduction : " + poJSON.get("message"));
                            return poJSON;
                        }
                    }
                }
//            }
            
            System.out.println("--------------------------SAVE OTHER TRANSACTION---------------------------------------------");
            //Update other linked transaction in DV Detail
            poJSON = updateLinkedTransactions(Master().getTransactionStatus());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            System.out.println("-----------------------------------------------------------------------");
            
            //Save Journal Proposal
            System.out.println("--------------------------SAVE JOURNAL PROPOSAL---------------------------------------------");
             poJSON = saveJournalProposal();
            if ("error".equals((String) poJSON.get("result"))) {
                paOrigJEP = null;
                return poJSON;
            }
            System.out.println("-----------------------------------------------------------------------");
            
            //Save Journal
            System.out.println("--------------------------SAVE JOURNAL---------------------------------------------");
            if(poJournal != null){
                if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                    poJSON = validateJournal();
                    boolean lbContinue = (boolean) poJSON.get("continue");
                    if ("error".equals((String) poJSON.get("result"))) {
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
                        if ("error".equals((String) poJSON.get("result"))) {
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
            
            poJSON = updatePaymentsStatus();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException | ParseException | NullPointerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        
        //Clear value 
        paOrigJEP = null;
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    private JSONObject saveJournalProposal() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException, ParseException{
        poJSON = new JSONObject();
            if(paOrigJEP != null){
                if(!paOrigJEP.isEmpty()){
                    for(int lnCtr = 0;lnCtr < paOrigJEP.size();lnCtr++){
                        System.out.println("Original JEP ROW : " + lnCtr );
                        System.out.println("Original JEP Edit Mode : " + paOrigJEP.get(lnCtr).getEditMode());
                        System.out.println("Original Transnox : " + paOrigJEP.get(lnCtr).Master().getTransactionNo());
                    }
                    paJournalProposal = null;
                    paJournalProposal = new ArrayList<JournalProposal>();
                    paJournalProposal.addAll(paOrigJEP);
                }
            }
            if(getJournalProposalList() != null){
                poJSON = populateOriginalValue();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                for(int lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                    System.out.println("JEP ROW : " + lnCtr );
                    System.out.println("JEP Edit Mode : " + JournalProposal(lnCtr).getEditMode() );
                    if(JournalProposal(lnCtr).getEditMode() == EditMode.ADDNEW || JournalProposal(lnCtr).getEditMode() == EditMode.UPDATE){
                        if(JournalProposal(lnCtr).Master().isReverse()){
                            if(JournalProposal(lnCtr).getTotalDebitAmount() > 0.0000 || JournalProposal(lnCtr).getTotalCreditAmount() > 0.0000){
                                poJSON = JournalProposal(lnCtr).validateJournalProposal();
                                boolean lbContinue = (boolean) poJSON.get("continue");
                                if ("error".equals((String) poJSON.get("result"))) {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", poJSON.get("message").toString());
                                    return poJSON;
                                } 
                                if(lbContinue){
                                    JournalProposal(lnCtr).Master().setSourceNo(Master().getTransactionNo());
                                    JournalProposal(lnCtr).Master().setModifyingId(poGRider.getUserID());
                                    JournalProposal(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                                    JournalProposal(lnCtr).setWithParent(true);
                                    poJSON = JournalProposal(lnCtr).SaveTransaction();
                                    if ("error".equals((String) poJSON.get("result"))) {
                                        System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                                        return poJSON;
                                    }
                                }
                            }
                        } else {
                            JournalProposal loObj = new CashflowControllers(poGRider, logwrapr).JournalProposal();
                            poJSON = loObj.InitTransaction();
                            if ("error".equals((String) poJSON.get("result"))) {
                                System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                                return poJSON;
                            }
                            poJSON = loObj.OpenTransaction(JournalProposal(lnCtr).Master().getTransactionNo());
                            if ("error".equals((String) poJSON.get("result"))) {
                                System.out.println("Save Journal Proposal : " + poJSON.get("message"));
                                return poJSON;
                            }
                            loObj.setWithParent(true);
                            loObj.setWithUI(false);
                            if(psApprover == null || "".equals(psApprover)){
                                psApprover = poGRider.getUserID();
                            }
                            loObj.setApproving(psApprover);
                            switch(loObj.Master().getTransactionStatus()){
                                case JournalProposalStatus.OPEN:
                                    poJSON = loObj.VoidTransaction("");
                                    if ("error".equals((String) poJSON.get("result"))) {
                                        System.out.println("Void Journal Proposal : " + poJSON.get("message"));
                                        return poJSON;
                                    }
                                break;
                                case JournalProposalStatus.CONFIRMED:
                                case JournalProposalStatus.RETURNED:
                                    poJSON = loObj.CancelTransaction("");
                                    if ("error".equals((String) poJSON.get("result"))) {
                                        System.out.println("Cancel Journal Proposal : " + poJSON.get("message"));
                                        return poJSON;
                                    }
                                break;
                            }
                        }
                    } 
                }
            } 
            
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Extracts unique category codes from disbursement details based on source type.
    *
    * @return list of distinct category codes from detail records
    * @throws SQLException database error
    * @throws GuanzonException custom business rule error
    */
    private List<String> getCategoryDetail() throws SQLException, GuanzonException{
        //get detail per category
        List<String> laPerCategory = new ArrayList();
        for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
            switch(Detail(lnCtr).getSourceCode()){
                case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                    switch(Detail(lnCtr).SOADetail().getSourceCode()){
                        case SOATaggingStatic.APPaymentAdjustment:
                            //TODO
                        break;
                        case SOATaggingStatic.PaymentRequest:
                            //TODO
                        break;
                        case SOATaggingStatic.POReceiving:
                            if(!laPerCategory.contains(Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster().getCategoryCode())){
                                laPerCategory.add(Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster().getCategoryCode());
                            }
                        break;
                    }
                break;
                case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                    //TODO
                break;
                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                    //TODO
                break;
                case DisbursementStatic.SourceCode.PO_RECEIVING:
                    if(!laPerCategory.contains(Detail(lnCtr).POReceiving().getCategoryCode())){
                        laPerCategory.add(Detail(lnCtr).POReceiving().getCategoryCode());
                    }
                break;
                case DisbursementStatic.SourceCode.PO_RETURN:
                    if(!laPerCategory.contains(Detail(lnCtr).POReturn().getCategoryCode())){
                        laPerCategory.add(Detail(lnCtr).POReturn().getCategoryCode());
                    }
                break;
            }
        }
        return laPerCategory;
    } 
    
    /**
    * Called after successful save completion.
    */
    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }
    
    /**
    * Sets flag for updating amount paid status.
    *
    * @param fdblAmountPaid true if amount paid should be updated
    */
    public void setUpdateAmountPaid(boolean fdblAmountPaid){
        pbIsUpdateAmountPaid = fdblAmountPaid;
    }
    
    /**
    * Updates all linked transactions related to the disbursement voucher details.
    *
    * @param fsStatus transaction status used for update rules
    * @return JSON result indicating success or failure
    * @throws SQLException database error
    * @throws GuanzonException business validation error
    * @throws CloneNotSupportedException cloning error
    * @throws ParseException date parsing error
    */
    public JSONObject updateLinkedTransactions(String fsStatus) throws SQLException, GuanzonException, CloneNotSupportedException, ParseException{
        poJSON = new JSONObject();
        //Call Class for updating of linked transactions in DV Details
        Disbursement_LinkedTransactions loDVExtend = new Disbursement_LinkedTransactions();
        loDVExtend.setDisbursemmentVoucher(this, poGRider, logwrapr);
        loDVExtend.setUpdateAmountPaid(pbIsUpdateAmountPaid);
        poJSON = loDVExtend.updateLinkedTransactions(fsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Updates related journal and payment transactions based on the disbursement status.
    * Handles confirmation, voiding, cancellation, return, and disapproval of records.
    *
    * @param fsStatus transaction status used to determine update action
    * @return JSON result indicating success or failure
    * @throws ParseException if date parsing fails
    * @throws SQLException database error
    * @throws GuanzonException business rule violation
    * @throws CloneNotSupportedException cloning error
    * @throws ScriptException scripting error during processing
    */
    public JSONObject updateRelatedTransactions(String fsStatus, String fsApprovalId) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            poJournal.setWithParent(true);
            poJournal.setWithUI(false);
            if(fsApprovalId == null || "".equals(fsApprovalId)){
                fsApprovalId = poGRider.getUserID();
            }
            poJournal.setApproving(fsApprovalId);
            //Update Journal
            switch(fsStatus){
                case DisbursementStatic.APPROVED:
                    //Confirm Journal
                    poJSON = poJournal.ConfirmTransaction("");
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    break;
                case DisbursementStatic.VOID:
                    //Void Journal
                    poJSON = poJournal.VoidTransaction("");
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    break;
                case DisbursementStatic.CANCELLED:
                case DisbursementStatic.DISAPPROVED:
                    //Cancel Journal
                    poJSON = poJournal.CancelTransaction("");
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    break;
                case DisbursementStatic.RETURNED:
                case DisbursementStatic.RETURNED_I:
                    if(JournalProposalStatus.OPEN.equals(poJournal.Master().getTransactionStatus())){
                    } else {
                        //Return Journal
                        poJSON = poJournal.ReturnTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                    break;
            }
        }
        
        //Update Journal Proposal
        for(int lnCtr = 0; lnCtr < getJournalProposalList().size(); lnCtr++){
            JournalProposal loObj = JournalProposal(lnCtr);
            loObj.setWithParent(true);
            loObj.setWithUI(false);
            if(fsApprovalId == null || "".equals(fsApprovalId)){
                fsApprovalId = poGRider.getUserID();
            }
            loObj.setApproving(fsApprovalId);
            switch(fsStatus){
                case DisbursementStatic.APPROVED:
                        poJSON = loObj.ConfirmTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    break;
                case DisbursementStatic.VOID:
                    poJSON = loObj.VoidTransaction("");
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    break;
                case DisbursementStatic.CANCELLED:
                case DisbursementStatic.DISAPPROVED:
                    poJSON = loObj.CancelTransaction("");
                    if (!"success".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    break;
                case DisbursementStatic.RETURNED:
                case DisbursementStatic.RETURNED_I:
                    if(JournalProposalStatus.OPEN.equals(loObj.Master().getTransactionStatus())){
                    } else {
                        poJSON = loObj.ReturnTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                    break;
            }
        }
        
        //Update record status of Check Payment or Other Payment
        switch(fsStatus){
            case DisbursementStatic.VOID:
            case DisbursementStatic.CANCELLED:
            case DisbursementStatic.DISAPPROVED:
                switch(Master().getDisbursementType()){
                    case DisbursementStatic.DisbursementType.CHECK:
                    case DisbursementStatic.DisbursementType.CHECK_DEPOSIT:
                        //Save Check Payment
                        if(poCheckPayments != null){
                            poCheckPayments.setWithParentClass(true);
                            poCheckPayments.setWithUI(false);
                            if(psApprover != null && !"".equals(psApprover)){
//                                poCheckPayments.setApproving(psApprover);
                            }
                            poJSON = poCheckPayments.VoidTransaction("");
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                        break;
                    case DisbursementStatic.DisbursementType.WIRED:
                    case DisbursementStatic.DisbursementType.DIGITAL_PAYMENT:
                        //Save Other Payment
                        if(poOtherPayments != null){
                            poOtherPayments.setWithParentClass(true);
                            poOtherPayments.setWithUI(false);
                            if(fsApprovalId != null && !"".equals(fsApprovalId)){
//                                poOtherPayments.setApproving(psApprover);
                            }
                            poJSON = poOtherPayments.VoidTransaction("");
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                        break;
                }
                break;
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
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
    
    /**
     * Converts a date string (yyyy-MM-dd) to LocalDate.
     *
     * @param val date string
     * @return LocalDate representation
     */
    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
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
    
    
    /**
     * Resets journal entries if the given source transaction does not exist
     * in the current detail list and journal is in ADDNEW mode.
     *
     * @param fsSourceNo source transaction number
     * @param fsSourceCode source transaction code
     */
    private void resetJournal(String fsSourceNo, String fsSourceCode){
        boolean lbExist = false;
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            if(Detail(lnCtr).getSourceNo().equals(fsSourceNo)
                && Detail(lnCtr).getSourceCode().equals(fsSourceCode)){
                lbExist = true;
                break;
            }
        }
        
        if(!lbExist){
            if(Journal().getEditMode() == EditMode.ADDNEW){
                resetJournal();
            }
        }
    }
    
    /**
     * Add payables to DV Detail
     * @param transactionNo PRF Transaction No / Cache Payable Transaction No
     * @param payableType
     * @return
     * @throws CloneNotSupportedException
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject populateDetail(String transactionNo, String payableType)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", 0);
        switch (payableType) {
            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                poJSON = setPRFToDetail(transactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("row", 0);
                    return poJSON;
                }
                break;
            case DisbursementStatic.SourceCode.CASH_PAYABLE:
                poJSON = setCachePayableToDetail( transactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("row", 0);
                    return poJSON;
                }
                break;
            case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                poJSON = setSOAToDetail(transactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("row", 0);
                    return poJSON;
                }
                break;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Validates that detail records do not mix incompatible source codes.
    *
    * @param fsSourceCode source code to validate against existing details
    * @return JSON result indicating success or validation error
    */
    private JSONObject validateDetailSourceCode(String fsSourceCode){
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            if(Detail(lnCtr).getSourceCode() != null && !"".equals(Detail(lnCtr).getSourceCode())){
                if(!DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(Detail(lnCtr).getSourceCode())){
                    if(!fsSourceCode.equals(Detail(lnCtr).getSourceCode())
                        && !fsSourceCode.equals(DisbursementStatic.SourceCode.AP_ADJUSTMENT)){
                        
                        //Check if fsSource code is PO Return and current row si PO Receiving allow to link
                        if((fsSourceCode.equals(DisbursementStatic.SourceCode.PO_RETURN) 
                            || fsSourceCode.equals(DisbursementStatic.SourceCode.PO_RECEIVING))
                            && (DisbursementStatic.SourceCode.PO_RECEIVING.equals(Detail(lnCtr).getSourceCode())
                                || DisbursementStatic.SourceCode.PO_RETURN.equals(Detail(lnCtr).getSourceCode()))){
                           
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", getSourceCodeDescription(fsSourceCode) + " cannot be mix with " + getSourceCodeDescription(Detail(lnCtr).getSourceCode()));
                            return poJSON;
                        }
                    }
                }
            }
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Returns a human-readable description of a source code.
    *
    * @param fsSourceCode source code
    * @return description of the source code
    */
    public String getSourceCodeDescription(String fsSourceCode){
        switch(fsSourceCode){
            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                return "Payment Request";
            case DisbursementStatic.SourceCode.PO_RECEIVING:
                return "PO Receiving";
            case DisbursementStatic.SourceCode.PO_RETURN:
                return "PO Return";
            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                return "AP Adjustment";
            case DisbursementStatic.SourceCode.CASH_PAYABLE:
                return "Cash Payable";
            case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                return "SOA";
        }
        return "";
    }
    
    /**
     * Populate DV Detail based on selected Transaction
     * @param transactionNo the transaction number
     * @return JSON
     * @throws CloneNotSupportedException
     * @throws GuanzonException
     * @throws SQLException 
     */
    private JSONObject setPRFToDetail(String transactionNo) throws CloneNotSupportedException, GuanzonException, SQLException{
        //Reset Journal
        resetJournal(transactionNo, DisbursementStatic.SourceCode.PAYMENT_REQUEST);
        
        PaymentRequest loController = new CashflowControllers(poGRider, logwrapr).PaymentRequest();
        loController.setWithParent(true);
        loController.InitTransaction();
        poJSON = loController.OpenTransaction(transactionNo);
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        //Validate linked of transaction per source code
        poJSON = validateDetailSourceCode(loController.getSourceCode());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        int lnRow = getDetailCount() - 1; //set default value 
        Double ldblBalance = loController.Master().getNetTotal() - loController.Master().getAmountPaid();
        String lsClientId = loController.Master().Payee().getAPClientID(); //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
        if(lsClientId == null || "".equals(lsClientId)){
            lsClientId = loController.Master().Payee().getClientID();
        }
        
        //Validate transaction to be add in DV Detail
        poJSON = validateDetail(ldblBalance, loController.Master().getPayeeID(), lsClientId,loController.Master().getIndustryID());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }

        //Add PRF Detail to DV Detail
        //Check if transaction already exists in the list
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (loController.Master().getTransactionNo().equals(Detail(lnCtr).getSourceNo())
                && loController.getSourceCode().equals(Detail(lnCtr).getSourceCode())) {
                //If already exist break the loop to current row and it will be the basis for setting of value
                lnRow = lnCtr; 
                break;
            }
        }
        
        Detail(lnRow).setSourceNo(loController.Master().getTransactionNo());
        Detail(lnRow).setSourceCode(loController.getSourceCode());
        Detail(lnRow).setAmount(ldblBalance);
        Detail(lnRow).setAmountApplied(ldblBalance); //Set transaction balance as default applied amount
        
        Detail(lnRow).setDetailAdvances(Master().getTransactionNo());
        Detail(lnRow).setDetailVatExempt(ldblBalance + (Detail(lnRow).getDetailAdvances() - Detail(lnRow).getDetailSourceDiscount())); //Do not add discount amount from the souce for vat computations
        AddDetail();
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    //Added by Arsiela - 05-27-2026 11:50 AM
    //Load Payee check primary contact person of selected supplier
    private String findPrimaryContactPerson(String fsClientId) throws CloneNotSupportedException, GuanzonException, SQLException{
        ClientInfo loClient = new ClientControllers(poGRider, logwrapr).ClientInfo();
        loClient.initialize();
        loClient.setWithParentClass(true);
        loClient.setWithUI(false);

        poJSON = loClient.openClientRecord(fsClientId);
        if ("success".equals((String) poJSON.get("result"))) {
            for(int lnCheck = 0;lnCheck < loClient.getInstiContactCount(); lnCheck++){
                if(Logical.YES.equals(loClient.InstiContact(lnCheck).getcPayeexxx())
                    && loClient.InstiContact(lnCheck).isPrimaryContactPersion()){
                    return loClient.InstiContact(lnCheck).getcCPrsonID();
                }
            }
        } 
    
        return fsClientId;
    }
    
    /**
     * Populate DV Detail based on selected Transaction
     * @param transactionNo the transaction number
     * @return JSON
     * @throws CloneNotSupportedException
     * @throws GuanzonException
     * @throws SQLException 
     */
    private JSONObject setCachePayableToDetail(String transactionNo) throws CloneNotSupportedException, GuanzonException, SQLException{
        CachePayable loController = new CashflowControllers(poGRider, logwrapr).CachePayable();
        loController.setWithParent(true);
        loController.InitTransaction();
        poJSON = loController.OpenTransaction(transactionNo);
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        //Reset Journal
        resetJournal(loController.Master().getSourceNo(), loController.Master().getSourceCode());
        
        Payee loPayee = new CashflowControllers(poGRider, logwrapr).Payee();
        loPayee.initialize();
        
        //Check setted payee for AP Adjustment if cache payable source is from AP Adujstment - Arsiela 05-27-2026
        String lsClientId = loController.Master().getClientId();
        String lsIssuedTo = "";
        
        //Load Payee from AP Adjustment 
        if(DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(loController.Master().getSourceCode())){
            Model_AP_Payment_Adjustment loObj = new CashflowModels(poGRider).APPaymentAdjustment();
            loObj.initialize();
            poJSON = loObj.openRecord(loController.Master().getSourceNo());
            if ("error".equals((String) poJSON.get("result"))) {
                poJSON.put("row", 0);
                return poJSON;
            }
            
            lsIssuedTo = loObj.getIssuedTo();
            lsClientId = loObj.getClientId();
        } 
        
        if(lsIssuedTo != null && !"".equals(lsIssuedTo)){
            poJSON = loPayee.getModel().openRecord(lsIssuedTo); //Get the actual Payee setted for AP Payment Adjustment
        } else {
            poJSON = loPayee.getModel().openRecordByReference(findPrimaryContactPerson(lsClientId));
        }
        
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            poJSON.put("message", ((String) poJSON.get("message") + "\nPlease contact system administrator to check data of Payee for supplier " + loController.Master().Client().getCompanyName() + "."));
            return poJSON;
        }
        
        //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
        lsClientId = loPayee.getModel().getAPClientID(); 
        if(lsClientId == null || "".equals(lsClientId)){
            lsClientId = loPayee.getModel().getClientID();
        }
        
        //Validate linked of transaction per source code
        poJSON = validateDetailSourceCode(loController.Master().getSourceCode());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        int lnRow = getDetailCount() - 1; //set default value 
        Double ldblBalance = loController.Master().getNetTotal() - loController.Master().getAmountPaid();
//        Validate transaction to be add in DV Detail
        poJSON = validateDetail(ldblBalance, loPayee.getModel().getPayeeID(), lsClientId,loController.Master().getIndustryCode());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        System.out.println("Payee : " + Master().Payee().getPayeeName());
        System.out.println("Payee Client : " + Master().Payee().Client().getCompanyName());

        //Check if transaction already exists in the list
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (loController.Master().getSourceNo().equals(Detail(lnCtr).getSourceNo())
                && loController.Master().getSourceCode().equals(Detail(lnCtr).getSourceCode())) {
                //If already exist break the loop to current row and it will be the basis for setting of value
                lnRow = lnCtr; 
                break;
            }
        }
        
        if(DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(loController.Master().getSourceCode())
            || DisbursementStatic.SourceCode.PO_RETURN.equals(loController.Master().getSourceCode())){
            if(loController.Master().getReceivables() > 0.0000){
                ldblBalance = -ldblBalance;
            }
        }
        
        Detail(lnRow).setSourceNo(loController.Master().getSourceNo());
        Detail(lnRow).setSourceCode(loController.Master().getSourceCode());
        Detail(lnRow).setAmount(ldblBalance);
        Detail(lnRow).setAmountApplied(ldblBalance); //Set transaction balance as default applied amount
        
        
        //Apply Vat
        if(ldblBalance < 0 || (DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(loController.Master().getSourceCode()))){
            if(DisbursementStatic.SourceCode.PO_RETURN.equals(loController.Master().getSourceCode())) {
                Detail(lnRow).setDetailVatAmount(loController.Master().getVATAmount() > 0 ? -loController.Master().getVATAmount() : loController.Master().getVATAmount());
                Detail(lnRow).setDetailVatSales(loController.Master().getVATSales() > 0 ? -loController.Master().getVATSales() : loController.Master().getVATSales());
                Detail(lnRow).setDetailVatExempt(loController.Master().getVATExempt() > 0 ? -loController.Master().getVATExempt() : loController.Master().getVATExempt());
            } else {
                if(Master().getVATAmount() > 0.0000){
                    computeDetail(lnRow);
                } else {
                    Detail(lnRow).setDetailVatExempt(ldblBalance);
                }
            }
        } else {
            Detail(lnRow).setDetailVatAmount(loController.Master().getVATAmount());
            Detail(lnRow).setDetailVatSales(loController.Master().getVATSales());
            Detail(lnRow).setDetailVatExempt(loController.Master().getVATExempt());
        }
        
        Detail(lnRow).setDetailVatRates(loController.Master().getVATRates());
        Detail(lnRow).setDetailZeroVat(loController.Master().getZeroRated());
        
        JSONObject loJSON = computeFields(true);
        if ("error".equals((String) loJSON.get("result"))) {
            Detail().remove(lnRow);
            AddDetail();
            loJSON.put("row", lnRow);
            return loJSON;
        }
        
        AddDetail();
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
    * Loads SOA-related transactions and maps them into disbursement details.
    * Handles linked sources such as Payment Request, PO Receiving, and AP Adjustments,
    * computes balances, VAT, and validates each detail before adding to the list.
    *
    * @param transactionNo SOA transaction number
    * @return JSON result indicating success or failure
    * @throws CloneNotSupportedException cloning error
    * @throws GuanzonException business rule error
    * @throws SQLException database error
    */
    private JSONObject setSOAToDetail(String transactionNo) throws CloneNotSupportedException, GuanzonException, SQLException{
        SOATagging loController = new CashflowControllers(poGRider, logwrapr).SOATagging();
        loController.InitTransaction();
        poJSON = loController.OpenTransaction(transactionNo);
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        //Reset Journal
        resetJournal(transactionNo, loController.getSourceCode());
        
        poJSON = validateDetailSourceCode(loController.getSourceCode());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        Double ldblBalance = loController.Master().getNetTotal().doubleValue() - loController.Master().getAmountPaid().doubleValue();
        String lsClientId = findPrimaryContactPerson(loController.Master().getClientId());
        String lsPayeeId = loController.Master().getIssuedTo();
        if(lsPayeeId == null || "".equals(lsPayeeId)){
            Model_Payee loPayee = new CashflowModels(poGRider).Payee();
            poJSON = loPayee.openRecordByReference(lsClientId);
            if ("error".equals((String) poJSON.get("result"))) {
                poJSON.put("row", 0);
                poJSON.put("message", ((String) poJSON.get("message") + "\nPlease contact system administrator to check data of Payee for supplier " + loController.Master().Supplier().getCompanyName() + "."));
                return poJSON;
            } else {
                lsPayeeId = loPayee.getPayeeID();
                //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
                lsClientId = loPayee.getAPClientID();
                if(lsClientId == null || "".equals(lsClientId)){
                    lsClientId = loPayee.getClientID();
                }
            }
        } else {
            //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
            lsClientId = loController.Master().Payee().getAPClientID();
            if(lsClientId == null || "".equals(lsClientId)){
                lsClientId = loController.Master().Payee().getClientID();
            }
        }
        
        //Validate transaction to be add in DV Detail
        poJSON = validateDetail(ldblBalance,  lsPayeeId, lsClientId,loController.Master().getIndustryId());
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("row", 0);
            return poJSON;
        }
        
        //Add Cache Payable Detail to DV Detail
        for(int lnCtr = 0; lnCtr <= loController.getDetailCount() - 1;lnCtr++){
            //Skip the removed detail in SOA detail
            if(!loController.Detail(lnCtr).isReverse()){
                continue;
            }
            int lnRow = getDetailCount() - 1;
            String lsSource = "";
            Double ldblSourceBalance = 0.0000;
            Double ldblVatAmount = 0.0000;    
            Double ldblVatableSales = 0.0000;
            Double ldblVatExempt = 0.0000;
            Double ldblVatZeroRated = 0.0000;   
            Double ldblVatRate = 0.0000;  
            
            ldblBalance = loController.Detail(lnCtr).getAppliedAmount().doubleValue();
            if(ldblBalance < 0.0000){
                ldblBalance = ldblBalance * -1;
            }
            
            ldblBalance = ldblBalance - loController.Detail(lnCtr).getAmountPaid().doubleValue();
            //skip detail that is already paid
            if(ldblBalance <= 0.0000){
                continue;
            }
            
            //Check if transaction already exists in the list
            for (int lnDetailCtr = 0; lnDetailCtr <= getDetailCount() - 1; lnDetailCtr++) {
                //If detail is equal to SOA
                if(Detail(lnDetailCtr).getSourceNo().equals(loController.Master().getTransactionNo())
                      &&   Detail(lnDetailCtr).getSourceCode().equals(loController.getSourceCode())){
                    //if detail is equal to SOA detail
                    if(Detail(lnDetailCtr).getDetailSource().equals(loController.Detail(lnCtr).getSourceNo())
                        && Detail(lnDetailCtr).getDetailNo() == loController.Detail(lnCtr).getEntryNo().intValue()){
                        lnRow = lnDetailCtr;
                        break;
                    }
                }
            }
            
            //Populate DV Detail based on transaction linked in SOA Detail
            switch(loController.Detail(lnCtr).getSourceCode()){
                case SOATaggingStatic.PaymentRequest: 
                    PaymentRequest loPRF = new CashflowControllers(poGRider, logwrapr).PaymentRequest();
                    loPRF.setWithParent(true);
                    loPRF.InitTransaction();
                    poJSON = loPRF.OpenTransaction(loController.Detail(lnCtr).getSourceNo());
                    if ("error".equals((String) poJSON.get("result"))) {
                        poJSON.put("row", 0);
                        return poJSON;
                    }

                    ldblSourceBalance = loPRF.Master().getNetTotal() - loPRF.Master().getAmountPaid();
                    ldblVatExempt = ldblSourceBalance;
                    lsClientId = loPRF.Master().Payee().getAPClientID(); //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
                    if(lsClientId == null || "".equals(lsClientId)){
                        lsClientId = loPRF.Master().Payee().getClientID();
                    }
                    
                    //Validate transaction to be add in DV Detail
                    poJSON = validateDetail(ldblSourceBalance, loPRF.Master().getPayeeID(), lsClientId,loPRF.Master().getIndustryID());
                    if ("error".equals((String) poJSON.get("result"))) {
                        poJSON.put("row", 0);
                        return poJSON;
                    }
                break;
                case SOATaggingStatic.POReceiving: //With cache payable
                case SOATaggingStatic.APPaymentAdjustment: //With cache payable
                    lsSource = getCachePayable(loController.Detail(lnCtr).getSourceNo(), loController.Detail(lnCtr).getSourceCode());
                    if(lsSource != null && !"".equals(lsSource)){
                        CachePayable loCachePayable = new CashflowControllers(poGRider, logwrapr).CachePayable();
                        loCachePayable.setWithParent(true);
                        loCachePayable.InitTransaction();
                        poJSON = loCachePayable.OpenTransaction(lsSource);
                        if ("error".equals((String) poJSON.get("result"))) {
                            poJSON.put("row", 0);
                            return poJSON;
                        }
        
                        Payee loPayee = new CashflowControllers(poGRider, logwrapr).Payee();
                        loPayee.initialize();
                        
                        //Check setted payee for AP Adjustment if cache payable source is from AP Adujstment - Arsiela 05-27-2026
                        lsClientId = loController.Master().getClientId();
                        String lsIssuedTo = "";

                        //Load Payee from AP Adjustment 
                        if(DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(loCachePayable.Master().getSourceCode())){
                            Model_AP_Payment_Adjustment loObj = new CashflowModels(poGRider).APPaymentAdjustment();
                            loObj.initialize();
                            poJSON = loObj.openRecord(loCachePayable.Master().getSourceNo());
                            if ("error".equals((String) poJSON.get("result"))) {
                                poJSON.put("row", 0);
                                return poJSON;
                            }

                            lsIssuedTo = loObj.getIssuedTo();
                            lsClientId = loObj.getClientId();
                        } 

                        if(lsIssuedTo != null && !"".equals(lsIssuedTo)){
                            poJSON = loPayee.getModel().openRecord(lsIssuedTo); //Get the actual Payee setted for AP Payment Adjustment
                        } else {
                            String lsContactPerson = findPrimaryContactPerson(lsClientId);
                            if(lsContactPerson != null && !"".equals(lsContactPerson)){
                                poJSON = loPayee.getModel().openRecordByReference(lsContactPerson);
                            } else {
                                poJSON = loPayee.getModel().openRecordByReference(lsClientId);
                            }
                        }

                        if ("error".equals((String) poJSON.get("result"))) {
                            poJSON.put("row", 0);
                            poJSON.put("message", ((String) poJSON.get("message") + "\nPlease contact system administrator to check data of Payee for supplier " + loCachePayable.Master().Client().getCompanyName() + "."));
                            return poJSON;
                        }

                        //mandatory get the AP client id for PRF if null then get the Client Id - Arsiela 05-27-2026
                        lsClientId = loPayee.getModel().getAPClientID(); 
                        if(lsClientId == null || "".equals(lsClientId)){
                            lsClientId = loPayee.getModel().getClientID();
                        }
                        
                        ldblSourceBalance = loCachePayable.Master().getNetTotal() - loCachePayable.Master().getAmountPaid();
                        //Validate transaction to be add in DV Detail
                        poJSON = validateDetail(ldblSourceBalance, loPayee.getModel().getPayeeID(), lsClientId,loController.Master().getIndustryId());
                        if ("error".equals((String) poJSON.get("result"))) {
                            poJSON.put("row", 0);
                            return poJSON;
                        }
                        
                        ldblVatAmount = loCachePayable.Master().getVATAmount();   
                        ldblVatableSales = loCachePayable.Master().getVATSales();
                        ldblVatExempt = loCachePayable.Master().getVATExempt();
                        ldblVatZeroRated = loCachePayable.Master().getZeroRated();   
                        ldblVatRate = loCachePayable.Master().getVATRates();
                        
                        if(DisbursementStatic.SourceCode.AP_ADJUSTMENT.equals(loCachePayable.Master().getSourceCode())){
                            if(loCachePayable.Master().getReceivables() > 0.0000){
                                ldblBalance = -ldblBalance;
                                ldblVatAmount = 0.0000;
                                ldblVatableSales = 0.0000;
                                ldblVatExempt = 0.0000;
                            }
                        }
                    }
                    break;
            }
                    
            Detail(lnRow).setSourceNo(loController.Master().getTransactionNo());
            Detail(lnRow).setSourceCode(loController.getSourceCode());
            Detail(lnRow).setDetailNo(loController.Detail(lnCtr).getEntryNo().intValue());
            Detail(lnRow).setDetailSource(loController.Detail(lnCtr).getSourceNo());
            Detail(lnRow).setAmount(ldblBalance);
            Detail(lnRow).setAmountApplied(ldblBalance); //Set transaction balance as default applied amount
            //Apply Vat
            switch(Detail(lnRow).SOADetail().getSourceCode()){
                case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                    //Apply Vat
                    if(ldblBalance < 0){
                        if(Master().getVATAmount() > 0.0000){
                            computeDetail(lnRow);
                        } else {
                            ldblVatExempt = ldblBalance;
                        }
                    }
                break;
            }
            
            Detail(lnRow).setDetailAdvances(Master().getTransactionNo());
            if(DisbursementStatic.SourceCode.PAYMENT_REQUEST.equals(Detail(lnRow).SOADetail().getSourceCode())){
                ldblVatExempt = ldblVatExempt + (Detail(lnRow).getDetailAdvances() - Detail(lnRow).getDetailSourceDiscount()); //Default to add advances in vat exempt
            }
            
            Detail(lnRow).setDetailVatAmount(ldblVatAmount);
            Detail(lnRow).setDetailVatSales(ldblVatableSales);
            Detail(lnRow).setDetailVatExempt(ldblVatExempt);
            Detail(lnRow).setDetailZeroVat(ldblVatZeroRated);
            Detail(lnRow).setDetailVatRates(ldblVatRate);
            
            
            JSONObject loJSON = computeFields(true);
            if ("error".equals((String) loJSON.get("result"))) {
                Detail().remove(lnRow);
                AddDetail();
                loJSON.put("row", lnRow);
                return loJSON;
            }
            
            AddDetail();
        }
    
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    /**
     * get Cache Payable Transaction No
     * @param fsSourceNo
     * @param fsSourceCode
     * @return 
     */
    public String getCachePayable(String fsSourceNo, String fsSourceCode){
        String lsTransactionNo = "";
        try {
            Model_Cache_Payable_Master object = new CashflowModels(poGRider).Cache_Payable_Master();
            String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(object),
                    " sSourceNo = " + SQLUtil.toSQL(fsSourceNo)
                   + " AND sSourceCd = " + SQLUtil.toSQL(fsSourceCode));
//                   + " AND sClientID = " + SQLUtil.toSQL(Master().Payee().getClientID()));  //Commented by Arsiela 05-27-2026; Replaced by script below

            if(Master().Payee().getAPClientID() != null && !"".equals(Master().Payee().getAPClientID())){
                lsSQL = lsSQL + " AND sClientID = " + SQLUtil.toSQL(Master().Payee().getAPClientID()); // If AP client ID is not null or not empty check to Payee AP Client ID
            } else {
                lsSQL = lsSQL + " AND sClientID = " + SQLUtil.toSQL(Master().Payee().getClientID()); //Check thru payee client ID
            }
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (loRS.next()) {
                lsTransactionNo = loRS.getString("sTransNox");
                MiscUtil.close(loRS);
            } 
        } catch (SQLException | GuanzonException ex) { 
            return lsTransactionNo;
        }
            
        return lsTransactionNo;
    }
    /**
     * get Inventory Type Code value
     * @param fsValue description
     * @return 
     */
    private String getInvTypeCode(String fsValue){
        try {
            String lsSQL = "SELECT sInvTypCd, sDescript FROM Inv_Type ";
            lsSQL = MiscUtil.addCondition(lsSQL, " cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                                + " AND lower(sDescript) LIKE " + SQLUtil.toSQL("%"+fsValue));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return  loRS.getString("sInvTypCd");
                    }
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
                return "";
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return "";
        }
            
        return  "";
    }
    /**
     * get Inventory Type Code value
     * @param
     * @return 
     */
    private boolean withCategory(String fsCategory, String fsInvTypeCode){
        try {
            Model_Category loCategory = new ParamModels(poGRider).Category();
            String lsSQL = MiscUtil.makeSelect(loCategory);
            lsSQL = MiscUtil.addCondition(lsSQL, " sInvTypCd = " + SQLUtil.toSQL(fsInvTypeCode)
                                                    + " AND sCategrCd = " + SQLUtil.toSQL(fsCategory));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return true;
                    }
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return false;
        }
            
        return  false;
    }
    /**
     * Get Particular based on inv type code: No connection for cache payable detail to particular
     * @param fsInvTypeCode
     * @param fsCategory
     * @return 
     */
    public String generateParticular(String fsInvTypeCode, String fsCategory){
        String lsDescript = "";
        try {
            String lsSQL = getInvTypeCategorySQL();
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sInvTypCd = " + SQLUtil.toSQL(fsInvTypeCode));
            if(fsCategory != null && !"".equals(fsCategory)){
                lsSQL =  lsSQL + " AND b.sCategrCd = " + SQLUtil.toSQL(fsCategory);
            }
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0 && loRS.next()) {
                    //PURCHASING
                    switch(loRS.getString("sInvTypex").toLowerCase().replace(" ", "")){
                        case "merchandiserelatedinventory":
                            switch(loRS.getString("sCategrCd")){
                                case DisbursementStatic.Category.MOBILEPHONE: //"0001": //CELLPHONE
                                    lsDescript = "Purchases - Mobile Phone";
                                    break;
                                case DisbursementStatic.Category.APPLIANCES: //"0002": //APPLIANCES
                                    lsDescript = "Purchases - Appliances";
                                    break;
                                case DisbursementStatic.Category.MOTORCYCLE: //"0003": //MC UNIT
                                    lsDescript = "Purchases - Motorcycle";
                                    break;
                                case DisbursementStatic.Category.SPMC: //"0004": //MC SPAREPARTS
                                    lsDescript = "Purchases - Spareparts";
                                    break;
                                case DisbursementStatic.Category.CAR: //"0005": //CAR UNIT
                                    lsDescript = "Purchases - Car";
                                    break;
                                case DisbursementStatic.Category.SPCAR: //"0006": //CAR SPAREPARTS
                                    lsDescript = "Purchases - Spareparts";
                                    break;
                                case DisbursementStatic.Category.GENERAL: //"0007": //GENERAL
                                    lsDescript = "Purchases - General";
                                    break;
                                case DisbursementStatic.Category.FOOD: //"0008": //LP - Food
                                case DisbursementStatic.Category.HOSPITALITY: //"0009": //Monarch - Food
                                    lsDescript = "Purchases - LP";
                                    break;
                            }
                        break;
                        default:
                            lsDescript = loRS.getString("sInvTypex").toLowerCase();
                        break;
                    }
                    lsDescript = getParticularId(lsDescript);
                    
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
                return "";
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return "";
        }
            
        return  lsDescript;
    }
    
    /**
     * get Inventory Type Code value
     * @param fsValue description
     * @return 
     */
    private String getParticularId(String fsValue){
        try {
            String lsSQL = "SELECT sPrtclrID, sDescript, sTranType FROM Particular ";
            lsSQL = MiscUtil.addCondition(lsSQL, " lower(sDescript) LIKE " + SQLUtil.toSQL("%"+fsValue+"%"));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return  loRS.getString("sPrtclrID");
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
    
    /**
     * Validate insertion of detail
     * @param fdblBalance
     * @param fsPayeeId
     * @param fsClientId
     * @return 
     */
    private JSONObject validateDetail(double fdblBalance, String fsPayeeId, String fsClientId, String fsIndustryId){
        try {
            if(Master().getIndustryID() == null || "".equals(Master().getIndustryID())){
                Master().setIndustryID(fsIndustryId);
            } else {
                if ((Detail(getDetailCount() - 1).getSourceNo() == null || "".equals(Detail(getDetailCount() - 1).getSourceNo()))
                    && Detail(getDetailCount() - 1).getAmountApplied() <= 0.0000
                    && getDetailCount() <= 1
                    && getEditMode() == EditMode.ADDNEW){
                    Master().setIndustryID(fsIndustryId);
                } else {
                    if (!Master().getIndustryID().equals(fsIndustryId)) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected transaction industry must be equal to current industry in disbursement.");
                        poJSON.put("row", 0);
                        return poJSON;
                    }
                }
            }
            System.out.println("Industry ID : " + Master().getIndustryID());
            if(fdblBalance <= 0.0000){
                poJSON.put("result", "error");
                poJSON.put("message", "No remaining balance for the selected transaction.\n\nContact System Administrator to address the issue.");
                poJSON.put("row", 0);
                return poJSON;
            }

            if(Master().getPayeeID() == null || "".equals(Master().getPayeeID())){
                Master().setPayeeID(fsPayeeId);
                setSearchPayee(Master().Payee().getPayeeName());
                if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
                    || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
                    CheckPayments().getModel().setPayeeID(Master().getPayeeID());
                }
            } else {
                if ((Detail(getDetailCount() - 1).getSourceNo() == null || "".equals(Detail(getDetailCount() - 1).getSourceNo()))
                    && Detail(getDetailCount() - 1).getAmountApplied() <= 0.0000
                    && getDetailCount() <= 1
                    && getEditMode() == EditMode.ADDNEW){
                    Master().setPayeeID(fsPayeeId);
                } else {
                    if (!Master().getPayeeID().equals(fsPayeeId)) {
                        //Check if payee ap client is equal to the ap client linked to disbursement voucher - Arsiela 05-28-2026
                        boolean lbSuccess = true;
                        Model_Payee loObj = new CashflowModels(poGRider).Payee();
                        loObj.initialize();
                        loObj.openRecord(fsPayeeId);
                        if(loObj.getEditMode() == EditMode.READY){
                            if(Master().Payee().getAPClientID() != null && !"".equals(Master().Payee().getAPClientID())){
                                lbSuccess = Master().Payee().getAPClientID().equals(loObj.getAPClientID());
                            } else {
                                lbSuccess = Master().Payee().getClientID().equals(loObj.getClientID());
                            }
                        } else {
                            lbSuccess = false;
                        }
                        if(!lbSuccess){
                            poJSON.put("result", "error");
                            poJSON.put("message", "Selected Payee of payables is not equal to transaction payee.");
                            poJSON.put("row", 0);
                            return poJSON;
                        }
                    }
                }
            }

            if(Master().getSupplierClientID() == null || "".equals(Master().getSupplierClientID())){
                if(Master().getPayeeID() == null || "".equals(Master().getPayeeID())){
                    Master().setPayeeID(Master().Payee().getPayeeID());
                    if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
                        || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
                        CheckPayments().getModel().setPayeeID(Master().getPayeeID());
                    }
                }
                
                Master().setSupplierClientID(fsClientId);
                
            } else {
                if ((Detail(getDetailCount() - 1).getSourceNo() == null || "".equals(Detail(getDetailCount() - 1).getSourceNo()))
                    && Detail(getDetailCount() - 1).getAmountApplied() <= 0.0000
                    && getDetailCount() <= 1
                    && getEditMode() == EditMode.ADDNEW){
                    Master().setSupplierClientID(fsClientId);
                } else {
                    if (!Master().getSupplierClientID().equals(fsClientId)) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Supplier of payables is not equal to transaction supplier.");
                        poJSON.put("row", 0);
                        return poJSON;
                    }
                }
            }
            
            //Update filtering of transaction by setted payee and ap client / client
            setSearchPayee(Master().Payee().getPayeeName());
            setSearchClient(Master().Payee().APClient().getCompanyName());
            if(Master().Payee().APClient().getCompanyName() == null || "".equals(Master().Payee().APClient().getCompanyName())){
                setSearchClient(Master().Payee().Client().getCompanyName());
            }
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("row", 0);
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", 0);
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
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
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
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                break;
                case EditMode.UPDATE:
                    if(poJournal.getEditMode() == EditMode.READY || poJournal.getEditMode() == EditMode.UNKNOWN){
                        poJSON = poJournal.OpenTransaction(lsJournal);
                        if ("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                        poJournal.UpdateTransaction();
                    } 
                break;
            }
        } else {
            
            if(DisbursementStatic.OPEN.equals(Master().getTransactionStatus()) 
                || DisbursementStatic.CONFIRMED.equals(Master().getTransactionStatus()) 
                || DisbursementStatic.VERIFIED.equals(Master().getTransactionStatus()) 
                || DisbursementStatic.RETURNED.equals(Master().getTransactionStatus()) 
                || DisbursementStatic.RETURNED_I.equals(Master().getTransactionStatus()) 
            ){
                //Continue
            } else {
                //skip creation of journal
                poJSON.put("result", "success");
                return poJSON;
            }
            
            if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poJournal.getEditMode() != EditMode.ADDNEW){
                poJSON = poJournal.NewTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                //Moved loading of TBJ on a function
//                loadTBJ();

                //Journa Entry Master
                poJournal.Master().setAccountPerId("");
                poJournal.Master().setIndustryCode(Master().getIndustryID());
                poJournal.Master().setBranchCode(Master().getBranchCode());
                poJournal.Master().setDepartmentId(poGRider.getDepartment());
                poJournal.Master().setTransactionDate(poGRider.getServerDate()); 
                poJournal.Master().setCompanyId(Master().getCompanyID());
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
        
        System.out.println("JOURNAL EDIT MODE :  " + poJournal.getEditMode());
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public void loadTBJ() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        System.out.println("loadTBJ JOURNAL EDIT MODE :  " + poJournal.getEditMode());
        String lsJournal = existJournal();
        if(poJournal.getEditMode() != EditMode.ADDNEW && (lsJournal == null || "".equals(lsJournal))){
            populateJournal();
        }
        
        if((poJournal.getTotalCreditAmount() > 0.0000 || Journal().getTotalDebitAmount() > 0.0000)){
            return;
        }
        
//        try { 
            //get detail per category
            List<String> laPerCategory = new ArrayList();
            for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                switch(Detail(lnCtr).getSourceCode()){
                    case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                        switch(Detail(lnCtr).SOADetail().getSourceCode()){
                            case SOATaggingStatic.APPaymentAdjustment:
                            case SOATaggingStatic.PaymentRequest:
                                //TODO
                                if(!laPerCategory.contains(psNoCategory)){
                                    laPerCategory.add(psNoCategory);
                                }
                            break;
                            case SOATaggingStatic.POReceiving:
                                if(!laPerCategory.contains(Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster().getCategoryCode())){
                                    laPerCategory.add(Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster().getCategoryCode());
                                }
                            break;
                        }
                    break;
                    case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                    case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                        //TODO
                        if(!laPerCategory.contains(psNoCategory)){
                            laPerCategory.add(psNoCategory);
                        }
                    break;
                    case DisbursementStatic.SourceCode.PO_RECEIVING:
                        if(!laPerCategory.contains(Detail(lnCtr).POReceiving().getCategoryCode())){
                            laPerCategory.add(Detail(lnCtr).POReceiving().getCategoryCode());
                        }
                    break;
                    case DisbursementStatic.SourceCode.PO_RETURN:
                        if(!laPerCategory.contains(Detail(lnCtr).POReturn().getCategoryCode())){
                            laPerCategory.add(Detail(lnCtr).POReturn().getCategoryCode());
                        }
                    break;
                }
            }

            for (int lnCategory = 0; lnCategory <= laPerCategory.size() - 1; lnCategory++){    
                //retreiving using column index
                JSONObject jsonmaster = new JSONObject();
                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++){
                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
                    jsonmaster.put(Master().getColumn(lnCtr),  Master().getValue(lnCtr));
                }

                String lsCategory = "";
                JSONArray jsondetails = new JSONArray();
                JSONObject jsondetail = new JSONObject();
                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                    switch(Detail(lnCtr).getSourceCode()){
                        case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                            switch(Detail(lnCtr).SOADetail().getSourceCode()){
                                case SOATaggingStatic.APPaymentAdjustment:
                                case SOATaggingStatic.PaymentRequest:
                                    //TODO
                                    lsCategory = psNoCategory;
                                break;
                                case SOATaggingStatic.POReceiving:
                                    lsCategory = Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster().getCategoryCode();
                                break;
                            }
                        break;
                        case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                        case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                            //TODO
                            lsCategory = psNoCategory;
                        break;
                        case DisbursementStatic.SourceCode.PO_RECEIVING:
                           lsCategory = Detail(lnCtr).POReceiving().getCategoryCode();
                        break;
                        case DisbursementStatic.SourceCode.PO_RETURN:
                           lsCategory = Detail(lnCtr).POReturn().getCategoryCode();
                        break;
                    }

                    if(laPerCategory.get(lnCategory).equals(lsCategory)){
                        //store detail values per row that equal to category
                        jsondetail = new JSONObject();
                        for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++){
                            System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
                            jsondetail.put(Detail(lnCtr).getColumn(lnCol),  Detail(lnCtr).getValue(lnCol));
                        }
                        jsondetails.add(jsondetail);
                    }
                }

                jsondetail = new JSONObject();
                jsondetail.put("Disbursement_Master", jsonmaster);
                jsondetail.put("Disbursement_Detail", jsondetails);
                lsCategory = laPerCategory.get(lnCategory);
                if(psNoCategory.equals(lsCategory)){
                    lsCategory = "";
                }
                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE,Master().getIndustryID(), lsCategory); //Master().getIndustryID()
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
            }
        
//        } catch (SQLException | GuanzonException | CloneNotSupportedException | ScriptException ex) {
//            System.out.println("getException :" + MiscUtil.getException(ex));
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//        } 
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
        
        if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
            && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
            lsSQL = lsSQL + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.VOID)
                          + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.CANCELLED);
        }
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
     * Check existing Journal Proposal
     * @return
     * @throws SQLException 
     */
    public JSONObject loadJournalProposal() throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.READY || getEditMode() == EditMode.UPDATE){ //Load saved journal proposal
            paJournalProposal = new ArrayList<JournalProposal>();

            Model_Journal_Master_Proposal loMaster = new CashflowModels(poGRider).Journal_Master_Proposal();
            String lsSQL = MiscUtil.makeSelect(loMaster);
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
            );
                  
            if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
                && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
                lsSQL = lsSQL + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.VOID)
                              + " AND cTranStat != " + SQLUtil.toSQL(JournalProposalStatus.CANCELLED);
            }
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            boolean lbExist = false;
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------JOURNAL ENTRY PROPOSAL--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        lbExist = false;
                        int lnCtr = 0;
                        for(lnCtr = 0;lnCtr < getJournalProposalList().size(); lnCtr++){
                            if(JournalProposal(lnCtr).Master().getTransactionNo().equals(loRS.getString("sTransNox"))){
                                lbExist = true;
                                break;
                            }
                        }

                        if(!lbExist){
                            AddJournalProposal();
                            lnCtr = getJournalProposalList().size() - 1;
                            poJSON = JournalProposal(lnCtr).OpenTransaction(loRS.getString("sTransNox"));
                            if("error".equals((String) poJSON.get("result"))){
                                return poJSON;
                            }
                        }
                        
                        if(getEditMode() == EditMode.UPDATE && JournalProposal(lnCtr).getEditMode() == EditMode.READY){
                            poJSON = JournalProposal(getJournalProposalList().size() - 1).UpdateTransaction();
                            if("error".equals((String) poJSON.get("result"))){
                                return poJSON;
                            }
                        }
                    }  
                }
            } 
            MiscUtil.close(loRS);
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "No record to load");
        return poJSON;
    }
    
    
    /**
     * Populate Check
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject populateCheck() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        if(poCheckPayments == null || getEditMode() == EditMode.READY){
            poCheckPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
            poCheckPayments.initialize();
        }
        
        String lsCheck = existCheckPayments();
        if(lsCheck != null && !"".equals(lsCheck)){
            switch(getEditMode()){
                case EditMode.READY:
                    poJSON = poCheckPayments.openRecord(lsCheck);
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                break;
                case EditMode.UPDATE:
                    if(poCheckPayments.getEditMode() == EditMode.READY || poCheckPayments.getEditMode() == EditMode.UNKNOWN){
                        poJSON = poCheckPayments.openRecord(lsCheck);
                        if ("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                        poCheckPayments.updateRecord();
                    } 
                break;
            }
        } else {
            if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poCheckPayments.getEditMode() != EditMode.ADDNEW){
                poJSON = poCheckPayments.newRecord();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                //Set initial value for check payment
                poCheckPayments.getModel().setCheckDate(Master().getTransactionDate());
                poCheckPayments.getModel().setSourceNo(Master().getTransactionNo());
                poCheckPayments.getModel().setBranchCode(Master().getBranchCode());
                poCheckPayments.getModel().setIndustryID(Master().getIndustryID());
                poCheckPayments.getModel().setTransactionStatus(CheckStatus.FLOAT);
                poCheckPayments.getModel().setSourceCode(getSourceCode());
                poCheckPayments.getModel().setPayeeID(Master().getPayeeID());
                
            } else if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poCheckPayments.getEditMode() == EditMode.ADDNEW) {
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
     * Check existing check payment
     * @return
     * @throws SQLException 
     */
    private String existCheckPayments() throws SQLException{
        Model_Check_Payments loMaster = new CashflowModels(poGRider).CheckPayments();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        
        if(DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
            || DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND sSourceCd = " + SQLUtil.toSQL(getSourceCode()));
            
                    if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
                        && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
                        lsSQL = lsSQL + " AND ( cTranStat = " + SQLUtil.toSQL(CheckStatus.FLOAT)
                         + " OR cTranStat = " + SQLUtil.toSQL(CheckStatus.OPEN)
                         + " OR cTranStat = " + SQLUtil.toSQL(CheckStatus.POSTED)
                         + " ) ";
                    }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND sSourceCd = " + SQLUtil.toSQL(getSourceCode()));
            
                if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
                    && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
                    lsSQL = lsSQL + " AND ( cTranStat = " + SQLUtil.toSQL(CheckStatus.FLOAT)
                    + " OR cTranStat = " + SQLUtil.toSQL(CheckStatus.OPEN)
                    + " ) ";
                }
        }
        lsSQL = lsSQL + " ORDER BY dTransact, sTransNox DESC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------CHECK PAYMENT--------------------------");
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
    
    public JSONObject populateCheckNo() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        if(poBankAccount == null || getEditMode() == EditMode.READY){
            poBankAccount = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
            poBankAccount.initialize();
        }
        
        switch(getEditMode()){
            case EditMode.READY:
                poJSON = poBankAccount.openRecord(poCheckPayments.getModel().getBankAcountID());
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            break;
            case EditMode.UPDATE:
                if(poBankAccount.getEditMode() == EditMode.ADDNEW || poBankAccount.getEditMode() == EditMode.READY || poBankAccount.getEditMode() == EditMode.UNKNOWN){
                    poJSON = poBankAccount.openRecord(poCheckPayments.getModel().getBankAcountID());
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                    poJSON = poBankAccount.updateRecord();
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                } 
            break;
        }

        String lsCheckNo = "";
        if(poBankAccount.getEditMode() == EditMode.UPDATE) {
            //Update check info in check payments
            if (poCheckPayments.getModel().getCheckNo() == null || "".equals(poCheckPayments.getModel().getCheckNo())) {
                //get the latest check no existed in bank account
                lsCheckNo = poBankAccount.getModel().getCheckNo();
                if(lsCheckNo != null && !"".equals(lsCheckNo)){
                    if (lsCheckNo.matches("\\d+")) {
                        long incremented = Long.parseLong(lsCheckNo) + 1;
                        lsCheckNo = String.format("%0" + lsCheckNo.length() + "d", incremented);
                    }
                } else {
                    lsCheckNo = "";
                }
                poCheckPayments.getModel().setCheckNo(lsCheckNo);
                poCheckPayments.getModel().setCheckDate(poGRider.getServerDate());
            }
            
            //Set check amount
            poCheckPayments.getModel().setAmount(Master().getNetTotal());
            poCheckPayments.getModel().setTransactionStatus(CheckStatus.OPEN); //Update check status in assign check
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject existCheckNo(String checkNo) throws SQLException {
        poJSON = new JSONObject();
        String lsSQL = "SELECT sCheckNox FROM Check_Payments ";
        lsSQL = MiscUtil.addCondition(lsSQL, " sCheckNox = " + SQLUtil.toSQL(checkNo)
                                            + " AND sSourceNo <> " + SQLUtil.toSQL(Master().getTransactionNo())
                                            + " LIMIT 1");

        ResultSet loRS = null;
        System.out.println("Executing SQL: " + lsSQL);
        loRS = poGRider.executeQuery(lsSQL);

        if (loRS != null && loRS.next()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Check no " + loRS.getString("sCheckNox") + " is already exist");
        } else {
            poJSON.put("result", "success");
        }
        MiscUtil.close(loRS);
        return poJSON;
    }
    
    /**
     * Retrieves the latest (maximum) check number used for the current bank account.
     *
     * @return highest check number as String
     * @throws SQLException database error during query execution
     */
    public String getMaxCheckNo() throws SQLException {
        String lsCheckNo = "";
        String lsSQL = " SELECT "
                    + " MAX(b.sCheckNox) AS sCheckNox "
                    + " FROM Bank_Account_Master a "
                    + " LEFT JOIN Check_Payments b ON b.sBnkActID = a.sBnkActID ";
        lsSQL = MiscUtil.addCondition(lsSQL, "a.sBnkActID = " + SQLUtil.toSQL(poCheckPayments.getModel().getBankAcountID()));

        ResultSet loRS = null;
        System.out.println("Executing SQL: " + lsSQL);
        loRS = poGRider.executeQuery(lsSQL);

        if (loRS != null && loRS.next()) {
            lsCheckNo = loRS.getString("sCheckNox");
        }
        MiscUtil.close(loRS);
        return lsCheckNo;
    }
    
    /**
     * Populate Check
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject populateOtherPayment() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        if(poOtherPayments == null || getEditMode() == EditMode.READY){
            poOtherPayments = new CashflowControllers(poGRider, logwrapr).OtherPayments();
            poOtherPayments.initialize();
        }
        
        String lsCheck = existOtherPayments();
        if(lsCheck != null && !"".equals(lsCheck)){
            switch(getEditMode()){
                case EditMode.READY:
                    poJSON = poOtherPayments.openRecord(lsCheck);
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                break;
                case EditMode.UPDATE:
                    if(poOtherPayments.getEditMode() == EditMode.READY || poOtherPayments.getEditMode() == EditMode.UNKNOWN){
                        poJSON = poOtherPayments.openRecord(lsCheck);
                        if ("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                        poOtherPayments.updateRecord();
                    } 
                    
                    poOtherPayments.getModel().setPaymentType(Master().getDisbursementType()); //get the latest payment type
                break;
            }
        } else {
            if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poOtherPayments.getEditMode() != EditMode.ADDNEW){
                poJSON = poOtherPayments.newRecord();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                //Set initial value for other payment
                poOtherPayments.getModel().setSourceNo(Master().getTransactionNo());
                poOtherPayments.getModel().setTransactionDate(Master().getTransactionDate());
                poOtherPayments.getModel().setBranchCode(Master().getBranchCode());
                poOtherPayments.getModel().setIndustryID(Master().getIndustryID());
                poOtherPayments.getModel().setTransactionStatus(OtherPaymentStatus.FLOAT);
                poOtherPayments.getModel().setSourceCode(getSourceCode());
                poOtherPayments.getModel().setTotalAmount(Master().getNetTotal());
                poOtherPayments.getModel().setCompanyID(Master().getCompanyID());
                poOtherPayments.getModel().setPaymentType(Master().getDisbursementType());
                
            } else if((getEditMode() == EditMode.UPDATE || getEditMode() == EditMode.ADDNEW) && poOtherPayments.getEditMode() == EditMode.ADDNEW) {
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
     * Check existing check payment
     * @return
     * @throws SQLException 
     */
    public String existOtherPayments() throws SQLException{
        Model_Other_Payments loMaster = new CashflowModels(poGRider).OtherPayments();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        
        if(!DisbursementStatic.DisbursementType.CHECK.equals(Master().getDisbursementType())
            && !DisbursementStatic.DisbursementType.CHECK_DEPOSIT.equals(Master().getDisbursementType())){
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND sSourceCd = " + SQLUtil.toSQL(getSourceCode()));
//                    + " AND ( cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.FLOAT)
//                    + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.OPEN)
//                    + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.POSTED)
//                    + " ) "
//            );
            if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
                && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
                lsSQL = lsSQL + " AND ( cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.FLOAT)
                 + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.OPEN)
                 + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.POSTED)
                 + " ) ";
            }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND sSourceCd = " + SQLUtil.toSQL(getSourceCode()));
//                    + " AND ( cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.FLOAT)
//                    + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.OPEN)
//                    + " ) "
//            );
            if(!DisbursementStatic.VOID.equals(Master().getTransactionStatus())
                && !DisbursementStatic.CANCELLED.equals(Master().getTransactionStatus())){
                lsSQL = lsSQL + " AND ( cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.FLOAT)
                + " OR cTranStat = " + SQLUtil.toSQL(OtherPaymentStatus.OPEN)
                + " ) ";
            }
        }
        lsSQL = lsSQL + " ORDER BY dTransact, sTransNox DESC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------OTHER PAYMENT--------------------------");
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
     * Populate Check
     * @return
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject populateWithholdingTaxDeduction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        switch(getEditMode()){
            case EditMode.READY:
                paWTaxDeductions = new ArrayList<>();
                Model_Withholding_Tax_Deductions loMaster = new CashflowModels(poGRider).Withholding_Tax_Deductions();
                String lsSQL = MiscUtil.makeSelect(loMaster);
                lsSQL = MiscUtil.addCondition(lsSQL,
                        " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                        + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
//                        + " AND cReversex = " + SQLUtil.toSQL(DisbursementStatic.Reverse.INCLUDE)
                );
                System.out.println("Executing SQL: " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                if (MiscUtil.RecordCount(loRS) > 0) {
                    while (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------WITHHOLDING TAX DEDUCTIONS--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            paWTaxDeductions.add( new CashflowControllers(poGRider,logwrapr).WithholdingTaxDeductions());
                            poJSON = paWTaxDeductions.get(paWTaxDeductions.size() - 1).openRecord(loRS.getString("sTransNox"));
                            if ("error".equals((String) poJSON.get("result"))){
                                if(Master().getWithTaxTotal() > 0.0000){
                                    return poJSON;
                                } 
                            } else {
                                //add tax code
                                paWTaxDeductions.get(paWTaxDeductions.size() - 1).getModel().setTaxCode(paWTaxDeductions.get(paWTaxDeductions.size() - 1).getModel().WithholdingTax().getTaxCode());
                            }
                        }  
                    }
                }
                MiscUtil.close(loRS);
            break;
            case EditMode.ADDNEW:   
                if(paWTaxDeductions.isEmpty()){
                    paWTaxDeductions.add(new CashflowControllers(poGRider,logwrapr).WithholdingTaxDeductions());
                    //set default period date
                    paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodFrom(Master().getTransactionDate());
                    paWTaxDeductions.get(getWTaxDeductionsCount() - 1).getModel().setPeriodTo(Master().getTransactionDate());
                }
            break;
            case EditMode.UPDATE:
                for(int lnCtr = 0; lnCtr <= getWTaxDeductionsCount() - 1;lnCtr++){
                    if(WTaxDeduction(lnCtr).getEditMode() == EditMode.READY){
                        poJSON = WTaxDeduction(lnCtr).updateRecord();
                        if ("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                    }
                }
            break;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
   /**
    * Retrieves linked disbursement voucher transactions for a given source.
    *
    * @param sourceNo source transaction number
    * @param sourceCode source transaction code
    * @param client client name filter
    * @return concatenated voucher numbers of linked transactions
    */
    public String getLinkedPayment(String sourceNo, String sourceCode, String client){
        String lsTransactionNo = "";
        try {
            ResultSet loRS;
            String lsSQL;
            
            //Check if transaction is already linked to DV
            lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                    " b.sSourceNo = " + SQLUtil.toSQL(sourceNo)
                    + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                    + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.CANCELLED)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.VOID)
                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.DISAPPROVED)
//                    + " AND a.cTranStat != " + SQLUtil.toSQL(DisbursementStatic.RETURNED)
                    + " AND ( e.sCompnyNm LIKE " + SQLUtil.toSQL("%"+client + "%") 
                    + "    OR d.sPayeeNme LIKE "+ SQLUtil.toSQL("%"+client+"%")+" )"
            );
            System.out.println("Executing SQL: " + lsSQL);
            loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------DV--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("------------------------------------------------------------------------------");
                    if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                        if(lsTransactionNo.isEmpty()){
                            lsTransactionNo = loRS.getString("sVouchrNo");
                        } else {
                            lsTransactionNo = lsTransactionNo + ", " + loRS.getString("sVouchrNo");
                        }
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return lsTransactionNo;
    }
    
    /**
     * Load Payables
     * @param payableType the transaction type selected
     * @return JSON and load all payables
     */
    public JSONObject loadPayables(String payableType) {
        poJSON = new JSONObject();
        
        try {
            //Default union all
            String lsSQL = getCachePayables() 
                            + " UNION  " + getPaymentRequest()
                            + " UNION  " + getSOA();

            switch(payableType){
                case DisbursementStatic.SourceCode.CASH_PAYABLE:
                    lsSQL = getCachePayables();
                break;
                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                    lsSQL = getPaymentRequest();
                break;
                case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                    lsSQL = getSOA();
                break;
            }

            lsSQL = lsSQL + " ORDER BY dDueDatex, Payee ASC ";
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (loRS == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }

            int lnctr = 0;
            String lsTransactionType = "";
            JSONArray dataArray = new JSONArray();
            while (loRS.next()) {
                switch(loRS.getString("TransactionType")){
                    case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                        lsTransactionType = "SOA";
                        break;
                    case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                        lsTransactionType = "Payment Request";
                        break;
                    case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                        lsTransactionType = "AP Adjustment";
                        break;
                    case DisbursementStatic.SourceCode.PO_RECEIVING:
                        lsTransactionType = "PO Receiving";
                        break;
                    case DisbursementStatic.SourceCode.PO_RETURN:
                        lsTransactionType = "PO Return";
                        break;
                }
                
                JSONObject record = new JSONObject();
                record.put("sTransNox", loRS.getString("sTransNox"));
                record.put("sBranchNme", loRS.getString("Branch"));
                record.put("dTransact", loRS.getDate("dDueDatex"));
                record.put("Balance", loRS.getDouble("Balance"));
                record.put("TransactionType", lsTransactionType);
                record.put("PayableType", loRS.getString("PayableType"));
                record.put("Reference", loRS.getString("Reference"));
                record.put("SourceNo", loRS.getString("sSourceNo"));
                record.put("SourceCd", loRS.getString("TransactionType"));
                record.put("Industry", loRS.getString("IndstryD"));
                //Load actual payee 
                record.put("Payee", loRS.getString("Payee"));
                record.put("ClientId", loRS.getString("xClientID"));
                
                dataArray.add(record);
                lnctr++;
            }
            MiscUtil.close(loRS);

            if (lnctr > 0) {
                poJSON.put("result", "success");
                poJSON.put("message", "Record(s) loaded successfully.");
                poJSON.put("data", dataArray);
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
                poJSON.put("data", new JSONArray());
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } 
        
        return poJSON;
    }
    
    //QUERY LIST
    private String getCachePayables(){
        String lsIndustry = "";
        if(Master().getIndustryID() != null && !"".equals(Master().getIndustryID())){
            lsIndustry =  " AND a.sIndstCdx =  " +  SQLUtil.toSQL(Master().getIndustryID()) ; 
        }
        String lsSQL = "SELECT a.sIndstCdx AS Industry, "
                + "a.sCompnyID AS Company, "
                + "b.sBranchNm AS Branch, "
                + "a.sTransNox, "
                + "a.dTransact, "
                //+ "(a.nNetTotal - a.nAmtPaidx) AS Balance, "
                + "CASE WHEN a.nRecvbles > 0 THEN -(a.nNetTotal - a.nAmtPaidx) ELSE (a.nNetTotal - a.nAmtPaidx) END AS Balance, "
                + SQLUtil.toSQL(DisbursementStatic.SourceCode.CASH_PAYABLE) +" AS PayableType, "
                + "a.sSourceCd AS TransactionType, "
                + SQLUtil.toSQL("Cache_Payable_Master") +" AS SourceTable, "
                + "IFNULL(c.sPayeeNme,cc.sCompnyNm) AS Payee, "
                + "a.sReferNox AS Reference, "
                + "a.sSourceNo AS sSourceNo, "
                + "IFNULL(a.dDueDatex,a.dTransact) AS dDueDatex"
                + ", d.sDescript AS IndstryD "
                + ", IFNULL(c.sAPClntID,IF(c.sAPClntID = '', a.sClientID, a.sClientID)) AS xClientID "
                + " FROM Cache_Payable_Master a "
                + " LEFT JOIN Payee c ON a.sClientID = c.sClientID " 
                + " LEFT JOIN Client_Master cc ON a.sClientID = cc.sClientID  LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx, "
                + " Branch b "
                + " WHERE a.sBranchCd = b.sBranchCd "
                + " AND a.cTranStat = " +  SQLUtil.toSQL(CachePayableStatus.CONFIRMED)
                + " AND (a.nNetTotal - a.nAmtPaidx) > '0.0000' "
                + " AND a.cProcessd = '0' " 
                +  lsIndustry
                + " AND a.sCompnyID = " +  SQLUtil.toSQL(psCompanyId)
                + " AND (a.cWithSOAx = '0' OR a.cWithSOAx = '' OR a.cWithSOAx IS NULL)" //Retrieve only transaction without SOA
                + " AND b.sBranchNm LIKE " +  SQLUtil.toSQL("%"+psBranch+"%");
        
                if((psPayee == null || "".equals(psPayee)) || (psClient == null || "".equals(psClient))){
                    if(psPayee != null && !"".equals(psPayee)){
                        lsSQL = lsSQL + " AND IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%");
                    }
                    if(psClient != null && !"".equals(psClient)){
                        lsSQL = lsSQL + " AND IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%");
                    }
                } else {
                    lsSQL = lsSQL +  " AND ( IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
                                    + " OR IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%")
                                    + " ) ";
                }
        
//                + " AND ( IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
//                + " OR IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%")
//                + " ) "
//                + "AND ( c.sPayeeNme LIKE  " +  SQLUtil.toSQL("%"+psPayee) + " OR c.sPayeeNme IS NULL ) "
//                + " GROUP BY a.sTransNox ";
                return lsSQL + " GROUP BY a.sTransNox ";
    }
    
    private String getPaymentRequest(){
        String lsIndustry = "";
        if(Master().getIndustryID() != null && !"".equals(Master().getIndustryID())){
            lsIndustry =  " AND a.sIndstCdx =  " +  SQLUtil.toSQL(Master().getIndustryID()) ; 
        }
        String lsSQL =  "SELECT a.sIndstCdx AS Industry, "
                + "a.sCompnyID AS Company, "
                + "b.sBranchNm AS Branch, "
                + "a.sTransNox, "
                + "a.dTransact, "
                + "(a.nNetTotal - a.nAmtPaidx) AS Balance, "
                + SQLUtil.toSQL(DisbursementStatic.SourceCode.PAYMENT_REQUEST) +" AS PayableType, "
                + SQLUtil.toSQL(DisbursementStatic.SourceCode.PAYMENT_REQUEST) +" AS TransactionType, "
                + SQLUtil.toSQL("Payment_Request_Master") +" AS SourceTable, "
                + "c.sPayeeNme AS Payee, "
                + "a.sSeriesNo AS Reference, "
                + "a.sTransNox AS sSourceNo, "
                + "a.dTransact AS dDueDatex "
                + ", d.sDescript AS IndstryD "
                + ", IFNULL(c.sAPClntID,IF(c.sAPClntID = '', c.sClientID, c.sClientID)) AS xClientID "
                + " FROM Payment_Request_Master a "
                + " LEFT JOIN Payee c ON a.sPayeeIDx = c.sPayeeIDx  "
                + " LEFT JOIN Client_Master cc ON cc.sClientID = c.sAPClntID  "
                + " LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx, "
                + " Branch b "
                + " WHERE a.sBranchCd = b.sBranchCd "
                + " AND a.cTranStat = " +  SQLUtil.toSQL(PaymentRequestStatus.CONFIRMED)
                + " AND (a.nNetTotal - a.nAmtPaidx) > '0.0000' " 
                + " AND a.cProcessd = '0' " 
//                + "AND a.sIndstCdx IN ( " +  SQLUtil.toSQL(psIndustryId) + ", '' ) "
                + lsIndustry
                + " AND (a.cWithSOAx = '0' OR a.cWithSOAx = '' OR a.cWithSOAx IS NULL)" //Retrieve only transaction without SOA
                + " AND a.sCompnyID = " +  SQLUtil.toSQL(psCompanyId)
                + " AND b.sBranchNm LIKE " +  SQLUtil.toSQL("%"+psBranch+"%");
//                + " AND c.sPayeeNme LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
//                + " GROUP BY a.sTransNox ";
        
        if((psPayee == null || "".equals(psPayee)) || (psClient == null || "".equals(psClient))){
            if(psPayee != null && !"".equals(psPayee)){
                lsSQL = lsSQL + " AND c.sPayeeNme LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%");
            }
            if(psClient != null && !"".equals(psClient)){
                lsSQL = lsSQL + " AND IFNULL(cc.sCompnyNm,c.sPayeeNme) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%");
            }
        } else {
            lsSQL = lsSQL +  " AND ( c.sPayeeNme LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
                            + " OR IFNULL(cc.sCompnyNm,c.sPayeeNme) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%")
                            + " ) ";
        }
        
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        
        return lsSQL;
    }
    
    private String getSOA(){
        String lsIndustry = "";
        if(Master().getIndustryID() != null && !"".equals(Master().getIndustryID())){
            lsIndustry =  " AND a.sIndstCdx =  " +  SQLUtil.toSQL(Master().getIndustryID()) ; 
        }
        String lsSQL = "SELECT a.sIndstCdx AS Industry, "
                + "a.sCompnyID AS Company, "
                + "b.sBranchNm AS Branch, "
                + "a.sTransNox, "
                + "a.dTransact, "
                + "(a.nNetTotal - a.nAmtPaidx) AS Balance, "
                + SQLUtil.toSQL(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE) +" AS PayableType, "
                + SQLUtil.toSQL(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE) +" AS TransactionType, "
                + SQLUtil.toSQL("AP_Payment_Master") +" AS SourceTable, "
                + "IFNULL(c.sPayeeNme,cc.sCompnyNm) AS Payee, "
                + "a.sSOANoxxx AS Reference, "
                + "a.sTransNox AS sSourceNo, "
                + "a.dTransact AS dDueDatex "
                + ", d.sDescript AS IndstryD "
                + ", IFNULL(c.sAPClntID,IF(c.sAPClntID = '', a.sClientID, a.sClientID)) AS xClientID "
                + " FROM AP_Payment_Master a "
                + " LEFT JOIN Payee c ON a.sIssuedTo = c.sPayeeIDx LEFT JOIN Client_Master cc ON a.sClientID = cc.sClientID  LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx, "
                + " Branch b "
                + " WHERE a.sBranchCd = b.sBranchCd "
                + " AND a.cTranStat = " +  SQLUtil.toSQL(PaymentRequestStatus.CONFIRMED)
                + " AND (a.nNetTotal - a.nAmtPaidx) > '0.0000' " 
                + " AND a.cProcessd = '0' " 
//                + "AND a.sIndstCdx IN  ( " +  SQLUtil.toSQL(psIndustryId) + ", '' ) "
                + lsIndustry
                + " AND a.sCompnyID = " +  SQLUtil.toSQL(psCompanyId)
                + " AND b.sBranchNm LIKE " +  SQLUtil.toSQL("%"+psBranch+"%");
        
                if((psPayee == null || "".equals(psPayee)) || (psClient == null || "".equals(psClient))){
                    if(psPayee != null && !"".equals(psPayee)){
                        lsSQL = lsSQL + " AND IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%");
                    }
                    if(psClient != null && !"".equals(psClient)){
                        lsSQL = lsSQL + " AND IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%");
                    }
                } else {
                    lsSQL = lsSQL +  " AND ( IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
                                    + " OR IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%")
                                    + " ) ";
                }
//                + " AND ( IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psPayee+"%")
//                + " OR IFNULL(c.sPayeeNme,cc.sCompnyNm) LIKE  " +  SQLUtil.toSQL("%"+psClient+"%")
//                + " ) "
//                + "AND ( c.sPayeeNme LIKE  " +  SQLUtil.toSQL("%"+psPayee) + " OR c.sPayeeNme IS NULL ) "
//                + " GROUP BY a.sTransNox ";
                return lsSQL + " GROUP BY a.sTransNox ";
    }
    
    private String getInvTypeCategorySQL(){
        return     " SELECT "                                            
            + "   a.sInvTypCd "                                     
            + " , b.sCategrCd "                                     
            + " , a.sDescript AS sInvTypex "                        
            + " , b.sDescript AS sCategory "                        
            + " FROM Inv_Type a "                                   
            + " LEFT JOIN Category b ON b.sInvTypCd = a.sInvTypCd ";
    }
    
    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox,"
                + " a.sVouchrNo,"
                + " a.dTransact,"
                + " c.sBranchNm,"
                + " d.sPayeeNme," 
                + " IFNULL(e.sCompnyNm, IFNULL(d.sPayeeNme, '')) AS supplier,"
                + " f.sDescript,"
                + " a.nNetTotal, "
                + " a.cDisbrsTp, "
                + " a.cBankPrnt, "
                + " k.sDescript AS sIndustry, "
                + " g.sTransNox AS sCheckPay, "
                + " h.sTransNox AS sOtherPay "
                + " FROM Disbursement_Master a "
                + " LEFT JOIN Disbursement_Detail b ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd "
                + " LEFT JOIN Payee d ON a.sPayeeIDx = d.sPayeeIDx "
                + " LEFT JOIN Client_Master e ON d.sAPClntID = e.sClientID "
                + " LEFT JOIN Particular f ON b.sPrtclrID = f.sPrtclrID"
                + " LEFT JOIN Check_Payments g ON a.sTransNox = g.sSourceNo"
                + " LEFT JOIN Other_Payments h ON a.sTransNox = h.sSourceNo"
                + " LEFT JOIN Banks i ON (g.sBankIDxx = i.sBankIDxx AND ( a.cDisbrsTp = "+  SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK) + " OR a.cDisbrsTp = "+  SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT) + " ) "
                                 + ") OR (h.sBankIDxx = i.sBankIDxx AND ( a.cDisbrsTp != "+  SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK) + " AND a.cDisbrsTp != "+  SQLUtil.toSQL(DisbursementStatic.DisbursementType.CHECK_DEPOSIT) + " ) )"
                + " LEFT JOIN Bank_Account_Master j ON g.sBnkActID = j.sBnkActID OR h.sBnkActID = j.sBnkActID"
                + " LEFT JOIN Industry k ON k.sIndstCdx = a.sIndstCdx";
    }
    
    
    public JSONObject PrintBIR(List<String> fsTransactionNos){
        poJSON = new JSONObject();
        
        BIR2307Print loObject = new BIR2307Print();
        loObject.poGRider = poGRider;
        
        poJSON = loObject.initialize();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poJSON = loObject.openSource(fsTransactionNos);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

//        poJSON.put("result", "success");
//        poJSON.put("message", "BIR 2307 printed successfully");
        return poJSON;
    }
    
    /*****************************************************DV AND CHECK PRINTING*************************************************************/
    /**
     * 
     * @param fsTransactionNos disbursement transaction no
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException
     * @throws CloneNotSupportedException
     * @throws ScriptException 
     */
    public JSONObject PrintCheck(List<String> fsTransactionNos) throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        pbPrint = true;
        if (fsTransactionNos.isEmpty()) {
            poJSON.put("error", "No transactions selected.");
            return poJSON;
        }
        
        for (int lnCtr = 0; lnCtr < fsTransactionNos.size(); lnCtr++) {
            poJSON = OpenTransaction(fsTransactionNos.get(lnCtr));
            if ("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
            
            if (!CheckStatus.PrintStatus.PRINTED.equals(Master().CheckPayments().getPrint())) {
                poJSON = UpdateTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }

                System.out.println("CHECK TRansaction : " + CheckPayments().getModel().getTransactionNo());

                if (!Master().isPrinted()) {
                    poJSON.put("message", "Check printing requires the Disbursement to be printed first.");
                    poJSON.put("result", "error");
                    return poJSON;
                }

                if(Master().CheckPayments().getCheckNo() == null  || "".equals(Master().CheckPayments().getCheckNo())){
                    poJSON.put("message", "Check printing requires to assign Check No before printing");
                    poJSON.put("result", "error");
                    return poJSON;
                }

                CheckPayments().getModel().setPrint(CheckStatus.PrintStatus.PRINTED);
                CheckPayments().getModel().setProcessed(CheckStatus.PrintStatus.PRINTED);
                CheckPayments().getModel().setLocation(CheckStatus.PrintStatus.PRINTED);
                CheckPayments().getModel().setDatePrint(poGRider.getServerDate());
            }
            
            String bank = Master().CheckPayments().Banks().getBankCode();
            String transactionno = fsTransactionNos.get(lnCtr);
            String sPayeeNme = CheckPayments().getModel().Payee().getPayeeName();
            String dCheckDte = CustomCommonUtil.formatDateToMMDDYYYY(Master().CheckPayments().getCheckDate());
            String nAmountxx = removeComma(String.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().CheckPayments().getAmount(), false))); 
            String xAmountWords = NumberToWords.convertToWords(new BigDecimal(nAmountxx));
            
            if(dCheckDte == null || "".equals(dCheckDte)){
                poJSON.put("result", "error");
                poJSON.put("message", "Check Date is not set.");
                return poJSON;
            }
            
            if(dCheckDte.replace("-", "").length() < 8 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid CheckDate.");
                return poJSON;
            }
            
            String bankCode = getDocumentCode(CheckPayments().getModel().getBankAcountID()); //CheckPayments().getModel().Banks().getBankCode()+"Chk"+;
//            bankCode = "MBTDSChk";
            if(bankCode == null || "".equals(bankCode)){
                poJSON.put("result", "error");
                poJSON.put("message", "Please configure the document code for bank account.");
                return poJSON;
            }
            
//            bankCode = "MBTDSChk";
            System.out.println("===============================================");
            System.out.println("No : " + (lnCtr + 1));
            System.out.println("transactionNo No : " + fsTransactionNos.get(lnCtr));
            System.out.println("payeeName : " + sPayeeNme);
            System.out.println("checkDate : " + dCheckDte);
            System.out.println("amountNumeric : " + nAmountxx);
            System.out.println("amountWords : " + xAmountWords);
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
            Transaction transaction = new Transaction(transactionno, sPayeeNme, dCheckDte, nAmountxx, bankCode, new BigDecimal(nAmountxx));
            
            // Now print the voucher using PrinterJob
            if (showPrintPreview(transaction, poDocumentMapping)) {
                poJSON = PrintCheck(transaction,poDocumentMapping);
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
                
                //Save Disbursement only if check print is not yet printed
                if (!CheckStatus.PrintStatus.PRINTED.equals(Master().CheckPayments().getPrint())) {
                    pbIsUpdateAmountPaid = true;
                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                }
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Print check aborted.");
                return poJSON;
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "Check printed successfully");
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
     * @param
     * @return 
     */
    private String getDocumentCode(String fsBankAccountId){
        try {
//            String lsSQL =   " SELECT "                                                        
//                        + " CONCAT(a.sBankCode,c.sSlipType,'Chk') AS sDocCodex "            
//                        + " FROM Banks a "                                                  
//                        + " LEFT JOIN Bank_Account_Master b ON b.sBankIDxx = a.sBankIDxx "  
//                        + " LEFT JOIN Branch_Bank_Account c ON c.sBnkActID = b.sBnkActID "  ;

            //New query from ma'am she 04-11-2026 08:59 AM
            String lsSQL = " SELECT CONCAT(c.sBankCode,b.sSlipType,'Chk') AS sDocCodex " 
                            + " FROM Bank_Account_Master a " 
                            + "	LEFT JOIN Banks c ON a.sBankIDxx = c.sBankIDxx " 
                            + ", Branch_Bank_Account b ";
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sBnkActID = b.sBnkActID "
                                                    + " AND a.sBnkActID = " + SQLUtil.toSQL(fsBankAccountId)
                                                    + " AND b.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode()));
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
    
    private JSONObject PrintCheck(Transaction tx, DocumentMapping poDocumentMapping) throws SQLException, GuanzonException, CloneNotSupportedException {
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

        double pw = layout.getPrintableWidth();   // points
        double ph = layout.getPrintableHeight();

        Node voucherNode = buildVoucherNode(tx, pw, ph,poDocumentMapping);

        job.getJobSettings().setPageLayout(layout);
        job.getJobSettings().setJobName("Voucher-" + tx.transactionNo);

        boolean okay = job.printPage(layout, voucherNode);
        if (okay) {
            job.endJob();

            System.out.println("[SUCCESS] Printed transaction " + tx.transactionNo
                    + " for " + tx.sPayeeNme
                    + " | Amount: ₱" + tx.nAmountxx);
            poJSON.put("result", "success");
        } else {
            job.cancelJob();
            System.err.println("[FAILED] Printing failed for transaction " + tx.transactionNo);
            poJSON.put("result", "error");
            poJSON.put("message", "[FAILED] Printing failed for transaction " + tx.transactionNo);
            return poJSON;
        }
        
        return poJSON;
    }

    private boolean showPrintPreview(Transaction tx, DocumentMapping poDocumentMapping) throws SQLException, GuanzonException, CloneNotSupportedException {
        Printer printer = Printer.getDefaultPrinter();
        PageLayout layout = printer.createPageLayout(Paper.NA_LETTER,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);

        double pw = layout.getPrintableWidth();
        double ph = layout.getPrintableHeight();
        
        Node voucher = buildVoucherNode(tx, pw, ph,poDocumentMapping);

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
        stage.setTitle("Print Preview DV " + tx.transactionNo);
        stage.setScene(new Scene(new BorderPane(scroll, null, null, footer, null), 660, 820));
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
    private Node buildVoucherNode(Transaction tx,
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
        
        for (int i = 0; i < poDocumentMapping.Detail().size(); i++) {
            String fieldName = poDocumentMapping.Detail(i).getFieldCode();
            String fontName = poDocumentMapping.Detail(i).getFontName();
            double fontSize = poDocumentMapping.Detail(i).getFontSize();
            double topRow = poDocumentMapping.Detail(i).getTopRow();
            double leftCol = poDocumentMapping.Detail(i).getLeftColumn();
            double colSpace = poDocumentMapping.Detail(i).getColumnSpace();

            // Determine font per field
            Font fieldFont;
            switch (fieldName) {
                case "sPayeeNme":
                    fieldFont = Font.font(fontName, fontSize);
                    break;
                case "nAmountxx":
                    fieldFont = Font.font(fontName, fontSize);
                    break;
                case "dCheckDte":
                    fieldFont = Font.font(fontName, fontSize);
                    break;
                case "xAmountW":
                    fieldFont = Font.font(fontName, fontSize);
                    break;
                default:
                    fieldFont = Font.font(fontName, fontSize);
            }

            String textValue;
            switch (fieldName) {
                case "sPayeeNme":
                    textValue = tx.sPayeeNme == null ? "" : tx.sPayeeNme.toUpperCase();
                    break;
                case "nAmountxx":
                    textValue = CustomCommonUtil.setIntegerValueToDecimalFormat(tx.nAmountxx, false);
                    break;
                case "dCheckDte":
                    int spaceCount = (int) Math.round(colSpace);
                    if (spaceCount < 0) {
                        throw new IllegalArgumentException("spaceCount must be non-negative");
                    }
                    String gap = String.join("", Collections.nCopies(spaceCount, " "));
                    String rawDate = tx.dCheckDte == null ? "" : tx.dCheckDte.replace("-", "");
//                    textValue = rawDate
//                            .replaceAll("(.{2})(.{2})(.{4})", "$1 $2 $3")
//                            .replaceAll("", gap)
//                            .trim();
                    textValue = addSpaceBetweenChars(rawDate.substring(0,2),spaceCount) 
                            + repeatSpace(spaceCount+1) //Add extra 1 space for gap
                            + addSpaceBetweenChars(rawDate.substring(2,4),spaceCount)
                            + repeatSpace(spaceCount+1) //Add extra 1 space for gap
                            + addSpaceBetweenChars(rawDate.substring(4,8),spaceCount) ;
                    break;
                case "xAmountW":
                    textValue = NumberToWords.convertToWords(new BigDecimal(tx.nAmountxx));
                    break;
                default:
                    throw new AssertionError("Unhandled field: " + fieldName);
            }

            double x = leftCol * CHAR_WIDTH;
            double y = TOP_MARGIN + topRow * LINE_HEIGHT;
            
            Text textNode = new Text(x, y, textValue == null ? "" : textValue);
            textNode.setFont(fieldFont);
            root.getChildren().add(textNode);
        }

        return root;
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

        final String transactionNo, sPayeeNme, dCheckDte, nAmountxx, bankCode;
        final BigDecimal nAmountxxValue;

        Transaction(String transactionNo, String sPayeeNme, String dCheckDte, String nAmountxx, String bankCode, BigDecimal nAmountxxValue) {
            this.transactionNo = transactionNo;
            this.sPayeeNme = sPayeeNme;
            this.dCheckDte = dCheckDte;
            this.nAmountxx = nAmountxx;
            this.bankCode = bankCode;
            this.nAmountxxValue = nAmountxxValue;
        }
    }
    
    public String getReferenceNo(int fnRow){
        try {
            switch(Detail(fnRow).getSourceCode()){
                case DisbursementStatic.SourceCode.PO_RECEIVING:
                    return Detail(fnRow).POReceiving().getReferenceNo();
                case DisbursementStatic.SourceCode.PO_RETURN:
                    return Detail(fnRow).POReturn().PurchaseOrderReceivingMaster().getReferenceNo();
                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                    return Detail(fnRow).PRF().getSeriesNo();
            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                return Detail(fnRow).APAdjustment().getReferenceNo();
            case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                return Detail(fnRow).SOAMaster().getSOANumber();
//                switch(Detail(fnRow).SOADetail().getSourceCode()){
//                    case SOATaggingStatic.POReceiving:
//                        return Detail(fnRow).POReceiving().getReferenceNo();
//                    case SOATaggingStatic.PaymentRequest:
//                        return Detail(fnRow).PRF().getSeriesNo();
//                    case SOATaggingStatic.APPaymentAdjustment:
//                        return Detail(fnRow).APAdjustment().getReferenceNo();
//                }
//                break;
            }
            
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    
        return "";
    }
    
    public String getSourceNo(int fnRow){
        try {
            switch(Detail(fnRow).getSourceCode()){
            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                return Detail(fnRow).PRF().getSourceNo();
            default:
                return "";
            case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                switch(Detail(fnRow).SOADetail().getSourceCode()){
                    case SOATaggingStatic.PaymentRequest:
                        return Detail(fnRow).PRF().getSourceNo();
                    default:
                        return "";
                }
            }
            
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    
        return "";
    }
    
    /**
     * DV Printing Detail Source
     * @param fsSouceCode
     * @return 
     */
    private String particular(String fsSouceCode){
        switch(fsSouceCode){
            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                return "PAYMENT REQUEST";
            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                return "ADJUSTMENT";
            case DisbursementStatic.SourceCode.PO_RECEIVING:
            default:
                return "PURCHASES";
        }
    }
    
    public JSONObject getUpdateStatusBy(String fsStatus) throws SQLException, GuanzonException {
        String lsUpdateBy = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM "+Master().getTable()+" a "
                     + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = "+ SQLUtil.toSQL(Master().getTable())
                     + " AND b.cRefrStat = "+ SQLUtil.toSQL(fsStatus) ;
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL STATUS : "+fsStatus+" : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsUpdateBy = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsUpdateBy = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsDate =  dModified.format(formatter);
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
        poJSON.put("sUpdateByx", lsUpdateBy);
        poJSON.put("sUpdateDte", lsDate);
        return poJSON;
    }

    public JSONObject printTransaction(List<String> fsTransactionNos)
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        JasperPrint masterPrint = null;       
        JasperReport jasperReport = null;      
        pbShowed = false; 
        pbIsPrinted = false;
        pbPrint = true;
        
        for (String txnNo : fsTransactionNos) {
            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
            loObject.openRecord(txnNo);
            if ("error".equals((String) poJSON.get("result"))){
                ShowMessageFX.Warning(null, "Computerized Accounting System",(String) poJSON.get("message"));
                return poJSON;
            }
                
            if(loObject.CheckPayments().getCheckNo() == null || "".equals(loObject.CheckPayments().getCheckNo())){
                poJSON.put("result", "error");
                poJSON.put("message", "Check number must be assigned before printing the disbursement no. "+loObject.getVoucherNo()+".");
                ShowMessageFX.Warning(null, "Computerized Accounting System",(String) poJSON.get("message"));
                return poJSON;
            }
        }
        
        try {
            String watermarkPath = "";
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/DisbursementVoucher.jrxml";//"D:\\GGC_Maven_Systems\\Reports\\CheckDisbursementVoucher.jrxml";
            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            for (String txnNo : fsTransactionNos) {
                watermarkPath = System.getProperty("sys.default.path.config") + "/Reports/images/"; // "D:\\GGC_Maven_Systems\\Reports\\images\\none.png"; 
                poJSON = OpenTransaction(txnNo);
                if ("error".equals((String) poJSON.get("result"))){
                    proceedAfterViewerClosed();
                    return poJSON;
                }
                
//                if(Master().CheckPayments().getCheckNo() == null || "".equals(Master().CheckPayments().getCheckNo())){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "Check number must be assigned before printing the disbursement no. "+Master().getVoucherNo()+".");
//                    proceedAfterViewerClosed();
//                    return poJSON;
//                }
                //Update the transaction only if it is not yet printed
                if(!Master().isPrinted()){
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))){
                        proceedAfterViewerClosed();
                        return poJSON;
                    }
                }
                
                Map<String, Object> params = new HashMap<>();
                System.out.println("voucher No : " + Master().getVoucherNo());
                System.out.println("transaction No : " + Master().getTransactionNo());
                System.out.println("payee : " + Master().CheckPayments().Payee().getPayeeName());
                params.put("voucherNo", Master().getVoucherNo());
                params.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
                params.put("sRemarks", Master().getRemarks());
                params.put("sPayeeNme", Master().CheckPayments().Payee().getPayeeName());
                params.put("sBankName", Master().CheckPayments().Banks().getBankName());
                params.put("sCheckNox", Master().CheckPayments().getCheckNo());
                params.put("dCheckDte", Master().CheckPayments().getCheckDate());
                params.put("nCheckAmountxx", Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().CheckPayments().getAmount(), false).replace(",", "")));
                
                   //Additional fields - Arsiela 06222026
                params.put("sCompany", Master().Company().getCompanyName());
                params.put("sBranch", Master().Branch().getBranchName());
                params.put("sBankAccountNo", Master().CheckPayments().Bank_Account_Master().getAccountNo());
//                params.put("sReceivedBy","");
                
                //Set Default value to empty to prevent null in display
                params.put("sEncoder","");
                params.put("sConfirmer","");
                params.put("sVerifier","");
                params.put("sApprover","");
                params.put("sCertifier","");
                params.put("sAuthorizer","");
                
                //Get Encoder
                JSONObject loJSONEntry = getEntryBy();
                if("error".equals((String) loJSONEntry.get("result"))){
                    return loJSONEntry;
                }
                if((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))){
                    params.put("sEncoder",(String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte"))); 
                }
                //Get Confirmer
                JSONObject loJSONConfirmer = getUpdateStatusBy(DisbursementStatic.CONFIRMED);
                if("error".equals((String) loJSONConfirmer.get("result"))){
                    return loJSONConfirmer;
                } else {
                    if((String) loJSONConfirmer.get("sUpdateByx") != null && !"".equals((String) loJSONConfirmer.get("sUpdateByx"))){
                        params.put("sConfirmer", (String) loJSONConfirmer.get("sUpdateByx") + " " + String.valueOf((String) loJSONConfirmer.get("sUpdateDte"))); 
                    }
                }
                //Get Verifiers
                JSONObject loJSONVerified = getUpdateStatusBy(DisbursementStatic.VERIFIED);
                if("error".equals((String) loJSONVerified.get("result"))){
                    return loJSONVerified;
                } else {
                    if((String) loJSONVerified.get("sUpdateByx") != null && !"".equals((String) loJSONVerified.get("sUpdateByx"))){
                        params.put("sVerifier", (String) loJSONVerified.get("sUpdateByx") + " " + String.valueOf((String) loJSONVerified.get("sUpdateDte"))); 
                    }
                }
                //Get Approver
                JSONObject loJSONApprover = getUpdateStatusBy(DisbursementStatic.APPROVED);
                if("error".equals((String) loJSONApprover.get("result"))){
                    return loJSONApprover;
                } else {
                    if((String) loJSONApprover.get("sUpdateByx") != null && !"".equals((String) loJSONApprover.get("sUpdateByx"))){
                        params.put("sApprover", (String) loJSONApprover.get("sUpdateByx") + " " + String.valueOf((String) loJSONApprover.get("sUpdateDte"))); 
                    }
                }
                //Get Certifier
                JSONObject loJSONCertified = getUpdateStatusBy(DisbursementStatic.CERTIFIED);
                if("error".equals((String) loJSONCertified.get("result"))){
                    return loJSONCertified;
                } else {
                    if((String) loJSONCertified.get("sUpdateByx") != null && !"".equals((String) loJSONCertified.get("sUpdateByx"))){
                        params.put("sCertifier", (String) loJSONCertified.get("sUpdateByx") + " " + String.valueOf((String) loJSONCertified.get("sUpdateDte"))); 
                    }
                }
                //Get Authorizer
                JSONObject loJSONAuthorize = getUpdateStatusBy(DisbursementStatic.AUTHORIZED);
                if("error".equals((String) loJSONAuthorize.get("result"))){
                    return loJSONAuthorize;
                } else {
                    if((String) loJSONAuthorize.get("sUpdateByx") != null && !"".equals((String) loJSONAuthorize.get("sUpdateByx"))){
                        params.put("sAuthorizer", (String) loJSONAuthorize.get("sUpdateByx") + " " + String.valueOf((String) loJSONAuthorize.get("sUpdateDte"))); 
                    }
                }
                
//                params.put("sPrepared", poGRider.getLogName());
//                params.put("sChecked", "Rex Adversalo");
//                params.put("sApproved", "Guanson Lo");
                
                if(Master().isPrinted()){
                    watermarkPath = watermarkPath + "reprint.png"; //"D:\\GGC_Maven_Systems\\Reports\\images\\reprint.png";
                } else {
                    watermarkPath = watermarkPath + "none.png" ; //"D:\\GGC_Maven_Systems\\Reports\\images\\none.png";
                }
                params.put("watermarkImagePath", watermarkPath);
                List<TransactionDetail> Details = new ArrayList<>();
                List<String> laParticular = new ArrayList<>();
                String lsParticular = "";
                for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
                    if(Detail(lnCtr).getAmountApplied() != 0.0000){
                        //populate particular
                        switch(Detail(lnCtr).getSourceCode()){
                            case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                            case DisbursementStatic.SourceCode.PO_RECEIVING:
                            case DisbursementStatic.SourceCode.PO_RETURN:
                            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                                lsParticular = particular(Detail(lnCtr).getSourceCode());
                                if(!laParticular.contains(lsParticular)){
                                    laParticular.add(lsParticular);
                                }
                                break;
                            case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                                lsParticular = particular(Detail(lnCtr).SOADetail().getSourceCode());
                                if(!laParticular.contains(lsParticular)){
                                    laParticular.add(lsParticular);
                                }
                                break;
                        }
                    }
                }
                
                //Particular
                lsParticular = ""; //Reset value
                for(int lnCtr = 0;lnCtr <= laParticular.size()-1;lnCtr++){
                    if(lsParticular.isEmpty()){
                        lsParticular = laParticular.get(lnCtr);
                    } else {
                        lsParticular = lsParticular + " AND " + laParticular.get(lnCtr);
                    }
                }
                
                Details.add(new TransactionDetail(
                        1,
                        lsParticular,
                        "",
                        Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getNetTotal(), false).replace(",", ""))
                ));
                
                JasperPrint currentPrint = JasperFillManager.fillReport(
                        jasperReport,
                        params,
                        new JRBeanCollectionDataSource(Details)
                );
                if (fsTransactionNos.size() == 1) {
                    masterPrint = currentPrint;
                } else {
                    showViewerAndWait(currentPrint);
                }
            }

            if (masterPrint != null) {
                CustomJasperViewer viewer = new CustomJasperViewer(masterPrint);
                viewer.setVisible(true);
                viewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        proceedAfterViewerClosed();
                    }

                });
            }
            
        } catch (JRException | SQLException | GuanzonException | ScriptException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } 

        return poJSON;
    }
    
    private boolean pbShowed = false;
    private void proceedAfterViewerClosed() {
        Platform.runLater(() -> {
            System.out.println("SHOWED!!!!!!!!!!!");

            if (pbShowed) {
                return;
            } else {
                pbShowed = true;
            }

            if ("error".equals((String) poJSON.get("result"))) {
                ShowMessageFX.Warning(null, "Computerized Accounting System",
                    (String) poJSON.get("message"));
            } else {
                if (pbIsPrinted) {
                    ShowMessageFX.Information(null, "Computerized Accounting System",
                        "Transaction Printed Successfully");
                } else {
                    ShowMessageFX.Warning(null, "Computerized Accounting System",
                        "Printing was canceled by the user.");
                }
            }
        });
    }

    private void showViewerAndWait(JasperPrint print) {
        // create viewer on Swing thread
        final CustomJasperViewer viewer = new CustomJasperViewer(print);
        viewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final CountDownLatch latch = new CountDownLatch(1);
        viewer.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                latch.countDown();             // release when user closes
                proceedAfterViewerClosed();
            }
        });

        javax.swing.SwingUtilities.invokeLater(() -> viewer.setVisible(true));

        try {
            latch.await();                     // 🔴 blocks here until viewer closes
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // keep interruption status
        }
    }

    public static class TransactionDetail {

        private final Integer nRowNo;
        private final String sParticular;
        private final String sRemarks;
        private final Double nTotalAmount;

        public TransactionDetail(Integer rowNo, String particular, String remarks, Double totalAmt) {
            this.nRowNo = rowNo;
            this.sParticular = particular;
            this.sRemarks = remarks;
            this.nTotalAmount = totalAmt;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsParticular() {
            return sParticular;
        }

        public String getsRemarks() {
            return sRemarks;
        }

        public Double getnTotalAmount() {
            return nTotalAmount;
        }
    }
    
    public static class TransactionPaymentSummaryDetail {

        private final Integer nRowNo;
        private final String sParticular;
        private final String sDepartment;
        private final String sReferNo;
        private final String sSourceNo;
        private final String sSourceCode;
        private Double nTotalAmount;

        public TransactionPaymentSummaryDetail(Integer rowNo, String particular, String department, String referNo, String sourceNo,String sourceCode, Double totalAmt) {
            this.nRowNo = rowNo;
            this.sParticular = particular;
            this.sDepartment = department;
            this.sReferNo = referNo;
            this.sSourceNo = sourceNo;
            this.sSourceCode = sourceCode;
            this.nTotalAmount = totalAmt;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }
        
        public String getsDepartment() {
            return sDepartment;
        }

        public String getsParticular() {
            return sParticular;
        }

        public String getsSourceNo() {
            return sSourceNo;
        }
        
        public String getsSourceCode() {
            return sSourceCode;
        }

        public String getsReferNo() {
            return sReferNo;
        }

        public Double getnTotalAmount() {
            return nTotalAmount;
        }
        
        public void setnTotalAmount(Double fdblAmount) {
            nTotalAmount = fdblAmount;
        }
    }
    
    private boolean pbIsPrinted = false;
    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(final JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        /* ---- toolbar patch ------------------------------------------ */
        private JSONObject customizePrintButton(final JasperPrint jasperPrint) {

            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "JRViewer not found!");
                    return poJSON;
                }
                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {

                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {

                                final JButton button = (JButton) toolbar.getComponent(j);

                                if ("Print".equals(button.getToolTipText())) {
                                    /* remove existing handlers */
                                    ActionListener[] old = button.getActionListeners();
                                    for (int k = 0; k < old.length; k++) {
                                        button.removeActionListener(old[k]);
                                    }

                                    /* add our own (anonymous inner‑class, not lambda) */
                                    button.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                pbIsPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                                if (pbIsPrinted) {
                                                    //Update the transaction only if it is not yet printed
                                                    if(!Master().isPrinted()){
                                                        Master().isPrinted(true);
                                                        Master().setDatePrint(poGRider.getServerDate());

                                                        Master().setModifyingId(poGRider.getUserID());
                                                        Master().setModifiedDate(poGRider.getServerDate());
                                                        poJSON = SaveTransaction();
                                                        if(!"error".equals((String) poJSON.get("result"))){
                                                            poJSON.put("result", "success");
                                                            poJSON.put("message",  "Transaction Printed Successfully.");
                                                        }
                                                    }
                                                    CustomJasperViewer.this.dispose();
                                                } else {
                                                    poJSON.put("result", "error");
                                                    poJSON.put("message",  "Printing was canceled by the user.");
                                                }
                                            } catch (SQLException | GuanzonException | CloneNotSupportedException | JRException ex) {
                                                poJSON.put("result", "error");
                                                poJSON.put("message",  "Print Failed: " + ex.getMessage());
                                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    });
                                } else {
                                    // Disable all other buttons
                                    button.setEnabled(false);
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message",  "Transaction print aborted!");
                                }
                            }
                        }
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
                poJSON.put("result", "error");
                poJSON.put("message", "Error customizing print button: " + e.getMessage());
            }
            
            return poJSON;
        }

        private void PrintTransaction(boolean fbIsPrinted)
                throws SQLException, CloneNotSupportedException, GuanzonException {
            final String msg = fbIsPrinted
                    ? "Transaction printed successfully."
                    : "Transaction print aborted.";

            Platform.runLater(() -> {
                if(fbIsPrinted){
                    ShowMessageFX.Information(null, "Computerized Accounting System", msg);
                } else {
                    ShowMessageFX.Warning(null, "Computerized Accounting System", msg);
                }
                SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
            });
        }

        private void warnAndRefocus(final String m) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
//                    ShowMessageFX.Warning(m, "Print Disbursement", null);
                    poJSON.put("result", "error");
                    poJSON.put("message", m);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            CustomJasperViewer.this.toFront();
                        }
                    });
                }
            });
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] comps = ((Container) parent).getComponents();
                for (int i = 0; i < comps.length; i++) {
                    JRViewer v = findJRViewer(comps[i]);
                    if (v != null) {
                        return v;
                    }
                }
            }
            return null;
        }
    }
    
    public JSONObject printTransactionPaymentSummary(List<String> fsTransactionNos)
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        JasperPrint masterPrint = null;       
        JasperReport jasperReport = null;      
        pbShowed = false; 
        pbIsPrinted = false;
        pbPrint = true;
        
        for (String txnNo : fsTransactionNos) {
            Model_Disbursement_Master loObject = new CashflowModels(poGRider).DisbursementMaster();
            loObject.openRecord(txnNo);
            if ("error".equals((String) poJSON.get("result"))){
                ShowMessageFX.Warning(null, "Computerized Accounting System",(String) poJSON.get("message"));
                return poJSON;
            }
                
            if(loObject.CheckPayments().getCheckNo() == null || "".equals(loObject.CheckPayments().getCheckNo())){
                poJSON.put("result", "error");
                poJSON.put("message", "Check number must be assigned before printing the disbursement no. "+loObject.getVoucherNo()+".");
                ShowMessageFX.Warning(null, "Computerized Accounting System",(String) poJSON.get("message"));
                return poJSON;
            }
        }
        
        try {
            String watermarkPath = "";
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/DisbursementVoucherPaymentSummary.jrxml";
            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            for (String txnNo : fsTransactionNos) {
                watermarkPath = System.getProperty("sys.default.path.config") + "/Reports/images/"; 
                poJSON = OpenTransaction(txnNo);
                if ("error".equals((String) poJSON.get("result"))){
                    proceedAfterViewerClosed();
                    return poJSON;
                }
                //Update the transaction only if it is not yet printed
                if(!Master().isPrinted()){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Disbursement voucher must be print before printing the disbursement no. "+Master().getVoucherNo()+" payment summary.");
                    ShowMessageFX.Warning(null, "Computerized Accounting System",(String) poJSON.get("message"));
                    return poJSON;
                }
                
                Map<String, Object> params = new HashMap<>();
                System.out.println("voucher No : " + Master().getVoucherNo());
                System.out.println("transaction No : " + Master().getTransactionNo());
                System.out.println("payee : " + Master().CheckPayments().Payee().getPayeeName());
                params.put("voucherNo", Master().getVoucherNo());
                params.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
                params.put("sRemarks", Master().getRemarks());
                params.put("sPayeeNme", Master().CheckPayments().Payee().getPayeeName());
                params.put("sBankName", Master().CheckPayments().Banks().getBankName());
                params.put("sCheckNox", Master().CheckPayments().getCheckNo());
                params.put("dCheckDte", Master().CheckPayments().getCheckDate());
                params.put("nCheckAmountxx", Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().CheckPayments().getAmount(), false).replace(",", "")));
                
                //Additional fields - Arsiela 06222026
                params.put("sCompany", Master().Company().getCompanyName());
                params.put("sBranch", Master().Branch().getBranchName());
                params.put("sBankAccountNo", Master().CheckPayments().Bank_Account_Master().getAccountNo());
//                params.put("sReceivedBy","");
                
                //Set Default value to empty to prevent null in display
                params.put("sEncoder","");
                params.put("sConfirmer","");
                params.put("sVerifier","");
                params.put("sApprover","");
                params.put("sCertifier","");
                params.put("sAuthorizer","");
                
                //Get Encoder
                JSONObject loJSONEntry = getEntryBy();
                if("error".equals((String) loJSONEntry.get("result"))){
                    return loJSONEntry;
                }
                if((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))){
                    params.put("sEncoder",(String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte"))); 
                }
                //Get Confirmer
                JSONObject loJSONConfirmer = getUpdateStatusBy(DisbursementStatic.CONFIRMED);
                if("error".equals((String) loJSONConfirmer.get("result"))){
                    return loJSONConfirmer;
                } else {
                    if((String) loJSONConfirmer.get("sUpdateByx") != null && !"".equals((String) loJSONConfirmer.get("sUpdateByx"))){
                        params.put("sConfirmer", (String) loJSONConfirmer.get("sUpdateByx") + " " + String.valueOf((String) loJSONConfirmer.get("sUpdateDte"))); 
                    }
                }
                //Get Verifiers
                JSONObject loJSONVerified = getUpdateStatusBy(DisbursementStatic.VERIFIED);
                if("error".equals((String) loJSONVerified.get("result"))){
                    return loJSONVerified;
                } else {
                    if((String) loJSONVerified.get("sUpdateByx") != null && !"".equals((String) loJSONVerified.get("sUpdateByx"))){
                        params.put("sVerifier", (String) loJSONVerified.get("sUpdateByx") + " " + String.valueOf((String) loJSONVerified.get("sUpdateDte"))); 
                    }
                }
                //Get Approver
                JSONObject loJSONApprover = getUpdateStatusBy(DisbursementStatic.APPROVED);
                if("error".equals((String) loJSONApprover.get("result"))){
                    return loJSONApprover;
                } else {
                    if((String) loJSONApprover.get("sUpdateByx") != null && !"".equals((String) loJSONApprover.get("sUpdateByx"))){
                        params.put("sApprover", (String) loJSONApprover.get("sUpdateByx") + " " + String.valueOf((String) loJSONApprover.get("sUpdateDte"))); 
                    }
                }
                //Get Certifier
                JSONObject loJSONCertified = getUpdateStatusBy(DisbursementStatic.CERTIFIED);
                if("error".equals((String) loJSONCertified.get("result"))){
                    return loJSONCertified;
                } else {
                    if((String) loJSONCertified.get("sUpdateByx") != null && !"".equals((String) loJSONCertified.get("sUpdateByx"))){
                        params.put("sCertifier", (String) loJSONCertified.get("sUpdateByx") + " " + String.valueOf((String) loJSONCertified.get("sUpdateDte"))); 
                    }
                }
                //Get Authorizer
                JSONObject loJSONAuthorize = getUpdateStatusBy(DisbursementStatic.AUTHORIZED);
                if("error".equals((String) loJSONAuthorize.get("result"))){
                    return loJSONAuthorize;
                } else {
                    if((String) loJSONAuthorize.get("sUpdateByx") != null && !"".equals((String) loJSONAuthorize.get("sUpdateByx"))){
                        params.put("sAuthorizer", (String) loJSONAuthorize.get("sUpdateByx") + " " + String.valueOf((String) loJSONAuthorize.get("sUpdateDte"))); 
                    }
                }
                
                params.put("dblAdvances",Master().getAdvancesTotal());
                watermarkPath = watermarkPath + "none.png" ; //"D:\\GGC_Maven_Systems\\Reports\\images\\none.png";
                params.put("watermarkImagePath", watermarkPath);

                double ldblTotalDiscount = 0.0000;
                double ldblDiscountRate = 0.0000;
                List<String> laSOA = new ArrayList<>();
                List<String> laSOASCode = new ArrayList<>();
                List<TransactionPaymentSummaryDetail> Details = new ArrayList<>();
                for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
                    switch(Detail(lnCtr).getSourceCode()){
                        case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                            ldblTotalDiscount += Detail(lnCtr).PRF().getDiscountAmount();
                            poJSON = getPRFDetail(Detail(lnCtr).PRF(),Details);
                            if(!isJSONSuccess(poJSON)){
                                return poJSON;
                            }
                            break;
                        case DisbursementStatic.SourceCode.PO_RECEIVING:
                            ldblDiscountRate = Detail(lnCtr).POReceiving().getDiscountRate().doubleValue();
                            if(ldblDiscountRate > 0.0000){
                                ldblDiscountRate = Detail(lnCtr).POReceiving().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
                            }
                            ldblTotalDiscount += (Detail(lnCtr).POReceiving().getDiscount().doubleValue() + ldblDiscountRate);
                            poJSON = getPOReceivingDetail(Detail(lnCtr).POReceiving(),Details);
                            if(!isJSONSuccess(poJSON)){
                                return poJSON;
                            }
                            break;
                        case DisbursementStatic.SourceCode.PO_RETURN:
//                            ldblDiscountRate = Detail(lnCtr).POReturn().getDiscountRate().doubleValue();
//                            if(ldblDiscountRate > 0.0000){
//                                ldblDiscountRate = Detail(lnCtr).POReturn().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
//                            }
//                            ldblTotalDiscount += (Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate);
                            
//                            if((Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate) > ldblTotalDiscount){
//                                ldblTotalDiscount = 0.0000;
//                            } else {
//                                ldblTotalDiscount = ldblTotalDiscount - (Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate);
//                            }
                            
                            poJSON = getPOReturnDetail(Detail(lnCtr).POReturn(),Details);
                            if(!isJSONSuccess(poJSON)){
                                return poJSON;
                            }
                            break;
                        case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                            poJSON = getAPAdjustment(Detail(lnCtr).APAdjustment(),Details);
                            if(!isJSONSuccess(poJSON)){
                                return poJSON;
                            }
                            break;
                        case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                            if(!laSOA.contains(Detail(lnCtr).getSourceNo()) && !laSOASCode.contains(Detail(lnCtr).getSourceCode())){
                                ldblTotalDiscount += Detail(lnCtr).SOAMaster().getDiscountAmount().doubleValue();
                            }
                            laSOA.add(Detail(lnCtr).getSourceNo());
                            laSOASCode.add(Detail(lnCtr).getSourceCode());
                            switch(Detail(lnCtr).SOADetail().getSourceCode()){
                                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                                    ldblTotalDiscount += Detail(lnCtr).PRF().getDiscountAmount();
                                    poJSON = getPRFDetail(Detail(lnCtr).SOADetail().PaymentRequestMaster(),Details);
                                    if(!isJSONSuccess(poJSON)){
                                        return poJSON;
                                    }
                                break;
                                case DisbursementStatic.SourceCode.PO_RECEIVING:
                                    ldblDiscountRate = Detail(lnCtr).POReceiving().getDiscountRate().doubleValue();
                                    if(ldblDiscountRate > 0.0000){
                                        ldblDiscountRate = Detail(lnCtr).POReceiving().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
                                    }
                                    ldblTotalDiscount += (Detail(lnCtr).POReceiving().getDiscount().doubleValue() + ldblDiscountRate);
                                    poJSON = getPOReceivingDetail(Detail(lnCtr).SOADetail().PurchasOrderReceivingMaster(),Details);
                                    if(!isJSONSuccess(poJSON)){
                                        return poJSON;
                                    }
                                break;
                                case DisbursementStatic.SourceCode.PO_RETURN:
//                                    ldblDiscountRate = Detail(lnCtr).POReturn().getDiscountRate().doubleValue();
//                                    if(ldblDiscountRate > 0.0000){
//                                        ldblDiscountRate = Detail(lnCtr).POReturn().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
//                                    }
//                                    ldblTotalDiscount += (Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate);
//                                    if((Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate) > ldblTotalDiscount){
//                                        ldblTotalDiscount = 0.0000;
//                                    } else {
//                                        ldblTotalDiscount = ldblTotalDiscount - (Detail(lnCtr).POReturn().getDiscount().doubleValue() + ldblDiscountRate);
//                                    }
                                    poJSON = getPOReturnDetail(Detail(lnCtr).SOADetail().PurchasOrderReturnMaster(),Details);
                                    if(!isJSONSuccess(poJSON)){
                                        return poJSON;
                                    }
                                break;
                                case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                                    poJSON = getAPAdjustment(Detail(lnCtr).SOADetail().APPaymentAdjustmentMaster(),Details);
                                    if(!isJSONSuccess(poJSON)){
                                        return poJSON;
                                    }
                                break;
                            }
                            break;
                    }
                }
                
                double ldblTotalDetailAmount = 0.0000;
                for(int lnCtr = 0;lnCtr < Details.size();lnCtr++){
                    ldblTotalDetailAmount += Details.get(lnCtr).getnTotalAmount();
                }
                
                params.put("nDiscount",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblTotalDiscount, false).replace(",", "")));
                params.put("nTtlAmntx",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(ldblTotalDetailAmount, false).replace(",", "")));
                params.put("nAdvances",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getAdvancesTotal(), false).replace(",", "")));
                params.put("nVatableSales",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVATSale(), false).replace(",", "")));
                params.put("nVatAmount",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVATAmount(), false).replace(",", "")));
                params.put("nVatZeroSales",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getZeroVATSales(), false).replace(",", "")));
                params.put("nVatExemptSale",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getVATExmpt(), false).replace(",", "")));
                params.put("nLessWHTax",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getWithTaxTotal(), false).replace(",", "")));
                params.put("nNetAmntx",Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(Master().getNetTotal(), false).replace(",", "")));
                
                JasperPrint currentPrint = JasperFillManager.fillReport(
                        jasperReport,
                        params,
                        new JRBeanCollectionDataSource(Details)
                );
                if (fsTransactionNos.size() == 1) {
                    masterPrint = currentPrint;
                } else {
                    showViewerAndWait(currentPrint);
                }
            }

            if (masterPrint != null) {
                CustomJasperViewer viewer = new CustomJasperViewer(masterPrint);
                viewer.setVisible(true);
                viewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        proceedAfterViewerClosed();
                    }

                });
            }
            
        } catch (JRException | SQLException | GuanzonException | ScriptException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } 

        return poJSON;
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
    
    private JSONObject getPRFDetail(Model_Payment_Request_Master foObject,  List<TransactionPaymentSummaryDetail> Details ) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
//        double ldblMasterDiscount = foObject.getDiscountAmount();
        double lnAmount = 0.0000;
        
        for(int lnCtr = 0; lnCtr < foObject.getEntryNo();lnCtr++){
            Model_Payment_Request_Detail loObject = new CashflowModels(poGRider).PaymentRequestDetail();
            poJSON = loObject.openRecord(foObject.getTransactionNo(), lnCtr+1);
            if(!isJSONSuccess(poJSON)){
                return poJSON;
            }
            if(!loObject.isReverse()){
                continue;
            }
            
            lnAmount = loObject.getAmount();
//            if(ldblMasterDiscount > 0.0000){
//                if(ldblMasterDiscount > lnAmount){
//                    ldblMasterDiscount = ldblMasterDiscount - lnAmount;
//                    lnAmount = 0.0000;
//                } else {
//                    lnAmount = lnAmount - ldblMasterDiscount;
//                    ldblMasterDiscount = 0.0000;
//                }
//            }
            boolean lbExist = false;
            for(int lnRow = 0;lnRow < Details.size();lnRow++){
                if(Details.get(lnRow).getsSourceCode().equals(DisbursementStatic.SourceCode.PAYMENT_REQUEST)){
                    if(Details.get(lnRow).getsSourceNo().equals(foObject.getTransactionNo())
                       && Details.get(lnRow).getsParticular().equals(loObject.Particular().getDescription())){
                        Details.get(lnRow).setnTotalAmount(Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", "")));
                        lbExist = true;
                    }
                }
            }
            
            if(!lbExist){
                Details.add(new TransactionPaymentSummaryDetail(
                        Details.size()+1,
                        loObject.Particular().getDescription(),
                        foObject.Department().getDescription(),
                        foObject.getSeriesNo(),
                        foObject.getTransactionNo(),
                        DisbursementStatic.SourceCode.PAYMENT_REQUEST,
                        Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", ""))
                ));
            }
        }
    
        return poJSON;
    }
    
    private JSONObject getPOReceivingDetail(Model_POR_Master foObject,  List<TransactionPaymentSummaryDetail> Details ) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        double lnAmount = 0.0000;
        String lsInvoiceNo = foObject.getSalesInvoice();
        if(lsInvoiceNo == null || "".equals(lsInvoiceNo) || "To-follow".equals(lsInvoiceNo)){
            lsInvoiceNo = foObject.getReferenceNo();
        }
        
        for(int lnCtr = 0; lnCtr < foObject.getEntryNo();lnCtr++){
            Model_POR_Detail loObject = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
            poJSON = loObject.openRecord(foObject.getTransactionNo(), lnCtr+1);
            if(!isJSONSuccess(poJSON)){
                return poJSON;
            }
            if(!loObject.isReverse()){
                continue;
            }
            String lsCategory = loObject.Inventory().CategoryLevel2().getDescription();
            if(lsCategory == null || "".equals(lsCategory)){
                lsCategory = loObject.Inventory().Category().getDescription();
            }
            lnAmount = loObject.getUnitPrce().doubleValue() * loObject.getQuantity().doubleValue();
            boolean lbExist = false;
            for(int lnRow = 0;lnRow < Details.size();lnRow++){
                if(Details.get(lnRow).getsSourceCode().equals(DisbursementStatic.SourceCode.PO_RECEIVING)){
                    if(Details.get(lnRow).getsSourceNo().equals(foObject.getTransactionNo())
                       && Details.get(lnRow).getsParticular().equals(lsCategory)){
                        lnAmount = lnAmount + Details.get(lnRow).getnTotalAmount();
                        Details.get(lnRow).setnTotalAmount(Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", "")));
                        lbExist = true;
                    }
                }
            }
            if(!lbExist){
                Details.add(new TransactionPaymentSummaryDetail(
                        Details.size()+1,
                        lsCategory,
                        foObject.Department().getDescription(),
                        lsInvoiceNo,
                        foObject.getTransactionNo(),
                        DisbursementStatic.SourceCode.PO_RECEIVING,
                        Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", ""))
                ));
            }
        }
        
        //Check if freight amount is exist
        if(foObject.getFreight().doubleValue() > 0.0000 && (foObject.getTruckingId() == null || "".equals(foObject.getTruckingId()) || foObject.getSupplierId().equals(foObject.getTruckingId()))){
            Details.add(new TransactionPaymentSummaryDetail(
                    Details.size()+1,
                    "Freight",
                    foObject.Department().getDescription(),
                    lsInvoiceNo,
                    foObject.getTransactionNo(),
                    DisbursementStatic.SourceCode.PO_RECEIVING,
                    Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat( foObject.getFreight().doubleValue(), false).replace(",", ""))
            ));
        }
        
        return poJSON;
    }
    
    private JSONObject getPOReturnDetail(Model_POReturn_Master foObject,  List<TransactionPaymentSummaryDetail> Details ) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        double lnAmount = 0.0000;
        String lsInvoiceNo = foObject.PurchaseOrderReceivingMaster().getSalesInvoice();
        if(lsInvoiceNo == null || "".equals(lsInvoiceNo) || "To-follow".equals(lsInvoiceNo)){
            lsInvoiceNo = foObject.PurchaseOrderReceivingMaster().getReferenceNo();
        }
        Double ldblTotalDiscount = 0.0000;
        Double ldblDiscountRate = foObject.getDiscountRate().doubleValue();
        if(ldblDiscountRate > 0.0000){
            ldblDiscountRate = foObject.getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
        }
        ldblTotalDiscount += (foObject.getDiscount().doubleValue() + ldblDiscountRate);
        
        for(int lnCtr = 0; lnCtr < foObject.getEntryNo();lnCtr++){
            Model_POReturn_Detail loObject = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnDetails();
            poJSON = loObject.openRecord(foObject.getTransactionNo(), lnCtr+1);
            if(!isJSONSuccess(poJSON)){
                return poJSON;
            }
            if(!loObject.isReverse()){
                continue;
            }
            String lsCategory = loObject.Inventory().CategoryLevel2().getDescription();
            if(lsCategory == null || "".equals(lsCategory)){
                lsCategory = loObject.Inventory().Category().getDescription();
            }
            lnAmount = loObject.getUnitPrce().doubleValue() * loObject.getQuantity().doubleValue();
            boolean lbExist = false;
            double lnDetAmt = 0.0000;
            for(int lnRow = 0;lnRow < Details.size();lnRow++){
                if(Details.get(lnRow).getsSourceCode().equals(DisbursementStatic.SourceCode.PO_RETURN)){
                    if(Details.get(lnRow).getsSourceNo().equals(foObject.getTransactionNo())
                       && Details.get(lnRow).getsParticular().equals(lsCategory)){
                        lnDetAmt = Details.get(lnRow).getnTotalAmount() * -1;
                        lnAmount = lnAmount + lnDetAmt;
                        lnAmount = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", ""));
                        if(lnAmount > 0.0000){
                            lnAmount = -lnAmount;
                        }
                        Details.get(lnRow).setnTotalAmount(lnAmount);
                        lbExist = true;
                    }
                }
            }
            if(!lbExist){
                if(lnAmount > ldblTotalDiscount){
                    lnAmount = lnAmount - ldblTotalDiscount;
                    ldblTotalDiscount = 0.0000;
                } else {
                    ldblTotalDiscount = ldblTotalDiscount - lnAmount;
                    lnAmount = 0.0000;
                }
                lnAmount = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(lnAmount, false).replace(",", ""));
                if(lnAmount > 0.0000){
                    lnAmount = -lnAmount;
                }
                Details.add(new TransactionPaymentSummaryDetail(
                        Details.size()+1,
                        lsCategory,
                        foObject.PurchaseOrderReceivingMaster().Department().getDescription(),
                        lsInvoiceNo,
                        foObject.getTransactionNo(),
                        DisbursementStatic.SourceCode.PO_RETURN,
                        lnAmount
                ));
            }
        }
        
        //Check if freight amount is exist
        if(foObject.getFreight().doubleValue() > 0.0000){
            
            Details.add(new TransactionPaymentSummaryDetail(
                    Details.size()+1,
                    "Freight",
                    foObject.PurchaseOrderReceivingMaster().Department().getDescription(),
                    lsInvoiceNo,
                    foObject.getTransactionNo(),
                    DisbursementStatic.SourceCode.PO_RETURN,
                    -Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat( foObject.getFreight().doubleValue(), false).replace(",", ""))
            ));
        }
    
        return poJSON;
    }
    
    private JSONObject getAPAdjustment(Model_AP_Payment_Adjustment foObject,  List<TransactionPaymentSummaryDetail> Details ) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        double lnAmount = 0.0000;
        if(foObject.getCreditAmount().doubleValue() > 0.0000){
            lnAmount = -Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(foObject.getCreditAmount().doubleValue(), false).replace(",", ""));
        } else {
            lnAmount = Double.valueOf(CustomCommonUtil.setIntegerValueToDecimalFormat(foObject.getDebitAmount().doubleValue(), false).replace(",", ""));
        }
        Details.add(new TransactionPaymentSummaryDetail(
                Details.size()+1,
                particular(DisbursementStatic.SourceCode.AP_ADJUSTMENT) + " - " + foObject.getRemarks(),
                "",
                foObject.getReferenceNo(),
                foObject.getTransactionNo(),
                DisbursementStatic.SourceCode.AP_ADJUSTMENT,
                lnAmount
                
        ));
    
        return poJSON;
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case DisbursementStatic.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case DisbursementStatic.VERIFIED:
                    crs.updateString("cRefrStat", "VERIFIED");
                    break;
                case DisbursementStatic.CERTIFIED:
                    crs.updateString("cRefrStat", "CERTIFIED");
                    break;
                case DisbursementStatic.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case DisbursementStatic.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case DisbursementStatic.AUTHORIZED:
                    crs.updateString("cRefrStat", "AUTHORIZED");
                    break;
                case DisbursementStatic.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                case DisbursementStatic.RETURNED_I:
                    crs.updateString("cRefrStat", "RETURNED+");
                    break;
                case DisbursementStatic.DISAPPROVED:
                    crs.updateString("cRefrStat", "DISAPPROVED");
                    break;
                case DisbursementStatic.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case DisbursementStatic.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case DisbursementStatic.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case DisbursementStatic.VERIFIED:
                            crs.updateString("cRefrStat", "VERIFIED");
                            break;
                        case DisbursementStatic.CERTIFIED:
                            crs.updateString("cRefrStat", "CERTIFIED");
                            break;
                        case DisbursementStatic.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case DisbursementStatic.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case DisbursementStatic.AUTHORIZED:
                            crs.updateString("cRefrStat", "AUTHORIZED");
                            break;
                        case DisbursementStatic.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                        case DisbursementStatic.RETURNED_I:
                            crs.updateString("cRefrStat", "RETURNED+");
                            break;
                        case DisbursementStatic.DISAPPROVED:
                            crs.updateString("cRefrStat", "DISAPPROVED");
                            break;
                        case DisbursementStatic.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case DisbursementStatic.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
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
        
        showStatusHistoryUI("Disbursement Voucher", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM Disbursement_Master a "
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

    /**
     * Retrieves Disbursement Summary Report records including company, payee,
     * bank account, check payment, transaction totals, tax amounts, and status
     * information.
     *
     * <p>This method generates a summarized disbursement dataset by joining
     * disbursement transactions with check payments, bank accounts, banks,
     * company information, and payee records.</p>
     *
     * <p>The result provides transaction-level details suitable for financial
     * reporting, check monitoring, audit verification, and payment reconciliation
     * purposes.</p>
     *
     * <p><b>Filters Applied:</b></p>
     * <ul>
     *   <li>Date range (a.dTransact BETWEEN dateFrom and dateThru)</li>
     *   <li>Payee name (d.sPayeeNme)</li>
     *   <li>Bank account number (e.sActNumbr)</li>
     *   <li>Bank account name (e.sActNamex)</li>
     *   <li>Bank name (f.sBankName)</li>
     *   <li>Check transaction status (b.cTranStat)</li>
     *   <li>Company ID and Industry Code (system context via Master())</li>
     *   <li>Transaction prefix (based on logged-in branch code)</li>
     *   <li>Disbursement transaction status (single or multi-value filter)</li>
     * </ul>
     *
     * <p><b>Output Fields:</b></p>
     * <ul>
     *   <li>sTransNox - Disbursement transaction number</li>
     *   <li>sCompnyCd - Company code</li>
     *   <li>dTransact - Transaction date</li>
     *   <li>sPayeeNme - Payee name</li>
     *   <li>sActNumbr - Bank account number</li>
     *   <li>sActNamex - Bank account name</li>
     *   <li>sBankName - Bank name</li>
     *   <li>sCheckNox - Check number</li>
     *   <li>nTranTotl - Gross transaction amount</li>
     *   <li>nDiscTotl - Total discount amount</li>
     *   <li>nWTaxTotl - Total withholding tax amount</li>
     *   <li>nVATSales - VAT sales amount</li>
     *   <li>nVATAmtxx - VAT amount</li>
     *   <li>nZroVATSl - Zero-rated VAT sales amount</li>
     *   <li>nVatExmpt - VAT exempt amount</li>
     *   <li>nNetTotal - Net transaction amount</li>
     *   <li>nAmountxx - Check payment amount</li>
     *   <li>sStatus - Human-readable disbursement transaction status</li>
     *   <li>sChkPrint - Check printing status</li>
     *   <li>sCheckStatus - Human-readable check transaction status</li>
     *   <li>sRemarksx - Transaction remarks</li>
     * </ul>
     *
     * @param issummarized flag indicating summarized or detailed mode
     *                     (reserved for future enhancement)
     * @param dateFrom start date filter (inclusive)
     * @param dateThru end date filter (inclusive)
     * @param Payee payee name filter
     * @param AccountNo bank account number filter
     * @param AccountNme bank account name filter
     * @param BankName bank name filter
     * @param CheckStat check transaction status filter
     *
     * @return a JSONObject containing:
     *         <ul>
     *           <li>result - success / error</li>
     *           <li>message - execution status</li>
     *           <li>data - JSONArray of summarized disbursement records</li>
     *         </ul>
     *
     * @throws SQLException if database query execution fails
     * @throws GuanzonException if business validation fails
     *
     * @author TEEJEI DE CELIS
     * @since 06-26-2026
     * @apiNote This method is intended for disbursement summary reporting,
     *          check monitoring, financial reconciliation, and audit purposes.
     */
    public JSONObject RetriveSummaryReports(Boolean issummarized,
                                            LocalDate dateFrom,
                                            LocalDate dateThru,
                                            String Payee,
                                            String AccountNo,
                                            String AccountNme,
                                            String BankName,
                                            String CheckStat)
            throws SQLException, GuanzonException {

        poJSON = new JSONObject();
        String lsTransStat = "";
        dfrom = String.valueOf(dateFrom);
        dthru = String.valueOf(dateThru);
        try {

            String lsSQL = "SELECT "
                    + "  a.sTransNox, "
                    + "  c.sCompnyCd, "
                    + "  a.dTransact, "
                    + "  d.sPayeeNme, "
                    + "  e.sActNumbr, "
                    + "  e.sActNamex, "
                    + "  f.sBankName, "
                    + "  b.sCheckNox, "
                    + "  a.nTranTotl, "
                    + "  a.nDiscTotl, "
                    + "  a.nWTaxTotl, "
                    + "  a.nVATSales, "
                    + "  a.nVATAmtxx, "
                    + "  a.nZroVATSl, "
                    + "  a.nVatExmpt, "
                    + "  a.nNetTotal, "
                    + "  b.nAmountxx, "
                    + "  CASE a.cTranStat "
                    + "      WHEN '0' THEN 'OPEN' "
                    + "      WHEN '1' THEN 'VERIFIED' "
                    + "      WHEN '3' THEN 'CANCELLED' "
                    + "      WHEN '4' THEN 'AUTHORIZED' "
                    + "      WHEN '5' THEN 'VOID' "
                    + "      WHEN '6' THEN 'DISAPPROVED' "
                    + "      WHEN '7' THEN 'CONFIRMED' "
                    + "      WHEN '8' THEN 'APPROVED' "
                    + "      WHEN '9' THEN 'RETURNED' "
                    + "      WHEN 'I' THEN 'RETURNED' "
                    + "  END AS sStatus, "
                    + "  CASE b.cPrintxxx "
                    + "      WHEN '0' THEN 'OPEN' "
                    + "      WHEN '1' THEN 'PRINTED' "
                    + "      ELSE 'Unknown' "
                    + "  END AS sChkPrint, "
                    + "  CASE b.cTranStat "
                    + "      WHEN '0' THEN 'OPEN(no check yet)' "
                    + "      WHEN '1' THEN 'CONFIRMED' "
                    + "      WHEN '2' THEN 'POSTED' "
                    + "      WHEN '3' THEN 'CANCELLED' "
                    + "      WHEN '4' THEN 'STALED' "
                    + "      WHEN '5' THEN 'for clarification' "
                    + "      WHEN '6' THEN 'BOUNCED' "
                    + "      WHEN '7' THEN 'VOID' "
                    + "  END AS sCheckStatus, "
                    + "  a.sRemarksx "
                    + "FROM Disbursement_Master a "
                    + "LEFT JOIN Check_Payments b "
                    + "    ON a.sTransNox = b.sSourceNo "
                    + "LEFT JOIN Bank_Account_Master e "
                    + "    ON b.sBnkActID = e.sBnkActID "
                    + "LEFT JOIN Banks f "
                    + "    ON b.sBankIDxx = f.sBankIDxx "
                    + "LEFT JOIN Company c "
                    + "    ON a.sCompnyID = c.sCompnyID "
                    + "LEFT JOIN Payee d "
                    + "    ON a.sPayeeIDx = d.sPayeeIDx ";

            // -------------------------------
            // FILTERS
            // -------------------------------
            List<String> lsFilter = new ArrayList<>();

            if (dateFrom != null && dateThru != null) {
                lsFilter.add("a.dTransact BETWEEN "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateFrom))
                        + " AND "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateThru)));
                dfrom = String.valueOf(dateFrom);
                dthru = String.valueOf(dateThru);
            }
            if (Payee != null && !Payee.trim().isEmpty()) {
                lsFilter.add("d.sPayeeNme LIKE " + SQLUtil.toSQL(Payee + "%"));
            }

            if (AccountNo != null && !AccountNo.trim().isEmpty()) {
                lsFilter.add("e.sActNumbr LIKE " + SQLUtil.toSQL(AccountNo + "%"));
            }

            if (AccountNme != null && !AccountNme.trim().isEmpty()) {
                lsFilter.add("e.sActNamex LIKE " + SQLUtil.toSQL(AccountNme + "%"));
            }

            if (BankName != null && !BankName.trim().isEmpty()) {
                lsFilter.add("f.sBankName LIKE " + SQLUtil.toSQL(BankName + "%"));
            }


            if (CheckStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= CheckStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(CheckStat.charAt(lnCtr)));
                }
                lsFilter.add( " b.cTranStat IN (" + lsTransStat.substring(2) + ")");
            } else {
                lsFilter.add( " b.cTranStat = " + SQLUtil.toSQL(CheckStat));
            }

            lsFilter.add("a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                    + " AND a.sIndstCdx = " +  SQLUtil.toSQL(Master().getIndustryID()));
            if(poGRider.getUserLevel()<=UserRight.ENCODER){
                lsFilter.add("a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%"));
            }
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsFilter.add( " a.cTranStat IN (" + lsTransStat.substring(2) + ")");
            } else {
                lsFilter.add( " a.cTranStat = " + SQLUtil.toSQL(psTranStat));
            }
            if (!lsFilter.isEmpty()) {
                lsSQL += " WHERE " + String.join(" AND ", lsFilter);
            }

            lsSQL += " ORDER BY a.dTransact,a.sTransNox ASC";

            System.out.println("Retrive Summary Reports QUERY: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (loRS == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }

            int lnctr = 0;
            JSONArray dataArray = new JSONArray();

            while (loRS.next()) {
                JSONObject record = new JSONObject  ();

                record.put("sTransNox", getString(loRS, "sTransNox"));
                record.put("sCompnyCd", getString(loRS, "sCompnyCd"));

                record.put("dTransact", getDate(loRS, "dTransact"));

                record.put("sPayeeNme", getString(loRS, "sPayeeNme"));

                record.put("sActNumbr", getString(loRS, "sActNumbr"));
                record.put("sActNamex", getString(loRS, "sActNamex"));

                record.put("sBankName", getString(loRS, "sBankName"));
                record.put("sCheckNox", getString(loRS, "sCheckNox"));

                record.put("nTranTotl", loRS.getDouble("nTranTotl"));
                record.put("nDiscTotl", loRS.getDouble("nDiscTotl"));
                record.put("nWTaxTotl", loRS.getDouble("nWTaxTotl"));
                record.put("nVATSales", loRS.getDouble("nVATSales"));
                record.put("nVATAmtxx", loRS.getDouble("nVATAmtxx"));
                record.put("nZroVATSl", loRS.getDouble("nZroVATSl"));
                record.put("nVatExmpt", loRS.getDouble("nVatExmpt"));
                record.put("nNetTotal", loRS.getDouble("nNetTotal"));

                record.put("nAmountxx", loRS.getDouble("nAmountxx"));

                record.put("sStatus", getString(loRS, "sStatus"));
                record.put("sChkPrint", getString(loRS, "sChkPrint"));
                record.put("sCheckStatus", getString(loRS, "sCheckStatus"));

                record.put("sRemarksx", getString(loRS, "sRemarksx"));

                dataArray.add(record);
                lnctr++;
            }

            MiscUtil.close(loRS);

            if (lnctr > 0) {
                poJSON.put("result", "success");
                poJSON.put("message", "Record(s) loaded successfully.");
                poJSON.put("data", dataArray);
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
                poJSON.put("data", new JSONArray());
            }

        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }
    /**
     * Retrieves detailed Disbursement Report records including payee information,
     * check details, source transactions, VAT applicability, and payment amounts.
     *
     * <p>This method generates a detailed transactional dataset by joining
     * disbursement master records with disbursement details, check payments,
     * bank accounts, banks, branches, and payee information.</p>
     *
     * <p>The result provides source-level payment breakdown suitable for
     * financial auditing, payment tracing, check reconciliation, and
     * disbursement analysis purposes.</p>
     *
     * <p><b>Filters Applied:</b></p>
     * <ul>
     *   <li>Date range (a.dTransact BETWEEN dateFrom and dateThru)</li>
     *   <li>Payee name (d.sPayeeNme)</li>
     *   <li>Bank account number (f.sActNumbr)</li>
     *   <li>Bank account name (f.sActNamex)</li>
     *   <li>Bank name (g.sBankName)</li>
     *   <li>Check transaction status (e.cTranStat)</li>
     *   <li>Company ID and Industry Code (system context via Master())</li>
     *   <li>Transaction prefix (based on logged-in branch code)</li>
     *   <li>Disbursement transaction status (single or multi-value filter)</li>
     * </ul>
     *
     * <p><b>Output Fields:</b></p>
     * <ul>
     *   <li>sTransNox - Disbursement transaction number</li>
     *   <li>dTransact - Transaction date</li>
     *   <li>sPayeeNme - Payee name</li>
     *   <li>sStatus - Human-readable disbursement status</li>
     *   <li>sCheckStatus - Human-readable check payment status</li>
     *   <li>sSourceCode - Source transaction type
     *       (PO Receiving, PRF, Adjustment, etc.)</li>
     *   <li>sSourceNo - Source transaction number</li>
     *   <li>nAmountxx - Original transaction amount</li>
     *   <li>nAmtAppld - Applied payment amount</li>
     *   <li>cWithVatx - VAT applicability flag (Yes/No)</li>
     *   <li>nDetVatAm - Detailed VAT amount</li>
     * </ul>
     *
     * @param issummarized flag indicating summarized or detailed mode
     *                     (reserved for future enhancement)
     * @param dateFrom start date filter (inclusive)
     * @param dateThru end date filter (inclusive)
     * @param Payee payee name filter
     * @param AccountNo bank account number filter
     * @param AccountNme bank account name filter
     * @param BankName bank name filter
     * @param CheckStat check transaction status filter
     *
     * @return a JSONObject containing:
     *         <ul>
     *           <li>result - success / error</li>
     *           <li>message - execution status</li>
     *           <li>data - JSONArray of detailed disbursement records</li>
     *         </ul>
     *
     * @throws SQLException if database query execution fails
     * @throws GuanzonException if business validation fails
     *
     * @author TEEJEI DE CELIS
     * @since 06-26-2026
     * @apiNote This method is intended for detailed disbursement reporting,
     *          source transaction auditing, check reconciliation, and
     *          financial analysis purposes.
     */
    public JSONObject RetriveSummaryDetailedReports(Boolean issummarized,
                                                    LocalDate dateFrom,
                                                    LocalDate dateThru,
                                                    String Payee,
                                                    String AccountNo,
                                                    String AccountNme,
                                                    String BankName,
                                                    String CheckStat)throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        try {

            String lsSQL = "SELECT DISTINCT "
                    + "  a.sTransNox, "
                    + "  a.dTransact, "
                    + "  d.sPayeeNme, "
                    + "  CASE a.cTranStat "
                    + "      WHEN '0' THEN 'OPEN' "
                    + "      WHEN '1' THEN 'VERIFIED' "
                    + "      WHEN '3' THEN 'CANCELLED' "
                    + "      WHEN '4' THEN 'AUTHORIZED' "
                    + "      WHEN '5' THEN 'VOID' "
                    + "      WHEN '6' THEN 'DISAPPROVED' "
                    + "      WHEN '7' THEN 'CONFIRMED' "
                    + "      WHEN '8' THEN 'APPROVED' "
                    + "      WHEN '9' THEN 'RETURNED' "
                    + "      WHEN 'I' THEN 'RETURNED' "
                    + "  END AS sStatus, "
                    + "  CASE e.cTranStat "
                    + "      WHEN '0' THEN 'OPEN(no check yet)' "
                    + "      WHEN '1' THEN 'CONFIRMED' "
                    + "      WHEN '2' THEN 'POSTED' "
                    + "      WHEN '3' THEN 'CANCELLED' "
                    + "      WHEN '4' THEN 'STALED' "
                    + "      WHEN '5' THEN 'for clarification' "
                    + "      WHEN '6' THEN 'BOUNCED' "
                    + "      WHEN '7' THEN 'VOID' "
                    + "  END AS sCheckStatus, "
                    + "  CASE b.sSourceCd "
                    + "      WHEN 'PODA' THEN 'PO Receiving' "
                    + "      WHEN 'PRFx' THEN 'PRF' "
                    + "      WHEN 'APAd' THEN 'Adjustment' "
                    + "      ELSE 'Unknown' "
                    + "  END AS sSourceCode, "
                    + "  b.sSourceNo, "
                    + "  b.nAmountxx, "
                    + "  b.nAmtAppld, "
                    + "  b.cWithVatx, "
                    + "  b.nDetVatAm "
                    + "FROM Disbursement_Master a "
                    + "LEFT JOIN Payee d "
                    + "    ON a.sPayeeIDx = d.sPayeeIDx "
                    + "LEFT JOIN Check_Payments e "
                    + "    ON a.sTransNox = e.sSourceNo "
                    + " LEFT JOIN Bank_Account_Master f "
                    +      "ON f.sBnkActID = e.sBnkActID "
                    + " LEFT JOIN Banks g "
                    +      "ON e.sBankIDxx = g.sBankIDxx "
                    + "INNER JOIN  Disbursement_Detail b "
                    + "    ON a.sTransNox = b.sTransNox "
                    + "LEFT JOIN Branch c "
                    + "    ON LEFT(b.sTransNox, 4) = c.sBranchCd ";

            // -------------------------------
            // FILTERS
            // -------------------------------
            List<String> lsFilter = new ArrayList<>();

            if (dateFrom != null && dateThru != null) {
                lsFilter.add("a.dTransact BETWEEN "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateFrom))
                        + " AND "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateThru)));
                dfrom = String.valueOf(dateFrom);
                dthru = String.valueOf(dateThru);
            }

            if (Payee != null && !Payee.trim().isEmpty()) {
                lsFilter.add("d.sPayeeNme LIKE " + SQLUtil.toSQL(Payee + "%"));
            }

            if (AccountNo != null && !AccountNo.trim().isEmpty()) {
                lsFilter.add("f.sActNumbr LIKE " + SQLUtil.toSQL(AccountNo + "%"));
            }

            if (AccountNme != null && !AccountNme.trim().isEmpty()) {
                lsFilter.add("f.sActNamex LIKE " + SQLUtil.toSQL(AccountNme + "%"));
            }

            if (BankName != null && !BankName.trim().isEmpty()) {
                lsFilter.add("g.sBankName LIKE " + SQLUtil.toSQL(BankName + "%"));
            }
            if (CheckStat != null && !CheckStat.trim().isEmpty()) {
                lsFilter.add("e.cTranStat LIKE " + SQLUtil.toSQL(CheckStat + "%"));
            }

            lsFilter.add("a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                    + " AND a.sIndstCdx = " +  SQLUtil.toSQL(Master().getIndustryID()));

            if(poGRider.getUserLevel()<=UserRight.ENCODER){
                lsFilter.add("a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%"));
            }
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsFilter.add( " a.cTranStat IN (" + lsTransStat.substring(2) + ")");
            } else {
                lsFilter.add( " a.cTranStat = " + SQLUtil.toSQL(psTranStat));
            }

            if (!lsFilter.isEmpty()) {
                lsSQL += " WHERE " + String.join(" AND ", lsFilter);
            }

            lsSQL += " ORDER BY a.dTransact, a.sTransNox, a.nEntryNox ASC";

            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (loRS == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }

            int lnctr = 0;
            JSONArray dataArray = new JSONArray();

            while (loRS.next()) {

                JSONObject record = new JSONObject();

                record.put("sTransNox", getString(loRS, "sTransNox"));
                record.put("dTransact", getDate(loRS, "dTransact"));

                record.put("sPayeeNme", getString(loRS, "sPayeeNme"));

                record.put("sStatus", getString(loRS, "sStatus"));
                record.put("sCheckStatus", getString(loRS, "sCheckStatus"));
                record.put("sSourceCode", getString(loRS, "sSourceCode"));

                record.put("sSourceNo", getString(loRS, "sSourceNo"));

                record.put("nAmountxx", loRS.getDouble("nAmountxx"));
                record.put("nAmtAppld", loRS.getDouble("nAmtAppld"));

                String withVat = getString(loRS, "cWithVatx");
                switch (withVat) {
                    case "0":
                        record.put("cWithVatx", "No");
                        break;
                    case "1":
                        record.put("cWithVatx", "Yes");
                        break;
                    default:
                        record.put("cWithVatx", withVat);
                }

                record.put("nDetVatAm", loRS.getDouble("nDetVatAm"));

                dataArray.add(record);
                lnctr++;
            }

            MiscUtil.close(loRS);

            if (lnctr > 0) {
                poJSON.put("result", "success");
                poJSON.put("message", "Record(s) loaded successfully.");
                poJSON.put("data", dataArray);
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
                poJSON.put("data", new JSONArray());
            }

        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }

    /**
     * Safely retrieves a String value from a ResultSet column.
     *
     * <p>This method prevents null or empty values from propagating to the JSON response.
     * If the database value is null or empty, a default placeholder "-" is returned.</p>
     *
     * @param rs the ResultSet containing the query result
     * @param col the column name to retrieve
     * @return a trimmed string value, or "-" if the value is null or empty
     * @throws SQLException if a database access error occurs
     *
     * @author TEEJEI DE CELIS
     * @since 06-26-2026
     * @apiNote This method is part of the Disbursement Voucher reporting module.
     */
    private String getString(ResultSet rs, String col) throws SQLException {
        String val = rs.getString(col);
        return (val == null || val.trim().isEmpty()) ? "-" : val;
    }
    /**
     * Safely retrieves a Date value from a ResultSet column.
     *
     * <p>This method ensures null-safe date handling for reporting output.
     * If the database value is null, an empty string ("") is returned.</p>
     *
     * <p>The returned format is the default ISO format (yyyy-MM-dd) as provided by
     * {@link java.sql.Date#toString()}.</p>
     *
     * @param rs the ResultSet containing the query result
     * @param col the column name to retrieve
     * @return the date in string format (yyyy-MM-dd), or empty string if null
     * @throws SQLException if a database access error occurs
     *
     * @author TEEJEI DE CELIS
     * @since 06-26-2026
     * @apiNote This method is part of the Disbursement Voucher reporting module.
     */
    private String getDate(ResultSet rs, String col) throws SQLException {
        java.sql.Date val = rs.getDate(col);
        return (val == null) ? "" : val.toString();
    }

    public JSONObject SearchAccount(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        BankAccountMaster object = new CashflowControllers(poGRider, logwrapr).BankAccountMaster();
        object.setRecordStatus("1");

        poJSON = object.searchRecordbyAccount(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            if(byCode){
                poJSON.put("accountNo",object.getModel().getAccountNo());
                poJSON.put("result", "success");
            }else{
                poJSON.put("accountName",object.getModel().getAccountName());
                poJSON.put("result", "success");
            }

        }

        return poJSON;
    }
    public JSONObject SearchBank(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Banks object = new ParamControllers(poGRider, logwrapr).Banks();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            poJSON.put("bankName",object.getModel().getBankName());
            poJSON.put("result", "success");
        }

        return poJSON;
    }
    public JSONObject SearchPayee(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            poJSON.put("payeeName",object.getModel().getPayeeName());
            poJSON.put("result", "success");
        }

        return poJSON;
    }

    public static String formatDateToText(String dfrom) {
        if (dfrom == null || dfrom.isEmpty()) {
            return "";
        }
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate date = LocalDate.parse(dfrom, inputFormatter);
        DateTimeFormatter outputFormatter= DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        return date.format(outputFormatter);
    }
    private double parseDouble(Object val) {
        try {
            return val == null ? 0.0000 : Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0000;
        }
    }
    private String safeStrings(Object value) {
        return value == null ? "" : value.toString();
    }


    public JSONObject printReports(Boolean isSummarized, JSONArray reportData) {
        poJSON = new JSONObject();

        try {

            System.out.println("Company Address : " + Master().Company().getCompanyAddress());
            System.out.println("Company Town : " + Master().Company().TownCity().getDescription());
            System.out.println("Company Province " + Master().Company().TownCity().Province().getDescription());
            System.out.println("Branch Address : " + Master().Branch().getAddress());
            System.out.println("Branch Town : " + Master().Branch().TownCity().getDescription());
            System.out.println("Branch Province " + Master().Branch().TownCity().Province().getDescription());

            String lsCompanyAddress = "";
            if (Master().Company().getCompanyAddress() != null && !"".equals(Master().Company().getCompanyAddress())) {
                lsCompanyAddress = Master().Company().getCompanyAddress().trim();
            }
            if (Master().Company().TownCity().getDescription() != null && !"".equals(Master().Company().TownCity().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + " " + Master().Company().TownCity().getDescription().trim();
            }
            if (Master().Company().TownCity().Province().getDescription() != null && !"".equals(Master().Company().TownCity().Province().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + ", " + Master().Company().TownCity().Province().getDescription().trim();
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sLogCompny", poGRider.getCompnyId());
            parameters.put("sAddressx", lsCompanyAddress);

            parameters.put("sCompany", Master().Company().getCompanyName());
            parameters.put("sIndustry", Master().Industry().getDescription());
            parameters.put("sBranch", Master().Branch().getBranchName());
            parameters.put("sDateRange", "FROM " + formatDateToText(dfrom) + " TO " + formatDateToText(dthru));

            List<Reports> reportList = new ArrayList<>();

            for (int i = 0; i < reportData.size(); i++) {

                JSONObject obj = (JSONObject) reportData.get(i);
                if (isSummarized) {
                    System.out.println("reportData : " +  obj);

                    reportList.add(new Reports(i + 1,
                            safeStrings(obj.get("sTransNox")),
                            safeStrings(obj.get("dTransact")),
                            safeStrings(obj.get("sPayeeNme")),
                            safeStrings(obj.get("sActNumbr")),
                            safeStrings(obj.get("sActNamex")),
                            safeStrings(obj.get("sBankName")),
                            safeStrings(obj.get("sCheckNox")),
                            parseDouble(obj.get("nTranTotl")),
                            parseDouble(obj.get("nDiscTotl")),
                            parseDouble(obj.get("nWTaxTotl")),
                            parseDouble(obj.get("nVATAmtxx")),
                            parseDouble(obj.get("nNetTotal")),
                            parseDouble(obj.get("nAmountxx")),
                            safeStrings(obj.get("sStatus")),
                            safeStrings(obj.get("sChkPrint")),
                            safeStrings(obj.get("sCheckStatus"))));

                } else {
                    System.out.println("reportData : " +  obj);
                    reportList.add(new Reports(i + 1,
                            safeStrings(obj.get("sTransNox")),
                            safeStrings(obj.get("dTransact")),
                            safeStrings(obj.get("sPayeeNme")),
                            safeStrings(obj.get("sStatus")),
                            safeStrings(obj.get("sCheckStatus")),
                            safeStrings(obj.get("sSourceCode")),
                            safeStrings(obj.get("sSourceNo")),
                            parseDouble(obj.get("nAmountxx")),
                            parseDouble(obj.get("nAmtAppld")),
                            safeStrings(obj.get("cWithVatx")),
                            parseDouble(obj.get("nDetVatAm"))));
                }
            }

            JRBeanCollectionDataSource dataSource
                    = new JRBeanCollectionDataSource(reportList);


            // 4. Compile and fill report
            String jrxmlPath;

            if (isSummarized) {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/DisbursementVoucherSummary.jrxml";
            } else {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/DisbursementVoucherSummaryDetail.jrxml";
            }

            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            if (System.getProperty("os.name").toLowerCase().contains("win")) {

                CustomJasperViewerReports.show(
                        jasperPrint,
                        "Disbursement Voucher Reports");
            } else {
                //mac 2026.02.21
                //export pdf file
                JasperExportManager.exportReportToPdfFile(jasperPrint, System.getProperty("sys.default.path.config") + "/temp/" + Master().getTransactionNo() + ".pdf");
            }

            poJSON.put("result", "success");
        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger
                    .getLogger(PurchaseOrder.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;

    }

    public static class Reports {

        private Integer nRowNo;
        private String sTransNox;
        private String dTransact;
        private String sPayeeNme;
        private String sActNumbr;
        private String sActNamex;
        private String sBankName;
        private String sCheckNox;
        private double nTranTotl;
        private double nDiscTotl;
        private double nWTaxTotl;
        private double nVATAmtxx;
        private double nNetTotal;
        private double nAmountxx;
        private String sStatus;
        private String sChkPrint;
        private String sCheckStatus;

        private String sSourceCd;
        private String sSourceNo;
        private double nAmtAppld;
        private String cWithVat;
        private double nDetVatAm;
        private double nTotal;

        public Reports(Integer nrowNo, String sTransNox, String dTransact, String sPayeeNme, String sActNumbr,
                       String sActNamex,String sBankName, String sCheckNox, double nTranTotl, double nDiscTotl, double nWTaxTotl,
                       double nVATAmtxx, double nNetTotal, double nAmountxx, String sStatus, String sChkPrint, String sCheckStatus) {

            this.nRowNo = nrowNo;
            this.sTransNox = sTransNox;
            this.dTransact = dTransact;
            this.sPayeeNme = sPayeeNme;
            this.sActNumbr = sActNumbr;
            this.sActNamex = sActNamex;
            this.sBankName = sBankName;
            this.sCheckNox = sCheckNox;
            this.nTranTotl = nTranTotl;
            this.nDiscTotl = nDiscTotl;
            this.nWTaxTotl = nWTaxTotl;
            this.nVATAmtxx = nVATAmtxx;
            this.nNetTotal = nNetTotal;
            this.nAmountxx = nAmountxx;
            this.sStatus = sStatus;
            this.sChkPrint = sChkPrint;
            this.sCheckStatus = sCheckStatus;
        }
        public Reports(Integer nrowNo, String sTransNox,String dTransact, String sPayeeNme, String sStatus, String sCheckStatus,
                       String sSourceCd, String sSourceNo,double nAmountxx, double nAmtAppld, String cWithVat, double nDetVatAm) {

            this.nRowNo = nrowNo;
            this.sTransNox = sTransNox;
            this.dTransact = dTransact;
            this.sPayeeNme = sPayeeNme;
            this.sStatus = sStatus;
            this.sCheckStatus = sCheckStatus;
            this.sSourceCd = sSourceCd;
            this.sSourceNo = sSourceNo;
            this.nAmountxx = nAmountxx;
            this.nAmtAppld = nAmtAppld;
            this.cWithVat = cWithVat;
            this.nVATAmtxx = nVATAmtxx;
        }


        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsTransNox() {
            return sTransNox;
        }

        public String getdTransact() {
            return dTransact;
        }

        public String getsPayeeNme() { 
            return sPayeeNme;
        }

        public String getsActNumbr() { 
            return sActNumbr;
        }

        public String getsActNamex() {
            return sActNamex;
        }

        public String getsBankName() {
            return sBankName;
        }

        public String getsCheckNox() {
            return sCheckNox;
        }
        
        public double getnTranTotl() {
            return nTranTotl;
        }

        public double getnDiscTotl() {
            return nDiscTotl;
        }

        public double getnWTaxTotl() {
            return nWTaxTotl;
        }

        public double getnVATAmtxx() {
            return nVATAmtxx;
        }

        public double getnNetTotal() {
            return nNetTotal;
        }

        public double getnAmountxx() {
            return nAmountxx;
        }
        
        public String getsStatus() {
            return sStatus;
        }
        
        public String getsChkPrint() {
            return sChkPrint;
        }

        public String getsCheckStatus() {
            return sStatus;
        }
        
        public String getsSourceCd() {
            return sSourceCd;
        }

        public String getsSourceNo() {
            return sSourceNo;
        }

        public double getnAmtAppld() {
            return nAmtAppld;
        }

        public String getcWithVat() {
            return cWithVat;
        }
        
        public double getnDetVatAm() {
            return nDetVatAm;
        }
    }

//    public class CustomJasperViewers extends JasperViewer {
//
//        public CustomJasperViewers(final JasperPrint jasperPrint) {
//            super(jasperPrint, false);
//            customizeToolbar(jasperPrint);
//        }
//
//        private void customizeToolbar(final JasperPrint jasperPrint) {
//
//            JRViewer viewer = getJRViewer(getContentPane());
//
//            if (viewer == null) {
//                System.out.println("JRViewer not found.");
//                return;
//            }
//
//            for (Component component : viewer.getComponents()) {
//
//                if (!(component instanceof JRViewerToolbar)) {
//                    continue;
//                }
//
//                JRViewerToolbar toolbar = (JRViewerToolbar) component;
//
//                for (Component c : toolbar.getComponents()) {
//                    if (c instanceof JButton) {
//                        handleButton((JButton) c, jasperPrint);
//                    }
//                }
//
//                toolbar.revalidate();
//                toolbar.repaint();
//            }
//        }
//
//        private void handleButton(JButton button, final JasperPrint jasperPrint) {
//
//            String tooltip = button.getToolTipText();
//
//            if ("Save".equals(tooltip)) {
//
//                button.setEnabled(true);
//                button.setVisible(true);
//
//                for (ActionListener listener : button.getActionListeners()) {
//                    button.removeActionListener(listener);
//                }
//
//                button.addActionListener(e -> saveReport(jasperPrint));
//                return;
//            }
//
//            if ("Print".equals(tooltip)) {
//
//                for (ActionListener listener : button.getActionListeners()) {
//                    button.removeActionListener(listener);
//                }
//
//                button.addActionListener(e -> printReport(jasperPrint));
//            }
//        }
//
//        private void printReport(final JasperPrint jasperPrint) {
//
//            try {
//
//                boolean isPrinted = JasperPrintManager.printReport(jasperPrint, true);
//
//                if (!isPrinted) {
//                    Platform.runLater(() -> {
//                        ShowMessageFX.Warning(
//                                "Printing was canceled by the user.",
//                                "Disbursement Voucher Reports",
//                                null
//                        );
//
//                        SwingUtilities.invokeLater(this::toFront);
//                    });
//
//                    return;
//                }
//
//                // Uncomment when needed
//                // PrintTransaction(true);
//
//            } catch (JRException ex) {
//
//                Platform.runLater(() -> {
//                    ShowMessageFX.Warning(
//                            "Print Failed: " + ex.getMessage(),
//                            "Disbursement Voucher Reports",
//                            null
//                    );
//
//                    SwingUtilities.invokeLater(this::toFront);
//                });
//            }
//        }
//
//        private JRViewer getJRViewer(Container container) {
//
//            for (Component component : container.getComponents()) {
//
//                if (component instanceof JRViewer) {
//                    return (JRViewer) component;
//                }
//
//                if (component instanceof Container) {
//
//                    JRViewer viewer = getJRViewer((Container) component);
//
//                    if (viewer != null) {
//                        return viewer;
//                    }
//                }
//            }
//
//            return null;
//        }
//
//        private void saveReport(final JasperPrint jasperPrint) {
//
//            JFileChooser chooser = new JFileChooser();
//            chooser.setDialogTitle("Save Report");
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("Word Document (*.docx)", "docx"));
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("CSV (*.csv)", "csv"));
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("HTML (*.html)", "html"));
//
//            chooser.addChoosableFileFilter(
//                    new FileNameExtensionFilter("JR XML (*.jrpxml)", "jrpxml"));
//
//            chooser.setAcceptAllFileFilterUsed(false);
//
//            int result = chooser.showSaveDialog(this);
//
//            if (result != JFileChooser.APPROVE_OPTION) {
//
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        ShowMessageFX.Warning(
//                                "Saving was canceled by the user.",
//                                "Disbursement Voucher Reports",
//                                null
//                        );
//                    }
//                });
//
//                return;
//            }
//
//            try {
//
//                FileNameExtensionFilter filter =
//                        (FileNameExtensionFilter) chooser.getFileFilter();
//
//                String extension = filter.getExtensions()[0];
//
//                File file = chooser.getSelectedFile();
//                String fileName = file.getAbsolutePath();
//
//                if (!fileName.toLowerCase().endsWith("." + extension)) {
//                    fileName += "." + extension;
//                }
//
//                switch (extension.toLowerCase()) {
//
//                    case "pdf":
//
//                        JasperExportManager.exportReportToPdfFile(
//                                jasperPrint,
//                                fileName
//                        );
//
//                        break;
//
//                    case "xlsx": {
//
//                        JRXlsxExporter exporter = new JRXlsxExporter();
//
//                        exporter.setExporterInput(
//                                new SimpleExporterInput(jasperPrint));
//
//                        exporter.setExporterOutput(
//                                new SimpleOutputStreamExporterOutput(fileName));
//
//                        SimpleXlsxReportConfiguration config =
//                                new SimpleXlsxReportConfiguration();
//
//                        config.setOnePagePerSheet(false);
//                        config.setDetectCellType(true);
//                        config.setCollapseRowSpan(false);
//
//                        exporter.setConfiguration(config);
//                        exporter.exportReport();
//
//                        break;
//                    }
//
//                    case "docx": {
//
//                        JRDocxExporter exporter = new JRDocxExporter();
//
//                        exporter.setExporterInput(
//                                new SimpleExporterInput(jasperPrint));
//
//                        exporter.setExporterOutput(
//                                new SimpleOutputStreamExporterOutput(fileName));
//
//                        exporter.exportReport();
//
//                        break;
//                    }
//
//                    case "csv": {
//
//                        JRCsvExporter exporter = new JRCsvExporter();
//
//                        exporter.setExporterInput(
//                                new SimpleExporterInput(jasperPrint));
//
//                        exporter.setExporterOutput(
//                                new SimpleWriterExporterOutput(fileName));
//
//                        exporter.exportReport();
//
//                        break;
//                    }
//
//                    case "html": {
//
//                        HtmlExporter exporter = new HtmlExporter();
//
//                        exporter.setExporterInput(
//                                new SimpleExporterInput(jasperPrint));
//
//                        exporter.setExporterOutput(
//                                new SimpleHtmlExporterOutput(fileName));
//
//                        exporter.exportReport();
//
//                        break;
//                    }
//
//                    case "jrpxml":
//
//                        JRXmlWriter.writeReport(
//                                (JRReport) jasperPrint,
//                                fileName,
//                                "UTF-8"
//                        );
//
//                        break;
//
//                    default:
//
//                        throw new IllegalArgumentException(
//                                "Unsupported file format: " + extension
//                        );
//                }
//
//                final String savedFile = fileName;
//
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        ShowMessageFX.Information(
//                                "Report saved successfully.\n\n" + savedFile,
//                                "Disbursement Voucher Reports",
//                                null
//                        );
//                    }
//                });
//
//            } catch (final Exception ex) {
//
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        ShowMessageFX.Warning(
//                                "Save failed:\n" + ex.getMessage(),
//                                "Disbursement Voucher Reports",
//                                null
//                        );
//                    }
//                });
//
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        CustomJasperViewers.this.setVisible(true);
//                        CustomJasperViewers.this.toFront();
//                        CustomJasperViewers.this.requestFocus();
//                    }
//                });
//            }
//        }
//    }



}
