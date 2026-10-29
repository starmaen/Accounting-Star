package com.starmaen.accountingstar

import android.app.Application
import com.starmaen.accountingstar.data.AppDatabase
import com.starmaen.accountingstar.data.Repository
import com.starmaen.accountingstar.data.SessionStore

class AccountingStarApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: Repository by lazy { Repository(database) }
    val session: SessionStore by lazy { SessionStore(this) }
}
