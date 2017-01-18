# Protobuf filter plugin for Embulk

An Embulk filter plugin for interconversion between Protocol Buffer message and JSON.

## Overview

* **Plugin type**: filter

## Configuration

* **serialize**, **deserialize**
  - whether to serialize or deserialize (boolean, default: `false`)
  - when `serialize: true`, convert JSON to encoded protobuf message
  - when `deserialize: true`, convert encoded protobuf message to JSON
  - either one must be `true` and exception is thrown when both are `true` or both are `false`
* **encoding**: encoding type, currently only Base64 is supported (string, required)
* **protobuf_jar_path**: jar path generated from your .proto
* **columns**: Input columns (array of hash, required)
  - **name**: name of the column (string, required)
  - **message**: package namespace of the message in `protobuf_jar_path`(string, required)

## Preparation

### Install protoc

See [official installation guide](https://github.com/google/protobuf/blob/master/src/README.md).

Since this plugin depends on [protobuf-3.1.0](https://github.com/google/protobuf/releases/tag/v3.1.0),  
it is recommended that you install the same version.

### Get protobuf-java-x.x.x.jar

You can get protobuf-java-x.x.x.jar [here](https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java).

For the same reason above, getting [protobuf-java-3.1.0](https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java/3.1.0) is recommended.

### Generate jar from your .proto

Here take `addressbook.proto` as an example, which is used in [java tutorial of Protocol Buffer](https://developers.google.com/protocol-buffers/docs/javatutorial#defining-your-protocol-format).

Commands like below will generate `AddressBookProtos.jar`.  
You can pass this `AddressBookProtos.jar` to `protobuf_jar_path` option in the config.

```sh
$ protoc --java_out=./ addressbook.proto
$ javac -classpath protobuf-java-3.1.0.jar com/example/tutorial/AddressBookProtos.java
$ jar cvf AddressBookProtos.jar com/example/tutorial/AddressBookProtos*.class
```

You should already have your `.proto` file,  
so generate `.jar` file from your `.proto` and pass it to `protobuf_jar_path` option.


## Example

Run commands below.

```sh
$ cd example
$ ./generate_jar_from_proto
```

This will generate `example/AddressBookProtosProto2Syntax.jar` and `example/AddressBookProtosProto3Syntax.jar`

### Serialization Example (JSON -> encoded protobuf message)

See [example_serialize.yml](./example/example_serialize.yml) and [example_serialize.json](./example/example_serialize.json) for details.

input:

```json
// John as JSON generated from proto2-syntax .proto
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000","type":"MOBILE"},
{"number":"555-4321","type":"HOME"}]}
// John as JSON generated from proto3-syntax .proto
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000"},{"number":"555-4321","type":"HOME"}]}
// Jane as JSON generated from proto2-syntax .proto
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888","type":"MOBILE"}]}
// Jane as JSON generated from proto3-syntax .proto
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888"}]}
```

When you pass `example/AddressBookProtosProto2Syntax.jar` to `protobuf_jar_path`:

```sh
$ embulk run example/example_serialize.yml
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIMCggxMTEtMDAwMBAAIgwKCDU1NS00MzIxEAE=
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB
CghKYW5lIERvZRDTCSIMCgg5OTktODg4OBAA
CghKYW5lIERvZRDTCSIKCgg5OTktODg4OA==
```

When you pass `example/AddressBookProtosProto3Syntax.jar` to `protobuf_jar_path`:

```sh
$ embulk run example/example_serialize.yml
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB
CghKYW5lIERvZRDTCSIKCgg5OTktODg4OA==
CghKYW5lIERvZRDTCSIKCgg5OTktODg4OA==
```

Using `.jar` generated from proto3-syntax `.proto`,  
json with or without enum default values are converted to the same encoded string  
because default values is not presented in message in proto3.

See [Default Values in proto3 language guide](https://developers.google.com/protocol-buffers/docs/proto3#default).

### Deserialization Example (encoded protobuf message -> JSON)

See [example_deserialize.yml](./example/example_deserialize.yml) and [example_deserialize.csv](./example/example_deserialize.csv) for more details.

input:

```
// John as encoded generated from proto2-syntax .proto
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIMCggxMTEtMDAwMBAAIgwKCDU1NS00MzIxEAE=
// John as encoded generated from proto3-syntax .proto
CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB
// Jane as encoded generated from proto3-syntax .proto
CghKYW5lIERvZRDTCSIMCgg5OTktODg4OBAA
// Jane as encoded generated from proto3-syntax .proto
CghKYW5lIERvZRDTCSIKCgg5OTktODg4OA==
```

When you pass `example/AddressBookProtosProto2Syntax.jar` to `protobuf_jar_path`:

```sh
$ embulk run example/example_deserialize.yml
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000","type":"MOBILE"},{"number":"555-4321","type":"HOME"}]}
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000"},{"number":"555-4321","type":"HOME"}]}
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888","type":"MOBILE"}]}
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888"}]}
```

When you pass `example/AddressBookProtosProto3Syntax.jar` to `protobuf_jar_path`:

```sh
$ embulk run example/example_deserialize.yml
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000"},{"number":"555-4321","type":"HOME"}]}
{"name":"John Doe","id":1234,"email":"jdoe@example.com","phone":[{"number":"111-0000"},{"number":"555-4321","type":"HOME"}]}
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888"}]}
{"name":"Jane Doe","id":1235,"phone":[{"number":"999-8888"}]}
```

Here again, encoded messages with or without default values are converted to the same JSON.  
This is because of the same reason mentioned above.

## TODO

* Support other encoding method (Base16, Base32, ...)
* Allow JSON type for input in serialization
  and output in deserialiazation

## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
