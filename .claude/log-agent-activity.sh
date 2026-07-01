#!/usr/bin/env bash
# Registra inicio de sesión, prompts de usuario y fin de interacción en .github/logs/activity-YYYY-MM-DD.jsonl
# Eventos: SessionStart | UserPromptSubmit | Stop
# Requiere finales de línea LF (no CRLF)
# Sin dependencias externas: solo bash, grep, sed (disponibles en macOS y Linux).

DONE='{"continue":true}'
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOGS_DIR="$SCRIPT_DIR/logs"

# Lee stdin completo en una variable
INPUT="$(cat)"

# Extrae el valor de un campo string del JSON plano: json_get <json> <field>
json_get() {
  echo "$1" | grep -o "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 \
             | sed 's/.*":[[:space:]]*"\(.*\)"/\1/'
}

# Escapar caracteres especiales JSON en un string
json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/\t/\\t/g; s/\r//g' \
    | awk '{printf "%s\\n", $0}' | sed '$ s/\\n$//'
}

HOOK_EVENT="$(json_get "$INPUT" hookEventName)"
[ -z "$HOOK_EVENT" ] && HOOK_EVENT="$(json_get "$INPUT" hook_event_name)"

NOW="$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")"
DATE="$(date -u +"%Y-%m-%d")"
SESSION_ID="$(json_get "$INPUT" session_id)"
[ -z "$SESSION_ID" ] && SESSION_ID="$(json_get "$INPUT" sessionId)"
CWD="$(json_get "$INPUT" cwd)"

ENTRY=""

if [ "$HOOK_EVENT" = "SessionStart" ]; then
  ENTRY="{\"type\":\"CREACION_SESION\",\"sessionId\":\"$(json_escape "$SESSION_ID")\",\"startedAt\":\"$NOW\",\"cwd\":\"$(json_escape "$CWD")\"}"

elif [ "$HOOK_EVENT" = "UserPromptSubmit" ]; then
  PROMPT="$(json_get "$INPUT" prompt)"
  PROMPT="${PROMPT:0:500}"
  ENTRY="{\"type\":\"INTERACCION\",\"sessionId\":\"$(json_escape "$SESSION_ID")\",\"timestamp\":\"$NOW\",\"prompt\":\"$(json_escape "$PROMPT")\"}"

elif [ "$HOOK_EVENT" = "Stop" ]; then
  ENTRY="{\"type\":\"FIN_INTERACCION\",\"sessionId\":\"$(json_escape "$SESSION_ID")\",\"timestamp\":\"$NOW\"}"

else
  echo "$DONE"
  exit 0
fi

mkdir -p "$LOGS_DIR"
printf '%s\n' "$ENTRY" >> "$LOGS_DIR/activity-$DATE.jsonl"

echo "$DONE"
