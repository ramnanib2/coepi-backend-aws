package org.coepi.api

open class CoEpiClientException : RuntimeException {
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
}