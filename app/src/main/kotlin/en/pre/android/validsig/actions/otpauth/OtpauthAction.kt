package en.pre.android.validsig.actions.otpauth

import en.pre.android.validsig.R
import en.pre.android.validsig.actions.SchemeAction

object OtpauthAction : SchemeAction() {
	override val iconResId: Int = R.drawable.ic_action_otpauth
	override val titleResId: Int = R.string.otpauth_add
	override val scheme: String = "otpauth"
}
