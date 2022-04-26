/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apicurio.studio.operator.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * This is the specification of the ApicurioStudio API.
 * @author laurent.broudoux@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class ApicurioStudioSpec {

    private String name;
    private String url;

    private ModuleSpec wsModule;
    private ModuleSpec apiModule;
    private ModuleSpec studioModule;

    private KeycloakSpec keycloak = new KeycloakSpec();
    private DatabaseSpec database = new DatabaseSpec();
    private FeaturesSpec features = new FeaturesSpec();

    public ApicurioStudioSpec() {
        wsModule = new ModuleSpec("apicurio/apicurio-studio-ws:latest");
        wsModule.setResources(new ResourceRequirements(
              Map.of(
                    "cpu", new Quantity("1"),
                    "memory", new Quantity("1800Mi")
              ), // Default limits.
              Map.of(
                    "cpu", new Quantity("100m"),
                    "memory", new Quantity("900Mi")
              ) // Default requests.
            )
        );
        apiModule = new ModuleSpec("apicurio/apicurio-studio-api:latest");
        apiModule.setResources(new ResourceRequirements(
              Map.of(
                    "cpu", new Quantity("1"),
                    "memory", new Quantity("1700Mi")
              ), // Default limits.
              Map.of(
                  "cpu", new Quantity("100m"),
                  "memory", new Quantity("800Mi")
              ) // Default requests.
            )
        );
        studioModule = new ModuleSpec("apicurio/apicurio-studio-ui:latest");
        studioModule.setResources(new ResourceRequirements(
              Map.of(
                    "cpu", new Quantity("1"),
                    "memory", new Quantity("1300Mi")
              ), // Default limits.
              Map.of(
                    "cpu", new Quantity("100m"),
                    "memory", new Quantity("600Mi")
              ) // Default requests.
            )
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ModuleSpec getWsModule() {
        return wsModule;
    }

    public void setWsModule(ModuleSpec wsModule) {
        this.wsModule = wsModule;
    }

    public ModuleSpec getApiModule() {
        return apiModule;
    }

    public void setApiModule(ModuleSpec apiModule) {
        this.apiModule = apiModule;
    }

    public ModuleSpec getStudioModule() {
        return studioModule;
    }

    public void setStudioModule(ModuleSpec studioModule) {
        this.studioModule = studioModule;
    }

    public KeycloakSpec getKeycloak() {
        return keycloak;
    }

    public void setKeycloak(KeycloakSpec keycloak) {
        this.keycloak = keycloak;
    }

    public DatabaseSpec getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseSpec database) {
        this.database = database;
    }

    public FeaturesSpec getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesSpec features) {
        this.features = features;
    }
}
