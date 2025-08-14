{{- define "kube-yaml-service.name" -}}
kube-yaml-service
{{- end -}}

{{- define "kube-yaml-service.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "kube-yaml-service.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
