package org.coepi.api.dao

import jdk.nashorn.internal.ir.annotations.Ignore
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * These tests are only reproducible in a specific state and rely on DDB Web Service.
 * In other words they are badly written.
 *
 * TODO: Use DynamoDBLocal to make the tests re-producible on local devboxes.
 *
 * Until then, don't expect them to succeed, just use them as documentation
 */
@Ignore
@Disabled
class ReportsDaoTest {

    private val dao: ReportsDao = ReportsDao()
    val cenKeys = arrayOf("foo", "bar")
    val reportData = "foobar".toByteArray(Charset.defaultCharset())

    @Test
    fun addReport_sanity() {
        val date = LocalDate.now()
        val batchNumber = 1
        dao.addReport(cenKeys, reportData, date, batchNumber, System.currentTimeMillis())
        val reports = dao.queryReports(date, batchNumber)
        Assertions.assertThat(reports.isNotEmpty())
        println(reports.joinToString("\n"))
    }
}