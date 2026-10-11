#!/usr/bin/env python3
"""Compare a matched native frame review without asserting portable speed thresholds."""
import json
import sys
from pathlib import Path

directory = Path(sys.argv[1])
baseline = json.loads((directory / 'baseline.json').read_text())
optimized = json.loads((directory / 'optimized.json').read_text())
expected = {'tabs-normal', 'tabs-double-text', 'requests-scroll', 'tartarus-scroll', 'day-complete', 'idle-today'}
for data, variant in ((baseline, 'baseline'), (optimized, 'optimized')):
    assert data['variant'] == variant
    assert data['sdk'] == 34 and data['motionScale'] == 1
    assert data['debuggable'] and not data['coldStartupMeasured']
    assert {row['scenario'] for row in data['scenarios']} == expected
    for row in data['scenarios']:
        assert row['frames'] >= 0 and row['droppedReports'] >= 0
        assert 0 <= row['overDeadline'] <= row['frames']
        if not row['scenario'].startswith('idle'):
            assert row['frames'] > 0 and row['p95Ms'] is not None
assert baseline['model'] == optimized['model']
assert abs(baseline['refreshRateHz'] - optimized['refreshRateHz']) < 0.1

before = {row['scenario']: row for row in baseline['scenarios']}
after = {row['scenario']: row for row in optimized['scenarios']}
def number(value):
    return '—' if value is None else f'{value:.2f}'

lines = [
    '# P3R frame review', '',
    f"Baseline: `{baseline['source']}` ({baseline['version']}); current: `{optimized['source']}` ({optimized['version']}).", '',
    f"Matched API {baseline['sdk']} {baseline['model']} AVD, software GLES, {baseline['refreshRateHz']:.2f} Hz, motion scale 1. Fonts/art warmed before sampling. Both APKs are debuggable.", '',
    'These are comparative review measurements. They do not measure cold startup or prove release performance on a physical device. Fixed action waits make elapsed time unsuitable as a navigation-latency claim. Dropped callbacks are recorded separately from frames exceeding their reported deadline.', '',
    '| Scenario | Frames before / after | p95 ms before / after | Over deadline % before / after | Dropped reports before / after |',
    '|---|---:|---:|---:|---:|',
]
for scenario in ('tabs-normal', 'tabs-double-text', 'requests-scroll', 'tartarus-scroll', 'day-complete', 'idle-today'):
    old, new = before[scenario], after[scenario]
    lines.append(f"| {scenario} | {old['frames']} / {new['frames']} | {number(old['p95Ms'])} / {number(new['p95Ms'])} | {number(old['overDeadlinePercent'])} / {number(new['overDeadlinePercent'])} | {old['droppedReports']} / {new['droppedReports']} |")
lines += ['', 'Raw JSON also retains p50, excluded first-draw frames, elapsed action time and process PSS. Frame-deadline comparisons use Android’s reported deadline, falling back to the measured refresh period when unavailable.', '']
(directory / 'comparison.md').write_text('\n'.join(lines))
print('\n'.join(lines))
