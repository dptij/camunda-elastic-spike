package at.jit.camundaelasticspike;

import java.util.Base64;

public final class ElasticSearchUtils {
    public static String composeBase64ApiKey(final String id, final String key) {
        return Base64.getEncoder().encodeToString(String.format("%s:%s", id, key).getBytes());
    }
    public static String composeElasticId(String processInstanceId) {
        return String.format("CAMUNDA-%s", processInstanceId);
    }
}
