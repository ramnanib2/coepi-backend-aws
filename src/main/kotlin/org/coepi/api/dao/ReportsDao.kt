package org.coepi.api.dao

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.apache.commons.lang3.Validate
import java.time.Instant
import java.util.*

class ReportsDao {
    private val dynamoMapper: DynamoDBMapper

    companion object {
        fun generateDateIdFromCurrentTime(): String {
            return generateDateIdFromInstant(Instant.now())
        }

        fun generateDateIdFromTimestamp(timestamp: Long): String {
            return generateDateIdFromInstant(Instant.ofEpochMilli(timestamp))
        }

        fun generateDateIdFromInstant(instant: Instant): String {
            return instant.toString().substring(0, 10)
        }
    }

    init {
        val ddbClient = AmazonDynamoDBClientBuilder.standard().build()
        this.dynamoMapper = DynamoDBMapper(ddbClient)
    }

    fun addReport(cenKeys: Array<String>, reportData: ByteArray): ReportItem {
        val dateNow = Instant.now()
        val timestamp = dateNow.toEpochMilli()
        val did = dateNow.toString().substring(0, 10)

        val reportItem = ReportItem(did, timestamp, reportData, cenKeys.toSet())
        this.dynamoMapper.save(reportItem)
        return reportItem
    }

    fun queryReports(day: String,
                     timestampLower: Optional<Long>,
                     timestampUpper: Optional<Long>): List<ReportItem> {
        val queryExpression = DynamoDBQueryExpression<ReportItem>()
        queryExpression.keyConditionExpression = "did = :val1"

        val attributeValueMap = HashMap<String, AttributeValue>()
        attributeValueMap[":val1"] = AttributeValue().withS(day)

        if (timestampLower.isPresent) {
            val timestampLowerVal = timestampLower.get()
            val timestampUpperVal = timestampUpper.orElseGet { Instant.now().toEpochMilli() }

            Validate.isTrue(timestampLowerVal <= timestampUpperVal,
                    "lowerTimestamp should be lesser than or equal to upperTimestamp. " +
                            "lowerTimestamp: $timestampLowerVal, upperTimestamp: $timestampUpperVal")

            queryExpression.keyConditionExpression = "${queryExpression.keyConditionExpression} and " +
                    "timestamp BETWEEN :val2 AND :val3"
            attributeValueMap[":val2"] = AttributeValue().withN("$timestampLowerVal")
            attributeValueMap[":val3"] = AttributeValue().withN("$timestampUpperVal")
        }
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