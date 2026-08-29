/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ph.com.guanzongroup.cas.cashflow.status;

/**
 *
 * @author Arsiela 08/22/2026
 */
public class ReplenishmentRequestStatus { 
    public static final  String OPEN = "0";
    public static final  String APPROVED = "1";
    public static final  String POSTED = "2"; 
    public static final  String CANCELLED = "3";
    public static final  String VOID = "4";
    
    public static class SourceCode  {
        public static final  String REPLENISHMENT = "REPL"; 
    }
}
