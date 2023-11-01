package com.toggl.komposable

import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.FileSpec

internal sealed interface FileGenerationResult {
    data class Success(val file: FileSpec) : FileGenerationResult
    data class Failure(val compilerError: String, val node: KSNode) : FileGenerationResult
}
