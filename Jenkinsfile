pipeline {
    agent any

    parameters {
        string(
            name: 'AWS_REGION',
            defaultValue: 'us-east-1',
            description: 'AWS Region'
        )

        string(
            name: 'AWS_ACCOUNT_ID',
            defaultValue: '888577028066',
            description: 'AWS Account ID'
        )

        string(
            name: 'ECR_REPO_NAME',
            defaultValue: 'fanverseeeee',
            description: 'ECR Repository Name'
        )

        string(
            name: 'IMAGE_TAG',
            defaultValue: 'backend-latest',
            description: 'Docker Image Tag'
        )

        string(
            name: 'EC2_HOST',
            defaultValue: 'ec2-34-227-7-122.compute-1.amazonaws.com',
            description: 'EC2 Host'
        )

        string(
            name: 'EC2_USER',
            defaultValue: 'ubuntu',
            description: 'EC2 SSH User'
        )

        string(
            name: 'PEM_PATH',
            defaultValue: 'C:\\Users\\Administrator\\Downloads\\testUbantu.pem',
            description: 'EC2 PEM file path'
        )

        string(
            name: 'CONTAINER_NAME',
            defaultValue: 'backend',
            description: 'Backend Docker container name'
        )

        string(
            name: 'DOCKER_NETWORK',
            defaultValue: 'fanverse-network',
            description: 'Docker network'
        )

        string(
            name: 'HOST_PORT',
            defaultValue: '6001',
            description: 'EC2 host port'
        )

        string(
            name: 'CONTAINER_PORT',
            defaultValue: '8085',
            description: 'Backend container port'
        )

        string(
            name: 'S3_BUCKET_NAME',
            defaultValue: 's3-test-01-navaneeth',
            description: 'S3 bucket'
        )
    }

    environment {

        AWS_REGION = "${params.AWS_REGION}"

        AWS_DEFAULT_REGION = "${params.AWS_REGION}"

        AWS_ACCOUNT_ID = "${params.AWS_ACCOUNT_ID}"

        ECR_REPO_NAME = "${params.ECR_REPO_NAME}"

        IMAGE_TAG = "${params.IMAGE_TAG}"

        ECR_REGISTRY = "${params.AWS_ACCOUNT_ID}.dkr.ecr.${params.AWS_REGION}.amazonaws.com"

        FULL_IMAGE = "${params.AWS_ACCOUNT_ID}.dkr.ecr.${params.AWS_REGION}.amazonaws.com/${params.ECR_REPO_NAME}:${params.IMAGE_TAG}"

        EC2_HOST = "${params.EC2_HOST}"

        EC2_USER = "${params.EC2_USER}"

        PEM_PATH = "${params.PEM_PATH}"

        CONTAINER_NAME = "${params.CONTAINER_NAME}"

        DOCKER_NETWORK = "${params.DOCKER_NETWORK}"

        HOST_PORT = "${params.HOST_PORT}"

        CONTAINER_PORT = "${params.CONTAINER_PORT}"

        S3_BUCKET_NAME = "${params.S3_BUCKET_NAME}"

        AWS_SHARED_CREDENTIALS_FILE = 'C:\\Users\\Administrator\\.aws\\credentials'

        AWS_CONFIG_FILE = 'C:\\Users\\Administrator\\.aws\\config'

        USERPROFILE = 'C:\\Users\\Administrator'

        HOME = 'C:\\Users\\Administrator'
    }

    stages {

        // ============================================================
        // 1. CLEAN & COMPILE
        // ============================================================

        stage('Clean & Compile') {

            steps {

                echo '========================================'
                echo 'Cleaning and compiling backend'
                echo '========================================'

                bat '''
                    @echo off

                    call mvnw.cmd clean compile -B

                    if %ERRORLEVEL% NEQ 0 (
                        echo Maven compilation FAILED
                        exit /b 1
                    )

                    echo Maven compilation SUCCESSFUL
                '''
            }
        }


        // ============================================================
        // 2. BUILD JAR
        // ============================================================

        stage('Build JAR') {

            steps {

                echo '========================================'
                echo 'Building Spring Boot JAR'
                echo '========================================'

                bat '''
                    @echo off

                    call mvnw.cmd package -DskipTests -B

                    if %ERRORLEVEL% NEQ 0 (
                        echo Maven package FAILED
                        exit /b 1
                    )

                    echo.
                    echo JAR BUILD SUCCESSFUL

                    dir target\\*.jar
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


        // ============================================================
        // 3. VERIFY AWS
        // ============================================================

        stage('Verify AWS Configuration') {

            steps {

                echo '========================================'
                echo 'Verifying AWS Configuration'
                echo '========================================'

                bat """
                    @echo off

                    set "AWS_SHARED_CREDENTIALS_FILE=C:\\Users\\Administrator\\.aws\\credentials"
                    set "AWS_CONFIG_FILE=C:\\Users\\Administrator\\.aws\\config"
                    set "USERPROFILE=C:\\Users\\Administrator"
                    set "HOME=C:\\Users\\Administrator"

                    echo AWS Region:
                    echo ${AWS_REGION}

                    echo.
                    echo Checking AWS identity...

                    aws sts get-caller-identity

                    if %ERRORLEVEL% NEQ 0 (
                        echo.
                        echo AWS AUTHENTICATION FAILED
                        echo Check C:\\Users\\Administrator\\.aws\\credentials
                        exit /b 1
                    )

                    echo.
                    echo AWS AUTHENTICATION SUCCESSFUL
                """
            }
        }


        // ============================================================
        // 4. BUILD DOCKER IMAGE
        // ============================================================

        stage('Build Docker Image') {

            steps {

                echo '========================================'
                echo 'Building Docker Image'
                echo '========================================'

                bat """
                    @echo off

                    echo Building:
                    echo ${FULL_IMAGE}

                    docker build -t ${FULL_IMAGE} .

                    if %ERRORLEVEL% NEQ 0 (
                        echo Docker build FAILED
                        exit /b 1
                    )

                    echo.
                    echo DOCKER BUILD SUCCESSFUL
                """
            }
        }


        // ============================================================
        // 5. LOGIN TO ECR
        // ============================================================

        stage('Login to ECR') {

            steps {

                echo '========================================'
                echo 'Logging into Amazon ECR'
                echo '========================================'

                bat """
                    @echo off

                    set "AWS_SHARED_CREDENTIALS_FILE=C:\\Users\\Administrator\\.aws\\credentials"
                    set "AWS_CONFIG_FILE=C:\\Users\\Administrator\\.aws\\config"
                    set "USERPROFILE=C:\\Users\\Administrator"
                    set "HOME=C:\\Users\\Administrator"

                    echo Getting ECR login password...

                    aws ecr get-login-password --region ${AWS_REGION} > ecr_password.txt

                    if %ERRORLEVEL% NEQ 0 (
                        echo ECR AUTHORIZATION FAILED
                        if exist ecr_password.txt del /f /q ecr_password.txt
                        exit /b 1
                    )

                    type ecr_password.txt | docker login --username AWS --password-stdin ${ECR_REGISTRY}

                    if %ERRORLEVEL% NEQ 0 (
                        echo DOCKER ECR LOGIN FAILED
                        if exist ecr_password.txt del /f /q ecr_password.txt
                        exit /b 1
                    )

                    if exist ecr_password.txt del /f /q ecr_password.txt

                    echo.
                    echo ECR LOGIN SUCCESSFUL
                """
            }
        }


        // ============================================================
        // 6. PUSH TO ECR
        // ============================================================

        stage('Push to ECR') {

            steps {

                echo '========================================'
                echo 'Pushing Backend Image to ECR'
                echo '========================================'

                bat """
                    @echo off

                    set "AWS_SHARED_CREDENTIALS_FILE=C:\\Users\\Administrator\\.aws\\credentials"
                    set "AWS_CONFIG_FILE=C:\\Users\\Administrator\\.aws\\config"
                    set "USERPROFILE=C:\\Users\\Administrator"
                    set "HOME=C:\\Users\\Administrator"

                    echo.
                    echo Image:
                    echo ${FULL_IMAGE}

                    docker push ${FULL_IMAGE}

                    if %ERRORLEVEL% NEQ 0 (
                        echo.
                        echo ECR PUSH FAILED
                        exit /b 1
                    )

                    echo.
                    echo ========================================
                    echo ECR PUSH SUCCESSFUL
                    echo ========================================
                """
            }
        }


        // ============================================================
        // 7. DEPLOY TO EC2
        // ============================================================

        stage('Deploy to EC2') {

            steps {

                echo '========================================'
                echo 'Deploying Backend to EC2'
                echo '========================================'

                script {

                    writeFile(
                        file: 'deploy_backend.sh',
                        text: """#!/bin/bash

set -e

echo "========================================"
echo "FANVERSE BACKEND DEPLOYMENT"
echo "========================================"

echo "EC2 Host:"
hostname

echo ""
echo "Logging into ECR..."

aws ecr get-login-password --region ${AWS_REGION} | sudo docker login --username AWS --password-stdin ${ECR_REGISTRY}

echo ""
echo "Pulling backend image..."

sudo docker pull ${FULL_IMAGE}

echo ""
echo "Stopping old backend container..."

if sudo docker ps -q -f name=${CONTAINER_NAME} | grep -q .; then
    sudo docker stop ${CONTAINER_NAME}
fi

echo ""
echo "Removing old backend container..."

if sudo docker ps -aq -f name=${CONTAINER_NAME} | grep -q .; then
    sudo docker rm ${CONTAINER_NAME}
fi

echo ""
echo "Creating Docker network..."

sudo docker network inspect ${DOCKER_NETWORK} >/dev/null 2>&1 || sudo docker network create ${DOCKER_NETWORK}

echo ""
echo "Starting backend container..."

sudo docker run -d \\
    --name ${CONTAINER_NAME} \\
    --network ${DOCKER_NETWORK} \\
    --restart unless-stopped \\
    -p ${HOST_PORT}:${CONTAINER_PORT} \\
    -e AWS_REGION=${AWS_REGION} \\
    -e AWS_S3_BUCKET_NAME=${S3_BUCKET_NAME} \\
    ${FULL_IMAGE}

echo ""
echo "Waiting for backend..."

sleep 10

echo ""
echo "========================================"
echo "BACKEND CONTAINER STATUS"
echo "========================================"

sudo docker ps --filter name=${CONTAINER_NAME}

echo ""
echo "========================================"
echo "BACKEND DEPLOYMENT COMPLETE"
echo "========================================"

"""
                    )

                    bat """
                        @echo off

                        echo.
                        echo Checking PEM file...

                        if not exist "${PEM_PATH}" (
                            echo ERROR: PEM file not found:
                            echo ${PEM_PATH}
                            exit /b 1
                        )

                        echo PEM file found.

                        echo.
                        echo Copying deployment script to EC2...

                        scp -o StrictHostKeyChecking=no -i "${PEM_PATH}" deploy_backend.sh ${EC2_USER}@${EC2_HOST}:/home/${EC2_USER}/deploy_backend.sh

                        if %ERRORLEVEL% NEQ 0 (
                            echo SCP FAILED
                            exit /b 1
                        )

                        echo.
                        echo Executing deployment script on EC2...

                        ssh -o StrictHostKeyChecking=no -i "${PEM_PATH}" ${EC2_USER}@${EC2_HOST} "chmod +x /home/${EC2_USER}/deploy_backend.sh && /home/${EC2_USER}/deploy_backend.sh"

                        if %ERRORLEVEL% NEQ 0 (
                            echo EC2 DEPLOYMENT FAILED
                            exit /b 1
                        )

                        echo.
                        echo ========================================
                        echo EC2 DEPLOYMENT SUCCESSFUL
                        echo ========================================
                    """
                }
            }
        }


        // ============================================================
        // 8. VERIFY EC2 CONTAINER
        // ============================================================

        stage('Verify Deployment') {

            steps {

                echo '========================================'
                echo 'Verifying Backend on EC2'
                echo '========================================'

                bat """
                    @echo off

                    ssh -o StrictHostKeyChecking=no -i "${PEM_PATH}" ${EC2_USER}@${EC2_HOST} "sudo docker ps --filter name=${CONTAINER_NAME}"

                    if %ERRORLEVEL% NEQ 0 (
                        echo Remote verification FAILED
                        exit /b 1
                    )

                    echo.
                    echo ========================================
                    echo BACKEND IS RUNNING
                    echo ========================================

                    echo.
                    echo EC2:
                    echo ${EC2_HOST}

                    echo.
                    echo Backend port:
                    echo ${HOST_PORT}
                """
            }
        }
    }


    // ============================================================
    // POST ACTIONS
    // ============================================================

    post {

        always {

            bat """
                @echo off

                if exist ecr_password.txt (
                    del /f /q ecr_password.txt
                )

                if exist deploy_backend.sh (
                    del /f /q deploy_backend.sh
                )

                docker image prune -f || exit /b 0
            """
        }


        success {

            echo '========================================'
            echo 'FANVERSE BACKEND PIPELINE SUCCESS'
            echo '========================================'

            echo "ECR Image: ${FULL_IMAGE}"

            echo "EC2 Host: ${EC2_HOST}"

            echo "Container: ${CONTAINER_NAME}"

            echo "Port: ${HOST_PORT}:${CONTAINER_PORT}"

            echo '========================================'
        }


        failure {

            echo '========================================'
            echo 'FANVERSE BACKEND PIPELINE FAILED'
            echo '========================================'

            echo 'Check the failed Jenkins stage.'

            echo '========================================'
        }
    }
}
