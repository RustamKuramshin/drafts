## Installing Legacy Zynaptiq Plug-Ins on Modern macOS

Hi everyone,

There is a known issue when trying to install older Zynaptiq plug-ins (like UNVEIL, UNFILTER, etc.) on recent versions of macOS.  
When you open the `.pkg` installer, you may see an error like:

> “This package is incompatible with this version of macOS”

If you still need to install one of these legacy packages, you can use a small helper script that bypasses the outdated installer and performs a safe manual installation.

---

## Download

Download the script here: 
https://gist.github.com/RustamKuramshin/2e15c52a044515a4712b3e46978ce6ce

---

## Setup

1. Open Terminal
    
2. Navigate to the folder where you downloaded the script
    
3. Make it executable:
    

```bash
chmod +x install_legacy_zynaptiq_pkg.sh
```

You can keep the script anywhere (e.g. your home directory or a tools folder).

---

## Usage

Run the script by passing the path to the `.pkg` file:

```bash
./install_legacy_zynaptiq_pkg.sh "/path/to/Zynaptiq Plug-In.pkg"
```

💡 Tip: you can drag & drop the `.pkg` file into Terminal to auto-fill the path.

---

## Help

To see basic usage info:

```bash
sh install_legacy_zynaptiq_pkg.sh
```

---

## What the script does (for transparency)

The script does **not modify your system in unsafe ways**. It simply reproduces what the original installer would do:

- Extracts the contents of the `.pkg` file
    
- Runs the package’s `preflight` script (if present)
    
- Copies only the relevant plug-in files into:
    
    - `/Applications`
        
    - `/Library/Audio/Plug-Ins`
        
    - `/Library/Application Support`
        
- Applies correct permissions **only to installed plug-in files**
    
- Removes macOS quarantine flags (so the plug-ins can run)
    
- Runs `postflight` (if present)
    

It does **not**:

- Touch unrelated system files
    
- Modify anything outside the installed plug-in components
    
- Install background services or daemons
    

All actions are logged, so you can inspect what happened if needed.

---

## Notes

- You will be prompted for your password (required to install system-level plug-ins)
    
- This works best with older Zynaptiq packages that still contain valid binaries
    
- Very old plug-ins (e.g. 32-bit) may still not work due to macOS limitations

---

Установка устаревших плагинов Zynaptiq на современных версиях macOS

Всем привет,

Существует известная проблема при попытке установить старые плагины Zynaptiq (такие как UNVEIL, UNFILTER и другие) на новых версиях macOS.  
При открытии установщика .pkg вы можете увидеть ошибку:

«Этот пакет несовместим с данной версией macOS»

Если вам всё же необходимо установить один из таких устаревших пакетов, можно воспользоваться небольшим вспомогательным скриптом, который обходит устаревший установщик и выполняет безопасную ручную установку.

Загрузка

Скачать скрипт можно здесь:  
https://gist.github.com/RustamKuramshin/2e15c52a044515a4712b3e46978ce6ce

Настройка

1. Откройте Terminal
    
2. Перейдите в папку, куда вы скачали скрипт
    
3. Сделайте его исполняемым:
    

chmod +x install_legacy_zynaptiq_pkg.sh

Скрипт можно хранить в любом месте (например, в домашней директории или папке с инструментами).

Использование

Запустите скрипт, передав путь к файлу .pkg:

./install_legacy_zynaptiq_pkg.sh "/path/to/Zynaptiq Plug-In.pkg"

Подсказка: вы можете перетащить файл .pkg в окно Terminal, чтобы путь подставился автоматически.

Справка

Чтобы увидеть базовую информацию по использованию:

sh install_legacy_zynaptiq_pkg.sh

Что делает скрипт (для прозрачности)

Скрипт не вносит небезопасных изменений в систему. Он просто воспроизводит действия оригинального установщика:

- Извлекает содержимое файла .pkg
    
- Запускает скрипт preflight (если он присутствует)
    
- Копирует только необходимые файлы плагинов в:  
    /Applications  
    /Library/Audio/Plug-Ins  
    /Library/Application Support
    
- Применяет корректные права доступа только к установленным файлам плагинов
    
- Удаляет флаг карантина macOS (чтобы плагины могли запускаться)
    
- Запускает postflight (если он присутствует)
    

Он не:

- Затрагивает посторонние системные файлы
    
- Изменяет что-либо вне компонентов устанавливаемых плагинов
    
- Устанавливает фоновые сервисы или демоны
    

Все действия записываются в лог, поэтому при необходимости вы можете проверить, что именно было выполнено.

Примечания

- Вам будет предложено ввести пароль (это необходимо для установки системных плагинов)
    
- Лучше всего это работает со старыми пакетами Zynaptiq, которые всё ещё содержат рабочие бинарные файлы
    
- Очень старые плагины (например, 32-битные) могут всё равно не работать из-за ограничений macOS