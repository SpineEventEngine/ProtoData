package io.spine.protodata.java

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Expression` should")
internal class ExpressionSpec {

    private val eight = "4 + 4"

    @Test
    fun `return the provided code`() {
        Expression<Int>(eight).code shouldBe eight
    }

    @Test
    fun `return code as string representation`() {
        "${Expression<Int>(eight)}" shouldBe eight
    }

    @Test
    fun `compare two expressions`() {
        val expression = "2 + 2 * 2"
        val six = Expression<Int>(expression)
        val anotherSix = Expression<Int>(expression)
        six shouldBe anotherSix
        six.hashCode() shouldBe anotherSix.hashCode()

        val nine = Expression<Int>("9")
        six shouldNotBe nine
        six.hashCode() shouldNotBe nine.hashCode()
    }
}
