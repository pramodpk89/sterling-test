package com.mycompany.sterling.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SterlingAPIHelper
 *
 * <p>Utility methods for safe XML attribute access and Sterling API invocation.
 * All methods are null-safe and log warnings rather than throwing unchecked
 * exceptions so that callers can use fail-open patterns.</p>
 *
 * <p>In a real Sterling project the API invocation helpers would use
 * {@code YIFApi} / {@code YIFClientFactory} to call the runtime; placeholder
 * comments show the real Sterling pattern.</p>
 *
 * @author mycompany-dev-team
 * @version 1.0
 */
public final class SterlingAPIHelper {

    private static final Logger LOG = Logger.getLogger(SterlingAPIHelper.class.getName());

    /** Utility class — no instances. */
    private SterlingAPIHelper() {}

    // =========================================================================
    // XML attribute helpers
    // =========================================================================

    /**
     * Returns the value of {@code attrName} on {@code element}, or {@code null}
     * if the element is null or the attribute is absent/empty.
     *
     * <pre>
     * // Real Sterling equivalent:
     * // YFCElement elem = ...;
     * // String value = elem.getAttribute("OrderTotal");
     * </pre>
     *
     * @param element  a DOM {@link Element}, may be null
     * @param attrName the attribute name to read
     * @return trimmed attribute value, or {@code null}
     */
    public static String safeGetAttribute(Element element, String attrName) {
        if (element == null || attrName == null || attrName.isEmpty()) {
            return null;
        }
        String value = element.getAttribute(attrName);
        return (value == null || value.isEmpty()) ? null : value.trim();
    }

    /**
     * Returns the value of {@code attrName} on {@code element}, falling back to
     * {@code defaultValue} when the attribute is absent or empty.
     *
     * @param element      a DOM {@link Element}, may be null
     * @param attrName     the attribute name to read
     * @param defaultValue value returned when the attribute is absent
     * @return attribute value or {@code defaultValue}
     */
    public static String getAttributeOrDefault(Element element, String attrName, String defaultValue) {
        String value = safeGetAttribute(element, attrName);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Parses {@code attrName} as a {@code double}. Returns {@code defaultValue}
     * when the attribute is absent, empty, or not numeric.
     *
     * @param element      a DOM {@link Element}, may be null
     * @param attrName     the attribute name to read
     * @param defaultValue fallback value on parse failure
     * @return parsed double or {@code defaultValue}
     */
    public static double getDoubleAttribute(Element element, String attrName, double defaultValue) {
        String raw = safeGetAttribute(element, attrName);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            LOG.warning("SterlingAPIHelper - Cannot parse '" + attrName + "'='" + raw
                    + "' as double; using default " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses {@code attrName} as an {@code int}. Returns {@code defaultValue}
     * when the attribute is absent, empty, or not numeric.
     *
     * @param element      a DOM {@link Element}, may be null
     * @param attrName     the attribute name to read
     * @param defaultValue fallback value on parse failure
     * @return parsed int or {@code defaultValue}
     */
    public static int getIntAttribute(Element element, String attrName, int defaultValue) {
        String raw = safeGetAttribute(element, attrName);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            LOG.warning("SterlingAPIHelper - Cannot parse '" + attrName + "'='" + raw
                    + "' as int; using default " + defaultValue);
            return defaultValue;
        }
    }

    // =========================================================================
    // Sterling API invocation helpers (stub — real Sterling pattern shown)
    // =========================================================================

    /**
     * Invokes a Sterling API and returns the response document.
     *
     * <p>This is a <b>stub</b>. In a real Sterling project the implementation
     * would be:</p>
     * <pre>
     * // YIFApi api = YIFClientFactory.getInstance().getLocalApi();
     * // Document outDoc = api.invoke(env, apiName, inputDoc);
     * // return outDoc;
     * </pre>
     *
     * @param apiName  the Sterling API name (e.g. "getOrderDetails")
     * @param inputDoc the input XML document
     * @return the response document, or {@code null} if the call failed
     *
     * In real Sterling the signature would include:
     * {@code (YFSEnvironment env, String apiName, Document inputDoc) throws YFSException}
     */
    public static Document invokeAPI(/* YFSEnvironment env, */ String apiName, Document inputDoc) {
        // In real Sterling:
        // try {
        //     YIFApi api = YIFClientFactory.getInstance().getLocalApi();
        //     return api.invoke(env, apiName, inputDoc);
        // } catch (YFSException e) {
        //     YFCLogCategory.instance(SterlingAPIHelper.class).error(
        //         "SterlingAPIHelper.invokeAPI failed for API=" + apiName, e);
        //     throw e;
        // }

        LOG.warning("SterlingAPIHelper.invokeAPI - STUB called for API='" + apiName
                + "'. Wire real YIFApi before deploying to Sterling.");
        return null;
    }

    /**
     * Invokes a Sterling API with fail-open semantics: if the call throws any
     * exception the error is logged and {@code null} is returned so the caller
     * can continue processing.
     *
     * @param apiName  the Sterling API name
     * @param inputDoc the input XML document
     * @return the response document, or {@code null} on any failure
     *
     * In real Sterling the signature would include {@code YFSEnvironment env} as
     * the first parameter.
     */
    public static Document invokeAPIFailOpen(/* YFSEnvironment env, */ String apiName, Document inputDoc) {
        try {
            return invokeAPI(/* env, */ apiName, inputDoc);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "SterlingAPIHelper.invokeAPIFailOpen - API='" + apiName + "' failed; returning null. "
                    + "Error: " + e.getMessage(), e);
            return null;
        }
    }
}
