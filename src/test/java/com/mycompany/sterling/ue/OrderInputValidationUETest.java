package com.mycompany.sterling.ue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OrderInputValidationUE}.
 *
 * <p>Covers all validation rules from KAN-4:
 * <ul>
 *   <li>Required header attributes (EnterpriseCode, BuyerOrganizationCode)</li>
 *   <li>Presence and non-emptiness of OrderLines</li>
 *   <li>Per-line required fields: ItemID, UnitOfMeasure, OrderedQty</li>
 *   <li>Quantity boundary conditions (zero, negative, non-numeric)</li>
 *   <li>Graceful handling of null input</li>
 * </ul>
 * </p>
 */
class OrderInputValidationUETest {

    private OrderInputValidationUE ue;
    private DocumentBuilder docBuilder;

    @BeforeEach
    void setUp() throws Exception {
        ue = new OrderInputValidationUE();
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void givenValidOrder_whenBeforeCreateOrder_thenNoException() {
        Document doc = buildValidOrder("DEFAULT", "BUYER001",
                new LineSpec("ITEM001", "EACH", "5"));
        assertDoesNotThrow(() -> ue.beforeCreateOrder(doc));
    }

    @Test
    void givenMultipleValidLines_whenBeforeCreateOrder_thenNoException() {
        Document doc = buildValidOrderMultiLine("ENT", "ORG",
                new LineSpec("ITEM_A", "EACH", "1"),
                new LineSpec("ITEM_B", "BOX",  "10.5"));
        assertDoesNotThrow(() -> ue.beforeCreateOrder(doc));
    }

    @Test
    void givenQtyOnOrderLineTranQuantity_whenBeforeCreateOrder_thenNoException() {
        Document doc = buildOrderWithTranQty("ENT", "ORG", "ITEM001", "EACH", "3");
        assertDoesNotThrow(() -> ue.beforeCreateOrder(doc));
    }

    // -------------------------------------------------------------------------
    // Null / empty doc
    // -------------------------------------------------------------------------

    @Test
    void givenNullInputDoc_whenBeforeCreateOrder_thenNoException() {
        // fail-open: null doc is logged and skipped, not thrown
        assertDoesNotThrow(() -> ue.beforeCreateOrder(null));
    }

    // -------------------------------------------------------------------------
    // Header attribute validation
    // -------------------------------------------------------------------------

    @Test
    void givenMissingEnterpriseCode_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder(null, "BUYER001", new LineSpec("ITEM1", "EACH", "1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_010"),
                "Error message should contain MYCO_ORDER_010");
    }

    @Test
    void givenEmptyEnterpriseCode_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("", "BUYER001", new LineSpec("ITEM1", "EACH", "1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_010"));
    }

    @Test
    void givenMissingBuyerOrganizationCode_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("DEFAULT", null, new LineSpec("ITEM1", "EACH", "1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_011"),
                "Error message should contain MYCO_ORDER_011");
    }

    @Test
    void givenEmptyBuyerOrganizationCode_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("DEFAULT", "", new LineSpec("ITEM1", "EACH", "1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_011"));
    }

    // -------------------------------------------------------------------------
    // OrderLines validation
    // -------------------------------------------------------------------------

    @Test
    void givenMissingOrderLines_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildOrderNoLines("ENT", "ORG");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_012"));
    }

    @Test
    void givenEmptyOrderLines_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildOrderEmptyLines("ENT", "ORG");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_012"));
    }

    // -------------------------------------------------------------------------
    // Per-line field validation
    // -------------------------------------------------------------------------

    @Test
    void givenMissingItemId_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec(null, "EACH", "2"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_013"));
    }

    @Test
    void givenEmptyItemId_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("", "EACH", "2"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_013"));
    }

    @Test
    void givenMissingUnitOfMeasure_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", null, "2"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_014"));
    }

    @Test
    void givenEmptyUnitOfMeasure_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", "", "2"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_014"));
    }

    @Test
    void givenMissingOrderedQty_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", "EACH", null));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_015"));
    }

    @Test
    void givenZeroOrderedQty_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", "EACH", "0"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_015"));
    }

    @Test
    void givenNegativeOrderedQty_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", "EACH", "-1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_015"));
    }

    @Test
    void givenNonNumericOrderedQty_whenBeforeCreateOrder_thenThrows() {
        Document doc = buildValidOrder("ENT", "ORG", new LineSpec("ITEM1", "EACH", "abc"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("MYCO_ORDER_015"));
    }

    // -------------------------------------------------------------------------
    // Multi-line: error reports correct line number
    // -------------------------------------------------------------------------

    @Test
    void givenSecondLineInvalid_whenBeforeCreateOrder_thenErrorMentionsLine2() {
        Document doc = buildValidOrderMultiLine("ENT", "ORG",
                new LineSpec("ITEM_A", "EACH", "1"),
                new LineSpec(null,     "EACH", "1")); // ItemID missing on line 2
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ue.beforeCreateOrder(doc));
        assertTrue(ex.getMessage().contains("OrderLine[2]"),
                "Error should reference OrderLine[2], got: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Simple holder for one line's required fields (null = omit the attribute). */
    private record LineSpec(String itemId, String uom, String qty) {}

    private Document buildValidOrder(String enterpriseCode, String buyerOrgCode, LineSpec line) {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        if (enterpriseCode != null) order.setAttribute("EnterpriseCode", enterpriseCode);
        if (buyerOrgCode   != null) order.setAttribute("BuyerOrganizationCode", buyerOrgCode);
        doc.appendChild(order);

        Element orderLines = doc.createElement("OrderLines");
        order.appendChild(orderLines);
        orderLines.appendChild(buildLine(doc, line, true));
        return doc;
    }

    private Document buildValidOrderMultiLine(String enterpriseCode, String buyerOrgCode,
                                              LineSpec... lines) {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", enterpriseCode);
        order.setAttribute("BuyerOrganizationCode", buyerOrgCode);
        doc.appendChild(order);

        Element orderLines = doc.createElement("OrderLines");
        order.appendChild(orderLines);
        for (LineSpec spec : lines) {
            orderLines.appendChild(buildLine(doc, spec, true));
        }
        return doc;
    }

    /** Builds an order where qty lives inside {@code <OrderLineTranQuantity>}. */
    private Document buildOrderWithTranQty(String ent, String org,
                                           String itemId, String uom, String qty) {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", ent);
        order.setAttribute("BuyerOrganizationCode", org);
        doc.appendChild(order);

        Element orderLines = doc.createElement("OrderLines");
        order.appendChild(orderLines);

        Element orderLine = doc.createElement("OrderLine");
        orderLines.appendChild(orderLine);

        Element item = doc.createElement("Item");
        item.setAttribute("ItemID", itemId);
        item.setAttribute("UnitOfMeasure", uom);
        orderLine.appendChild(item);

        Element tranQty = doc.createElement("OrderLineTranQuantity");
        tranQty.setAttribute("OrderedQty", qty);
        orderLine.appendChild(tranQty);

        return doc;
    }

    private Document buildOrderNoLines(String ent, String org) {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", ent);
        order.setAttribute("BuyerOrganizationCode", org);
        doc.appendChild(order);
        return doc;
    }

    private Document buildOrderEmptyLines(String ent, String org) {
        Document doc = docBuilder.newDocument();
        Element order = doc.createElement("Order");
        order.setAttribute("EnterpriseCode", ent);
        order.setAttribute("BuyerOrganizationCode", org);
        doc.appendChild(order);
        order.appendChild(doc.createElement("OrderLines")); // empty
        return doc;
    }

    /**
     * Builds a single {@code <OrderLine>} element.
     *
     * @param useLineAttrQty when {@code true}, OrderedQty is placed directly on OrderLine;
     *                        when {@code false}, it is omitted entirely (caller adds qty elsewhere)
     */
    private Element buildLine(Document doc, LineSpec spec, boolean useLineAttrQty) {
        Element orderLine = doc.createElement("OrderLine");
        if (useLineAttrQty && spec.qty() != null) {
            orderLine.setAttribute("OrderedQty", spec.qty());
        }

        Element item = doc.createElement("Item");
        if (spec.itemId() != null) item.setAttribute("ItemID", spec.itemId());
        if (spec.uom()    != null) item.setAttribute("UnitOfMeasure", spec.uom());
        orderLine.appendChild(item);

        return orderLine;
    }
}
