package io.spine.protodata.java

import assertCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Literal` expression should print")
internal class LiteralSpec {

    @Test
    fun `a number`() = assertCode(Literal(42), "42")

    @Test
    fun `a boolean`() = assertCode(Literal(false), "false")

    @Test
    fun `a string`() = assertCode(StringLiteral("foo"), "\"foo\"")

    @Test
    fun `an arbitrary value`() {
        val anything = "Frankie says relax"
        val expression = Literal(anything)
        assertCode(expression, anything)
    }
}
