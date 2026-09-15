package ph.com.guanzongroup.cas.cashflow;

import java.sql.SQLException;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Parameter;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.parameter.AccountChart;
import org.guanzon.cas.parameter.TaxCode;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.model.Model_Withholding_Tax;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;

public class WithholdingTax extends Parameter {

    Model_Withholding_Tax poModel;

    @Override
    public void initialize() throws SQLException, GuanzonException {
        psRecdStat = Logical.YES;

        CashflowModels model = new CashflowModels(poGRider);
        poModel = model.Withholding_Tax();

        super.initialize();
    }

    @Override
    public JSONObject isEntryOkay() throws SQLException {
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() < UserRight.SYSADMIN) {
            poJSON.put("result", "error");
            poJSON.put("message", "User is not allowed to save record.");
            return poJSON;
        } else {
            poJSON = new JSONObject();

            if (poModel.getAccountCode().isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Account code must not be empty.");
                return poJSON;
            }

            if (poModel.getDescription() == null || poModel.getDescription().isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Description must not be empty.");
                return poJSON;
            }

            if (poModel.getTaxRate() <= 0.0000) {
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid tax rate.");
                return poJSON;
            }

            if (poModel.getTaxType() == null || poModel.getTaxType().isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Tax Type must not be empty.");
                return poJSON;
            }

            if (poModel.getTaxCode() == null || poModel.getTaxCode().isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Tax Code must not be empty.");
                return poJSON;
            }

            if (poModel.getAccountCode() == null || poModel.getAccountCode().isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Account Code must not be empty.");
                return poJSON;
            }
        }

        poModel.setModifyingBy(poGRider.Encrypt(poGRider.getUserID()));
        poModel.setModifiedDate(poGRider.getServerDate());

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public Model_Withholding_Tax getModel() {
        return poModel;
    }

    @Override
    public JSONObject searchRecord(String value, boolean byCode) throws SQLException, GuanzonException {
        String lsSQL = getSQ_Browse();

        lsSQL = lsSQL + " GROUP BY a.sTaxRteID ";
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Description»Tax Code»Account",
                "sTaxRteID»sTaxDescr»sATaxCode»xAccountx",
                "a.sTaxRteID»a.sTaxDescr»a.sATaxCode»IFNULL(b.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sTaxRteID"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject SearchAccountName(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        AccountChart loObject = new ParamControllers(poGRider, logwrapr).AccountChart();
        loObject.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = loObject.searchRecordByIndustry(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            poModel.setAccountCode(loObject.getModel().getAccountCode());
        }
        return poJSON;
    }

    public JSONObject SearchTaxCode(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        TaxCode object = new ParamControllers(poGRider, logwrapr).TaxCode();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            poModel.setTaxCode(object.getModel().getTaxCode());
        }
        return poJSON;
    }
    private String psIndustryId = "";

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public JSONObject searchRecord(String value, boolean byCode, String taxCode) throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), " a.sATaxCode = " + SQLUtil.toSQL(taxCode));

        if (psIndustryId != null && !"".equals(psIndustryId)) {
            lsSQL = lsSQL + " AND b.sIndstCde = " + SQLUtil.toSQL(psIndustryId);
        }

        System.out.println("WT Account : " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Account»Tax Code»Description",
                "sTaxRteID»xAccountx»sATaxCode»sTaxDescr",
                "a.sTaxRteID»IFNULL(b.sDescript, '')»a.sATaxCode»a.sTaxDescr",
                byCode ? 0 : 1);

        if (poJSON != null) {
            return poModel.openRecord((String) poJSON.get("sTaxRteID"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    @Override
    public String getSQ_Browse() {
        String lsCondition = "";

        if (psRecdStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
            }

            lsCondition = "a.cRecdStat IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "a.cRecdStat = " + SQLUtil.toSQL(psRecdStat);
        }

        String lsSQL = "SELECT"
                + "  a.sTaxRteID"
                + ", a.sTaxDescr"
                + ", a.sATaxCode"
                + ", a.cRecdStat"
                + ", IFNULL(b.sDescript, '') xAccountx"
                + " FROM Withholding_Tax a"
                + " LEFT JOIN Account_Chart b ON a.sAcctCode = b.sAcctCode";

        return MiscUtil.addCondition(lsSQL, lsCondition);
    }
}
