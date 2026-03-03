package com.mycompany.sterling.api;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom API: CreateOrderAPI
 *
 * <p>Wraps the Sterling {@code createOrder} API to submit new orders into Sterling OMS.
 * Accepts an Order document with OrderLines and PersonInfoShipTo, delegates to
 * {@code createOrder} via {@code YIFApi.invoke()}, and returns the assigned
 * {@code OrderHeaderKey} and {@code OrderNo}.</p>
 *
 * <p>In a real Sterling deployment this class would extend
 * {@code com.yantra.interop.japi.YIFCustomApi} and be registered
 * in {@code <INSTALL>/extensions/global/api_list.xml}.</p>
 *
 * <pre>
 * Sterling registration (api_list.xml):
 * {@code
 * <API Name="myco.createOrder"
 *      Factory="com.mycompany.sterling.api.CreateOrderAPI"/>
 * }
 * </pre>
 *
 * <p><b>Input XML structure:</b></p>
 * <pre>{@code
 * <Order EnterpriseCode="DEFAULT"
 *        BuyerOrganizationCode="BUYER_ORG"
 *        CustomerID="CUST001"
 *        OrderTotal="250.00"
 *        ShipToID="SHIP001">
 *   <OrderLines>
 *     <OrderLine ItemID="ITEM001" UnitOfMeasure="EACH" OrderedQty="2" UnitPrice="125.00"/>
 *   </OrderLines>
 *   <PersonInfoShipTo FirstName="John" LastName="Doe"
 *                     AddressLine1="123 Main St" City="Springfield"
 *                     State="IL" ZipCode="62701" Country="US"/>
 * </Order>
 * }</pre>
 *
 * <p><b>Output XML (success):</b></p>
 * <pre>{@code
 * <CreateOrderOutput Status="SUCCESS"
 *                    OrderHeaderKey="OHK-000001"
 *                    OrderNo="ORD-000001"/>
 * }</pre>
 *
 * <p><b>Output XML (failure):</b></p>
 * <pre>{@code
 * <CreateOrderOutput Status="ERROR"
 *                    ErrorCode="MYCO_ORDER_011"
 *                    ErrorMessage="createOrder API call failed"/>
 * }</pre>
 *
 * <p><b>Configurable property:</b> {@code mycompany.order.enterprise.code} in
 * {@code yfs.properties} sets a default EnterpriseCode when none is provided
 * in the input document.</p>
 */
// In real Sterling: public class CreateOrderAPI extends YIFCustomApi {
public class CreateOrderAPI {

    // In real Sterling: private static final YFCLogCategory LOG = YFCLogCategory.instance(CreateOrderAPI.class);
    private static final Logger LOG = Logger.getLogger(CreateOrderAPI.class.getName());

    /** yfs.properties key for a default EnterpriseCode. */
    static final String PROP_ENTERPRISE_CODE = "mycompany.order.enterprise.code";

    private static final String DEFAULT_ENTERPRISE_CODE = "DEFAULT";

    // -------------------------------------------------------------------------
    // Public API entry point
    // -------------------------------------------------------------------------

    /**
     * Submits a new order into Sterling OMS by invoking the {@code createOrder} API.
     *
     * <p>In real Sterling the method signature would be:
     * {@code public Document invoke(YFSEnvironment env, Document inputDoc) throws YFSException}</p>
     *
     * @param inputDoc the Order XML document (see class-level Javadoc for structure)
     * @return {@code CreateOrderOutput} element with {@code Status=SUCCESS} and key/number,
     *         or {@code Status=ERROR} with {@code ErrorCode} and {@code ErrorMessage}
     */
    // In real Sterling: @Override
    public Document invoke(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            LOG.warning("MYCO_ORDER_010 - CreateOrderAPI received null inputDoc; returning error response.");
            return buildErrorResponse("MYCO_ORDER_010", "Input document is null");
        }

        try {
            // In real Sterling:
            // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
            // YFCElement orderElem = inDoc.getDocumentElement();
            // String enterpriseCode = orderElem.getAttribute("EnterpriseCode"); // returns "" not null
            Element orderElem = inputDoc.getDocumentElement(); // stub

            // --- Read input attributes ---
            String enterpriseCode = SterlingAPIHelper.getAttributeOrDefault(
                    orderElem, "EnterpriseCode", resolveEnterpriseCode());
            String buyerOrgCode = SterlingAPIHelper.safeGetAttribute(orderElem, "BuyerOrganizationCode");
            String customerID   = SterlingAPIHelper.safeGetAttribute(orderElem, "CustomerID");
            String orderTotal   = SterlingAPIHelper.safeGetAttribute(orderElem, "OrderTotal");
            String shipToID     = SterlingAPIHelper.safeGetAttribute(orderElem, "ShipToID");

            LOG.info("MYCO_ORDER_010 - CreateOrderAPI invoked: EnterpriseCode=" + enterpriseCode
                    + ", CustomerID=" + customerID + ", OrderTotal=" + orderTotal);

            // --- Build createOrder input XML ---
            // In real Sterling:
            // YFCDocument createOrderInput = YFCDocument.createDocument("Order");
            // YFCElement createOrderRoot = createOrderInput.getDocumentElement();
            // createOrderRoot.setAttribute("EnterpriseCode", enterpriseCode);
            Document createOrderInputDoc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            Element createOrderRoot = createOrderInputDoc.createElement("Order");
            createOrderInputDoc.appendChild(createOrderRoot);

            createOrderRoot.setAttribute("EnterpriseCode", enterpriseCode);
            if (buyerOrgCode != null) createOrderRoot.setAttribute("BuyerOrganizationCode", buyerOrgCode);
            if (customerID   != null) createOrderRoot.setAttribute("CustomerID", customerID);
            if (orderTotal   != null) createOrderRoot.setAttribute("OrderTotal", orderTotal);
            if (shipToID     != null) createOrderRoot.setAttribute("ShipToID", shipToID);

            // --- Copy OrderLines from input ---
            // In real Sterling:
            // YFCElement inputOrderLines = orderElem.getChildElement("OrderLines");
            // if (inputOrderLines != null) { ... copy via YFCDocument ... }
            NodeList orderLinesList = orderElem.getElementsByTagName("OrderLines");
            if (orderLinesList.getLength() > 0) {
                Element inputOrderLines = (Element) orderLinesList.item(0);
                Element copiedLines = (Element) createOrderInputDoc.importNode(inputOrderLines, true);
                createOrderRoot.appendChild(copiedLines);
            } else {
                LOG.warning("MYCO_ORDER_010 - CreateOrderAPI: no OrderLines present in input; proceeding.");
            }

            // --- Copy PersonInfoShipTo from input ---
            // In real Sterling:
            // YFCElement inputShipTo = orderElem.getChildElement("PersonInfoShipTo");
            // if (inputShipTo != null) { ... copy via YFCDocument ... }
            NodeList shipToList = orderElem.getElementsByTagName("PersonInfoShipTo");
            if (shipToList.getLength() > 0) {
                Element inputShipTo = (Element) shipToList.item(0);
                Element copiedShipTo = (Element) createOrderInputDoc.importNode(inputShipTo, true);
                createOrderRoot.appendChild(copiedShipTo);
            } else {
                LOG.warning("MYCO_ORDER_010 - CreateOrderAPI: no PersonInfoShipTo present in input.");
            }

            // --- Invoke createOrder ---
            // In real Sterling:
            // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
            // Document createOrderOutput = api.invoke(env, "createOrder", createOrderInput.getDocument());
            Document createOrderOutput = invokeCreateOrder(createOrderInputDoc);

            if (createOrderOutput == null) {
                LOG.severe("MYCO_ORDER_012 - CreateOrderAPI: createOrder returned null response.");
                return buildErrorResponse("MYCO_ORDER_012", "createOrder API returned null response");
            }

            // --- Extract response attributes ---
            // In real Sterling:
            // YFCDocument outDoc = YFCDocument.getDocumentFor(createOrderOutput);
            // YFCElement outOrder = outDoc.getDocumentElement();
            // String orderHeaderKey = outOrder.getAttribute("OrderHeaderKey"); // "" not null
            // String orderNo        = outOrder.getAttribute("OrderNo");
            Element outOrder      = createOrderOutput.getDocumentElement(); // stub
            String orderHeaderKey = SterlingAPIHelper.safeGetAttribute(outOrder, "OrderHeaderKey");
            String orderNo        = SterlingAPIHelper.safeGetAttribute(outOrder, "OrderNo");

            LOG.info("MYCO_ORDER_010 - CreateOrderAPI success: OrderHeaderKey=" + orderHeaderKey
                    + ", OrderNo=" + orderNo);

            return buildSuccessResponse(orderHeaderKey, orderNo);

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "MYCO_ORDER_011 - Unexpected error in CreateOrderAPI: " + e.getMessage(), e);
            return buildErrorResponse("MYCO_ORDER_011", "createOrder API call failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Package-private hooks (overridable in tests via Mockito spy)
    // -------------------------------------------------------------------------

    /**
     * Invokes the Sterling {@code createOrder} API.
     *
     * <p>Package-private to allow stubbing in unit tests via Mockito spy
     * without requiring a live Sterling runtime.</p>
     *
     * <p>In real Sterling:
     * {@code api.invoke(env, "createOrder", createOrderInputDoc)}</p>
     *
     * @param createOrderInputDoc the assembled Order XML input
     * @return the createOrder response document, or {@code null} on failure
     */
    Document invokeCreateOrder(Document createOrderInputDoc) {
        // In real Sterling:
        // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
        // return api.invoke(env, "createOrder", createOrderInputDoc);
        return SterlingAPIHelper.invokeAPI(/* env, */ "createOrder", createOrderInputDoc);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the default EnterpriseCode from {@code yfs.properties}.
     *
     * <p>In real Sterling:
     * {@code YFCProperties.getInstance().getProperty("mycompany.order.enterprise.code", "DEFAULT")}</p>
     */
    private String resolveEnterpriseCode() {
        // In real Sterling:
        // return YFCProperties.getInstance().getProperty(PROP_ENTERPRISE_CODE, DEFAULT_ENTERPRISE_CODE);
        String fromSysProp = System.getProperty(PROP_ENTERPRISE_CODE);
        return (fromSysProp != null && !fromSysProp.isEmpty()) ? fromSysProp : DEFAULT_ENTERPRISE_CODE;
    }

    /**
     * Builds a success {@code CreateOrderOutput} document with
     * {@code OrderHeaderKey} and {@code OrderNo}.
     *
     * <p>In real Sterling:
     * {@code YFCDocument outDoc = YFCDocument.createDocument("CreateOrderOutput"); ...}</p>
     */
    private Document buildSuccessResponse(String orderHeaderKey, String orderNo) {
        try {
            Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element outRoot = outDoc.createElement("CreateOrderOutput");
            outRoot.setAttribute("Status", "SUCCESS");
            if (orderHeaderKey != null) outRoot.setAttribute("OrderHeaderKey", orderHeaderKey);
            if (orderNo        != null) outRoot.setAttribute("OrderNo", orderNo);
            outDoc.appendChild(outRoot);
            return outDoc;
        } catch (Exception e) {
            LOG.severe("MYCO_ORDER_011 - Failed to build success response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Builds an error {@code CreateOrderOutput} document.
     *
     * <p>In real Sterling a business error would be surfaced via:
     * {@code throw new YFSException(errorCode, errorMessage, null);}</p>
     */
    private Document buildErrorResponse(String errorCode, String errorMessage) {
        try {
            Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element outRoot = outDoc.createElement("CreateOrderOutput");
            outRoot.setAttribute("Status", "ERROR");
            outRoot.setAttribute("ErrorCode", errorCode);
            outRoot.setAttribute("ErrorMessage", errorMessage);
            outDoc.appendChild(outRoot);
            return outDoc;
        } catch (Exception e) {
            LOG.severe("MYCO_ORDER_011 - Failed to build error response: " + e.getMessage());
            return null;
        }
    }
}
