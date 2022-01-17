FROM golang:1.13-alpine

USER root

ADD bin/* /go/src/exporter/
ADD conf/kube_config.yaml /root/.kube/config
ADD conf/templates.json /root/.config/exporter/templates.json

WORKDIR /go/src/exporter

RUN apk add --no-cache git && \
    wget -O - https://raw.githubusercontent.com/golang/dep/master/install.sh | sh && \
    dep ensure && \
    export GOOS=linux && export GOARCH=amd64 && \
    go build -ldflags="-w -s" -o /go/bin/exporter /go/src/exporter/*.go

EXPOSE 2112
ENTRYPOINT [ "/go/bin/exporter" ]
