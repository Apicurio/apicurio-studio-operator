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
package io.apicurio.studio.operator.watcher;

import io.apicurio.studio.operator.ApicurioStudioController;
import io.apicurio.studio.operator.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

import org.jboss.logging.Logger;

/**
 * A watcher for deployments created by the operator.
 * @author laurent.broudoux@gmail.com
 */
public class DeploymentEventSource implements Watcher<Deployment> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final ApicurioStudioController controller;

   private final KubernetesClient client;

   public static DeploymentEventSource createAndRegisterWatch(ApicurioStudioController controller, KubernetesClient client) {
      DeploymentEventSource deploymentEventSource = new DeploymentEventSource(controller, client);
      deploymentEventSource.registerWatch();
      return deploymentEventSource;
   }

   private DeploymentEventSource(ApicurioStudioController controller, KubernetesClient client) {
      this.controller = controller;
      this.client = client;
   }

   private void registerWatch() {
      client.apps()
            .deployments().inNamespace(client.getNamespace())
            .withLabel(Constants.MANAGED_BY_LABEL, Constants.OPERATOR_ID)
            .watch(this);
   }

   @Override
   public void eventReceived(Action action, Deployment deployment) {
      logger.infof("Event for action: '%s', Deployment: '%s' (rr='%s')", action.name(),
            deployment.getMetadata().getName(), deployment.getStatus().getReadyReplicas());

      switch (action) {
         case ADDED:
            break;
         case MODIFIED:
            controller.handleModifiedDeployment(deployment);
            break;
         case DELETED:
            controller.handleDeletedDeployment(deployment);
            break;
      }
   }

   @Override
   public void onClose(WatcherException e) {
      if (e == null) {
         return;
      }
      if (e.isHttpGone()) {
         logger.warn("Received error for watch, will try to reconnect.", e);
         registerWatch();
      } else {
         // Note that this should not happen normally, since fabric8 client handles reconnect.
         // In case it tries to reconnect this method is not called.
         logger.error("Unexpected error happened with watch. Will exit.", e);
         System.exit(1);
      }
   }
}
