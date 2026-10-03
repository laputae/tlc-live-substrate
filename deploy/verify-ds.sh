#!/bin/bash
cd ~/tlc
docker compose up -d --force-recreate agent 2>&1 | tail -1
KEY=$(grep DEEPSEEK_API_KEY .env | cut -d= -f2)
CODE=$(curl -s --max-time 20 https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $KEY" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"ping"}],"max_tokens":5}' \
  -o /tmp/ds.json -w '%{http_code}')
echo "deepseek api http: $CODE"
grep -o '"model":"deepseek-chat"' /tmp/ds.json | head -1