
Kotlin XML Builder
=

## Credits
Forked from [https://github.com/redundent/kotlin-xml-builder](https://github.com/redundent/kotlin-xml-builder) to update to the lastest
version of kotlin, inling, and modifications etc to make it easier to use / better

License
-
Apache 2.0

Usage
=
To use in Gradle, simply add the Maven Central repository and then add the following dependency.
```gradle
repositories {
    mavenCentral()
}

dependencies {
    compile("org.redundent:kotlin-xml-builder:[VERSION]")
}
```

Similarly in Maven:
```xml
<dependencies>
    <dependency>
        <groupId>org.redundent</groupId>
        <artifactId>kotlin-xml-builder</artifactId>
        <version>[VERSION]</version>
    </dependency>
</dependencies>
```

Example
=
```kotlin
val people = xml("people") {
    xmlns = "http://example.com/people"
    "person" {
        attribute("id", 1)
        "firstName" {
            -"John"
        }
        "lastName" {
            -"Doe"
        }
        "phone" {
            -"555-555-5555"
        }
    }
}

val asString = people.toString()
```
produces
```xml
<people xmlns="http://example.com/people">
    <person id="1">
        <firstName>
            John
        </firstName>
        <lastName>
            Doe
        </lastName>
        <phone>
            555-555-5555
        </phone>
    </person>
</people>
```

```kotlin
class Person(val id: Long, val firstName: String, val lastName: String, val phone: String)

val listOfPeople = listOf(
    Person(1, "John", "Doe", "555-555-5555"),
    Person(2, "Jane", "Doe", "555-555-6666")
)

val people = xml("people") {
    xmlns = "http://example.com/people"
    for (person in listOfPeople) {
        "person" {
            attribute("id", person.id)
            "firstName" {
                -person.firstName
            }
            "lastName" {
                -person.lastName
            }
            "phone" {
                -person.phone
            }
        }
    }    
}

val asString = people.toString()
```
produces
```xml
<people xmlns="http://example.com/people">
    <person id="1">
        <firstName>
            John
        </firstName>
        <lastName
            >Doe
        </lastName>
        <phone>
            555-555-5555
        </phone>
    </person>
    <person id="2">
        <firstName>
            Jane
        </firstName>
        <lastName>
            Doe
        </lastName>
        <phone>
            555-555-6666
        </phone>
    </person>
</people>
```
### Processing Instructions
You can add processing instructions to any element by using the `processingInstruction` method.

```kotlin
xml("root") {
    processingInstruction("instruction")
}
```

```xml
<root>
    <?instruction?>
</root>
```

#### Global Instructions
Similarly you can add a global (top-level) instruction by call `globalProcessingInstruction` on the
root node. This method only applies to the root. If it is called on any other element, it will be ignored.


```kotlin
xml("root") {
    globalProcessingInstruction("xml-stylesheet", "type" to "text/xsl", "href" to "style.xsl")
}
```

```xml
<?xml-stylesheet type="text/xsl" href="style.xsl"?>
<root/>
```

## DOCTYPE

As of version 1.7.4, you can specify a DTD (Document Type Declaration).

```kotlin
xml("root") {
    doctype(systemId = "mydtd.dtd")
}
```

```xml
<!DOCTYPE root SYSTEM "mydtd.dtd">
<root/>
```

### Limitations with DTD

Complex DTDs are not supported.

## Print Options
You can now control how your xml will look when rendered by passing the new PrintOptions class as an argument to `toString`.

`pretty` - This is the default and will produce the xml you see above.

`singleLineTextElements` - This will render single text element nodes on a single line if `pretty` is true
```xml
<root>
    <element>value</element>
</root>
```
as opposed to:
```xml
<root>
    <element>
        value
    </element>
</root>
```
`useSelfClosingTags` - Use `<element/>` instead of `<element></element>` for empty tags
`useCharacterReference` - Use character references instead of escaped characters. i.e. `&#39;` instead of `&apos;`