package com.toggl.komposable.processors

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.toggl.komposable.FileGenerationResult
import com.toggl.komposable.architecture.ChildStates
import com.toggl.komposable.architecture.ParentPath
import com.toggl.komposable.getSymbolsAnnotatedWith
import com.toggl.komposable.toCamelCase
import kotlin.reflect.KClass

class StateMappingSymbolProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
) : KotlinPoetSymbolProcessor(codeGenerator, logger) {
    override fun generateFiles(resolver: Resolver): Sequence<FileGenerationResult> =
        resolver
            .getSymbolsAnnotatedWith(ChildStates::class)
            .map { parentState ->
                val parentStateClassName = parentState.toClassName()
                val parentStateTypeName = parentStateClassName.simpleName

                val parentStateArgumentName = parentStateTypeName.toCamelCase()

                parentState
                    .findChildStateClasses()
                    .mapNotNull { resolver.getClassDeclarationByName(it.toClassName().canonicalName) }
                    .fold(parentState.stateMappingFileBuilder()) { fileBuilder, childState ->
                        val childStateClassName = childState.toClassName()
                        val childStateTypeName = childStateClassName.simpleName
                        val childStateArgumentName = childStateTypeName.toCamelCase()

                        fileBuilder
                            .addFunction(
                                FunSpec
                                    .builder("map${parentStateTypeName}To$childStateTypeName")
                                    .addParameter(parentStateArgumentName, parentState.toClassName())
                                    .returns(childStateClassName)
                                    // .trimIndent does not behave the way we expect here, hence the weird format.
                                    .addCode(
                                        """return $childStateTypeName(
    ${buildChildStateConstructorParameterList(childState, parentStateArgumentName)})""",
                                    )
                                    .build(),
                            )
                            .addFunction(
                                FunSpec
                                    .builder("map${childStateTypeName}To$parentStateTypeName")
                                    .addParameter(parentStateArgumentName, parentStateClassName)
                                    .addParameter(childStateArgumentName, childStateClassName)
                                    .returns(parentStateClassName)
                                    .addCode(
                                        """return $parentStateArgumentName.copy(
    ${buildParentStateConstructorParameterList(childState, childStateArgumentName)}    )""",
                                    )
                                    .build(),
                            )

                        fileBuilder
                    }.build().run(FileGenerationResult::Success)
            }

    private fun buildChildStateConstructorParameterList(
        childState: KSClassDeclaration,
        parentStateArgumentName: String,
    ): String =
        childState
            .getConstructors()
            .first()
            .parameters
            .fold(StringBuilder()) { builder, parameter ->
                val name = parameter.name?.getShortName().orEmpty()
                val path = parameter.getParentPath() ?: name

                builder.appendLine("$name = $parentStateArgumentName.$path,")
            }.toString()

    private fun buildParentStateConstructorParameterList(
        childState: KSClassDeclaration,
        childStateArgumentName: String,
    ): String =
        childState
            .getConstructors()
            .first()
            .parameters
            .fold(StringBuilder()) { builder, parameter ->
                val name = parameter.name?.getShortName().orEmpty()
                val path = parameter.getParentPath() ?: name

                builder.appendLine("    $path = $childStateArgumentName.$name,")
            }.toString()

    private fun KSClassDeclaration.stateMappingFileBuilder(): FileSpec.Builder {
        val className = toClassName()

        return FileSpec.builder(
            packageName = className.packageName,
            fileName = "${className.simpleName}StateMappings",
        )
    }

    // Yes, we need to do this instead of simply calling getAnnotationsByType<ChildStates>().
    // Explanation here: https://github.com/google/ksp/issues/888
    private fun KSClassDeclaration.findChildStateClasses() =
        annotationsWithType(ChildStates::class)
            .flatMap { it.arguments.toList().single().value as ArrayList<*> }
            .mapNotNull { it as? KSType }


    private fun KSValueParameter.getParentPath(): String? =
        annotationsWithType(ParentPath::class)
            .firstOrNull()
            ?.arguments?.first()?.value as? String

    private fun KSAnnotated.annotationsWithType(kClass: KClass<*>) =
        annotations.toList().filter {
            it.annotationType.resolve().toClassName().canonicalName == kClass.qualifiedName
        }
}

class StateMappingSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        StateMappingSymbolProcessor(environment.codeGenerator, environment.logger)
}
