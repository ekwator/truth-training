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
# 5. Резервное копирование перед инициализацией
# -----------------------------
echo "[INFO] Проверяю наличие кастомных файлов для резервного копирования…"

# Резервное копирование constitution.md
if [[ -f ".specify/memory/constitution.md" ]]; then
    echo "[INFO] Создаю резервную копию constitution.md…"
    mkdir -p .specify/memory
    cp .specify/memory/constitution.md .specify/memory/constitution-backup.md
    echo "[OK] Резервная копия создана: .specify/memory/constitution-backup.md"
fi

# Резервное копирование кастомных шаблонов
if [[ -d ".specify/templates" ]]; then
    echo "[INFO] Создаю резервную копию кастомных шаблонов…"
    cp -r .specify/templates .specify/templates-backup
    echo "[OK] Резервная копия создана: .specify/templates-backup"
fi

# -----------------------------
# 6. Инициализация Speckit в проекте
# -----------------------------
echo "[INFO] Инициализирую Speckit в текущем репозитории…"

specify init . \
    --ai cursor-agent \
    --force

echo "[OK] Speckit успешно инициализирован."

# -----------------------------
# 7. Восстановление кастомных файлов из git (если это git репозиторий)
# -----------------------------
if command -v git >/dev/null 2>&1 && git rev-parse --git-dir >/dev/null 2>&1; then
    echo "[INFO] Обнаружен git репозиторий. Восстанавливаю кастомные файлы из git истории…"
    
    # Восстановление constitution.md из git
    if [[ -f ".specify/memory/constitution-backup.md" ]]; then
        if git ls-files --error-unmatch .specify/memory/constitution.md >/dev/null 2>&1; then
            echo "[INFO] Восстанавливаю constitution.md из git…"
            if ! git restore .specify/memory/constitution.md 2>/dev/null; then
                echo "[WARN] Не удалось восстановить из git. Восстанавливаю из резервной копии…"
                mv .specify/memory/constitution-backup.md .specify/memory/constitution.md
                echo "[OK] constitution.md восстановлен из резервной копии."
            else
                echo "[OK] constitution.md восстановлен из git."
                # Удаляем резервную копию, так как восстановление из git успешно
                rm -f .specify/memory/constitution-backup.md
            fi
        else
            echo "[INFO] constitution.md не отслеживается git. Восстанавливаю из резервной копии…"
            mv .specify/memory/constitution-backup.md .specify/memory/constitution.md
            echo "[OK] constitution.md восстановлен из резервной копии."
        fi
    fi
    
    # Восстановление шаблонов из git
    if [[ -d ".specify/templates-backup" ]]; then
        echo "[INFO] Восстанавливаю кастомные шаблоны из git…"
        # Пытаемся восстановить файлы из git
        TEMP_FLAG=$(mktemp)
        while IFS= read -r template_file; do
            # Получаем относительный путь от .specify/templates-backup
            rel_path="${template_file#.specify/templates-backup/}"
            git_file=".specify/templates/$rel_path"
            if git ls-files --error-unmatch "$git_file" >/dev/null 2>&1; then
                if git restore "$git_file" 2>/dev/null; then
                    echo "1" > "$TEMP_FLAG"
                fi
            fi
        done < <(find .specify/templates-backup -type f)
        
        # Проверяем, удалось ли восстановить хотя бы один файл из git
        if [[ -f "$TEMP_FLAG" ]] && [[ -s "$TEMP_FLAG" ]]; then
            echo "[OK] Шаблоны восстановлены из git."
            rm -f "$TEMP_FLAG"
            # Удаляем резервную копию, так как восстановление из git успешно
            rm -rf .specify/templates-backup
        else
            echo "[WARN] Не удалось восстановить шаблоны из git. Восстанавливаю из резервной копии…"
            rm -f "$TEMP_FLAG"
            rm -rf .specify/templates
            mv .specify/templates-backup .specify/templates
            echo "[OK] Шаблоны восстановлены из резервной копии."
        fi
    fi
    echo "[OK] Восстановление из git завершено."
else
    echo "[INFO] Git репозиторий не обнаружен. Восстанавливаю из резервных копий…"
    
    # Восстановление constitution.md из резервной копии
    if [[ -f ".specify/memory/constitution-backup.md" ]]; then
        echo "[INFO] Восстанавливаю constitution.md из резервной копии…"
        mv .specify/memory/constitution-backup.md .specify/memory/constitution.md
        echo "[OK] constitution.md восстановлен из резервной копии."
    fi
    
    # Восстановление шаблонов из резервной копии
    if [[ -d ".specify/templates-backup" ]]; then
        echo "[INFO] Восстанавливаю кастомные шаблоны из резервной копии…"
        rm -rf .specify/templates
        mv .specify/templates-backup .specify/templates
        echo "[OK] Шаблоны восстановлены из резервной копии."
    fi
fi

# -----------------------------
# 8. Проверка конфигурации
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
