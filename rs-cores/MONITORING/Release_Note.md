# RS Addon : MONITORING (trace-processor)

## Prerequisites

- Global requirements are met (See [Global readme file](../../README.md)).
- Credentials are available for the PostgreSQL server
- PostgreSQL database name `monitoring` is created

## Deployment

### Principle

This RS-Core may be deployed either using the automated process, or manually.  
See [General installation](../../README.md#Installation).

### Additional resources

The [Additional resources](Executables/additional_resources) will create:

- A Configmap for `trace-filter` application to configure which trace to keep for monitoring
- A Configmap for `trace-ingestor` application to configure how to store and distribute trace information in database table 
- Secret to access to PostgreSQL database

### Requirements

Here are the basic requirements for the main components:

| Resource          | trace-filter | trace-ingestor |
|-------------------|:------------:|:--------------:|
| CPU               |    1000m     |     1000m      |
| Memory            |     1Gi      |      1Gi       |
| Disk size (local) |      -       |       -        |

## Configuration

See [Stream parameters](Executables/stream-parameters.properties) for a full list with defaults.

### Deployer settings

_Prefix_: deployer.&lt;APP&gt;.kubernetes
_Apps_: trace-filter, trace-ingestor

| Property                   | Description                            |   Default (trace-filter)   |                           Default (trace-ingestor)                            |
|----------------------------|----------------------------------------|:--------------------------:|:-----------------------------------------------------------------------------:|
| imagePullPolicy            | k8s image pull policy                  |           Always           |                                    Always                                     |
| namespace                  | k8s namespace to deploy to             |         monitoring         |                                  monitoring                                   |
| livenessProbeDelay         | Probe delay for liveness (seconds)     |             10             |                                      10                                       |
| livenessProbePath          | Probe path for liveness                | /actuator/health/liveness  |                           /actuator/health/liveness                           |
| livenessProbePeriod        | Probe interval for liveness (seconds)  |            120             |                                      120                                      |
| livenessProbePort          | Port for liveness probe                |            8080            |                                     8080                                      |
| livenessProbeTimeout       | Timeout for liveness (seconds)         |             20             |                                      20                                       |
| maxTerminatedErrorRestarts | Max number of restarts on error        |             3              |                                       3                                       |
| readinessProbeDelay        | Probe delay for readiness (seconds)    |             10             |                                      10                                       |
| readinessProbePath         | Probe path for readiness               | /actuator/health/readiness |                          /actuator/health/readiness                           |
| readinessProbePeriod       | Probe interval for readiness (seconds) |            120             |                                      120                                      |
| readinessProbePort         | Port for readiness probe               |            8080            |                                     8080                                      |
| readinessProbeTimeout      | Timeout for readiness (seconds)        |             20             |                                      20                                       |
| requests.memory            | Memory requets                         |           512Mi            |                                     512Mi                                     |
| requests.cpu               | CPU request                            |            500m            |                                     500m                                      |
| limits.memory              | Memory limit                           |           1000Mi           |                                    1000Mi                                     |
| limits.cpu                 | CPU limit                              |           1000m            |                                     1000m                                     |
| secretRefs                 | Name of the secret to bind             |             -              |                                trace-ingestor                                 |
| volumeMounts               | Volume mounts                          |             -              |              [ {name: ingestor-config, mountPath: '/config' } ]               |
| volumes                    | Volumes                                |             -              | [ {name: ingestor-config, configmap: </br>{ name: trace-ingestor-config } } ] |

### Trace filter

_Prefix_: app.trace-filter

| Property                                               | Description                                      |         Default          |
|--------------------------------------------------------|--------------------------------------------------|:------------------------:|
| filter.path                                            | File path where filter rules are stored          | file:/config/filter.yaml | 
| spring.cloud.stream.bindings.input.group               | Kafka consumer group                             |       trace-filter       | 
| spring.cloud.stream.kafka.binder.minPartitionCount     | Minimum number of partitions configured on topic |            5             |

### Trace ingestor

_Prefix_: app.trace-ingestor

| Property                                                            | Description                                                                                                                                                |                                      Default                                       |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------:|
| ingestion.path                                                      | File path where ingestion configuration are stored                                                                                                         |                              file:/config/filter.yaml                              | 
| spring.datasource.url                                               | JDBC URL of the database (include database name)                                                                                                           | jdbc:postgresql://postgresql-primary-hl.database.svc.cluster.local:5432/monitoring |
| spring.datasource.username                                          | Login username of the database                                                                                                                             |                                      postgres                                      |
| spring.cloud.stream.bindings.input.consumer.maxAttempts             | The number of attempts of re-processing an inbound message                                                                                                 |                                     2147483647                                     |
| spring.cloud.stream.bindings.input.consumer.backOffMaxInterval      | The maximum backoff interval (expressed in millisecond) first attempt will be done 1s after, the following 2s and then 4s until reaching the defined value |                                       60000                                        |
| spring.cloud.stream.kafka.bindings.input.consumer.autoCommitOnError | If set to false force application to replay message until it succeed.                                                                                      |                                       false                                        | 
| spring.cloud.stream.kafka.binder.minPartitionCount                  | Minimum number of partitions configured on topic                                                                                                           |                                         1                                          |