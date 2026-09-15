/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Check_Payments;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

/**
 *
 * @author Guillier
 * 
 * NOTE: Procedures on initialize should be properly arranged to avoid conflicts on saving and fetching
 */
public class Model_Check_Release_Detail extends Model{

    private Model_Check_Payments poCheckPayment;

    @Override
    public void initialize() {
        
        try{
            
            //Step 1. get XML metada file, to get temporary data getter and setter for sql table and columns
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            //Step 2. get last row inserted and move to last row
            poEntity.last();
            poEntity.moveToInsertRow();

            //Step 3. initialize row
            MiscUtil.initRowSet(poEntity);
            
            //Step 4. insert new row from initialized row
            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            //Step 5. move to last initialized row
            poEntity.absolute(1);
            
            //Step 6. set row properties
            poEntity.updateObject("nEntryNox", 1);
            poEntity.updateObject("cReversex", "+");
            poEntity.updateObject("dModified", poGRider.getServerDate());

            //Step 7. get primary id from metadata, and initialized to variable as row id
            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(2);

            pnEditMode = EditMode.UNKNOWN;
            
        }catch(SQLException e){
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }
    
    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }
    
    //sSourceCd
    public JSONObject setSourceCD(String sSourceCd) {
        return setValue("sSourceCd", sSourceCd);
    }

    public String getSourceCD() {
        return (String) getValue("sSourceCd");
    }
    
    //sSourceNo
    public JSONObject setSourceNo(String sSourceNo) {
        return setValue("sSourceNo", sSourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }
    
    //dModified
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
    
    public Model_Check_Payments CheckPayment() throws SQLException, GuanzonException {
        if (poCheckPayment == null) {
            poCheckPayment = new CashflowModels(poGRider).CheckPayments();
        }
        if (!"".equals(getValue("sSourceNo"))) {
            if (this.poCheckPayment.getEditMode() == 1 && this.poCheckPayment
                    .getTransactionNo().equals(getValue("sSourceNo"))) {
                return this.poCheckPayment;
            }
            this.poJSON = this.poCheckPayment.openRecord((String) getValue("sSourceNo"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poCheckPayment;
            }
            this.poCheckPayment.initialize();
            return this.poCheckPayment;
        }
        poCheckPayment.initialize();
        return this.poCheckPayment;
    }
    
}
