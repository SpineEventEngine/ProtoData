package io.spine.protodata.java

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.DisplayName

@DisplayName("`AnElement` should")
internal class AnElementSpec {

    private val main = "public static void main(String[] args) { }"

    @Test
    fun `return the provided code`() {
        AnElement(main).code shouldBe main
    }

    @Test
    fun `return code as string representation`() {
        "${AnElement(main)}" shouldBe main
    }

    @Test
    fun `compare two elements`() {
        val code = "private static final"
        val modifiers = AnElement(code)
        val anotherModifiers = AnElement(code)
        modifiers shouldBe anotherModifiers
        modifiers.hashCode() shouldBe anotherModifiers.hashCode()

        val deskClass = AnElement("final class Desk { }")
        modifiers shouldNotBe deskClass
        modifiers.hashCode() shouldNotBe deskClass.hashCode()
    }
}
