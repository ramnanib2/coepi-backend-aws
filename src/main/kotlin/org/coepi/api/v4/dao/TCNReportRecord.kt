package org.coepi.api.v4.dao

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable

@DynamoDBTable(tableName = "TCNReportsDao")
data class TCNReportRecord(
    @DynamoDBHashKey
    var reportId: String = "",

    @DynamoDBRangeKey
    var randomId: String = "",

    @DynamoDBAttribute
    var reportTimestamp: Long = 0,

    @DynamoDBAttribute
    var report: ByteArray = byteArrayOf()
)