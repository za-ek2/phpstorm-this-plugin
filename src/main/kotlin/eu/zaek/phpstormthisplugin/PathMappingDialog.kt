package eu.zaek.phpstormthisplugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PathMappingDialog(
    private val project: Project,
    private val existingMapping: PathMapping?
) : DialogWrapper(project) {

    private val dirField = TextFieldWithBrowseButton()
    private val classField = TextFieldWithBrowseButton()

    init {
        title = if (existingMapping == null) "Add Directory Mapping" else "Edit Directory Mapping"
        init()

        // Делаем окно шире
        setSize(600, 250)

        existingMapping?.let {
            dirField.text = it.directory
            classField.text = it.fqn
        }

        // Добавляем слушатели для валидации формы (кнопка OK)
        val docListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = checkValidity()
            override fun removeUpdate(e: DocumentEvent?) = checkValidity()
            override fun changedUpdate(e: DocumentEvent?) = checkValidity()
        }
        dirField.textField.document.addDocumentListener(docListener)
        classField.textField.document.addDocumentListener(docListener)

        dirField.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val file = FileChooser.chooseFile(descriptor, project, null)
            if (file != null) {
                val basePath = project.basePath
                dirField.text = if (basePath != null && file.path.startsWith(basePath)) {
                    file.path.removePrefix(basePath).trimStart('/', '\\')
                } else {
                    file.path
                }
            }
        }

        classField.addActionListener {
            val model = PhpGotoClassModel(project)
            val popupComponent = com.intellij.ide.util.gotoByName.ChooseByNameFactory
                .getInstance(project)
                .createChooseByNamePopupComponent(model)

            popupComponent.invoke(object : com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent.Callback() {
                override fun elementChosen(element: Any?) {
                    if (element is com.jetbrains.php.lang.psi.elements.PhpClass) {
                        classField.text = element.fqn
                    }
                }
            }, ModalityState.defaultModalityState(), false)
        }

        // Инициализируем начальное состояние кнопки OK
        checkValidity()
    }

    private fun checkValidity() {
        val isDirValid = dirField.text.isNotBlank()
        val isClassValid = classField.text.isNotBlank()
        // Правильный способ управления кнопкой OK
        okAction.isEnabled = isDirValid && isClassValid
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return dirField.textField // Ставим фокус на первое поле при открытии
    }

    override fun createCenterPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><b>Directory:</b> (relative to project root or absolute)</html>"))
            .addComponent(dirField)
            .addVerticalGap(10)
            .addComponent(JBLabel("<html><b>Target PHP Class:</b> (FQN)</html>"))
            .addComponent(classField)
            .panel
    }

    fun getMapping(): PathMapping {
        return PathMapping(dirField.text.trim(), classField.text.trim())
    }
}