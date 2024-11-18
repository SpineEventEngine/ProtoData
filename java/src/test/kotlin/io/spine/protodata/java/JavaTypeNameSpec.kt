package io.spine.protodata.java

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`JavaTypeName` should")
internal class JavaTypeNameSpec {

    private val myCanonical = "my-canonical-name"
    private val myType = typeName(myCanonical)

    @Test
    fun `return the provided canonical name`() {
        myType.canonical shouldBe myCanonical
    }

    @Test
    fun `return the canonical name as string representation`() {
        "$myType" shouldBe myCanonical
    }

    @Test
    fun `compare two elements`() {
        val myType2 = typeName(myCanonical)
        myType shouldBe myType2
        myType.hashCode() shouldBe myType2.hashCode()

        val anotherType = typeName("another-canonical-name")
        myType shouldNotBe anotherType
        myType.hashCode() shouldNotBe anotherType.hashCode()
    }
}

private fun typeName(canonical: String) = object : JavaTypeName() {
    override val canonical: String = canonical
}
