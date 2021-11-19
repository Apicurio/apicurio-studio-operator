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

import java.util.Collections;
import java.util.Map;

/**
 * This is the specification of Ingress configuration for Apicurio Studio.
 * @author laurent.broudoux@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(
      editableEnabled = false,
      builderPackage = "io.fabric8.kubernetes.api.builder"
)
public class IngressSpec {

   private boolean generateCert = true;
   private String secretRef;
   private Map<String, String> annotations;

   public IngressSpec() {
   }

   public boolean isGenerateCert() {
      return generateCert;
   }

   public void setGenerateCert(boolean generateCert) {
      this.generateCert = generateCert;
   }

   public String getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(String secretRef) {
      this.secretRef = secretRef;
   }

   public Map<String, String> getAnnotations() {
      return Collections.unmodifiableMap(annotations);
   }

   public void setAnnotations(Map<String, String> annotations) {
      this.annotations = annotations;
   }
}
