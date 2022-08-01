# trace-processor

Ingest in database filtered trace

## trace-filter

### Configuration file

Application in charge of filtering trace is based on one or more rules.
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

The behavior of the filter is to take in the order each filter definition and find if one filter can be associated to the trace. A filter is associated to the trace if all rules can be validated. A rule is valid if the key path exists in the trace structure and if the value correspond to the one available in the trace (if the value defined by the path is null it's considered that the rule does not match). 
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

## trace-ingestor

### Configuration file

Application in charge of trace ingestion is based on one or more ingestion strategy. 
Ingestion strategy are defined in a YAML configuration file with the following syntax:
```yaml
ingestions:
  -
    name: ingestion-1
    mappings:
      task.input[filename_string]: dsib.filename
      header.mission: dsib.mission
    dependencies:
      dsib.filename
      
  -
    name: ingestion-2
    mappings:
      task.input[filename_string]: chunk.filename
      header.mission: chunk.mission
    dependencies:
      chunk.filename
```

When creating an ingestion, 'name' field must be set and have to match with a name of a filter defined for trace-filter application (cf. [Trace filter configuration](#trace-filter)).  

Each ingestion strategy is composed of two parts:
- mappings: Define one or more mapping between a trace and a table plus corresponding column in database.
- dependencies: Define one or more column to use to identify if an entry is already stored in database

A mapping is composed of two parts (for example `task.input[filename_string]: dsib.filename`) and separated by ':':
- one defining the path of the value to get in a trace (left part)
- one defining the table and associated column where to set value (right part)

A dependency is composed of a single part which represents the table and column on which to do find operation in database.

In case of trace a path is a list of field name which are separated by a dot value which define in trace structure where to access desired value.
For accessing field in custom field (as of output for end task) you must set field name between bracket(for example `task.output[key1]`). 
If the custom field is a complex structure and value to access to is deep, simply chain field name between bracket and don't use dot to separate them (for example 'task.output[struct1][key1]'). To identify custom field please refer to ICD trace document.

### Behavior

The behavior of the *ingestor* is to find the *ingestion strategy* name matching the filter name which filtered trace. If one match the *ingestion strategy* is applied to split trace information into different table in database.
If no ingestion strategy is found an error is raised.

When applying an *ingestion strategy* the application will first check with *dependencies* rules if some information where not already stored. If so trace information will be merged with stored one according to *mappings rules* set.
If an information defined by a *mapping* rule already exists in database, the value is systematically replaced by the new one. 

In case of custom field (as of 'aux_data.custom') the existing value is not erased but a merge attempt is done. Depending on existing value type and new value type, merge result is not the same:
- object to object merge: all fields of the new object will be added to the existing one (to know how field array/object are merged please refer to the next point)
- object to simple value merge: object is replaced by simple value
- object to array merge: object is replace by array
- array to array merge: all non-existing values of the new array will be added to the existing one (avoid duplicate value)
- array to object merge: object is added to array if it doesn't already exist
- array to simple value merge: new value is added to array if it doesn't already exist 
- simple value to simple value merge: old value is replaced by new one.

Note: For root level it's considered to be systematically an object type. So an attempt to replace it by an array or a simple value will raise an error. If you want to put a single/array value in custom field just define a key name where to put a value (for example 'header.mission: aux_data.custom[mission]')


### Execution

To start the application please define the following property 'ingestion.path'. It indicates where configuration file is located. If property is not set or path wrong, the application won't start.

Path must be defined with the following prefix 'file:' (for example `file:/config/ingestion.yaml`)

You also have to define properties related to database access:
- spring.datasource.username
- spring.datasource.password
- spring.datasource.url

spring datasource url must be of the form `jdbc:postgresql://<ip>:<port>/<database name>`

Also define property 'spring.kafka.bootstrap-servers' to indicate kafka URL server 