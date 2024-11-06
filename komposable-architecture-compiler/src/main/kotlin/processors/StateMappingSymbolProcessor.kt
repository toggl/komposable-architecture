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
import com.google.devtools.ksp.symbol.Modifier
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
                val isDataClass = parentState.modifiers.contains(Modifier.DATA)
                if (!isDataClass) {
                    val errorMessage = "${parentState.toClassName().canonicalName} must be a data class to create state mappings."
                    return@map FileGenerationResult.Failure(errorMessage, parentState)
                }
                val parentStateClassName = parentState.toClassName()
                val parentStateTypeName = parentStateClassName.simpleName
                val parentStateArgumentName = parentStateTypeName.toCamelCase()

                parentState
                    .findChildStateClasses(resolver)
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
${buildParentStateConstructorParameterList(childState, parentStateArgumentName, childStateArgumentName)}    )""",
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

                builder.appendLine("    $name = $parentStateArgumentName.$path,")
            }.toString()

    private fun buildParentStateConstructorParameterList(
        childState: KSClassDeclaration,
        parentStateArgumentName: String,
        childStateArgumentName: String,
    ): String =
        childState
            .getConstructors()
            .first()
            .parameters
            .fold(ParentStateCopyParameterNode("", "", "", emptyList())) { node, parameter ->
                val pathInChild = parameter.name?.getShortName().orEmpty()
                val pathInParent = parameter.getParentPath() ?: pathInChild

                // Function to edit the node in place.
                fun createOrEditNode(
                    node: ParentStateCopyParameterNode,
                    fullParentPath: List<String>,
                    depth: Int,
                ): ParentStateCopyParameterNode {
                    // The current path we are creating/editing is the full path - the depth we currently are at.
                    val currentFullPathInParent = fullParentPath.drop(depth)
                    if (currentFullPathInParent.isEmpty()) {
                        return node
                    }

                    val currentPathInParent = currentFullPathInParent.first()
                    val existingNode = node.children.firstOrNull { it.pathInParent == currentPathInParent }
                    val nodeToEditNext =
                        existingNode
                            ?: ParentStateCopyParameterNode(
                                currentPathInParent,
                                pathInChild,
                                // dropLast(1) removes the name of the property we are copying.
                                currentFullPathInParent.dropLast(1).joinToString("."),
                                emptyList(),
                            )
                    val newNode = createOrEditNode(nodeToEditNext, fullParentPath, depth + 1)
                    val newChildren = node.children.filter { it.pathInParent == currentPathInParent } + listOf(newNode)
                    return node.copy(children = newChildren)
                }

                var nodeToReturn = node
                val rawPathComponents = pathInParent.split(".")
                val pathComponents = List(rawPathComponents.size) { i -> rawPathComponents.take(i + 1) }
                for (component in pathComponents) {
                    nodeToReturn = createOrEditNode(node, component, 0)
                }

                nodeToReturn
            }.run {
                fun traverseDepthFirst(node: ParentStateCopyParameterNode, stringBuilder: StringBuilder, depth: Int) {
                    val tabulation = "    ".repeat(depth + 1)

                    // If there are no children, we are simply assigning the property.
                    // Otherwise, it means we are doing a property copy of a complex object.
                    if (node.children.isEmpty()) {
                        stringBuilder.appendLine("$tabulation${node.pathInParent} = $childStateArgumentName.${node.pathInChild},")
                    } else {
                        val includeCopyLine = depth > 0

                        // Skip the copy line for the first depth.
                        if (includeCopyLine) {
                            stringBuilder.appendLine("$tabulation${node.pathInParent} = $parentStateArgumentName.${node.fullPathInParent}.copy(")
                        }
                        for (child in node.children) {
                            traverseDepthFirst(child, stringBuilder, depth + 1)
                        }

                        // Skip closing the parenthesis we did not open above.
                        if (includeCopyLine) {
                            stringBuilder.appendLine("$tabulation)")
                        }
                    }
                }

                val builder = StringBuilder()
                traverseDepthFirst(this, builder, 0)
                builder.toString()
            }

    private fun KSClassDeclaration.stateMappingFileBuilder(): FileSpec.Builder {
        val className = toClassName()

        return FileSpec.builder(
            packageName = className.packageName,
            fileName = "${className.simpleName}StateMappings",
        )
    }

    // Yes, we need to do this instead of simply calling getAnnotationsByType<ChildStates>().
    // Explanation here: https://github.com/google/ksp/issues/888
    private fun KSClassDeclaration.findChildStateClasses(resolver: Resolver) =
        annotationsWithType(ChildStates::class)
            .flatMap { it.arguments.toList().single().value as ArrayList<*> }
            .mapNotNull { it as? KSType }
            .mapNotNull { resolver.getClassDeclarationByName(it.toClassName().canonicalName) }

    private fun KSValueParameter.getParentPath(): String? =
        annotationsWithType(ParentPath::class)
            .firstOrNull()
            ?.arguments?.first()?.value as? String

    private fun KSAnnotated.annotationsWithType(kClass: KClass<*>) =
        annotations.toList().filter {
            it.annotationType.resolve().toClassName().canonicalName == kClass.qualifiedName
        }

    private data class ParentStateCopyParameterNode(
        val pathInParent: String,
        val pathInChild: String,
        val fullPathInParent: String,
        val children: List<ParentStateCopyParameterNode>,
    )
}

class StateMappingSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        StateMappingSymbolProcessor(environment.codeGenerator, environment.logger)
}
