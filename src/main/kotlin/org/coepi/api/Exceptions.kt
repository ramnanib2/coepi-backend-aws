package org.coepi.api

open class CoEpiClientException : RuntimeException {
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
}

open class TCNClientException : RuntimeException {
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
}

open class InvalidTCNSignatureException : RuntimeException {
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
}

open class UnexpectedIntervalLengthException : RuntimeException {
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
}