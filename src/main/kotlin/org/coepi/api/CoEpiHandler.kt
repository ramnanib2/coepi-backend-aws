package org.coepi.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.coepi.api.dao.ReportItem
import org.coepi.api.dao.ReportsDao
import org.coepi.api.models.Body
import org.coepi.api.models.Report
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class CoEpiHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val reportsDao: ReportsDao = ReportsDao()
    private val objectMapper: ObjectMapper = ObjectMapper()

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val logger = context?.logger

        logger.log(input.httpMethod)

        if (input.httpMethod == "GET") {
            logger.log("Handling Get Request")
            return handleGetReport(input)
        }
        return handlePostReport(input)
    }

    fun handleGetReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val pathParameters = input.pathParameters
        var timestampLower = Optional.empty<Long>()
        var timestampUpper = Optional.empty<Long>()

        val did = ReportsDao.generateDateIdFromCurrentTime()

        // TODO: Return reports for a given timestamp range
        val reports = reportsDao
                .queryReports(did, Optional.empty(), Optional.empty())

        return APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(objectMapper.writeValueAsString(reports))
    }

    fun handlePostReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val body = objectMapper.readValue(input.body, Body::class.java)
        val cenKeys = body.cenKeys
        val reportData = body.report

        reportsDao.addReport(cenKeys, reportData)

        return APIGatewayProxyResponseEvent()
                .withStatusCode(200)
    }

    private fun mapReportItemToModel(reportItem: ReportItem): Report {
        return Report(
                reportItem.did,
                reportItem.report,
                reportItem.cenKeys.toTypedArray(),
                Timestamp.from(Instant.ofEpochMilli(reportItem.timestamp))
        )
    }

}