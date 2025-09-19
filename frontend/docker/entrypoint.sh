#!/bin/sh
set -e

: "${BACKEND_BASE_URL:=http://backend:8080}"

if [ -f /etc/nginx/templates/default.conf.template ]; then
    envsubst '${BACKEND_BASE_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf
else
    envsubst '${BACKEND_BASE_URL}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf
fi

exec nginx -g 'daemon off;'
