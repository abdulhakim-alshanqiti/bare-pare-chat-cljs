# bare-pare-chat

A peer-to-peer terminal chat app that runs on Holepunch's **Bare** runtime
(and can be launched through **Pear**), written in **ClojureScript**
and compiled to JS with `shadow-cljs`.

Networking is handled by `hyperswarm` — peers who join the same topic
(a 32-byte hex string) find each other over the DHT and get a direct
encrypted stream, no server involved. Anything you type gets broadcast
to every connected peer; anything they type shows up in your terminal.

## Layout

```
bare-pare-chat/
├── package.json          # npm + Pear app manifest
├── shadow-cljs.edn        # ClojureScript build config
├── src/chat/core.cljs     # app source (this is what you edit)
├── build/index.js         # compiled output (generated, checked in as placeholder)
└── README.md
```

## Setup

```bash
npm install          # pulls in hyperswarm, b4a, shadow-cljs
npm run build         # compiles src/chat/core.cljs -> build/index.js
```

`npm run watch` recompiles on save while you're developing.

## Running

Pick a topic to chat about — any 32-byte hex string works as the shared
"room". If you don't pass one, the app generates a fresh one and prints
it so you can share it with whoever you want to talk to.

Through Pear (recommended — this is a Pear "terminal" app):

```bash
pear run -d . 
```

Or directly on the Bare runtime, if you have it installed:

```bash
bare build/index.js [topic-hex]
```

To chat with yourself locally, open two terminals:

```bash
# terminal 1 — creates a new room and prints its topic
pear run -d .

# terminal 2 — paste the topic printed by terminal 1
pear run -d . 5f3a9c...  
```

Type a line + Enter to send it to every connected peer. Ctrl+C to quit.

## Chatting from a Calva REPL instead of stdin

Because networking (`hyperswarm`) doesn't need the Bare/Pear runtime
specifically — it runs on plain Node too — the easiest dev loop is to
run the app under `shadow-cljs watch`, connect Calva to it, and call
`chat.core/send!` directly as you type in your editor. (You'd only run
it through `bare`/`pear` for the real deployed version.)

1. **Start the app with a REPL attached:**

   ```bash
   npm install
   npx shadow-cljs watch app
   ```

   This compiles `src/chat/core.cljs`, immediately runs it as a Node
   process (so it joins the swarm right away and prints a topic hex
   the same way `bare`/`pear` would), and opens an nREPL server on the
   port set in `shadow-cljs.edn` (`9000` by default). It also writes a
   `.nrepl-port` file to the project root.

2. **Connect Calva to it, from VS Code, in this project folder:**

   - Command palette → **"Calva: Connect to a Running REPL Server in
     the Project"**
   - Pick project type **shadow-cljs**
   - Pick build **`app`**

   Calva will attach and drop you into the `chat.core` namespace of
   the *already-running* process — the same one that's holding the
   live `Hyperswarm` connection and peer sockets.

3. **Send messages by evaluating forms** in `core.cljs` (or the REPL
   prompt) with Calva's usual eval-at-cursor / eval-top-form shortcuts:

   ```clojure
   (chat.core/status)               ;; check topic + connected peers
   (chat.core/send! "hey there")    ;; broadcast to every connected peer
   (chat.core/join! "abc123...")    ;; switch to a different room
   ```

   Anything a peer sends still prints to the process's own stdout
   (visible in the integrated terminal where you ran `shadow-cljs
   watch`, and also over the REPL's `*out*` in Calva's output pane).

Because the state (`peers`, `swarm`, `topic-hex*`) is held in
`defonce`'d atoms, redefining functions and re-evaluating code from
Calva during development doesn't drop your peer connections.

## Notes on the Bare/Pear side

- `hyperswarm` and `b4a` both work unmodified under Bare, so the exact
  same compiled `build/index.js` runs whether you launch it with `pear
  run` or `bare` directly.
- The `pear` block in `package.json` marks this as a **terminal** type
  app (no GUI), which is what tells Pear to just execute `main` in the
  Bare runtime instead of opening a window.
- `shadow-cljs`'s `:node-script` target is used because it emits a
  single CommonJS-style entry point that `require`s its npm deps at
  runtime the normal Node/Bare way — no bundler-specific runtime needed,
  which keeps it Bare-compatible.
