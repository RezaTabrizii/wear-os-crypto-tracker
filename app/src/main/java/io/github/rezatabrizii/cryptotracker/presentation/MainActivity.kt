package io.github.rezatabrizii.cryptotracker.presentation

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import io.github.rezatabrizii.cryptotracker.data.PriceFormat
import io.github.rezatabrizii.cryptotracker.data.PriceRepository
import io.github.rezatabrizii.cryptotracker.data.PriceSnapshot
import io.github.rezatabrizii.cryptotracker.data.PriceStore
import kotlinx.coroutines.launch

private val UsdtGreen = Color(0xFF26A17B)
private const val STALE_ON_OPEN_MILLIS = 5 * 60 * 1000L

data class PriceUiState(
    val snapshot: PriceSnapshot? = null,
    val error: String? = null,
    val loading: Boolean = false,
)

class MainActivity : ComponentActivity() {
    private lateinit var store: PriceStore
    private var uiState by mutableStateOf(PriceUiState())

    // Reflects updates written by the background worker while the screen is open.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reloadFromStore() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PriceStore(this)
        setContent {
            MaterialTheme {
                PriceScreen(state = uiState, onRefresh = ::refresh)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        store.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        reloadFromStore()
        val age = System.currentTimeMillis() - (uiState.snapshot?.fetchedAtMillis ?: 0L)
        if (age > STALE_ON_OPEN_MILLIS) refresh()
    }

    override fun onStop() {
        store.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onStop()
    }

    private fun reloadFromStore() {
        uiState = uiState.copy(snapshot = store.snapshot(), error = store.lastError())
    }

    private fun refresh() {
        if (uiState.loading) return
        uiState = uiState.copy(loading = true)
        lifecycleScope.launch {
            PriceRepository.refresh(this@MainActivity)
            uiState = uiState.copy(loading = false)
            reloadFromStore()
        }
    }
}

@Composable
private fun PriceScreen(state: PriceUiState, onRefresh: () -> Unit) {
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "USDT", color = UsdtGreen, style = MaterialTheme.typography.title3)
            Text(
                text = state.snapshot?.let { PriceFormat.full(it.toman) } ?: "--",
                style = MaterialTheme.typography.display3,
                maxLines = 1,
            )
            Text(text = "Toman", style = MaterialTheme.typography.caption2)
            Spacer(Modifier.height(4.dp))
            state.snapshot?.let {
                Text(
                    text = "${it.source} · ${PriceFormat.time(it.fetchedAtMillis)}",
                    color = MaterialTheme.colors.onSurfaceVariant,
                    style = MaterialTheme.typography.caption3,
                )
            }
            if (state.error != null && !state.loading) {
                Text(
                    text = if (state.snapshot == null) state.error else "Update failed",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption3,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            CompactChip(
                onClick = onRefresh,
                enabled = !state.loading,
                label = { Text(if (state.loading) "Updating…" else "Refresh") },
            )
        }
    }
}
