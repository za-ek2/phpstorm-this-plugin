package eu.zaek.phpstormthisplugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Function as PhpFunction
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.PhpClass

class ContentThisCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val project = position.project
                    val filePath = parameters.originalFile.virtualFile?.path ?: return
                    val targetClass = ContentThisSettings.getInstance().getTargetClassForFile(filePath, project) ?: return

                    val methodRef = PsiTreeUtil.getParentOfType(position, MethodReference::class.java)
                    val fieldRef = PsiTreeUtil.getParentOfType(position, FieldReference::class.java)
                    val memberRef = methodRef ?: fieldRef ?: return

                    val base = memberRef.firstChild ?: return

                    val enclosingFunction = PsiTreeUtil.getParentOfType(position, PhpFunction::class.java, false)
                    if (enclosingFunction != null) {
                        if (!enclosingFunction.name.isNullOrEmpty()) return
                        val outerFunction = PsiTreeUtil.getParentOfType(enclosingFunction, PhpFunction::class.java, true)
                        if (outerFunction != null && !outerFunction.name.isNullOrEmpty()) return
                    }

                    val phpIndex = PhpIndex.getInstance(project)

                    // Кейс 1: Просто $this->
                    if (base is Variable && base.name == "this") {
                        val appClasses = phpIndex.getClassesByFQN(targetClass)
                        addMembers(appClasses, result)
                        return
                    }

                    // Кейс 2: Выражение слева от ->
                    if (base is PhpTypedElement) {
                        var foundAny = false
                        val baseTypes = base.type.global(project).getTypes()

                        // Пытаемся разрешить тип стандартным путем
                        baseTypes.forEach { typeName ->
                            val cleanType = typeName.substringBefore("|").substringBefore("?").trim()
                            if (cleanType.isNotEmpty() && !cleanType.startsWith("#") && cleanType != "mixed" && cleanType != "void") {
                                val fqn = if (!cleanType.startsWith("\\")) "\\$cleanType" else cleanType
                                val classes = phpIndex.getClassesByFQN(fqn)
                                if (classes.isNotEmpty()) {
                                    addMembers(classes, result)
                                    foundAny = true
                                }
                            }
                        }

                        // Ручной fallback, если PhpStorm не смог разрешить цепочку $this->method()
                        if (!foundAny && base is MethodReference) {
                            val innerBase = base.firstChild
                            if (innerBase is Variable && innerBase.name == "this") {
                                val methodName = base.name
                                if (methodName != null) {
                                    val appClasses = phpIndex.getClassesByFQN(targetClass)
                                    appClasses.forEach { appClass ->
                                        var currentClass: PhpClass? = appClass
                                        while (currentClass != null) {
                                            val method = currentClass.methods.find { it.name == methodName }
                                            if (method != null) {
                                                // Нашли метод, достаем его возвращаемый тип
                                                val returnTypes = method.type.getTypes()
                                                returnTypes.forEach { retTypeName ->
                                                    val cleanRet = retTypeName.trim()
                                                    if (cleanRet.isNotEmpty() && !cleanRet.startsWith("#") && cleanRet != "mixed") {
                                                        val fqn = if (!cleanRet.startsWith("\\")) "\\$cleanRet" else cleanRet
                                                        val classes = phpIndex.getClassesByFQN(fqn)
                                                        addMembers(classes, result)
                                                    }
                                                }
                                                break
                                            }
                                            currentClass = currentClass.superClass
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun addMembers(classes: Collection<PhpClass>, result: CompletionResultSet) {
        classes.forEach { phpClass ->
            // МЕТОДЫ (с автоматическими скобками)
            phpClass.methods.forEach { method ->
                if (!method.modifier.isPrivate && !method.modifier.isProtected) {
                    result.addElement(
                        LookupElementBuilder.create(method)
                            .withIcon(method.getIcon())
                            .withTypeText(phpClass.name, true)
                            .withInsertHandler { ctx, _ ->
                                val document = ctx.document
                                val tailOffset = ctx.tailOffset
                                // Если скобок еще нет, вставляем их и ставим курсор внутрь
                                if (tailOffset >= document.textLength || document.charsSequence[tailOffset] != '(') {
                                    document.insertString(tailOffset, "()")
                                    ctx.editor.caretModel.moveToOffset(tailOffset + 1)
                                }
                            }
                    )
                }
            }
            // СВОЙСТВА
            phpClass.fields.forEach { field ->
                if (!field.modifier.isPrivate && !field.modifier.isProtected) {
                    result.addElement(
                        LookupElementBuilder.create(field)
                            .withIcon(field.getIcon())
                            .withTypeText(field.type.toString(), true)
                    )
                }
            }
        }
    }
}