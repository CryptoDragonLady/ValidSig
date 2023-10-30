package en.pre.android.validsig.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import en.pre.android.validsig.R
import en.pre.android.validsig.adapter.prettifyFormatName
import en.pre.android.validsig.app.addFragment
import en.pre.android.validsig.app.prefs
import en.pre.android.validsig.text.unescape
import en.pre.android.validsig.view.hideSoftKeyboard
import en.pre.android.validsig.view.setPaddingFromWindowInsets
import en.pre.android.validsig.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp.Format
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var ecLabel: TextView
	private lateinit var ecSpinner: Spinner
	private lateinit var colorsLabel: TextView
	private lateinit var colorsSpinner: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
	private lateinit var marginLayout: View
	private lateinit var marginView: TextView
	private lateinit var marginBarView: SeekBar
	private lateinit var contentView: EditText
	private lateinit var unescapeCheckBox: CheckBox

	private val formats = arrayListOf(
		Format.AZTEC,
		Format.CODABAR,
		Format.CODE_39,
		Format.CODE_128,
		Format.DATA_MATRIX,
		Format.EAN_8,
		Format.EAN_13,
		Format.ITF,
		Format.PDF_417,
		Format.QR_CODE,
		Format.UPC_A
	)

	private var minMargin = 0
	private var bytes: ByteArray? = null

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		when (requestCode) {
			PICK_FILE_RESULT_CODE -> {
				if (resultCode == Activity.RESULT_OK &&
					resultData != null &&
					resultData.data != null
				) {
					val ac = activity ?: return
					ac.hideSoftKeyboard(contentView)
					val uri = resultData.data ?: return
					bytes = ac.contentResolver?.openInputStream(uri)?.use {
						it.readBytesMax(4096)
					} ?: return
					setEncodeByteArray()
				}
			}
		}
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		val ac = activity ?: return null
		ac.setTitle(R.string.compose_barcode)

		val view = inflater.inflate(
			R.layout.fragment_encode,
			container,
			false
		)

		formatView = view.findViewById(R.id.format)
		val formatAdapter = ArrayAdapter(
			ac,
			android.R.layout.simple_spinner_item,
			formats.map { prettifyFormatName(it.name) }
		)
		formatAdapter.setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item
		)
		formatView.adapter = formatAdapter
		formatView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parentView: AdapterView<*>?,
				selectedItemView: View?,
				position: Int,
				id: Long
			) {
				val format = formats[position]
				val arrayId = when (format) {
					Format.AZTEC -> R.array.aztec_error_correction_levels
					Format.QR_CODE -> R.array.qr_error_correction_levels
					Format.PDF_417 -> R.array.pdf417_error_correction_levels
					else -> 0
				}
				if (arrayId > 0) {
					ecSpinner.setEntries(arrayId)
					val idx = format.unpackEcLevel(
						prefs.indexOfLastSelectedEcLevel
					)
					if (idx < ecSpinner.adapter.count) {
						ecSpinner.setSelection(idx)
					}
					true
				} else {
					false
				}.setVisibilityFor(ecLabel, ecSpinner)
				format.canBeInverted().setVisibilityFor(
					colorsLabel, colorsSpinner
				)
				when (format) {
					Format.AZTEC -> setMarginBar(4, 4)
					Format.DATA_MATRIX -> setMarginBar(1, 1)
					Format.QR_CODE -> setMarginBar(4, 4)
					Format.PDF_417 -> setMarginBar(0, 30)
					else -> false
				}.setVisibilityFor(marginLayout)
			}

			override fun onNothingSelected(parentView: AdapterView<*>?) {}
		}

		ecLabel = view.findViewById(R.id.error_correction_label)
		ecSpinner = view.findViewById(R.id.error_correction_level)
		ecSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parentView: AdapterView<*>?,
				selectedItemView: View?,
				position: Int,
				id: Long
			) {
				val format = formats[formatView.selectedItemPosition]
				prefs.indexOfLastSelectedEcLevel = format.packEcLevel(
					prefs.indexOfLastSelectedEcLevel,
					position
				)
			}

			override fun onNothingSelected(parentView: AdapterView<*>?) {}
		}

		colorsLabel = view.findViewById(R.id.colors_label)
		colorsSpinner = view.findViewById(R.id.colors)

		sizeView = view.findViewById(R.id.size_display)
		sizeBarView = view.findViewById(R.id.size_bar)
		initSizeBar()

		marginLayout = view.findViewById(R.id.margin)
		marginView = view.findViewById(R.id.margin_display)
		marginBarView = view.findViewById(R.id.margin_bar)
		initMarginBar()

		contentView = view.findViewById(R.id.content)
		unescapeCheckBox = view.findViewById(R.id.unescape)
		unescapeCheckBox.isChecked = prefs.expandEscapeSequences

		val args = arguments
		args?.getString(CONTENT_TEXT)?.let {
			contentView.setText(it)
		}
		args?.getByteArray(CONTENT_RAW)?.let {
			bytes = it
			setEncodeByteArray()
		}

		val barcodeFormat = args?.getString(FORMAT)
		if (barcodeFormat != null) {
			formatView.setSelection(
				formats.indexOf(barcodeFormat.toFormat())
			)
		} else if (state == null) {
			formatView.post {
				formatView.setSelection(prefs.indexOfLastSelectedFormat)
			}
		}

		view.findViewById<View>(R.id.encode).setOnClickListener {
			it.context.encode()
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		(view.findViewById(R.id.scroll_view) as View).setPaddingFromWindowInsets()

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_encode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.pick_file -> {
				startActivityForResult(
					Intent.createChooser(
						Intent(Intent.ACTION_GET_CONTENT).apply {
							type = "*/*"
						},
						getString(R.string.pick_file)
					),
					PICK_FILE_RESULT_CODE
				)
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onPause() {
		super.onPause()
		prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
		prefs.expandEscapeSequences = unescapeCheckBox.isChecked
		prefs.lastMargin = marginBarView.progress
	}

	private fun Context.encode() {
		hideSoftKeyboard(contentView)
		val content = if (bytes != null) {
			bytes
		} else {
			var text = contentView.text.toString()
			if (unescapeCheckBox.isChecked) {
				try {
					text = text.unescape()
				} catch (e: IllegalArgumentException) {
					toast(e.message ?: "Invalid escape sequence")
					return
				}
			}
			if (text.isEmpty()) {
				toast(R.string.error_no_content)
				return
			}
			text
		}
		val format = formats[formatView.selectedItemPosition]
		fragmentManager?.addFragment(
			BarcodeFragment.newInstance(
				content,
				format,
				getSize(sizeBarView.progress),
				when (format) {
					Format.AZTEC,
					Format.DATA_MATRIX,
					Format.QR_CODE,
					Format.PDF_417 -> marginBarView.progress

					else -> -1
				},
				format.getErrorCorrectionLevel(ecSpinner.selectedItemPosition),
				if (format.canBeInverted()) {
					colorsSpinner.selectedItemPosition
				} else 0
			)
		)
	}

	private fun initSizeBar() {
		updateSize(sizeBarView.progress)
		sizeBarView.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar,
					progressValue: Int,
					fromUser: Boolean
				) {
					updateSize(progressValue)
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {}
			})
	}

	private fun updateSize(power: Int) {
		val size = getSize(power)
		sizeView.text = getString(R.string.size_width_by_height, size, size)
	}

	private fun initMarginBar() {
		updateMargin(marginBarView.progress)
		marginBarView.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar,
					progressValue: Int,
					fromUser: Boolean
				) {
					if (progressValue >= minMargin) {
						updateMargin(progressValue)
					}
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {}
			})
	}

	private fun updateMargin(margin: Int) {
		marginView.text = getString(R.string.margin_size, margin)
	}

	private fun setMarginBar(minimum: Int, value: Int): Boolean {
		minMargin = minimum
		marginBarView.apply {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				min = minimum
			}
			progress = max(
				minimum, if (prefs.lastMargin > -1) {
					prefs.lastMargin
				} else {
					value
				}
			)
		}
		return true
	}

	private fun setEncodeByteArray() {
		contentView.text = null
		contentView.hint = getString(R.string.binary_data)
		contentView.isEnabled = false
		unescapeCheckBox.isEnabled = false
	}

	companion object {
		private const val CONTENT_TEXT = "content_text"
		private const val CONTENT_RAW = "content_raw"
		private const val FORMAT = "format"
		private const val PICK_FILE_RESULT_CODE = 1

		fun <T> newInstance(
			content: T? = null,
			format: String? = null
		): Fragment {
			val args = Bundle()
			content?.let {
				when (content) {
					is String -> args.putString(CONTENT_TEXT, content)
					is ByteArray -> args.putByteArray(CONTENT_RAW, content)
					else -> throw IllegalArgumentException(
						"content must be a String or a ByteArray"
					)
				}
			}
			format?.let { args.putString(FORMAT, it) }
			val fragment = EncodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun Boolean.setVisibilityFor(vararg views: View) {
	val visibility = if (this) View.VISIBLE else View.GONE
	for (view in views) {
		view.visibility = visibility
	}
}

private fun Format.packEcLevel(packed: Int, level: Int): Int {
	val s = ecLevelShift()
	return (level shl s) or (packed and (15 shl s).inv())
}

private fun Format.unpackEcLevel(packed: Int) =
	(packed shr ecLevelShift()) and 15

private fun Format.ecLevelShift() = when (this) {
	Format.AZTEC -> 0
	Format.QR_CODE -> 4
	Format.PDF_417 -> 8
	else -> throw IllegalArgumentException("$this does not have error levels")
}

private fun Format.canBeInverted() = when (this) {
	Format.AZTEC,
	Format.DATA_MATRIX,
	Format.QR_CODE -> true

	else -> false
}

private fun String.toFormat(default: Format = Format.QR_CODE): Format = try {
	Format.valueOf(this)
} catch (_: IllegalArgumentException) {
	default
}

private fun Format.getErrorCorrectionLevel(position: Int) = when (this) {
	Format.AZTEC -> position
	Format.QR_CODE -> (position + 1) * 2
	Format.PDF_417 -> position
	else -> 0
}.coerceIn(0, 8)

private fun Spinner.setEntries(resId: Int) = ArrayAdapter.createFromResource(
	this.context,
	resId,
	android.R.layout.simple_spinner_item
).also { aa ->
	aa.setDropDownViewResource(
		android.R.layout.simple_spinner_dropdown_item
	)
	adapter = aa
}

private fun getSize(power: Int) = 128 * (power + 1)

private fun InputStream.readBytesMax(max: Int): ByteArray {
	var offset = 0
	var remaining = min(available(), max)
	val result = ByteArray(remaining)
	while (remaining > 0) {
		val read = read(result, offset, remaining)
		if (read < 0) {
			break
		}
		remaining -= read
		offset += read
	}
	return result
}