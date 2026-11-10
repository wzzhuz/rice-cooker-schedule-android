#!/usr/bin/env python3
"""电饭煲预约助手 · 自动编译与验收

由 build.yml 在每次 push / PR 时执行，全程无需密钥：
1. 校验归档文件齐全、原型内联 JS 语法通过
2. 用无头浏览器按 openspec 规格逐条验收（模拟时间注入 ?sim=）
3. 生成 dist/ 可部署站点（原型 + 预览 + 构建信息）
4. 生成归档压缩包供 artifact 下载

纯静态项目没有「编译」产物，因此把「编译」定义为：
校验 → 验收 → 生成预览 → 组装可发布站点 → 打包。
"""
from __future__ import annotations

import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime, timezone, timedelta

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / '.github' / 'scripts'))

REQUIRED = [
    'README.md',
    'docs/需求与界面设计_v0.5.md',
    'prototype/index.html',
    'previews/主界面.png',
    'previews/步进设置.png',
    'openspec/project.md',
    'openspec/AGENTS.md',
    'openspec/specs/schedule-list/spec.md',
    'openspec/changes/archive/2026-11-11-rice-cooker-schedule/proposal.md',
    'openspec/changes/archive/2026-11-11-rice-cooker-schedule/design.md',
    'openspec/changes/archive/2026-11-11-rice-cooker-schedule/tasks.md',
    'openspec/changes/archive/2026-11-11-rice-cooker-schedule/specs/schedule-list/spec.md',
]

results: list[tuple[str, bool, str]] = []


def check(name: str, ok: bool, detail: str = '') -> None:
    results.append((name, bool(ok), detail))
    mark = 'PASS' if ok else 'FAIL'
    print(f'[{mark}] {name}' + (f' — {detail}' if detail else ''), flush=True)
    if not ok:
        raise AssertionError(f'{name} 校验失败：{detail}')


def verify_files() -> None:
    missing = [p for p in REQUIRED if not (ROOT / p).is_file()]
    check('归档文件齐全（12 项）', not missing, '缺失: ' + ', '.join(missing) if missing else f'{len(REQUIRED)} 个文件均在')

    html = (ROOT / 'prototype/index.html').read_text(encoding='utf-8')
    scripts = re.findall(r'<script>(.*?)</script>', html, re.S)
    check('原型含内联脚本', len(scripts) == 1, f'找到 {len(scripts)} 段')

    tmp = ROOT / '.github' / 'scripts' / '_inline_check.js'
    tmp.write_text(scripts[0], encoding='utf-8')
    try:
        r = subprocess.run(['node', '--check', str(tmp)], capture_output=True, text=True)
        check('原型 JS 语法', r.returncode == 0, (r.stderr or r.stdout).strip()[:300])
    finally:
        tmp.unlink(missing_ok=True)

    # 规格与文档里必须出现的关键约定
    spec = (ROOT / 'openspec/specs/schedule-list/spec.md').read_text(encoding='utf-8')
    check('规格含 Requirement 与 Scenario',
          spec.count('### Requirement:') >= 10 and spec.count('#### Scenario:') >= 15,
          f"{spec.count('### Requirement:')} 个 Requirement / {spec.count('#### Scenario:')} 个 Scenario")


def verify_prototype() -> None:
    from playwright.sync_api import sync_playwright

    url = (ROOT / 'prototype/index.html').resolve().as_uri()
    errors: list[str] = []

    with sync_playwright() as p:
        launch_kwargs = {'headless': True, 'args': ['--no-sandbox']}
        chromium_path = os.environ.get('CHROMIUM_PATH')
        if chromium_path:
            launch_kwargs['executable_path'] = chromium_path
        browser = p.chromium.launch(**launch_kwargs)

        def open_page(sim: str):
            page = browser.new_page(viewport={'width': 460, 'height': 800}, device_scale_factor=2)
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(url + '?sim=' + sim)
            page.wait_for_timeout(250)
            return page

        def vals(page):
            return [v.strip() for v in page.locator('.val').all_inner_texts()]

        # 场景 A：睡前正常设置（对应 spec.md 第一个 Scenario）
        a = open_page('2026-11-09T21:37')
        got = vals(a)
        check('A 21:37 三行预约值', got == ['9 小时 50 分', '10 小时 20 分', '11 小时 50 分'], str(got))
        actual = [x.strip() for x in a.locator('.a').all_inner_texts()]
        check('A 实际完成时刻', actual == ['实际 07:27 完成', '实际 07:57 完成', '实际 09:27 完成'], str(actual))
        check('A 夜间自动深色', a.locator('body.dark').count() > 0)
        check('A 底部步进文字链', '步进 10 分钟' in a.inner_text('#stepBtn'))

        # 构建产物：主界面预览
        dist = ROOT / 'dist'
        (dist / 'previews').mkdir(parents=True, exist_ok=True)
        a.screenshot(path=str(dist / 'previews' / '主界面.png'))

        # 场景 B：今日已过自动顺延
        b = open_page('2026-11-10T08:05')
        got = vals(b)
        check('B 08:05 顺延与今天/明天', got == ['23 小时 20 分', '23 小时 50 分', '1 小时 20 分'], str(got))
        tags = [t.strip() for t in b.locator('.dst .d').all_inner_texts()]
        check('B 日期标注', tags == ['明天', '明天', '今天'], str(tags))
        check('B 日间自动浅色', b.locator('body.dark').count() == 0)

        # 构建产物：步进设置面板预览
        b.click('#stepBtn')
        b.wait_for_timeout(260)
        check('B 步进面板展开', b.locator('#stepPanel').is_visible())
        opts = [x.strip() for x in b.locator('#stepOpts button').all_inner_texts()]
        check('B 四个预设', opts == ['10 分', '15 分', '30 分', '60 分'], str(opts))
        b.screenshot(path=str(dist / 'previews' / '步进设置.png'))

        # 场景 C：目标时刻已到，跳到 24 小时且不迟到
        c = open_page('2026-11-11T07:30')
        got = vals(c)
        check('C 07:30 跳到 24 小时', got[:1] == ['24 小时'], str(got))
        check('C 提前 0 分钟', '实际 07:30 完成' in c.locator('.a').first.inner_text())

        # 场景 D：自定义步进 25 分（不整除 1440）+ maxSet 兜底
        d = open_page('2026-11-09T21:37')
        d.evaluate("() => localStorage.setItem('rice_cooker_v5', JSON.stringify({targets:['07:30','08:00','09:30'],stepMinutes:25}))")
        d.reload()
        d.wait_for_timeout(250)
        got = vals(d)
        check('D 自定义 25 分', got == ['9 小时 35 分', '10 小时', '11 小时 40 分'], str(got))
        check('D 自定义态标题', '自定义' in d.inner_text('#stepCap'))

        e = open_page('2026-11-11T07:29')
        e.evaluate("() => localStorage.setItem('rice_cooker_v5', JSON.stringify({targets:['07:30','08:00','09:30'],stepMinutes:25}))")
        e.reload()
        e.wait_for_timeout(250)
        first = vals(e)[0]
        check('E 25 分顺延不超 24 小时', first == '23 小时 45 分', first)

        # 场景 F：非法步进输入被拦住
        f = open_page('2026-11-09T21:37')
        f.click('#stepBtn')
        f.fill('#customStep', '5')
        f.click('#customApply')
        f.wait_for_timeout(120)
        check('F 非法输入行内提示', '10–240' in f.inner_text('#stepErr'))
        check('F 当前步进未变', '步进 10 分钟' in f.inner_text('#stepBtn'))

        # 场景 G：添加 / 删除 / 自动保存
        g = open_page('2026-11-09T21:37')
        g.click('#addBtn')
        g.fill('#newTime', '06:45')
        g.dispatch_event('#newTime', 'change')
        g.wait_for_timeout(160)
        check('G 添加后排首行', g.locator('.dst .t').first.inner_text() == '06:45')
        g.reload()
        g.wait_for_timeout(250)
        check('G 刷新后仍在', g.locator('.dst .t').first.inner_text() == '06:45')
        g.evaluate("() => [...document.querySelectorAll('.rowwrap')].find(x=>x.dataset.t==='08:00').classList.add('open')")
        g.locator('.rowwrap.open .del').click()
        g.wait_for_timeout(160)
        check('G 左滑删除生效', g.locator('[data-t="08:00"]').count() == 0)

        check('无浏览器运行时错误', not errors, '; '.join(errors[:3]))
        browser.close()


def build_dist() -> None:
    dist = ROOT / 'dist'
    dist.mkdir(exist_ok=True)
    (dist / 'docs').mkdir(exist_ok=True)
    (dist / 'openspec').mkdir(exist_ok=True)

    shutil.copy2(ROOT / 'prototype/index.html', dist / 'index.html')
    shutil.copy2(ROOT / 'docs/需求与界面设计_v0.5.md', dist / 'docs/需求与界面设计_v0.5.md')
    shutil.copytree(ROOT / 'openspec', dist / 'openspec', dirs_exist_ok=True)

    info = {
        'project': '电饭煲预约助手',
        'version': 'v0.5',
        'built_at': datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='seconds'),
        'commit': os.environ.get('GITHUB_SHA', 'local')[:12],
        'ref': os.environ.get('GITHUB_REF', 'local'),
        'checks': [{'name': n, 'passed': ok} for n, ok, _ in results],
        'checks_total': len(results),
        'checks_passed': sum(1 for _, ok, _ in results if ok),
        'entry': 'index.html',
    }
    (dist / 'BUILD_INFO.json').write_text(json.dumps(info, ensure_ascii=False, indent=2), encoding='utf-8')
    check('构建信息落盘', (dist / 'BUILD_INFO.json').is_file())

    # 归档压缩包（供 artifact 下载），不含 dist 自身与 CI 缓存
    bundle = ROOT / '电饭煲预约助手_项目归档.zip'
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as z:
        for path in sorted(ROOT.rglob('*')):
            if not path.is_file():
                continue
            rel = path.relative_to(ROOT)
            parts = rel.parts
            if parts[0] in ('dist', '.git') or rel.name == bundle.name:
                continue
            if '__pycache__' in parts or path.suffix == '.pyc':
                continue
            z.write(path, str(pathlib.Path('rice-cooker-schedule-archive') / rel))
    check('归档压缩包生成', bundle.is_file() and bundle.stat().st_size > 10000,
          f'{bundle.stat().st_size} bytes')


def main() -> None:
    verify_files()
    verify_prototype()
    build_dist()
    ok = sum(1 for _, passed, _ in results if passed)
    print(f'\n=== 自动编译完成：{ok}/{len(results)} 项通过 ===', flush=True)
    print('产出：dist/（可发布站点）、电饭煲预约助手_项目归档.zip', flush=True)


if __name__ == '__main__':
    main()
