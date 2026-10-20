# Como contribuir

Este repositório é um componente da plataforma KubeOptix.

## Branches

- Mantenha apenas `develop` e `main`.
- Inicie as alterações a partir de `develop`; `main` é a branch de produção/release.
- Descreva a alteração, o impacto e as validações ao propor um merge de `develop` para `main`.

## CI e releases

- Abra PRs locais `feature/*` ou `fix/*` para `develop`; para `main`, use `develop` ou `fix/*`.
- O check `check-flow` valida o repositório de origem e a rota do PR. O check `ci-required` exige a validação Java 25/Maven, os testes com PostgreSQL efêmero, actionlint, Trivy, build do `Containerfile`, execução não-root e scan da imagem.
- Um merge válido em `main` inicia a release protegida no ambiente `quay-release`. O pipeline reserva uma versão SemVer de patch em um commit/tag separado, testa exatamente essa fonte e publica no Quay as referências de versão e `sha-<commit>`.
- `latest` e a proposta de atualização de `ConfigurationsImage` no Operator só avançam para a release mais recente cuja fonte ainda é a ponta de `main`. O pipeline abre ou retoma um PR no Operator; não faz merge nem deploy no OpenShift.
- Para recuperar uma execução, use `workflow_dispatch` em `main` com o SHA completo do merge original. Não apague nem mova tags de release.

Antes da primeira release, configure o ambiente `quay-release`, seus revisores e os secrets exigidos conforme [o runbook de CI/CD](docs/ci-cd.md). O build e a publicação são automáticos; a aprovação do ambiente e o merge do PR no Operator continuam manuais.

## Comandos

```bash
git fetch origin
git switch develop
git pull --ff-only origin develop
```