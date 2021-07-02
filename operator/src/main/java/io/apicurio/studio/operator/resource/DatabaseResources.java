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
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Holds utility methods to create Database resources from specification.
 * @author laurent.broudoux@gmail.com
 */
public class DatabaseResources {

   /** Spec type for a Postgresql database. */
   public static final String POSTGRESQL_TYPE = "postgresql";
   /** Spec type for a Mysql database. */
   public static final String MYSQL_TYPE = "mysql";

   /** The name of the Database module. */
   public static final String APICURIO_STUDIO_DB_MODULE = "apicurio-studio-db";

   /**
    * Get a JDBC connection URL from studio specifications.
    * @param spec The studio custom resource.
    * @return A connection URL for database.
    */
   public static String getDatabaseConnectionUrl(ApicurioStudioSpec spec) {
      String databaseUrl = null;
      // If database is provided, we already got it's host:port address.
      if (!spec.getDatabase().isInstall() && spec.getDatabase().getUrl() != null) {
         databaseUrl = spec.getDatabase().getUrl();
      } else {
         // Else we have to recompose it from Service/Deployment name.
         databaseUrl = getDatabaseDeploymentName(spec);
         switch (spec.getDatabase().getDriver()) {
            case MYSQL_TYPE:
               databaseUrl += ":3306";
               break;
            case POSTGRESQL_TYPE:
            default:
               databaseUrl += ":5432";
         }
      }
      // Now compose a JDBC connection URL.
      return "jdbc:" + spec.getDatabase().getDriver() + "://" + databaseUrl + "/" + spec.getDatabase().getDatabase();
   }

   /**
    * Get the name of Secret holding database credentials.
    * @param spec The studio custom resource.
    * @return The secret name
    */
   public static String getDatabaseSecretName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db-connection";
   }

   /**
    * Prepare the Database credentials secret.
    * @param spec The studio custom resource.
    * @return A full Secret
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
    * Get the Database volume claim name from the spec.
    * @param spec The studio custom resource.
    * @return The claim name
    */
   public static String getDatabasePVCName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db-claim";
   }

   /**
    * Prepare the Database volume claim.
    * @param spec The studio custom resource.
    * @return A full PVC
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
    * Get the Database deployment name from spec.
    * @param spec The studio custom resource.
    * @return The deployment name
    */
   public static String getDatabaseDeploymentName(ApicurioStudioSpec spec) {
      return spec.getName() + "-db";
   }

   /**
    * Prepare a new Deployment for the Database.
    * @param client A Kubernetes API client for loading resources.
    * @param spec The studio custom resource.
    * @return The full deployment.
    */
   public static Deployment prepareDatabaseDeployment(KubernetesClient client, ApicurioStudioSpec spec) {

      Deployment deployment = null;

      switch (spec.getDatabase().getDriver()) {
         case MYSQL_TYPE:
            deployment = client.apps().deployments()
                  .load(DatabaseResources.class.getResourceAsStream("/k8s/mysql-deployment.yml")).get();
            break;
         case POSTGRESQL_TYPE:
         default:
            deployment = client.apps().deployments()
                  .load(DatabaseResources.class.getResourceAsStream("/k8s/postgresql-deployment.yml")).get();
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
                           .addNewVolume()
                              .withName("postgresql-apicurio")
                              .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(getDatabasePVCName(spec), false))
                           .endVolume()
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
    * Prepare a new Service for the Database.
    * @param spec The studio custom resource.
    * @return The full service.s
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
      switch (spec.getDatabase().getDriver()) {
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
