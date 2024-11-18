package io.spine.protodata.java

import assertCode
import com.google.protobuf.Message
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`KeywordExpressions` should provide an expression")
internal class KeywordExpressionsSpec {

    @Test
    fun `for 'null' keyword`() = assertCode(Null, "null")

    @Test
    fun `for 'this' keyword`() = assertCode(This<Message>(), "this")
}
