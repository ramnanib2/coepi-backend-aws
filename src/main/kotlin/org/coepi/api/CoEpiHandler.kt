package org.coepi.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.coepi.api.Config.NUM_INTERVALS_PER_DAY

import org.coepi.api.dao.ReportsDao
import org.coepi.api.models.Body
import org.coepi.api.models.Report
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.*

class CoEpiHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val reportsDao: ReportsDao = ReportsDao()
    private val objectMapper: ObjectMapper = ObjectMapper()
    private var logger: LambdaLogger? = null

    companion object {
        const val DATE_KEY = "date"
        const val BATCH_KEY = "batchNumber"
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        logger = context.logger

        logger?.log("Processing request: ${context.awsRequestId}. " +
                "Query params: ${input.queryStringParameters}. " +
                "Body: ${input.body}")
        try {
            if (input.httpMethod == "GET") {
                logger?.log("Handling GET Request for :${input.path}. ReqId: ${context.awsRequestId}")
                return handleGetReport(input)
            }
            logger?.log("Handling POST Request for :${input.path}. ReqId: ${context.awsRequestId}")
            return handlePostReport(input)
        } catch (ex: Exception) {
            logger?.log("Failed to serve request: ${context.awsRequestId}. Cause: ${ex.message}")
            return APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("CoEpi Service Internal Failure")
        }
    }

    fun handleGetReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        var statusCode: Int?
        var body: String?

        try {
            val queryParams = parseQueryParameters(input)

            val date = queryParams.first.orElse(LocalDate.now())
            val bathNumber = queryParams.second.orElse(generateBatchForCurrentTime())

            val reports = reportsDao.queryReports(date, bathNumber)
                    .map {
                        rItem -> Report(rItem.report, rItem.cenKeys.toTypedArray())
                    }

            body = objectMapper.writeValueAsString(reports)
            statusCode = 200
        } catch (ex: CoEpiClientException) {
            body = ex.message
            statusCode = 400
        }

        return APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
    }

    private fun parseQueryParameters(input: APIGatewayProxyRequestEvent): Pair<Optional<LocalDate>, Optional<Int>> {
        val queryParameters = input.queryStringParameters
        var date = Optional.empty<LocalDate>()
        var batchNumber = Optional.empty<Int>()

        if (queryParameters == null) return Pair(date, batchNumber)


        try {
            if(queryParameters.containsKey(DATE_KEY) && !queryParameters[DATE_KEY].isNullOrEmpty()) {
                val rawDate = LocalDate.parse(queryParameters[DATE_KEY]) // Unit Test
                date = Optional.of(rawDate)
            }

            if(queryParameters.containsKey(BATCH_KEY)) {
                val rawBatch = queryParameters[BATCH_KEY]?.toInt()
                batchNumber = if (rawBatch != null) Optional.of(rawBatch) else Optional.empty()
            }
        } catch (ex: DateTimeParseException) {
            throw CoEpiClientException("$DATE_KEY in illegal date format.", ex)
        } catch (ex: NumberFormatException) {
            throw CoEpiClientException("$BATCH_KEY in illegal number format.", ex)
        }

        if (batchNumber.isPresent && (batchNumber.get() < 1
                        || batchNumber.get() > NUM_INTERVALS_PER_DAY)) {
            throw CoEpiClientException("$BATCH_KEY should be between 1 and $NUM_INTERVALS_PER_DAY")
        }
        return Pair(date, batchNumber)
    }

    fun handlePostReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        try {
            val body = objectMapper.readValue(input.body, Body::class.java)
            val cenKeys = body.cenKeys
            val reportData = body.report

            val date = LocalDate.now(ZoneId.of("UTC"))
            val timeStamp = System.currentTimeMillis()
            val batchNumber = generateBatchForCurrentTime()
            reportsDao.addReport(cenKeys, reportData, date, batchNumber, timeStamp)

            return APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
        } catch (ex: Exception) {
            logger?.log("Failed to server request: $input. Message: ${ex.message}")

            when(ex) {
                is JsonProcessingException,
                is JsonMappingException,
                is IllegalArgumentException,
                is NullPointerException -> {
                    return APIGatewayProxyResponseEvent()
                            .withStatusCode(400)
                            .withBody("Illegal payload. ${ex.message}")

                } else -> throw ex
            }
        }
    }

    private fun generateBatchForCurrentTime(): Int {
        for (i in (LocalDateTime.now().hour + 1)..24) {
            if (i % NUM_INTERVALS_PER_DAY == 0) return i
        }
        return NUM_INTERVALS_PER_DAY
    }
}