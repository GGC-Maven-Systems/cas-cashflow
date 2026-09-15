package ph.com.guanzongroup.cas.cashflow.model;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.status.CheckTransferStatus;

/**
 *
 * @author maynevval 08-09-2025
 */
public class Model_Check_Transfer_Master extends Model {

    //reference objects
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Client_Master poPrepareClient;
    Model_Department poDepartment;
    Model_Branch poBranch;
    Model_Branch poBranchDestination;
    private String bankid;
    private String compnyID;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("dTransact", poGRider.getServerDate());
            poEntity.updateDouble("nTranTotl", 0.0000);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateNull("sDeptIDxx");
            poEntity.updateNull("sPrepared");            
            poEntity.updateObject("dPrepared", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateNull("dReceived");
            poEntity.updateString("cPrintedx", "0"); 
            poEntity.updateString("cTranStat", CheckTransferStatus.OPEN);           
            poEntity.updateObject("sModified", poGRider.getUserID());
            poEntity.updateObject("dModified", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);

            //add model here
            pnEditMode = EditMode.UNKNOWN;

        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

        /**
     * Converts a Date to a string in yyyy-MM-dd format.
     * @param fdValue Date value
     * @return formatted date string
     */
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(fdValue);
    }

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //sIndstCdx
    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

//    //sBranchCd
//    public JSONObject setBranchCode(String branchCode) {
//        return setValue("sBranchCd", branchCode);
//    }
//
//    public String getBranchCode() {
//        return (String) getValue("sBranchCd");
//    }
    //dTransact
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    //sDestinat
    public JSONObject setDestination(String destination) {
        return setValue("sDestinat", destination);
    }

    public String getDestination() {
        return (String) getValue("sDestinat");
    }

    //sDeptIDxx
    public JSONObject setDepartment(String destination) {
        return setValue("sDeptIDxx", destination);
    }

    public String getDepartment() {
        return (String) getValue("sDeptIDxx");
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }//sReceived

    public JSONObject setReceivedBy(String receivedBy) {
        return setValue("sReceived", receivedBy);
    }

    public String getReceivedBy() {
        return (String) getValue("sReceived");
    }

    //dPrepared

    public JSONObject setPreparedDate(Date modifiedDate) {
        return setValue("dPrepared", modifiedDate);
    }

    public Date getPreparedDate() {
        return (Date) getValue("dPrepared");
    }

    //sPrepared
    public JSONObject setPreparedBy(String receivedBy) {
        return setValue("sPrepared", receivedBy);
    }

    public String getPreparedBy() {
        return (String) getValue("sPrepared");
    }
    
    public JSONObject setReceivedDate(LocalDateTime receivedDate) {
        return setValue("dReceived", receivedDate);
    }

    public LocalDateTime getReceivedDate() {
        Object value = getValue("dReceived");

        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return null; // empty string → null
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(str, formatter);
        }

        return null;
    }

    //dPrepared
    

    //nTranTotl
    public JSONObject setTransactionTotal(Double transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Double getTransactionTotal() {
        return Double.valueOf(getValue("nTranTotl").toString());
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //cPrintedx
    public JSONObject setPrintStatus(String printStatus) {
        return setValue("cPrintedx", printStatus);
    }

    public String getPrintStatus() {
        return (String) getValue("cPrintedx");
    }

    public boolean isPrintedStatus() {
        return RecordStatus.ACTIVE.equals(getValue("cPrintedx"));
    }

    //cTranStat
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    //sModified
    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }
    
    
    public String setBanks(String bankidx) {
        return bankid = bankidx;
    }

    public String getBanks() {
        return bankid;
    }
    
    
    public String setCompany(String bankidx) {
        return compnyID = bankidx;
    }

    public String getCompany() {
        return compnyID;
    }
    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
    public Model_Branch BranchDestination() throws SQLException, GuanzonException {
        if (poBranchDestination == null) {
            poBranchDestination = new ParamModels(poGRider).Branch();
        }
        if (!"".equals(getValue("sDestinat"))) {
            if (this.poBranchDestination.getEditMode() == EditMode.READY
                    && this.poBranchDestination.getBranchCode().equals((String)getValue("sDestinat"))) {
                return this.poBranchDestination;
            }
            if (ReferenceCache.tryLoad("Branch", (String) getValue("sDestinat"), poBranchDestination)) {
                return this.poBranchDestination;
            }
            this.poJSON = this.poBranchDestination.openRecord((String) getValue("sDestinat"));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Branch", (String) getValue("sDestinat"), poBranchDestination);
                return this.poBranchDestination;
            }
            this.poBranchDestination.initialize();
            return this.poBranchDestination;
        }
        this.poBranchDestination.initialize();
        return this.poBranchDestination;
    }

    public Model_Department Department() throws SQLException, GuanzonException {
        if (poDepartment == null) {
            poDepartment = new ParamModels(poGRider).Department();
        }
        if (!"".equals(getValue("sDeptIDxx"))) {
            if (this.poDepartment.getEditMode() == EditMode.READY
                    && this.poDepartment.getDepartmentId().equals(getValue("sDeptIDxx"))) {
                return this.poDepartment;
            }
            if (ReferenceCache.tryLoad("Department", (String) getValue("sDeptIDxx"), poDepartment)) {
                return this.poDepartment;
            }
            this.poJSON = this.poDepartment.openRecord((String) getValue("sDeptIDxx"));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Department", (String) getValue("sDeptIDxx"), poDepartment);
                return this.poDepartment;
            }
            this.poDepartment.initialize();
            return this.poDepartment;
        }
        this.poDepartment.initialize();
        return this.poDepartment;
    }

    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        if (!"".equals((String) getValue("sDestinat"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sDestinat"))) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", (String) getValue("sDestinat"), poBranch)) {
                    return poBranch;
                }
                poJSON = poBranch.openRecord((String) getValue("sDestinat"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", (String) getValue("sDestinat"), poBranch);
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

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        if (!"".equals(getValue("sIndstCdx"))) {
            if (this.poIndustry.getEditMode() == 1 && this.poIndustry
                    .getIndustryId().equals(getValue("sIndstCdx"))) {
                return this.poIndustry;
            }
            if (ReferenceCache.tryLoad("Industry", (String) getValue("sIndstCdx"), poIndustry)) {
                return this.poIndustry;
            }
            this.poJSON = this.poIndustry.openRecord((String) getValue("sIndstCdx"));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Industry", (String) getValue("sIndstCdx"), poIndustry);
                return this.poIndustry;
            }
            this.poIndustry.initialize();
            return this.poIndustry;
        }
        this.poIndustry.initialize();
        return this.poIndustry;
    }
    public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        if (!"".equals(compnyID)) {
            if (this.poCompany.getEditMode() == 1 && this.poCompany
                    .getCompanyId().equals(compnyID)) {
                return this.poCompany;
            }
            if (ReferenceCache.tryLoad("Company", compnyID, poCompany)) {
                return this.poCompany;
            }
            this.poJSON = this.poCompany.openRecord(this.getCompany());
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Company", compnyID, poCompany);
                return this.poCompany;
            }
            this.poCompany.initialize();
            return this.poCompany;
        }
        this.poCompany.initialize();
        return this.poCompany;
    }


    public Model_Client_Master PreparedClient() throws SQLException, GuanzonException {
        if (poPrepareClient == null) {
            poPrepareClient = new ClientModels(poGRider).ClientMaster();
        }
        if (!"".equals(getValue("sPrepared"))) {
            if (this.poPrepareClient.getEditMode() == 1 && this.poPrepareClient
                    .getClientId().equals(getValue("sIndstCdx"))) {
                return this.poPrepareClient;
            }
            if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sIndstCdx"), poPrepareClient)) {
                return this.poPrepareClient;
            }
            this.poJSON = this.poPrepareClient.openRecord((String) getValue("sIndstCdx"));
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Client_Master", (String) getValue("sIndstCdx"), poPrepareClient);
                return this.poPrepareClient;
            }
            this.poPrepareClient.initialize();
            return this.poPrepareClient;
        }
        this.poPrepareClient.initialize();
        return this.poPrepareClient;
    }

}
