/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apicurio.studio.operator.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.fabric8.kubernetes.api.model.KubernetesResource;

/**
 * @author laurent.broudoux@gmail.com
 */
public class ModuleStatus implements KubernetesResource {

    private ApicurioStudioStatus.State state = ApicurioStudioStatus.State.UNKNOWN;
    private boolean error;
    private String message;
    private String lastTransitionTime;

    public ModuleStatus(ApicurioStudioStatus.State state) {
        this.state = state;
        this.error = false;
        this.lastTransitionTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ModuleStatus(ApicurioStudioStatus.State state, boolean error, String message) {
        this.state = state;
        this.error = error;
        this.message = message;
        this.lastTransitionTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ApicurioStudioStatus.State getState() {
        return state;
    }

    public void setState(ApicurioStudioStatus.State state) {
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

    public String getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(String lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }
}
