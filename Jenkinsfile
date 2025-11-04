pipeline {
  agent {
    kubernetes {
      //label 'maven-kaniko'
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
      secret:
        secretName: regcred
        items:                     
          - key: .dockerconfigjson
            path: config.json
  containers:
    - name: jnlp
      image: jenkins/inbound-agent:3341.v0766d82b_dec0-1
      tty: true
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent

    - name: maven
      image: maven:3.9-eclipse-temurin-17
      command: ["cat"]
      tty: true
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent

    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug
      command: ["cat"]
      tty: true
      env:
        - name: DOCKER_CONFIG
          value: /kaniko/.docker       
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace
          mountPath: /home/jenkins/agent
        - name: docker-config
          mountPath: /kaniko/.docker      
          readOnly: true

    - name: kubectl
      image: bitnami/kubectl:latest
      command: ["tail","-f","/dev/null"]   
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
          retry(2) {
            sh '''
              set -euo pipefail
              /kaniko/executor \
                --context="${WORKSPACE}" \
                --dockerfile="${WORKSPACE}/Dockerfile" \
                --destination="${IMAGE}" \
                --build-arg JAR_FILE=$(ls target/*.jar | head -n1)
            '''
          }
        }
      }
    }

  stage('Sanity: kubectl shell') {
  options { timeout(time: 1, unit: 'MINUTES') }
  steps {
    container('kubectl') {
      script {
        sh 'echo "[PING] kubectl alive $(kubectl version --client=true --short)"'
      }
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
