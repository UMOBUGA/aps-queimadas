(function () {
  'use strict';

  var MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  var INFERNO = [[0, '#2A0B4A'], [0.15, '#5C1370'], [0.3, '#932667'], [0.45, '#C73E4C'], [0.6, '#EB6A26'], [0.8, '#FBA40A'], [1, '#FCFFA4']];
  var SP = [[-25.35, -53.15], [-19.75, -44.15]];
  var ESRI = 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/';
  var escuro = window.matchMedia('(prefers-color-scheme: dark)');
  var semMovimento = window.matchMedia('(prefers-reduced-motion: reduce)');

  var d = { resumo: null, focos: null, municipios: [], algoritmos: null, malha: null };
  var porChave = {}, porNome = {};
  var ui = { aba: null, de: -1, ate: -1, relogio: null, limite: 40, tabela: 0, ordem: 'ms', curva: 'Merge Sort', folha: null };
  var mapa = null, camadaPontos = null, camadaFundo = null, camadaDestaque = null, base = null, offline = false;

  function $(s, r) { return (r || document).querySelector(s); }
  function el(tag, cls, html) { var e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; }
  function fmt(n) { return Number(n).toLocaleString('pt-BR'); }
  function dec(n, c) { return Number(n).toLocaleString('pt-BR', { minimumFractionDigits: c, maximumFractionDigits: c }); }
  function esc(s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function semAcentos(s) { return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase(); }
  function css(v) { return getComputedStyle(document.documentElement).getPropertyValue(v).trim(); }
  function plural(n, um, varios) { return fmt(n) + ' ' + (n === 1 ? um : varios); }
  function mesRotulo(i) { var a = d.resumo.anoInicial + Math.floor(i / 12); return MESES[i % 12] + '/' + a; }
  function mesChave(i) { return (d.resumo.anoInicial + Math.floor(i / 12)) * 100 + (i % 12) + 1; }

  function hex(h) { return [1, 3, 5].map(function (i) { return parseInt(h.substr(i, 2), 16); }); }
  function inferno(t) {
    t = Math.max(0, Math.min(1, t));
    for (var i = 1; i < INFERNO.length; i++) {
      if (t <= INFERNO[i][0]) {
        var a = INFERNO[i - 1], b = INFERNO[i], f = (t - a[0]) / (b[0] - a[0]);
        var ca = hex(a[1]), cb = hex(b[1]);
        return 'rgb(' + ca.map(function (v, k) { return Math.round(v + (cb[k] - v) * f); }).join(',') + ')';
      }
    }
    return INFERNO[INFERNO.length - 1][1];
  }
  function calor(v, max) { return v === 0 ? css('--superficie-2') : inferno(Math.sqrt(v / max)); }

  function aviso(texto) {
    var a = $('#aviso');
    a.textContent = texto;
    a.hidden = false;
    clearTimeout(aviso.t);
    aviso.t = setTimeout(function () { a.hidden = true; }, 3200);
  }

  function json(url) {
    return fetch(url).then(function (r) {
      if (!r.ok) throw new Error(url + ': ' + r.status);
      return r.json();
    });
  }

  function iniciar() {
    Promise.all([json('dados/resumo.json'), json('dados/focos.json'), json('dados/municipios.json'), json('dados/algoritmos.json')])
      .then(function (r) {
        d.resumo = r[0]; d.focos = r[1]; d.municipios = r[2]; d.algoritmos = r[3];
        d.municipios.forEach(function (m) { porChave[m.chave] = m; porNome[m.nome] = m; });
        montarFaixa();
        montarMunicipios();
        montarAlgoritmos();
        montarSobre();
        window.addEventListener('hashchange', rota);
        rota();
      })
      .catch(function (e) {
        document.getElementById('conteudo').innerHTML = '<div class="pagina"><h1 class="titulo">Sem dados</h1><p class="texto">'
          + 'Não foi possível carregar os dados. Verifique a conexão e tente de novo.</p><p class="nota">' + esc(e.message) + '</p></div>';
      });
  }

  function rota() {
    var alvo = (location.hash || '#mapa').slice(1);
    if (['mapa', 'municipios', 'algoritmos', 'sobre'].indexOf(alvo) < 0) alvo = 'mapa';
    ui.aba = alvo;
    document.querySelectorAll('.aba').forEach(function (s) { s.hidden = s.id !== 'aba-' + alvo; });
    document.querySelectorAll('.abas a').forEach(function (a) {
      if (a.dataset.aba === alvo) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
    if (alvo === 'algoritmos') desenharCurvas();
    if (alvo === 'mapa') {
      if (!mapa) montarMapa(); else mapa.invalidateSize();
    } else {
      pausar();
      window.scrollTo(0, 0);
    }
  }

  function montarMapa() {
    mapa = L.map('mapa', { preferCanvas: true, zoomControl: false, worldCopyJump: true, maxZoom: 16, zoomSnap: 0.25 });
    L.control.zoom({ position: 'topright' }).addTo(mapa);
    enquadrarSP();
    camadaFundo = L.layerGroup().addTo(mapa);
    camadaPontos = L.layerGroup().addTo(mapa);
    camadaDestaque = L.layerGroup().addTo(mapa);
    aplicarBase();
    escuro.addEventListener('change', function () { aplicarBase(); desenharPontos(); if (offline) desenharFundo(); });
    setTimeout(function () { if (base && !base._carregou) entrarOffline(); }, 7000);
    desenharPontos();
  }

  function enquadrarSP() {
    mapa.fitBounds(SP, { paddingTopLeft: [4, 70], paddingBottomRight: [4, 120] });
  }

  function aplicarBase() {
    if (base) mapa.removeLayer(base);
    if (offline) return;
    var carregou = base && base._carregou, erros = 0;
    base = L.tileLayer(ESRI + (escuro.matches ? 'World_Dark_Gray_Base' : 'World_Light_Gray_Base') + '/MapServer/tile/{z}/{y}/{x}', {
      maxNativeZoom: 16, maxZoom: 16,
      attribution: 'Mapa-base &copy; Esri · focos: INPE · malha: IBGE'
    });
    base._carregou = carregou;
    base.on('tileload', function () { base._carregou = true; });
    base.on('tileerror', function () { erros++; if (!base._carregou && erros >= 3) entrarOffline(); });
    base.addTo(mapa);
  }

  function entrarOffline() {
    if (offline) return;
    offline = true;
    if (base) mapa.removeLayer(base);
    $('#mapa').classList.add('offline');
    aviso('Sem internet: mapa com países e municípios embutidos');
    Promise.all([carregarMundo(), carregarMalha()]).then(function () { desenharFundo(); desenharPontos(); });
  }

  function carregarMundo() {
    if (window.MUNDO) return Promise.resolve();
    return new Promise(function (ok) {
      var s = document.createElement('script');
      s.src = 'vendor/mundo.js';
      s.onload = ok;
      s.onerror = ok;
      document.head.appendChild(s);
    });
  }

  function carregarMalha() {
    if (d.malha) return Promise.resolve(d.malha);
    return json('dados/malha.geojson').then(function (g) { d.malha = g; return g; });
  }

  function desenharFundo() {
    camadaFundo.clearLayers();
    if (window.MUNDO) {
      L.geoJSON(window.MUNDO, { interactive: false, style: function () {
        return { color: css('--pais-borda'), weight: 0.7, fillColor: css('--pais'), fillOpacity: 1 };
      } }).addTo(camadaFundo);
    }
    if (d.malha) {
      L.geoJSON(d.malha, { interactive: false, style: function () {
        return { color: css('--terra-borda'), weight: 0.5, fillColor: css('--terra'), fillOpacity: 1 };
      } }).addTo(camadaFundo);
    }
  }

  function focosVisiveis() {
    var f = d.focos.focos;
    if (ui.de < 0) return f;
    var de = mesChave(ui.de), ate = mesChave(ui.ate);
    return f.filter(function (x) { return x[4] >= de && x[4] <= ate; });
  }

  function corBioma(i) {
    var nome = d.focos.biomas[i];
    return nome === 'Cerrado' ? css('--cerrado') : nome === 'Mata Atlântica' ? css('--mata') : css('--outro');
  }

  function desenharPontos() {
    if (!mapa) return;
    camadaPontos.clearLayers();
    var borda = escuro.matches ? '#0A0807' : '#FFFFFF';
    var cores = d.focos.biomas.map(function (b, i) { return corBioma(i); });
    var focos = focosVisiveis();
    var raio = focos.length > 4000 ? 3 : 4.5;
    focos.forEach(function (f) {
      L.circleMarker([f[0], f[1]], { radius: raio, weight: 0.8, color: borda, fillColor: cores[f[2]], fillOpacity: 0.9 })
        .on('click', function (e) { abrirPopup(f, e.latlng); })
        .addTo(camadaPontos);
    });
    atualizarPlacar(focos.length);
  }

  function abrirPopup(f, latlng) {
    var m = d.municipios[f[3]];
    var html = '<div class="pop-titulo">' + esc(m ? m.nome : 'Município não identificado') + '</div>'
      + '<div class="pop-linha">' + esc(f[5]) + ' (GMT)</div>'
      + '<div class="pop-linha">' + esc(d.focos.biomas[f[2]]) + '</div>'
      + (m ? '<button class="pop-acao" type="button">Ficha do município →</button>' : '');
    var p = L.popup({ maxWidth: 260 }).setLatLng(latlng).setContent(html).openOn(mapa);
    var b = p.getElement() && p.getElement().querySelector('.pop-acao');
    if (b) b.addEventListener('click', function () { mapa.closePopup(); abrirFicha(m); });
  }

  function atualizarPlacar(n) {
    $('#placar-numero').textContent = fmt(n);
    var t = ui.de < 0 ? 'no mapa' : ui.de === ui.ate ? 'em ' + mesRotulo(ui.de) : 'de ' + mesRotulo(ui.de) + ' a ' + mesRotulo(ui.ate);
    $('#placar-texto').textContent = (n === 1 ? 'foco ' : 'focos ') + t;
  }

  function montarFaixa() {
    var faixa = $('#faixa'), meses = d.resumo.meses, max = Math.max.apply(null, meses.concat([1]));
    meses.forEach(function (n, i) {
      var b = el('button');
      b.type = 'button';
      b.style.background = calor(n, max);
      b.setAttribute('aria-label', mesRotulo(i) + ': ' + plural(n, 'foco', 'focos'));
      b.setAttribute('aria-pressed', 'false');
      b.title = mesRotulo(i) + ': ' + plural(n, 'foco', 'focos');
      b.addEventListener('click', function () { pausar(); escolherMes(ui.de === i && ui.ate === i ? -1 : i); });
      faixa.appendChild(b);
    });
    var rot = el('div', 'faixa-rotulos');
    meses.forEach(function (n, i) {
      rot.appendChild(el('span', '', i % 12 === 0 ? '<b>' + (d.resumo.anoInicial + i / 12) + '</b>' : i % 3 === 0 ? MESES[i % 12] : ''));
    });
    faixa.after(rot);
    $('#tocar').addEventListener('click', function () { if (ui.relogio) pausar(); else tocar(); });
    $('#todos').addEventListener('click', function () { pausar(); escolherMes(-1); });
  }

  function escolherMes(i, fim) {
    ui.de = i;
    ui.ate = i < 0 ? -1 : (fim == null ? i : fim);
    var botoes = document.querySelectorAll('#faixa button');
    $('#faixa').classList.toggle('selecao', i >= 0);
    botoes.forEach(function (b, k) { b.setAttribute('aria-pressed', String(i >= 0 && k >= ui.de && k <= ui.ate)); });
    $('#periodo').textContent = i < 0 ? 'Todos os meses' : ui.de === ui.ate ? mesRotulo(i) : mesRotulo(ui.de) + ' – ' + mesRotulo(ui.ate);
    $('#todos').disabled = i < 0;
    if (mapa) { mapa.closePopup(); desenharPontos(); }
  }

  function tocar() {
    var n = d.resumo.meses.length;
    escolherMes(ui.de < 0 || ui.de >= n - 1 || ui.de !== ui.ate ? 0 : ui.de + 1);
    botaoTocar(true);
    ui.relogio = setInterval(function () {
      if (ui.de + 1 >= n) { pausar(); return; }
      escolherMes(ui.de + 1);
      if (ui.de + 1 >= n) pausar();
    }, 900);
  }

  function pausar() {
    if (ui.relogio) clearInterval(ui.relogio);
    ui.relogio = null;
    botaoTocar(false);
  }

  function botaoTocar(tocando) {
    var b = $('#tocar');
    b.querySelector('use').setAttribute('href', tocando ? '#i-pausa' : '#i-play');
    b.querySelector('span').textContent = tocando ? 'Pausar' : 'Tocar';
  }

  function mini(meses) {
    var max = Math.max.apply(null, meses.concat([1]));
    return '<span class="mini" aria-hidden="true">' + meses.map(function (v) {
      return '<i style="height:' + (v === 0 ? 0 : Math.max(8, 100 * v / max)) + '%"></i>';
    }).join('') + '</span>';
  }

  function montarMunicipios() {
    var r = d.resumo;
    $('#municipios-apoio').textContent = fmt(r.municipiosComFocos) + ' dos ' + fmt(r.municipiosNaMalha)
      + ' municípios paulistas tiveram focos em ' + r.anos[0].ano + '–' + r.anos[r.anos.length - 1].ano + '. Toque num nome para abrir a ficha.';
    $('#busca').addEventListener('input', function () { ui.limite = 40; listarMunicipios(); });
    $('#mais').addEventListener('click', function () { ui.limite += 60; listarMunicipios(); });
    listarMunicipios();
  }

  function listarMunicipios() {
    var q = semAcentos($('#busca').value.trim());
    var lista = q ? d.municipios.filter(function (m) { return m.busca.indexOf(q) >= 0; }) : d.municipios;
    var ol = $('#ranking');
    ol.innerHTML = '';
    lista.slice(0, ui.limite).forEach(function (m) {
      var li = el('li');
      var b = el('button', '', '<span class="pos">' + m.posicao + 'º</span><span><span class="nome">' + esc(m.nome) + '</span>'
        + mini(m.meses) + '</span><span class="total">' + fmt(m.total) + '<small>' + (m.total === 1 ? 'foco' : 'focos') + '</small></span>');
      b.type = 'button';
      b.setAttribute('aria-label', m.posicao + 'º, ' + m.nome + ', ' + plural(m.total, 'foco', 'focos'));
      b.addEventListener('click', function () { abrirFicha(m); });
      li.appendChild(b);
      ol.appendChild(li);
    });
    if (!lista.length) ol.innerHTML = '<li class="nota" style="padding:16px 0">Nenhum município com esse nome teve focos.</li>';
    $('#mais').hidden = lista.length <= ui.limite;
  }

  function variacao(m) {
    if (m.anos.length < 2 || m.anos[0] === 0) return '—';
    var v = 100 * (m.anos[m.anos.length - 1] - m.anos[0]) / m.anos[0];
    return (v >= 0 ? '+' : '−') + dec(Math.abs(v), 0) + '%';
  }
  function pico(m) {
    var i = -1;
    m.meses.forEach(function (v, k) { if (v > 0 && (i < 0 || v > m.meses[i])) i = k; });
    return i;
  }
  function densidade(m) { return m.densidade == null ? '—' : dec(m.densidade, 1); }

  function abrirFolha(titulo, sobre, larga) {
    var f = $('#folha'), fundo = $('#fundo-folha');
    $('#folha-titulo').textContent = titulo;
    $('#folha-sobre').textContent = sobre;
    f.classList.toggle('larga', !!larga);
    if (!ui.folha) {
      history.pushState({ folha: true }, '', location.hash || '#mapa');
      f.hidden = false;
      fundo.hidden = false;
      f.style.transform = '';
      requestAnimationFrame(function () { requestAnimationFrame(function () { f.classList.add('aberta'); fundo.classList.add('aberta'); }); });
      ui.folha = document.activeElement;
    }
    $('#folha-corpo').scrollTop = 0;
    setTimeout(function () { $('#fechar-folha').focus({ preventScroll: true }); }, 60);
    return $('#folha-corpo');
  }

  function fecharFolha(daHistoria) {
    if (!ui.folha) return;
    var f = $('#folha'), fundo = $('#fundo-folha'), volta = ui.folha;
    ui.folha = null;
    f.classList.remove('aberta');
    fundo.classList.remove('aberta');
    setTimeout(function () { f.hidden = true; fundo.hidden = true; f.style.transform = ''; }, semMovimento.matches ? 0 : 320);
    if (camadaDestaque) camadaDestaque.clearLayers();
    if (!daHistoria && history.state && history.state.folha) history.back();
    if (volta && volta.focus) volta.focus({ preventScroll: true });
  }

  function abrirFicha(m) {
    var corpo = abrirFolha(m.nome, 'Ficha do município', false);
    var pk = pico(m), max = Math.max.apply(null, m.meses.concat([1]));
    var anos = d.resumo.anos.map(function (a, i) { return '<div><b>' + fmt(m.anos[i] || 0) + '</b><span>' + a.ano + '</span></div>'; }).join('');
    var html = '<div class="ficha-total">' + fmt(m.total) + '</div>'
      + '<div class="ficha-sub">' + (m.total === 1 ? 'foco' : 'focos') + ' de ' + d.resumo.anos[0].ano + ' a ' + d.resumo.anos[d.resumo.anos.length - 1].ano + '</div>'
      + '<p class="ficha-ranking">' + m.posicao + 'º município com mais focos, entre ' + fmt(d.resumo.municipiosComFocos) + ' com registro.</p>'
      + '<div class="ficha-grade">' + anos + '<div><b>' + variacao(m) + '</b><span>variação</span></div>'
      + '<div class="largo"><b>' + esc(m.bioma || '—') + '</b><span>bioma principal</span></div>'
      + '<div><b>' + densidade(m) + '</b><span>por 1.000 km²</span></div></div>'
      + '<div class="secao-folha"><h3>Focos por mês</h3><p>Toque num mês para vê-lo no mapa.</p></div>'
      + '<div class="barras" role="list">' + m.meses.map(function (v, i) {
        return '<i role="listitem" data-mes="' + i + '" title="' + mesRotulo(i) + ': ' + fmt(v) + '" aria-label="' + mesRotulo(i) + ': ' + plural(v, 'foco', 'focos')
          + '" style="height:' + (v === 0 ? 0 : Math.max(3, 100 * v / max)) + '%;background:' + calor(v, max) + '"></i>';
      }).join('') + '</div>' + rotulosMeses()
      + (pk >= 0 ? '<p class="nota">Pico em ' + mesRotulo(pk) + ', com ' + plural(m.meses[pk], 'foco', 'focos') + '.</p>' : '')
      + '<div class="secao-folha"><h3>Vizinhos com focos</h3><p>' + (m.nVizinhos ? m.vizinhosComFocos + ' de ' + m.nVizinhos + ' vizinhos (malha do IBGE) tiveram focos.' : 'Sem vizinhos na malha do IBGE.') + '</p></div>'
      + '<ul class="vizinhos">' + vizinhos(m) + '</ul>'
      + '<div class="botoes-folha"><button class="botao primario" type="button" data-acao="mapa"><svg aria-hidden="true"><use href="#i-alvo"/></svg>Ver no mapa</button></div>'
      + '<div class="secao-folha"><h3>Comparar</h3><p>Coloque este município lado a lado com outro.</p></div>'
      + '<div class="comparar"><label class="busca"><svg aria-hidden="true"><use href="#i-busca"/></svg><span class="sr">Comparar com</span>'
      + '<input type="search" placeholder="Digite outro município…" autocomplete="off" enterkeyhint="search"></label><ul class="sugestoes" hidden></ul></div>';
    corpo.innerHTML = html;
    corpo.querySelector('[data-acao="mapa"]').addEventListener('click', function () { verNoMapa([m]); });
    corpo.querySelectorAll('.barras i').forEach(function (i) {
      i.addEventListener('click', function () { verNoMapa([m], +i.dataset.mes); });
    });
    corpo.querySelectorAll('.vizinhos button').forEach(function (b) {
      b.addEventListener('click', function () { var v = porNome[b.dataset.nome]; if (v) abrirFicha(v); });
    });
    ligarComparacao(corpo, m);
  }

  function rotulosMeses() {
    return '<div class="barras-rotulos" aria-hidden="true">' + d.resumo.meses.map(function (v, i) {
      return '<span>' + (i % 12 === 0 ? '<b>' + (d.resumo.anoInicial + i / 12) + '</b>' : i % 3 === 0 ? MESES[i % 12] : '') + '</span>';
    }).join('') + '</div>';
  }

  function vizinhos(m) {
    var max = m.vizinhos.length ? m.vizinhos[0][1] : 1;
    return m.vizinhos.map(function (v) {
      return '<li><button type="button" data-nome="' + esc(v[0]) + '">' + esc(v[0]) + '</button><i style="width:' + Math.max(4, 100 * v[1] / max)
        + '%"></i><b>' + fmt(v[1]) + '</b></li>';
    }).join('');
  }

  function ligarComparacao(corpo, m) {
    var input = corpo.querySelector('.comparar input'), lista = corpo.querySelector('.sugestoes');
    input.addEventListener('input', function () {
      var q = semAcentos(input.value.trim());
      lista.innerHTML = '';
      if (!q) { lista.hidden = true; return; }
      var inicio = [], meio = [];
      d.municipios.forEach(function (x) {
        if (x === m) return;
        if (x.busca.indexOf(q) === 0) inicio.push(x); else if (x.busca.indexOf(q) > 0) meio.push(x);
      });
      inicio.concat(meio).slice(0, 6).forEach(function (x) {
        var li = el('li'), b = el('button', '', '<span>' + esc(x.nome) + '</span><small>' + plural(x.total, 'foco', 'focos') + '</small>');
        b.type = 'button';
        b.addEventListener('click', function () { abrirComparacao(m, x); });
        li.appendChild(b);
        lista.appendChild(li);
      });
      lista.hidden = !lista.children.length;
    });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { var b = lista.querySelector('button'); if (b) b.click(); }
    });
  }

  function abrirComparacao(a, b) {
    var corpo = abrirFolha(a.nome + ' × ' + b.nome, 'Comparação de municípios', true);
    var max = 1;
    a.meses.forEach(function (v, i) { max = Math.max(max, v, b.meses[i]); });
    var linhas = [['Focos', '<span class="grande">' + fmt(a.total) + '</span>', '<span class="grande">' + fmt(b.total) + '</span>'],
      ['No ranking', a.posicao + 'º', b.posicao + 'º']];
    d.resumo.anos.forEach(function (ano, i) { linhas.push([String(ano.ano), fmt(a.anos[i] || 0), fmt(b.anos[i] || 0)]); });
    linhas.push(['Variação', variacao(a), variacao(b)], ['Bioma principal', esc(a.bioma || '—'), esc(b.bioma || '—')],
      ['Por 1.000 km²', densidade(a), densidade(b)],
      ['Pico', pico(a) >= 0 ? mesRotulo(pico(a)) + ' (' + fmt(a.meses[pico(a)]) + ')' : '—', pico(b) >= 0 ? mesRotulo(pico(b)) + ' (' + fmt(b.meses[pico(b)]) + ')' : '—']);
    var html = '<table class="comp-tabela"><thead><tr><th><span class="sr">Medida</span></th><th class="a" scope="col">' + esc(a.nome) + '</th><th class="b" scope="col">'
      + esc(b.nome) + '</th></tr></thead><tbody>' + linhas.map(function (l) {
        return '<tr><th scope="row">' + l[0] + '</th><td>' + l[1] + '</td><td>' + l[2] + '</td></tr>';
      }).join('') + '</tbody></table>'
      + '<p class="texto">' + esc(resumoComparacao(a, b)) + '</p>'
      + '<div class="secao-folha"><h3>Focos por mês</h3><p>Mesma escala para os dois.</p></div>'
      + '<div class="legenda-pares"><span>' + esc(a.nome) + '</span><span class="b">' + esc(b.nome) + '</span></div>'
      + '<div class="barras pares">' + a.meses.map(function (v, i) {
        var w = b.meses[i];
        return '<span title="' + mesRotulo(i) + ': ' + fmt(v) + ' × ' + fmt(w) + '"><i class="a" style="height:' + (v === 0 ? 0 : Math.max(3, 100 * v / max))
          + '%"></i><i class="b" style="height:' + (w === 0 ? 0 : Math.max(3, 100 * w / max)) + '%"></i></span>';
      }).join('') + '</div>' + rotulosMeses()
      + '<div class="botoes-folha"><button class="botao primario" type="button" data-acao="mapa"><svg aria-hidden="true"><use href="#i-alvo"/></svg>Ver os dois no mapa</button>'
      + '<button class="botao secundario" type="button" data-acao="voltar">Voltar à ficha de ' + esc(a.nome) + '</button></div>';
    corpo.innerHTML = html;
    corpo.querySelector('[data-acao="mapa"]').addEventListener('click', function () { verNoMapa([a, b]); });
    corpo.querySelector('[data-acao="voltar"]').addEventListener('click', function () { abrirFicha(a); });
  }

  function resumoComparacao(a, b) {
    var s;
    if (a.total === b.total) s = 'Os dois tiveram ' + plural(a.total, 'foco', 'focos') + '.';
    else {
      var maior = a.total > b.total ? a : b, menor = maior === a ? b : a;
      s = maior.nome + ' teve ' + (menor.total === 0 ? fmt(maior.total) + ' focos; ' + menor.nome + ', nenhum.'
        : dec(maior.total / menor.total, 1) + ' vezes os focos de ' + menor.nome + '.');
    }
    if (a.vizinhos.some(function (v) { return v[0] === b.nome; })) s += ' Os dois fazem fronteira.';
    return s;
  }

  function verNoMapa(lista, mes) {
    var abertos = lista.slice();
    fecharFolha(true);
    if (history.state && history.state.folha) history.replaceState(null, '', '#mapa');
    else if (location.hash !== '#mapa') history.pushState(null, '', '#mapa');
    rota();
    setTimeout(function () {
      if (!mapa) montarMapa();
      mapa.invalidateSize();
      if (mes != null) { pausar(); escolherMes(mes); }
      carregarMalha().then(function (g) { destacar(g, abertos); });
    }, 80);
  }

  function destacar(g, lista) {
    camadaDestaque.clearLayers();
    var cores = lista.length > 1 ? [css('--fogo'), css('--frio')] : [css('--fogo')], limites = null;
    lista.forEach(function (m, i) {
      var alvo = g.features.filter(function (f) { return f.properties.chave === m.chave; });
      if (!alvo.length) return;
      var camada = L.geoJSON(alvo, { interactive: false, style: function () {
        return { color: cores[i], weight: 3, fillColor: cores[i], fillOpacity: 0.12 };
      } }).addTo(camadaDestaque);
      limites = limites ? limites.extend(camada.getBounds()) : camada.getBounds();
    });
    if (limites) mapa.fitBounds(limites, { paddingTopLeft: [20, 100], paddingBottomRight: [20, 140], maxZoom: 11 });
  }

  function montarAlgoritmos() {
    var chips = $('#criterios');
    d.algoritmos.tabelas.forEach(function (t, i) {
      var b = el('button', '', esc(rotuloCriterio(t.criterio)));
      b.type = 'button';
      b.setAttribute('role', 'radio');
      b.setAttribute('aria-checked', String(i === 0));
      b.addEventListener('click', function () { ui.tabela = i; marcar(chips, b); listarAlgoritmos(); });
      chips.appendChild(b);
    });
    $('#ordem').querySelectorAll('button').forEach(function (b) {
      b.addEventListener('click', function () { ui.ordem = b.dataset.ordem; marcar($('#ordem'), b); listarAlgoritmos(); });
    });
    listarAlgoritmos();
    montarCurvas();
  }

  function rotuloCriterio(c) {
    return c.replace(/ [↑↓]/g, '').replace(/ → /g, ' → ');
  }

  function marcar(grupo, ativo) {
    grupo.querySelectorAll('button').forEach(function (b) { b.setAttribute('aria-checked', String(b === ativo)); });
  }

  function tempo(ms) { return ms >= 1000 ? dec(ms / 1000, 2) + ' s' : ms >= 10 ? dec(ms, 1) + ' ms' : dec(ms, 2) + ' ms'; }

  function listarAlgoritmos() {
    var t = d.algoritmos.tabelas[ui.tabela], chave = ui.ordem;
    $('#algoritmos-apoio').textContent = 'Os ' + fmt(t.n) + ' focos ordenados por ' + rotuloCriterio(t.criterio).toLowerCase()
      + ', na ordem original do arquivo. Cada número é uma execução real do programa, contada pelo vetor instrumentado.'
      + (t.resultados.length < 13 ? ' Radix e Counting Sort só aparecem nos critérios com chave numérica.' : '');
    var lista = t.ordens[chave].map(function (i) { return t.resultados[i]; });
    var max = Math.max.apply(null, lista.map(function (r) { return r[chave]; }).concat([1]));
    var ol = $('#algoritmos');
    ol.innerHTML = '';
    lista.forEach(function (r) {
      var v = r[chave], frac = Math.log10(1 + v) / Math.log10(1 + max);
      var valor = chave === 'ms' ? tempo(v) : fmt(v);
      var rotulo = chave === 'ms' ? 'tempo' : chave === 'comparacoes' ? (v === 0 ? 'ordena sem comparar' : 'comparações') : 'trocas';
      var li = el('li', '', '<div class="alg-topo"><div class="alg-nome">' + esc(r.algoritmo) + '<small>' + esc(r.complexidade) + ' no caso médio</small></div>'
        + '<div class="alg-valor">' + valor + '<small>' + rotulo + '</small></div></div>'
        + '<div class="alg-barra" aria-hidden="true"><i style="width:' + Math.max(1, 100 * frac) + '%"></i></div>'
        + '<div class="alg-extra"><span><b>' + fmt(r.comparacoes) + '</b> comparações</span><span><b>' + fmt(r.trocas) + '</b> trocas</span>'
        + '<span><b>' + fmt(r.acessos) + '</b> acessos</span><span><b>' + tempo(r.ms) + '</b></span>'
        + (r.verificado ? '<span class="selo">resultado verificado</span>' : '') + '</div>');
      ol.appendChild(li);
    });
  }

  function montarCurvas() {
    var c = d.algoritmos.curvas;
    if (!c) { $('#t-curvas').parentNode.hidden = true; return; }
    var sel = $('#curva-algoritmo');
    c.series.forEach(function (s) {
      var o = el('option', '', esc(s.algoritmo));
      o.value = s.algoritmo;
      if (s.algoritmo === ui.curva) o.selected = true;
      sel.appendChild(o);
    });
    sel.addEventListener('change', function () { ui.curva = sel.value; desenharCurvas(); });
    var largura = 0;
    window.addEventListener('resize', function () {
      var w = $('#curvas').getBoundingClientRect().width;
      if (w && Math.abs(w - largura) > 40) { largura = w; desenharCurvas(); }
    });
    $('#curvas-apoio').textContent = 'Comparações de cada algoritmo conforme a entrada cresce de 100 para ' + fmt(10378)
      + ' focos (critério ' + c.criterio + ', entrada ' + c.cenario.toLowerCase() + '). Escala logarítmica nos dois eixos: a inclinação mostra o expoente do custo.';
    desenharCurvas();
  }

  function desenharCurvas() {
    var c = d.algoritmos.curvas, svg = $('#curvas');
    var W = Math.max(300, Math.min(720, Math.round(svg.getBoundingClientRect().width) || 640)), H = Math.round(W * 0.62);
    var L0 = 50, R = 12, T = 14, B = 30;
    var series = c.series.filter(function (s) { return s.pontos.every(function (p) { return p[1] > 0; }); });
    var xs = [], ys = [];
    series.forEach(function (s) { s.pontos.forEach(function (p) { xs.push(p[0]); ys.push(p[1]); }); });
    var x0 = Math.log10(Math.min.apply(null, xs)), x1 = Math.log10(Math.max.apply(null, xs));
    var y0 = Math.floor(Math.log10(Math.min.apply(null, ys))), y1 = Math.ceil(Math.log10(Math.max.apply(null, ys)));
    function X(v) { return L0 + (Math.log10(v) - x0) / (x1 - x0) * (W - L0 - R); }
    function Y(v) { return T + (1 - (Math.log10(v) - y0) / (y1 - y0)) * (H - T - B); }
    var g = '<g class="grade">';
    for (var e = y0; e <= y1; e++) g += '<line x1="' + L0 + '" x2="' + (W - R) + '" y1="' + Y(Math.pow(10, e)) + '" y2="' + Y(Math.pow(10, e)) + '"/>';
    g += '</g><g class="eixo">';
    for (var k = y0; k <= y1; k++) g += '<text x="' + (L0 - 8) + '" y="' + (Y(Math.pow(10, k)) + 4) + '" text-anchor="end">' + (k <= 3 ? fmt(Math.pow(10, k)) : '10' + sup(k)) + '</text>';
    [100, 1000, 10000].forEach(function (v) {
      var x = X(v), ancora = x > W - R - 30 ? 'end' : x < L0 + 20 ? 'start' : 'middle';
      g += '<text x="' + (ancora === 'end' ? W - R : x) + '" y="' + (H - 10) + '" text-anchor="' + ancora + '">' + fmt(v) + (ancora === 'end' ? ' focos' : '') + '</text>';
    });
    g += '</g>';
    var ativa = null;
    series.forEach(function (s) {
      var dPath = s.pontos.map(function (p, i) { return (i ? 'L' : 'M') + X(p[0]).toFixed(1) + ',' + Y(p[1]).toFixed(1); }).join('');
      if (s.algoritmo === ui.curva) ativa = { s: s, d: dPath };
      else g += '<path class="serie" d="' + dPath + '"/>';
    });
    if (ativa) {
      g += '<path class="serie ativa" d="' + ativa.d + '"/>';
      ativa.s.pontos.forEach(function (p) { g += '<circle class="ponto" r="4.5" cx="' + X(p[0]) + '" cy="' + Y(p[1]) + '"/>'; });
      var u = ativa.s.pontos[ativa.s.pontos.length - 1];
      g += '<text class="rotulo" x="' + (X(u[0]) - 8) + '" y="' + (Y(u[1]) - 10) + '" text-anchor="end">' + esc(ativa.s.algoritmo) + '</text>';
    }
    svg.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
    svg.innerHTML = g;
    var fora = c.series.length - series.length;
    $('#curvas-legenda').textContent = 'Linhas cinza: os outros algoritmos. Fonte: benchmark gravado com o programa (média das repetições).'
      + (fora ? ' Radix Sort não aparece porque não faz comparações.' : '');
    var tab = $('#curva-tabela');
    tab.innerHTML = '<thead><tr><th>n</th><th>Comparações</th><th>Tempo médio</th></tr></thead><tbody>' + (ativa ? ativa.s.pontos.map(function (p) {
      return '<tr><td>' + fmt(p[0]) + '</td><td>' + fmt(p[1]) + '</td><td>' + tempo(p[2]) + '</td></tr>';
    }).join('') : '') + '</tbody>';
    tab.setAttribute('aria-label', 'Valores de ' + ui.curva);
  }

  function sup(n) { return String(n).split('').map(function (c) { return '⁰¹²³⁴⁵⁶⁷⁸⁹'[+c]; }).join(''); }

  function montarSobre() {
    var r = d.resumo, pk = r.meses.indexOf(Math.max.apply(null, r.meses));
    var ult = r.anos[r.anos.length - 1], pri = r.anos[0];
    var itens = [['Focos na base', fmt(r.total)], ['Municípios com focos', fmt(r.municipiosComFocos)],
      ['Pico: ' + mesRotulo(pk), fmt(r.meses[pk])],
      [ult.ano + ' em relação a ' + pri.ano, pri.total ? dec(ult.total / pri.total, 1) + '×' : '—']];
    $('#sobre-numeros').innerHTML = itens.map(function (i) { return '<div><dt>' + esc(i[0]) + '</dt><dd>' + esc(i[1]) + '</dd></div>'; }).join('');
    $('#sobre-versao').textContent = 'Dados gerados pelo APS Queimadas ' + r.versao + ' em ' + r.geradoEm.split('-').reverse().join('/') + '. ' + r.fontes;
    var dica = $('#instalar-dica');
    dica.textContent = /iphone|ipad/i.test(navigator.userAgent)
      ? 'No iPhone: toque em Compartilhar e depois em "Adicionar à Tela de Início" para abrir como app, até sem internet.'
      : 'No celular, use "Instalar app" ou "Adicionar à tela inicial" no menu do navegador para abrir como app, até sem internet.';
  }

  function ligarFolha() {
    $('#fechar-folha').addEventListener('click', function () { fecharFolha(); });
    $('#fundo-folha').addEventListener('click', function () { fecharFolha(); });
    $('#alca').addEventListener('click', function () { fecharFolha(); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && ui.folha) fecharFolha(); });
    window.addEventListener('popstate', function () { if (ui.folha) fecharFolha(true); });
    var f = $('#folha'), y0 = 0, t0 = 0, arrastando = false;
    function inicio(e) {
      if (window.innerWidth >= 900) return;
      arrastando = true; y0 = e.clientY; t0 = Date.now();
      f.style.transition = 'none';
      e.target.setPointerCapture && e.target.setPointerCapture(e.pointerId);
    }
    function mover(e) {
      if (!arrastando) return;
      var dy = e.clientY - y0;
      f.style.transform = 'translateY(' + (dy > 0 ? dy : dy / 6) + 'px)';
    }
    function fim(e) {
      if (!arrastando) return;
      arrastando = false;
      var dy = e.clientY - y0, v = dy / Math.max(1, Date.now() - t0);
      f.style.transition = '';
      if (dy > 120 || v > 0.5) fecharFolha();
      else f.style.transform = '';
    }
    [$('#alca'), $('.folha-cabeca')].forEach(function (alvo) {
      alvo.addEventListener('pointerdown', inicio);
      alvo.addEventListener('pointermove', mover);
      alvo.addEventListener('pointerup', fim);
      alvo.addEventListener('pointercancel', fim);
    });
  }

  if ('serviceWorker' in navigator && location.protocol !== 'file:') {
    window.addEventListener('load', function () { navigator.serviceWorker.register('sw.js').catch(function () {}); });
  }
  document.addEventListener('DOMContentLoaded', function () { ligarFolha(); iniciar(); });
})();
