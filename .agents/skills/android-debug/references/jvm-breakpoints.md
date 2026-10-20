# Live JVM breakpoints with JDWP and jdb

Use this mode for Kotlin/Java running in ART when a falsifiable hypothesis requires runtime values, control flow, or thread stacks. It is not DAP, but provides the relevant debugger operations through tools already included with the Android/JDK installation.

## Constraints

- Use a debuggable build only.
- `jdb` covers Kotlin/Java bytecode. It does not debug native C/C++.
- Kotlin coroutines, inline functions, Compose-generated code, and optimized bytecode can make source lines or local variables unavailable. Prefer breakpoints in ordinary ViewModel, repository, mapper, worker, or domain functions.
- Start with a small number of breakpoints based on an explicit hypothesis. Avoid stepping through framework startup.
- Treat `print` expressions as potentially stateful; inspect values without calling mutating methods.

## Start a suspended debug process

Use the selected session serial. Let ADB allocate an available local port through
the session helper, which records the forward for cleanup:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell am force-stop ninja.doskey.app.levana
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell am set-debug-app -w ninja.doskey.app.levana
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell monkey -p ninja.doskey.app.levana -c android.intent.category.LAUNCHER 1
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell pidof ninja.doskey.app.levana
python3 "$HELPER" forward --session "$SESSION" --pid <pid>
# Set DEBUG_PORT to the returned JSON port.
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/jdb -attach localhost:"$DEBUG_PORT"
```

Run `jdb` in a PTY so the same debugger process can receive follow-up commands. The app waits until the debugger attaches.

If `pidof` is briefly empty, retry the bounded PID lookup after confirming the launch
command succeeded. `adb jdwp` streams updates on current platform-tools; do not run
it as an unbounded listing. The helper checks the PID still belongs to Levana before
creating the forward. An actual `jdb` attach verifies JDWP is available; do not forward
to an unverified PID.

## Useful jdb commands

```text
classes <pattern>                 find the runtime class name
methods <fully.qualified.Class>   inspect method names
stop at <Class>:<line>            set a source-line breakpoint
stop in <Class>.<method>          set a method breakpoint
run                               begin execution if required
cont                              continue
next                              step over
step                              step into
step up                           return to caller
where                             show the current stack
where all                         show all thread stacks
locals                            show visible locals
print <expression>                inspect an expression
dump <expression>                 inspect an object
threads                           list threads
thread <id>                       switch thread
clear <Class>:<line>              remove a breakpoint
exit                              detach and exit
```

Kotlin top-level functions compile into `FileNameKt`; nested and generated classes may contain `$`. Use `classes` and `methods` rather than guessing compiled names. A deferred breakpoint is normal before the class loads.

## Debug loop

At each stop, compare actual locals and the call stack with the hypothesis. If two observations at the same location disprove the model, stop stepping and choose a new boundary or state transition. Prefer a later targeted breakpoint over repeated framework stepping.

## Cleanup

Exit `jdb`, then always run:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell am clear-debug-app
```

Confirm the app can launch normally after clearing the debug setting. The session's
`stop` command removes its recorded forwards. If continuing the same emulator session,
remove only this allocated forward with `adb -s "$SERIAL" forward --remove tcp:"$DEBUG_PORT"`;
never remove all forwards. Do not reuse the hardcoded port 8700 across agents.
