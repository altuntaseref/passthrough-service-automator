apiVersion: apps/v1
kind: Deployment
metadata:
name: ${projectName}-deployment
namespace: default
labels:
app: ${projectName}
spec:
replicas: 1
selector:
matchLabels:
app: ${projectName}
template:
metadata:
labels:
app: ${projectName}
spec:
containers:
- name: spring-boot-container
image: altuntasserf/${projectName}:latest
ports:
- containerPort: 8080