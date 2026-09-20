# KubeOptix Configurations API

API REST em Java com Quarkus para gerenciar configurações do sistema e o catálogo de documentos do KubeOptix. A aplicação expõe operações de CRUD para documentos, versões, autores, clientes e uma única configuração global do sistema, além de endpoints de health check e documentação OpenAPI.

## O que a aplicação faz

A aplicação centraliza as informações de configuração operacionais da plataforma e o conteúdo documental que a interface ou outros serviços podem consultar.

### 1) Configurações do sistema
A entidade `SystemSettings` guarda os dados globais do ambiente, como:

- idioma padrão como locale BCP 47 (por exemplo, `en`, `pt-BR`, `es-MX` ou `zh-Hans`)
- chave e modelo do Cursor
- chave e modelo do provedor LLM
- status do sistema (`active` / `inactive`)
- método de extração padrão (`ml` / `llm`)
- logo da aplicação em bytes
- timestamps de criação

A API oferece uma única instância de configurações para o sistema, com endpoints para criar/atualizar, consultar status e armazenar a logo.

### 2) Gestão documental
A área `documents` concentra o cadastro de documentos e seus metadados, incluindo:

- `documentName` como identificador único
- `title`
- `projectManager`
- `costumer`
- `authorId`
- `costumersListId`
- data de criação

Cada documento pode ter múltiplas versões associadas.

### 3) Controle de versões
A entidade `Version` guarda o histórico textual de um documento:

- número de versão
- descrição
- conteúdo em Markdown
- referência ao documento pai
- data de criação

A regra de negócio implementada evita criar uma nova revisão quando a descrição e o conteúdo são idênticos ao último registro.

### 4) Autores e clientes
Os recursos `/authors` e `/costumers-list` gerenciam pessoas envolvidas com os documentos. O nome do segundo endpoint reflete o pacote e a convenção atual do projeto (`costumers-list`, com grafia preservada no código).

### 5) Observabilidade
A aplicação expõe endpoints do MicroProfile Health para uso em Kubernetes/OpenShift:

- `/q/health/live`
- `/q/health/ready`
- `/q/health/started`

## Stack e tecnologias

- Java 25
- Red Hat build of Quarkus 3.33.3.redhat-00001
- REST com Quarkus REST / JAX-RS
- Hibernate ORM + Panache
- PostgreSQL
- OpenAPI / Swagger UI
- Health checks com SmallRye Health
- Helm para deploy no cluster

## Estrutura do projeto

```text
kubeoptix-configurations/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/shiftwise/ai/kubeoptix/
│   │   │       ├── documents/      # Documentos, versões, autores e clientes
│   │   │       ├── health/         # Health checks
│   │   │       └── settings/       # Configuração global e enums
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
├── helm/
│   ├── configurations-api/
│   └── postgresql/
├── Containerfile
├── Containerfile.native
├── compose.yaml
├── install.sh
├── post-install-cleanup.sh
├── mvnw
├── pom.xml
├── README.md
└── database/
    └── kubeoptix-db.dbml
```

## Endpoints principais

### Configurações do sistema

| Método | Endpoint | Descrição |
|---|---|---|
| PUT | `/system-settings` | Cria ou atualiza a única configuração do sistema |
| PATCH | `/system-settings` | Atualiza apenas os campos enviados |
| GET | `/system-settings` | Retorna a configuração completa |
| GET | `/system-settings/status` | Retorna apenas o status |
| PUT | `/system-settings/logo` | Upload da logo em bytes |
| GET | `/system-settings/logo` | Download da logo |
| DELETE | `/system-settings/logo` | Remove a logo |

### Documentos

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/documents` | Lista todos os documentos |
| GET | `/documents/{documentName}` | Busca um documento por nome |
| POST | `/documents` | Cria um documento |
| PUT | `/documents/{documentName}` | Atualiza um documento |
| DELETE | `/documents/{documentName}` | Remove o documento e suas versões |

### Versões

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/versions` | Lista todas as versões |
| GET | `/versions/{id}` | Busca uma versão |
| POST | `/versions` | Cria uma nova versão para um documento |
| PUT | `/versions/{id}` | Atualiza uma versão |
| DELETE | `/versions/{id}` | Remove uma versão |

### Autores e clientes

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/authors` | Lista autores |
| GET | `/authors/{id}` | Busca autor |
| POST | `/authors` | Cria autor |
| PUT | `/authors/{id}` | Atualiza autor |
| DELETE | `/authors/{id}` | Remove autor |
| GET | `/costumers-list` | Lista clientes |
| GET | `/costumers-list/{id}` | Busca cliente |
| POST | `/costumers-list` | Cria cliente |
| PUT | `/costumers-list/{id}` | Atualiza cliente |
| DELETE | `/costumers-list/{id}` | Remove cliente |

## Configuração e ambiente

O arquivo principal de configuração está em `src/main/resources/application.properties`:

```properties
quarkus.application.name=kubeoptix-configurations
quarkus.http.port=8000
quarkus.http.host=0.0.0.0

quarkus.smallrye-openapi.info-title=KubeOptix Configurations API
quarkus.smallrye-openapi.info-version=1.0.0

quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${POSTGRESQL_USER}
quarkus.datasource.password=${POSTGRESQL_PASSWORD}
quarkus.datasource.jdbc.url=jdbc:postgresql://${POSTGRESQL_HOST:kubeoptix-db}:${POSTGRESQL_PORT:5432}/${POSTGRESQL_DATABASE}

%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://${POSTGRESQL_HOST:localhost}:${POSTGRESQL_PORT:5432}/${POSTGRESQL_DATABASE}
quarkus.hibernate-orm.database.generation=drop-and-create
```

Variáveis esperadas:

```bash
POSTGRESQL_USER
POSTGRESQL_PASSWORD
POSTGRESQL_HOST
POSTGRESQL_PORT
POSTGRESQL_DATABASE
```

Na prática, em desenvolvimento local o banco costuma estar em `localhost:5432` ou em um container/compose. Em cluster/OpenShift, normalmente as variáveis são injetadas por secret e pela Helm chart.

## Requisitos

- JDK 25
- Maven 3.9+
- PostgreSQL acessível
- Podman ou Docker para imagens locais
- Helm 3 para deploy em Kubernetes/OpenShift

## Executando localmente

### 1) Preparar o banco
O projeto espera que o PostgreSQL esteja disponível antes da inicialização. O mais simples é usar o compose do repositório:

```bash
podman compose up -d
```

Ou, se preferir só o banco:

```bash
podman compose up -d postgresql
```

### 2) Rodar em modo desenvolvimento

```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./mvnw quarkus:dev
```

A aplicação sobe em:

- http://localhost:8000
- Swagger UI: http://localhost:8000/q/swagger-ui
- Health: http://localhost:8000/q/health

### 3) Validar a API

```bash
curl http://localhost:8000/q/health
curl http://localhost:8000/system-settings
```

> Observação: ao subir a aplicação em modo teste ou dev, o Hibernate tenta criar o schema conforme as entidades. Se o PostgreSQL não estiver acessível, a inicialização falha. Isso também explica por que a suíte de testes exige banco configurado.

## Compilar e testar

### Build da aplicação

```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./mvnw package
```

### Execução de testes

```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./mvnw test
```

### Build nativo

```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk \
PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH \
./mvnw package -Dnative
```

## Containers

### Construir imagem JVM

```bash
podman build -f Containerfile -t kubeoptix-configurations:latest .
```

### Construir imagem nativa

```bash
podman build -f Containerfile.native -t kubeoptix-configurations:native .
```

## Deploy em OpenShift/Kubernetes

Há um script de instalação para provisionar o banco e a aplicação em namespace do cluster:

```bash
./install.sh
```

Parâmetros comuns:

```bash
NAMESPACE=shiftwise-ai \
DATABASE_RELEASE_NAME=kubeoptix-db \
APPLICATION_RELEASE_NAME=kubeoptix-configurations \
INSTALL_DATABASE=true \
INSTALL_APPLICATION=true \
./install.sh
```

A instalação usa os charts em `helm/postgresql` e `helm/configurations-api`, além de verificar acesso ao cluster, secret do repositório de origem e status da build do OpenShift.

## Observações de projeto

- O nome do pacote é `com.shiftwise.ai.kubeoptix`.
- Os comentários e mensagens de código devem seguir o padrão em inglês.
- O endpoint `/costumers-list` foi mantido conforme implementação atual; embora o nome correto em português seja `customers`, o código atual usa a grafia `costumers`.
- A aplicação usa `drop-and-create` no Hibernate em ambiente de desenvolvimento; por isso o banco deve ser resetado ou reprovisionado conforme a necessidade.

## Troubleshooting

### Banco não acessível

Se a aplicação falhar ao iniciar com erros de conexão JDBC, verifique:

```bash
curl http://localhost:8000/q/health
podman ps
podman logs <container>
```

e confirme se as variáveis `POSTGRESQL_*` apontam para o host e porta corretos.

### Build em cluster não finaliza

```bash
oc get builds -n shiftwise-ai
oc logs -n shiftwise-ai bc/kubeoptix-configurations -f
```

### Secret para Git/OpenShift ausente

```bash
oc get secret gitlab -n github-auth
```

Se necessário, crie o secret manualmente antes da instalação.

## Resumo

Essa aplicação é um serviço de configuração e metadados da plataforma KubeOptix, com foco em:

- persistência de configurações globais
- gestão de documentos e versões
- cadastro de autores e clientes
- integração com Kubernetes/OpenShift
- exposição de APIs e health checks para governança operacional

Com essa estrutura, ela funciona como uma API de suporte ao funcionamento e à configuração da plataforma, não apenas como um microserviço de configuração isolada.