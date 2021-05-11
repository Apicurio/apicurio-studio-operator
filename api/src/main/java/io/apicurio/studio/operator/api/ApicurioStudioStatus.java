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

/**
 * @author laurent.broudoux@gmail.com
 */
public class ApicurioStudioStatus {

    private String studioUrl;
    private String keycloakUrl;
    private State state = State.UNKNOWN;
    private boolean error;
    private String message;

    private ModuleStatus apiModule;
    private ModuleStatus wsModule;
    private ModuleStatus uiModule;
    private ModuleStatus keycloakModule;
    private ModuleStatus databaseModule;

    public enum State {
        PREEXISTING,
        DEPLOYING,
        CREATED,
        ERROR,
        UNKNOWN
    }

    public String getStudioUrl() {
        return studioUrl;
    }

    public void setStudioUrl(String studioUrl) {
        this.studioUrl = studioUrl;
    }

    public String getKeycloakUrl() {
        return keycloakUrl;
    }

    public void setKeycloakUrl(String keycloakUrl) {
        this.keycloakUrl = keycloakUrl;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ModuleStatus getApiModule() {
        return apiModule;
    }

    public void setApiModule(ModuleStatus apiModule) {
        this.apiModule = apiModule;
    }

    public ModuleStatus getWsModule() {
        return wsModule;
    }

    public void setWsModule(ModuleStatus wsModule) {
        this.wsModule = wsModule;
    }

    public ModuleStatus getUiModule() {
        return uiModule;
    }

    public void setUiModule(ModuleStatus uiModule) {
        this.uiModule = uiModule;
    }

    public ModuleStatus getKeycloakModule() {
        return keycloakModule;
    }

    public void setKeycloakModule(ModuleStatus keycloakModule) {
        this.keycloakModule = keycloakModule;
    }

    public ModuleStatus getDatabaseModule() {
        return databaseModule;
    }

    public void setDatabaseModule(ModuleStatus databaseModule) {
        this.databaseModule = databaseModule;
    }
}
