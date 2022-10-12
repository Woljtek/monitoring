FROM golang:1.19-alpine

USER root

ADD bin/* /go/src/exporter/

WORKDIR /go/src/exporter
# go get "github.com/aws/aws-sdk-go/aws"

RUN export GOOS=linux && export GOARCH=amd64 && \
    go build -ldflags="-w -s" -o /go/bin/exporter /go/src/exporter/*.go

EXPOSE 2112
ENTRYPOINT [ "/go/bin/exporter" ]
