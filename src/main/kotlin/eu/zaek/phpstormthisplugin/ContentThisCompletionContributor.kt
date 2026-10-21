package eu.zaek.phpstormthisplugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Function as PhpFunction
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.ParenthesizedExpression

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

                    // Проверка контекста (только замыкания или глобальная область)
                    val enclosingFunction = PsiTreeUtil.getParentOfType(position, PhpFunction::class.java, false)
                    if (enclosingFunction != null) {
                        if (!enclosingFunction.name.isNullOrEmpty()) return
                        val outerFunction = PsiTreeUtil.getParentOfType(enclosingFunction, PhpFunction::class.java, true)
                        if (outerFunction != null && !outerFunction.name.isNullOrEmpty()) return
                    }

                    val phpIndex = PhpIndex.getInstance(project)

                    // Запускаем рекурсивный поиск типа для выражения слева от стрелки
                    val classes = resolveTypeFromExpression(base, targetClass, phpIndex, mutableSetOf())
                    if (classes.isNotEmpty()) {
                        addMembers(classes, result)
                    }
                }
            }
        )
    }

    /**
     * Рекурсивно определяет тип PHP-класса для выражения.
     * Понимает: $this, $var, $var->method(), $this->method() и т.д.
     */
    private fun resolveTypeFromExpression(
        expression: PsiElement?,
        targetClass: String,
        phpIndex: PhpIndex,
        visited: MutableSet<String>
    ): List<PhpClass> {
        if (expression == null) return emptyList()

        // 1. Если это $this — возвращаем наш целевой класс
        if (expression is Variable && expression.name == "this") {
            return phpIndex.getClassesByFQN(targetClass).toList() // <--- ДОБАВЛЕНО .toList()
        }

        // 2. Если это локальная переменная — ищем её присваивание выше по коду
        if (expression is Variable) {
            val varName = expression.name
            if (visited.contains(varName)) return emptyList() // Защита от бесконечной рекурсии
            visited.add(varName)

            val scope = PsiTreeUtil.getParentOfType(expression, PhpFunction::class.java) ?: expression.containingFile
            val assignments = PsiTreeUtil.findChildrenOfType(scope, AssignmentExpression::class.java)
                .filter { it.variable?.text == expression.text && it.textOffset < expression.textOffset }
                .sortedByDescending { it.textOffset } // Берем самое ближайшее сверху

            val latest = assignments.firstOrNull()
            return if (latest != null) {
                resolveTypeFromExpression(latest.value, targetClass, phpIndex, visited)
            } else {
                emptyList()
            }
        }

        // 3. Если это вызов метода ($var->method() или $this->method())
        if (expression is MethodReference) {
            val methodName = expression.name ?: return emptyList()
            val base = expression.firstChild

            // Рекурсивно узнаем, какого класса база (например, для $ingExplainer->getStructure() базой будет $ingExplainer)
            val baseClasses = resolveTypeFromExpression(base, targetClass, phpIndex, visited)
            val result = mutableListOf<PhpClass>()

            for (baseClass in baseClasses) {
                var currentClass: PhpClass? = baseClass
                while (currentClass != null) {
                    val method = currentClass.methods.find { it.name == methodName }
                    if (method != null) {
                        val returnTypes = method.type.getTypes()
                        returnTypes.forEach { retTypeName ->
                            val cleanRet = retTypeName.substringBefore("|").substringBefore("?").trim()
                            if (cleanRet.isNotEmpty() && !cleanRet.startsWith("#") && cleanRet != "mixed" && cleanRet != "void") {
                                val fqn = if (!cleanRet.startsWith("\\")) "\\$cleanRet" else cleanRet
                                result.addAll(phpIndex.getClassesByFQN(fqn))
                            }
                        }
                        break
                    }
                    currentClass = currentClass.superClass
                }
            }
            return result
        }

        // 4. Если выражение в скобках — разворачиваем
        if (expression is ParenthesizedExpression) {
            val child = PsiTreeUtil.getChildOfType(expression, PsiElement::class.java)
            return resolveTypeFromExpression(child, targetClass, phpIndex, visited)
        }

        return emptyList()
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