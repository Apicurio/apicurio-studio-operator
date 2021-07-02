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
 */package io.apicurio.studio.operator.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the builder/fluent interface generated.
 * @author laurent.broudoux@gmail.com
 */
public class ApicurioStudioTest {

   @Test
   public void testBuilder() {
      ApicurioStudio studioCR = new ApicurioStudioBuilder()
            .withNewMetadata()
               .withName("apicurio-sample")
            .endMetadata()
            .withNewSpec()
               .withName("apicurio-sample")
               .withNewDatabase()
                  .withDatabase("apicuriodb")
                  .withDriver("mysql")
                  .withType("mysql")
               .endDatabase()
               .withNewFeatures()
                  .withAsyncAPI(true)
                  .withGraphQL(true)
                  .withNewMicrocks()
                     .withApiUrl("https://microcks-microcks.apps.cluster-0f5f.0f5f.sandbox1056.opentlc.com/api")
                     .withClientId("microcks-serviceaccount")
                     .withClientSecret("ab54d329-e435-41ae-a900-ec6b3fe15c54")
                  .endMicrocks()
               .endFeatures()
            .endSpec()
         .build();

      assertEquals("apicurio-sample", studioCR.getMetadata().getName());
      assertEquals("apicurio-sample", studioCR.getSpec().getName());
      assertEquals("apicuriodb", studioCR.getSpec().getDatabase().getDatabase());
      assertEquals("mysql", studioCR.getSpec().getDatabase().getDriver());
      assertTrue(studioCR.getSpec().getFeatures().isAsyncAPI());
      assertTrue(studioCR.getSpec().getFeatures().isGraphQL());
      assertEquals("microcks-serviceaccount", studioCR.getSpec().getFeatures().getMicrocks().getClientId());
   }
}
