package com.linetra.upishortcut

import java.net.URLDecoder

/**
 * Parsing and validation of `upi://pay?...` links (NPCI UPI linking spec).
 * Pure Kotlin so it can be unit-tested on the JVM.
 *
 * The link is stored exactly as scanned (only whitespace is normalised) so that merchant
 * parameters like `mc`, `orgid` and `sign` reach the UPI app untouched.
 */
object UpiLink {

    private val VPA = Regex("^[A-Za-z0-9._-]{1,256}@[A-Za-z][A-Za-z0-9.-]{1,64}$")
    private val IN_TEXT = Regex("""upi://pay\?[^\s"'<>]+""", RegexOption.IGNORE_CASE)

    enum class Warning {
        /** `am` is set: likely a QR printed for one bill. */
        AMOUNT_SET,

        /** `tr` is set: transaction reference, often one-time. */
        TXN_REF,

        /** No merchant category code: a person-to-person payee. */
        NO_MERCHANT_CODE,
    }

    sealed interface Result

    data class Valid(val link: String, val params: Map<String, String>, val warnings: List<Warning>) : Result {
        val payee: String get() = params.getValue("pa")
        val payeeName: String? get() = params["pn"]?.trim()?.takeIf { it.isNotEmpty() }
        val amount: String? get() = params["am"]?.takeIf { it.isNotBlank() }
        val merchantCode: String? get() = params["mc"]?.takeIf { it.isNotBlank() }
    }

    data class Invalid(val reason: String) : Result

    /** Trims, drops line breaks and encodes spaces (e.g. `pn=Example Services`) so the URI parses. */
    fun normalize(input: String): String =
        input.trim().replace("\r", "").replace("\n", "").replace(" ", "%20")

    /** Finds the first `upi://pay?...` link inside arbitrary shared text. */
    fun find(text: String): String? = IN_TEXT.find(text)?.value

    fun parse(input: String): Result {
        val link = normalize(input)
        if (link.isEmpty()) return Invalid("Enter a UPI link")
        val q = link.indexOf('?')
        val base = if (q < 0) link else link.substring(0, q)
        if (!base.equals("upi://pay", ignoreCase = true) && !base.equals("upi://pay/", ignoreCase = true)) {
            return Invalid("Not a UPI payment link (must start with upi://pay?)")
        }
        if (q < 0) return Invalid("Link has no payment details")

        val params = queryParams(link.substring(q + 1))
        val pa = params["pa"].orEmpty().trim()
        if (pa.isEmpty()) return Invalid("Missing UPI ID (pa=)")
        if (!VPA.matches(pa)) return Invalid("\"$pa\" is not a valid UPI ID")

        val warnings = buildList {
            val am = params["am"]?.toDoubleOrNull()
            if (am != null && am > 0) add(Warning.AMOUNT_SET)
            if (!params["tr"].isNullOrBlank()) add(Warning.TXN_REF)
            val mc = params["mc"]
            if (mc.isNullOrBlank() || mc == "0000") add(Warning.NO_MERCHANT_CODE)
        }
        return Valid(link, params + ("pa" to pa), warnings)
    }

    /** UPI ID of a stored link, or null if it doesn't parse. */
    fun payeeOf(link: String): String? = (parse(link) as? Valid)?.payee

    /** Removes [key] from the link's query, leaving every other parameter byte-for-byte intact. */
    fun withoutParam(link: String, key: String): String {
        val q = link.indexOf('?')
        if (q < 0) return link
        val kept = link.substring(q + 1).split('&').filter { it.substringBefore('=') != key }
        return link.substring(0, q + 1) + kept.joinToString("&")
    }

    private fun queryParams(query: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val key = part.substringBefore('=')
            val raw = part.substringAfter('=', "")
            val value = try {
                URLDecoder.decode(raw, "UTF-8")
            } catch (e: IllegalArgumentException) {
                raw // malformed %-escape: keep as-is rather than rejecting the link
            }
            out.putIfAbsent(key, value)
        }
        return out
    }
}
