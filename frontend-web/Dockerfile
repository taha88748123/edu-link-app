# syntax=docker/dockerfile:1.6

# --- Stage 1 : build ---------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Install pnpm. If a pnpm-lock.yaml is present we use it for reproducible installs;
# otherwise we fall back to npm via the also-copied package-lock.json.
RUN corepack enable && corepack prepare pnpm@9 --activate

COPY package.json ./
COPY pnpm-lock.yaml* package-lock.json* ./

RUN if [ -f pnpm-lock.yaml ]; then \
        pnpm install --frozen-lockfile; \
    else \
        npm ci --omit=optional || npm install; \
    fi

COPY . .

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN if [ -f pnpm-lock.yaml ]; then pnpm build; else npm run build; fi

# --- Stage 2 : nginx runtime -------------------------------------------------
FROM nginx:1.27-alpine

# Replace the default config with our SPA-aware one.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the static build output.
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
