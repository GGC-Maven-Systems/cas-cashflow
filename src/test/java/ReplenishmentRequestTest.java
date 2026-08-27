
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;

import org.h2.tools.RunScript;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.Logical;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.ReplenishmentRequest;
import ph.com.guanzongroup.cas.cashflow.model.Model_Cash_Fund_Ledger;
import ph.com.guanzongroup.cas.cashflow.model.Model_PettyCashLedger;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.status.ReplenishmentRequestStatus;

//@Ignore("Pending schema and SQL test data setup")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReplenishmentRequestTest {
    static GRiderCAS instance;
    static ReplenishmentRequest poController;
    static Connection conn;
    private static String psUserId = "GCO1260011";//M001250015;
    private static String psIndustryId = "08";
    private static String psCompanyId = "M001";
    private static String psCategorCd = "0000007";
    private String psTransNo = "";
    private String psCashFund = "GCO126000000002";
    private String psPettyCash = "0000005";

    @BeforeClass
    public static void setUpClass() throws GuanzonException, SQLException, IOException {
        instance = new GRiderCAS();

        if (!instance.loadEnv("gRider")) {
            System.err.println(instance.getMessage());
            System.exit(1);
        }

        if (!instance.logUser("gRider", "M001250015")) {
            System.err.println(instance.getMessage());
            System.exit(1);
        }

        loadCorePrimary();

        String path;
        String tempPath;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Maven_Systems";
            tempPath = "D:/temp";
        } else {
            path = "/srv/GGC_Maven_Systems";
            tempPath = "/srv/temp";
        }

        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.path.metadata", path + "/config/metadata/new/");
        System.setProperty("sys.default.path.temp", tempPath);

        if (!loadProperties()) {
            System.err.println("Unable to load config.");
            System.exit(1);
        }
        
        resetController();
    }

    @AfterClass
    public static void tearDownClass() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
        System.clearProperty("sys.default.path.config");
        System.clearProperty("sys.default.path.metadata");
        System.clearProperty("sys.default.path.temp");

        System.clearProperty("sys.main.industry");
        System.clearProperty("sys.general.industry");
        System.clearProperty("sys.dept.finance");
        System.clearProperty("sys.dept.procurement");
        System.clearProperty("user.selected.industry");
        System.clearProperty("user.selected.category");
        System.clearProperty("user.selected.company");
        System.clearProperty("sys.default.client.token");
        System.clearProperty("sys.default.access.token");
        System.clearProperty("sys.default.path.temp.attachments");
        System.clearProperty("allowed.department");
    }

    private static boolean loadProperties() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(System.getProperty("sys.default.path.config") + "/config/cas.properties"));

            System.setProperty("sys.main.industry", props.getProperty("sys.main.industry"));
            System.setProperty("sys.general.industry", props.getProperty("sys.general.industry"));
            System.setProperty("sys.dept.finance", props.getProperty("sys.dept.finance"));
            System.setProperty("sys.dept.procurement", props.getProperty("sys.dept.procurement"));
            System.setProperty("user.selected.industry", props.getProperty("user.selected.industry"));
            System.setProperty("user.selected.category", props.getProperty("user.selected.category"));
            System.setProperty("user.selected.company", props.getProperty("user.selected.company"));
            System.setProperty("sys.default.client.token", System.getProperty("sys.default.path.config") + "/client.token");
            System.setProperty("sys.default.access.token", System.getProperty("sys.default.path.config") + "/access.token");
            System.setProperty("sys.default.path.temp.attachments", props.getProperty("sys.default.path.temp.attachments"));
            System.setProperty("allowed.department", props.getProperty("allowed.department"));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    
    private static void loadCorePrimary() throws IOException, SQLException {
        conn = instance.getGConnection().getConnection();

        List<String> schemaScripts = new ArrayList<>();
        List<String> dataScripts = new ArrayList<>();

        schemaScripts.add("industry_schema");
//        schemaScripts.add("category_schema");
        schemaScripts.add("company_schema");
        schemaScripts.add("branch_schema");
        schemaScripts.add("department_schema");
        schemaScripts.add("parameter_status_history_schema");
        schemaScripts.add("transaction_status_history_schema");
        schemaScripts.add("position_schema");

        schemaScripts.add("replenishment_cashfund_schema");
        schemaScripts.add("replenishment_cashfund_ledger_schema");
        schemaScripts.add("replenishment_pettycash_schema");
        schemaScripts.add("replenishment_pettycash_ledger_schema");
        schemaScripts.add("replenishment_request_schema");
        schemaScripts.add("payment_request_master_schema");
        schemaScripts.add("payment_request_detail_schema");
        schemaScripts.add("client_master_schema");
        schemaScripts.add("payee_schema");


        dataScripts.add("industry_data");
//        dataScripts.add("category_data");
        dataScripts.add("company_data");
        dataScripts.add("branch_data");
        dataScripts.add("department_data");
        dataScripts.add("parameter_status_history_data");
        dataScripts.add("transaction_status_history_data");
        dataScripts.add("position_data");

        dataScripts.add("replenishment_cashfund_data");
        dataScripts.add("replenishment_cashfund_ledger_data");
        dataScripts.add("replenishment_pettycash_data");
        dataScripts.add("replenishment_pettycash_ledger_data");
        dataScripts.add("replenishment_request_data");
        dataScripts.add("payment_request_master_data");
        dataScripts.add("payment_request_detail_data");
        dataScripts.add("client_master_data");
        dataScripts.add("payee_data");

        for (String schema : schemaScripts) {
            try (FileReader schemaReader = new FileReader("test-data/" + schema + ".sql")) {
                RunScript.execute(conn, schemaReader);
            }
        }

        for (String data : dataScripts) {
            try (FileReader dataReader = new FileReader("test-data/" + data + ".sql")) {
                RunScript.execute(conn, dataReader);
            }
        }

    }
    private static void resetController() {
        try {
            poController = new CashflowControllers(instance, null).ReplenishmentRequest();
            poController.setWithUI(false);
            Assert.assertNotNull(poController);
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(ReplenishmentRequestTest.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
    }

    private static void startNewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }
        poController.initialize();

        poController.setIndustryId(psIndustryId);
        poController.setCompanyId(psCompanyId);

        JSONObject loJSON = poController.newRecord();
        Assert.assertEquals("success", loJSON.get("result"));
    }
    
    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }
    
    @Test
    public void test001CashFund(){
        try {
            JSONObject loJSON = new JSONObject();
            resetController();
            startNewTransaction();
//            poController.isUnitTest(true);
            poController.setWithUI(false);
            poController.getModel().setCompanyId(psCompanyId);
            poController.getModel().setIndustryId(psIndustryId);
            loJSON = poController.getModel().setTransactionDate(SQLUtil.toDate(xsDateShort(instance.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setRemarks("Test Cash Fund Replenishment");
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setTransactionStatus(ReplenishmentRequestStatus.OPEN);
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.getModel().setFundType(Logical.YES);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psCashFund);
            Assert.assertEquals("success", loJSON.get("result"));
            
            //load ledger
            loJSON = poController.loadLedger(true);
            Assert.assertEquals("success", loJSON.get("result"));
            
            if(Logical.YES.equals(poController.getModel().getFundType())){
                System.out.println("------LOAD CASH FUND LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getLoadCashFundLedgerListCount(); lnCtr++){
                    System.out.println("Cash Fund Id : " + poController.LoadCashFundLedgerList(lnCtr).getCashFundId());
                    System.out.println("Ledger No : " + poController.LoadCashFundLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.LoadCashFundLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.LoadCashFundLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.LoadCashFundLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.LoadCashFundLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.LoadCashFundLedgerList(lnCtr).getDebitAmount());
                }
                
                System.out.println("---------ADD CASH FUND LEDGER------------");
                List<Model_Cash_Fund_Ledger> laCashFundLedger = new ArrayList<>();
                laCashFundLedger.add(poController.LoadCashFundLedgerList(0));
                laCashFundLedger.add(poController.LoadCashFundLedgerList(1));
                loJSON = poController.AddCashFundLedger(laCashFundLedger);
                Assert.assertEquals("success", loJSON.get("result"));
                
                System.out.println("------CASH FUND LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getCashFundLedgerListCount(); lnCtr++){
                    System.out.println("Cash Fund Id : " + poController.CashFundLedgerList(lnCtr).getCashFundId());
                    System.out.println("Ledger No : " + poController.CashFundLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.CashFundLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.CashFundLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.CashFundLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.CashFundLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.CashFundLedgerList(lnCtr).getDebitAmount());
                }
            } 
            
            poController.computeFields();
            loJSON = poController.SaveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.updateRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            if(Logical.YES.equals(poController.getModel().getFundType())){
                
                System.out.println("---------REMOVE CASH FUND LEDGER------------");
                List<Model_Cash_Fund_Ledger> laRemoveCashFundLedger = new ArrayList<>();
                laRemoveCashFundLedger.add(poController.CashFundLedgerList(1));
                poController.RemoveCashFundLedger(laRemoveCashFundLedger);
                
                System.out.println("------REMOVED CASH FUND LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getRemovedCashFundLedgerListCount(); lnCtr++){
                    System.out.println("Cash Fund Id : " + poController.RemovedCashFundLedgerList(lnCtr).getCashFundId());
                    System.out.println("Ledger No : " + poController.RemovedCashFundLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.RemovedCashFundLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.RemovedCashFundLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.RemovedCashFundLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.RemovedCashFundLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.RemovedCashFundLedgerList(lnCtr).getDebitAmount());
                }
            } 
            
            poController.computeFields();
            loJSON = poController.SaveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.VoidRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            //Set back to OPEN
            loJSON = poController.newRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundType(Logical.YES);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psCashFund);
            Assert.assertEquals("success", loJSON.get("result"));
            //load ledger
            loJSON = poController.loadLedger(true);
            Assert.assertEquals("success", loJSON.get("result"));
            System.out.println("---------ADD CASH FUND LEDGER------------");
            List<Model_Cash_Fund_Ledger> laCashFundLedger = new ArrayList<>();
            laCashFundLedger.add(poController.LoadCashFundLedgerList(0));
            loJSON = poController.AddCashFundLedger(laCashFundLedger);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.SaveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.ApproveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.CancelRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            //Set back to APPROVED
            loJSON = poController.newRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundType(Logical.YES);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psCashFund);
            Assert.assertEquals("success", loJSON.get("result"));
            //load ledger
            loJSON = poController.loadLedger(true);
            Assert.assertEquals("success", loJSON.get("result"));
            System.out.println("---------ADD CASH FUND LEDGER------------");
            laCashFundLedger = new ArrayList<>();
            laCashFundLedger.add(poController.LoadCashFundLedgerList(0));
            loJSON = poController.AddCashFundLedger(laCashFundLedger);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.SaveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.ApproveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.PostRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            
        } catch (CloneNotSupportedException | SQLException | GuanzonException | ParseException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
        }
        
    }
    
    @Test
    public void test002PettyCash(){
        try {
            JSONObject loJSON = new JSONObject();
            resetController();
            startNewTransaction();
//            poController.isUnitTest(true);
            poController.setWithUI(false);
            poController.getModel().setCompanyId(psCompanyId);
            poController.getModel().setIndustryId(psIndustryId);
            loJSON = poController.getModel().setTransactionDate(SQLUtil.toDate(xsDateShort(instance.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setRemarks("Test Cash Fund Replenishment");
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setTransactionStatus(ReplenishmentRequestStatus.OPEN);
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.getModel().setFundType(Logical.NO);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psPettyCash);
            Assert.assertEquals("success", loJSON.get("result"));
            
            //load ledger
            loJSON = poController.loadLedger(true);
            Assert.assertEquals("success", loJSON.get("result"));
            
            if(Logical.NO.equals(poController.getModel().getFundType())){
                System.out.println("------LOAD PETTY CASH LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getLoadPettyCashLedgerListCount(); lnCtr++){
                    System.out.println("Petty Cash Id : " + poController.LoadPettyCashLedgerList(lnCtr).getPettyID());
                    System.out.println("Ledger No : " + poController.LoadPettyCashLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.LoadPettyCashLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.LoadPettyCashLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.LoadPettyCashLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.LoadPettyCashLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.LoadPettyCashLedgerList(lnCtr).getDebitAmount());
                }
                
                System.out.println("LOAD PETTY CASH LEDGER COUNT : " +  poController.getLoadPettyCashLedgerListCount());
                System.out.println("---------ADD PETTY CASH LEDGER------------");
                List<Model_PettyCashLedger> laPettyCashLedger = new ArrayList<>();
                laPettyCashLedger.add(poController.LoadPettyCashLedgerList(0));
                laPettyCashLedger.add(poController.LoadPettyCashLedgerList(1));
                loJSON = poController.AddPettyCashLedger(laPettyCashLedger);
                Assert.assertEquals("success", loJSON.get("result"));
                
                System.out.println("------PETTY CASH LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getPettyCashLedgerListCount(); lnCtr++){
                    System.out.println("Petty Cash Id : " + poController.PettyCashLedgerList(lnCtr).getPettyID());
                    System.out.println("Ledger No : " + poController.PettyCashLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.PettyCashLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.PettyCashLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.PettyCashLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.PettyCashLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.PettyCashLedgerList(lnCtr).getDebitAmount());
                }
            } 
            
            poController.computeFields();
            loJSON = poController.SaveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.updateRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            if(Logical.NO.equals(poController.getModel().getFundType())){
                System.out.println("---------REMOVE PETTY CASH LEDGER------------");
                List<Model_PettyCashLedger> laRemovePettyCashFundLedger = new ArrayList<>();
                laRemovePettyCashFundLedger.add(poController.PettyCashLedgerList(1));
                poController.RemovePettyCashLedger(laRemovePettyCashFundLedger);
                
                System.out.println("------REMOVED PETTY CASH LEDGER LIST----");
                for(int lnCtr = 0;lnCtr < poController.getRemovedPettyCashLedgerListCount(); lnCtr++){
                    System.out.println("Petty Cash Id : " + poController.RemovedPettyCashLedgerList(lnCtr).getPettyID());
                    System.out.println("Ledger No : " + poController.RemovedPettyCashLedgerList(lnCtr).getLedgerNo());
                    System.out.println("Source Code : " + poController.RemovedPettyCashLedgerList(lnCtr).getSourceCode());
                    System.out.println("Source No : " + poController.RemovedPettyCashLedgerList(lnCtr).getSourceNo());
                    System.out.println("Transaction Date : " + poController.RemovedPettyCashLedgerList(lnCtr).getTransactionDate());
                    System.out.println("Credit Amount : " + poController.RemovedPettyCashLedgerList(lnCtr).getCreditAmount());
                    System.out.println("Debit Amount : " + poController.RemovedPettyCashLedgerList(lnCtr).getDebitAmount());
                }
            } 
            
            poController.computeFields();
            loJSON = poController.SaveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.VoidRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            //Set back to OPEN
            loJSON = poController.newRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundType(Logical.NO);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psPettyCash);
            Assert.assertEquals("success", loJSON.get("result"));
            //load ledger
            loJSON = poController.loadLedger(true);
            Assert.assertEquals("success", loJSON.get("result"));
            System.out.println("LOAD PETTY CASH LEDGER COUNT : " +  poController.getLoadPettyCashLedgerListCount());
            System.out.println("---------ADD PETTY CASH LEDGER------------");
            List<Model_PettyCashLedger> laPettyCashLedger = new ArrayList<>();
            laPettyCashLedger.add(poController.LoadPettyCashLedgerList(0));
            loJSON = poController.AddPettyCashLedger(laPettyCashLedger);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.SaveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.ApproveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.CancelRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            //Set back to OPEN
            loJSON = poController.newRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundType(Logical.NO);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.getModel().setFundId(psPettyCash);
            Assert.assertEquals("success", loJSON.get("result"));
            //load ledger
            loJSON = poController.loadLedger(true);
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            System.out.println("LOAD PETTY CASH LEDGER COUNT : " +  poController.getLoadPettyCashLedgerListCount());
            System.out.println("---------ADD PETTY CASH LEDGER------------");
            laPettyCashLedger = new ArrayList<>();
            laPettyCashLedger.add(poController.LoadPettyCashLedgerList(0));
            loJSON = poController.AddPettyCashLedger(laPettyCashLedger);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.SaveRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
                        
            psTransNo = poController.getModel().getTransactionNo();
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.ApproveRecord();
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.OpenRecord(psTransNo);
            Assert.assertEquals("success", loJSON.get("result"));
            loJSON = poController.PostRecord();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.OpenRecord(psTransNo);
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));

            try {
                poController.setWithUI(false);
                poController.ShowStatusHistory();
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
            }
            
            poController.getSysUser(psUserId);
            
            loJSON = poController.getEntryBy();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
         
        } catch (CloneNotSupportedException | SQLException | GuanzonException | ParseException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
        }
    }
    
    @Test
    public void test003LoadTransactionList() {
        try {
            JSONObject loJSON = new JSONObject();
            resetController();
            poController.setWithUI(false);
            poController.setCompanyId(psCompanyId);
            poController.setIndustryId(psIndustryId);
            System.out.println("---------LOAD TRANSACTION LIST------------");
            loJSON = poController.loadTransactionList("", "");
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            for(int lnCtr = 0; lnCtr < poController.getTransactionListCount(); lnCtr++){
                System.out.println("Transaction No : " + poController.TransactionList(lnCtr).getTransactionNo());
            }
            
        } catch (SQLException | GuanzonException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
        } 
    }
    
    @Test
    public void test004SearchFund() {
        try {
            JSONObject loJSON = new JSONObject();
            resetController();
            startNewTransaction();
            poController.setWithUI(false);
            poController.getModel().setCompanyId(psCompanyId);
            poController.getModel().setIndustryId(psIndustryId);
//            loJSON = poController.SearchFund("",false,false);
//            System.out.println("MESSAGE : " + loJSON.get("message"));
//            Assert.assertEquals("success", loJSON.get("result"));
            
            poController.setFund("test");
            Assert.assertEquals("test", poController.getfund());
            
            loJSON = poController.SearchFund("",false,true);
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
            loJSON = poController.searchRecord("",false);
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
        } 
    }
    
    @Test
    public void test005IsEntryOkay() {
        try {
            JSONObject loJSON = new JSONObject();
            resetController();
            startNewTransaction();
            poController.setWithUI(false);
            poController.getModel().setCompanyId(psCompanyId);
            poController.getModel().setIndustryId(psIndustryId);
            
            psTransNo = poController.getModel().getNextCode();
            poController.getModel().setTransactionNo("");
            loJSON = poController.isEntryOkay();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("error", loJSON.get("result"));
            
            poController.getModel().setTransactionNo(psTransNo);
            loJSON = poController.isEntryOkay();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("error", loJSON.get("result"));
            
            poController.getModel().setTransactionNo(psTransNo);
            poController.getModel().setFundType(Logical.YES);
            loJSON = poController.isEntryOkay();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("error", loJSON.get("result"));
            
            poController.getModel().setTransactionNo(psTransNo);
            poController.getModel().setFundType(Logical.YES);
            poController.getModel().setFundId(psCashFund);
            loJSON = poController.isEntryOkay();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("error", loJSON.get("result"));
            
            poController.getModel().setTransactionNo(psTransNo);
            poController.getModel().setFundType(Logical.YES);
            poController.getModel().setFundId(psCashFund);
            poController.getModel().setTransactionAmount(1000.0000);
            loJSON = poController.isEntryOkay();
            System.out.println("MESSAGE : " + loJSON.get("message"));
            Assert.assertEquals("success", loJSON.get("result"));
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
        } 
    }

    @Test
    public void test006GetStatus() {
        Assert.assertNotNull(poController);

        Assert.assertEquals("Open", poController.getStatus(ReplenishmentRequestStatus.OPEN));
        Assert.assertEquals("Approved", poController.getStatus(ReplenishmentRequestStatus.APPROVED));
        Assert.assertEquals("Posted", poController.getStatus(ReplenishmentRequestStatus.POSTED));
        Assert.assertEquals("Cancelled", poController.getStatus(ReplenishmentRequestStatus.CANCELLED));
        Assert.assertEquals("Voided", poController.getStatus(ReplenishmentRequestStatus.VOID));

        Assert.assertEquals("UNKNOWN", poController.getStatus("X"));
    }

    @Test
    public void test007() {
//        try {
//            Assert.assertNotNull(poController);
//
//            
//        
//        } catch (SQLException | GuanzonException  ex) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//            Assert.assertEquals(MiscUtil.getException(ex), MiscUtil.getException(ex));
//        } 
    }
    
}
