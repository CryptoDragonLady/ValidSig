package en.pre.android.validsig.content

fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }