package com.jacobtread.xml

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class CDATAElementTest {
	@Test
	fun testHashCode() {
		val text = com.jacobtread.xml.CDATAElement("test")

		assertNotEquals(text.text.hashCode(), text.hashCode(), "CDATA hashcode is not just text.hashCode()")
	}

	@Test
	fun `equals null`() {
		val text = com.jacobtread.xml.CDATAElement("test")

		assertFalse(text.equals(null))
	}

	@Test
	fun `equals different type`() {
		val text = com.jacobtread.xml.CDATAElement("test")
		val other = TextElement("test")

		assertNotEquals(text, other)
	}

	@Test
	fun `equals different text`() {
		val text1 = com.jacobtread.xml.CDATAElement("text1")
		val text2 = com.jacobtread.xml.CDATAElement("text2")

		assertNotEquals(text1, text2)
		assertNotEquals(text2, text1)
	}

	@Test
	fun equals() {
		val text1 = com.jacobtread.xml.CDATAElement("text1")
		val text2 = com.jacobtread.xml.CDATAElement("text1")

		assertEquals(text1, text2)
		assertEquals(text2, text1)
	}
}