package ph.com.guanzongroup.cas.cashflow.validator;

import java.util.ArrayList;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.iface.GValidator;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Detail;
import ph.com.guanzongroup.cas.cashflow.model.Model_Payment_Request_Master;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;

public class PaymentRequestValidator implements GValidator{
    GRiderCAS poGrider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_Payment_Request_Master poMaster;
    ArrayList<Model_Payment_Request_Detail> poDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGrider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_Payment_Request_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        poDetail.clear();
        for(int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++){
            poDetail.add((Model_Payment_Request_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        switch (psTranStat){
            case PaymentRequestStatus.OPEN:
                return validateNew();
            case PaymentRequestStatus.CONFIRMED:
                return validateConfirmed();
            case PaymentRequestStatus.PAID:
                return validatePaid();
            case PaymentRequestStatus.CANCELLED:
                return validateCancelled();
            case PaymentRequestStatus.VOID:
                return validateVoid();
            case PaymentRequestStatus.POSTED:
                return validatePosted();
            case PaymentRequestStatus.RETURNED:
                return validateReturned();
            default:
                poJSON = new JSONObject();
                poJSON.put("result", "success");
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew(){
        poJSON = new JSONObject();
        
        
        if (poMaster.getBranchCode()== null || poMaster.getBranchCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Branch");
            return poJSON;
        }
        
//        if (poGrider.isMainOffice() || poGrider.isWarehouse()){
//            if (poMaster.getDepartmentID()== null || poMaster.getDepartmentID().isEmpty()) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "Department is not set");
//                return poJSON;
//            }
//        }
        
        if (poMaster.getPayeeID()== null || "".equals(poMaster.getPayeeID())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Payee information is missing or not set.");
            return poJSON;
        }
        
        if (poMaster.getRemarks() == null || "".equals(poMaster.getRemarks())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Remarks cannot be empty.");
            return poJSON;
        }
        
//        if (!poGrider.isMainOffice() || !poGrider.isWarehouse()){
//            if (poMaster.getSeriesNo()== null || poMaster.getSeriesNo().isEmpty()) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "Series No is not set");
//                return poJSON;
//            }
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateConfirmed(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePaid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateCancelled(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    
    private JSONObject validateVoid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePosted(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateReturned(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
}
