const fs = require('fs');
const path = require('path');

const I18N = path.join(__dirname, '..', 'src', 'assets', 'i18n');
const TARGETS = ['en', 'de', 'ar'];
const API = 'https://openrouter.ai/api/v1/chat/completions';
const MODEL = 'openai/gpt-4o-mini';

const SYSTEM = 'Vous êtes un traducteur professionnel. Traduisez le contenu du JSON fourni en ' +
  '{LANG}, dans un français->{LANG} professionnel adapté au contexte B2B pour PME, ton formel et concis. ' +
  'RÉPONDEZ UNIQUEMENT avec le JSON traduit : mêmes clés EXACTES, mêmes placeholders {{...}}, seules les valeurs changent. ' +
  'Pour l\'arabe, utilisez l\'arabe standard moderne (MSA).';

async function translate(lang, source) {
  const res = await fetch(API, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${process.env.OPENROUTER_API_KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: MODEL,
      response_format: { type: 'json_object' },
      messages: [
        { role: 'system', content: SYSTEM.replace('{LANG}', lang) },
        { role: 'user', content: JSON.stringify(source, null, 2) }
      ]
    })
  });
  if (!res.ok) throw new Error(`OpenRouter ${res.status}: ${await res.text()}`);
  const data = await res.json();
  return data.choices[0].message.content;
}

function extractJson(content) {
  const cleaned = content.replace(/^```(?:json)?\s*/m, '').replace(/```\s*$/m, '');
  try { return JSON.parse(cleaned); } catch { throw new Error('Réponse LLM non-JSON: ' + content.slice(0, 200)); }
}

function assertKeys(a, b, lang, ctx = '') {
  for (const k of Object.keys(a)) {
    const p = ctx ? `${ctx}.${k}` : k;
    if (typeof a[k] === 'object' && a[k] !== null) {
      if (!b[k] || typeof b[k] !== 'object') throw new Error(`[${lang}] clé manquante: ${p}`);
      assertKeys(a[k], b[k], lang, p);
    } else if (!(k in b)) {
      throw new Error(`[${lang}] clé manquante: ${p}`);
    }
  }
  if (Object.keys(b).length !== Object.keys(a).length) throw new Error(`[${lang}] clés en trop`);
}

(async () => {
  const key = process.env.OPENROUTER_API_KEY;
  if (!key) { console.error('ERREUR: OPENROUTER_API_KEY absente. Ajoutez-la dans .env (racine) ou exportez-la.'); process.exit(1); }
  const source = JSON.parse(fs.readFileSync(path.join(I18N, 'fr.json'), 'utf8'));
  for (const lang of TARGETS) {
    console.log(`→ traduction ${lang}...`);
    const out = extractJson(await translate(lang, source));
    assertKeys(source, out, lang);
    fs.writeFileSync(path.join(I18N, `${lang}.json`), JSON.stringify(out, null, 2) + '\n');
    console.log(`  ✓ ${lang}.json écrit (${Object.keys(out).length} clés top-level)`);
  }
  console.log('Terminé.');
})().catch(e => { console.error(e.message); process.exit(1); });