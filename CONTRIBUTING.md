# Como contribuir

Este repositório é um componente da plataforma KubeOptix. Use o mesmo fluxo de branches nos repositórios dos demais componentes.

## Estratégia de branches

- `develop` é a branch de integração do trabalho normal.
- `stage` é a branch de candidato a release e aceita PR somente de `develop`.
- `main` é a branch de produção/release. Aceita PR de `stage` ou `hotfix/*`.
- Crie funcionalidades e correções comuns como `feature/<descricao-curta>`, a partir de `develop`.
- Crie correções de produção como `hotfix/<descricao-curta>`, a partir da `main` mais recente.

O GitHub registra a ancestralidade dos commits, não a branch selecionada no momento da criação da branch. O workflow valida nomes e destinos de PR; criar a branch a partir da base correta continua sendo responsabilidade de quem contribui.

## Fluxo de funcionalidades

```text
feature/* -> develop -> stage -> main
```

Abra PR de `feature/*` somente para `develop`. Promova uma release por PR de `develop` para `stage`, valide nesse ambiente e então abra PR de `stage` para `main`.

## Fluxo de hotfix

```text
hotfix/* -> main
hotfix/* -> develop -> stage
```

Crie o hotfix a partir de `main` e abra PRs dessa branch para `main` e `develop`. Não abra PR de hotfix diretamente para `stage`; `stage` aceita somente `develop`. Use merge commits nos dois PRs do hotfix, mantenha a branch até ambos serem integrados e evite aplicar a mesma correção com cherry-pick em commits separados. Promova `develop` para `stage` pelo PR normal de release. Revise esse PR com cuidado: ele inclui todas as alterações de `develop` que ainda não estão em `stage`, não apenas o hotfix.

## Pull requests e proteções

- Use as branches de origem e destino definidas acima e descreva a alteração, o impacto e as validações realizadas.
- Execute as verificações documentadas no README do componente antes de solicitar o merge.
- O workflow `Validate Branch Flow / check-flow` rejeita destinos de PR inválidos.
- Administradores do repositório devem habilitar Rulesets ou Branch Protection no GitHub para `main`, `stage` e `develop`, exigindo PR e o status check `Validate Branch Flow / check-flow`, bloqueando atualizações diretas e exclusão e impedindo bypass. O workflow sozinho não bloqueia push direto nem exclusão de branches.
- Este guia não define quantidade mínima de aprovações.

## Processo de release

1. Abra PR de `develop` para `stage` para preparar o candidato a release.
2. Valide o candidato no ambiente de stage e resolva bloqueios de release.
3. Abra PR de `stage` para `main` e faça o merge após a aprovação da release.
4. Crie uma tag no commit publicado em `main` no formato `vMAJOR.MINOR.PATCH` e publique a GitHub Release correspondente.

## Comandos

Criar uma funcionalidade a partir da `develop` atual:

```bash
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/<descricao-curta>
git push --set-upstream origin feature/<descricao-curta>
```

Criar um hotfix a partir da `main` atual:

```bash
git fetch origin
git switch main
git pull --ff-only origin main
git switch -c hotfix/<descricao-curta>
git push --set-upstream origin hotfix/<descricao-curta>
```

Abra os PRs conforme os fluxos acima; não envie commits diretamente para `main`, `stage` ou `develop`.