#!/usr/bin/env bash
set -e

echo "=== Speckit Setup Script ==="

# -----------------------------
# 1. Проверка наличия Speckit
# -----------------------------
if command -v specify >/dev/null 2>&1; then
    echo "[OK] Speckit CLI уже установлен."
else
    echo "[INFO] Speckit CLI не найден. Устанавливаю…"

    # Пытаемся установить в $HOME/.local/bin (не требует sudo)
    INSTALL_DIR="$HOME/.local/bin"
    mkdir -p "$INSTALL_DIR"

    curl -fsSL "https://raw.githubusercontent.com/github/spec-kit/main/install.sh" \
        | bash -s -- --bin-dir "$INSTALL_DIR"

    # Добавить в PATH
    if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
        echo "export PATH=\"$INSTALL_DIR:\$PATH\"" >> "$HOME/.bashrc"
        echo "[INFO] Добавил $INSTALL_DIR в PATH (bashrc)."
    fi

    echo "[OK] Speckit CLI установлено."
fi

# Перезагрузка PATH в текущей сессии
export PATH="$HOME/.local/bin:$PATH"

# -----------------------------
# 2. Проверка наличия каталога .specify
# -----------------------------
if [[ ! -d ".specify" ]]; then
    echo "[WARN] Каталог .specify не найден. Создаю…"
    mkdir -p .specify/scripts/bash
fi

# -----------------------------
# 3. Дать права на исполнение bash-скриптам
# -----------------------------
if [[ -d ".specify/scripts/bash" ]]; then
    chmod +x .specify/scripts/bash/*.sh 2>/dev/null || true
    echo "[OK] Скрипты в .specify/scripts/bash сделаны исполняемыми."
fi

# -----------------------------
# 4. Добавить .specify/scripts/bash в PATH
# -----------------------------
SPECIFY_SCRIPTS="$(pwd)/.specify/scripts/bash"
if [[ ":$PATH:" != *":$SPECIFY_SCRIPTS:"* ]]; then
    echo "export PATH=\"$SPECIFY_SCRIPTS:\$PATH\"" >> "$HOME/.bashrc"
    echo "[INFO] Добавил .specify/scripts/bash в PATH (bashrc)."
fi
export PATH="$SPECIFY_SCRIPTS:$PATH"

# -----------------------------
# 5. Инициализация Speckit в проекте
# -----------------------------
echo "[INFO] Инициализирую Speckit в текущем репозитории…"

specify init . \
    --ai cursor-agent \
    --force

echo "[OK] Speckit успешно инициализирован."

# -----------------------------
# 6. Проверка конфигурации
# -----------------------------
echo "[INFO] Запускаю 'specify check'…"
specify check || {
    echo "[ERROR] Speckit сообщает о проблемах. Проверьте вывод выше."
    exit 1
}

echo
echo "========================================="
echo "[SUCCESS] Speckit готов к работе!"
echo "Теперь команды /speckit.* будут работать."
echo "Откройте новый терминал, чтобы PATH обновился."
echo "========================================="
