"""Regression checks for ownership and cleanup; live concurrency is tested separately."""

import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location("emulator_session", Path(__file__).with_name("emulator_session.py"))
session = importlib.util.module_from_spec(spec)
spec.loader.exec_module(session)


class SessionTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def state(self, **changes):
        state = dict(owned=True, status="ready", pid=os.getpid(), process_start="old-start",
                     serial="emulator-5556", forwards=[dict(local="tcp:12345", remote="jdwp:42")])
        state.update(changes)
        session.save(self.root, state)
        return state

    def stop(self):
        return session.operate(type("Args", (), dict(command="stop", session=str(self.root)))())

    def test_stale_pid_never_signalled_or_used_for_adb(self):
        self.state()
        with patch.object(session, "process_identity", return_value="new-start"), \
                patch.object(session.os, "kill") as kill, patch.object(session, "adb") as adb:
            self.assertEqual(self.stop()["status"], "stopped")
            kill.assert_not_called()
            adb.assert_not_called()

    def test_forward_cleanup_failure_does_not_skip_owned_termination(self):
        self.state()
        with patch.object(session, "alive", return_value=True), \
                patch.object(session, "adb", side_effect=RuntimeError("ADB unavailable")), \
                patch.object(session, "terminate") as terminate:
            result = self.stop()
            terminate.assert_called_once()
            self.assertEqual(result["status"], "stopped")
            self.assertIn("ADB unavailable", result["cleanup_warning"])

    def test_borrowed_stop_keeps_device_running_and_is_idempotent(self):
        self.state(owned=False, forwards=[])
        with patch.object(session, "terminate") as terminate, patch.object(session, "adb") as adb:
            self.stop()
            self.stop()
            terminate.assert_not_called()
            adb.assert_not_called()

    def test_rebound_forward_is_not_removed(self):
        state = self.state()
        with patch.object(session, "adb", return_value="emulator-5558 tcp:12345 jdwp:42") as adb:
            session.remove_forwards(state)
            self.assertEqual(adb.call_args_list, [(('forward', '--list'),)])

    def test_only_exact_recorded_forward_removed(self):
        state = self.state()
        with patch.object(session, "adb", side_effect=[
            "emulator-5556 tcp:12345 jdwp:42\nemulator-5558 tcp:12346 jdwp:42", ""
        ]) as adb:
            session.remove_forwards(state)
            self.assertEqual(adb.call_args_list[-1].args,
                             ("-s", "emulator-5556", "forward", "--remove", "tcp:12345"))

    def test_dead_owned_session_does_not_report_reused_serial_ready(self):
        self.state()
        args = type("Args", (), dict(command="status", session=str(self.root)))()
        with patch.object(session, "alive", return_value=False), patch.object(session, "adb") as adb:
            self.assertFalse(session.operate(args)["device_ready"])
            adb.assert_not_called()

    def test_template_copies_configuration_only_and_is_reusable(self):
        avds = self.root / "avds"
        avds.mkdir()
        source = avds / "source.avd"
        source.mkdir()
        sdk = self.root / "sdk"
        (sdk / "system-images/test").mkdir(parents=True)
        (source / "config.ini").write_text(
            "AvdId=source\nimage.sysdir.1=system-images/test\nhw.ramSize=2048\n"
            "disk.dataPartition.path=/live/userdata.img\nsdcard.path=/live/sdcard.img\n")
        (source / "userdata.img").write_text("live data must never be copied")
        (source / "hardware-qemu.ini.lock").write_text("live lock")
        (avds / "source.ini").write_text(f"path={source}\ntarget=android-36.1\n")
        with patch.object(session, "SDK", sdk), patch.object(session, "avd_home", return_value=avds):
            first = session.prepare_template("source")
            self.assertTrue(first["created"])
            target = Path(first["path"])
            self.assertFalse((target / "userdata.img").exists())
            self.assertFalse((target / "hardware-qemu.ini.lock").exists())
            config = session.properties(target / "config.ini")
            self.assertNotIn("disk.dataPartition.path", config)
            self.assertNotIn("sdcard.path", config)
            self.assertEqual(config["hw.ramSize"], "2048")
            self.assertFalse(session.prepare_template("source")["created"])
            self.assertEqual((source / "userdata.img").read_text(), "live data must never be copied")

    def test_unmanaged_template_registration_is_not_overwritten(self):
        registration = self.root / f"{session.DEFAULT_AVD}.ini"
        registration.write_text("path=/user/avd\n")
        with patch.object(session, "avd_home", return_value=self.root):
            with self.assertRaisesRegex(RuntimeError, "unmanaged"):
                session.prepare_template()
        self.assertEqual(registration.read_text(), "path=/user/avd\n")

    def test_adb_environment_does_not_inherit_another_agents_target_or_server(self):
        with patch.dict(os.environ, {"ANDROID_SERIAL": "other-device", "ADB_SERVER_SOCKET": "other",
                                     "ANDROID_ADB_SERVER_PORT": "9999"}):
            env = session.environment()
        for key in ("ANDROID_SERIAL", "ADB_SERVER_SOCKET", "ANDROID_ADB_SERVER_PORT"):
            self.assertNotIn(key, env)

    def test_forward_uses_bounded_pid_lookup_and_records_allocated_port(self):
        self.state(owned=False, forwards=[])
        args = type("Args", (), dict(command="forward", session=str(self.root), pid=42))()
        with patch.object(session, "adb", side_effect=["42", "61234"]) as adb:
            result = session.operate(args)
            self.assertEqual(result["port"], 61234)
            self.assertEqual(adb.call_args_list[0].args,
                             ("-s", "emulator-5556", "shell", "pidof", "ninja.doskey.app.levana"))
            self.assertEqual(adb.call_args_list[1].args,
                             ("-s", "emulator-5556", "forward", "tcp:0", "jdwp:42"))
        recorded = session.json.loads((self.root / "session.json").read_text())
        self.assertEqual(recorded["forwards"], [dict(local="tcp:61234", remote="jdwp:42")])

    def test_wrong_app_pid_never_creates_a_forward(self):
        self.state(owned=False, forwards=[])
        args = type("Args", (), dict(command="forward", session=str(self.root), pid=99))()
        with patch.object(session, "adb", return_value="42") as adb:
            with self.assertRaisesRegex(RuntimeError, "not a running Levana"):
                session.operate(args)
            self.assertEqual(adb.call_count, 1)

    def test_port_collision_detection_ignores_previous_attempt_logs(self):
        log = self.root / "emulator.log"
        log.write_text("Address already in use\n")
        offset = log.stat().st_size
        with log.open("a") as output:
            output.write("Missing system image\n")
        self.assertFalse(session.port_collision(log, offset))
        self.assertTrue(session.port_collision(log, 0))


if __name__ == "__main__":
    unittest.main()
