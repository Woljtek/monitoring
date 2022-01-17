FROM golang:1.13-alpine

USER root

ADD bin/* /go/src/exporter/

WORKDIR /go/src/exporter
# go get "github.com/aws/aws-sdk-go/aws"
RUN apk add --no-cache git && \
    wget -O - https://raw.githubusercontent.com/golang/dep/master/install.sh | sh && \
    dep ensure && \
    export GOOS=linux && export GOARCH=amd64 && \
    go build -ldflags="-w -s" -o /go/bin/exporter /go/src/exporter/*.go

EXPOSE 2112
ENTRYPOINT [ "/go/bin/exporter" ]
