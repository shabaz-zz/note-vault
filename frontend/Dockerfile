# ============================================================
#  Dockerfile — Frontend
#  Serves index.html via Nginx and proxies /api calls
#  to the VaultCourier backend container.
# ============================================================

# Use the tiny Alpine version of Nginx
FROM nginx:alpine

# Remove default Nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy our frontend code into the Nginx web folder
COPY index.html /usr/share/nginx/html/

# Copy our custom config — this is what routes /api
# calls to the backend container
COPY nginx.conf /etc/nginx/nginx.conf

# Expose port 80 (Standard Web Port)
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
