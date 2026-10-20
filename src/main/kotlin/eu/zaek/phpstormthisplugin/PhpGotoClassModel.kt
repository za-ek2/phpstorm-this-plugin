package eu.zaek.phpstormthisplugin

import com.intellij.icons.AllIcons
import com.intellij.ide.util.gotoByName.GotoClassModel2
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.php.lang.psi.elements.PhpClass
import javax.swing.JList
import javax.swing.ListCellRenderer

class PhpGotoClassModel(project: Project) : GotoClassModel2(project) {
    override fun getListCellRenderer(): ListCellRenderer<Any> {
        return object : ColoredListCellRenderer<Any>() {
            override fun customizeCellRenderer(
                list: JList<out Any>,
                value: Any?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value is PhpClass) {
                    icon = AllIcons.Nodes.Class
                    append(value.fqn, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                } else if (value != null) {
                    // Фолбэк для других элементов, если они попадут в список
                    append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
    }
}