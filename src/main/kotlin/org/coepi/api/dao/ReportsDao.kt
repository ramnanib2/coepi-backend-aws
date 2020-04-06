package org.coepi.api.dao

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.apache.commons.lang3.Validate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ReportsDao {
    private val dynamoMapper: DynamoDBMapper

    companion object {
        val TIME_ZONE_ID = ZoneId.of("UTC")

        fun generateDateIdFromCurrentTime(): String {
            return generateDateIdFromInstant(Instant.now())
        }

        fun generateDateIdFromTimestamp(timestamp: Long): String {
            return generateDateIdFromInstant(Instant.ofEpochMilli(timestamp))
        }

        fun generateLocalDateFromTimestamp(timestamp: Long): LocalDate {
            return generateLocalDateFromInstant(Instant.ofEpochMilli(timestamp))
        }

        fun generateLocalDateFromInstant(instant: Instant): LocalDate {
            return instant.atZone(TIME_ZONE_ID).toLocalDate()
        }

        fun generateDateIdFromInstant(instant: Instant): String {
            return generateLocalDateFromInstant(instant).toString()
        }
    }

    init {
        val ddbClient = AmazonDynamoDBClientBuilder.standard().build()
        this.dynamoMapper = DynamoDBMapper(ddbClient)
    }

    fun addReport(cenKeys: Array<String>, reportData: ByteArray): ReportItem {
        return addReport(cenKeys, reportData, Instant.now().toEpochMilli())
    }

    fun addReport(cenKeys: Array<String>, reportData: ByteArray, timestamp: Long): ReportItem {
        Validate.isTrue(cenKeys.isNotEmpty(), "cenKeys cannot be empty")
        Validate.isTrue(reportData.isNotEmpty(), "reportData cannot be empty")
        Validate.isTrue(timestamp > 0, "timestamp needs to be positive")

        val did = generateDateIdFromTimestamp(timestamp)
        val reportItem = ReportItem(did, timestamp, reportData, cenKeys.toSet())
        this.dynamoMapper.save(reportItem)
        return reportItem
    }

    fun queryReports(timestampLower: Optional<Long>, timestampUpper: Optional<Long>): List<ReportItem> {
        if (timestampLower.isPresent && timestampUpper.isPresent) {
            Validate.isTrue(timestampUpper.get() <= timestampUpper.get(),
                    "timestampLower should be before timestampUpper")

            // check if the dates are the same. If not, then increment the dates
            // until the timestampUpper date arrives, and keep collecting items
            var lowerDate = generateLocalDateFromTimestamp(timestampLower.get())
            val upperDate = generateLocalDateFromTimestamp(timestampUpper.get())
            val outputList = mutableListOf<ReportItem>()

            var tl = timestampLower
            var tu = timestampUpper

            do {
                val partialList = queryReports(lowerDate.toString(), tl, tu)
                outputList.addAll(partialList)
                lowerDate = lowerDate.plusDays(1)
                tl = Optional.of(lowerDate.toEpochDay() * 24 * 3600 * 1000)
            } while (lowerDate <= upperDate)

            return outputList
        } else if (timestampLower.isPresent) {
            return queryReports(generateDateIdFromTimestamp(timestampLower.get()),
                    timestampLower, timestampUpper)
        }
        return queryReports(generateDateIdFromCurrentTime(), timestampLower, timestampUpper)
    }

    private fun queryReports(did: String,
                             timestampLower: Optional<Long>,
                             timestampUpper: Optional<Long>): List<ReportItem> {
        val queryExpression = DynamoDBQueryExpression<ReportItem>()
        queryExpression.keyConditionExpression = "did = :val1"

        val attributeValueMap = HashMap<String, AttributeValue>()
        attributeValueMap[":val1"] = AttributeValue().withS(did)

        if (timestampLower.isPresent) {
            val timestampLowerVal = timestampLower.get()
            val timestampUpperVal = timestampUpper.orElseGet { Instant.now().toEpochMilli() }

            Validate.isTrue(timestampLowerVal <= timestampUpperVal,
                    "lowerTimestamp should be lesser than or equal to upperTimestamp. " +
                            "lowerTimestamp: $timestampLowerVal, upperTimestamp: $timestampUpperVal")

            queryExpression.keyConditionExpression = "${queryExpression.keyConditionExpression} and " +
                    "reportTimestamp BETWEEN :val2 AND :val3"
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