def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
    containerTemplate(name: 'maven', image: 'maven:3.6.1-jdk-8', ttyEnabled: true, command: 'cat'),
    containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true)
],
volumes: [
    hostPathVolume(mountPath: '/root/.m2/repository', hostPath: '/root/.m2/repository'),
  hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
]) {

    node(label) {
        def myRepo = checkout scm
        def gitCommit = myRepo.GIT_COMMIT
        def gitBranch = myRepo.GIT_BRANCH
        def branchName = sh(script: "echo $gitBranch | cut -c8-", returnStdout: true)
        def shortGitCommit = "${gitCommit[0..10]}"
        def previousGitCommit = sh(script: "git rev-parse ${gitCommit}~", returnStdout: true)
        def gitCommitCount = sh(script: "git rev-list --all --count", returnStdout: true)
        def regURL = "registry-sonatype-nexus.pipeline:8081/docker-internal"
        def regNamespace = "paruff"
        def artifactID = sh(script: "grep '<artifactId>' pom.xml | head -n 1 | sed -e 's/artifactId//g' | sed -e 's/\\s*[<>/]*//g' | tr -d '\\r\\n'", returnStdout: true)
        def POMversion = sh(script: "grep '<version>' pom.xml | head -n 1 | sed -e 's/version//g' | sed -e 's/\\s*[<>/]*//g' | tr -d '\\r\\n'", returnStdout: true)
 
        try {
        notifySlack()

        stage('Maven project') {
            container('maven') {

                stage('install hygieia-core') {
                    git url: 'https://github.com/Hygieia/hygieia-core.git'
                    sh 'mvn -B  clean install' 
                }
                
                stage('build hygieia') {
                    git url: 'https://github.com/paruff/hygieia-api.git', branch: 'develop'
                    sh 'mvn -B  clean install' 
                }
                
            }
        }
        stage('Create Docker images') {
          container('docker') {
         withCredentials([[$class: 'UsernamePasswordMultiBinding',
           credentialsId: 'dockerhub',
           usernameVariable: 'DOCKER_REG_USER',
           passwordVariable: 'DOCKER_REG_PASSWORD']]) {
          sh """
            echo $branchName          
            docker login -u ${DOCKER_REG_USER}  -p ${DOCKER_REG_PASSWORD}
            docker system prune -f
        cd UI
            docker build -t ${regNamespace}/hygieia-api .
            docker tag ${regNamespace}/hygieia-ui ${regNamespace}/hygieia-api:3.0.2.${BUILD_NUMBER}
            docker push ${regNamespace}/hygieia-api
        
            """
         }
      }
    }


    
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        notifySlack(currentBuild.result)
    }
    }
}

def notifySlack(String buildStatus = 'STARTED') {
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'

    def color

    if (buildStatus == 'STARTED') {
        color = '#D4DADF'
    } else if (buildStatus == 'SUCCESS') {
        color = '#BDFFC3'
    } else if (buildStatus == 'UNSTABLE') {
        color = '#FFFE89'
    } else {
        color = '#FF9FA1'
    }

    def msg = "${buildStatus}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}"

    slackSend(color: color, message: msg)
}
