# Apicurio Studio Operator

Kubernetes Operator for easy setup and management of Apicurio Studio instances.

## Table of contents

<!--ts-->
* [Installation](#installation)
    * [Manual procedure](#manual-procedure)
    * [Via OLM add-on](#via-olm-add-on)
* [Usage](#usage)
    * [Minimalist CRD](#minimalist-crd)
    * [Complete CRD](#complete-crd)
    * [ApicurioStudio details](#apicuriostudio-details)
    * [Status tracking](#status-tracking)
* [Build](#build)
<!--te-->

## Installation

### Manual procedure

For development or on bare OpenShift and Kubernetes clusters, without Operator Lifecycle Management (OLM).

Start cloning this repos and then, optionally, create a new project:

```sh
$ git clone https://github.com/apicurio/apicurio-studio-operator.git
$ cd apicurio-studio-operator/
$ kubectl create namespace apicurio
```

Then, from this repository root directory, create the specific CRDS and resources needed for Operator:

```sh
$ kubectl create -f deploy/crd/apicuriostudios.studio.apicur.io-v1.yml
$ kubectl create -f deploy/service_account.yaml -n apicurio 
$ kubectl create -f deploy/role.yaml -n apicurio
$ kubectl create -f deploy/role_binding.yaml -n apicurio
```

Finally, deploy the operator:

```sh
$ kubectl create -f deploy/operator.yaml -n apicurio
```

Wait a minute or two and check everything is running:

```sh
$ kubectl get pods -n microcks                                  
NAME                                        READY     STATUS    RESTARTS   AGE
apicurio-studio-operator-76d47d899f-2tzzm   1/1       Running   0          1m
```

Now just create a `ApicurioStudio` CRD!

### Via OLM add-on

[Operator Lyfecycle Manager](https://github.com/operator-framework/operator-lifecycle-manager) shoud be installed on your cluster first. Please follow this [guideline](https://github.com/operator-framework/operator-lifecycle-manager/blob/master/Documentation/install/install.md) to know how to proceed.

You can then use the [OperatorHub.io](https://operatorhub.io) catalog of Kubernetes Operators sourced from multiple providers. It offers you an alternative way to install stable versions of Microcks using the Microcks Operator. To install Microcks from [OperatorHub.io](https://operatorhub.io), locate the *Apicurio Studio Operator* and follow the instructions provided.

As an alternative, raw resources can also be found into the `/deploy/olm` directory of this repo.

### Minimalist CRD

Here's below a minimalistic `ApicurioStudio` CRD that I use on my OpenShift cluster. This let all the defaults applies (see below for details).

```yaml
apiVersion: studio.apicur.io/v1alpha1
kind: ApicurioStudio
metadata:
  name: apicurio-sample
spec:
  name: apicurio-sample
```

> This form can only be used on OpenShift as vanilla Kubernetes will need more information to customize `Ingress` resources.

### Complete CRD

Here's now a complete `MicrocksInstall` CRD that I use - for example - on Minikube for testing vanilla Kubernetes support. This one adds the `url` attributes that are mandatory on vanilla Kubernetes.

```yaml
apiVersion: studio.apicur.io/v1alpha1
kind: ApicurioStudio
metadata:
  name: apicurio-sample
spec:
  name: apicurio-sample
  keycloak:
    install: true
    realm: apicurio
    volumeSize: 1Gi
  database:
    install: true
    database: apicuriodb
    driver: postgresql
    type: postgresql9
    volumeSize: 1Gi
  features:
    asyncAPI: true
    graphQL: true
    microcks:
      apiUrl: https://microcks-microcks.apps.cluster-0f5f.0f5f.sandbox1056.opentlc.com/api
      clientId: microcks-serviceaccount
      clientSecret: ab54d329-e435-41ae-a900-ec6b3fe15c54
```

### ApicurioStudio details

The table below describe all the fields of the `ApicurioStudio` CRD, providing information on what's mandatory and what's optional as well as default values.

`TO FINALIZE`

### Status tracking

Creating an instance of `ApicurioStudio` with embedded Keycloak and Database modules implies the deployment of 5 pods in addition of the operator itself.

```shell
$ kubectl get pods -n apicurio
NAME                                        READY   STATUS    RESTARTS   AGE
apicurio-sample-api-97666ccdf-vqfb8         1/1     Running   0          62m
apicurio-sample-auth-b864b59dd-fpkhx        1/1     Running   0          62m
apicurio-sample-db-6f8895cc4-tr2rh          1/1     Running   0          62m
apicurio-sample-ui-78d96ff4cf-6vtm8         1/1     Running   1          62m
apicurio-sample-ws-7d4f996679-tgfbd         1/1     Running   0          62m
apicurio-studio-operator-76d47d899f-2tzzm   1/1     Running   0          64m
```

The `ApicurioStudio` is managing its status as a sub-customresource that is updated as the deployment is going on.
Each module of the studio has its own status tracking information as shown in the example below:

```yaml
status:
  uiModule:
    error: false
    lastTransitionTime: '2021-07-02T08:09:52.71923'
    message: 1 ready replica(s)
    state: READY
  apiModule:
    error: false
    lastTransitionTime: '2021-07-02T08:08:28.552723'
    message: 1 ready replica(s)
    state: READY
  message: All module deployments are ready
  databaseModule:
    error: false
    lastTransitionTime: '2021-07-02T08:08:23.990673'
    message: 1 ready replica(s)
    state: READY
  wsModule:
    error: false
    lastTransitionTime: '2021-07-02T08:08:30.833432'
    message: 1 ready replica(s)
    state: READY
  error: false
  state: READY
  wsUrl: >-
    apicurio-sample-ws-apicurio-operator.apps.cluster-9d5e.9d5e.sandbox1893.opentlc.com
  apiUrl: >-
    apicurio-sample-api-apicurio-operator.apps.cluster-9d5e.9d5e.sandbox1893.opentlc.com
  studioUrl: >-
    apicurio-sample-ui-apicurio-operator.apps.cluster-9d5e.9d5e.sandbox1893.opentlc.com
  keycloakModule:
    error: false
    lastTransitionTime: '2021-07-02T08:09:51.319282'
    message: 1 ready replica(s)
    state: READY
  keycloakUrl: >-
    apicurio-sample-auth-apicurio-operator.apps.cluster-9d5e.9d5e.sandbox1893.opentlc.com
```

## Build

The operator is made of 2 modules:
* `api` contains the model for manipulating Custom Resources elements using Java,
* `operator` contains the Kubernetes controller implementing the remediation logic. It is implemented in [Quarkus](https://www.quarkus.io).

### Api module

Simply execute:

```sh
mvn clean install
```

### Operator module

Produce a native container image with the name elements specified within the `pom.xml`:
``
```sh
mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```