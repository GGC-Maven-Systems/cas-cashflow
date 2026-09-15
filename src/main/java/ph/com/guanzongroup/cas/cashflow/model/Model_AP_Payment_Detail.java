/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.APPaymentAdjustment;
import ph.com.guanzongroup.cas.cashflow.CachePayable;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 *
 * @author Aldrich && Arsiela Team 2 05232025
 */
public class Model_AP_Payment_Detail extends Model {

    //reference objects
    Model_Payment_Request_Master poPaymentRequest;
    Model_POR_Master poPOReceiving;
    Model_POReturn_Master poPOReturn;
    Model_AP_Payment_Adjustment poAPAdjustment;
    
    CachePayable poCachePayableTrans;
    PurchaseOrderReceiving poPOReceivingTrans;
    APPaymentAdjustment poAPAdjustmentTrans;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
//            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nDebitAmt", 0.0000);
            poEntity.updateObject("nCredtAmt", 0.0000);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nAppliedx", 0.0000);
            poEntity.updateObject("nAmtPaidx", 0.0000);
            poEntity.updateObject("cReversex", "+");
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

            //initialize reference objects (non-Model transaction wrappers only - the
            //services.Model reference fields below (poPaymentRequest, poAPAdjustment,
            //poPOReceiving, poPOReturn) are intentionally NOT constructed here - see their
            //accessor methods, which build them lazily on first access)
            CashflowControllers glController = new CashflowControllers(poGRider, logwrapr);
            poCachePayableTrans = glController.CachePayable();
            poAPAdjustmentTrans = glController.APPaymentAdjustment();

            PurchaseOrderReceivingControllers PORController = new PurchaseOrderReceivingControllers(poGRider, logwrapr);
            poPOReceivingTrans = PORController.PurchaseOrderReceiving();
            //end - initialize reference objects

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        } catch (GuanzonException ex) {
            Logger.getLogger(Model_AP_Payment_Detail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(Number entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    public JSONObject setTransactionTotal(Number transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Number getTransactionTotal() {
        if (getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))) {
            return 0.0000;
        }
        return (Number) getValue("nTranTotl");
    }

    public JSONObject setDebitAmount(Number debitAmount) {
        return setValue("nDebitAmt", debitAmount);
    }

    public Number getDebitAmount() {
        if (getValue("nDebitAmt") == null || "".equals(getValue("nDebitAmt"))) {
            return 0.0000;
        }
        return (Number) getValue("nDebitAmt");
    }

    public JSONObject setCreditAmount(Number creditAmount) {
        return setValue("nCredtAmt", creditAmount);
    }

    public Number getCreditAmount() {
        if (getValue("nCredtAmt") == null || "".equals(getValue("nCredtAmt"))) {
            return 0.0000;
        }
        return (Number) getValue("nCredtAmt");
    }

    public JSONObject setAppliedAmount(Number appliedAmount) {
        return setValue("nAppliedx", appliedAmount);
    }

    public Number getAppliedAmount() {
        if (getValue("nAppliedx") == null || "".equals(getValue("nAppliedx"))) {
            return 0.0000;
        }
        return (Number) getValue("nAppliedx");
    }

    public JSONObject setAmountPaid(Number amountPaid) {
        return setValue("nAmtPaidx", amountPaid);
    }

    public Number getAmountPaid() {
        if (getValue("nAmtPaidx") == null || "".equals(getValue("nAmtPaidx"))) {
            return 0.0000;
        }
        return (Number) getValue("nAmtPaidx");
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return "";
    }

    //reference object models
    public Model_Payment_Request_Master PaymentRequestMaster() throws SQLException, GuanzonException {
        if (poPaymentRequest == null) {
            poPaymentRequest = new CashflowModels(poGRider).PaymentRequestMaster();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPaymentRequest.getEditMode() == EditMode.READY
                    && poPaymentRequest.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPaymentRequest;
            } else {
                poJSON = poPaymentRequest.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPaymentRequest;
                } else {
                    poPaymentRequest.initialize();
                    return poPaymentRequest;
                }
            }
        } else {
            poPaymentRequest.initialize();
            return poPaymentRequest;
        }
    }
    
//    public Model_Cache_Payable_Master CachePayableMaster() throws SQLException, GuanzonException {
//        if (!"".equals((String) getValue("sSourceNo"))) {
//            if (poCachePayable.getEditMode() == EditMode.READY
//                    && poCachePayable.getTransactionNo().equals((String) getValue("sSourceNo"))) {
//                return poCachePayable;
//            } else {
//                poJSON = poCachePayable.openRecord((String) getValue("sSourceNo"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poCachePayable;
//                } else {
//                    poCachePayable.initialize();
//                    return poCachePayable;
//                }
//            }
//        } else {
//            poCachePayable.initialize();
//            return poCachePayable;
//        }
//    }
//    
//    public CachePayable CachePayable() throws SQLException, GuanzonException, CloneNotSupportedException {
//        if (!"".equals((String) getValue("sSourceNo"))) {
//            if (poCachePayableTrans.getEditMode() == EditMode.READY
//                    && poCachePayableTrans.Master().getTransactionNo().equals((String) getValue("sSourceNo"))) {
//                return poCachePayableTrans;
//            } else {
//                poJSON = poCachePayableTrans.OpenTransaction((String) getValue("sSourceNo"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poCachePayableTrans;
//                } else {
//                    poCachePayableTrans.InitTransaction();
//                    return poCachePayableTrans;
//                }
//            }
//        } else {
//            poCachePayableTrans.InitTransaction();
//            return poCachePayableTrans;
//        }
//    }
    
    public Model_AP_Payment_Adjustment APPaymentAdjustmentMaster() throws SQLException, GuanzonException {
        if (poAPAdjustment == null) {
            poAPAdjustment = new CashflowModels(poGRider).APPaymentAdjustment();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poAPAdjustment.getEditMode() == EditMode.READY
                    && poAPAdjustment.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poAPAdjustment;
            } else {
                poJSON = poAPAdjustment.openRecord((String) getValue("sSourceNo"));

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
    
    public APPaymentAdjustment APPaymentAdjustment() throws SQLException, GuanzonException, CloneNotSupportedException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poAPAdjustmentTrans.getEditMode() == EditMode.READY
                    && poAPAdjustmentTrans.getModel().getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poAPAdjustmentTrans;
            } else {
                poJSON = poAPAdjustmentTrans.OpenTransaction((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poAPAdjustmentTrans;
                } else {
                    poAPAdjustmentTrans.initialize();
                    return poAPAdjustmentTrans;
                }
            }
        } else {
            poAPAdjustmentTrans.initialize();
            return poAPAdjustmentTrans;
        }
    }
    
    public Model_POR_Master PurchasOrderReceivingMaster() throws SQLException, GuanzonException {
        if (poPOReceiving == null) {
            poPOReceiving = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPOReceiving.getEditMode() == EditMode.READY
                    && poPOReceiving.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPOReceiving;
            } else {
                poJSON = poPOReceiving.openRecord((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOReceiving;
                } else {
                    poPOReceiving.initialize();
                    return poPOReceiving;
                }
            }
        } else {
            poPOReceiving.initialize();
            return poPOReceiving;
        }
    }
    
    public Model_POReturn_Master PurchasOrderReturnMaster() throws SQLException, GuanzonException {
        if (poPOReturn == null) {
            poPOReturn = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
        }
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPOReturn.getEditMode() == EditMode.READY
                    && poPOReturn.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPOReturn;
            } else {
                poJSON = poPOReturn.openRecord((String) getValue("sSourceNo"));

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
    
    public PurchaseOrderReceiving PurchaseOrderReceiving() throws SQLException, GuanzonException, CloneNotSupportedException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPOReceivingTrans.getEditMode() == EditMode.READY
                    && poPOReceivingTrans.Master().getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPOReceivingTrans;
            } else {
                poJSON = poPOReceivingTrans.OpenTransaction((String) getValue("sSourceNo"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOReceivingTrans;
                } else {
                    poPOReceivingTrans.InitTransaction();
                    return poPOReceivingTrans;
                }
            }
        } else {
            poPOReceivingTrans.InitTransaction();
            return poPOReceivingTrans;
        }
    }
    //end reference object models
}
