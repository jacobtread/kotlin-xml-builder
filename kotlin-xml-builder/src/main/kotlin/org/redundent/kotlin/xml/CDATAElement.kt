package org.redundent.kotlin.xml

/**
 * Similar to a [TextElement] except that the inner text is wrapped inside a <![CDATA[]]> tag.
 */
class CDATAElement internal constructor(text: String) : TextElement(text) {
    override fun renderedText(printOptions: PrintOptions): String {
        fun String.escapeCData(): String {
            val cdataEnd = "]]>"
            val cdataStart = "<![CDATA["
            return this
                // split cdataEnd into two pieces so XML parser doesn't recognize it
                .replace(cdataEnd, "]]$cdataEnd$cdataStart>")
        }

        return "<![CDATA[${text.escapeCData()}]]>"
    }

    override fun equals(other: Any?): Boolean = super.equals(other) && other is CDATAElement
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + javaClass.hashCode()
        return result
    }
}