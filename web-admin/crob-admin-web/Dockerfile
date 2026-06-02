FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/node:20.19.0-bullseye-slim AS builder

WORKDIR /app
COPY . .

RUN npm config set fund false
RUN npm config set audit false
RUN npm i -g pnpm@9.15.9
RUN pnpm install
RUN pnpm build:local

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY --from=builder /app/nginx.conf /etc/nginx/conf.d/default.conf
