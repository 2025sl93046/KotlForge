# Watch Mode

KotlForge watch mode monitors your source directories and automatically rebuilds the project when files change. It is designed for fast inner-loop development without leaving the terminal.

## Basic usage

```
kotlforge watch --project /path/to/android-project
```

Options:

| Flag | Default | Description |
|------|---------|-------------|
| `--project` / `-p` | `.` | Path to the Android project root |
| `--variant` / `-v` | `debug` | Build variant (`debug`, `release`, …) |
| `--deploy` / `-d` | _(none)_ | Deploy the rebuilt APK to the given device serial after each successful build |
| `--auto-deploy` | _(off)_ | Like `--deploy` but auto-selects the only connected device |

Press **Ctrl+C** to stop. KotlForge will stop the Gradle daemon cleanly before exiting.

## How it works

1. **Watch** — Java `WatchService` monitors all source directories listed in `kotlforge.json` (`watchPaths`). New subdirectories are registered automatically.
2. **Debounce** — Rapid save bursts (e.g. auto-format on save) are coalesced into a single rebuild trigger. The window is 300 ms by default.
3. **Build** — `./gradlew assemble<Variant>` is invoked incrementally. Gradle's up-to-date checks avoid recompiling unchanged modules.
4. **Report** — Each cycle prints the number of changed files, their names, and the total rebuild duration.
5. **Deploy** _(optional)_ — If `--deploy` or `--auto-deploy` is given and the build succeeds, the APK is installed on the target device via ADB.
6. **Shutdown** — Ctrl+C triggers a JVM shutdown hook that stops the file watcher, flushes the debounce queue, and runs `./gradlew --stop` to terminate the Gradle daemon.

## Configuration (`kotlforge.json`)

```json
{
  "watchPaths": ["src", "res"],
  "watchIgnorePatterns": [
    "**/*.class",
    "**/build/**",
    "**/.gradle/**",
    "**/.git/**"
  ]
}
```

`watchPaths` is relative to the project root. If a listed path does not exist it is silently skipped. If none exist the whole project root is watched.

`watchIgnorePatterns` uses glob syntax:

| Token | Meaning |
|-------|---------|
| `*` | Any filename characters (not `/`) |
| `**` | Any path, including path separators |
| `?` | Any single filename character (not `/`) |
| `*.class` | All files ending in `.class` |
| `**/build/**` | Anything inside any `build/` directory |

## Known limitations

- **No network shares or virtual filesystems.** Java `WatchService` relies on native OS kernel events (`inotify` on Linux, `FSEvents` on macOS, `ReadDirectoryChangesW` on Windows). Files on NFS mounts, Docker bind mounts, or WSL2 interop paths may not deliver events reliably.
- **macOS coalesces rapid events.** On macOS, `WatchService` polls with a ~2 s delay rather than receiving instant kernel notifications. For sub-second feedback, consider running the project inside a Linux VM or using the Docker-based build flow.
- **Large source trees are slow to register.** On first start KotlForge walks the entire source tree to register every subdirectory. Projects with tens of thousands of source files may take a few seconds to become ready.
- **Deleted directories are not unregistered.** If you delete a directory while the watcher is running, its watch key is silently dropped. The watcher remains functional for all other paths.
- **No IDE hot-swap or Instant Run.** Watch mode triggers a full Gradle incremental build and then deploys an APK. It does not perform bytecode patching or class-level hot-swap. The app restarts on every deploy cycle.
- **One project at a time.** A single `kotlforge watch` process handles one project root. Run multiple processes for a multi-project workspace.
- **Ctrl+C on Windows may not propagate.** On Windows the `SIGINT` signal path differs from Unix. If the daemon is not stopped automatically, run `.\gradlew --stop` manually.
