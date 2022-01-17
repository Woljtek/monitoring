package main

import (
	"flag"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

const (
	namespace = "MONITORING"
	subsystem = "FINOPS"
)

var (
	isDebug   bool
	apiURL    string
	queueName string
	port      int
	endpoint  string
	registry  *prometheus.Registry

	s3LastModifiedObjectDate = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: namespace,
			Subsystem: subsystem,
			Name:      "last_modified_object_date",
			Help:      "The last modified date of the object that was modified most recently",
		},
		[]string{
			"bucket",
		},
	)
	s3LastModifiedObjectSize = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: namespace,
			Subsystem: subsystem,
			Name:      "last_modified_object_size_bytes",
			Help:      "The size of the object that was modified most recently",
		},
		[]string{
			"bucket",
		},
	)
	s3ObjectTotal = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: namespace,
			Subsystem: subsystem,
			Name:      "objects_total",
			Help:      "The total number of objects for the bucket/prefix combination",
		},
		[]string{
			"bucket",
		},
	)
	s3SumSize = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: namespace,
			Subsystem: subsystem,
			Name:      "objects_size_sum_bytes",
			Help:      "The total size of all objects summed",
		},
		[]string{
			"bucket",
		},
	)
	s3BiggestSize = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: namespace,
			Subsystem: subsystem,
			Name:      "biggest_object_size_bytes",
			Help:      "The size of the biggest object",
		},
		[]string{
			"bucket",
		},
	)
)

type processingTypes []string

func createS3Client() (*s3.S3, error) {
	endpoint := os.Getenv("S3_ENDPOINT")
	if endpoint == "" {
		return nil, fmt.Errorf("S3_ENDPOINT is not set")
	}
	region := os.Getenv("S3_REGION")
	if region == "" {
		return nil, fmt.Errorf("S3_REGION is not set")
	}
	accessKey := os.Getenv("S3_ACCESS_KEY")
	if accessKey == "" {
		return nil, fmt.Errorf("S3_ACCESS_KEY is not set")
	}
	secretKey := os.Getenv("S3_SECRET_KEY")
	if secretKey == "" {
		return nil, fmt.Errorf("S3_SECRET_KEY is not set")
	}
	s3Config := &aws.Config{
		Credentials: credentials.NewStaticCredentials(accessKey, secretKey, ""),
		Endpoint:    aws.String(endpoint),
		Region:      aws.String(region),
	}

	s3Session, err := session.NewSession(s3Config)
	if err != nil {
		return nil, fmt.Errorf("Failed to create a new session - %s", err.Error())
	}
	return s3.New(s3Session), nil
}

func watchObjectStorage() {
	s3Client, err := createS3Client()
	if err != nil {
		DisplayLog("FATAL", fmt.Sprintf("Failed to create s3Client - %s", err.Error()))
		os.Exit(1)
	}
	DisplayLog("INFO", fmt.Sprintf("object-storage-exporter connected to S3 endpoint %s", endpoint))

	for {
		buckets, err := s3Client.ListBuckets(nil)
		if err != nil {
			DisplayLog("FATAL", fmt.Sprintf("Failed to list buckets - %s", err.Error()))
			os.Exit(2)
		}
		for _, bucket := range buckets.Buckets {
			var lastModified time.Time
			var numberOfObjects float64
			var totalSize int64
			var biggestObjectSize int64
			var lastObjectSize int64

			s3Client.ListObjectsPages(&s3.ListObjectsInput{
				Bucket: bucket.Name,
			}, func(p *s3.ListObjectsOutput, last bool) (shouldContinue bool) {

				for _, object := range p.Contents {
					numberOfObjects++
					totalSize = totalSize + *object.Size
					if object.LastModified.After(lastModified) {
						lastModified = *object.LastModified
						lastObjectSize = *object.Size
					}
					if *object.Size > biggestObjectSize {
						biggestObjectSize = *object.Size
					}
				}
				return true
			})
			s3LastModifiedObjectDate.WithLabelValues(
				*bucket.Name,
			).Set(float64(lastModified.UnixNano() / 1e9))
			s3LastModifiedObjectSize.WithLabelValues(
				*bucket.Name,
			).Set(float64(lastObjectSize))
			s3ObjectTotal.WithLabelValues(
				*bucket.Name,
			).Set(numberOfObjects)
			s3BiggestSize.WithLabelValues(
				*bucket.Name,
			).Set(float64(biggestObjectSize))
			s3SumSize.WithLabelValues(
				*bucket.Name,
			).Set(float64(totalSize))
		}
		time.Sleep(5 * time.Second)
	}
}

func init() {
	const (
		isDebugDefault  = false
		isDebugUsage    = "Run in debug mode"
		portDefault     = 2112
		portUsage       = "Port to export on"
		endpointDefault = "/metrics"
		endpointUsage   = "Endpoint to export on"
	)
	flag.BoolVar(&isDebug, "debug", isDebugDefault, isDebugUsage)
	flag.BoolVar(&isDebug, "d", isDebugDefault, isDebugUsage+" (shorthand)")
	flag.IntVar(&port, "port", portDefault, portUsage)
	flag.IntVar(&port, "p", portDefault, portUsage+" (shorthand)")
	flag.StringVar(&endpoint, "endpoint", endpointDefault, endpointUsage)
	flag.StringVar(&endpoint, "e", endpointDefault, endpointUsage+" (shorthand)")

	if boolValue, err := strconv.ParseBool(os.Getenv("API_EXPORTER_DEBUG")); err == nil {
		isDebug = boolValue
	}

	prometheus.MustRegister(s3BiggestSize)
	prometheus.MustRegister(s3LastModifiedObjectDate)
	prometheus.MustRegister(s3LastModifiedObjectSize)
	prometheus.MustRegister(s3ObjectTotal)
	prometheus.MustRegister(s3SumSize)
}

func main() {
	flag.Parse()

	go watchObjectStorage()

	DisplayLog("INFO", fmt.Sprintf("object-storage-exporter started to listen on http://x.x.x.x:%d%s", port, endpoint))
	http.Handle(endpoint, promhttp.Handler())
	http.ListenAndServe(":"+strconv.Itoa(port), nil)
	DisplayLog("INFO", "Object-storage-exporter stopped")
}
