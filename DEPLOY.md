# Deploying the Ride Matching Service

Two ways to run the backend outside your IDE:

1. [Locally with Docker](#1-local-deploy-with-docker): one command, no JDK needed.
2. [Google Cloud App Engine](#2-google-cloud-app-engine): Standard or Flexible.


## 1. Local deploy with Docker

**Needs:** Docker Desktop (or any Docker engine with Compose v2).

### Start

```bash
docker compose up --build
```

- API: http://localhost:8080/api/v1/drivers
- Swagger: http://localhost:8080/swagger-ui.html
- Starts with the `local` profile, so the 5 demo drivers are loaded.

The container reports `healthy` once the API answers:

```bash
docker compose ps
```

### variations

```bash
BACKEND_PORT=9090 docker compose up --build                 # port 8080 already in use
SPRING_PROFILES_ACTIVE=prod docker compose up --build       # try the production settings
docker compose up -d --build --wait                         # run in the background, return when healthy
docker compose logs -f backend                              # follow logs
docker compose down                                         # stop and remove
```

### Without Compose

```bash
docker build -t ride-matching-service .
docker run --rm -p 8080:8080 ride-matching-service                                   # local profile
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ride-matching-service    # prod profile
```

## 2. Google Cloud App Engine

### Which environment?

| | **Standard** (recommended)    | **Flexible** |
|---|-------------------------------|---|
| What gets deployed | the jar you build             | the `Dockerfile`, built by Cloud Build |
| Config file | `src/main/appengine/app.yaml` | `app-flex.yaml` |
| Instance | `B2` (768 MB), 1 instance     | 1 vCPU / 1 GB VM, 1 instance |
| Cost | lower                         | higher (a VM runs all the time) |


### One-time setup

1. Install the gcloud CLI: https://cloud.google.com/sdk/docs/install
2. Log in and pick a project that has billing enabled:

   ```bash
   gcloud auth login
   gcloud config set project <PROJECT_ID>
   ```

3. Create the App Engine app, once per project. **The region cannot be changed later.**

   ```bash
   gcloud app create --region=europe-west1        # or us-central, asia-south1, …
   ```

4. Flexible only: enable the build API.

   ```bash
   gcloud services enable appengineflex.googleapis.com cloudbuild.googleapis.com
   ```

###  App Engine Standard (Java 21 runtime)

```bash
# 1. Test and build the jar (JDK 21)
mvn test
mvn -DskipTests package

# 2. Deploy the jar with the Standard config
gcloud app deploy target/ride-matching-service-0.0.1-SNAPSHOT.jar \
  --appyaml=src/main/appengine/app.yaml

# 3. Check it
gcloud app browse                    # opens https://<PROJECT_ID>.<REGION_ID>.r.appspot.com
curl https://<PROJECT_ID>.<REGION_ID>.r.appspot.com/api/v1/drivers
```

```yaml
env_variables:
  SPRING_PROFILES_ACTIVE: prod
  APP_SEED_ENABLED: "false"
  SPRINGDOC_API_DOCS_ENABLED: "false"
  SPRINGDOC_SWAGGER_UI_ENABLED: "false"
```



### Day-to-day commands

```bash
gcloud app logs tail -s default             # live logs
gcloud app versions list                    # every deployed version
gcloud app versions stop <VERSION>          # stop an old version (stops billing for it)
gcloud app versions delete <VERSION>        # remove it
```

Each deploy creates a new version and moves all traffic to it. Stop or delete old versions: with manual scaling, a version that is kept running keeps costing money.

---

## Checklist before deploying

- [ ] `mvn test` passes
- [ ] `SPRING_PROFILES_ACTIVE=prod` is set (it is in both App Engine configs)
- [ ] Decided whether this deployment should keep the demo drivers and public Swagger (both on in `prod`)
- [ ] One instance only
- [ ] Old App Engine versions stopped or deleted
