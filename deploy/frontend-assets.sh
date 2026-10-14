#!/bin/sh
set -eu

# Bind server-rendered detail HTML to the exact tested frontend build.
# Hash URLs also avoid stale browser assets when a CDN overrides cache headers.
entry_js="$(sed -n 's/.*src="\([^" ]*\/assets\/index-[^" ]*\.js\)".*/\1/p' /srv/index.html | head -n 1)"
entry_css="$(sed -n 's/.*href="\([^" ]*\/assets\/index-[^" ]*\.css\)".*/\1/p' /srv/index.html | head -n 1)"
test -n "$entry_js" && test -n "$entry_css"
sed -i "s|__TOWNPET_ENTRY_JS__|$entry_js|g; s|__TOWNPET_ENTRY_CSS__|$entry_css|g" \
  /etc/nginx/templates/default.conf.template
