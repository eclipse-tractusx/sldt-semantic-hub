# semantic-hub

![Version: 0.5.0](https://img.shields.io/badge/Version-0.5.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.6.0](https://img.shields.io/badge/AppVersion-0.6.0-informational?style=flat-square)

**Helm Chart for the Catena-X Semantic Hub Application** <br/>
This Helm charts installs the Semantic Hub application and its dependencies. 

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | keycloak | 13.3.0 |

## Prerequisites
- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure

## Install
```
kubectl create namespace semantics
helm install hub -n semantics ./charts/semantic-hub
```

## Values

| Key | Type | Default                                                                                                                                                                     | Description |
|-----|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|
| enableKeycloak | bool | `true`                                                                                                                                                                      |  |
| graphdb.args[0] | string | `"--tdb2"`                                                                                                                                                                  |  |
| graphdb.args[1] | string | `"--update"`                                                                                                                                                                |  |
| graphdb.args[2] | string | `"--loc"`                                                                                                                                                                   |  |
| graphdb.args[3] | string | `"databases/"`                                                                                                                                                              |  |
| graphdb.args[4] | string | `"/ds"`                                                                                                                                                                     |  |
| graphdb.containerPort | int | `3030`                                                                                                                                                                      |  |
| graphdb.enabled | bool | `false`                                                                                                                                                                     |  |
| graphdb.image | string | `"ghcr.io/catenax-ev/jena-fuseki:4.7.0"`                                                                                                                                    |  |
| graphdb.imagePullPolicy | string | `"IfNotPresent"`                                                                                                                                                            |  |
| graphdb.javaOptions | string | `"-Xmx1048m -Xms1048m"`                                                                                                                                                     |  |
| graphdb.password | string | `"admin"`                                                                                                                                                                   |  |
| graphdb.pvcAccessModes[0] | string | `"ReadWriteOnce"`                                                                                                                                                           |  |
| graphdb.queryEndpoint | string | `"query"`                                                                                                                                                                   |  |
| graphdb.replicaCount | int | `1`                                                                                                                                                                         |  |
| graphdb.resources.limits.memory | string | `"1024Mi"`                                                                                                                                                                  |  |
| graphdb.resources.requests.memory | string | `"512Mi"`                                                                                                                                                                   |  |
| graphdb.service.port | int | `3030`                                                                                                                                                                      |  |
| graphdb.storageClassName | string | `"default"`                                                                                                                                                                 |  |
| graphdb.storageSize | string | `"50Gi"`                                                                                                                                                                    |  |
| graphdb.updateEndpoint | string | `"update"`                                                                                                                                                                  |  |
| graphdb.username | string | `"admin"`                                                                                                                                                                   |  |
| hub.authentication | bool | `false`                                                                                                                                                                     |  |
| hub.containerPort | int | `4242`                                                                                                                                                                      |  |
| hub.embeddedTripleStore | bool | `false`                                                                                                                                                                     |  |
| hub.graphdbBaseUrl | string | `"http://graphdb:3030"`                                                                                                                                                     |  |
| hub.host | string | `"minikube"`                                                                                                                                                                |  |
| hub.idpClientId | string | `"default-client"`                                                                                                                                                          |  |
| hub.idpIssuerUri | string | `""`                                                                                                                                                                        |  |
| hub.image.registry | string | `"docker.io"`                                                                                                                                                               |  |
| hub.image.repository | string | `"tractusx/sldt-semantic-hub"`                                                                                                                                              |  |
| hub.image.version | string | `""`                                                                                                                                                                        |  |
| hub.imagePullPolicy | string | `"IfNotPresent"`                                                                                                                                                            |  |
| hub.ingress.annotations | list | `[]`                                                                                                                                                                        |  |
| hub.ingress.className | string | `""`                                                                                                                                                                        |  |
| hub.ingress.enabled | bool | `false`                                                                                                                                                                     |  |
| hub.ingress.tls | bool | `true`                                                                                                                                                                      |  |
| hub.ingress.tlsSecretName | string | `""`                                                                                                                                                                        |  |
| hub.ingress.urlPrefix | string | `"/semantics/hub"`                                                                                                                                                          |  |
| hub.replicaCount | int | `1`                                                                                                                                                                         |  |
| hub.resources.limits.memory | string | `"1024Mi"`                                                                                                                                                                  |  |
| hub.resources.requests.memory | string | `"512Mi"`                                                                                                                                                                   |  |
| hub.service.port | int | `8080`                                                                                                                                                                      |  |
| hub.service.type | string | `"ClusterIP"`                                                                                                                                                               |  |
| keycloak.args[0] | string | `"kc.sh import --file /opt/keycloak/data/import/default-realm-import.json; kc.sh start-dev --hostname=registry-keycloak --hostname-strict=false --proxy=edge --proxy=edge"` |  |
| keycloak.auth.adminPassword | string | `"admin"`                                                                                                                                                                   |  |
| keycloak.auth.adminUser | string | `"admin"`                                                                                                                                                                   |  |
| keycloak.command[0] | string | `"/bin/sh"`                                                                                                                                                                 |  |
| keycloak.command[1] | string | `"-c"`                                                                                                                                                                      |  |
| keycloak.extraVolumeMounts[0].mountPath | string | `"/opt/keycloak/data/import/default-realm-import.json"`                                                                                                                     |  |
| keycloak.extraVolumeMounts[0].name | string | `"init-script-vol"`                                                                                                                                                         |  |
| keycloak.extraVolumeMounts[0].subPath | string | `"default-realm-import.json"`                                                                                                                                               |  |
| keycloak.extraVolumes[0].configMap.name | string | `"init-script-vol"`                                                                                                                                                         |  |
| keycloak.extraVolumes[0].name | string | `"init-script-vol"`                                                                                                                                                         |  |
| keycloak.fullnameOverride | string | `"hub-keycloak"`                                                                                                                                                            |  |
| keycloak.postgresql.enabled | bool | `true`                                                                                                                                                                      |  |
| keycloak.service.type | string | `"ClusterIP"`                                                                                                                                                               |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
