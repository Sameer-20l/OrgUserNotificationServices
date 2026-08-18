package com.catalogue.verg.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hashing for the credentials this registry stores (password, pin). Uses BCrypt: every call salts
 * independently, so the same secret never produces the same hash twice.
 *
 * <p>Because of that, a stored hash can never be verified by re-hashing and comparing strings —
 * always use {@link #matches(String, String)}. The encoded value is self-describing
 * ({@code $2a$<cost>$<salt+hash>}), so any BCrypt implementation can verify it.
 */
@Slf4j
public final class HashUtil {

    /**
     * BCrypt cost factor. Each increment doubles the work; 10 is ~50-100ms per hash, which keeps
     * bulk import (one hash per secret per row) usable while staying expensive to brute-force.
     */
    private static final int STRENGTH = 10;

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(STRENGTH);

    private HashUtil() {
    }

    /**
     * Hashes a raw secret for storage.
     *
     * @param rawSecret the plaintext password or pin; must not be null
     * @return the BCrypt hash (60 characters)
     * @throws IllegalArgumentException if {@code rawSecret} is null
     */
    public static String encode(String rawSecret) {
        if (rawSecret == null) {
            throw new IllegalArgumentException("HashUtil::rawSecret must not be null");
        }
        return ENCODER.encode(rawSecret);
    }

    /**
     * Checks a plaintext secret against a stored BCrypt hash.
     *
     * @return true when the secret matches; false if either argument is null or the stored value is
     *         not a valid BCrypt hash
     */
    public static boolean matches(String rawSecret, String encodedSecret) {
        if (rawSecret == null || encodedSecret == null) {
            return false;
        }
        return ENCODER.matches(rawSecret, encodedSecret);
    }

    /**
     * Returns a copy of the payload with each of the given fields replaced by its BCrypt hash, so
     * raw credentials never reach postgres, Elasticsearch or Redis. The incoming node is left
     * untouched, and fields absent from the payload are skipped.
     */
    public static JsonNode hashSecrets(JsonNode payload, String... fields) {
        if (payload == null || !payload.isObject()) {
            log.warn("HashUtil::hashSecrets::payload is not an object, nothing to hash");
            return payload;
        }
        ObjectNode copy = ((ObjectNode) payload).deepCopy();
        for (String field : fields) {
            if (payload.hasNonNull(field)) {
                copy.put(field, encode(payload.get(field).asText()));
            } else {
                log.warn("HashUtil::hashSecrets::no {} on the payload, nothing to hash", field);
            }
        }
        return copy;
    }
}
