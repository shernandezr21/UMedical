# Compila el panel Angular y lo sirve con Caddy (HTTPS automatico + proxy hacia la API).
# Contexto de construccion: la raiz del repositorio.

# ---- Etapa 1: compilar Angular ----
FROM node:22 AS build
WORKDIR /app
ENV NODE_OPTIONS=--max-old-space-size=1536
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
# 1) En produccion el panel llama a la API por la misma direccion (/api/v1): sin CORS ni IP fija.
#    Se reemplaza la URL de desarrollo en TODO el codigo (algunos servicios la declaran por su cuenta).
# 2) Los limites de tamano (budgets) de Angular no deben impedir publicar la demo.
RUN grep -rl "http://localhost:8080/api/v1" src | xargs -r sed -i "s#http://localhost:8080/api/v1#/api/v1#g" \
 && ! grep -rq "localhost:8080" src \
 && node -e "const fs=require('fs');const a=JSON.parse(fs.readFileSync('angular.json','utf8'));const p=Object.values(a.projects)[0];const c=p.architect.build.configurations&&p.architect.build.configurations.production;if(c&&c.budgets){c.budgets.forEach(b=>{if(b.type==='initial'){b.maximumWarning='3mb';b.maximumError='6mb'}if(b.type==='anyComponentStyle'){b.maximumWarning='30kb';b.maximumError='60kb'}})}fs.writeFileSync('angular.json',JSON.stringify(a,null,2))" \
 && npx ng build --configuration production

# ---- Etapa 2: servidor web ----
FROM caddy:2-alpine
COPY --from=build /app/dist/*/browser/ /srv/
COPY deploy/Caddyfile /etc/caddy/Caddyfile
