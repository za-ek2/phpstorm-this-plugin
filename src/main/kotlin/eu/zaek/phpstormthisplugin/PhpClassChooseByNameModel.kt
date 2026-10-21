package eu.zaek.phpstormthisplugin

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.ide.util.gotoByName.ChooseByNameModel
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.icons.AllIcons
import com.jetbrains.php.PhpIndex
import javax.swing.JList
import javax.swing.ListCellRenderer

class PhpClassChooseByNameModel(private val project: Project) : ChooseByNameModel {

    override fun getPromptText(): String = "Enter PHP class name:"
    override fun getNotInMessage(): String = "No matching PHP classes"
    override fun getNotFoundMessage(): String = "PHP class not found"
    override fun getCheckBoxName(): String? = null

    override fun loadInitialCheckBoxState(): Boolean = false
    override fun saveInitialCheckBoxState(state: Boolean) {}
    override fun useMiddleMatching(): Boolean = true
    override fun getHelpId(): String? = null
    override fun getSeparators(): Array<String> = emptyArray()

    override fun getNames(checkboxState: Boolean): Array<String> {
        return ReadAction.nonBlocking<Array<String>> {
            val phpIndex = PhpIndex.getInstance(project)
            phpIndex.getAllClassNames(object : PrefixMatcher("") {
                override fun prefixMatches(name: String): Boolean = true
                override fun cloneWithPrefix(prefix: String): PrefixMatcher = this
            })
                .filter { !it.contains("\\vendor\\") }
                .sorted()
                .toTypedArray()
        }.executeSynchronously()
    }

    // ВОЗВРАЩАЕМ СТРОКУ ВМЕСТО PhpClass! Никаких тяжелых PSI-объектов.
    override fun getElementsByName(name: String, checkboxState: Boolean, pattern: String): Array<Any> {
        return arrayOf(name)
    }

    override fun getElementName(element: Any): String? {
        return element as? String
    }

    override fun getFullName(element: Any): String? {
        return element as? String
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getListCellRenderer(): ListCellRenderer<Any> {
        return object : ColoredListCellRenderer<Any>() {
            override fun customizeCellRenderer(
                list: JList<out Any>,
                value: Any?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value is String) {
                    icon = AllIcons.Nodes.Class
                    append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
    }

    override fun willOpenEditor(): Boolean = false
}