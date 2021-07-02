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

/**
 * Some constants identifying operator.
 * @author laurent.broudoux@gmail.com
 */
public class Constants {

   /** The 'managed-by' label. */
   public static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
   /** The Operator identifier. */
   public static final String OPERATOR_ID = "apicurio-studio-operator";
}
