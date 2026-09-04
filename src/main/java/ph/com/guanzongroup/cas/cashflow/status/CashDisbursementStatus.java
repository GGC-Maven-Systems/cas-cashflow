/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.status;

/**
 *
 * @author Arsiela 03-24-2026
 */
public class CashDisbursementStatus {
    public static final String OPEN = "0";
    public static final  String CONFIRMED = "1";
    public static final  String APPROVED = "2";
    public static final  String CANCELLED = "3";
    public static final  String VOID = "4";

    //Additional Status - Arsiela 06-27-2026
    public static final  String VERIFIED = "8";
    public static final  String RETURNED = "9";
    
    public static final String DEFAULT_VOUCHER_NO = "00000001";
    
    public static class Reverse  {
        public static final  String INCLUDE = "+"; 
        public static final  String EXCLUDE = "-"; 
    }
    
    public static class SourceCode  {
        public static final  String CASHADVANCE = "CADV"; 
        public static final  String PAYMENTREQUEST = "PRFx"; 
    }
    
}
