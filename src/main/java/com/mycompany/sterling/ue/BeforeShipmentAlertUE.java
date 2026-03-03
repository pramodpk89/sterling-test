package com.mycompany.sterling.ue;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User Exit: BeforeShipmentAlertUE
 *
 * <p>Fires before a shipment is confirmed in Sterling OMS. This is a
 * fail-open implementation — errors are logged but never block the
 * confirmShipment operation.</p>
 *
 * <p>In a real Sterling deployment this class would implement
 * {@code com.yantra.yfs.japi.ue.YFSBeforeConfirmShipmentUE} and be
 * registered in {@code <INSTALL>/extensions/global/api_list.xml}.</p>
 *
 * <pre>
 * Sterling registration (api_list.xml):
 * {@code
 * <API Name="confirmShipment">
 *   <UEList>
 *     <UE Name="YFSBeforeConfirmShipmentUE"
 *         Factory="com.mycompany.sterling.ue.BeforeShipmentAlertUE"/>
 *   </UEList>
 * </API>
 * }
 * </pre>
 */
// In real Sterling: implements YFSBeforeConfirmShipmentUE
public class BeforeShipmentAlertUE {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(BeforeShipmentAlertUE.class);
    private static final Logger LOG = Logger.getLogger(BeforeShipmentAlertUE.class.getName());

    // In real Sterling: @Override
    public void beforeConfirmShipment(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            LOG.warning("MYCO_SHIP_001 - beforeConfirmShipment received null inputDoc; skipping.");
            return;
        }

        try {
            // In real Sterling:
            // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
            // YFCElement root = inDoc.getDocumentElement();
            // String val = root.getAttribute("ShipmentKey"); // returns "" not null — check with "".equals(val)
            Element root = inputDoc.getDocumentElement();
            String shipmentKey = SterlingAPIHelper.safeGetAttribute(root, "ShipmentKey");

            // TODO: implement ShipmentAlert logic here

        } catch (Exception e) {
            // Fail-open: log the error but do NOT rethrow so confirmShipment proceeds.
            LOG.log(Level.SEVERE,
                    "MYCO_SHIP_002 - Unexpected error in BeforeShipmentAlertUE; "
                    + "confirmShipment will proceed. Error: " + e.getMessage(), e);
        }
    }
}
