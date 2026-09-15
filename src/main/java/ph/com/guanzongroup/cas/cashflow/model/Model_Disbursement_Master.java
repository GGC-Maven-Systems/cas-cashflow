/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.status.JournalProposalStatus;

/**
 *
 * @author User
 */
public class Model_Disbursement_Master extends Model {

    Model_Payee poPayee;
    Model_Branch poBranch;
    Model_Company poCompany;
    Model_Industry poIndustry;
    Model_Check_Payments poCheckPayments;
    Model_Other_Payments poOtherPayments;
    private String oldDisbursementType = DisbursementStatic.DisbursementType.CHECK;
    private String supplierClientID = "";
    private String searchParticularID = "";
    private String searchBranchID = "";
    private String searchPayee = "";
    private double advancesTotal = 0.0000;
    private boolean isSummarized;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateNull("dPrintxxx");
            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", DisbursementStatic.DefaultValues.default_value_integer);
            poEntity.updateObject("nTranTotl", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nDiscTotl", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nWTaxTotl", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nVATSales", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nVATAmtxx", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nZroVATSl", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nVatExmpt", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateObject("nNetTotal", DisbursementStatic.DefaultValues.default_value_double_0000);
            poEntity.updateString("cTranStat", DisbursementStatic.OPEN);
            poEntity.updateString("cPrintxxx", DisbursementStatic.OPEN);
            poEntity.updateString("cPrintBIR", DisbursementStatic.OPEN);
            poEntity.updateNull("dPrintBIR");

//            poEntity.updateString("cTranStat", DisbursementStatic.OPEN);
//            poEntity.updateString("cDisbrsTp", DisbursementStatic.DisbursementType.CHECK);
//            poEntity.updateObject("nAmountxx", DisbursementStatic.DefaultValues.default_value_double);
//            poEntity.updateObject("nDiscTotl", DisbursementStatic.DefaultValues.default_value_double);
//            poEntity.updateObject("nWTaxTotl", DisbursementStatic.DefaultValues.default_value_double);
//            poEntity.updateObject("nNetTotal", DisbursementStatic.DefaultValues.default_value_double);
//            poEntity.updateObject("nEntryNox", DisbursementStatic.DefaultValues.default_value_integer);
//            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
//            poEntity.updateObject("sBranchCd", poGRider.getBranchCode());
            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            ID = "sTransNox";

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

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
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

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setVoucherNo(String voucherNo) {
        return setValue("sVouchrNo", voucherNo);
    }

    public String getVoucherNo() {
        return (String) getValue("sVouchrNo");
    }

    public JSONObject setDisbursementType(String disbursementType) {
        return setValue("cDisbrsTp", disbursementType);
    }

    public String getDisbursementType() {
        return (String) getValue("cDisbrsTp");
    }

    public JSONObject setPayeeID(String payeeID) {
        return setValue("sPayeeIDx", payeeID);
    }

    public String getPayeeID() {
        return (String) getValue("sPayeeIDx");
    }

    public JSONObject setTransactionTotal(double transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public double getTransactionTotal() {
        return Double.parseDouble(String.valueOf(getValue("nTranTotl")));
    }

    public JSONObject setDiscountTotal(double discountTotal) {
        return setValue("nDiscTotl", discountTotal);
    }

    public double getDiscountTotal() {
        return Double.parseDouble(String.valueOf(getValue("nDiscTotl")));
    }

    public JSONObject setWithTaxTotal(double withTaxTotal) {
        return setValue("nWTaxTotl", withTaxTotal);
    }

    public double getWithTaxTotal() {
        return Double.parseDouble(String.valueOf(getValue("nWTaxTotl")));
    }

    public JSONObject setNonVATSale(double nonVATSale) {
        return setValue("nNonVATSl", nonVATSale);
    }

    public double getNonVATSale() {
        return Double.parseDouble(String.valueOf(getValue("nNonVATSl")));
    }

    public JSONObject setVATSale(double VATSale) {
        return setValue("nVATSales", VATSale);
    }

    public double getVATSale() {
        return Double.parseDouble(String.valueOf(getValue("nVATSales")));
    }
    
    public JSONObject setVATAmount(double vatAmount) {
        return setValue("nVATAmtxx", vatAmount);
    }

    public double getVATAmount() {
        return Double.parseDouble(String.valueOf(getValue("nVATAmtxx")));
    }

    public JSONObject setZeroVATSales(double zeroVATSales) {
        return setValue("nZroVATSl", zeroVATSales);
    }

    public double getZeroVATSales() {
        return Double.parseDouble(String.valueOf(getValue("nZroVATSl")));
    }

    public JSONObject setVATExmpt(double vatExmpt) {
        return setValue("nVatExmpt", vatExmpt);
    }

    public double getVATExmpt() {
        return Double.parseDouble(String.valueOf(getValue("nVatExmpt")));
    }

    public JSONObject setNetTotal(double netTotal) {
        return setValue("nNetTotal", netTotal);
    }

    public double getNetTotal() {
        return Double.parseDouble(String.valueOf(getValue("nNetTotal")));
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setApproved(String approved) {
        return setValue("sApproved", approved);
    }

    public String getApproved() {
        return (String) getValue("sApproved");
    }

    public JSONObject setBankPrint(String bankPrint) {
        return setValue("cBankPrnt", bankPrint);
    }

    public String getBankPrint() {
        return (String) getValue("cBankPrnt");
    }

    public JSONObject setPrint(String print) {
        return setValue("cPrintxxx", print);
    }

    public String getPrint() {
        return (String) getValue("cPrintxxx");
    }
    
    public JSONObject isPrinted(boolean replaced) {
        return setValue("cPrintxxx", replaced ? "1" : "0");
    }

    public boolean isPrinted() {
        Object value = getValue("cPrintxxx");
        return "1".equals(String.valueOf(value));
    }

    public JSONObject setDatePrint(Date datePrint) {
        return setValue("dPrintxxx", datePrint);
    }

    public Date getDatePrint() {
        return (Date) getValue("dPrintxxx");
    }
    
    public JSONObject isBIRPrinted(boolean replaced) {
        return setValue("cPrintBIR", replaced ? "1" : "0");
    }

    public boolean isBIRPrinted() {
        Object value = getValue("cPrintBIR");
        return "1".equals(String.valueOf(value));
    }

    public JSONObject setBIRPrintDate(Date datePrint) {
        return setValue("dPrintBIR", datePrint);
    }

    public Date getBIRPrintDate() {
        return (Date) getValue("dPrintBIR");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }
    
    public String getConvertedTransactionStatus() {
        if("ABCDEFGHIJ".contains((String) getValue("cTranStat"))){
            return String.valueOf(((String) getValue("cTranStat")).getBytes()[0] - 64);
        }
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

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    // getter and setter below is only cache
    public void setOldDisbursementType(String oldDisbursementType) {
        this.oldDisbursementType = oldDisbursementType;
    }

    public String getOldDisbursementType() {
        return this.oldDisbursementType;
    }
    
    public void setSearchParticular(String searchParticular) {
        this.searchParticularID = searchParticular;
    }

    public String getSearchParticular() {
        return this.searchParticularID;
    }
    
    public void setSearchBranch(String searchBranch) {
        this.searchBranchID = searchBranch;
    }

    public String getSearchBranch() {
        return this.searchBranchID;
    }
    
    public void setSearchpayee(String searchpayee) {
        this.searchPayee = searchpayee;
    }

    public String getSearchpayee() {
        return this.searchPayee;
    }
    
    public void setSupplierClientID(String SupplierClientID) {
        this.supplierClientID = SupplierClientID;
    }

    public String getSupplierClientID() {
        return this.supplierClientID;
    }
    
    public void setAdvancesTotal(Double fdblAdvancesTotal) {
        this.advancesTotal = fdblAdvancesTotal;
    }

    public double getAdvancesTotal() {
        return this.advancesTotal;
    }
    public void setSummarized(boolean summarized) {
        this.isSummarized = summarized;
    }

    public Model_Payee Payee() throws GuanzonException, SQLException {
        if (poPayee == null) {
            poPayee = new CashflowModels(poGRider).Payee();
        }
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
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", (String) getValue("sBranchCd"), poBranch)) {
                    return poBranch;
                }

                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", (String) getValue("sBranchCd"), poBranch);
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

    public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        if (!"".equals((String) getValue("sCompnyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompnyID"))) {
                return poCompany;
            } else {
                if (ReferenceCache.tryLoad("Company", (String) getValue("sCompnyID"), poCompany)) {
                    return poCompany;
                }

                poJSON = poCompany.openRecord((String) getValue("sCompnyID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Company", (String) getValue("sCompnyID"), poCompany);
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

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                if (ReferenceCache.tryLoad("Industry", (String) getValue("sIndstCdx"), poIndustry)) {
                    return poIndustry;
                }

                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Industry", (String) getValue("sIndstCdx"), poIndustry);
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

    public Model_Check_Payments CheckPayments() throws SQLException, GuanzonException {
        if (poCheckPayments == null) {
            poCheckPayments = new CashflowModels(poGRider).CheckPayments();
        }
        if (poOtherPayments == null) {
            poOtherPayments = new CashflowModels(poGRider).OtherPayments();
        }
        if (!"".equals((String) getValue("sTransNox"))) {
            if (poOtherPayments.getEditMode() == EditMode.READY && poCheckPayments.getSourceNo().equals((String) getValue("sTransNox"))) {
                return poCheckPayments;
            } else {
                //find the recent check payment to open
                Model_Check_Payments loMaster = new CashflowModels(poGRider).CheckPayments();
                String lsSQL = MiscUtil.makeSelect(loMaster);
                lsSQL = MiscUtil.addCondition(lsSQL,
                        " sSourceNo = " + SQLUtil.toSQL(getTransactionNo())
                        + " AND sSourceCd = " + SQLUtil.toSQL(DisbursementStatic.SourceCode.DISBURSEMENT_VOUCHER)
                );

                lsSQL = lsSQL + " ORDER BY dTransact,sTransNox DESC ";
                System.out.println("Executing SQL: " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                String lsCheckTransNo = "";
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------CHECK PAYMENT--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            lsCheckTransNo = loRS.getString("sTransNox");
                        }  
                    }
                }
                MiscUtil.close(loRS);
                
                poJSON = poCheckPayments.openRecord(lsCheckTransNo);
//                poJSON = poCheckPayments.openRecordbySourceNo((String) getValue("sTransNox"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poCheckPayments;
                } else {
                    poCheckPayments.initialize();
                    return poCheckPayments;
                }
            }
        } else {
            poCheckPayments.initialize();
            return poCheckPayments;
        }
    }
    
    public Model_Other_Payments OtherPayments() throws SQLException, GuanzonException {
        if (poOtherPayments == null) {
            poOtherPayments = new CashflowModels(poGRider).OtherPayments();
        }
        if (!"".equals((String) getValue("sTransNox"))) {
            if (poOtherPayments.getEditMode() == EditMode.READY
                    && poOtherPayments.getSourceNo().equals((String) getValue("sTransNox"))) {
                return poOtherPayments;
            } else {
                //find the recent other payment to open
                Model_Other_Payments loMaster = new CashflowModels(poGRider).OtherPayments();
                String lsSQL = MiscUtil.makeSelect(loMaster);
                lsSQL = MiscUtil.addCondition(lsSQL,
                        " sSourceNo = " + SQLUtil.toSQL(getTransactionNo())
                        + " AND sSourceCd = " + SQLUtil.toSQL(DisbursementStatic.SourceCode.DISBURSEMENT_VOUCHER)
                );

                lsSQL = lsSQL + " ORDER BY dTransact,sTransNox DESC ";
                System.out.println("Executing SQL: " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                poJSON = new JSONObject();
                String lsOthTransNo = "";
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if (loRS.next()) {
                        // Print the result set
                        System.out.println("--------------------------OTHER PAYMENT--------------------------");
                        System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                        System.out.println("------------------------------------------------------------------------------");
                        if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                            lsOthTransNo = loRS.getString("sTransNox");
                        }  
                    }
                }
                MiscUtil.close(loRS);
                
                poJSON = poOtherPayments.openRecord(lsOthTransNo);
//                poJSON = poOtherPayments.openRecordbySourceNo((String) getValue("sTransNox"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poOtherPayments;
                } else {
                    poOtherPayments.initialize();
                    return poOtherPayments;
                }
            }
        } else {
            poOtherPayments.initialize();
            return poOtherPayments;
        }
    }

    
    
}
