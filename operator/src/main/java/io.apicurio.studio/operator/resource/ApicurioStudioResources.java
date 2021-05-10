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
package io.apicurio.studio.operator.resource;

import io.apicurio.studio.operator.api.ApicurioStudioSpec;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

/**
 * @author laurent.broudoux@gmail.com
 */
public class ApicurioStudioResources {

   public static final String APICURIO_STUDIO_API_MODULE = "apicurio-studio-api";
   public static final String APICURIO_STUDIO_WS_MODULE = "apicurio-studio-ws";
   public static final String APICURIO_STUDIO_UI_MODULE = "apicurio-studio-ui";

   /**
    * Get the name of Deployment to create for the API module.
    * @param spec The specification from custom resource
    * @return The deployment name.
    */
   public static String getAPIDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-api";
   }

   /**
    * Prepare a new Deployment for the API module.
    * @param spec The specification from custom resource
    * @return The full deployment.
    */
   public static Deployment prepareAPIDeployment(ApicurioStudioSpec spec) {
      // Building a fresh new Deployment according the spec.
      DeploymentBuilder builder = new DeploymentBuilder()
            .withNewMetadata()
               .withName(getAPIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_API_MODULE)
            .endMetadata()
            .withNewSpec()
               .withReplicas(1)
               .withNewSelector()
                  .addToMatchLabels("app", spec.getName())
                  .addToMatchLabels("module", APICURIO_STUDIO_API_MODULE)
               .endSelector()
               .withNewTemplate()
                  .withNewMetadata()
                     .addToLabels("app", spec.getName())
                     .addToLabels("module", APICURIO_STUDIO_API_MODULE)
                  .endMetadata()
                  .withNewSpec()
                     .addNewContainer()
                        .withName("api")
                        .withImage("apicurio/apicurio-studio-api:latest")
                        .addToPorts(
                              new ContainerPortBuilder().withNewContainerPort(8080).withProtocol("TCP").build()
                        )
                        .addToEnv(
                              new EnvVar("APICURIO_KC_REALM", spec.getKeycloak().getRealm(), null),
                              new EnvVar("APICURIO_DB_TYPE", spec.getDatabase().getType(), null),
                              new EnvVar("APICURIO_DB_DRIVER_NAME", spec.getDatabase().getDriver(), null),
                              new EnvVar("APICURIO_DB_CONNECTION_URL", spec.getDatabase().getUrl(), null)
                        )
                        .addNewEnv()
                           .withName("APICURIO_DB_USER_NAME")
                           .withNewValueFrom()
                              .withNewSecretKeyRef().withName(DatabaseResources.getDatabaseSecretName(spec)).withKey("database-user").endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .addNewEnv()
                           .withName("APICURIO_DB_PASSWORD")
                           .withNewValueFrom()
                              .withNewSecretKeyRef().withName(DatabaseResources.getDatabaseSecretName(spec)).withKey("database-password").endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .withNewLivenessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/system/ready").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(30)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endLivenessProbe()
                        .withNewReadinessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/system/ready").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(15)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endReadinessProbe()
                     .endContainer()
                  .endSpec()
               .endTemplate()
            .endSpec();

      return builder.build();
   }

   /**
    * Prepare a new Service for the API module.
    * @param spec The specification from custom resource
    * @return The full service.
    */
   public static Service prepareAPIService(ApicurioStudioSpec spec) {
      // Building a fresh new Service according the spec.
      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getAPIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_API_MODULE)
               .addToAnnotations("prometheus.io/scrape", "true")
               .addToAnnotations("prometheus.io.path", "/system/metrics")
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", spec.getName())
               .addToSelector("module", APICURIO_STUDIO_API_MODULE)
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
    * Get the name of Deployment to create for the WS module.
    * @param spec The specification from custom resource
    * @return The deployment name.
    */
   public static String getWSDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-ws";
   }

   /**
    * Prepare a new Deployment for the WS module.
    * @param spec The specification from custom resource
    * @return The full deployment.
    */
   public static Deployment prepareWSDeployment(ApicurioStudioSpec spec) {
      // Building a fresh new Deployment according the spec.
      DeploymentBuilder builder = new DeploymentBuilder()
            .withNewMetadata()
               .withName(getWSDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_WS_MODULE)
            .endMetadata()
            .withNewSpec()
               .withReplicas(1)
               .withNewSelector()
                  .addToMatchLabels("app", spec.getName())
                  .addToMatchLabels("module", APICURIO_STUDIO_WS_MODULE)
               .endSelector()
               .withNewTemplate()
                  .withNewMetadata()
                     .addToLabels("app", spec.getName())
                     .addToLabels("module", APICURIO_STUDIO_WS_MODULE)
                  .endMetadata()
                  .withNewSpec()
                     .addNewContainer()
                        .withName("ws")
                        .withImage("apicurio/apicurio-studio-ws:latest")
                        .addToPorts(
                              new ContainerPortBuilder().withNewContainerPort(8080).withProtocol("TCP").build()
                        )
                        .addToEnv(
                              new EnvVar("APICURIO_DB_TYPE", spec.getDatabase().getType(), null),
                              new EnvVar("APICURIO_DB_DRIVER_NAME", spec.getDatabase().getDriver(), null),
                              new EnvVar("APICURIO_DB_CONNECTION_URL", spec.getDatabase().getUrl(), null)
                        )
                        .addNewEnv()
                           .withName("APICURIO_DB_USER_NAME")
                           .withNewValueFrom()
                              .withNewSecretKeyRef().withName(DatabaseResources.getDatabaseSecretName(spec)).withKey("database-user").endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .addNewEnv()
                           .withName("APICURIO_DB_PASSWORD")
                           .withNewValueFrom()
                              .withNewSecretKeyRef().withName(DatabaseResources.getDatabaseSecretName(spec)).withKey("database-password").endSecretKeyRef()
                           .endValueFrom()
                        .endEnv()
                        .withNewLivenessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/metrics").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(30)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endLivenessProbe()
                        .withNewReadinessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/metrics").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(15)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endReadinessProbe()
                     .endContainer()
                  .endSpec()
               .endTemplate()
            .endSpec();

      return builder.build();
   }

   /**
    * Prepare a new Service for the WS module.
    * @param spec The specification from custom resource
    * @return The full service.
    */
   public static Service prepareWSService(ApicurioStudioSpec spec) {
      // Building a fresh new Service according the spec.
      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getWSDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_WS_MODULE)
               .addToAnnotations("prometheus.io/scrape", "true")
               .addToAnnotations("prometheus.io.path", "/metrics")
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", spec.getName())
               .addToSelector("module", APICURIO_STUDIO_WS_MODULE)
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
    * Get the name of Deployment to create for the UI module.
    * @param spec The specification from custom resource
    * @return The deployment name.
    */
   public static String getUIDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-ui";
   }

   /**
    * Prepare a new Deployment for the UI module.
    * @param spec The specification from custom resource
    * @return The full deployment.
    */
   public static Deployment prepareUIDeployment(ApicurioStudioSpec spec) {
      // Building a fresh new Deployment according the spec.
      DeploymentBuilder builder = new DeploymentBuilder()
            .withNewMetadata()
               .withName(getUIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
            .endMetadata()
            .withNewSpec()
               .withReplicas(1)
               .withNewSelector()
                  .addToMatchLabels("app", spec.getName())
                  .addToMatchLabels("module", APICURIO_STUDIO_UI_MODULE)
               .endSelector()
               .withNewTemplate()
                  .withNewMetadata()
                     .addToLabels("app", spec.getName())
                     .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
                  .endMetadata()
                  .withNewSpec()
                     .addNewContainer()
                        .withName("ui")
                        .withImage("apicurio/apicurio-studio-ui:latest")
                        .addToPorts(
                              new ContainerPortBuilder().withNewContainerPort(8080).withProtocol("TCP").build()
                        )
                        .addToEnv(
                              new EnvVar("APICURIO_KS_REALM", spec.getKeycloak().getRealm(), null),
                              new EnvVar("APICURIO_UI_LOGOUT_REDIRECT", "/", null)
                        )
                        .withNewLivenessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/ready").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(30)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endLivenessProbe()
                        .withNewReadinessProbe()
                           .withHttpGet(new HTTPGetActionBuilder().withPath("/ready").withPort(new IntOrString(8080)).withScheme("HTTP").build())
                           .withInitialDelaySeconds(15)
                           .withTimeoutSeconds(5)
                           .withPeriodSeconds(10)
                           .withSuccessThreshold(1)
                           .withFailureThreshold(3)
                        .endReadinessProbe()
                     .endContainer()
                  .endSpec()
               .endTemplate()
            .endSpec();

      return builder.build();
   }

   /**
    * Prepare a new Service for the UI module.
    * @param spec The specification from custom resource
    * @return The full service.
    */
   public static Service prepareUIService(ApicurioStudioSpec spec) {
      // Building a fresh new Service according the spec.
      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getUIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", spec.getName())
               .addToSelector("module", APICURIO_STUDIO_UI_MODULE)
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
}
