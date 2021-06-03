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

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * This is the API definition for ApicurioStudio custom resource.
 * @author laurent.broudoux@gmail.com
 */
@Group("studio.apicur.io")
@Version("v1alpha1")
@ShortNames("studio")
@Plural("ApicurioStudios")
public class ApicurioStudio extends CustomResource<ApicurioStudioSpec, ApicurioStudioStatus> implements Namespaced {

   /*
   @JsonIgnore
   public boolean isDeploying() {
      return getStatus().getState().equals(ApicurioStudioStatus.State.DEPLOYING);
   }

   @JsonIgnore
   public boolean isReady() {
      return getStatus().getState().equals(ApicurioStudioStatus.State.READY);
   }
   */
}
