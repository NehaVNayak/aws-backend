// =============================================================================
// Jenkinsfile — Backend (Spring Boot)
// Pipeline: Clean Compile → Build JAR → Docker Image → Push to ECR → Deploy EC2
// =============================================================================

pipeline {

    agent any

    // -------------------------------------------------------------------------
    // Pipeline-level environment variables
    // -------------------------------------------------------------------------
    environment {
        // AWS / ECR
        AWS_REGION          = 'us-east-1'
        AWS_ACCOUNT_ID      = '888577028066'
        ECR_REPO            = 'fanverseeeee'
        IMAGE_TAG           = 'backend-latest'
        ECR_REGISTRY        = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        FULL_IMAGE          = "${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}"

        // EC2
        EC2_USER            = 'ubuntu'
        EC2_HOST            = 'ec2-34-227-7-122.compute-1.amazonaws.com'
        CONTAINER_NAME      = 'backend'
        DOCKER_NETWORK      = 'fanverse-network'
        HOST_PORT           = '6001'
        CONTAINER_PORT      = '8085'

        // S3 bucket name injected at runtime
        S3_BUCKET           = 's3-test-01-navaneeth'
    }


    stages {

        // =====================================================================
        // STAGE 1 — Clean & Compile
        // =====================================================================
        stage('Clean & Compile') {
            steps {
                dir('backend') {
                    sh '''
                        echo "========== Clean & Compile =========="
                        chmod +x mvnw
                        ./mvnw clean compile -B
                    '''
                }
            }
        }

        // =====================================================================
        // STAGE 2 — Build JAR (package, skip unit tests for speed)
        // =====================================================================
        stage('Build JAR') {
            steps {
                dir('backend') {
                    sh '''
                        echo "========== Packaging JAR =========="
                        ./mvnw package -DskipTests -B
                        echo "--- Built artifacts ---"
                        ls -lh target/*.jar
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        // =====================================================================
        // STAGE 3 — Build Docker Image
        // =====================================================================
        stage('Build Docker Image') {
            steps {
                dir('backend') {
                    sh """
                        echo "========== Building Docker image =========="
                        docker build -t ${FULL_IMAGE} .
                        docker images | grep ${ECR_REPO}
                    """
                }
            }
        }

        // =====================================================================
        // STAGE 4 — Push Docker Image to ECR
        // =====================================================================
        stage('Push to ECR') {
            steps {
                withCredentials([
                    string(credentialsId: 'AWS_ACCESS_KEY_ID',     variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        echo "========== Logging into ECR =========="
                        export AWS_ACCESS_KEY_ID=\$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=\$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        aws ecr get-login-password --region ${AWS_REGION} | \\
                            docker login --username AWS --password-stdin ${ECR_REGISTRY}

                        echo "========== Pushing image to ECR =========="
                        docker push ${FULL_IMAGE}

                        echo "========== Push complete =========="
                    """
                }
            }
        }

        // =====================================================================
        // STAGE 5 — Deploy to EC2
        // SSH in, pull latest image, stop old container, run new one
        // =====================================================================
        stage('Deploy to EC2') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId  : 'EC2_PEM_KEY',
                        keyFileVariable: 'PEM_FILE'
                    ),
                    string(credentialsId: 'AWS_ACCESS_KEY_ID',     variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        echo "========== Deploying Backend to EC2 =========="

                        ssh -o StrictHostKeyChecking=no -i "\$PEM_FILE" ${EC2_USER}@${EC2_HOST} "
                            # Authenticate Docker with ECR
                            export AWS_ACCESS_KEY_ID=\$AWS_ACCESS_KEY_ID
                            export AWS_SECRET_ACCESS_KEY=\$AWS_SECRET_ACCESS_KEY
                            export AWS_DEFAULT_REGION=${AWS_REGION}

                            aws ecr get-login-password --region ${AWS_REGION} | \\
                                docker login --username AWS --password-stdin ${ECR_REGISTRY}

                            # Pull latest image
                            echo 'Pulling ${FULL_IMAGE} ...'
                            docker pull ${FULL_IMAGE}

                            # Stop & remove existing container if it exists
                            if docker ps -a --format '{{.Names}}' | grep -q '^${CONTAINER_NAME}\$'; then
                                echo 'Stopping existing container: ${CONTAINER_NAME}'
                                docker stop  ${CONTAINER_NAME} || true
                                docker rm -f ${CONTAINER_NAME} || true
                            fi

                            # Clean up old dangling images
                            docker image prune -f || true

                            # Create Docker network if not present
                            docker network inspect ${DOCKER_NETWORK} > /dev/null 2>&1 || \\
                                docker network create ${DOCKER_NETWORK}

                            # Run the new container
                            docker run -d \\
                                --name ${CONTAINER_NAME} \\
                                --network ${DOCKER_NETWORK} \\
                                --restart unless-stopped \\
                                -p ${HOST_PORT}:${CONTAINER_PORT} \\
                                -e AWS_ACCESS_KEY_ID='\$AWS_ACCESS_KEY_ID' \\
                                -e AWS_SECRET_ACCESS_KEY='\$AWS_SECRET_ACCESS_KEY' \\
                                -e AWS_REGION='${AWS_REGION}' \\
                                -e AWS_S3_BUCKET_NAME='${S3_BUCKET}' \\
                                ${FULL_IMAGE}

                            echo '--- Running containers ---'
                            docker ps --filter name=${CONTAINER_NAME}
                        "

                        echo "========== Deployment complete =========="
                    """
                }
            }
        }

    } // end stages

    // -------------------------------------------------------------------------
    // Post-build actions
    // -------------------------------------------------------------------------
    post {
        success {
            echo "SUCCESS: Backend deployed! Image: ${FULL_IMAGE}"
        }
        failure {
            echo "FAILED: Backend pipeline failed. Check the stage logs above."
        }
        always {
            sh 'docker image prune -f || true'
        }
    }

} // end pipeline
