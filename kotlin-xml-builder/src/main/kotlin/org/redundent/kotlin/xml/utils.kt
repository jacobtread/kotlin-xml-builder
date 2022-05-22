package org.redundent.kotlin.xml

internal fun escapeValue(value: Any?, xmlVersion: XmlVersion, useCharacterReference: Boolean = false): String? {
    val asString = value?.toString() ?: return null
    if (useCharacterReference) {
        return referenceCharacter(asString)
    }
    return when (xmlVersion) {
        XmlVersion.V10 -> escapeXml10(asString)
        XmlVersion.V11 -> escapeXml11(asString)
    }
}

internal fun escapeXml10(value: String): String {
    val out = StringBuilder()
    value.forEach {
        when (it.code) {
            34 /* " */ -> out.append("&quot;")
            38 /* & */ -> out.append("&amp;")
            60 /* < */ -> out.append("&lt;")
            62 /* > */ -> out.append("&gt;")
            39 /* ' */ -> out.append("&apos;")
            in 0..8,
            0xb, 0xc, 0xe, 0xf, 0xfffe, 0xffff,
            in 0x10..0x19,
            in 0x1a..0x1f,
            in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code,
            -> return@forEach
            in 0x7f..0x84,
            in 0x86..0x9f,
            -> out.append("&#").append(it.code.toString(10)).append(';')
            else -> out.append(it)
        }
    }
    return out.toString()
}

internal fun escapeXml11(value: String): String {
    val out = StringBuilder()
    value.forEach {
        when (it.code) {
            34 /* " */ -> out.append("&quot;")
            38 /* & */ -> out.append("&amp;")
            60 /* < */ -> out.append("&lt;")
            62 /* > */ -> out.append("&gt;")
            39 /* ' */ -> out.append("&apos;")
            0, 0xfffe, 0xffff,
            in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code,
            -> return@forEach
            in 0x1..0x8,
            in 0xb..0xc,
            in 0xe..0x1f,
            in 0x7f..0x84,
            in 0x86..0x9f,
            -> out.append("&#").append(it.code.toString(10)).append(';')
            else -> out.append(it)
        }
    }
    return out.toString()
}

internal fun referenceCharacter(asString: String): String {
    val builder = StringBuilder()

    asString.toCharArray().forEach { character ->
        when (character) {
            '\'' -> builder.append("&#39;")
            '&' -> builder.append("&#38;")
            '<' -> builder.append("&#60;")
            '>' -> builder.append("&#62;")
            '"' -> builder.append("&#34;")
            else -> builder.append(character)
        }
    }

    return builder.toString()
}