pipeline {
  agent { label 'maven-kaniko' }

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    skipDefaultCheckout(true)    // 기본 체크아웃 비활성화 (아래에서 직접 수행)
  }

  environment {
    IMAGE = "index.docker.io/frogrammer123/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {

    stage('Checkout') {
      steps {
        container('maven') {
          // ✅ 이 컨테이너(=maven) 워크스페이스에 직접 체크아웃
          checkout scm
          sh '''
            set -e
            echo "[CHECKOUT] pwd=$(pwd)"
            echo "[CHECKOUT] ls -la (repo root):"
            ls -la
            test -f Dockerfile || { echo "[ERROR] Dockerfile not found in repo root"; exit 1; }
          '''
        }
      }
    }

    stage('Build JAR') {
      steps {
        container('maven') {
          retry(2) {
            sh '''
              set -e
              echo "[DEBUG] PWD=$(pwd)"
              echo "[DEBUG] LS (repo root):"
              ls -la
              mvn -U \
                -Dmaven.wagon.http.retryHandler.count=5 \
                -Dmaven.wagon.rto=60000 \
                -Dmaven.wagon.connectTimeout=60000 \
                -Dmaven.test.skip=true clean package

              echo "[DEBUG] LS target/:"
              ls -la target || true
            '''
          }
        }
      }
    }

    stage('Build & Push Docker Image') {
      steps {
        container('kaniko') {
          withEnv(['DOCKER_CONFIG=/workspace/.docker']) {
            retry(2) {
              sh '''
                set -euo pipefail
                echo "[DEBUG] Kaniko context check:"
                echo "WORKSPACE=${WORKSPACE}"
                ls -la "${WORKSPACE}"
                test -f "${WORKSPACE}/Dockerfile" || { echo "[ERROR] Dockerfile not found at ${WORKSPACE}/Dockerfile"; exit 1; }

                mkdir -p "${DOCKER_CONFIG}"

                if [ -f /kaniko/.docker/.dockerconfigjson ]; then
                  echo "[DEBUG] Copying docker auth to ${DOCKER_CONFIG}/config.json"
                  cp /kaniko/.docker/.dockerconfigjson "${DOCKER_CONFIG}/config.json"
                else
                  echo "[WARN] /kaniko/.docker/.dockerconfigjson not found. Private registry push may fail."
                fi

                /kaniko/executor \
                  --context="${WORKSPACE}" \
                  --dockerfile="${WORKSPACE}/Dockerfile" \
                  --destination="${IMAGE}" \
                  --skip-tls-verify
              '''
            }
          }
        }
      }
    }

    stage('Sanity: kubectl shell') {
      options { timeout(time: 1, unit: 'MINUTES') }
      steps {
        container('kubectl') {
          sh '''
            set -e
            echo "[PING] kubectl container shell alive"
            echo "[WHOAMI]"; whoami || true
            echo "[WHICH SH]"; which sh || true
            echo "[/bin/sh]"; ls -l /bin/sh || true
            echo "[PWD]"; pwd; ls -la .
          '''
        }
      }
    }

    stage('Deploy to Kubernetes') {
      options { timeout(time: 5, unit: 'MINUTES') }
      steps {
        container('kubectl') {
          sh """
            set -euo pipefail

            echo "[INFO] Target image: ${env.IMAGE}"
            echo "[CHECK] kubectl connectivity"
            kubectl version --short || true
            kubectl get ns || true

            echo "[APPLY] Set image on Deployment"
            kubectl -n app-spring set image deploy/hello-spring app=${env.IMAGE}

            echo "[WAIT] Rollout status (with timeout)"
            if ! kubectl -n app-spring rollout status deploy/hello-spring --timeout=120s; then
              echo "[ERROR] Rollout timed out. Dumping diagnostics..."
              kubectl -n app-spring get pods -o wide || true
              kubectl -n app-spring describe deploy/hello-spring || true
              kubectl -n app-spring get events --sort-by=.metadata.creationTimestamp | tail -n 100 || true
              exit 1
            fi

            echo "[OK] Rollout succeeded."
            kubectl -n app-spring get pods -o wide
          """
        }
      }
    }
  }
}
