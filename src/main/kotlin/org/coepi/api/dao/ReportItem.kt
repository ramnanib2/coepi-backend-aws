package org.coepi.api.dao

import com.amazonaws.services.dynamodbv2.datamodeling.*

@DynamoDBTable(tableName = "Reports")
data class ReportItem (
        @DynamoDBHashKey
        var did: String = "",

        @DynamoDBRangeKey
        var reportTimestamp: Long = 0,

        @DynamoDBAttribute
        var report: ByteArray = byteArrayOf(),

        @DynamoDBAttribute
        var cenKeys: Set<String> = setOf()
)