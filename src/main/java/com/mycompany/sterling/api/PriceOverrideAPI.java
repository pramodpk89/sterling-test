package com.mycompany.sterling.api;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom API: PriceOverrideAPI
 *
 * <p>In a real Sterling deployment this class would extend
 * {@code com.yantra.interop.japi.YIFCustomApi} and be registered
 * in {@code <INSTALL>/extensions/global/api_list.xml}.</p>
 *
 * <pre>
 * Sterling registration (api_list.xml):
 * {@code
 * <API Name="myco.priceOverride"
 *      Factory="com.mycompany.sterling.api.PriceOverrideAPI"/>
 * }
 * </pre>
 */
// In real Sterling: extends YIFCustomApi
public class PriceOverrideAPI {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(PriceOverrideAPI.class);
    private static final Logger LOG = Logger.getLogger(PriceOverrideAPI.class.getName());

    // In real Sterling: @Override
    public Document invoke(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            LOG.warning("MYCO_ORDER_001 - PriceOverrideAPI received null inputDoc; returning null.");
            return null;
        }

        try {
            // In real Sterling:
            // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
            // YFCElement root = inDoc.getDocumentElement();
            // String val = root.getAttribute("OrderNo"); // returns "" not null — check with "".equals(val)
            Element root = inputDoc.getDocumentElement();
            String orderNo = SterlingAPIHelper.safeGetAttribute(root, "OrderNo");

            // TODO: implement PriceOverride logic here

            // In real Sterling:
            // YFCDocument outDoc = YFCDocument.createDocument("Output");
            // outDoc.getDocumentElement().setAttribute("Status", "SUCCESS");
            // return outDoc.getDocument();
            Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element outRoot = outDoc.createElement("Output");
            outRoot.setAttribute("Status", "SUCCESS");
            outDoc.appendChild(outRoot);
            return outDoc;

        } catch (Exception e) {
            // Fail-open: log and return null so the caller can decide how to proceed.
            LOG.log(Level.SEVERE,
                    "MYCO_ORDER_002 - Unexpected error in PriceOverrideAPI; "
                    + "returning null. Error: " + e.getMessage(), e);
            return null;
        }
    }
}
