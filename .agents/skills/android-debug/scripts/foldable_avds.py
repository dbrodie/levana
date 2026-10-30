#!/usr/bin/env python3
"""Creates the Galaxy Z Fold8 / Fold8 Ultra foldable AVDs (TASK-52.1). Standard library only.

Each AVD unfolds to the inner display and folds to the cover display, like the stock
Pixel Fold and Pixel 9 Pro Fold definitions in Android Studio, with Samsung's published
pixel sizes. Samsung publishes no Android density, so both use an assumed 420 dpi. The
AVDs copy the agent template's static configuration (same system image), are marked as
managed, and are never overwritten; delete one with its .ini and .avd to recreate it.

    python3 foldable_avds.py create      # creates any that are missing
    python3 foldable_avds.py list
"""

import argparse
import json
import sys

from emulator_session import DEFAULT_AVD, avd_home, prepare_template, properties

MARKER = "levana-foldable-avd.json"

# Inner (unfolded) pixels as width x height in the display's natural orientation, and the
# cover (folded) display. The emulator gives only its Pixel Fold device names a second
# built-in display for the cover, which the system switches to on fold like a real
# Samsung foldable; a generic hinge AVD reports the posture but keeps the inner display.
# The Fold8's inner is wider than tall like the Pixel Fold; the Ultra's is near-square
# like the Pixel 9 Pro Fold (an assumption: Samsung does not state its orientation).
FOLDABLES = {
    "Levana_Fold8_API_36_1": dict(
        name="Levana Galaxy Z Fold8", base="pixel_fold", inner=(2448, 1848), cover=(1248, 1972)),
    "Levana_Fold8_Ultra_API_36_1": dict(
        name="Levana Galaxy Z Fold8 Ultra", base="pixel_9_pro_fold", inner=(2256, 2504), cover=(1080, 2520)),
}
DENSITY = 420


def config_for(avd, spec, template):
    (width, height), (cover_width, cover_height) = spec["inner"], spec["cover"]
    config = dict(template)
    config.update({
        "AvdId": avd,
        "avd.ini.displayname": spec["name"],
        "hw.device.manufacturer": "Google",
        "hw.device.name": spec["base"],
        "hw.lcd.width": width,
        "hw.lcd.height": height,
        "hw.lcd.density": DENSITY,
        "hw.initialOrientation": "landscape" if width > height else "portrait",
        "hw.ramSize": 4096,
        "skin.name": f"{width}x{height}",
        "skin.path": f"{width}x{height}",
        "skin.dynamic": "yes",
        "showDeviceFrame": "no",
        "hw.sensor.hinge": "yes",
        "hw.sensor.hinge.count": 1,
        "hw.sensor.hinge.type": 1,
        "hw.sensor.hinge.sub_type": 1,
        "hw.sensor.hinge.ranges": "0-180",
        "hw.sensor.hinge.defaults": 180,
        # x-y-width-height of the hinge line, down the middle
        "hw.sensor.hinge.areas": f"{width // 2}-0-0-{height}",
        "hw.sensor.posture_list": "1, 2, 3",
        "hw.sensor.hinge_angles_posture_definitions": "0-30, 30-150, 150-180",
        "hw.sensor.hinge.fold_to_displayRegion.0.1_at_posture": 1,
        "hw.displayRegion.0.1.xOffset": 0,
        "hw.displayRegion.0.1.yOffset": 0,
        "hw.displayRegion.0.1.width": cover_width,
        "hw.displayRegion.0.1.height": cover_height,
    })
    return config


def create():
    template_ini = prepare_template()
    template = properties(avd_home() / f"{DEFAULT_AVD}.avd" / "config.ini")
    registration = properties(avd_home() / f"{DEFAULT_AVD}.ini")
    results = []
    for avd, spec in FOLDABLES.items():
        target = avd_home() / f"{avd}.avd"
        ini = avd_home() / f"{avd}.ini"
        if ini.exists() or target.exists():
            if not (target / MARKER).is_file():
                raise RuntimeError(f"Refusing to reuse unmanaged AVD: {target}")
            results.append(dict(avd=avd, created=False))
            continue
        target.mkdir()
        config = config_for(avd, spec, template)
        (target / "config.ini").write_text("".join(f"{k}={v}\n" for k, v in config.items()))
        (target / MARKER).write_text(json.dumps(dict(template=template_ini["avd"], **spec)) + "\n")
        ini.write_text(f"avd.ini.encoding=UTF-8\npath={target}\ntarget={registration['target']}\n")
        results.append(dict(avd=avd, created=True))
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["create", "list"])
    args = parser.parse_args()
    try:
        if args.command == "create":
            print(json.dumps(create()))
        else:
            print(json.dumps({avd: dict(spec, exists=(avd_home() / f"{avd}.ini").exists())
                              for avd, spec in FOLDABLES.items()}))
    except (OSError, RuntimeError, KeyError) as error:
        print(f"foldable-avds: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
