package org.coepi.api.v4.dao

import org.coepi.api.v4.TCNCloudAPIHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TCNReportsDaoTest {

    private val dao = TCNReportsDao()
    private val reportData = "foobar".toByteArray(Charset.defaultCharset())

    @Test
    fun addReport_sanity() {
        val date = LocalDate.now(ZoneId.of("UTC"))
        val now = Instant.now().toEpochMilli()
        val intervalNumber = TCNCloudAPIHandler.generateIntervalForTimestamp(now)
        dao.addReport(reportData, date, intervalNumber, now)
        val reports = dao.queryReports(date, intervalNumber)
        Assertions.assertTrue(reports.isNotEmpty())
        println(reports.joinToString("\n"))
    }
}