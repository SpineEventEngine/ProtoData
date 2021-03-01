/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.subscriber

/**
 * A description of a certain change to source code.
 *
 * [Subscriber]s produce `CodeEnhancement`s and [Renderer][io.spine.protodata.renderer.Renderer]s
 * consume them.
 *
 * This is a marker interface. Concrete implementations must include all the info needed by
 * the renderers to apply the enhancement to source code.
 *
 * An enhancement should be self-sufficient. This means not relying on other enhancements.
 * The processing mechanism for an enhancement should not assume that another enhancement was
 * processed beforehand. For example, in case if a language element, such as a class, a method,
 * or a field, which is being modified is missing, the enhancement should specify whether or not
 * it is OK to create it.
 */
public interface CodeEnhancement

/**
 * A code enhancement which overrides all the other enhancements and disallows launching
 * the renderer.
 *
 * If [Subscriber]s produce no enhancements, the renderer is launched anyway, as it may have some
 * kind of unconditional logic. To prevent launching the renderer (and also, possibly, speed up
 * a processing which is not going to produce any changes anyway), a subscriber should produce
 * the `SkipEverything` enhancement.
 */
public object SkipEverything: CodeEnhancement
