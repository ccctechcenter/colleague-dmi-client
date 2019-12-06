#!groovy
@Library('jenkinsLib') _
currentBuild.displayName = "#${env.BUILD_NUMBER}-${branch_name}"
pipeline {
    agent { node { label 'build-slave' } }
    environment {
        channel = "#college-adaptor"
        service = "colleague-dmi-client"
    }
    stages {
        stage('build') {
            steps {
                script {
                    build_action = "Build"
                    ceBuild.mvnBuild('mvn -Dmaven.test.failure.ignore-true clean package')

                    //publish coverage report
                    jacoco classPattern: '**/target/classes', execPattern: '**/target/jacoco.exec'
                }
            }
        }
        stage('publish') {
            steps {
                script {
                    pom = readMavenPom file: 'pom.xml'
                    artifact_version = pom.version
                    echo "DEBUG: artifact_version: $artifact_version"
                    if (env.BRANCH_NAME == "master" && !(artifact_version =~ /SNAPSHOT/)) { //publish to github package registry  if we're on master, and we're not using a snapshot version
                        ceBuild.mvnBuild('mvn deploy')
                    }
                }
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml'
        }
        failure {
            script {
                ceDeploy.slackNotify(env.channel, "danger", build_action + " failed", env.service, "N/A", "N/A", artifact_version)
            }
        }
        success {
            script {
                ceDeploy.slackNotify(env.channel, "good", build_action + " success", env.service, "N/A", "N/A", artifact_version)
            }
        }
        unstable {
            script {
                ceDeploy.slackNotify(env.channel, "warning", build_action + " had test failures", env.service, "N/A", "N/A", artifact_version)
            }
        }
    }
}
