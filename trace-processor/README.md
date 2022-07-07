# trace-processor

Ingest in database filtered trace

## trace-filter

### Configuration file

Application in charge of filtering trace based on one or more rules.
Rules are defined in a YAML configuration file with the following syntax:
```yaml
filters:
  -
    name: filter-1
    rules:
      task.event: begin
      output: toto
  -
    name: filter-2
    rules:
      task.event: begin|end
      header.type: REPORT
      kubernetes.pod_id: .*459c.*|d9e908c8-459c-410f-b538-18e3c442c877
```

The configuration file depict a list of filter to apply (in the above example we have two filter defined: "filter-1" and "filter-2"). 'filters' property name is mandatory and must be the first line of the configuration file. Each filter definition start with a line containing only a dash and is followed by a filter name and a 'rules' property name.

Rules section can be composed of one or more rule. Each rule is a key/value association:
- Keys are 'path' of trace structure where each level is separated by a dot. For example 'header.type' key path is associated to 'type' key of the 'header' section.
  - Values can be of three type:
    - fixed characters (i.e. compression-L0)
    - regular expression (i.e. compression-.*)
    - multiple value (i.e. compression-.*|generation) separated by a '|' character

To define regular expression please refer to [Java regex](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html). For special characters as of '.', '*' and so on you have to escape them with '\\' characters. For example to escape dot and use it as simple character use the following syntax: '\.'. List of special characters is the following:
- \
- .
- ?
- \*
- \+
- ^
- $
- [
- ]
- (
- )
- {
- }
- !
- :

### Behavior

The behavior of the filter is to take in the order each filter definition and find if one filter can be associated to the trace. A filter is associated to the trace if all rules can be validated. A rule is valid if the key path exists in the trace structure and if the value correspond to the one available in the trace. 
Once a filter match the trace the others are not checked. If no filter can be associated, the trace is 'sent to the trash'.


Trace filter is also able to detect file changes and to reload configuration. Changes are applied on start of trace check and not during a trace check. File changes is checked every minute. A log is displayed on the console when configuration file is reloaded (Configuration file '<file-path>' loaded)


Before filters are applied to a trace, the application will first check that the trace is valid. A trace is valid if :
- all required field are set
- format for date field correspond to the one defined
- format for uid field correspond to the one defined
- field limited in size does not exceed the quota.

### Execution

To start the application please define the following property 'filter.path'. It indicates where configuration file is located. If property is not set or path wrong, the application won't start.

Path must be defined with the following prefix 'file:' (for example 'file:/config/filter.yaml')

Also define property 'spring.kafka.bootstrap-servers' to indicate kafka URL server 

#### Troubleshooting

If the application don't start/run properly you can set property 'logging.level.eu.csgroup.coprs' to 'DEBUG' or 'TRACE' to have details on application execution.
If the issue is due to a dependency you can set property 'logging.level' to 'DEBUG' value to have dependency log.

 