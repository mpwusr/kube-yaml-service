package com.example.kube.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/**
 * KubernetesYamlServiceApp is a utility service responsible for loading
 * Kubernetes resources from YAML, JSON, or Helm charts.
 *
 * It supports loading resources from:
 * - Local file paths via file:// URIs
 * - Remote HTTPS resources
 * - Helm chart directories or tarballs (.tgz)
 */

@Service
public class KubernetesYamlServiceApp {

    // YAML parser using safe constructor to avoid unsafe object creation
    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    // JSON parser from Jackson
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Main entry method to load Kubernetes resources from the given URI.
     * Determines file type and dispatches to appropriate loader method.
     *
     * @param uriStr URI as string, e.g., file:///... or https://...
     * @return List of parsed resource Maps
     * @throws IOException on read or network error
     */
    public List<Map<String, Object>> loadYamlResources(String uriStr) throws IOException {
        URI uri = URI.create(uriStr);
        String scheme = uri.getScheme();

        Path localPath;
        // Handle file:// URI
        if ("file".equals(scheme)) {
            localPath = Paths.get(uri);
        }
        // Handle https:// URI (downloads to a temporary file)
        else if ("https".equals(scheme)) {
            localPath = downloadRemoteToTemp(uri);
        }
        else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
        }

        String filename = localPath.getFileName().toString().toLowerCase();

        // Determine format by extension or directory
        if (filename.endsWith(".json")) {
            return loadFromJson(localPath.toString());
        } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            return loadFromYaml(localPath.toString());
        } else if (filename.endsWith(".tgz") || Files.isDirectory(localPath)) {
            return loadFromHelm(localPath.toString());
        } else {
            throw new IllegalArgumentException("Unsupported file type at: " + uriStr);
        }
    }

    /**
     * Parses a multi-document YAML file into a list of Kubernetes resource maps.
     *
     * @param path Path to the YAML file
     * @return Parsed resources as a list of maps
     */
    private List<Map<String, Object>> loadFromYaml(String path) throws IOException {
        try (InputStream input = Files.newInputStream(Paths.get(path))) {
            Iterable<Object> docs = yaml.loadAll(input);
            List<Map<String, Object>> resources = new ArrayList<>();
            for (Object doc : docs) {
                if (doc instanceof Map) {
                    resources.add((Map<String, Object>) doc);
                }
            }
            return resources;
        }
    }

    /**
     * Parses a JSON array of Kubernetes resources from a file.
     *
     * @param path Path to the JSON file
     * @return Parsed list of maps
     */
    private List<Map<String, Object>> loadFromJson(String path) throws IOException {
        try (InputStream input = Files.newInputStream(Paths.get(path))) {
            return mapper.readValue(input, List.class);
        }
    }

    /**
     * Renders a Helm chart using the `helm template` command and loads the result as YAML.
     *
     * @param chartPath Path to the Helm chart (folder or .tgz)
     * @return Parsed resources as list of maps
     * @throws IOException if Helm command fails or output cannot be parsed
     */
    private List<Map<String, Object>> loadFromHelm(String chartPath) throws IOException {
        Path tmpYaml = Files.createTempFile("helm-rendered", ".yaml");

        ProcessBuilder pb = new ProcessBuilder("helm", "template", chartPath);
        pb.redirectOutput(tmpYaml.toFile());  // Write output of Helm to temp file
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Forward stderr

        Process process = pb.start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Helm failed to render chart: " + chartPath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Helm render interrupted", e);
        }

        return loadFromYaml(tmpYaml.toString());
    }

    /**
     * Downloads a remote HTTPS YAML or JSON file into a temporary file.
     *
     * @param uri HTTPS URI
     * @return Path to downloaded temporary file
     */
    private Path downloadRemoteToTemp(URI uri) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestProperty("Accept", "application/x-yaml, application/json");

        Path tempFile = Files.createTempFile("remote-resource", ".tmp");
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(tempFile)) {
            in.transferTo(out); // Copy stream content to file
        }
        return tempFile;
    }

    /**
     * Builds the Kubernetes API URL path for a given resource.
     * NOTE: This is a simplified version and may need to support groupVersionKind in production.
     *
     * @param kind      Kubernetes resource kind (e.g., Pod, Service)
     * @param namespace Target namespace
     * @param name      Resource name
     * @return Fully-qualified Kubernetes API endpoint
     */
    public String buildApiPath(String kind, String namespace, String name) {
        String host = System.getenv("KUBERNETES_SERVICE_HOST");
        String port = System.getenv("KUBERNETES_SERVICE_PORT");

        String basePath = String.format("https://%s:%s", host, port);
        String resourcePath = String.format("/api/v1/namespaces/%s/%ss/%s", namespace, kind.toLowerCase(), name);

        return basePath + resourcePath;
    }
}
