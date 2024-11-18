package io.spine.protodata.java

import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.protodata.java.JavaTypeName.BOOLEAN
import io.spine.protodata.java.JavaTypeName.BYTE
import io.spine.protodata.java.JavaTypeName.CHAR
import io.spine.protodata.java.JavaTypeName.DOUBLE
import io.spine.protodata.java.JavaTypeName.FLOAT
import io.spine.protodata.java.JavaTypeName.INT
import io.spine.protodata.java.JavaTypeName.LONG
import io.spine.protodata.java.JavaTypeName.SHORT
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ArrayTypeName` should print array")
internal class ArrayTypeNameSpec {

    @Test
    fun `of primitive types`() {
        arrayOf(BYTE) shouldBe "byte[]"
        arrayOf(SHORT) shouldBe "short[]"
        arrayOf(INT) shouldBe "int[]"
        arrayOf(LONG) shouldBe "long[]"
        arrayOf(FLOAT) shouldBe "float[]"
        arrayOf(DOUBLE) shouldBe "double[]"
        arrayOf(BOOLEAN) shouldBe "boolean[]"
        arrayOf(CHAR) shouldBe "char[]"
    }

    @Test
    fun `of classes`() {
        val string = ClassName(String::class)
        arrayOf(string) shouldBe "java.lang.String[]"
    }

    @Test
    fun `of generic variables`() {
        arrayOf(TypeVariableName.T) shouldBe "T[]"
        arrayOf(TypeVariableName.E) shouldBe "E[]"
    }

    @Test
    fun `of parameterized types`() {
        val timestamp = ClassName(Timestamp::class)
        val comparator = ClassName(Comparator::class)
        val timestampComparator = ParameterizedClassName(comparator, timestamp)
        arrayOf(timestampComparator) shouldBe "java.util.Comparator<com.google.protobuf.Timestamp>[]"
    }

    @Test
    fun `of other arrays`() {
        val ints = ArrayTypeName(INT)
        arrayOf(ints) shouldBe "int[][]"
    }
}

private fun arrayOf(type: JavaTypeName) = "${ArrayTypeName(type)}"
