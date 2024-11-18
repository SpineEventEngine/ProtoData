package io.spine.protodata.java

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.DisplayName

@DisplayName("`AnElement` should")
internal class AnElementSpec {

    private val mainCode = "public static void main(String[] args) { }"
    private val main = AnElement(mainCode)

    @Test
    fun `return the provided code`() {
        main.code shouldBe mainCode
    }

    @Test
    fun `return the code as string representation`() {
        "$main" shouldBe mainCode
    }

    @Test
    fun `compare two elements`() {
        val main2 = AnElement(mainCode)
        main shouldBe main2
        main.hashCode() shouldBe main2.hashCode()

        val anotherFunc = AnElement("public void printSum(int a, int b) { }")
        main shouldNotBe anotherFunc
        main.hashCode() shouldNotBe anotherFunc.hashCode()
    }
}
