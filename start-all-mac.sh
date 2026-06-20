#!/bin/bash
# 网上信息发布子系统 — 一键环境检查 + 启动全部服务 (macOS)
set -e

RED='\033[0;31m' GREEN='\033[0;32m' YELLOW='\033[1;33m' NC='\033[0m'
pass() { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "========================================"
echo "  网上信息发布子系统 — 环境检查与启动"
echo "  Platform: macOS"
echo "========================================"
echo ""

# ----- 1. JDK 17 -----
echo "[1/5] 检查 JDK 17..."

# 按优先级查找 JDK 17
JAVA_HOME_CANDIDATES=(
    "$PROJECT_DIR/jdk-17.0.12+7/Contents/Home"
    "/Users/chenyyyyyy/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home"
    "$(dirname $(dirname $(readlink -f $(which java) 2>/dev/null) 2>/dev/null) 2>/dev/null)"
)

JAVA17_HOME=""
for candidate in "${JAVA_HOME_CANDIDATES[@]}"; do
    if [ -x "$candidate/bin/java" ]; then
        JAVA_VER=$("$candidate/bin/java" -version 2>&1 | head -1)
        if echo "$JAVA_VER" | grep -qE 'version "(17\.|[1-9][0-9]+\.)'; then
            JAVA17_HOME="$candidate"
            break
        fi
    fi
done

# 如果没找到 JDK 17+，尝试用 /usr/libexec/java_home
if [ -z "$JAVA17_HOME" ] && [ -x /usr/libexec/java_home ]; then
    JAVA17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null) || true
fi

if [ -z "$JAVA17_HOME" ]; then
    fail "未找到 JDK 17，请安装: brew install openjdk@17"
fi

export JAVA_HOME="$JAVA17_HOME"
JAVA_VER=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)
pass "JDK: $JAVA_HOME ($JAVA_VER)"

# ----- 2. MySQL -----
echo "[2/5] 检查 MySQL..."

# 读取 application.yml 中的 MySQL 配置
MYSQL_USER=$(grep -A5 'datasource:' "$PROJECT_DIR/src/main/resources/application.yml" | grep 'username:' | awk '{print $2}')
MYSQL_PASS=$(grep -A5 'datasource:' "$PROJECT_DIR/src/main/resources/application.yml" | grep 'password:' | awk '{print $2}')
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-root}"

MYSQL_RUNNING=false
mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1" > /dev/null 2>&1 && MYSQL_RUNNING=true

if [ "$MYSQL_RUNNING" = false ]; then
    # 尝试无密码连接 (Homebrew 默认)
    mysql -u root -e "SELECT 1" > /dev/null 2>&1 && MYSQL_RUNNING=true
fi

if [ "$MYSQL_RUNNING" = false ]; then
    warn "MySQL 未运行，尝试通过 Homebrew 启动..."
    brew services start mysql 2>/dev/null || true
    for i in $(seq 1 15); do
        sleep 2
        mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1" > /dev/null 2>&1 && { MYSQL_RUNNING=true; break; }
    done
fi

mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1" > /dev/null 2>&1 || {
    fail "MySQL 连接失败，请检查 application.yml 中的用户名/密码，或手动启动 MySQL"
}
pass "MySQL 就绪 ($MYSQL_USER@localhost)"

# 确保数据库存在
mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "CREATE DATABASE IF NOT EXISTS stock_publish DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" > /dev/null 2>&1

# ----- 3. Redis -----
echo "[3/5] 检查 Redis..."

REDIS_RUNNING=false
redis-cli ping 2>/dev/null && REDIS_RUNNING=true

if [ "$REDIS_RUNNING" = false ]; then
    warn "Redis 未运行，尝试通过 Homebrew 启动..."
    brew services start redis 2>/dev/null || true
    for i in $(seq 1 10); do
        sleep 1
        redis-cli ping 2>/dev/null && { REDIS_RUNNING=true; break; }
    done
fi

redis-cli ping 2>/dev/null || fail "Redis 启动失败，请手动启动: brew services start redis"
pass "Redis 就绪"

# ----- 4. Node.js -----
echo "[4/5] 检查 Node.js..."

NODE_VER=$(node -v 2>/dev/null) || fail "未找到 Node.js，请安装: brew install node"
pass "Node.js $NODE_VER"

cd "$PROJECT_DIR/publish-frontend"
if [ ! -d "node_modules" ]; then
    warn "安装前端依赖..."
    npm install
fi

# ----- 5. Backend + Frontend -----
echo "[5/5] 编译并启动服务..."

# 杀掉旧进程
cd "$PROJECT_DIR"
pkill -f "online-info-publish-subsys" 2>/dev/null || true
pkill -f "vite" 2>/dev/null || true
sleep 2

# 检查 Maven
MVN_CMD="mvn"
if [ -x "$PROJECT_DIR/apache-maven-3.9.9/bin/mvn" ]; then
    MVN_CMD="$PROJECT_DIR/apache-maven-3.9.9/bin/mvn"
fi

# 后端
echo "  编译后端 (Maven)..."
export PATH="$JAVA_HOME/bin:$PATH"
$MVN_CMD package -DskipTests -q 2>&1 || fail "后端编译失败，请检查代码"
JAR_FILE=$(ls target/online-info-publish-subsys-*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    fail "未找到 jar 文件，编译可能未成功"
fi
echo "  启动后端: $JAR_FILE"
nohup java -jar "$JAR_FILE" > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "  后端 PID: $BACKEND_PID"

for i in $(seq 1 25); do
    sleep 2
    curl -s "http://localhost:8080/api/publish/stock/search?keyword=600" > /dev/null 2>&1 && break
done
curl -s "http://localhost:8080/api/publish/stock/search?keyword=600" > /dev/null 2>&1 || {
    warn "后端可能仍在启动中，查看日志: tail -f /tmp/backend.log"
}
pass "后端: http://localhost:8080/api/publish"

# 前端
echo "  启动前端 (Vite)..."
cd "$PROJECT_DIR/publish-frontend"
nohup npx vite --host > /tmp/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "  前端 PID: $FRONTEND_PID"

for i in $(seq 1 10); do
    sleep 1
    curl -s "http://localhost:3000" > /dev/null 2>&1 && break
done
curl -s "http://localhost:3000" > /dev/null 2>&1 || {
    warn "前端可能仍在启动中，查看日志: tail -f /tmp/frontend.log"
}
pass "前端: http://localhost:3000"

echo ""
echo "========================================"
echo "  ${GREEN}全部服务已启动${NC}"
echo "  前端: http://localhost:3000"
echo "  后端: http://localhost:8080/api/publish"
echo "  PID  : 后端=$BACKEND_PID  前端=$FRONTEND_PID"
echo ""
echo "  停止服务: kill $BACKEND_PID $FRONTEND_PID"
echo "  查看日志: tail -f /tmp/backend.log /tmp/frontend.log"
echo "========================================"

# 保持前台
wait
