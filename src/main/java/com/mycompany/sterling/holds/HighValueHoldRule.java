package com.mycompany.sterling.holds;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HighValueHoldRule
 *
 * <p>Checks if an order's total exceeds a configurable threshold and applies
 * a {@code HIGH_VALUE_HOLD} via the Sterling {@code changeOrder} API.
 * This is a fail-open implementation: errors are logged but never thrown.</p>
 *
 * <p>Jira: <a href="https://pramodtest.atlassian.net/browse/KAN-6">KAN-6</a></p>
 */
public class HighValueHoldRule {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(HighValueHoldRule.class);
    private static final Logger LOG = Logger.getLogger(HighValueHoldRule.class.getName());

    private static final double DEFAULT_THRESHOLD = 5000.00;

    /**
     * Evaluates the high-value hold rule against the supplied order document.
     *
     * @param orderDoc XML document containing the {@code <Order>} element
     * @return {@code true} if a hold was applied, {@code false} otherwise
     *
     * In real Sterling the signature includes: {@code YFSEnvironment env} as first param
     * and {@code throws YFSException}
     */
    public boolean evaluateHighValueHold(/* YFSEnvironment env, */ Document orderDoc) {

        if (orderDoc == null) {
            LOG.warning("MYCO_HOLD_001 - evaluateHighValueHold received null orderDoc; returning false.");
            return false;
        }

        // In real Sterling:
        // YFCDocument inDoc = YFCDocument.getDocumentFor(orderDoc);
        // YFCElement orderElem = inDoc.getDocumentElement();
        // String orderTotalStr = orderElem.getAttribute("OrderTotal"); // returns "" not null
        Element orderElem = orderDoc.getDocumentElement();
        String orderTotalStr = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderTotal");

        if (orderTotalStr == null) {
            LOG.warning("MYCO_HOLD_001 - OrderTotal attribute missing or empty; skipping high-value check.");
            return false;
        }

        double orderTotal;
        double threshold;
        try {
            orderTotal = Double.parseDouble(orderTotalStr);
            // In real Sterling: YFCProperties.getInstance().getProperty("mycompany.order.hold.highvalue.threshold", "5000.00")
            threshold = Double.parseDouble(
                    System.getProperty("mycompany.order.hold.highvalue.threshold",
                                       String.valueOf(DEFAULT_THRESHOLD)));
        } catch (NumberFormatException e) {
            LOG.warning("MYCO_HOLD_001 - OrderTotal '" + orderTotalStr + "' is not a valid number; returning false.");
            return false;
        }

        LOG.fine("MYCO_HOLD HighValueRule: orderTotal=" + orderTotal + " threshold=" + threshold);

        if (orderTotal <= threshold) {
            return false;
        }

        // OrderTotal exceeds threshold — apply HIGH_VALUE_HOLD via changeOrder
        String orderHeaderKey  = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderHeaderKey");
        String enterpriseCode  = SterlingAPIHelper.safeGetAttribute(orderElem, "EnterpriseCode");
        String reasonText      = String.format("OrderTotal %.2f exceeds threshold %.2f", orderTotal, threshold);

        return applyHold(/* env, */ orderHeaderKey, enterpriseCode, reasonText);
    }

    // -------------------------------------------------------------------------

    private boolean applyHold(/* YFSEnvironment env, */
            String orderHeaderKey, String enterpriseCode, String reasonText) {
        try {
            // In real Sterling:
            // YFCDocument changeInput = YFCDocument.createDocument("Order");
            // YFCElement orderElem = changeInput.getDocumentElement();
            // orderElem.setAttribute("OrderHeaderKey", orderHeaderKey);
            // orderElem.setAttribute("EnterpriseCode", enterpriseCode);
            // YFCElement holdTypesElem = orderElem.createChild("OrderHoldTypes");
            // YFCElement holdTypeElem  = holdTypesElem.createChild("OrderHoldType");
            // holdTypeElem.setAttribute("HoldType",   "HIGH_VALUE_HOLD");
            // holdTypeElem.setAttribute("Status",     "1");
            // holdTypeElem.setAttribute("ReasonText", reasonText);
            Document changeInput = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element orderElem = changeInput.createElement("Order");
            changeInput.appendChild(orderElem);
            if (orderHeaderKey != null) orderElem.setAttribute("OrderHeaderKey", orderHeaderKey);
            if (enterpriseCode != null) orderElem.setAttribute("EnterpriseCode", enterpriseCode);

            Element holdTypesElem = changeInput.createElement("OrderHoldTypes");
            orderElem.appendChild(holdTypesElem);

            Element holdTypeElem = changeInput.createElement("OrderHoldType");
            holdTypeElem.setAttribute("HoldType",   "HIGH_VALUE_HOLD");
            holdTypeElem.setAttribute("Status",     "1");
            holdTypeElem.setAttribute("ReasonText", reasonText);
            holdTypesElem.appendChild(holdTypeElem);

            SterlingAPIHelper.invokeAPIFailOpen(/* env, */ "changeOrder", changeInput);

            LOG.info("MYCO_HOLD HIGH_VALUE_HOLD applied for OrderHeaderKey=" + orderHeaderKey
                    + " reason: " + reasonText);
            return true;

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_003 - changeOrder failed while applying HIGH_VALUE_HOLD"
                    + " for OrderHeaderKey=" + orderHeaderKey + "; returning false.", e);
            return false;
        }
    }
}
