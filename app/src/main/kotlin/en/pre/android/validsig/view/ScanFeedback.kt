package en.pre.android.validsig.view

import android.content.Context
import android.media.AudioManager
import en.pre.android.validsig.app.prefs
import en.pre.android.validsig.media.beepConfirm
import en.pre.android.validsig.media.beepError
import en.pre.android.validsig.os.error
import en.pre.android.validsig.os.getVibrator
import en.pre.android.validsig.os.vibrate

fun Context.scanFeedback() {
	if (prefs.vibrate) {
		getVibrator().vibrate()
	}
	if (prefs.beep && !isSilent()) {
		beepConfirm()
	}
}

fun Context.errorFeedback() {
	if (prefs.vibrate) {
		getVibrator().error()
	}
	if (prefs.beep && !isSilent()) {
		beepError()
	}
}

private fun Context.isSilent(): Boolean {
	val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
	return when (am.ringerMode) {
		AudioManager.RINGER_MODE_SILENT,
		AudioManager.RINGER_MODE_VIBRATE -> true

		else -> false
	}
}
