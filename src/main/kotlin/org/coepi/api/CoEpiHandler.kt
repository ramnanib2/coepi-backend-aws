package org.coepi.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper

import org.coepi.api.dao.ReportsDao
import org.coepi.api.models.Body
import java.util.*

class CoEpiHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val reportsDao: ReportsDao = ReportsDao()
    private val objectMapper: ObjectMapper = ObjectMapper()
    private var logger: LambdaLogger? = null

    companion object {
        const val TIMESTAMP_LOWER_KEY = "timestampLower"
        const val TIMESTAMP_UPPER_KEY = "timestampUpper"
        const val MAX_RANGE_MS = 5 * 24 * 3600 * 1000 // 5 Days
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
            val timestampPair = parseTimestamps(input)

            val reports = reportsDao
                    .queryReports(timestampPair.first, timestampPair.second)
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

    private fun parseTimestamps(input: APIGatewayProxyRequestEvent): Pair<Optional<Long>, Optional<Long>> {
        val queryParameters = input.queryStringParameters
        var timestampLower = Optional.empty<Long>()
        var timestampUpper = Optional.empty<Long>()

        if (queryParameters == null) return Pair(timestampLower, timestampUpper)


        try {
            if(queryParameters.containsKey(TIMESTAMP_LOWER_KEY)) {
                val tl = queryParameters[TIMESTAMP_LOWER_KEY]?.toLong()
                timestampLower = if (tl != null) Optional.of(tl) else Optional.empty()
            }

            if(queryParameters.containsKey(TIMESTAMP_UPPER_KEY)) {
                val tu = queryParameters[TIMESTAMP_UPPER_KEY]?.toLong()
                timestampUpper = if (tu != null) Optional.of(tu) else Optional.empty()
            }
        } catch (ex: NumberFormatException) {
            throw CoEpiClientException("$TIMESTAMP_LOWER_KEY and $TIMESTAMP_UPPER_KEY must be in number format.", ex)
        }

        if (timestampLower.isPresent and timestampUpper.isPresent) {
            val tl = timestampLower.get()
            val tu = timestampUpper.get()

            if (tl > tu) {
                throw CoEpiClientException("$TIMESTAMP_LOWER_KEY: $tl needs to be lesser than " +
                        "or equal to $TIMESTAMP_UPPER_KEY: $tu")
            }

            if (tu - tl > MAX_RANGE_MS) {
                throw CoEpiClientException("Difference between $TIMESTAMP_UPPER_KEY and $TIMESTAMP_LOWER_KEY " +
                        "should be at least $MAX_RANGE_MS milliseconds")
            }
        }
        return Pair(timestampLower, timestampUpper)
    }

    fun handlePostReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        try {
            val body = objectMapper.readValue(input.body, Body::class.java)
            val cenKeys = body.cenKeys
            val reportData = body.report

            reportsDao.addReport(cenKeys, reportData)

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
}