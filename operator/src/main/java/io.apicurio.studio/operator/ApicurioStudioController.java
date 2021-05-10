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
package io.apicurio.studio.operator;

import javax.inject.Inject;

import io.apicurio.studio.operator.api.ModuleStatus;
import io.apicurio.studio.operator.resource.DatabaseResources;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
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
 * @author laurent.broudoux@gmail.com
 */
@Controller(namespaces = Controller.WATCH_CURRENT_NAMESPACE)
public class ApicurioStudioController implements ResourceController<ApicurioStudio> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Inject
   KubernetesClient client;

   @Override
   public void init(EventSourceManager eventSourceManager) {

   }

   @Override
   public UpdateControl<ApicurioStudio> createOrUpdateResource(ApicurioStudio apicurioStudio,
                                                               Context<ApicurioStudio> context) {

      final ApicurioStudioSpec spec = apicurioStudio.getSpec();
      logger.infof("Starting CreateOrUpdate reconcile operation for '%s'", spec.getName());
      logger.infof("Context events: " + context.getEvents().getList());

      Optional<CustomResourceEvent> latestCREvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
      if (latestCREvent.isPresent()) {
         if (apicurioStudio.getStatus() == null) {
            apicurioStudio.setStatus(new ApicurioStudioStatus());
         }
         createOrUpdateKeycloakResources(apicurioStudio);
         createOrUpdateDatabaseResources(apicurioStudio);
         createOrUpdateApicurioStudioResources(apicurioStudio);
         apicurioStudio.getStatus().setState(ApicurioStudioStatus.State.DEPLOYING);
         return UpdateControl.updateStatusSubResource(apicurioStudio);
      }

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
    *
    * @param cr
    */
   public void createOrUpdateKeycloakResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      if (spec.getKeycloak().isInstall()) {
         cr.getStatus().setKeycloakModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
      } else {
         cr.getStatus().setKeycloakModule(new ModuleStatus(ApicurioStudioStatus.State.PREEXISTING));
      }
   }

   /**
    *
    * @param cr
    */
   public void createOrUpdateDatabaseResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      if (spec.getDatabase().isInstall()) {
         logger.debugf("Creating a new Secret for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseSecretName(spec));
         Secret dbSecret = DatabaseResources.prepareDatabaseSecret(spec);
         dbSecret.getMetadata().setOwnerReferences(refs);
         client.secrets().inNamespace(ns).createOrReplace(dbSecret);

         logger.debugf("Creating a new PersistentVolumeClaim for apicurio-studio-db, named '%s'", DatabaseResources.getDatabasePVCName(spec));
         PersistentVolumeClaim dbPVC = DatabaseResources.prepareDatabasePVC(spec);
         dbPVC.getMetadata().setOwnerReferences(refs);
         client.persistentVolumeClaims().inNamespace(ns).createOrReplace(dbPVC);

         logger.debugf("Creating a new Service for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseDeploymentName(spec));
         Service dbService = DatabaseResources.prepareDatabaseService(spec);
         dbService.getMetadata().setOwnerReferences(refs);
         client.services().inNamespace(ns).createOrReplace(dbService);

         logger.debugf("Creating a new Deployment for apicurio-studio-db, named '%s'", DatabaseResources.getDatabaseDeploymentName(spec));
         Deployment dbDeployment = DatabaseResources.prepareDatabaseDeployment(client, spec);
         dbDeployment.getMetadata().setOwnerReferences(refs);
         client.apps().deployments().inNamespace(ns).createOrReplace(dbDeployment);

         cr.getStatus().setDatabaseModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
      } else {
         cr.getStatus().setDatabaseModule(new ModuleStatus(ApicurioStudioStatus.State.PREEXISTING));
      }
   }

   /**
    *
    * @param cr
    */
   public void createOrUpdateApicurioStudioResources(ApicurioStudio cr) {
      final ApicurioStudioSpec spec = cr.getSpec();
      final String ns = cr.getMetadata().getNamespace();
      final List<OwnerReference> refs = List.of(getOwnerReference(cr));

      // Dealing with resources of Api module.
      logger.debugf("Creating a new Service for apicurio-studio-api, named '%s'", ApicurioStudioResources.getAPIDeploymentName(spec));
      Service apiService = ApicurioStudioResources.prepareAPIService(spec);
      apiService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(apiService);

      logger.debugf("Creating a new Deployment for apicurio-studio-api, named '%s'", ApicurioStudioResources.getAPIDeploymentName(spec));
      Deployment apiDeployment = ApicurioStudioResources.prepareAPIDeployment(spec);
      apiDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(apiDeployment);

      cr.getStatus().setApiModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));

      // Dealing with resources of Ws module.
      logger.debugf("Creating a new Service for apicurio-studio-ws, named '%s'", ApicurioStudioResources.getWSDeploymentName(spec));
      Service wsService = ApicurioStudioResources.prepareWSService(spec);
      wsService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(wsService);

      logger.debugf("Creating a new Deployment for apicurio-studio-ws, named '%s'", ApicurioStudioResources.getWSDeploymentName(spec));
      Deployment wsDeployment = ApicurioStudioResources.prepareWSDeployment(spec);
      wsDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(wsDeployment);

      cr.getStatus().setWsModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));

      // Dealing with resources of UI module.
      logger.debugf("Creating a new Service for apicurio-studio-ui, named '%s'", ApicurioStudioResources.getUIDeploymentName(spec));
      Service uiService = ApicurioStudioResources.prepareUIService(spec);
      uiService.getMetadata().setOwnerReferences(refs);
      client.services().inNamespace(ns).createOrReplace(uiService);

      logger.debugf("Creating a new Deployment for apicurio-studio-ui, named '%s'", ApicurioStudioResources.getUIDeploymentName(spec));
      Deployment uiDeployment = ApicurioStudioResources.prepareUIDeployment(spec);
      uiDeployment.getMetadata().setOwnerReferences(refs);
      client.apps().deployments().inNamespace(ns).createOrReplace(uiDeployment);

      cr.getStatus().setUiModule(new ModuleStatus(ApicurioStudioStatus.State.DEPLOYING));
   }

   /** Build a new OwnerReference to assign to CR resources. */
   private OwnerReference getOwnerReference(ApicurioStudio cr) {
      return new OwnerReferenceBuilder()
            .withController(true)
            .withKind(cr.getKind())
            .withApiVersion(cr.getApiVersion())
            .withName(cr.getMetadata().getName())
            .withNewUid(cr.getMetadata().getUid())
            .build();
   }

   /** */
   private Deployment findDeploymentByName(List<Deployment> deployments, String name) {
      for (Deployment deployment : deployments) {
         if (name.equals(deployment.getMetadata().getName())) {
            return deployment;
         }
      }
      return null;
   }
}
