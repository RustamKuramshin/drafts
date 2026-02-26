# Guidelines: `shell/relman.py`

Этот файл фиксирует проектные договорённости и практические инструкции для дальнейшей разработки `shell/relman.py`.

## 1) Структура и архитектурные ограничения

- **Основной функциональный код** CLI-инструмента должен оставаться **строго в одном файле**: `shell/relman.py`.
  - Нельзя «раскладывать» бизнес-логику по отдельным модулям/пакетам.
  - `relman.py` — **самодостаточный** CLI-скрипт, который можно запускать напрямую.
- Допускаются:
  - **временные** тестовые/диагностические файлы (подлежит удалению после проверки);
  - **вспомогательные утилиты** (минимальные и без вынесения основной логики). Сейчас пример такого файла: `shell/relman_config_utils.py`.
    - Важно: не превращать это в «второй слой» архитектуры; любые существенные изменения поведения/CLI должны вноситься в `relman.py`.

Практический смысл ограничения: все изменения CLI и логики релиз-менеджмента ищутся/ревьюятся/отлаживаются в одном месте, без прыжков по модулям.

## 2) Сборка/установка и запуск

### Python / окружение

Скрипт ожидает Python 3.12+ (в проекте встречается запуск из conda). Минимальный ориентир:

```bash
conda create -n relman python=3.12
conda activate relman

python -m pip install python-gitlab "typer[all]" jira rich requests pyyaml
```

`relman.py` проверяет наличие ключевых библиотек **на этапе import** и печатает понятные подсказки по установке (это влияет на тестирование: импорт без зависимостей не пройдёт).

### Конфигурация (`config.yaml`)

`relman.py` ищет `config.yaml` в следующем порядке:

1. `--config <path>`
2. рядом с `shell/relman.py` (например `shell/config.yaml`)
3. в текущем рабочем каталоге (`./config.yaml`)
4. `~/.relman/config.yaml`

См. пример структуры в `shell/config.yaml`:
- `defaults.gitlab` / `defaults.jira` / `defaults.commit_filter`
- `projects[]` с `id/name/repo_url` и `targets` (например `stage/prod` с парами веток `from/to`).

### Переменные окружения (типовые)

По умолчанию скрипт использует:

- `GITLAB_TOKEN`
- `JIRA_TOKEN` (и/или `JIRA_USER` для basic auth)
- `JIRA_BASE`
- `JIRA_KEY_RE`
- `USER_AGENT`

### Быстрый sanity check

Команды, которые не требуют доступа к GitLab/Jira и подходят для быстрой проверки окружения:

```bash
python3 shell/relman.py --help
python3 shell/relman.py get issues --help
```

## 3) Тестирование

### Как запускать тесты

В репозитории нет «официальной» Python-тестовой инфраструктуры, но для `relman.py` удобно использовать `unittest`/`pytest` на уровне smoke/юнит-тестов.

Минимальные команды:

```bash
# Проверка синтаксиса без выполнения import-time логики зависимостей
python3 -c "import py_compile; py_compile.compile('shell/relman.py', doraise=True)"

# Smoke-тест через запуск CLI в subprocess (требует установленных зависимостей)
python3 -m unittest -v path/to/test_file.py
```

### Рекомендации по добавлению новых тестов

- Предпочитайте тестировать **чистые функции** из `relman.py` (например парсинг URL/regex/фильтрацию коммитов), т.к. это:
  - не требует реальных GitLab/Jira;
  - не зависит от токенов/сети;
  - быстро и стабильно.
- Для CLI-поведения используйте тесты через `subprocess` (smoke), но:
  - не вызывайте команды, которые ходят в сеть, если тест должен быть стабильным;
  - фиксируйте `cwd` (важно для поиска `config.yaml`).

### Демонстрационный тест (smoke)

Ниже пример простого smoke-теста, который проверяет, что `--help` работает и возвращает код 0.

```python
import pathlib
import subprocess
import sys
import unittest


class RelmanSmokeTest(unittest.TestCase):
    def test_help_command_runs(self) -> None:
        repo_root = pathlib.Path(__file__).resolve().parents[1]
        relman_py = repo_root / "shell" / "relman.py"

        proc = subprocess.run(
            [sys.executable, str(relman_py), "--help"],
            cwd=str(repo_root),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        self.assertEqual(proc.returncode, 0)
        self.assertIn("Usage: relman.py", proc.stdout)


if __name__ == "__main__":
    unittest.main(verbosity=2)
```

Запуск:

```bash
python3 path/to/test_file.py
```

## 4) Доп. сведения для разработки/отладки

- `relman.py` — Typer CLI. Обратите внимание: help выводится через callback-опцию `--help/-h` и `ctx.get_help()`; конфиг грузится только для «не-help» команд.
- `_build_examples_epilog()` специально форматирует epilog под особенности Typer/Rich (важны пустые строки между «строками» примеров).
- При добавлении опций/команд сохраняйте текущий стиль:
  - явные типы, `Optional[...]`, `List[str]` и т.п.;
  - единый подход к сообщениям об ошибках (через rich-консоли и `typer.Exit`/`typer.BadParameter`).
- Избегайте скрытых сетевых вызовов при импорте модуля: импорт должен оставаться предсказуемым (сейчас сетевые вызовы выполняются только внутри команд).
