# ---------- Stage 1 : Build ----------
# Node LTS 22 (compatible Angular 22)
FROM node:22-alpine AS build
WORKDIR /app

# Copier le lock file d'abord -> meilleur cache Docker layer (npm ci deterministe)
COPY package.json package-lock.json ./
RUN npm ci

# Copier les sources puis builder Angular en production
COPY . .
RUN npm run build -- --configuration production

# ---------- Stage 2 : Runtime nginx ----------
FROM nginx:alpine

# Config reverse proxy / SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier le build Angular (output du builder @angular/build:application)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
