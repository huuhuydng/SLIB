package vn.edu.dlu.slib

import android.content.Context
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
}
