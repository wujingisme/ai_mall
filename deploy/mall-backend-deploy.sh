#!/usr/bin/env bash
set -Eeuo pipefail

# The script is uploaded to the server by .github/workflows/deploy-backend.yml.
# Keep secrets in backend.env; never put them in this file or the Git repository.

APP_DIR="${APP_DIR:-/www/wwwroot/mymall/backend}"
APP_JAR="$APP_DIR/mall-backend-1.0.0.jar"
RELEASE_DIR="$APP_DIR/releases"
BACKUP_DIR="$APP_DIR/backups"
ENV_FILE="$APP_DIR/backend.env"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/mall-backend.log"
PID_FILE="$APP_DIR/mall-backend.pid"
JAVA_BIN="${JAVA_BIN:-/www/server/java/jdk-17.0.8/bin/java}"
STAGED_JAR="${1:-}"

if [[ "$EUID" -ne 0 ]]; then
  echo "This deployment script must run as root or through passwordless sudo." >&2
  exit 1
fi

if [[ -z "$STAGED_JAR" || ! -f "$STAGED_JAR" ]]; then
  echo "Usage: $0 /path/to/staged/mall-backend-<commit>.jar" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing environment file: $ENV_FILE" >&2
  exit 1
fi

if grep -q $'\r' "$ENV_FILE"; then
  echo "backend.env contains Windows CRLF line endings. Convert it with: sed -i 's/\\r$//' $ENV_FILE" >&2
  exit 1
fi

if ! id www >/dev/null 2>&1; then
  echo "The www service user does not exist." >&2
  exit 1
fi

if [[ ! -x "$JAVA_BIN" ]]; then
  echo "Java executable not found: $JAVA_BIN" >&2
  exit 1
fi

if ! runuser -u www -- test -r "$ENV_FILE"; then
  echo "The www user cannot read $ENV_FILE. Fix its ownership/permissions before deploying." >&2
  exit 1
fi

load_env_file() {
  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    if [[ "$line" != *=* ]]; then
      echo "Invalid environment line (expected KEY=VALUE): $line" >&2
      exit 1
    fi
    key="${line%%=*}"
    value="${line#*=}"
    if [[ ! "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      echo "Invalid environment variable name: $key" >&2
      exit 1
    fi
    # Export as one assignment instead of sourcing the file: DB_URL contains
    # ampersands, and sourcing an unquoted URL would let the shell split it.
    export "$key=$value"
  done < "$ENV_FILE"
}

load_env_file
SERVER_PORT="${SERVER_PORT:-8080}"

if [[ ! "$SERVER_PORT" =~ ^[0-9]+$ ]]; then
  echo "SERVER_PORT must be numeric; got: $SERVER_PORT" >&2
  exit 1
fi

mkdir -p "$RELEASE_DIR" "$BACKUP_DIR" "$LOG_DIR"
chown www:www "$RELEASE_DIR" "$BACKUP_DIR" "$LOG_DIR"
chmod 750 "$RELEASE_DIR" "$BACKUP_DIR" "$LOG_DIR"
touch "$LOG_FILE"
chown www:www "$LOG_FILE"
chmod 640 "$LOG_FILE"

stop_app() {
  local pid=""
  if [[ -f "$PID_FILE" ]]; then
    read -r pid < "$PID_FILE" || true
  fi

  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    for _ in {1..30}; do
      if ! kill -0 "$pid" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"

  # Baota may have started the process without our PID file. Match only this
  # exact jar path so unrelated Java services are not stopped.
  while read -r old_pid; do
    [[ -z "$old_pid" || "$old_pid" == "$$" ]] && continue
    kill "$old_pid" 2>/dev/null || true
  done < <(pgrep -f -- "$APP_JAR" || true)
  sleep 1
}

start_app() {
  rm -f "$PID_FILE"
  touch "$PID_FILE"
  chown www:www "$PID_FILE"
  runuser -u www -- /bin/bash -c '
    set -Eeuo pipefail
    while IFS= read -r line || [[ -n "$line" ]]; do
      [[ -z "$line" || "$line" == \#* ]] && continue
      key="${line%%=*}"
      value="${line#*=}"
      export "$key=$value"
    done < "$1"
    nohup "$2" -jar "$3" >> "$4" 2>&1 < /dev/null &
    echo $! > "$5"
  ' _ "$ENV_FILE" "$JAVA_BIN" "$APP_JAR" "$LOG_FILE" "$PID_FILE"
  sleep 1
}

wait_for_ready() {
  local pid=""
  for _ in {1..30}; do
    if [[ -f "$PID_FILE" ]]; then
      read -r pid < "$PID_FILE" || true
    fi
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      if curl --fail --silent --show-error --max-time 3 \
        "http://127.0.0.1:$SERVER_PORT/api/v1/shop/products?page=1&pageSize=1" \
        >/dev/null 2>&1; then
        return 0
      fi
    fi
    sleep 2
  done
  return 1
}

backup_file=""
if [[ -f "$APP_JAR" ]]; then
  backup_file="$BACKUP_DIR/mall-backend-$(date +%Y%m%d%H%M%S).jar"
  cp -p "$APP_JAR" "$backup_file"
fi

stop_app
install -o www -g www -m 0644 "$STAGED_JAR" "$APP_JAR"
start_app

if ! wait_for_ready; then
  echo "New backend failed the health check. Rolling back the previous JAR." >&2
  tail -n 100 "$LOG_FILE" >&2 || true
  stop_app
  if [[ -n "$backup_file" && -f "$backup_file" ]]; then
    install -o www -g www -m 0644 "$backup_file" "$APP_JAR"
    start_app
    if ! wait_for_ready; then
      echo "Rollback also failed. Inspect $LOG_FILE immediately." >&2
      exit 1
    fi
  fi
  exit 1
fi

# Keep the five newest backups for a quick manual rollback.
mapfile -t old_backups < <(ls -1t "$BACKUP_DIR"/mall-backend-*.jar 2>/dev/null || true)
if (( ${#old_backups[@]} > 5 )); then
  for old_backup in "${old_backups[@]:5}"; do
    rm -f -- "$old_backup"
  done
fi

echo "Backend deployed successfully: $APP_JAR"
