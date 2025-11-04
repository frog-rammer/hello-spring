pipeline {
  agent { label 'maven-kaniko' }

  environment {
    IMAGE = "index.docker.io/frogrammer123/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {

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

                # writable docker config for kaniko
                mkdir -p "${DOCKER_CONFIG}"

                # copy readonly secret content if present
                if [ -f /kaniko/.docker/.dockerconfigjson ]; then
                  echo "[DEBUG] Found /kaniko/.docker/.dockerconfigjson -> copying to ${DOCKER_CONFIG}/config.json"
                  cp /kaniko/.docker/.dockerconfigjson "${DOCKER_CONFIG}/config.json"
                else
                  echo "[WARN] /kaniko/.docker/.dockerconfigjson not found. If pushing to a private registry, auth may fail."
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

    /* ====== 새로 추가: kubectl 컨테이너 진입/쉘 확인 ====== */
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
      options { timeout(time: 5, unit: 'MINUTES') }  // 무한대기 방지
      steps {
        container('kubectl') {
          sh """
            set -euo pipefail

            echo "[INFO] Target image: ${env.IMAGE}"
            echo "[CHECK] kubectl connectivity"
            kubectl version --short || true
            kubectl get ns || true

            echo "[APPLY] Set image on Deployment"
            # 컨테이너 이름 'app'이 Deployment 내 컨테이너 이름과 반드시 일치해야 함
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
