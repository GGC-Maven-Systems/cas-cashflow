package ph.com.guanzongroup.cas.cashflow.services;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.LogWrapper;
import ph.com.guanzongroup.cas.cashflow.APPaymentAdjustment;
import ph.com.guanzongroup.cas.cashflow.AccountChart;
import ph.com.guanzongroup.cas.cashflow.BIR2307Filler;
import ph.com.guanzongroup.cas.cashflow.Particular;
import ph.com.guanzongroup.cas.cashflow.BankAccountMaster;
import ph.com.guanzongroup.cas.cashflow.CachePayable;
import ph.com.guanzongroup.cas.cashflow.CashAdvance;
import ph.com.guanzongroup.cas.cashflow.CashDisbursement;
import ph.com.guanzongroup.cas.cashflow.CashFund;
import ph.com.guanzongroup.cas.cashflow.CashLiquidation;
import ph.com.guanzongroup.cas.cashflow.CheckDeposit;
import ph.com.guanzongroup.cas.cashflow.CheckDeposits;
import ph.com.guanzongroup.cas.cashflow.CheckImporting;
import ph.com.guanzongroup.cas.cashflow.CheckPaymentImporting;
import ph.com.guanzongroup.cas.cashflow.CheckPayments;
import ph.com.guanzongroup.cas.cashflow.CheckPrintingRequest;
import ph.com.guanzongroup.cas.cashflow.CheckReleases;
import ph.com.guanzongroup.cas.cashflow.CheckStatusUpdate;
import ph.com.guanzongroup.cas.cashflow.CheckTransfers;
import ph.com.guanzongroup.cas.cashflow.Disbursement;
import ph.com.guanzongroup.cas.cashflow.DisbursementVoucher;
import ph.com.guanzongroup.cas.cashflow.DocumentMapping;
import ph.com.guanzongroup.cas.cashflow.Journal;
import ph.com.guanzongroup.cas.cashflow.JournalProposal;
import ph.com.guanzongroup.cas.cashflow.OtherPaymentStatusUpdate;
import ph.com.guanzongroup.cas.cashflow.OtherPayments;
import ph.com.guanzongroup.cas.cashflow.Payee;
import ph.com.guanzongroup.cas.cashflow.PaymentRequest;
import ph.com.guanzongroup.cas.cashflow.PettyCash;
import ph.com.guanzongroup.cas.cashflow.PettyCashDisbursement;
import ph.com.guanzongroup.cas.cashflow.RecurringExpense;
import ph.com.guanzongroup.cas.cashflow.RecurringExpenseSchedule;
import ph.com.guanzongroup.cas.cashflow.RecurringIssuance;
import ph.com.guanzongroup.cas.cashflow.ReplenishmentRequest;
import ph.com.guanzongroup.cas.cashflow.SOATagging;
import ph.com.guanzongroup.cas.cashflow.SubClass.Disbursement_PRF;
import ph.com.guanzongroup.cas.cashflow.TransactionAccountChart;
import ph.com.guanzongroup.cas.cashflow.WithholdingTax;
import ph.com.guanzongroup.cas.cashflow.WithholdingTaxDeductions;

public class CashflowControllers {

    public CashflowControllers(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public CachePayable CachePayable() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CachePayable: Application driver is not set.");
            return null;
        }

        if (poCachePayable != null) {
            return poCachePayable;
        }

        poCachePayable = new CachePayable();
        poCachePayable.setApplicationDriver(poGRider);
        poCachePayable.setBranchCode(poGRider.getBranchCode());
        poCachePayable.setVerifyEntryNo(true);
        poCachePayable.setWithParent(false);
        poCachePayable.setLogWrapper(poLogWrapper);
        return poCachePayable;
    }

    public BankAccountMaster BankAccountMaster() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.BankAccountMaster: Application driver is not set.");
            return null;
        }

        if (poBankAccountMaster != null) {
            return poBankAccountMaster;
        }

        poBankAccountMaster = new BankAccountMaster();
        poBankAccountMaster.setApplicationDriver(poGRider);
        poBankAccountMaster.setWithParentClass(false);
        poBankAccountMaster.setLogWrapper(poLogWrapper);
        poBankAccountMaster.initialize();
        poBankAccountMaster.newRecord();
        return poBankAccountMaster;
    }

    public RecurringIssuance RecurringIssuance() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.RecurringIssuance: Application driver is not set.");
            return null;
        }

        if (poRecurringIssuance != null) {
            return poRecurringIssuance;
        }

        poRecurringIssuance = new RecurringIssuance();
        poRecurringIssuance.setApplicationDriver(poGRider);
        poRecurringIssuance.setWithParentClass(false);
        poRecurringIssuance.setLogWrapper(poLogWrapper);
        poRecurringIssuance.initialize();
        poRecurringIssuance.newRecord();
        return poRecurringIssuance;
    }

    public Payee Payee() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.Payee: Application driver is not set.");
            return null;
        }

        if (poPayee != null) {
            return poPayee;
        }

        poPayee = new Payee();
        poPayee.setApplicationDriver(poGRider);
        poPayee.setWithParentClass(false);
        poPayee.setLogWrapper(poLogWrapper);
        poPayee.initialize();
        poPayee.newRecord();
        return poPayee;
    }

    public Particular Particular() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.Particular: Application driver is not set.");
            return null;
        }

        if (poParticular != null) {
            return poParticular;
        }

        poParticular = new Particular();
        poParticular.setApplicationDriver(poGRider);
        poParticular.setWithParentClass(false);
        poParticular.setLogWrapper(poLogWrapper);
        poParticular.initialize();
        poParticular.newRecord();
        return poParticular;
    }

    public PaymentRequest PaymentRequest() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.PaymentRequest: Application driver is not set.");
            return null;
        }

        if (poPaymentRequest != null) {
            return poPaymentRequest;
        }

        poPaymentRequest = new PaymentRequest();
        poPaymentRequest.setApplicationDriver(poGRider);
        poPaymentRequest.setBranchCode(poGRider.getBranchCode());
        poPaymentRequest.setLogWrapper(poLogWrapper);
        poPaymentRequest.setVerifyEntryNo(true);
        poPaymentRequest.setWithParent(false);
        return poPaymentRequest;
    }

    public Disbursement DisbursementBase() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.DisbursementBase: Application driver is not set.");
            return null;
        }

        if (poDisbursementBase != null) {
            return poDisbursementBase;
        }

        poDisbursementBase = new Disbursement();
        poDisbursementBase.setApplicationDriver(poGRider);
        poDisbursementBase.setBranchCode(poGRider.getBranchCode());
        poDisbursementBase.setLogWrapper(poLogWrapper);
        poDisbursementBase.setVerifyEntryNo(true);
        poDisbursementBase.setWithParent(false);
        return poDisbursementBase;
    }

    public Disbursement Disbursement() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.Disbursement: Application driver is not set.");
            return null;
        }

        if (poDisbursement != null) {
            return poDisbursement;
        }

        poDisbursement = new Disbursement();
        poDisbursement.setApplicationDriver(poGRider);
        poDisbursement.setBranchCode(poGRider.getBranchCode());
        poDisbursement.setLogWrapper(poLogWrapper);
        poDisbursement.setVerifyEntryNo(true);
        poDisbursement.setWithParent(false);
        return poDisbursement;
    }

    public DisbursementVoucher DisbursementVoucher() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.DisbursementVoucher: Application driver is not set.");
            return null;
        }

        if (poDisbursementVoucher != null) {
            return poDisbursementVoucher;
        }

        poDisbursementVoucher = new DisbursementVoucher();
        poDisbursementVoucher.setApplicationDriver(poGRider);
        poDisbursementVoucher.setBranchCode(poGRider.getBranchCode());
        poDisbursementVoucher.setLogWrapper(poLogWrapper);
        poDisbursementVoucher.setVerifyEntryNo(true);
        poDisbursementVoucher.setWithParent(false);
        return poDisbursementVoucher;
    }

    public CheckPayments CheckPayments() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckPayments: Application driver is not set.");
            return null;
        }

        if (poCheckPayments != null) {
            return poCheckPayments;
        }

        poCheckPayments = new CheckPayments();
        poCheckPayments.setApplicationDriver(poGRider);
        poCheckPayments.setWithParentClass(true);
        poCheckPayments.setLogWrapper(poLogWrapper);
        poCheckPayments.initialize();
        return poCheckPayments;
    }

    public CheckPaymentImporting CheckPaymentImporting() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckPaymentImporting: Application driver is not set.");
            return null;
        }

        if (poCheckPaymentImports != null) {
            return poCheckPaymentImports;
        }

        poCheckPaymentImports = new CheckPaymentImporting();
        poCheckPaymentImports.setApplicationDriver(poGRider);
        poCheckPaymentImports.setWithParentClass(true);
        poCheckPaymentImports.setLogWrapper(poLogWrapper);
        poCheckPaymentImports.initialize();
        return poCheckPaymentImports;
    }

    public SOATagging SOATagging() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.SOATagging: Application driver is not set.");
            return null;
        }

        if (poSOATagging != null) {
            return poSOATagging;
        }

        poSOATagging = new SOATagging();
        poSOATagging.setApplicationDriver(poGRider);
        poSOATagging.setBranchCode(poGRider.getBranchCode());
        poSOATagging.setVerifyEntryNo(true);
        poSOATagging.setWithParent(false);
        poSOATagging.setLogWrapper(poLogWrapper);
        return poSOATagging;
    }

    public CheckPrintingRequest CheckPrintingRequest() {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowController.CheckPrintingRequest: Application driver is not set.");
            return null;
        }

        if (poCheckPrintingRequest != null) {
            return poCheckPrintingRequest;
        }

        poCheckPrintingRequest = new CheckPrintingRequest();
        poCheckPrintingRequest.setApplicationDriver(poGRider);
        poCheckPrintingRequest.setBranchCode(poGRider.getBranchCode());
        poCheckPrintingRequest.setLogWrapper(poLogWrapper);
        poCheckPrintingRequest.setVerifyEntryNo(true);
        poCheckPrintingRequest.setWithParent(false);
        return poCheckPrintingRequest;
    }

    public APPaymentAdjustment APPaymentAdjustment() {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.APPaymentAdjustment: Application driver is not set.");
            return null;
        }

        if (poAPPaymentAdjustment != null) {
            return poAPPaymentAdjustment;
        }

        poAPPaymentAdjustment = new APPaymentAdjustment();
        poAPPaymentAdjustment.setApplicationDriver(poGRider);
        poAPPaymentAdjustment.setWithParentClass(false);
        poAPPaymentAdjustment.setLogWrapper(poLogWrapper);
        poAPPaymentAdjustment.initialize();
        return poAPPaymentAdjustment;
    }

    public AccountChart AccountChart() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.AccountChart: Application driver is not set.");
            return null;
        }

        if (poAccountChart != null) {
            return poAccountChart;
        }

        poAccountChart = new AccountChart();
        poAccountChart.setApplicationDriver(poGRider);
        poAccountChart.setWithParentClass(false);
        poAccountChart.setLogWrapper(poLogWrapper);
        poAccountChart.initialize();
        return poAccountChart;
    }

    public TransactionAccountChart TransactionAccountChart() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.TransactionAccountChart: Application driver is not set.");
            return null;
        }

        if (poTransactionAccountChart != null) {
            return poTransactionAccountChart;
        }

        poTransactionAccountChart = new TransactionAccountChart();
        poTransactionAccountChart.setApplicationDriver(poGRider);
        poTransactionAccountChart.setWithParentClass(false);
        poTransactionAccountChart.setLogWrapper(poLogWrapper);
        poTransactionAccountChart.initialize();
        poTransactionAccountChart.newRecord();
        return poTransactionAccountChart;
    }

    public Journal Journal() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.Journal: Application driver is not set.");
            return null;
        }

        if (poJournal != null) {
            return poJournal;
        }

        poJournal = new Journal();
        poJournal.setApplicationDriver(poGRider);
        poJournal.setBranchCode(poGRider.getBranchCode());
        poJournal.setLogWrapper(poLogWrapper);
        poJournal.setWithParent(true);
        return poJournal;
    }

    public JournalProposal JournalProposal() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.JournalProposal: Application driver is not set.");
            return null;
        }

        if (poJournalProposal != null) {
            return poJournalProposal;
        }

        poJournalProposal = new JournalProposal();
        poJournalProposal.setApplicationDriver(poGRider);
        poJournalProposal.setBranchCode(poGRider.getBranchCode());
        poJournalProposal.setLogWrapper(poLogWrapper);
        poJournalProposal.setWithParent(true);
        return poJournalProposal;
    }

    public DocumentMapping DocumentMapping() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.CheckPrinting: Application driver is not set.");
            return null;
        }

        if (poDocummentMapping != null) {
            return poDocummentMapping;
        }

        poDocummentMapping = new DocumentMapping();
        poDocummentMapping.setApplicationDriver(poGRider);
        poDocummentMapping.setBranchCode(poGRider.getBranchCode());
        poDocummentMapping.setLogWrapper(poLogWrapper);
        poDocummentMapping.setVerifyEntryNo(true);
        poDocummentMapping.setWithParent(false);
        return poDocummentMapping;
    }

    public CheckStatusUpdate CheckStatusUpdate() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.CheckPrinting: Application driver is not set.");
            return null;
        }

        if (poCheckStatusUpdate != null) {
            return poCheckStatusUpdate;
        }

        poCheckStatusUpdate = new CheckStatusUpdate();
        poCheckStatusUpdate.setApplicationDriver(poGRider);
        poCheckStatusUpdate.setBranchCode(poGRider.getBranchCode());
        poCheckStatusUpdate.setLogWrapper(poLogWrapper);
        poCheckStatusUpdate.setVerifyEntryNo(true);
        poCheckStatusUpdate.setWithParent(false);
        return poCheckStatusUpdate;
    }

    public CheckImporting CheckImporting() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.CheckPrinting: Application driver is not set.");
            return null;
        }

        if (poCheckImporting != null) {
            return poCheckImporting;
        }

        poCheckImporting = new CheckImporting();
        poCheckImporting.setApplicationDriver(poGRider);
        poCheckImporting.setBranchCode(poGRider.getBranchCode());
        poCheckImporting.setLogWrapper(poLogWrapper);
        poCheckImporting.setWithParent(false);
        return poCheckImporting;
    }

    public OtherPayments OtherPayments() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.OtherPayments: Application driver is not set.");
            return null;
        }

        if (poOtherPayments != null) {
            return poOtherPayments;
        }

        poOtherPayments = new OtherPayments();
        poOtherPayments.setApplicationDriver(poGRider);
        poOtherPayments.setWithParentClass(true);
        poOtherPayments.setLogWrapper(poLogWrapper);
        poOtherPayments.initialize();
        return poOtherPayments;
    }

    public OtherPaymentStatusUpdate OtherPaymentStatusUpdate() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.OtherPaymentStatusUpdate: Application driver is not set.");
            return null;
        }

        if (poOtherPaymentStatusUpdate != null) {
            return poOtherPaymentStatusUpdate;
        }

        poOtherPaymentStatusUpdate = new OtherPaymentStatusUpdate();
        poOtherPaymentStatusUpdate.setApplicationDriver(poGRider);
        poOtherPaymentStatusUpdate.setBranchCode(poGRider.getBranchCode());
        poOtherPaymentStatusUpdate.setLogWrapper(poLogWrapper);
        poOtherPaymentStatusUpdate.setVerifyEntryNo(true);
        poOtherPaymentStatusUpdate.setWithParent(false);
        return poOtherPaymentStatusUpdate;
    }

    public BIR2307Filler BIR2307Filler() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.BIR2307Filler: Application driver is not set.");
            return null;
        }

        if (poBIR2307Filler != null) {
            return poBIR2307Filler;
        }
        poOtherPayments = new OtherPayments();
        return poBIR2307Filler;
    }

    public WithholdingTax WithholdingTax() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.WithholdingTax: Application driver is not set.");
            return null;
        }

        if (poWithholdingTax != null) {
            return poWithholdingTax;
        }

        poWithholdingTax = new WithholdingTax();
        poWithholdingTax.setApplicationDriver(poGRider);
        poWithholdingTax.setWithParentClass(false);
        poWithholdingTax.setLogWrapper(poLogWrapper);
        poWithholdingTax.initialize();
        poWithholdingTax.newRecord();
        return poWithholdingTax;
    }

    public WithholdingTaxDeductions WithholdingTaxDeductions() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.WithholdingTaxDeductions: Application driver is not set.");
            return null;
        }

        if (poWithholdingTaxDeductions != null) {
            return poWithholdingTaxDeductions;
        }

        poWithholdingTaxDeductions = new WithholdingTaxDeductions();
        poWithholdingTaxDeductions.setApplicationDriver(poGRider);
        poWithholdingTaxDeductions.setWithParentClass(false);
        poWithholdingTaxDeductions.setLogWrapper(poLogWrapper);
        poWithholdingTaxDeductions.initialize();
        poWithholdingTaxDeductions.newRecord();
        return poWithholdingTaxDeductions;
    }

    public CheckTransfers CheckTransfers() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckTransfers: Application driver is not set.");
            return null;
        }

        if (poCheckTransfer != null) {
            return poCheckTransfer;
        }

        poCheckTransfer = new CheckTransfers();
        poCheckTransfer.setApplicationDriver(poGRider);
        poCheckTransfer.setBranchCode(poGRider.getBranchCode());
        poCheckTransfer.setLogWrapper(poLogWrapper);
        poCheckTransfer.setVerifyEntryNo(true);
        poCheckTransfer.setWithParent(false);
        return poCheckTransfer;
    }

    public CheckDeposits CheckDeposits() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckDeposits: Application driver is not set.");
            return null;
        }

        if (poCheckDeposits != null) {
            return poCheckDeposits;
        }

        poCheckDeposits = new CheckDeposits();
        poCheckDeposits.setApplicationDriver(poGRider);
        poCheckDeposits.setBranchCode(poGRider.getBranchCode());
        poCheckDeposits.setLogWrapper(poLogWrapper);
        poCheckDeposits.setVerifyEntryNo(true);
        poCheckDeposits.setWithParent(false);
        return poCheckDeposits;
    }
    
        public CheckDeposit CheckDeposit() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckDeposit: Application driver is not set.");
            return null;
        }

        if (poCheckDeposit != null) {
            return poCheckDeposit;
        }

        poCheckDeposit = new CheckDeposit();
        poCheckDeposit.setApplicationDriver(poGRider);
        poCheckDeposit.setBranchCode(poGRider.getBranchCode());
        poCheckDeposit.setLogWrapper(poLogWrapper);
        poCheckDeposit.setVerifyEntryNo(true);
        poCheckDeposit.setWithParent(false);
        return poCheckDeposit;
    }

    public CheckReleases CheckReleases() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CheckRelease: Application driver is not set.");
            return null;
        }

        if (poCheckRelease != null) {
            return poCheckRelease;
        }

        poCheckRelease = new CheckReleases();
        poCheckRelease.setApplicationDriver(poGRider);
        poCheckRelease.setBranchCode(poGRider.getBranchCode());
        poCheckRelease.setLogWrapper(poLogWrapper);
        poCheckRelease.setVerifyEntryNo(true);
        poCheckRelease.setWithParent(false);
        return poCheckRelease;
    }

    public RecurringExpense RecurringExpense() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashflowControllers.RecurringExpense: Application driver is not set.");
            return null;
        }

        if (poRecurringExpense != null) {
            return poRecurringExpense;
        }

        poRecurringExpense = new RecurringExpense();
        poRecurringExpense.setApplicationDriver(poGRider);
        poRecurringExpense.setWithParentClass(false);
        poRecurringExpense.setLogWrapper(poLogWrapper);
        poRecurringExpense.initialize();
        poRecurringExpense.newRecord();
        return poRecurringExpense;
    }

    public RecurringExpenseSchedule RecurringExpenseSchedule() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.RecurringExpenseSchedule: Application driver is not set.");
            return null;
        }

        if (poRecurringExpenseSchedule != null) {
            return poRecurringExpenseSchedule;
        }

        poRecurringExpenseSchedule = new RecurringExpenseSchedule();
        poRecurringExpenseSchedule.setApplicationDriver(poGRider);
        poRecurringExpenseSchedule.setWithParentClass(false);
        poRecurringExpenseSchedule.setLogWrapper(poLogWrapper);
        poRecurringExpenseSchedule.initialize();
        return poRecurringExpenseSchedule;
    }

    public CashFund CashFund() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CashFund: Application driver is not set.");
            return null;
        }

        if (poCashFund != null) {
            return poCashFund;
        }

        poCashFund = new CashFund();
        poCashFund.setApplicationDriver(poGRider);
        poCashFund.setWithParentClass(false);
        poCashFund.setLogWrapper(poLogWrapper);
        poCashFund.initialize();
        poCashFund.newRecord();
        return poCashFund;
    }

    public PettyCash PettyCash() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CashFund: Application driver is not set.");
            return null;
        }

        if (poCashFund != null) {
            return poPettyCash;
        }

        poPettyCash = new PettyCash();
        poPettyCash.setApplicationDriver(poGRider);
        poPettyCash.setWithParentClass(false);
        poPettyCash.setLogWrapper(poLogWrapper);
        poPettyCash.initialize();
        poPettyCash.newRecord();
        return poPettyCash;
    }

    public CashAdvance CashAdvance() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CashAdvance: Application driver is not set.");
            return null;
        }

        if (poCashAdvance != null) {
            return poCashAdvance;
        }

        poCashAdvance = new CashAdvance();
        poCashAdvance.setApplicationDriver(poGRider);
        poCashAdvance.setBranchCode(poGRider.getBranchCode());
        poCashAdvance.setLogWrapper(poLogWrapper);
        poCashAdvance.setVerifyEntryNo(false);
        poCashAdvance.setWithParent(false);
        return poCashAdvance;
    }

    public CashLiquidation CashLiquidation() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CashLiquidation: Application driver is not set.");
            return null;
        }

        if (poCashLiquidation != null) {
            return poCashLiquidation;
        }

        poCashLiquidation = new CashLiquidation();
        poCashLiquidation.setApplicationDriver(poGRider);
        poCashLiquidation.setBranchCode(poGRider.getBranchCode());
        poCashLiquidation.setLogWrapper(poLogWrapper);
        poCashLiquidation.setVerifyEntryNo(false);
        poCashLiquidation.setWithParent(false);
        return poCashLiquidation;
    }

    public CashDisbursement CashDisbursement() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.CashDisbursement: Application driver is not set.");
            return null;
        }

        if (poCashDisbursement != null) {
            return poCashDisbursement;
        }

        poCashDisbursement = new CashDisbursement();
        poCashDisbursement.setApplicationDriver(poGRider);
        poCashDisbursement.setBranchCode(poGRider.getBranchCode());
        poCashDisbursement.setLogWrapper(poLogWrapper);
        poCashDisbursement.setVerifyEntryNo(true);
        poCashDisbursement.setWithParent(false);
        return poCashDisbursement;
    }


    public PettyCashDisbursement PettyCashDisbursement() {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.PettyCashDisbursement: Application driver is not set.");
            return null;
        }

        if (poPettyCashDisbursement != null) {
            return poPettyCashDisbursement;
        }

        poPettyCashDisbursement = new PettyCashDisbursement();
        poPettyCashDisbursement.setApplicationDriver(poGRider);
        poPettyCashDisbursement.setBranchCode(poGRider.getBranchCode());
        poPettyCashDisbursement.setLogWrapper(poLogWrapper);
        poPettyCashDisbursement.setVerifyEntryNo(true);
        poPettyCashDisbursement.setWithParent(false);
        return poPettyCashDisbursement;
    }
    
    public ReplenishmentRequest ReplenishmentRequest() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("CashFlowcontrollers.ReplenishmentRequest: Application driver is not set.");
            return null;
        }

        if (poReplenishmentRequest != null) {
            return poReplenishmentRequest;
        }

        poReplenishmentRequest = new ReplenishmentRequest();
        poReplenishmentRequest.setApplicationDriver(poGRider);
        poReplenishmentRequest.setWithParentClass(false);
        poReplenishmentRequest.setLogWrapper(poLogWrapper);
        poReplenishmentRequest.initialize();
        poReplenishmentRequest.newRecord();
        return poReplenishmentRequest;
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            poCachePayable = null;
            poBankAccountMaster = null;
            poRecurringIssuance = null;
            poCheckTransfer = null;
            poCheckDeposits = null;
            poCheckDeposit = null;
            poCheckRelease = null;
            poParticular = null;
            poPayee = null;
            poRecurringIssuance = null;
            poCachePayable = null;
            poBankAccountMaster = null;
            poDisbursementVoucher = null;
            poJournalProposal = null;
            poDisbursement = null;
            poDisbursementBase = null;
            poDisbursement_PRF = null;
            poCheckPayments = null;
            poCheckPaymentImports = null;
            poPaymentRequest = null;
            poCheckPrintingRequest = null;
            poAccountChart = null;
            poTransactionAccountChart = null;
            poDocummentMapping = null;
            poCheckStatusUpdate = null;
            poCheckImporting = null;
            poBIR2307Filler = null;
            poWithholdingTax = null;
            poWithholdingTaxDeductions = null;
            poOtherPayments = null;
            poOtherPaymentStatusUpdate = null;
            poCashAdvance = null;
            poRecurringExpense = null;
            poRecurringExpenseSchedule = null;
            poCashFund = null;
            poCashLiquidation = null;
            poCashDisbursement = null;
            poPettyCashDisbursement = null;
            poReplenishmentRequest = null;
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }

    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;

    private BankAccountMaster poBankAccountMaster;
    private CachePayable poCachePayable;
    private RecurringIssuance poRecurringIssuance;
    private Particular poParticular;
    private Payee poPayee;
    private PaymentRequest poPaymentRequest;
    private DisbursementVoucher poDisbursementVoucher;
    private Disbursement poDisbursement;
    private Disbursement poDisbursementBase;
    private Disbursement_PRF poDisbursement_PRF;
    private CheckPayments poCheckPayments;
    private SOATagging poSOATagging;
    private APPaymentAdjustment poAPPaymentAdjustment;
    private CheckPrintingRequest poCheckPrintingRequest;
    private AccountChart poAccountChart;
    private TransactionAccountChart poTransactionAccountChart;
    private Journal poJournal;
    private JournalProposal poJournalProposal;
    private DocumentMapping poDocummentMapping;
    private CheckStatusUpdate poCheckStatusUpdate;
    private CheckImporting poCheckImporting;
    private OtherPayments poOtherPayments;
    private OtherPaymentStatusUpdate poOtherPaymentStatusUpdate;
    private CheckPaymentImporting poCheckPaymentImports;
    private BIR2307Filler poBIR2307Filler;
    private WithholdingTax poWithholdingTax;
    private WithholdingTaxDeductions poWithholdingTaxDeductions;
    private CashAdvance poCashAdvance;
    private CheckTransfers poCheckTransfer;
    private CheckDeposits poCheckDeposits;
    private CheckDeposit poCheckDeposit;
    private CheckReleases poCheckRelease;
    private RecurringExpense poRecurringExpense;
    private RecurringExpenseSchedule poRecurringExpenseSchedule;
    private CashFund poCashFund;
    private PettyCash poPettyCash;
    private CashLiquidation poCashLiquidation;
    private CashDisbursement poCashDisbursement;
    private PettyCashDisbursement poPettyCashDisbursement;
    private ReplenishmentRequest poReplenishmentRequest;
}
