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
import io.sundr.builder.annotations.Buildable;

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
    private IngressSpec wsIngress;
    private IngressSpec apiIngress;
    private IngressSpec studioIngress;
    private KeycloakSpec keycloak = new KeycloakSpec();
    private DatabaseSpec database = new DatabaseSpec();
    private FeaturesSpec features = new FeaturesSpec();

    public ApicurioStudioSpec() {
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

    public IngressSpec getWsIngress() {
        return wsIngress;
    }

    public void setWsIngress(IngressSpec wsIngress) {
        this.wsIngress = wsIngress;
    }

    public IngressSpec getApiIngress() {
        return apiIngress;
    }

    public void setApiIngress(IngressSpec apiIngress) {
        this.apiIngress = apiIngress;
    }

    public IngressSpec getStudioIngress() {
        return studioIngress;
    }

    public void setStudioIngress(IngressSpec studioIngress) {
        this.studioIngress = studioIngress;
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
