# Contextual This for PHP

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains_Marketplace-v1.0.0-blue.svg)](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Contextual This** is a PhpStorm plugin designed for modern PHP frameworks that utilize "magic" dependency injection. It allows PhpStorm to resolve `$this` in global scopes and closures by mapping specific project directories to target PHP classes.

## The Problem

In many modern PHP applications, scripts executed in a global scope (or inside closures) still have access to a fully initialized application instance via `$this`.

However, standard IDE behavior treats `$this` outside of a class as invalid or `mixed`. This means you lose all the magic of PhpStorm: no autocompletion for `$this->`, no type inference, and no navigation to class methods.

## Features

* **Directory-to-Class Mapping:** Map any number of directories (e.g., `/content/`, `/routes/`) to specific classes (e.g., `\App\Kernel`).
* **Full Autocompletion:** Get method and property suggestions for `$this->` as if you were inside the class itself.
* **Chained Method Resolution:** Autocompletion works seamlessly for chains like `$this->getRequest()->getQuery()`.
* **Closure Support:** Works perfectly inside global scopes and anonymous functions (closures).
* **Native IDE UI:** Uses the standard PhpStorm class search and file picker dialogs for a seamless configuration experience.
* **Vendor Filtering:** Fast, index-based searching automatically ignores `vendor` and `node_modules` directories to keep your class list clean.

## Screenshots
![Plugin settings](screenshots/Screenshot%202026-10-20%20at%2022.22.36.png)
<p align="center"><i>The settings window</i></p>

![Map project class](screenshots/Screenshot%202026-10-20%20at%2022.22.48.png)

<p align="center"><i>Map each directory to their correspondent classes to enable full IDE support for <code>$this</code>.</i></p>

![IDE highlight](screenshots/Screenshot%202026-10-20%20at%2022.29.57.png)

<p align="center">
  <i>Autocomplete works for <code>$this</code> hidding private/protected properties and methods</i>
</p>

## Installation

### From JetBrains Marketplace (Recommended)
1. Open PhpStorm.
2. Go to **Settings** (or **Preferences**) -> **Plugins**.
3. Click on the **Marketplace** tab and search for `Contextual This`.
4. Click **Install** and restart the IDE.

### Manual Installation
1. Download the latest release `.zip` file from the [Releases](https://github.com/YOUR_USERNAME/phpstorm-this-plugin/releases) page.
2. Go to **Settings** -> **Plugins** -> ⚙️ (Gear Icon) -> **Install Plugin from Disk...**
3. Select the downloaded `.zip` file and restart PhpStorm.

## Configuration

1. Open **Settings** -> **Tools** -> **Contextual This**.
2. Click the **+** button to add a new directory mapping.
3. Choose the directory you want to map (e.g., `content/api`).
4. Choose the target PHP class using the built-in fast class picker (e.g., `\vitamin\Kernel\App`).
5. Click **OK**, then **Apply**.

*Note: The plugin automatically handles nested directories. If you have mappings for both `content` and `content/api`, the plugin will correctly choose the most specific match (`content/api`).*

## Building from Source

If you want to contribute or build the plugin yourself, you'll need JDK 17+ and Gradle.

1. Clone the repository:
   `git clone https://github.com/YOUR_USERNAME/phpstorm-this-plugin.git`
2. Navigate to the project folder:
   `cd phpstorm-this-plugin`
3. Build the plugin (the `.zip` will appear in `build/distributions`):
   `./gradlew buildPlugin`
4. Run a sandboxed instance of PhpStorm with the plugin installed:
   `./gradlew runIde`

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Known Limitations
Note: Due to IntelliJ Platform restrictions, the IDE may not show type hints when hovering
over \$this in a global scope. However, full autocompletion for \$this-> and chained methods
works perfectly.
```kt
override fun interceptsNativeSignature(): Boolean = true
```


## License

Distributed under the MIT License. See `LICENSE` for more information.

## Disclaimer

*This plugin is an independent open-source project and is not affiliated with, endorsed by, or sponsored by JetBrains. PhpStorm is a registered trademark of JetBrains s.r.o.*
