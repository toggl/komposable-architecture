package com.toggl.komposable

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import kotlin.reflect.KClass

fun Resolver.getSymbolsAnnotatedWith(kClass: KClass<*>) =
    this.getSymbolsWithAnnotation(kClass.qualifiedName.orEmpty())
        .filterIsInstance<KSClassDeclaration>()
        .filter(KSNode::validate)

fun String.toCamelCase() =
    first().lowercaseChar() + this.substring(1..length - 1)
