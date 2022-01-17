package main

import (
	"fmt"
	"math"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
)

var (
	k8sClient *kubernetes.Clientset
)

// NodeGabarit todo
type NodeGabarit struct {
	Name      string
	Role      string
	CPU       int
	Memory    int64
	Disk      int64
	Readiness string
}

func getRole(labels map[string]string) string {
	role := "unknown"
	for key := range labels {
		if strings.HasPrefix(key, "node-role.kubernetes.io/") {
			role = strings.TrimPrefix(key, "node-role.kubernetes.io/")
		}
	}
	return role
}

func round(val float64, roundOn float64, places int) (newVal float64) {
	var round float64
	pow := math.Pow(10, float64(places))
	digit := pow * val
	_, div := math.Modf(digit)
	if div >= roundOn {
		round = math.Ceil(digit)
	} else {
		round = math.Floor(digit)
	}
	newVal = round / pow
	return
}

func humanReadableFormat(byteCount float64) string {
	suffixes := [5]string{"B", "KB", "MB", "GB", "TB"}
	base := math.Log(byteCount) / math.Log(1024)
	getSize := round(math.Pow(1024, base-math.Floor(base)), .5, 2)
	getSuffix := suffixes[int(math.Floor(base))]
	return strconv.FormatFloat(getSize, 'f', -1, 64) + string(getSuffix)
}

func newK8SClient() error {
	config, err := rest.InClusterConfig()
	if err != nil {
		DisplayLog("DEBUG", "Outside of a k8s cluster !")
		filePath := filepath.Join(os.Getenv("HOME"), ".kube", "config")
		if os.Getenv("K8S_FILE") != "" {
			filePath = os.Getenv("K8S_FILE")
		}
		config, err = clientcmd.BuildConfigFromFlags("", filePath)
		if err != nil {
			return fmt.Errorf("The k8s config cannot be read - %s", err.Error())
		}
	}
	clientcmd, err := kubernetes.NewForConfig(config)
	if err != nil {
		return fmt.Errorf("The k8s clientcmd cannot be created - %s", err.Error())
	}
	k8sClient = clientcmd
	return nil
}

// GetNodeGabarits todo
func GetNodeGabarits() ([]NodeGabarit, error) {
	nodeGabarits := []NodeGabarit{}
	if k8sClient == nil {
		if err := newK8SClient(); err != nil {
			return nodeGabarits, fmt.Errorf("Failed to create the k8s client - %s", err.Error())
		}
	}
	nodes, err := k8sClient.CoreV1().Nodes().List(metav1.ListOptions{})
	if err != nil {
		return nodeGabarits, fmt.Errorf("Failed to list the nodes - %s", err.Error())
	}
	for _, node := range nodes.Items {
		metadata := node.ObjectMeta
		status := node.Status
		cpuQuantity := status.Capacity["cpu"]
		cpuNumber, _ := (&cpuQuantity).AsInt64()
		memoryQuantity := status.Capacity["memory"]
		memoryNumber, _ := (&memoryQuantity).AsInt64()
		diskQuantity := status.Capacity["ephemeral-storage"]
		diskNumber, _ := (&diskQuantity).AsInt64()
		var readiness string
		for _, condition := range status.Conditions {
			if condition.Type == "Ready" {
				switch condition.Status {
				case v1.ConditionTrue:
					readiness = "True"
				case v1.ConditionFalse:
					readiness = "False"
				case v1.ConditionUnknown:
					readiness = "Unknown"
				default:
					return nodeGabarits, fmt.Errorf("Unknown Condition status - %v", condition.Status)
				}
			}
		}

		nodeGabarits = append(nodeGabarits, NodeGabarit{
			metadata.Name,
			getRole(metadata.Labels),
			int(cpuNumber),
			memoryNumber,
			diskNumber,
			readiness,
		})
	}
	return nodeGabarits, nil
}
