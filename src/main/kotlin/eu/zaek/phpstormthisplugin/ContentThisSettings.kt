package eu.zaek.phpstormthisplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

// Класс для хранения одной пары "Папка - Класс"
class PathMapping(
    var directory: String = "",
    var fqn: String = ""
)

@State(
    name = "ContentThisSettings",
    storages = [Storage("ContentThisSettings.xml")]
)
class ContentThisSettings : PersistentStateComponent<ContentThisSettings> {

    var mappings: MutableList<PathMapping> = mutableListOf(
        PathMapping("content", "\\vitamin\\Kernel\\App") // Дефолтное значение
    )

    override fun getState(): ContentThisSettings = this

    override fun loadState(state: ContentThisSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): ContentThisSettings {
            return ApplicationManager.getApplication().getService(ContentThisSettings::class.java)
        }
    }

    // Утилита для поиска класса по пути к файлу
    fun getTargetClassForFile(filePath: String, project: Project): String? {
        val basePath = project.basePath?.replace('\\', '/') ?: return null
        val normalizedFilePath = filePath.replace('\\', '/')

        // Сортируем по убыванию длины пути, чтобы более точные совпадения были в приоритете
        val sortedMappings = mappings.sortedByDescending { it.directory.length }

        for (mapping in sortedMappings) {
            if (mapping.directory.isBlank() || mapping.fqn.isBlank()) continue

            val normDir = mapping.directory.replace('\\', '/').trim('/')
            val absoluteDir = if (normDir.startsWith("/") || normDir.contains(":")) {
                normDir // Абсолютный путь
            } else {
                "$basePath/$normDir" // Относительный путь
            }

            if (normalizedFilePath.startsWith("$absoluteDir/")) {
                return mapping.fqn
            }
        }
        return null
    }
}