# 🧪 Lab: Comunicação entre Microserviços com Quarkus (REST)

Laboratório de estudo que simula, de forma simplificada, a arquitetura de um
orquestrador (`gestao-fluxo`) que delega processamento a um worker
(`worker-interno`) e dispara notificações (`notificacao-service`) via
**REST Client declarativo** do Quarkus.

> **Objetivo:** absorver o padrão de comunicação síncrona entre microserviços
> usado em arquiteturas reais (ex.: plataformas de assinatura digital),
> antes de evoluir para mensageria (Kafka) e persistência (MongoDB/Panache).

---

## 📋 Stack

| Tecnologia | Versão / Detalhe |
|---|---|
| Java | OpenJDK 17 |
| Quarkus | 3.x (via Quarkus CLI / SDKMAN) |
| Extensões | `rest-jackson`, `rest-client-jackson` (só no orquestrador) |
| Ambiente | WSL Debian + zsh |

---

## 🏗️ Arquitetura

Três serviços independentes, cada um com seu próprio projeto Maven:

| Serviço | Porta | Papel |
|---|---|---|
| **gestao-fluxo** | `8080` | Orquestrador — expõe `/fluxos` e consome os outros dois serviços |
| **worker-interno** | `8081` | Worker — expõe `/processar` e executa a "lógica pesada" |
| **notificacao-service** | `8082` | Notificador — expõe `/notificacoes` e simula envio de e-mail/push |

### Fluxo de uma requisição (`POST /fluxos`)

```
curl ──POST /fluxos──▶ [gestao-fluxo :8080]
                            │
                       FluxoResource       ◀── recebe (rota EXISTE aqui)
                            │
                       FluxoService        ◀── orquestra a lógica
                            │
                    WorkerInternoClient    ◀── envia (rota NÃO existe aqui,
                            │                   é só o "endereço" do destino)
                            ▼
                  ──POST /processar──▶ [worker-interno :8081]
                                            │
                                    ProcessamentoResource   ◀── recebe (rota EXISTE aqui)
                                            │
                                    ProcessamentoService    ◀── processa, guarda o
                            │                                   estado e responde
                            ▼
                     NotificacaoClient
                            │
                  ──POST /notificacoes──▶ [notificacao-service :8082]
                                            │
                                    NotificacaoResource     ◀── recebe (rota EXISTE aqui)
                                            │
                                    NotificacaoService      ◀── "envia" a notificação
                                                                (log no console)
```

Além do fluxo principal, o gestao-fluxo também expõe
`GET /fluxos/{idDocumento}/status`, que repassa a consulta para
`GET /processar/{idDocumento}/status` no worker.

### Resource vs. Client — a diferença fundamental

Ambos usam as mesmas anotações JAX-RS (`@Path`, `@POST`...), mas apontam em
**direções opostas**:

| | Resource | REST Client |
|---|---|---|
| Tipo | Classe | Interface |
| Papel | **Servidor** — expõe API | **Consumidor** — chama API de outro serviço |
| O `@Path` significa | "rota que EU atendo" | "rota que VOU chamar no outro serviço" |
| Anotação chave | `@Path` na classe | `@RegisterRestClient` |
| Implementação | Escrita por você | Gerada pelo Quarkus em tempo de build |

> 💡 **Prova prática:** `POST http://localhost:8080/processar` retorna **404**,
> pois o `@Path("/processar")` do `WorkerInternoClient` não registra rota no
> gestao-fluxo — ele apenas descreve o endpoint do worker que será chamado.

### Quem tem o quê

| Serviço | `resource/` | `service/` | `client/` | Por quê |
|---|:---:|:---:|:---:|---|
| gestao-fluxo | ✅ | ✅ | ✅ | Expõe API **e** consome os outros dois (orquestrador) |
| worker-interno | ✅ | ✅ | ❌ | Só expõe — não chama ninguém |
| notificacao-service | ✅ | ✅ | ❌ | Só expõe — idem |

**Todo serviço tem `resource/` + `service/`.** O `client/` só existe em quem
consome outros serviços.

---

## 📁 Estrutura do projeto

```
~/projetos/lab-microservicos/
│
├── gestao-fluxo/                          # Orquestrador (porta 8080)
│   ├── src/main/java/br/com/keidson/
│   │   ├── resource/
│   │   │   └── FluxoResource.java         # POST /fluxos + GET /fluxos/{id}/status
│   │   ├── service/
│   │   │   └── FluxoService.java          # Orquestração: worker + notificação
│   │   ├── client/
│   │   │   ├── WorkerInternoClient.java   # REST Client → worker-interno (:8081)
│   │   │   └── NotificacaoClient.java     # REST Client → notificacao-service (:8082)
│   │   └── dto/
│   │       ├── FluxoRequest.java          # contrato próprio
│   │       ├── FluxoResponse.java         # contrato próprio
│   │       ├── ProcessamentoRequest.java  # contrato do worker (cópia local)
│   │       ├── ProcessamentoResponse.java # contrato do worker (cópia local)
│   │       ├── StatusResponse.java        # contrato do worker (cópia local)
│   │       ├── NotificacaoRequest.java    # contrato da notificação (cópia local)
│   │       └── NotificacaoResponse.java   # contrato da notificação (cópia local)
│   ├── src/main/resources/
│   │   └── application.properties         # porta 8080 + URLs dos 2 serviços
│   └── pom.xml
│
├── worker-interno/                        # Worker (porta 8081)
│   ├── src/main/java/br/com/keidson/
│   │   ├── resource/
│   │   │   └── ProcessamentoResource.java # POST /processar + GET /processar/{id}/status
│   │   ├── service/
│   │   │   └── ProcessamentoService.java  # "Lógica pesada" + estado em memória
│   │   └── dto/
│   │       ├── ProcessamentoRequest.java
│   │       ├── ProcessamentoResponse.java
│   │       └── StatusResponse.java
│   ├── src/main/resources/
│   │   └── application.properties         # porta 8081
│   └── pom.xml
│
└── notificacao-service/                   # Notificador (porta 8082)
    ├── src/main/java/br/com/keidson/
    │   ├── resource/
    │   │   └── NotificacaoResource.java   # POST /notificacoes
    │   ├── service/
    │   │   └── NotificacaoService.java    # Simula envio (log no console)
    │   └── dto/
    │       ├── NotificacaoRequest.java
    │       └── NotificacaoResponse.java
    ├── src/main/resources/
    │   └── application.properties         # porta 8082
    └── pom.xml
```

### Papel de cada pacote

- **`resource/`** — apenas a borda HTTP: recebe, delega ao service, devolve.
  Zero regra de negócio (equivalente ao Controller no Spring).
- **`service/`** — onde vive a lógica de negócio / orquestração.
- **`client/`** — interfaces `@RegisterRestClient` para serviços externos.
  **Uma interface por serviço consumido**, nunca uma interface gigante
  misturando serviços.
- **`dto/`** — objetos que trafegam na borda (contrato JSON da API).

> 📌 **Por que os DTOs são duplicados entre projetos?**
> Em microserviços, o contrato é o **JSON que trafega**, não uma classe
> compartilhada. Cada serviço mantém sua própria cópia dos DTOs —
> isso garante baixo acoplamento e deploys independentes. O orquestrador
> é quem mais acumula DTOs: o contrato próprio + uma cópia do contrato
> de cada serviço que consome.

> 📌 **Onde está o `model/`?**
> Ainda não existe, de propósito. `model/` (entidades) entra quando houver
> persistência (MongoDB + Panache). A distinção importa:
> `dto/` = contrato externo (muda quando a API muda);
> `model/` = representação interna persistida (muda quando o domínio muda).
> Nunca exponha a entity diretamente no endpoint.
> Por enquanto, o worker guarda estado num `ConcurrentHashMap` em memória
> (some ao reiniciar — é o gancho para o MongoDB no roadmap).

---

## ⚙️ Configuração

### gestao-fluxo — `application.properties`

```properties
quarkus.http.port=8080
quarkus.rest-client.worker-interno.url=http://localhost:8081
quarkus.rest-client.notificacao-service.url=http://localhost:8082
```

O padrão é `quarkus.rest-client.<configKey>.url` — a chave casa com o
`@RegisterRestClient(configKey = "...")` de cada interface. Em outros
ambientes (dev/hml/prod), só a config muda (variável de ambiente ou
ConfigMap); o código não sabe onde os serviços moram.

### worker-interno — `application.properties`

```properties
quarkus.http.port=8081
```

### notificacao-service — `application.properties`

```properties
quarkus.http.port=8082
```

---

## 🚀 Como executar

Pré-requisitos: Java 17+ e Quarkus CLI (instalável via [SDKMAN](https://sdkman.io)).

Três terminais (sobe os consumidos primeiro, o orquestrador por último):

```bash
# Terminal 1 — worker
cd ~/projetos/lab-microservicos/worker-interno && quarkus dev

# Terminal 2 — notificação
cd ~/projetos/lab-microservicos/notificacao-service && quarkus dev

# Terminal 3 — orquestrador
cd ~/projetos/lab-microservicos/gestao-fluxo && quarkus dev
```

---

## 🧪 Testando

### 1. Fluxo completo (POST)

```bash
curl -X POST http://localhost:8080/fluxos \
  -H "Content-Type: application/json" \
  -d '{"idDocumento": "doc-123", "nomeArquivo": "contrato.pdf"}'
```

**Resposta esperada:**

```json
{
  "idFluxo": "<uuid gerado>",
  "idDocumento": "doc-123",
  "estado": "DOCUMENTO_CONCLUIDO"
}
```

E no terminal do notificacao-service deve aparecer o log:

```
📧 Notificação enviada | documento=doc-123 | mensagem=Documento concluído!
```

### 2. Consulta de status (GET)

```bash
curl http://localhost:8080/fluxos/doc-123/status
```

```json
{
  "idDocumento": "doc-123",
  "estado": "DOCUMENTO_CONCLUIDO",
  "processadoEm": "2026-08-02T..."
}
```

### 3. Documento inexistente (404 → 500 ⚠️)

```bash
curl -i http://localhost:8080/fluxos/doc-999/status
```

O worker responde **404**, mas o REST Client do gestao-fluxo lança exceção
em respostas 4xx/5xx e, sem tratamento, isso vira **500** para o cliente
final — semanticamente errado (deveria propagar o 404). Resolver essa
propagação de erro está no roadmap.

### 4. Serviço fora do ar (falha em cascata)

1. Derrube **só** o notificacao-service (`Ctrl+C` no Terminal 2).
2. Repita o `curl` do teste 1.
3. O worker processa com sucesso, mas a resposta final é **500** —
   documento processado + erro para o cliente. Essa é a dor do
   encadeamento síncrono, e a motivação para notificação ser
   **assíncrona via Kafka** (roadmap).

---

## 🗺️ Roadmap de evolução do lab

- [x] Comunicação REST síncrona entre serviços (POST)
- [x] Separação em camadas (`resource` / `service` / `client` / `dto`)
- [x] Endpoint de consulta com `GET` + `@PathParam` (status do documento)
- [x] Estado em memória no worker (`ConcurrentHashMap`) + `NotFoundException` (404)
- [x] Terceiro serviço (`notificacao-service`) — orquestrador com múltiplos clients
- [ ] **Propagação de erro** — repassar o 404 do worker em vez de estourar 500
      (`ExceptionMapper` / captura de `WebApplicationException`)
- [ ] **Fault tolerance** — `@Timeout`, `@Retry` e `@Fallback`
      (extensão `smallrye-fault-tolerance`); atenção à idempotência em retries de POST
- [ ] **Mensageria** — notificação assíncrona via Kafka
      (evento `DOCUMENTO_CONCLUIDO`), eliminando a falha em cascata
- [ ] **Persistência** — MongoDB com Panache no worker
      (introduzindo o pacote `model/` e aposentando o mapa em memória)
- [ ] **Testes** — `@QuarkusTest` com stubs manuais CDI (sem Mockito)

---

## 📚 Referências

- [Quarkus — REST Client](https://quarkus.io/guides/rest-client)
- [Quarkus — Writing REST Services](https://quarkus.io/guides/rest)
- [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance)