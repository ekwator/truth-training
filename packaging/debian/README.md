# Debian packaging for truth-core-service.deb

## Состав пакета:
- /usr/local/bin/truth_core (бинарник, предоставьте заранее)
- /etc/truth-core/config.yml (пример конфигурации)
- /etc/systemd/system/truth-core.service (systemd unit)
- system user: truthd (без login, без home)
- postinst/prerm/postrm скрипты (создание пользователя, enable/start/stop/purge)

## Установка:
```
sudo dpkg -i truth-core-service.deb
```

## Управление сервисом:
```
sudo systemctl status truth-core.service
sudo systemctl restart truth-core.service
sudo systemctl stop truth-core.service
```

## Примечания
- Пользователь truthd создаётся без home и shell (безопасный сервисный юзер).
- Перезапуск/автостарт настроены через systemd.
- Ограничение: никакой модификации/логирования identities или acknowledgment — только автостарт, перезапуск, чистое event propagation.
- Для удаления с очисткой: sudo dpkg --purge truth-core-service
