pipeline {
  agent {
    kubernetes {
      label 'maven-kaniko'            // 이 잡 전용 라벨
      defaultContainer 'maven'
      yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins: agent
spec:
  serviceAccountName: jenkins
  restartPolicy: Never
  volumes:
    - name: workspace
      emptyDir: {}
    - name: docker-config
      emptyDir: {}
  containers:
    - name: jnlp
      image: jenkins/inbound-agent:3341.v0766d82b_dec0-1
      tty: true
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent

    - name: maven
      image: maven:3.9-eclipse-temurin-17
      command: ["cat"]                # keep-alive
      tty: true
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent

    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug
      command: ["cat"]                # keep-alive
      tty: true
      env:
        - name: DOCKER_CONFIG
          value: /workspace/.docker
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent
        - name: docker-config
          mountPath: /workspace/.docker

    - name: kubectl
      image: bitnami/kubectl:latest   # 지금 잘 됐던 latest로 복구
      command: ["sh","-c","sleep 365d"]
      tty: true
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent
"""
    }
  }

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    skipDefaultCheckout(true)
  }

  environment {
    IMAGE = "index.docker.io/frogrammer123/hello-spring:1.0.${BUILD_NUMBER}"
  }

  stages {
    // ⬇️ 아래는 지금 쓰시던 Checkout/Build/Kaniko/Sanity/Deploy 스테이지 그대로 유지
    stage('Checkout') {
      steps {
        container('maven') {
          checkout scm
          sh '''
            set -e
            echo "[CHECKOUT] pwd=$(pwd)"
            ls -la
            test -f Dockerfile || { echo "[ERROR] Dockerfile not found"; exit 1; }
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
              mvn -U \
                -Dmaven.wagon.http.retryHandler.count=5 \
                -Dmaven.wagon.rto=60000 \
                -Dmaven.wagon.connectTimeout=60000 \
                -Dmaven.test.skip=true clean package
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
                mkdir -p "${DOCKER_CONFIG}"
                if [ -f /kaniko/.docker/.dockerconfigjson ]; then
                  cp /kaniko/.docker/.dockerconfigjson "${DOCKER_CONFIG}/config.json"
                fi
                /kaniko/executor \
                  --context="${WORKSPACE}" \
                  --dockerfile="${WORKSPACE}/Dockerfile" \
                  --destination="${IMAGE}" \
                  --build-arg JAR_FILE=$(ls target/*.jar | head -n1) \
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
            echo "[PING] kubectl OK"; whoami; pwd; ls -la .
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
            kubectl -n app-spring set image deploy/hello-spring app=${env.IMAGE}
            kubectl -n app-spring rollout status deploy/hello-spring --timeout=120s
            kubectl -n app-spring get pods -o wide
          """
        }
      }
    }
  }
}
