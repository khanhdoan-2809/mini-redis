# MiniRedis

MiniRedis is a Redis-compatible server built from scratch in Java.

The purpose of this project is to study:

- TCP networking
- RESP
- event-driven architecture
- Redis data structures
- expiration
- persistence
- memory eviction
- transactions
- Pub/Sub
- replication
- clustering
- distributed-system correctness
- benchmarking and profiling

## Technology

- Java 21
- Maven
- Java NIO
- JUnit 5
- AssertJ
- SLF4J
- Logback
- Docker
- GitHub Actions

## Principles

MiniRedis intentionally avoids frameworks that hide the systems concepts
being studied.

Therefore the core server does not use:

- Spring Boot
- Spring WebFlux
- Netty
- Spring Data Redis
- Hibernate
- external databases

## Build

```bash
./mvnw clean verify
```

# RESP — Redis Serialization Protocol

RESP is the protocol Redis clients and Redis servers use to communicate.

A Redis client does not send Java objects such as:

```java
new GetCommand("name");
```

It sends **bytes** using RESP format.

For example:

```bash
GET name
```

is sent roughly as:

```text
*2\r\n
$3\r\n
GET\r\n
$4\r\n
name\r\n
```

The server reads these bytes and converts them into Java objects.

---

## Basic Flow

```text
Redis Client

GET name
   ↓
RESP encoding
   ↓
*2\r\n$3\r\nGET\r\n$4\r\nname\r\n
   ↓
TCP
   ↓
RESP decoder
   ↓
Java objects
```

RESP is therefore the format used to organize bytes sent through TCP.

---

# RESP2 Types

RESP2 has five main data types:

| RESP Type     | Prefix | Example            | Java Type          |
| ------------- | -----: | ------------------ | ------------------ |
| Simple String |    `+` | `+OK\r\n`          | `RespSimpleString` |
| Error         |    `-` | `-ERR message\r\n` | `RespError`        |
| Integer       |    `:` | `:100\r\n`         | `RespInteger`      |
| Bulk String   |    `$` | `$5\r\nhello\r\n`  | `RespBulkString`   |
| Array         |    `*` | `*2\r\n...`        | `RespArray`        |

RESP also supports null values.

---

# 1. Simple String

Syntax:

```text
+<value>\r\n
```

Example:

```text
+OK\r\n
```

Meaning:

```text
Simple String
value = "OK"
```

Java representation:

```java
new RespSimpleString("OK");
```

Another example:

```text
+PONG\r\n
```

Java:

```java
new RespSimpleString("PONG");
```

Simple Strings are normally used for short responses.

---

# 2. Error

Syntax:

```text
-<error-message>\r\n
```

Example:

```text
-ERR unknown command\r\n
```

Meaning:

```text
Error
message = "ERR unknown command"
```

Java representation:

```java
new RespError("ERR unknown command");
```

Example:

```text
-WRONGTYPE Operation against a key holding the wrong kind of value\r\n
```

Java:

```java
new RespError(
    "WRONGTYPE Operation against a key holding the wrong kind of value"
);
```

---

# 3. Integer

Syntax:

```text
:<number>\r\n
```

Example:

```text
:100\r\n
```

Meaning:

```text
Integer
value = 100
```

Java representation:

```java
new RespInteger(100);
```

Negative values are also valid:

```text
:-10\r\n
```

Java:

```java
new RespInteger(-10);
```

Redis commands such as `INCR`, `DEL`, and `EXISTS` can return integers.

---

# 4. Bulk String

Syntax:

```text
$<length>\r\n
<value>\r\n
```

Example:

```text
$5\r\n
hello\r\n
```

Meaning:

```text
Bulk String

length = 5
value  = hello
```

Java representation:

```java
new RespBulkString(
    "hello".getBytes(StandardCharsets.UTF_8)
);
```

The important part is:

```text
$5
```

It means:

> Read exactly the next 5 bytes as the value.

For example:

```text
$3\r\n
GET\r\n
```

means:

```text
length = 3
value = GET
```

Java:

```java
new RespBulkString(
    "GET".getBytes(StandardCharsets.UTF_8)
);
```

Bulk Strings use `byte[]` instead of `String` because Redis data is binary-safe.

That means Redis can store:

```text
text
images
serialized objects
protobuf
arbitrary bytes
```

---

# 5. Null Bulk String

Syntax:

```text
$-1\r\n
```

Meaning:

```text
null value
```

Java representation:

```java
RespNullBulkString.INSTANCE
```

For example, later:

```bash
GET missing-key
```

may return:

```text
$-1\r\n
```

---

# 6. Array

Syntax:

```text
*<number-of-elements>\r\n
<element-1>
<element-2>
...
```

Example:

```text
*2\r\n
$3\r\n
GET\r\n
$4\r\n
name\r\n
```

Meaning:

```text
Array with 2 elements

element 1 = "GET"
element 2 = "name"
```

Java representation:

```java
new RespArray(
    List.of(
        new RespBulkString(
            "GET".getBytes(StandardCharsets.UTF_8)
        ),
        new RespBulkString(
            "name".getBytes(StandardCharsets.UTF_8)
        )
    )
);
```

Redis commands are normally sent as arrays of bulk strings.

---

# Example: PING

Command:

```bash
PING
```

RESP:

```text
*1\r\n
$4\r\n
PING\r\n
```

Meaning:

```text
Array
└── Bulk String "PING"
```

Java:

```java
new RespArray(
    List.of(
        new RespBulkString(
            "PING".getBytes(StandardCharsets.UTF_8)
        )
    )
);
```

Redis response:

```text
+PONG\r\n
```

Java:

```java
new RespSimpleString("PONG");
```

---

# Example: GET

Command:

```bash
GET name
```

RESP:

```text
*2\r\n
$3\r\n
GET\r\n
$4\r\n
name\r\n
```

Meaning:

```text
Array
├── Bulk String "GET"
└── Bulk String "name"
```

Java:

```java
new RespArray(
    List.of(
        new RespBulkString(
            "GET".getBytes(StandardCharsets.UTF_8)
        ),
        new RespBulkString(
            "name".getBytes(StandardCharsets.UTF_8)
        )
    )
);
```

---

# Example: SET

Command:

```bash
SET name Alice
```

RESP:

```text
*3\r\n
$3\r\n
SET\r\n
$4\r\n
name\r\n
$5\r\n
Alice\r\n
```

Meaning:

```text
Array with 3 elements

1. SET
2. name
3. Alice
```

Java:

```java
new RespArray(
    List.of(
        new RespBulkString(
            "SET".getBytes(StandardCharsets.UTF_8)
        ),
        new RespBulkString(
            "name".getBytes(StandardCharsets.UTF_8)
        ),
        new RespBulkString(
            "Alice".getBytes(StandardCharsets.UTF_8)
        )
    )
);
```

---

# What does `\r\n` mean?

RESP uses:

```text
\r\n
```

to terminate protocol lines.

They are two bytes:

```text
\r = carriage return = 13
\n = line feed       = 10
```

For example:

```text
+OK\r\n
```

contains:

```text
+
O
K
\r
\n
```

---

# Encoder

The encoder converts Java RESP objects into RESP bytes.

```text
Java object
    ↓
RespEncoder
    ↓
RESP bytes
    ↓
TCP
```

Example:

```java
new RespSimpleString("OK");
```

becomes:

```text
+OK\r\n
```

So:

```text
Encoder = Java → RESP
```

---

# Decoder

The decoder converts RESP bytes into Java RESP objects.

```text
TCP
 ↓
RESP bytes
 ↓
RespDecoder
 ↓
Java object
```

Example:

```text
:100\r\n
```

becomes:

```java
new RespInteger(100);
```

So:

```text
Decoder = RESP → Java
```

---

# Important: TCP Does Not Send RESP Messages

TCP only provides an ordered stream of bytes.

For example, the client may send:

```text
*2\r\n$3\r\nGET\r\n$4\r\nname\r\n
```

but the server might receive:

```text
read 1:
*2\r\n$3\r

read 2:
\nGET\r\n$4\r\nna

read 3:
me\r\n
```

Therefore the RESP decoder must keep incomplete bytes until the complete RESP value arrives.

The opposite can also happen.

One TCP read might contain multiple commands:

```text
*1\r\n$4\r\nPING\r\n
*1\r\n$4\r\nPING\r\n
```

Therefore:

```text
1 TCP read != 1 Redis command
```

---

# Java Type Summary

```text
RESP

RespValue
│
├── RespSimpleString
│     +OK\r\n
│
├── RespError
│     -ERR message\r\n
│
├── RespInteger
│     :100\r\n
│
├── RespBulkString
│     $5\r\nhello\r\n
│
├── RespNullBulkString
│     $-1\r\n
│
├── RespArray
│     *2\r\n...
│
└── RespNullArray
      *-1\r\n
```

---

# Syntax Cheat Sheet

| Syntax            | Meaning               | Java                          |
| ----------------- | --------------------- | ----------------------------- |
| `+OK\r\n`         | Simple String `"OK"`  | `RespSimpleString("OK")`      |
| `-ERR bad\r\n`    | Error                 | `RespError("ERR bad")`        |
| `:10\r\n`         | Integer `10`          | `RespInteger(10)`             |
| `$5\r\nhello\r\n` | 5-byte Bulk String    | `RespBulkString(byte[])`      |
| `$-1\r\n`         | Null Bulk String      | `RespNullBulkString.INSTANCE` |
| `*2\r\n...`       | Array with 2 elements | `RespArray(List<RespValue>)`  |
| `*-1\r\n`         | Null Array            | `RespNullArray.INSTANCE`      |

---

# Mental Model

Remember this flow:

```text
GET name

   ↓

RESP

*2\r\n
$3\r\n
GET\r\n
$4\r\n
name\r\n

   ↓

bytes

   ↓ TCP

MiniRedis

   ↓

RespDecoder

   ↓

RespArray[
    RespBulkString("GET"),
    RespBulkString("name")
]

   ↓

Command layer
```

The three most important ideas are:

```text
RESP = Redis communication format

Encoder = Java → RESP bytes

Decoder = RESP bytes → Java
```
