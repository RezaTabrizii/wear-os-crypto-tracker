# Wear OS Crypto Tracker

A small, battery-friendly Wear OS app that shows the **USDT price in Toman**.
Built for Samsung Galaxy Watch 7 (works on any Wear OS 3+ watch, API 30+).

It gives you three ways to see the price:

| Surface | What it shows | How to add it |
|---|---|---|
| **App** | Price, source, last update time, *Refresh* button | Open "Crypto Tracker" from the app list |
| **Tile** | Price + update time; tap to open the app | Swipe right from watch face → add tile → "USDT price" |
| **Complication** | Short: `112.4K` · Long: `112,350 T` | Long-press watch face → Customize → pick a slot → "USDT (Toman)" |

## How it works

- **Price sources** (tried in order, first success wins):
  1. Nobitex – `usdt-rls` (Rial ÷ 10 = Toman)
  2. Wallex – `USDTTMN` (Toman)
  3. Bitpin – `USDT_IRT` (Toman)

  The app shows which source was used.
- **Internet**: the watch uses the paired phone's internet automatically (Bluetooth), or Wi‑Fi/LTE when available. No phone app is needed.
- **Battery**:
  - Background refresh every **15 minutes** (Android's minimum) with WorkManager, only when a network is available and the battery is **not low**.
  - The tile and complication never poll and never use the network; they show the cached price and are updated by push after each successful fetch.
  - The complication is only redrawn when the price actually changes.
  - A single extra refresh happens when you look at the app/tile/complication and the data is older than ~20 minutes.

## Build (no PC needed)

The APK is built by GitHub Actions (`.github/workflows/build.yml`):

- Every push builds and tests the app; the APK is attached to the run as an artifact (zip).
- For a direct download link: **Actions → Build APK → Run workflow**. This creates a GitHub Release containing `crypto-tracker-1.0.N.apk`.

## Install on the watch (phone only)

1. **Watch**: Settings → About watch → Software information → tap *Software version* until developer mode is on.
2. **Watch**: Settings → Developer options → enable **ADB debugging** and **Wireless debugging**.
3. Phone and watch on the **same Wi‑Fi**.
4. **Phone**: install **Bugjaeger** or **Wear Installer 2** from Google Play.
5. **Watch**: Wireless debugging → *Pair new device* → enter the IP, port and pairing code in the phone app.
6. Download the APK from the GitHub Release on your phone and install it with the phone app.
7. Turn **Wireless debugging off** afterwards (it drains the battery).

## Updating

Each build has a higher version number, so installing a new APK over the old one works **if both were signed with the same key**.

- Without your own key (default), each CI build is signed with a different temporary debug key → **uninstall the old version first**. You only lose the cached price.
- To enable in-place updates, add these repository secrets (Settings → Secrets and variables → Actions):
  `KEYSTORE_BASE64` (base64 of a `.jks` file), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
  Never commit the keystore itself — this repository is public.

## Project layout

```
app/src/main/java/io/github/rezatabrizii/cryptotracker/
├── CryptoApp.kt                  # schedules the periodic refresh
├── data/
│   ├── PriceSource.kt            # Nobitex / Wallex / Bitpin endpoints + parsers
│   ├── PriceFetcher.kt           # fallback chain + HttpURLConnection
│   ├── PriceRepository.kt        # fetch → store → push to tile/complication
│   ├── PriceStore.kt             # SharedPreferences cache
│   └── PriceFormat.kt            # 112,350 / 112.4K / HH:mm
├── work/PriceRefreshWorker.kt    # WorkManager worker + scheduler
├── presentation/MainActivity.kt  # Compose for Wear OS screen
├── tile/PriceTileService.kt
└── complication/PriceComplicationService.kt
```

Unit tests (`app/src/test`) cover all three parsers, the fallback order and number formatting.
