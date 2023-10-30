package en.pre.android.validsig.app

import android.app.Application
import en.pre.android.validsig.database.Database
import en.pre.android.validsig.preference.Preferences

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
		prefs.init(this)
	}
}
