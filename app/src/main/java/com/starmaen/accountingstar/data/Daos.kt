package com.starmaen.accountingstar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

data class StockRow(
    val productId: Long,
    val name: String,
    val barcode: String,
    val unit: String,
    val quantity: Double,
    val minQty: Double,
    val costPrice: Double,
    val retailPrice: Double,
    val wholesalePrice: Double,
    val halfWholesalePrice: Double,
    val categoryName: String
)

data class TopProductRow(val name: String, val qty: Double, val total: Double)

data class WarehouseStockRow(
    val productId: Long,
    val name: String,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: Double,
    val minQty: Double
)

data class InstallmentRow(
    val id: Long,
    val invoiceId: Long,
    val invoiceNo: String,
    val customerName: String,
    val seq: Int,
    val dueDate: Long,
    val amount: Double,
    val paidAmount: Double,
    val status: String
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id")
    suspend fun all(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun byId(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE projectId = :projectId ORDER BY isDefault DESC, id")
    suspend fun byProject(projectId: Long): List<BranchEntity>

    @Insert
    suspend fun insert(branch: BranchEntity): Long

    @Update
    suspend fun update(branch: BranchEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND active = 1 LIMIT 1")
    suspend fun byUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY id")
    suspend fun all(): List<UserEntity>

    @Query("SELECT * FROM users WHERE projectId = :projectId OR projectId = 0 ORDER BY id")
    suspend fun forProject(projectId: Long): List<UserEntity>

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses WHERE projectId = :projectId ORDER BY isDefault DESC, id")
    suspend fun byProject(projectId: Long): List<WarehouseEntity>

    @Insert
    suspend fun insert(warehouse: WarehouseEntity): Long

    @Update
    suspend fun update(warehouse: WarehouseEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY name")
    suspend fun byProject(projectId: Long): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long
}

@Dao
interface ProductDao {
    @Query(
        """SELECT * FROM products WHERE projectId = :projectId AND active = 1
        AND (:q = '' OR name LIKE '%' || :q || '%' OR barcode LIKE '%' || :q || '%' OR sku LIKE '%' || :q || '%')
        ORDER BY name"""
    )
    suspend fun search(projectId: Long, q: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE projectId = :projectId ORDER BY name")
    suspend fun all(projectId: Long): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE projectId = :projectId AND barcode = :barcode AND barcode <> '' LIMIT 1")
    suspend fun byBarcode(projectId: Long, barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}

@Dao
interface StockDao {
    @Query(
        """SELECT s.productId AS productId, p.name AS name, p.barcode AS barcode, p.unit AS unit,
        s.quantity AS quantity, p.minQty AS minQty, p.costPrice AS costPrice,
        p.retailPrice AS retailPrice, p.wholesalePrice AS wholesalePrice,
        p.halfWholesalePrice AS halfWholesalePrice, COALESCE(c.name, '') AS categoryName
        FROM stock s INNER JOIN products p ON p.id = s.productId
        LEFT JOIN categories c ON c.id = p.categoryId
        WHERE s.warehouseId = :warehouseId AND p.active = 1
        AND (:q = '' OR p.name LIKE '%' || :q || '%' OR p.barcode LIKE '%' || :q || '%')
        ORDER BY p.name"""
    )
    suspend fun rows(warehouseId: Long, q: String): List<StockRow>

    @Query("SELECT * FROM stock WHERE warehouseId = :warehouseId AND productId = :productId LIMIT 1")
    suspend fun find(warehouseId: Long, productId: Long): StockEntity?

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE productId = :productId")
    suspend fun totalForProduct(productId: Long): Double

    @Insert
    suspend fun insert(stock: StockEntity): Long

    @Update
    suspend fun update(stock: StockEntity)

    @Query(
        """SELECT s.productId AS productId, p.name AS name, s.warehouseId AS warehouseId,
        w.name AS warehouseName, s.quantity AS quantity, p.minQty AS minQty
        FROM stock s INNER JOIN products p ON p.id = s.productId
        INNER JOIN warehouses w ON w.id = s.warehouseId
        WHERE p.projectId = :projectId AND p.active = 1 AND s.quantity <= p.minQty
        ORDER BY s.quantity"""
    )
    suspend fun lowStock(projectId: Long): List<WarehouseStockRow>
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE projectId = :projectId AND active = 1 AND (:q = '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%') ORDER BY name")
    suspend fun search(projectId: Long, q: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun byId(id: Long): CustomerEntity?

    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = balance + :delta WHERE id = :id")
    suspend fun addBalance(id: Long, delta: Double)

    @Query("SELECT COALESCE(SUM(balance), 0) FROM customers WHERE projectId = :projectId AND active = 1")
    suspend fun totalBalance(projectId: Long): Double
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE projectId = :projectId AND active = 1 AND (:q = '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%') ORDER BY name")
    suspend fun search(projectId: Long, q: String): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun byId(id: Long): SupplierEntity?

    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET balance = balance + :delta WHERE id = :id")
    suspend fun addBalance(id: Long, delta: Double)

    @Query("SELECT COALESCE(SUM(balance), 0) FROM suppliers WHERE projectId = :projectId AND active = 1")
    suspend fun totalBalance(projectId: Long): Double
}

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun byId(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE projectId = :projectId AND (:type = '' OR type = :type) ORDER BY date DESC, id DESC")
    suspend fun list(projectId: Long, type: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE projectId = :projectId AND partyType = :partyType AND partyId = :partyId ORDER BY date DESC")
    suspend fun forParty(projectId: Long, partyType: String, partyId: Long): List<InvoiceEntity>

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id")
    suspend fun items(invoiceId: Long): List<InvoiceItemEntity>

    @Insert
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY date")
    suspend fun payments(invoiceId: Long): List<PaymentEntity>

    @Insert
    suspend fun insertInstallments(items: List<InstallmentEntity>)

    @Query("SELECT * FROM installments WHERE invoiceId = :invoiceId ORDER BY seq")
    suspend fun installments(invoiceId: Long): List<InstallmentEntity>

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query(
        """SELECT i.id AS id, i.invoiceId AS invoiceId, v.invoiceNo AS invoiceNo,
        v.partyName AS customerName, i.seq AS seq, i.dueDate AS dueDate, i.amount AS amount,
        i.paidAmount AS paidAmount, i.status AS status
        FROM installments i INNER JOIN invoices v ON v.id = i.invoiceId
        WHERE i.projectId = :projectId AND i.status <> 'PAID' AND i.dueDate <= :until
        ORDER BY i.dueDate"""
    )
    suspend fun dueInstallments(projectId: Long, until: Long): List<InstallmentRow>

    @Query("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE projectId = :projectId AND type = :type AND date BETWEEN :from AND :to")
    suspend fun totalBetween(projectId: Long, type: String, from: Long, to: Long): Double

    @Query("SELECT COALESCE(SUM(remaining), 0) FROM invoices WHERE projectId = :projectId AND type = 'SALE'")
    suspend fun receivables(projectId: Long): Double

    @Query("SELECT COALESCE(SUM(remaining), 0) FROM invoices WHERE projectId = :projectId AND type = 'PURCHASE'")
    suspend fun payables(projectId: Long): Double

    @Query(
        """SELECT COALESCE(SUM(ii.lineTotal - (ii.costPrice * ii.qty)), 0)
        FROM invoice_items ii INNER JOIN invoices i ON i.id = ii.invoiceId
        WHERE i.projectId = :projectId AND i.type = 'SALE' AND i.date BETWEEN :from AND :to"""
    )
    suspend fun profitBetween(projectId: Long, from: Long, to: Long): Double

    @Query(
        """SELECT ii.name AS name, SUM(ii.qty) AS qty, SUM(ii.lineTotal) AS total
        FROM invoice_items ii INNER JOIN invoices i ON i.id = ii.invoiceId
        WHERE i.projectId = :projectId AND i.type = 'SALE' AND i.date BETWEEN :from AND :to
        GROUP BY ii.name ORDER BY qty DESC LIMIT 10"""
    )
    suspend fun topProducts(projectId: Long, from: Long, to: Long): List<TopProductRow>

    @Query("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE projectId = :projectId AND type = 'SALE' AND date >= :from")
    suspend fun salesFrom(projectId: Long, from: Long): Double

    @Query("SELECT COUNT(*) FROM invoices WHERE projectId = :projectId AND type = :type")
    suspend fun countByType(projectId: Long, type: String): Int
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun between(projectId: Long, from: Long, to: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE projectId = :projectId AND date BETWEEN :from AND :to")
    suspend fun totalBetween(projectId: Long, from: Long, to: Long): Double
}

@Dao
interface StockMoveDao {
    @Insert
    suspend fun insert(move: StockMoveEntity): Long

    @Query("SELECT * FROM stock_moves WHERE projectId = :projectId AND productId = :productId ORDER BY date DESC LIMIT 50")
    suspend fun forProduct(projectId: Long, productId: Long): List<StockMoveEntity>
}

@Dao
interface TransferDao {
    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Query("SELECT * FROM transfers WHERE projectId = :projectId ORDER BY date DESC LIMIT 100")
    suspend fun byProject(projectId: Long): List<TransferEntity>
}
