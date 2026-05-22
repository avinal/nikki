# Task Format

Nikki extracts tasks from standard markdown checkboxes in your memos. Any line matching `- [ ]` or `- [x]` is parsed as a task. Metadata is extracted inline — no special syntax beyond what you'd naturally write.

## Basic Format

```
- [ ] task description [due-date] [due-time] [!reminder] [p#] [#list]
```

All metadata fields are optional. Order doesn't matter. The task text is everything that remains after metadata is stripped.

## Due Dates

ISO format or natural language.

| Input | Meaning |
|-------|---------|
| `2026-06-15` | June 15, 2026 |
| `today` | Today's date |
| `tomorrow` | Tomorrow |
| `yesterday` | Yesterday |

```
- [ ] submit report 2026-06-15
- [ ] buy milk today
```

## Due Times

12-hour or 24-hour format.

| Input | Meaning |
|-------|---------|
| `5pm` | 17:00 |
| `10:30am` | 10:30 |
| `14:00` | 14:00 |
| `9am` | 09:00 |

```
- [ ] standup today 9am
- [ ] deploy 2026-06-15 14:00
```

If a time is set without a date, today is assumed (with a warning).

## Reminders

How far before the due date/time to send a notification. Prefixed with `!`.

| Input | Meaning |
|-------|---------|
| `!30min` | 30 minutes before |
| `!1hr` | 1 hour before |
| `!2day` | 2 days before |
| `!1week` | 1 week before |

```
- [ ] dentist tomorrow 3pm !1hr
- [ ] taxes 2026-04-15 !1week
```

A reminder without a due date or time is flagged as an error — there's nothing to count back from.

## Priority

Three levels: p1 (high), p2 (medium), p3 (low).

| Level | Notification behavior |
|-------|----------------------|
| `p1` | Alarm sound, vibration, bypasses DND |
| `p2` | Notification sound, heads-up |
| `p3` | Notification sound, subtle |

```
- [ ] server is down p1
- [ ] update docs p3
```

Tasks without a priority are treated as unclassified (silent notification).

## Lists / Tags

Hashtags assign tasks to lists. A task can belong to multiple lists.

```
- [ ] buy groceries #shopping
- [ ] fix login bug #backend #urgent
```

If a task has no `#tag`, it inherits the memo-level tags (tags set at the top of the memo). Task-level tags always take priority over memo-level tags.

## Complete Examples

```markdown
# Sprint 14

- [ ] fix auth timeout 2026-06-10 p1 #backend
- [ ] update onboarding copy tomorrow 5pm !30min p2 #frontend
- [ ] write migration tests 2026-06-12 #backend #testing
- [x] deploy staging
- [ ] review PR today 3pm !15min p2
- [ ] buy coffee #personal
```

This produces 6 tasks. The completed one (`deploy staging`) is tracked but excluded from validation. Tasks can be grouped by due date, list, priority, source memo, or completion status.

## Validation

The parser doctor checks incomplete tasks for issues and shows inline warnings/errors in the task view.

### Errors

| Issue | Example | Message |
|-------|---------|---------|
| Invalid date | `2026-13-45` | invalid date, use YYYY-MM-DD format |
| Invalid priority | `p5` | invalid priority, only p1, p2, p3 are supported |
| Reminder without due | `!30min` (no date/time) | reminder has no due date or time to count back from |
| Invalid reminder unit | `!5blah` | invalid reminder unit, use min, hr, day, or week |

### Warnings

| Issue | Example | Message |
|-------|---------|---------|
| Time without date | `5pm` (no date) | time without date, using today |
| Past date | `2020-01-01` | date is in the past |
| Multiple priorities | `p1 p2` | multiple priorities, using first (p1) |
| Multiple dates | `2026-06-01 2026-07-01` | multiple dates found, using first |
| Multiple reminders | `!30min !1hr` | multiple reminders, using first |
| Typo | `tomorow` | did you mean "tomorrow"? |

### Typo Detection

Common misspellings are caught and suggestions offered:

- `tday`, `todya`, `toaday`, `toady` → today
- `tmrw`, `tomorow`, `tommorow` → tomorrow
- `yestrday`, `ysterday` → yesterday
- Day name typos: `munday`, `tusday`, `wendsday`, `thurday`, `firday`, `saterday`, `sundie`, etc.

## Notification Behavior

When a task has both a due date/time and a reminder, two notifications may fire:

1. **Reminder notification** — fires `!duration` before the due time
2. **Due notification** — fires at the due time itself

If a task has a date but no time, the notification fires at the default notify time (configurable in settings, default 8pm).

If a task has neither date nor time, no notification is scheduled.
