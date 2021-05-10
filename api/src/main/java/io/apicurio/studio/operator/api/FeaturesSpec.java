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

/**
 * @author laurent.broudoux@gmail.com
 */
public class FeaturesSpec {

    private boolean asyncAPI = false;
    private boolean graphQL = false;
    private MicrocksSpec microcks = new MicrocksSpec();

    public boolean isAsyncAPI() {
        return asyncAPI;
    }

    public void setAsyncAPI(boolean asyncAPI) {
        this.asyncAPI = asyncAPI;
    }

    public boolean isGraphQL() {
        return graphQL;
    }

    public void setGraphQL(boolean graphQL) {
        this.graphQL = graphQL;
    }

    public MicrocksSpec getMicrocks() {
        return microcks;
    }

    public void setMicrocks(MicrocksSpec microcks) {
        this.microcks = microcks;
    }
}
