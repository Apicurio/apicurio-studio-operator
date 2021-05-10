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
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author laurent.broudoux@gmail.com
 */
public class DatabaseResources {

   /** Spec type for a Postgresql database. */
   public static final String POSTGRESQL_TYPE = "postgresql";
   /** Spec type for a Mysql database. */
   public static final String MYSQL_TYPE = "mysql";

   public static final String APICURIO_STUDIO_DB_MODULE = "apicurio-studio-db";

   /**
    *
    * @param spec
    * @return
    */
   public static String getDatabaseSecretName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db-connection";
   }

   /**
    *
    * @param spec
    * @return
    */
   public static Secret prepareDatabaseSecret(ApicurioStudioSpec spec) {
      // Building a fresh new Secret according the spec.
      SecretBuilder builder = new SecretBuilder()
            .withNewMetadata()
               .withName(getDatabaseSecretName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_DB_MODULE)
            .endMetadata()
            .addToStringData("database-user", StringUtils.defaultIfBlank(spec.getDatabase().getUser(), RandomStringUtils.randomAlphanumeric(8)))
            .addToStringData("database-password", StringUtils.defaultIfBlank(spec.getDatabase().getPassword(), RandomStringUtils.randomAlphanumeric(16)))
            .addToStringData("database-rootPassword", StringUtils.defaultIfBlank(spec.getDatabase().getRootPassword(), RandomStringUtils.randomAlphanumeric(16)));

      return builder.build();
   }

   /**
    *
    * @param spec
    * @return
    */
   public static String getDatabasePVCName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db-claim";
   }

   /**
    *
    * @param spec
    * @return
    */
   public static PersistentVolumeClaim prepareDatabasePVC(ApicurioStudioSpec spec) {
      // Building a fresh new PersistentVolumeClain according the spec.
      PersistentVolumeClaimBuilder builder = new PersistentVolumeClaimBuilder()
            .withNewMetadata()
               .withName(getDatabasePVCName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_DB_MODULE)
            .endMetadata()
            .withNewSpec()
               .withAccessModes("ReadWriteOnce")
               .withNewResources()
                  .addToRequests("storage", new Quantity(spec.getDatabase().getVolumeSize()))
               .endResources()
            .endSpec();

      return builder.build();
   }

   /**
    *
    * @param spec
    * @return
    */
   public static String getDatabaseDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db";
   }

   /**
    *
    * @param client
    * @param spec
    * @return
    */
   public static Deployment prepareDatabaseDeployment(KubernetesClient client, ApicurioStudioSpec spec) {

      Deployment deployment = null;

      switch (spec.getDatabase().getType()) {
         case MYSQL_TYPE:
            deployment = client.apps().deployments()
                  .load(DatabaseResources.class.getResourceAsStream("/operator/src/main/resources/k8s/mysql-deployment.yml")).get();
            break;
         case POSTGRESQL_TYPE:
         default:
            deployment = client.apps().deployments()
                  .load(DatabaseResources.class.getResourceAsStream("/operator/src/main/resources/k8s/postgresql-deployment.yml")).get();
            deployment = new DeploymentBuilder(deployment)
                  .editSpec()
                     .editTemplate()
                        .editSpec()
                           .editContainer(0)
                              .addToEnv(
                                    new EnvVar("POSTGRESQL_DATABASE", spec.getDatabase().getDatabase(), null),
                                    new EnvVarBuilder()
                                          .withName("POSTGRESQL_USER")
                                          .withValueFrom(
                                                new EnvVarSourceBuilder()
                                                      .withSecretKeyRef(
                                                            new SecretKeySelector("database-user", getDatabaseSecretName(spec), false)
                                                      )
                                                .build()
                                          )
                                          .build(),
                                    new EnvVarBuilder()
                                          .withName("POSTGRESQL_PASSWORD")
                                          .withValueFrom(
                                                new EnvVarSourceBuilder()
                                                      .withSecretKeyRef(
                                                            new SecretKeySelector("database-password", getDatabaseSecretName(spec), false)
                                                      )
                                                      .build()
                                          )
                                          .build()
                              )
                           .endContainer()
                           .addToVolumes(
                                 new VolumeBuilder()
                                       .withName("postgresql-apicurio")
                                       .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(getDatabasePVCName(spec), false))
                                       .build()
                           )
                        .endSpec()
                     .endTemplate()
                  .endSpec()
                  .build();
      }
      // Complete with generic labels and selectors.
      deployment = new DeploymentBuilder(deployment)
            .editMetadata()
               .withName(getDatabaseDeploymentName(spec))
               .addToLabels("app", spec.getName())
            .endMetadata()
            .editSpec()
               .editSelector().addToMatchLabels("app", spec.getName()).endSelector()
               .editTemplate().editMetadata().addToLabels("app", spec.getName()).endMetadata().endTemplate()
            .endSpec()
            .build();

      return deployment;
   }

   /**
    *
    * @param spec
    * @return
    */
   public static Service prepareDatabaseService(ApicurioStudioSpec spec) {
      // Building a fresh new Service according the spec.
      ServiceBuilder builder = new ServiceBuilder()
            .withNewMetadata()
               .withName(getDatabaseDeploymentName(spec))
               .addToLabels("app", spec.getName())
               .addToLabels("module", APICURIO_STUDIO_DB_MODULE)
            .endMetadata()
            .withNewSpec()
               .addToSelector("app", spec.getName())
               .addToSelector("module", APICURIO_STUDIO_DB_MODULE)
               .withSessionAffinity("None")
               .withType("ClusterIP")
            .endSpec();

      Service service = builder.build();

      // Complete port depending on database type.
      switch (spec.getDatabase().getType()) {
         case MYSQL_TYPE:
            service.getSpec().setPorts(
                  List.of(new ServicePortBuilder()
                        .withPort(3306)
                        .withProtocol("TCP")
                        .withTargetPort(new IntOrString(3306))
                        .build())
            );
            break;
         case POSTGRESQL_TYPE:
         default:
            service.getSpec().setPorts(
                  List.of(new ServicePortBuilder()
                        .withPort(5432)
                        .withProtocol("TCP")
                        .withTargetPort(new IntOrString(5432))
                        .build())
            );
      }
      return service;
   }
}
