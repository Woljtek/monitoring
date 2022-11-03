# Build plugins
FROM node:17.3.0-alpine3.12 AS builder
RUN apk update && apk add --no-cache \
    g++ \
    git \
    make

RUN mkdir ./plugins

COPY grafana/plugins/queue-error-management ./plugins/queue-error-management
RUN cd grafana/plugins/Queue-error-management && yarn install && yarn build && rm -rf node_modules

COPY grafana/plugins/invalidations ./plugins/invalidations
RUN cd grafana/plugins/invalidations && yarn install && yarn build && rm -rf node_modules

# Build Grafana
FROM grafana/grafana:9.0.2
USER root

RUN apk update && apk add --no-cache \
    curl

USER grafana
COPY --from=builder plugins/ "$GF_PATHS_PLUGINS"
