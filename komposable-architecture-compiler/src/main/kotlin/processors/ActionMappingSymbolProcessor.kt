package com.toggl.komposable.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.toggl.komposable.FileGenerationResult
import com.toggl.komposable.architecture.WrapperAction
import com.toggl.komposable.getSymbolsAnnotatedWith
import com.toggl.komposable.toCamelCase

class ActionMappingSymbolProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
) : KotlinPoetSymbolProcessor(codeGenerator, logger) {

    override fun generateFiles(resolver: Resolver): Sequence<FileGenerationResult> =
        resolver
            .getSymbolsAnnotatedWith(WrapperAction::class)
            .map { parentAction ->
                val parentActionClassName = parentAction.toClassName()
                val parentActionProperties = parentAction.getAllProperties().toList()
                when (parentActionProperties.size) {
                    1 -> {
                        // TODO: Figure out what to do when wrapper actions have interfaces
                        val parentActionSealedType = parentAction.superTypes.single().resolve()
                        val childActionPropertyInParentAction = parentActionProperties.single()
                        val childAction = childActionPropertyInParentAction.type.resolve()
                        val childActionClassName = childAction.toClassName()

                        val sealedParentActionTypeName = parentActionSealedType.toClassName().simpleName
                        val parentActionTypeName = parentActionClassName.canonicalName
                            .substring(parentActionClassName.packageName.length, parentActionClassName.canonicalName.length)
                            .trim('.')
                        val childActionTypeName = childActionClassName.simpleName

                        val childActionArgumentName = childActionTypeName.toCamelCase()
                        val sealedParentActionArgumentName = sealedParentActionTypeName.toCamelCase()

                        parentAction
                            .actionMappingFileBuilder()
                            .addImport(parentActionClassName.canonicalName, names = arrayOf(""))
                            .addFunction(
                                FunSpec
                                    .builder("map${sealedParentActionTypeName}To$childActionTypeName")
                                    .addParameter(sealedParentActionArgumentName, parentActionSealedType.toTypeName())
                                    .returns(childAction.makeNullable().toTypeName())
                                    .addCode("return if($sealedParentActionArgumentName is $parentActionTypeName) $sealedParentActionArgumentName.$childActionPropertyInParentAction else null")
                                    .build(),
                            )
                            .addFunction(
                                FunSpec
                                    .builder("map${childActionTypeName}To$sealedParentActionTypeName")
                                    .addParameter(childActionArgumentName, childActionClassName)
                                    .returns(parentActionClassName)
                                    .addCode("return $parentActionTypeName($childActionArgumentName)")
                                    .build(),
                            )
                            .build()
                            .run(FileGenerationResult::Success)
                    }
                    else -> {
                        val errorMessage = "${parentActionClassName.canonicalName} needs to have exactly one property to be annotated with @WrapperAction."
                        FileGenerationResult.Failure(errorMessage, parentAction)
                    }
                }
            }

    private fun KSClassDeclaration.actionMappingFileBuilder(): FileSpec.Builder {
        val className = toClassName()

        return FileSpec.builder(
            packageName = className.packageName,
            fileName = "${className.simpleName}ActionMappings",
        )
    }
}

class ActionMappingSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ActionMappingSymbolProcessor(environment.codeGenerator, environment.logger)
}
