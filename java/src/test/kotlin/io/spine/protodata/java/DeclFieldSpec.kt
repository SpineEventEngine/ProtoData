package io.spine.protodata.java

import assertCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`DeclField` should")
internal class DeclFieldSpec : AnElementSpec() {

    private val surname = DeclField<String>(
        modifiers = "private final",
        type = ClassName(String::class),
        name = "surname"
    )

    @Test
    fun `declare a Java field`() {
        assertCode(surname, "private final java.lang.String surname;")
    }

    @Test
    fun `set a value to the declared field`() {
        val anderson = "Anderson"
        val expression = StringLiteral(anderson)
        assertCode(surname.set(expression), "surname = \"$anderson\";")
        assertCode(surname.set(expression, useThis = true), "this.surname = \"$anderson\";")
    }

    @Test
    fun `provide a read access to the declared field`() {
        assertCode(surname.read(), "surname")
        assertCode(surname.read(useThis = true), "this.surname")
    }
}
