package com.catalogue.verg.core.config;

import com.catalogue.verg.core.exception.CustomException;
import com.catalogue.verg.core.util.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single authority for whether the editorial lifecycle (draft -> add -> approve -> review)
 * applies to a given catalogue.
 *
 * <p>Two switches, combined with AND:
 * <ul>
 *   <li>{@code catalogue.lifecycle.enabled} — service-wide master switch;</li>
 *   <li>{@code catalogue.lifecycle.entities.<name>} — per-catalogue override, keyed by the
 *       catalogue's URL segment. An absent key means enabled.</li>
 * </ul>
 *
 * <p>When the lifecycle is disabled for a catalogue, its lifecycle endpoints 404 and
 * {@code create} persists {@link Constants#ACTIVE} instead of {@link Constants#PENDING}.
 * CRUD, search, read, import and loadFromPrimary are unaffected.
 *
 * <p>Configuration only seeds {@link #live} at startup; {@link #live} is what is actually
 * read, so a future admin endpoint can flip a catalogue at runtime via
 * {@link #setEnabledFor(String, boolean)} without a restart.
 *
 * <p>Note: no class-level Lombok {@code @Getter}/{@code @Setter} here on purpose — it would
 * expose {@link #live} as a phantom {@code catalogue.lifecycle.live.*} binding target.
 * Likewise the class must keep its default constructor, or Spring switches to constructor
 * binding and the field defaults stop applying.
 */
@Component
@ConfigurationProperties(prefix = "catalogue.lifecycle")
@Slf4j
public class LifecyclePolicy {

    /** Service-wide master switch. false => no catalogue follows the lifecycle. */
    private boolean enabled = true;

    /** Per-catalogue overrides as bound from configuration. Absent key = enabled. */
    private Map<String, Boolean> entities = new HashMap<>();

    /** Normalised, mutable view of {@link #entities}. Not a binding target. */
    private final Map<String, Boolean> live = new ConcurrentHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Boolean> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Boolean> entities) {
        this.entities = entities;
    }

    @PostConstruct
    void seedFromConfig() {
        entities.forEach((key, value) -> live.put(normalize(key), Boolean.TRUE.equals(value)));
        log.info("LifecyclePolicy: catalogue.lifecycle.enabled={} overrides={}", enabled, live);
    }

    /** Effective policy for a catalogue: the global switch AND its per-catalogue override. */
    public boolean isEnabledFor(String catalogue) {
        return enabled && live.getOrDefault(normalize(catalogue), Boolean.TRUE);
    }

    /**
     * Status a freshly created record should carry: PENDING when the lifecycle runs for this
     * catalogue (it still has to be approved and reviewed), ACTIVE when it does not.
     */
    public String initialStatus(String catalogue) {
        return isEnabledFor(catalogue) ? Constants.PENDING : Constants.ACTIVE;
    }

    /**
     * Guard for lifecycle-only endpoints. Throws a 404 CustomException — which
     * RestExceptionHandling renders as the service's standard ErrorResponse — when the
     * lifecycle is disabled for this catalogue.
     */
    public void requireEnabled(String catalogue) {
        if (!isEnabledFor(catalogue)) {
            log.debug("LifecyclePolicy::requireEnabled:lifecycle is disabled for catalogue: {}", catalogue);
            throw new CustomException(Constants.LIFECYCLE_DISABLED,
                    "Lifecycle is disabled for catalogue '" + catalogue + "'", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Flips a catalogue at runtime. Hook for the admin toggle endpoint; the change is
     * in-memory only, so it resets to configuration on restart and applies to this instance
     * alone.
     */
    public void setEnabledFor(String catalogue, boolean lifecycleEnabled) {
        live.put(normalize(catalogue), lifecycleEnabled);
        log.info("LifecyclePolicy::setEnabledFor:{}={}", normalize(catalogue), lifecycleEnabled);
    }

    /**
     * Canonicalises a catalogue name so configuration keys and lookups agree regardless of
     * case or separators ({@code cropCategory}, {@code crop-category} and {@code cropcategory}
     * all resolve to the same key, matching main.py's {@code service_name_lower}).
     */
    private static String normalize(String catalogue) {
        return catalogue == null
                ? ""
                : catalogue.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
