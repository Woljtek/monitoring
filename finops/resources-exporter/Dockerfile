FROM golang:1.19-alpine

USER root

ADD bin/* /go/src/exporter/
ADD conf/kube_config.yaml /root/.kube/config
ADD conf/templates.json /root/.config/exporter/templates.json

WORKDIR /go/src/exporter

RUN export GOOS=linux && export GOARCH=amd64 && \
    go build -ldflags="-w -s" -o /go/bin/exporter /go/src/exporter/*.go

EXPOSE 2112
ENTRYPOINT [ "/go/bin/exporter" ]
