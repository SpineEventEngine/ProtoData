package io.spine.protodata.java

import assertCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`InitField` should")
internal class InitFieldSpec {

    private val surname = InitField(
        modifiers = "private final",
        type = ClassName(String::class),
        name = "surname",
        value = StringLiteral("Anderson")
    )

    @Test
    fun `create an initialized Java field`() {
        assertCode(surname, "private final java.lang.String surname = \"Anderson\";")
    }

    @Test
    fun `provide a read access to the created field`() {
        assertCode(surname.read(), "surname")
        assertCode(surname.read(useThis = true), "this.surname")
    }
}
