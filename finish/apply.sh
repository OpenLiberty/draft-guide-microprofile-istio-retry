#kubectl apply -f kubernetes.yaml
kubectl apply -f istio.yaml
kubectl apply -f traffic.yaml
kubectl apply -f config.yaml 
echo waiting...
sleep 10
echo waiting...
sleep 10
echo waiting...
sleep 10
echo waiting...
sleep 10
echo waiting...
sleep 5
./list.sh