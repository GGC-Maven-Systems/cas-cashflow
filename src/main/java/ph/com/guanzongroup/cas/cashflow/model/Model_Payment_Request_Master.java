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
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.PaymentRequestStatus;

/**
 *
 * @author User
 */
public class Model_Payment_Request_Master extends Model {

    Model_Department poDepartment;
    Model_Payee poPayee;
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Recurring_Expense_Payment_Monitor poRecurringExpense;
    Model_PO_Master poPOMaster;
    private boolean isSummarized;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateString("cTranStat", PaymentRequestStatus.OPEN);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nDiscAmtx", 0.0000);
            poEntity.updateObject("nTaxAmntx", 0.0000);
            poEntity.updateObject("nNetTotal", 0.0000);
            poEntity.updateObject("nAmtPaidx", 0.0000);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("cProcessd", 0);
            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("sBranchCd", poGRider.getBranchCode());
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = "sTransNox";

            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poDepartment = model.Department();
            poBranch = model.Branch();
            poIndustry = model.Industry();
            poCompany = model.Company();
            
            CashflowModels cashFlow = new CashflowModels(poGRider);
            poPayee = cashFlow.Payee();
            poRecurringExpense = cashFlow.Recurring_Expense_Payment_Monitor();
            
            poPOMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();

            //end - initialize reference objects
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setIndustryID(String industryID) {
        return setValue("sIndstCdx", industryID);
    }

    public String getIndustryID() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setCompanyID(String companyID) {
        return setValue("sCompnyID", companyID);
    }

    public String getCompanyID() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setDepartmentID(String deptartmentID) {
        return setValue("sDeptIDxx", deptartmentID);
    }

    public String getDepartmentID() {
        return (String) getValue("sDeptIDxx");
    }

    public JSONObject setPayeeID(String payeeID) {
        return setValue("sPayeeIDx", payeeID);
    }

    public String getPayeeID() {
        return (String) getValue("sPayeeIDx");
    }

    public JSONObject setSource(String source) {
        return setValue("cSourcexx", source);
    }

    public String getSource() {
        return (String) getValue("cSourcexx");
    }

    public JSONObject setSeriesNo(String seriesNo) {
        return setValue("sSeriesNo", seriesNo);
    }

    public String getSeriesNo() {
        return (String) getValue("sSeriesNo");
    }

    public JSONObject setTranTotal(double tranTotal) {
        return setValue("nTranTotl", tranTotal);
    }

    public double getTranTotal() {
        return Double.parseDouble(String.valueOf(getValue("nTranTotl")));
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setDiscountAmount(double discountAmount) {
        return setValue("nDiscAmtx", discountAmount);
    }

    public double getDiscountAmount() {
        return Double.parseDouble(String.valueOf(getValue("nDiscAmtx")));
    }

    public JSONObject setTaxAmount(double taxAmount) {
        return setValue("nTaxAmntx", taxAmount);
    }

    public double getTaxAmount() {
        return Double.parseDouble(String.valueOf(getValue("nTaxAmntx")));
    }

    public JSONObject setNetTotal(double netTotal) {
        return setValue("nNetTotal", netTotal);
    }

    public double getNetTotal() {
        return Double.parseDouble(String.valueOf(getValue("nNetTotal")));
    }

    public JSONObject setAmountPaid(double amountPaid) {
        return setValue("nAmtPaidx", amountPaid);
    }

    public double getAmountPaid() {
        return Double.parseDouble(String.valueOf(getValue("nAmtPaidx")));
    }

//    public JSONObject setVatAmount(double vatAmount) {
//        return setValue("nTaxAmntx", vatAmount);
//    }
//
//    public double getVatAmount() {
//        return Double.parseDouble(String.valueOf(getValue("nTaxAmntx")));
//    }
//
//    public JSONObject setVatExempt(double vatExempt) {
//        return setValue("nTaxAmntx", vatExempt);
//    }
//
//    public double getVatExempt() {
//        return Double.parseDouble(String.valueOf(getValue("nTaxAmntx")));
//    }

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

    public JSONObject setWithSoa(String process) {
        return setValue("cWithSOAx", process);
    }

    public String getWithSoa() {
        return (String) getValue("cWithSOAx");
    }

    public JSONObject setProcess(String process) {
        return setValue("cProcessd", process);
    }

    public String getProcess() {
        return (String) getValue("cProcessd");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    public void setSummarized(boolean summarized) {
        this.isSummarized = summarized;
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
    public Model_Department Department() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sDeptIDxx"))) {
            if (poDepartment.getEditMode() == EditMode.READY
                    && poDepartment.getDepartmentId().equals((String) getValue("sDeptIDxx"))) {
                return poDepartment;
            } else {
                poJSON = poDepartment.openRecord((String) getValue("sDeptIDxx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poDepartment;
                } else {
                    poDepartment.initialize();
                    return poDepartment;
                }
            }
        } else {
            poDepartment.initialize();
            return poDepartment;
        }
    }

    public Model_Payee Payee() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sPayeeIDx"))) {
            if (poPayee.getEditMode() == EditMode.READY
                    && poPayee.getPayeeID().equals((String) getValue("sPayeeIDx"))) {
                return poPayee;
            } else {
                poJSON = poPayee.openRecord((String) getValue("sPayeeIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poPayee;
                } else {
                    poPayee.initialize();
                    return poPayee;
                }
            }
        } else {
            poPayee.initialize();
            return poPayee;
        }
    }

    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poBranch;
                } else {
                    poBranch.initialize();
                    return poBranch;
                }
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
    }

    public Model_Company Company() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sCompnyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompnyID"))) {
                return poCompany;
            } else {
                poJSON = poCompany.openRecord((String) getValue("sCompnyID"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poCompany;
                } else {
                    poCompany.initialize();
                    return poCompany;
                }
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
    }

    public Model_Industry Industry() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
    }
    
    public Model_Recurring_Expense_Payment_Monitor RecurringExpensePaymentMonitor() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poRecurringExpense.getEditMode() == EditMode.READY
                    && poRecurringExpense.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poRecurringExpense;
            } else {
                poJSON = poRecurringExpense.openRecord((String) getValue("sSourceNo"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poRecurringExpense;
                } else {
                    poRecurringExpense.initialize();
                    return poRecurringExpense;
                }
            }
        } else {
            poRecurringExpense.initialize();
            return poRecurringExpense;
        }
    }
    
    public Model_PO_Master PurchaseOrder() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poPOMaster.getEditMode() == EditMode.READY
                    && poPOMaster.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poPOMaster;
            } else {
                poJSON = poPOMaster.openRecord((String) getValue("sSourceNo"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOMaster;
                } else {
                    poPOMaster.initialize();
                    return poPOMaster;
                }
            }
        } else {
            poPOMaster.initialize();
            return poPOMaster;
        }
    }
    
    @Override
    public JSONObject openRecord(String id) throws SQLException, GuanzonException {
        this.poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, this.ID + " = " + SQLUtil.toSQL(id));
        ResultSet loRS = this.poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
                    Object loValue = loRS.getObject(lnCtr);
                    int lnType = loRS.getMetaData().getColumnType(lnCtr); // get JDBC type

                    if (loValue != null) {
                        switch (lnType) {
                            case 91: // DATE
                                loValue = loRS.getDate(lnCtr);
                                break;
                            case 93: // TIMESTAMP / DATETIME
                                loValue = loRS.getTimestamp(lnCtr); // use explicit getter first
                                if (loValue == null) {
                                    loValue = toTimestamp(loRS.getString(lnCtr)); // fallback
                                }
                                break;
                            default:
                                loValue = loRS.getObject(lnCtr);
                                break;
                        }
                    }

                    setValue(lnCtr, loValue);
                    System.out.println(lnCtr + " - " + (setValue(lnCtr, loValue)).toJSONString());
                }
                MiscUtil.close(loRS);
                this.pnEditMode = 1;
                this.poJSON = new JSONObject();
                this.poJSON.put("result", "success");
                this.poJSON.put("message", "Record loaded successfully.");
            } else {
                this.poJSON = new JSONObject();
                this.poJSON.put("result", "error");
                this.poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            this.poJSON = new JSONObject();
            this.poJSON.put("result", "error");
            this.poJSON.put("message", e.getMessage());
        }
        return this.poJSON;
    }

    public static java.sql.Timestamp toTimestamp(Object loValue) {
        if (loValue == null) {
            return null;
        }
        if (loValue instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp) loValue;
        }
        if (loValue instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) loValue).getTime());
        }

        String lsValue = loValue.toString().trim();
        if (lsValue.isEmpty()) {
            return null;
        }

        // Try all possible formats
        String[] laFormats = {
            "yyyy-MM-dd HH:mm:ss.SSS", 
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS", 
            "yyyy-MM-dd'T'HH:mm:ss", 
            "MM/dd/yyyy HH:mm:ss", 
            "yyyy-MM-dd", 
        };

        for (String lsFormat : laFormats) {
            try {
                java.text.SimpleDateFormat loFmt = new java.text.SimpleDateFormat(lsFormat);
                loFmt.setLenient(false);
                return new java.sql.Timestamp(loFmt.parse(lsValue).getTime());
            } catch (java.text.ParseException e) {
                // try next format
            }
        }

        // Log what value is actually coming in
        System.err.println("Cannot parse Timestamp from: [" + lsValue + "]");
        throw new IllegalArgumentException("Cannot parse datetime value: " + lsValue);
    }
}
