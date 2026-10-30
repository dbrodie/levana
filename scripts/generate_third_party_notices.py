#!/usr/bin/env python3
"""
Generate THIRD_PARTY_NOTICES.md from the release dependency graph.

Run from the project root, with JAVA_HOME / ANDROID_HOME loaded:
    set -a && . ./.env && set +a && python3 scripts/generate_third_party_notices.py

How it works
------------
1. Runs `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`, the
   libraries that are actually packaged into the release APK, and takes each module's
   resolved version (the right-hand side of `->`). Constraint-only entries `(c)` and
   BOM / POM-only modules are skipped.
2. Reads each module's POM, following <parent> POMs, for its name, URL and <licenses>.
   POMs come from the Gradle cache (~/.gradle/caches/modules-2) when present; Gradle skips
   them for modules that publish Gradle module metadata, so the rest are downloaded from
   the project's repositories (Google Maven, then Maven Central) into build/pom-cache.
3. Normalises licence names / URLs to SPDX identifiers and writes THIRD_PARTY_NOTICES.md.

Uses only the Python standard library. Exits non-zero if a POM or licence cannot be
found, so the output is never silently incomplete.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import urllib.request
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "THIRD_PARTY_NOTICES.md"
CONFIGURATION = "releaseRuntimeClasspath"
GRADLE_HOME = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
CACHE = GRADLE_HOME / "caches" / "modules-2" / "files-2.1"
POM_CACHE = ROOT / "build" / "pom-cache"
# Same order as dependencyResolutionManagement in settings.gradle.kts
REPOSITORIES = ("https://dl.google.com/dl/android/maven2", "https://repo.maven.apache.org/maven2")

# Groups that are proprietary (not free software); flagged in the output.
PROPRIETARY_GROUPS = ("com.google.android.gms", "com.google.firebase")

LICENSE_TEXT_URLS = {
    "Apache-2.0": "https://www.apache.org/licenses/LICENSE-2.0",
    "LGPL-2.1": "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
    "MIT": "https://opensource.org/license/mit",
    "BSD-3-Clause": "https://opensource.org/license/bsd-3-clause",
    "EPL-1.0": "https://www.eclipse.org/legal/epl-v10.html",
    "EPL-2.0": "https://www.eclipse.org/legal/epl-2.0/",
}

# (regex on the licence name or URL, SPDX id); first match wins
LICENSE_PATTERNS = [
    (r"apache.*2|apache-2\.0|LICENSE-2\.0", "Apache-2.0"),
    (r"lgpl.*2\.1|lesser general public license.*2\.1|lgpl-2\.1", "LGPL-2.1"),
    (r"\bmit\b", "MIT"),
    (r"bsd.?3|new bsd|bsd-3-clause", "BSD-3-Clause"),
    (r"eclipse public license.*2|epl-2\.0", "EPL-2.0"),
    (r"eclipse public license|epl-1\.0", "EPL-1.0"),
    (r"android software development kit license|developer\.android\.com/studio/terms",
     "Android SDK License (proprietary)"),
]

ns = {"m": "http://maven.apache.org/POM/4.0.0"}


def run_gradle() -> str:
    cmd = ["./gradlew", "-q", ":app:dependencies", "--configuration", CONFIGURATION]
    return subprocess.run(cmd, cwd=ROOT, check=True, capture_output=True, text=True).stdout


def parse_modules(tree: str) -> set[tuple[str, str, str]]:
    modules = set()
    for line in tree.splitlines():
        m = re.search(r"[+\\]--- (\S+?):(\S+?):(\S+)(.*)$", line)
        if not m:
            continue
        group, artifact, version, rest = m.groups()
        if "(c)" in rest or "(n)" in rest:
            continue
        resolved = re.search(r"-> (\S+)", rest)
        if resolved:
            version = resolved.group(1)
        modules.add((group, artifact, version))
    return modules


def find_pom(group: str, artifact: str, version: str) -> Path | None:
    hits = sorted((CACHE / group / artifact / version).glob(f"*/{artifact}-{version}.pom"))
    if hits:
        return hits[0]
    local = POM_CACHE / group / f"{artifact}-{version}.pom"
    if local.exists():
        return local
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    for repo in REPOSITORIES:
        try:
            with urllib.request.urlopen(f"{repo}/{path}", timeout=30) as resp:
                data = resp.read()
        except OSError:
            continue
        local.parent.mkdir(parents=True, exist_ok=True)
        local.write_bytes(data)
        return local
    return None


def text(el: ET.Element | None, path: str) -> str | None:
    if el is None:
        return None
    found = el.find(path, ns)
    return found.text.strip() if found is not None and found.text else None


def read_pom(group: str, artifact: str, version: str) -> dict:
    """Name, URL, packaging and licences, inheriting licences/URL from parent POMs."""
    pom = find_pom(group, artifact, version)
    if pom is None:
        raise SystemExit(f"POM not found in Gradle cache: {group}:{artifact}:{version}")
    root = ET.parse(pom).getroot()
    info = {
        "name": text(root, "m:name"),
        "url": text(root, "m:url"),
        "packaging": text(root, "m:packaging") or "jar",
        "licenses": [
            (text(lic, "m:name") or "", text(lic, "m:url") or "")
            for lic in root.findall("m:licenses/m:license", ns)
        ],
    }
    parent = root.find("m:parent", ns)
    if parent is not None and (not info["licenses"] or not info["url"]):
        p = read_pom(text(parent, "m:groupId"), text(parent, "m:artifactId"),
                     text(parent, "m:version"))
        info["licenses"] = info["licenses"] or p["licenses"]
        info["url"] = info["url"] or p["url"]
    return info


def spdx(name: str, url: str) -> str:
    for pattern, ident in LICENSE_PATTERNS:
        if re.search(pattern, name, re.I) or re.search(pattern, url, re.I):
            return ident
    return name or url or "UNKNOWN"


def main() -> None:
    modules = parse_modules(run_gradle())
    rows = []
    for group, artifact, version in sorted(modules):
        info = read_pom(group, artifact, version)
        if info["packaging"] == "pom":  # BOMs / platforms ship no code
            continue
        licenses = sorted({spdx(n, u) for n, u in info["licenses"]})
        if not licenses:
            raise SystemExit(f"No licence declared: {group}:{artifact}:{version}")
        rows.append((group, artifact, version, info["name"] or artifact, info["url"], licenses))

    by_license = defaultdict(int)
    for *_, licenses in rows:
        for lic in licenses:
            by_license[lic] += 1

    out = [
        "# Third-party notices",
        "",
        "Levana itself is licensed under the MIT License (see [LICENSE](LICENSE)). The release",
        "APK bundles the third-party libraries below, each under its own licence.",
        "",
        "<!-- Generated by scripts/generate_third_party_notices.py from the "
        f"`{CONFIGURATION}` dependency graph; do not edit by hand. -->",
        "",
        "## GNU LGPL-2.1 notice for KosherJava",
        "",
        "Levana uses the [KosherJava Zmanim library](https://github.com/KosherJava/zmanim),",
        "licensed under the GNU Lesser General Public License, version 2.1. It is used",
        "unmodified, as the published `com.kosherjava:zmanim` artifact. Its source is available",
        "from the KosherJava repository, and Levana's own source (to rebuild the app against a",
        "modified copy of the library) is in this repository.",
        "",
    ]
    proprietary = [r for r in rows if r[0].startswith(PROPRIETARY_GROUPS)]
    if proprietary:
        out += [
            "## Proprietary dependencies (pending removal)",
            "",
            "The following Google Play Services libraries are not free software. They are being",
            "removed in TASK-53.2 (replaced by the platform `LocationManager`); rerun this script",
            "afterwards to drop them from this file.",
            "",
            *[f"- `{g}:{a}:{v}`" for g, a, v, *_ in proprietary],
            "",
        ]
    out += ["## Licence summary", "", "| Licence | Libraries |", "|---|---|"]
    for lic, count in sorted(by_license.items(), key=lambda kv: (-kv[1], kv[0])):
        link = LICENSE_TEXT_URLS.get(lic)
        out.append(f"| {f'[{lic}]({link})' if link else lic} | {count} |")
    out += ["", "## Libraries", "", "| Library | Artifact | Version | Licence |", "|---|---|---|---|"]
    for group, artifact, version, name, url, licenses in rows:
        title = f"[{name}]({url})" if url else name
        out.append(f"| {title} | `{group}:{artifact}` | {version} | {', '.join(licenses)} |")
    out.append("")

    OUTPUT.write_text("\n".join(out))
    print(f"Wrote {OUTPUT.relative_to(ROOT)}: {len(rows)} libraries, "
          f"{len(proprietary)} proprietary")
    unknown = [lic for lic in by_license if lic not in LICENSE_TEXT_URLS
               and "proprietary" not in lic]
    if unknown:
        print(f"Unrecognised licences, check the output: {unknown}", file=sys.stderr)


if __name__ == "__main__":
    main()
