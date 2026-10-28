package com.accountingstar.repositories

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Account::class, Warehouse::class, Product::class], version = 1)
abstrclass AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun productDao(): ProductDao

    companion object {
        fun buildDatabase(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "accounting-db").build()
    }
}