#!/bin/bash
while getopts t:d:b:u: flag; do
    case "${flag}" in
    t) DATE="${OPTARG}" ;;
    d) DRIVER="${OPTARG}" ;;
    esac
done

echo "Testing daily Docker image"

sed -i "\#<artifactId>liberty-maven-plugin</artifactId>#a<configuration><install><runtimeUrl>https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/nightly/"$DATE"/"$DRIVER"</runtimeUrl></install></configuration>" inventory/pom.xml system/pom.xml
cat inventory/pom.xml system/pom.xml

sed -i "s;FROM openliberty/open-liberty:kernel-java8-openj9-ubi;openliberty/daily:latest;g" inventory/Dockerfile system/Dockerfile
cat inventory/Dockerfile system/Dockerfile

docker pull "openliberty/daily:latest"

sudo ../scripts/startMinikube.sh
sudo ../scripts/installIstio.sh
sudo ../scripts/testApp.sh
sudo ../scripts/stopMinikube.sh