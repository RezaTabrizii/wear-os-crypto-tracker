package io.github.rezatabrizii.cryptotracker.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.rezatabrizii.cryptotracker.data.PriceRepository
import io.github.rezatabrizii.cryptotracker.data.PriceStore
import java.util.concurrent.TimeUnit

class PriceRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (PriceRepository.refresh(applicationContext)) return Result.success()
        // One quick retry for transient Bluetooth/network hiccups; otherwise wait for the next period.
        return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_RETRIES = 1
    }
}

object RefreshScheduler {
    private const val PERIODIC_WORK = "price-refresh-periodic"
    private const val ONE_TIME_WORK = "price-refresh-now"

    /** 15 minutes is the minimum Android allows for periodic work. */
    private const val INTERVAL_MINUTES = 15L

    /** A tile/complication/app shown with data older than this triggers one background refresh. */
    private const val STALE_AFTER_MILLIS = 20 * 60 * 1000L

    // Battery: run only when a network (phone via Bluetooth, or Wi-Fi) is available and the battery is not low.
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Idempotent: KEEP leaves an existing schedule untouched. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<PriceRefreshWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun refreshIfStale(context: Context) {
        val fetchedAt = PriceStore(context).snapshot()?.fetchedAtMillis ?: 0L
        if (System.currentTimeMillis() - fetchedAt < STALE_AFTER_MILLIS) return
        val request = OneTimeWorkRequestBuilder<PriceRefreshWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, request)
    }
}
