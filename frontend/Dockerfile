# Imagen del frontend (React + Vite) servido por Nginx.
# Las URLs de la API se inyectan como build-args (Vite las "hornea" en el
# bundle). Todas apuntan al Ingress (http://parqueadero.local) que reenvia
# /api al gateway Kong.
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

ARG VITE_API_USUARIOS
ARG VITE_API_ZONAS
ARG VITE_API_ASIGNACIONES
ARG VITE_API_TICKETS
ARG VITE_API_VEHICULOS
ARG VITE_API_AUDIT
ENV VITE_API_USUARIOS=$VITE_API_USUARIOS \
    VITE_API_ZONAS=$VITE_API_ZONAS \
    VITE_API_ASIGNACIONES=$VITE_API_ASIGNACIONES \
    VITE_API_TICKETS=$VITE_API_TICKETS \
    VITE_API_VEHICULOS=$VITE_API_VEHICULOS \
    VITE_API_AUDIT=$VITE_API_AUDIT
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
