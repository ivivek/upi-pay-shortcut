package com.linetra.upishortcut

import com.linetra.upishortcut.data.Merchant
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON export/import of saved merchants, for moving to a new phone by hand.
 * (Android Auto Backup also covers the database; home-screen icons are never restored, so re-pin.)
 */
object Backup {

    private const val FORMAT = "upi-shortcuts"
    private const val VERSION = 1

    fun toJson(merchants: List<Merchant>): String {
        val list = JSONArray()
        for (m in merchants) {
            list.put(
                JSONObject()
                    .put("name", m.name)
                    .put("upiLink", m.upiLink)
                    .put("upiPackage", m.upiPackage ?: JSONObject.NULL)
                    .put("color", m.color)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("merchants", list)
            .toString(2)
    }

    /** Parses an export; entries without a valid UPI link are dropped. Throws on a non-export file. */
    fun fromJson(text: String): List<Merchant> {
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Not a UPI Shortcuts export" }
        val list = root.getJSONArray("merchants")
        return (0 until list.length()).mapNotNull { i ->
            val o = list.getJSONObject(i)
            val parsed = UpiLink.parse(o.optString("upiLink")) as? UpiLink.Valid ?: return@mapNotNull null
            Merchant(
                name = o.optString("name").ifBlank { parsed.payeeName ?: parsed.payee },
                upiLink = parsed.link,
                upiPackage = if (o.isNull("upiPackage")) null else o.optString("upiPackage"),
                color = o.optInt("color", 0),
            )
        }
    }
}
