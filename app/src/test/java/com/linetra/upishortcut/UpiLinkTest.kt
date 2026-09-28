package com.linetra.upishortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiLinkTest {

    private val example =
        "upi://pay?ver=01&pa=examplestore@okbank&pn=Example Services&tn=%20&am=&mode=00&purpose=00&orgid=000000&sign=&mc=5411"

    private fun valid(link: String) = UpiLink.parse(link) as UpiLink.Valid

    @Test
    fun parsesMerchantQr() {
        val v = valid(example)
        assertEquals("examplestore@okbank", v.payee)
        assertEquals("Example Services", v.payeeName)
        assertEquals("5411", v.merchantCode)
        assertNull(v.amount)
        assertTrue(v.warnings.isEmpty())
    }

    @Test
    fun encodesSpacesButKeepsEverythingElse() {
        val v = valid(example)
        assertEquals(example.replace(" ", "%20"), v.link)
    }

    @Test
    fun rejectsNonUpi() {
        assertTrue(UpiLink.parse("https://example.com/?pa=a@b") is UpiLink.Invalid)
        assertTrue(UpiLink.parse("upi://mandate?pa=a@ybl") is UpiLink.Invalid)
        assertTrue(UpiLink.parse("") is UpiLink.Invalid)
    }

    @Test
    fun rejectsMissingOrBadPayee() {
        assertTrue(UpiLink.parse("upi://pay?pn=Shop") is UpiLink.Invalid)
        assertTrue(UpiLink.parse("upi://pay?pa=not-a-vpa") is UpiLink.Invalid)
    }

    @Test
    fun schemeIsCaseInsensitive() {
        assertEquals("shop@ybl", valid("UPI://PAY?pa=shop@ybl&mc=1234").payee)
    }

    @Test
    fun warnsOnOneTimeAndPersonalQrs() {
        val v = valid("upi://pay?pa=friend@okaxis&pn=Friend&am=250.00&tr=ABC123")
        assertEquals(
            listOf(UpiLink.Warning.AMOUNT_SET, UpiLink.Warning.TXN_REF, UpiLink.Warning.NO_MERCHANT_CODE),
            v.warnings,
        )
    }

    @Test
    fun zeroAmountIsNotAWarning() {
        assertTrue(UpiLink.Warning.AMOUNT_SET !in valid("upi://pay?pa=s@ybl&mc=5411&am=0").warnings)
    }

    @Test
    fun withoutParamRemovesOnlyThatKey() {
        assertEquals(
            "upi://pay?pa=s@ybl&mc=5411&cu=INR",
            UpiLink.withoutParam("upi://pay?pa=s@ybl&am=99&mc=5411&cu=INR", "am"),
        )
    }

    @Test
    fun findsLinkInSharedText() {
        assertEquals(
            "upi://pay?pa=s@ybl&pn=Shop",
            UpiLink.find("Pay me here: upi://pay?pa=s@ybl&pn=Shop thanks"),
        )
        assertNull(UpiLink.find("no link here"))
    }

    @Test
    fun malformedEscapeKeptRaw() {
        assertEquals("100%", valid("upi://pay?pa=s@ybl&tn=100%").params["tn"])
    }
}
