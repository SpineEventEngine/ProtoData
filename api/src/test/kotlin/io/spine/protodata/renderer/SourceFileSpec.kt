/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.protodata.renderer

import com.intellij.psi.PsiJavaFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.string.ti
import io.spine.tools.code.Java
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SourceFile` should")
internal class SourceFileSpec {

    private lateinit var sourceFile: SourceFile<Java>

    @BeforeEach
    fun createSourceFile(@TempDir input: Path, @TempDir output: Path) {
        val className = "HelloWorld"
        val fileName = "$className.java"
        val file = input.resolve(fileName)
        file.writeText("""
            public class HelloWorld { }
            """.ti()
        )
        val set = SourceFileSet.create<Java>(input, output)
        sourceFile = set.file(file)
        sourceFile shouldNotBe null
    }

    @Test
    fun `provide 'PsiFile' instance`() {
        val firstPsi = assertDoesNotThrow {
            sourceFile.psi()
        }

        // The type of file was calculated by its name.
        (firstPsi is PsiJavaFile) shouldBe true

        // Repeated calls returns the same instance.
        sourceFile.psi() shouldBe firstPsi
        sourceFile.psi() shouldBe firstPsi

        // Overwriting the code should result in getting a new instance of `PsiJavaFile`.
        sourceFile.overwrite("""
            public final class HelloWorld { 
                System.out.println("Hello, World!"); 
            }
            """.ti()
        )
        val updatedPsi = assertDoesNotThrow {
            sourceFile.psi()
        }
        updatedPsi shouldNotBe firstPsi
        (updatedPsi is PsiJavaFile) shouldBe true
    }
}
