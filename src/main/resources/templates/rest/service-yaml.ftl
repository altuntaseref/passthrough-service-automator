apiVersion: v1
kind: Service
metadata:
name: ${projectName}
spec:
selector:
app: ${projectName}
ports:
- protocol: TCP
port: 80
targetPort: 8080
type: NodePort