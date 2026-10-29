package com.starmaen.accountingstar.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starmaen.accountingstar.AccountingStarApp
import com.starmaen.accountingstar.data.BranchEntity
import com.starmaen.accountingstar.data.CustomerEntity
import com.starmaen.accountingstar.data.DashboardStats
import com.starmaen.accountingstar.data.InvoiceDetail
import com.starmaen.accountingstar.data.InvoiceLineInput
import com.starmaen.accountingstar.data.InvoiceRequest
import com.starmaen.accountingstar.data.InvoiceStatus
import com.starmaen.accountingstar.data.InvoiceType
import com.starmaen.accountingstar.data.InstallmentRow
import com.starmaen.accountingstar.data.PartyType
import com.starmaen.accountingstar.data.PaymentType
import com.starmaen.accountingstar.data.PriceTier
import com.starmaen.accountingstar.data.ProductEntity
import com.starmaen.accountingstar.data.ProjectEntity
import com.starmaen.accountingstar.data.ReportBundle
import com.starmaen.accountingstar.data.Roles
import com.starmaen.accountingstar.data.StockRow
import com.starmaen.accountingstar.data.SupplierEntity
import com.starmaen.accountingstar.data.UserEntity
import com.starmaen.accountingstar.data.WarehouseEntity
import com.starmaen.accountingstar.data.WarehouseStockRow
import com.starmaen.accountingstar.print.ReceiptInfo
import com.starmaen.accountingstar.print.ReceiptLine
import com.starmaen.accountingstar.print.ReceiptPrinter
import com.starmaen.accountingstar.print.ReceiptText
import com.starmaen.accountingstar.util.Dates
import com.starmaen.accountingstar.util.Money
import com.starmaen.accountingstar.util.Security
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Dest(val label: String) {
    DASHBOARD("الرئيسية"),
    POS("نقطة البيع"),
    PURCHASE("المشتريات"),
    PRODUCTS("المنتجات"),
    INVENTORY("المخزون"),
    INVOICES("الفواتير"),
    CUSTOMERS("العملاء"),
    SUPPLIERS("الموردون"),
    INSTALLMENTS("الأقساط"),
    REPORTS("التقارير"),
    SETTINGS("الإعدادات"),
    PROJECTS("المشاريع")
}

data class CartItem(
    val product: ProductEntity,
    val qty: Double,
    val unitPrice: Double
) {
    val total: Double get() = Money.round(qty * unitPrice)
}

data class AppUiState(
    val loading: Boolean = true,
    val currentUser: UserEntity? = null,
    val currentProject: ProjectEntity? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val warehouses: List<WarehouseEntity> = emptyList(),
    val categories: List<com.starmaen.accountingstar.data.CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val stockRows: List<StockRow> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val invoices: List<com.starmaen.accountingstar.data.InvoiceEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val lowStock: List<WarehouseStockRow> = emptyList(),
    val dueInstallments: List<InstallmentRow> = emptyList(),
    val report: ReportBundle? = null,
    val activeBranchId: Long = 0,
    val activeWarehouseId: Long = 0,
    val activeBranchName: String = "",
    val activeWarehouseName: String = "",
    val cart: List<CartItem> = emptyList(),
    val priceTier: String = PriceTier.RETAIL,
    val paymentType: String = PaymentType.CASH,
    val selectedCustomer: CustomerEntity? = null,
    val selectedSupplier: SupplierEntity? = null,
    val invoiceDetail: InvoiceDetail? = null,
    val receiptPreview: String? = null,
    val pendingPrintId: Long? = null,
    val message: String? = null
) {
    val cartTotal: Double get() = Money.round(cart.sumOf { it.total })
    val cartCount: Double get() = cart.sumOf { it.qty }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AccountingStarApp
    val repo = app.repository
    val session = app.session

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() = viewModelScope.launch {
        runCatching {
            repo.ensureSeeded()
            val projects = repo.projects.all()
            val user = session.userId.takeIf { it > 0 }?.let { repo.userById(it) }
            _state.update { it.copy(projects = projects, currentUser = user) }
            if (user != null) {
                val projectId = session.projectId.takeIf { id -> projects.any { it.id == id } }
                    ?: user.projectId.takeIf { id -> id > 0 && projects.any { it.id == id } }
                    ?: projects.firstOrNull()?.id
                    ?: 0L
                if (projectId > 0) loadProjectData(projectId)
                else _state.update { it.copy(loading = false) }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }.onFailure { fail(it) }
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        if (username.isBlank() || password.isBlank()) {
            message("أدخل اسم المستخدم وكلمة المرور")
            return@launch
        }
        runCatching {
            val user = repo.login(username, password)
                ?: error("اسم المستخدم أو كلمة المرور غير صحيحة")
            session.userId = user.id
            val projects = repo.projects.all()
            val projectId = user.projectId.takeIf { id -> id > 0 && projects.any { it.id == id } }
                ?: projects.firstOrNull()?.id ?: 0L
            _state.update { it.copy(currentUser = user, projects = projects, loading = false) }
            if (projectId > 0) loadProjectData(projectId)
        }.onFailure { fail(it) }
    }

    fun logout() {
        session.clearLogin()
        _state.value = AppUiState(loading = false, projects = _state.value.projects)
    }

    fun selectProject(projectId: Long) = viewModelScope.launch {
        runCatching { loadProjectData(projectId) }.onFailure { fail(it) }
    }

    fun refresh() = viewModelScope.launch {
        val id = _state.value.currentProject?.id ?: return@launch
        runCatching { loadProjectData(id) }.onFailure { fail(it) }
    }

    private suspend fun loadProjectData(projectId: Long) {
        val project = repo.projects.byId(projectId) ?: return
        val branches = repo.branches.byProject(projectId)
        val warehouses = repo.warehouses.byProject(projectId)
        val branchId = session.branchId.takeIf { id -> branches.any { it.id == id } }
            ?: branches.firstOrNull()?.id ?: 0L
        val warehouseId = session.warehouseId.takeIf { id ->
            warehouses.any { it.id == id && it.branchId == branchId }
        } ?: warehouses.firstOrNull { it.branchId == branchId }?.id
            ?: warehouses.firstOrNull()?.id ?: 0L
        session.projectId = projectId
        session.branchId = branchId
        session.warehouseId = warehouseId
        val nowEnd = Dates.dayEnd()
        val stockRows = if (warehouseId > 0) repo.stock.rows(warehouseId, "") else emptyList()
        val projects = repo.projects.all()
        val categories = repo.categories.byProject(projectId)
        val products = repo.products.all(projectId)
        val customers = repo.customers.search(projectId, "")
        val suppliers = repo.suppliers.search(projectId, "")
        val invoices = repo.invoices.list(projectId, "")
        val users = repo.users.all()
        val stats = repo.dashboard(projectId)
        val lowStock = repo.stock.lowStock(projectId)
        val dueInstallments = repo.invoices.dueInstallments(projectId, nowEnd)
        _state.update {
            it.copy(
                loading = false,
                currentProject = project,
                projects = projects,
                branches = branches,
                warehouses = warehouses,
                activeBranchId = branchId,
                activeWarehouseId = warehouseId,
                activeBranchName = branches.firstOrNull { b -> b.id == branchId }?.name ?: "",
                activeWarehouseName = warehouses.firstOrNull { w -> w.id == warehouseId }?.name ?: "",
                categories = categories,
                products = products,
                stockRows = stockRows,
                customers = customers,
                suppliers = suppliers,
                invoices = invoices,
                users = users,
                stats = stats,
                lowStock = lowStock,
                dueInstallments = dueInstallments,
                cart = emptyList(),
                selectedCustomer = null,
                selectedSupplier = null
            )
        }
    }

    fun setActiveBranch(branchId: Long) = viewModelScope.launch {
        runCatching {
            session.branchId = branchId
            val wh = repo.warehouses.byProject(session.projectId).firstOrNull { it.branchId == branchId }
            session.warehouseId = wh?.id ?: session.warehouseId
            loadProjectData(session.projectId)
        }.onFailure { fail(it) }
    }

    fun setActiveWarehouse(warehouseId: Long) = viewModelScope.launch {
        runCatching {
            session.warehouseId = warehouseId
            loadProjectData(session.projectId)
        }.onFailure { fail(it) }
    }

    fun setPriceTier(tier: String) {
        _state.update { it.copy(priceTier = tier) }
    }

    fun setPaymentType(type: String) {
        _state.update { it.copy(paymentType = type) }
    }

    fun setCustomer(customer: CustomerEntity?) {
        _state.update { it.copy(selectedCustomer = customer) }
    }

    fun setSupplier(supplier: SupplierEntity?) {
        _state.update { it.copy(selectedSupplier = supplier) }
    }

    fun addToCart(product: ProductEntity, qty: Double = 1.0) {
        val tier = _state.value.priceTier
        _state.update { current ->
            val existing = current.cart.firstOrNull { it.product.id == product.id }
            val cart = if (existing == null) {
                current.cart + CartItem(product, qty, product.priceFor(tier))
            } else {
                current.cart.map {
                    if (it.product.id == product.id) it.copy(qty = it.qty + qty) else it
                }
            }
            current.copy(cart = cart)
        }
    }

    fun updateCartQty(productId: Long, qty: Double) {
        if (qty <= 0) {
            removeFromCart(productId)
            return
        }
        _state.update { current ->
            current.copy(cart = current.cart.map {
                if (it.product.id == productId) it.copy(qty = qty) else it
            })
        }
    }

    fun removeFromCart(productId: Long) {
        _state.update { current -> current.copy(cart = current.cart.filter { it.product.id != productId }) }
    }

    fun clearCart() = _state.update { it.copy(cart = emptyList()) }

    fun checkout(
        paid: Double,
        discount: Double,
        taxRate: Double,
        installmentCount: Int
    ) = viewModelScope.launch {
        val current = _state.value
        if (current.cart.isEmpty()) {
            message("أضف صنفاً واحداً على الأقل")
            return@launch
        }
        if (current.paymentType != PaymentType.CASH && current.selectedCustomer == null) {
            message("اختر عميلاً للبيع بالدين أو التقسيط")
            return@launch
        }
        if (current.paymentType == PaymentType.INSTALLMENT && installmentCount < 2) {
            message("عدد الأقساط يجب أن يكون 2 أو أكثر")
            return@launch
        }
        runCatching {
            val id = withContext(Dispatchers.IO) {
                repo.createInvoice(
                    InvoiceRequest(
                        projectId = current.currentProject?.id ?: 0,
                        branchId = current.activeBranchId,
                        warehouseId = current.activeWarehouseId,
                        type = InvoiceType.SALE,
                        paymentType = current.paymentType,
                        partyType = PartyType.CUSTOMER,
                        partyId = current.selectedCustomer?.id,
                        partyName = current.selectedCustomer?.name ?: "عميل نقدي",
                        lines = current.cart.map {
                            InvoiceLineInput(
                                productId = it.product.id,
                                name = it.product.name,
                                unit = it.product.unit,
                                qty = it.qty,
                                unitPrice = it.unitPrice,
                                costPrice = it.product.costPrice
                            )
                        },
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid,
                        installmentCount = installmentCount
                    )
                )
            }
            _state.update { it.copy(pendingPrintId = id, cart = emptyList()) }
            loadProjectData(current.currentProject?.id ?: 0)
            message("تم حفظ الفاتورة بنجاح")
        }.onFailure { fail(it) }
    }

    fun createPurchase(
        supplier: SupplierEntity?,
        lines: List<InvoiceLineInput>,
        paid: Double,
        discount: Double,
        taxRate: Double
    ) = viewModelScope.launch {
        val current = _state.value
        if (lines.isEmpty()) {
            message("أضف أصنافاً للفاتورة")
            return@launch
        }
        if (supplier == null) {
            message("اختر المورد")
            return@launch
        }
        runCatching {
            val id = withContext(Dispatchers.IO) {
                repo.createInvoice(
                    InvoiceRequest(
                        projectId = current.currentProject?.id ?: 0,
                        branchId = current.activeBranchId,
                        warehouseId = current.activeWarehouseId,
                        type = InvoiceType.PURCHASE,
                        paymentType = if (paid >= lines.sumOf { it.unitPrice * it.qty }) PaymentType.CASH else PaymentType.CREDIT,
                        partyType = PartyType.SUPPLIER,
                        partyId = supplier.id,
                        partyName = supplier.name,
                        lines = lines,
                        discount = discount,
                        taxRate = taxRate,
                        paid = paid
                    )
                )
            }
            _state.update { it.copy(pendingPrintId = id) }
            loadProjectData(current.currentProject?.id ?: 0)
            message("تم حفظ فاتورة الشراء وتحديث المخزون")
        }.onFailure { fail(it) }
    }

    fun payInvoice(invoiceId: Long, amount: Double, method: String) = viewModelScope.launch {
        if (amount <= 0) {
            message("أدخل مبلغاً صحيحاً")
            return@launch
        }
        runCatching {
            withContext(Dispatchers.IO) { repo.addPayment(invoiceId, amount, method) }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            openInvoice(invoiceId)
            message("تم تسجيل الدفعة")
        }.onFailure { fail(it) }
    }

    fun openInvoice(id: Long) = viewModelScope.launch {
        runCatching {
            val invoice = withContext(Dispatchers.IO) { repo.invoices.byId(id) }
                ?: return@launch
            val items = withContext(Dispatchers.IO) { repo.invoices.items(id) }
            val payments = withContext(Dispatchers.IO) { repo.invoices.payments(id) }
            val installments = withContext(Dispatchers.IO) { repo.invoices.installments(id) }
            _state.update {
                it.copy(invoiceDetail = InvoiceDetail(invoice, items, payments, installments))
            }
        }.onFailure { fail(it) }
    }

    fun closeInvoice() = _state.update { it.copy(invoiceDetail = null) }

    fun saveProduct(product: ProductEntity, openingQty: Double = 0.0) = viewModelScope.launch {
        if (product.name.isBlank()) {
            message("اسم المنتج مطلوب")
            return@launch
        }
        runCatching {
            withContext(Dispatchers.IO) {
                repo.saveProduct(product, openingQty, _state.value.activeWarehouseId)
            }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            message("تم حفظ المنتج")
        }.onFailure { fail(it) }
    }

    fun deleteProduct(id: Long) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) { repo.deleteProduct(id) }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            message("تم تعطيل المنتج")
        }.onFailure { fail(it) }
    }

    fun saveCustomer(customer: CustomerEntity) = viewModelScope.launch {
        if (customer.name.isBlank()) {
            message("اسم العميل مطلوب")
            return@launch
        }
        runCatching {
            withContext(Dispatchers.IO) { repo.saveCustomer(customer) }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            message("تم حفظ العميل")
        }.onFailure { fail(it) }
    }

    fun saveSupplier(supplier: SupplierEntity) = viewModelScope.launch {
        if (supplier.name.isBlank()) {
            message("اسم المورد مطلوب")
            return@launch
        }
        runCatching {
            withContext(Dispatchers.IO) { repo.saveSupplier(supplier) }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            message("تم حفظ المورد")
        }.onFailure { fail(it) }
    }

    fun adjustStock(productId: Long, newQuantity: Double, note: String = "") = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                repo.adjustStock(
                    _state.value.currentProject?.id ?: 0,
                    _state.value.activeWarehouseId,
                    productId,
                    newQuantity,
                    note.ifBlank { "تعديل يدوي" }
                )
            }
            loadProjectData(_state.value.currentProject?.id ?: 0)
            message("تم تحديث المخزون")
        }.onFailure { fail(it) }
    }

    fun transferStock(productId: Long, toWarehouseId: Long, qty: Double) = viewModelScope.launch {
        val current = _state.value
        runCatching {
            withContext(Dispatchers.IO) {
                repo.transferStock(
                    current.currentProject?.id ?: 0,
                    current.activeWarehouseId,
                    toWarehouseId,
                    productId,
                    qty
                )
            }
            loadProjectData(current.currentProject?.id ?: 0)
            message("تم نقل الكمية بين المستودعين")
        }.onFailure { fail(it) }
    }

    fun addExpense(title: String, category: String, amount: Double, notes: String = "") =
        viewModelScope.launch {
            if (title.isBlank() || amount <= 0) {
                message("أدخل عنوان المصروف والمبلغ")
                return@launch
            }
            val current = _state.value
            runCatching {
                withContext(Dispatchers.IO) {
                    repo.addExpense(
                        com.starmaen.accountingstar.data.ExpenseEntity(
                            projectId = current.currentProject?.id ?: 0,
                            branchId = current.activeBranchId,
                            title = title,
                            category = category,
                            amount = amount,
                            notes = notes
                        )
                    )
                }
                loadProjectData(current.currentProject?.id ?: 0)
                val currentReport = _state.value.report
                if (currentReport != null) {
                    val refreshed = withContext(Dispatchers.IO) {
                        repo.report(current.currentProject?.id ?: 0, currentReport.from, currentReport.to)
                    }
                    _state.update { it.copy(report = refreshed) }
                }
                message("تم تسجيل المصروف")
            }.onFailure { fail(it) }
        }

    fun loadReport(from: Long, to: Long) = viewModelScope.launch {
        val projectId = _state.value.currentProject?.id ?: return@launch
        runCatching {
            val report = withContext(Dispatchers.IO) { repo.report(projectId, from, to) }
            _state.update { it.copy(report = report) }
        }.onFailure { fail(it) }
    }

    fun addProject(name: String, type: String, currency: String, taxRate: Double) =
        viewModelScope.launch {
            if (name.isBlank()) {
                message("اسم المشروع مطلوب")
                return@launch
            }
            runCatching {
                val id = withContext(Dispatchers.IO) {
                    val projectId = repo.projects.insert(
                        ProjectEntity(name = name, businessType = type, currency = currency, taxRate = taxRate)
                    )
                    val branchId = repo.branches.insert(
                        BranchEntity(projectId = projectId, name = "الفرع الرئيسي", isDefault = true)
                    )
                    repo.warehouses.insert(
                        WarehouseEntity(
                            projectId = projectId,
                            branchId = branchId,
                            name = "المستودع الرئيسي",
                            isDefault = true
                        )
                    )
                    projectId
                }
                val projects = withContext(Dispatchers.IO) { repo.projects.all() }
                _state.update { it.copy(projects = projects) }
                loadProjectData(id)
                message("تم إنشاء المشروع")
            }.onFailure { fail(it) }
        }

    fun updateProject(project: ProjectEntity) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) { repo.projects.update(project) }
            loadProjectData(project.id)
            message("تم تحديث بيانات النشاط")
        }.onFailure { fail(it) }
    }

    fun addBranch(name: String) = viewModelScope.launch {        val projectId = _state.value.currentProject?.id ?: return@launch
        if (name.isBlank()) return@launch
        runCatching {
            withContext(Dispatchers.IO) {
                repo.branches.insert(BranchEntity(projectId = projectId, name = name))
            }
            loadProjectData(projectId)
            message("تمت إضافة الفرع")
        }.onFailure { fail(it) }
    }

    fun addWarehouse(name: String, branchId: Long) = viewModelScope.launch {
        val projectId = _state.value.currentProject?.id ?: return@launch
        if (name.isBlank()) return@launch
        runCatching {
            withContext(Dispatchers.IO) {
                repo.warehouses.insert(
                    WarehouseEntity(projectId = projectId, branchId = branchId, name = name)
                )
            }
            loadProjectData(projectId)
            message("تمت إضافة المستودع")
        }.onFailure { fail(it) }
    }

    fun addUser(username: String, password: String, fullName: String, role: String) =
        viewModelScope.launch {
            if (username.isBlank() || password.length < 4) {
                message("اسم المستخدم مطلوب وكلمة المرور 4 أحرف على الأقل")
                return@launch
            }
            val projectId = _state.value.currentProject?.id ?: 0
            runCatching {
                withContext(Dispatchers.IO) {
                    val salt = Security.newSalt()
                    repo.users.insert(
                        UserEntity(
                            projectId = projectId,
                            username = username.trim(),
                            passwordHash = Security.hash(password, salt),
                            salt = salt,
                            fullName = fullName.ifBlank { username },
                            role = role
                        )
                    )
                }
                loadProjectData(projectId)
                message("تمت إضافة المستخدم")
            }.onFailure { fail(it) }
        }

    fun canManage(): Boolean {
        val role = _state.value.currentUser?.role ?: return false
        return role == Roles.ADMIN || role == Roles.MANAGER
    }

    fun showReceipt(invoiceId: Long) = viewModelScope.launch {
        runCatching {
            val info = buildReceipt(invoiceId) ?: error("الفاتورة غير موجودة")
            _state.update { it.copy(receiptPreview = ReceiptPrinter.preview(info, session.paperWidth)) }
        }.onFailure { fail(it) }
    }

    fun closeReceipt() = _state.update { it.copy(receiptPreview = null) }

    fun consumePendingPrint() = _state.update { it.copy(pendingPrintId = null) }

    fun printInvoice(context: Context, invoiceId: Long) = viewModelScope.launch {
        runCatching {
            val info = buildReceipt(invoiceId) ?: error("الفاتورة غير موجودة")
            val result = ReceiptPrinter.print(context, info, session.thermalMac, session.paperWidth)
            result.onSuccess { message(it) }.onFailure { fail(it) }
        }.onFailure { fail(it) }
    }

    fun printTest(context: Context) = viewModelScope.launch {
        runCatching {
            val info = ReceiptPrinter.testInfo(
                _state.value.currentProject?.name ?: "نجم المحاسبة",
                session.receiptFooter
            )
            ReceiptPrinter.print(context, info, session.thermalMac, session.paperWidth)
                .onSuccess { message(it) }
                .onFailure { fail(it) }
        }.onFailure { fail(it) }
    }

    fun savePrinterSettings(mac: String, paperWidth: Int, footer: String, taxNumber: String) {
        session.thermalMac = mac.trim()
        session.paperWidth = paperWidth
        session.receiptFooter = footer
        session.taxNumber = taxNumber
        message("تم حفظ إعدادات الطباعة")
    }

    fun backup(context: Context) = viewModelScope.launch {
        runCatching {
            val source = context.getDatabasePath(com.starmaen.accountingstar.data.AppDatabase.NAME)
            val target = File(context.cacheDir, "accounting-star-backup-${Dates.format(System.currentTimeMillis())}.db")
            withContext(Dispatchers.IO) {
                if (source.exists()) source.copyTo(target, overwrite = true)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                target
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, "مشاركة النسخة الاحتياطية").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            message("تم إنشاء نسخة احتياطية من قاعدة البيانات")
        }.onFailure { fail(it) }
    }

    private suspend fun buildReceipt(invoiceId: Long): ReceiptInfo? {
        val invoice = withContext(Dispatchers.IO) { repo.invoices.byId(invoiceId) } ?: return null
        val items = withContext(Dispatchers.IO) { repo.invoices.items(invoiceId) }
        val project = withContext(Dispatchers.IO) { repo.projects.byId(invoice.projectId) }
        val branch = withContext(Dispatchers.IO) {
            repo.branches.byProject(invoice.projectId).firstOrNull { it.id == invoice.branchId }
        }
        return ReceiptInfo(
            shopName = project?.name ?: "نجم المحاسبة",
            address = branch?.address?.ifBlank { project?.address ?: "" } ?: "",
            phone = branch?.phone?.ifBlank { project?.phone ?: "" } ?: "",
            taxNumber = session.taxNumber,
            invoiceNo = invoice.invoiceNo,
            dateText = Dates.formatDateTime(invoice.date),
            invoiceType = invoice.type,
            paymentType = invoice.paymentType,
            partyLabel = if (invoice.partyType == PartyType.CUSTOMER) "العميل" else "المورد",
            partyName = invoice.partyName,
            cashier = _state.value.currentUser?.fullName ?: "",
            lines = items.map {
                ReceiptLine(it.name, it.qty, it.unitPrice, it.lineTotal)
            },
            subtotal = invoice.subtotal,
            discount = invoice.discount,
            taxAmount = invoice.taxAmount,
            total = invoice.total,
            paid = invoice.paid,
            remaining = invoice.remaining,
            footer = session.receiptFooter,
            currency = project?.currency ?: "ر.س"
        )
    }

    fun message(text: String) = _state.update { it.copy(message = text) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun fail(t: Throwable) {
        _state.update { it.copy(message = t.message ?: "حدث خطأ غير متوقع", loading = false) }
    }
}
