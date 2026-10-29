package com.starmaen.accountingstar.data

import androidx.room.withTransaction
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money
import com.starmaen.accountingstar.util.Security
import kotlin.math.max
import kotlin.math.min

data class InvoiceLineInput(
    val productId: Long,
    val name: String,
    val unit: String,
    val qty: Double,
    val unitPrice: Double,
    val costPrice: Double,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0
)

data class InvoiceRequest(
    val projectId: Long,
    val branchId: Long,
    val warehouseId: Long,
    val type: String,
    val paymentType: String,
    val partyType: String,
    val partyId: Long?,
    val partyName: String,
    val lines: List<InvoiceLineInput>,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val paid: Double = 0.0,
    val notes: String = "",
    val installmentCount: Int = 0,
    val firstDueDate: Long = Dates.addMonths(System.currentTimeMillis(), 1)
)

data class DashboardStats(
    val todaySales: Double = 0.0,
    val monthSales: Double = 0.0,
    val monthProfit: Double = 0.0,
    val receivables: Double = 0.0,
    val payables: Double = 0.0,
    val lowStockCount: Int = 0,
    val dueInstallments: Int = 0,
    val expenses: Double = 0.0
)

data class ReportBundle(
    val from: Long,
    val to: Long,
    val sales: Double = 0.0,
    val purchases: Double = 0.0,
    val returns: Double = 0.0,
    val profit: Double = 0.0,
    val expenses: Double = 0.0,
    val topProducts: List<TopProductRow> = emptyList(),
    val lowStock: List<WarehouseStockRow> = emptyList()
)

data class InvoiceDetail(
    val invoice: InvoiceEntity,
    val items: List<InvoiceItemEntity>,
    val payments: List<PaymentEntity>,
    val installments: List<InstallmentEntity>
)

class Repository(private val db: AppDatabase) {
    val projects = db.projectDao()
    val branches = db.branchDao()
    val users = db.userDao()
    val warehouses = db.warehouseDao()
    val categories = db.categoryDao()
    val products = db.productDao()
    val stock = db.stockDao()
    val customers = db.customerDao()
    val suppliers = db.supplierDao()
    val invoices = db.invoiceDao()
    val expenses = db.expenseDao()
    val stockMoves = db.stockMoveDao()
    val transfers = db.transferDao()

    suspend fun ensureSeeded() {
        if (projects.count() > 0) return
        val projectId = projects.insert(
            ProjectEntity(name = "متجري", businessType = "GENERAL", currency = "ر.س", taxRate = 0.0)
        )
        val branchId = branches.insert(
            BranchEntity(projectId = projectId, name = "الفرع الرئيسي", isDefault = true)
        )
        warehouses.insert(
            WarehouseEntity(projectId = projectId, branchId = branchId, name = "المستودع الرئيسي", isDefault = true)
        )
        val salt = Security.newSalt()
        users.insert(
            UserEntity(
                projectId = 0,
                username = "admin",
                passwordHash = Security.hash("admin123", salt),
                salt = salt,
                fullName = "مدير النظام",
                role = Roles.ADMIN
            )
        )
        listOf("عام", "ألبسة", "بقالة", "مستحضرات تجميل", "إلكترونيات").forEach {
            categories.insert(CategoryEntity(projectId = projectId, name = it))
        }
    }

    suspend fun login(username: String, password: String): UserEntity? {
        val user = users.byUsername(username.trim())
        if (user == null) return null
        return if (Security.verify(password, user.salt, user.passwordHash)) user else null
    }

    suspend fun userById(id: Long): UserEntity? = users.byId(id)

    suspend fun saveProduct(product: ProductEntity, openingQty: Double, warehouseId: Long): Long {
        val id = if (product.id == 0L) {
            products.insert(product)
        } else {
            products.update(product)
            product.id
        }
        if (product.id == 0L && warehouseId > 0) {
            stock.insert(
                StockEntity(
                    projectId = product.projectId,
                    warehouseId = warehouseId,
                    productId = id,
                    quantity = openingQty
                )
            )
            if (openingQty != 0.0) {
                stockMoves.insert(
                    StockMoveEntity(
                        projectId = product.projectId,
                        warehouseId = warehouseId,
                        productId = id,
                        qty = openingQty,
                        reason = StockReason.OPENING,
                        note = "رصيد افتتاحي"
                    )
                )
            }
        }
        return id
    }

    suspend fun deleteProduct(id: Long) = products.deactivate(id)

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        val entity = if (customer.id == 0L) {
            customer.copy(balance = customer.openingBalance)
        } else customer
        return if (entity.id == 0L) customers.insert(entity) else {
            customers.update(entity); entity.id
        }
    }

    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        val entity = if (supplier.id == 0L) supplier.copy(balance = supplier.openingBalance) else supplier
        return if (entity.id == 0L) suppliers.insert(entity) else {
            suppliers.update(entity); entity.id
        }
    }

    suspend fun createInvoice(request: InvoiceRequest): Long = db.withTransaction {
        require(request.lines.isNotEmpty()) { "لا يمكن حفظ فاتورة بدون أصناف" }
        val subtotal = Money.round(request.lines.sumOf { lineTotal(it) })
        val discount = Money.round(min(request.discount, subtotal).coerceAtLeast(0.0))
        val taxable = subtotal - discount
        val tax = Money.round(taxable * request.taxRate / 100.0)
        val total = Money.round(taxable + tax)
        val isCash = request.paymentType == PaymentType.CASH
        val paid = if (isCash) total else Money.round(min(max(request.paid, 0.0), total))
        val remaining = Money.round(total - paid)
        val status = when {
            remaining <= 0.0 -> InvoiceStatus.PAID
            paid > 0.0 -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.UNPAID
        }
        val prefix = when (request.type) {
            InvoiceType.SALE -> "S"
            InvoiceType.PURCHASE -> "P"
            InvoiceType.SALE_RETURN -> "SR"
            else -> "PR"
        }
        val seq = invoices.countByType(request.projectId, request.type) + 1
        val invoiceNo = "$prefix-${seq.toString().padStart(4, '0')}"

        val invoiceId = invoices.insert(
            InvoiceEntity(
                projectId = request.projectId,
                branchId = request.branchId,
                warehouseId = request.warehouseId,
                invoiceNo = invoiceNo,
                type = request.type,
                paymentType = request.paymentType,
                partyType = request.partyType,
                partyId = request.partyId,
                partyName = request.partyName,
                discount = discount,
                taxRate = request.taxRate,
                taxAmount = tax,
                subtotal = subtotal,
                total = total,
                paid = paid,
                remaining = remaining,
                status = status,
                notes = request.notes
            )
        )

        invoices.insertItems(
            request.lines.map { line ->
                InvoiceItemEntity(
                    invoiceId = invoiceId,
                    productId = line.productId,
                    name = line.name,
                    unit = line.unit,
                    qty = line.qty,
                    unitPrice = line.unitPrice,
                    costPrice = line.costPrice,
                    discount = Money.round(line.discount),
                    taxRate = line.taxRate,
                    lineTotal = lineTotal(line)
                )
            }
        )

        request.lines.forEach { line ->
            val delta = when (request.type) {
                InvoiceType.SALE -> -line.qty
                InvoiceType.PURCHASE -> line.qty
                InvoiceType.SALE_RETURN -> line.qty
                else -> -line.qty
            }
            changeStock(
                projectId = request.projectId,
                warehouseId = request.warehouseId,
                productId = line.productId,
                delta = delta,
                reason = request.type,
                refId = invoiceId,
                note = "فاتورة $invoiceNo"
            )
        }

        val partyDelta = when (request.type) {
            InvoiceType.SALE -> remaining
            InvoiceType.SALE_RETURN -> -remaining
            InvoiceType.PURCHASE -> remaining
            else -> -remaining
        }
        if (partyDelta != 0.0 && request.partyId != null) {
            if (request.partyType == PartyType.CUSTOMER) customers.addBalance(request.partyId, partyDelta)
            else suppliers.addBalance(request.partyId, partyDelta)
        }

        if (paid > 0.0) {
            invoices.insertPayment(
                PaymentEntity(
                    projectId = request.projectId,
                    invoiceId = invoiceId,
                    partyType = request.partyType,
                    partyId = request.partyId,
                    amount = paid,
                    method = if (isCash) "CASH" else "PARTIAL",
                    notes = "دفعة عند إنشاء الفاتورة"
                )
            )
        }

        if (request.paymentType == PaymentType.INSTALLMENT && remaining > 0.0 && request.installmentCount > 0) {
            val each = Money.round(remaining / request.installmentCount)
            val rows = mutableListOf<InstallmentEntity>()
            var allocated = 0.0
            for (i in 1..request.installmentCount) {
                val amount = if (i == request.installmentCount) {
                    Money.round(remaining - allocated)
                } else each
                allocated = Money.round(allocated + amount)
                rows.add(
                    InstallmentEntity(
                        projectId = request.projectId,
                        invoiceId = invoiceId,
                        seq = i,
                        dueDate = Dates.addMonths(request.firstDueDate, i - 1),
                        amount = amount
                    )
                )
            }
            invoices.insertInstallments(rows)
        }
        invoiceId
    }

    suspend fun addPayment(invoiceId: Long, amount: Double, method: String, note: String = "") =
        db.withTransaction {
            val invoice = invoices.byId(invoiceId) ?: return@withTransaction
            val value = Money.round(min(max(amount, 0.0), invoice.remaining))
            if (value <= 0.0) return@withTransaction
            val newPaid = Money.round(invoice.paid + value)
            val newRemaining = Money.round(invoice.total - newPaid)
            invoices.update(
                invoice.copy(
                    paid = newPaid,
                    remaining = newRemaining,
                    status = if (newRemaining <= 0.0) InvoiceStatus.PAID else InvoiceStatus.PARTIAL
                )
            )
            invoices.insertPayment(
                PaymentEntity(
                    projectId = invoice.projectId,
                    invoiceId = invoiceId,
                    partyType = invoice.partyType,
                    partyId = invoice.partyId,
                    amount = value,
                    method = method,
                    notes = note
                )
            )
            if (invoice.partyId != null) {
                val delta = when (invoice.type) {
                    InvoiceType.SALE -> -value
                    InvoiceType.SALE_RETURN -> value
                    InvoiceType.PURCHASE -> -value
                    else -> value
                }
                if (invoice.partyType == PartyType.CUSTOMER) {
                    customers.addBalance(invoice.partyId, delta)
                } else {
                    suppliers.addBalance(invoice.partyId, delta)
                }
            }
            var left = value
            invoices.installments(invoiceId).forEach { installment ->
                if (left > 0.0 && installment.status != InvoiceStatus.PAID) {
                    val pay = Money.round(min(left, installment.amount - installment.paidAmount))
                    if (pay > 0.0) {
                        val paidNow = Money.round(installment.paidAmount + pay)
                        invoices.updateInstallment(
                            installment.copy(
                                paidAmount = paidNow,
                                status = if (paidNow >= installment.amount) InvoiceStatus.PAID else InvoiceStatus.PARTIAL
                            )
                        )
                        left = Money.round(left - pay)
                    }
                }
            }
        }

    suspend fun adjustStock(
        projectId: Long,
        warehouseId: Long,
        productId: Long,
        newQuantity: Double,
        note: String = ""
    ) = db.withTransaction {
        val current = stock.find(warehouseId, productId)
        val delta = Money.round(newQuantity - (current?.quantity ?: 0.0))
        if (delta != 0.0) {
            changeStock(projectId, warehouseId, productId, delta, StockReason.ADJUST, null, note)
        }
    }

    suspend fun transferStock(
        projectId: Long,
        fromWarehouseId: Long,
        toWarehouseId: Long,
        productId: Long,
        qty: Double,
        note: String = ""
    ) = db.withTransaction {
        require(fromWarehouseId != toWarehouseId) { "يجب اختيار مستودعين مختلفين" }
        require(qty > 0.0) { "الكمية يجب أن تكون أكبر من صفر" }
        changeStock(projectId, fromWarehouseId, productId, -qty, StockReason.TRANSFER_OUT, null, note)
        changeStock(projectId, toWarehouseId, productId, qty, StockReason.TRANSFER_IN, null, note)
        transfers.insert(
            TransferEntity(
                projectId = projectId,
                fromWarehouseId = fromWarehouseId,
                toWarehouseId = toWarehouseId,
                productId = productId,
                qty = qty,
                notes = note
            )
        )
    }

    private suspend fun changeStock(
        projectId: Long,
        warehouseId: Long,
        productId: Long,
        delta: Double,
        reason: String,
        refId: Long?,
        note: String
    ) {
        val current = stock.find(warehouseId, productId)
        if (current == null) {
            stock.insert(
                StockEntity(
                    projectId = projectId,
                    warehouseId = warehouseId,
                    productId = productId,
                    quantity = delta
                )
            )
        } else {
            stock.update(current.copy(quantity = Money.round(current.quantity + delta)))
        }
        stockMoves.insert(
            StockMoveEntity(
                projectId = projectId,
                warehouseId = warehouseId,
                productId = productId,
                qty = delta,
                reason = reason,
                refId = refId,
                note = note
            )
        )
    }

    suspend fun addExpense(expense: ExpenseEntity): Long = expenses.insert(expense)

    suspend fun dashboard(projectId: Long): DashboardStats {
        val now = System.currentTimeMillis()
        val todayStart = Dates.dayStart(now)
        val todayEnd = Dates.dayEnd(now)
        val monthStart = Dates.monthStart(now)
        return DashboardStats(
            todaySales = invoices.totalBetween(projectId, InvoiceType.SALE, todayStart, todayEnd),
            monthSales = invoices.totalBetween(projectId, InvoiceType.SALE, monthStart, todayEnd),
            monthProfit = invoices.profitBetween(projectId, monthStart, todayEnd),
            receivables = invoices.receivables(projectId),
            payables = invoices.payables(projectId),
            lowStockCount = stock.lowStock(projectId).size,
            dueInstallments = invoices.dueInstallments(projectId, todayEnd).size,
            expenses = expenses.totalBetween(projectId, monthStart, todayEnd)
        )
    }

    suspend fun report(projectId: Long, from: Long, to: Long) = ReportBundle(
        from = from,
        to = to,
        sales = invoices.totalBetween(projectId, InvoiceType.SALE, from, to),
        purchases = invoices.totalBetween(projectId, InvoiceType.PURCHASE, from, to),
        returns = invoices.totalBetween(projectId, InvoiceType.SALE_RETURN, from, to),
        profit = invoices.profitBetween(projectId, from, to),
        expenses = expenses.totalBetween(projectId, from, to),
        topProducts = invoices.topProducts(projectId, from, to),
        lowStock = stock.lowStock(projectId)
    )

    private fun lineTotal(line: InvoiceLineInput): Double =
        Money.round((line.unitPrice * line.qty) - line.discount)
}
