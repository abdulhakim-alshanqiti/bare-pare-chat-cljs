(ns chat.repl
  "A scratch file for driving the chat app from a connected REPL
   (Calva, or any nREPL client) — start to finish.

   How to use this file in Calva:
     1. Make sure `npx shadow-cljs watch app` is running in a terminal
        (see README.md) and Calva is connected to the `app` build.
     2. Open this file. Calva's default keybinding for \"evaluate the
        top-level form the cursor is in\" is alt+enter (Cmd+Enter on
        Mac) — put your cursor in a form below and hit it, top to
        bottom.
     3. Everything below is wrapped in (comment ...) so loading/eval'ing
        the whole file does nothing by itself — you eval one form at a
        time as you go."
  (:require [chat.core :as chat]))

(comment

  ;; ---------------------------------------------------------------
  ;; 1. Check what's already running.
  ;;
  ;;    `shadow-cljs watch app` auto-starts chat.core/main, which
  ;;    joins a random topic immediately — so there's usually already
  ;;    a swarm up by the time you connect. This just tells you what
  ;;    it's doing.
  ;; ---------------------------------------------------------------

  (chat/status)
  ;; => {:started true, :topic "3fa9...", :peer-count 0, :peer-ids ()}


  ;; ---------------------------------------------------------------
  ;; 2. (Optional) join a specific room instead of the random one
  ;;    main picked. Useful if a friend already gave you a topic hex,
  ;;    or you want a fixed room while testing across two terminals.
  ;;
  ;;    Safe to call more than once — it leaves the current swarm
  ;;    first.
  ;; ---------------------------------------------------------------

  (chat/join! "a0d4b53d821e1eee73cd078d5de879e56f70712cf7aa56e52ef43f82bfae9896")

  ;; ...or go back to a fresh random room:
  (chat/join! nil)


  ;; ---------------------------------------------------------------
  ;; 3. Get a peer to join you.
  ;;
  ;;    Grab the topic hex chat/status printed and either:
  ;;      - hand it to a friend running `pear run -d . <topic>`, or
  ;;      - open a second terminal yourself and run:
  ;;          npx shadow-cljs watch app
  ;;        then, once it's up, from that second REPL:
  ;;          (chat.core/join! "<the topic hex from step 1>")
  ;;
  ;;    Watch this file's connected terminal — you should see:
  ;;      "* peer XXXXXXXX joined — 1 connected *"
  ;; ---------------------------------------------------------------

  (chat/status) ;; re-check — :peer-count should now be > 0


  ;; ---------------------------------------------------------------
  ;; 4. Send messages. This is the main thing you'll re-run.
  ;; ---------------------------------------------------------------

  (chat/send! "hello from the repl")
  (chat/send! "this one too")


  ;; ---------------------------------------------------------------
  ;; 5. Whatever peers send back prints automatically to stdout of the
  ;;    process shadow-cljs is running (the terminal where you ran
  ;;    `npx shadow-cljs watch app`) — no need to poll for it, but you
  ;;    can always recheck the peer count here:
  ;; ---------------------------------------------------------------

  (chat/status)


  ;; ---------------------------------------------------------------
  ;; 6. Done — leave the swarm cleanly.
  ;; ---------------------------------------------------------------

  (when @chat/swarm
    (.destroy @chat/swarm)))