package eu.zaek.phpstormthisplugin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.elements.Function as PhpFunction
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

class ContentThisTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = 'X'

    override fun getType(element: PsiElement): PhpType? {
        val project = element.project
        val filePath = element.containingFile.virtualFile?.path ?: return null
        val targetClass = ContentThisSettings.getInstance().getTargetClassForFile(filePath, project) ?: return null

        val enclosingFunction = PsiTreeUtil.getParentOfType(element, PhpFunction::class.java, false)
        if (enclosingFunction != null) {
            if (!enclosingFunction.name.isNullOrEmpty()) return null
            val outerFunction = PsiTreeUtil.getParentOfType(enclosingFunction, PhpFunction::class.java, true)
            if (outerFunction != null && !outerFunction.name.isNullOrEmpty()) return null
        }

        if (element is Variable && element.name == "this") {
            return PhpType().add(targetClass)
        }

        if (element is MethodReference) {
            val base = element.firstChild
            if (base is Variable && base.name == "this") {
                val methodName = element.name
                if (methodName != null) {
                    return PhpType().add("#" + getKey() + "$targetClass::$methodName")
                }
            }
        }

        return null
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project
    ): Collection<PhpNamedElement> {
        if (!expression.startsWith(getKey().toString())) return emptyList()
        val signature = expression.substring(1)

        if (signature.contains("::")) {
            val parts = signature.split("::")
            val fqn = parts[0]
            val methodName = parts[1]

            val phpIndex = PhpIndex.getInstance(project)
            val classes = phpIndex.getClassesByFQN(fqn)
            val result = mutableListOf<PhpNamedElement>()

            for (phpClass in classes) {
                var currentClass: PhpClass? = phpClass
                while (currentClass != null) {
                    val method = currentClass.methods.find { it.name == methodName }
                    if (method != null) {
                        result.add(method)
                        break
                    }
                    currentClass = currentClass.superClass
                }
            }
            return result
        } else {
            return PhpIndex.getInstance(project).getClassesByFQN(signature)
        }
    }

    override fun complete(s: String, project: Project): PhpType? = null
}