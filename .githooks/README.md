# Proteção de credenciais e sincronização operacional

O KRONOS versiona `cache/`, `logs/` e `relatorios/` para permitir continuidade
entre computadores. Arquivos locais de configuração e credenciais permanecem
ignorados pelo Git.

Depois de clonar o repositório em outra máquina, ative o guardião versionado:

```powershell
git config core.hooksPath .githooks
```

Antes de trocar de computador, encerre o KRONOS para concluir as escritas e use:

```powershell
git add cache logs relatorios
git commit -m "chore: atualiza estado operacional do KRONOS"
git push
```

Na máquina de destino, com o KRONOS encerrado:

```powershell
git pull --ff-only
git config core.hooksPath .githooks
```

O pre-commit bloqueia padrões conhecidos de chaves de API, tokens e senhas sem
imprimir o valor detectado. `application-local.yml`, `.env`, certificados,
arquivos de credenciais e `backups/` não entram no repositório.
