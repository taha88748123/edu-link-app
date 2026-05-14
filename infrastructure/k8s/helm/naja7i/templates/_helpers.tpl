{{/*
=============================================================================
NAJA7I Helm helpers
=============================================================================
*/}}

{{/* Full chart name (release-aware) */}}
{{- define "naja7i.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/* Chart name */}}
{{- define "naja7i.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Chart + version label */}}
{{- define "naja7i.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels — apply on every resource */}}
{{- define "naja7i.labels" -}}
helm.sh/chart: {{ include "naja7i.chart" . }}
app.kubernetes.io/name: {{ include "naja7i.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: naja7i
{{- end -}}

{{/* Selector labels for a *specific* component
     Usage: include "naja7i.componentSelectorLabels" (dict "root" $ "name" "auth-service") */}}
{{- define "naja7i.componentSelectorLabels" -}}
app.kubernetes.io/name: {{ include "naja7i.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .name }}
{{- end -}}

{{/* Per-component labels — base labels + component */}}
{{- define "naja7i.componentLabels" -}}
{{ include "naja7i.labels" .root }}
app.kubernetes.io/component: {{ .name }}
{{- end -}}

{{/* Image reference — combines registry + image + tag.
     Usage: include "naja7i.image" (dict "root" $ "svc" $cfg) */}}
{{- define "naja7i.image" -}}
{{- $registry := .root.Values.global.imageRegistry -}}
{{- $tag := .root.Values.global.imageTag -}}
{{- if $registry -}}
{{ printf "%s/%s:%s" $registry .svc.image $tag }}
{{- else -}}
{{ printf "%s:%s" .svc.image $tag }}
{{- end -}}
{{- end -}}

{{/* envFrom-secrets renderer — produces an `env:` block of secretKeyRef entries.
     Usage: include "naja7i.envFromSecrets" (dict "items" $cfg.envFromSecrets) */}}
{{- define "naja7i.envFromSecrets" -}}
{{- range .items }}
- name: {{ .name }}
  valueFrom:
    secretKeyRef:
      name: {{ .secretName }}
      key:  {{ .key }}
{{- end }}
{{- end -}}

{{/* env-map renderer — turns a map into env entries.
     Usage: include "naja7i.envMap" (dict "data" $cfg.env) */}}
{{- define "naja7i.envMap" -}}
{{- range $k, $v := .data }}
- name: {{ $k }}
  value: {{ $v | quote }}
{{- end }}
{{- end -}}

{{/* ServiceAccount name */}}
{{- define "naja7i.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "naja7i.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}
