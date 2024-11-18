package io.spine.protodata.java

import assertCode
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.spine.protobuf.TypeConverter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ProtobufExpressions` should")
internal class ProtobufExpressionsSpec {

    @Test
    fun `pack value into 'Any'`() {
        val expression = ReadVar<Message>("myMessage")
        val packed = expression.packToAny()
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
}
