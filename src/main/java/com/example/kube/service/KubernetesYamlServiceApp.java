package com.example.kube.service;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KubernetesYamlServiceApp {

    private static final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    /**
     * Loads YAML resources from a file path URI and returns a list of Kubernetes resource maps.
     */
    public List<Map<String, Object>> loadYamlResources(String filePath) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        Iterable<Object> loadedObjects = yaml.loadAll(new FileReader(filePath));

        for (Object obj : loadedObjects) {
            if (obj instanceof Map) {
                result.add((Map<String, Object>) obj);
            }
        }
        return result;
    }

    /**
     * Builds the Kubernetes API path from the given kind, namespace, and name.
     */
    public String buildApiPath(String kind, String namespace, String name) {
        String baseUrl = "https://" + System.getenv("KUBERNETES_SERVICE_HOST") + ":" + System.getenv("KUBERNETES_SERVICE_PORT");

        String apiVersion = switch (kind.toLowerCase()) {
            case "pod", "service", "configmap", "secret" -> "api/v1";
            default -> "apis/apps/v1";
        };

        String pluralKind = kind.toLowerCase() + "s"; // basic pluralization

        return baseUrl + "/" + apiVersion + "/namespaces/" + namespace + "/" + pluralKind + (name != null ? ("/" + name) : "");
    }
}
