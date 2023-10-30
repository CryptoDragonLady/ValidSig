package en.pre.android.validsig.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import en.pre.android.validsig.R
import en.pre.android.validsig.app.PERMISSION_LOCATION
import en.pre.android.validsig.app.PERMISSION_WRITE
import en.pre.android.validsig.app.applyLocale
import en.pre.android.validsig.app.permissionGrantedCallback
import en.pre.android.validsig.app.prefs
import en.pre.android.validsig.app.setFragment
import en.pre.android.validsig.database.Scan
import en.pre.android.validsig.fragment.DecodeFragment
import en.pre.android.validsig.fragment.EncodeFragment
import en.pre.android.validsig.fragment.HistoryFragment
import en.pre.android.validsig.fragment.PreferencesFragment
import en.pre.android.validsig.view.colorSystemAndToolBars
import en.pre.android.validsig.view.initSystemBars
import en.pre.android.validsig.view.recordToolbarHeight

class MainActivity : AppCompatActivity() {
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		when (requestCode) {
			PERMISSION_LOCATION, PERMISSION_WRITE -> if (grantResults.isNotEmpty() &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED
			) {
				permissionGrantedCallback?.invoke()
				permissionGrantedCallback = null
			}
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		val fm = supportFragmentManager
		if (fm != null && fm.backStackEntryCount > 0) {
			fm.popBackStack()
		} else {
			finish()
		}
		return true
	}

	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_main)

		initSystemBars(this)
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		recordToolbarHeight(toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@MainActivity)
		}

		if (state == null) {
			supportFragmentManager?.setFragment(getFragmentForIntent(intent))
		}
	}

	companion object {
		private const val PREFERENCES = "preferences"
		private const val HISTORY = "history"
		private const val ENCODE = "encode"
		const val DECODED = "decoded"

		private fun getFragmentForIntent(intent: Intent?): Fragment {
			intent ?: return PreferencesFragment()
			return when {
				intent.hasExtra(PREFERENCES) -> PreferencesFragment()
				intent.hasExtra(HISTORY) -> HistoryFragment()
				intent.hasExtra(ENCODE) -> EncodeFragment.newInstance(
					intent.getStringExtra(ENCODE)
				)

				intent.hasExtra(DECODED) -> DecodeFragment.newInstance(
					intent.getParcelableExtra(DECODED)!!
				)

				else -> PreferencesFragment()
			}
		}

		fun getPreferencesIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(PREFERENCES, true)
			return intent
		}

		fun getHistoryIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(HISTORY, true)
			return intent
		}

		fun getEncodeIntent(
			context: Context,
			text: String? = "",
			isExternal: Boolean = false
		): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(ENCODE, text)
			if (isExternal) {
				val flagActivityClearTask =
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						Intent.FLAG_ACTIVITY_CLEAR_TASK
					} else 0
				intent.addFlags(
					Intent.FLAG_ACTIVITY_NO_HISTORY or
							flagActivityClearTask or
							Intent.FLAG_ACTIVITY_NEW_TASK
				)
			}
			return intent
		}

		fun getDecodeIntent(context: Context, scan: Scan): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(DECODED, scan)
			return intent
		}
	}
}
