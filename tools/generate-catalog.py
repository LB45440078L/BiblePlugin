#!/usr/bin/env python3
"""Regenerates bibleplugin-source/src/main/resources/catalog.json.

Source of truth: https://api.getbible.net/v2/translations.json (free, no key, public-domain or
openly licensed editions). For every English edition the full text is downloaded once and its
reading difficulty is measured with the same formulas as
org.bibleplugin.core.readability.ReadabilityAnalyzer, so the numbers shown in-game
match what the plugin would compute itself.

Usage:
    python tools/generate-catalog.py [--cache-dir DIR] [--output FILE]

Run it with the -I flag if you keep the cache in an untrusted location.
"""

import argparse
import datetime
import json
import os
import re
import sys
import urllib.request

TRANSLATIONS_URL = "https://api.getbible.net/v2/translations.json"
BOOK_URL = "https://api.getbible.net/v2/{abbr}.json"

MARKUP = re.compile(r"<[^>]+>|\{[^}]*\}|\[[^\]]*\]")
WORD = re.compile(r"[A-Za-z][A-Za-z'’-]*")
SENTENCE_END = re.compile(r"[.!?;]+(?=\s|$)")
VOWEL_GROUP = re.compile(r"[aeiouy]+")


def fetch(url, cache_path):
    if cache_path and os.path.exists(cache_path):
        with open(cache_path, "rb") as handle:
            return handle.read()
    with urllib.request.urlopen(url, timeout=300) as response:
        data = response.read()
    if cache_path:
        os.makedirs(os.path.dirname(cache_path), exist_ok=True)
        with open(cache_path, "wb") as handle:
            handle.write(data)
    return data


def count_syllables(word):
    letters = re.sub(r"[^a-z]", "", word.lower())
    if not letters:
        return 0
    count = len(VOWEL_GROUP.findall(letters))
    if letters.endswith("e") and not letters.endswith(("le", "ee")) and count > 1:
        count -= 1
    return max(1, count)


def difficulty_for(grade):
    if grade < 6.6:
        return "EASY"
    if grade < 7.5:
        return "MODERATE"
    if grade < 8.5:
        return "ADVANCED"
    return "SCHOLARLY"


def readability(bible_json):
    words = syllables = sentences = 0
    for book in bible_json["books"]:
        for chapter in book["chapters"]:
            for verse in chapter["verses"]:
                text = MARKUP.sub(" ", verse["text"])
                tokens = WORD.findall(text)
                if not tokens:
                    continue
                words += len(tokens)
                syllables += sum(count_syllables(token) for token in tokens)
                sentences += max(1, len(SENTENCE_END.findall(text)))
    if words == 0:
        return None
    per_sentence = words / sentences
    per_word = syllables / words
    ease = 206.835 - 1.015 * per_sentence - 84.6 * per_word
    grade = 0.39 * per_sentence + 11.8 * per_word - 15.59
    return {
        "readingEase": round(ease, 1),
        "gradeLevel": round(grade, 1),
        "difficulty": difficulty_for(grade),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--cache-dir", default=os.path.join(os.path.dirname(__file__), ".cache"))
    parser.add_argument("--output", default=os.path.join(os.path.dirname(__file__), "..",
                        "bibleplugin-source", "src", "main", "resources", "catalog.json"))
    args = parser.parse_args()

    translations = json.loads(fetch(TRANSLATIONS_URL, os.path.join(args.cache_dir, "translations.json")))
    entries = []
    for key, meta in sorted(translations.items()):
        language_code = (meta.get("lang") or "und").strip() or "und"
        language = (meta.get("language") or "").strip() or language_code
        name = (meta.get("translation") or "").strip() or key.upper()
        license_text = (meta.get("distribution_license") or "").strip() or "Unknown"
        year = (meta.get("distribution_version_date") or "")[:4]
        measured = None
        if language_code == "en":
            cache_path = os.path.join(args.cache_dir, key + ".json")
            print("measuring English edition", key, file=sys.stderr)
            full = json.loads(fetch(BOOK_URL.format(abbr=key), cache_path).decode("utf-8-sig"))
            measured = readability(full)
        entries.append({
            "id": key,
            "name": name,
            "languageCode": language_code,
            "language": language,
            "license": license_text,
            "year": year,
            "url": meta.get("url") or BOOK_URL.format(abbr=key),
            "readability": measured,
        })

    output = {
        "generatedFrom": TRANSLATIONS_URL,
        "generatedAt": datetime.date.today().isoformat(),
        "entries": entries,
    }
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    with open(args.output, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(output, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"wrote {len(entries)} entries to {os.path.abspath(args.output)}", file=sys.stderr)


if __name__ == "__main__":
    main()
