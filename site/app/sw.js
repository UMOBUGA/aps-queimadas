const VERSAO = 'queimadas-2.3.1';
const ESSENCIAIS = [
  './', 'index.html', 'app.css', 'app.js', 'manifest.webmanifest',
  'vendor/leaflet.js', 'vendor/leaflet.css', 'vendor/mundo.js',
  'dados/resumo.json', 'dados/focos.json', 'dados/municipios.json', 'dados/algoritmos.json', 'dados/malha.geojson',
  'dados/historia.json', 'dados/ml.json', 'dados/passos.json',
  'icones/icone-192.png', '../favicon.svg',
  '../fonts/BigShouldersDisplay-Black.woff2', '../fonts/BigShouldersDisplay-ExtraBold.woff2',
  '../fonts/Inter-Regular.woff2', '../fonts/Inter-SemiBold.woff2'
];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(VERSAO).then(c => c.addAll(ESSENCIAIS)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(chaves => Promise.all(chaves.filter(k => k !== VERSAO).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;
  e.respondWith(
    fetch(e.request, { cache: 'no-cache' })
      .then(r => {
        if (r.ok) {
          const copia = r.clone();
          caches.open(VERSAO).then(c => c.put(e.request, copia));
        }
        return r;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }).then(r => r || caches.match('index.html')))
  );
});
