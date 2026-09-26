package io.github.rezatabrizii.cryptotracker

import android.app.Application
import io.github.rezatabrizii.cryptotracker.work.RefreshScheduler

class CryptoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Runs whenever the process starts (app, tile or complication), so the schedule survives updates.
        RefreshScheduler.schedulePeriodic(this)
    }
}
