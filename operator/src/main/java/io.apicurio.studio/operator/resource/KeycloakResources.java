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
package io.apicurio.studio.operator.resource;

import io.apicurio.studio.operator.api.ApicurioStudioSpec;
import io.apicurio.studio.operator.api.ApicurioStudioStatus;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author laurent.broudoux@gmail.com
 */
public class KeycloakResources {

   public static final String APICURIO_STUDIO_AUTH_MODULE = "apicurio-studio-auth";

   /**
    *
    * @param spec
    * @return
    */
   public static String getKeycloakSecretName(ApicurioStudioSpec spec) {
      return spec.getName() + "-auth-keycloak";
   }

   /**
    *
    * @param spec
    * @return
    */
   public static Secret prepareKeycloakSecret(ApicurioStudioSpec spec) {
      // Building a fresh new Secret according the spec.
      SecretBuilder builder = new SecretBuilder()
            .withNewMetadata()
               .withName(getKeycloakSecretName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_AUTH_MODULE)
            .endMetadata()
            .addToStringData("keycloak-user", StringUtils.defaultIfBlank(spec.getKeycloak().getUser(), RandomStringUtils.randomAlphanumeric(8)))
            .addToStringData("keycloak-password", StringUtils.defaultIfBlank(spec.getKeycloak().getPassword(), RandomStringUtils.randomAlphanumeric(16)));

      return builder.build();
   }

   /**
    *
    * @param spec
    * @return
    */
   public static String getKeycloakPVCName(ApicurioStudioSpec spec) {
      return spec.getName() + "-auth-claim";
   }

   /**
    *
    * @param spec
    * @return
    */
   public static PersistentVolumeClaim prepareKeycloakDbPVC(ApicurioStudioSpec spec) {
      // Building a fresh new PersistentVolumeClain according the spec.
      PersistentVolumeClaimBuilder builder = new PersistentVolumeClaimBuilder()
            .withNewMetadata()
               .withName(getKeycloakPVCName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_AUTH_MODULE)
            .endMetadata()
            .withNewSpec()
               .withAccessModes("ReadWriteOnce")
               .withNewResources()
                  .addToRequests("storage", new Quantity(spec.getKeycloak().getVolumeSize()))
               .endResources()
            .endSpec();

      return builder.build();
   }

   /**
    *
    * @param spec
    * @return
    */
   public static String getKeycloakDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-auth";
   }

   /**
    *
    * @param client
    * @param spec
    * @return
    */
   public static Deployment prepareKeycloakDeployment(KubernetesClient client, ApicurioStudioSpec spec, ApicurioStudioStatus status) {
      // Building a fresh new Deployment according the spec.
      Deployment deployment = client.apps().deployments()
            .load(KeycloakResources.class.getResourceAsStream("/k8s/keycloak-deployment.yml")).get();

      // Complete with app specific labels and selectors.
      deployment = new DeploymentBuilder(deployment)
            .editMetadata()
               .withName(getKeycloakDeploymentName(spec))
               .addToLabels("app", spec.getName())
            .endMetadata()
            .editSpec()
               .editSelector().addToMatchLabels("app", spec.getName()).endSelector()
               .editTemplate()
                  .editMetadata()
                     .addToLabels("app", spec.getName())
                  .endMetadata()
                  .editSpec()
                     .editContainer(0)
                        .addNewEnv()
                           .withName("APICURIO_UI_URL")
                           .withValue("https://" + status.getStudioUrl())
                        .endEnv()
                        .addNewEnv()
                           .withName("APICURIO_KEYCLOAK_USER")
                           .withNewValueFrom()
                              .withNewSecretKeyRef()
                                 .withName(getKeycloakSecretName(spec))
                                 .withKey("keycloak-user")
                              .endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .addNewEnv()
                           .withName("APICURIO_KEYCLOAK_PASSWORD")
                           .withNewValueFrom()
                              .withNewSecretKeyRef()
                                 .withName(getKeycloakSecretName(spec))
                                 .withKey("keycloak-password")
                              .endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .withNewResources()
                           .addToRequests(Map.of("cpu", new Quantity("100m")))
                           .addToRequests(Map.of("memory", new Quantity("600Mi")))
                           .addToLimits(Map.of("cpu", new Quantity("1")))
                           .addToLimits(Map.of("memory", new Quantity("1300Mi")))
                        .endResources()
                     .endContainer()
                     .addNewVolume()
                        .withName("keycloak-data")
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(getKeycloakPVCName(spec), false))
                     .endVolume()
                  .endSpec()
               .endTemplate()
            .endSpec().build();

      return deployment;
   }

   /**
    *
    * @param spec
    * @return
    */
   public static Service prepareKeycloakService(ApicurioStudioSpec spec) {
      // Building a fresh new Service according the spec.
      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getKeycloakDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_AUTH_MODULE)
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", spec.getName())
               .addToSelector("module", APICURIO_STUDIO_AUTH_MODULE)
               .addNewPort()
                  .withPort(8080)
                  .withProtocol("TCP")
                  .withTargetPort(new IntOrString(8080))
               .endPort()
               .withSessionAffinity("None")
               .withType("ClusterIP")
            .endSpec();

      return builder.build();
   }

   /**
    *
    * @param spec
    * @return
    */
   public static Route prepareKeycloakRoute(ApicurioStudioSpec spec) {
      // Building a fresh new Route according the spec.
      RouteBuilder builder = new RouteBuilder()
            .withNewMetadata()
               .withName(getKeycloakDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_AUTH_MODULE)
            .endMetadata()
            .withNewSpec()
               .withNewTo()
                  .withKind("Service")
                  .withName(getKeycloakDeploymentName(spec))
               .endTo()
               .withNewPort()
                  .withNewTargetPort(8080)
               .endPort()
               .withNewTls()
                  .withTermination("edge")
                  .withInsecureEdgeTerminationPolicy("Redirect")
               .endTls()
            .endSpec();

      return builder.build();
   }
}
