package com.mycompany.sterling.service;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service: OrderHoldValidationService
 *
 * <p>Evaluates three hold rules against an incoming {@code createOrder} document
 * and applies any triggered holds via a single {@code changeOrder} call. This is
 * a fail-open implementation: errors in any rule or in {@code changeOrder} are
 * logged but never block order creation.</p>
 *
 * <p>In a real Sterling deployment this class would extend
 * {@code com.yantra.interop.japi.YIFCustomApi} and be registered
 * in {@code extensions/global/services.xml}.</p>
 *
 * <pre>
 * Sterling registration (services.xml):
 * {@code
 * <ServiceDefinition
 *     Name="mycompany.OrderHoldValidationService"
 *     FactoryClass="com.mycompany.sterling.service.OrderHoldValidationService"
 *     TransactionType="REQUIRED"/>
 * }
 * </pre>
 *
 * <p>Properties required in {@code yfs.properties}:</p>
 * <pre>
 * mycompany.order.hold.highvalue.threshold=5000.00
 * mycompany.order.hold.restricted.states=AK,HI,PR
 * mycompany.order.hold.restricted.zipcodes=
 * </pre>
 *
 * @see com.mycompany.sterling.ue.BeforeCreateOrderHoldUE
 */
// In real Sterling: extends YIFCustomApi
public class OrderHoldValidationService {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(OrderHoldValidationService.class);
    private static final Logger LOG = Logger.getLogger(OrderHoldValidationService.class.getName());

    private static final double DEFAULT_HIGH_VALUE_THRESHOLD = 5000.00;

    // In real Sterling: @Override
    public Document invoke(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            return null;
        }

        // In real Sterling:
        // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
        // YFCElement orderElem = inDoc.getDocumentElement();
        Element orderElem = inputDoc.getDocumentElement();

        String orderNo         = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderNo");
        String enterpriseCode  = SterlingAPIHelper.safeGetAttribute(orderElem, "EnterpriseCode");
        String customerID      = SterlingAPIHelper.safeGetAttribute(orderElem, "CustomerID");

        LOG.fine("MYCO_HOLD evaluating holds for OrderNo=" + orderNo
                + " EnterpriseCode=" + enterpriseCode);

        // Collect all triggered holds before issuing a single changeOrder call
        List<HoldEntry> holdsToApply = new ArrayList<>();

        // Rule 1 — High Value Order
        evaluateHighValueRule(orderElem, holdsToApply);

        // Rule 2 — Flagged Customer (fail-open: skipped if getCustomerList fails)
        evaluateFlaggedCustomerRule(/* env, */ customerID, enterpriseCode, holdsToApply);

        // Rule 3 — Restricted Destination
        evaluateRestrictedDestinationRule(orderElem, holdsToApply);

        // Apply all collected holds in one changeOrder call
        if (!holdsToApply.isEmpty()) {
            applyHolds(/* env, */ orderNo, enterpriseCode, holdsToApply);
        }

        return null; // UE delegates; no output document returned
    }

    // -------------------------------------------------------------------------
    // Rule 1 — High Value Order
    // -------------------------------------------------------------------------

    private void evaluateHighValueRule(Element orderElem, List<HoldEntry> holds) {
        String orderTotalStr = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderTotal");
        if (orderTotalStr == null) {
            LOG.warning("MYCO_HOLD_001 - OrderTotal attribute missing or empty; skipping Rule 1.");
            return;
        }

        double orderTotal;
        double threshold;
        try {
            orderTotal = Double.parseDouble(orderTotalStr);
            // In real Sterling: YFCProperties.getInstance().getProperty("mycompany.order.hold.highvalue.threshold", "5000.00")
            threshold  = Double.parseDouble(
                    System.getProperty("mycompany.order.hold.highvalue.threshold",
                                       String.valueOf(DEFAULT_HIGH_VALUE_THRESHOLD)));
        } catch (NumberFormatException e) {
            LOG.warning("MYCO_HOLD_001 - OrderTotal '" + orderTotalStr
                    + "' is not a valid number; skipping Rule 1.");
            return;
        }

        LOG.fine("MYCO_HOLD Rule1: orderTotal=" + orderTotal + " threshold=" + threshold);

        if (orderTotal > threshold) {
            String reason = String.format(
                    "OrderTotal %.2f exceeds threshold %.2f", orderTotal, threshold);
            LOG.info("MYCO_HOLD applying HIGH_VALUE_HOLD: " + reason);
            holds.add(new HoldEntry("HIGH_VALUE_HOLD", "1", reason));
        }
    }

    // -------------------------------------------------------------------------
    // Rule 2 — Flagged Customer
    // -------------------------------------------------------------------------

    private void evaluateFlaggedCustomerRule(/* YFSEnvironment env, */
            String customerID, String enterpriseCode, List<HoldEntry> holds) {

        if (customerID == null) {
            LOG.fine("MYCO_HOLD Rule2: CustomerID not present; skipping.");
            return;
        }

        Document customerListDoc;
        try {
            // In real Sterling:
            // YFCDocument custInput = YFCDocument.createDocument("Customer");
            // YFCElement custElem = custInput.getDocumentElement();
            // custElem.setAttribute("CustomerID", customerID);
            // custElem.setAttribute("OrganizationCode", enterpriseCode);
            Document custInput = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element custElem = custInput.createElement("Customer");
            custInput.appendChild(custElem);
            custElem.setAttribute("CustomerID", customerID);
            if (enterpriseCode != null) {
                custElem.setAttribute("OrganizationCode", enterpriseCode);
            }

            customerListDoc = SterlingAPIHelper.invokeAPIFailOpen(/* env, */ "getCustomerList", custInput);

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_002 - getCustomerList failed for CustomerID=" + customerID
                    + "; skipping Rule 2.", e);
            return;
        }

        if (customerListDoc == null) {
            LOG.warning("MYCO_HOLD_002 - getCustomerList returned null for CustomerID="
                    + customerID + "; skipping Rule 2.");
            return;
        }

        // In real Sterling:
        // YFCDocument custListDoc = YFCDocument.getDocumentFor(customerListDoc);
        // for (YFCElement customer : custListDoc.getDocumentElement().getChildren("Customer")) {
        //     String flag = customer.getAttribute("CustomerFlag"); // returns "" not null
        NodeList customers = customerListDoc.getElementsByTagName("Customer");
        for (int i = 0; i < customers.getLength(); i++) {
            Element customer = (Element) customers.item(i);
            String flag = SterlingAPIHelper.safeGetAttribute(customer, "CustomerFlag");
            LOG.fine("MYCO_HOLD Rule2: CustomerID=" + customerID + " CustomerFlag=" + flag);
            if ("FLAGGED".equals(flag) || "RESTRICTED".equals(flag)) {
                String reason = "CustomerID " + customerID + " is on the restricted customer list";
                LOG.info("MYCO_HOLD applying FLAGGED_CUSTOMER_HOLD: " + reason);
                holds.add(new HoldEntry("FLAGGED_CUSTOMER_HOLD", "1", reason));
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rule 3 — Restricted Shipping Destination
    // -------------------------------------------------------------------------

    private void evaluateRestrictedDestinationRule(Element orderElem, List<HoldEntry> holds) {
        // In real Sterling:
        // YFCElement shipTo = orderElem.getChildElement("PersonInfoShipTo");
        // if (shipTo == null) { ... }
        NodeList shipToNodes = orderElem.getElementsByTagName("PersonInfoShipTo");
        if (shipToNodes.getLength() == 0) {
            LOG.warning("MYCO_HOLD_005 - PersonInfoShipTo element missing; skipping Rule 3.");
            return;
        }

        Element shipTo  = (Element) shipToNodes.item(0);
        String state    = SterlingAPIHelper.safeGetAttribute(shipTo, "State");
        String zipCode  = SterlingAPIHelper.safeGetAttribute(shipTo, "ZipCode");

        // In real Sterling:
        // YFCProperties props = YFCProperties.getInstance();
        // String restrictedStatesProp = props.getProperty("mycompany.order.hold.restricted.states", "");
        // String restrictedZipsProp   = props.getProperty("mycompany.order.hold.restricted.zipcodes", "");
        String restrictedStatesProp = System.getProperty("mycompany.order.hold.restricted.states",   "");
        String restrictedZipsProp   = System.getProperty("mycompany.order.hold.restricted.zipcodes", "");

        Set<String> restrictedStates = toSet(restrictedStatesProp);
        Set<String> restrictedZips   = toSet(restrictedZipsProp);

        LOG.fine("MYCO_HOLD Rule3: state=" + state + " zip=" + zipCode
                + " restrictedStates=" + restrictedStates);

        boolean stateRestricted = state   != null && restrictedStates.contains(state);
        boolean zipRestricted   = zipCode != null && restrictedZips.contains(zipCode);

        if (stateRestricted || zipRestricted) {
            String matchedValue = stateRestricted ? "State " + state : "ZipCode " + zipCode;
            String reason = "ShipTo " + matchedValue + " matches restricted destination pattern";
            LOG.info("MYCO_HOLD applying RESTRICTED_DESTINATION_HOLD: " + reason);
            holds.add(new HoldEntry("RESTRICTED_DESTINATION_HOLD", "1", reason));
        }
    }

    // -------------------------------------------------------------------------
    // Apply holds via changeOrder
    // -------------------------------------------------------------------------

    private void applyHolds(/* YFSEnvironment env, */
            String orderNo, String enterpriseCode, List<HoldEntry> holds) {
        try {
            // In real Sterling:
            // YFCDocument changeInput = YFCDocument.createDocument("Order");
            // YFCElement orderElem = changeInput.getDocumentElement();
            // orderElem.setAttribute("OrderNo", orderNo);
            // orderElem.setAttribute("EnterpriseCode", enterpriseCode);
            // YFCElement holdTypesElem = orderElem.createChild("OrderHoldTypes");
            // for (HoldEntry hold : holds) {
            //     YFCElement holdTypeElem = holdTypesElem.createChild("OrderHoldType");
            //     holdTypeElem.setAttribute("HoldType", hold.holdType);
            //     holdTypeElem.setAttribute("Status",   hold.status);
            //     holdTypeElem.setAttribute("ReasonText", hold.reasonText);
            // }
            Document changeInput = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element orderElem = changeInput.createElement("Order");
            changeInput.appendChild(orderElem);
            if (orderNo != null)       orderElem.setAttribute("OrderNo",        orderNo);
            if (enterpriseCode != null) orderElem.setAttribute("EnterpriseCode", enterpriseCode);

            Element holdTypesElem = changeInput.createElement("OrderHoldTypes");
            orderElem.appendChild(holdTypesElem);

            for (HoldEntry hold : holds) {
                Element holdTypeElem = changeInput.createElement("OrderHoldType");
                holdTypeElem.setAttribute("HoldType",   hold.holdType);
                holdTypeElem.setAttribute("Status",     hold.status);
                holdTypeElem.setAttribute("ReasonText", hold.reasonText);
                holdTypesElem.appendChild(holdTypeElem);
            }

            SterlingAPIHelper.invokeAPIFailOpen(/* env, */ "changeOrder", changeInput);

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_003 - changeOrder failed while applying holds for OrderNo=" + orderNo
                    + "; holds NOT applied but order will proceed.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Set<String> toSet(String csv) {
        Set<String> result = new HashSet<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        for (String token : csv.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** Immutable holder for a single pending hold. */
    private static final class HoldEntry {
        final String holdType;
        final String status;
        final String reasonText;

        HoldEntry(String holdType, String status, String reasonText) {
            this.holdType   = holdType;
            this.status     = status;
            this.reasonText = reasonText;
        }
    }
}
