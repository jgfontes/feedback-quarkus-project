# Plano de Arquitetura - Plataforma de Avaliacao (AWS)

## 1) Objetivo
Construir uma plataforma serverless em AWS para:
- receber feedbacks de alunos,
- identificar casos criticos (nota baixa),
- notificar administradores automaticamente,
- gerar relatorio semanal consolidado.

## 2) Escopo funcional
Entrada principal:
- `POST /feedback`
  - body:
    - `description` (string)
    - `grade` (int 0..10)

Saidas esperadas:
- persistencia do feedback,
- evento para casos criticos (notas < 3),
- notificacao por e-mail em caso critico,
- relatorio semanal com metricas.

## 3) Arquitetura alvo
### Componentes AWS
- Amazon API Gateway (HTTP API): recebe requisicoes externas.
- AWS Lambda `feedback-create`: valida e persiste feedback.
- Amazon DynamoDB `Feedback`: armazenamento serverless.
- Event bus para eventos de criticidade: Amazon EventBridge.
- AWS Lambda `feedback-notify`: consome evento critico e envia notificacao.
- Servico de envio de e-mail:
  - Recomendado: Amazon SES.
  - Alternativa: SNS + e-mail subscription.
- AWS Lambda `feedback-weekly-report`: gera relatorio semanal.
- EventBridge Scheduler: agenda execucao semanal da Lambda de relatorio.

### Fluxo ponta a ponta
1. Cliente chama `POST /feedback` no API Gateway.
2. API Gateway invoca Lambda `feedback-create`.
3. `feedback-create` valida payload e grava no DynamoDB.
4. Se `grade` for abaixo do limiar (ex.: `< 6`), publica evento de criticidade.
5. EventBridge encaminha evento para Lambda `feedback-notify`.
6. `feedback-notify` envia e-mail aos administradores com dados do feedback.
7. Semanalmente, EventBridge Scheduler aciona `feedback-weekly-report`.
8. `feedback-weekly-report` agrega dados e salva relatorio (S3 e/ou DynamoDB) e opcionalmente envia por e-mail.

## 4) Modelagem de dados
### Tabela DynamoDB `Feedback`
- Partition key: `id` (Number ou String).
- Atributos:
  - `description` (String)
  - `grade` (Number)
  - `createdAt` (String ISO-8601)

### Regras de negocio
- `grade` obrigatoria entre 0 e 10.
- `description` obrigatoria (tamanho minimo recomendado: 3).

## 5) Contrato de evento (criticidade)
Evento padrao (EventBridge detail):
```json
{
  "eventId": "uuid",
  "feedbackId": "123",
  "description": "Aula confusa",
  "grade": 3,
  "urgency": "HIGH",
  "createdAt": "2026-05-05T20:00:00Z",
  "correlationId": "uuid"
}
```

Requisitos do evento:
- idempotencia por `eventId`/`feedbackId` no consumidor.
- retries com backoff e DLQ para falhas permanentes.

## 6) Lambdas (responsabilidade unica)
### Lambda 1 - `feedback-create`
- Trigger: API Gateway.
- Responsabilidades:
  - validar requisicao,
  - persistir no DynamoDB,
  - publicar evento de criticidade quando aplicavel.

### Lambda 2 - `feedback-notify`
- Trigger: EventBridge rule.
- Responsabilidades:
  - processar evento critico,
  - enviar e-mail para administradores,
  - registrar status de envio para auditoria.

### Lambda 3 - `feedback-weekly-report`
- Trigger: EventBridge Scheduler (1x por semana).
- Responsabilidades:
  - calcular media de notas,
  - consolidar quantidade por dia,
  - consolidar quantidade por urgencia,
  - salvar e/ou enviar relatorio.

## 7) Observabilidade
- TBD: definir estrategia de logs, metricas e tracing (ex.: CloudWatch Logs, CloudWatch Metrics, X-Ray).

## 8) Seguranca e governanca
- IAM com menor privilegio para cada Lambda.
- Segredos e configuracoes sensiveis em AWS Secrets Manager/SSM Parameter Store.
- CloudTrail para auditoria de chamadas administrativas.
- Politica de retencao de logs por ambiente.

## 9) Ambientes e deploy
- TBD: definir estrategia de deploy (ex.: IaC com CloudFormation/Terraform, CDK, Serverless Framework).

## 13) Roadmap de implementacao (sugerido)
### Fase 1 - Base funcional
- API Gateway + Lambda `feedback-create` + DynamoDB.
- Persistencia e retorno de sucesso.

### Fase 2 - Criticidade e notificacao
- Publicacao de evento de nota baixa.
- Lambda `feedback-notify` + SES.
- DLQ e alarmes basicos.

### Fase 3 - Relatorio semanal
- Lambda `feedback-weekly-report` + Scheduler.
- Persistencia/entrega do relatorio.

### Fase 4 - Hardening
- Observabilidade completa, SLO, seguranca, custo, runbooks.

## 14) Riscos principais e mitigacoes
- Risco: duplicidade de eventos.
  - Mitigacao: idempotencia com `eventId` e registro de processamento.
- Risco: perda de evento por falha temporaria.
  - Mitigacao: retries + DLQ + monitoramento.
- Risco: timeout em Lambdas sob carga.
  - Mitigacao: ajuste de memoria/timeout e testes de carga.
- Risco: acoplamento excessivo em uma unica funcao.
  - Mitigacao: separar responsabilidades conforme as 3 Lambdas.
- Risco: custo inesperado com logs/eventos.
  - Mitigacao: budgets, retencao de logs e revisao periodica.

