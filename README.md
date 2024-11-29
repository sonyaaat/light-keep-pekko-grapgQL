# lite-keep-be

Cassandra provisioning
```cassandraql
CREATE KEYSPACE IF NOT EXISTS pekko_projection WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

CREATE TABLE IF NOT EXISTS pekko_projection.offset_store (
                                                            projection_name text,
                                                            partition int,
                                                            projection_key text,
                                                            offset text,
                                                            manifest text,
                                                            last_updated timestamp,
                                                            PRIMARY KEY ((projection_name, partition), projection_key));

CREATE TABLE IF NOT EXISTS pekko_projection.projection_management (
                                                                     projection_name text,
                                                                     partition int,
                                                                     projection_key text,
                                                                     paused boolean,
                                                                     last_updated timestamp,
                                                                     PRIMARY KEY ((projection_name, partition), projection_key));
```

# REST API sample requests

## Create a new TextCard
```shell
http POST localhost:8081/text-cards
```

## Update TextCard title
```shell
http PUT localhost:8081/text-cards/<id>/title title=foo
```


## Frontend setup
#### Go to terminal and start sbt shell by the following command:
```shell
sbt
```
#### Select frontend project
```shell
project frontend
```
#### Run the following command to set up build.sbt for frontend
```shell
~fastOptJS
```
#### Open index.html file in any browser from resources folder (full path: ./frontend/src/main/resources/index.html)

### CLI GraphQL samples
```shell
gq http://localhost:8088/api/graphql -q 'query { mof(id: "bbf77364-b9e5-4418-93d3-85d017e60089") }'
```
 
