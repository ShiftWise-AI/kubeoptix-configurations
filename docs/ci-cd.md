# CI/CD: kubeoptix-configurations

Este documento descreve os passos administrativos necessários para habilitar a publicação do workflow em `.github/workflows/ci-release.yml`. O workflow valida PRs, testa Java 25 com PostgreSQL efêmero, escaneia fontes e imagem, publica no Quay e propõe uma atualização pinada por digest no Operator. Ele não implanta no OpenShift.

## Estado remoto verificado

- GitHub Actions está habilitado e permite actions de terceiros.
- `main` e `develop` exigem os checks `check-flow` e `ci-required` e exigem branches atualizadas.
- As permissões padrão do `GITHUB_TOKEN` são somente leitura; o workflow também declara permissões por job. O token não pode aprovar PRs.
- `main` e `develop` ainda não exigem aprovação nem revisão de CODEOWNER.
- O ambiente `quay-release` não existe. O ambiente `copilot` existente não é usado por este pipeline.
- Não foram encontrados repository secrets, CODEOWNERS ou rulesets de proteção de tags.

## Configuração antes do primeiro merge em main

1. Em **Settings > Environments**, crie `quay-release`. Restrinja deployments à branch `main`, exija um revisor designado e impeça autoaprovação. Não conceda acesso aos secrets em workflows de PR.
2. Crie estes **environment secrets** dentro de `quay-release`:
   - `QUAY_USERNAME`: usuário de uma conta robot do Quay.
   - `QUAY_PASSWORD`: token dessa conta robot.
   - `OPERATOR_TOKEN`: token fine-grained para `ShiftWise-AI/kubeoptix-operator`, com `Contents: read/write` e `Pull requests: read/write`; mantenha prazo de expiração e aprovação da organização. Um GitHub App de escopo equivalente pode substituí-lo.
3. Limite a conta robot a escrita em `parraes/kubeoptix-configurations`. No Quay, habilite tags imutáveis para versões e confirme que o robô consegue consultar, publicar e baixar a imagem.
4. Em **Settings > Rules > Rulesets**, proteja tags `v*`: impeça atualização e exclusão de releases. Garanta que o workflow autorizado consiga criar novas tags sem permitir que usuários movam ou apaguem tags existentes.
5. Preserve os checks obrigatórios já configurados em `main` e `develop`. Ative pelo menos uma aprovação, descarte aprovações obsoletas, exija aprovação do último push, exija revisão de CODEOWNER e mantenha a política estrita de branch atualizada. Desabilite force-push e exclusão das branches protegidas e não configure bypasses gerais.
6. Crie `.github/CODEOWNERS` com os mantenedores atuais dos workflows e do processo de release. O arquivo ainda não existe no remoto; confirme os logins antes de adicioná-los. A regra de proteção de CODEOWNER não terá efeito sem esse arquivo.
7. Confira que o Operator tem CI obrigatório em `main` e que o token de `OPERATOR_TOKEN` pode criar branch e PR sem contornar suas proteções. A automação só abre ou retoma `fix/configurations-<versão>`; não faz merge.

## Primeiro fluxo e operação

1. Entregue estas alterações por um PR local `feature/*` para `develop`; os checks obrigatórios continuam chamados `check-flow` e `ci-required`.
2. Depois do merge em `develop`, abra um PR `develop` para `main`. Configure e teste o ambiente/secrets antes de aprovar qualquer release.
3. Após o merge em `main`, aprove o deployment no ambiente `quay-release`. A reserva cria uma tag SemVer anotada e um commit filho contendo apenas a versão do `pom.xml` e o `info-version` do OpenAPI. A versão é testada, construída e publicada com tags imutáveis, `sha-<commit>` e, se ainda for a ponta de `main`, `latest`.
4. Revise e mescle manualmente o PR de atualização criado no Operator. A implantação continua pelo processo operacional do Operator.
5. Para recuperar uma execução, execute `CI and Quay release` por `workflow_dispatch` em `main`, informando o SHA completo do merge original em `source`. Não remova reservas nem reutilize uma versão para outra fonte.

O repositório já tem tags até `v1.0.5`; com a versão atual `1.0.0` no `pom.xml`, a primeira reserva esperada é `v1.0.6`. A criação efetiva da release depende do merge aprovado em `main`, dos secrets, do ambiente e da aceitação do campo `queue: max` pelo GitHub Actions.