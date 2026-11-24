#!/usr/bin/env bash
set -uo pipefail

cd "$(dirname "$0")" || exit 1

if [[ -d /usr/lib/jvm/java-21-openjdk ]]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
fi

java_sources="src/main/java"
plugin_descriptor="src/main/resources/plugin.yml"
errors=0
warnings=0

error() {
    echo "error: $*"
    errors=$((errors + 1))
}

warning() {
    echo "warning: $*"
    warnings=$((warnings + 1))
}

build_log="$(mktemp)"
trap 'rm -f "$build_log"' EXIT

echo "==> Building"
if ! mvn -B clean verify -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true >"$build_log" 2>&1; then
    error "maven build failed: $(grep -m1 -E '\[ERROR\]' "$build_log" || echo 'see maven output')"
fi
while IFS= read -r line; do
    warning "javac: $line"
done < <(grep -E '\[WARNING\] .*\.java:\[[0-9]+' "$build_log")

echo "==> Checking jar"
version="$(sed -n '0,/<version>/s|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -1)"
jar="target/ZeroWeapons.jar"
if [[ ! -f "$jar" ]]; then
    error "$jar was not built"
else
    jar_version="$(unzip -p "$jar" plugin.yml 2>/dev/null | sed -n "s/^version: '\{0,1\}\([^']*\)'\{0,1\}$/\1/p")"
    if [[ "$jar_version" != "$version" ]]; then
        error "plugin.yml version '$jar_version' does not match pom version '$version'"
    fi
fi

echo "==> Checking commands"
declared_commands="$(awk '/^commands:/{inside=1; next} /^[a-z]/{inside=0} inside && /^  [a-z0-9_-]+:$/{gsub(/[ :]/, ""); print}' "$plugin_descriptor")"
for command in $declared_commands; do
    if ! grep -rqF "\"$command\"" "$java_sources"; then
        error "/$command is declared in plugin.yml but never registered in Java"
    fi
done

echo "==> Checking permissions"
declared_permissions="$(awk '/^permissions:/{inside=1; next} /^[a-z]/{inside=0} inside && /^  [a-z0-9_.*-]+:$/{gsub(/[ :]/, ""); print}' "$plugin_descriptor")"
while IFS= read -r permission; do
    if ! grep -qxF "$permission" <<<"$declared_permissions"; then
        warning "permission $permission is used in Java but not declared in plugin.yml"
    fi
done < <(grep -rhoE '"zeroweapons\.[a-z0-9_.-]*[a-z0-9_-]"' "$java_sources" | tr -d '"' | sort -u)

echo "==> Checking sources"
while IFS= read -r line; do
    warning "unfinished: $line"
done < <(grep -rnE '\b(TODO|FIXME|XXX)\b' src)
if ! grep -q '^main: ' "$plugin_descriptor"; then
    error "plugin.yml must declare main"
fi

echo "==> $errors error(s), $warnings warning(s)"
[[ $errors -eq 0 && $warnings -eq 0 ]]
