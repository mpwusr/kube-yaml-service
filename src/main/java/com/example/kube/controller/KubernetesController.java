package com.example.kube.controller;

import com.example.kube.service.KubernetesYamlServiceApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kube")
@Tag(name = "Kubernetes YAML API", description = "Apply Kubernetes or Helm resources from JSON instructions")
public class KubernetesController {

    private final OkHttpClient client;
    private final KubernetesYamlServiceApp yamlService;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
    private static final String IN_CLUSTER_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String K8S_API_HOST = System.getenv("KUBERNETES_SERVICE_HOST");
    private static final String K8S_API_PORT = System.getenv("KUBERNETES_SERVICE_PORT");

    @Autowired
    public KubernetesController(OkHttpClient client, KubernetesYamlServiceApp yamlService) {
        this.client = client;
        this.yamlService = yamlService;
    }

    @PostMapping("/apply")
    @Operation(
            summary = "Apply Kubernetes or Helm resources",
            description = "Reads JSON-formatted instructions pointing to YAML/Helm charts and applies them to the cluster.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully applied all resources",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input or unsupported action"),
                    @ApiResponse(responseCode = "500", description = "Kubernetes API error or IO failure")
            }
    )
    public String applyResources(
            @org.springframework.web.bind.annotation.RequestBody List<Map<String, Object>> instructions
    ) throws IOException {
        String token = Files.readString(Paths.get(IN_CLUSTER_TOKEN)).trim();

        for (Map<String, Object> instruction : instructions) {
            String uri = (String) instruction.get("uri");
            String action = (String) instruction.get("action");

            List<Map<String, Object>> resources = yamlService.loadYamlResources(uri);

            for (Map<String, Object> resource : resources) {
                String kind = (String) resource.get("kind");
                Map<String, Object> metadata = (Map<String, Object>) resource.get("metadata");
                String namespace = metadata != null && metadata.containsKey("namespace") ?
                        (String) metadata.get("namespace") : "default";
                String name = metadata != null ? (String) metadata.get("name") : null;

                String apiPath = yamlService.buildApiPath(kind, namespace, name);

                Request request = null;

                String jsonBody = mapper.writeValueAsString(resource);
                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, MediaType.parse("application/json"));

                switch (action.toLowerCase()) {
                    case "create":
                        request = new Request.Builder()
                                .url(apiPath)
                                .addHeader("Authorization", "Bearer " + token)
                                .post(body)
                                .build();
                        break;
                    case "replace":
                        if ("Pod".equals(kind)) {
                            // delete first
                            Request deleteReq = new Request.Builder()
                                    .url(apiPath)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .delete()
                                    .build();
                            try (Response ignored = client.newCall(deleteReq).execute()) {
                                // deleted prior
                            }
                        }
                        request = new Request.Builder()
                                .url(apiPath)
                                .addHeader("Authorization", "Bearer " + token)
                                .put(body)
                                .build();
                        break;
                    case "delete":
                        request = new Request.Builder()
                                .url(apiPath)
                                .addHeader("Authorization", "Bearer " + token)
                                .delete()
                                .build();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported action: " + action);
                }

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return "Failed with code: " + response.code() + ", body: " + response.body().string();
                    }
                }
            }
        }
        return "Resources applied successfully.";
    }
}
