package com.starmaen.accountingstar.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object InvoiceType {
    const val SALE = "SALE"
    const val PURCHASE = "PURCHASE"
    const val SALE_RETURN = "SALE_RETURN"
    const val PURCHASE_RETURN = "PURCHASE_RETURN"
}

object PaymentType {
    const val CASH = "CASH"
    const val CREDIT = "CREDIT"
    const val INSTALLMENT = "INSTALLMENT"
}

object PartyType {
    const val CUSTOMER = "CUSTOMER"
    const val SUPPLIER = "SUPPLIER"
}

object Roles {
    const val ADMIN = "ADMIN"
    const val MANAGER = "MANAGER"
    const val CASHIER = "CASHIER"
}

object InvoiceStatus {
    const val PAID = "PAID"
    const val PARTIAL = "PARTIAL"
    const val UNPAID = "UNPAID"
}

object PriceTier {
    const val RETAIL = "RETAIL"
    const val WHOLESALE = "WHOLESALE"
    const val HALF_WHOLESALE = "HALF_WHOLESALE"
}

object StockReason {
    const val SALE = "SALE"
    const val PURCHASE = "PURCHASE"
    const val SALE_RETURN = "SALE_RETURN"
    const val PURCHASE_RETURN = "PURCHASE_RETURN"
    const val ADJUST = "ADJUST"
    const val TRANSFER_IN = "TRANSFER_IN"
    const val TRANSFER_OUT = "TRANSFER_OUT"
    const val OPENING = "OPENING"
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val businessType: String = "GENERAL",
    val currency: String = "ر.س",
    val taxRate: Double = 0.0,
    val phone: String = "",
    val address: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "branches", indices = [Index("projectId")])
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String,
    val role: String = Roles.CASHIER,
    val active: Boolean = true
)

@Entity(tableName = "warehouses", indices = [Index("projectId"), Index("branchId")])
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val branchId: Long,
    val name: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "categories", indices = [Index("projectId")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String
)

@Entity(tableName = "products", indices = [Index("projectId"), Index("barcode")])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val barcode: String = "",
    val sku: String = "",
    val categoryId: Long? = null,
    val unit: String = "قطعة",
    val costPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val halfWholesalePrice: Double = 0.0,
    val retailPrice: Double = 0.0,
    val minQty: Double = 0.0,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun priceFor(tier: String): Double = when (tier) {
        PriceTier.WHOLESALE -> wholesalePrice.takeIf { it > 0 } ?: retailPrice
        PriceTier.HALF_WHOLESALE -> halfWholesalePrice.takeIf { it > 0 } ?: retailPrice
        else -> retailPrice.takeIf { it > 0 } ?: costPrice
    }
}

@Entity(
    tableName = "stock",
    indices = [Index(value = ["warehouseId", "productId"], unique = true), Index("productId")]
)
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val warehouseId: Long,
    val productId: Long,
    val quantity: Double = 0.0
)

@Entity(tableName = "customers", indices = [Index("projectId")])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val type: String = "RETAIL",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val balance: Double = 0.0,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers", indices = [Index("projectId")])
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0,
    val balance: Double = 0.0,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    indices = [Index("projectId"), Index("partyId"), Index("date"), Index("type")]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val branchId: Long,
    val warehouseId: Long,
    val invoiceNo: String,
    val type: String,
    val paymentType: String,
    val partyType: String,
    val partyId: Long?,
    val partyName: String = "",
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val remaining: Double = 0.0,
    val status: String = InvoiceStatus.UNPAID,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoice_items", indices = [Index("invoiceId"), Index("productId")])
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val name: String,
    val unit: String = "قطعة",
    val qty: Double = 1.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val lineTotal: Double = 0.0
)

@Entity(tableName = "payments", indices = [Index("invoiceId"), Index("partyId")])
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val invoiceId: Long,
    val partyType: String,
    val partyId: Long?,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val method: String = "CASH",
    val notes: String = ""
)

@Entity(tableName = "installments", indices = [Index("invoiceId"), Index("dueDate")])
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val invoiceId: Long,
    val seq: Int,
    val dueDate: Long,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val status: String = InvoiceStatus.UNPAID
)

@Entity(tableName = "stock_moves", indices = [Index("productId"), Index("warehouseId"), Index("date")])
data class StockMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val warehouseId: Long,
    val productId: Long,
    val qty: Double,
    val reason: String,
    val refId: Long? = null,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses", indices = [Index("projectId"), Index("date")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val branchId: Long,
    val title: String,
    val category: String = "تشغيلية",
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "transfers", indices = [Index("projectId"), Index("date")])
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val fromWarehouseId: Long,
    val toWarehouseId: Long,
    val productId: Long,
    val qty: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
