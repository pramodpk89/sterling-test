package com.mycompany.sterling.holds;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FlaggedCustomerHoldRule
 *
 * <p>Checks whether the customer on an order is flagged or restricted and applies a
 * {@code FLAGGED_CUSTOMER_HOLD} via the Sterling {@code changeOrder} API.
 * This is a fail-open implementation: errors in external API calls are caught,
 * logged, and the rule returns {@code false} without propagating exceptions.</p>
 *
 * <p>Jira: <a href="https://pramodtest.atlassian.net/browse/KAN-7">KAN-7</a></p>
 *
 * <pre>
 * Registration snippet — add to extensions/global/services.xml:
 *
 * &lt;ServiceDefinition&gt;
 *   &lt;Service Name="mycompany.FlaggedCustomerHoldRuleService"
 *            FactoryClass="com.mycompany.sterling.holds.FlaggedCustomerHoldRule"
 *            TransactionType="REQUIRED"/&gt;
 * &lt;/ServiceDefinition&gt;
 *
 * If invoked from a before-createOrder UE, also add to api_list.xml:
 *
 * &lt;API Name="createOrder"&gt;
 *   &lt;UEList&gt;
 *     &lt;UE Name="YFSBeforeCreateOrderUE"
 *         Factory="com.mycompany.sterling.ue.BeforeCreateOrderHoldUE"/&gt;
 *   &lt;/UEList&gt;
 * &lt;/API&gt;
 * </pre>
 */
public class FlaggedCustomerHoldRule {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(FlaggedCustomerHoldRule.class);
    private static final Logger LOG = Logger.getLogger(FlaggedCustomerHoldRule.class.getName());

    /**
     * Evaluates the flagged-customer hold rule against the supplied order document.
     *
     * <ol>
     *   <li>Reads {@code CustomerID} and {@code EnterpriseCode} from the order root element.</li>
     *   <li>Calls {@code getCustomerList} to look up the customer record.</li>
     *   <li>If {@code CustomerFlag} is {@code "FLAGGED"} or {@code "RESTRICTED"}, applies
     *       {@code FLAGGED_CUSTOMER_HOLD} via {@code changeOrder}.</li>
     *   <li>Returns {@code true} if the hold was applied, {@code false} otherwise.</li>
     * </ol>
     *
     * <p>Fail-open: any exception from Sterling API calls is caught, logged with the
     * appropriate {@code MYCO_HOLD_*} code, and treated as a non-blocking result.</p>
     *
     * @param orderDoc XML document whose root element is {@code <Order>}
     * @return {@code true} if {@code FLAGGED_CUSTOMER_HOLD} was applied, {@code false} otherwise
     *
     * In real Sterling the signature would include:
     * {@code (YFSEnvironment env, Document orderDoc) throws YFSException}
     */
    public boolean evaluateFlaggedCustomerHold(/* YFSEnvironment env, */ Document orderDoc) {

        if (orderDoc == null) {
            LOG.warning("MYCO_HOLD_002 - evaluateFlaggedCustomerHold received null orderDoc; returning false.");
            return false;
        }

        // In real Sterling:
        // YFCDocument inDoc = YFCDocument.getDocumentFor(orderDoc);
        // YFCElement orderElem = inDoc.getDocumentElement();
        // String customerId    = orderElem.getAttribute("CustomerID");     // returns "" not null
        // String enterpriseCode = orderElem.getAttribute("EnterpriseCode");
        // String orderHeaderKey = orderElem.getAttribute("OrderHeaderKey");
        Element orderElem     = orderDoc.getDocumentElement();
        String customerId     = SterlingAPIHelper.safeGetAttribute(orderElem, "CustomerID");
        String enterpriseCode = SterlingAPIHelper.safeGetAttribute(orderElem, "EnterpriseCode");
        String orderHeaderKey = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderHeaderKey");

        if (customerId == null) {
            LOG.warning("MYCO_HOLD_002 - CustomerID attribute missing or empty in order; "
                    + "skipping flagged-customer check.");
            return false;
        }

        LOG.fine("MYCO_HOLD FlaggedCustomerRule: evaluating CustomerID=" + customerId);

        String customerFlag = lookupCustomerFlag(/* env, */ customerId, enterpriseCode);
        if (customerFlag == null) {
            // Already logged inside lookupCustomerFlag
            return false;
        }

        if (!"FLAGGED".equals(customerFlag) && !"RESTRICTED".equals(customerFlag)) {
            LOG.fine("MYCO_HOLD FlaggedCustomerRule: CustomerID=" + customerId
                    + " has flag=" + customerFlag + "; no hold applied.");
            return false;
        }

        String reasonText = "CustomerID " + customerId + " is on the restricted customer list";
        return applyFlaggedCustomerHold(/* env, */ orderHeaderKey, enterpriseCode, customerId, reasonText);
    }

    // -------------------------------------------------------------------------
    // Internal helpers — package-private to allow unit-test overriding via spy
    // -------------------------------------------------------------------------

    /**
     * Builds the {@code getCustomerList} input, invokes the API, and returns the
     * {@code CustomerFlag} attribute from the first matching customer record.
     *
     * @param customerId     the customer identifier to look up
     * @param enterpriseCode the enterprise / organization code (may be null)
     * @return {@code CustomerFlag} value, or {@code null} if not found or API error
     *
     * In real Sterling: {@code (YFSEnvironment env, String customerId, String enterpriseCode)}
     */
    String lookupCustomerFlag(/* YFSEnvironment env, */ String customerId, String enterpriseCode) {

        Document customerListInput;
        try {
            // In real Sterling:
            // YFCDocument inputDoc = YFCDocument.createDocument("Customer");
            // YFCElement custElem = inputDoc.getDocumentElement();
            // custElem.setAttribute("CustomerID", customerId);
            // if (enterpriseCode != null) custElem.setAttribute("OrganizationCode", enterpriseCode);
            customerListInput = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element custElem = customerListInput.createElement("Customer");
            customerListInput.appendChild(custElem);
            custElem.setAttribute("CustomerID", customerId);
            if (enterpriseCode != null) {
                custElem.setAttribute("OrganizationCode", enterpriseCode);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_002 - Failed to build getCustomerList input for CustomerID=" + customerId, e);
            return null;
        }

        Document customerListOutput;
        try {
            // In real Sterling:
            // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
            // customerListOutput = api.invoke(env, "getCustomerList", customerListInput);
            customerListOutput = invokeGetCustomerList(/* env, */ customerListInput);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_002 - getCustomerList failed for CustomerID=" + customerId
                    + "; returning false (fail-open).", e);
            return null;
        }

        if (customerListOutput == null) {
            LOG.fine("MYCO_HOLD_002 - getCustomerList returned null for CustomerID=" + customerId
                    + "; no hold applied.");
            return null;
        }

        // In real Sterling:
        // YFCDocument outDoc = YFCDocument.getDocumentFor(customerListOutput);
        // YFCElement customerListElem = outDoc.getDocumentElement();
        // YFCElement firstCustomer = customerListElem.getChildElement("Customer");
        // if (firstCustomer == null) return null;
        // String flag = firstCustomer.getAttribute("CustomerFlag"); // returns "" not null
        Element customerListElem = customerListOutput.getDocumentElement();
        if (customerListElem == null) {
            LOG.fine("MYCO_HOLD_002 - getCustomerList response has no root element for CustomerID=" + customerId);
            return null;
        }

        NodeList customers = customerListElem.getElementsByTagName("Customer");
        if (customers == null || customers.getLength() == 0) {
            LOG.fine("MYCO_HOLD_002 - getCustomerList returned no Customer records for CustomerID=" + customerId);
            return null;
        }

        Element firstCustomer = (Element) customers.item(0);
        String flag = SterlingAPIHelper.safeGetAttribute(firstCustomer, "CustomerFlag");
        LOG.fine("MYCO_HOLD FlaggedCustomerRule: CustomerID=" + customerId + " CustomerFlag=" + flag);
        return flag; // null if attribute absent; checked by caller
    }

    /**
     * Builds the {@code changeOrder} input and invokes the API to apply
     * {@code FLAGGED_CUSTOMER_HOLD}.
     *
     * @param orderHeaderKey  the order header key (may be null for brand-new orders)
     * @param enterpriseCode  the enterprise code
     * @param customerId      the flagged customer ID (used in log messages)
     * @param reasonText      human-readable hold reason
     * @return {@code true} if the hold was applied, {@code false} if {@code changeOrder} threw
     *
     * In real Sterling: {@code (YFSEnvironment env, String orderHeaderKey, ...)}
     */
    boolean applyFlaggedCustomerHold(/* YFSEnvironment env, */
            String orderHeaderKey, String enterpriseCode, String customerId, String reasonText) {
        try {
            // In real Sterling:
            // YFCDocument changeInput = YFCDocument.createDocument("Order");
            // YFCElement orderElem = changeInput.getDocumentElement();
            // orderElem.setAttribute("OrderHeaderKey", orderHeaderKey);
            // orderElem.setAttribute("EnterpriseCode", enterpriseCode);
            // YFCElement holdTypesElem = orderElem.createChild("OrderHoldTypes");
            // YFCElement holdTypeElem  = holdTypesElem.createChild("OrderHoldType");
            // holdTypeElem.setAttribute("HoldType",   "FLAGGED_CUSTOMER_HOLD");
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
            holdTypeElem.setAttribute("HoldType",   "FLAGGED_CUSTOMER_HOLD");
            holdTypeElem.setAttribute("Status",     "1");
            holdTypeElem.setAttribute("ReasonText", reasonText);
            holdTypesElem.appendChild(holdTypeElem);

            invokeChangeOrder(/* env, */ changeInput);

            LOG.info("MYCO_HOLD FLAGGED_CUSTOMER_HOLD applied: OrderHeaderKey=" + orderHeaderKey
                    + " CustomerID=" + customerId + " reason: " + reasonText);
            return true;

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_003 - changeOrder failed while applying FLAGGED_CUSTOMER_HOLD"
                    + " for OrderHeaderKey=" + orderHeaderKey + " CustomerID=" + customerId
                    + "; returning false (fail-open).", e);
            return false;
        }
    }

    /**
     * Invokes the Sterling {@code getCustomerList} API.
     * Extracted as a package-private method to allow unit-test spying.
     *
     * <pre>
     * // In real Sterling:
     * // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
     * // return api.invoke(env, "getCustomerList", input);
     * </pre>
     *
     * @param input the XML input document
     * @return the API response document, or {@code null} from the stub
     */
    Document invokeGetCustomerList(/* YFSEnvironment env, */ Document input) {
        return SterlingAPIHelper.invokeAPI(/* env, */ "getCustomerList", input);
    }

    /**
     * Invokes the Sterling {@code changeOrder} API.
     * Extracted as a package-private method to allow unit-test spying.
     *
     * <pre>
     * // In real Sterling:
     * // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
     * // api.invoke(env, "changeOrder", input);
     * </pre>
     *
     * @param input the XML input document
     */
    void invokeChangeOrder(/* YFSEnvironment env, */ Document input) {
        SterlingAPIHelper.invokeAPI(/* env, */ "changeOrder", input);
    }
}