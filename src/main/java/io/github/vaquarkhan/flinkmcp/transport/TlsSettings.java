package io.github.vaquarkhan.flinkmcp.transport;

/**
 * Optional TLS settings for the HTTP MCP transport.
 *
 * @author Viquar Khan
 */
public final class TlsSettings {

    private final boolean enabled;
    private final String keystorePath;
    private final String keystorePassword;
    private final String keystoreType;

    public TlsSettings(boolean enabled, String keystorePath, String keystorePassword, String keystoreType) {
        this.enabled = enabled;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType == null || keystoreType.isBlank() ? "PKCS12" : keystoreType;
    }

    public static TlsSettings disabled() {
        return new TlsSettings(false, null, null, "PKCS12");
    }

    public boolean enabled() {
        return enabled;
    }

    public String keystorePath() {
        return keystorePath;
    }

    public String keystorePassword() {
        return keystorePassword;
    }

    public String keystoreType() {
        return keystoreType;
    }
}
