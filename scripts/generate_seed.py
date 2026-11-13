"""
Synthetic data generator for database/init/02_seed.sql.

Reproducible: the same SEED always produces the same file, so the analytics
in the data analysis report can be recalculated by anyone.

Usage (from the repository root):
    python scripts/generate_seed.py                  # writes database/init/02_seed.sql
    python scripts/generate_seed.py -o other.sql     # writes somewhere else
"""

import argparse
import json
import random
import unicodedata
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path

SEED = 2026

# Bogotá has no daylight saving time, so a fixed -05:00 offset is exact
# (and avoids depending on the tzdata package on Windows).
BOGOTA = timezone(timedelta(hours=-5))

INCIDENT_COUNT = 150
# 12 full weeks, Monday to Sunday, fixed so the data does not change with the current date
PERIOD_START = date(2026, 8, 10)
PERIOD_END = date(2026, 11, 1)
WEEKEND_WEIGHT = 0.25          # support receives fewer requests on weekends
WEEKLY_GROWTH = 0.04           # +4 % per week: a mild upward trend to analyse
WORK_HOURS = (7, 18)           # incidents are created between 07:00 and 18:59
NO_ARTICLE_TOPIC_WEIGHT = 0.3  # topics without an article are rare (test 3)
PRIORITY_WEIGHTS = {"P1": 10, "P2": 25, "P3": 40, "P4": 25}

USER_COUNT = 10
EMAIL_DOMAIN = "empresa.com"
FIRST_NAMES = ["Laura", "Andrés", "Camila", "Julián", "Valentina", "Santiago",
               "Natalia", "Felipe", "Daniela", "Mateo", "Paula", "Sebastián"]
LAST_NAMES = ["Gómez", "Rodríguez", "Martínez", "López", "Hernández", "Ramírez",
              "Torres", "Castro", "Vargas", "Moreno", "Rojas", "Muñoz"]
AREAS = ["Finanzas", "Ventas", "Recursos Humanos", "Operaciones", "Logística",
         "Compras", "Marketing", "Tecnología"]

ROOT = Path(__file__).resolve().parent.parent
CATALOG_PATH = ROOT / "scripts" / "catalog.json"
DEFAULT_OUTPUT = ROOT / "database" / "init" / "02_seed.sql"


def sql_text(value):
    """Quotes a value as a SQL string literal (single quotes doubled)."""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def load_catalog():
    with CATALOG_PATH.open(encoding="utf-8") as f:
        return json.load(f)["categories"]


def categories_sql(categories):
    """Categories get explicit ids (1..5) so incidents can reference them directly."""
    lines = ["-- Categories", "INSERT INTO categories (id, name, description) VALUES"]
    rows = [
        f"    ({i}, {sql_text(c['name'])}, {sql_text(c['description'])})"
        for i, c in enumerate(categories, start=1)
    ]
    lines.append(",\n".join(rows) + ";")
    return lines


def ascii_slug(text):
    """'Muñoz' -> 'munoz': emails without accents, in lower case (as the user API stores them)."""
    plain = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("ascii")
    return plain.lower().replace(" ", "")


def generate_users(rng):
    """10 users with unique names; sampling without replacement avoids duplicate emails."""
    first_names = rng.sample(FIRST_NAMES, USER_COUNT)
    last_names = rng.sample(LAST_NAMES, USER_COUNT)
    users = []
    for i, (first, last) in enumerate(zip(first_names, last_names), start=1):
        users.append({
            "id": i,
            "name": f"{first} {last}",
            "email": f"{ascii_slug(first)}.{ascii_slug(last)}@{EMAIL_DOMAIN}",
            "area": rng.choice(AREAS),
        })
    return users


def users_sql(users):
    lines = ["-- Users", "INSERT INTO users (id, name, email, area) VALUES"]
    rows = [
        f"    ({u['id']}, {sql_text(u['name'])}, {sql_text(u['email'])}, {sql_text(u['area'])})"
        for u in users
    ]
    lines.append(",\n".join(rows) + ";")
    return lines


def day_weights():
    """Every day of the period with its relative weight: weekdays > weekends, later weeks > earlier."""
    days, weights = [], []
    day = PERIOD_START
    while day <= PERIOD_END:
        week = (day - PERIOD_START).days // 7
        weight = (1 + WEEKLY_GROWTH * week) * (WEEKEND_WEIGHT if day.weekday() >= 5 else 1)
        days.append(day)
        weights.append(weight)
        day += timedelta(days=1)
    return days, weights


def random_created_at(rng, day):
    hour = rng.randint(*WORK_HOURS)
    minute = rng.randint(0, 59)
    return datetime.combine(day, time(hour, minute), tzinfo=BOGOTA)


def pick_topic(rng, category):
    topics = category["topics"]
    weights = [1 if t["has_article"] else NO_ARTICLE_TOPIC_WEIGHT for t in topics]
    return rng.choices(topics, weights=weights)[0]


def generate_incidents(rng, categories, users):
    """150 open incidents spread over dates, categories, topics, users and priorities.
    Ids follow creation order, like a real system."""
    days, weights = day_weights()
    category_weights = [c["weight"] for c in categories]
    priorities = list(PRIORITY_WEIGHTS)
    priority_weights = list(PRIORITY_WEIGHTS.values())

    incidents = []
    for _ in range(INCIDENT_COUNT):
        category_index = rng.choices(range(len(categories)), weights=category_weights)[0]
        topic = pick_topic(rng, categories[category_index])
        incidents.append({
            "category_id": category_index + 1,
            "topic": topic["key"],
            "title": rng.choice(topic["incident_titles"]),
            "description": rng.choice(topic["descriptions"]),
            "user_id": rng.choice(users)["id"],
            "priority": rng.choices(priorities, weights=priority_weights)[0],
            "created_at": random_created_at(rng, rng.choices(days, weights=weights)[0]),
        })

    incidents.sort(key=lambda i: i["created_at"])
    for i, incident in enumerate(incidents, start=1):
        incident["id"] = i
    return incidents


def sql_timestamp(value):
    return sql_text(value.isoformat(sep=" "))


def incidents_sql(incidents):
    lines = [
        "-- Incidents",
        "INSERT INTO incidents (id, title, description, category_id, user_id, priority, status,"
        " created_at, updated_at) VALUES",
    ]
    rows = [
        f"    ({i['id']}, {sql_text(i['title'])}, {sql_text(i['description'])}, {i['category_id']},"
        f" {i['user_id']}, {sql_text(i['priority'])}, 'OPEN',"
        f" {sql_timestamp(i['created_at'])}, {sql_timestamp(i['created_at'])})"
        for i in incidents
    ]
    lines.append(",\n".join(rows) + ";")
    return lines


def reset_sequence_sql(table):
    """Explicit ids do not advance the sequence; move it past the max id."""
    return f"SELECT setval(pg_get_serial_sequence('{table}', 'id'), (SELECT max(id) FROM {table}));"


def build_seed(rng):
    categories = load_catalog()
    users = generate_users(rng)
    incidents = generate_incidents(rng, categories, users)
    sections = [
        categories_sql(categories),
        users_sql(users),
        incidents_sql(incidents),
        [reset_sequence_sql("categories"), reset_sequence_sql("users"), reset_sequence_sql("incidents")],
    ]
    return sections


def render(sections):
    header = [
        "-- =====================================================================",
        "-- 02_seed.sql — GENERATED by scripts/generate_seed.py. Do not edit by hand.",
        f"-- Seed: {SEED}. Regenerate with: python scripts/generate_seed.py",
        "-- =====================================================================",
    ]
    body = ["\n".join(section) for section in sections]
    return "\n".join(header) + "\n\n" + "\n\n".join(body) + "\n"


def main():
    parser = argparse.ArgumentParser(description="Generate the synthetic seed SQL.")
    parser.add_argument("-o", "--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    rng = random.Random(SEED)
    sql = render(build_seed(rng))
    args.output.write_text(sql, encoding="utf-8", newline="\n")
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
