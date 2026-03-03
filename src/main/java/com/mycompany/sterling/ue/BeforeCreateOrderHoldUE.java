package com.mycompany.sterling.ue;

import com.mycompany.sterling.service.OrderHoldValidationService;

import org.w3c.dom.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User Exit: BeforeCreateOrderHoldUE
 *
 * <p>Fires before a new order is created in Sterling OMS. Delegates hold
 * evaluation to {@link OrderHoldValidationService}. This is a fail-open
 * implementation — any unexpected exception is caught and swallowed so
 * that order creation is never blocked by a failure in this UE.</p>
 *
 * <p>In a real Sterling deployment this class would implement
 * {@code com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE} and be registered
 * in {@code <INSTALL>/extensions/global/api_list.xml}.</p>
 *
 * <pre>
 * Sterling registration (api_list.xml):
 * {@code
 * <API Name="createOrder">
 *   <UEList>
 *     <UE Name="YFSBeforeCreateOrderUE"
 *         Factory="com.mycompany.sterling.ue.BeforeCreateOrderHoldUE"/>
 *   </UEList>
 * </API>
 * }
 * </pre>
 */
// In real Sterling: implements YFSBeforeCreateOrderUE
public class BeforeCreateOrderHoldUE {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(BeforeCreateOrderHoldUE.class);
    private static final Logger LOG = Logger.getLogger(BeforeCreateOrderHoldUE.class.getName());

    private final OrderHoldValidationService holdService = new OrderHoldValidationService();

    // In real Sterling: @Override
    public void beforeCreateOrder(/* YFSEnvironment env, */ Document inputDoc) {
        // In real Sterling: throws YFSException

        if (inputDoc == null) {
            LOG.warning("MYCO_HOLD_004 - beforeCreateOrder received null inputDoc; skipping hold validation.");
            return;
        }

        try {
            holdService.invoke(/* env, */ inputDoc);
        } catch (Exception e) {
            // Fail-open: log the error but do NOT rethrow so order creation proceeds.
            LOG.log(Level.SEVERE,
                    "MYCO_HOLD_004 - Unexpected error in BeforeCreateOrderHoldUE; "
                    + "order creation will proceed without hold validation.", e);
        }
    }
}
