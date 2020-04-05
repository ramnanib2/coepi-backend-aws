package org.coepi.api.dao

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.util.*

class ReportsDaoTest {

    private val dao: ReportsDao = ReportsDao()

    @Test
    fun addReport_sanity() {
        val cenKeys = arrayOf("foo", "bar")
        val reportData = "foobar".toByteArray(Charset.defaultCharset())
        dao.addReport(cenKeys, reportData)

        val reports = dao.queryReports(ReportsDao.generateDateIdFromCurrentTime(),
                Optional.empty(), Optional.empty())
        Assertions.assertThat(reports.isNotEmpty())
    }
}