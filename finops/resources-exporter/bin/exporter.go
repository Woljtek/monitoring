package main

import (
	"flag"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	isDebug              bool
	apiURL               string
	queueName            string
	port                 int
	endpoint             string
	registry             *prometheus.Registry
	machineGabaritGauges = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Namespace: "MONITORING",
			Subsystem: "FINOPS",
			Name:      "machineUsage",
			Help:      "Infos about all the machines currently in the k8s cluster",
		},
		[]string{
			"name",
			"cpu",
			"memory",
		},
	)
)

type processingTypes []string

func watchMachineGabarit() {
	for {
		nodeGabarits, err := GetNodeGabarits()
		if err != nil {
			DisplayLog("FATAL", err.Error())
			os.Exit(1)
		}
		machineRepartition, err := MatchAll(nodeGabarits)
		if err != nil {
			DisplayLog("FATAL", err.Error())
			os.Exit(2)
		}
		for template, nbMachine := range machineRepartition {
			machineGabaritGauges.WithLabelValues(
				template.Name,
				strconv.Itoa(template.CPU),
				strconv.Itoa(template.Memory),
			).Set(float64(nbMachine))
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

	prometheus.MustRegister(machineGabaritGauges)
}

func main() {
	flag.Parse()

	go watchMachineGabarit()

	DisplayLog("INFO", fmt.Sprintf("machine-usage-exporter started to listen on http://x.x.x.x:%d%s", port, endpoint))
	http.Handle(endpoint, promhttp.Handler())
	http.ListenAndServe(":"+strconv.Itoa(port), nil)
	DisplayLog("INFO", "Machine-usage-exporter stopped")
}
