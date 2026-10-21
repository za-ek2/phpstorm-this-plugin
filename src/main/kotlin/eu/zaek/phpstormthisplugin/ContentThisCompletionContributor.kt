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
import com.jetbrains.php.lang.psi.elements.AssignmentExpression // Импорт

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
                    val file = parameters.originalFile

                    val filePath = file.virtualFile?.path ?: return
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

                    // Кейс 2: Локальная переменная, созданная из $this->method()
                    if (base is Variable && base.name != "this") {
                        // Ищем область видимости (функцию или весь файл)
                        val scope = PsiTreeUtil.getParentOfType(base, PhpFunction::class.java) ?: file

                        // Находим ВСЕ присваивания в этой области, которые были ДО текущей позиции
                        val assignments = PsiTreeUtil.findChildrenOfType(scope, AssignmentExpression::class.java)
                            .filter { it.variable?.text == base.text && it.textOffset < base.textOffset }
                            .sortedByDescending { it.textOffset } // Берем самое ближайшее сверху

                        val latestAssignment = assignments.firstOrNull()
                        if (latestAssignment != null) {
                            var value = latestAssignment.value
                            // Если выражение обернуто в скобки, разворачиваем
                            if (value is com.jetbrains.php.lang.psi.elements.ParenthesizedExpression) {
                                value = PsiTreeUtil.getChildOfType(value, MethodReference::class.java)
                            }

                            if (value is MethodReference) {
                                val innerBase = value.firstChild
                                if (innerBase is Variable && innerBase.name == "this") {
                                    val methodName = value.name
                                    if (methodName != null) {
                                        val appClasses = phpIndex.getClassesByFQN(targetClass)
                                        appClasses.forEach { appClass ->
                                            var currentClass: PhpClass? = appClass
                                            while (currentClass != null) {
                                                val method = currentClass.methods.find { it.name == methodName }
                                                if (method != null) {
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
                                        return // Нашли и обработали, дальше не идем
                                    }
                                }
                            }
                        }
                    }

                    // Кейс 3: Цепочка (например, $this->request()->)
                    if (base is PhpTypedElement && base !is Variable) {
                        var foundAny = false
                        val baseTypes = base.type.global(project).getTypes()

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
            phpClass.methods.forEach { method ->
                if (!method.modifier.isPrivate && !method.modifier.isProtected) {
                    result.addElement(
                        LookupElementBuilder.create(method)
                            .withIcon(method.getIcon())
                            .withTypeText(phpClass.name, true)
                            .withInsertHandler { ctx, _ ->
                                val document = ctx.document
                                val tailOffset = ctx.tailOffset
                                if (tailOffset >= document.textLength || document.charsSequence[tailOffset] != '(') {
                                    document.insertString(tailOffset, "()")
                                    ctx.editor.caretModel.moveToOffset(tailOffset + 1)
                                }
                            }
                    )
                }
            }
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