# lastname-todo

lastname-todo is a small Spring Boot Todo API backed by MongoDB. It follows the deployment conventions from the [Deployment Crash Course](https://gitlab.com/ustynov.dmitriy/deployment-crash-course-2026/): a multi-stage Docker build, a Compose-managed database with a named volume, an internal-only database port, host-level Nginx, and a manual git pull + rebuild redeploy.

## API

The API is available under /api/todos:

| Method | Path | Meaning |
|---|---|---|
| GET | /api/todos | List all todos |
| GET | /api/todos/{id} | Get one todo |
| POST | /api/todos | Create a todo |
| PUT | /api/todos/{id} | Replace a todo |
| DELETE | /api/todos/{id} | Delete a todo |

Todo JSON has this shape:

~~~json
{
  "title": "Buy milk",
  "completed": false
}
~~~

GET / is a small status endpoint used to check that the deployment is alive.

## Run locally with Docker Compose

Docker and Docker Compose are required. The Compose file starts both the API and MongoDB; the MongoDB container is not published to the host. This is the same layout used on the VM and does not depend on the host's MongoDB service.

~~~bash
cp .env.example .env
# Edit .env and replace DB_PASSWORD with a private, URL-safe password.
docker compose up -d --build
docker compose ps
curl http://localhost:5200/
~~~

Try the CRUD API:

~~~bash
curl -X POST http://localhost:5200/api/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn deployment","completed":false}'

curl http://localhost:5200/api/todos

# Replace TODO_ID with the id returned by POST.
curl -X PUT http://localhost:5200/api/todos/TODO_ID \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn Docker deployment","completed":true}'

curl -X DELETE http://localhost:5200/api/todos/TODO_ID
~~~

Stop the containers without deleting the database volume:

~~~bash
docker compose down
~~~

Do not use docker compose down -v unless you intentionally want to delete the Todo data.

## Run directly against the installed MongoDB

The application defaults to mongodb://localhost:27017/lastname_todo, so it can also be run outside Docker against the MongoDB already installed on the machine. Java 17+ is required:

~~~bash
./mvnw test
./mvnw spring-boot:run
~~~

If the local MongoDB requires authentication, provide your own URI:

~~~bash
MONGODB_URI='mongodb://user:password@localhost:27017/lastname_todo?authSource=admin' \
./mvnw spring-boot:run
~~~

## Port map for student index 20

The course formula is OFFSET = index × 10. For index 20, OFFSET = 200:

| Service | Port | Where it is configured |
|---|---:|---|
| Spring Boot API | 5200 | .env → API_PORT; SERVER_PORT is passed to the container |
| MongoDB | 27217 | .env → DB_PORT; internal Compose port only |
| Nginx | 8280 | .env → WEB_PORT for reference and the Nginx site config |

The only port you normally change for the API is API_PORT in .env. Compose uses it for both sides of the API port mapping and passes it to Spring Boot as SERVER_PORT. If you change the API port after configuring Nginx, also change the proxy_pass port in nginx/lastname.conf.example.

The MongoDB port must stay consistent between DB_PORT, the MongoDB command, the healthcheck, and the connection URI. The database deliberately has no ports section, so it cannot be reached directly from outside Docker.

If “index 20” was intended to mean group slot 2, use the course's slot-2 values instead: API 5020, MongoDB 27037, and Nginx 8100. Update .env, the Compose defaults if .env is absent, and the Nginx example together.

## Deploy to the remote VM

These steps assume the repository has been pushed to GitLab or another Git host, the VM has a public IP, and your account's public SSH key is already installed. Replace <vm-ip>, <username>, and <your-repo-url>; keep the private key on your own computer.

### 1. Push the project

From the project directory, use the existing Git remote if one is already configured. Otherwise:

~~~bash
git init
git add -A
git commit -m "Build MongoDB Todo API"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
~~~

.env is ignored and must never be committed. Commit .env.example instead.

### 2. Configure the SSH alias

On your own computer, create or edit ~/.ssh/config:

~~~sshconfig
Host deploy-vm
    HostName <vm-ip>
    User <username>
    IdentityFile ~/.ssh/id_ed25519
~~~

Use the actual private-key path if yours has a different name. Then verify the key-based connection:

~~~bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519
ssh deploy-vm
~~~

If the VM was provisioned with another public key, install the matching .pub key in ~/.ssh/authorized_keys on the VM first. Never copy or commit the private key.

### 3. Prepare the VM checkout and secrets

The course assumes Docker Engine, Docker Compose, and Git are already installed on the VM. After connecting:

~~~bash
ssh deploy-vm
docker --version
docker compose version
git clone <your-repo-url> ~/lastname-app
exit
~~~

On your own computer, create the private deployment environment file and send it separately from Git:

~~~bash
cp .env.example .env
# Edit .env: use a real private password, not change_me_20.
scp .env deploy-vm:~/lastname-app/.env
~~~

The VM .env for index 20 should contain:

~~~dotenv
API_PORT=5200
WEB_PORT=8280
DB_HOST=db
DB_PORT=27217
DB_NAME=lastname_todo
DB_USERNAME=lastname_user
DB_PASSWORD=replace_with_a_private_url_safe_password
~~~

MongoDB runs as the db Compose service. Do not set DB_HOST=localhost in this file: from inside the server container, localhost means the server container, not MongoDB.

### 4. Start and check the stack

On the VM:

~~~bash
ssh deploy-vm
cd ~/lastname-app
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 db
docker compose logs --tail=100 server
~~~

Wait until db is healthy and server is Up, then test from your own computer:

~~~bash
curl http://<vm-ip>:5200/
curl http://<vm-ip>:5200/api/todos
~~~

If the raw port times out, the VM firewall may need a temporary rule while you perform this check. Coordinate with everyone sharing the VM before changing firewall rules:

~~~bash
sudo ufw status
sudo ufw allow 5200/tcp
~~~

Before starting or stopping anything on a shared VM, inspect ownership:

~~~bash
docker ps --format "table {{.Names}}\t{{.Ports}}"
df -h
~~~

Only stop or remove containers beginning with lastname-. Never run docker system prune -a --volumes on the shared VM without group approval.

### 5. Put Nginx in front of the API

Nginx is installed on the VM host, not in Compose:

~~~bash
sudo apt update
sudo apt install -y nginx
~~~

Copy the example from the checkout, or create the same file with an editor:

~~~bash
sudo cp nginx/lastname.conf.example /etc/nginx/sites-available/lastname
sudo ln -s /etc/nginx/sites-available/lastname /etc/nginx/sites-enabled/lastname
sudo nginx -t
sudo systemctl reload nginx
~~~

The example listens on 8280 and proxies to 127.0.0.1:5200. Verify it:

~~~bash
curl -I http://<vm-ip>:8280/
curl http://<vm-ip>:8280/api/todos
~~~

After Nginx works, close the raw API port. Check the shared firewall policy first; do not touch ports belonging to other students:

~~~bash
sudo ufw allow 8280/tcp
sudo ufw deny 5200/tcp
~~~

Nginx can still reach the API through 127.0.0.1; only direct external access to port 5200 is blocked. Keep Strict-Transport-Security and Content-Security-Policy out of this plain-HTTP configuration until HTTPS is actually enabled.

### 6. HTTPS and the public hostname

The crash-course README says the instructor connects each student's hostname through a Cloudflare Tunnel. You do not configure Certbot or TLS in this student app. Once the instructor wires the hostname to port 8280, verify it:

~~~bash
curl -I https://<your-hostname>.csit-mitit.com
~~~

If the VM IP and Nginx port work but the hostname does not, report the tunnel or DNS issue to the instructor.

### 7. Redeploy after a code change

The course intentionally uses a manual, repeatable redeploy instead of CI/CD:

~~~bash
# On your computer
git add -A
git commit -m "Update Todo behavior"
git push

# On the VM
ssh deploy-vm
cd ~/lastname-app
git pull
docker compose up -d --build
docker compose ps
curl http://<vm-ip>:8280/
~~~

This should not require changing .env, the MongoDB volume, or the Nginx configuration. Do not use docker compose down -v during a redeploy.

