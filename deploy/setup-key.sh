#!/bin/bash
if bash -lc '[ -n "$DEEPSEEK_API_KEY" ]'; then
  VAL=$(bash -lc 'echo "$DEEPSEEK_API_KEY"')
elif grep -qE 'DEEPSEEK_API_KEY' ~/.bashrc 2>/dev/null; then
  # .bashrc 对非交互 shell 提前 return，直接从文件解析
  VAL=$(grep -E '^export DEEPSEEK_API_KEY=' ~/.bashrc | tail -1 | sed -e 's/^export[[:space:]]*DEEPSEEK_API_KEY=//' -e "s/^['\"]//" -e "s/['\"]\$//")
elif grep -qE '^DEEPSEEK_API_KEY=' ~/.bashrc 2>/dev/null; then
  VAL=$(grep -E '^DEEPSEEK_API_KEY=' ~/.bashrc | tail -1 | cut -d= -f2-)
else
  echo "MISS: .bashrc 里也没有"
  exit 1
fi
[ -n "$VAL" ] || { echo "MISS: 解析结果为空"; exit 1; }
printf 'DEEPSEEK_API_KEY=%s\n' "$VAL" > ~/tlc/.env
chmod 600 ~/tlc/.env
echo "OK: .env written, key length=${#VAL}, prefix=${VAL:0:3}***"