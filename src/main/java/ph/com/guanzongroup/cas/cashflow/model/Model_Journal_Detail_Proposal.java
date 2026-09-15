package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Account_Chart;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_Journal_Detail_Proposal extends Model {
    Model_Account_Chart poAccountChart;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateObject("dForMonth", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nDebitAmt", 0.0000);
            poEntity.updateObject("nCredtAmt", 0.0000);
            poEntity.updateObject("cReversex", "+");
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            ID3 = "sAcctCode";

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
    
    public JSONObject setEntryNumber(int entryNo){
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNumber() {
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setAccountCode(String accountCode) {
        return setValue("sAcctCode", accountCode);
    }

    public String getAccountCode() {
        return (String) getValue("sAcctCode");
    }
    
    public JSONObject setDebitAmount(double amount){
        return setValue("nDebitAmt", amount);
    }

    public double getDebitAmount() {
        return Double.parseDouble(String.valueOf(getValue("nDebitAmt")));
    }
    
    public JSONObject setCreditAmount(double amount){
        return setValue("nCredtAmt", amount);
    }

    public double getCreditAmount() {
        return Double.parseDouble(String.valueOf(getValue("nCredtAmt")));
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }
    
    public JSONObject setForMonthOf(Date date){
        return setValue("dForMonth", date);
    }

    public Date getForMonthOf() {
        return (Date) getValue("dForMonth");
    }
        
    @Override
    public String getNextCode(){
        return ""; 
    }
    
    public Model_Account_Chart Account_Chart() throws SQLException, GuanzonException{
        if (poAccountChart == null) {
            poAccountChart = new ParamModels(poGRider).AccountChart();
        }
        if (!"".equals((String) getValue("sAcctCode"))){
            if (poAccountChart.getEditMode() == EditMode.READY &&
                poAccountChart.getAccountCode().equals((String) getValue("sAcctCode")))
                return poAccountChart;
            else{
                if (ReferenceCache.tryLoad("Account_Chart", (String) getValue("sAcctCode"), poAccountChart)) {
                    return poAccountChart;
                }
                poJSON = poAccountChart.openRecord((String) getValue("sAcctCode"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Account_Chart", (String) getValue("sAcctCode"), poAccountChart);
                    return poAccountChart;
                }
                else {
                    poAccountChart.initialize();
                    return poAccountChart;
                }
            }
        } else {
            poAccountChart.initialize();
            return poAccountChart;
        }
    }
}