# BiblePlugin

A Bible reader for Spigot and Paper servers, from **Minecraft 1.21 to 26.x**. Read, search and count
words in dozens of translations, browse them in menus, get temporary written books with passages,
and see how hard each English edition is to read.

- **117 editions in 69 languages**, downloaded from the free [getBible](https://api.getbible.net) API.
- **Reading difficulty** for every English edition, measured from its text (Flesch-Kincaid).
- **Menus** for the library, books, chapters, the reader and search results.
- **Anvil or sign prompts** for typing searches.
- **Temporary books**: a passage as a written book that disappears after a set time.
- **Sounds**, colour-coded chat, bookmarks, verse of the moment on join.
- **Modular, tested codebase** that other plugins can depend on through `bibleplugin-api`.

## Commands

Every command starts with `/bible` (aliases `/bib`, `/scripture`). `/bible help` lists the ones you can use.
The Bible name is optional wherever a default is set, so `/bible verse John 3:16` works.

| Command | What it does |
| --- | --- |
| `/bible available [language \| page]` | List languages, or the editions of one language. Shows difficulty. |
| `/bible downloaded` | Show which editions are loaded or only installed. |
| `/bible download <id>` | Download an edition from the catalogue (admin: `bible.download`). |
| `/bible load <id>` / `unload <id>` / `delete <id>` | Manage editions in memory or on disk (`bible.admin`). |
| `/bible default <id>` | Set the default edition (`bible.admin`). |
| `/bible info [bible]` | Details of an edition: language, licence, books, verses, difficulty. |
| `/bible import <file> <id> <language-code> [name]` | Import your own JSON Bible from `plugins/BiblePlugin/imports/` (`bible.admin`). |
| `/bible verse [bible] <reference>` | Show a verse, a range or a chapter. E.g. `John 3:16-18`, `Psalm 23`. |
| `/bible read [bible] [reference]` | Open the reader menu at a chapter. |
| `/bible continue` | Resume where you stopped. |
| `/bible book [bible] <reference>` | Get a **temporary written book** with the passage. |
| `/bible chapters [bible] <book>` | Number of chapters in a book. |
| `/bible verses [bible] <book:chapter>` | Number of verses in a chapter. |
| `/bible books [bible] [page]` | List the books of a Bible. |
| `/bible random [bible]` / `daily [bible]` | A random verse, or the verse of the day (the same for everyone). |
| `/bible occurrences [bible] <word> [in <reference>]` | Count how often a word appears, optionally in one book or chapter. |
| `/bible find [bible] <phrase> [in <reference>]` | List verses containing a phrase. |
| `/bible search [bible]` | Type a search in an anvil (or sign) and browse results in a menu. |
| `/bible bookmark add [bible] <reference>` / `list` / `remove <n>` | Save and manage bookmarks. |
| `/bible loginverse on\|off` | Turn the verse shown when you join on or off. |
| `/bible gui` | Open the Bible library. |
| `/bible reload` | Reload `config.yml` and `messages.yml` (`bible.admin`). |

Reference syntax accepts `Book Chapter:Verse-EndVerse`, `Book:Chapter:Verse`, `1 John 3:16`, `I John 3:16`,
and unique prefixes such as `gen 1:1` or `rev 22`. If a prefix is ambiguous the command lists the
candidates. Note that the old `Exodus:6:15:4` (four-part) form is no longer accepted.

### Reader controls

In `/bible read` (or the library), the bottom row has:

- **Previous / next page** and **previous / next chapter** arrows.
- **Search** (anvil or sign prompt, then a results menu).
- **Books** to jump to another book, and **Back** when you came from another menu.
- **★ Bookmark** saves the chapter. **Temporary book** gives you the chapter as a book.
- Left-click a verse to share it in chat. Right-click to bookmark it.

## Permissions

| Node | Default | Covers |
| --- | --- | --- |
| `bible.use` | everyone | Reading, search, bookmarks, menus, join verse |
| `bible.book` | everyone | Temporary written books |
| `bible.download` | op | Downloading editions |
| `bible.admin` | op | Load, unload, delete, default, import, reload |
| `bible.*` | op | All of the above |

The old `bible.commands` node is gone; grant `bible.use` instead.

## Configuration

`config.yml` (commented in the file) controls the default edition, the editions loaded on startup,
download timeouts, search limits, the prompt input (`ANVIL` or `SIGN`), temporary book lifetime and
page size, sounds, and the join verse. `messages.yml` holds every message, with `&` colour codes and
`{0}`-style placeholders.

## Supported editions

Editions come from the [getBible](https://api.getbible.net/v2/translations.json) catalogue, which
lists 117 translations across 69 languages. The list is bundled as
`bibleplugin-source/src/main/resources/catalog.json`, so the plugin works offline for browsing.
Each entry records its licence as published by getBible. **Check the licence before you redistribute
any edition**.

### English editions and reading difficulty

Difficulty is measured from the actual text. The plugin computes the **Flesch-Kincaid grade level**
(US school grade) and **Flesch Reading Ease** for each edition. Bands:

| Grade | Band | Example editions |
| --- | --- | --- |
| below 6.6 | Easy | Basic English, World English Bible |
| 6.6 to 7.5 | Moderate | KJV, American Standard, Webster's, Tyndale |
| 7.5 to 8.5 | Advanced | Weymouth NT, Douay-Rheims |
| 8.5 and above | Scholarly | Young's Literal Translation |

| Edition | ID | Books | Grade | Ease | Band |
| --- | --- | --- | --- | --- | --- |
| Basic English Bible | `basicenglish` | 66 | 6.5 | 82.5 | Easy |
| World English Bible | `web` | 66 | 6.4 | 78.3 | Easy |
| William Tyndale (1525/1530) | `tyndale` | 33 | 6.7 | 79.5 | Moderate |
| American Standard Version | `asv` | 66 | 6.8 | 76.3 | Moderate |
| Webster's Bible | `wb` | 66 | 6.8 | 77.8 | Moderate |
| American King James Version | `akjv` | 66 | 6.8 | 78.7 | Moderate |
| King James Version | `kjv` | 66 | 7.1 | 77.0 | Moderate |
| KJV 1769 with Strong's | `kjva` | 80 | 7.3 | 76.2 | Moderate |
| Weymouth New Testament | `weymouth` | 27 | 7.7 | 73.0 | Advanced |
| Douay-Rheims | `douayrheims` | 73 | 8.0 | 75.0 | Advanced |
| Young's Literal Translation | `ylt` | 66 | 8.6 | 71.7 | Scholarly |

These metrics are a fair guide for ranking editions against each other. They are not an absolute
measure. The figures were produced by `tools/generate-catalog.py`, and the runtime
`ReadabilityAnalyzer` uses the same formulas, so imported editions are rated consistently.

### NIV, NKJV, ESV, NLT, CSB

These are **copyrighted** and no free API serves them, so the plugin does not download them. If you
own a licensed electronic copy, convert it to the JSON format below and use `/bible import`. You are
responsible for the licence terms.

### Accepted JSON formats

- **getBible layout** (used by the catalogue):
  `{"books":[{"name":"Genesis","chapters":[{"chapter":1,"verses":[{"verse":1,"text":"..."}]}]}]}`
- **Array layout** (thiagobodruk/bible, and older BiblePlugin files):
  `[{"name":"Genesis","abbrev":"gn","chapters":[["In the beginning...","..."]]}]`

Both are streamed, so a 9 MB Bible never exists as a full JSON tree in memory.

## Languages and search

Search is case-insensitive and accent-insensitive (`senor` finds `señor`). Whole words are matched
for ordinary languages. Scripts without spaces (Chinese, Japanese, Thai) fall back to substring
matching automatically.

## Architecture

```
bibleplugin-core         Pure Java model: Bible, Reference, parser, search index, readability,
                         temporary-book pagination. No server dependencies.
bibleplugin-source       Catalogue, getBible and array-format parsers, HTTP downloader with retries.
bibleplugin-persistence  Compact gzip binary cache, atomic file store, player data (bookmarks, settings).
bibleplugin-api          Public, Bukkit-free API (BiblePluginApi, BibleListener) for other plugins.
bibleplugin-spigot       The plugin: commands, menus, prompts, sounds, temporary books, listeners.
```

Dependencies only point downwards: `spigot → api, persistence, source → core`.

### Design patterns

- **Composition root**: `PluginContext` wires every collaborator once. Nothing uses statics.
- **Command**: each `/bible` action is a `Subcommand` record with its own handler and completer,
  grouped into `CommandModule`s.
- **Template method**: `Menu` defines `open` and `refresh`; subclasses fill in `populate`.
- **Strategy**: search uses `IndexedStrategy` or `ScanStrategy`; text input uses `AnvilPrompt` or
  sign input behind `PromptService.requestText`.
- **Builder**: `Settings.builder()` validates configuration in one place.
- **Observer**: `BibleListener` is notified when Bibles load or unload.
- **Facade**: `BiblePluginApi` hides the registry and search engine from other plugins.
- **Immutable value objects**: `Bible`, `Reference`, `PlayerRecord` and friends never change after
  creation.

### Performance

- Full-text search uses an **inverted index** built once per Bible and cached weakly, so repeated
  searches do not rescan 31,000 verses.
- Persisted Bibles load from a **binary cache** instead of re-parsing JSON.
- Downloads, parsing, indexing and search run on **virtual threads**, never on the server thread.
- Menus render only the current page.

## Building

The project uses the **Gradle wrapper** (Gradle 9.8.1 is downloaded automatically), so you do not need
Gradle installed. You only need a **JDK 21 or newer** on your `PATH` or in `JAVA_HOME`.

Check your setup:

```bash
java -version          # must report version 21 or higher
```

### Build the plugin jar

Run these from the project root, the folder that contains `gradlew`:

| Platform | Command |
| --- | --- |
| Windows (PowerShell or cmd) | `.\gradlew.bat build` |
| Linux / macOS / Git Bash | `./gradlew build` |

A successful build compiles every module, runs all 89 unit tests, and produces the plugin jar:

```
bibleplugin-spigot/build/libs/BiblePlugin-2.0.0.jar
```

That single jar is the plugin. Its dependencies (Gson, relocated under `org.bibleplugin.libs`) are
bundled inside it, so copy only this file into your server's `plugins/` folder.

### Useful build tasks

| Task | What it does |
| --- | --- |
| `build` | Compile, test, and assemble the shaded plugin jar. This is the default for most work. |
| `test` | Run the unit tests only. |
| `clean` | Delete every `build/` folder. Use it after changing `gradle.properties`. |
| `:bibleplugin-spigot:shadowJar` | Assemble just the plugin jar. |
| `:bibleplugin-core:test` | Run the tests of a single module (use any module name). |
| `dependencies` | Print the dependency tree of a module, for example `:bibleplugin-spigot:dependencies`. |

Examples:

```bash
./gradlew test                       # all unit tests
./gradlew :bibleplugin-core:test     # only the core module
./gradlew clean build                # fresh build from scratch
```

### Choosing the Spigot API version

The plugin is compiled against the **oldest supported API, 1.21**, so the same jar runs on every
server from 1.21 to 26.x. To check that it also compiles against the newest API, pass `-PspigotApi`:

```bash
./gradlew build -PspigotApi=26.3-R0.1-SNAPSHOT
```

The default is set in `gradle/libs.versions.toml` and can be overridden per build as shown above.
Gradle downloads Spigot API snapshots from `hub.spigotmc.org` the first time you build, so the first
build needs an internet connection.

### Build settings

`gradle.properties` holds the project version (`version=2.0.0`), the Gradle JVM options, and the speed
settings: parallel builds, the build cache, and the configuration cache. The code is compiled with
`--release 21`. To change the version, edit `version` there and rebuild; `plugin.yml` picks it up
automatically.

### Continuous integration

`.github/workflows/build.yml` runs `./gradlew build` on every push and pull request, once against
Spigot API 1.21 and once against 26.3. The 1.21 build's jar is uploaded as an artifact.

### Regenerating the Bible catalogue

The bundled edition list is generated from the getBible API. Regenerate it when getBible adds or
removes editions. This needs Python 3 and an internet connection; it is not part of the Gradle build:

```bash
python tools/generate-catalog.py
```

Downloaded English editions are cached under `tools/.cache/`, which is ignored by git.

## Testing

```bash
./gradlew test
```

Unit tests cover reference parsing, book resolution, search (including accents, phrases, scopes and
unindexed scripts), ordinals and ranges, readability, pagination, temporary-book composition, the
JSON parsers (both layouts), the bundled catalogue, the binary codec, the file and player stores, and
the pure helpers the GUI and chat use.

## Known limitations

- The plugin was built and unit-tested here, but **not run on a live server** in this environment.
  Please try it on a test server before a public release, especially the anvil and sign prompts,
  which depend on the client and the server version.
- The sign prompt places a real sign three blocks above the player, and removes it afterwards. Other
  nearby players may briefly see it. Use `reader.input: ANVIL` (the default) if that matters.
- Written books have a size limit, so long passages are truncated with a notice after
  `temporary-book.max-pages` pages.
- Only editions that are openly available are downloadable. Commercial editions need the import route.
- Folia and other region-threaded forks are not supported.

## Licence

See [LICENSE](LICENSE). The original project's licence is AGPL-3.0, and this rewrite keeps it.
