Reads Jaeger traces from a cassandra database, analyzes them in order to find all business transactions and attaches the found 
business transaction as a tag to the traces. 
The tag is called `business_transaction` and can be found in the `tags` field within the trace.

# Running the application

Available arguments to pass to the application:
```
usage: jaegerBusinessTransactionDetector
 -b,--start <arg>      The start time
 -e,--end <arg>        The end time
 -h,--host <arg>       The database host
 -k,--keyspace <arg>   The database keyspace name
 -s,--service <arg>    The service name to validate
```

## Gradle 
Example of how to run the application with gradle:

`./gradlew run -PappArgs="['-hlocalhost', '-kjaeger_v1_test', '-sAppFin']"`

## Jar

To build the fat jar run the following gradle command:

`./gradlew fatJar `

The fat jar can now be found in the `build/libs` directory.
Run the fat jar application with:

`java -jar rocks.inspectit.jaeger.bt-all-1.0.jar -h localhost -k jaeger_v1_test -s AppFin`