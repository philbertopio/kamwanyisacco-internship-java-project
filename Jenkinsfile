
pipeline {
    agent { label 'host-server' }

    // Global environment
    environment {
        // Image
        IMAGE_NAME      = "kimwanyisacco"
        IMAGE_TAG       = "latest"

        // Container names
        APP_CONTAINER   = "sacco-app"
        DB_CONTAINER    = "sacco-mysql"

        // Ports  (host → container)
        APP_HOST_PORT   = "9090"          // browse at http://<server>:9090/KimwanyiSacco/
        APP_CTR_PORT    = "8080"
        DB_HOST_PORT    = "3306"          // MySQL exposed locally only
        DB_CTR_PORT     = "3306"

        // Shared internal network name (created once, reused)
        PODMAN_NETWORK  = "sacco-net"

        // Source
        GIT_REPO_URL    = "https://github.com/kamwanyisacco/kamwanyisacco-internship-java-project.git"
        GIT_BRANCH      = "ft_deploy"

        // MySQL image
        MYSQL_IMAGE     = "docker.io/library/mysql:8.0"
    }

    stages {

        // 1. Source code
        stage('Checkout') {
            steps {
                echo "Cloning ${GIT_REPO_URL} @ ${GIT_BRANCH}..."
                git branch: "${GIT_BRANCH}",
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO_URL}"
            }
        }

        // 2. Unit / integration tests
        stage('Test') {
            tools {
                maven 'maven3'   // must match the name set in Jenkins → Tools
            }
            steps {
                dir('kamwanyisacco-internship-java-project') {
                    echo "Running Maven tests..."
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'kamwanyisacco-internship-java-project/target/surefire-reports/*.xml'
                }
            }
        }

        // 3. Build Podman image
        stage('Build Image') {
            steps {
                dir('kamwanyisacco-internship-java-project') {
                    echo "Building Podman image ${IMAGE_NAME}:${IMAGE_TAG}..."
                    sh """
                        podman build \\
                            -f Containerfile \\
                            -t ${IMAGE_NAME}:${IMAGE_TAG} \\
                            --label "build.number=${BUILD_NUMBER}" \\
                            --label "git.commit=${GIT_COMMIT}" \\
                            .
                    """
                }
            }
        }

        //4. Ensure internal network exists
        stage('Prepare Network') {
            steps {
                sh """
                    podman network exists ${PODMAN_NETWORK} \\
                        || podman network create ${PODMAN_NETWORK}
                """
            }
        }

        // 5. Stop & remove old containers
        stage('Remove Old Containers') {
            steps {
                echo "Tearing down old containers (if any)..."
                sh """
                    podman stop ${APP_CONTAINER} 2>/dev/null || true
                    podman rm   ${APP_CONTAINER} 2>/dev/null || true

                    podman stop ${DB_CONTAINER}  2>/dev/null || true
                    podman rm   ${DB_CONTAINER}  2>/dev/null || true
                """
            }
        }

        //  6. Start MySQL
        stage('Start MySQL') {
            steps {
                // Load the production .env secret file, then start the DB container
                withCredentials([file(credentialsId: 'sacco-env-file', variable: 'ENV_FILE')]) {
                    sh """
                        # Parse only the DB-related vars we need
                        set -a; source "\${ENV_FILE}"; set +a

                        podman run -d \\
                            --name  ${DB_CONTAINER} \\
                            --network ${PODMAN_NETWORK} \\
                            --restart unless-stopped \\
                            -p 127.0.0.1:${DB_HOST_PORT}:${DB_CTR_PORT} \\
                            -v sacco_mysql_data:/var/lib/mysql \\
                            -e MYSQL_ROOT_PASSWORD="\${MYSQL_ROOT_PASSWORD}" \\
                            -e MYSQL_DATABASE="\${DB_NAME}" \\
                            -e MYSQL_USER="\${DB_USERNAME}" \\
                            -e MYSQL_PASSWORD="\${DB_PASSWORD}" \\
                            ${MYSQL_IMAGE}
                    """
                }

                // Wait until MySQL is healthy before proceeding
                echo "Waiting for MySQL to be ready..."
                sh """
                    RETRIES=20
                    until podman exec ${DB_CONTAINER} \\
                          mysqladmin ping -u root --silent 2>/dev/null; do
                        RETRIES=\$((RETRIES - 1))
                        if [ "\$RETRIES" -le 0 ]; then
                            echo "ERROR: MySQL did not start in time."
                            exit 1
                        fi
                        echo "  MySQL not ready yet – retrying in 5 s (attempts left: \$RETRIES)..."
                        sleep 5
                    done
                    echo "MySQL is ready."
                """
            }
        }

        // 7. Deploy App container
        stage('Deploy App') {
            steps {
                withCredentials([file(credentialsId: 'sacco-env-file', variable: 'ENV_FILE')]) {
                    sh """
                        set -a; source "\${ENV_FILE}"; set +a

                        podman run -d \\
                            --name  ${APP_CONTAINER} \\
                            --network ${PODMAN_NETWORK} \\
                            --restart unless-stopped \\
                            -p ${APP_HOST_PORT}:${APP_CTR_PORT} \\
                            -e DB_HOST="${DB_CONTAINER}" \\
                            -e DB_PORT="\${DB_PORT:-3306}" \\
                            -e DB_NAME="\${DB_NAME}" \\
                            -e DB_USERNAME="\${DB_USERNAME}" \\
                            -e DB_PASSWORD="\${DB_PASSWORD}" \\
                            -e PESAPAL_CONSUMER_KEY="\${PESAPAL_CONSUMER_KEY}" \\
                            -e PESAPAL_CONSUMER_SECRET="\${PESAPAL_CONSUMER_SECRET}" \\
                            -e PESAPAL_BASE_URL="\${PESAPAL_BASE_URL}" \\
                            -e PESAPAL_CALLBACK_URL="\${PESAPAL_CALLBACK_URL}" \\
                            -e PESAPAL_IPN_URL="\${PESAPAL_IPN_URL}" \\
                            -e PAYMENT_CURRENCY="\${PAYMENT_CURRENCY}" \\
                            -e BUSINESS_NAME="\${BUSINESS_NAME}" \\
                            ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        // 8. Health check
        stage('Health Check') {
            steps {
                echo "Waiting 30 s for Tomcat + app to fully start..."
                sh """
                    sleep 30
                    STATUS=\$(curl -s -o /dev/null -w "%{http_code}" \\
                               http://localhost:${APP_HOST_PORT}/KimwanyiSacco/ || echo "000")

                    echo "HTTP status: \$STATUS"

                    if echo "\$STATUS" | grep -qE '^(200|302|303)'; then
                        echo "App is UP and responding."
                    else
                        echo "WARNING: App returned HTTP \$STATUS – may still be initialising."
                        echo "Check logs with:  podman logs ${APP_CONTAINER}"
                    fi
                """
            }
        }

        // 9. Clean up dangling images
        stage('Cleanup') {
            steps {
                sh 'podman image prune -f || true'
            }
        }
    }

    //  Post-pipeline notifications
    post {
        success {
            echo """

  Deployment SUCCESSFUL  (build #${BUILD_NUMBER})
 App: http://localhost:${APP_HOST_PORT}/KimwanyiSacco/

"""
        }
        failure {
            echo "Pipeline FAILED on build #${BUILD_NUMBER}. Check the logs above."
            sh """
                echo "=== App container logs (last 50 lines) ==="
                podman logs --tail=50 ${APP_CONTAINER} 2>/dev/null || true
                echo "=== DB container logs (last 20 lines) ==="
                podman logs --tail=20 ${DB_CONTAINER}  2>/dev/null || true
            """
        }
        always {
            echo "Container status:"
            sh """
                podman ps -a --filter name=${APP_CONTAINER} --filter name=${DB_CONTAINER} || true
            """
        }
    }
}
