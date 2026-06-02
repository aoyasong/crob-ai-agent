#!/bin/bash
cd /d/work/workspace/crob-ai-agent/tests/e2e
npx playwright test -g "06 - 菜单列表" --reporter=list 2>&1
