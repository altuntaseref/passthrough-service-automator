document.getElementById("projectForm").addEventListener("submit", async function(event) {
    event.preventDefault(); // Sayfanın yenilenmesini engeller

    // Form verilerini alalım
    const formData = {
        projectName: document.getElementById("projectName").value,
        projectType: document.getElementById("projectType").value,
        groupId: document.getElementById("groupId").value,
        artifactId: document.getElementById("artifactId").value,
        outputPath: document.getElementById("outputPath").value,
        dependencies: document.getElementById("dependencies").value.split(",").map(dep => dep.trim()),
        javaVersion: document.getElementById("javaVersion").value,
        springBootVersion: document.getElementById("springBootVersion").value,
        authType: document.getElementById("authType").value,
        baseUrl: document.getElementById("baseUrl").value,
        username: document.getElementById("username").value,
        password: document.getElementById("password").value,
        apiKey: document.getElementById("apiKey").value,
        systemName: document.getElementById("systemName").value,
        jsonBody: JSON.parse(document.getElementById("jsonBody").value || "{}") // Eğer JSON değilse boş JSON nesnesi
    };

    try {
        // API'ye istek gönderme
        const response = await fetch("/generate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(formData)
        });

        const result = await response.text(); // API'nin dönüş verisi

        // Kullanıcıya mesaj gösterme
        document.getElementById("responseMessage").innerHTML = `<p class="alert alert-success">${result}</p>`;
    } catch (error) {
        // Hata durumunda kullanıcıya mesaj gösterme
        document.getElementById("responseMessage").innerHTML = `<p class="alert alert-danger">Error: ${error.message}</p>`;
    }
});