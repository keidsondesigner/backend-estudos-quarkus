# httpclient-quarkus

Projeto de estudo em **Java 17 + Quarkus 3.33 LTS** mostrando como consumir APIs HTTP externas, usando consulta de CEP como exemplo:

- **ViaCEP** (`https://viacep.com.br`) – provedor principal
- **BrasilAPI** (`https://brasilapi.com.br`) – fallback quando o ViaCEP falha

São mostradas duas formas de fazer a chamada HTTP:

| Abordagem | Onde | Quando usar |
|---|---|---|
| **REST Client declarativo** (MicroProfile) | `client/viacep/ViaCepClient`, `client/brasilapi/BrasilApiClient` | Padrão no Quarkus. Você escreve só a interface, e a URL, os timeouts e o log ficam no `application.properties`. Fácil de mockar nos testes. |
| **`java.net.http.HttpClient`** (JDK) | `provider/JdkHttpCepProvider` | Para entender o que acontece por baixo, ou quando não se quer depender de extensão. Você monta request, trata status e faz o parse do JSON na mão (versão síncrona e assíncrona). |

Também estão incluídos:

- **Resiliência** com SmallRye Fault Tolerance (`@Timeout`, `@Retry` com `abortOn`)
- **Fallback** manual ViaCEP → BrasilAPI no `CepService`
- **Cache** Caffeine com `@CacheResult` (1h por CEP)
- **Tratamento de erro** global com `@ServerExceptionMapper` (400 / 404 / 503 / 504)
- **Bean Validation** nos query params
- **Swagger UI** e **testes** com `@InjectMock` nos REST Clients, sem acessar a internet

## Estrutura

```
src/main/java/br/com/keidson/cep
├── client
│   ├── viacep/       ViaCepClient (interface REST Client) + ViaCepResponse (DTO)
│   └── brasilapi/    BrasilApiClient + BrasilApiResponse
├── provider/         ViaCepProvider, BrasilApiProvider (timeout/retry), JdkHttpCepProvider
├── service/          CepService (cache + fallback)
├── resource/         CepResource (endpoints REST)
├── model/            Endereco (modelo de domínio único)
├── exception/        exceções + ExceptionHandlers + ErroResponse
└── util/             CepUtils (normalizar/formatar CEP)
```

## Requisitos (WSL)

- Java 17+ (`java --version`)
- Maven 3.9+: `sudo apt update && sudo apt install -y maven`

## Rodando

```bash
mvn quarkus:dev
```

- API: http://localhost:8080/ceps/69005040
- Swagger UI: http://localhost:8080/q/swagger-ui
- Dev UI: http://localhost:8080/q/dev-ui

> No WSL2, o `localhost` funciona direto no navegador do Windows.

No modo dev, as requisições e respostas do REST Client aparecem no log (nível DEBUG).

## Endpoints

```bash
# Recomendado: cache + ViaCEP (retry/timeout) + fallback BrasilAPI
curl http://localhost:8080/ceps/69005-040

# Busca de CEP por endereço (ViaCEP)
curl "http://localhost:8080/ceps?uf=AM&cidade=Manaus&logradouro=Djalma"

# Didáticos: cada provedor isolado
curl http://localhost:8080/ceps/69005040/viacep
curl http://localhost:8080/ceps/69005040/brasilapi
curl http://localhost:8080/ceps/69005040/jdk
curl http://localhost:8080/ceps/69005040/jdk-async

# Erros
curl -i http://localhost:8080/ceps/123        # 400 - formato inválido
curl -i http://localhost:8080/ceps/99999999   # 404 - não encontrado
```

Exemplo de resposta:

```json
{
  "cep": "69005-040",
  "logradouro": "Avenida Eduardo Ribeiro",
  "bairro": "Centro",
  "cidade": "Manaus",
  "uf": "AM",
  "ibge": "1302603",
  "ddd": "92",
  "fonte": "viacep"
}
```

Exemplo de erro:

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "CEP 99999999 não encontrado",
  "timestamp": "2026-11-05T16:30:00Z"
}
```

## Testando o fallback na prática

Aponte o ViaCEP para um host inválido e chame o endpoint principal. O campo `fonte` vai vir como `brasilapi`:

```bash
mvn quarkus:dev -Dquarkus.rest-client.viacep.url=http://localhost:9999
curl http://localhost:8080/ceps/01001000
```

## Testes

```bash
mvn test
```

O `CepResourceTest` mocka os dois REST Clients com `@InjectMock @RestClient`. Ele cobre: sucesso, CEP inválido, não encontrado, fallback (incluindo o número de retries), falha total (503), cache e busca por endereço.

## Build e execução do JAR

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

## Fluxo da requisição principal

```
GET /ceps/{cep}
   └─ CepUtils.normalizar  ──(inválido)──> 400
   └─ CepService.buscar  @CacheResult("ceps")
        ├─ ViaCepProvider  @Timeout(3s) @Retry(2x)
        │     ├─ ok ─────────────────────────────> 200 (fonte=viacep)
        │     ├─ {"erro": true} ─────────────────> 404 (sem retry, sem fallback)
        │     └─ falha de rede/timeout/5xx
        │           └─ BrasilApiProvider @Timeout(5s) @Retry(1x)
        │                 ├─ ok ─────────────────> 200 (fonte=brasilapi)
        │                 ├─ 404 ────────────────> 404
        │                 └─ falha ──────────────> 503
```

## Observações

- **Rede corporativa / proxy:** o REST Client usa as propriedades `quarkus.rest-client.proxy-address` ou `quarkus.rest-client.<configKey>.proxy-address`. O `JdkHttpCepProvider` respeita `-Dhttps.proxyHost` / `-Dhttps.proxyPort`.
- **Versão do Quarkus:** está fixada em `3.33.4.1` (LTS) no `pom.xml`. Se quiser a LTS mais nova (3.40), troque `quarkus.platform.version` ou rode `quarkus update`.
