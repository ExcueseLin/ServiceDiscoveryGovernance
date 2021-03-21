package cn.sunrain.SDG.share.enums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lin
 * @date 2021/2/21 12:01
 */
public enum InstanceStatus {


    UP, // Ready to receive traffic
    DOWN, // Do not send traffic- healthcheck callback failed
    STARTING, // Just about starting- initializations to be done - do not
    // send traffic
    OUT_OF_SERVICE, // Intentionally shutdown for traffic
    UNKNOWN;

    private static final Logger log = LoggerFactory.getLogger(InstanceStatus.class);

    public static InstanceStatus toEnum(String s) {
        if (s != null) {
            try {
                return InstanceStatus.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore and fall through to unknown
                log.debug("illegal argument supplied to InstanceStatus.valueOf: {}, defaulting to {}", s, UNKNOWN);
            }
        }
        return UNKNOWN;
    }

}
