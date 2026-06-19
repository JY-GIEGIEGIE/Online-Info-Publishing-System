#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA17_HOME="/Users/chenyyyyyy/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"; }
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }

cleanup() {
    log "正在关闭所有服务..."
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null && ok "后端已关闭 (PID $BACKEND_PID)"
    [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null && ok "前端已关闭 (PID $FRONTEND_PID)"
    log "所有服务已停止"
}
trap cleanup EXIT INT TERM

# ── 1. 检查 JDK 17 ──────────────────────────────────────────
log "检查 JDK 17..."
if [ ! -d "$JAVA17_HOME" ]; then
    fail "JDK 17 未找到: $JAVA17_HOME"
    exit 1
fi
export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
ok "JAVA_HOME = $JAVA_HOME ($(java -version 2>&1 | head -1))"

# ── 2. 检查 MySQL ───────────────────────────────────────────
log "检查 MySQL..."
if ! mysqladmin ping -u root -p'354500$AAbbcc' --silent 2>/dev/null; then
    warn "MySQL 未运行，尝试通过 brew services 启动..."
    brew services start mysql 2>/dev/null || {
        fail "无法启动 MySQL，请手动启动"
        exit 1
    }
    sleep 3
fi
ok "MySQL 运行中"

# ── 3. 检查 Redis ───────────────────────────────────────────
log "检查 Redis..."
if ! redis-cli ping &>/dev/null; then
    warn "Redis 未运行，尝试通过 brew services 启动..."
    brew services start redis 2>/dev/null || {
        fail "无法启动 Redis，请手动启动"
        exit 1
    }
    sleep 2
fi
ok "Redis 运行中"

# ── 4. 启动后端 (Spring Boot) ──────────────────────────────
log "启动后端 (Spring Boot)..."
cd "$PROJECT_DIR"
mvn spring-boot:run -q &
BACKEND_PID=$!
log "后端 PID: $BACKEND_PID"

# 等后端就绪
log "等待后端启动..."
for i in $(seq 1 60); do
    if curl -s http://localhost:8080/api/publish &>/dev/null; then
        ok "后端启动完成 (http://localhost:8080/api/publish)"
        break
    fi
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        fail "后端进程异常退出，请检查上方日志"
        exit 1
    fi
    sleep 2
done

if [ "$i" = "60" ]; then
    warn "后端可能仍在启动中，继续启动前端..."
fi

# ── 5. 启动前端 (Vite) ─────────────────────────────────────
log "启动前端 (Vite)..."
cd "$PROJECT_DIR/publish-frontend"
npm run dev &
FRONTEND_PID=$!
log "前端 PID: $FRONTEND_PID"

sleep 3
if kill -0 "$FRONTEND_PID" 2>/dev/null; then
    ok "前端启动完成 (http://localhost:3000)"
else
    fail "前端启动失败，请检查日志"
    exit 1
fi

# ── 6. 汇总 ─────────────────────────────────────────────────
echo ""
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo -e "${GREEN}  系统已启动！${NC}"
echo -e "${GREEN}  前端: ${CYAN}http://localhost:3000${NC}"
echo -e "${GREEN}  后端: ${CYAN}http://localhost:8080/api/publish${NC}"
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 等待任意子进程退出
wait
