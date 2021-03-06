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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

/**
 * This is the main entry point for Quarkus based operator.
 * @author laurent.broudoux@gmail.com
 */
@QuarkusMain
public class OperatorApplication implements QuarkusApplication {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Inject
   Operator operator;

   @Inject
   ObjectMapper objectMapper;

   public static void main(String... args) {
      Quarkus.run(OperatorApplication.class, args);
   }

   @Override
   public int run(String... args) throws Exception {
      logger.info("Starting Apicurio Studio operator");
      operator.start();
      Quarkus.waitForExit();
      return 0;
   }
}
