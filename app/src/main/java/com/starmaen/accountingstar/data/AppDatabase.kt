package com.starmaen.accountingstar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        BranchEntity::class,
        UserEntity::class,
        WarehouseEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        StockEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentEntity::class,
        InstallmentEntity::class,
        StockMoveEntity::class,
        ExpenseEntity::class,
        TransferEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun branchDao(): BranchDao
    abstract fun userDao(): UserDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun stockMoveDao(): StockMoveDao
    abstract fun transferDao(): TransferDao

    companion object {
        const val NAME = "accounting_star.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
