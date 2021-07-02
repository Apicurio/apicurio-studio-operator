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
package io.apicurio.studio.operator;

import javax.inject.Inject;

import io.apicurio.studio.operator.api.ModuleStatus;
import io.apicurio.studio.operator.resource.DatabaseResources;
import io.apicurio.studio.operator.resource.KeycloakResources;
import io.apicurio.studio.operator.watcher.DeploymentEvent;
import io.apicurio.studio.operator.watcher.DeploymentEventSource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jboss.logging.Logger;

import io.apicurio.studio.operator.api.ApicurioStudio;
import io.apicurio.studio.operator.api.ApicurioStudioSpec;
import io.apicurio.studio.operator.api.ApicurioStudioStatus;
import io.apicurio.studio.operator.resource.ApicurioStudioResources;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

import java.util.List;
import java.util.Optional;

/**
 * This is the Operator controller that managed the reconciliation loop.
 * @author laurent.broudoux@gmail.com
 */
@Controller(namespaces = Controller.WATCH_CURRENT_NAMESPACE)
public class ApicurioStudioController implements ResourceController<ApicurioStudio> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Inject
   KubernetesClient client;

   private boolean isOpenShift = false;

   private DeploymentEventSource deploymentEventSource;

   @Override
   public void init(EventSourceManager eventSourceManager) {
      this.deploymentEventSource = DeploymentEventSource.createAndRegisterWatch(this, client);
      eventSourceManager.registerEventSource("deployment-event-source", this.deploymentEventSource);
   }

   @Override
   public UpdateControl<ApicurioStudio> createOrUpdateResource(ApicurioStudio apicurioStudio,
                                                               Context<ApicurioStudio> context) {

      final ApicurioStudioSpec spec = apicurioStudio.getSpec();
      logger.infof("Starting CreateOrUpdate reconcile operation for '%s'", spec.getName());
      logger.infof("Context events: " + context.getEvents().getList());

      isOpenShift = client.isAdaptable(OpenShiftClient.class);
      final String ns = apicurioStudio.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(apicurioStudio));

      Optional<CustomResourceEvent> latestCREvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
      if (latestCREvent.isPresent()) {
         Action action = latestCREvent.get().getAction();
         logger.infof("Latest CR event action is: " + latestCREvent.get().getAction());

         if (apicurioStudio.getStatus() == null) {
            apicurioStudio.setStatus(new ApicurioStudioStatus());
         }
         if (apicurioStudio.getStatus().isReady()) {
            // Maybe operator has been restarted...
            logger.infof("ApicurioStudio '%s' seems to be ready, exiting reconciliation loop.", spec.getName());
            return UpdateControl.noUpdate();
         }

         // First thing is to retrieve the UI module Host that is needed by Keycloak.
         if (isOpenShift) {
            // Create an OpenShift Route...
            logger.infof("Creating a new Route for apicurio-studio-ui, name '%s'", ApicurioStudioResources.getUIDeploymentName(spec));
            Route uiRoute = ApicurioStudioResources.prepareUIRoute(spec);
            uiRoute.getMetadata().setOwnerReferences(refs);
            uiRoute = client.adapt(OpenShiftClient.class).routes().inNamespace(ns).createOrReplace(uiRoute);

            apicurioStudio.getStatus().setStudioUrl(uiRoute.getSpec().getHost());
         } else {
            // Create a vanilla Kubernetes Ingress...
         }

         try {
            createOrUpdateKeycloakResources(apicurioStudio);
            createOrUpdateDatabaseResources(apicurioStudio);
            createOrUpdateApicurioStudioResources(apicurioStudio);
            apicurioStudio.getStatus().setState(ApicurioStudioStatus.State.DEPLOYING);
            logger.infof("Finishing the reconciliation loop with update of Status");
            return UpdateControl.updateStatusSubResource(apicurioStudio);
         } catch (Throwable t) {
            t.printStackTrace();
            logger.error("Caught a Throwable", t);
         }
      }

      List<Event> allEvents = context.getEvents().getList();
      for (Event event : allEvents) {
         if (event instanceof DeploymentEvent) {
            DeploymentEvent depEvent = (DeploymentEvent) event;
            logger.infof("Got a Deployment event for action '%s' on resource '%s'",
                  depEvent.getAction(), depEvent.getDeployment().getMetadata().getName());

            switch (depEvent.getAction()) {
               case MODIFIED:
                  handleModifiedDeployment(depEvent.getDeployment());
                  break;
               case DELETED:
                  handleDeletedDeployment(depEvent.getDeployment());
                  break;
            }
         }
      }

      logger.infof("Finishing the reconciliation loop with no update");
      return UpdateControl.noUpdate();
   }

   @Override
   public DeleteControl deleteResource(ApicurioStudio resource, Context<ApicurioStudio> context) {
      logger.infof("Deleting ApicurioStudio object {}", resource.getMetadata().getName());
      // Nothing to do here...
      // Framework takes care of deleting the ApicurioStudio object.
      // k8s takes care of deleting resources and pods because of ownerreference set.
      return DeleteControl.DEFAULT_DELETE;
   }

   /**
    * Manage Keycloak related resources.
    * @param cr The studio custom resource.
    */
   public void createOrUpdateKeycloakResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      if (spec.getKeycloak().isInstall()) {
         logger.infof("Creating a new Secret for apicurio-studio-auth, named '%s'", KeycloakResources.getKeycloakSecretName(spec));
         Secret authSecret = KeycloakResources.prepareKeycloakSecret(spec);
         authSecret.getMetadata().setOwnerReferences(refs);
         client.secrets().inNamespace(ns).createOrReplace(authSecret);

         String kcPVCName = KeycloakResources.getKeycloakPVCName(spec);
         if (client.persistentVolumeClaims().inNamespace(ns).withName(kcPVCName).get() == null) {
            logger.infof("Creating a new PersistentVolumeClaim for apicurio-studio-auth, named '%s'", kcPVCName);
            PersistentVolumeClaim authPVC = KeycloakResources.prepareKeycloakDbPVC(spec);
            authPVC.getMetadata().setOwnerReferences(refs);
            client.persistentVolumeClaims().inNamespace(ns).createOrReplace(authPVC);
         }

         logger.infof("Creating a new Service for apicurio-studio-auth, named '%s'", KeycloakResources.getKeycloakDeploymentName(spec));
         Service authService = KeycloakResources.prepareKeycloakService(spec);
         authService.getMetadata().setOwnerReferences(refs);
         client.services().inNamespace(ns).createOrReplace(authService);

         if (isOpenShift) {
            // Create an OpenShift Route...
            logger.infof("Creating a new Route for apicurio-studio-auth, name '%s'", KeycloakResources.getKeycloakDeploymentName(spec));
            Route authRoute = KeycloakResources.prepareKeycloakRoute(spec);
            authRoute.getMetadata().setOwnerReferences(refs);
            authRoute = client.adapt(OpenShiftClient.class).routes().inNamespace(ns).createOrReplace(authRoute);

            cr.getStatus().setKeycloakUrl(authRoute.getSpec().getHost());
         } else {
            // Create a vanilla Kubernetes Ingress...
         }

         logger.infof("Creating a new Deployment for apicurio-studio-auth, named '%s'", KeycloakResources.getKeycloakDeploymentName(spec));
         Deployment authDeployment = KeycloakResources.prepareKeycloakDeployment(client, spec, cr.getStatus());
         authDeployment.getMetadata().setOwnerReferences(refs);
         client.apps().deployments().inNamespace(ns).createOrReplace(authDeployment);

         cr.getStatus().setKeycloakModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
      } else {
         cr.getStatus().setKeycloakModule(new ModuleStatus(ApicurioStudioStatus.State.PREEXISTING));
      }
   }

   /**
    * Manage Database related resources.
    * @param cr The studio custom resource.s
    */
   public void createOrUpdateDatabaseResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      if (spec.getDatabase().isInstall()) {
         logger.infof("Creating a new Secret for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseSecretName(spec));
         Secret dbSecret = DatabaseResources.prepareDatabaseSecret(spec);
         dbSecret.getMetadata().setOwnerReferences(refs);
         client.secrets().inNamespace(ns).createOrReplace(dbSecret);

         String dbPVCName = DatabaseResources.getDatabasePVCName(spec);
         if (client.persistentVolumeClaims().inNamespace(ns).withName(dbPVCName).get() == null) {
            logger.infof("Creating a new PersistentVolumeClaim for apicurio-studio-db, named '%s'", dbPVCName);
            PersistentVolumeClaim dbPVC = DatabaseResources.prepareDatabasePVC(spec);
            dbPVC.getMetadata().setOwnerReferences(refs);
            client.persistentVolumeClaims().inNamespace(ns).createOrReplace(dbPVC);
         }

         logger.infof("Creating a new Service for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseDeploymentName(spec));
         Service dbService = DatabaseResources.prepareDatabaseService(spec);
         dbService.getMetadata().setOwnerReferences(refs);
         client.services().inNamespace(ns).createOrReplace(dbService);

         logger.infof("Creating a new Deployment for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseDeploymentName(spec));
         Deployment dbDeployment = DatabaseResources.prepareDatabaseDeployment(client, spec);
         dbDeployment.getMetadata().setOwnerReferences(refs);
         client.apps().deployments().inNamespace(ns).createOrReplace(dbDeployment);

         cr.getStatus().setDatabaseModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
      } else {
         cr.getStatus().setDatabaseModule(new ModuleStatus(ApicurioStudioStatus.State.PREEXISTING));
      }
   }

   /**
    * Manage the Apicurio Studio own resources.
    * @param cr The studio custom resource.
    */
   public void createOrUpdateApicurioStudioResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      // Dealing with resources of Api module.
      logger.infof("Creating a new Service for apicurio-studio-api, named '%s'", ApicurioStudioResources.getAPIDeploymentName(spec));
      Service apiService = ApicurioStudioResources.prepareAPIService(spec);
      apiService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(apiService);

      if (isOpenShift) {
         // Create an OpenShift Route...
         logger.infof("Creating a new Route for apicurio-studio-api, name '%s'", ApicurioStudioResources.getAPIDeploymentName(spec));
         Route apiRoute = ApicurioStudioResources.prepareAPIRoute(spec);
         apiRoute.getMetadata().setOwnerReferences(refs);
         apiRoute = client.adapt(OpenShiftClient.class).routes().inNamespace(ns).createOrReplace(apiRoute);

         logger.infof("Updating apiUrl in status with '%s'", apiRoute.getSpec().getHost());
         cr.getStatus().setApiUrl(apiRoute.getSpec().getHost());
      } else {
         // Create a vanilla Kubernetes Ingress...
      }

      logger.infof("Creating a new Deployment for apicurio-studio-api, named '%s'", ApicurioStudioResources.getAPIDeploymentName(spec));
      Deployment apiDeployment = ApicurioStudioResources.prepareAPIDeployment(spec, cr.getStatus());
      apiDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(apiDeployment);

      cr.getStatus().setApiModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));

      // Dealing with resources of Ws module.
      logger.infof("Creating a new Service for apicurio-studio-ws, named '%s'", ApicurioStudioResources.getWSDeploymentName(spec));
      Service wsService = ApicurioStudioResources.prepareWSService(spec);
      wsService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(wsService);

      if (isOpenShift) {
         // Create an OpenShift Route...
         logger.infof("Creating a new Route for apicurio-studio-ws, name '%s'", ApicurioStudioResources.getWSDeploymentName(spec));
         Route wsRoute = ApicurioStudioResources.prepareWSRoute(spec);
         wsRoute.getMetadata().setOwnerReferences(refs);
         wsRoute = client.adapt(OpenShiftClient.class).routes().inNamespace(ns).createOrReplace(wsRoute);

         logger.infof("Updating wsUrl in status with '%s'", wsRoute.getSpec().getHost());
         cr.getStatus().setWsUrl(wsRoute.getSpec().getHost());
      } else {
         // Create a vanilla Kubernetes Ingress...
      }

      logger.infof("Creating a new Deployment for apicurio-studio-ws, named '%s'", ApicurioStudioResources.getWSDeploymentName(spec));
      Deployment wsDeployment = ApicurioStudioResources.prepareWSDeployment(spec);
      wsDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(wsDeployment);

      cr.getStatus().setWsModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));

      // Dealing with resources of UI module.
      logger.infof("Creating a new Service for apicurio-studio-ui, named '%s'", ApicurioStudioResources.getUIDeploymentName(spec));
      Service uiService = ApicurioStudioResources.prepareUIService(spec);
      uiService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(uiService);

      logger.infof("Creating a new Deployment for apicurio-studio-ui, named '%s'", ApicurioStudioResources.getUIDeploymentName(spec));
      Deployment uiDeployment = ApicurioStudioResources.prepareUIDeployment(spec, cr.getStatus());
      uiDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(uiDeployment);

      cr.getStatus().setUiModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
   }

   /**
    * Handle the deletion of a Deployment by recreating it if CR is not marked for deletion.
    * Updating CR status.
    * @param deployment The deployment that has been modified.
    */
   public void handleDeletedDeployment(Deployment deployment) {
      // Retrieve owning custom resource.
      String crName = deployment.getMetadata().getOwnerReferences().get(0).getName();
      ApicurioStudio apicurioStudio = client.customResources(ApicurioStudio.class)
            .inNamespace(client.getNamespace()).withName(crName).get();

      if (apicurioStudio != null && !apicurioStudio.isMarkedForDeletion()) {
         String moduleName = deployment.getMetadata().getLabels().get("module");
         switch (moduleName) {
            case ApicurioStudioResources.APICURIO_STUDIO_API_MODULE:
            case ApicurioStudioResources.APICURIO_STUDIO_WS_MODULE:
            case ApicurioStudioResources.APICURIO_STUDIO_UI_MODULE:
               createOrUpdateApicurioStudioResources(apicurioStudio);
               break;
            case KeycloakResources.APICURIO_STUDIO_AUTH_MODULE:
               createOrUpdateKeycloakResources(apicurioStudio);
               break;
            case DatabaseResources.APICURIO_STUDIO_DB_MODULE:
               createOrUpdateDatabaseResources(apicurioStudio);
               break;
         }
      }
   }

   /**
    * Handle the modification of a Deployment and the update of CR Status.
    * @param deployment The deployment that has been modified
    */
   public void handleModifiedDeployment(Deployment deployment) {
      // Retrieve owning custom resource.
      String crName = deployment.getMetadata().getOwnerReferences().get(0).getName();
      ApicurioStudio apicurioStudio = client.customResources(ApicurioStudio.class)
            .inNamespace(client.getNamespace()).withName(crName).get();

      // Maybe CR is null if deleted...
      // Maybe status is still null as the main loop is not yet finished...
      if (apicurioStudio != null && apicurioStudio.getStatus() != null) {
         logger.infof("Handling modified Deployment for CR: " + ToStringBuilder.reflectionToString(apicurioStudio.getStatus()));

         if (!apicurioStudio.isMarkedForDeletion()) {
            ModuleStatus status = findModuleStatus(apicurioStudio.getStatus(), deployment.getMetadata().getLabels().get("module"));
            logger.info("Got a ModuleStatus ? " + status);
            if (status != null) {
               logger.infof("Looking if something has to be updated for '%s'", deployment.getMetadata().getLabels().get("module"));
               boolean updated = false;

               if (!deployment.isMarkedForDeletion()) {
                  logger.info("Status.isReady() ? " + status.isReady() + " - " + status.getState());
                  logger.info("ReadyReplicas: " + deployment.getStatus().getReadyReplicas());
                  if (!status.isReady() && deployment.getStatus().getReadyReplicas() != null
                        && deployment.getStatus().getReadyReplicas() > 0) {
                     status.setState(ApicurioStudioStatus.State.READY);
                     status.setError(false);
                     status.setMessage(deployment.getStatus().getReadyReplicas() + " ready replica(s)");
                     status.updateLastTransitionTime();
                     updated = true;
                  }
               } else {
                  status.setState(ApicurioStudioStatus.State.ERROR);
                  status.setError(true);
                  status.setMessage("Deployment has been deleted with no reason");
                  status.updateLastTransitionTime();
                  updated = true;
               }

               // Now just re-update the status if necessary.
               if (updated) {
                  client.customResources(ApicurioStudio.class)
                        .inNamespace(client.getNamespace()).withName(crName).updateStatus(apicurioStudio);
               }
            }

            // Should you update global status?
            // Refresh our local version before checking.
            apicurioStudio = client.customResources(ApicurioStudio.class).inNamespace(client.getNamespace()).withName(crName).get();
            if (apicurioStudio.getStatus().getState() != ApicurioStudioStatus.State.READY) {
               ApicurioStudioStatus st = apicurioStudio.getStatus();
               if (st.getApiModule() != null && st.getApiModule().isReady()
                     && st.getWsModule() != null && st.getWsModule().isReady()
                     && st.getUiModule() != null && st.getUiModule().isReady()
                     && st.getKeycloakModule() != null && (st.getKeycloakModule().isReady() || st.getKeycloakModule().isPreexisting())
                     && st.getDatabaseModule() != null && (st.getDatabaseModule().isReady() || st.getDatabaseModule().isPreexisting())) {
                  st.setState(ApicurioStudioStatus.State.READY);
                  st.setMessage("All module deployments are ready");
                  client.customResources(ApicurioStudio.class)
                        .inNamespace(client.getNamespace()).updateStatus(apicurioStudio);
               }
            } else {
               ApicurioStudioStatus st = apicurioStudio.getStatus();
               if (st.getApiModule() == null || !st.getApiModule().isReady()
                     || st.getWsModule() == null || !st.getWsModule().isReady()
                     || st.getUiModule() == null || !st.getUiModule().isReady()
                     || st.getKeycloakModule() == null || (!st.getKeycloakModule().isReady() && !st.getKeycloakModule().isPreexisting())
                     || st.getDatabaseModule() == null || (!st.getDatabaseModule().isReady() && !st.getDatabaseModule().isPreexisting())) {
                  st.setState(ApicurioStudioStatus.State.DEPLOYING);
                  st.setMessage("Currently reconciliating...");
                  client.customResources(ApicurioStudio.class)
                        .inNamespace(client.getNamespace()).updateStatus(apicurioStudio);
               }
            }
         }
      }
   }

   /** Build a new OwnerReference to assign to CR resources. */
   private OwnerReference getOwnerReference(ApicurioStudio cr) {
      return new OwnerReferenceBuilder()
            .withController(true)
            .withKind(cr.getKind())
            .withApiVersion(cr.getApiVersion())
            .withName(cr.getMetadata().getName())
            .withUid(cr.getMetadata().getUid())
            .build();
   }

   /** Find the ModuleStatus corresponding to deployment. */
   private ModuleStatus findModuleStatus(ApicurioStudioStatus status, String moduleName) {
      switch (moduleName) {
         case ApicurioStudioResources.APICURIO_STUDIO_API_MODULE: return status.getApiModule();
         case ApicurioStudioResources.APICURIO_STUDIO_WS_MODULE: return status.getWsModule();
         case ApicurioStudioResources.APICURIO_STUDIO_UI_MODULE: return status.getUiModule();
         case KeycloakResources.APICURIO_STUDIO_AUTH_MODULE: return status.getKeycloakModule();
         case DatabaseResources.APICURIO_STUDIO_DB_MODULE: return status.getDatabaseModule();
      }
      return null;
   }

   /** Find a Deployment using its name within a list. */
   private Deployment findDeploymentByName(List<Deployment> deployments, String name) {
      for (Deployment deployment : deployments) {
         if (name.equals(deployment.getMetadata().getName())) {
            return deployment;
         }
      }
      return null;
   }
}
