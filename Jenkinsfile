```groovy
// =============================================================================
// Jenkinsfile — Backend (Spring Boot)
// Windows Jenkins Agent
//
// Pipeline:
// Clean Compile → Build JAR → Docker Image → Push ECR → Deploy EC2
// =============================================================================

pipeline {

    agent any

    environment {

        AWS_REGION     = 'us-east-1'
        AWS_ACCOUNT_ID = '888577028066'

        ECR_REPO       = 'fanverseeeee'
        IMAGE_TAG      = 'backend-latest'

        ECR_REGISTRY   = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        FULL_IMAGE     = "${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}"

        EC2_USER       = 'ubuntu'
        EC2_HOST       = 'ec2-34-227-7-122.compute-1.amazonaws.com'

        CONTAINER_NAME = 'backend'
        DOCKER_NETWORK = 'fanverse-network'

        HOST_PORT      = '6001'
        CONTAINER_PORT = '8085'

        S3_BUCKET      = 's3-test-01-navaneeth'

        // IMPORTANT:
        // Put your NEW rotated AWS credentials here.
        // Do not use credentials that have already been exposed.

        AWS_ACCESS_KEY_ID     = 'YOUR_NEW_AWS_ACCESS_KEY'
        AWS_SECRET_ACCESS_KEY = 'YOUR_NEW_AWS_SECRET_KEY'
    }


    stages {

        // =====================================================================
        // STAGE 1 — Clean & Compile
        // =====================================================================

        stage('Clean & Compile') {

            steps {

                bat '''
                    echo ========================================
                    echo Cleaning and compiling backend
                    echo ========================================
                    
                    dir

                    mvnw.cmd clean compile -B

                    echo ========================================
                    echo COMPILE COMPLETE
                    echo ========================================
                '''
            }
        }


        // =====================================================================
        // STAGE 2 — Build JAR
        // =====================================================================

        stage('Build JAR') {

            steps {

                bat '''
                    echo ========================================
                    echo Packaging Spring Boot JAR
                    echo ========================================

                    mvnw.cmd package -DskipTests -B

                    echo ========================================
                    echo JAR BUILD COMPLETE
                    echo ========================================

                    dir target
                '''
            }

            post {

                success {

                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }


        // =====================================================================
        // STAGE 3 — Build Docker Image
        // =====================================================================

        stage('Build Docker Image') {

            steps {

                bat """
                    echo ========================================
                    echo Building Docker Image
                    echo ========================================

                    docker build -t ${FULL_IMAGE} .

                    echo ========================================
                    echo Docker image built:
                    echo ${FULL_IMAGE}
                    echo ========================================
                """
            }
        }


        // =====================================================================
        // STAGE 4 — Push Docker Image to ECR
        // =====================================================================

        stage('Push to ECR') {

            steps {

                bat """

                    echo ========================================
                    echo Logging into AWS ECR
                    echo ========================================

                    set AWS_ACCESS_KEY_ID=%AWS_ACCESS_KEY_ID%
                    set AWS_SECRET_ACCESS_KEY=%AWS_SECRET_ACCESS_KEY%
                    set AWS_DEFAULT_REGION=${AWS_REGION}

                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

                    if %ERRORLEVEL% NEQ 0 (
                        echo ECR LOGIN FAILED
                        exit /b 1
                    )

                    echo ========================================
                    echo Pushing Docker Image to ECR
                    echo ========================================

                    docker push ${FULL_IMAGE}

                    if %ERRORLEVEL% NEQ 0 (
                        echo ECR PUSH FAILED
                        exit /b 1
                    )

                    echo ========================================
                    echo ECR PUSH COMPLETE
                    echo ========================================

                """
            }
        }


        // =====================================================================
        // STAGE 5 — Deploy to EC2
        // =====================================================================

        stage('Deploy to EC2') {

            steps {

                // Only the EC2 PEM key comes from Jenkins Credentials.
                // AWS credentials come from the environment above.

                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'EC2_PEM_KEY',
                        keyFileVariable: 'PEM_FILE'
                    )
                ]) {


                    // =========================================================
                    // Create deployment script
                    // =========================================================

                    writeFile(
                        file: 'deploy_backend.sh',
                        text: """#!/bin/bash

set -e

echo "========================================"
echo "AWS Configuration"
echo "========================================"

export AWS_ACCESS_KEY_ID='${AWS_ACCESS_KEY_ID}'
export AWS_SECRET_ACCESS_KEY='${AWS_SECRET_ACCESS_KEY}'
export AWS_REGION='${AWS_REGION}'
export AWS_S3_BUCKET_NAME='${S3_BUCKET}'


echo "========================================"
echo "Logging into AWS ECR"
echo "========================================"

aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}


echo "========================================"
echo "Pulling latest backend image"
echo "========================================"

docker pull ${FULL_IMAGE}


echo "========================================"
echo "Stopping old backend container"
echo "========================================"

docker stop ${CONTAINER_NAME} 2>/dev/null || true

docker rm -f ${CONTAINER_NAME} 2>/dev/null || true


echo "========================================"
echo "Cleaning unused Docker images"
echo "========================================"

docker image prune -f || true


echo "========================================"
echo "Creating Docker network if required"
echo "========================================"

docker network inspect ${DOCKER_NETWORK} >/dev/null 2>&1 || docker network create ${DOCKER_NETWORK}


echo "========================================"
echo "Starting new backend container"
echo "========================================"

docker run -d \\
    --name ${CONTAINER_NAME} \\
    --network ${DOCKER_NETWORK} \\
    --restart unless-stopped \\
    -p ${HOST_PORT}:${CONTAINER_PORT} \\
    -e AWS_ACCESS_KEY_ID="\$AWS_ACCESS_KEY_ID" \\
    -e AWS_SECRET_ACCESS_KEY="\$AWS_SECRET_ACCESS_KEY" \\
    -e AWS_REGION="\$AWS_REGION" \\
    -e AWS_S3_BUCKET_NAME="\$AWS_S3_BUCKET_NAME" \\
    ${FULL_IMAGE}


echo "========================================"
echo "Checking backend container"
echo "========================================"

docker ps --filter name=${CONTAINER_NAME}


echo "========================================"
echo "Backend deployment complete"
echo "========================================"

"""
                    )


                    // =========================================================
                    // Copy deployment script to EC2
                    // =========================================================

                    bat """

                        echo ========================================
                        echo Copying deployment script to EC2
                        echo ========================================

                        scp -o StrictHostKeyChecking=no -i "%PEM_FILE%" deploy_backend.sh ${EC2_USER}@${EC2_HOST}:/tmp/deploy_backend.sh

                    """


                    // =========================================================
                    // Execute deployment script on EC2
                    // =========================================================

                    bat """

                        echo ========================================
                        echo Executing deployment on EC2
                        echo ========================================

                        ssh -o StrictHostKeyChecking=no -i "%PEM_FILE%" ${EC2_USER}@${EC2_HOST} "export AWS_ACCESS_KEY_ID=%AWS_ACCESS_KEY_ID% && export AWS_SECRET_ACCESS_KEY=%AWS_SECRET_ACCESS_KEY% && export AWS_REGION=${AWS_REGION} && export AWS_S3_BUCKET_NAME=${S3_BUCKET} && chmod +x /tmp/deploy_backend.sh && bash /tmp/deploy_backend.sh"

                    """


                    // =========================================================
                    // Remove temporary deployment script
                    // =========================================================

                    bat """

                        if exist deploy_backend.sh del deploy_backend.sh

                    """
                }
            }
        }
    }


    // =========================================================================
    // POST ACTIONS
    // =========================================================================

    post {

        success {

            echo """
========================================
SUCCESS
Backend deployed successfully

Docker Image:
${FULL_IMAGE}

EC2:
${EC2_HOST}

Container:
${CONTAINER_NAME}

Port:
${HOST_PORT}:${CONTAINER_PORT}

S3 Bucket:
${S3_BUCKET}

========================================
"""
        }


        failure {

            echo """
========================================
FAILED

Backend pipeline failed.
Check the stage logs above.

========================================
"""
        }


        always {

            bat '''
                docker image prune -f || exit /b 0
            '''
        }
    }
}
```
