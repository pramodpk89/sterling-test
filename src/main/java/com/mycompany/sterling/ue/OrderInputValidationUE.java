package com.mycompany.sterling.ue;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User Exit: OrderInputValidationUE
 *
 * <p>Fires before a new order is created in Sterling OMS. Validates the incoming
 * order XML document for required header attributes, presence of order lines,
 * and required per-line fields (ItemID, UnitOfMeasure, OrderedQty).</p>
 *
 * <h2>Fail Strategy</h2>
 * <ul>
 *   <li><b>Fail-closed</b> for missing required fields — a {@link IllegalArgumentException}
 *       is thrown (stub for {@code YFSException}) so the caller receives a clear,
 *       actionable error and the order is rejected.</li>
 *   <li><b>Fail-open</b> for unexpected internal errors — caught, logged, and swallowed
 *       so that a bug in this UE never silently blocks order creation.</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ol>
 *   <li>{@code Order/@EnterpriseCode} must be present and non-empty  (MYCO_ORDER_010)</li>
 *   <li>{@code Order/@BuyerOrganizationCode} must be present and non-empty (MYCO_ORDER_011)</li>
 *   <li>{@code Order/OrderLines} must contain at least one {@code <OrderLine>} (MYCO_ORDER_012)</li>
 *   <li>Each {@code OrderLine/Item/@ItemID} must be present (MYCO_ORDER_013)</li>
 *   <li>Each {@code OrderLine/Item/@UnitOfMeasure} must be present (MYCO_ORDER_014)</li>
 *   <li>Each {@code OrderLine} must have a positive numeric {@code OrderedQty}
 *       (on the line itself or inside {@code <OrderLineTranQuantity>}) (MYCO_ORDER_015)</li>
 * </ol>
 *
 * <p>In a real Sterling deployment this class would implement
 * {@code com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE} and be registered in
 * {@code <INSTALL>/extensions/global/api_list.xml}.</p>
 *
 * <pre>
 * Sterling registration (api_list.xml):
 * {@code
 * <API Name="createOrder">
 *   <UEList>
 *     <UE Name="YFSBeforeCreateOrderUE"
 *         Factory="com.mycompany.sterling.ue.OrderInputValidationUE"/>
 *   </UEList>
 * </API>
 * }
 * </pre>
 *
 * @author mycompany-dev-team
 * @version 1.0
 * @see <a href="https://pramodtest.atlassian.net/browse/KAN-4">KAN-4</a>
 */
// In real Sterling: implements YFSBeforeCreateOrderUE
public class OrderInputValidationUE {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(OrderInputValidationUE.class);
    private static final Logger LOG = Logger.getLogger(OrderInputValidationUE.class.getName());

    // -------------------------------------------------------------------------
    // User Exit entry point
    // -------------------------------------------------------------------------

    /**
     * Called by Sterling immediately before {@code createOrder} persists the order.
     * Validates all required fields and throws for missing required data.
     * Unexpected internal errors are caught and swallowed (fail-open) to avoid
     * blocking order creation due to a UE bug.
     *
     * <p>In real Sterling the signature would be:</p>
     * <pre>
     * {@code public void beforeCreateOrder(YFSEnvironment env, Document inputDoc)
     *         throws YFSException}
     * </pre>
     *
     * @param inputDoc the raw {@link Document} passed to {@code createOrder}
     * @throws IllegalArgumentException (stub for {@code YFSException}) when a required field is missing
     */
    // In real Sterling: @Override
    public void beforeCreateOrder(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            LOG.warning("MYCO_ORDER_009 - beforeCreateOrder received null inputDoc; skipping validation.");
            return;
        }

        // In real Sterling:
        // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
        // YFCElement orderElem = inDoc.getDocumentElement();
        Element orderElem = inputDoc.getDocumentElement(); // stub

        if (orderElem == null) {
            LOG.warning("MYCO_ORDER_009 - Order document has no root element; skipping validation.");
            return;
        }

        try {
            validateHeaderAttributes(orderElem);
            validateOrderLines(orderElem);
        } catch (IllegalArgumentException e) {
            // In real Sterling: throw new YFSException(errorCode, e.getMessage(), null);
            LOG.severe(e.getMessage());
            throw e; // re-throw so Sterling aborts the createOrder transaction
        } catch (Exception e) {
            // Fail-open: unexpected errors never block order creation
            LOG.log(Level.SEVERE,
                    "MYCO_ORDER_016 - Unexpected error in OrderInputValidationUE; "
                    + "order creation will proceed. Error: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Header validation
    // -------------------------------------------------------------------------

    /**
     * Validates that the required header attributes are present on the root
     * {@code <Order>} element.
     *
     * @param orderElem the root Order element (non-null)
     * @throws IllegalArgumentException if a required attribute is missing or empty
     */
    private void validateHeaderAttributes(Element orderElem) {
        // In real Sterling:
        // String enterpriseCode = orderElem.getAttribute("EnterpriseCode"); // returns "" not null
        // if (YFCLogCategory.isEmpty(enterpriseCode)) { throw new YFSException(...); }
        String enterpriseCode = SterlingAPIHelper.safeGetAttribute(orderElem, "EnterpriseCode");
        if (enterpriseCode == null) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_010 - createOrder input is missing required attribute Order/@EnterpriseCode.");
        }

        String buyerOrgCode = SterlingAPIHelper.safeGetAttribute(orderElem, "BuyerOrganizationCode");
        if (buyerOrgCode == null) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_011 - createOrder input is missing required attribute"
                    + " Order/@BuyerOrganizationCode.");
        }

        LOG.fine("MYCO_ORDER - header valid: EnterpriseCode=" + enterpriseCode
                + " BuyerOrganizationCode=" + buyerOrgCode);
    }

    // -------------------------------------------------------------------------
    // OrderLines validation
    // -------------------------------------------------------------------------

    /**
     * Validates that the {@code <OrderLines>} element is present and contains at
     * least one {@code <OrderLine>}, then delegates per-line validation.
     *
     * @param orderElem the root Order element (non-null)
     * @throws IllegalArgumentException if lines are absent or empty
     */
    private void validateOrderLines(Element orderElem) {
        // In real Sterling:
        // YFCElement orderLinesElem = orderElem.getChildElement("OrderLines");
        // if (orderLinesElem == null) { throw new YFSException("MYCO_ORDER_012", ..., null); }
        NodeList orderLinesNodes = orderElem.getElementsByTagName("OrderLines");
        if (orderLinesNodes.getLength() == 0) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_012 - createOrder input is missing required element Order/OrderLines.");
        }

        Element orderLinesElem = (Element) orderLinesNodes.item(0);
        NodeList orderLineNodes = orderLinesElem.getElementsByTagName("OrderLine");
        if (orderLineNodes.getLength() == 0) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_012 - createOrder input has an empty OrderLines element; "
                    + "at least one OrderLine is required.");
        }

        for (int i = 0; i < orderLineNodes.getLength(); i++) {
            Element orderLine = (Element) orderLineNodes.item(i);
            validateOrderLine(orderLine, i + 1);
        }
    }

    /**
     * Validates a single {@code <OrderLine>} element for required fields:
     * {@code Item/@ItemID}, {@code Item/@UnitOfMeasure}, and a positive
     * {@code OrderedQty}.
     *
     * @param orderLine the OrderLine element (non-null)
     * @param lineNo    1-based line number used in error messages
     * @throws IllegalArgumentException if a required field is missing or invalid
     */
    private void validateOrderLine(Element orderLine, int lineNo) {
        // In real Sterling:
        // YFCElement itemElem = orderLine.getChildElement("Item");
        NodeList itemNodes = orderLine.getElementsByTagName("Item");
        Element itemElem = (itemNodes.getLength() > 0) ? (Element) itemNodes.item(0) : null;

        // Validate ItemID
        String itemId = (itemElem != null)
                ? SterlingAPIHelper.safeGetAttribute(itemElem, "ItemID") : null;
        if (itemId == null) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_013 - OrderLine[" + lineNo + "]/Item/@ItemID is missing or empty.");
        }

        // Validate UnitOfMeasure
        String uom = (itemElem != null)
                ? SterlingAPIHelper.safeGetAttribute(itemElem, "UnitOfMeasure") : null;
        if (uom == null) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_014 - OrderLine[" + lineNo + "]/Item/@UnitOfMeasure is missing or empty.");
        }

        // Validate OrderedQty — check the line attribute first, then the child element
        String orderedQty = SterlingAPIHelper.safeGetAttribute(orderLine, "OrderedQty");
        if (orderedQty == null) {
            // In real Sterling:
            // YFCElement qtyElem = orderLine.getChildElement("OrderLineTranQuantity");
            NodeList qtyNodes = orderLine.getElementsByTagName("OrderLineTranQuantity");
            if (qtyNodes.getLength() > 0) {
                orderedQty = SterlingAPIHelper.safeGetAttribute((Element) qtyNodes.item(0), "OrderedQty");
            }
        }

        if (orderedQty == null) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_015 - OrderLine[" + lineNo + "] is missing required quantity. "
                    + "Provide OrderedQty on the OrderLine element or inside OrderLineTranQuantity.");
        }

        try {
            double qty = Double.parseDouble(orderedQty);
            if (qty <= 0) {
                throw new IllegalArgumentException(
                        "MYCO_ORDER_015 - OrderLine[" + lineNo + "] OrderedQty=" + orderedQty
                        + " must be greater than zero.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "MYCO_ORDER_015 - OrderLine[" + lineNo + "] OrderedQty='" + orderedQty
                    + "' is not a valid number.");
        }

        LOG.fine("MYCO_ORDER - OrderLine[" + lineNo + "] valid: ItemID=" + itemId
                + " UOM=" + uom + " OrderedQty=" + orderedQty);
    }
}
