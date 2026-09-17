(ns chat.core
  "P2P terminal chat over Hyperswarm, meant to run on the Bare runtime
   (directly, or launched via `pear run`) — and to be driven live from
   a connected REPL (e.g. Calva in VS Code) during development.

   Two ways to send messages once it's running:
     1. Type a line into the process's stdin + Enter.
     2. From a connected REPL: (chat.core/send! \"hello\")

   Standalone usage:
     bare build/index.js [topic-hex]
     pear run -d . [topic-hex]

   REPL usage (see README for the Calva side):
     shadow-cljs watch app
     ;; then connect Calva to the shadow-cljs nREPL, pick build \"app\"
     (chat.core/status)
     (chat.core/send! \"hello from the repl\")"
  (:require ["hyperswarm" :as Hyperswarm]
            ["b4a" :as b4a]
            ["crypto" :as crypto]))

;; ---------------------------------------------------------------------
;; State — `defonce` so a REPL hot-reload doesn't blow away a live swarm
;; or drop connected peers.
;; ---------------------------------------------------------------------

(defonce peers (atom {}))          ;; id -> socket
(defonce swarm (atom nil))         ;; the Hyperswarm instance, once started
(defonce topic-hex* (atom nil))    ;; the room we're in, once started
(defonce started? (atom false))

(defn timestamp []
  (-> (js/Date.) .toTimeString (.slice 0 8)))

(defn log [& parts]
  (println (str "[" (timestamp) "] " (apply str parts))))

(defn random-topic-hex []
  (-> (.randomBytes crypto 32) (.toString "hex")))

(defn short-id [id]
  (subs (str id) 0 8))

;; ---------------------------------------------------------------------
;; Sending / receiving
;; ---------------------------------------------------------------------

(defn broadcast!
  "Write raw `line` to every currently-connected peer socket."
  [line]
  (doseq [[_ socket] @peers]
    (.write socket (str line "\n"))))

(defn send!
  "The REPL-facing entry point: evaluate `(chat.core/send! \"hi\")` from
   Calva (or any connected nREPL client) to broadcast a message to every
   peer currently connected to the swarm."
  [text]
  (if (empty? @peers)
    (log "(no peers connected yet — nothing sent: " text ")")
    (do
      (log "me> " text)
      (broadcast! text)))
  nil)

(defn status
  "Quick REPL check: are we joined, on what topic, and to how many peers."
  []
  {:started    @started?
   :topic      @topic-hex*
   :peer-count (count @peers)
   :peer-ids   (keys @peers)})

;; ---------------------------------------------------------------------
;; Connection handling
;; ---------------------------------------------------------------------

(defn handle-connection!
  [socket info]
  (let [id (short-id (or (some-> (.-publicKey info) (.toString "hex"))
                         (random-topic-hex)))]
    (swap! peers assoc id socket)
    (log "* peer " id " joined — " (count @peers) " connected *")

    (.on socket "data"
         (fn [data]
           (let [text (.trim (b4a/toString data))]
             (when (not= text "")
               (log id "> " text)))))

    (.on socket "close"
         (fn []
           (swap! peers dissoc id)
           (log "* peer " id " left — " (count @peers) " connected *")))

    (.on socket "error"
         (fn [err]
           (log "! connection error from " id ": " (some-> err .-message))))))

;; ---------------------------------------------------------------------
;; stdin (optional — you can chat by typing directly into the process
;; instead of / as well as sending from the REPL)
;; ---------------------------------------------------------------------

(defn read-stdin-lines!
  [on-line]
  (let [stdin js/process.stdin
        buf   (atom "")]
    (.setEncoding stdin "utf8")
    (.on stdin "data"
         (fn [chunk]
           (swap! buf str chunk)
           (loop []
             (let [s (deref buf)
                   i (.indexOf s "\n")]
               (when (>= i 0)
                 (let [line (.slice s 0 i)
                       rest (.slice s (inc i))]
                   (reset! buf rest)
                   (on-line (.trim line))
                   (recur)))))))))

;; ---------------------------------------------------------------------
;; Startup
;; ---------------------------------------------------------------------

(defn join!
  "Join (or create) a chat room. Safe to call again with a new
   topic-hex from the REPL — it leaves the current swarm first."
  [topic-hex]
  (when @swarm
    (log "leaving current room " @topic-hex* "...")
    (.destroy @swarm)
    (reset! peers {}))

  (let [hex   (or topic-hex (random-topic-hex))
        topic (b4a/from hex "hex")
        sw    (Hyperswarm.)]

    (reset! swarm sw)
    (reset! topic-hex* hex)

    (when-not topic-hex
      (log "no topic given — created a new room:")
      (log "  " hex)
      (log "share that string with a peer so they can join with:")
      (log "  pear run -d . " hex))

    (log "joining topic " hex "...")
    (.on sw "connection" handle-connection!)

    (-> (.join sw topic #js {:client true :server true})
        .flushed
        (.then #(log "joined the swarm — waiting for peers")))
    nil))

(defn main [& args]
  (when-not @started?
    (reset! started? true)
    (join! (first args))
    (read-stdin-lines!
     (fn [line]
       (when (not= line "")
         (send! line)))))
  nil)

(defn reload!
  "Called by shadow-cljs after a hot-reload. Intentionally does nothing —
   the defonce'd swarm/peers keep running across code reloads, so you
   can redefine send!/handle-connection!/etc. from the REPL without
   dropping your connections."
  [])
