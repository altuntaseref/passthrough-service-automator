// Jenkinsfile

// Pipeline genel ayarları
properties([
// Eski build'leri otomatik sil
buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5')),
// Aynı anda sadece bir build çalıştır
disableConcurrentBuilds(),
// İlk kez pipeline oluşturulduğunda otomatik build başlat
pipelineTriggers([
pollSCM('H/5 * * * *')  // Her 5 dakikada bir değişiklik kontrolü
])
])

pipeline {
// Jenkins'in bu pipeline'ı nerede çalıştıracağını belirtir.
agent any

// Pipeline içinde kullanılacak ortam değişkenleri
environment {
DOCKERHUB_CREDENTIALS_ID = 'docker-hub'
PROJECT_NAME = '${projectName}'
DOCKER_IMAGE_NAME        = 'altuntasserf/${projectName}'
KUBERNETES_NAMESPACE     = 'default'
KUBERNETES_DEPLOYMENT    = '${projectName}-deployment'
BASE_IMAGE_NAME          = 'altuntasserf/microservice-base'
}

// Build sırasında kullanılacak araçlar
${jenkistxt}