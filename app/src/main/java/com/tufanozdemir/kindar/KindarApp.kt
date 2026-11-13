package com.tufanozdemir.kindar

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.tufanozdemir.kindar.contacts.ContactsSync
import com.tufanozdemir.kindar.data.KindarDatabase
import com.tufanozdemir.kindar.data.KindarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Basit bağımlılık kabı. Tek kullanıcılı, sunucusuz bir uygulama için DI kütüphanesine gerek yok. */
class KindarApp : Application() {

    lateinit var repository: KindarRepository
        private set
    lateinit var contactsSync: ContactsSync
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var observerRegistered = false
    private var pendingSync: Job? = null

    override fun onCreate() {
        super.onCreate()
        val db = KindarDatabase.create(this)
        repository = KindarRepository(db)
        contactsSync = ContactsSync(this, db.dao())
        startContactSyncIfAllowed()
    }

    /**
     * İzin varsa rehberi hemen eşitler ve rehberdeki değişiklikleri dinlemeye başlar.
     * İzin sonradan verildiğinde de çağrılır.
     */
    fun startContactSyncIfAllowed() {
        if (!contactsSync.hasPermission()) return
        appScope.launch { runCatching { contactsSync.sync() } }
        if (!observerRegistered) {
            observerRegistered = true
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        // Rehber değişiklikleri art arda gelir; kısa bir bekleme ile tek senkronizasyona indir.
                        pendingSync?.cancel()
                        pendingSync = appScope.launch {
                            delay(3_000)
                            runCatching { contactsSync.sync() }
                        }
                    }
                },
            )
        }
    }
}
