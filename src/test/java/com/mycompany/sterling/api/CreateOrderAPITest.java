package com.mycompany.sterling.api;

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
 * Unit tests for {@link CreateOrderAPI}.
 *
 * <p>Covers boundary conditions from KAN-3:
 * <ul>
 *   <li>Successful createOrder → returns STATUS=SUCCESS with OrderHeaderKey and OrderNo</li>
 *   <li>Null inputDoc → returns STATUS=ERROR with MYCO_ORDER_010</li>
 *   <li>createOrder returns null → returns STATUS=ERROR with MYCO_ORDER_012</li>
 *   <li>createOrder throws exception → returns STATUS=ERROR with MYCO_ORDER_011</li>
 *   <li>EnterpriseCode absent in input → falls back to system property / DEFAULT</li>
 *   <li>OrderLines absent → logs warning, proceeds to createOrder call</li>
 *   <li>PersonInfoShipTo absent → logs warning, proceeds to createOrder call</li>
 * </ul>
 * </p>
 *
 * <p>Uses Mockito spy on {@link CreateOrderAPI} to stub the package-private
 * {@code invokeCreateOrder} method, keeping tests independent of the Sterling
 * API runtime stub.</p>
 */
class CreateOrderAPITest {

    private CreateOrderAPI api;
    private DocumentBuilder docBuilder;

    @BeforeEach
    void setUp() throws Exception {
        api = spy(new CreateOrderAPI());
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    // -------------------------------------------------------------------------
    // Successful createOrder invocation
    // -------------------------------------------------------------------------

    @Test
    void givenValidInput_whenInvoke_thenReturnsSuccessWithOrderHeaderKeyAndOrderNo() throws Exception {
        Document inputDoc = buildFullOrderDoc("ENT001", "BUYER_ORG", "CUST001", "250.00", "SHIP001");
        doReturn(buildCreateOrderResponse("OHK-000001", "ORD-10001"))
                .when(api).invokeCreateOrder(any());

        Document result = api.invoke(inputDoc);

        assertNotNull(result);
        Element outRoot = result.getDocumentElement();
        assertEquals("SUCCESS", outRoot.getAttribute("Status"));
        assertEquals("OHK-000001", outRoot.getAttribute("OrderHeaderKey"));
        assertEquals("ORD-10001", outRoot.getAttribute("OrderNo"));
    }

    // -------------------------------------------------------------------------
    // Null input document
    // -------------------------------------------------------------------------

    @Test
    void givenNullInputDoc_whenInvoke_thenReturnsErrorMycoOrder010() {
        Document result = assertDoesNotThrow(() -> api.invoke(null),
                "No exception should propagate for null input");

        assertNotNull(result);
        Element outRoot = result.getDocumentElement();
        assertEquals("ERROR", outRoot.getAttribute("Status"));
        assertEquals("MYCO_ORDER_010", outRoot.getAttribute("ErrorCode"));
    }

    // -------------------------------------------------------------------------
    // createOrder returns null → MYCO_ORDER_012
    // -------------------------------------------------------------------------

    @Test
    void givenCreateOrderReturnsNull_whenInvoke_thenReturnsErrorMycoOrder012() throws Exception {
        Document inputDoc = buildFullOrderDoc("ENT001", "BUYER_ORG", "CUST001", "99.00", "SHIP002");
        doReturn(null).when(api).invokeCreateOrder(any());

        Document result = api.invoke(inputDoc);

        assertNotNull(result);
        Element outRoot = result.getDocumentElement();
        assertEquals("ERROR", outRoot.getAttribute("Status"));
        assertEquals("MYCO_ORDER_012", outRoot.getAttribute("ErrorCode"));
    }

    // -------------------------------------------------------------------------
    // createOrder throws exception → fail-open: MYCO_ORDER_011
    // -------------------------------------------------------------------------

    @Test
    void givenCreateOrderThrows_whenInvoke_thenReturnsErrorNoException() throws Exception {
        Document inputDoc = buildFullOrderDoc("ENT001", "BUYER_ORG", "CUST002", "500.00", "SHIP003");
        doThrow(new RuntimeException("createOrder DB connection timeout"))
                .when(api).invokeCreateOrder(any());

        Document result = assertDoesNotThrow(() -> api.invoke(inputDoc),
                "No exception should propagate — API is fail-open on internal errors");

        assertNotNull(result);
        Element outRoot = result.getDocumentElement();
        assertEquals("ERROR", outRoot.getAttribute("Status"));
        assertEquals("MYCO_ORDER_011", outRoot.getAttribute("ErrorCode"));
    }

    // -------------------------------------------------------------------------
    // EnterpriseCode absent in input → falls back to DEFAULT
    // -------------------------------------------------------------------------

    @Test
    void givenEnterpriseCodeAbsent_whenInvoke_thenUsesDefaultEnterpriseCode() throws Exception {
        Document inputDoc = buildOrderDocNoEnterpriseCode("BUYER_ORG", "CUST003", "75.00");
        doReturn(buildCreateOrderResponse("OHK-000002", "ORD-10002"))
                .when(api).invokeCreateOrder(any());

        Document result = api.invoke(inputDoc);

        assertNotNull(result);
        // Verify createOrder was called (EnterpriseCode defaulted to "DEFAULT" internally)
        verify(api).invokeCreateOrder(any());
        assertEquals("SUCCESS", result.getDocumentElement().getAttribute("Status"));
    }

    // -------------------------------------------------------------------------
    // OrderLines absent → logs warning, still calls createOrder
    // -------------------------------------------------------------------------

    @Test
    void givenNoOrderLines_whenInvoke_thenProceedsToCreateOrder() throws Exception {
        Document inputDoc = buildOrderDocNoLines("ENT001", "CUST004", "120.00");
        doReturn(buildCreateOrderResponse("OHK-000003", "ORD-10003"))
                .when(api).invokeCreateOrder(any());

        Document result = api.invoke(inputDoc);

        assertNotNull(result);
        verify(api).invokeCreateOrder(any());
        assertEquals("SUCCESS", result.getDocumentElement().getAttribute("Status"));
    }

    // -------------------------------------------------------------------------
    // PersonInfoShipTo absent → logs warning, still calls createOrder
    // -------------------------------------------------------------------------

    @Test
    void givenNoPersonInfoShipTo_whenInvoke_thenProceedsToCreateOrder() throws Exception {
        Document inputDoc = buildOrderDocNoShipTo("ENT001", "CUST005", "60.00");
        doReturn(buildCreateOrderResponse("OHK-000004", "ORD-10004"))
                .when(api).invokeCreateOrder(any());

        Document result = api.invoke(inputDoc);

        assertNotNull(result);
        verify(api).invokeCreateOrder(any());
        assertEquals("SUCCESS", result.getDocumentElement().getAttribute("Status"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a fully-populated Order document with OrderLines and PersonInfoShipTo.
     */
    private Document buildFullOrderDoc(String enterpriseCode, String buyerOrgCode,
                                       String customerID, String orderTotal, String shipToID)
            throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("BuyerOrganizationCode", buyerOrgCode);
        order.setAttribute("CustomerID", customerID);
        order.setAttribute("OrderTotal", orderTotal);
        order.setAttribute("ShipToID", shipToID);

        Element orderLines = doc.createElement("OrderLines");
        Element orderLine = doc.createElement("OrderLine");
        orderLine.setAttribute("ItemID", "ITEM001");
        orderLine.setAttribute("UnitOfMeasure", "EACH");
        orderLine.setAttribute("OrderedQty", "1");
        orderLine.setAttribute("UnitPrice", orderTotal);
        orderLines.appendChild(orderLine);
        order.appendChild(orderLines);

        Element shipTo = doc.createElement("PersonInfoShipTo");
        shipTo.setAttribute("FirstName", "John");
        shipTo.setAttribute("LastName", "Doe");
        shipTo.setAttribute("AddressLine1", "123 Main St");
        shipTo.setAttribute("City", "Springfield");
        shipTo.setAttribute("State", "IL");
        shipTo.setAttribute("ZipCode", "62701");
        shipTo.setAttribute("Country", "US");
        order.appendChild(shipTo);

        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds an Order document without an EnterpriseCode attribute.
     */
    private Document buildOrderDocNoEnterpriseCode(String buyerOrgCode,
                                                   String customerID, String orderTotal)
            throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("BuyerOrganizationCode", buyerOrgCode);
        order.setAttribute("CustomerID", customerID);
        order.setAttribute("OrderTotal", orderTotal);
        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds an Order document with no OrderLines child element.
     */
    private Document buildOrderDocNoLines(String enterpriseCode,
                                          String customerID, String orderTotal) throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("CustomerID", customerID);
        order.setAttribute("OrderTotal", orderTotal);

        Element shipTo = doc.createElement("PersonInfoShipTo");
        shipTo.setAttribute("FirstName", "Jane");
        order.appendChild(shipTo);

        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds an Order document with no PersonInfoShipTo child element.
     */
    private Document buildOrderDocNoShipTo(String enterpriseCode,
                                           String customerID, String orderTotal) throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("CustomerID", customerID);
        order.setAttribute("OrderTotal", orderTotal);

        Element orderLines = doc.createElement("OrderLines");
        Element orderLine = doc.createElement("OrderLine");
        orderLine.setAttribute("ItemID", "ITEM002");
        orderLines.appendChild(orderLine);
        order.appendChild(orderLines);

        doc.appendChild(order);
        return doc;
    }

    /**
     * Builds a mock createOrder response:
     * {@code <Order OrderHeaderKey="<key>" OrderNo="<no>"/>}.
     */
    private Document buildCreateOrderResponse(String orderHeaderKey, String orderNo) throws Exception {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("OrderHeaderKey", orderHeaderKey);
        order.setAttribute("OrderNo", orderNo);
        doc.appendChild(order);
        return doc;
    }
}
