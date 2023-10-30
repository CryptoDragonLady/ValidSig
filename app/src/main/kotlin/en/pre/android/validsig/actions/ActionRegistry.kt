package en.pre.android.validsig.actions

import en.pre.android.validsig.actions.mail.MailAction
import en.pre.android.validsig.actions.mail.MatMsgAction
import en.pre.android.validsig.actions.otpauth.OtpauthAction
import en.pre.android.validsig.actions.search.OpenOrSearchAction
import en.pre.android.validsig.actions.sms.SmsAction
import en.pre.android.validsig.actions.tel.TelAction
import en.pre.android.validsig.actions.vtype.vcard.VCardAction
import en.pre.android.validsig.actions.vtype.vevent.VEventAction
import en.pre.android.validsig.actions.web.WebAction
import en.pre.android.validsig.actions.wifi.WifiAction

object ActionRegistry {
	val DEFAULT_ACTION: en.pre.android.validsig.actions.IAction = OpenOrSearchAction

	private val REGISTRY: Set<en.pre.android.validsig.actions.IAction> = setOf(
		MailAction,
		MatMsgAction,
		OtpauthAction,
		SmsAction,
		TelAction,
		VCardAction,
		VEventAction,
		WifiAction,
		// Try WebAction last because recognizing colloquial URLs is
		// very aggressive.
		WebAction
	)

	fun getAction(data: ByteArray): en.pre.android.validsig.actions.IAction = en.pre.android.validsig.actions.ActionRegistry.REGISTRY.find {
		it.canExecuteOn(data)
	} ?: en.pre.android.validsig.actions.ActionRegistry.DEFAULT_ACTION
}
