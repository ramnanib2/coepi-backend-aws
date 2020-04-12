package org.coepi.api.dao

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.apache.commons.lang3.Validate
import java.time.LocalDate
import java.util.*

class ReportsDao {
    private val dynamoMapper: DynamoDBMapper

    companion object {
        fun generateReportId(date: LocalDate, batchNumber: Int): String {
            return date.toString() + ":" + batchNumber
        }
    }

    init {
        val ddbClient = AmazonDynamoDBClientBuilder.standard().build()
        this.dynamoMapper = DynamoDBMapper(ddbClient)
    }

    fun addReport(cenKeys: Array<String>,
                  reportData: ByteArray,
                  date: LocalDate,
                  batchNumber: Int,
                  timestamp: Long): ReportItem {
        Validate.isTrue(cenKeys.isNotEmpty(), "cenKeys cannot be empty")
        Validate.isTrue(reportData.isNotEmpty(), "reportData cannot be empty")
        Validate.isTrue(batchNumber > 0, "batchNumber should be positive")
        Validate.isTrue(timestamp > 0, "timestamp needs to be positive")

        val reportId = generateReportId(date, batchNumber)
        val randomId = UUID.randomUUID().toString()
        val reportItem = ReportItem(reportId, randomId,  timestamp, reportData, cenKeys.toSet())
        this.dynamoMapper.save(reportItem)
        return reportItem
    }

    fun queryReports(date: LocalDate, batchNumber: Int): List<ReportItem> {
        val reportId = generateReportId(date, batchNumber)
        val queryExpression = DynamoDBQueryExpression<ReportItem>()
        queryExpression.keyConditionExpression = "reportId = :val1"

        val attributeValueMap = HashMap<String, AttributeValue>()
        attributeValueMap[":val1"] = AttributeValue().withS(reportId)
        queryExpression.expressionAttributeValues = attributeValueMap

        val outputList = mutableListOf<ReportItem>()
        var lastEvalKey: Map<String, AttributeValue>? = null

        do {
            queryExpression.exclusiveStartKey = lastEvalKey
            val pageOutput = dynamoMapper.queryPage(ReportItem::class.java, queryExpression)
            outputList.addAll(pageOutput.results)
            lastEvalKey = pageOutput.lastEvaluatedKey
        } while (lastEvalKey != null)

        return outputList
    }
}