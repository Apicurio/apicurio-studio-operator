/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

/**
 * This is the specification of a Module configuration for Apicurio Studio.
 * @author laurent.broudoux@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class ModuleSpec {

   private String image;
   private IngressSpec ingress;
   private ResourceRequirements resources;

   /** Default empty constructor for deserializer. */
   public ModuleSpec() {
   }

   /**
    * Create a module spec with default image name.
    * @param image The image name for this module.
    */
   public ModuleSpec(String image) {
      this.image = image;
   }

   public String getImage() {
      return image;
   }

   public void setImage(String image) {
      this.image = image;
   }

   public IngressSpec getIngress() {
      return ingress;
   }

   public void setIngress(IngressSpec ingress) {
      this.ingress = ingress;
   }

   public ResourceRequirements getResources() {
      return resources;
   }

   public void setResources(ResourceRequirements resources) {
      this.resources = resources;
   }
}
