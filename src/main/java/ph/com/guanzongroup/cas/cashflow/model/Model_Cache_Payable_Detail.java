package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Inv_Type;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_Cache_Payable_Detail extends Model {    
    
    
    Model_Inv_Type poInvType;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("nEntryNox");
            poEntity.updateObject("nGrossAmt", 0.00);
            poEntity.updateObject("nDiscAmtx", 0.00);
            poEntity.updateObject("nDeductnx", 0.00);
            poEntity.updateObject("nPayables", 0.00);
            poEntity.updateObject("nRecvbles", 0.00);
            poEntity.updateObject("nAmtPaidx", 0.00);
            
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            ID = "sTransNox";
            ID2 = "nEntryNox";
            
            //poInvType is intentionally NOT constructed here - see InvType() below, which
            //builds it lazily on first access so opening this record never touches Inv_Type.

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNumber(int entryNumber){
        return setValue("nEntryNox", entryNumber);
    }

    public int getEntryNumber() {
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setTransactionType(String transactionType) {
        return setValue("sTranType", transactionType);
    }

    public String getTransactionType() {
        return (String) getValue("sTranType");
    }
    
    public JSONObject setGrossAmount(Number amount){
        return setValue("nGrossAmt", amount);
    }

    public double getGrossAmount() {
        return Double.parseDouble(String.valueOf(getValue("nGrossAmt")));
    }
    
    public JSONObject setDiscountAmount(Number amount){
        return setValue("nDiscAmtx", amount);
    }

    public double getDiscountAmount() {
        return Double.parseDouble(String.valueOf(getValue("nDiscAmtx")));
    }
    
    public JSONObject setDeductionAmount(Number amount){
        return setValue("nDeductnx", amount);
    }

    public double getDeductionAmount() {
        return Double.parseDouble(String.valueOf(getValue("nDeductnx")));
    }
    
    public JSONObject setPayables(Number amount){
        return setValue("nPayables", amount);
    }

    public double getPayables() {
        return Double.parseDouble(String.valueOf(getValue("nPayables")));
    }
    
    public JSONObject setReceivables(Number amount){
        return setValue("nRecvbles", amount);
    }

    public double getReceivables() {
        return Double.parseDouble(String.valueOf(getValue("nRecvbles")));
    }
    
    public JSONObject setAmountPaid(Number amount){
        return setValue("nAmtPaidx", amount);
    }

    public double getAmountPaid() {
        return Double.parseDouble(String.valueOf(getValue("nAmtPaidx")));
    }
        
    @Override
    public String getNextCode(){
        return ""; 
    }
    
    public Model_Inv_Type InvType() throws SQLException, GuanzonException {
        if (poInvType == null) {
            poInvType = new ParamModels(poGRider).InventoryType();
        }
        if (!"".equals((String) getValue("sTranType"))) {
            if (poInvType.getEditMode() == EditMode.READY
                    && poInvType.getInventoryTypeId().equals((String) getValue("sTranType"))) {
                return poInvType;
            } else {
                if (ReferenceCache.tryLoad("Inv_Type", (String) getValue("sTranType"), poInvType)) {
                    return poInvType;
                }

                poJSON = poInvType.openRecord((String) getValue("sTranType"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Inv_Type", (String) getValue("sTranType"), poInvType);
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
}