# Como contribuir

Este repositório é um componente da plataforma KubeOptix. Use o mesmo fluxo de branches nos repositórios dos demais componentes.

## Estratégia de branches

- `develop` é a branch de integração do trabalho normal.
- `main` é a branch de produção/release e aceita PR somente da `develop` deste repositório.
- Crie funcionalidades e correções comuns como `feature/<descricao-curta>`, a partir de `develop`.
- Correções de produção seguem o mesmo fluxo `feature/*`. A branch `stage` não participa do pipeline.

O GitHub registra a ancestralidade dos commits, não a branch selecionada no momento da criação da branch. O workflow valida nomes e destinos de PR; criar a branch a partir da base correta continua sendo responsabilidade de quem contribui.

## Fluxo de funcionalidades

```text
feature/* -> develop -> main
```

Abra PR de `feature/*` somente para `develop` e promova por PR de `develop` para `main`. PRs executam SAST com Semgrep e scans de dependências/segredos com Trivy, sem compilação, testes ou build de imagem. Achados de segurança ou falhas dos scanners reprovam o check obrigatório `ci-required` e impedem o merge.

## Pull requests e proteções

- Use as branches de origem e destino definidas acima e descreva a alteração, o impacto e as validações realizadas.
- Execute as verificações documentadas no README do componente antes de solicitar o merge.
- O workflow `Validate Branch Flow / check-flow` rejeita destinos de PR inválidos.
- Proteja `main` e `develop`, exigindo PR e os checks `check-flow` e `ci-required`, bloqueando atualizações diretas e exclusão e impedindo bypass de administradores. O workflow sozinho não aplica as proteções do repositório.
- Este guia não define quantidade mínima de aprovações.

## Processo de release

1. Abra PR de `develop` para `main` e faça o merge após revisão e aprovação dos checks de segurança.
2. O push em `main` executa testes, build e scan da imagem e publica automaticamente no Quay como `latest` e `sha-<commit>`. Falhas impedem a publicação.
3. Tags opcionais `vMAJOR.MINOR.PATCH` devem apontar para commits da `main`; o pipeline existente de releases versionadas foi preservado.

## Comandos

Criar uma funcionalidade a partir da `develop` atual:

```bash
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/<descricao-curta>
git push --set-upstream origin feature/<descricao-curta>
```

Abra os PRs conforme o fluxo acima; não envie commits diretamente para `main` ou `develop`.