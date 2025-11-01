#!/bin/bash
# Скрипт для создания тега и релиза на GitHub
# Использование: ./create-release.sh [--force] [путь_к_файлу_информации]
#   --force: автоматически удалять существующие теги и релизы без подтверждения
# По умолчанию: release-info.txt

set -e  # Остановка при ошибке

# Обработка аргументов
FORCE=false
RELEASE_INFO_FILE="release-info.txt"

for arg in "$@"; do
    case $arg in
        --force|-f)
            FORCE=true
            shift
            ;;
        *)
            RELEASE_INFO_FILE="$arg"
            ;;
    esac
done

# Если --force не указан, а аргумент выглядит как файл
if [ "$FORCE" = false ] && [ -f "$1" ] && [ "$1" != "release-info.txt" ]; then
    RELEASE_INFO_FILE="$1"
fi

# Проверка наличия файла
if [ ! -f "$RELEASE_INFO_FILE" ]; then
    echo "❌ Ошибка: Файл '$RELEASE_INFO_FILE' не найден!"
    exit 1
fi

echo "📖 Чтение информации о релизе из '$RELEASE_INFO_FILE'..."

# Чтение первой строки - имя тега
TAG_NAME=$(sed -n '1p' "$RELEASE_INFO_FILE" | tr -d '\r' | xargs)
if [ -z "$TAG_NAME" ]; then
    echo "❌ Ошибка: Имя тега (первая строка) не найдено!"
    exit 1
fi

# Чтение второй строки - имя релиза
RELEASE_NAME=$(sed -n '2p' "$RELEASE_INFO_FILE" | tr -d '\r' | xargs)
if [ -z "$RELEASE_NAME" ]; then
    echo "❌ Ошибка: Имя релиза (вторая строка) не найдено!"
    exit 1
fi

# Чтение остальных строк (начиная с 3-й) - описание релиза
RELEASE_BODY=$(sed -n '3,$p' "$RELEASE_INFO_FILE")

if [ -z "$RELEASE_BODY" ]; then
    echo "⚠️  Предупреждение: Описание релиза пустое!"
fi

echo "✓ Имя тега: $TAG_NAME"
echo "✓ Имя релиза: $RELEASE_NAME"
echo "✓ Длина описания: ${#RELEASE_BODY} символов"

# Проверка наличия gh CLI
if ! command -v gh &> /dev/null; then
    echo "❌ Ошибка: GitHub CLI (gh) не установлен!"
    echo "   Установите его: https://cli.github.com/"
    exit 1
fi

# Проверка аутентификации GitHub
if ! gh auth status &> /dev/null; then
    echo "❌ Ошибка: Не выполнена аутентификация в GitHub CLI!"
    echo "   Выполните: gh auth login"
    exit 1
fi

echo ""
echo "🔍 Проверка текущей ветки..."
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "   Текущая ветка: $CURRENT_BRANCH"

# Проверка, что мы на master ветке (или main)
if [ "$CURRENT_BRANCH" != "master" ] && [ "$CURRENT_BRANCH" != "main" ]; then
    echo "⚠️  Предупреждение: Вы не на ветке master/main!"
    read -p "   Продолжить? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Проверка, существует ли тег
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "⚠️  Тег '$TAG_NAME' уже существует локально!"
    if [ "$FORCE" = true ]; then
        echo "🗑️  Удаление локального тега (--force)..."
        git tag -d "$TAG_NAME" || true
    else
        read -p "   Удалить и пересоздать? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "🗑️  Удаление локального тега..."
            git tag -d "$TAG_NAME" || true
        else
            echo "❌ Прервано пользователем"
            exit 1
        fi
    fi
fi

# Проверка, существует ли тег на удаленном репозитории
if git ls-remote --tags origin | grep -q "refs/tags/$TAG_NAME"; then
    echo "⚠️  Тег '$TAG_NAME' уже существует на удаленном репозитории!"
    if [ "$FORCE" = true ]; then
        echo "🗑️  Удаление удаленного тега (--force)..."
        git push origin :refs/tags/"$TAG_NAME" || true
    else
        read -p "   Удалить и пересоздать? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "🗑️  Удаление удаленного тега..."
            git push origin :refs/tags/"$TAG_NAME" || true
        else
            echo "❌ Прервано пользователем"
            exit 1
        fi
    fi
fi

# Проверка, существует ли релиз
if gh release view "$TAG_NAME" &> /dev/null; then
    echo "⚠️  Релиз '$TAG_NAME' уже существует!"
    if [ "$FORCE" = true ]; then
        echo "🗑️  Удаление существующего релиза (--force)..."
        gh release delete "$TAG_NAME" --yes || true
    else
        read -p "   Удалить и пересоздать? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "🗑️  Удаление существующего релиза..."
            gh release delete "$TAG_NAME" --yes || true
        else
            echo "❌ Прервано пользователем"
            exit 1
        fi
    fi
fi

echo ""
echo "📝 Создание тега '$TAG_NAME' на текущем коммите..."
git tag -a "$TAG_NAME" -m "Release $RELEASE_NAME"

echo "📤 Отправка тега на GitHub..."
git push origin "$TAG_NAME"

echo ""
echo "🚀 Создание релиза на GitHub..."

# Создание временного файла для тела релиза
TEMP_BODY_FILE=$(mktemp)
echo "$RELEASE_BODY" > "$TEMP_BODY_FILE"

# Создание релиза
gh release create "$TAG_NAME" \
    --title "$RELEASE_NAME" \
    --notes-file "$TEMP_BODY_FILE" \
    --target "$CURRENT_BRANCH"

# Удаление временного файла
rm -f "$TEMP_BODY_FILE"

echo ""
echo "✅ Готово! Тег и релиз успешно созданы:"
echo "   Тег: $TAG_NAME"
echo "   Релиз: $RELEASE_NAME"
echo "   Ссылка: https://github.com/$(gh repo view --json owner,name -q '.owner.login + "/" + .name')/releases/tag/$TAG_NAME"

