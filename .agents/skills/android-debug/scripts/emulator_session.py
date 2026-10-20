#!/usr/bin/env python3
"""Owned, disposable Android emulator sessions. Uses only the Python standard library."""

import argparse
import contextlib
import fcntl
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time


SDK = Path.home() / "Library/Android/sdk"
ADB = SDK / "platform-tools/adb"
EMULATOR = SDK / "emulator/emulator"
DEFAULT_AVD = "Levana_Agent_Template_API_36_1"


def avd_home():
    return Path(os.environ.get("ANDROID_AVD_HOME", str(Path.home() / ".android/avd"))).resolve()


def properties(path):
    return dict(line.split("=", 1) for line in path.read_text().splitlines()
                if "=" in line and not line.lstrip().startswith("#"))


def prepare_template(source_avd="Medium_Phone_API_36.1"):
    """Copy static configuration only; never copy a live emulator's disks or locks."""
    home = avd_home()
    home.mkdir(parents=True, exist_ok=True)
    with (home / ".levana-agent-template.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        target = home / f"{DEFAULT_AVD}.avd"
        registration = home / f"{DEFAULT_AVD}.ini"
        marker = target / "levana-agent-template.json"
        if registration.exists():
            if not marker.is_file() or properties(registration).get("path") != str(target):
                raise RuntimeError(f"Refusing to reuse unmanaged template: {registration}")
            return dict(avd=DEFAULT_AVD, path=str(target), created=False)
        source_ini = properties(home / f"{source_avd}.ini")
        source = Path(source_ini["path"])
        config = properties(source / "config.ini")
        image = (SDK / config["image.sysdir.1"]).resolve()
        if not image.is_relative_to((SDK / "system-images").resolve()) or not image.is_dir():
            raise RuntimeError("Source AVD must reference an installed SDK system image")
        # Preserve device hardware settings, not runtime disks or chosen snapshots.
        config = {key: value for key, value in config.items()
                  if not (key.endswith(".path") and key != "skin.path")
                  and not key.startswith("snapshot.")}
        config.update({"AvdId": DEFAULT_AVD, "avd.ini.displayname": "Levana Agent Template",
                       "fastboot.chosenSnapshotFile": "", "fastboot.forceChosenSnapshotBoot": "no"})
        target.mkdir()  # An incomplete/existing directory requires inspection, never overwrite it.
        try:
            (target / "config.ini").write_text("".join(f"{k}={v}\n" for k, v in config.items()))
            marker.write_text(json.dumps(dict(source_avd=source_avd, fresh_userdata=True)) + "\n")
            temporary = registration.with_suffix(".ini.tmp")
            temporary.write_text(f"avd.ini.encoding=UTF-8\npath={target}\ntarget={source_ini['target']}\n")
            temporary.replace(registration)
        except BaseException:
            shutil.rmtree(target)
            raise
        return dict(avd=DEFAULT_AVD, path=str(target), created=True)


def environment():
    env = os.environ.copy()
    # All sessions deliberately use the standard shared local ADB server.
    for name in ("ADB_SERVER_SOCKET", "ANDROID_ADB_SERVER_PORT", "ANDROID_SERIAL"):
        env.pop(name, None)
    return env


def adb(*args, timeout=10):
    result = subprocess.run(
        [str(ADB), "-H", "localhost", "-P", "5037", *map(str, args)],
        env=environment(), capture_output=True, text=True, timeout=timeout,
    )
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "ADB failed")
    return result.stdout.strip()


def process_identity(pid):
    result = subprocess.run(
        ["/bin/ps", "-p", str(pid), "-o", "lstart=", "-o", "stat="],
        capture_output=True, text=True, timeout=5,
    )
    fields = result.stdout.strip().split()
    if result.returncode or len(fields) < 6 or fields[-1].startswith("Z"):
        return None
    return " ".join(fields[:-1])


def alive(state):
    return bool(state.get("owned") and state.get("process_start") and
                process_identity(state["pid"]) == state["process_start"])


def save(directory, state):
    target = directory / "session.json"
    temporary = directory / "session.json.tmp"
    temporary.write_text(json.dumps(state, indent=2) + "\n")
    temporary.replace(target)


@contextlib.contextmanager
def locked_session(path):
    directory = Path(path).resolve(strict=True)
    with (directory / "session.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        state = json.loads((directory / "session.json").read_text())
        yield directory, state


def terminate(state):
    if not alive(state):
        return
    try:
        os.kill(state["pid"], signal.SIGTERM)
    except ProcessLookupError:
        return
    deadline = time.monotonic() + 10
    while alive(state) and time.monotonic() < deadline:
        time.sleep(0.2)
    if alive(state):
        try:
            os.kill(state["pid"], signal.SIGKILL)
        except ProcessLookupError:
            pass


def ready(state):
    serial = state["serial"]
    return (adb("-s", serial, "get-state") == "device" and
            adb("-s", serial, "shell", "getprop", "sys.boot_completed") == "1")


def owns_console(state):
    """Do not mistake another emulator on a racing/reused port for our child."""
    port = state["serial"].removeprefix("emulator-")
    result = subprocess.run(
        ["/usr/sbin/lsof", "-a", "-p", str(state["pid"]), "-iTCP:" + port,
         "-sTCP:LISTEN", "-t"], capture_output=True, text=True, timeout=5,
    )
    return str(state["pid"]) in result.stdout.splitlines()


def port_collision(log_path, offset):
    with open(log_path, "rb") as log:
        log.seek(offset)
        message = log.read().decode(errors="replace")
    return bool(re.search(r"(?:[Pp]ort.*(?:in use|already used)|[Aa]ddress already in use)", message))


def start(args):
    project = Path(args.project_root).resolve(strict=True)
    if not (project / "gradlew").is_file():
        raise RuntimeError(f"No gradlew in project root: {project}")
    if args.avd == DEFAULT_AVD:
        prepare_template()
    # Keep the Unix socket path short even when macOS TMPDIR is long.
    directory = Path(tempfile.mkdtemp(prefix="levana-emu-", dir="/tmp"))
    (directory / "artifacts").mkdir()
    state = dict(session=str(directory), project_root=str(project), avd=args.avd,
                 owned=True, status="starting", serial=None, forwards=[],
                 log=str(directory / "emulator.log"), pid=None, process_start=None,
                 avd_home=str(avd_home()))
    save(directory, state)
    process = None
    try:
        adb("start-server", timeout=20)
        # Retry only the specific automatic port allocation race, not arbitrary failures.
        for attempt in range(3):
            report = directory / "report.sock"
            report.unlink(missing_ok=True)
            state.update(serial=None, pid=None, process_start=None, attempt=attempt + 1)
            with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as listener:
                listener.bind(str(report))
                listener.listen(1)
                listener.settimeout(0.5)
                command = [str(EMULATOR), "-avd", args.avd, "-read-only",
                           "-report-console", f"unix:{report}", "-no-metrics"]
                if args.headless:
                    command.append("-no-window")
                with open(state["log"], "ab", buffering=0) as log:
                    log_offset = log.tell()
                    process = subprocess.Popen(command, stdin=subprocess.DEVNULL,
                                               stdout=log, stderr=log,
                                               env=environment(), start_new_session=True)
                state.update(pid=process.pid, process_start=process_identity(process.pid))
                save(directory, state)
                deadline = time.monotonic() + min(args.timeout, 60)
                connection = None
                while time.monotonic() < deadline:
                    if process.poll() is not None:
                        break
                    try:
                        connection, _ = listener.accept()
                        break
                    except socket.timeout:
                        continue
                if connection is None:
                    if process.poll() is not None:
                        if attempt < 2 and port_collision(state["log"], log_offset):
                            continue
                        raise RuntimeError(f"Emulator exited with code {process.returncode}")
                    raise RuntimeError("Timed out waiting for emulator console report")
                with connection:
                    connection.settimeout(5)
                    data = bytearray()
                    while True:
                        chunk = connection.recv(64)
                        if not chunk:
                            break
                        data.extend(chunk)
                        if len(data) > 64:
                            raise RuntimeError("Invalid oversized console report")
                port = bytes(data).decode("ascii").strip("\x00\r\n ")
                if not re.fullmatch(r"[0-9]{4,5}", port) or not 1024 <= int(port) <= 65534:
                    raise RuntimeError(f"Invalid console port report: {port!r}")
                state["serial"] = f"emulator-{port}"
                save(directory, state)
            report.unlink(missing_ok=True)
            # A report precedes actual port binding. Check process liveness while booting.
            deadline = time.monotonic() + args.timeout
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    if attempt < 2 and port_collision(state["log"], log_offset):
                        break
                    raise RuntimeError(f"Emulator exited with code {process.returncode}")
                try:
                    if owns_console(state) and ready(state) and process.poll() is None:
                        state["status"] = "ready"
                        save(directory, state)
                        return state
                except (RuntimeError, subprocess.TimeoutExpired):
                    pass
                time.sleep(0.5)
            else:
                raise RuntimeError("Timed out waiting for Android boot completion")
        raise RuntimeError("Emulator could not allocate ports after three attempts")
    except BaseException as error:
        # Popen is the strongest ownership handle during this invocation.
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        state.update(status="failed", error=str(error) or type(error).__name__)
        save(directory, state)
        print(json.dumps(state), file=sys.stderr)
        raise
    finally:
        (directory / "report.sock").unlink(missing_ok=True)


def attach(args):
    if adb("-s", args.serial, "get-state") != "device":
        raise RuntimeError(f"Device is not ready: {args.serial}")
    directory = Path(tempfile.mkdtemp(prefix="levana-emu-", dir="/tmp"))
    (directory / "artifacts").mkdir()
    state = dict(session=str(directory), project_root=str(Path(args.project_root).resolve()),
                 owned=False, status="ready", serial=args.serial, forwards=[])
    save(directory, state)
    return state


def remove_forwards(state):
    if not state["forwards"]:
        return
    current = {tuple(line.split()) for line in adb("forward", "--list").splitlines()}
    for forward in state["forwards"]:
        entry = (state["serial"], forward["local"], forward["remote"])
        if entry in current:
            adb("-s", state["serial"], "forward", "--remove", forward["local"])
    state["forwards"] = []


def operate(args):
    with locked_session(args.session) as (directory, state):
        if args.command == "status":
            running = alive(state) if state["owned"] else False
            try:
                available = ready(state) if state["status"] == "ready" and (
                    (running and owns_console(state)) or not state["owned"]) else False
            except (RuntimeError, subprocess.TimeoutExpired):
                available = False
            return dict(state, process_alive=running, device_ready=available)
        if args.command == "forward":
            if state["status"] != "ready" or (state["owned"] and (
                    not alive(state) or not owns_console(state))):
                raise RuntimeError("Session is not running")
            # `adb jdwp` streams updates on current platform-tools; it is not a
            # bounded validation command. Confirm the caller's PID is still Levana.
            pids = adb("-s", state["serial"], "shell", "pidof", "ninja.doskey.app.levana").split()
            if str(args.pid) not in pids:
                raise RuntimeError(f"PID {args.pid} is not a running Levana process on this device")
            remote = f"jdwp:{args.pid}"
            port = adb("-s", state["serial"], "forward", "tcp:0", remote)
            if not port.isdigit():
                raise RuntimeError(f"Invalid allocated debugger port: {port!r}")
            state["forwards"].append(dict(local=f"tcp:{port}", remote=remote))
            save(directory, state)
            return dict(port=int(port), session=str(directory), serial=state["serial"])
        if state["status"] == "stopped":
            return state
        # A stale owned session must never act on a serial that may have been reused.
        cleanup_error = None
        try:
            if not state["owned"] or alive(state):
                remove_forwards(state)
        except (OSError, RuntimeError, subprocess.SubprocessError) as error:
            cleanup_error = str(error)
        finally:
            if state["owned"]:
                terminate(state)
        state["status"] = "stopped"
        if cleanup_error:
            state["cleanup_warning"] = cleanup_error
        save(directory, state)
        return state


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    launch = commands.add_parser("start")
    launch.add_argument("--project-root", default=os.getcwd())
    launch.add_argument("--avd", default=DEFAULT_AVD)
    launch.add_argument("--headless", action="store_true")
    launch.add_argument("--timeout", type=float, default=180)
    borrow = commands.add_parser("attach")
    borrow.add_argument("--serial", required=True)
    borrow.add_argument("--project-root", default=os.getcwd())
    prepare = commands.add_parser("prepare-template")
    prepare.add_argument("--source-avd", default="Medium_Phone_API_36.1")
    for name in ("status", "stop", "forward"):
        command = commands.add_parser(name)
        command.add_argument("--session", required=True)
        if name == "forward":
            command.add_argument("--pid", required=True, type=int)
    args = parser.parse_args()
    if args.command == "start" and args.timeout <= 0:
        parser.error("--timeout must be positive")
    try:
        result = prepare_template(args.source_avd) if args.command == "prepare-template" else (
            start(args) if args.command == "start" else (
                attach(args) if args.command == "attach" else operate(args)))
        print(json.dumps(result))
    except (OSError, RuntimeError, ValueError, KeyError, subprocess.SubprocessError) as error:
        print(f"emulator-session: {error}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        return 130
    return 0


if __name__ == "__main__":
    sys.exit(main())
