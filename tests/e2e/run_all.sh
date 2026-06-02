#!/bin/bash
cd /d/work/workspace/crob-ai-agent/tests/e2e
npx playwright test --reporter=list --timeout=60000 --workers=1 "$@"
