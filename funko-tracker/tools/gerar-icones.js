#!/usr/bin/env node
'use strict';

// Gera os ícones PNG do Funko Tracker sem dependências externas.
// Uso: node tools/gerar-icones.js   (a partir da pasta funko-tracker)

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const FUNDO = [0x0f, 0x0f, 0x0f];
const ROSA = [0xe8, 0x43, 0x93];
const ESCALA = 0.74; // mantém o desenho dentro da zona segura dos ícones maskable
const AMOSTRAS = 4;  // supersampling 4x4 para suavizar as bordas

// ---------- desenho ----------

function dentroRetArredondado(x, y, cx, cy, w, h, r) {
  const dx = Math.abs(x - cx);
  const dy = Math.abs(y - cy);
  if (dx > w / 2 || dy > h / 2) return false;
  const qx = Math.max(dx - (w / 2 - r), 0);
  const qy = Math.max(dy - (h / 2 - r), 0);
  return qx * qx + qy * qy <= r * r;
}

function dentroCirculo(x, y, cx, cy, r) {
  return (x - cx) ** 2 + (y - cy) ** 2 <= r * r;
}

// Silhueta de um Pop: cabeça grande, corpo pequeno e dois olhos.
function corNoPonto(xn, yn) {
  const x = 0.5 + (xn - 0.5) / ESCALA;
  const y = 0.5 + (yn - 0.5) / ESCALA;
  const olho = dentroCirculo(x, y, 0.38, 0.44, 0.055) || dentroCirculo(x, y, 0.62, 0.44, 0.055);
  const cabeca = dentroRetArredondado(x, y, 0.5, 0.40, 0.66, 0.50, 0.16);
  const corpo = dentroRetArredondado(x, y, 0.5, 0.76, 0.38, 0.26, 0.08);
  if (olho) return FUNDO;
  if (cabeca || corpo) return ROSA;
  return FUNDO;
}

function desenhar(tamanho) {
  const pixels = Buffer.alloc(tamanho * tamanho * 3);
  for (let py = 0; py < tamanho; py++) {
    for (let px = 0; px < tamanho; px++) {
      let r = 0, g = 0, b = 0;
      for (let sy = 0; sy < AMOSTRAS; sy++) {
        for (let sx = 0; sx < AMOSTRAS; sx++) {
          const c = corNoPonto((px + (sx + 0.5) / AMOSTRAS) / tamanho, (py + (sy + 0.5) / AMOSTRAS) / tamanho);
          r += c[0]; g += c[1]; b += c[2];
        }
      }
      const n = AMOSTRAS * AMOSTRAS;
      const i = (py * tamanho + px) * 3;
      pixels[i] = Math.round(r / n);
      pixels[i + 1] = Math.round(g / n);
      pixels[i + 2] = Math.round(b / n);
    }
  }
  return pixels;
}

// ---------- PNG escrito à mão ----------

const TABELA_CRC = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = TABELA_CRC[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function bloco(tipo, dados) {
  const tam = Buffer.alloc(4);
  tam.writeUInt32BE(dados.length);
  const tipoEDados = Buffer.concat([Buffer.from(tipo, 'ascii'), dados]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(tipoEDados));
  return Buffer.concat([tam, tipoEDados, crc]);
}

function codificarPng(tamanho, pixels) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(tamanho, 0);
  ihdr.writeUInt32BE(tamanho, 4);
  ihdr[8] = 8;  // 8 bits por canal
  ihdr[9] = 2;  // RGB, sem transparência (exigência do ícone da Apple)
  ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;

  const linha = tamanho * 3;
  const bruto = Buffer.alloc((linha + 1) * tamanho);
  for (let y = 0; y < tamanho; y++) {
    bruto[y * (linha + 1)] = 0; // filtro "None"
    pixels.copy(bruto, y * (linha + 1) + 1, y * linha, (y + 1) * linha);
  }

  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    bloco('IHDR', ihdr),
    bloco('IDAT', zlib.deflateSync(bruto, { level: 9 })),
    bloco('IEND', Buffer.alloc(0)),
  ]);
}

// ---------- saída ----------

const destino = path.join(__dirname, '..', 'icons');
fs.mkdirSync(destino, { recursive: true });

const icones = [
  ['icon-192.png', 192],
  ['icon-512.png', 512],
  ['apple-touch-icon.png', 180],
];

for (const [nome, tamanho] of icones) {
  const arquivo = path.join(destino, nome);
  fs.writeFileSync(arquivo, codificarPng(tamanho, desenhar(tamanho)));
  console.log(`${nome}: ${tamanho}x${tamanho} (${fs.statSync(arquivo).size} bytes)`);
}
