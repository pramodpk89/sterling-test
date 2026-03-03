package com.mycompany.sterling.holds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FlaggedCustomerHoldRule}.
 *
 * <p>Covers all boundary conditions from KAN-7:
 * <ul>
 *   <li>CustomerFlag="FLAGGED" → hold applied, returns true</li>
 *   <li>CustomerFlag="RESTRICTED" → hold applied, returns true</li>
 *   <li>CustomerFlag="PREFERRED" → no hold, returns false</li>
 *   <li>CustomerFlag absent/empty → no hold, returns false</li>
 *   <li>getCustomerList throws → logs ERROR MYCO_HOLD_002, returns false, no exception</li>
 *   <li>CustomerID missing from order → logs WARN MYCO_HOLD_002, returns false, no exception</li>
 *   <li>changeOrder throws → logs ERROR MYCO_HOLD_003, returns false, no exception</li>
 * </ul>
 * </p>
 *
 * <p>Uses Mockito spy on {@link FlaggedCustomerHoldRule} to stub the package-private
 * {@code invokeGetCustomerList} and {@code invokeChangeOrder} methods, keeping tests
 * independent of the Sterling API runtime stub.</p>
 */
class FlaggedCustomerHoldRuleTest {

    private FlaggedCustomerHoldRule rule;
    private DocumentBuilder docBuilder;

    @BeforeEach
    void setUp() throws Exception {
        rule = spy(new FlaggedCustomerHoldRule());
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    // -------------------------------------------------------------------------
    // CustomerFlag = FLAGGED → hold applied
    // -------------------------------------------------------------------------

    @Test
    void givenFlaggedCustomer_whenEvaluate_thenHoldAppliedReturnsTrue() throws Exception {
        Document orderDoc = buildOrder("CUST-8812", "MYCO_ENT", "OHK-000001");
        doReturn(buildCustomerListResponse("FLAGGED")).when(rule).invokeGetCustomerList(any());

        boolean result = rule.evaluateFlaggedCustomerHold(orderDoc);

        assertTrue(result, "Expected hold to be applied for FLAGGED customer");
        verify(rule).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // CustomerFlag = RESTRICTED → hold applied
    // -------------------------------------------------------------------------

    @Test
    void givenRestrictedCustomer_whenEvaluate_thenHoldAppliedReturnsTrue() throws Exception {
        Document orderDoc = buildOrder("CUST-9001", "MYCO_ENT", "OHK-000002");
        doReturn(buildCustomerListResponse("RESTRICTED")).when(rule).invokeGetCustomerList(any());

        boolean result = rule.evaluateFlaggedCustomerHold(orderDoc);

        assertTrue(result, "Expected hold to be applied for RESTRICTED customer");
        verify(rule).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // CustomerFlag = PREFERRED → no hold
    // -------------------------------------------------------------------------

    @Test
    void givenPreferredCustomer_whenEvaluate_thenNoHoldReturnsFalse() throws Exception {
        Document orderDoc = buildOrder("CUST-1001", "MYCO_ENT", "OHK-000003");
        doReturn(buildCustomerListResponse("PREFERRED")).when(rule).invokeGetCustomerList(any());

        boolean result = rule.evaluateFlaggedCustomerHold(orderDoc);

        assertFalse(result, "Expected no hold for PREFERRED customer");
        verify(rule, never()).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // CustomerFlag absent → no hold
    // -------------------------------------------------------------------------

    @Test
    void givenCustomerFlagAbsent_whenEvaluate_thenNoHoldReturnsFalse() throws Exception {
        Document orderDoc = buildOrder("CUST-2002", "MYCO_ENT", "OHK-000004");
        // Customer element present but no CustomerFlag attribute
        doReturn(buildCustomerListResponse(null)).when(rule).invokeGetCustomerList(any());

        boolean result = rule.evaluateFlaggedCustomerHold(orderDoc);

        assertFalse(result, "Expected no hold when CustomerFlag is absent");
        verify(rule, never()).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // getCustomerList throws → fail-open: logs ERROR, returns false, no exception
    // -------------------------------------------------------------------------

    @Test
    void givenGetCustomerListThrows_whenEvaluate_thenReturnsFalseNoException() throws Exception {
        Document orderDoc = buildOrder("CUST-3003", "MYCO_ENT", "OHK-000005");
        doThrow(new RuntimeException("getCustomerList connection timeout"))
                .when(rule).invokeGetCustomerList(any());

        boolean result = assertDoesNotThrow(() -> rule.evaluateFlaggedCustomerHold(orderDoc),
                "No exception should propagate — rule is fail-open");

        assertFalse(result, "Expected false when getCustomerList throws");
        verify(rule, never()).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // CustomerID missing from order → logs WARN, returns false, no exception
    // -------------------------------------------------------------------------

    @Test
    void givenMissingCustomerId_whenEvaluate_thenReturnsFalseNoException() throws Exception {
        Document orderDoc = buildOrderNoCustomerId("MYCO_ENT", "OHK-000006");

        boolean result = assertDoesNotThrow(() -> rule.evaluateFlaggedCustomerHold(orderDoc),
                "No exception should propagate when CustomerID is missing");

        assertFalse(result, "Expected false when CustomerID is missing");
        verify(rule, never()).invokeGetCustomerList(any());
        verify(rule, never()).invokeChangeOrder(any());
    }

    // -------------------------------------------------------------------------
    // changeOrder throws → fail-open: logs ERROR MYCO_HOLD_003, returns false
    // -------------------------------------------------------------------------

    @Test
    void givenChangeOrderThrows_whenEvaluate_thenReturnsFalseNoException() throws Exception {
        Document orderDoc = buildOrder("CUST-4004", "MYCO_ENT", "OHK-000007");
        doReturn(buildCustomerListResponse("FLAGGED")).when(rule).invokeGetCustomerList(any());
        doThrow(new RuntimeException("changeOrder DB timeout"))
                .when(rule).invokeChangeOrder(any());

        boolean result = assertDoesNotThrow(() -> rule.evaluateFlaggedCustomerHold(orderDoc),
                "No exception should propagate — changeOrder failure is fail-open");

        assertFalse(result, "Expected false when changeOrder throws");
    }

    // -------------------------------------------------------------------------
    // Null orderDoc → fail-open
    // -------------------------------------------------------------------------

    @Test
    void givenNullOrderDoc_whenEvaluate_thenReturnsFalseNoException() {
        boolean result = assertDoesNotThrow(() -> rule.evaluateFlaggedCustomerHold(null),
                "No exception should propagate for null input");

        assertFalse(result, "Expected false for null orderDoc");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds an {@code <Order>} document with CustomerID, EnterpriseCode, and OrderHeaderKey.
     */
    private Document buildOrder(String customerId, String enterpriseCode, String orderHeaderKey)
            throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("CustomerID", customerId);
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("OrderHeaderKey", orderHeaderKey);
        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds an {@code <Order>} document without a CustomerID attribute.
     */
    private Document buildOrderNoCustomerId(String enterpriseCode, String orderHeaderKey)
            throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("OrderHeaderKey", orderHeaderKey);
        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds a mock {@code getCustomerList} response:
     * {@code <CustomerList><Customer CustomerFlag="<flag>"/></CustomerList>}.
     * When {@code flag} is null the attribute is omitted (simulates absent CustomerFlag).
     */
    private Document buildCustomerListResponse(String flag) throws Exception {
        Document doc = docBuilder.newDocument();
        Element customerList = doc.createElement("CustomerList");
        doc.appendChild(customerList);

        Element customer = doc.createElement("Customer");
        if (flag != null) {
            customer.setAttribute("CustomerFlag", flag);
        }
        customerList.appendChild(customer);
        return doc;
    }
}
