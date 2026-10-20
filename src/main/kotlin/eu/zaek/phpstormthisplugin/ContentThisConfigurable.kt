package eu.zaek.phpstormthisplugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

class ContentThisConfigurable : SearchableConfigurable {
    private var panel: JPanel? = null
    private var listModel: CollectionListModel<PathMapping>? = null
    private var list: JBList<PathMapping>? = null

    override fun getId(): String = "eu.zaek.phpstormthisplugin.settings"
    override fun getDisplayName(): String = "Contextual This"

    override fun createComponent(): JComponent {
        val settings = ContentThisSettings.getInstance()

        val mappingsCopy = settings.mappings.map { PathMapping(it.directory, it.fqn) }.toMutableList()
        listModel = CollectionListModel(mappingsCopy)
        list = JBList(listModel!!)

        // Улучшенный рендерер списка
        list!!.cellRenderer = object : ColoredListCellRenderer<PathMapping>() {
            override fun customizeCellRenderer(
                list: JList<out PathMapping>, value: PathMapping?, index: Int,
                selected: Boolean, hasFocus: Boolean
            ) {
                if (value != null) {
                    icon = AllIcons.Nodes.Folder // Иконка папки для директории
                    append(value.directory, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    append("  ->  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(value.fqn, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }

        val project = ProjectManager.getInstance().openProjects.firstOrNull { !it.isDefault }

        val decorator = ToolbarDecorator.createDecorator(list!!)
            .setAddAction {
                if (project != null) {
                    val dialog = PathMappingDialog(project, null)
                    if (dialog.showAndGet()) {
                        listModel!!.add(dialog.getMapping())
                    }
                }
            }
            .setEditAction {
                val selected = list!!.selectedValue
                if (selected != null && project != null) {
                    val dialog = PathMappingDialog(project, selected)
                    if (dialog.showAndGet()) {
                        val index = list!!.selectedIndex
                        listModel!!.remove(index)
                        listModel!!.add(index, dialog.getMapping())
                    }
                }
            }
            .setRemoveAction {
                val index = list!!.selectedIndex
                if (index >= 0) listModel!!.remove(index)
            }
            .disableUpDownActions()

        // Создаем панель с описанием и списком
        panel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel(
                "<html>" +
                        "<b>Directory Mappings</b><br>" +
                        "Map specific project directories to target PHP classes.<br>" +
                        "Inside these directories, the <code>\$this</code> variable will be resolved to the specified class." +
                        "</html>"
            ))
            .addVerticalGap(10)
            .addComponentFillVertically(decorator.createPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = ContentThisSettings.getInstance()
        if (listModel?.items?.size != settings.mappings.size) return true
        return listModel?.items?.zip(settings.mappings)?.any { (a, b) -> a.directory != b.directory || a.fqn != b.fqn } ?: false
    }

    override fun apply() {
        val settings = ContentThisSettings.getInstance()
        settings.mappings = (listModel?.items ?: emptyList()).toMutableList()
    }

    override fun reset() {
        val settings = ContentThisSettings.getInstance()
        val mappingsCopy = settings.mappings.map { PathMapping(it.directory, it.fqn) }.toMutableList()
        listModel?.replaceAll(mappingsCopy)
    }

    override fun disposeUIResources() {
        panel = null
        listModel = null
        list = null
    }
}