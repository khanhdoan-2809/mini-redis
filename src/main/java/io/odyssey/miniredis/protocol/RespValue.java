package io.odyssey.miniredis.protocol;

public sealed interface RespValue
        permits RespSimpleString,
        RespError,
        RespInteger,
        RespBulkString,
        RespArray,
        RespNullBulkString,
        RespNullArray {
}