# Funko Tracker

Catálogo de Funko Pops para iPhone, feito como PWA. Organize sua coleção e sua wishlist, acompanhe quanto pagou e quanto a estante vale, e use tudo offline.

- Sem backend, sem login e sem dependências: HTML, CSS e JavaScript puro.
- Os dados ficam no próprio aparelho (`localStorage`).
- Todos os caminhos são relativos (`./`), então o app funciona em subpastas, como `usuario.github.io/funko-tracker/`.

## Arquivos

```
funko-tracker/
├── index.html              app completo (CSS e JS embutidos)
├── manifest.json           nome, cores e ícones do app instalado
├── service-worker.js       cache offline (cache-first)
├── README.md
├── tools/gerar-icones.js   gera os PNGs dos ícones (Node, sem dependências)
└── icons/
    ├── icon-192.png
    ├── icon-512.png
    └── apple-touch-icon.png
```

Para refazer os ícones depois de mudar o desenho ou as cores no script:

```bash
node tools/gerar-icones.js
```

## 1. Rodar localmente no VS Code

O service worker só funciona por `http://localhost` ou `https://`. Abrir o `index.html` direto (`file://`) não ativa o modo offline.

**Opção A: extensão Live Server**
1. Instale a extensão **Live Server** (Ritwick Dey) no VS Code.
2. Abra a pasta `funko-tracker`.
3. Clique com o botão direito em `index.html` → **Open with Live Server**.

**Opção B: Python**
```bash
cd funko-tracker
python3 -m http.server 8080
```
Acesse `http://localhost:8080/`.

Para testar no iPhone pela rede de casa, use o IP do computador (`http://192.168.x.x:8080/`). Por HTTP sem localhost o Safari não registra o service worker, então o modo offline só vale depois de publicar no GitHub Pages.

## 2. Publicar no GitHub Pages

1. No GitHub, crie um repositório **público** chamado `funko-tracker`, sem README.
2. No terminal, dentro da pasta `funko-tracker`:
   ```bash
   git init
   git add .
   git commit -m "Funko Tracker"
   git branch -M main
   git remote add origin https://github.com/SEU-USUARIO/funko-tracker.git
   git push -u origin main
   ```
3. No repositório, abra **Settings → Pages**.
4. Em **Build and deployment**, escolha **Source: Deploy from a branch**, branch **main** e pasta **/ (root)**. Clique em **Save**.
5. Espere de 1 a 2 minutos e acesse `https://SEU-USUARIO.github.io/funko-tracker/`.

## 3. Instalar no iPhone

1. Abra o link no **Safari**. Outros navegadores do iOS não instalam PWAs da mesma forma.
2. Toque em **Compartilhar** (o quadrado com a seta para cima).
3. Role e toque em **Adicionar à Tela de Início**.
4. Confirme o nome **Funko** e toque em **Adicionar**.

O app abre em tela cheia, sem a barra do Safari. Depois da primeira abertura, ele funciona sem internet.

## 4. Atualizar o app depois

O service worker guarda os arquivos em cache. Para o iPhone baixar uma versão nova:

1. Altere os arquivos.
2. Em `service-worker.js`, aumente a versão do cache:
   ```js
   const CACHE_NAME = 'funko-v2'; // era funko-v1
   ```
3. Se criar arquivos novos, inclua cada um na lista `ARQUIVOS` do mesmo arquivo, sempre com `./` na frente.
4. Faça commit e push.
5. No iPhone, abra o app, feche-o pelo seletor de apps e abra de novo. A versão nova entra na segunda abertura.

Os dados da coleção não são apagados na atualização: eles ficam no `localStorage`, separados do cache.

## 5. Faça backup

O Safari pode **apagar o armazenamento de sites e apps da Tela de Início que ficam sem uso** por algumas semanas. Apagar o app da Tela de Início, limpar os dados do Safari ou trocar de iPhone também apaga a coleção.

Para não perder nada:

1. Abra a aba **Dashboard**.
2. Toque em **Exportar backup (JSON)**.
3. No menu de compartilhar, escolha **Salvar em Arquivos** e guarde no **iCloud Drive**.

Para restaurar, toque em **Importar backup** e escolha o arquivo `.json`. O app mostra quantos Funkos o backup tem e pede confirmação antes de substituir os dados atuais.

As fotos ficam dentro do backup. O navegador costuma limitar o `localStorage` a cerca de 5 MB. Cada foto é reduzida para no máximo 800 px e costuma ocupar algo entre 50 e 150 KB, o que dá algumas dezenas de Funkos com foto. Se o espaço acabar, o app avisa e não perde o que já estava salvo.
