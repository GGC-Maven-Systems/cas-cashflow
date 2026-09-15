/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Inv_Type;
import org.guanzon.cas.parameter.model.Model_Tax_Code;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;
import ph.com.guanzongroup.cas.cashflow.status.SOATaggingStatic;

/**
 *
 * @author User
 */
public class Model_Disbursement_Detail extends Model {

    Model_Particular poParticular;
    Model_Inv_Type poInvType;
    Model_Tax_Code poTaxCode;
    Model_Payment_Request_Master poPRF;
    Model_POR_Master poPOR;
    Model_POReturn_Master poPOReturn;
    Model_AP_Payment_Adjustment poAPAdjustment;
    Model_AP_Payment_Master poAPMaster;
    Model_AP_Payment_Detail poAPDetail;
    String InvType = "";

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateObject("nEntryNox", DisbursementStatic.DefaultValues.default_value_integer);
            poEntity.updateObject("nAmountxx", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nDetVatSl", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nDetVatRa", DisbursementStatic.DefaultValues.default_value_double);
            poEntity.updateObject("nDetVatAm", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nDetZroVa", DisbursementStatic.DefaultValues.default_value_double_0000);            
            poEntity.updateObject("nDetVatEx", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nAmtAppld", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nTaxRatex", DisbursementStatic.DefaultValues.default_value_double);
            poEntity.updateObject("nTaxAmtxx", DisbursementStatic.DefaultValues.default_value_double_0000);
            
            poEntity.updateObject("nDetailNo", DisbursementStatic.DefaultValues.default_value_integer);
//            poEntity.updateObject("cReversex", "+");

            //end - assign default values
            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

            //end - initialize reference objects
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setDetailNo(int detailNo) {
        return setValue("nDetailNo", detailNo);
    }

    public int getDetailNo() {
        return (int) getValue("nDetailNo");
    }

    public JSONObject setDetailSource(String detailSource) {
        return setValue("sDetlSrce", detailSource);
    }

    public String getDetailSource() {
        return (String) getValue("sDetlSrce");
    }

    public JSONObject setParticularID(String particular) {
        return setValue("sPrtclrID", particular);
    }

    public String getParticularID() {
        return (String) getValue("sPrtclrID");
    }

    public JSONObject setAmount(double amount) {
        return setValue("nAmountxx", amount);
    }

    public double getAmount() {
        return Double.parseDouble(String.valueOf(getValue("nAmountxx")));
    }

    public JSONObject setAmountApplied(double amountApplied) {
        return setValue("nAmtAppld", amountApplied);
    }

    public double getAmountApplied() {
        return Double.parseDouble(String.valueOf(getValue("nAmtAppld")));
    }

    public JSONObject isWithVat(boolean iswithvat) {
        return setValue("cWithVATx", iswithvat ? "1" : "0");
    }

    public boolean isWithVat() {
        Object value = getValue("cWithVATx");
        return "1".equals(String.valueOf(value));
    }

    public JSONObject setDetailVatSales(double detailVatSales) {
        return setValue("nDetVatSl", detailVatSales);
    }

    public double getDetailVatSales() {
        return Double.parseDouble(String.valueOf(getValue("nDetVatSl")));
    }

    public JSONObject setDetailVatRates(double detailVatRates) {
        return setValue("nDetVatRa", detailVatRates);
    }

    public double getDetailVatRates() {
        return Double.parseDouble(String.valueOf(getValue("nDetVatRa")));
    }

    public JSONObject setDetailVatAmount(double detailVatAmount) {
        return setValue("nDetVatAm", detailVatAmount);
    }

    public double getDetailVatAmount() {
        return Double.parseDouble(String.valueOf(getValue("nDetVatAm")));
    }

    public JSONObject setDetailZeroVat(double detailZeroVat) {
        return setValue("nDetZroVa", detailZeroVat);
    }

    public double getDetailZeroVat() {
        return Double.parseDouble(String.valueOf(getValue("nDetZroVa")));
    }

    public JSONObject setDetailVatExempt(double detailVatExempt) {
        return setValue("nDetVatEx", detailVatExempt);
    }

    public double getDetailVatExempt() {
        return Double.parseDouble(String.valueOf(getValue("nDetVatEx")));
    }

    public JSONObject setTaxCode(String taxCode) {
        return setValue("sTaxCodex", taxCode);
    }

    public String getTaxCode() {
        return (String) getValue("sTaxCodex");
    }

    public JSONObject setTaxRates(double taxRates) {
        return setValue("nTaxRatex", taxRates);
    }

    public double getTaxRates() {
        return Double.parseDouble(String.valueOf(getValue("nTaxRatex")));
    }

    public JSONObject setTaxAmount(double taxamount) {
        return setValue("nTaxAmtxx", taxamount);
    }
    
//    public JSONObject isReverse(boolean isReverse) {
//        return setValue("cReversex", isReverse ? "+" : "-");
//    }
//
//    public boolean isReverse() {
//        return ((String) getValue("cReversex")).equals("+");
//    }
    
//    public JSONObject isReverse(boolean fIsZeroOutAmount){
//        poJSON = new JSONObject();
//        if(fIsZeroOutAmount){
//            setAmountApplied(0.0000);
//        } else {
//            setAmountApplied(getAmount());
//        }
//        
//        return poJSON;
//    }
//    
//    public boolean isReverse() {
//        return (getAmountApplied() > 0.0000);
//    }

    public double getTaxAmount() {
        return Double.parseDouble(String.valueOf(getValue("nTaxAmtxx")));
    }

    public String setInvType(String invType) {
        return this.InvType = invType;
    }

    public String getInvType() {
        return this.InvType;
    }
    
    public JSONObject setDetailAdvances(String fsTransactionNo) {
        return getAdvancesAmount(fsTransactionNo);
    }
    
    private double pdblAdvancesAmount = 0.0000;
    public double getDetailAdvances() {
        return pdblAdvancesAmount;
    }
    
    private double pdblDiscountAmount = 0.0000;
    public double getDetailSourceDiscount() {
        return pdblDiscountAmount;
    }
    
    private JSONObject getAdvancesAmount(String fsTransactionNo){
        poJSON = new JSONObject();
        pdblAdvancesAmount = 0.0000;
        pdblDiscountAmount = 0.0000;
        try {
            List<String> llistPurchaseOrder = new ArrayList<>();
            String lsPOTransNo = "";
            switch(getSourceCode()){
                case DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE:
                    if(SOATaggingStatic.PaymentRequest.equals(SOADetail().getSourceCode())){
                        if(DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(SOADetail().PaymentRequestMaster().getSourceCode())){
                            lsPOTransNo = SOADetail().PaymentRequestMaster().getSourceNo();
                        }
                    } else if(SOATaggingStatic.POReceiving.equals(SOADetail().getSourceCode())){
//                        pdblAdvancesAmount += SOADetail().PurchasOrderReceivingMaster().getAmountPaid().doubleValue(); //Only get the amount paid reflected at PO Receiving during POSTING
                        poJSON = getPOAdvancesInPOReceiving(getDetailSource(),fsTransactionNo);
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        return poJSON;
                    }
                break;
    //            case DisbursementStatic.SourceCode.AP_ADJUSTMENT:
                case DisbursementStatic.SourceCode.PAYMENT_REQUEST:
                    if(DisbursementStatic.SourceCode.PURCHASE_ORDER.equals(PRF().getSourceCode())){
                        lsPOTransNo = PRF().getSourceNo();
                    }
                    break;
                case DisbursementStatic.SourceCode.PO_RECEIVING:
//                    pdblAdvancesAmount += POReceiving().getAmountPaid().doubleValue();  //Only get the amount paid reflected at PO Receiving during POSTING //conflict in Disbursement Voucher History.
                    poJSON = getPOAdvancesInPOReceiving(getSourceNo(),fsTransactionNo);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    return poJSON;
            }

            if(!llistPurchaseOrder.contains(lsPOTransNo)){
                llistPurchaseOrder.add(lsPOTransNo);
            }

            poJSON = getAdvancePayment(llistPurchaseOrder,fsTransactionNo);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        } 
        return poJSON;
    }
    
    //Need to observe computation of advances - already concerned to project manager's
    private JSONObject getAdvancePayment(List<String> faPOTransNo, String fsTransactionNo) throws SQLException, GuanzonException {
        String lsSQL = "";
        try {
            for(int lnCtr = 0; lnCtr < faPOTransNo.size(); lnCtr++){
                lsSQL =  " SELECT e.sTransNox, SUM(e.nAmtPaidx) AS nPRFPaidx, SUM(e.nTranTotl) AS nPRFTotal, SUM(e.nDiscAmtx) AS nPRFDiscx FROM Disbursement_Master a " +
                        " LEFT JOIN Disbursement_Detail b ON b.sTransNox = a.sTransNox   " +
                        " LEFT JOIN AP_Payment_Master c ON c.sTransNox = b.sSourceNo AND b.sSourceCd = "+ SQLUtil.toSQL(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE)+
                        " LEFT JOIN AP_Payment_Detail d ON d.sTransNox = c.sTransNox  " +
                        " LEFT JOIN Payment_Request_Master e ON (e.sTransNox = b.sSourceNo AND b.sSourceCd = " + SQLUtil.toSQL(DisbursementStatic.SourceCode.PAYMENT_REQUEST)
                            + " ) OR (e.sTransNox = b.sDetlSrce AND e.sTransNox = d.sSourceNo AND d.sSourceCd =  " + SQLUtil.toSQL(DisbursementStatic.SourceCode.PAYMENT_REQUEST)
                            + " )  " ;
                lsSQL = MiscUtil.addCondition(lsSQL,
                        " e.sSourceNo = " + SQLUtil.toSQL(faPOTransNo.get(lnCtr))
                        + " AND e.sSourceCd = " + SQLUtil.toSQL(DisbursementStatic.SourceCode.PURCHASE_ORDER)
                        + " AND a.sTransNox != " + SQLUtil.toSQL(fsTransactionNo) 
                );
                lsSQL = lsSQL + " GROUP BY e.sTransNox ";
                System.out.println("Executing SQL: " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                if (MiscUtil.RecordCount(loRS) >= 0) {
                    while (loRS.next()) {
                        if(loRS.getDouble("nPRFPaidx") > 0.0000){
                            // Print the result set
                            System.out.println("--------------------------PRF--------------------------");
                            System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                            System.out.println("nPRFPaidx: " + loRS.getDouble("nPRFPaidx"));
                            System.out.println("nPRFTotal: " + loRS.getDouble("nPRFTotal"));
                            System.out.println("nPRFDiscx: " + loRS.getDouble("nPRFDiscx"));
                            System.out.println("------------------------------------------------------------------------------");
                            pdblAdvancesAmount += loRS.getDouble("nPRFTotal");
                            pdblDiscountAmount += loRS.getDouble("nPRFDiscx");
                        }
                    }
                }
                MiscUtil.close(loRS);
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
//    private JSONObject getAdvancePayment(List<String> faPOTransNo) throws SQLException, GuanzonException {
//        Model_PO_Master loObject = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
//        for(int lnCtr = 0; lnCtr < faPOTransNo.size(); lnCtr++){
//            if(faPOTransNo.get(lnCtr) != null && !"".equals(faPOTransNo.get(lnCtr))){
//                poJSON = loObject.openRecord(faPOTransNo.get(lnCtr));
//                if ("error".equals((String) poJSON.get("result"))) {
//                    poJSON.put("message",(String) poJSON.get("message") + "\nWhile reloading PO Purchase Order.");
//                    return poJSON;
//                }
//                
//                poJSON = setPRFDiscount(faPOTransNo.get(lnCtr));
//                if ("error".equals((String) poJSON.get("result"))) {
//                    poJSON.put("message",(String) poJSON.get("message") + "\nWhile reloading PO Purchase Order.");
//                    return poJSON;
//                }
//                pdblDiscountAmount += (Double) poJSON.get("discount");
//                pdblAdvancesAmount += (loObject.getAmountPaid().doubleValue());//  - (Double) poJSON.get("discount"));
//            }
//        }
//        
//        poJSON.put("result", "success");
//        poJSON.put("message", "success");
//        return poJSON;
//    }
//    
//    private JSONObject setPRFDiscount(String fsPOTransNo){
//        String lsSQL = "";
//        double ldDiscount = 0.0000;
//        try {
//            Model_Payment_Request_Master loModel = new CashflowModels(poGRider).PaymentRequestMaster();
//            loModel.initialize();
//            lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loModel),
//                    " sSourceNo = " + SQLUtil.toSQL(fsPOTransNo)
//                    + " AND sSourceCd = " + SQLUtil.toSQL(DisbursementStatic.SourceCode.PURCHASE_ORDER)
//                    + " AND cTranStat = " + SQLUtil.toSQL(PaymentRequestStatus.PAID) //get PAID in PRF by default since dv does not allow to modify Applied Amount so it must be based on paid amount in PRF
//            );
//            System.out.println("Executing SQL: " + lsSQL);
//            ResultSet loRS = poGRider.executeQuery(lsSQL);
//            poJSON = new JSONObject();
//            if (MiscUtil.RecordCount(loRS) >= 0) {
//                while (loRS.next()) {
//                    // Print the result set
//                    System.out.println("--------------------------PRF--------------------------");
//                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
//                    System.out.println("nAmtPaidx: " + loRS.getDouble("nAmtPaidx"));
//                    System.out.println("nDiscAmtx: " + loRS.getDouble("nDiscAmtx"));
//                    System.out.println("------------------------------------------------------------------------------");
//                    ldDiscount = ldDiscount + loRS.getDouble("nDiscAmtx"); 
//                }
//            }
//            MiscUtil.close(loRS);
//        } catch (SQLException e) {
//            poJSON.put("discount", ldDiscount);
//            poJSON.put("result", "error");
//            poJSON.put("message", e.getMessage());
//        }
//        poJSON.put("discount", ldDiscount);
//        poJSON.put("result", "success");
//        poJSON.put("message", "success");
//        return poJSON;
//    }
    
    private JSONObject getPOAdvancesInPOReceiving(String fsTransNo,String fsTransactionNo) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        Model_POR_Master loObject = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poJSON = loObject.openRecord(fsTransNo);
        if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("message", (String) poJSON.get("message") + "\nWhile reloading PO Receiving Master." );
            return poJSON;
        }
        
        if(!PurchaseOrderReceivingStatus.Purpose.REGULAR.equals(loObject.getPurpose())){
            return poJSON;
        }
        List<String> llistPurchaseOrder = new ArrayList<>();
        for(int lnCtr = 0;lnCtr < loObject.getEntryNo();lnCtr++){
            Model_POR_Detail loDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
            poJSON = loDetail.openRecord(fsTransNo,(lnCtr+1) );
            if ("error".equals((String) poJSON.get("result"))) {
            poJSON.put("message",(String) poJSON.get("message") + "\nWhile reloading PO Receiving Detail.");
                return poJSON;
            }
            if(loDetail.getQuantity().doubleValue() > 0.0000){
                if(loDetail.getOrderNo() != null && !"".equals(loDetail.getOrderNo())){
                    if(!llistPurchaseOrder.contains(loDetail.getOrderNo())){
                        llistPurchaseOrder.add(loDetail.getOrderNo());
                    } 
                }
            }
        }
        
        poJSON = getAdvancePayment(llistPurchaseOrder,fsTransactionNo);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //Get Amount Paid from PO that not yet reflect on previous PO Receiving
        //1. Check Previous PO Receiving of PO that is already PAID
        //2. Check the PO Qty and Amount reflected on PAID PO Receiving
        for(int lnCtr = 0; lnCtr < llistPurchaseOrder.size(); lnCtr++){
            double ldblPOAmtPaid = getPORPayment(llistPurchaseOrder.get(lnCtr),loObject); 
            if(ldblPOAmtPaid > 0.0000){
                if(pdblAdvancesAmount > ldblPOAmtPaid){
                    pdblAdvancesAmount = pdblAdvancesAmount - ldblPOAmtPaid;
                } else {
                    pdblAdvancesAmount = 0.0000;
                }
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    //Get Payment from PO Receiving
    private Double getPORPayment(String fsPOTransNo, Model_POR_Master foObjet) throws SQLException, GuanzonException {
        Double ldblAmtPaid = 0.0000;
        String lsSQL =  " SELECT SUM(b.nUnitPrce * b.nQuantity) AS nPOAmount, a.sTransNox AS sTransNox " +
                        " FROM PO_Receiving_Master a " +
                        " LEFT JOIN PO_Receiving_Detail b ON b.sTransNox = a.sTransNox ";
        try {
            lsSQL = MiscUtil.addCondition(lsSQL,
                    " b.sOrderNox = " + SQLUtil.toSQL(fsPOTransNo)
                    + " AND a.cPurposex = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.Purpose.REGULAR)
                    + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.PAID)
                    + " AND a.sTransNox != " + SQLUtil.toSQL(foObjet.getTransactionNo()) 
                    + " AND a.dTransact < " + SQLUtil.toSQL(xsDateShort(foObjet.getTransactionDate())) 
            );
            lsSQL = lsSQL + " GROUP BY a.sTransNox ";
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("--------------------------PREVIOUS PO RECEIVING--------------------------");
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("nPOAmount: " + loRS.getDouble("nPOAmount"));
                    System.out.println("------------------------------------------------------------------------------");
                    ldblAmtPaid += loRS.getDouble("nPOAmount");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            return 0.0000;
        }
        
        return ldblAmtPaid;
    }
    
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    public Model_Particular Particular() throws SQLException, GuanzonException {
        if (poParticular == null) {
            poParticular = new CashflowModels(poGRider).Particular();
        }
        if (!"".equals((String) getValue("sPrtclrID"))) {
            if (poParticular.getEditMode() == EditMode.READY
                    && poParticular.getParticularID().equals((String) getValue("sPrtclrID"))) {
                return poParticular;
            } else {
                poJSON = poParticular.openRecord((String) getValue("sPrtclrID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poParticular;
                } else {
                    poParticular.initialize();
                    return poParticular;
                }
            }
        } else {
            poParticular.initialize();
            return poParticular;
        }
    }

    public Model_Tax_Code TaxCode() throws SQLException, GuanzonException {
        if (poTaxCode == null) {
            poTaxCode = new ParamModels(poGRider).TaxCode();
        }
        if (!"".equals((String) getValue("sTaxCodex"))) {
            if (poTaxCode.getEditMode() == EditMode.READY
                    && poTaxCode.getTaxCode().equals((String) getValue("sTaxCodex"))) {
                return poTaxCode;
            } else {
                if (ReferenceCache.tryLoad("Tax_Code", (String) getValue("sTaxCodex"), poTaxCode)) {
                    return poTaxCode;
                }

                poJSON = poTaxCode.openRecord((String) getValue("sTaxCodex"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Tax_Code", (String) getValue("sTaxCodex"), poTaxCode);
                    return poTaxCode;
                } else {
                    poTaxCode.initialize();
                    return poTaxCode;
                }
            }
        } else {
            poTaxCode.initialize();
            return poTaxCode;
        }
    }

    public Model_Inv_Type InvType() throws SQLException, GuanzonException {
        if (poInvType == null) {
            poInvType = new ParamModels(poGRider).InventoryType();
        }
        if (!"".equals(InvType)) {
            if (poInvType.getEditMode() == EditMode.READY
                    && poInvType.getInventoryTypeId().equals(InvType)) {
                return poInvType;
            } else {
                if (ReferenceCache.tryLoad("Inv_Type", InvType, poInvType)) {
                    return poInvType;
                }

                poJSON = poInvType.openRecord(InvType);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Inv_Type", InvType, poInvType);
                    return poInvType;
                } else {
                    poInvType.initialize();
                    return poInvType;
                }
            }
        } else {
            poInvType.initialize();
            return poInvType;
        }
    }

    public Model_Payment_Request_Master PRF() throws SQLException, GuanzonException {
        if (poPRF == null) {
            poPRF = new CashflowModels(poGRider).PaymentRequestMaster();
        }
        String lsSourceNo = (String) getValue("sSourceNo");
        if(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals((String) getValue("sSourceCd"))){
            lsSourceNo = (String) getValue("sDetlSrce");
        }
        
        if (!"".equals(lsSourceNo)) {
            if ((poPRF.getEditMode() == EditMode.READY || poPRF.getEditMode() == EditMode.UPDATE)
                    && poPRF.getTransactionNo().equals(lsSourceNo)) {
                return poPRF;
            } else {
                poJSON = poPRF.openRecord(lsSourceNo);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPRF;
                } else {
                    poPRF.initialize();
                    return poPRF;
                }
            }
        } else {
            poPRF.initialize();
            return poPRF;
        }
    }

    public Model_POR_Master POReceiving() throws SQLException, GuanzonException {
        if (poPOR == null) {
            poPOR = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        }
        String lsSourceNo = (String) getValue("sSourceNo");
        if(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals((String) getValue("sSourceCd"))){
            lsSourceNo = (String) getValue("sDetlSrce");
        }
        
        if (!"".equals(lsSourceNo)) {
            if ((poPOR.getEditMode() == EditMode.READY || poPOR.getEditMode() == EditMode.UPDATE)
                    && poPOR.getTransactionNo().equals(lsSourceNo)) {
                return poPOR;
            } else {
                poJSON = poPOR.openRecord(lsSourceNo);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOR;
                } else {
                    poPOR.initialize();
                    return poPOR;
                }
            }
        } else {
            poPOR.initialize();
            return poPOR;
        }
    }
    
    public Model_POReturn_Master POReturn() throws SQLException, GuanzonException {
        if (poPOReturn == null) {
            poPOReturn = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
        }
        String lsSourceNo = (String) getValue("sSourceNo");
        if(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals((String) getValue("sSourceCd"))){
            lsSourceNo = (String) getValue("sDetlSrce");
        }
        
        if (!"".equals(lsSourceNo)) {
            if ((poPOReturn.getEditMode() == EditMode.READY || poPOReturn.getEditMode() == EditMode.UPDATE)
                    && poPOReturn.getTransactionNo().equals(lsSourceNo)) {
                return poPOReturn;
            } else {
                poJSON = poPOReturn.openRecord(lsSourceNo);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOReturn;
                } else {
                    poPOReturn.initialize();
                    return poPOReturn;
                }
            }
        } else {
            poPOReturn.initialize();
            return poPOReturn;
        }
    }


    public Model_AP_Payment_Detail SOADetail() throws SQLException, GuanzonException {
        if (poAPDetail == null) {
            poAPDetail = new CashflowModels(poGRider).SOATaggingDetails();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if ((poAPDetail.getEditMode() == EditMode.READY || poPOR.getEditMode() == EditMode.UPDATE)
                    && poAPDetail.getTransactionNo().equals((String) getValue("sSourceNo"))
                    && poAPDetail.getEntryNo().equals( getValue("nDetailNo"))) {
                return poAPDetail;
            } else {
                poJSON = poAPDetail.openRecord((String) getValue("sSourceNo"),  getValue("nDetailNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poAPDetail;
                } else {
                    poAPDetail.initialize();
                    return poAPDetail;
                }
            }
        } else {
            poAPDetail.initialize();
            return poAPDetail;
        }
    }
    
    public Model_AP_Payment_Master SOAMaster() throws SQLException, GuanzonException {
        if (poAPMaster == null) {
            poAPMaster = new CashflowModels(poGRider).SOATaggingMaster();
        }
        String lsSourceNo = (String) getValue("sSourceNo");
        if (!"".equals(lsSourceNo)) {
            if ((poAPMaster.getEditMode() == EditMode.READY || poAPMaster.getEditMode() == EditMode.UPDATE)
                    && poAPMaster.getTransactionNo().equals(lsSourceNo)) {
                return poAPMaster;
            } else {
                poJSON = poAPMaster.openRecord(lsSourceNo);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poAPMaster;
                } else {
                    poAPMaster.initialize();
                    return poAPMaster;
                }
            }
        } else {
            poAPMaster.initialize();
            return poAPMaster;
        }
    }
    
    public Model_AP_Payment_Adjustment APAdjustment() throws SQLException, GuanzonException {
        if (poAPAdjustment == null) {
            poAPAdjustment = new CashflowModels(poGRider).APPaymentAdjustment();
        }
        String lsSourceNo = (String) getValue("sSourceNo");
        if(DisbursementStatic.SourceCode.ACCOUNTS_PAYABLE.equals((String) getValue("sSourceCd"))){
            lsSourceNo = (String) getValue("sDetlSrce");
        }
        
        if (!"".equals(lsSourceNo)) {
            if ((poAPAdjustment.getEditMode() == EditMode.READY || poAPAdjustment.getEditMode() == EditMode.UPDATE)
                    && poAPAdjustment.getTransactionNo().equals(lsSourceNo)) {
                return poAPAdjustment;
            } else {
                poJSON = poAPAdjustment.openRecord(lsSourceNo);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poAPAdjustment;
                } else {
                    poAPAdjustment.initialize();
                    return poAPAdjustment;
                }
            }
        } else {
            poAPAdjustment.initialize();
            return poAPAdjustment;
        }
    }
}
