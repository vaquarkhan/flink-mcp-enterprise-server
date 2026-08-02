package io.github.vaquarkhan.flinkmcp.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ApprovalTokens {

    private final byte[] secret;
    private final NonceStore nonces;
    private final SecureRandom rng = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    public ApprovalTokens(String secret, NonceStore nonces) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.nonces = nonces;
    }

    public String mint(String tool, String scope, long ttlMillis) {
        String sc = (scope == null || scope.isBlank()) ? "*" : scope;
        byte[] nonceBytes = new byte[16];
        rng.nextBytes(nonceBytes);
        String nonce = B64.encodeToString(nonceBytes);
        long exp = System.currentTimeMillis() + ttlMillis;
        String payload = tool + "|" + sc + "|" + exp + "|" + nonce;
        String payloadB64 = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = B64.encodeToString(hmac(payload.getBytes(StandardCharsets.UTF_8)));
        return payloadB64 + "." + sig;
    }

    public boolean verify(String token, String tool, String requiredScope) {
        if (token == null || secret.length == 0) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot >= token.length() - 1) {
            return false;
        }
        String payloadB64 = token.substring(0, dot);
        String sigB64 = token.substring(dot + 1);
        byte[] payloadBytes;
        byte[] providedSig;
        try {
            payloadBytes = B64D.decode(payloadB64);
            providedSig = B64D.decode(sigB64);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] expected = hmac(payloadBytes);
        if (!MessageDigest.isEqual(expected, providedSig)) {
            return false;
        }
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 4) {
            return false;
        }
        if (!parts[0].equals(tool)) {
            return false;
        }
        String tokenScope = parts[1];
        String req = requiredScope == null ? "" : requiredScope;
        if (!(tokenScope.isEmpty() || tokenScope.equals(req))) {
            return false;
        }
        long exp;
        try {
            exp = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (System.currentTimeMillis() > exp) {
            return false;
        }
        return nonces.useOnce(parts[3], exp);
    }

    private byte[] hmac(byte[] payload) {
        try {
            // SecretKeySpec rejects empty keys; mint may still be called with a null secret
            // (verify always fails when secret is empty).
            byte[] key = secret.length == 0 ? new byte[]{0} : secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ApprovalTokens <secret> <tool> [scope=*] [ttlSeconds=300]");
            System.exit(1);
        }
        String secret = args[0];
        String tool = args[1];
        String scope = "*";
        long ttlSeconds = 300;
        if (args.length >= 3) {
            if (args[2].chars().allMatch(Character::isDigit)) {
                ttlSeconds = Long.parseLong(args[2]);
            } else {
                scope = args[2];
                if (args.length >= 4) {
                    ttlSeconds = Long.parseLong(args[3]);
                }
            }
        }
        ApprovalTokens tokens = new ApprovalTokens(secret, new NonceStore());
        System.out.println(tokens.mint(tool, scope, ttlSeconds * 1000L));
    }
}
