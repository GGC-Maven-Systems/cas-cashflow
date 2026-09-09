/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javax.sql.rowset.CachedRowSet;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Transfer_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Transfer_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CheckTransferStatus;
import ph.com.guanzongroup.cas.cashflow.validator.CheckTransferValidatorFactory;

/**
 *
 * @author user
 */
public class CheckTransfers extends Transaction {

    List<Model_Check_Transfer_Master> poCheckTransferMaster;
    List<Model_Check_Payments> paChecks;
    List<CheckPayments> poChecks;
    private ObservableList<Model_Check_Transfer_Detail> poReceiveList;
    private boolean pbApproval = false;
    LocalDateTime pdDateTime;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "Dlvr";

        poMaster = new CashflowModels(poGRider).CheckTransferMaster();
        poDetail = new CashflowModels(poGRider).CheckTransferDetail();
        paDetail = new ArrayList<>();
        paChecks = new ArrayList<>();
        poChecks = new ArrayList<>();

        return initialize();
    }

    public JSONObject SearchDistination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestination(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchDepartment(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDepartment(object.getModel().getDepartmentId());
        }

        return poJSON;
    }
    


    public JSONObject SearchChecks(String fsCheckTransNo, String fsCheckNo, int fnRow, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException {
        CheckPayments object = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        object.setRecordStatus("1");

        poJSON = object.searchRecordforChecktrans(fsCheckTransNo, fsCheckNo, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = addCheckPaymentToDetail(object.getModel().getTransactionNo());
            
//            
//            Detail(fnRow).setSourceNo(object.getModel().getTransactionNo());
//            computeMasterFields();
        }

        return poJSON;
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, CloneNotSupportedException, GuanzonException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public JSONObject ConfirmTransaction(String remarks) throws  SQLException, GuanzonException, CloneNotSupportedException {

        poJSON = new JSONObject();

        String lsStatus = CheckTransferStatus.CONFIRMED;
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
        poJSON = isEntryOkay(CheckTransferStatus.CONFIRMED);
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
            setApproving((String) poJSON.get("sUserIDxx"));
        }
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (!Detail(lnCtr).isReverse()) continue;
            String loc = Detail(lnCtr).CheckPayment().getLocation();
            if (loc != null && (loc.equals("1") || loc.equals("4"))) {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to proceed with confirmation. Check No. "
                        + Detail(lnCtr).CheckPayment().getCheckNo()
                        + " has a location that is already transferred.");
                return poJSON;
            }
        }
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = saveUpdates();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = CheckTransferStatus.CANCELLED;
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

        poJSON = isEntryOkay(CheckTransferStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws  SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String status = (String) poMaster.getValue("cTranStat");
        String lsStatus = CheckTransferStatus.VOID;
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

        poJSON = isEntryOkay(CheckTransferStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (CheckTransferStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
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
            }
        }
        
        if (CheckTransferStatus.CONFIRMED.equals(status)) {
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm,true);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        
        if (CheckTransferStatus.CONFIRMED.equals(status)) {
            poJSON = saveUpdates();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws  SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = CheckTransferStatus.POSTED;
        boolean lbConfirm = true;

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

        boolean allNotReceived = true;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (Detail(lnCtr).isReceived()) {
                allNotReceived = false; 
                break; 
            }
        }

        if (allNotReceived) {
            poJSON.put("result", "error");
            poJSON.put("message", "At least one detail must be received.");
            return poJSON;
        }
        
        System.out.println("DATE TIME " + pdDateTime);

        poJSON = isEntryOkay(CheckTransferStatus.POSTED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
//        JSONObject updateResult = updateReceivedDetails();
//        if (!"success".equals(updateResult.get("result"))) {
//            return updateResult; // stop if update failed
//        }
        
        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
//        receivedCheck((String) poMaster.getValue("sTransNox"));
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            Model_Check_Transfer_Detail loDetail = Detail(lnCtr);
           
            if (loDetail.getSourceNo() == null || loDetail.getSourceNo().isEmpty()) {
                continue;
            }

            if (!loDetail.isReceived()) {
                continue;
            }

            poJSON = ReceiveCheckPaymentTransaction(loDetail);

            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        Model_Check_Transfer_Master loMaster = Master();
        poJSON = updateTransferMaster(loMaster,pdDateTime );
        if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        
        poGRider.commitTrans();
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject updateTransferMaster( Model_Check_Transfer_Master loMaster, LocalDateTime  dReceive) throws GuanzonException, SQLException {
        JSONObject poJSON = new JSONObject();
        
        String lsSQL = "UPDATE "
                + poMaster.getTable()
                + " SET   dReceived = " + SQLUtil.toSQL(dReceive)                
                + "    ,sReceived = " + SQLUtil.toSQL(poGRider.getUserID())                
                + "    ,cTranStat = " + SQLUtil.toSQL(CheckTransferStatus.POSTED)   
                + " WHERE sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo());

        Long lnResult = poGRider.executeQuery(lsSQL,
                poMaster.getTable(),
                poGRider.getBranchCode(), "", "");
        if (lnResult <= 0L) {
            poGRider.rollbackTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "error");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject ReceiveCheckPaymentTransaction(Model_Check_Transfer_Detail loDetail)
        throws SQLException, GuanzonException {

    JSONObject poJSON = new JSONObject();

    Model_Check_Payments loCheckPayment = loDetail.CheckPayment();

    if (loCheckPayment.getEditMode() == EditMode.READY) {

        loCheckPayment.updateRecord();
        loCheckPayment.setBranchCode(Master().getDestination());
        loCheckPayment.setLocation("1");

        poJSON = loCheckPayment.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        


        loDetail.updateRecord();
        loDetail.getTransactionNo();
        loDetail.isReceived(true);
        loDetail.setModifiedDate(poGRider.getServerDate());

        poJSON = loDetail.saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
    }

    poJSON = new JSONObject();
    poJSON.put("result", "success");
    return poJSON;
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

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox, "
                + " a.dTransact, "
                + " c.sBranchNm AS xBranchNm, "
                + " d.sBranchNm AS xDestinat, "
                + " a.sDestinat, "
                + " a.cTranStat, "
                + " k.sPayeeNme, "
                + " f.sBankName "
                + " FROM Check_Transfer_Master a "
                + " LEFT JOIN Check_Transfer_Detail b "
                + "   ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Branch c "
                + "   ON LEFT(a.sTransNox, 4) = c.sBranchCd "
                + " LEFT JOIN Branch d "
                + "   ON a.sDestinat = d.sBranchCd "
                + " LEFT JOIN check_payments e "
                + "   ON b.sSourceNo = e.sTransNox "
                + " LEFT JOIN Banks f "
                + "   ON f.sbankIdxx = e.sBankidxx "
                + " LEFT JOIN Check_Payments g "
                + "   ON b.sSourceNo = g.ssourceNo "
                + " LEFT JOIN Payee k "
                + "   ON k.sPayeeIDx = g.sPayeeIDx ";

//        + " GROUP BY a.sTransNox";
    }

    public JSONObject SearchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        List<String> lsFilter = new ArrayList<>();
        
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
           lsFilter.add( "  a.cTranStat IN (" + lsTransStat.substring(2) + ")");
        } else {
            lsFilter.add("  a.cTranStat = " + SQLUtil.toSQL(psTranStat));
        }
        initSQL();
//        String lsFilterCondition = String.join(" AND ", "a.sDestinat = " + SQLUtil.toSQL(fsDistinantion),
//                " a.sTransNox = " + SQLUtil.toSQL(fsTransNo),
//                " a.dTransact = " + SQLUtil.toSQL(fsdateTransact));
//
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        String lsSQL = SQL_BROWSE;
        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "", 
                "Transaction Date»Transaction No»Branch»Payee»Payee",
                "a.dTransact»a.sTransNox»xBranchNm»k.sPayeeNme»f.sBankName",
                "a.dTransact»a.sTransNox»c.sBranchNm»k.sPayeeNme»f.sBankName",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    public JSONObject SearchTransaction(String lsValue, boolean byCode) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        String lsFilterCondition = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        if(byCode){
            lsFilterCondition = String.join(" AND ", "d.sBranchNm LIKE " + SQLUtil.toSQL(lsValue + "%"));
        }else{
            lsFilterCondition = String.join(" AND ", "a.sTransNox LIKE " + SQLUtil.toSQL("%" + lsValue + "%"));
        }
        

        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
//        String lsSQL = SQL_BROWSE;
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "", 
                "Transaction Date»Transaction No»Destination»Payee»Payee",
                "a.dTransact»a.sTransNox»xDestinat»k.sPayeeNme»f.sBankName",
                "a.dTransact»a.sTransNox»d.sBranchNm»k.sPayeeNme»f.sBankName",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject SearchPayee(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
//            Master().setPayeeID(object.getModel().getPayeeID());
        }

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
    public Model_Check_Transfer_Master Master() {
        return (Model_Check_Transfer_Master) poMaster;
    }

    @Override
    public Model_Check_Transfer_Detail Detail(int row) {
        return (Model_Check_Transfer_Detail) paDetail.get(row);
    }

    public Model_Check_Payments poCheckMaster(int row) {
        return (Model_Check_Payments) paChecks.get(row);
    }

    @Override
    public JSONObject willSave() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        boolean lbUpdated = false;
        boolean hasReverse = false;
        //remove items with no stockid or quantity order
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();

            Object rawValue = item.getValue("sSourceNo");

            if (rawValue == null || rawValue.toString().trim().isEmpty()) {
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
            Detail(lnCtr).setSourceCode(Detail(lnCtr).CheckPayment().getSourceCode());
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());

            /**
             * Checks whether the current disbursement transaction is associated with a
             * replenishment request. If a replenishment record is found, its transaction
             * number is added to the detail payload under the {@code replenishment_request}
             * key. If no replenishment record is found, the current detail is skipped.
             *  @author TEEJEI DE CELIS
             *  @since 2026-09-09
             *  @approvedBy Grace De Guzman
             */

            JSONObject loJSON = isReplenishmentSource(Master().getTransactionNo());

            if ("success".equals(loJSON.get("result"))) {
                JSONObject loPayload = new JSONObject();
                loPayload.put("replenishment_request", loJSON.get("RepTransNox"));
                Detail(lnCtr).setPayload(loPayload.toJSONString());
            } else {
                continue;
            }

        if (Detail(lnCtr).isReverse()) {
                hasReverse = true; // at least one is reversed
            }
        }
        if (!hasReverse) {
            poJSON.put("result", "error");
            poJSON.put("message", " Cannot save the transaction. \nAt least one detail must be marked as reversed (active).");
            return poJSON;
        }
        if(getEditMode() == EditMode.ADDNEW){
            Master().setPreparedBy(poGRider.getUserID());
            Master().setPreparedDate(poGRider.getSysDate());
        }

        if (CheckTransferStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = setValueToOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        if (CheckTransferStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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
            }
        }
        
        System.out.println("=================START TEST SAVE====================");
            for (int lnCntr = 0; lnCntr <= getDetailCount()- 1; lnCntr++) {    
                System.out.println("===============================================");
                System.out.println("No : " + Detail(lnCntr).getEntryNo());
                System.out.println("Transaction No. : " + Detail(lnCntr).getTransactionNo());
                System.out.println("Source No. : " + Detail(lnCntr).getSourceNo());
                System.out.println("REVERSE. : " + Detail(lnCntr).isReverse());
                System.out.println("===============================================");
            }
            System.out.println("==================END TEST SAVE=====================");

        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Checks whether the specified disbursement transaction is associated with
     * one or more replenishment request transactions.
     *
     * <p>
     * The method searches the {@code disbursement_detail} table for details
     * matching the specified source transaction number and source code
     * {@code PRFx}. If matching replenishment request transactions are found,
     * their transaction numbers are returned in the {@code RepTransNox} JSON
     * array.
     * </p>
     *
     * <p>
     * The returned JSON object contains:
     * <ul>
     *     <li>{@code result} - {@code success} if replenishment transactions
     *         are found; otherwise {@code error}.</li>
     *     <li>{@code RepTransNox} - A {@link JSONArray} containing the
     *         replenishment request transaction number(s) when found.</li>
     *     <li>{@code message} - An error message when no replenishment
     *         transaction is found.</li>
     * </ul>
     * </p>
     *
     * @param sourceNo the transaction number of the disbursement to be checked
     * @return a {@link JSONObject} containing the result and replenishment
     *         transaction number(s), if available
     * @throws SQLException if an error occurs while executing the SQL query
     * @throws GuanzonException if a Guanzon framework-related error occurs
     *
     * @author TEEJEI DE CELIS
     * @since 2026-09-09
     * @approvedBy Grace De Guzman
     */
    private JSONObject isReplenishmentSource(String sourceNo) throws SQLException, GuanzonException {
        JSONObject poJSON = new JSONObject();
        JSONArray laReplenishment = new JSONArray();

        String lsSQL = "SELECT b.sTransNox "
                + "FROM disbursement_detail a "
                + "LEFT JOIN replenishment_request b "
                + "    ON a.sSourceNo = b.sTransNox "
                + "WHERE a.sTransNox = " + SQLUtil.toSQL(sourceNo)
                + "  AND a.sSourceCd = 'PRFx' "
                + "  AND b.sTransNox IS NOT NULL";

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        while (loRS.next()) {
            laReplenishment.add(loRS.getString("sTransNox"));
        }

        MiscUtil.close(loRS);

        if (laReplenishment.size() > 0) {
            poJSON.put("result", "success");
            poJSON.put("RepTransNox", laReplenishment);
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No replenishment transaction found.");
        }

        return poJSON;
    }

    @Override
    public JSONObject save() {
        return isEntryOkay(CheckTransferStatus.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        poJSON = new JSONObject();
        int lnCtr;

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = (GValidator) new CheckTransferValidatorFactory();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        poJSON = loValidator.validate();
        return poJSON;
    }

    private Model_Check_Payments ChecksList() {
        return new CashflowModels(poGRider).CheckPayments();
    }

    public int getCheckCount() {
        if (paChecks == null) {
            return 0;
        }
        return paChecks.size();
    }
    
    public JSONObject getCheckTransferForPosting(String lsSource, String fsTransNo,LocalDate fddate) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        String lsSQL = "SELECT "
             + "  a.sTransNox, "
             + "  a.dTransact, "
             + "  a.sDestinat, "
             + "  b.sBranchNm, "
             + "  c.sDeptIDxx, "
             + "  c.sDeptName, "
             + "  LEFT(a.sTransNox, 4) AS source, "
             + "  d.sBranchNm, "
             + "  a.cTranStat "
             + " FROM "
             + "  Check_Transfer_Master a "
             + "  LEFT JOIN Branch b ON a.sDestinat = b.sBranchCd" 
             + " LEFT JOIN Department c on a.sDeptIDxx = c.sDeptIDxx"
             + " LEFT JOIN Branch d on  LEFT(a.sTransNox, 4) = d.sBranchCd";
        
        List<String> lsFilter = new ArrayList<>();

        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
           lsFilter.add( "  a.cTranStat IN (" + lsTransStat.substring(2) + ")");
        } else {
            lsFilter.add("  a.cTranStat = " + SQLUtil.toSQL(psTranStat));
        }
        

        if (lsSource != null && !lsSource.trim().isEmpty()) {
            lsFilter.add(" d.sBranchNm = " + SQLUtil.toSQL(lsSource));
        }
        if (fsTransNo != null && !fsTransNo.trim().isEmpty()) {
            lsFilter.add(" a.sTransNox  LIKE " + SQLUtil.toSQL("%" + fsTransNo));
        }

        if (fddate != null ) {
            lsFilter.add("a.dTransact = " + SQLUtil.toSQL(java.sql.Date.valueOf(fddate)));
        }
        
        lsFilter.add("a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode()));
  

        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }

        
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY a.dTransact DESC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            poCheckTransferMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                poCheckTransferMaster.add(CheckTransferMasterList());
                poCheckTransferMaster.get(poCheckTransferMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            poCheckTransferMaster = new ArrayList<>();
            poCheckTransferMaster.add(CheckTransferMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    public JSONObject getCheckTransfer(String fsDestination, String fsTransNo,LocalDate fddate) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        String lsSQL = "SELECT "
             + "  a.sTransNox, "
             + "  a.dTransact, "
             + "  a.sDestinat, "
             + "  b.sBranchNm, "
             + "  a.cTranStat "
             + " FROM "
             + "  Check_Transfer_Master a "
             + "  LEFT JOIN Branch b ON a.sDestinat = b.sBranchCd";
        
        List<String> lsFilter = new ArrayList<>();

        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
           lsFilter.add( "  a.cTranStat IN (" + lsTransStat.substring(2) + ")");
        } else {
            lsFilter.add("  a.cTranStat = " + SQLUtil.toSQL(psTranStat));
        }
        

        if (fsDestination != null && !fsDestination.trim().isEmpty()) {
            lsFilter.add(" b.sBranchNm LIKE " + SQLUtil.toSQL(fsDestination + "%"));
        }
        if (fsTransNo != null && !fsTransNo.trim().isEmpty()) {
            lsFilter.add(" a.sTransNox  LIKE " + SQLUtil.toSQL("%" + fsTransNo));
        }

        if (fddate != null ) {
            lsFilter.add("a.dTransact = " + SQLUtil.toSQL(java.sql.Date.valueOf(fddate)));
        }
        
        
  

        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }

        
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY a.dTransact DESC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            poCheckTransferMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                poCheckTransferMaster.add(CheckTransferMasterList());
                poCheckTransferMaster.get(poCheckTransferMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            poCheckTransferMaster = new ArrayList<>();
            poCheckTransferMaster.add(CheckTransferMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }
    
    /**
     * Loads check payment records based on the provided filtering criteria.
     *
     * <p>
     * This method dynamically builds an SQL query to retrieve check payment
     * transactions from the {@code Check_Payments} table, including related
     * information from {@code Banks}, {@code Payee}, and
     * {@code Disbursement_Master}. The query supports filtering by check number
     * (bank name), payee name, and check date range.</p>
     *
     * <p>
     * <b>Note:</b> The {@code Disbursement_Master} table was not originally
     * part of this query. It was later included due to a business requirement
     * related to disbursement BR (Business Rule) adjustments.</p>
     *
     * <p>
     * The following conditions are always applied:
     * <ul>
     * <li>Check is not released ({@code a.cReleased = '0'})</li>
     * <li>Transaction status is not equal to 3 ({@code a.cTranStat <> 3})</li>
     * <li>Check is marked for printing ({@code a.cPrintxxx = '1'})</li>
     * <li>Only disbursement type {@code '0'} is included
     * ({@code c.cDisbrsTp = '0'})</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Important:</b> The condition {@code c.cDisbrsTp = '0'} is explicitly
     * applied because {@code c.cDisbrsTp = '1'} represents <i>check
     * deposit</i>, which must be excluded from this result set.</p>
     *
     * <p>
     * If records are found, they are loaded into the {@code paChecks} list by
     * calling {@code ChecksList()} and {@code openRecord()} using the
     * transaction number. Otherwise, an empty list is initialized.</p>
     *
     * @param fscheckno the check number or bank name filter (prefix matching
     * using LIKE). If {@code null} or empty, this filter is ignored.
     * @param fspayee the payee name filter (prefix matching using LIKE). If
     * {@code null} or empty, this filter is ignored.
     * @param dateFrom the start date for filtering check dates (inclusive).
     * Must be used together with {@code dateThru}. If {@code null}, date
     * filtering is ignored.
     * @param dateThru the end date for filtering check dates (inclusive). Must
     * be used together with {@code dateFrom}. If {@code null}, date filtering
     * is ignored.
     *
     * @return a {@code JSONObject} containing the result of the operation:
     * <ul>
     * <li>{@code result} - "success" if records are found, otherwise
     * "error"</li>
     * <li>{@code message} - descriptive message of the operation result</li>
     * <li>{@code continue} - (optional) flag indicating continuation on
     * error</li>
     * </ul>
     *
     * @throws SQLException if a database access error occurs during query
     * execution
     * @throws GuanzonException if an application-specific error occurs
     *
     * @author Teejei De Celis
     * @date Modified: April 10, 2026
     *
     * @see SQLUtil#toSQL(Object)
     * @see MiscUtil#RecordCount(ResultSet)
     * @see MiscUtil#close(ResultSet)
     */
    public JSONObject loadCheckPayment(String Bank, LocalDate dateFrom, LocalDate dateThru) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsSQL = "SELECT DISTINCT "
                + "  a.sTransNox, "
                + "  a.sBranchCd, "
                + "  a.sBankIDxx, "
                + "  a.sPayeeIDx, "
                + "  a.dCheckDte, "
                + "  a.sCheckNox, "
                + "  a.nAmountxx, "
                + "  b.sBankName AS bankname, "
                + "  d.sPayeeNme AS payeename, "
                + "  a.cReleased, "
                + "  a.cTranStat, "
                + "  a.sSourceNo, "
                + "  a.sSourceCd, "
                + "  c.cDisbrsTp  "
                + " FROM "
                + "  Check_Payments a "
                + "  LEFT JOIN Banks b ON a.sBankIDxx = b.sBankIDxx "
                + "  LEFT JOIN Payee d ON a.sPayeeIDx = d.sPayeeIDx "
                + "  LEFT JOIN Disbursement_Master c ON a.sSourceNo = c.sTransNox ";

        // Build filter conditions dynamically
        List<String> lsFilter = new ArrayList<>();

        if (Bank != null && !Bank.trim().isEmpty()) {
            lsFilter.add("b.sBankName LIKE " + SQLUtil.toSQL(Bank + "%"));
        }

        if (dateFrom != null && dateThru != null) {
            lsFilter.add("a.dCheckDte BETWEEN "
                    + SQLUtil.toSQL(java.sql.Date.valueOf(dateFrom))
                    + " AND "
                    + SQLUtil.toSQL(java.sql.Date.valueOf(dateThru)));
        }

        lsFilter.add("a.cReleased = '0' AND a.cTranStat <> 3 AND a.cPrintxxx = '1' AND c.cDisbrsTp = '0'");

        // Append WHERE clause if any filter exists
        if (lsSQL != null && !lsSQL.trim().isEmpty() && lsFilter != null && !lsFilter.isEmpty()) {
            lsSQL += " WHERE " + String.join(" AND ", lsFilter);
        }

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paChecks = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sPrtclrID: " + loRS.getString("sTransNox"));
                System.out.println("sBranchCd: " + loRS.getString("sPayeeIDx"));
                System.out.println("sBranchCd: " + loRS.getString("sCheckNox"));
                System.out.println("------------------------------------------------------------------------------");

                paChecks.add(ChecksList());
                paChecks.get(paChecks.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            paChecks = new ArrayList<>();
            paChecks.add(ChecksList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }
    
    public JSONObject addCheckPaymentToDetail(String chekTransaction) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        int lnRow = 0;
        CheckPayments poCheckPayments;
//        psAccountNo = AcctNo;
//        psParticularID = particularNo;

        // Initialize RecurringIssuance and load the record
        poCheckPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
        poJSON = poCheckPayments.openRecord(chekTransaction);
        if ("error".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", (String) poJSON.get("message"));
            return poJSON;
        }

        // Check if the particular already exists in the details
        for (lnRow = 0; lnRow < getDetailCount(); lnRow++) {
            // Skip if the particular ID is empty
            if (Detail(lnRow).getSourceNo() == null || Detail(lnRow).getSourceNo().isEmpty()) {
                continue;
            }

            // Compare with the current record's particular ID
            if (Detail(lnRow).getSourceNo().equals(poCheckPayments.getModel().getTransactionNo())) {
                lbExist = true;
                break; // Stop checking once a match is found
            }
        }

        if (!lbExist) {
            // Make sure you're writing to an empty row
            Detail(getDetailCount() - 1).setSourceNo(poCheckPayments.getModel().getTransactionNo());
            //Detail(getDetailCount() - 1).setSource("your source code here"); //enable this when setting the source code

            // Only add the detail if it's not empty
            if (Detail(getDetailCount() - 1).getSourceNo() != null && !Detail(getDetailCount() - 1).getSourceNo().isEmpty()) {
                AddDetail();
            }
        } else {
           if (!Detail(lnRow).isReverse()) {
                Detail(lnRow).isReverse(true);
                poJSON.put("result", "success");
                return poJSON;
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Checkpayment: " + Detail(lnRow).getSourceNo() + " already exists in table at row " + (lnRow + 1) + ".");
                poJSON.put("tableRow", lnRow);
                poJSON.put("warning", "false");
                return poJSON;
            }
        }

        // Return success
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject computeMasterFields() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        double totalAmount = 0.0000;

        for (int lnCtr = 0; lnCtr < getDetailCount()-1; lnCtr++) {

            // include only reversed ("+")
            if (Detail(lnCtr).isReverse()) {
                totalAmount += Detail(lnCtr).CheckPayment().getAmount();
            }
        }

        Master().setTransactionTotal(totalAmount);
        return poJSON;
    }

    private Model_Check_Transfer_Master CheckTransferMasterList() {
        return new CashflowModels(poGRider).CheckTransferMaster();
    }


    public int getCheckTransferMasterCount() {
        return this.poCheckTransferMaster.size();
    }

    public Model_Check_Transfer_Master poCheckTransferMaster(int row) {
        return (Model_Check_Transfer_Master) poCheckTransferMaster.get(row);
    }
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        paChecks = new ArrayList<>();
//
        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            String fsCheckTransNo = Detail(lnCtr).getSourceNo();
            updateCheckPayments(fsCheckTransNo,status);
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    private CheckPayments CheckPayments() throws GuanzonException, SQLException {
        return new CashflowControllers(poGRider, logwrapr).CheckPayments();
    }
    private JSONObject saveUpdates()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnCtr;
        for (lnCtr = 0; lnCtr <= poChecks.size() - 1; lnCtr++) {

            poChecks.get(lnCtr).setWithParentClass(true);
            poChecks.get(lnCtr).poModel.getReleased();
            poJSON = poChecks.get(lnCtr).poModel.saveRecord();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    private void updateCheckPayments(String fsCheckTransNo,String fsstatus)
            throws GuanzonException, SQLException, CloneNotSupportedException {

        CheckPayments issuance = CheckPayments();
        poChecks.add(issuance);

        System.out.printf(fsCheckTransNo);

        JSONObject record = issuance.poModel.openRecord(fsCheckTransNo);

        issuance.poModel.updateRecord();
        
        if(CheckTransferStatus.CONFIRMED.equals(fsstatus)){
        // Set updated values
            issuance.poModel.setReleased("1");
            issuance.poModel.setBranchCode(Master().getDestination());
            issuance.poModel.setLocation("4");
            issuance.poModel.setModifyingId(poGRider.getUserID());
            issuance.poModel.setModifiedDate(poGRider.getServerDate());
        }else if(CheckTransferStatus.VOID.equals(fsstatus)){
            issuance.poModel.setReleased("0");
            issuance.poModel.setBranchCode(Master().getDestination());
            issuance.poModel.setLocation("1");
            issuance.poModel.setModifyingId(poGRider.getUserID());
            issuance.poModel.setModifiedDate(poGRider.getServerDate());
        }else if(CheckTransferStatus.POSTED.equals(fsstatus)){
            issuance.poModel.setReleased("1");
            issuance.poModel.setBranchCode(Master().getDestination());
            issuance.poModel.setLocation("1");
            issuance.poModel.setModifyingId(poGRider.getUserID());
            issuance.poModel.setModifiedDate(poGRider.getServerDate());
        }
    }
    
        private JSONObject receivedCheck(String fstransNox)
            throws GuanzonException, SQLException, CloneNotSupportedException {
        poJSON = new JSONObject();

        
           for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++){
                Detail(lnCtr).isReceived();
            }
        poJSON = SaveTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
        }
        return poJSON;

    }
    
    public JSONObject printTransaction() {
        if(!Master().getTransactionStatus().equals(CheckTransferStatus.CONFIRMED)
            && !Master().getTransactionStatus().equals(CheckTransferStatus.POSTED)){
            JSONObject loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "Transaction is not yet Confirm. \nPlease Confirm the transaction before printing");
            return loJSON;
        }
        
        if(Master().isPrintedStatus()){
            try {
                JSONObject loJSON = new JSONObject();
                loJSON = seekApproval();
                if("error".equals(loJSON.get("result"))){
                    return loJSON;
                }
            } catch (SQLException | GuanzonException ex) {
                Logger.getLogger(CheckReleases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            
//            System.out.println("Company Address : " + Master().Company().getCompanyAddress());
//            System.out.println("Company Town : " + Master().Company().TownCity().getDescription());
//            System.out.println("Company Province " + Master().Company().TownCity().Province().getDescription() );
//            System.out.println("Branch Address : " + Master().Branch().getAddress());
//            System.out.println("Branch Town : " + Master().Branch().TownCity().getDescription());
//            System.out.println("Branch Province " + Master().Branch().TownCity().Province().getDescription() );
            
            String lsCompanyAddress = "";
//            if(Master().Company().getCompanyAddress() != null && !"".equals(Master().Company().getCompanyAddress())){
//                lsCompanyAddress  = Master().Company().getCompanyAddress().trim();
//            }
//            if(Master().Company().TownCity().getDescription() != null && !"".equals(Master().Company().TownCity().getDescription())){
//                lsCompanyAddress  = lsCompanyAddress + " " + Master().Company().TownCity().getDescription().trim();
//            }
//            if(Master().Company().TownCity().Province().getDescription() != null && !"".equals(Master().Company().TownCity().Province().getDescription())){
//                lsCompanyAddress  = lsCompanyAddress + ", " + Master().Company().TownCity().Province().getDescription().trim();
//            }
            
            //Branch / Destination
            String lsDestinationAddress = "";
            if(Master().Branch().getAddress() != null && !"".equals(Master().Branch().getAddress())){
                lsDestinationAddress = Master().Branch().getAddress().trim();
            }
            if(Master().Branch().TownCity().getDescription() != null && !"".equals(Master().Branch().TownCity().getDescription())){
                lsDestinationAddress = lsDestinationAddress + " " + Master().Branch().TownCity().getDescription().trim();
            }
            if(Master().Branch().TownCity().Province().getDescription() != null && !"".equals(Master().Branch().TownCity().Province().getDescription())){
                lsDestinationAddress = lsDestinationAddress + ", " + Master().Branch().TownCity().Province().getDescription().trim();
            }
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sCompnyNm", ""); 
            parameters.put("sConfirmed", ""); 
            
            JSONObject loJSONEntry = getEntryBy();
            if("error".equals((String) loJSONEntry.get("result"))){
                return loJSONEntry;
            }
            if((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))){
                parameters.put("sCompnyNm",(String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte"))); 
            }
            
            JSONObject loJSONConfirm = getConfirmedBy();
            if("error".equals((String) loJSONConfirm.get("result"))){
                return loJSONConfirm;
            } else {
                if((String) loJSONConfirm.get("sConfirmed") != null && !"".equals((String) loJSONConfirm.get("sConfirmed"))){
                    parameters.put("sConfirmed",  (String) loJSONConfirm.get("sConfirmed") + " " + String.valueOf((String) loJSONConfirm.get("sConfrmDte"))); 
                }
            }
            
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", lsCompanyAddress);
//            parameters.put("sCompnyNm", "Prepared by: "+ poGRider.getLogName()+ " " + poGRider.getServerDate()); //poGRider.getClientName()
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("sDestinat", Master().BranchDestination().getBranchName());            
            parameters.put("sDeptIDxx", Master().Department().getDescription());
            parameters.put("sDestinationAddress", lsDestinationAddress);
            parameters.put("sRemarksx", Master().getRemarks());
            //set default value
            parameters.put("sApprval1",""); 
            parameters.put("sApprval2", "");
            parameters.put("sApprval3", "");
            //Update value when approved
            if(Master().getTransactionStatus().equals(CheckTransferStatus.POSTED)){
                List<String> lsList = getApprover();
                for(int lnCtr = 0;lnCtr <= lsList.size() - 1;lnCtr++){
                    parameters.put("sApprval"+(lnCtr+1),lsList.get(lnCtr)); 
                }
            }
            
            parameters.put("dTransact", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));

            switch (Master().getTransactionStatus()) {
                case CheckTransferStatus.POSTED:
                case CheckTransferStatus.CONFIRMED:
                    if ("1".equals(Master().isPrintedStatus())) {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approvedreprint.png";
                    } else {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
                    }
                    break;
                case CheckTransferStatus.VOID:
                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\cancelled.png";
                    break;
            }
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0000;
            
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                if (!Detail(lnCtr).isReverse()) continue;
                orderDetails.add(new OrderDetail(lnCtr + 1,
                    safeString(Detail(lnCtr).getSourceNo()), // Source No
                        safeString(Detail(lnCtr).CheckPayment().getCheckNo()), // Barcode
                         safeString(Detail(lnCtr).CheckPayment().Payee().getPayeeName()), // Brand
                        safeString(Detail(lnCtr).getRemarks())
                ));
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            String jrxmlPath = "";
            jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\CheckTransfer.jrxml";
            
           

            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(jasperReport,parameters,dataSource);

            CustomJasperViewer viewer = new CustomJasperViewer(jasperPrint);
            viewer.setVisible(true);

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
    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }
    public static class OrderDetail {
        private Integer nRowNo;
        private String sSourceNo;
        private String sCheckNox;
        private String sPayeeIDx;
        private String sRemarksx;

        public OrderDetail(Integer rowNo,String checkTransNo, String checkno, String payee, String remarks) {
            this.nRowNo = rowNo;
            this.sSourceNo = checkTransNo;
            this.sCheckNox = checkno;
            this.sPayeeIDx = payee;
            this.sRemarksx = remarks;
            
        }
        public Integer getnRowNo() {
            return nRowNo;
        }

        
        public String getsSourceNo() {
            return sSourceNo;
        }

        public String getsCheckNox() {
            return sCheckNox;
        }

        public String getsPayeeIDx() {
            return sPayeeIDx;
        }

        public String getsRemarksx() {
            return sRemarksx;
        }


    }

    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        private void customizePrintButton(JasperPrint jasperPrint) {
            poJSON = new JSONObject();
            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    System.out.println("JRViewer not found!");
                    return;
                }

                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {
                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {
                                JButton button = (JButton) toolbar.getComponent(j);

                                //if ever na kailangan e hide si button save
//                                if (button.getToolTipText() != null) {
//                                    if (button.getToolTipText().equals("Save")) {
//                                        button.setEnabled(false);  // Disable instead of hiding
//                                        button.setVisible(false);  // Hide it completely
//                                    }
//                                }
                                if ("Print".equals(button.getToolTipText())) {
                                    for (ActionListener al : button.getActionListeners()) {
                                        button.removeActionListener(al);
                                    }
                                    button.addActionListener(e -> {
                                        try {
                                            boolean isPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                            if (isPrinted) {
                                                PrintTransaction(true);
                                            } else {
                                                Platform.runLater(() -> {
                                                    ShowMessageFX.Warning("Printing was canceled by the user.", "Print Purchase Order", null);
                                                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());

                                                });
                                            }
                                        } catch (JRException ex) {
                                            Platform.runLater(() -> {
                                                ShowMessageFX.Warning("Print Failed: " + ex.getMessage(), "Computerized Accounting System", null);
                                                SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                                            });
                                        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                                            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    });
                                }
                            }
                        }

                        // Force UI refresh after hiding the button
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
            }
        }

        private void PrintTransaction(boolean fbIsPrinted) throws SQLException, CloneNotSupportedException, GuanzonException {
            poJSON = new JSONObject();
            if (fbIsPrinted) {
                if (((String) poMaster.getValue("cTranStat")).equals(PurchaseOrderStatus.APPROVED)) {
                    poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    poMaster.setValue("dModified", poGRider.getServerDate());
                    poMaster.setValue("sModified", poGRider.getUserID());
                    poMaster.setValue("cPrintxxx", Logical.YES);

                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed successfully.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            } else {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed aborted.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            }
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] components = ((Container) parent).getComponents();
                for (Component component : components) {
                    JRViewer viewer = findJRViewer(component);
                    if (viewer != null) {
                        return viewer;
                    }
                }
            }
            return null;
        }
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
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM Check_Transfer_Master a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
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
    
    public JSONObject getConfirmedBy() throws SQLException, GuanzonException {
        String lsConfirm = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM Check_Transfer_Master a "
                     + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = 'Check_Transfer_Master' "
                     + " AND ( b.cRefrStat = "+ SQLUtil.toSQL(CheckTransferStatus.CONFIRMED) 
                     + " OR (ASCII(b.cRefrStat) - 64)  = "+ SQLUtil.toSQL(CheckTransferStatus.CONFIRMED) + " )";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsConfirm = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsConfirm = getSysUser(loRS.getString("sModified")); 
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
        poJSON.put("sConfirmed", lsConfirm);
        poJSON.put("sConfrmDte", lsDate);
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
                case CheckTransferStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case CheckTransferStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case CheckTransferStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case CheckTransferStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case CheckTransferStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                 case CheckTransferStatus.RETURN:
                    crs.updateString("cRefrStat", "RETURN");
                    break;   
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case CheckTransferStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case CheckTransferStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "VERIFIED");
                            break;
                        case CheckTransferStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case CheckTransferStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case CheckTransferStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case CheckTransferStatus.RETURN:
                            crs.updateString("cRefrStat", "RETURN");
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
        
        showStatusHistoryUI("Check Transfer", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public List<String> getApprover() throws SQLException{
        List<String> lsList = new ArrayList<String>();
            String lsSQL =   " SELECT "
                        + "     a.sSourceCD "
                        + " ,	a.sSourceNo "
                        + " ,	a.cTranStat "
                        + " ,	d.sCompnyNm "
                        + " ,	c.dApproved "
                        + " ,   CONCAT(c.sEmployID, ' - ',d.sCompnyNm,' ',c.dApproved) AS sApprover "
                        + " FROM Transaction_Authorization_Master a  "
                        + " ,	Transaction_Authorization_Detail b     "
                        + " ,	Transaction_Authorization_Recipient c  "
                        + " LEFT JOIN Client_Master d ON c.sEmployID = d.sClientID "
                        + " WHERE a.sTransNox = b.sSourceNo AND b.sSourceNo = c.sSourceNo    "
                        + " AND a.sSourceCD = " + SQLUtil.toSQL(getSourceCode())
                        + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                        + " AND a.cTranStat = '2' "
                        + " AND c.cTranStat = '1' ";
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lsList.add(loRS.getString("sApprover"));
                }
                MiscUtil.close(loRS);
            }
            
        return lsList;
    }
        /**
     * Returns the selected date from a DatePicker as LocalDateTime,
     * combining it with the current system time. If no date is selected,
     * returns the current date and time.
     *
     * Example return: 2026-03-06T09:30:29 (ready for DATETIME storage)
     */
    public static LocalDateTime getDateTime(LocalDate selectedDate) {
        LocalDateTime now = LocalDateTime.now();

        if (selectedDate != null) {
           
            return selectedDate.atTime(now.getHour(), now.getMinute(), now.getSecond());
        }
        
        return now; // no selection → current timestamp
    }
    public void updateDateTime(LocalDate selectedDate) {
        // Call the static method and store the result in pdDateTime
        this.pdDateTime = getDateTime(selectedDate);
    }
    
    
    
    private boolean hasRightToSave() {
      return true;
    }
    
    @Override
    protected JSONObject saveTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        this.poJSON = new JSONObject();
        if (!this.pbInitTran) {
            this.poJSON.put("result", "error");
            this.poJSON.put("message", "Object is not initialized.");
            return this.poJSON;
        }
        if (this.pnEditMode == 1) {
            this.poJSON.put("result", "error");
            this.poJSON.put("message", "Saving of unmodified transaction is not allowed.");
            return this.poJSON;
        }
        if (!hasRightToSave()) {
            this.poJSON.put("result", "error");
            this.poJSON.put("message", "User has no right to save.");
            return this.poJSON;
        }

        this.poJSON = willSave();
        if ("error".equals(this.poJSON.get("result"))) {
            return this.poJSON;
        }

        if (getEditMode() == 0 || getEditMode() == 2) {
            this.pdModified = this.poGRider.getServerDate();
            this.poMaster.setValue("sModified", this.poGRider.Encrypt(this.poGRider.getUserID()));
        }

        if (!this.pbWthParent) {
            this.poGRider.beginTrans((String) this.poEvent.get("event"), this.poMaster.getTable(), this.SOURCE_CODE, String.valueOf(this.poMaster.getValue(1)));
        }

        this.poJSON = save();

        if ("success".equals(this.poJSON.get("result"))) {

            if (this.pbVerifyEntryNo) {
                this.poMaster.setValue("nEntryNox", Integer.valueOf(this.paDetail.size()));
            }

            if (this.pnEditMode == 0 || this.pnEditMode == 2) {

                this.poMaster.setValue("dModified", this.pdModified);
                this.poJSON = this.poMaster.saveRecord();

                if ("error".equals(this.poJSON.get("result"))) {
                    if (!this.pbWthParent) {
                        this.poGRider.rollbackTrans();
                    }
                    return this.poJSON;
                }

                if (this.pnEditMode == 0) {

                    for (int lnCtr = 0; lnCtr <= this.paDetail.size() - 1; lnCtr++) {
                        ((Model) this.paDetail.get(lnCtr)).setValue("dModified", this.pdModified);
                        this.poJSON = ((Model) this.paDetail.get(lnCtr)).saveRecord();

                        if ("error".equals(this.poJSON.get("result"))) {
                            if (!this.pbWthParent) {
                                this.poGRider.rollbackTrans();
                            }
                            return this.poJSON;
                        }
                    }

                } else {

                    String lsSQL = "SELECT * FROM " + this.poDetail.getTable()
                            + " WHERE sTransNox = "
                            + SQLUtil.toSQL(this.poMaster.getValue(this.poDetail.getColumn("sTransNox")))
                            + " ORDER BY nEntryNox";

                    ResultSet loRS = this.poGRider.executeQuery(lsSQL);
                    long lnRow = MiscUtil.RecordCount(loRS);

                    if (lnRow <= this.paDetail.size()) {

                        for (int lnCtr = 0; lnCtr <= this.paDetail.size() - 1; lnCtr++) {

                            boolean lbDelete = false;

                            if (lnCtr <= lnRow - 1L) {

                                this.poDetail.openRecord(
                                        (String) this.poMaster.getValue(this.poDetail.getColumn("sTransNox")),
                                        lnCtr + 1
                                );

                                Object loOld = this.poDetail.getValue(this.poDetail.getColumn("sSourceNo"));
                                Object loNew = ((Model) this.paDetail.get(lnCtr)).getValue(this.poDetail.getColumn("sSourceNo"));

                                if (loOld == null || !loOld.equals(loNew)) {
                                    lbDelete = true;
                                }
                            }

                            if (lbDelete) {

                                lsSQL = "DELETE FROM " + this.poDetail.getTable()
                                        + " WHERE sTransNox = "
                                        + SQLUtil.toSQL(this.poMaster.getValue(this.poDetail.getColumn("sTransNox")))
                                        + " AND nEntryNox = " + SQLUtil.toSQL(lnCtr + 1);

                                if (this.poGRider.executeQuery(lsSQL, this.poDetail.getTable(), this.psBranchCode, this.psDestination, "") <= 0L) {
                                    if (!this.pbWthParent) {
                                        this.poGRider.rollbackTrans();
                                    }
                                    this.poJSON.put("result", "error");
                                    this.poJSON.put("message", "Unable to remove old records.");
                                    return this.poJSON;
                                }

                                ((Model) this.paDetail.get(lnCtr)).setEditMode(0);

                            } else {

                                if (lnCtr <= lnRow - 1L) {
                                    ((Model) this.paDetail.get(lnCtr)).setEditMode(2);
                                } else {
                                    ((Model) this.paDetail.get(lnCtr)).setEditMode(0);
                                }
                            }

                            ((Model) this.paDetail.get(lnCtr)).setValue("dModified", this.pdModified);
                            this.poJSON = ((Model) this.paDetail.get(lnCtr)).saveRecord();

                            if ("error".equals(this.poJSON.get("result"))) {
                                if (!this.pbWthParent) {
                                    this.poGRider.rollbackTrans();
                                }
                                return this.poJSON;
                            }
                        }

                    } else {

                        lsSQL = "DELETE FROM " + this.poDetail.getTable()
                                + " WHERE sTransNox = "
                                + SQLUtil.toSQL(this.poMaster.getValue(this.poDetail.getColumn("sTransNox")))
                                + " AND nEntryNox > " + this.paDetail.size();

                        if (this.poGRider.executeQuery(lsSQL, this.poDetail.getTable(), this.psBranchCode, this.psDestination, "") <= 0L) {
                            if (!this.pbWthParent) {
                                this.poGRider.rollbackTrans();
                            }
                            this.poJSON.put("result", "error");
                            this.poJSON.put("message", "Unable to remove old records.");
                            return this.poJSON;
                        }
                    }
                }

            } else {

                if (!this.pbWthParent) {
                    this.poGRider.rollbackTrans();
                }

                this.poJSON.put("result", "error");
                this.poJSON.put("message", "Edit mode is not allowed to save transaction.");
                return this.poJSON;
            }

        } else {

            if (!this.pbWthParent) {
                this.poGRider.rollbackTrans();
            }

            return this.poJSON;
        }

        this.poJSON = saveOthers();

        if ("error".equals(this.poJSON.get("result"))) {
            if (!this.pbWthParent) {
                this.poGRider.rollbackTrans();
            }
            return this.poJSON;
        }

        if (!this.pbWthParent) {
            this.poGRider.commitTrans();
        }

        saveComplete();
        this.pnEditMode = -1;
        this.pbRecordExist = true;

        this.poJSON = new JSONObject();
        this.poJSON.put("result", "success");
        this.poJSON.put("message", "Transaction saved successfully.");

        return this.poJSON;
    }
    
    public JSONObject seekApproval() throws SQLException, GuanzonException {
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
        }
    poJSON.put("result", "success");
    return poJSON;
    }

}

