# springboot-harness-demo

A sample Spring Boot 3 REST service with a Harness CI/CD pipeline. The pipeline enforces
quality gates, runs functional tests on QA, and promotes or rejects the build based on the result.

## Pipeline flow

```
 Build & Quality Gate (CI)
   ├─ mvn clean verify ........... unit tests + JaCoCo line coverage >= 70%   ── gate 1
   ├─ SonarQube scan
   ├─ SonarQube quality gate ..... polls Sonar API, fails unless status == OK ── gate 2
   └─ Build & push image  <repo>:<sequenceId>
            │
 Deploy QA (K8s rolling, auto-rollback on failure)
            │
 Functional Tests (CI)
   ├─ Wait for QA ................ healthy AND /api/info reports this build's tag
   ├─ Run functional tests ....... REST Assured vs QA  →  output FT_STATUS = PASSED | FAILED
   └─ Promote image (if PASSED) .. registry retag → qa-verified-<tag>, qa-verified
            │
   ┌────────┴──────────────────────────┐
 FT_STATUS != PASSED               FT_STATUS == PASSED
 Reject Build ─ fails pipeline,    Prod Approval (manual)
 Prod never touched                    │
                                   Deploy Prod (same image as QA, via useFromStage)
```

The functional-test step always exits 0 and publishes its result as `FT_STATUS`.
Each downstream stage has a `when` condition on that value, so the pipeline branches
explicitly instead of simply stopping on a red step.

## Project layout

| Path | Purpose |
|---|---|
| `src/main/java/...` | `/api/products` CRUD (in-memory), `/api/info`, actuator health |
| `src/test/java/.../service`, `.../controller` | Unit tests (Surefire) |
| `src/test/java/.../functional/*FT.java` | Functional tests (Failsafe, `-Pfunctional-tests`) |
| `Dockerfile` | Runtime image that packages `target/app.jar` |
| `k8s/` | Deployment + Service templates and values with Harness expressions |
| `.harness/pipelines/springboot_demo_cicd.yaml` | The pipeline |
| `.harness/services`, `environments`, `infrastructures`, `triggers` | Supporting Harness entities |

## Run locally

```bash
mvn clean verify                       # unit tests + coverage gate
java -jar target/app.jar               # start on :8080
mvn verify -Pfunctional-tests -Dskip.unit.tests=true -Djacoco.skip=true -Dapp.baseUrl=http://localhost:8080
```

## Harness setup

1. **Push this folder to a Git repo** named `springboot-harness-demo`, or change `repoName` in the YAML files.
2. **Create connectors** in project `default_project` / org `default`. Change these IDs in the YAML if yours differ:
   - `github_connector`: GitHub (API access enabled, for triggers)
   - `dockerhub_connector`: Docker registry
   - `k8s_cluster_connector`: Kubernetes cluster (via a delegate)
   - `account.harnessImage`: built-in connector used to pull step images
3. **Create secrets:** `sonar_token`, `dockerhub_username`, `dockerhub_token`.
4. **SonarQube:** create project `springboot-harness-demo` and assign it a quality gate (e.g. "Sonar way").
   For SonarCloud, also add `-Dsonar.organization=<org>` to the scan step.
5. **Import the entities** (*Git Experience → Import from Git*, or paste the YAML) in this order:
   environments → infrastructures → service → pipeline → trigger.
6. **Prepare the cluster** (this sample targets Docker Desktop Kubernetes, but any cluster works):
   ```bash
   kubectl create namespace springboot-qa
   kubectl create namespace springboot-prod
   kubectl create namespace harness-builds
   ```
   Install a Harness delegate into the cluster with Helm; `k8s_cluster_connector` uses that delegate.
7. **Docker Hub repository must be public**, or add an `imagePullSecret`, so the cluster can pull the image.

The build stage runs on Harness Cloud. The functional-test stage runs as pods in the cluster
(namespace `harness-builds`), so it reaches QA at
`http://springboot-harness-demo.springboot-qa.svc.cluster.local` (the `qaBaseUrl` default) and
QA never needs to be exposed publicly.

To open an environment in your browser:

```bash
kubectl port-forward svc/springboot-harness-demo -n springboot-qa 8081:80   # http://localhost:8081/api/info
```

## Seeing the gates work

- **Quality gate fail:** delete a unit test so coverage drops below 70%, or add a code smell that breaks the Sonar gate.
  The pipeline stops before any deployment.
- **Functional test fail:** change `productLifecycle` in `ProductApiFT` to expect a different status code.
  QA deploys, `FT_STATUS=FAILED`, the image is not promoted, *Reject Build* fails the pipeline, and Prod is skipped.
