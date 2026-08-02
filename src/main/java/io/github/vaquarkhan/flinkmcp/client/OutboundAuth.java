package io.github.vaquarkhan.flinkmcp.client;

import io.github.vaquarkhan.flinkmcp.security.CallerContext;
import io.github.vaquarkhan.flinkmcp.security.CallerIdentity;

/**
 * Resolves the Authorization value sent to Flink REST / SQL Gateway.
 * Prefers the current {@link CallerIdentity} outbound credential (O2 phase B),
 * then falls back to the static server-wide header from config.
 *
 * @author Viquar Khan
 */
public final class OutboundAuth {

    private OutboundAuth() {}

    public static String resolveFlink(String staticFallback) {
        return CallerContext.current()
                .map(CallerIdentity::flinkAuthHeader)
                .filter(OutboundAuth::present)
                .orElse(blankToNull(staticFallback));
    }

    public static String resolveGateway(String staticFallback) {
        return CallerContext.current()
                .map(CallerIdentity::gatewayAuthHeader)
                .filter(OutboundAuth::present)
                .orElse(blankToNull(staticFallback));
    }

    /** Normalize to a full {@code Authorization} header value. */
    public static String toAuthorizationValue(String authHeader) {
        if (!present(authHeader)) {
            return null;
        }
        int sp = authHeader.indexOf(' ');
        if (sp > 0) {
            return authHeader;
        }
        return "Bearer " + authHeader;
    }

    private static boolean present(String v) {
        return v != null && !v.isBlank();
    }

    private static String blankToNull(String v) {
        return present(v) ? v : null;
    }
}
