# Plataforma de Feedback - Tech Challenge Fase 4 (FIAP)

Plataforma serverless para recebimento de feedbacks de alunos, notificação automática de avaliações críticas e geração de relatórios semanais.

## Arquitetura

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│   Cliente   │────▶│   API Gateway    │────▶│ Lambda       │
│  (POST/GET) │     │  (HTTP API)      │     │ feedback-    │
└─────────────┘     └──────────────────┘     │ create       │
                                             └──────┬───────┘
                                                     │
                                    ┌────────────────┼─────────────────┐
                                    ▼                                  ▼
                           ┌──────────────┐                  ┌──────────────┐
                           │   DynamoDB   │                  │ EventBridge  │
                           │  (feedback)  │                  │ (grade < 6)  │
                           └──────────────┘                  └──────┬───────┘
                                  ▲                                 │
                                  │                                 ▼
                           ┌──────────────┐                  ┌──────────────┐
                           │ Lambda       │                  │ Lambda       │
                           │ feedback-    │                  │ feedback-    │
                           │ weekly-report│                  │ notify       │
                           └──────┬───────┘                  └──────┬───────┘
                                  │                                 │
                                  ▼                                 ▼
                           ┌──────────────┐                  ┌──────────────┐
                           │ EventBridge  │                  │   Amazon     │
                           │ Scheduler    │                  │     SES      │
                           │ (7 dias)     │                  │  (e-mail)    │
                           └──────────────┘                  └──────────────┘
```

### Componentes AWS

| Componente | Serviço | Função |
|---|---|---|
| API | API Gateway (REST) | Recebe POST/GET /feedback |
| Persistência | DynamoDB (PAY_PER_REQUEST) | Armazena feedbacks |
| Processamento | 3 AWS Lambdas (Java 21, Quarkus) | Lógica de negócio |
| Eventos | EventBridge | Roteamento de eventos críticos |
| Agendamento | EventBridge Scheduler | Dispara relatório semanal |
| Notificação | Amazon SES | Envio de e-mails |
| Resiliência | SQS (Dead Letter Queue) | Captura falhas |
| Monitoramento | CloudWatch Alarms + Logs | Alertas e observabilidade |

## Funções Serverless

### 1. feedback-create (Lambda)
- **Trigger:** API Gateway (POST/GET /feedback)
- **Responsabilidades:**
  - Validar entrada (grade 0-10, description mínimo 3 caracteres)
  - Persistir feedback no DynamoDB com ID, description, grade, createdAt
  - Publicar evento `FeedbackCritical` no EventBridge quando grade < 6
  - Retornar feedbacks (GET) ou feedback criado (POST)

### 2. feedback-notify (Lambda)
- **Trigger:** EventBridge rule (source: `feedback.platform`, detail-type: `FeedbackCritical`)
- **Responsabilidades:**
  - Consumir evento de feedback crítico
  - Enviar e-mail ao administrador via SES com: descrição, urgência, data

### 3. feedback-weekly-report (Lambda)
- **Trigger:** EventBridge Scheduler (rate: 7 dias)
- **Responsabilidades:**
  - Consultar feedbacks dos últimos 7 dias no DynamoDB
  - Calcular: média de notas, quantidade por dia, quantidade por urgência
  - Enviar relatório por e-mail via SES

## Endpoints

### POST /feedback
Cria um novo feedback.

**Request:**
```json
{
  "description": "Aula excelente sobre serverless",
  "grade": 9
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "description": "Aula excelente sobre serverless",
  "grade": 9,
  "createdAt": "2026-05-11T14:52:09.502Z",
  "urgency": "NORMAL"
}
```

**Validações:**
- `description`: obrigatória, mínimo 3 caracteres
- `grade`: obrigatório, inteiro entre 0 e 10

### GET /feedback
Lista todos os feedbacks.

### GET /feedback/{id}
Busca feedback por ID.

## Classificação de Urgência

| Nota | Urgência | Ação |
|---|---|---|
| 0 a 5 | HIGH | Publica evento → e-mail de alerta |
| 6 a 10 | NORMAL | Apenas persiste |

## Instruções de Deploy

### Pré-requisitos
- Java 21 (Corretto)
- Maven 3.9+
- AWS SAM CLI
- Conta AWS com e-mail verificado no SES

### Deploy manual

```bash
# 1. Build do projeto
./mvnw package -DskipTests

# 2. Build SAM
sam build --template template.yaml

# 3. Deploy
sam deploy \
  --stack-name feedback-platform \
  --region us-east-1 \
  --capabilities CAPABILITY_IAM \
  --resolve-s3 \
  --no-confirm-changeset \
  --parameter-overrides "AdminEmail=seu@email.com SenderEmail=seu@email.com"
```

### Deploy automatizado (CI/CD)

O projeto possui pipeline GitHub Actions (`.github/workflows/deploy.yml`) que executa automaticamente no push para `main`:

1. Build Java 21
2. Execução de testes
3. SAM build + deploy

**Secrets necessários no GitHub:**
| Secret | Descrição |
|---|---|
| `AWS_ACCESS_KEY_ID` | Chave de acesso IAM |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta IAM |
| `ADMIN_EMAIL` | E-mail do administrador (destino) |
| `SENDER_EMAIL` | E-mail verificado no SES (remetente) |

### Verificar e-mail no SES

```bash
aws ses verify-email-identity --email-address seu@email.com --region us-east-1
```

## Monitoramento

### CloudWatch Alarms

| Alarme | Condição | Período |
|---|---|---|
| Erros feedback-create | Errors >= 1 | 5 min |
| Erros feedback-notify | Errors >= 1 | 5 min |
| Erros feedback-weekly-report | Errors >= 1 | 5 min |
| DLQ não vazia | Messages >= 1 | 5 min |

### Logs

- Retenção: 14 dias
- Log Groups: `/aws/lambda/<function-name>`

### Resiliência

- Dead Letter Queue (SQS) captura invocações que falharam
- Retenção de mensagens na DLQ: 14 dias

## Segurança

- **IAM Least Privilege:** cada Lambda possui apenas as permissões necessárias
  - feedback-create: DynamoDB CRUD + EventBridge PutEvents
  - feedback-notify: SES SendEmail
  - feedback-weekly-report: DynamoDB Read + SES SendEmail
- **Dados sensíveis:** e-mails configurados via variáveis de ambiente (não hardcoded)
- **DLQ:** falhas são capturadas para auditoria

## Tecnologias

- Java 21 (Amazon Corretto)
- Quarkus 3.34.6 (framework serverless)
- AWS Lambda, DynamoDB, API Gateway, EventBridge, SES, SQS, CloudWatch
- AWS SAM (Infrastructure as Code)
- GitHub Actions (CI/CD)
- JUnit 5 + Mockito (testes)

## Estrutura do Projeto

```
src/main/java/org/fiap/com/
├── models/
│   └── Feedback.java              # Modelo com from(), getUrgency()
├── service/
│   ├── AbstractFeedbackService.java  # Base com requests DynamoDB
│   ├── FeedbackLambda.java           # Handler principal (API Gateway)
│   ├── FeedbackServiceImpl.java      # Lógica CRUD + EventBridge
│   ├── FeedbackNotifyLambda.java     # Consumidor de eventos críticos
│   └── FeedbackWeeklyReportLambda.java # Relatório semanal
└── FeedbackMapperUtil.java           # Utilitário JSON

template.yaml          # IaC (SAM)
samconfig.toml         # Config de deploy
.github/workflows/     # Pipeline CI/CD
```

## Testes

```bash
./mvnw test
```

Cobertura: modelo, validação, service, notify e report (26 testes unitários).
