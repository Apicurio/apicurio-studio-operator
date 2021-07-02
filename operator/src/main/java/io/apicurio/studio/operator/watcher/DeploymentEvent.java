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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

/**
 * An event for a Deployment managed by Apicurio Studio Operator.
 * @author laurent.broudoux@gmail.com
 */
public class DeploymentEvent extends AbstractEvent {

   private final Watcher.Action action;
   private final Deployment deployment;

   /**
    * Build a new Deployment event.
    * @param action Original action
    * @param resource The target resource
    * @param deploymentEventSource The event source that has materialized event
    */
   public DeploymentEvent(
         Watcher.Action action, Deployment resource, DeploymentEventSource deploymentEventSource) {
      // TODO: this mapping is really critical and should be made more explicit
      super(resource.getMetadata().getOwnerReferences().get(0).getUid(), deploymentEventSource);
      this.action = action;
      this.deployment = resource;
   }

   /** @return Origin action of this event. */
   public Watcher.Action getAction() {
      return action;
   }
   /** @return The target of this event. */
   public Deployment getDeployment() {
      return deployment;
   }
   /** @return The Uid of the deployment resource. */
   public String resourceUid() {
      return getDeployment().getMetadata().getUid();
   }

   @Override
   public String toString() {
      return "CustomResourceEvent{"
            + "action="
            + action
            + ", resource=[ name="
            + getDeployment().getMetadata().getName()
            + ", kind="
            + getDeployment().getKind()
            + ", apiVersion="
            + getDeployment().getApiVersion()
            + " ,resourceVersion="
            + getDeployment().getMetadata().getResourceVersion()
            + ", markedForDeletion: "
            + (getDeployment().getMetadata().getDeletionTimestamp() != null
            && !getDeployment().getMetadata().getDeletionTimestamp().isEmpty())
            + " ]"
            + '}';
   }
}
