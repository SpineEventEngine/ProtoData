package io.spine.protodata.java

import assertCode
import com.google.protobuf.Timestamp
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
        assertArrayName(BYTE, "byte[]")
        assertArrayName(SHORT, "short[]")
        assertArrayName(INT, "int[]")
        assertArrayName(LONG, "long[]")
        assertArrayName(FLOAT, "float[]")
        assertArrayName(DOUBLE, "double[]")
        assertArrayName(BOOLEAN, "boolean[]")
        assertArrayName(CHAR, "char[]")
    }

    @Test
    fun `of classes`() {
        val string = ClassName(String::class)
        assertArrayName(string, "java.lang.String[]")
    }

    @Test
    fun `of generic variables`() {
        assertArrayName(TypeVariableName.T, "T[]")
        assertArrayName(TypeVariableName.E, "E[]")
    }

    @Test
    fun `of parameterized types`() {
        val timestamp = ClassName(Timestamp::class)
        val comparator = ClassName(Comparator::class)
        val timestampComparator = ParameterizedClassName(comparator, timestamp)
        assertArrayName(
            timestampComparator,
            "java.util.Comparator<com.google.protobuf.Timestamp>[]"
        )
    }

    @Test
    fun `of other arrays`() {
        val ints = ArrayTypeName(INT)
        assertArrayName(ints, "int[][]")
    }
}

private fun assertArrayName(type: JavaTypeName, expected: String) =
    assertCode(ArrayTypeName(type), expected)
