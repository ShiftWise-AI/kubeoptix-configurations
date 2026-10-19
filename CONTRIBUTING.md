# Como contribuir

Este repositório é um componente da plataforma KubeOptix.

## Branches

- Mantenha apenas `develop` e `main`.
- Inicie as alterações a partir de `develop`; `main` é a branch de produção/release.
- Descreva a alteração, o impacto e as validações ao propor um merge de `develop` para `main`.

## Validação e releases

Execute manualmente as verificações documentadas no README do componente. Build de imagens, publicação e implantação são operações manuais sob responsabilidade dos mantenedores.

## Comandos

```bash
git fetch origin
git switch develop
git pull --ff-only origin develop
```