# Prometheus Push Rundeck Plugin
A notification plugin to send the result of a Rundeck job execution to the Prometheus Pushgateway.  
Supported Rundeck job trigger types are: success, failure

<br />

## Build Requirements
- Gradle
- Java >= 1.8

<br />

## Installation
Copy your compiled jar into /var/lib/rundeck/libext (can also be done via the interface in newer versions via Configuration -> Plugins -> Upload Plugin).
```bash
chown rundeck:rundeck /var/lib/rundeck/libext/rundeck-prometheuspushplugin*.jar
rm -rf /var/lib/rundeck/libext/cache
service rundeckd restart
```

<br />

## Plugin Properties
| Name                  | Description                                   | Default Value                               |
| --------------------- | --------------------------------------------- | ------------------------------------------- |
| pushgateway_endpoints | Comma seperated list of Pushgateway endpoints | ${globals.prometheus_pushgateway_endpoints} |
| metric_prefix         | Metric name prefix                            | ${globals.prometheus_metric_prefix}         |

<br />

Project level configuration example (Project Settings -> Edit Configuration -> Edit Configuration File):  
```ini
project.globals.prometheus_pushgateway_endpoints=pushgateway1:9091,pushgateway2:9091
project.globals.prometheus_metric_prefix=app
```

<br />

## Prometheus Data
| Metric             | Labels                        | Example                                                                                                                                      | Description                                 | Source                                                |
| ------------------ | ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- | ----------------------------------------------------- |
| *                  | job                           | my_rundeck_job                                                                                                                               | Prometheus job ID                           | Rundeck job ID                                        |
| `PREFIX`           | name type environment project | app{job="my_rundeck_job",name="My Rundeck Job",project="RundeckProjectName",<br />type="RundeckJobGroupFirst",environment="RundeckJobGroupSecond"} | Root of `PREFIX` labels                     | Rundeck Project, Rundeck Job Group (type/environment) |
| `PREFIX`_state     |                               | app_state{job="my_rundeck_job"}                                                                                                              | Execution state (1 = healthy 0 = unhealthy) | Rundeck job execution succeeded/failed                |
| `PREFIX`_epochtime |                               |                                                                                                                                              | Execution timestamp                         | Rundeck job execution startime                        |
| `PREFIX`_duration  |                               |                                                                                                                                              | Time taken to get retrieve health state     | Rundeck job execution duration                        |
| `PREFIX`_execution | instance execution            | app_execution{execution="322335",instance="https://myrundeck:8080",job="my_rundeck_job"}                                                     | Root of execution labels                    | Rundeck instance and job execution ID                 |

<br />

### Data Example
```promql
app{job="check_application_state",name="Check Application State",project="Internal",type="Applicative",environment="Development"}
```

### PromQL Example
Get application state based on environment:
```promql
app_state and on(job) app{environment="Development"}
```

<br />

## Log
Plugin errors are written into /var/log/rundeck/service.log.  
On failure it will notify Rundeck by returning "false", but it is currently unclear how you can actually act on it...