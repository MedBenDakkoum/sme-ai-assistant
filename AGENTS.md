# Contexte Projet — rag-mcp-assistant

Ce fichier définit les règles et conventions à respecter pour toute tâche future sur ce projet.

---

## Architecture

- **Monolithe modulaire Spring Boot**, organisé par domaine : `document`, `chat`, `user`, `mcp`, `llm`, `knowledge`.
- **Pas de microservices** — seule exception : le serveur MCP peut être extrait en service séparé (potentiellement dans un autre langage, ex. Python) si le SDK MCP Java s'avère trop limité. Dans tous les cas, il reste orchestré via le même docker-compose.yml.
- Structure en couches classique : `Controller → Service → Repository JPA` (pas d'interfaces inutiles).

## Conventions de code

- **Abstraction via interface Java uniquement là où c'est utile** (ex. `LlmClient` → `OpenRouterClient`).
- Sinon : **Controller → Service → Repository JPA direct**, sans interfaces superflues.
- Packages par fonctionnalité (feature-based), pas par couche technique.

## Stack technique

- **Backend** : Spring Boot 3 + Spring AI
- **Base de données** : PostgreSQL + pgvector via **Docker uniquement** (aucune installation locale)
- **Frontend** : Angular (standalone components) + Tailwind CSS
- **Pas de librairie de composants UI** (Angular Material, PrimeNG, etc. interdits)

## Contrainte stricte anti-hallucination

> Le LLM ne doit **répondre qu'à partir des chunks retournés par la recherche sémantique**.  
> Si l'information n'est pas dans les chunks → répondre **« Je ne sais pas »**.

Cette règle s'applique à tous les prompts, chains, et endpoints RAG.

## Lancement & Déploiement

- **Une seule commande** pour tout démarrer en dev/prod :
  ```bash
  docker compose up -d --build
  ```
- Le `docker-compose.yml` à la racine orchestre : PostgreSQL/pgvector, backend Spring Boot, frontend Angular (via nginx ou dev server), et serveur MCP si activé.

---

## Rappels pour l'agent

- Ne jamais proposer d'installation locale de PostgreSQL/pgvector.
- Ne jamais ajouter de librairie de composants UI Angular.
- Ne jamais créer d'interfaces Java sans justification (mock/test ou multi-implémentation réelle).
- Toujours valider que le RAG respecte la contrainte anti-hallucination avant de livrer du code LLM.
- Tout changement doit rester compatible avec `docker compose up -d --build`.

# UI/UX Pro Max

## Layout
- Mobile-first.
- Max width: `max-w-7xl mx-auto`.
- Padding: `px-6 md:px-8`.
- Section: `py-16 md:py-24`.
- Gap: `gap-4 md:gap-6`.
- Radius: `rounded-2xl`.
- Grid. Éviter hauteurs fixes.

## Dark Theme
- Background: `bg-zinc-950`.
- Surface: `bg-zinc-900/70`.
- Border: `border border-white/10`.
- Text: `text-zinc-100`.
- Muted: `text-zinc-400`.
- Accent: `bg-gradient-to-r from-indigo-500 to-cyan-500`.

## Glass
- `backdrop-blur-xl`.
- `bg-white/5`.
- `border-white/10`.
- `shadow-xl shadow-black/20`.

## Typography
- Base: `text-base`.
- Title: `text-3xl md:text-5xl font-bold`.
- Subtitle: `text-lg text-zinc-400`.
- Line-height: `leading-relaxed`.
- Maximum trois tailles.

## Spacing
- Éviter éléments collés.
- Hiérarchie visuelle claire.
- Espaces réguliers.
- Alignements cohérents.

## Cards
- `rounded-2xl`.
- `border border-white/10`.
- `bg-zinc-900/60`.
- `shadow-lg shadow-black/20`.
- `p-6`.

## Buttons
- `rounded-xl`.
- `px-5 py-3`.
- `font-medium`.
- Gradient accent.
- `shadow-lg`.

## Inputs
- `rounded-xl`.
- `bg-zinc-900`.
- `border-white/10`.
- `px-4 py-3`.
- Focus visible.

## Hover
- `hover:scale-[1.02]`.
- `hover:shadow-xl`.
- `hover:border-white/20`.

## Focus
- `focus:outline-none`.
- `focus:ring-2`.
- `focus:ring-indigo-500/60`.

## Active
- `active:scale-[0.98]`.

## Transition
- `transition-all`.
- `duration-200`.
- `ease-out`.

## Icons
- Taille uniforme.
- Style unique.
- Emoji interdits.

## Accessibility
- Contraste élevé.
- Labels visibles.
- Focus clavier.
- Cibles ≥44px.

## Responsive
- Empiler sur mobile.
- Grilles desktop.
- Aucun scroll horizontal.

## Avoid
- Ombres fortes.
- Bordures épaisses.
- Trop de couleurs.
- Trop de gradients.
- Trop d'animations.
- Espacements incohérents.
- Texte gris faible.
- Coins mélangés.
- Durées >300ms.
```

## Constraints & Access
- Uniform icon sizing.
- Zero emojis in text/UI.
- Interactive targets >= 44px.
- High contrast placeholder vs text.
- Keyboard accessible navigation patterns.
- Keep terms and nouns strictly consistent.
- Format numbers/units with non-breaking space (e.g., 10&nbsp;MB).
- Error messages must dictate a clear solution, never just state failure.
