pipeline {
    agent {
        node {
            label 'dind'
        }
    }
    tools {
        git 'git'
        jdk 'jdk-11.0.10'
        maven 'Maven 3.6.3'
    }
    parameters {
        string(
                name: 'MODIFIED_BRANCH',
                description: 'Ветка, которая была изменена',
                defaultValue: ''
        )
        string(
                name: 'MODIFIED_BRANCH_COMMIT_HASH',
                description: 'Последний коммит в MODIFIED_BRANCH',
                defaultValue: ''
        )
        string(
                name: 'TUZ_CRED_ID',
                description: 'Креды ТУЗа',
                defaultValue: '182ca036-bf73-47aa-bf81-b8ac100087ea'
        )
        string(
                name: 'BITBUCKET_REPO_URL',
                description: 'Репо bitbucket',
                defaultValue: 'https://domain.com'
        )
        string(
                name: 'NEXUS_URL',
                description: 'Nexus URL',
                defaultValue: 'http://domain.com:8081/nexus'
        )
        string(
                name: 'JENKINS_URL',
                description: 'Jenkis URL ',
                defaultValue: 'https://domain.com/jenkins'
        )
        string(
                name: 'DRAFT_REPO_SSH_URL',
                description: 'Draft repo ssh URL',
                defaultValue: 'ssh://git@domain.com:7999/pt/payment-document-drafts.git'
        )
        string(
                name: 'RELEASE_BRANCH_ALPHA',
                description: 'Release branch Alpha',
                defaultValue: 'release/alpha'
        )
        string(
                name: 'DRAFT_REPO_GIT_URL',
                description: 'Draft repo git URL',
                defaultValue: 'https://domain.com/scm/pt/payment-document-drafts.git'
        )
        string(
                name: 'JIRA_ISSUE',
                description: 'Jira issue',
                defaultValue: 'ALATAU-123'
        )
        booleanParam(
                name: 'AWAIT_BUILD_JENKINS_JOB',
                defaultValue: true,
                description: 'Ждать завершения джобы, которую запустила данная джоба'
        )
        booleanParam(
                name: 'THIS_IS_JENKINS_PIPELINE',
                defaultValue: true,
                description: 'Скрипт подготовки релиза запускается из Jenkins'
        )
        string(
                name: 'JIRA_URL',
                description: 'Jira URL',
                defaultValue: 'https://domain.com'
        )
    }
    options {
        durabilityHint('PERFORMANCE_OPTIMIZED')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        skipStagesAfterUnstable()
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    setBuildName()
                }
            }
        }
        stage('Tools testing') {
            steps {
                script {
                    sh 'pwd'
                    sh 'ls -la'
                    sh 'mvn -v'
                    sh 'java -version'
                    sh 'javac -version'
                    sh 'git --version'
                }
            }
        }
        stage('Run release branch preparation script') {
            steps {
                script {
                    dir("utils/scripts") {
                        withCredentials([usernamePassword(credentialsId: params.TUZ_CRED_ID, passwordVariable: 'TUZ_PASSWORD', usernameVariable: 'TUZ_USERNAME')]) {
                            def args = "${TUZ_USERNAME} ${TUZ_PASSWORD} ${params.MODIFIED_BRANCH} ${params.NEXUS_URL} ${params.JENKINS_URL} ${params.DRAFT_REPO_SSH_URL} ${params.THIS_IS_JENKINS_PIPELINE} ${params.RELEASE_BRANCH_ALPHA} ${params.DRAFT_REPO_GIT_URL} ${params.JIRA_ISSUE} ${params.AWAIT_BUILD_JENKINS_JOB}"

                            echo "Параметры для запуска скрипта подготовки релизной ветки:"
                            echo args

                            sh "export GROOVY_HOME=./groovy-4.0.0-rc-1 TUZ_USERNAME=${TUZ_USERNAME} TUZ_PASSWORD=${TUZ_PASSWORD}; ../../groovy-4.0.0-rc-1/bin/groovy payment-document-drafts-release-preparation.groovy"
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            deleteDir()
        }
        success {
            script {
                setBuildResult 'SUCCESS'
                notifyBitbucket()
                printPASSED()
            }
        }
        failure {
            script {
                setBuildResult 'FAILED'
                notifyBitbucket()
                printFAILLED()
            }
        }
    }
}

def notifyBitbucket() {
    step([$class                       : 'StashNotifier',
          commitSha1                   : params.MODIFIED_BRANCH_COMMIT_HASH,
          credentialsId                : params.TUZ_CRED_ID,
          disableInprogressNotification: false,
          considerUnstableAsSuccess    : false,
          ignoreUnverifiedSSLPeer      : true,
          includeBuildNumberInKey      : false,
          prependParentProjectKey      : false,
          projectKey                   : 'PT',
          stashServerBaseUrl           : params.BITBUCKET_REPO_URL])
}

def setBuildName() {
    currentBuild.displayName = "${currentBuild.displayName}-Build distro from ${params.MODIFIED_BRANCH} on ${params.MODIFIED_BRANCH_COMMIT_HASH}"
}

def setBuildResult(String result) {
    currentBuild.result = result
}

def disableMavenSslCheck() {
    "-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
}

def printFAILLED() {
    println "███████╗     █████╗     ██╗    ██╗         ███████╗    ██████╗\n" +
            "██╔════╝    ██╔══██╗    ██║    ██║         ██╔════╝    ██╔══██╗\n" +
            "█████╗      ███████║    ██║    ██║         █████╗      ██║  ██║\n" +
            "██╔══╝      ██╔══██║    ██║    ██║         ██╔══╝      ██║  ██║\n" +
            "██║         ██║  ██║    ██║    ███████╗    ███████╗    ██████╔╝\n" +
            "╚═╝         ╚═╝  ╚═╝    ╚═╝    ╚══════╝    ╚══════╝    ╚═════╝"
}

def printPASSED() {
    println "██████╗      █████╗     ███████╗    ███████╗    ███████╗    ██████╗ \n" +
            "██╔══██╗    ██╔══██╗    ██╔════╝    ██╔════╝    ██╔════╝    ██╔══██╗\n" +
            "██████╔╝    ███████║    ███████╗    ███████╗    █████╗      ██║  ██║\n" +
            "██╔═══╝     ██╔══██║    ╚════██║    ╚════██║    ██╔══╝      ██║  ██║\n" +
            "██║         ██║  ██║    ███████║    ███████║    ███████╗    ██████╔╝\n" +
            "╚═╝         ╚═╝  ╚═╝    ╚══════╝    ╚══════╝    ╚══════╝    ╚═════╝"
}