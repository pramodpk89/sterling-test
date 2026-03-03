package com.mycompany.sterling.agent;

import com.mycompany.sterling.util.SterlingAPIHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent: HoldReviewAgent
 *
 * <p>Scheduled agent that processes queued work for hold review. Runs outside
 * the API transaction — must manage its own error handling and never let
 * exceptions escape {@code executeTask}.</p>
 *
 * <p>In a real Sterling deployment this class would extend
 * {@code com.yantra.yfs.japi.YFSAbstractTask} and be configured
 * in {@code <INSTALL>/extensions/global/agent_criteria.xml}.</p>
 *
 * <pre>
 * Sterling registration (agent_criteria.xml):
 * {@code
 * <AgentCriteria AgentCriteriaId="MYCO_HOLDREVIEW_AGENT"
 *                AgentCode="com.mycompany.sterling.agent.HoldReviewAgent"
 *                CriteriaId="DEFAULT"
 *                NumberOfThreads="4"
 *                MaxRecordsToBuffer="100"/>
 * }
 * </pre>
 */
// In real Sterling: extends YFSAbstractTask
public class HoldReviewAgent {

    // In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(HoldReviewAgent.class);
    private static final Logger LOG = Logger.getLogger(HoldReviewAgent.class.getName());

    // In real Sterling: @Override
    public void executeTask(/* YFSEnvironment env, */ Document criteriaDoc) {
        // In real Sterling: throws YFSException

        if (criteriaDoc == null) {
            LOG.warning("MYCO_AGENT_001 - HoldReviewAgent received null criteriaDoc; skipping.");
            return;
        }

        try {
            // In real Sterling:
            // YFCDocument criteria = YFCDocument.getDocumentFor(criteriaDoc);
            // YFCElement root = criteria.getDocumentElement();
            // String val = root.getAttribute("EnterpriseCode"); // returns "" not null
            Element root = criteriaDoc.getDocumentElement();
            String enterpriseCode = SterlingAPIHelper.safeGetAttribute(root, "EnterpriseCode");

            // TODO: implement HoldReview agent logic here

        } catch (Exception e) {
            // Agents run outside the API transaction — exceptions must never escape.
            LOG.log(Level.SEVERE,
                    "MYCO_AGENT_002 - Unexpected error in HoldReviewAgent; "
                    + "task will not be retried automatically. Error: " + e.getMessage(), e);
        }
    }
}
