"""Read FTB's newline-delimited SNBT, retaining object spans for surgical edits.

No game classes are initialized. Native FTB SNBT validation is run separately.
"""
import argparse
import collections
import csv
import json
from pathlib import Path
import re


class Parser:
    def __init__(self, text):
        self.text, self.pos, self.spans = text, 0, {}

    def skip(self):
        while self.pos < len(self.text):
            if self.text[self.pos].isspace() or self.text[self.pos] == ',':
                self.pos += 1
            elif self.text.startswith('//', self.pos) or self.text[self.pos] == '#':
                end = self.text.find('\n', self.pos)
                self.pos = len(self.text) if end < 0 else end + 1
            else:
                break

    def string(self):
        start = self.pos
        quote = self.text[self.pos]
        self.pos += 1
        result = ''
        while self.pos < len(self.text):
            c = self.text[self.pos]
            self.pos += 1
            if c == quote:
                return result
            if c == '\\':
                c = self.text[self.pos]
                self.pos += 1
                # SNBT only escapes the delimiter/backslash; JSON NBT stays a string.
                if c not in (quote, '\\'):
                    result += '\\'
            result += c
        raise ValueError(f'Unterminated string at {start}')

    def value(self):
        self.skip()
        start = self.pos
        c = self.text[self.pos]
        if c in ('"', "'"):
            return self.string()
        if c == '{':
            result = {}
            self.pos += 1
            while True:
                self.skip()
                if self.text[self.pos] == '}':
                    self.pos += 1
                    self.spans[id(result)] = (start, self.pos)
                    return result
                if self.text[self.pos] in ('"', "'"):
                    key = self.string()
                else:
                    match = re.match(r'[^\s:{}\[\],]+', self.text[self.pos:])
                    if not match:
                        raise ValueError(f'Invalid key at {self.pos}')
                    key = match.group()
                    self.pos += len(key)
                self.skip()
                assert self.text[self.pos] == ':', (key, self.pos)
                self.pos += 1
                assert key not in result, f'Duplicate compound key {key}'
                result[key] = self.value()
        if c == '[':
            self.pos += 1
            if re.match(r'[BIL];', self.text[self.pos:], re.I):
                self.pos += 2
            result = []
            while True:
                self.skip()
                if self.text[self.pos] == ']':
                    self.pos += 1
                    self.spans[id(result)] = (start, self.pos)
                    return result
                result.append(self.value())
        match = re.match(r'[^\s,}\]]+', self.text[self.pos:])
        if not match:
            raise ValueError(f'Invalid value at {self.pos}')
        token = match.group()
        self.pos += len(token)
        if token in ('true', 'false'):
            return token == 'true'
        if re.fullmatch(r'[+-]?\d+[bBsSlL]?', token):
            return int(token.rstrip('bBsSlL'))
        if re.fullmatch(r'[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?[fFdD]?', token):
            return float(token.rstrip('fFdD'))
        return token

    def parse(self):
        result = self.value()
        self.skip()
        assert self.pos == len(self.text), f'Trailing data at {self.pos}'
        return result


def load(path):
    parser = Parser(path.read_text(encoding='utf-8-sig'))
    return parser.parse(), parser


def walk(value, path=()):
    if isinstance(value, dict):
        yield path, value
        for key, child in value.items():
            yield from walk(child, path + (str(key),))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk(child, path + (str(index),))


def audit(quests_dir):
    rows, chapters, types, unusual = [], [], collections.Counter(), []
    for file in sorted((quests_dir / 'chapters').glob('*.snbt')):
        chapter, _ = load(file)
        summary = dict(file=file.name, title=chapter.get('title', file.stem), quests=len(chapter.get('quests', [])), fixed=0, repeatable=0, costs=0)
        for quest in chapter.get('quests', []):
            repeat = bool(quest.get('can_repeat', False))
            row = dict(chapter=summary['title'], chapter_file=file.name, quest_id=quest['id'], title=quest.get('title', ''), repeatable=repeat, reward=0, costs=0)
            for path, reward in walk(quest.get('rewards', [])):
                kind = reward.get('type', 'item')
                types[kind] += 1
                if kind == 'ftbmoney:money':
                    if len(path) != 1:
                        unusual.append(dict(reason='nested money reward', quest=quest['id'], path=path, reward=reward))
                    else:
                        row['reward'] += int(reward.get('ftb_money', 0))
                if kind in ('command', 'choice', 'random', 'loot'):
                    unusual.append(dict(reason='review reward', quest=quest['id'], reward=reward))
            for task in quest.get('tasks', []):
                if task.get('type') == 'ftbmoney:money':
                    row['costs'] += int(task.get('value', 0))
            summary['repeatable' if repeat else 'fixed'] += row['reward']
            summary['costs'] += row['costs']
            if row['reward'] or row['costs'] or repeat:
                rows.append(row)
        chapters.append(summary)
    tables = []
    for file in sorted((quests_dir/'reward_tables').glob('*.snbt')):
        table, _ = load(file)
        for path, reward in walk(table.get('rewards', [])):
            if reward.get('type') in ('ftbmoney:money', 'command'):
                tables.append(dict(file=file.name, path=path, reward=reward))
    return dict(chapters=chapters, rows=rows, reward_types=dict(types), review=unusual, money_in_tables=tables,
                fixed=sum(c['fixed'] for c in chapters), repeatable_per_round=sum(c['repeatable'] for c in chapters),
                task_money_costs=sum(c['costs'] for c in chapters))


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('quests_dir', type=Path)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    report = audit(args.quests_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k not in ('rows','review')}, ensure_ascii=False, indent=2))
