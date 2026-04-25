package vn.edu.dlu.slib

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

	private val channelName = "slib/hce"

	override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
		super.configureFlutterEngine(flutterEngine)

		MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
			.setMethodCallHandler { call, result ->
				when (call.method) {
					"setUserId" -> {
						val userId = call.argument<String>("userId") ?: ""
						saveUserId(userId)
						result.success(null)
					}
					"clearUserId" -> {
						clearUserId()
						result.success(null)
					}
					"isNfcEnabled" -> result.success(isNfcEnabled())
					"isDefaultPaymentService" -> result.success(isDefaultPaymentService())
					"requestDefaultPaymentService" -> result.success(requestDefaultPaymentService())
					"openNfcPaymentSettings" -> result.success(openNfcPaymentSettings())
					"openNfcSettings" -> result.success(openNfcSettings())
					else -> result.notImplemented()
				}
			}
	}

	private fun saveUserId(userId: String) {
		val prefs = getSharedPreferences("hce_prefs", Context.MODE_PRIVATE)
		prefs.edit().putString("HCE_USER_ID", userId).apply()
	}

	private fun clearUserId() {
		val prefs = getSharedPreferences("hce_prefs", Context.MODE_PRIVATE)
		prefs.edit().remove("HCE_USER_ID").apply()
	}

	private fun hceComponentName(): ComponentName {
		return ComponentName(this, MyHostApduService::class.java)
	}

	private fun isNfcEnabled(): Boolean {
		val adapter = NfcAdapter.getDefaultAdapter(this) ?: return false
		return adapter.isEnabled
	}

	private fun isDefaultPaymentService(): Boolean {
		val adapter = NfcAdapter.getDefaultAdapter(this) ?: return false
		val cardEmulation = CardEmulation.getInstance(adapter)
		return cardEmulation.isDefaultServiceForCategory(
			hceComponentName(),
			CardEmulation.CATEGORY_PAYMENT
		)
	}

	private fun requestDefaultPaymentService(): Boolean {
		val adapter = NfcAdapter.getDefaultAdapter(this) ?: return false
		if (!adapter.isEnabled) {
			return openNfcSettings()
		}

		if (isDefaultPaymentService()) return true

		val intent = Intent(CardEmulation.ACTION_CHANGE_DEFAULT).apply {
			putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
			putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, hceComponentName())
		}

		return startIntent(intent) || openNfcPaymentSettings()
	}

	private fun openNfcPaymentSettings(): Boolean {
		return startIntent(Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)) ||
			openNfcSettings()
	}

	private fun openNfcSettings(): Boolean {
		return startIntent(Intent(Settings.ACTION_NFC_SETTINGS)) ||
			startIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS))
	}

	private fun startIntent(intent: Intent): Boolean {
		return try {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			startActivity(intent)
			true
		} catch (_: ActivityNotFoundException) {
			false
		} catch (_: Exception) {
			false
		}
	}
}
