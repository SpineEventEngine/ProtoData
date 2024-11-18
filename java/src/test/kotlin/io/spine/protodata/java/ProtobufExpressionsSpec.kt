package io.spine.protodata.java

import assertCode
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.spine.protobuf.TypeConverter
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.TypeInstances
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.fieldName
import io.spine.protodata.ast.fieldType
import io.spine.protodata.ast.mapEntryType
import io.spine.protodata.ast.toFieldType
import io.spine.protodata.ast.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ProtobufExpressions` should")
internal class ProtobufExpressionsSpec {

    private val message = ReadVar<Message>("messageVar")

    @Test
    fun `pack value into 'Any'`() {
        val packed = message.packToAny()
        assertCode(packed, "${TypeConverter::class.qualifiedName}.toAny(messageVar)")
    }

    @Test
    fun `yield the given 'ByteString' using 'copyFrom()'`() {
        val bytes = "foobar".toByteArray()
        val expression = CopyByteString(ByteString.copyFrom(bytes))
        assertCode(
            expression,
            "${ByteString::class.qualifiedName}.copyFrom(new byte[]{${bytes.joinToString()}})"
        )
    }

    @Nested inner class
    `for message expressions` {

        private val fieldName: FieldName = fieldName { value = "baz" }
        private val typeName: TypeName = typeName {
            simpleName = "StubType"
            packageName = "given.message"
        }

        @Test
        fun `access a singular field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = TypeInstances.string.toFieldType()
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBaz()")
        }

        @Test
        fun `access a list field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = fieldType { list = TypeInstances.string }
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBazList()")
        }

        @Test
        fun `access a map field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = fieldType {
                    map = mapEntryType {
                        keyType = PrimitiveType.TYPE_STRING
                        valueType = TypeInstances.string
                    }
                }
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBazMap()")
        }
    }
}
