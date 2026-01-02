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
					"setStudentCode" -> {
						val code = call.argument<String>("code") ?: ""
						saveStudentCode(code)
						result.success(null)
					}
					"clearStudentCode" -> {
						clearStudentCode()
						result.success(null)
					}
					else -> result.notImplemented()
				}
			}
	}

	private fun saveStudentCode(code: String) {
		val prefs = getSharedPreferences("hce_prefs", Context.MODE_PRIVATE)
		prefs.edit().putString("HCE_STUDENT_CODE", code).apply()
	}

	private fun clearStudentCode() {
		val prefs = getSharedPreferences("hce_prefs", Context.MODE_PRIVATE)
		prefs.edit().remove("HCE_STUDENT_CODE").apply()
	}
}
