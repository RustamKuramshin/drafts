#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import csv
import json
import re
from typing import Tuple, Optional, Dict

LEAK_PHRASE = "Connection leak detection triggered"


def extract_inner_line_json(text: str) -> Optional[str]:
    """
    Если строка выглядит как JSON-объект, содержащий поле "line": "<escaped-json>",
    извлечь и вернуть декодированное значение этого поля (как обычный текст). В противном случае вернуть None.
    Предназначено для экспортов в виде JSON-массива, где каждый объект имеет строковое поле "line" с экранированными символами.
    """
    if '"line"' not in text:
        return None

    m = re.search(r'"line"\s*:\s*"((?:[^"\\]|\\.)*)"', text)
    if not m:
        return None

    encoded = m.group(1)
    try:
        plain = json.loads(f'"{encoded}"')
        return plain
    except Exception:
        return None


def slice_first_json_object(s: str) -> Optional[str]:
    """
    Если строка может содержать префикс (время и т. п.), после которого идёт JSON-объект,
    вернуть подстроку первого сбалансированного JSON-объекта {...}. Лучшее приближение.
    """
    start = s.find('{')
    if start == -1:
        return None
    depth = 0
    for i in range(start, len(s)):
        ch = s[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return s[start:i+1]
    return None


def iter_potential_entries(file_obj):
    """
    Итерироваться по файлу логов, возвращая кандидаты текстов лог‑записей,
    которые могут содержать сообщение об утечке и (если есть) последующие строки stack trace.

    Поддерживаются два распространённых формата:
      1) Обычные/смешанные текстовые логи, где перед JSON‑объектом идёт метка времени; строки стека могут продолжаться на следующих строках.
      2) Экспорт JSON‑массива, где каждый элемент содержит поле "line": "<escaped-JSON>", включающее всю запись.
    """
    while True:
        line = file_obj.readline()
        if not line:
            break

        # Формат (2): экспорт JSON-массива с полем "line": "<escaped>"
        inner = extract_inner_line_json(line)
        if inner:
            yield inner
            continue

        # Формат (1): обычный текст / смешанные логи с меткой времени, за которой следует JSON
        if LEAK_PHRASE in line:
            collected = [line.rstrip("\n")]

            # Читаем вперёд, чтобы захватить непрерывные строки стека (эвристика)
            while True:
                pos = file_obj.tell()
                nxt = file_obj.readline()
                if not nxt:
                    break
                stripped = nxt.rstrip("\n")

                if not stripped:
                    break

                # Следующая запись, вероятно, начинается с метки времени
                if re.match(r'^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}[.,]\d{3}', stripped):
                    file_obj.seek(pos)
                    break

                # Продолжение stack trace (реальный табулятор \t, а не литерал обратной косой черты и буквы t)
                if stripped.startswith("\tat ") or stripped.startswith("\t... ") or stripped.startswith("at ") or stripped.startswith("... "):
                    collected.append(stripped)
                    continue

                # Нераспознанная строка — откатываемся для внешнего цикла
                file_obj.seek(pos)
                break

            yield "\n".join(collected)
            continue


def extract_first_app_frame(entry_text: str) -> Optional[Tuple[str, str, int]]:
    """
    По полному тексту записи (содержит сообщение об утечке и stack trace)
    найти первый кадр стека, принадлежащий пакету приложения (содержит 'kazanexpress')
    и имеющий конкретный Java‑файл с номером строки, например (SellerSkuController.java:51).

    Возвращает (package, class, line) или None, если не найдено.
    """
    stack_part = entry_text

    # Если удаётся распарсить JSON-объект, отдаём предпочтение полю 'stack_trace'
    obj_text = None
    if '"stack_trace"' in entry_text and '{' in entry_text:
        obj_text = slice_first_json_object(entry_text)

    if obj_text:
        try:
            obj = json.loads(obj_text)
            if isinstance(obj, dict) and isinstance(obj.get('stack_trace'), str):
                stack_part = obj['stack_trace']
        except Exception:
            # Оставляем stack_part как есть
            pass

    # Нормализуем переносы строк
    stack_part = stack_part.replace("\r\n", "\n").replace("\r", "\n").replace("\\n", "\n")

    lines = stack_part.splitlines()

    frame_re = re.compile(
        r'^\s*at\s+([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)\.([A-Za-z0-9_$]+)\.[^(]*\(([^():]+):(\d+)\)'
    )

    for raw in lines:
        m = frame_re.match(raw)
        if not m:
            continue
        pkg_path = m.group(1)   # например, com.kazanexpress.core.controller.seller
        cls_name = m.group(2)   # например, SellerSkuController
        line_no = m.group(4)    # например, 51

        if "kazanexpress" not in pkg_path:
            continue

        try:
            line_int = int(line_no)
        except ValueError:
            continue

        package_name = pkg_path
        return (package_name, cls_name, line_int)

    return None


def analyze_file(input_path: str) -> Dict[Tuple[str, str, int], int]:
    """
    Потоково прочитать файл и посчитать вхождения, сгруппировав по (package, class, line).
    """
    counts: Dict[Tuple[str, str, int], int] = {}

    with open(input_path, "r", encoding="utf-8", errors="ignore") as f:
        for entry in iter_potential_entries(f):
            if LEAK_PHRASE not in entry:
                continue

            triple = extract_first_app_frame(entry)
            if not triple:
                continue

            counts[triple] = counts.get(triple, 0) + 1

    return counts


def write_csv(counts: Dict[Tuple[str, str, int], int], output_path: str) -> None:
    """
    Записать агрегированные результаты в CSV с разделителем ';'.
    Колонки:
      1) package
      2) class
      3) line
      4) Кол-во таких stack trace
    """
    rows = sorted(
        [(pkg, cls, line, cnt) for (pkg, cls, line), cnt in counts.items()],
        key=lambda x: (-x[3], x[0], x[1], x[2])
    )

    with open(output_path, "w", newline="", encoding="utf-8") as csvfile:
        writer = csv.writer(csvfile, delimiter=';')
        writer.writerow(["package", "class", "line", "count"])
        for pkg, cls, line, cnt in rows:
            writer.writerow([pkg, cls, line, cnt])


def main():
    parser = argparse.ArgumentParser(
        description="Парсинг логов Spring Boot для поиска утечек коннекшенов Hikari"
    )
    parser.add_argument("--input", required=True, help="Относительный путь к файлу с логами")
    parser.add_argument(
        "--output",
        required=False,
        default="hikari_leaks.csv",
        help='Имя выходного CSV-файла (по умолчанию: "hikari_leaks.csv")',
    )

    args = parser.parse_args()
    counts = analyze_file(args.input)
    write_csv(counts, args.output)


if __name__ == "__main__":
    main()
