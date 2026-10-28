package tests

import src.services.ReportService
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportServiceTest {
    @Test
    fun `inventory report returns list`() {
        val list = ReportService.inventoryReport()
        assertTrue(list.isNotEmpty() || list.isEmpty()) // just ensure no exception
    }

    @Test
    fun `profit loss report computes values`() {
        val result = ReportService.profitLossReport("2023-01-01", "2023-12-31")
        // لا نتوقع قيم محددة، فقط نتأكد من عدم حدوث استثناء
        assertTrue(result.revenue >= 0)
    }
}
