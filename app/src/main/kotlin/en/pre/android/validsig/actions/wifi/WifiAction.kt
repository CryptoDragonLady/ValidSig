package en.pre.android.validsig.actions.wifi

import android.content.Context
import en.pre.android.validsig.R
import en.pre.android.validsig.actions.IAction
import en.pre.android.validsig.widget.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WifiAction : IAction {
	override val iconResId = R.drawable.ic_action_wifi
	override val titleResId = R.string.connect_to_wifi

	var password: String? = null

	override fun canExecuteOn(data: ByteArray): Boolean =
		WifiConnector.parse(String(data)) {
			password = it
		} != null

	override suspend fun execute(
		context: Context,
		data: ByteArray
	) = withContext(Dispatchers.IO) {
		val message = WifiConnector.parse(String(data))?.let {
			WifiConnector.addNetwork(context, it)
		} ?: R.string.wifi_config_failed
		withContext(Dispatchers.Main) {
			context.toast(message)
		}
	}
}
