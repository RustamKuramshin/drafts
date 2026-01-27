#!/usr/bin/env bash
#
# search-by-envs-and-secrets.sh — поиск подстроки в окружении деплойментов и связанных Secret/ConfigMap.
#
# Возможности:
# - Принимает kubectl context (обязателен), namespace (опционально) и строку поиска (обязательна)
# - Если namespace не указан, обходит все namespace в кластере
# - Просматривает все Deployment: env/envFrom, Secret/ConfigMap из env*, volumes и projected volumes
# - Печатает найденные вхождения и помечает проверенные namespace/deployment ('-' если совпадений нет)
#
set -Eeuo pipefail
IFS=$'\n\t'

log()  { printf "[%s] %s\n" "$(date '+%F %T')" "$*"; }
inf()  { log "INFO  $*"; }
wrn()  { log "WARN  $*"; }
err()  { log "ERROR $*" >&2; }
die()  { err "$*"; exit 1; }

usage() {
  cat <<'EOF'
Usage:
  search-by-envs-and-secrets.sh --context <k8s-context> --search <substring> [--namespace <ns>] [--ignore-case]

Required:
  -k, --context       Имя kubectl контекста
  -s, --search        Подстрока для поиска

Optional:
  -n, --namespace     Namespace; если не указан — обойти все
  -i, --ignore-case   Нечувствительный к регистру поиск
  -h, --help          Показать справку

Пример:
  ./search-by-envs-and-secrets.sh -k market-dev-app -n svc-b2bc-backend -s redis-svc-b2bc-backend --ignore-case
EOF
}

CTX=""
NS=""
QUERY=""
IGNORE_CASE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -k|--context)      CTX="${2:-}"; shift 2;;
    -n|--namespace)    NS="${2:-}"; shift 2;;
    -s|--search)       QUERY="${2:-}"; shift 2;;
    -i|--ignore-case)  IGNORE_CASE=1; shift;;
    -h|--help)         usage; exit 0;;
    *) die "Неизвестный аргумент: $1 (см. --help)";;
  esac
done

[[ -z "$CTX" || -z "$QUERY" ]] && { usage; die "Нужно указать --context и --search"; }

command -v kubectl >/dev/null 2>&1 || die "Не найден kubectl в PATH"
command -v python3  >/dev/null 2>&1 || die "Не найден python3 в PATH"

inf "Старт поиска: context='${CTX}', namespace='${NS:-<all>}', query='${QUERY}', ignore-case=${IGNORE_CASE}"

python3 - "$CTX" "${NS:-}" "$QUERY" "$IGNORE_CASE" <<'PY'
import base64
import datetime as dt
import json
import subprocess
import sys
from typing import Dict, List, Set, Tuple

ctx = sys.argv[1]
namespace_arg = sys.argv[2]
query = sys.argv[3]
ignore_case = sys.argv[4] == "1"
query_lower = query.lower()


def log(level: str, msg: str, stream=sys.stdout):
    ts = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{ts}] {level:<5} {msg}", file=stream)


def inf(msg: str):
    log("INFO", msg)


def wrn(msg: str):
    log("WARN", msg)


def err(msg: str):
    log("ERROR", msg, stream=sys.stderr)


def die(msg: str, code: int = 1):
    err(msg)
    sys.exit(code)


def match(text: str) -> bool:
    if text is None:
        return False
    hay = str(text)
    return query_lower in hay.lower() if ignore_case else query in hay


def run_kubectl(args: List[str], allow_fail: bool = False) -> str:
    cmd = ["kubectl", "--context", ctx] + args
    try:
        proc = subprocess.run(cmd, check=True, capture_output=True, text=True)
        return proc.stdout
    except subprocess.CalledProcessError as exc:
        if allow_fail:
            wrn(f"kubectl {' '.join(args)} завершился с ошибкой: {exc.stderr.strip() or exc}")
            return ""
        die(f"kubectl {' '.join(args)} завершился с ошибкой: {exc.stderr.strip() or exc}", exc.returncode or 1)
        return ""  # недостижимо


def run_kubectl_json(args: List[str], allow_fail: bool = False) -> dict:
    out = run_kubectl(args + ["-o", "json"], allow_fail=allow_fail)
    if not out.strip():
        return {}
    try:
        return json.loads(out)
    except json.JSONDecodeError as exc:
        die(f"Не удалось распарсить JSON kubectl {' '.join(args)}: {exc}")
        return {}


def get_namespaces() -> List[str]:
    if namespace_arg:
        return [namespace_arg]
    data = run_kubectl_json(["get", "ns"])
    return [item.get("metadata", {}).get("name") for item in data.get("items", [])]


def decode_b64(val: str) -> str:
    try:
        return base64.b64decode(val).decode("utf-8", "replace")
    except Exception:
        return ""


def load_configmaps(ns: str) -> Dict[str, Dict[str, str]]:
    data = run_kubectl_json(["get", "configmap", "-n", ns], allow_fail=True)
    result: Dict[str, Dict[str, str]] = {}
    for item in data.get("items", []):
        name = item.get("metadata", {}).get("name")
        if not name:
            continue
        combined = {}
        for k, v in (item.get("data") or {}).items():
            combined[k] = v or ""
        for k, v in (item.get("binaryData") or {}).items():
            combined[k] = decode_b64(v)
        result[name] = combined
    return result


def load_secrets(ns: str) -> Dict[str, Dict[str, str]]:
    data = run_kubectl_json(["get", "secret", "-n", ns], allow_fail=True)
    result: Dict[str, Dict[str, str]] = {}
    for item in data.get("items", []):
        name = item.get("metadata", {}).get("name")
        if not name:
            continue
        decoded = {}
        for k, v in (item.get("data") or {}).items():
            decoded[k] = decode_b64(v)
        result[name] = decoded
    return result


def find_lines_with_match(val: str) -> List[str]:
    if val is None:
        return []
    text = str(val)
    lines = text.splitlines() or [text]
    matches = []
    for line in lines:
        if match(line):
            matches.append(line)
    return matches


def get_secret_value(secrets: Dict[str, Dict[str, str]], name: str, key: str) -> Tuple[str, bool]:
    if not name:
        return "", False
    data = secrets.get(name)
    if data is None:
        wrn(f"Secret '{name}' отсутствует в namespace")
        return "<missing secret>", False
    if key not in data:
        wrn(f"Secret '{name}' не содержит ключ '{key}'")
        return "<missing key>", False
    return data.get(key, ""), True


def get_configmap_value(configmaps: Dict[str, Dict[str, str]], name: str, key: str) -> Tuple[str, bool]:
    if not name:
        return "", False
    data = configmaps.get(name)
    if data is None:
        wrn(f"ConfigMap '{name}' отсутствует в namespace")
        return "<missing configmap>", False
    if key not in data:
        wrn(f"ConfigMap '{name}' не содержит ключ '{key}'")
        return "<missing key>", False
    return data.get(key, ""), True


def scan_deployment(dep: dict, configmaps: Dict[str, Dict[str, str]], secrets: Dict[str, Dict[str, str]]) -> Tuple[List[str], List[str], List[str]]:
    env_matches: List[str] = []
    secret_matches: List[str] = []
    configmap_matches: List[str] = []
    attached_configmaps: Set[str] = set()
    attached_secrets: Set[str] = set()

    dep_name = dep.get("metadata", {}).get("name", "<unknown>")
    spec = dep.get("spec", {}).get("template", {}).get("spec", {})
    volumes = spec.get("volumes") or []
    containers = (spec.get("containers") or []) + (spec.get("initContainers") or [])

    for vol in volumes:
        cm = (vol.get("configMap") or {}).get("name")
        if cm:
            attached_configmaps.add(cm)
        sec = (vol.get("secret") or {}).get("secretName")
        if sec:
            attached_secrets.add(sec)
        projected = (vol.get("projected") or {}).get("sources") or []
        for src in projected:
            cm = (src.get("configMap") or {}).get("name")
            if cm:
                attached_configmaps.add(cm)
            sec = (src.get("secret") or {}).get("name")
            if sec:
                attached_secrets.add(sec)

    for c in containers:
        cname = c.get("name", "<container>")
        for env in c.get("env") or []:
            env_name = env.get("name", "")
            matched = False
            value_text = ""
            source_desc = ""

            if "value" in env:
                value_text = env.get("value") or ""
                source_desc = "literal"
                matched = match(env_name) or match(value_text)
            elif "valueFrom" in env:
                vf = env["valueFrom"] or {}
                if "secretKeyRef" in vf:
                    ref = vf["secretKeyRef"] or {}
                    sname = ref.get("name")
                    key = ref.get("key", "")
                    if sname:
                        attached_secrets.add(sname)
                    value_text, resolved = get_secret_value(secrets, sname, key)
                    source_desc = f"secret/{sname}:{key}"
                    matched = match(env_name) or match(sname) or match(key) or (resolved and match(value_text))
                elif "configMapKeyRef" in vf:
                    ref = vf["configMapKeyRef"] or {}
                    cmname = ref.get("name")
                    key = ref.get("key", "")
                    if cmname:
                        attached_configmaps.add(cmname)
                    value_text, resolved = get_configmap_value(configmaps, cmname, key)
                    source_desc = f"configmap/{cmname}:{key}"
                    matched = match(env_name) or match(cmname) or match(key) or (resolved and match(value_text))
                elif "fieldRef" in vf:
                    fp = (vf.get("fieldRef") or {}).get("fieldPath", "")
                    value_text = f"<fieldRef:{fp}>"
                    source_desc = value_text
                    matched = match(env_name) or match(fp)
                elif "resourceFieldRef" in vf:
                    rfr = vf.get("resourceFieldRef") or {}
                    resource = rfr.get("resource", "")
                    cn = rfr.get("containerName", "")
                    value_text = f"<resourceField:{resource}>"
                    source_desc = value_text
                    matched = match(env_name) or match(resource) or match(cn)

            if matched:
                env_matches.append(f"{cname}: {env_name}={value_text} ({source_desc or 'valueFrom'})")

        for env_from in c.get("envFrom") or []:
            prefix = env_from.get("prefix", "") or ""
            if "secretRef" in env_from:
                ref = env_from["secretRef"] or {}
                sname = ref.get("name")
                optional = ref.get("optional", False)
                if sname:
                    attached_secrets.add(sname)
                data = secrets.get(sname)
                if data is None:
                    if not optional:
                        wrn(f"Secret '{sname}' из envFrom контейнера '{cname}' в деплойменте '{dep_name}' не найден")
                    continue
                for key, val in data.items():
                    env_name = f"{prefix}{key}"
                    if match(env_name) or match(sname) or match(key) or match(val):
                        env_matches.append(f"{cname}: {env_name}={val} (envFrom secret/{sname})")
            elif "configMapRef" in env_from:
                ref = env_from["configMapRef"] or {}
                cmname = ref.get("name")
                optional = ref.get("optional", False)
                if cmname:
                    attached_configmaps.add(cmname)
                data = configmaps.get(cmname)
                if data is None:
                    if not optional:
                        wrn(f"ConfigMap '{cmname}' из envFrom контейнера '{cname}' в деплойменте '{dep_name}' не найден")
                    continue
                for key, val in data.items():
                    env_name = f"{prefix}{key}"
                    if match(env_name) or match(cmname) or match(key) or match(val):
                        env_matches.append(f"{cname}: {env_name}={val} (envFrom configmap/{cmname})")

    for sec_name in sorted(attached_secrets):
        data = secrets.get(sec_name)
        if data is None:
            wrn(f"Secret '{sec_name}' указан в деплойменте '{dep_name}', но не найден")
            continue
        for key, val in data.items():
            lines = find_lines_with_match(val)
            if lines or match(sec_name) or match(key):
                if not lines:
                    lines = [val]
                for line in lines:
                    secret_matches.append(f"{sec_name}:{key}={line}")

    for cm_name in sorted(attached_configmaps):
        data = configmaps.get(cm_name)
        if data is None:
            wrn(f"ConfigMap '{cm_name}' указан в деплойменте '{dep_name}', но не найден")
            continue
        for key, val in data.items():
            lines = find_lines_with_match(val)
            if lines or match(cm_name) or match(key):
                if not lines:
                    lines = [val]
                for line in lines:
                    configmap_matches.append(f"{cm_name}:{key}={line}")

    return env_matches, secret_matches, configmap_matches


def main():
    namespaces = get_namespaces()
    if not namespaces:
        die("Список namespace пуст — нечего сканировать")

    for ns in namespaces:
        inf(f"Namespace '{ns}'")
        deploy_list = run_kubectl_json(["get", "deploy", "-n", ns])
        deployments = deploy_list.get("items", [])
        configmaps = load_configmaps(ns)
        secrets = load_secrets(ns)

        if not deployments:
            inf("  Deployments: -")
            continue

        for dep in deployments:
            dep_name = dep.get("metadata", {}).get("name", "<unknown>")
            inf(f"  Deployment '{dep_name}'")
            env_m, sec_m, cm_m = scan_deployment(dep, configmaps, secrets)

            if env_m:
                for line in env_m:
                    inf(f"    env: {line}")
            else:
                inf("    env: -")

            if sec_m:
                for line in sec_m:
                    inf(f"    secrets: {line}")
            else:
                inf("    secrets: -")

            if cm_m:
                for line in cm_m:
                    inf(f"    configmaps: {line}")
            else:
                inf("    configmaps: -")


if __name__ == "__main__":
    main()
PY
