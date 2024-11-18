package io.spine.protodata.java

import assertCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`DeclVar` should")
internal class DeclVarSpec {

    private val surname = DeclVar<String>(
        type = ClassName(String::class),
        name = "surname"
    )

    @Test
    fun `declare a Java variable`() {
        assertCode(surname, "java.lang.String surname;")
    }

    @Test
    fun `provide a read access to the created variable`() {
        assertCode(surname.read(), "surname")
    }

    @Test
    fun `set a value to the declared variable`() {
        val anderson = "Anderson"
        val expression = StringLiteral(anderson)
        assertCode(surname.set(expression), "surname = \"$anderson\";")
    }
}
