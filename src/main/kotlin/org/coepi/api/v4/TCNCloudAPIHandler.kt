package org.coepi.api.v4

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.coepi.api.CoEpiClientException
import org.coepi.api.InvalidTCNSignatureException
import org.coepi.api.TCNClientException
import org.coepi.api.UnexpectedIntervalLengthException
import org.coepi.api.v4.dao.TCNReportsDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.*


class TCNCloudAPIHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val reportsDao: TCNReportsDao = TCNReportsDao()
    private val objectMapper: ObjectMapper = ObjectMapper()
    private var logger: LambdaLogger? = null

    companion object {
        const val DATE_KEY = "date"
        const val INTERVAL_NUMBER_KEY = "intervalNumber"
        const val INTERVAL_LENGTH_MS_KEY = "intervalLengthMs"
        const val INTERVAL_LENGTH_MS: Long = 6 * 3600 * 1000

        fun generateIntervalForCurrentTime(): Long {
            return generateIntervalForTimestamp(Instant.now().toEpochMilli())
        }

        fun generateIntervalForTimestamp(timestamp: Long): Long {
            return timestamp / INTERVAL_LENGTH_MS
        }
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
            val intervalNumber = queryParams.second.orElse(generateIntervalForCurrentTime())

            val reports = reportsDao.queryReports(date, intervalNumber)
                    .map {
                        record -> Base64.getEncoder().encode(record.report)
                    }

            body = objectMapper.writeValueAsString(reports)
            statusCode = 200
        } catch (ex: TCNClientException) {
            body = ex.message
            statusCode = 400
        } catch (ex: UnexpectedIntervalLengthException) {
            body = ex.message
            statusCode = 401
        }

        return APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
    }

    private fun parseQueryParameters(input: APIGatewayProxyRequestEvent): Pair<Optional<LocalDate>, Optional<Long>> {
        val queryParameters = input.queryStringParameters
        var date = Optional.empty<LocalDate>()
        var intervalNumber = Optional.empty<Long>()

        if (queryParameters == null) return Pair(date, intervalNumber)


        try {
            if(queryParameters.containsKey(DATE_KEY) && !queryParameters[DATE_KEY].isNullOrEmpty()) {
                val rawDate = LocalDate.parse(queryParameters[DATE_KEY]) // Unit Test
                date = Optional.of(rawDate)
            }

            if(queryParameters.containsKey(INTERVAL_NUMBER_KEY)) {
                val rawBatch = queryParameters[INTERVAL_NUMBER_KEY]?.toLong()
                intervalNumber = if (rawBatch != null) Optional.of(rawBatch) else Optional.empty()

                if (!queryParameters.containsKey(INTERVAL_LENGTH_MS_KEY)) {
                    throw TCNClientException("$INTERVAL_LENGTH_MS_KEY query parameter is required if " +
                            " $INTERVAL_LENGTH_MS_KEY is provided")
                }
                val intervalLengthMs = queryParameters[INTERVAL_LENGTH_MS_KEY]?.toLong()

                if (intervalLengthMs != INTERVAL_LENGTH_MS) {
                    throw UnexpectedIntervalLengthException("$intervalLengthMs is invalid for the date " +
                            "$DATE_KEY. Please use $INTERVAL_LENGTH_MS to calculate $INTERVAL_NUMBER_KEY")
                }
            }
        } catch (ex: DateTimeParseException) {
            throw TCNClientException("$DATE_KEY in illegal date format.", ex)
        } catch (ex: NumberFormatException) {
            throw TCNClientException("$INTERVAL_NUMBER_KEY or $INTERVAL_LENGTH_MS_KEY in " +
                    "illegal number format.", ex)
        }

        if (intervalNumber.isPresent && intervalNumber.get() < 0) {
            throw CoEpiClientException("$INTERVAL_LENGTH_MS_KEY should be positive")
        }
        return Pair(date, intervalNumber)
    }

    fun handlePostReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        return try {
            val reportData = Base64.getDecoder().decode(input.body)
            val timestamp = Instant.now().toEpochMilli()
            val date = LocalDate.now(ZoneId.of("UTC"))
            val intervalNumber = generateIntervalForTimestamp(timestamp)

            // TODO: Validate reportData and signature
            reportsDao.addReport(reportData, date, intervalNumber, timestamp)

            APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
        } catch (ex: InvalidTCNSignatureException) {
            APIGatewayProxyResponseEvent()
                    .withBody(ex.message)
                    .withStatusCode(401)
        } catch (ex: IllegalArgumentException) {
            APIGatewayProxyResponseEvent()
                    .withBody(ex.message)
                    .withStatusCode(400)
        }
    }
}