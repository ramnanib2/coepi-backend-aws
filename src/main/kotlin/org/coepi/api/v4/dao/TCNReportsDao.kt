package org.coepi.api.v4.dao

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.apache.commons.lang3.Validate
import java.time.LocalDate
import java.util.*

class TCNReportsDao {
    private val dynamoMapper: DynamoDBMapper

    companion object {
        fun generateReportId(date: LocalDate, intervalNumber: Long): String {
            return date.toString() + ":" + intervalNumber
        }
    }

    init {
        val ddbClient = AmazonDynamoDBClientBuilder.standard().build()
        this.dynamoMapper = DynamoDBMapper(ddbClient)
    }

    fun addReport(reportData: ByteArray,
                  date: LocalDate,
                  intervalNumber: Long,
                  timestamp: Long): TCNReportRecord {
        Validate.isTrue(reportData.isNotEmpty(), "reportData cannot be empty")
        Validate.isTrue(intervalNumber > 0, "intervalNumber should be positive")
        Validate.isTrue(timestamp > 0, "timestamp needs to be positive")

        val reportId = generateReportId(date, intervalNumber)
        val randomId = UUID.randomUUID().toString()
        val reportRecord = TCNReportRecord(reportId, randomId,  timestamp, reportData)
        this.dynamoMapper.save(reportRecord)
        return reportRecord
    }

    fun queryReports(date: LocalDate, intervalNumber: Long): List<TCNReportRecord> {
        val reportId = generateReportId(date, intervalNumber)
        val queryExpression = DynamoDBQueryExpression<TCNReportRecord>()
        queryExpression.keyConditionExpression = "reportId = :val1"

        val attributeValueMap = HashMap<String, AttributeValue>()
        attributeValueMap[":val1"] = AttributeValue().withS(reportId)
        queryExpression.expressionAttributeValues = attributeValueMap

        val outputList = mutableListOf<TCNReportRecord>()
        var lastEvalKey: Map<String, AttributeValue>? = null

        do {
            queryExpression.exclusiveStartKey = lastEvalKey
            val pageOutput = dynamoMapper.queryPage(TCNReportRecord::class.java, queryExpression)
            outputList.addAll(pageOutput.results)
            lastEvalKey = pageOutput.lastEvaluatedKey
        } while (lastEvalKey != null)

        return outputList
    }
}