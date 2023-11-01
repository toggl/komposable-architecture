package com.toggl.komposable.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies.Companion.ALL_FILES
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import com.toggl.komposable.FileGenerationResult

abstract class KotlinPoetSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    final override fun process(resolver: Resolver): List<KSAnnotated> {
        for (fileGenerationResult in generateFiles(resolver)) {
            when (fileGenerationResult) {
                is FileGenerationResult.Failure -> {
                    logger.error(
                        message = fileGenerationResult.compilerError,
                        symbol = fileGenerationResult.node,
                    )
                }
                is FileGenerationResult.Success ->
                    fileGenerationResult.file.writeTo(
                        codeGenerator = codeGenerator,
                        dependencies = ALL_FILES,
                    )
            }
        }

        return emptyList()
    }

    internal abstract fun generateFiles(resolver: Resolver): Sequence<FileGenerationResult>
}
