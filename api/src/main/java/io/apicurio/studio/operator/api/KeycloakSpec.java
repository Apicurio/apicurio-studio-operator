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

/**
 * This is the specification of the Keycloak module of Apicurio Studio.
 * @author laurent.broudoux@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakSpec {

    private boolean install = true;
    private String realm = "apicurio";
    private String url;
    private String volumeSize = "500Mi";
    private String user;
    private String password;

    public boolean isInstall() {
        return install;
    }

    public void setInstall(boolean install) {
        this.install = install;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(String volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
