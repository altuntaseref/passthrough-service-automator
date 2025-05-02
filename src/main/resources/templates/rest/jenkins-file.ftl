// Jenkinsfile

// Pipeline genel ayarları
properties([
// Eski build'leri otomatik sil (isteğe bağlı ama önerilir)
buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5')),
// Aynı anda sadece bir build çalıştır (isteğe bağlı)
disableConcurrentBuilds()
])

pipeline {
// Jenkins'in bu pipeline'ı nerede çalıştıracağını belirtir.
// Şimdilik Jenkins'in kendi üzerinde (built-in node) çalıştıracağız.
agent any

// Pipeline içinde kullanılacak ortam değişkenleri
environment {
// Docker Hub kullanıcı adınız ve imaj adı
DOCKERHUB_CREDENTIALS_ID = 'docker-hub' // <<< --- DEĞİŞTİR: Jenkins'te oluşturduğunuz Docker Hub credential ID'si --- >>>
DOCKER_IMAGE_NAME      = 'altuntasserf/${projectName}' // <<< --- DEĞİŞTİR: Docker Hub kullanıcı adı/imaj adı --- >>>
KUBERNETES_NAMESPACE   = 'default' // <<< --- DEĞİŞTİR (İsteğe bağlı): Uygulamayı dağıtacağınız K8s namespace (örn: spring-boot-app) --- >>>
KUBERNETES_DEPLOYMENT  = '${projectName}-deployment' // <<< --- DEĞİŞTİR: deployment.yaml'daki Deployment adı --- >>>
}

// Build sırasında kullanılacak araçlar (Manage Jenkins -> Tools'da tanımlananlar)
tools {
maven 'MAVEN' // <<< --- DEĞİŞTİR: Jenkins'te Tools'da tanımladığınız Maven kurulumunun adı --- >>>
jdk 'JDK-17'      // <<< --- DEĞİŞTİR: Jenkins'te Tools'da tanımladığınız JDK kurulumunun adı --- >>>
// Git genellikle PATH'den bulunur, tanımlamaya gerek olmayabilir
}

stages {
// 1. Aşama: Kodu Çekme (Jenkins bunu 'Pipeline script from SCM' ile otomatik yapar, ama göstermek için eklenebilir)
// stage('Checkout') {
//     steps {
//         echo "Koddaki değişiklikler alınıyor..."
//         checkout scm
//     }
// }

// 2. Aşama: Uygulamayı Derleme ve Paketleme
stage('Build') {
steps {
echo "Maven ile uygulama derleniyor ve paketleniyor..."
// Windows için 'bat', Linux/macOS için 'sh' kullanılır
bat "mvn clean package -DskipTests"
}
}

// 3. Aşama: Docker İmajı Oluşturma
stage('Build Docker Image') {
steps {
echo "Docker imajı oluşturuluyor..."
// Dockerfile'ın bulunduğu dizin genellikle workspace köküdür.
// Dockerfile'daki multi-stage build'i kullanırız.
script {
// Docker imajını build et ve etiketle (tag)
// BUILD_NUMBER Jenkins tarafından otomatik sağlanan bir ortam değişkenidir.
def dockerImage = docker.build("${dockerImageName}:${buildNumber}", ".")
}
}
}

// 4. Aşama: Docker İmajını Yükleme (Push)
stage('Push Docker Image') {
steps {
echo "Docker imajı Docker Hub'a yükleniyor..."
script {
// Docker Hub'a login ol ve imajı push et
docker.withRegistry('https://registry.hub.docker.com', env.DOCKERHUB_CREDENTIALS_ID) {
// Daha önce build edilen imajı referans alıyoruz
docker.image("${dockerImageName}:${buildNumber}").push()
// 'latest' tag'ini de push edebiliriz (isteğe bağlı)
docker.image("${dockerImageName}:${buildNumber}").push('latest')
}
}
}
}

// 5. Aşama: Kubernetes'e Dağıtma (Deploy)
stage('Deploy to Kubernetes') {
steps {
echo "Uygulama Kubernetes'e dağıtılıyor..."
// Jenkins'in çalıştığı makinede kubectl'in kurulu ve Docker Desktop kümesine
// bağlanacak şekilde yapılandırılmış olduğunu varsayıyoruz.
// Deployment'ın imajını güncelliyoruz.
withCredentials([file(credentialsId: 'kubeconfig-dockerdesktop', variable: 'KUBECONFIG_FILE')]) {
script {
// Ortam değişkenini kullanarak kubectl komutlarını çalıştır
// Windows'ta KUBECONFIG ortam değişkenini set etmek için 'set' kullanılır
bat """
set KUBECONFIG=%KUBECONFIG_FILE%
kubectl set image deployment/${kubernetesDeployment} spring-boot-container=${dockerImageName}:${buildNumber} -n ${kubernetesNamespace}
kubectl rollout status deployment/${kubernetesDeployment} -n ${kubernetesNamespace}
set KUBECONFIG=
"""
// Linux/macOS agent kullanılsaydı:
// sh '''
// export KUBECONFIG=$KUBECONFIG_FILE
// kubectl set image deployment/${kubernetesDeployment} spring-boot-container=${dockerImageName}:${buildNumber} -n ${kubernetesNamespace}
// kubectl rollout status deployment/${kubernetesDeployment} -n ${kubernetesNamespace}
// '''
}
}
}
}
}

// Pipeline bittikten sonra yapılacaklar (başarılı, başarısız vb.)
post {
always {
echo 'Pipeline tamamlandı.'
// Çalışma alanını temizle (Workspace Cleanup eklentisi gerektirir)
cleanWs()
}
success {
echo 'Pipeline başarıyla tamamlandı!'
// Başarılı olunca bildirim gönderilebilir (E-posta, Slack vb. eklentilerle)
}
failure {
echo 'Pipeline başarısız oldu!'
// Başarısız olunca bildirim gönderilebilir
}
}
}