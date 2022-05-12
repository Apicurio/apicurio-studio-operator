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

import io.apicurio.studio.operator.Constants;
import io.apicurio.studio.operator.api.ApicurioStudioSpec;
import io.apicurio.studio.operator.api.ApicurioStudioStatus;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;

import java.util.Map;

/**
 * Holds utility methods to create Apicurio resources from specification.
 * @author laurent.broudoux@gmail.com
 */
public class ApicurioStudioResources {

   public static final String APICURIO_STUDIO_API_MODULE = "apicurio-studio-api";
   public static final String APICURIO_STUDIO_WS_MODULE = "apicurio-studio-ws";
   public static final String APICURIO_STUDIO_UI_MODULE = "apicurio-studio-ui";

   public static final String APICURIO_STUDIO_API_MODULE_DEFAULT_INGRESS_SECRET = APICURIO_STUDIO_API_MODULE + "-ingress-secret";
   public static final String APICURIO_STUDIO_WS_MODULE_DEFAULT_INGRESS_SECRET = APICURIO_STUDIO_WS_MODULE + "-ingress-secret";
   public static final String APICURIO_STUDIO_UI_MODULE_DEFAULT_INGRESS_SECRET = APICURIO_STUDIO_UI_MODULE + "-ingress-secret";

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
    * @param status The current status of custom resource
    * @return The full deployment.
    */
   public static Deployment prepareAPIDeployment(ApicurioStudioSpec spec, ApicurioStudioStatus status) {
      // Building a fresh new Deployment according the spec.
      DeploymentBuilder builder = new DeploymentBuilder()
            .withNewMetadata()
               .withName(getAPIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_API_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
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
                        .withImage(spec.getApiModule().getImage())
                        .addNewPort().withContainerPort(8080).withProtocol("TCP").endPort()
                        .addNewEnv().withName("APICURIO_KC_AUTH_URL").withValue("https://" + status.getKeycloakUrl() + "/auth").endEnv()
                        .addNewEnv().withName("APICURIO_KC_REALM").withValue(spec.getKeycloak().getRealm()).endEnv()
                        .addNewEnv().withName("APICURIO_DB_TYPE").withValue(spec.getDatabase().getType()).endEnv()
                        .addNewEnv().withName("APICURIO_DB_DRIVER_NAME").withValue(spec.getDatabase().getDriver()).endEnv()
                        .addNewEnv().withName("APICURIO_DB_CONNECTION_URL").withValue(DatabaseResources.getDatabaseConnectionUrl(spec)).endEnv()
                        .addNewEnv().withName("APICURIO_HUB_STORAGE_JDBC_TYPE").withValue(spec.getDatabase().getType()).endEnv()
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
                        .withResources(spec.getApiModule().getResources())
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

      // Add optional features if specified.
      if (spec.getFeatures().getMicrocks().getApiUrl() != null) {
         builder.editSpec()
                  .editTemplate()
                     .editSpec()
                        .editContainer(0)
                           .addNewEnv().withName("APICURIO_MICROCKS_API_URL").withValue(spec.getFeatures().getMicrocks().getApiUrl()).endEnv()
                           .addNewEnv().withName("APICURIO_MICROCKS_CLIENT_ID").withValue(spec.getFeatures().getMicrocks().getClientId()).endEnv()
                           .addNewEnv().withName("APICURIO_MICROCKS_CLIENT_SECRET").withValue(spec.getFeatures().getMicrocks().getClientSecret()).endEnv()
                        .endContainer()
                     .endSpec()
                  .endTemplate()
               .endSpec();
      }

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
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
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
    * Prepare a new OpenShift Route for the API module.
    * @param spec The specification from custom resource
    * @return The full Route
    */
   public static Route prepareAPIRoute(ApicurioStudioSpec spec) {
      // Building a fresh new Route according the spec.
      RouteBuilder builder = new RouteBuilder()
            .withNewMetadata()
               .withName(getAPIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_API_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
            .endMetadata()
            .withNewSpec()
               .withNewTo()
                  .withKind("Service")
                  .withName(getAPIDeploymentName(spec))
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

   /**
    * Get the API module Ingress host name from spec.
    * @param spec The specification from custom resource
    * @return The host name to use for Kubernetes Ingress
    */
   public static String getAPIIngressHost(ApicurioStudioSpec spec) {
      return APICURIO_STUDIO_API_MODULE + "." + spec.getUrl();
   }

   /**
    * Prepare a vanilla Kubernetes Ingress for the API module.
    * @param spec The specification from custom resource
    * @return A full Ingress
    */
   public static Ingress prepareAPIIngress(ApicurioStudioSpec spec) {
      // Building a fresh new Ingress according the spec.
      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getAPIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_API_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(IngressSpecUtil.getAnnotationsIfAny(spec.getApiModule().getIngress()))
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addNewHost(getAPIIngressHost(spec))
                  .withSecretName(IngressSpecUtil.getSecretName(spec.getApiModule().getIngress(),
                        APICURIO_STUDIO_API_MODULE_DEFAULT_INGRESS_SECRET))
               .endTl()
               .addNewRule()
                  .withHost(getAPIIngressHost(spec))
                  .withNewHttp()
                     .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                           .withNewService()
                              .withName(getAPIDeploymentName(spec))
                              .withNewPort()
                                 .withNumber(8080)
                              .endPort()
                           .endService()
                        .endBackend()
                     .endPath()
                  .endHttp()
               .endRule()
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
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
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
                        .withImage(spec.getWsModule().getImage())
                        .addNewPort().withContainerPort(8080).withProtocol("TCP").endPort()
                        .addNewEnv().withName("APICURIO_DB_TYPE").withValue(spec.getDatabase().getType()).endEnv()
                        .addNewEnv().withName("APICURIO_DB_DRIVER_NAME").withValue(spec.getDatabase().getDriver()).endEnv()
                        .addNewEnv().withName("APICURIO_DB_CONNECTION_URL").withValue(DatabaseResources.getDatabaseConnectionUrl(spec)).endEnv()
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
                        .addNewEnv().withName("APICURIO_HUB_STORAGE_JDBC_TYPE").withValue(spec.getDatabase().getType()).endEnv()
                        .withResources(spec.getWsModule().getResources())
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
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
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
    * Prepare a new OpenShift Route for the WS module.
    * @param spec The specification from custom resource
    * @return The full Route
    */
   public static Route prepareWSRoute(ApicurioStudioSpec spec) {
      // Building a fresh new Route according the spec.
      RouteBuilder builder = new RouteBuilder()
            .withNewMetadata()
               .withName(getWSDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_WS_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
            .endMetadata()
            .withNewSpec()
               .withNewTo()
                  .withKind("Service")
                  .withName(getWSDeploymentName(spec))
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

   /**
    * Get the WS module Ingress host name from spec.
    * @param spec The specification from custom resource
    * @return The host name to use for Kubernetes Ingress
    */
   public static String getWSIngressHost(ApicurioStudioSpec spec) {
      return APICURIO_STUDIO_WS_MODULE + "." + spec.getUrl();
   }

   /**
    * Prepare a vanilla Kubernetes Ingress for the WS module.
    * @param spec The specification from custom resource
    * @return A full Ingress
    */
   public static Ingress prepareWSIngress(ApicurioStudioSpec spec) {
      // Building a fresh new Ingress according the spec.
      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getWSDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_WS_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(IngressSpecUtil.getAnnotationsIfAny(spec.getWsModule().getIngress()))
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addNewHost(getWSIngressHost(spec))
                  .withSecretName(IngressSpecUtil.getSecretName(spec.getWsModule().getIngress(),
                        APICURIO_STUDIO_WS_MODULE_DEFAULT_INGRESS_SECRET))
               .endTl()
               .addNewRule()
                  .withHost(getWSIngressHost(spec))
                  .withNewHttp()
                     .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                           .withNewService()
                              .withName(getWSDeploymentName(spec))
                              .withNewPort()
                                 .withNumber(8080)
                              .endPort()
                           .endService()
                        .endBackend()
                     .endPath()
                  .endHttp()
               .endRule()
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
    * @param status The current status of custom resource
    * @return The full deployment.
    */
   public static Deployment prepareUIDeployment(ApicurioStudioSpec spec, ApicurioStudioStatus status) {
      // Building a fresh new Deployment according the spec.
      DeploymentBuilder builder = new DeploymentBuilder()
            .withNewMetadata()
               .withName(getUIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
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
                        .withImage(spec.getStudioModule().getImage())
                        .addNewPort().withContainerPort(8080).withProtocol("TCP").endPort()
                        .addNewEnv().withName("APICURIO_KC_AUTH_URL").withValue("https://" + status.getKeycloakUrl() + "/auth").endEnv()
                        .addNewEnv().withName("APICURIO_KC_REALM").withValue(spec.getKeycloak().getRealm()).endEnv()
                        .addNewEnv().withName("APICURIO_UI_HUB_API_URL").withValue("https://" + status.getApiUrl()).endEnv()
                        .addNewEnv().withName("APICURIO_UI_EDITING_URL").withValue("wss://" + status.getWsUrl()).endEnv()
                        .addNewEnv().withName("APICURIO_UI_LOGOUT_REDIRECT").withValue("/").endEnv()
                        .withResources(spec.getStudioModule().getResources())
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

      // Add optional features if specified.
      if (spec.getFeatures().isAsyncAPI()) {
         builder.editSpec()
               .editTemplate()
                  .editSpec()
                     .editContainer(0)
                        .addNewEnv().withName("APICURIO_UI_FEATURE_ASYNCAPI").withValue("true").endEnv()
                     .endContainer()
                  .endSpec()
               .endTemplate()
               .endSpec();
      }
      if (spec.getFeatures().isGraphQL()) {
         builder.editSpec()
               .editTemplate()
                  .editSpec()
                     .editContainer(0)
                        .addNewEnv().withName("APICURIO_UI_FEATURE_GRAPHQL").withValue("true").endEnv()
                     .endContainer()
                  .endSpec()
               .endTemplate()
               .endSpec();
      }
      if (spec.getFeatures().getMicrocks().getApiUrl() != null) {
         builder.editSpec()
               .editTemplate()
                  .editSpec()
                     .editContainer(0)
                        .addNewEnv().withName("APICURIO_UI_FEATURE_MICROCKS").withValue("true").endEnv()
                     .endContainer()
                  .endSpec()
               .endTemplate()
               .endSpec();
      }

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

   /**
    * Prepare a new OpenShift Route for the UI module.
    * @param spec The specification from custom resource
    * @return The full Route
    */
   public static Route prepareUIRoute(ApicurioStudioSpec spec) {
      // Building a fresh new Route according the spec.
      RouteBuilder builder = new RouteBuilder()
            .withNewMetadata()
               .withName(getUIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
            .endMetadata()
            .withNewSpec()
               .withNewTo()
                  .withKind("Service")
                  .withName(getUIDeploymentName(spec))
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

   /**
    * Get the UI module Ingress host name from spec.
    * @param spec The specification from custom resource
    * @return The host name to use for Kubernetes Ingress
    */
   public static String getUIIngressHost(ApicurioStudioSpec spec) {
      return APICURIO_STUDIO_UI_MODULE + "." + spec.getUrl();
   }

   /**
    * Prepare a vanilla Kubernetes Ingress for the UI module.
    * @param spec The specification from custom resource
    * @return A full Ingress
    */
   public static Ingress prepareUIIngress(ApicurioStudioSpec spec) {
      // Building a fresh new Ingress according the spec.
      IngressBuilder builder = new IngressBuilder()
            .withNewMetadata()
               .withName(getUIDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_UI_MODULE)
               .addToLabels(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
               .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
               .addToAnnotations(IngressSpecUtil.getAnnotationsIfAny(spec.getStudioModule().getIngress()))
            .endMetadata()
            .withNewSpec()
               .addNewTl()
                  .addNewHost(getUIIngressHost(spec))
                  .withSecretName(IngressSpecUtil.getSecretName(spec.getStudioModule().getIngress(),
                        APICURIO_STUDIO_UI_MODULE_DEFAULT_INGRESS_SECRET))
               .endTl()
               .addNewRule()
                  .withHost(getUIIngressHost(spec))
                  .withNewHttp()
                     .addNewPath()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                           .withNewService()
                              .withName(getUIDeploymentName(spec))
                              .withNewPort()
                                 .withNumber(8080)
                              .endPort()
                           .endService()
                        .endBackend()
                     .endPath()
                  .endHttp()
               .endRule()
            .endSpec();

      return builder.build();
   }
}
