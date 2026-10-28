package src.services

import src.repositories.DatabaseHelper
import java.time.LocalDate

object ReportService {
    // تقرير المخزون الحالي لكل مستودع
    fun inventoryReport(): List<InventoryRow> {
        val sql = """
            SELECT p.name AS product, w.name AS warehouse, i.quantity
            FROM inventory i
            JOIN products p ON i.product_id = p.id
            JOIN warehouses w ON i.warehouse_id = w.id
            ORDER BY w.name, p.name
        """
        val stmt = DatabaseHelper.connection.createStatement()
        val rs = stmt.executeQuery(sql)
        val list = mutableListOf<InventoryRow>()
        while (rs.next()) {
            list.add(
                InventoryRow(
                    product = rs.getString("product"),
                    warehouse = rs.getString("warehouse"),
                    quantity = rs.getDouble("quantity")
                )
            )
        }
        rs.close(); stmt.close()
        return list
    }

    // تقرير الأرباح والخسائر للفترة المحددة
    fun profitLossReport(startDate: String, endDate: String): ProfitLoss {
        // إيرادات (فواتير بيع)
        val revSql = """
            SELECT SUM(total) AS revenue FROM invoices
            WHERE type = 'SELL' AND date BETWEEN ? AND ?
        """
        val revStmt = DatabaseHelper.connection.prepareStatement(revSql)
        revStmt.setString(1, startDate)
        revStmt.setString(2, endDate)
        val revRs = revStmt.executeQuery()
        val revenue = if (revRs.next()) revRs.getDouble("revenue") else 0.0
        revRs.close(); revStmt.close()

        // مصاريف (فواتير شراء + إهلاك)
        val expSql = """
            SELECT SUM(total) AS expenses FROM invoices WHERE type = 'BUY' AND date BETWEEN ? AND ?
        """
        val expStmt = DatabaseHelper.connection.prepareStatement(expSql)
        expStmt.setString(1, startDate)
        expStmt.setString(2, endDate)
        val expRs = expStmt.executeQuery()
        val purchases = if (expRs.next()) expRs.getDouble("expenses") else 0.0
        expRs.close(); expStmt.close()

        // إهلاك
        val depSql = """
            SELECT SUM(yearly_depreciation) AS depreciation FROM depreciation
            WHERE purchase_date BETWEEN ? AND ?
        """
        val depStmt = DatabaseHelper.connection.prepareStatement(depSql)
        depStmt.setString(1, startDate)
        depStmt.setString(2, endDate)
        val depRs = depStmt.executeQuery()
        val depreciation = if (depRs.next()) depRs.getDouble("depreciation") else 0.0
        depRs.close(); depStmt.close()

        val totalExpenses = purchases + depreciation
        val netProfit = revenue - totalExpenses
        return ProfitLoss(revenue, totalExpenses, netProfit)
    }

    // تقرير الميزانية (قائمة الأصول والخصوم) حتى تاريخ معين
    fun balanceSheetReport(asOfDate: String): BalanceSheet {
        // الأصول من جدول الحسابات حيث الكود يبدأ بـ 1 (مثال)
        val assetsSql = """
            SELECT SUM(debit - credit) AS total FROM journal_entries
            WHERE date <= ? AND account_id IN (SELECT id FROM accounts WHERE code LIKE '1%')
        """
        val assetsStmt = DatabaseHelper.connection.prepareStatement(assetsSql)
        assetsStmt.setString(1, asOfDate)
        val assetsRs = assetsStmt.executeQuery()
        val assets = if (assetsRs.next()) assetsRs.getDouble("total") else 0.0
        assetsRs.close(); assetsStmt.close()

        // الخصوم من حسابات الكود يبدأ بـ 2
        val liabilitiesSql = """
            SELECT SUM(credit - debit) AS total FROM journal_entries
            WHERE date <= ? AND account_id IN (SELECT id FROM accounts WHERE code LIKE '2%')
        """
        val liabStmt = DatabaseHelper.connection.prepareStatement(liabilitiesSql)
        liabStmt.setString(1, asOfDate)
        val liabRs = liabStmt.executeQuery()
        val liabilities = if (liabRs.next()) liabRs.getDouble("total") else 0.0
        liabRs.close(); liabStmt.close()

        // حقوق الملكية (كود 3)
        val equitySql = """
            SELECT SUM(credit - debit) AS total FROM journal_entries
            WHERE date <= ? AND account_id IN (SELECT id FROM accounts WHERE code LIKE '3%')
        """
        val eqStmt = DatabaseHelper.connection.prepareStatement(equitySql)
        eqStmt.setString(1, asOfDate)
        val eqRs = eqStmt.executeQuery()
        val equity = if (eqRs.next()) eqRs.getDouble("total") else 0.0
        eqRs.close(); eqStmt.close()

        return BalanceSheet(assets, liabilities, equity)
    }
}

// كائنات بيانات لتسهيل الإرجاع
data class InventoryRow(val product: String, val warehouse: String, val quantity: Double)

data class ProfitLoss(val revenue: Double, val expenses: Double, val netProfit: Double)

data class BalanceSheet(val assets: Double, val liabilities: Double, val equity: Double)
