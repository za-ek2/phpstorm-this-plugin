plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "eu.zaek"
version = "1.1.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        local("/Applications/PhpStorm.app")
        bundledPlugin("com.jetbrains.php")
    }
}

intellijPlatform {
    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        description = """
            <h2>Overview</h2>
            <p>Modern PHP frameworks often use "magic" dependency injection, where scripts executed in a global scope or inside closures still have access to a fully initialized application instance via <code>${'$'}this</code>. However, IDEs like PhpStorm usually treat <code>${'$'}this</code> outside of classes as invalid or <code>mixed</code>, losing all type inference, autocompletion, and navigation.</p>
            
            <p><b>Contextual This for PHP</b> solves this problem by allowing you to map specific project directories to target PHP classes. Once configured, the plugin forces PhpStorm to recognize <code>${'$'}this</code> as an instance of your specified class inside those directories.</p>
            
            <h3>Features</h3>
            <ul>
            <li><b>Directory-to-Class Mapping:</b> Map any number of directories (e.g., <code>/content/</code>, <code>/routes/</code>) to specific classes (e.g., <code>\App\Kernel</code>).</li>
            <li><b>Full Autocompletion:</b> Get method and property suggestions for <code>${'$'}this-></code> as if you were inside the class itself.</li>
            <li><b>Chained Method Resolution:</b> Autocompletion works perfectly even for chains like <code>${'$'}this->getRequest()-></code>.</li>
            <li><b>Closure Support:</b> Works seamlessly inside global scopes and anonymous functions (closures).</li>
            <li><b>Easy Configuration:</b> Use the native IDE class picker to select your target classes and directories without typing FQNs manually.</li>
            </ul>
            
            <h3>How to use</h3>
            <ol>
            <li>Open <b>Settings</b> (or <b>Preferences</b>) -> <b>Tools</b> -> <b>Contextual This</b>.</li>
            <li>Click the <b>+</b> button to add a new mapping.</li>
            <li>Select the directory (e.g., <code>content/api</code>) and the target PHP class (e.g., <code>\MyApp\Kernel</code>).</li>
            <li>Click <b>Apply</b> and enjoy full IDE support for <code>${'$'}this</code> in your scripts!</li>
            </ol>
            
            <p><i>Note: This plugin is designed for PHP frameworks that utilize this specific architectural pattern.</i></p>
        """.trimIndent()

        changeNotes = """
            <b>1.1.1</b>
            <ul>
                <li>Replaced deprecated <code>ReadAction.compute</code> with modern <code>ReadAction.nonBlocking</code>.</li>
                <li>Fixed deprecated API usages (<code>DynamicBundle</code>, <code>ChooseByNameModel</code>, <code>ComponentWithBrowseButton</code>).</li>
                <li>Improved future compatibility with new PhpStorm releases.</li>
            </ul>
            <br>
            <b>1.1.0</b>
            <ul>
                <li>Removed usage of Internal API (<code>interceptsNativeSignature</code>) to comply with JetBrains Marketplace guidelines.</li>
            </ul>
            <br>
            <b>1.0.0</b>
            <ul>
                <li>Initial release.</li>
                <li>Added directory-to-class mapping settings.</li>
                <li>Added type resolution and autocompletion for <code>${'$'}this</code> and chained methods.</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "25"
        targetCompatibility = "25"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}
