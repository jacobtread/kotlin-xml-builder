package com.jacobtread.xml

/**
 * Base type for all elements. This is what handles pretty much all the rendering and building.
 */
open class Node(val nodeName: String) : Element {
    private companion object {
        private val isReflectionAvailable: Boolean by lazy {
            Node::class.java.classLoader.getResource("kotlin/reflect/full") != null
        }
    }

    /**
     * The default xmlns for the document. To add other namespaces, use the [namespace] method
     */
    var xmlns: String?
        get() = this["xmlns"]
        set(value) {
            this["xmlns"] = value
        }

    /**
     * Whether to include the xml prolog, i.e. <?xml version="1.0" encoding="UTS-8"?>
     *
     * <p>NOTE: this only applies to the root element. It is ignored an all children
     */
    var includeXmlProlog = false

    /**
     * Sets the encoding on the document. Setting this value will set [includeXmlProlog] to true
     */
    var encoding: String = Charsets.UTF_8.name()
        set(value) {
            includeXmlProlog = true
            field = value
        }

    var version: XmlVersion = XmlVersion.V10
        set(value) {
            includeXmlProlog = true
            field = value
        }

    var standalone: Boolean? = null
        set(value) {
            includeXmlProlog = true
            field = value
        }

    /**
     * Any attributes that belong to this element. You can either interact with this property directly or use the [get] and [set] operators
     * @sample [set]
     */
    val attributes = LinkedHashMap<String, Any?>()

    private val _globalLevelProcessingInstructions = ArrayList<ProcessingInstructionElement>()

    private var doctype: Doctype? = null

    val children = ArrayList<Element>()

    private val childOrderMap: Map<String, Int>? by lazy {
        if (!isReflectionAvailable) {
            return@lazy null
        }
        val xmlTypeAnnotation = this::class.annotations.firstOrNull { it is XmlType } as? XmlType ?: return@lazy null
        val childOrder = xmlTypeAnnotation.childOrder
        childOrder.indices.associateBy { childOrder[it] }
    }


    /**
     * Allows for easy access of this node's attributes
     *
     * <code>
     *     val attr = element["key"]
     * </code>
     */
    operator fun <T> get(attributeName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes[attributeName] as T?
    }

    /**
     * Allows for easy access of adding/updating this node's attributes
     *
     * <code>
     *     element["key"] = "value"
     * </code>
     */
    operator fun set(attributeName: String, value: Any?) {
        if (value == null) {
            attributes.remove(attributeName)
        } else {
            attributes[attributeName] = value
        }
    }

    fun hasAttribute(attributeName: String): Boolean = attributes.containsKey(attributeName)

    override fun render(builder: Appendable, indent: String, printOptions: PrintOptions) {
        val lineEnding = getLineEnding(printOptions)
        builder.append("$indent<$nodeName${renderAttributes(printOptions)}")

        if (!isEmptyOrSingleEmptyTextElement()) {
            if (printOptions.pretty && printOptions.singleLineTextElements
                && children.size == 1 && children[0] is TextElement
            ) {
                builder.append(">")
                (children[0] as TextElement).renderSingleLine(builder, printOptions)
                builder.append("</$nodeName>$lineEnding")
            } else {
                builder.append(">$lineEnding")
                for (c in sortedChildren()) {
                    c.render(builder, getIndent(printOptions, indent), printOptions)
                }

                builder.append("$indent</$nodeName>$lineEnding")
            }
        } else {
            builder.append("${getEmptyTagClosing(printOptions)}$lineEnding")
        }
    }

    private fun isEmptyOrSingleEmptyTextElement(): Boolean {
        if (children.isEmpty()) {
            return true
        }

        if (children.size == 1 && children[0] is TextElement) {
            return (children[0] as TextElement).text.isEmpty()
        }

        return false
    }

    private fun getEmptyTagClosing(printOptions: PrintOptions): String = if (printOptions.useSelfClosingTags)
        "/>"
    else
        "></$nodeName>"

    private fun sortedChildren(): List<Element> {
        return if (childOrderMap == null) {
            children
        } else {
            children.sortedWith { a, b ->
                val indexA = if (a is Node) childOrderMap!![a.nodeName] else 0
                val indexB = if (b is Node) childOrderMap!![b.nodeName] else 0

                compareValues(indexA, indexB)
            }
        }
    }

    private fun renderAttributes(printOptions: PrintOptions): String {
        if (attributes.isEmpty()) {
            return ""
        }

        return " " + attributes.entries.joinToString(" ") {
            "${it.key}=\"${escapeValue(it.value, printOptions.xmlVersion, printOptions.useCharacterReference)}\""
        }
    }

    private fun getIndent(printOptions: PrintOptions, indent: String): String = if (!printOptions.pretty) "" else "$indent${printOptions.indent}"

    /**
     * Get the xml representation of this object with prettyFormat = true
     */
    override fun toString() = toString(prettyFormat = true)

    /**
     * Get the xml representation of this object
     *
     * @param [prettyFormat] true to format the xml with newlines and tabs; otherwise no formatting
     */
    fun toString(prettyFormat: Boolean): String = toString(PrintOptions(pretty = prettyFormat))

    fun toString(printOptions: PrintOptions): String = StringBuilder().also { writeTo(it, printOptions) }.toString().trim()

    fun writeTo(appendable: Appendable, printOptions: PrintOptions = PrintOptions()) {
        val lineEnding = getLineEnding(printOptions)

        printOptions.xmlVersion = version

        if (includeXmlProlog) {
            appendable.append("<?xml version=\"${printOptions.xmlVersion.value}\" encoding=\"$encoding\"")

            standalone?.run {
                appendable.append(" standalone=\"${if (this) "yes" else "no"}\"")
            }

            appendable.append("?>$lineEnding")
        }

        doctype?.apply {
            render(appendable, "", printOptions)
        }

        if (_globalLevelProcessingInstructions.isNotEmpty()) {
            _globalLevelProcessingInstructions.forEach { it.render(appendable, "", printOptions) }
        }

        render(appendable, "", printOptions)
    }

    operator fun String.unaryMinus() = text(this)

    fun text(text: String) {
        children.add(TextElement(text))
    }

    /**
     * Adds an xml comment to the document.
     * <code>
     *     comment("my comment")
     * </code>
     *
     * @param text The text of the comment. This text will be rendenered unescaped except for replace "--" with "&#45;&#45;"]
     */
    fun comment(text: String) {
        children.add(Comment(text))
    }

    /**
     * Adds a basic element with the specific name to the parent.
     * <code>
     *     element("url") {
     *     		...
     *     }
     * </code>
     *
     * @param name The name of the element.
     */
    fun element(name: String): Node {
        val node = Node(name)
        children.add(node)
        return node
    }

    fun element(name: String, value: Any): Node {
        val node = element(name)
        node.text(value.toString())
        return node
    }

    fun element(name: String, value: String): Node {
        val node = element(name)
        node.text(value)
        return node
    }

    /**
     * element Inline function for initializing an element
     * inlined to prevent extra object creation
     *
     * @param name
     * @param init
     * @receiver
     * @return
     */
    inline fun element(name: String, init: Node.() -> Unit): Node {
        val node = element(name)
        node.init()
        return node
    }

    /**
     * Adds a basic element with the specific name and value to the parent. This cannot be used for complex elements.
     * <code>
     *     "url"("https://google.com")
     * </code>
     *
     * @receiver The name of the element.
     * @param value The inner text of the element
     */
    operator fun String.invoke(value: String): Node = element(this, value)

    /**
     * Adds a basic element with the specific name to the parent. This method
     * allows you to specify optional attributes and content
     * <code>
     *     "url"("key" to "value") {
     *     		...
     *     }
     * </code>
     *
     * @receiver The name of the element.
     * @param attributes Any attributes to add to this element. Can be omited.
     * @param init The block that defines the content of the element.
     */
    inline operator fun String.invoke(vararg attributes: Pair<String, Any>, init: Node.() -> Unit): Node {
        val e = element(this)
        e.attributes(*attributes)
        e.apply(init)
        return e
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun String.invoke(vararg attributes: Pair<String, Any>): Node {
        val e = element(this)
        e.attributes(*attributes)
        return e
    }

    /**
     * Adds an attribute to the current element
     * <code>
     *     "url" {
     *         attribute("key", "value")
     *     }
     * </code>
     *
     * @param name The name of the attribute. This is currenly no validation against the name.
     * @param value The attribute value.
     */
    fun attribute(name: String, value: Any) {
        attributes[name] = value.toString()
    }

    /**
     * Adds a set of attributes to the current element.
     * @see [attribute]
     *
     * <code>
     *     "url" {
     *         attributes(
     *             "key" to "value",
     *             "id" to "1"
     *         )
     *     }
     * </code>
     *
     * @param attrs Collection of the attributes to apply to this element.
     */
    fun attributes(vararg attrs: Pair<String, Any>) {
        attrs.forEach { attribute(it.first, it.second) }
    }

    /**
     * Adds the supplied text as a CDATA element
     *
     * @param text The inner text of the CDATA element.
     */
    fun cdata(text: String) {
        children.add(com.jacobtread.xml.CDATAElement(text))
    }

    /**
     * Adds the supplied text as a processing instruction element
     *
     * @param text The inner text of the processing instruction element.
     * @param attributes Optional set of attributes to apply to this processing instruction.
     */
    fun processingInstruction(text: String, vararg attributes: Pair<String, String>) {
        children.add(ProcessingInstructionElement(text, linkedMapOf(*attributes)))
    }


    /**
     * Adds the supplied text as a processing instruction element to the root of the document.
     *
     * @param text The inner text of the processing instruction element.
     * @param attributes Optional set of attributes to apply to this processing instruction.
     */
    fun globalProcessingInstruction(text: String, vararg attributes: Pair<String, String>) {
        _globalLevelProcessingInstructions.add(ProcessingInstructionElement(text, linkedMapOf(*attributes)))
    }

    /**
     * Add a DTD to the document.
     *
     * @param name The name of the DTD element. Not supplying this or passing <code>null</code> will default to [nodeName].
     * @param publicId The public declaration of the DTD.
     * @param systemId The system declaration of the DTD.
     */
    fun doctype(name: String? = null, publicId: String? = null, systemId: String? = null) {
        if (publicId != null && systemId == null) {
            throw IllegalStateException("systemId must be provided if publicId is provided")
        }

        doctype = Doctype(name ?: nodeName, publicId = publicId, systemId = systemId)
    }

    /**
     * Adds the specified namespace to the element.
     * <code>
     *     "url" {
     *         namespace("t", "http://someurl.org")
     *     }
     * </code>
     *
     * @param name The name of the namespace.
     * @param value The url or descriptor of the namespace
     */
    fun namespace(name: String, value: String) {
        attributes["xmlns:$name"] = value
    }

    /**
     * Adds a node to the element.
     * @param node The node to append.
     */
    fun addNode(node: Node) {
        children.add(node)
    }

    /**
     * Adds a node to the element after the specific node.
     * @param node The node to add
     * @param after The node to add [node] after
     *
     * @throws IllegalArgumentException If [after] can't be found
     */
    fun addNodeAfter(node: Node, after: Node) {
        val index = findIndex(after)
        if (index + 1 == children.size) {
            children.add(node)
        } else {
            children.add(index + 1, node)
        }
    }

    /**
     * Adds a node to the element before the specific node.
     * @param node The node to add
     * @param before The node to add [node] before
     *
     * @throws IllegalArgumentException If [before] can't be found
     */
    fun addNodeBefore(node: Node, before: Node) {
        children.add(findIndex(before), node)
    }

    /**
     * Removes a node from the element
     * @param node The node to remove
     *
     * @throws IllegalArgumentException If [node] can't be found
     */
    fun removeNode(node: Node) {
        val index = findIndex(node)
        children.removeAt(index)
    }

    /**
     * Replaces a node with a different node
     * @param existing The existing node to replace
     * @param newNode The node to replace [existing] with
     *
     * @throws IllegalArgumentException If [existing] can't be found
     */
    fun replaceNode(existing: Node, newNode: Node) {
        val index = findIndex(existing)

        children.removeAt(index)
        children.add(index, newNode)
    }

    /**
     * Returns a list containing only elements whose nodeName matches [name].
     */
    fun filter(name: String): List<Node> = filter { it.nodeName == name }

    /**
     * Returns a list containing only elements matching the given [predicate].
     */
    fun filter(predicate: (Node) -> Boolean): List<Node> = filterChildrenToNodes().filter(predicate)

    /**
     * Returns the first element whose nodeName matches [name].
     * @throws [NoSuchElementException] if no such element is found.
     */
    fun first(name: String): Node = filterChildrenToNodes().first { it.nodeName == name }

    /**
     * Returns the first element matching the given [predicate].
     * @throws [NoSuchElementException] if no such element is found.
     */
    fun first(predicate: (Element) -> Boolean): Element = children.first(predicate)

    /**
     * Returns the first element whose nodeName matches [name], or `null` if element was not found.
     */
    fun firstOrNull(name: String): Node? = filterChildrenToNodes().firstOrNull { it.nodeName == name }

    /**
     * Returns the first element matching the given [predicate], or `null` if element was not found.
     */
    fun firstOrNull(predicate: (Element) -> Boolean): Element? = children.firstOrNull(predicate)

    /**
     * Returns `true` if at least one element's nodeName matches [name].
     */
    fun exists(name: String): Boolean = filterChildrenToNodes().any { it.nodeName == name }

    /**
     * Returns `true` if at least one element matches the given [predicate].
     */
    fun exists(predicate: (Element) -> Boolean): Boolean = children.any(predicate)

    private fun filterChildrenToNodes(): List<Node> = children.filterIsInstance(Node::class.java)

    private fun findIndex(node: Node): Int {
        val index = children.indexOf(node)
        if (index == -1) {
            throw IllegalArgumentException("Node with nodeName '${node.nodeName}' is not a child of '$nodeName'")
        }
        return index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        if (nodeName != other.nodeName) return false
        if (encoding != other.encoding) return false
        if (version != other.version) return false
        if (attributes != other.attributes) return false
        if (_globalLevelProcessingInstructions != other._globalLevelProcessingInstructions) return false
        if (children != other.children) return false
        return true
    }

    override fun hashCode(): Int {
        var result = nodeName.hashCode()
        result = 31 * result + encoding.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + _globalLevelProcessingInstructions.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}